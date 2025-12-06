package com.github.david32768.jynxfor.instruction;

import org.objectweb.asm.MethodVisitor;

import com.github.david32768.jynxfor.ops.JvmOp;
import com.github.david32768.jynxfor.scan.ConstType;
import com.github.david32768.jynxfor.scan.Line;

public class LdcInstruction extends AbstractInstruction {

    private final Object cst;
    private final ConstType ct;

    public LdcInstruction(JvmOp jvmop,  Object cst, ConstType ct, Line line) {
        super(jvmop, line);
        this.cst = cst;
        this.ct = ct;
    }

    public ConstType constType() {
        return ct;
    }
    
    @Override
    public Integer maxLength() {
        if (jvmop == JvmOp.asm_ldc) {
            return JvmOp.opc_ldc_w.length();
        }
        return jvmop.length();
    }

    @Override
    public void accept(MethodVisitor mv) {
        mv.visitLdcInsn(cst);
    }

    @Override
    public String toString() {
        Object2String o2s = new Object2String();
        return String.format("%s %s",jvmop,o2s.asm2String(cst));
    }

}
