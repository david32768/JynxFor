package com.github.david32768.jynxfor.instruction;

import org.objectweb.asm.MethodVisitor;

import com.github.david32768.jynxfor.ops.JvmOp;

import jynx2asm.StackLocals;

public class StackInstruction implements JynxInstruction {

    private final JvmOp jvmop;    

    public StackInstruction(JvmOp jop) {
        this.jvmop = jop;
    }

    @Override
    public JvmOp jvmop() {
        return jvmop;
    }
    
    @Override
    public void accept(MethodVisitor mv) {
        mv.visitInsn(jvmop.asmOpcode());
    }

    @Override
    public void adjust(StackLocals stackLocals) {
        stackLocals.adjustStackOp(jvmop);
    }

    @Override
    public String toString() {
        return jvmop.toString();
    }
    
}
