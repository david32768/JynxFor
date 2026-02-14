package com.github.david32768.jynxfor.ops;

import java.util.function.BiFunction;

import static com.github.david32768.jynxfor.my.Message.M354;

import com.github.david32768.jynxfor.my.JynxGlobal;
import com.github.david32768.jynxfor.scan.Line;

import com.github.david32768.jynxfree.jynx.Global;
import com.github.david32768.jynxfree.jynx.LogUnexpectedEnumValueException;
import com.github.david32768.jynxfree.jynx.NameDesc;
import com.github.david32768.jynxfree.jynx.StringUtil;

public class AdjustToken implements LineOp {
    
    private static enum Adjustment {
       INSERT,
       TRANSFORM,
       JOIN,
       ;
    }
   

    private final Adjustment type;
    private final String adjust;
    private final BiFunction<CurrentState,String, String> op;

    private AdjustToken(Adjustment type, String adjust) {
        this.adjust = adjust;
        this.type = type;
        this.op = null;
        assert type == Adjustment.INSERT || type == Adjustment.JOIN;
    }

    private AdjustToken(Adjustment type, BiFunction<CurrentState,String, String> op) {
        this.type = type;
        this.adjust = null;
        this.op = op;
        assert type == Adjustment.TRANSFORM;
    }

    @Override
    public Integer length() {
        return 0;
    }

    @Override
    public void adjustLine(CurrentState state) {
        Line line = state.line();
        String tokenstr = switch(type) {
            case INSERT -> adjust;
            case JOIN -> line.nextToken().asString() + adjust + line.nextToken().asString();
            case TRANSFORM -> op.apply(state, line.nextToken().asString());
            default -> throw new LogUnexpectedEnumValueException(type);
        };
        line.insert(tokenstr);
    }

    @Override
    public String toString() {
        return String.format("*%s %s", type, adjust);
    }

    public static AdjustToken insert(String str) {
        return new AdjustToken(AdjustToken.Adjustment.INSERT, str);
    }
    
    public static AdjustToken join(String str) {
        return new AdjustToken(AdjustToken.Adjustment.JOIN, str);
    }
    
    public static AdjustToken insertMethod(String klass, String method, String desc) {
        NameDesc.CLASS_NAME.validate(klass);
        NameDesc.METHOD_ID.validate(method);
        NameDesc.DESC.validate(desc);
        return new AdjustToken(AdjustToken.Adjustment.INSERT, klass + '.' + method + desc);
    }
    
    public static AdjustToken prepend(String str) {
        return new AdjustToken(AdjustToken.Adjustment.TRANSFORM, (state,s) -> str + s);
    }
    
    public static AdjustToken append(String str) {
        return new AdjustToken(AdjustToken.Adjustment.TRANSFORM, (state,s) -> s + str);
    }

    public static AdjustToken surround(String pre, String post) {
        if (pre.equals("\"") && post.equals("\"")) {
            // "adjusted token with enquote rather than enclosed with '"'"
            Global.LOG(M354);
            return tok_enquote;
        }
        return new AdjustToken(AdjustToken.Adjustment.TRANSFORM, (state,s) -> pre + s + post);
    }
    
    public static AdjustToken replace(String from, String to) {
        return new AdjustToken(AdjustToken.Adjustment.TRANSFORM, (state,s) -> s.replace(from,to));
    }
    
    public static AdjustToken removePrefix(String prefix) {
        return new AdjustToken(AdjustToken.Adjustment.TRANSFORM,
                (state,s) -> s.startsWith(prefix)? s.substring(prefix.length()): s);
    }

    public final static AdjustToken tok_enquote = new AdjustToken(AdjustToken.Adjustment.TRANSFORM,
                (state,s) -> StringUtil.enquote(s));        

    public final static AdjustToken tok_toUC = new AdjustToken(AdjustToken.Adjustment.TRANSFORM,
            (state,s) -> s.toUpperCase());
    
    public final static AdjustToken tok_toLC = new AdjustToken(AdjustToken.Adjustment.TRANSFORM,
            (state,s) -> s.toLowerCase());
    
    public static AdjustToken translateDesc() {
        return new AdjustToken(AdjustToken.Adjustment.TRANSFORM, (state,s) -> JynxGlobal.TRANSLATE_DESC(s));
    }
    
    public final static AdjustToken xxx_ivarindex = new AdjustToken(Adjustment.TRANSFORM,
        (state, s) -> state.indexOf('i', s));
    
    public final static AdjustToken xxx_lvarindex = new AdjustToken(Adjustment.TRANSFORM,
        (state, s) -> state.indexOf('l', s));
    
    public final static AdjustToken xxx_fvarindex = new AdjustToken(Adjustment.TRANSFORM,
        (state, s) -> state.indexOf('f', s));
    
    public final static AdjustToken xxx_dvarindex = new AdjustToken(Adjustment.TRANSFORM,
        (state, s) -> state.indexOf('d', s));
    
}
