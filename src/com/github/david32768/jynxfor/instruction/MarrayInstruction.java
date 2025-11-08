package com.github.david32768.jynxfor.instruction;

import java.util.Arrays;

import org.objectweb.asm.MethodVisitor;

import com.github.david32768.jynxfor.ops.JvmOp;

import jynx2asm.StackLocals;

public class MarrayInstruction implements JynxInstruction {

    private final JvmOp jvmop;    
    private final String type;
    private final int dims;

    public MarrayInstruction(JvmOp jop, String type, int dims) {
        this.jvmop = jop;
        this.type = type;
        this.dims = dims;
    }

    @Override
    public JvmOp jvmop() {
        return jvmop;
    }
    
    @Override
    public void accept(MethodVisitor mv) {
        mv.visitMultiANewArrayInsn(type,dims);
    }

    @Override
    public void adjust(StackLocals stackLocals) {
        char[] parmarray = new char[dims];
        Arrays.fill(parmarray, 'I');
        String parms = String.valueOf(parmarray);
        parms = "(" + parms + ")" + type;
        stackLocals.adjustStackOperand(parms);
    }

    @Override
    public String toString() {
        return String.format("%s %s %d",jvmop,type, dims);
    }

    @Override
    public boolean needLineNumber() {
        return true;
    }

}
