package jynx2asm.handles;

import org.objectweb.asm.Handle;

import static com.github.david32768.jynxfor.my.Message.M341;
import static com.github.david32768.jynxfor.my.Message.M99;
import static com.github.david32768.jynxfree.jynx.Global.LOG;

import com.github.david32768.jynxfor.ops.JvmOp;
import com.github.david32768.jynxfree.jvm.HandleType;
import com.github.david32768.jynxfree.jynx.LogIllegalArgumentException;

public interface JynxHandle {
    
    public String desc();

    public default String returnDesc() {
        String desc = desc();
        int index = desc.lastIndexOf(')');
        return desc.substring(index + 1);
    }
    
    public default HandleType ht() {
        throw new AssertionError();
    }

    public String name();

    public default String owner() {
        throw new AssertionError();
    }

    public default String ownerL() {
        String owner = owner();
        return owner.charAt(0) == '['?owner:"L" + owner + ";";
    }
    
    public default boolean isInterface() {
        return false;
    }

    public default Handle handle() {
        return new Handle(ht().reftype(),owner(),name(),desc(),isInterface());
    }
    
    public String ond();

    public default String iond() {
        if (isInterface()) {
            return HandlePart.INTERFACE_PREFIX + ond();
        }
        return ond();
    }
    
    public static JynxHandle of(Handle handle) {
        HandleType ht = HandleType.getInstance(handle.getTag());
        if (ht.isField()) {
            return FieldHandle.of(handle);
        } else {
            return MethodHandle.of(handle);
        }
    }
    
    public static Handle getHandle(String token) {
        int colon = token.indexOf(HandleType.SEP);
        if (colon < 0) {
            // "Separator '%s' not found in %s"
            throw new LogIllegalArgumentException(M99,HandleType.SEP,token);
        }
        String htag = token.substring(0,colon);
        HandleType ht = HandleType.fromMnemonic(htag);
        String handle = token.substring(colon + 1);
        if (ht.isField()) {
            return FieldHandle.getInstance(handle, ht).handle();
        } else {
            return MethodHandle.getInstance(handle, ht).handle();
        }
    }

    public static JvmOp getReturnOp(LocalMethodHandle lmh) {
        String rtdesc = lmh.returnDesc();
        char rtchar = rtdesc.charAt(0);
        return switch (rtchar) {
            case 'V' -> JvmOp.asm_return;
            case 'Z', 'B', 'C', 'S', 'I' -> JvmOp.asm_ireturn;
            case 'F' -> JvmOp.asm_freturn;
            case 'D' -> JvmOp.asm_dreturn;
            case 'J' -> JvmOp.asm_lreturn;
            case 'L', '[' -> JvmOp.asm_areturn;
            default -> {
                //    M341("invalid return type %s"),
                LOG(M341, rtdesc);    
                yield JvmOp.asm_areturn;
            }
        };
    }
    
}
