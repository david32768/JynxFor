package com.github.david32768.jynxfor.ops;

import jynx2asm.InstList;
import jynx2asm.Line;

public interface SelectOp extends JynxOp {

    @Override
    default public Integer length(){
        return null;
    }
    
    public JynxOp getOp(Line line, InstList instlist);

}
