package com.github.david32768.jynxfor.instruction;

import java.util.Arrays;

import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.MethodVisitor;

import com.github.david32768.jynxfor.ops.JvmOp;
import com.github.david32768.jynxfor.scan.Line;

public class DynamicInstruction extends AbstractInstruction {

    private final ConstantDynamic cd;
    private final Object[] bsmArgs;

    public DynamicInstruction(JvmOp jvmop,  ConstantDynamic  cstdyn, Line line) {
        super(jvmop, line);
        this.cd = cstdyn;
        this.bsmArgs = new Object[cd.getBootstrapMethodArgumentCount()];
        Arrays.setAll(bsmArgs, cd::getBootstrapMethodArgument);
    }

    public ConstantDynamic constantDynamic() {
        return cd;
    }
    
    @Override
    public void accept(MethodVisitor mv) {
        mv.visitInvokeDynamicInsn(cd.getName(), cd.getDescriptor(), cd.getBootstrapMethod(),bsmArgs);
    }

    @Override
    public String toString() {
        Object2String o2s = new Object2String();
        return String.format("%s %s", jvmop, o2s.constDynamic2String(cd));
    }

}
