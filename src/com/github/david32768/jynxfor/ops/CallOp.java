package com.github.david32768.jynxfor.ops;

import java.lang.invoke.MethodType;

import com.github.david32768.jynxfree.jvm.Feature;
import com.github.david32768.jynxfree.jvm.OpArg;

public class CallOp {

    private CallOp() {}
    

    
    public static MacroOp ofStatic(String classname, String methodname, String desc) {
        return of(classname, methodname, desc, JvmOp.asm_invokestatic, Feature.unlimited);
    }

    public static MacroOp ofStatic(Class<?> klass, String methodname, String desc) {
        return of(className(klass), methodname, desc, JvmOp.asm_invokestatic, Feature.unlimited);
    }

    public static MacroOp ofStatic(Class<?> klass, String methodname, String desc, Feature feature) {
        return of(className(klass), methodname, desc, JvmOp.asm_invokestatic, feature);
    }

    public static MacroOp ofVirtual(String classname, String methodname, String desc) {
        return of(classname, methodname, desc, JvmOp.asm_invokevirtual, Feature.unlimited);
    }

    public static MacroOp ofVirtual(Class<?> klass, String methodname, String desc) {
        return of(className(klass), methodname, desc, JvmOp.asm_invokevirtual, Feature.unlimited);
    }

    private static MacroOp of(String classname, String methodname, String desc, JvmOp op, Feature feature) {
        assert op.args() == OpArg.arg_method;
        JynxOp insm = AdjustToken.insertMethod(classname, methodname, desc);
        if (feature == Feature.unlimited) {
            return MacroOp.of(insm, op);
        } else {
            return MacroOp.of(LineOp.checkVersion(feature), insm, op);            
        }
    }

    public static String className(Class<?> klass) {
        if (klass.isPrimitive()) {
            return parmName(klass);
        }
        return klass.getName().replace('.', '/');
    }
    
    public static String parmName(Class<?> klass) {
        return MethodType.methodType(klass).toMethodDescriptorString().substring(2);
    }
    
    public static String descFrom(Class<?> rtype, Class<?>... ptypes) {
        return MethodType.methodType(rtype, ptypes).toMethodDescriptorString();
    }
    
}
