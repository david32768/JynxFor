package jynx2asm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.objectweb.asm.MethodVisitor;

import static com.github.david32768.jynxfor.my.Message.*;

import static com.github.david32768.jynxfree.jvm.Constants.MAX_CODE;
import static com.github.david32768.jynxfree.jynx.Global.LOG;
import static com.github.david32768.jynxfree.jynx.Global.OPTION;
import static com.github.david32768.jynxfree.jynx.Global.SUPPORTS;
import static com.github.david32768.jynxfree.jynx.GlobalOption.WARN_UNNECESSARY_LABEL;

import com.github.david32768.jynxfor.ops.JvmOp;

import com.github.david32768.jynxfree.jvm.Feature;
import com.github.david32768.jynxfree.jynx.Directive;
import com.github.david32768.jynxfree.jynx.GlobalOption;

import asm.instruction.Instruction;
import asm.instruction.LabelInstruction;
import asm.instruction.LineInstruction;
import asm.JynxVar;
import jynx2asm.frame.LocalFrame;
import jynx2asm.frame.LocalVars;
import jynx2asm.frame.MethodParameters;
import jynx2asm.frame.OperandStack;
import jynx2asm.frame.OperandStackFrame;
import jynx2asm.handles.JynxHandle;

public class StackLocals {

    public enum Last {
        OP("op"),
        LABEL("label"),
        LINE(".line"),
        FRAME(".stack"),
        ;
        
        private final String extname;

        private Last(String extname) {
            this.extname = extname;
        }
        
        @Override
        public String toString() {
            return extname;
        }
    }
    
    private final LocalVars locals;
    private final OperandStack stack;
    private final JynxLabelMap labelmap;
    private final JvmOp returnOp;
    private final List<JynxLabel> activeLabels;

    private boolean returns;
    private boolean hasThrow;
    private boolean frameRequired;
    private Optional<JynxLabel> lastLab;
    private JvmOp lastop;
    private Last completion;
    private int minLength;
    private int maxLength;
    
    private StackLocals(LocalVars locals, OperandStack stack, JynxLabelMap labelmap, JvmOp returnop) {
        this.locals = locals;
        this.stack = stack;
        this.labelmap = labelmap;
        this.returnOp = returnop;
        this.returns = false;
        this.hasThrow = false;
        this.lastLab = Optional.empty();
        this.lastop = JvmOp.asm_nop;
        this.frameRequired = false;
        this.completion = Last.OP;
        this.activeLabels = new ArrayList<>();
        this.minLength = 0;
        this.maxLength = 0;
    }
    
    public static StackLocals getInstance(MethodParameters parameters, JynxLabelMap labelmap) {
        OperandStack os = OperandStack.getInstance();
        LocalVars lv = LocalVars.getInstance(parameters);
        return new StackLocals(lv, os, labelmap, parameters.getReturnOp());
    }

    public LocalVars locals() {
        return locals;
    }

    public OperandStack stack() {
        return stack;
    }

    public int getMinLength() {
        return minLength;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public JvmOp getReturnOp() {
        return returnOp;
    }

    public JvmOp lastOp() {
        return lastLab.isPresent()?null:lastop;
    }

    public boolean isUnreachable() {
        return lastop.isUnconditional() && !lastLab.isPresent();
    }
    
    // unreachable unless only backward references 
    public boolean isUnreachableForwards() {
        boolean lastgoto = lastop.isUnconditional();
        boolean usedlab = lastLab.isPresent() && lastLab.get().isUsedInCode();
        return lastgoto && !usedlab;
    }
    
    public void visitTryCatchBlock(JynxCatch jcatch) {
        JynxLabel usingref = jcatch.usingLab();
        stack.checkCatch(usingref);
    }
    
    public void adjustLabelDefine(JynxLabel target) {
        int adjustmax = target.forwardBranchAdjustment();
        if (adjustmax > 0) {
            int oldmax = maxLength;
            maxLength -= adjustmax;
            // "at label %s: min length = %d max length = %d -> %d (adjusted = -%d)"
            LOG(M802, target,minLength, oldmax, maxLength,adjustmax);
            assert maxLength >= minLength;
        }
        List<JynxCatch> catchlist = labelmap.getCatches(target);
        if (!catchlist.isEmpty()) {
            target.visitCatch(catchlist);
        }
        if (lastLab.isPresent()) { // new label is alias
            JynxLabel base = lastLab.get();
            if (OPTION(WARN_UNNECESSARY_LABEL)) {
                LOG(M220,target.name(),base.name()); // "label %s is an alias for label %s"
            }
            labelmap.aliasJynxLabel(target.name(), target.definedLine(), base);
            target.aliasOf(base);
            locals.visitAlias(target, base,lastLab);
            stack.visitAlias(target, base);
        } else {
            stack.visitLabel(target);
            locals.visitLabel(target, lastLab);
            lastLab = Optional.of(target);
            activeLabels.add(target);
            frameRequired = lastop.isUnconditional();
            changeCompletionTo(Last.LABEL);
        }
    }

    private void visitLineNumber(Line line) {
        if (lastLab.isPresent()) {
            labelmap.weakUseOfJynxLabel(lastLab.get(), line);
        }
        changeCompletionTo(Last.LINE);
    }
    
    public void visitFrame(List<Object> stackarr, List<Object> localarr, Line line) {
        if (lastLab.isPresent()) {
            labelmap.weakUseOfJynxLabel(lastLab.get(), line);
        }
        stack.visitFrame(stackarr,lastLab);
        locals.visitFrame(localarr,lastLab);
        frameRequired = false;
        changeCompletionTo(Last.FRAME);
    }
    
    private void changeCompletionTo(Last next) {
        if (next != completion) {
            completion = next;
        }
    }
    
    private void checkMethodLength(Instruction in) {
        int minbefore = minLength;
        int maxbefore = maxLength;
        minLength += in.minLength();
        maxLength += in.maxLength();
        assert maxLength >= minLength;
        if (minbefore <= MAX_CODE && minLength > MAX_CODE) {
            // "maximum code size of %d exceeded; current size = [%d,%d]"
            LOG(M339, MAX_CODE, minLength, maxLength);
        } else if (maxbefore <= MAX_CODE && maxLength > MAX_CODE) {
            // "maximum code size of %d may have been exceeded; current size = [%d,%d]"
            LOG(M312, MAX_CODE, minLength, maxLength);
        }
    }
    
    public boolean visitInsn(Instruction in, Line line) {
        if (in == null) {
            return false;
        }
        JvmOp jvmop = in.resolve(minLength,maxLength);
        if (in instanceof LabelInstruction) {
            in.adjust(this);
            return true;
        }
        if (isUnreachable()) {
            Object drop = in instanceof LineInstruction? Directive.dir_line: jvmop;
            // "Instruction '%s' dropped as unreachable after '%s' without intervening label"
            LOG(M121, drop, lastop);
            return false;
        }
        if (in instanceof LineInstruction) {
            visitLineNumber(line);
            return true;
        }
        assert jvmop.opcode() >= 0;
        if (jvmop.isReturn()) {
            if (jvmop == returnOp) {
                returns = true;
            } else {
                LOG(M191, returnOp, jvmop); // "method requires %s but found %s"
                return false;
            }
        }
        hasThrow |= jvmop == JvmOp.asm_athrow; 
        if (jvmop == JvmOp.asm_new && lastLab.isPresent()) {
            labelmap.weakUseOfJynxLabel(lastLab.get(), line);
        }
        if (frameRequired && SUPPORTS(Feature.stackmap) && OPTION(GlobalOption.USE_STACK_MAP)) {
                LOG(M124);  // "stack frame is definitely required here"
        }
        frameRequired = false; // to prevent multiple error messages

        visitPreJvmOp(jvmop);
        in.adjust(this);
        checkMethodLength(in);
        visitPostJvmOp(jvmop);
        changeCompletionTo(Last.OP);
        return true;
    }

    private void visitPreJvmOp(JvmOp asmop) {
        locals.preVisitJvmOp(lastLab);
        stack.preVisitJvmOp(lastLab);
        lastLab = Optional.empty();
        lastop = asmop;
    }

    private void visitPostJvmOp(JvmOp asmop) {
        boolean startblock = asmop.isUnconditional();
        if (startblock) {
            stack.startBlock();
            locals.startBlock();
            activeLabels.clear();
        }
    }

    public void acceptVarDirectives(MethodVisitor mv, List<JynxVar> jvars) {
        locals().addSymbolicVars(jvars);
        for (JynxVar jvar:jvars) {
            boolean ok = visitVarDirective(jvar);
            if (ok) {
                jvar.accept(mv, labelmap);
            } else {
                LOG(jvar.getLine().toString(), M54, jvar.varnum()); // "variable %d has not been written to"
            }
        }
    }
    
    private boolean visitVarDirective(JynxVar jvar) {
        return locals().visitVarDirective(FrameElement.fromDesc(jvar.desc()), jvar.varnum());
    }

    
    public void visitVarAnnotation(int num) {
        locals.visitVarAnnotation(num);
    }
    
    public void visitEnd() {
        locals.visitEnd();
        if(!returns && (returnOp != JvmOp.asm_return || !hasThrow)) {
            LOG(M196,returnOp); // "no %s instruction found"
        }
        switch(completion) {
            case OP -> {
                if (!lastop.isUnconditional()) {
                    LOG(M208,completion,lastop); // "code not complete - last %s was %s"
                }
            }
            case LABEL -> {
                JynxLabel jlabel = lastLab.get();
                if (jlabel.isUsedInCode() || !lastop.isUnconditional()) {
                    LOG(M208,completion,jlabel.name()); // "code not complete - last %s was %s"
                }
            }
            case LINE, FRAME -> LOG(M209,completion);// "code not complete - last was %s"
            default -> throw new AssertionError();
        }
        if (minLength > MAX_CODE) {
            // "maximum code size of %d exceeded; method size = [%d,%d]"
            LOG(M330, MAX_CODE, minLength, maxLength);
        } else {
            // "min length = %d max length = %d"
            LOG(M801, minLength, maxLength);
        }
    }

    private void updateLocal(JynxLabel label, LocalFrame osf) {
        label.updateLocal(osf);
    }
    

    public void adjustLabelJump(JynxLabel label, JvmOp jvmop) {
        stack.checkStack(label, jvmop);
        LocalFrame osf = locals.currentFrame();
        updateLocal(label, osf);
        for (JynxLabel using:labelmap.getThrowsTo(label)) {
            updateLocal(using, osf);
        }
    }
    
    private void updateLocal(JynxLabel dflt,Collection<JynxLabel> labels) {
        LocalFrame osf = locals.currentFrame();
        updateLocal(dflt,osf);
        for (JynxLabel label:labels) {
            updateLocal(label,osf);
        }
    }

    private void checkStack(JynxLabel dflt, Collection<JynxLabel> labels) {
        OperandStackFrame osf = stack.currentFrame();
        stack.checkStack(dflt,osf);
        for (JynxLabel label:labels) {
            stack.checkStack(label,osf);
        }
    }
    

    public void adjustLabelSwitch(JynxLabel dflt, Collection<JynxLabel> labels) {
        checkStack(dflt, labels);
        updateLocal(dflt, labels);
    }

    public void adjustStackOperand(String desc) {
        stack.adjustOperand(desc);
    }
    
    public void adjustStackOperand(JvmOp jvmop, JynxHandle mh) {
        stack.adjustInvoke(jvmop, mh);
    }
    
    public int adjustIncr(Token vartoken) {
        char ctype = 'I';
        int var = locals.loadVarNumber(vartoken);
        FrameElement fe = locals.loadType(ctype,var);
        for (JynxLabel lab:activeLabels) {
            lab.load(fe,var);
        }
        return var;
    }
    
    private int adjustLoad(char ctype, Token vartoken) {
        int var = locals.loadVarNumber(vartoken);
        FrameElement fe = locals.loadType(ctype,var);
        for (JynxLabel lab:activeLabels) {
            lab.load(fe,var);
        }
        stack.load(fe, var);
        return var;
    }
    
    private int adjustStore(char ctype, Token vartoken) {
        FrameElement fe = stack.storeType(ctype);
        int var = locals.storeFrameElement(fe,vartoken);
        for (JynxLabel lab:activeLabels) {
            lab.store(fe,var);
        }
        return var;
    }

    public int adjustLoadStore(JvmOp jop, Token vartoken) {
        char ctype = jop.vartype();
        if (jop.isStoreVar()) {
            return adjustStore(ctype, vartoken);
        } else {
            return adjustLoad(ctype, vartoken);
        }
    }
    
    public void adjustStack(JvmOp jop) {
        String opdesc = jop.desc();
        if (opdesc == null) {
            throw new AssertionError("" + jop);
        } else {
            stack.adjustOpDesc(opdesc);
        }
    }
    
    public void adjustStackOp(JvmOp jop) {
        if (jop.isStack()) {
            stack.adjustStackOp(jop);
        } else {
            throw new AssertionError("" + jop);
        }
    }
    
    public String stringLocals() {
        return locals.stringForm();
    }
    
    public String stringStack() {
        return stack.stringForm();
    }
}
