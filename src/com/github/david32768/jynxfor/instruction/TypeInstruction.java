package com.github.david32768.jynxfor.instruction;

import org.objectweb.asm.MethodVisitor;

import com.github.david32768.jynxfor.ops.JvmOp;

public class TypeInstruction implements JynxInstruction {

    private final JvmOp jvmop;    
    private final String type;

    public TypeInstruction(JvmOp jop, String type) {
        this.jvmop = jop;
        this.type = type;
    }

    @Override
    public JvmOp jvmop() {
        return jvmop;
    }
    
    @Override
    public void accept(MethodVisitor mv) {
        mv.visitTypeInsn(jvmop.asmOpcode(), type);
    }

    @Override
    public String toString() {
        return String.format("%s %s",jvmop,type);
    }

    @Override
    public boolean needLineNumber() {
        return true;
    }

}
