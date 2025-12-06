package com.github.david32768.jynxfor.instruction;

import org.objectweb.asm.MethodVisitor;

import com.github.david32768.jynxfor.ops.JvmOp;
import com.github.david32768.jynxfor.scan.Line;

public class StackInstruction extends AbstractInstruction {

    public StackInstruction(JvmOp jvmop, Line line) {
        super(jvmop, line);
    }

    @Override
    public void accept(MethodVisitor mv) {
        mv.visitInsn(jvmop.asmOpcode());
    }

    @Override
    public String toString() {
        return jvmop.toString();
    }
    
}
