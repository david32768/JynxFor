package com.github.david32768.jynxfor.instruction;

import org.objectweb.asm.MethodVisitor;

import com.github.david32768.jynxfor.ops.JvmOp;
import com.github.david32768.jynxfor.scan.Token;

import jynx2asm.StackLocals;

public class VarInstruction implements JynxInstruction {

    private JvmOp jvmop;    
    private final Token varToken;
    private int varnum = -1;
    
    public VarInstruction(JvmOp jop, Token vartoken) {
        this.jvmop = jop;
        this.varToken = vartoken;
    }

    @Override
    public JvmOp jvmop() {
        return jvmop;
    }
    
    @Override
    public void adjust(StackLocals stackLocals) {
        varnum = stackLocals.adjustLoadStore(jvmop, varToken);
        assert varnum >= 0;
        this.jvmop = jvmop.exactVar(varnum);
    }

    @Override
    public void accept(MethodVisitor mv) {
        mv.visitVarInsn(jvmop.asmOpcode(),varnum);
    }
    
    @Override
    public String toString() {
        if (varnum < 0) {
            return String.format("%s %s",jvmop,varToken);
        }
        return String.format("%s %d",jvmop,varnum);
    }

}
