package jynx2asm;

import java.util.EnumSet;
import java.util.function.UnaryOperator;
import java.util.Objects;
import java.util.Optional;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;

import static com.github.david32768.jynxfor.my.Message.*;
import static com.github.david32768.jynxfree.jynx.Global.*;

import com.github.david32768.jynxfree.jvm.NumType;
import com.github.david32768.jynxfree.jynx.Directive;
import com.github.david32768.jynxfree.jynx.LogIllegalArgumentException;
import com.github.david32768.jynxfree.jynx.LogIllegalStateException;
import com.github.david32768.jynxfree.jynx.NameDesc;
import com.github.david32768.jynxfree.jynx.ReservedWord;
import com.github.david32768.jynxfree.jynx.ReservedWordType;
import com.github.david32768.jynxfree.jynx.StringUtil;

public class Token {

    public final static Token END_TOKEN = new Token("\n     END_TOKEN");
    public final static Token END_CLASS_TOKEN = new Token(Directive.DIRECTIVE_INDICATOR + Directive.end_class.name());
    private final static String2Object S2O = new String2Object();
    
    private final static int  MAX_UTF8_STRING = 2*Short.MAX_VALUE + 1;
    
    private final String token;

    private Token(String token) {
        this.token = token;
    }

    public boolean isEndToken() {
        return this == END_TOKEN;
    }
    
    public Token checkNotEnd() {
        if (isEndToken()) {
            throw new LogIllegalArgumentException(M409); // "illegal operation on END_TOKEN"
        }
        return this;
    }
  
    private final static String QUOTES = "\"'";
    
    private void checkNotQuoted() {
        if (!token.isEmpty() && QUOTES.indexOf(token.charAt(0)) >= 0) {
            throw new LogIllegalArgumentException(M410); // "cannot amend quoted token"
        }
    }
    
    public static Token getInstance(String tokenstr) {
        assert Objects.nonNull(tokenstr);
        if (!tokenstr.isEmpty() && tokenstr.charAt(0) == '\"') {
            NameDesc.QUOTED_STRING.validate(tokenstr);
        } else {
            NameDesc.TOKEN.validate(tokenstr);
        }
        long len = tokenstr.length();
        if (len > MAX_UTF8_STRING/3 && StringUtil.modifiedUTF8Length(tokenstr) > MAX_UTF8_STRING) {
            // "String length of %d exceeds maximum %d"
            throw new LogIllegalArgumentException(M179,len,MAX_UTF8_STRING);
        }
        return new Token(tokenstr);
    }

    public static Token getInstance(ReservedWord res) {
        return getInstance(res.externalName());
    }

    public Token transform(UnaryOperator<String> op) {
        checkNotEnd();
        checkNotQuoted();
        String str = op.apply(token);
        return getInstance(str);
    }
    
    public String asString() {
        checkNotEnd();
        return token;
    }

    public String asReservedWordType(ReservedWordType rwtype) {
        return switch(rwtype) {
            case NAME -> asName();
            case QUOTED -> asQuoted();
            case LABEL, TOKEN -> asString();
            default -> throw new EnumConstantNotPresentException(rwtype.getClass(), rwtype.name());
        };
    }

    public String asLabel() {
        checkNotEnd();
        int index = token.indexOf(Line.LABEL_INDICATOR);
        if (index == 0 || index !=  token.length() - 1 || token.contains(" ")) {
            LOG(M49,token);  // "Invalid label - %s"
        }
        return token.substring(0, index);
    }
    
    public String asQuoted() {
        checkNotEnd();
        return StringUtil.unescapeString(token);
    }
    
    public boolean asBoolean() {
        checkNotEnd();
        return S2O.parseBoolean(token);
    }
    
    public byte asByte() {
        checkNotEnd();
        return (byte)S2O.decodeLong(token, NumType.t_byte);
    }

    public char asChar() {
        checkNotEnd();
        return S2O.parseCharacter(token);
    }
    
    public short asShort() {
        checkNotEnd();
        return (short)S2O.decodeLong(token, NumType.t_short);
    }
    
    public int asInt() {
        checkNotEnd();
        if (token.charAt(0) == '\'') {
            return (int)asChar();
        }
        return (int)S2O.decodeLong(token, NumType.t_int);
    }

    public long asLong() {
        checkNotEnd();
        return S2O.decodeLong(token, NumType.t_long);
    }
    
    public float asFloat() {
        checkNotEnd();
        return S2O.parseFloat(token);
    }
    
    public double asDouble() {
        checkNotEnd();
        return S2O.parseDouble(token);
    }
    
    public Type asType() {
        checkNotEnd();
        return S2O.parseType(token);
    }
    
    public Type asMethodType() {
        checkNotEnd();
        return Type.getType(token);
    }
    
    public Handle asHandle() {
        checkNotEnd();
        return S2O.parseHandle(token);
    }

    public long asUnsignedLong() {
        checkNotEnd();
        return S2O.decodeUnsignedLong(token, NumType.t_long);
    }
    
    public int asUnsignedInt() {
        checkNotEnd();
        return (int)S2O.decodeUnsignedLong(token, NumType.t_int);
    }
    
    public int asUnsignedShort() {
        checkNotEnd();
        return (int)S2O.decodeUnsignedLong(token, NumType.t_short);
    }
    
    public short asUnsignedByte() {
        checkNotEnd();
        return (short)S2O.decodeUnsignedLong(token, NumType.t_byte);
    }
    
    public Optional<Integer> asOptInt() {
        if (isEndToken()) {
            return Optional.empty();
        }
        return Optional.of(asInt());
    }


    public Object getValue(ConstType ct) {
        checkNotEnd();
        return switch(ct) {
            case ct_boolean -> asBoolean();
            case ct_byte -> asByte();
            case ct_char -> asChar();
            case ct_short -> asShort();
            case ct_int -> asInt();
            case ct_long -> asLong();
            case ct_float -> asFloat();
            case ct_double -> asDouble();
            case ct_string -> asQuoted();
            case ct_class -> asType();
            case ct_method_handle -> asHandle();
            case ct_method_type -> asMethodType();
            case ct_const_dynamic -> throw new AssertionError(ct.name());
            case ct_enum -> asQuoted();
            case ct_annotation -> throw new AssertionError(ct.name());
            case ct_object -> getConst();
            default -> throw new EnumConstantNotPresentException(ct.getClass(), ct.name());
        };
    }
    
    public Object getConst() {
        checkNotEnd();
        return S2O.getConst(this);
    }
    
    public String asName() {
        checkNotEnd();
        String name = StringUtil.removeEndQuotes(token);
        if (name.isEmpty()) {
            // "zero length name"
            throw new LogIllegalArgumentException(M152);
        }
        return name;
    }

    public int asTypeCode() {
        checkNotEnd();
        NumType type = NumType.fromString(token)
                .orElseThrow(() -> new LogIllegalArgumentException(M159, token)); // "Invalid type - %s"
        return type.typecode();
    }

    public Directive asDirective() {
        checkNotEnd();
        if (token.startsWith(Directive.DIRECTIVE_INDICATOR)) {
            String dirstr = token.substring(Directive.DIRECTIVE_INDICATOR.length());
            return  Directive.getDirInstance(dirstr)
                        // "Unknown directive = %s"
                        .orElseThrow(()->new LogIllegalStateException(M245,dirstr));
        }
        return null;
    }

    public boolean is(ReservedWord res) {
        return !isEndToken() && res.externalName().equals(token);
    }
    
    private Optional<ReservedWord> mayBe(EnumSet<ReservedWord> rwset) {
        if (isEndToken()) {
            return Optional.empty();
        }
        return ReservedWord.getOptInstance(token)
            .filter(rwset::contains);
    }
    
    public Optional<ReservedWord> mayBe(ReservedWord res1,ReservedWord... res) {
        return mayBe(EnumSet.of(res1,res));
    }
    
    public void mustBe(ReservedWord res) {
        checkNotEnd();
        if (!is(res)) {
            throw new LogIllegalArgumentException(M109,res, token); // "reserved word %s expected but found %s"
        }
    }
    
    public ReservedWord expectOneOf(ReservedWord res1,ReservedWord... res) {
        EnumSet<ReservedWord> rwset = EnumSet.of(res1,res);
        return expectOneOf(rwset);
    }
    
    public ReservedWord expectOneOf(EnumSet<ReservedWord> rwset) {
        checkNotEnd();
        return mayBe(rwset)
                 // "reserved word %s expected but found %s"
                .orElseThrow(()->new LogIllegalArgumentException(M109,rwset, token));
    }
    
    public void noneOf(ReservedWord res1,ReservedWord... res) {
        checkNotEnd();
        Optional<ReservedWord> optword = mayBe(res1,res);
        if (optword.isPresent()) {
            throw new LogIllegalStateException(M277,token); // "unexpected reserved word %s found"
        }
    }
    
    @Override
    public String toString() {
        if (isEndToken()) {
            return "\\n";
        }
        return token;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Token other) {
            if (isEndToken() || other.isEndToken()) {
                return this == other;
            }
            return token.equals(other.token);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return token.hashCode();
    }

}
