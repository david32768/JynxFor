package com.github.david32768.jynxfor.instruction;

import org.objectweb.asm.MethodVisitor;

import com.github.david32768.jynxfor.ops.JvmOp;

import jynx2asm.handles.MethodHandle;
import jynx2asm.StackLocals;

public class MethodInstruction implements JynxInstruction {

    private final JvmOp jvmop;    
    private final MethodHandle mh;

    public MethodInstruction(JvmOp jop, MethodHandle mh) {
        this.jvmop = jop;
        this.mh = mh;
    }

    @Override
    public JvmOp jvmop() {
        return jvmop;
    }
    
    @Override
    public void accept(MethodVisitor mv) {
        mv.visitMethodInsn(jvmop.opcode(),mh.owner(), mh.name(), mh.desc(), mh.isInterface());
    }

    @Override
    public void adjust(StackLocals stackLocals) {
        stackLocals.adjustStackOperand(jvmop,mh);
    }

    @Override
    public String toString() {
        return String.format("%s %s",jvmop,mh.iond());
    }

    @Override
    public boolean needLineNumber() {
        return true;
    }

}
