package com.github.david32768.jynxfor.node;


import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.TypeAnnotationNode;

import static com.github.david32768.jynxfor.my.Message.M203;
import static com.github.david32768.jynxfor.my.Message.M279;
import static com.github.david32768.jynxfree.jynx.Global.LOG;

import com.github.david32768.jynxfor.scan.Line;
import com.github.david32768.jynxfree.jvm.TypeRef;

import jynx2asm.JynxLabel;

public class JynxCatchNode implements LabelRange {

    public static record LabelRecord(JynxLabel from, JynxLabel to, JynxLabel using){}
    
    private final JynxLabel fromLab;
    private final JynxLabel toLab;
    private final JynxLabel usingLab;
    private final String exception;
    private final Line line;
    private final JynxAnnotationsNode typeAnnotations;
    
    public JynxCatchNode(JynxLabel fromLab, JynxLabel toLab, JynxLabel usingLab, String exception, Line line) {
        if (fromLab.equals(usingLab)) {
                LOG(M203); // "potential infinite loop - catch using label equals catch from label"
        }
        if (fromLab.equals(toLab)) {
                LOG(M279); // "empty catch block - from label equals to label"
        }
        this.fromLab = fromLab;
        this.toLab = toLab;
        this.usingLab = usingLab;
        this.exception = exception;
        this.line = line;
        this.typeAnnotations = new JynxAnnotationsNode();
    }

    @Override
    public JynxLabel from() {
        return fromLab;
    }

    @Override
    public JynxLabel to() {
        return toLab;
    }

    @Override
    public Line line() {
        return line;
    }
    
    public JynxLabel usingLab() {
        return usingLab;
    }

    public LabelRecord labels() {
        return new LabelRecord(fromLab, toLab, usingLab);
    }
    
    public void addAnnotation(TypeAnnotationNode annotation, boolean visible) {
        assert TypeRef.fromASM(annotation.typeRef) == TypeRef.trt_except;
        typeAnnotations.addAnnotation(annotation, visible);
    }

    public void accept(MethodVisitor mv) {
        mv.visitTryCatchBlock(fromLab.asmlabel(), toLab.asmlabel(), usingLab.asmlabel(), exception);
        typeAnnotations.accept(mv);
    }
    
    @Override
    public String toString() {
        return String.format("%s %s %s %s", fromLab, toLab,usingLab,exception);
    }
}
