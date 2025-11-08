package com.github.david32768.jynxfor.instruction;

import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;

import static com.github.david32768.jynxfor.my.Message.M134;
import static com.github.david32768.jynxfor.my.Message.M144;
import static com.github.david32768.jynxfree.jynx.Global.LOG;

import com.github.david32768.jynxfor.scan.ConstType;

import com.github.david32768.jynxfree.jvm.ConstantPoolType;
import com.github.david32768.jynxfree.jvm.Context;
import com.github.david32768.jynxfree.jvm.HandleType;
import com.github.david32768.jynxfree.jvm.NumType;
import com.github.david32768.jynxfree.jynx.Global;
import com.github.david32768.jynxfree.jynx.ReservedWord;
import com.github.david32768.jynxfree.jynx.StringUtil;

import jynx2asm.handles.JynxHandle;

public class Object2String {

    public Object2String() {
    }
    
    public String handle2String(Object value) {
        Global.CHECK_CAN_LOAD(ConstantPoolType.CONSTANT_MethodHandle);
        Handle handle = (Handle) value;
        int tag = handle.getTag();
        String prefix = HandleType.getPrefix(tag);
        JynxHandle jh = JynxHandle.of(handle);
        return prefix + jh.iond();
    }
    
    public String constDynamic2String(Object value) {
        StringBuilder sb = new StringBuilder();
        ConstantDynamic dyncst = (ConstantDynamic) value;
        sb.append(ReservedWord.left_brace)
                .append(' ')
                .append(dyncst.getName())
                .append(' ')
                .append(dyncst.getDescriptor())
                .append(' ')
                .append(handle2String(dyncst.getBootstrapMethod()));
        for (int i = 0, n = dyncst.getBootstrapMethodArgumentCount(); i < n;++i) {
            Object argvalue = dyncst.getBootstrapMethodArgument(i);
            String cststr = asm2String(argvalue);
            sb.append(' ')
                    .append(cststr);
        }
        sb.append(' ')
                .append(ReservedWord.right_brace);
        return sb.toString();
    }

    public String long2String(Object value) {
        Long L = (Long)value;
        String suffix = "L";
        return L.toString() + suffix;
    }
    
    public String double2String(Object value) {
        Double dval = (Double)value;
        String prefix = dval.isNaN() || dval == Double.POSITIVE_INFINITY?"+":"";
        String strval = Double.toHexString(dval);
        return prefix + strval;
    }

    public String float2String(Object value) {
        Float fval = (Float)value;
        String prefix = fval.isNaN() || fval == Float.POSITIVE_INFINITY?"+":"";
        String strval = Float.toHexString(fval);
        return prefix + strval + "F";
    }
    
    public String char2String(Object value) {
        int ival;
        if (value instanceof Character) {
            ival = (int)(char)value;
        } else {
            ival = (int)value;
        }
        NumType.t_char.checkInRange(ival);
        if (StringUtil.isVisibleAscii(ival)) {
            char c = (char)ival;
            return "'" + StringUtil.escapeChar(c) + "'";
        }
        return Integer.toString(ival);
    }
    
    public String boolean2String(Object value) {
        // Boolean constant is stored as Integer but check
        if (value == Boolean.TRUE || value instanceof Integer && 1 == (Integer)value) {
            return Boolean.TRUE.toString();
        }
        if (value == Boolean.FALSE || value instanceof Integer && 0 == (Integer)value) {
            return Boolean.FALSE.toString();
        }
        LOG(M134,value,value.getClass()); // "boolean value is neither Boolean or Integer 0/1: value = %s class = %s"
        return Boolean.TRUE.toString();
    }
    
    public String asm2String(Object value) {
        ConstType ct = ConstType.getFromASM(value,Context.JVMCONSTANT);
        if (ct == ConstType.ct_object) {
            LOG(M144,value.getClass().getName()); // "unknown value class = %s"
            throw new UnsupportedOperationException();
        }
        if (ct == ConstType.ct_class) {
            return ((Type)value).getInternalName();
        }
        return stringFrom(ct, value);
    }

    public String stringFrom(ConstType ct, Object value) {
        return switch (ct) {
            case ct_boolean -> boolean2String(value);
            case ct_char -> char2String(value);
            case ct_long -> long2String(value);
            case ct_float -> float2String(value);
            case ct_double -> double2String(value);
            case ct_string -> StringUtil.QuoteEscape((String)value);
            case ct_method_handle -> handle2String(value);
            case ct_const_dynamic -> constDynamic2String(value);
            default -> value.toString();
        };
    }
}
