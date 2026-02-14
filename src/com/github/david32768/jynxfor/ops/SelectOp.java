package com.github.david32768.jynxfor.ops;

@FunctionalInterface
public interface SelectOp extends JynxOp {

    @Override
    default public Integer length(){
        return null;
    }
    
    public JynxOp getOp(CurrentState state);

}
