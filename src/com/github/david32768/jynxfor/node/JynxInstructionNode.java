package com.github.david32768.jynxfor.node;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.TypeAnnotationNode;

import com.github.david32768.jynxfor.instruction.JynxInstruction;
import com.github.david32768.jynxfree.jvm.Context;
import com.github.david32768.jynxfree.jvm.TypeRef;

public record JynxInstructionNode
        (JynxInstruction instruction, JynxAnnotationsNode annotations) {

    public JynxInstructionNode(JynxInstruction instruction) {
        this(instruction, new JynxAnnotationsNode());
    }

    public final void accept(MethodVisitor mv) {
        instruction.accept(mv);
        annotations.accept(mv);
    }

    public void addTypeAnnotation(TypeAnnotationNode annotation, boolean visible) {
        var typeref = TypeRef.fromASM(annotation.typeRef);
        assert switch(typeref) {
            case tro_var, tro_resource -> false;
            default -> typeref.context() == Context.CODE;
        };
        annotations.addAnnotation(annotation, visible);
    }

}
