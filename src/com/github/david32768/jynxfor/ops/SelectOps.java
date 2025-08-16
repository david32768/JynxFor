package com.github.david32768.jynxfor.ops;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static com.github.david32768.jynxfor.ops.ExtendedOps.*;
import static com.github.david32768.jynxfor.ops.JvmOp.*;

import static com.github.david32768.jynxfor.my.Message.M278;
import static com.github.david32768.jynxfor.my.Message.M282;

import com.github.david32768.jynxfree.jvm.NumType;
import com.github.david32768.jynxfree.jynx.LogIllegalArgumentException;

import jynx2asm.FrameElement;
import jynx2asm.InstList;
import jynx2asm.Line;
import jynx2asm.Token;

public enum SelectOps implements SelectOp {
    
    opc_ildc(Type.ILDC,
            asm_iconst_0,asm_iconst_1,asm_iconst_2,asm_iconst_3,asm_iconst_4,asm_iconst_5,
            asm_iconst_m1,asm_bipush,asm_sipush,asm_ldc),
    opc_lldc(Type.LLDC,
            asm_lconst_0,asm_lconst_1,ext_lconst_2,ext_lconst_3,ext_lconst_4,ext_lconst_5,
            ext_lconst_m1,ext_blpush,ext_slpush,opc_ldc2_w),
    opc_fldc(Type.FLDC,asm_fconst_0,asm_fconst_1,asm_fconst_2,asm_ldc,xxx_fraw),
    opc_dldc(Type.DLDC,asm_dconst_0,asm_dconst_1,opc_ldc2_w,xxx_draw),

    xxx_xreturn(Type.RETURN),
    
    xxx_xload(Type.LOCAL,asm_iload,asm_lload,asm_fload,asm_dload,asm_aload),
    xxx_xstore(Type.STACK,asm_istore,asm_lstore,asm_fstore,asm_dstore,asm_astore),
    xxx_x2z(Type.STACK,ext_i2z,ext_l2z,ext_f2z,ext_d2z,ext_a2z),
    ;
        
    private final Select selector;

    private SelectOps(SelectOps.Type type, JynxOp... ops) {
        this.selector = new Select(type, ops);
    }

    public static Map<String,JynxOp> getMacros() {
        Map<String,JynxOp> map = new HashMap<>();
        Stream.of(values())
                .filter(m -> m.name().startsWith("opc_"))
                .forEach(m -> map.put(m.toString(), m));
        return map;
    }
    
    @Override
    public JynxOp getOp(Line line, InstList instlist) {
        return selector.getOp(line,instlist);
    }

    @Override
    public String toString() {
        return name().substring(4);
    }

    private static enum Type {
        STACK_LENGTH,
        ILDC,
        LLDC,
        FLDC,
        DLDC,
        STACK,
        LOCAL,
        RETURN,
        ;
    }

    public static SelectOp of12(JynxOp oplen1, JynxOp oplen2) {
        return new Select(Type.STACK_LENGTH, oplen1, oplen2);
    }

    public static SelectOp stackILFDA(JynxOp iop, JynxOp jop, JynxOp fop, JynxOp dop, JynxOp aop) {
        return new Select(Type.STACK, iop, jop, fop, dop, aop);
    }

    private static class Select implements SelectOp {

        private final Type type;
        private final JynxOp[] ops;

        private Select(SelectOps.Type type, JynxOp... ops) {
            this.type = type;
            this.ops = ops;
        }

        @Override
        public JynxOp getOp(Line line,InstList instlist) {
            int index;
            switch (type) {
                case RETURN -> {
                    return instlist.getReturnOp();
                }
                case STACK_LENGTH -> index = getLength(line,instlist);
                case STACK -> index = getTypeIndex(instlist.peekTOS());
                case LOCAL -> index = getLocal(line,instlist);
                case ILDC -> index = getIldc(line,instlist);
                case LLDC -> index = getLldc(line,instlist);
                case FLDC -> index = getFldc(line,instlist);
                case DLDC -> index = getDldc(line,instlist);
                default -> throw new EnumConstantNotPresentException(type.getClass(), type.name());
            }
            if (index < 0 || index >= ops.length) {
                throw new AssertionError();
            }
            return ops[index];
        }

        private int getLength(Line line,InstList instlist) {
            FrameElement fe = instlist.peekTOS();
            return fe.slots() - 1;
        }

        private final static String ILFDA = "ilfda";
        
        private int getTypeIndex(FrameElement fe) {
            char fetype = fe.instLetter();
            int index = ILFDA.indexOf(fetype);
            if (index < 0 || index >= ops.length) {
                String valid = ILFDA.substring(0,Math.min(ILFDA.length(),ops.length));
                // "instruction type '%c' (%s) of element at top of stack is not one of %s"
                throw new LogIllegalArgumentException(M282,fetype,fe,valid);
            }
            assert ops[index].toString().charAt(0) == fetype:
                    String.format("%s %c", ops[index],fetype);
            return index;
        }
        
        private int getLocal(Line line, InstList instlist) {
            Token token = line.peekToken();
            return getTypeIndex(instlist.peekVarNum(token));
        }
        
        private int rangeType(long lval) {
            if (lval >= 0 && lval <= 5) {
                return (int)lval;
            }
            if (lval == -1) {
                return 6;
            }
            if (NumType.t_byte.isInRange(lval)) {
                return 7;
            }
            if (NumType.t_short.isInRange(lval)) {
                return 8;
            } 
            return 9;
        }
        
        private int getIldc(Line line,InstList instlist) {
            Token token = line.nextToken();
            int ival = token.asInt();
            int rval = rangeType(ival);
            if (rval > 6) {
                line.insert(Integer.toString(ival));
            }
            return rval;
        }

        private int getLldc(Line line,InstList instlist) {
            Token token = line.nextToken();
            long lval = token.asLong();
            int rval = rangeType(lval);
            if (rval <= 6) {
                return rval;
            }
            if (rval == 9) {
                line.insert(Long.toString(lval) + 'L');
            } else {
                line.insert(Long.toString(lval));
            }
            return rval;
        }

        private final static int F_NAN_PREFIX = 0x7f800000;
        private final static int F_NAN_CANONICAL = Float.floatToRawIntBits(Float.NaN);
        private final static int F_NAN_LIMIT = 1 << 23;
        
        private int getFldc(Line line,InstList instlist) {
            Token token = line.nextToken();
            String str = token.toString();
            if (str.equals("-nan") || str.equals("nan") || str.equals("+nan")) {
                str += ":" + Integer.toHexString(F_NAN_CANONICAL & ~F_NAN_PREFIX);
            }
            if (str.startsWith("nan:") || str.startsWith("-nan:")  || str.startsWith("+nan:")) {
                int num = Integer.valueOf(str.substring(str.indexOf(':') + 1),16);
                if (num <= 0 || num >= F_NAN_LIMIT) {
                    // "NaN type %#x is not in (0,%#x)"
                    throw new LogIllegalArgumentException(M278, num, F_NAN_LIMIT);
                }
                num |= F_NAN_PREFIX;
                if (str.charAt(0) == '-') {
                    num |= 1 << 31; // set sign bit
                }
                line.insert("0x" + Integer.toHexString(num));
                assert ops[4] == ExtendedOps.xxx_fraw;
                return 4;
            }
            if (str.equals("inf") || str.equals("+inf") || str.equals("-inf")) {
                if (str.equals("inf")) {
                    str = "+" + str;
                }
                line.insert(str.replace("inf","InfinityF"));
                assert ops[3] == asm_ldc;
                return 3;
            }
            float fval = token.asFloat();
            if (Float.compare(fval, +0.0F) == 0) { // NOT -0/0F
                assert ops[0] == asm_fconst_0;
                return 0;
            } else if (fval == 1.0f) {
                assert ops[1] == asm_fconst_1;
                return 1;
            } else if (fval == 2.0f) {
                assert ops[2] == asm_fconst_2;
                return 2;
            } else {
                if (Float.isNaN(fval)) {
                    line.insert("+NaNF"); // ldc requires + sign
                } else if (Float.isInfinite(fval) && fval > 0) {
                    line.insert("+InfinityF");  // ldc requires + sign
                } else  {
                    line.insert(Float.toHexString(fval) + 'F');
                }
                assert ops[3] == asm_ldc;
                return 3;
            }
        }

        private final static long D_NAN_PREFIX = 0x7ff0000000000000L;
        private final static long D_NAN_CANONICAL = Double.doubleToRawLongBits(Double.NaN);
        private final static long D_NAN_LIMIT = 1L << 52;
        
        private int getDldc(Line line,InstList instlist) {
            Token token = line.nextToken();
            String str = token.toString();
            if (str.equals("-nan") || str.equals("nan") || str.equals("+nan")) {
                str += ":" + Long.toHexString(D_NAN_CANONICAL & ~D_NAN_PREFIX);
            }
            if (str.startsWith("nan:") || str.startsWith("-nan:")  || str.startsWith("+nan:")) {
                String hexstr = str.substring(str.indexOf(':') + 1);
                long num = Long.valueOf(hexstr,16);
                if (num <= 0 || num >= D_NAN_LIMIT) {
                    // "NaN type %#x is not in (0,%#x)"
                    throw new LogIllegalArgumentException(M278, num, D_NAN_LIMIT);
                }
                num |= D_NAN_PREFIX;
                if (str.charAt(0) == '-') {
                    num |= 1L << 63; // set sign bit 
                }
                line.insert("0x" + Long.toHexString(num) + "L");
                assert ops[3] == ExtendedOps.xxx_draw;
                return 3;
            }
            if (str.equals("inf") || str.equals("+inf") || str.equals("-inf")) {
                if (str.equals("inf")) {
                    str = "+" + str;
                }
                line.insert(str.replace("inf","InfinityF"));
                assert ops[2] == opc_ldc2_w;
                return 2;
            }
            double dval = token.asDouble();
            if (Double.compare(dval,0.0) == 0) { // Not -0.0
                assert ops[0] == asm_dconst_0;
                return 0;
            } else if (dval == 1.0) {
                assert ops[1] == asm_dconst_1;
                return 1;
            } else {
                if (Double.isNaN(dval)) {
                    line.insert("+NaN"); // ldc requires + sign
                } else if (Double.isInfinite(dval) && dval > 0) {
                    line.insert("+Infinity"); // ldc requires + sign
                } else {
                    line.insert(Double.toHexString(dval));
                }
                assert ops[2] == opc_ldc2_w;
                return 2;
            }
        }
    
    }
    
}
