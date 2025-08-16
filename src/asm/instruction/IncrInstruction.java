package asm.instruction;

import org.objectweb.asm.MethodVisitor;

import com.github.david32768.jynxfor.ops.JvmOp;

import jynx2asm.StackLocals;
import jynx2asm.Token;

public class IncrInstruction extends Instruction {

    private final Token varToken;
    private final int incr;

    private int varnum;

    public IncrInstruction(JvmOp jop, Token vartoken, int incr) {
        super(jop);
        this.varToken = vartoken;
        this.incr = incr;
    }

    @Override
    public void adjust(StackLocals stackLocals) {
        this.varnum = stackLocals.adjustIncr(varToken);
        this.jvmop = jvmop.exactIncr(varnum, incr);
    }

    @Override
    public void accept(MethodVisitor mv) {
        mv.visitIincInsn(varnum, incr);
    }

    @Override
    public String toString() {
        return String.format("%s %d %d",jvmop,varnum, incr);
    }

}
