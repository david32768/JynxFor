package com.github.david32768.jynxfor.ops;

import com.github.david32768.jynxfor.scan.Line;

import jynx2asm.InstList;

public interface SelectOp extends JynxOp {

    @Override
    default public Integer length(){
        return null;
    }
    
    public JynxOp getOp(Line line, InstList instlist);

}
