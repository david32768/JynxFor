package com.github.david32768.jynxfor.instruction;

import org.objectweb.asm.MethodVisitor;

import com.github.david32768.jynxfor.ops.JvmOp;

import jynx2asm.StackLocals;

public class OpcodeInstruction implements JynxInstruction {

    private final JvmOp jvmop;    

    private OpcodeInstruction(JvmOp jvmop) {
        this.jvmop = jvmop;
    }

    public static OpcodeInstruction getInstance(JvmOp jvmop) {
        assert !jvmop.isStack();
        return new OpcodeInstruction(jvmop);
    }

    @Override
    public JvmOp jvmop() {
        return jvmop;
    }
    
    @Override
    public boolean needLineNumber() {
        return jvmop == JvmOp.asm_idiv || jvmop == JvmOp.asm_ldiv;
    }

    @Override
    public void adjust(StackLocals stackLocals) {
        stackLocals.adjustStack(jvmop);
    }

    @Override
    public void accept(MethodVisitor mv) {
        mv.visitInsn(jvmop.asmOpcode());
    }

    @Override
    public String toString() {
        return String.format("%s",jvmop);
    }

}
