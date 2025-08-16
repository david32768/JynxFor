package jynx2asm;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.objectweb.asm.Type;

import static com.github.david32768.jynxfor.my.Message.M206;
import static com.github.david32768.jynxfor.my.Message.M906;

import com.github.david32768.jynxfree.jvm.FrameType;
import com.github.david32768.jynxfree.jynx.LogAssertionError;
import com.github.david32768.jynxfree.jynx.LogIllegalArgumentException;

public enum FrameElement {

    ANY(' ','?',true),
    ERROR('X','?',true),
    UNUSED('_', '?',true),
    TOP('2','?',true),

    RETURN_ADDRESS('R','?'),
    THIS('T','a'), // uninitialised this and this in <init> methods

    OBJECT('A','a'),
    INTEGER('I','i'),
    FLOAT('F','f'),
    DOUBLE('D','d'),
    LONG('J','l'),
;

    private final char typeChar;
    private final char instChar;
    private final boolean localsOnly;

    private FrameElement(char typeLetter, Character instChar) {
        this(typeLetter, instChar, false);
    }

    private FrameElement(char typeChar, char instChar, boolean localsOnly) {
        this.typeChar = typeChar;
        this.instChar = instChar;
        this.localsOnly = localsOnly;
    }

    private final static Map<Character, FrameElement> TYPE_MAP;
    
    static {
        TYPE_MAP = new HashMap<>();
        for (FrameElement fe:values()) {
            FrameElement shouldbenull = TYPE_MAP.put(fe.typeChar,fe);
            assert shouldbenull == null;
        }
    }

    private boolean isObject() {
        return instChar == 'a';
    }
    
    private char typeLetter() {
        return typeChar;
    }

    public boolean isCompatibleWith(FrameElement that) {
        return this == that || this == ERROR;
    }
    
    public boolean isAfterCompatibleWith(FrameElement that) {
        return isCompatibleWith(that) 
                || this == FrameElement.UNUSED
                || this == FrameElement.ANY;
    }

    public char instLetter() {
        return instChar;
    }
    
    public boolean isLocalsOnly() {
        return localsOnly;
    }
    
    public boolean isTwo() {
        return this == DOUBLE || this == LONG;
    }

    public int slots() {
        return isTwo()?2:1;
    }
    
    public FrameElement next() {
        if (isTwo()) {
            return TOP;
        }
        throw new AssertionError();
    } 

    public boolean checkNext(FrameElement fe) {
        return isTwo() && fe == TOP;
    } 

    public boolean matchStack(FrameElement required) {
        assert !required.isLocalsOnly();
        return this == required || isObject() && required == FrameElement.OBJECT;
    }
    
    public boolean matchLocal(FrameElement required) {
        return this == required || isObject() && required == FrameElement.OBJECT;
    }
    
    public boolean isValidInContext(FrameClass fc) {
        return switch(fc) {
            case STACK -> !isLocalsOnly();
            case LOCALS -> true;
            case MAPLOCALS -> !isLocalsOnly() || this == TOP;
        };
    }
    

    public static FrameElement combine(FrameElement fe1, FrameElement fe2) {
        if (fe1 == fe2) {
            return fe1;
        }
        if (fe1.isObject() && fe2.isObject()) {
            return OBJECT;
        }
        return ERROR;
    }

    public static boolean equivalent(FrameElement fe1, FrameElement fe2) {
        return fe1.compLetter() == fe2.compLetter();
    }
    
    private char compLetter() {
        return isObject()? OBJECT.typeChar: typeChar;
    }
    
    public static FrameElement fromStack(char type) {
        FrameElement stack = TYPE_MAP.get(type);
        if (stack == null || stack.isLocalsOnly()) {
            throw new LogIllegalArgumentException(M206, type,(int)type); // "Invalid type letter '%c' (%d)"
        }
        return stack;
    }
    
    public static Optional<FrameElement> fromReturn(char type) {
        if (type == 'V') {
            return Optional.empty();
        }
        return Optional.of(fromStack(type));
    }
    
    public static FrameElement fromLocal(char type) {
        FrameElement local = TYPE_MAP.get(type);
        if (local == null) {
            throw new LogIllegalArgumentException(M206, type,(int)type); // "Invalid type letter '%c' (%d)"
        }
        return local;
    }
    
    public static String stringForm(Stream<FrameElement> festream) {
        return festream
                .map(FrameElement::typeLetter)
                .map(String::valueOf)
                .collect(Collectors.joining()); 
    }
    
    public static FrameElement fromType(Type type) {
        int sort = type.getSort();
        return switch (sort) {
            case Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> INTEGER;
            case Type.FLOAT -> FLOAT;
            case Type.LONG -> LONG;
            case Type.DOUBLE -> DOUBLE;
            case Type.OBJECT, Type.ARRAY -> OBJECT;
            default -> throw new LogAssertionError(M906, type); // "unknown ASM type - %s"
        };
    }
    
    public static Optional<FrameElement> fromReturnType(Type rt) {
        if (rt == Type.VOID_TYPE) {
            return Optional.empty();
        }
        return Optional.of(fromType(rt));
    }
    
    public static FrameElement fromDesc(String typestr) {
        Type type = Type.getType(typestr);
        return fromType(type);
    }
    
    public static FrameElement fromFrame(FrameType ft) {
        return switch (ft) {
            case ft_Double -> DOUBLE;
            case ft_Float -> FLOAT;
            case ft_Integer -> INTEGER;
            case ft_Long -> LONG;
            case ft_Null, ft_Object, ft_Uninitialized -> OBJECT;
            case ft_Uninitialized_This -> THIS;
            case ft_Top -> TOP;
        };
    }

    public String desc() {
        return switch (this) {
            case INTEGER, LONG, FLOAT, DOUBLE -> "" + typeChar;
            default -> ConstType.ct_object.getDesc();
        }; 
    }
}
