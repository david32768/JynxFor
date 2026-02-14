package com.github.david32768.jynxfor.ops;

import com.github.david32768.jynxfor.code.VarIndexTranslator;
import com.github.david32768.jynxfor.code.VarTranslator;
import com.github.david32768.jynxfor.scan.Line;
import com.github.david32768.jynxfor.scan.Token;

import jynx2asm.FrameElement;
import jynx2asm.StackLocals;

public class CurrentState {

    private final Line line;
    private final StackLocals stackLocals;
    private final VarTranslator varTrans;
    private final VarIndexTranslator vtmap;

    public CurrentState(Line line, StackLocals stackLocals, VarTranslator varTrans, VarIndexTranslator vtmap) {
        this.line = line;
        this.stackLocals = stackLocals;
        this.varTrans = varTrans;
        this.vtmap = vtmap;
    }
    
    Line line() {
        return line;
    }
    
    FrameElement peekTOS() {
        return stackLocals.stack().peekTOS();
    }
    
    FrameElement peekVarNum(Token token) {
        int num = varTrans.local(token);
        return stackLocals.locals().peekVarNumber(num);
    }
    
    JvmOp getReturnOp() {
        return stackLocals.getReturnOp();
    }
    
    String indexOf(char type, String str) {
        return Integer.toString(vtmap.indexOf(type, str));
    }
}
