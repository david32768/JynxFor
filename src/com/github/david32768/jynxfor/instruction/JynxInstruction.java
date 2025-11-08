package com.github.david32768.jynxfor.instruction;

import org.objectweb.asm.MethodVisitor;

import com.github.david32768.jynxfor.ops.JvmOp;

import jynx2asm.StackLocals;

public interface JynxInstruction {

    public JvmOp jvmop();
    
    public void accept(MethodVisitor mv);

    public default void adjust(StackLocals stackLocals) {
        stackLocals.adjustStack(jvmop());        
    }

    public default Integer maxLength() {
        return jvmop().length();
    }

    public default Integer minLength() {
        return jvmop().length();
    }

    public default boolean needLineNumber() {
        return false;
    }

    public default void resolve(int minoffset, int maxoffset){}
    
}
