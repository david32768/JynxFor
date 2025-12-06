package com.github.david32768.jynxfor.instruction;

import org.objectweb.asm.MethodVisitor;

import com.github.david32768.jynxfor.ops.JvmOp;
import com.github.david32768.jynxfor.scan.Line;

public class OpcodeInstruction extends AbstractInstruction {

    protected OpcodeInstruction(JvmOp jvmop, Line line) {
        super(jvmop, line);
    }

    public static OpcodeInstruction getInstance(JvmOp jvmop, Line line) {
        assert !jvmop.isStack();
        return new OpcodeInstruction(jvmop, line);
    }

    @Override
    public void accept(MethodVisitor mv) {
        mv.visitInsn(jvmop.asmOpcode());
    }

    @Override
    public String toString() {
        return String.format("%s",jvmop);
    }

}
