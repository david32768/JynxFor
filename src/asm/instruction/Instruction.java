package asm.instruction;

import org.objectweb.asm.MethodVisitor;

import com.github.david32768.jynxfor.ops.JvmOp;

import jynx2asm.StackLocals;

public class Instruction {

    protected JvmOp jvmop;
    

    protected Instruction(JvmOp jvmop) {
        this.jvmop = jvmop;
    }

    public static Instruction getInstance(JvmOp jvmop) {
        assert !jvmop.isStack();
        return new Instruction(jvmop);
    }
    
    public boolean needLineNumber() {
        return jvmop == JvmOp.asm_idiv  || jvmop == JvmOp.asm_ldiv;
    }
    
    public JvmOp resolve(int minoffset, int maxoffset) {
        return jvmop;
    }

    public void adjust(StackLocals stackLocals) {
        stackLocals.adjustStack(jvmop);
    }

    public Integer minLength() {
        return jvmop.length();
    }
    
    public Integer maxLength() {
        return jvmop.length();
    }
    
    public void accept(MethodVisitor mv) {
        mv.visitInsn(jvmop.asmOpcode());
    }

    @Override
    public String toString() {
        return String.format("%s",jvmop);
    }

}
