package com.github.david32768.jynxfor.ops;

import com.github.david32768.jynxfor.scan.Line;
import com.github.david32768.jynxfor.scan.Token;
import com.github.david32768.jynxfree.jynx.LogUnexpectedEnumValueException;

import com.github.david32768.jynxfree.jynx.NameDesc;

import jynx2asm.LabelStack;

public enum LabelOps implements LabelOp {

    mac_label,

    lab_peek,
    lab_peek_if,
    lab_peek_else,
    lab_push,
    lab_push_if,
    lab_pop,
    
    ;    
    
    private LabelOps() {}

    private final static String ELSE = "ELSE";

    @Override
    public void adjustLine(Line line, int macrolevel, LabelStack labelStack) {
        switch(this) {
            case mac_label -> {
                String labstr = String.format("%cL%dML%d",NameDesc.GENERATED_LABEL_MARKER,line.getLinect(),macrolevel);
                Token maclabel  = Token.getInstance(labstr);
                line.insert(maclabel);
            }

            case lab_pop -> line.insert(labelStack.pop());
            case lab_peek -> line.insert(labelStack.peek());
            case lab_peek_else -> line.insert(labelStack.peek().transform(s->s + ELSE));
            case lab_peek_if -> line.insert(labelStack.peekIf());
            case lab_push -> labelStack.push(line.nextToken());
            case lab_push_if -> labelStack.pushIf(line.nextToken());

            default -> throw new LogUnexpectedEnumValueException(this);
        }
    }

}
