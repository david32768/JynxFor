package com.github.david32768.jynxfor.instruction;

import org.objectweb.asm.MethodVisitor;

import com.github.david32768.jynxfor.ops.JvmOp;

import jynx2asm.JynxLabel;
import jynx2asm.StackLocals;

public class LabelInstruction implements JynxInstruction {

    private final JvmOp jvmop;    
    private final JynxLabel jlab;

    public LabelInstruction(JvmOp jop, JynxLabel jlab) {
        this.jvmop = jop;
        this.jlab = jlab;
    }

    @Override
    public JvmOp jvmop() {
        return jvmop;
    }
    
    @Override
    public void resolve(int minoffset, int maxoffset) {
        jlab.setOffset(minoffset, maxoffset);
    }

    @Override
    public void adjust(StackLocals stackLocals) {
        stackLocals.adjustLabelDefine(jlab);
    }

    @Override
    public void accept(MethodVisitor mv) {
        mv.visitLabel(jlab.asmlabel());
    }

    @Override
    public String toString() {
        return String.format("label %s",jlab.name());
    }

}
