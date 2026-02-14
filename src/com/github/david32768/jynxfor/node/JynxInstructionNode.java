package com.github.david32768.jynxfor.node;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.TypeAnnotationNode;

import com.github.david32768.jynxfor.instruction.AbstractInstruction;
import com.github.david32768.jynxfor.ops.JvmOp;
import com.github.david32768.jynxfor.scan.Line;
import com.github.david32768.jynxfree.jvm.Context;
import com.github.david32768.jynxfree.jvm.TypeRef;

public interface JynxInstructionNode {
    
    public Line line();
    public JvmOp jvmop();
    public JynxAnnotationsNode annotations();

    public default Integer maxLength() {
        return jvmop().length();
    }

    public default Integer minLength() {
        return jvmop().length();
    }

    public default void resolve(int minoffset, int maxoffset){}

    public default boolean canThrow() {
        return jvmop().canThrow();
    }
    
    public default void addTypeAnnotation(TypeAnnotationNode annotation, boolean visible) {
        var typeref = TypeRef.fromASM(annotation.typeRef);
        assert switch(typeref) {
            case tro_var, tro_resource -> false;
            default -> typeref.context() == Context.CODE;
        };
        annotations().addAnnotation(annotation, visible);
    }

    public static void accept(JynxInstructionNode jin, MethodVisitor mv) {
        AbstractInstruction ain = (AbstractInstruction)jin;
        ain.accept(mv);
        jin.annotations().accept(mv);
    }
}
