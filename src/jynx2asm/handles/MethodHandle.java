package jynx2asm.handles;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Objects;

import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.MethodInsnNode;

import static com.github.david32768.jynxfor.my.Message.M135;
import static com.github.david32768.jynxfor.my.Message.M139;
import static com.github.david32768.jynxfor.my.Message.M145;
import static com.github.david32768.jynxfor.my.Message.M242;
import static com.github.david32768.jynxfree.jynx.Global.CHECK_SUPPORTS;
import static com.github.david32768.jynxfree.jynx.Global.CLASS_NAME;
import static com.github.david32768.jynxfree.jynx.Global.LOG;
import static com.github.david32768.jynxfree.jynx.NameDesc.ARRAY_METHOD_NAME_DESC;
import static com.github.david32768.jynxfree.jynx.NameDesc.CLASS_NAME;
import static com.github.david32768.jynxfree.jynx.NameDesc.INTERFACE_METHOD_NAME;
import static com.github.david32768.jynxfree.jynx.NameDesc.INTERFACE_METHOD_NAME_DESC;
import static com.github.david32768.jynxfree.jynx.NameDesc.METHOD_NAME;
import static com.github.david32768.jynxfree.jynx.NameDesc.METHOD_NAME_DESC;

import com.github.david32768.jynxfor.ops.JvmOp;
import com.github.david32768.jynxfree.jvm.Constants;
import com.github.david32768.jynxfree.jvm.Feature;
import com.github.david32768.jynxfree.jvm.HandleType;
import com.github.david32768.jynxfree.jynx.LogIllegalArgumentException;

import static jynx2asm.handles.HandlePart.DESC;
import static jynx2asm.handles.HandlePart.INTERFACE;
import static jynx2asm.handles.HandlePart.NAME;
import static jynx2asm.handles.HandlePart.OWNER;

public class MethodHandle implements JynxHandle, Comparable<MethodHandle> {

    private final String owner;
    private final String name;
    private final String desc;
    private final boolean itf;
    private final HandleType ht;

    public MethodHandle(String owner, String name, String desc, boolean itf, HandleType ht) {
        this.owner = owner;
        this.name = name;
        this.desc = desc;
        this.itf = itf;
        this.ht = ht;
    }

    @Override
    public String owner() {
        return owner;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String desc() {
        return desc;
    }

    @Override
    public boolean isInterface() {
        return itf;
    }

    @Override
    public HandleType ht() {
        return ht;
    }
    
    @Override
    public String ond() {
        return HandlePart.ownerName(owner, name) + desc;
    }

    @Override
    public String toString() {
        return ht.getPrefix() + ond();
    }

    public boolean isInit() {
        return HandlePart.isInit(name, desc);
    } 

    @Override
    public int compareTo(MethodHandle other) {
        int result = owner.compareTo(other.owner);
        if (result == 0) {
            result = name.compareTo(other.name);
        }
        if (result == 0) {
            result = desc.compareTo(other.desc);
        }
        if (result == 0) {
            result = ht.compareTo(other.ht);
        }
        if (result == 0) {
            if (itf && !other.itf) {
                return 1;
            }
            if (!itf && other.itf) {
                return -1;
            }
        }
        return result;
    }
   
    
    public void checkReference() {
        assert !owner.equals(CLASS_NAME());
        (new CheckReference(this)).check();
    }
    
    public static MethodHandle of(Handle handle) {
        HandleType ht = HandleType.getInstance(handle.getTag());
        assert !ht.isField();
        return new MethodHandle(handle.getOwner(),handle.getName(),handle.getDesc(),handle.isInterface(),ht);
    } 

    public static MethodHandle of(MethodInsnNode min) {
        int opcode = min.getOpcode();
        JvmOp op = JvmOp.getOp(opcode);
        HandleType ht = HandleType.fromOp(op.getOpcode(), min.itf);
        return new MethodHandle(min.owner, min.name, min.desc, min.itf, ht);
    }
    
    private static boolean checkInterface(JvmOp op, boolean ownerInterface) {
        return switch(op) {
            case asm_invokeinterface -> {
                if (!ownerInterface){
                    LOG(M135,HandlePart.INTERFACE_PREFIX,op);   // "for consistency add %s prefix to method name for %s"
                }
                yield true;
            }
            case asm_invokevirtual -> {
                if (ownerInterface) {
                    LOG(M139,HandlePart.INTERFACE_PREFIX,op);   // "%s prefix is invalid for %s"
                }
                yield false;
            }
            case asm_invokespecial -> {
                if (ownerInterface) {
                    ownerInterface = CHECK_SUPPORTS(Feature.invokespecial_interface);
                }
                yield ownerInterface;
            }
            case asm_invokestatic -> {
                if (ownerInterface) {
                    ownerInterface = CHECK_SUPPORTS(Feature.invokestatic_interface);
                }
                yield ownerInterface;
            }
            default -> {
                if (ownerInterface) {
                    LOG(M139,HandlePart.INTERFACE_PREFIX,op);   // "%s prefix is invalid for %s"
                }
                yield false;
            }
        };
    }
    
    private static boolean isStaticInit(String name,String desc) {
        return desc.charAt(0) == '(' && Constants.STATIC_INIT.equalsString(name+desc);
    }
    
    private static final char ARRAY_MARKER = '[';


    public static MethodHandle getInstance(String mspec, HandleType ht) {
        return getInstance(mspec,JvmOp.of(ht.opcode()));
    }
    
    public static MethodHandle getInstance(String mspec, JvmOp op) {
        EnumMap<HandlePart,String> map = HandlePart.getInstance(mspec,EnumSet.of(OWNER,NAME,DESC,INTERFACE));
        String owner = map.get(OWNER);
        String name = map.get(NAME);
        String desc = map.get(DESC);
        boolean itf = map.containsKey(INTERFACE);
        itf = checkInterface(op, itf);
        boolean init = HandlePart.isInit(name, desc);
        HandleType ht = HandleType.fromOp(op.getOpcode(), init);
        if (isStaticInit(name,desc) || init && op != JvmOp.asm_invokespecial) {
            // "either init method %s is static or op  is not %s"
            throw new LogIllegalArgumentException(M242,mspec,JvmOp.asm_invokespecial);
        }
        if (owner.charAt(0) == ARRAY_MARKER) {
            if (itf) {
                // "Invalid method description %s"
                throw new LogIllegalArgumentException(M145,mspec);
            }
            ARRAY_METHOD_NAME_DESC.validate(name+desc);
        } else {
            CLASS_NAME.validate(owner);
            if (itf) {
                INTERFACE_METHOD_NAME.validate(name);
                INTERFACE_METHOD_NAME_DESC.validate(name+desc);
            } else {
                METHOD_NAME.validate(name);
                METHOD_NAME_DESC.validate(name+desc);
            }
        }
        return new MethodHandle(owner,name,desc,itf,ht);
    }
    
    private boolean equals(MethodHandle other) {
        return owner.equals(other.owner) && name.equals(other.name) && desc.equals(other.desc)
                && itf == other.itf && ht == other.ht;
    }
    
    @Override
    public boolean equals(Object obj) {
        return obj instanceof MethodHandle && equals((MethodHandle)obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner,name,desc,itf,ht);
    }

}
