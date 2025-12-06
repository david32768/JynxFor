package com.github.david32768.jynxfor.instruction;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.TypeAnnotationNode;

import com.github.david32768.jynxfor.ops.JvmOp;
import com.github.david32768.jynxfor.scan.Line;

public interface JynxInstruction {

    public JvmOp jvmop();
    public Line line();
    
    public void accept(MethodVisitor mv);

    public default Integer maxLength() {
        return jvmop().length();
    }

    public default Integer minLength() {
        return jvmop().length();
    }

    public default void resolve(int minoffset, int maxoffset){}
    
}
