package asm;

import org.objectweb.asm.ClassTooLargeException;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodTooLargeException;

import static com.github.david32768.jynxfree.jynx.Global.*;

import static com.github.david32768.jynxfor.my.Message.M357;
import static com.github.david32768.jynxfree.jvm.StandardAttribute.StackMapTable;

import com.github.david32768.jynxfor.verify.Resolver;
import com.github.david32768.jynxfree.jynx.Access;

public class ASMClassNode extends JynxClassNode {

    private final ClassWriter cw;

    private ASMClassNode(Access accessname, ClassWriter cw) {
        super(accessname, cw);
        this.cw = cw;
    }
    
    public static ASMClassNode getInstance(Access accessname, boolean usestack, Resolver resolver) {
        int cwflags = 0;
        if (!usestack && accessname.jvmVersion().supports(StackMapTable)) {
            cwflags = ClassWriter.COMPUTE_FRAMES;
        }
        JynxClassWriter cw = new JynxClassWriter(cwflags, resolver);
        return new ASMClassNode(accessname, cw);
    }

    @Override
    public byte[] toByteArray() {
        byte[] ba = null;
        try {
            ba = cw.toByteArray();
        } catch (ClassTooLargeException ex) {
            // "class has more than 65535 constant pool entries"
            LOG(M357);
            LOG(ex);
        } catch (MethodTooLargeException ex) {
            LOG(ex);
        }
        return ba;
    }
    
}
