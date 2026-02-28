package com.github.david32768.jynxfor.ops;

import static com.github.david32768.jynxfor.ops.AdjustToken.insert;
import static com.github.david32768.jynxfor.ops.LineOps.tok_swap;

import com.github.david32768.jynxfree.jvm.OpArg;

public class FieldOp {

    private FieldOp() {}

    public static MacroOp getStatic(String name, String desc) {
        return field(name, desc, JvmOp.asm_getstatic);
    }
    
    public static MacroOp putStatic(String name, String desc) {
        return field(name, desc, JvmOp.asm_putstatic);
    }
    
    public static MacroOp getVirtual(String name, String desc) {
        return field(name, desc, JvmOp.asm_getfield);
    }
    
    public static MacroOp putVirtual(String name, String desc) {
        return field(name, desc, JvmOp.asm_putfield);
    }
    
    private static MacroOp field(String name, String desc, JvmOp fieldop) {
        assert fieldop.args() == OpArg.arg_field;
        return MacroOp.of(insert(desc), insert(name), fieldop);
    }
    
    // following have variable name on line
    public static MacroOp getStatic(String desc) {
        return field(desc, JvmOp.asm_getstatic);
    }
    
    public static MacroOp putStatic(String desc) {
        return field(desc, JvmOp.asm_putstatic);
    }
    
    public static MacroOp getVirtual(String desc) {
        return field(desc, JvmOp.asm_getfield);
    }
    
    public static MacroOp putVirtual(String desc) {
        return field(desc, JvmOp.asm_putfield);
    }
    
    private static MacroOp field(String desc, JvmOp fieldop) {
        assert fieldop.args() == OpArg.arg_field;
        return MacroOp.of(insert(desc), tok_swap, fieldop);
    }
    
}
