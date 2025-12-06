package com.github.david32768.jynxfor.instruction;

import org.objectweb.asm.MethodVisitor;

import com.github.david32768.jynxfor.ops.JvmOp;
import com.github.david32768.jynxfor.scan.Line;

import jynx2asm.handles.FieldHandle;

public class FieldInstruction extends AbstractInstruction {

    private final FieldHandle fh;

    public FieldInstruction(JvmOp jvmop, FieldHandle fh, Line line) {
        super(jvmop, line);
        this.fh = fh;
    }

    public FieldHandle fieldHandle() {
        return fh;
    }
    
    @Override
    public void accept(MethodVisitor mv) {
        mv.visitFieldInsn(jvmop.asmOpcode(),fh.owner(), fh.name(), fh.desc());
    }

    @Override
    public String toString() {
        return String.format("%s %s",jvmop,fh.ond());
    }

}
