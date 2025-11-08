package asm;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.TypePath;

import static com.github.david32768.jynxfor.my.Message.*;

import static com.github.david32768.jynxfree.jynx.Global.*;
import static com.github.david32768.jynxfree.jynx.NameDesc.*;
import static com.github.david32768.jynxfree.jynx.ReservedWord.*;

import static com.github.david32768.jynxfree.jvm.AccessFlag.acc_final;
import static com.github.david32768.jynxfree.jvm.AccessFlag.acc_static;
import static com.github.david32768.jynxfree.jvm.StandardAttribute.ConstantValue;

import com.github.david32768.jynxfor.scan.ConstType;
import com.github.david32768.jynxfor.scan.JynxScanner;
import com.github.david32768.jynxfor.scan.Line;
import com.github.david32768.jynxfor.scan.Token;

import com.github.david32768.jynxfree.jvm.AccessFlag;
import com.github.david32768.jynxfree.jvm.Context;
import com.github.david32768.jynxfree.jynx.Access;
import com.github.david32768.jynxfree.jynx.Directive;

import jynx2asm.ClassChecker;
import jynx2asm.UniqueDirectiveChecker;

public class JynxFieldNode implements ContextDependent, HasAccessFlags {

    private final FieldNode fnode;
    private final Line line;
    private final Access accessName;
    private final ClassChecker checker;
    
    private final UniqueDirectiveChecker unique_checker;

    private JynxFieldNode(Line line, FieldNode fnode, Access accessname,ClassChecker checker) {
        this.fnode = fnode;
        this.checker = checker;
        this.accessName = accessname;
        this.line = line;
        this.unique_checker = new UniqueDirectiveChecker();
    }

    public static JynxFieldNode getInstance(Line line, ClassChecker checker) {
        Access accessname = checker.getAccess(Context.FIELD, line);
        String name = accessname.name();
        String desc = line.nextToken().asString();
        FIELD_DESC.validate(desc);
        if (checker.isComponent(Context.FIELD, name, desc)) {
            accessname.setComponent();
        }
        Token token = line.nextToken();
        Object value = null;
        if (!token.isEndToken()) {
            token.mustBe(equals_sign);
            token = line.lastToken();
            CHECK_SUPPORTS(ConstantValue);
            ConstType ctf = ConstType.getFromDesc(desc, Context.FIELD_VALUE);
            value = token.getValue(ctf);    // check range
            value = ctf.toJvmValue(value);
            if (accessname.is(acc_static)) {
                if (!accessname.is(acc_final)) {
                    // 5.5 bullet 6 overriding 4.7.2
                    // however openjdk seems to initialise non_final static fields
                    // although javac uses <clinit> method
                    LOG(M71,name,token); // "as %s is not a final static field, ' = %s' may be silently ignored by JVM (JVMS 5.5 6)"
                }
            } else {
                LOG(M53,name,token); // "as %s is not a static field, ' = %s' is silently ignored by JVM "
            }
            FIELD_NAME.validate(accessname);
        } else {
            FIELD_NAME.validate(accessname.name());
        }
        accessname.check4Field();
        FieldNode fnode = new FieldNode(accessname.getAccess(), name, desc, null, value);
        JynxFieldNode jfn = new JynxFieldNode(line, fnode, accessname, checker);
        checker.checkField(jfn);
        return jfn;
    }
    
    public Line getLine() {
        return line;
    }

    public String getName() {
        return fnode.name;
    }
    
    public String getDesc() {
        return fnode.desc;
    }
    
    @Override
    public boolean is(AccessFlag flag) {
        assert flag.isValid(Context.FIELD);
        return accessName.is(flag);
    }

    @Override
    public Context getContext() {
        return Context.FIELD;
    }

    @Override
    public void visitDirective(Directive dir, JynxScanner js) {
        Line linex = js.getLine();
        unique_checker.checkUnique(dir, line);
        switch (dir) {
            default -> visitCommonDirective(dir, linex, js);
        }
    }
    
    @Override
    public void setSignature(Line line) {
        assert fnode.signature == null; // dir_signature is unique within
        String signature = line.nextToken().asQuoted();
        FIELD_SIGNATURE.validate(signature);
        if (isComponent()) {
            checker.checkSignature4Field(signature, fnode.name);
        }
        fnode.signature = signature;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return fnode.visitAnnotation(desc, visible);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeref, TypePath tp, String desc, boolean visible) {
        return fnode.visitTypeAnnotation(typeref, tp, desc, visible);
    }

    private boolean hasAnnotations() {
        return fnode.invisibleAnnotations != null || fnode.invisibleTypeAnnotations != null
                || fnode.visibleAnnotations != null || fnode.visibleTypeAnnotations != null;
    }
    
    public FieldNode visitEnd(Directive dir) {
        String signature = fnode.signature;
        String name = fnode.name;
        if (signature == null && isComponent()) {
            checker.checkSignature4Field(signature, name);
        }
        if (dir ==  null && hasAnnotations()) {
            LOG(M270, Directive.end_field); // "%s directive missing but assumed"
        }
        return fnode;
    }
    
    @Override
    public String toString() {
        return String.format("[%s %s %s]",fnode.name,fnode.desc,fnode.signature);
    }
    
}
