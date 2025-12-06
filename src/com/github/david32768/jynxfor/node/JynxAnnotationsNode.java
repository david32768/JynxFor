package com.github.david32768.jynxfor.node;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableAnnotationNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

import com.github.david32768.jynxfree.jvm.TypeRef;

public class JynxAnnotationsNode {
      
        private final List<AnnotationNode> visibleAnnotations;
        private final List<AnnotationNode> invisibleAnnotations;
        
        public JynxAnnotationsNode() {
            this.visibleAnnotations = new ArrayList<>();
            this.invisibleAnnotations = new ArrayList<>();
        }
        
        public void addAnnotation(AnnotationNode annotation, boolean visible) {
            if (visible) {
                visibleAnnotations.add(annotation);
            } else {
                invisibleAnnotations.add(annotation);                
            }
        }
        
        public boolean isEmpty() {
            return visibleAnnotations.isEmpty() && invisibleAnnotations.isEmpty();
        }
        
        public void accept(MethodVisitor mv) {
            visibleAnnotations.stream().forEach(a -> acceptAnnotation(mv, a, true));
            invisibleAnnotations.stream().forEach(a -> acceptAnnotation(mv, a, false));
        }
        
        private void acceptAnnotation(MethodVisitor mv, AnnotationNode annotation, boolean visible) {
            AnnotationVisitor av = switch(annotation) {
                case LocalVariableAnnotationNode lva -> {
                    Label[] starts = lva.start.stream()
                            .map(LabelNode::getLabel)
                            .toArray(Label[]::new);
                    Label[] ends = lva.end.stream()
                            .map(LabelNode::getLabel)
                            .toArray(Label[]::new);
                    int[] indices = lva.index.stream()
                            .mapToInt(Integer::intValue)
                            .toArray();
                    yield mv.visitLocalVariableAnnotation(lva.typeRef, lva.typePath,
                        starts, ends, indices,
                        annotation.desc, visible);
                }
                case TypeAnnotationNode a when TypeRef.fromASM(a.typeRef) == TypeRef.trt_except ->
                        mv.visitTryCatchAnnotation(a.typeRef, a.typePath, a.desc, visible);
                case TypeAnnotationNode a ->
                    mv.visitInsnAnnotation(a.typeRef, a.typePath, a.desc, visible);
                case AnnotationNode a ->
                    mv.visitAnnotation(a.desc, visible);
            };
            annotation.accept(av);
        }
    }

