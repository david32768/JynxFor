package com.github.david32768.jynxfor.ops;

import java.util.function.Predicate;

import static com.github.david32768.jynxfor.my.Message.M356;
import static com.github.david32768.jynxfor.my.Message.M408;
import static com.github.david32768.jynxfor.my.Message.M412;
import static com.github.david32768.jynxfor.my.Message.M418;

import com.github.david32768.jynxfor.my.Message;
import com.github.david32768.jynxfor.scan.Token;

import com.github.david32768.jynxfree.jynx.LogIllegalArgumentException;
import com.github.david32768.jynxfree.jynx.NameDesc;

public class CheckToken implements LineOp {
    
    private final Predicate<Token> pred;
    private final Message msg;
    private final Object check;

    public CheckToken(Predicate<Token> pred, Message msg, Object check) {
        this.pred = pred;
        this.msg = msg;
        this.check = check;
    }

    @Override
    public void adjustLine(CurrentState state) {
        var token = state.line().peekToken();
        if (!pred.test(token)) {
            throw new LogIllegalArgumentException(msg, check, token);
        }
    }
    
    public static CheckToken checkND(NameDesc nd) {
        // "check token failure: %s"
        return new CheckToken(tok -> nd.validate(tok.asName()), M356, nd.name());
    }
    
    public static CheckToken checkIs(String required) {
        // "expected %s but found %s"
        return new CheckToken(tok -> tok.asString().equals(required), M408, required);
    }

    public static CheckToken checkNot(String mustnot) {
        // "%s not supported"
        return new CheckToken(tok -> !tok.asString().equals(mustnot), M412, mustnot);
    }

    public static CheckToken checkRange(int start, int end) {
        String msg = String.format("[%d, %d)", start, end);
        // "token not in range %s: token = %d"
        return new CheckToken(tok -> tok.asLong() >= start && tok.asLong() < end, M418, msg);
    }
}
