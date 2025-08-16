package jynx2asm;

import org.objectweb.asm.MethodVisitor;

import static com.github.david32768.jynxfor.my.Message.M203;
import static com.github.david32768.jynxfor.my.Message.M279;
import static com.github.david32768.jynxfree.jynx.Global.LOG;

public class JynxCatch {

    private final JynxLabel fromLab;
    private final JynxLabel toLab;
    private final JynxLabel usingLab;
    private final String exception;
    private final Line line;

    JynxCatch(JynxLabel fromLab, JynxLabel toLab, JynxLabel usingLab, String exception, Line line) {
        this.fromLab = fromLab;
        this.toLab = toLab;
        this.usingLab = usingLab;
        if (fromLab.equals(usingLab)) {
                LOG(M203); // "potential infinite loop - catch using label equals catch from label"
        }
        if (fromLab.equals(toLab)) {
                LOG(M279); // "empty catch block - from label equals to label"
        }
        this.exception = exception;
        this.line = line;
    }

    public JynxLabel fromLab() {
        return fromLab;
    }

    public JynxLabel toLab() {
        return toLab;
    }

    public JynxLabel usingLab() {
        return usingLab;
    }

    public void accept(MethodVisitor mv) {
        mv.visitTryCatchBlock(fromLab.asmlabel(), toLab.asmlabel(), usingLab.asmlabel(), exception);
    }
    
    @Override
    public String toString() {
        return String.format("%s %s %s %s", fromLab, toLab,usingLab,exception);
    }
}
