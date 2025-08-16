package asm;

import java.util.Optional;

import org.objectweb.asm.ClassWriter;

import static com.github.david32768.jynxfor.my.Message.M58;
import static com.github.david32768.jynxfree.jynx.Global.LOG;

import com.github.david32768.jynxfor.verify.Resolver;

public class JynxClassWriter extends ClassWriter {

    private final Resolver resolver;
    
    public JynxClassWriter(int cwflags, Resolver resolver) {
        super(cwflags);
        this.resolver = resolver;
    }

    @Override
    protected String getCommonSuperClass(final String type1, final String type2) {
        Optional<String> common = resolver.getCommonSuperClass(type1, type2);
        if (common.isEmpty()) {
            return super.getCommonSuperClass(type1, type2);
        } else {
            // "used hint: %s is common supertype of %s and %s"
            LOG(M58, common, type1, type2);
            return common.orElseThrow();
        }
    }
}
