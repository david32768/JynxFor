package com.github.david32768.jynxfor.ops;

@FunctionalInterface
public interface LineOp extends JynxOp {

    @Override
    default public Integer length(){
        return 0;
    }

    public void adjustLine(CurrentState state);

}
