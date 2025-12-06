package com.github.david32768.jynxfor.instruction;

import org.objectweb.asm.MethodVisitor;

import com.github.david32768.jynxfor.ops.JvmOp;
import com.github.david32768.jynxfor.scan.Line;
import com.github.david32768.jynxfor.scan.Token;

public class IncrInstruction extends AbstractInstruction {

    private final Token varToken;
    private final int incr;

    private int varnum;

    public IncrInstruction(JvmOp jvmop, Token vartoken, int incr, Line line) {
        super(jvmop, line);
        this.varToken = vartoken;
        this.incr = incr;
        this.varnum = -1;
    }

    @Override
    public JvmOp jvmop() {
        return varnum < 0? jvmop: jvmop.exactIncr(varnum, incr);
    }
    
    public Token varToken() {
        return varToken;
    }

    public void setVarnum(int varnum) {
        assert varnum >= 0;
        this.varnum = varnum;
    }
    
    @Override
    public void accept(MethodVisitor mv) {
        assert varnum >= 0;
        mv.visitIincInsn(varnum, incr);
    }

    @Override
    public String toString() {
        if (varnum < 0) {
            return String.format("%s %s %d", jvmop, varToken, incr);        
        } else {
            return String.format("%s %d %d", jvmop(), varnum, incr);                    
        }
    }

}
