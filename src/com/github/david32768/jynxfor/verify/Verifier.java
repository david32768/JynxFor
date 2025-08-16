package com.github.david32768.jynxfor.verify;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassFile.ClassHierarchyResolverOption;
import java.lang.classfile.ClassModel;

import static com.github.david32768.jynxfor.my.Message.M413;
import static com.github.david32768.jynxfor.my.Message.M618;
import static com.github.david32768.jynxfor.my.Message.M619;
import static com.github.david32768.jynxfor.my.Message.M624;
import static com.github.david32768.jynxfree.jynx.Global.LOG;

import com.github.david32768.jynxfree.jynx.Global;
import com.github.david32768.jynxfree.transform.ClassModels;
import com.github.david32768.jynxfree.transform.Transforms;

public class Verifier {

    private final Resolver resolver;
    
    public Verifier(Resolver resolver) {
        this.resolver = resolver;
    }

    public boolean verify(byte[] bytes) {
        int errct = verifyChooser(bytes.clone());
        if (errct == 0) {
            // "Verification successful"
            LOG(M618);
            return true;
        } else {
            // "Verification failed with %d errors"
            LOG(M619, errct);
            return false;
        }
    }

    private int verifyChooser(byte[] bytes) {
        ClassHierarchyResolverOption resolveroption = resolver.getResolverOption();
        var classfile = ClassFile.of(resolveroption);
        ClassModel cm = classfile.parse(bytes);
        int major = cm.majorVersion();
        if (major > ClassFile.JAVA_6_VERSION || major == ClassFile.JAVA_6_VERSION && ClassModels.hasStackMap(cm)) {
            return classfileVerify(bytes, resolveroption);
        }
        try {
            bytes = Transforms.upgradeToAtLeastV6(classfile, cm);
            return classfileVerify(bytes, resolveroption);
        } catch (UnsupportedOperationException ex) {
            // "ASM Simple Verifier used"),
            Global.LOG(M624);
            return ASMVerify(bytes);
        }
    }

    private int classfileVerify(byte[] bytes,ClassHierarchyResolverOption resolveroption) {
        var classfile = ClassFile.of(resolveroption);
        var errors = classfile.verify(bytes);
        for (var error : errors) {
            // "Verification: %s"
            LOG(M413, error);
        }
        return errors.size();        
    }
    
    private int ASMVerify(byte[] bytes) {
        return JynxSimpleVerifier.verify(bytes, resolver);
    }

}
