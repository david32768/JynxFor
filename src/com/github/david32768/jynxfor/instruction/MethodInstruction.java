package com.github.david32768.jynxfor.instruction;

import org.objectweb.asm.MethodVisitor;

import com.github.david32768.jynxfor.ops.JvmOp;
import com.github.david32768.jynxfor.scan.Line;

import jynx2asm.handles.MethodHandle;

public class MethodInstruction extends AbstractInstruction {

    private final MethodHandle mh;

    public MethodInstruction(JvmOp jvmop, MethodHandle mh, Line line) {
        super(jvmop, line);
        this.mh = mh;
    }

    public MethodHandle methodHandle() {
        return mh;
    }
    
    @Override
    public void accept(MethodVisitor mv) {
        mv.visitMethodInsn(jvmop.opcode(),mh.owner(), mh.name(), mh.desc(), mh.isInterface());
    }

    @Override
    public String toString() {
        return String.format("%s %s",jvmop,mh.iond());
    }

}
