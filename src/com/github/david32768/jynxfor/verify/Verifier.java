package com.github.david32768.jynxfor.verify;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassFile.ClassHierarchyResolverOption;
import java.lang.classfile.ClassModel;
import java.util.List;

import static com.github.david32768.jynxfor.my.Message.M413;
import static com.github.david32768.jynxfor.my.Message.M624;
import static com.github.david32768.jynxfor.my.Message.M649;
import static com.github.david32768.jynxfree.jynx.Global.LOG;
import static com.github.david32768.jynxfree.jynx.GlobalOption.DEBUG;

import com.github.david32768.jynxfree.jynx.Global;
import com.github.david32768.jynxfree.transform.ClassModels;
import com.github.david32768.jynxfree.transform.Transforms;

public class Verifier {

    private final Resolver resolver;
    
    public Verifier(Resolver resolver) {
        this.resolver = resolver;
    }

    public boolean verify(byte[] bytes) {
        ClassHierarchyResolverOption resolveroption = resolver.getResolverOption();
        ClassFile classfile = ClassFile.of(resolveroption);
        ClassModel cm = classfile.parse(bytes);
        int major = cm.majorVersion();
        try {
            List<VerifyError> errors;
            if (major < ClassFile.JAVA_6_VERSION
                    || major == ClassFile.JAVA_6_VERSION && !ClassModels.hasStackMap(cm)) {
                bytes = Transforms.addStackMapForVerification(classfile, cm);
                errors = classfile.verify(bytes);
            } else {
                errors = classfile.verify(cm);
            }
            int errct = printErrors(errors);
            if(errct > 0) {
                // "ASM Simple Verifier used to try and clarify error reported by ClassFile.verify"
                Global.LOG(M649);
                ASMVerify(bytes);
            }
        } catch (UnsupportedOperationException ex) {
            // "ASM Simple Verifier used"),
            Global.LOG(M624);
            ASMVerify(bytes);
        }
        String classname = cm.thisClass().asInternalName();
        return Global.END_MESSAGES(classname);
    }

    private int printErrors(List<VerifyError> errors) {
        for (var error : errors) {
            if (Global.OPTION(DEBUG)) {
                error.printStackTrace();
            }
            // "Verification: %s"
            LOG(M413, error);
        }
        return errors.size();        
    }
    
    private int ASMVerify(byte[] bytes) {
        return JynxSimpleVerifier.verify(bytes, resolver);
    }

}
