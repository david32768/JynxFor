package com.github.david32768.jynxfor.instruction;

import org.objectweb.asm.MethodVisitor;

import static com.github.david32768.jynxfor.my.Message.M908;

import com.github.david32768.jynxfor.ops.JvmOp;
import com.github.david32768.jynxfree.jynx.LogAssertionError;

import jynx2asm.handles.FieldHandle;
import jynx2asm.StackLocals;

public class FieldInstruction implements JynxInstruction {

    private final JvmOp jvmop;    
    private final FieldHandle fh;

    public FieldInstruction(JvmOp jop, FieldHandle fh) {
        this.jvmop = jop;
        this.fh = fh;
    }

    @Override
    public JvmOp jvmop() {
        return jvmop;
    }
    
    @Override
    public void accept(MethodVisitor mv) {
        mv.visitFieldInsn(jvmop.asmOpcode(),fh.owner(), fh.name(), fh.desc());
    }

    @Override
    public void adjust(StackLocals stackLocals) {
        String desc = fh.desc();
        String stackdesc;
        switch (jvmop) {
            case asm_getfield -> stackdesc = String.format("(L%s;)%s",fh.owner(),desc);
            case asm_getstatic -> stackdesc = "()" + desc;
            case asm_putfield -> stackdesc = String.format("(L%s;%s)V",fh.owner(),desc);
            case asm_putstatic -> stackdesc = "(" + desc + ")V";
            default -> // "unexpected Op %s in this instruction"),
                throw new LogAssertionError(M908,jvmop.name());
        }
        stackLocals.adjustStackOperand(stackdesc);
    }

    @Override
    public String toString() {
        return String.format("%s %s",jvmop,fh.ond());
    }

}
