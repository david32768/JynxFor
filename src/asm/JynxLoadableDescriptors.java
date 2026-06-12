package asm;

import java.util.List;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ByteVector;
import org.objectweb.asm.ClassWriter;

public class JynxLoadableDescriptors extends Attribute {
    
    private final List<String> classes;
    
    public JynxLoadableDescriptors(List<String> classes) {
        super("LoadableDescriptors");
        this.classes = classes;
    }

    @Override
    protected ByteVector write(ClassWriter classWriter, byte[] code, int codeLength, int maxStack, int maxLocals) {
        assert code == null;
        ByteVector result = new ByteVector();
        result.putShort(classes.size());
        for (String klass : classes) {
            int cpi = classWriter.newUTF8(klass);
            result.putShort(cpi);
        }
        return result;
    }
    
}
