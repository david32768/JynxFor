package com.github.david32768.jynxfor.instruction;

import org.objectweb.asm.MethodVisitor;

import com.github.david32768.jynxfor.ops.JvmOp;
import com.github.david32768.jynxfree.jvm.OpArg;

public class IntInstruction implements JynxInstruction {

    private final JvmOp jvmop;    
    private final int value;

    public IntInstruction(JvmOp jop, int value) {
        this.jvmop = jop;
        this.value = value;
    }

    @Override
    public JvmOp jvmop() {
        return jvmop;
    }
    
    @Override
    public void accept(MethodVisitor mv) {
        mv.visitIntInsn(jvmop.asmOpcode(),value);
    }

    @Override
    public String toString() {
        return String.format("%s %d",jvmop,value);
    }

    @Override
    public boolean needLineNumber() {
        return jvmop.args() == OpArg.arg_atype;
    }

}
