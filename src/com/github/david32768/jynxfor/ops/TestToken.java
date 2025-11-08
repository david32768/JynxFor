package com.github.david32768.jynxfor.ops;

import static com.github.david32768.jynxfor.my.Message.M321;
import static com.github.david32768.jynxfor.my.Message.M408;
import static com.github.david32768.jynxfor.my.Message.M412;

import static com.github.david32768.jynxfree.jynx.Global.LOG;

import com.github.david32768.jynxfor.scan.Line;
import com.github.david32768.jynxfor.scan.Token;

import com.github.david32768.jynxfree.jynx.JynxMessage;
import com.github.david32768.jynxfree.jynx.LogIllegalStateException;

import jynx2asm.LabelStack;

public class TestToken implements LineOp {
    
    private static enum TestType {
       CHECK(M408), // "expected %s but found %s"
       CHECKNOT(M412), // "%s not supported"
       TEST_UINT_MAX(M321), // "%d does not in [0,%d]"
       ;
       
       private final JynxMessage msg;

        private TestType(JynxMessage msg) {
            this.msg = msg;
        }
       
    }
   
    private final TestType type;
    private final Object aux;

    private TestToken(TestType type, Object aux) {
        this.type = type;
        this.aux = aux;
    }

    @Override
    public void adjustLine(Line line, int macrolevel, MacroOp macroop, LabelStack labelStack){
        Token token;
        switch(type) {
            case CHECK -> {
                token = line.nextToken();
                if (!token.asString().equals(aux)) {
                    throw new LogIllegalStateException(type.msg,aux,token.asString());
                }
            }
            case CHECKNOT -> {
                token = line.peekToken();
                if (!token.isEndToken() && token.asString().equals(aux)) {
                    throw new LogIllegalStateException(type.msg,aux);
                }
            }
            case TEST_UINT_MAX -> {
                token = line.peekToken();
                long uint = token.asUnsignedInt();
                if (uint > (Long)aux) {
                    LOG(type.msg,uint,aux);
                }
            }
            default -> throw new EnumConstantNotPresentException(type.getClass(), type.name());
        }
    }

    @Override
    public String toString() {
        return String.format("*%s %s", type, aux);
    }

    public static LineOp check(String str) {
        return new TestToken(TestToken.TestType.CHECK,str);
    }

    public static LineOp checkNot(String str) {
        return new TestToken(TestToken.TestType.CHECKNOT,str);
    }

    public static LineOp testUIntMax(long max) {
        return new TestToken(TestType.TEST_UINT_MAX, max);
    }

}
