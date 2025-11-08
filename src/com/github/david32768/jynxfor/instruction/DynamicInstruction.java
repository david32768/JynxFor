package com.github.david32768.jynxfor.instruction;

import java.util.Arrays;

import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.MethodVisitor;

import com.github.david32768.jynxfor.ops.JvmOp;

import jynx2asm.StackLocals;

public class DynamicInstruction implements JynxInstruction {

    private final JvmOp jvmop;    
    private final ConstantDynamic cd;
    private final Object[] bsmArgs;

    public DynamicInstruction(JvmOp jvmop,  ConstantDynamic  cstdyn) {
        this.jvmop = jvmop;
        this.cd = cstdyn;
        this.bsmArgs = new Object[cd.getBootstrapMethodArgumentCount()];
        Arrays.setAll(bsmArgs, cd::getBootstrapMethodArgument);
    }

    @Override
    public JvmOp jvmop() {
        return jvmop;
    }
    
    @Override
    public void accept(MethodVisitor mv) {
        mv.visitInvokeDynamicInsn(cd.getName(), cd.getDescriptor(), cd.getBootstrapMethod(),bsmArgs);
    }

    @Override
    public void adjust(StackLocals stackLocals) {
        stackLocals.adjustStackOperand(cd.getDescriptor());
    }

    @Override
    public boolean needLineNumber() {
        return true;
    }

    @Override
    public String toString() {
        Object2String o2s = new Object2String();
        return String.format("%s %s", jvmop, o2s.constDynamic2String(cd));
    }

}
