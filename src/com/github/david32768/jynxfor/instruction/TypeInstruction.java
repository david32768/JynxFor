package com.github.david32768.jynxfor.instruction;

import org.objectweb.asm.MethodVisitor;

import com.github.david32768.jynxfor.ops.JvmOp;
import com.github.david32768.jynxfor.scan.Line;

public class TypeInstruction extends AbstractInstruction {

    private final String type;

    public TypeInstruction(JvmOp jvmop, String type, Line line) {
        super(jvmop, line);
        this.type = type;
    }

    @Override
    public void accept(MethodVisitor mv) {
        mv.visitTypeInsn(jvmop.asmOpcode(), type);
    }

    @Override
    public String toString() {
        return String.format("%s %s",jvmop,type);
    }

}
