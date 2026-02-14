package com.github.david32768.jynxfor.code;

import static com.github.david32768.jynxfree.jynx.Global.OPTION;

import com.github.david32768.jynxfor.ops.JvmOp;
import com.github.david32768.jynxfor.scan.Token;
import com.github.david32768.jynxfree.jynx.GlobalOption;

import jynx2asm.frame.MethodParameters;

public interface VarTranslator {

    public default int local(JvmOp jvmop, Token vartoken) {
        return vartoken.asShort();
    }
    
    public default int local(Token vartoken) {
        return vartoken.asShort();
    }
    
    public default void addVars(JynxCodeNodeBuilder codebuilder) {}
    
    public static VarTranslator of(MethodParameters parameters) {
        return OPTION(GlobalOption.SYMBOLIC_LOCAL)?
                SymbolicVarTranslator.getInstance(parameters):
                new VarTranslator(){};        
    }
}
