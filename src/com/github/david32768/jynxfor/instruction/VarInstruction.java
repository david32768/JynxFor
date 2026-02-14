package com.github.david32768.jynxfor.instruction;

import org.objectweb.asm.MethodVisitor;

import com.github.david32768.jynxfor.ops.JvmOp;
import com.github.david32768.jynxfor.scan.Line;

public class VarInstruction extends AbstractInstruction {

    private final int varnum;
    
    public VarInstruction(JvmOp jvmop, int varnum, Line line) {
        super(jvmop.exactVar(varnum), line);
        this.varnum = varnum;
    }

    public int varnum() {
        return varnum;
    }
    
    @Override
    public void accept(MethodVisitor mv) {
        assert varnum >= 0;
        mv.visitVarInsn(jvmop.asmOpcode(),varnum);
    }
    
    @Override
    public String toString() {
        return String.format("%s %d", jvmop, varnum);
    }

}
