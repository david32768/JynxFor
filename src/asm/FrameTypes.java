package asm;

import java.util.ArrayList;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import static com.github.david32768.jynxfor.my.Message.M901;
import static com.github.david32768.jynxfor.my.Message.M903;

import com.github.david32768.jynxfree.jvm.FrameType;
import com.github.david32768.jynxfree.jynx.LogAssertionError;

import jynx2asm.handles.LocalMethodHandle;

public class FrameTypes {

    private FrameTypes(){}
    
    private static Object objectFrom(String tdesc) {
        return switch(tdesc.charAt(0)) {
            case 'Z', 'B', 'S', 'C', 'I' -> FrameType.ft_Integer.asmType();
            case 'J' -> FrameType.ft_Long.asmType();
            case 'F' -> FrameType.ft_Float.asmType();
            case 'D' -> FrameType.ft_Double.asmType();
            case 'L' -> {
                tdesc = tdesc.substring(1,tdesc.length() - 1);
                yield tdesc;
            }
            case '[' -> tdesc;
            default -> throw new LogAssertionError(M901,tdesc,tdesc.charAt(0)); // "unknown ASM type %s as it starts with '%c'"
        };
    }

    public static FrameType fromObject(Object obj) {
        return switch(obj) {
            case Integer i -> FrameType.fromAsmType(i);
            case String _ -> FrameType.ft_Object;
            case Label _ -> FrameType.ft_Uninitialized;
            // "unknown class %s for ASM frametype"
            default -> throw new LogAssertionError(M903,obj.getClass());
        };
    }
    
    // set classname == null for static method
    public static ArrayList<Object> getInitFrame(String classname, boolean isstatic, LocalMethodHandle lmh) {
        ArrayList<Object> localStack = new ArrayList<>();
        if (!isstatic) {
            if (lmh.isInit()) {
                localStack.add(FrameType.ft_Uninitialized_This.asmType());
            } else {
                localStack.add(classname);
            }
        }
        Type[] parmtypes = Type.getArgumentTypes(lmh.desc());
        for (Type type:parmtypes) {
            String tdesc = type.getDescriptor();
            localStack.add(objectFrom(tdesc));
        }
        return localStack;
    }
    
}
