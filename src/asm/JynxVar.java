package asm;

import java.util.Optional;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static com.github.david32768.jynxfor.my.Message.M217;

import static com.github.david32768.jynxfree.jvm.StandardAttribute.LocalVariableTypeTable;
import static com.github.david32768.jynxfree.jvm.StandardAttribute.Signature;
import static com.github.david32768.jynxfree.jynx.Global.LOG;
import static com.github.david32768.jynxfree.jynx.ReservedWord.res_from;
import static com.github.david32768.jynxfree.jynx.ReservedWord.res_is;
import static com.github.david32768.jynxfree.jynx.ReservedWord.res_signature;
import static com.github.david32768.jynxfree.jynx.ReservedWord.res_to;

import com.github.david32768.jynxfree.jynx.Global;
import com.github.david32768.jynxfree.jynx.NameDesc;

import jynx2asm.JynxLabel;
import jynx2asm.JynxLabelMap;
import jynx2asm.Line;
import jynx2asm.Token;

public class JynxVar {
    
    private final int varnum;
    private final String name;
    private final String desc;
    private final String signature;
    private final JynxLabel fromref;
    private final JynxLabel toref;
    private final Line line;

    private JynxVar(int varnum, String name, String desc, String signature, JynxLabel fromref, JynxLabel toref, Line line) {
        this.varnum = varnum;
        this.name = name;
        this.desc = desc;
        this.signature = signature;
        this.fromref = fromref;
        this.toref = toref;
        this.line = line;
    }

    public int varnum() {
        return varnum;
    }

    public String desc() {
        return desc;
    }

    public Line getLine() {
        return line;
    }
    
    public static JynxVar getInstance(Line line, JynxLabelMap labelmap) {
        int varnum = line.nextToken().asUnsignedShort();
        String name = line.after(res_is);
        String desc = line.nextToken().asString();
        Optional<String> vsignature = line.optAfter(res_signature);
        JynxLabel fromref = labelmap.startLabel();
        JynxLabel toref = labelmap.endLabel();
        Token token = line.peekToken();
        if (!token.isEndToken()) {
            String fromname = line.after(res_from);
            fromref = labelmap.useOfJynxLabel(fromname, line);
            String toname = line.after(res_to);
            toref = labelmap.useOfJynxLabel(toname, line);
        } else {
            line.nextToken();
        }
        line.noMoreTokens();
        vsignature.ifPresent(sig -> {
                Global.CHECK_SUPPORTS(LocalVariableTypeTable);
                Global.CHECK_SUPPORTS(Signature);
                NameDesc.FIELD_SIGNATURE.validate(sig);
            });
        return new JynxVar(varnum, name, desc, vsignature.orElse(null), fromref, toref, line);
    }
    
    public static JynxVar getIntance(int varnum, String name, String desc) {
        return new JynxVar(varnum, name, desc, null, null, null, Line.GENERATED);
    }
    
    public void accept(MethodVisitor mv, JynxLabelMap labelmap) {
        JynxLabel jfrom = fromref == null? labelmap.startLabel(): fromref;
        JynxLabel jto = toref == null? labelmap.endLabel(): toref;
        if (jfrom.isLessThan(jto)) {
            Label from = jfrom.asmlabel();
            Label to = jto.asmlabel();
            mv.visitLocalVariable(name, desc, signature, from, to, varnum);
        } else {
            //"from label %s is not before to label %s"
            LOG(line.toString(), M217, fromref.name(), toref.name());
        }
    }
}
