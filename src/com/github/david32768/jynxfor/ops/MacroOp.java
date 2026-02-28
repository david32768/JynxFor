package com.github.david32768.jynxfor.ops;

@FunctionalInterface
public interface MacroOp extends JynxOp {

    public JynxOp[] getJynxOps();

    public static MacroOp of(JynxOp... jynxops) {
        return () -> jynxops;
    }
    
}
