package com.github.david32768.jynxfor.ops;

import com.github.david32768.jynxfor.node.JynxInstructionNode;
import com.github.david32768.jynxfor.scan.Line;

import jynx2asm.ClassChecker;

public interface DynamicOp extends JynxOp {
    
    public JynxInstructionNode getInstruction(Line line, ClassChecker checker);

    public static DynamicOp of(String name, String desc, String bootclass,String bootmethod) {
        return DynamicSimpleOp.getInstance(name, desc, bootclass, bootmethod,"");
    }

    public static DynamicOp withBootParms(String name, String desc,String bootclass,String bootmethod,
            String bootdescplus, String... bootparms) {
        return DynamicSimpleOp.getInstance(name, desc, bootclass, bootmethod,bootdescplus,bootparms);
    }

}
