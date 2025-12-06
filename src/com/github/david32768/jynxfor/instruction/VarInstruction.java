package com.github.david32768.jynxfor.instruction;

import org.objectweb.asm.MethodVisitor;

import com.github.david32768.jynxfor.ops.JvmOp;
import com.github.david32768.jynxfor.scan.Line;
import com.github.david32768.jynxfor.scan.Token;

public class VarInstruction extends AbstractInstruction {

    private final Token varToken;
    private int varnum = -1;
    
    public VarInstruction(JvmOp jvmop, Token vartoken, Line line) {
        super(jvmop, line);
        this.varToken = vartoken;
    }

    @Override
    public JvmOp jvmop() {
        return myop();
    }

    private JvmOp myop() {
        return varnum == -1? jvmop: jvmop.exactVar(varnum);
    }
    
    public Token varToken() {
        return varToken;
    }

    public void setVarnum(int varnum) {
        this.varnum = varnum;
    }
    
    @Override
    public void accept(MethodVisitor mv) {
        assert varnum >= 0;
        mv.visitVarInsn(myop().asmOpcode(),varnum);
    }
    
    @Override
    public String toString() {
        if (varnum < 0) {
            return String.format("%s %s", jvmop, varToken);
        }
        return String.format("%s %d", myop(), varnum);
    }

}
