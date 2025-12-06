package com.github.david32768.jynxfor.instruction;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.github.david32768.jynxfor.ops.JvmOp;
import com.github.david32768.jynxfor.scan.Line;

import jynx2asm.JynxLabel;

public class LabelInstruction extends AbstractInstruction {

    private final JynxLabel jlab;
    
    private Object[] stackArray;
    private Object[] localArray;

    public LabelInstruction(JvmOp jvmop, JynxLabel jlab, Line line) {
        super(jvmop, line);
        this.jlab = jlab;
    }

    public JynxLabel jynxlab() {
        return jlab;
    }
    
    public void setFrame(Object[] stackArray, Object[] localArray) {
        this.stackArray = stackArray;
        this.localArray = localArray;
    }
    
    @Override
    public void resolve(int minoffset, int maxoffset) {
        jlab.setOffset(minoffset, maxoffset);
    }

    @Override
    public void accept(MethodVisitor mv) {
        mv.visitLabel(jlab.asmlabel());
        if (localArray != null) {
            mv.visitFrame(Opcodes.F_NEW, localArray.length, localArray, stackArray.length, stackArray);            
        }        
    }

    @Override
    public String toString() {
        return String.format("label %s",jlab.name());
    }

}
