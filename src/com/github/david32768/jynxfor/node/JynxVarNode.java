package com.github.david32768.jynxfor.node;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import com.github.david32768.jynxfor.scan.Line;

import jynx2asm.JynxLabel;

public record JynxVarNode(int varnum, String name, String desc, String signature,
    JynxLabel from, JynxLabel to, Line line) implements LabelRange {

    public void accept(MethodVisitor mv, Label startLabel, Label endLabel) {
        Label fromlab = from == null? startLabel: from.asmlabel();
        Label tolab = to == null? endLabel: to.asmlabel();
        mv.visitLocalVariable(name, desc, signature, fromlab, tolab, varnum);
    }
}
