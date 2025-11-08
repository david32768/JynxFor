package asm;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.TypePath;

import static com.github.david32768.jynxfor.my.Message.*;

import com.github.david32768.jynxfor.scan.JynxScanner;
import com.github.david32768.jynxfor.scan.Line;

import com.github.david32768.jynxfree.jvm.Context;
import com.github.david32768.jynxfree.jynx.Directive;
import com.github.david32768.jynxfree.jynx.LogIllegalStateException;

public interface ContextDependent {
    
    public void visitDirective(Directive dir, JynxScanner js);

    // exceptions are thrown so tokens are skipped
    
    public default Context getContext() {
        throw new LogIllegalStateException(M42,Directive.dir_signature); // "%s invalid in context"
    }
    
    public default void setSource(Line line) {
        throw new LogIllegalStateException(M42,Directive.dir_signature); // "%s invalid in context"
    }
    
    public default void setSignature(Line line) {
        throw new LogIllegalStateException(M42,Directive.dir_signature); // "%s invalid in context"
    }
    
    public default AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        throw new LogIllegalStateException(M42,Directive.dir_annotation); // "%s invalid in context"
    }
    
    public default AnnotationVisitor visitTypeAnnotation(int typeref, TypePath tp, String desc, boolean visible) {
        throw new LogIllegalStateException(M42,Directive.dir_annotation); // "%s invalid in context"
    }
    
    public default AnnotationVisitor visitAnnotationDefault() {
        throw new LogIllegalStateException(M42,Directive.dir_annotation); // "%s invalid in context"
    }
    
    public default AnnotationVisitor visitParameterAnnotation(String desc, int parameter, boolean visible) {
        throw new LogIllegalStateException(M42,Directive.dir_annotation);  // "%s invalid in context"
    }
    
    public default void visitCommonDirective(Directive dir, Line line, JynxScanner js) {
        switch(dir) {
            case dir_source -> setSource(line);
            case dir_signature -> setSignature(line);
            default -> {
                if (dir.isAnotation()) {
                    JynxAnnotation.setAnnotation(dir,this,js);
                } else {
                    throw new EnumConstantNotPresentException(dir.getClass(), dir.name());
                }
            }
        }
    }
    
}
