package jynx2asm;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.SortedMap;
import java.util.TreeMap;

import org.objectweb.asm.ConstantDynamic;

import static com.github.david32768.jynxfor.my.Message.*;
import static com.github.david32768.jynxfree.jynx.Global.*;
import static com.github.david32768.jynxfree.jynx.NameDesc.*;
import static com.github.david32768.jynxfree.jynx.ReservedWord.*;

import static com.github.david32768.jynxfree.jynx.GlobalOption.GENERATE_LINE_NUMBERS;

import com.github.david32768.jynxfor.instruction.*;

import com.github.david32768.jynxfor.my.JynxGlobal;
import com.github.david32768.jynxfor.ops.DynamicOp;
import com.github.david32768.jynxfor.ops.IndentType;
import com.github.david32768.jynxfor.ops.JvmOp;
import com.github.david32768.jynxfor.ops.JynxOp;
import com.github.david32768.jynxfor.ops.JynxOps;
import com.github.david32768.jynxfor.ops.LineOp;
import com.github.david32768.jynxfor.ops.MacroOp;
import com.github.david32768.jynxfor.ops.SelectOp;

import com.github.david32768.jynxfor.scan.ConstType;
import com.github.david32768.jynxfor.scan.Line;
import com.github.david32768.jynxfor.scan.Token;
import com.github.david32768.jynxfor.scan.TokenArray;

import com.github.david32768.jynxfree.jvm.ConstantPoolType;
import com.github.david32768.jynxfree.jvm.Context;
import com.github.david32768.jynxfree.jvm.HandleType;
import com.github.david32768.jynxfree.jvm.OpArg;
import com.github.david32768.jynxfree.jynx.GlobalOption;
import com.github.david32768.jynxfree.jynx.LogIllegalStateException;
import com.github.david32768.jynxfree.jynx.LogUnexpectedEnumValueException;
import com.github.david32768.jynxfree.jynx.NameDesc;


import jynx2asm.frame.OperandStack;
import jynx2asm.frame.OperandStackFrame;
import jynx2asm.handles.FieldHandle;
import jynx2asm.handles.MethodHandle;

public class String2Insn {

    private final JynxLabelMap labelMap;
    private final LabelStack labelStack;
    private final ClassChecker checker;
    private final JynxOps opmap;
    
    private Line line;
    private boolean multi;
    private int macroCount;
    private int indent;
    
    private String2Insn(JynxLabelMap labelmap, ClassChecker checker, JynxOps opmap) {
        this.labelMap = labelmap;
        this.labelStack = new LabelStack();
        this.checker = checker;
        this.opmap = opmap;
        this.indent = IndentType.BEGIN.after();
    }

    public static String2Insn getInstance(ClassChecker checker, JynxOps opmap) {
        JynxLabelMap labelmap = new JynxLabelMap();
        return new String2Insn(labelmap, checker, opmap);
    }

    public JynxLabelMap getLabelMap() {
        return labelMap;
    }

    public void getInsts(InstList instlist) {
        line = instlist.getLine();
        if (line.isLabel()) {
            String lab = line.firstToken().asLabel();
            addLabel(lab, instlist);
            return;
        }
        String tokenstr = line.firstToken().asString();
        JynxOp jynxop = opmap.get(tokenstr);
        if (jynxop == null) {
            if (opmap.isLabel(tokenstr)) {
                addLabel(tokenstr, instlist);
                return;
            }
            LOG(M86,tokenstr); // "invalid op - %s"
            line.skipTokens();
            return;
        }
        if (OPTION(GlobalOption.__WARN_INDENT)) {
            IndentType itype = jynxop.indentType();
            indent += itype.before();
            if (line.getIndent() != indent) {
                LOG(M228, line.getIndent(), indent); // "indent %d found but expected %d"
            }
            indent += itype.after();
        }
        add(jynxop, instlist);
    }

    private void addLabel(String lab, InstList instlist) {
        Token stack = line.nextToken();
        line.noMoreTokens();
        JynxLabel target = labelMap.defineJynxLabel(lab, line);
        if (!stack.isEndToken()) {
            String desc = stack.asString();
            desc = JynxGlobal.TRANSLATE_PARMS(desc);
            boolean ok = NameDesc.PARMS.validate(desc);
            if (ok) {
                FrameElement[] elements = OperandStack.frameElementsFrom(desc);
                OperandStackFrame osf = new OperandStackFrame(elements);
                target.updateStack(osf);
            }
        }
        instlist.add(new LabelInstruction(JvmOp.xxx_label, target));
    }
    
    public void add(JynxOp jynxop, InstList instlist) {
        line = instlist.getLine();
        multi = false;
        macroCount = 0;
        add(jynxop, new ArrayDeque<>(), macroCount, instlist);
        line.noMoreTokens();
    }
    
    private final static int MAX_MACROS_FOR_LINE = 64;
    
    private void add(JynxOp jop, Deque<MacroOp> macrostack, int macct, InstList instlist) {
        if (multi) {
            LOG(M254,jop); // "%s is used in a macro after a mulit-line op"
        }
        switch (jop) {
            case SelectOp selector -> add(selector.getOp(line, instlist), macrostack, macct, instlist);
            case JvmOp jvmop -> addJvmOp(jvmop,instlist);
            case DynamicOp dynamicop -> instlist.add(dynamicop.getInstruction(line, checker));
            case LineOp lineop -> lineop.adjustLine(line, macct, macrostack.peekLast(), labelStack);
            case MacroOp macroop -> {
                macrostack.addLast(macroop);
                ++macroCount;
                if (macroCount > MAX_MACROS_FOR_LINE) {
                    // "number of macro ops exceeds maximum of %d for %s"
                    throw new LogIllegalStateException(M317, MAX_MACROS_FOR_LINE, macrostack.peekFirst());
                }
                macct = macroCount;
                for (JynxOp mjop:macroop.getJynxOps()) {
                    add(mjop, macrostack, macct, instlist);
                }
                macrostack.removeLast();
            }
            default -> throw new AssertionError();
        }
    }
    
    public void visitEnd() {
        if (!labelStack.isEmpty()) {
            LOG(M249, labelStack.size()); // "structured op(s) missing; level at end is %d"
        }
    }

    private void addJvmOp(JvmOp jvmop, InstList instlist) {
        OpArg oparg = jvmop.args();
        JynxInstruction insn = switch(oparg) {
            case arg_atype -> arg_atype(jvmop);
            case arg_byte -> arg_byte(jvmop);
            case arg_callsite -> arg_callsite(jvmop);
            case arg_class -> arg_class(jvmop);
            case arg_constant -> arg_constant(jvmop);
            case arg_dir -> arg_dir(jvmop);
            case arg_field -> arg_field(jvmop);
            case arg_incr -> arg_incr(jvmop);
            case arg_label -> arg_label(jvmop,instlist.isUnreachable());
            case arg_marray -> arg_marray(jvmop);
            case arg_method, arg_interface -> arg_method(jvmop);
            case arg_none -> arg_none(jvmop);
            case arg_short -> arg_short(jvmop);
            case arg_stack -> arg_stack(jvmop);
            case arg_switch -> arg_switch(jvmop);
            case arg_var -> arg_var(jvmop);
        };
        if (insn == null) {
            return;
        }
        instlist.add(insn);
    }
    
    private JynxInstruction arg_atype(JvmOp jvmop) {
        int atype = line.nextToken().asTypeCode();
        return new IntInstruction(jvmop, atype);
    }
    
    private JynxInstruction arg_byte(JvmOp jvmop) {
        int v = line.nextToken().asByte();
        return new IntInstruction(jvmop,v);
    }
    
    private JynxInstruction arg_callsite(JvmOp jvmop) {
        JynxConstantDynamic jcd = new JynxConstantDynamic(line, checker);
        ConstantDynamic cd = jcd.getConstantDynamic4Invoke();
        return new DynamicInstruction(jvmop, cd);
    }
    
    private JynxInstruction arg_class(JvmOp jvmop) {
        String typeo = line.nextToken().asString();
        String type = JynxGlobal.TRANSLATE_TYPE(typeo, false);
        if (jvmop == JvmOp.asm_new) {
            CLASS_NAME.validate(type);
            checker.usedNew(type);
        } else {
            OBJECT_NAME.validate(type);
        }
        return new TypeInstruction(jvmop, type);
    }
  
    private JynxInstruction simpleConstant(JvmOp jvmop) {
        Token token = line.nextToken();
        Object value = token.getConst();
        ConstType ct = ConstType.getFromASM(value,Context.JVMCONSTANT);
        if (jvmop == JvmOp.opc_ldc2_w) {
            switch (ct) {
                case ct_int -> {
                    value = ((Integer)value).longValue();
                    ct = ConstType.ct_long;
                }
                case ct_float -> {
                    value = ((Float)value).doubleValue();
                    ct = ConstType.ct_double;
                }
                case ct_long -> {}
                case ct_double -> {}
                default -> {
                    LOG(M138, JvmOp.opc_ldc2_w, value);   // "%s cannot be used for constant - %s"
                    jvmop = JvmOp.asm_ldc;
                }
            }
        }
        if (jvmop != JvmOp.opc_ldc2_w) {
            switch (ct) {
                case ct_long -> {
                    jvmop = JvmOp.opc_ldc2_w;
                    // "%s changed to %s where necessary"
                    LOG(M154, JvmOp.asm_ldc, JvmOp.opc_ldc2_w);
                }
                case ct_double -> {
                    jvmop = JvmOp.opc_ldc2_w;
                    // "%s changed to %s where necessary"
                    LOG(M154, JvmOp.asm_ldc, JvmOp.opc_ldc2_w);
                }
                case ct_method_handle -> {
                    CHECK_CAN_LOAD(ConstantPoolType.CONSTANT_MethodHandle);
                    checker.mayBeHandle(value, line);
                }
                default -> {}
            }
        }
        return new LdcInstruction(jvmop, value, ct);
    }
    
    private JynxInstruction dynamicConstant(JvmOp jvmop) {
        ConstType ct = ConstType.ct_const_dynamic;
        JynxConstantDynamic jcd = new JynxConstantDynamic(line, checker);
        ConstantDynamic dyn = jcd.getConstantDynamic4Load();
        String desc = dyn.getDescriptor();
        if (desc.length() == 1) {
            ct = ConstType.getFromDesc(desc, Context.JVMCONSTANT);
        }
        JvmOp actual = jvmop == JvmOp.opc_ldc_w? JvmOp.asm_ldc: jvmop;
        JvmOp required = dyn.getSize() == 2? JvmOp.opc_ldc2_w: JvmOp.asm_ldc;
        if (required != actual) {
            // "%s changed to %s where necessary"
            LOG(M154, jvmop, required);
            jvmop = required;
        }
        return new LdcInstruction(jvmop, dyn, ct);
    }
    
    private JynxInstruction arg_constant(JvmOp jvmop) {
        Token leftbrace = line.peekToken();
        if (leftbrace.is(left_brace)) {
            return dynamicConstant(jvmop);
        } else {
            return simpleConstant(jvmop);
        }
    }
    
    private JynxInstruction arg_dir(JvmOp jvmop) {
        switch (jvmop) {
            case xxx_label -> {
                String labstr = line.nextToken().asString();
                JynxLabel target = labelMap.defineJynxLabel(labstr, line);
                return new LabelInstruction(jvmop, target);
            }
            case xxx_label_weak -> {
                String labstr = line.nextToken().asString();
                JynxLabel target = labelMap.defineWeakJynxLabel(labstr, line);
                return target == null?null:new LabelInstruction(jvmop, target);
            }
            case xxx_line -> {
                int lineno = line.nextToken().asUnsignedShort();
                if (OPTION(GENERATE_LINE_NUMBERS)) {
                    LOG(M95,GENERATE_LINE_NUMBERS); // ".line directives ignored as %s specified"
                    return null;
                }
                return new LineInstruction(lineno);
            }
            default -> throw new LogUnexpectedEnumValueException(jvmop);
        }
    }
    
    private JynxInstruction arg_field(JvmOp jvmop) {
        String fname = line.nextToken().asString();
        String desc = line.nextToken().asString();
        FieldHandle fh = FieldHandle.getInstance(fname, desc,HandleType.fromOp(jvmop.getOpcode(), false));
        checker.usedField(fh);
        return new FieldInstruction(jvmop,fh);
    }
    
    private JynxInstruction arg_incr(JvmOp jvmop) {
        Token vartoken = line.nextToken();
        int incr = line.nextToken().asShort();
        return new IncrInstruction(jvmop, vartoken, incr);
    }

    private JynxInstruction arg_label(JvmOp jvmop, boolean unreachable) {
        Token label = line.nextToken();
        if (jvmop == JvmOp.xxx_goto_weak) {
            if (unreachable) {
                return null;
            }
            jvmop = JvmOp.asm_goto;
        }
        JynxLabel jlab = getJynxLabel(label);
        return new JumpInstruction(jvmop, jlab);
    }

    private JynxInstruction arg_marray(JvmOp jvmop) {
        String desc = line.nextToken().asString();
        ARRAY_DESC.validate(desc);
        int lastbracket = desc.lastIndexOf('[') + 1;
        int dims = line.nextToken().asUnsignedByte();
        if (dims == 0 || dims > lastbracket) {
            LOG(M253,dims,lastbracket);  // "illegal number of dimensions %d; must be in range [0,%d]"
        }
        return new MarrayInstruction(jvmop, desc, dims);
    }

    private JynxInstruction arg_method(JvmOp jvmop) {
        String mspec = line.nextToken().asString();
        MethodHandle mh = MethodHandle.getInstance(mspec,jvmop);
        checker.usedMethod(mh, jvmop, line);
        return new MethodInstruction(jvmop, mh);
    }

    private JynxInstruction arg_none(JvmOp jvmop) {
        if (jvmop == JvmOp.opc_wide) {
            LOG(M210,JvmOp.opc_wide);    // "%s instruction ignored as not required"
            return null;
        }
        return OpcodeInstruction.getInstance(jvmop);
    }

    private JynxInstruction arg_short(JvmOp jvmop) {
        int v = line.nextToken().asShort();
        return new IntInstruction(jvmop, v);
    }
    
    private JynxInstruction arg_stack(JvmOp jvmop) {
        return new StackInstruction(jvmop);
    }

    private JynxInstruction arg_switch(JvmOp jvmop) {
        int low = Integer.MIN_VALUE;
        int high = Integer.MAX_VALUE;
        boolean hasLH = false;
        if (jvmop == JvmOp.asm_tableswitch) {
            Token token = line.peekToken();
            if (!token.is(res_default)) {
                low = line.nextToken().asInt();
                high = line.nextToken().asInt();
                hasLH = true;
            }
        }
        line.nextToken().mustBe(res_default);
        JynxLabel dflt = getJynxLabel(line.nextToken());
        if (OPTION(GlobalOption.GENERIC_SWITCH)) {
            jvmop = JvmOp.opc_switch;
            hasLH = false;
        }
        SortedMap<Integer,JynxLabel> swmap = new TreeMap<>();
        try (TokenArray dotarray = line.getTokenArray()) {
            multi |= dotarray.isMultiLine(); 
            while (true) {
                Token value = dotarray.firstToken();
                if (value.is(right_array)) {
                    if (hasLH) {
                        swmap.putIfAbsent(low, dflt);
                        swmap.putIfAbsent(high, dflt);
                    }
                    return SwitchInstruction.getInstance(jvmop, dflt, swmap);
                }
                int key = value.asInt();
                dotarray.nextToken().mustBe(right_arrow);
                Token label = dotarray.nextToken();
                JynxLabel target = getJynxLabel(label);
                dotarray.noMoreTokens();
                
                if (key < low || key > high) {
                    // "key %d is not in range [%d,%d]"
                    LOG(M626, key, low, high);
                    continue;
                }
                
                if (jvmop != JvmOp.asm_tableswitch && target.equals(dflt)) {
                    // "unneccessary case %d -> %s in %s as target is default label"
                    LOG(M189, key, target, jvmop);
                }
                JynxLabel previous = swmap.putIfAbsent(key, target);
                if (previous != null) {
                    if (previous.equals(target)) {
                        // "duplicate key %d; target = %s"
                        LOG(M255, key, target.name());
                    } else {
                        // "ambiguous key %d; previous target = %s, current target = %s"
                        LOG(M229, key, previous.name(), target.name());
                    }
                } 
            }
        }
    }

    private JynxLabel getJynxLabel(Token token) {
        String labstr = token.asString();
        if (OPTION(GlobalOption.__STRUCTURED_LABELS) && Character.isDigit(labstr.codePointAt(0))) {
            int index = token.asInt();
            labstr = labelStack.peek(index).asString();
        }
        return labelMap.codeUseOfJynxLabel(labstr, line);
    }

    private JynxInstruction arg_var(JvmOp jvmop) {
        Token token;
        if (jvmop.isImmediate()) {
            if (OPTION(GlobalOption.SYMBOLIC_LOCAL)) {
                // "%s not supported if %s specified"
                LOG(M212, jvmop, GlobalOption.SYMBOLIC_LOCAL);
            }
            String opname = jvmop.externalName();
            char suffix = opname.charAt(opname.length() - 1);
            token = Token.getInstance("" + suffix);
        } else {
            token = line.nextToken();
        }
        return new VarInstruction(jvmop, token);
    }

}
