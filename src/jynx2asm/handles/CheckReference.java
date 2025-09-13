package jynx2asm.handles;

import java.lang.invoke.MethodType;
import java.lang.reflect.AccessibleObject;

import static com.github.david32768.jynxfor.my.JynxGlobal.CLASS_NAME;
import static com.github.david32768.jynxfor.my.Message.M319;
import static com.github.david32768.jynxfor.my.Message.M400;
import static com.github.david32768.jynxfor.my.Message.M407;

import static com.github.david32768.jynxfree.jynx.Global.LOG;

import com.github.david32768.jynxfree.jvm.Context;
import com.github.david32768.jynxfree.jvm.HandleType;

public class CheckReference {

    private final String ondstr;
    private final String owner;
    private final String name;
    private final String desc;
    private final HandleType ht;
    private final Context context;
    private final boolean isInterface;

    public CheckReference(JynxHandle jh) {
        this.ondstr = jh.ond();
        this.owner = jh.owner();
        this.name = jh.name();
        this.ht = jh.ht();
        if (ht.isField()) {
            this.desc = "()" + jh.desc();
            this.context = Context.FIELD;
        } else {
            this.desc = jh.desc();
            this.context = Context.METHOD;
        }
        this.isInterface = jh.isInterface();
    }

    private void checkDeprecated(AccessibleObject mfc) {
        boolean has = mfc.isAnnotationPresent(Deprecated.class);
        if (has) {
            LOG(M407,context,ondstr); // "%s %s is deprecated"
        }
    }
    
    void check() {
        try {
            MethodType mt = MethodType.fromMethodDescriptorString(desc, null);
            Class<?> klass = Class.forName(owner.replace('/', '.'),false,
                    ClassLoader.getSystemClassLoader());
            Class<?>[] parms = mt.parameterArray();
            AccessibleObject mfc;
            mfc = switch (ht) {
                case REF_invokeStatic, REF_invokeSpecial, REF_invokeInterface -> {
                    if (isInterface != klass.isInterface()) {
                        // "%s is an interface and so '%c' must be prepended to %s"
                        LOG(M319,owner,HandlePart.INTERFACE_PREFIX,ondstr);
                    }
                    yield klass.getMethod(name, parms);
                }
                case REF_invokeVirtual -> klass.getMethod(name, parms);
                case REF_newInvokeSpecial -> klass.getConstructor(parms);
                case REF_getStatic, REF_putStatic, REF_getField, REF_putField -> klass.getField(name);
            };
            checkDeprecated(mfc);
        } catch (ClassNotFoundException
                | IllegalArgumentException
                | NoSuchFieldException
                | NoSuchMethodException ex) {
             // "unable to find %s %s because of %s"
            LOG(M400,context,ondstr,ex.getClass().getSimpleName());
        } catch (SecurityException iaex) {
            if (ht == HandleType.REF_invokeSpecial) { // maybe protected
                return;
            } else if (HandlePart.isSamePackage(CLASS_NAME(),owner)) { // maybe package-private
                return;
            }
             // "unable to find %s %s because of %s"
            LOG(M400,context,ondstr,iaex.getClass().getSimpleName());
        } catch (TypeNotPresentException typex) {
            String typename = typex.typeName().replace(".","/");
            if (typename.equals(CLASS_NAME()))  {
                return;
            }
            String cause = typex.getClass().getSimpleName() + " " + typename;
             // "unable to find %s %s because of %s"
            LOG(M400,context,ondstr,cause);
        }
    }
    
}