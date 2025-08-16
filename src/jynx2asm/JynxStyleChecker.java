package jynx2asm;

import java.util.Optional;

import org.objectweb.asm.Type;
import org.objectweb.asm.util.CheckClassAdapter;

import static com.github.david32768.jynxfree.jynx.Style.*;

import static com.github.david32768.jynxfor.my.Message.M158;
import static com.github.david32768.jynxfor.my.Message.M236;
import static com.github.david32768.jynxfor.my.Message.M258;
import static com.github.david32768.jynxfor.my.Message.M401;
import static com.github.david32768.jynxfor.my.Message.M93;

import static com.github.david32768.jynxfree.jynx.Global.LOG;
import static com.github.david32768.jynxfree.jynx.Global.SUPPORTS;

import com.github.david32768.jynxfree.jvm.JavaReserved;
import com.github.david32768.jynxfree.jynx.Global;
import com.github.david32768.jynxfree.jynx.GlobalOption;
import com.github.david32768.jynxfree.jynx.NameDesc;
import com.github.david32768.jynxfree.jynx.StyleChecker;

public class JynxStyleChecker implements StyleChecker {
    
    @Override
    public void checkNotJavaReserved(String str) {
        Optional<JavaReserved> javaid = JavaReserved.of(str);
        if (javaid.isPresent()) {
            JavaReserved jres = javaid.get();
            if (jres.isContextual()) {
                LOG(M401,str); // "%s is a contextual reserved word"
            } else if (SUPPORTS(jres.feature())) {
                LOG(M258,str); // "%s is a reserved word and cannot be a Java Id"
            }
        }
    }
    
    @Override
    public void checkClassStyle(String str) {
        int index = str.lastIndexOf('/');
        if (index >= 0) {
            checkPackageStyle(str.substring(0,index));
        }
        String klass = str.substring(index + 1);
        checkNotJavaReserved(klass);
        if (Global.OPTION(GlobalOption.WARN_STYLE)) {
            int ch = klass.codePointAt(0);
            if (!Character.isUpperCase(ch)) {
                String classname = str.substring(index + 1);
                LOG(M93,classname); // "class name (%s) does not start with uppercase letter"
            }
        }
    }
    
    private boolean packageChar(int codepoint) {
        return Character.isLowerCase(codepoint)
                || Character.isDigit(codepoint)
                || codepoint == '/';
    }
    
    @Override
    public void checkPackageStyle(String str) {
        String[] components = str.split("/");
        for (String component:components) {
            checkNotJavaReserved(component);
        }
        if (NameDesc.isJava(str) || Global.OPTION(GlobalOption.WARN_STYLE)) {
            if (!str.codePoints().allMatch(this::packageChar)) {
                LOG(M158,str); // "components of package %s are not all lowercase"
            }
        }
    }

    @Override
    public void checkJavaMethodNameStyle(String str) {
        int first = str.codePointAt(0);
        if (Character.isUpperCase(first) && !str.equalsIgnoreCase(str)) {
            LOG(M236,METHOD_NAME,str); // "%s (%s) starts with uppercase letter and is not all uppercase"
        }
    }
    
    @Override
    public void checkMethodNameStyle(String str) {
        checkNotJavaReserved(str);
        if (Global.OPTION(GlobalOption.WARN_STYLE)) {
            checkJavaMethodNameStyle(str);
        }
    }
    
    @Override
    public void checkTypeStyle(String str) {
        Type type = Type.getType(str);
        checkType(type);
    }
    
    @Override
    public void checkFieldNameStyle(String str) {
        checkNotJavaReserved(str);
        if (Global.OPTION(GlobalOption.WARN_STYLE)) {
            int first = str.codePointAt(0);
            if (Character.isUpperCase(first) && !str.equalsIgnoreCase(str)) {
                LOG(M236,FIELD_NAME,str); // "%s (%s) starts with uppercase letter and is not all uppercase"
            }
        }
    }

    private void checkType(Type type) {
        if (type.getSort() == Type.ARRAY) {
            type = type.getElementType();
        }
        if (type.getSort() == Type.OBJECT) {
            checkClassStyle(type.getInternalName());
        }
    }
    
    @Override
    public void checkDescStyle(String str) {
        Type mnd = Type.getMethodType(str);
        Type[] parmt = mnd.getArgumentTypes();
        for (Type type:parmt) {
            checkType(type);
        }
        Type rt = mnd.getReturnType();
        checkType(rt);
    }

    @Override
    public void checkClassSignature(String str) {
        CheckClassAdapter.checkClassSignature(str);
    }

    @Override
    public void checkMethodSignature(String str) {
        CheckClassAdapter.checkMethodSignature(str);
    }

    @Override
    public void checkFieldSignature(String str) {
        CheckClassAdapter.checkFieldSignature(str);
    }
}
