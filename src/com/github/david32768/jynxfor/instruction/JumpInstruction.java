package com.github.david32768.jynxfor.instruction;

import org.objectweb.asm.MethodVisitor;

import com.github.david32768.jynxfor.ops.JvmOp;

import jynx2asm.JynxLabel;
import jynx2asm.StackLocals;

public class JumpInstruction implements JynxInstruction {

    private final JvmOp jvmop;    
    private final JynxLabel jlab;
    
    private boolean isDefinitelyNotWide;
    private boolean isDefinitelyWide;

    public JumpInstruction(JvmOp jop, JynxLabel jlab) {
        this.jvmop = jop;
        this.jlab = jlab;
        this.isDefinitelyNotWide = false;
        this.isDefinitelyWide = false;
    }

    @Override
    public JvmOp jvmop() {
        return jvmop;
    }
    
    @Override
    public void resolve(int minoffset, int maxoffset) {
        this.isDefinitelyNotWide = jlab.isDefinitelyNotWide(minoffset, maxoffset);
        this.isDefinitelyWide = jlab.isDefinitelyWide(minoffset, maxoffset);
        
        jlab.usedAt(minoffset, maxoffset, maxLength() - minLength());
    }

    @Override
    public void adjust(StackLocals stackLocals) {
        stackLocals.adjustStack(jvmop);        
        stackLocals.adjustLabelJump(jlab, jvmop);
    }

    private int wideLength() {
        return switch (jvmop) {
            case asm_goto, opc_goto_w -> JvmOp.opc_goto_w.length();
            case asm_jsr, opc_jsr_w -> JvmOp.opc_jsr_w.length();
            default -> jvmop.length() + JvmOp.opc_goto_w.length();
        };
    }
    
    @Override
    public Integer minLength() {
        if (isDefinitelyWide) {
            return wideLength();
        } else {
            return jvmop.length();
        }
    }
    
    @Override
    public Integer maxLength() {
        if (isDefinitelyNotWide) {
            return jvmop.length();
        } else {
            return wideLength();
        }
    }
    
    @Override
    public void accept(MethodVisitor mv) {
        mv.visitJumpInsn(jvmop.asmOpcode(), jlab.asmlabel());
    }

    @Override
    public String toString() {
        return String.format("%s %s",jvmop,jlab.name());
    }

}
