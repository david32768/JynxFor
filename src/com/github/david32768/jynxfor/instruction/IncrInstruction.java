package com.github.david32768.jynxfor.instruction;

import org.objectweb.asm.MethodVisitor;

import com.github.david32768.jynxfor.ops.JvmOp;
import com.github.david32768.jynxfor.scan.Line;

public class IncrInstruction extends AbstractInstruction {

    private final int varnum;
    private final int incr;


    public IncrInstruction(JvmOp jvmop, int varnum, int incr, Line line) {
        super(jvmop.exactIncr(varnum, incr), line);
        assert varnum >= 0;
        this.varnum = varnum;
        this.incr = incr;
    }

    public int varnum() {
        return varnum;
    }
    
    @Override
    public void accept(MethodVisitor mv) {
        mv.visitIincInsn(varnum, incr);
    }

    @Override
    public String toString() {
        return String.format("%s %d %d", jvmop, varnum, incr);                    
    }
}
