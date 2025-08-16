package asm.instruction;

import org.objectweb.asm.MethodVisitor;

import com.github.david32768.jynxfor.ops.JvmOp;
import com.github.david32768.jynxfree.jvm.OpArg;

public class IntInstruction extends Instruction {

    private final int value;

    public IntInstruction(JvmOp jop, int value) {
        super(jop);
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

    @Override
    public boolean needLineNumber() {
        return jvmop.args() == OpArg.arg_atype;
    }

}
