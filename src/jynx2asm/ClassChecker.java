package jynx2asm;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.objectweb.asm.Handle;

import static com.github.david32768.jynxfor.my.Message.*;
import static com.github.david32768.jynxfree.jvm.HandleType.*;
import static com.github.david32768.jynxfree.jynx.Global.*;

import static com.github.david32768.jynxfree.jvm.AccessFlag.acc_final;
import static com.github.david32768.jynxfree.jvm.Context.FIELD;
import static com.github.david32768.jynxfree.jvm.Context.METHOD;
import static com.github.david32768.jynxfree.jynx.ClassType.RECORD;

import com.github.david32768.jynxfor.ops.JvmOp;

import com.github.david32768.jynxfree.jvm.AccessFlag;
import com.github.david32768.jynxfree.jvm.Constants;
import com.github.david32768.jynxfree.jvm.Context;
import com.github.david32768.jynxfree.jvm.Feature;
import com.github.david32768.jynxfree.jvm.HandleType;
import com.github.david32768.jynxfree.jvm.JvmVersion;
import com.github.david32768.jynxfree.jynx.Access;
import com.github.david32768.jynxfree.jynx.ClassType;
import com.github.david32768.jynxfree.jynx.Directive;
import com.github.david32768.jynxfree.jynx.GlobalOption;
import com.github.david32768.jynxfree.jynx.NameDesc;

import asm.JynxComponentNode;
import asm.JynxFieldNode;
import asm.JynxMethodNode;
import jynx2asm.handles.FieldHandle;
import jynx2asm.handles.LocalMethodHandle;
import jynx2asm.handles.MethodHandle;

public class ClassChecker {
    
    private final Map<String,JynxComponentNode> components = new HashMap<>();

    private final Map<LocalMethodHandle,ObjectLine<HandleType>> ownMethods;
    private final Map<MethodHandle,ObjectLine<HandleType>> ownMethodsUsed;
    
    private int fieldComponentCt = 0;
    private int instanceFieldCt = 0;
    private final Map<String,JynxFieldNode> fields = new HashMap<>();

    private final String className;
    private final Access classAccess;
    private final ClassType classType;
    private final JvmVersion jvmVersion;
    private String superClassName;
    private boolean hasImplements;

    private int specialct;
    private int newct;
    
    private ClassChecker(Access classAccess) {
        this.className = classAccess.name();
        this.classAccess = classAccess;
        this.classType = classAccess.classType();
        this.jvmVersion = classAccess.jvmVersion();
        this.ownMethodsUsed = new TreeMap<>(); // sorted for reproducibilty
        this.ownMethods = new TreeMap<>(); // sorted for reproducibilty
        this.hasImplements = false;
    }

    public final static LocalMethodHandle EQUALS_METHOD = LocalMethodHandle.of(Constants.EQUALS);
    public final static LocalMethodHandle TOSTRING_METHOD = LocalMethodHandle.of(Constants.TOSTRING);
    public final static LocalMethodHandle HASHCODE_METHOD = LocalMethodHandle.of(Constants.HASHCODE);

    public final static LocalMethodHandle FINALIZE_METHOD = LocalMethodHandle.of(Constants.FINALIZE);

    public final static Map<String,LocalMethodHandle> SERIAL_METHODS;
    
    static {
        SERIAL_METHODS = new HashMap<>();
        for (Constants method: Constants.PRIVATE_SERIALIZATION_METHODS) {
            LocalMethodHandle mh = LocalMethodHandle.of(method);
            SERIAL_METHODS.put(mh.name(),mh);
        }
    }
  
    private final static ObjectLine<HandleType> VIRTUAL_METHOD_HANDLE_LINE = new ObjectLine<>(REF_invokeVirtual, Line.EMPTY);
    private final static ObjectLine<HandleType> STATIC_METHOD_HANDLE_LINE = new ObjectLine<>(REF_invokeStatic, Line.EMPTY);
    
    public static ClassChecker getInstance(Access classAccess) {
        ClassChecker checker = new ClassChecker(classAccess);
        checker.addStandardMethods();
        return checker;
    }

    private void addStandardMethods() {
        if (classType == ClassType.ENUM) { // final methods in java/lang/Enum
            Constants.FINAL_ENUM_METHODS.stream()
                    .map(Constants::toString)
                    .map(LocalMethodHandle::getInstance)
                    .forEach(lmh -> ownMethods.put(lmh,VIRTUAL_METHOD_HANDLE_LINE));
            LocalMethodHandle compare = LocalMethodHandle.getInstance(String.format(
                    Constants.COMPARETO_FORMAT.stringValue(), className));
            ownMethods.put(compare,VIRTUAL_METHOD_HANDLE_LINE);
        }
        if (!Constants.isObjectClass(className)
                && classType != ClassType.MODULE_CLASS && classType != ClassType.PACKAGE) {
            Constants.FINAL_OBJECT_METHODS.stream()
                    .map(Constants::stringValue)
                    .map(LocalMethodHandle::getInstance)
                    .forEach(lmh -> ownMethods.put(lmh,VIRTUAL_METHOD_HANDLE_LINE));
        }
    }
    
    public void setSuper(String csuper) {
        if (classType == ClassType.ENUM && Constants.ENUM_SUPER.equalsString(csuper)) {
            String str = String.format(Constants.VALUES_FORMAT.stringValue(),className);
            MethodHandle values = MethodHandle.getInstance(str,REF_invokeStatic);
            ownMethodsUsed.put(values,STATIC_METHOD_HANDLE_LINE);
            str = String.format(Constants.VALUEOF_FORMAT.stringValue(),className);
            MethodHandle valueof = MethodHandle.getInstance(str,REF_invokeStatic);
            ownMethodsUsed.put(valueof,STATIC_METHOD_HANDLE_LINE);
        }
        superClassName = csuper;
    }
    
    public void hasImplements() {
        hasImplements = true;
    }
    
    public String getClassName() {
        return className;
    }

    public JvmVersion getJvmVersion() {
        return jvmVersion;
    }

    public ClassType getClassType() {
        return classType;
    }

    public void usedMethod(MethodHandle mh, JvmOp jvmop, Line line) {
        HandleType ht = HandleType.fromOp(jvmop.getOpcode(), mh.isInit());
        if (ht == REF_newInvokeSpecial
                && (mh.owner().equals(className) || mh.owner().equals(superClassName))) {
            ++specialct;
        }
        usedMethod(mh, ht, line);
    }
    
    public void usedNew(String classname) {
        if (classname.equals(className) || classname.equals(superClassName)) {
            ++newct;
        }
    }
    
    private void usedMethod(MethodHandle mh, HandleType ht, Line line) {
        assert !ht.isField();
        String owner = mh.owner();
        if (owner.equals(className)) {
            ObjectLine<HandleType> objline = new ObjectLine<>(ht,line);
            ObjectLine<HandleType> previous = ownMethodsUsed.putIfAbsent(mh, objline);
            if (previous != null && !ht.maybeOK(previous.object())) {
                // "%s has different type %s from previous %s at line %d"
                LOG(M405,mh.ond(),ht, previous.object(),previous.line().getLinect());
            }
        } else if (OPTION(GlobalOption.CHECK_REFERENCES)) {
            mh.checkReference();
        }
    }
    
    public void mayBeHandle(Object handleobj, Line line) {
        if (handleobj instanceof Handle handle) {
            HandleType ht = HandleType.getInstance(handle.getTag());
            if (ht.isField()) {
                FieldHandle fh = FieldHandle.of(handle);
                usedField(fh);
            } else {
                MethodHandle mh = MethodHandle.of(handle);
                usedMethod(mh,ht,line);
            }
        }
    }
    
    public Access getAccess(Context context, Line line) {
        EnumSet<AccessFlag> flags = line.getAccFlags();
        String name = line.nextToken().asName();
        return Access.getInstance(flags, jvmVersion, name, classType);
    }
    
    public Access getAccessOptName(Context context, Line line) {
        EnumSet<AccessFlag> flags = line.getAccFlags();
        Token optname = line.nextToken();
        String name = optname.isEndToken()? null: optname.asName();
        return Access.getInstance(flags, jvmVersion, name, classType);
    }
    
    public void checkComponent(JynxComponentNode jcn) {
        String compname = jcn.getName();
        JynxComponentNode previous = components.put(compname,jcn);
        if (previous != null) {
            // "duplicate %s: %s already defined at line %d"
            LOG(M40,Directive.dir_component,compname,previous.getLine().getLinect());
        }
    }

    public void checkMethod(JynxMethodNode jmn) {
        LocalMethodHandle lmh = jmn.getLocalMethodHandle();
        String name = jmn.getName();
        specialct = 0;
        newct = 0;
        HandleType ht;
        if (jmn.isStatic()) {
            ht = REF_invokeStatic;
        } else if (jmn.isInit()){
            ht = REF_newInvokeSpecial;
        } else if (classType == ClassType.INTERFACE) {
            ht = REF_invokeInterface;
        } else {
            ht = REF_invokeVirtual;
        }
        switch (classType) {
            case ANNOTATION_CLASS -> {
                if (jmn.isStatic() || !jmn.isAbstract() || !jmn.getDesc().startsWith("()")) {
                    // "method %s in %s class must be %s, not %s and have no parameters"
                    LOG(M406, lmh.ond(), classType,AccessFlag.acc_abstract, AccessFlag.acc_static);
                }
            }
        }
        ObjectLine<HandleType> objline = new ObjectLine<>(ht,jmn.getLine());
        ObjectLine<HandleType> previous = ownMethods.put(lmh, objline);
        if (previous != null) {
            if (previous.line() == Line.EMPTY) {
                // "%s cannot be overridden"            
                LOG(M262,lmh.ond());
            } else {
                // "duplicate %s: %s already defined at line %d"            
                LOG(M40, Directive.dir_method, lmh.ond(), previous.line().getLinect());
            }
        }
        if (classAccess.is(acc_final) && jmn.isAbstract()) {
            LOG(M59, name);  // "method %s cannot be abstract in final class"
        }
        LocalMethodHandle sermd = SERIAL_METHODS.get(name);
        if (sermd != null) {
            if (sermd.equals(lmh)) {
                if (!jmn.isPrivate()) {
                    LOG(M207, name); // "possible serialization method %s is not private"
                }
            } else if (jmn.isPrivate()) {
                LOG(M227, lmh.ond(), sermd.ond()); // "possible serialization method %s does not match %s"
            }
        }
   }    

    public void endMethod(JynxMethodNode jmn) {
        if (jmn.isInit()) {
            int standard = Constants.isObjectClass(className)? 0: 1;
            int net = specialct - newct;
            if (net < standard) {
                // "init method does not contain this or super: %s = %d %s = %d"
                LOG(M102,HandleType.REF_newInvokeSpecial,specialct, JvmOp.asm_new,newct);
            } else if (net > standard) {
                // "init method contains %d %s and %d %s for net of %d"
                LOG(M103,specialct,HandleType.REF_newInvokeSpecial,newct,JvmOp.asm_new,net);
            }
        }
    }
    
    private JynxComponentNode getComponent4Method(String mname, String mdesc) {
        JynxComponentNode jcn = components.get(mname);
        if (jcn != null && jcn.getLocalMethodHandle().desc().equals(mdesc)) {
            return jcn;
        }
        return null;
    }
    
    public boolean isComponent(Context context, String name, String desc) {
        if (context == Context.METHOD) {
            JynxComponentNode jcn = getComponent4Method(name, desc);
            return jcn != null;
        } else if (context == Context.FIELD) {
            return components.containsKey(name);
        }
        return false;
    }
    
    private JynxComponentNode getComponent4Field(String mname) {
        return components.get(mname);
    }
    
    public void checkField(JynxFieldNode jfn) {
        String name = jfn.getName();
        JynxFieldNode previous = fields.put(name,jfn);
        if (previous != null) {
            // "duplicate %s: %s %s already defined at line %d"
            LOG(M55,Directive.dir_field,jfn.getName(),"",previous.getLine().getLinect());
        }
        if (!jfn.isStatic()) {
            ++instanceFieldCt;
            if (classType == RECORD) {
                JynxComponentNode jcn = components.get(name);
                if (jcn != null && !jcn.getDesc().equals(jfn.getDesc())) {
                    // "component %s description %s differs from field description %s"
                    LOG(M269,name,jcn.getDesc(),jfn.getDesc());
                }
                ++fieldComponentCt;
            }
        }
    }

    public void usedField(FieldHandle fh) {
        if (fh.owner().equals(className)) {
            HandleType ht = fh.ht();
            boolean instance = ht == REF_getField || ht == REF_putField;
            JynxFieldNode jfn = fields.get(fh.name());
            if (jfn == null || !fh.desc().equals(jfn.getDesc())) {
                if (Constants.isObjectClass(superClassName) && !hasImplements) {
                    // "field %s %s does not exist"
                    LOG(M199,fh.name(), fh.desc());
                } else {
                    // "field %s %s does not exist in this class but may exist in superclass/superinterface"
                    LOG(M214,fh.name(), fh.desc());
                }
            } else if (jfn.isStatic() == instance) {
                String fieldtype = jfn.isStatic()?"static":"instance";
                String optype = instance?"instance":"static";
                LOG(M215,fieldtype,fh.name(),optype,ht.opcode().name().toLowerCase()); // " %s field %s accessed by %s op %s"
            }
        } else if (OPTION(GlobalOption.CHECK_REFERENCES)) {
            fh.checkReference();
        }
    }
    
    public void checkSignature4Method(String signature, String name, String desc) {
        JynxComponentNode jcn = getComponent4Method(name,desc);
        jcn.checkSignature(signature, METHOD);
    }
    
    public void checkSignature4Field(String signature, String name) {
        JynxComponentNode jcn = getComponent4Field(name);
        jcn.checkSignature(signature, FIELD);
    }
    
    private void mustHaveVirtualMethod(LocalMethodHandle lmh) {
        ObjectLine<HandleType> objline = ownMethods.get(lmh);
        if (objline == null || objline.object() != REF_invokeVirtual) {
            // "%s must have a %s method of type %s"
            LOG(M132,classType, lmh.ond(),REF_invokeVirtual);
        }
    }
    
    private void shouldHaveVirtualMethod(LocalMethodHandle has, LocalMethodHandle should) {
        ObjectLine<HandleType> objline = ownMethods.get(should);
        if (objline == null || objline.object() != REF_invokeVirtual) {
            // "as class has a %s method it should have a %s method"
            LOG(M153,has.ond(),should.ond());
        }
    }

    private boolean isMethodDefined(MethodHandle mh, HandleType ht) {
        LocalMethodHandle lmh = LocalMethodHandle.getInstance(mh.name()+mh.desc());
        ObjectLine<HandleType> objline = ownMethods.get(lmh);
        return objline != null && objline.object() == ht;
    }

    private void visitRecordEnd() {
        if (fieldComponentCt != components.size()) {
            // "number of Record components %d disagrees with number of instance fields %d"
            LOG(M48,components.size(),fieldComponentCt);
        }
        for (JynxComponentNode jcn : components.values()) {
            LocalMethodHandle lmh = jcn.getLocalMethodHandle();
            mustHaveVirtualMethod(lmh);
        }
        mustHaveVirtualMethod(TOSTRING_METHOD);
        mustHaveVirtualMethod(HASHCODE_METHOD);
        mustHaveVirtualMethod(EQUALS_METHOD);
    }
    
    private void visitClassEnd() {
        boolean init = ownMethods.keySet().stream()
            .filter(LocalMethodHandle::isInit)
            .findFirst()
            .isPresent();
        long instanceMethodCoumt = ownMethods.values().stream()
            .filter(ol-> ol.object() == REF_invokeVirtual)
            .filter(ol->ol.line() != Line.EMPTY)
            .count();
        if (!init && (instanceFieldCt != 0 || instanceMethodCoumt != 0)) {
            LOG(M156,NameDesc.CLASS_INIT_NAME); // "instance variables or methods with no %s method"
        }
        boolean equals = ownMethods.keySet().stream()
            .filter(ond -> ond.equals(EQUALS_METHOD))
            .findFirst()
            .isPresent();
        if (equals) {
            shouldHaveVirtualMethod(EQUALS_METHOD, HASHCODE_METHOD);
        }
        Optional<LocalMethodHandle> xequals = ownMethods.entrySet().stream()
            .filter(me -> me.getValue().object() == REF_invokeVirtual)
            .map(me -> me.getKey())
            .filter(lmh -> lmh.name().equals(EQUALS_METHOD.name()))
            .findFirst();
        if (xequals.isPresent() && !equals) {
            //"%s does not override object equals method in %s"
            LOG(M239,xequals.get().ond(),className);
        }
        ownMethodsUsed.entrySet().stream()
                .filter(me -> me.getValue().object() == REF_newInvokeSpecial)
                .forEach(me->{
                    if (!isMethodDefined(me.getKey(),REF_newInvokeSpecial)) {
                         // "own init method %s not found"
                        LOG(me.getValue().line().toString(), M252, me.getKey().name());
                    }
                });
        checkMissing(REF_invokeVirtual);
    }

    private void checkMissing(HandleType ht) {
        ObjectLine<HandleType> virtual = new ObjectLine<>(ht, Line.EMPTY); 
        ownMethods.putIfAbsent(EQUALS_METHOD, virtual);
        ownMethods.putIfAbsent(HASHCODE_METHOD, virtual);
        ownMethods.putIfAbsent(TOSTRING_METHOD, virtual);
        String[] missing = ownMethodsUsed.entrySet().stream()
                .filter(me -> me.getValue().object() == ht)
                .map(me-> me.getKey())
                .filter(k->!isMethodDefined(k,ht))
                .map(k->k.name())
                .toArray(String[]::new);
        if (missing.length != 0) {
            // "the following own virtual method(s) are used but not found in class (but may be in super class or interface)%n    %s"
           LOG(M250,Arrays.asList(missing));
        }
    }
    
    public void visitEnd() {
        switch (classType) {
            case RECORD -> {
                visitRecordEnd();
                visitClassEnd();
            }
            case ENUM, BASIC, VALUE_CLASS -> visitClassEnd();
            case INTERFACE -> checkMissing(REF_invokeInterface);
            case ANNOTATION_CLASS -> {
            }
            case MODULE_CLASS, PACKAGE -> {
                assert ownMethods.isEmpty() && ownMethodsUsed.isEmpty();
            }
            default -> throw new EnumConstantNotPresentException(classType.getClass(), classType.name());
        }
        ownMethodsUsed.entrySet().stream()
                .filter(me -> me.getValue().object() == REF_invokeStatic)
                .forEach(me->{
                    if (!isMethodDefined(me.getKey(), REF_invokeStatic)) {
                         // "own static method %s not found (but may be in super class)"
                        LOG(me.getValue().line().toString(), M251, me.getKey().name());
                    }
                });
        if (ownMethods.containsKey(FINALIZE_METHOD) && classType != ClassType.ENUM ) {
            jvmVersion.checkSupports(Feature.finalize);
        }
    }
    
}
