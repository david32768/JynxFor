package com.github.david32768.jynxfor.ops;

import com.github.david32768.jynxfree.jvm.JvmVersionRange;

public interface MacroOp extends JynxOp {

    public JynxOp[] getJynxOps();

    @Override
    default public Integer length() {
        return JynxOps.length(this);
    }
    
    @Override
    default public JvmVersionRange range() {
        return JynxOps.range(this);
    }

    public static MacroOp of(JynxOp... jynxops) {
        return () -> jynxops;
    }
    
}
