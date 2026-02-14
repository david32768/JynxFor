package com.github.david32768.jynxfor.code;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.objectweb.asm.tree.ParameterNode;

import static com.github.david32768.jynxfor.my.Message.M204;
import static com.github.david32768.jynxfor.my.Message.M230;
import static com.github.david32768.jynxfor.my.Message.M337;
import static com.github.david32768.jynxfor.my.Message.M348;
import static com.github.david32768.jynxfree.jynx.Global.LOG;

import com.github.david32768.jynxfor.ops.JvmOp;
import com.github.david32768.jynxfor.scan.Token;
import com.github.david32768.jynxfree.jvm.OpArg;
import com.github.david32768.jynxfree.jynx.GlobalOption;
import com.github.david32768.jynxfree.jynx.LogIllegalArgumentException;
import com.github.david32768.jynxfree.jynx.NameDesc;

import jynx2asm.frame.MethodParameters;
import jynx2asm.frame.StackMapLocals;
import jynx2asm.FrameElement;

public class SymbolicVarTranslator implements VarTranslator {
    
    private final static String THIS = "$this";
    
    private final Map<String,Integer> varmap;
    private final Map<String,FrameElement> typemap;
    private final boolean isVirtual;
    
    private int next;

    private SymbolicVarTranslator(boolean isvirtual) {
        this.varmap = new HashMap<>();
        this.typemap = new HashMap<>();
        this.isVirtual = isvirtual;
        this.next = 0;
    }

   public static SymbolicVarTranslator getInstance(MethodParameters parameters) {
        SymbolicVarTranslator symvt  = new SymbolicVarTranslator(!parameters.isStatic());
        ParameterNode[] parmnodes = parameters.getParameters();
        StackMapLocals smlocals = StackMapLocals.getInstance(parameters.getInitFrame());
        symvt.setParms(smlocals, parmnodes);
        return symvt ;
    }

    private String defaultParmName(int num) {
        if (isVirtual && num == 0) {
            return THIS;
        } else {
            int parmnum = isVirtual? num - 1: num;
            return "$" + parmnum;
        }
    }
    
    private void setParms(StackMapLocals  smlocals, ParameterNode[] parmnodes) {
        assert smlocals.size() == parmnodes.length;
        for (int i = 0; i < smlocals.size(); ++i) {
            FrameElement fe = smlocals.at(i);
            ParameterNode pni = parmnodes[i];
            String name = pni == null?
                defaultParmName(i):
                pni.name;
            newNumber(name, fe);
        }
    }

    @Override
    public int local(JvmOp jvmop, Token token) {
        assert Objects.nonNull(jvmop);
        assert Objects.nonNull(token);
        String tokenstr = token.asString();
        Integer number = varmap.get(tokenstr);
        jvmop.checkArg(OpArg.arg_var, OpArg.arg_incr);
        FrameElement fe = FrameElement.fromLocal(jvmop.vartype());
        if (number == null) {
            number = newNumber(tokenstr, fe);
        } else if (typemap.get(tokenstr) != fe) {
            // "different types for %s; was %s but now %s"
            LOG(M204, token, typemap.get(tokenstr), fe);
        }
        if (isVirtual && number == 0 && !jvmop.isLoadVar()) {
            // "attempting to overwrite %s using %s"
            LOG(M230, THIS, GlobalOption.SYMBOLIC_LOCAL);
        }
        return number;        
    }

    @Override
    public int local(Token token) {
        assert Objects.nonNull(token);
        String tokenstr = token.asString();
        Integer number = varmap.get(tokenstr);
        if (number == null) {
            // "local %s is not known"
            throw new LogIllegalArgumentException(M348, tokenstr);
        }
        return number;
    }

    private int newNumber(String tokenstr, FrameElement fe) {
        if (tokenstr.equals(THIS) && fe != FrameElement.THIS) {
            // "%s is predefined"
            throw new LogIllegalArgumentException(M337, THIS);
        }
        NameDesc.SYMBOLIC_VAR.validate(tokenstr);
        int number = next;
        FrameElement shouldbenull = typemap.putIfAbsent(tokenstr,fe);
        assert shouldbenull == null;
        varmap.put(tokenstr, number);
        next += fe.slots();
        return number;        
    }

    @Override
    public void addVars(JynxCodeNodeBuilder codebuilder) {
        for (var me : varmap.entrySet()) {
            var name = me.getKey();
            int num = me.getValue();
            var fe = typemap.get(name);
            assert fe != null;
            codebuilder.addVar(num, name, fe.desc());
        }
    }

}
