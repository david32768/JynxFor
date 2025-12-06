package com.github.david32768.jynxfor.instruction;

import org.objectweb.asm.MethodVisitor;

import com.github.david32768.jynxfor.ops.JvmOp;
import com.github.david32768.jynxfor.scan.Line;

public class MarrayInstruction extends AbstractInstruction {

    private final String type;
    private final int dims;

    public MarrayInstruction(JvmOp jvmop, String type, int dims, Line line) {
        super(jvmop, line);
        this.type = type;
        this.dims = dims;
    }

    public int dims() {
        return dims;
    }

    public String type() {
        return type;
    }
    
    @Override
    public void accept(MethodVisitor mv) {
        mv.visitMultiANewArrayInsn(type,dims);
    }

    @Override
    public String toString() {
        return String.format("%s %s %d",jvmop,type, dims);
    }

}
