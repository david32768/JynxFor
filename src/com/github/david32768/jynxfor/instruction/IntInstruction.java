package com.github.david32768.jynxfor.instruction;

import org.objectweb.asm.MethodVisitor;

import com.github.david32768.jynxfor.ops.JvmOp;
import com.github.david32768.jynxfor.scan.Line;

public class IntInstruction extends AbstractInstruction {

    private final int value;

    public IntInstruction(JvmOp jvmop, int value, Line line) {
        super(jvmop, line);
        this.value = value;
    }

    @Override
    public void accept(MethodVisitor mv) {
        mv.visitIntInsn(jvmop.asmOpcode(),value);
    }

    @Override
    public String toString() {
        return String.format("%s %d",jvmop,value);
    }

}
