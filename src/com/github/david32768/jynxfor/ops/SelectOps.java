package com.github.david32768.jynxfor.ops;

import java.util.function.Function;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static com.github.david32768.jynxfor.ops.JvmOp.*;

import static com.github.david32768.jynxfor.my.Message.M278;
import static com.github.david32768.jynxfor.my.Message.M355;

import com.github.david32768.jynxfor.scan.Line;
import com.github.david32768.jynxfor.scan.Token;
import com.github.david32768.jynxfree.classfile.SmallOpcodeType;
import com.github.david32768.jynxfree.jynx.Global;


public enum SelectOps implements SelectOp {
    
    opc_ildc(SelectOps::getIldc),
    opc_lldc(SelectOps::getLldc),
    opc_fldc(SelectOps::getFldc),
    opc_dldc(SelectOps::getDldc),

    ;
        
    private final Function<CurrentState, JynxOp> fn;

    private SelectOps(Function<CurrentState, JynxOp>  fn) {
        this.fn = fn;
    }

    public static Map<String,JynxOp> getMacros() {
        Map<String,JynxOp> map = new HashMap<>();
        Stream.of(values())
                .filter(m -> m.name().startsWith("opc_"))
                .forEach(m -> map.put(m.toString(), m));
        return map;
    }
    
    @Override
    public JynxOp getOp(CurrentState state) {
        return fn.apply(state);
    }

    @Override
    public String toString() {
        return name().substring(4);
    }

    private static JynxOp getIldc(CurrentState state) {
        Line line = state.line();
        Token token = line.nextToken();
        int ival = token.asInt();
        JvmOp op = JvmOp.of(SmallOpcodeType.opFor(ival));
        if (!op.isImmediate()) {
            line.insert(Integer.toString(ival));
        }
        return op;
    }

    private static JynxOp getLldc(CurrentState state) {
        Line line = state.line();
        Token token = line.nextToken();
        long lval = token.asLong();
        var optiop = SmallOpcodeType.intOpFor(lval);
        if (optiop.isPresent()) {
            JvmOp iop = JvmOp.of(optiop.get());
            if (!iop.isImmediate()) {
                line.insert(Integer.toString((int)lval));
            }
            return MacroOp.of(iop, asm_i2l);
        } else {
            JvmOp lop = JvmOp.of(SmallOpcodeType.opFor(lval));
            if (lop.isImmediate()) {
                return lop;
            }
            line.insert(Long.toString(lval) + 'L');
            return lop;
        }
    }

    private final static int F_NAN_PREFIX = 0x7f800000;
    private final static int F_NAN_CANONICAL = Float.floatToRawIntBits(Float.NaN);
    private final static int F_NAN_LIMIT = 1 << 23;
    private final static int JAVA_F_NAN_SUFFIX = F_NAN_CANONICAL & ~F_NAN_PREFIX;
    private final static CallOp inv_iasf = CallOp.of(Float.class,"intBitsToFloat","(I)F");

    private static JynxOp getFldc(CurrentState state) {
        Line line = state.line();
        Token token = line.nextToken();
        String str = token.toString();
        if (str.startsWith("nan") || str.startsWith("inf")) {
            str = "+" + str;
        }
        if (str.startsWith("-nan")  || str.startsWith("+nan")) {
            int num = (int)nanSuffix(str, F_NAN_LIMIT, JAVA_F_NAN_SUFFIX);
            num |= F_NAN_PREFIX;
            if (str.charAt(0) == '-') {
                num |= 1 << 31; // set sign bit
            }
            long lnum = Integer.toUnsignedLong(num);
            line.insert("0x" + Long.toHexString(lnum));
            return MacroOp.of(asm_ldc, inv_iasf);
        }
        if (str.equals("+inf") || str.equals("-inf")) {
            line.insert(str.replace("inf","InfinityF"));
            return asm_ldc;
        }
        float fval = token.asFloat();
        var optiop = SmallOpcodeType.intOpFor(fval);
        if (optiop.isPresent()) {
            JvmOp iop = JvmOp.of(optiop.get());
            if (!iop.isImmediate()) {
                line.insert(Integer.toString((int)fval));
            }
            return MacroOp.of(iop, asm_i2f);
        } else {
            JvmOp fop = JvmOp.of(SmallOpcodeType.opFor(fval));
            if (fop.isImmediate()) {
                return fop;
            }
            if (Float.isNaN(fval)) {
                line.insert("+NaNF"); // ldc requires + sign
            } else if (Float.isInfinite(fval) && fval > 0) {
                line.insert("+InfinityF");  // ldc requires + sign
            } else  {
                line.insert(Float.toHexString(fval) + 'F');
            }
            return fop;
        }
    }

    private final static long D_NAN_PREFIX = 0x7ff0000000000000L;
    private final static long D_NAN_CANONICAL = Double.doubleToRawLongBits(Double.NaN);
    private final static long D_NAN_LIMIT = 1L << 52;
    private final static long JAVA_D_NAN_SUFFIX = D_NAN_CANONICAL & ~D_NAN_PREFIX;
    private final static CallOp inv_lasd = CallOp.of(Double.class,"longBitsToDouble","(J)D");

    private static JynxOp getDldc(CurrentState state) {
        Line line = state.line();
        Token token = line.nextToken();
        String str = token.toString();
        if (str.startsWith("nan") || str.startsWith("inf")) {
            str = "+" + str;
        }
        if (str.startsWith("-nan")  || str.startsWith("+nan")) {
            long num = nanSuffix(str, D_NAN_LIMIT, JAVA_D_NAN_SUFFIX);
            num |= D_NAN_PREFIX;
            if (str.charAt(0) == '-') {
                num |= 1L << 63; // set sign bit 
            }
            line.insert("0x" + Long.toHexString(num) + "L");
            return MacroOp.of(opc_ldc2_w, inv_lasd);
        }
        if (str.equals("+inf") || str.equals("-inf")) {
            line.insert(str.replace("inf","InfinityF"));
            return opc_ldc2_w;
        }
        double dval = token.asDouble();
        var optiop = SmallOpcodeType.intOpFor(dval);
        if (optiop.isPresent()) {
            JvmOp iop = JvmOp.of(optiop.get());
            if (!iop.isImmediate()) {
                line.insert(Integer.toString((int)dval));
            }
            return MacroOp.of(iop, asm_i2d);
        } else {
            JvmOp dop = JvmOp.of(SmallOpcodeType.opFor(dval));
            if (dop.isImmediate()) {
                return dop;
            }
            if (Double.isNaN(dval)) {
                line.insert("+NaN"); // ldc requires + sign
            } else if (Double.isInfinite(dval) && dval > 0) {
                line.insert("+Infinity"); // ldc requires + sign
            } else {
                line.insert(Double.toHexString(dval));
            }
            return dop;
        }
    }
    
    private static long nanSuffix(String str , long limit, long defaultnum) {
        assert str.startsWith("-nan") || str.startsWith("+nan");
        if (str.length() == 4) {
            return defaultnum;
        }
        if (str.charAt(4) != ':') {
            // "%s is not a valid nan suffix"
            Global.LOG(M355,str.substring(4));
            return defaultnum;
        }
        String hexstr = str.substring(5);
        long num;
        try {
            num = Long.parseLong(hexstr, 16);
        } catch(NumberFormatException ex) {
            // "%s is not a valid nan suffix"
            Global.LOG(M355,str.substring(4));
            return defaultnum;
        }
        if (num <= 0 || num >= limit) {
            // "NaN type %#x is not in (0,%#x)"
            Global.LOG(M278, num, limit);
            num = defaultnum;
        }
        return num;
    }
}

