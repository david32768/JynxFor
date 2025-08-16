package com.github.david32768.jynxfor.verify;

import java.util.List;
import java.util.Optional;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Interpreter;
import org.objectweb.asm.tree.analysis.SimpleVerifier;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.Type;

import static com.github.david32768.jynxfor.my.Message.M75;
import static com.github.david32768.jynxfree.jynx.Global.LOG;

public class JynxSimpleVerifier extends SimpleVerifier {

    private final Resolver resolver;

    private JynxSimpleVerifier(
            final Type currentClass,
            final Type currentSuperClass,
            final List<Type> currentClassInterfaces,
            final boolean isInterface,
            final Resolver resolver) {
        super(Opcodes.ASM9, currentClass, currentSuperClass, currentClassInterfaces, isInterface);
        this.resolver = resolver;
        super.setClassLoader(resolver.getLoader());
    }
    
    public static JynxSimpleVerifier of(ClassNode cnode, Resolver resolver) {
        Type classtype = Type.getObjectType(cnode.name);
        Type supertype = cnode.superName == null? null: Type.getObjectType(cnode.superName);
        boolean isinterface = (cnode.access & Opcodes.ACC_INTERFACE) != 0;
        List<Type> interfaces = cnode.interfaces.stream()
                .map(itf -> Type.getObjectType(itf))
                .toList();
        return new JynxSimpleVerifier(classtype, supertype, interfaces, isinterface, resolver);
    }

    @Override
    protected boolean isSubTypeOf(BasicValue value, BasicValue expected) {
        Type type = value.getType();
        Type expectedType = expected.getType();
        if (type == null || expectedType == null) {
            return type == null && expectedType == null;
        }
        String valuestr = type.getInternalName();
        String expectedstr = expectedType.getInternalName();
        return resolver.isSubTypeOf(valuestr, expectedstr) || super.isSubTypeOf(value, expected);
    }

    @Override
    public BasicValue merge(BasicValue value1, BasicValue value2) {
        Type value1type = value1.getType();
        Type value2type = value2.getType();
        if (value1type == null || value2type == null) {
            return BasicValue.UNINITIALIZED_VALUE;
        }
        String value1str = value1type.getInternalName();
        String value2str = value2type.getInternalName();
        Optional<String> common = resolver.getCommonSuperClass(value1str, value2str);
        if (common.isEmpty()) {
            return super.merge(value1, value2);
        } else {
            Type result = Type.getObjectType(common.orElseThrow());
            return new BasicValue(result);
        }
    }

    @Override
    protected boolean isInterface(Type type) {
        return resolver.isInterface(type.getInternalName()) || super.isInterface(type);
    }

    public void setLoader(ClassLoader loader) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Type getSuperClass(Type type) {
        Optional<String> superClass = resolver.getSuperClass(type.getInternalName());
        if (superClass.isEmpty()) {
            return super.getSuperClass(type);
        } else {
            return Type.getObjectType(superClass.orElseThrow());
        }
    }

    public static int verify(byte[] bytes, Resolver resolver) {
        int errct = 0;
        ClassReader cr = new ClassReader(bytes);
        ClassNode cn = new ClassNode();
        cr.accept(cn,0);
        Interpreter<BasicValue> verifier = JynxSimpleVerifier.of(cn, resolver);
        Analyzer<BasicValue> analyzer = new Analyzer<>(verifier);
        for (var mnode: cn.methods) {
            try {
                analyzer.analyze(cn.name, mnode);
            } catch (AnalyzerException | IllegalArgumentException e) {
                ++errct;
                // "Method %s failed %s check:%n    %s"
                LOG(e, M75, mnode.name, "simple verifier", e.getMessage());
            }
        }
        return errct;        
    }
/*
// FROM ASM SimpleVerifier DOC
 * A semantic bytecode analyzer. <i>This class does not fully check that JSR and RET instructions
 * are valid.</i>
// 
*/
}
