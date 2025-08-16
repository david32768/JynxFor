package com.github.david32768.jynxfor.ops;

import com.github.david32768.jynxfree.jvm.Feature;
import com.github.david32768.jynxfree.jvm.JvmVersioned;
import com.github.david32768.jynxfree.jvm.JvmVersionRange;

public interface JynxOp extends JvmVersioned {

    @Override
    default JvmVersionRange range() {
        return Feature.unlimited.range();
    }
    
    default public Integer length() {
        return null; // unknown
    }
    
    default public IndentType indentType() {
        return IndentType.NONE;
    }
    
}
