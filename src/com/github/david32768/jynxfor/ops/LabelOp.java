package com.github.david32768.jynxfor.ops;

import com.github.david32768.jynxfor.scan.Line;

import jynx2asm.LabelStack;

public interface LabelOp extends JynxOp {

    public void adjustLine(Line line, int macrolevel, LabelStack labelStack);

}
