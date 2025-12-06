package asm;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableAnnotationNode;
import org.objectweb.asm.tree.TypeAnnotationNode;
import org.objectweb.asm.TypePath;

import static com.github.david32768.jynxfor.my.Message.*;
import static com.github.david32768.jynxfree.jynx.Global.*;
import static com.github.david32768.jynxfree.jynx.GlobalOption.*;
import static com.github.david32768.jynxfree.jynx.ReservedWord.*;
import static com.github.david32768.jynxfree.jvm.StandardAttribute.Code;
import static com.github.david32768.jynxfree.jvm.StandardAttribute.StackMapTable;

import com.github.david32768.jynxfor.node.JynxCatchNode;
import com.github.david32768.jynxfor.node.JynxCodeNode;
import com.github.david32768.jynxfor.node.JynxCodeNodeBuilder;
import com.github.david32768.jynxfor.ops.JvmOp;
import com.github.david32768.jynxfor.scan.JynxScanner;
import com.github.david32768.jynxfor.scan.Line;
import com.github.david32768.jynxfor.scan.LinesIterator;
import com.github.david32768.jynxfor.scan.Token;
import com.github.david32768.jynxfor.scan.TokenArray;

import com.github.david32768.jynxfree.jvm.Context;
import com.github.david32768.jynxfree.jvm.FrameType;
import com.github.david32768.jynxfree.jvm.JvmVersion;
import com.github.david32768.jynxfree.jvm.TypeRef;
import com.github.david32768.jynxfree.jynx.Directive;
import com.github.david32768.jynxfree.jynx.NameDesc;
import com.github.david32768.jynxfree.jynx.ReservedWord;

import jynx2asm.*;
import jynx2asm.frame.MethodParameters;

public class JynxCodeHdr implements ContextDependent {

    private final JynxScanner js;
    private final int errorsAtStart;
    private final StackLocals stackLocals;
    private final EnumMap<ReservedWord,Integer> options;
    private final JynxLabelMap labelmap;
    private final String2Insn s2a;
    private final UniqueDirectiveChecker unique_checker;
    private final JynxCodeNodeBuilder codeBuilder;

    private List<Object> localStack;
    private int printFlag = 0;
    private int endif = 0;
    
    
    private JynxCodeHdr(JynxScanner js, StackLocals stackLocals,
            List<Object> localStack, String2Insn s2a, JynxCodeNodeBuilder codeBuilder) {
        
        this.js = js;
        this.errorsAtStart = LOGGER().numErrors();
        this.stackLocals = stackLocals;
        this.s2a = s2a;
        this.labelmap = s2a.getLabelMap();
        this.unique_checker = new UniqueDirectiveChecker();
        this.options = new EnumMap<>(ReservedWord.class);
        this.codeBuilder = codeBuilder;
        this.localStack = localStack;
    }

    public static JynxCodeHdr getInstance(JynxScanner js, MethodParameters parameters, String2Insn s2a) {
        
        CHECK_SUPPORTS(Code);
        var codeBuilder = new JynxCodeNodeBuilder();
        StackLocals stackLocals = StackLocals.getInstance(parameters, s2a.getLabelMap(), codeBuilder);
        return new JynxCodeHdr(js, stackLocals, parameters.getInitFrame(), s2a, codeBuilder);
    }

    @Override
    public Context getContext() {
        return Context.CODE;
    }

    @Override
    public void visitDirective(Directive dir, JynxScanner js) {
        Line line = js.getLine();
        unique_checker.checkUnique(dir, line);
        switch(dir) {
            case dir_limit -> setLimit(line);
            case dir_catch -> setCatch(line);
            case dir_var -> visitVar(line);
            case dir_line -> visitLineNumber(line);
            case dir_stack -> {
                LOGGER().pushContext();
                visitStackFrame(line);
                LOGGER().popContext();
            }
            case dir_print -> setPrint(line);
            case state_opcode -> visitInsn(line);
            case dir_if -> visitIf(line);
            case end_if -> visitEndIf();
            default -> visitCommonDirective(dir, line, js);
        }
    }
    
    private void setCatch(Line line) {
        Token token = line.nextToken();
        String exception = token.is(res_all)?null:token.asString(); // all = finally
        String fromname = line.after(res_from);
        String toname = line.after(res_to);
        String usingname = line.after(res_using);
        line.noMoreTokens();
        JynxCatchNode jcatch =  labelmap.getCatch(fromname, toname, usingname, exception, line);
        if (jcatch != null) {
            stackLocals.visitTryCatchBlock(jcatch);
            codeBuilder.addCatch(jcatch);
        }
    }

    private static final EnumSet<ReservedWord> PRINT_OPTIONS = EnumSet.of(res_expand,res_stack,res_locals,res_offset);

    private void setPrint(Line line) {
        ReservedWord rw  = line.nextToken().expectOneOf(res_on, res_off, res_label);
        switch (rw) {
            case res_label -> {
                String lab = line.nextToken().asString();
                String labelframe = labelmap.printJynxlabelFrame(lab, line);
                LOG(M995,labelframe); // "%s"
            }
            case res_on -> {
                ++printFlag;
                Token token = line.nextToken();
                if (token.isEndToken()) {
                    PRINT_OPTIONS.forEach(opt -> options.putIfAbsent(opt,printFlag));
                } else {
                    while (!token.isEndToken()) {
                        ReservedWord optrw = token.expectOneOf(PRINT_OPTIONS);
                        options.putIfAbsent(optrw,printFlag);
                        token = line.nextToken();
                    }
                }
                LOG(M293,options); // "print options = %s"
                Integer localsct = options.get(res_locals);
                if (localsct != null && localsct == 1) {
                    LOG(M294, res_locals, stackLocals.stringLocals()); // "%s = %s"
                }
            }
            case res_off -> {
                if (printFlag > 0) {
                    PRINT_OPTIONS.stream()
                            .filter(opt -> (Objects.equals(printFlag, options.get(opt))))
                            .forEach(opt -> options.remove(opt));
                    --printFlag;
                    LOG(M329,options); // "print options = %s"
                } else {
                    // "%s nest level is already zero"
                    LOG(M328,Directive.dir_print);
                }
            }
            default -> throw new AssertionError();
        }
        line.noMoreTokens();
    }
    
    private void visitIf(Line line) {
        Token rw = line.lastToken();
        rw.mustBe(res_reachable);
        if (stackLocals.isUnreachableForwards()) {
            js.skipNested(Directive.dir_if,Directive.end_if,
                    EnumSet.of(Directive.dir_line,Directive.dir_stack,Directive.end_stack));
        } else {
            ++endif;
        }
    }
    
    private void visitEndIf() {
        assert endif > 0;
        --endif;
    }

    private void visitInsn(Line line) {
        InstList instlist = new InstList(stackLocals,line,options);
        s2a.getInsts(instlist);
        instlist.accept(codeBuilder);
    }
    
    private void visitLineNumber(Line line) {
        InstList instlist = new InstList(stackLocals,line,options);
        s2a.add(JvmOp.xxx_line, instlist);
        instlist.accept(codeBuilder);
    }

    private Object getAsmFrameType(FrameType ft,Line line) {
        Object frame;
        if (ft.extra()) {
            String arg = line.nextToken().asString(); // verification arg
            switch (ft) {
                case ft_Uninitialized -> frame = labelmap.codeUseOfJynxLabel(arg, line).asmlabel();
                case ft_Object -> {
                    NameDesc.FRAME_NAME.validate(arg);
                    frame = arg;
                }
                default -> throw new EnumConstantNotPresentException(ft.getClass(), ft.name());
            }
        } else {
            frame = ft.asmType();
        }
        return frame;
    }

    private void visitStackFrame(Line line) {
        List<Object> frame_local = new ArrayList<>();
        Token use = line.nextToken();
        if (!use.isEndToken()) {
            use.mustBe(res_use);
            Token nstr = line.nextToken();
            int n;
            if (nstr.is(res_locals)) {
                n = localStack.size();
            } else {
                n = nstr.asUnsignedShort();
                line.nextToken().mustBe(res_locals);
                if (n > localStack.size()) {
                    LOG(M188,n, localStack.size());  // "n (%d) is greater than current local size(%d)"
                    n = localStack.size();
                }
            }
            frame_local.addAll(localStack.subList(0, n));
        }
        line.noMoreTokens();
        Line dirline = line;

        List<Object> frame_stack = new ArrayList<>();
        try (LinesIterator lines = new LinesIterator(js,Directive.end_stack)) {
            while (lines.hasNext()) {
                line = lines.next();
                Token token = line.firstToken();
                ReservedWord rw = token.expectOneOf(res_stack, res_locals);
                String type = line.nextToken().asString(); // verification type
                FrameType ft = FrameType.fromString(type);
                Object item = getAsmFrameType(ft,line);
                line.noMoreTokens();
                if (rw == res_stack) {
                    frame_stack.add(item);
                } else {
                    frame_local.add(item);
                }
            }
        }
        stackLocals.visitFrame(frame_stack, frame_local, dirline);
        if (SUPPORTS(StackMapTable) && OPTION(USE_STACK_MAP) && !OPTION(SYMBOLIC_LOCAL)) {
            Object[] stackarr = frame_stack.toArray();
            Object[] localarr = frame_local.toArray();
            codeBuilder.addFrameToLabel(stackarr, localarr);
        }
        localStack = frame_local;
    }

    private void setLimit(Line line) {
        ReservedWord type = line.nextToken().expectOneOf(res_stack, res_locals);
        int num = line.lastToken().asUnsignedShort();
        if (type == res_locals) {
            stackLocals.locals().setLimit(num, line);
        } else {
            stackLocals.stack().setLimit(num, line);
        }
    }
    
    private void visitVar(Line line) {
        if (OPTION(SYMBOLIC_LOCAL)) {
            // "%s not supported if %s specified"
            LOG(M212,Directive.dir_var,SYMBOLIC_LOCAL);
            return;
        }
        codeBuilder.addVar(line, labelmap);
    }
    
    private void undefinedLabel(JynxLabel lr) {
        String usage = lr.used()
                .map(Line::toString)
                .collect(Collectors.joining(System.lineSeparator()));
        LOG(M266,lr.name(),usage); // "Label %s not defined; used in%n%s"
    }

    private void unusedLabel(JynxLabel lr) {
        LOG(M272,lr.name(),lr.definedLine());    // "Label %s not used - defined in line:%n  %s"
    }
    
    public JynxCodeNode visitEnd() {
        s2a.visitEnd();
        stackLocals.visitEnd();
        labelmap.checkCatch();
        labelmap.stream()
                .filter(lr->!lr.isDefined())
                .forEach(this::undefinedLabel);        
        if (OPTION(WARN_UNNECESSARY_LABEL)) {
            labelmap.stream()
                    .filter(lr->lr.isUnused())
                    .forEach(this::unusedLabel); 
        }
        var codeNode = codeBuilder.build(stackLocals);
        boolean ok = LOGGER().numErrors() == errorsAtStart;
        if (ok) {
            return codeNode;
        }

        return null;
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation
            (int typeref, TypePath typepath, String desc, boolean visible) {
        Line line = js.getLine();
        TypeRef tr = TypeRef.fromASM(typeref);
        AnnotationVisitor av = switch (tr) {
            case trt_except -> visitTryCatchAnnotation(typeref, typepath, desc, visible);
            case tro_var, tro_resource -> visitLocalVariableAnnotation(line,typeref, typepath, desc, visible);
            default -> visitInsnAnnotation(tr, typeref, typepath, desc, visible);
        };
        line.noMoreTokens();
        return av;
    }
            
    private AnnotationVisitor visitTryCatchAnnotation(int typeref, TypePath tp, String desc, boolean visible) {
        TypeAnnotationNode av = new TypeAnnotationNode(typeref, tp, desc);
        codeBuilder.addCatchAnnotation(av, visible);
        return av;
    }

    private AnnotationVisitor visitInsnAnnotation
        (TypeRef tr, int typeref, TypePath tp, String desc, boolean visible) {
            checkAnnotatedInst(tr, stackLocals.lastOp());
            var av = new TypeAnnotationNode(typeref, tp, desc);
            codeBuilder.addAnnotationToLastInstruction(av, visible);
            return av;
    }
        
    private static void checkAnnotatedInst(TypeRef tr, JvmOp lastjop) {
        EnumSet<JvmOp> lastjops = switch (tr) {
            case tro_cast -> EnumSet.of(JvmOp.asm_checkcast);
            case tro_instanceof -> EnumSet.of(JvmOp.asm_instanceof);
            case tro_new -> EnumSet.of(JvmOp.asm_new);
            case tro_argmethod -> EnumSet.of(JvmOp.asm_invokeinterface, JvmOp.asm_invokestatic, JvmOp.asm_invokevirtual);
            case tro_argnew -> EnumSet.of(JvmOp.asm_invokespecial);
            default -> null;
        };
        if (lastjops != null) {
            if (lastjop == null || !lastjops.contains(lastjop)) {
                if (JVM_VERSION().compareTo(JvmVersion.V9) < 0) {
                    LOG(M231, lastjop, lastjops); // "Last instruction was %s: expected %s"
                } else {
                    LOG(M232, lastjop, lastjops); // "Last instruction was %s: expected %s"
                }
            }
        }
    }
    
    private AnnotationVisitor visitLocalVariableAnnotation
        (Line line,int typeref, TypePath tp, String desc, boolean visible) {
            ArrayList<Integer> indexlist = new ArrayList<>();
            ArrayList<String> startlist = new ArrayList<>();
            ArrayList<String> endlist = new ArrayList<>();
            try (TokenArray array = line.getTokenArray()) {
                while(true) {
                    Token token = array.firstToken();
                    if (token.is(right_array)) {
                        break;
                    }
                    indexlist.add(token.asInt());
                    startlist.add(array.nextToken().asString());
                    endlist.add(array.nextToken().asString());
                    array.noMoreTokens();
                }
            }
            int[] index_arr = new int[indexlist.size()];
            Label[] start_arr = new Label[startlist.size()];
            Label[] end_arr = new Label[endlist.size()];
            for (int i = 0; i < index_arr.length; ++i) {
                index_arr[i] = indexlist.get(i);
                stackLocals.visitVarAnnotation(index_arr[i]);
                JynxLabel startref = labelmap.useOfJynxLabel(startlist.get(i), line);
                JynxLabel endref = labelmap.useOfJynxLabel(endlist.get(i), line);
                start_arr[i] = startref.asmlabel();
                end_arr[i] = endref.asmlabel();
            }
            LabelNode[] start = Stream.of(start_arr)
                    .map(LabelNode::new)
                    .toArray(LabelNode[]::new);
            LabelNode[] end = Stream.of(end_arr)
                    .map(LabelNode::new)
                    .toArray(LabelNode[]::new);
            var av = new LocalVariableAnnotationNode(typeref, tp, start, end, index_arr, desc);
            codeBuilder.addVarAnnotation(av, visible);
            return av;
    }

}
