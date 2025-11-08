package com.github.david32768.jynxfor.instruction;

import org.objectweb.asm.MethodVisitor;

import com.github.david32768.jynxfor.ops.JvmOp;
import com.github.david32768.jynxfor.scan.Token;

import jynx2asm.StackLocals;

public class IncrInstruction implements JynxInstruction {

    private JvmOp jvmop;    
    private final Token varToken;
    private final int incr;

    private int varnum;

    public IncrInstruction(JvmOp jop, Token vartoken, int incr) {
        this.jvmop = jop;
        this.varToken = vartoken;
        this.incr = incr;
    }

    @Override
    public JvmOp jvmop() {
        return jvmop;
    }
    
    @Override
    public void adjust(StackLocals stackLocals) {
        this.varnum = stackLocals.adjustIncr(varToken);
        this.jvmop = jvmop.exactIncr(varnum, incr);
    }

    @Override
    public void accept(MethodVisitor mv) {
        mv.visitIincInsn(varnum, incr);
    }

    @Override
    public String toString() {
        return String.format("%s %d %d",jvmop,varnum, incr);
    }

}
