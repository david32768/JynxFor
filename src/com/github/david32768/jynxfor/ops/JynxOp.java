package com.github.david32768.jynxfor.ops;

public interface JynxOp {

    default public IndentType indentType() {
        return IndentType.NONE;
    }
    
}
