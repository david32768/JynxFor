package com.github.david32768.jynxfor.ops;

@FunctionalInterface
public interface SelectOp extends JynxOp {

    public JynxOp getOp(CurrentState state);

}
