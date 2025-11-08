package com.github.david32768.jynxfor.ops;

import java.util.Objects;

import org.objectweb.asm.ConstantDynamic;

import com.github.david32768.jynxfor.instruction.DynamicInstruction;
import com.github.david32768.jynxfor.instruction.JynxInstruction;
import com.github.david32768.jynxfor.scan.Line;

import com.github.david32768.jynxfree.jynx.NameDesc;

import jynx2asm.ClassChecker;
import jynx2asm.JynxConstantDynamic;

public class DynamicSimpleOp implements DynamicOp {

    private final String name;
    private final String desc;
    private final String bootmethodName;
    private final String bootdescplus;
    private final String[] bootparms;

    private DynamicSimpleOp(String name, String desc, String bootmethodName,
            String bootdescplus, String... bootparms) {
        this.name = name;
        this.desc = desc;
        this.bootmethodName = bootmethodName;
        this.bootdescplus = bootdescplus;
        this.bootparms = bootparms;
    }
    
    public static DynamicSimpleOp getInstance(String name, String desc,
            String bootclass, String bootmethod, String bootdescplus, String... bootparms) {
        assert Objects.nonNull(bootclass);
        assert Objects.nonNull(bootmethod);
        assert name == null || NameDesc.METHOD_ID.validate(name);
        assert desc == null || NameDesc.DESC.validate(desc);
        assert NameDesc.CLASS_NAME.validate(bootclass);
        assert NameDesc.METHOD_ID.validate(bootmethod);
        String boot = bootclass + '.' + bootmethod;
        return new DynamicSimpleOp(name, desc, boot, bootdescplus,bootparms);
    }

    @Override
    public JynxInstruction getInstruction(Line line, ClassChecker checker) {
        String namex = name;
        String descx = desc;
        if (namex == null) {
            String namedesc = line.nextToken().asString();
            int lbindex = namedesc.indexOf('(');
            if (lbindex >= 0 && desc == null) {
                namex = namedesc.substring(0,lbindex);
                descx = namedesc.substring(lbindex);
            } else {
                namex = namedesc;
            }
        }
        if (descx == null) {
            descx = line.nextToken().asString();
        }
        JynxConstantDynamic jcd = new JynxConstantDynamic(line, checker);
        ConstantDynamic cd = jcd.getSimple(namex, descx, bootmethodName, bootdescplus,bootparms);
        return new DynamicInstruction(JvmOp.asm_invokedynamic, cd);
    }

    @Override
    public String toString() {
        return String.format("*DynamicSimple boot %s %s %s",
                bootmethodName,bootdescplus,String.join(" ", bootparms));
    }
    
}
