package com.github.david32768.jynxfor.ops;

import com.github.david32768.jynxfree.jynx.NameDesc;

import jynx2asm.LabelStack;
import jynx2asm.Line;
import jynx2asm.Token;

public enum LineOps implements LineOp {

    mac_label,

    lab_peek,
    lab_peek_if,
    lab_peek_else,
    lab_push,
    lab_push_if,
    lab_pop,

    line_num,
    
    tok_skip,
    tok_skipall,
    tok_swap,
    tok_dup,
    tok_print, // for debugging
    
    ;    
    
    private LineOps() {}

    @Override
    public void adjustLine(Line line, int macrolevel, MacroOp macroop, LabelStack labelStack) {
        lineop(line, macrolevel, labelStack);
    }

    private final static String ELSE = "ELSE";

    private void lineop(Line line, int macrolevel,LabelStack labelStack) {
        Token token;
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

            case line_num -> line.insert("" + line.getLinect());
                
            case tok_skip -> line.nextToken();
            case tok_skipall -> line.skipTokens();
            case tok_print -> System.err.format("token = %s%n", line.peekToken());
            case tok_dup -> {
                token = line.peekToken().checkNotEnd();
                line.insert(token);
            }
            case tok_swap -> {
                Token first = line.nextToken().checkNotEnd();
                Token second = line.nextToken().checkNotEnd();
                line.insert(first);
                line.insert(second);
            }
            default -> throw new EnumConstantNotPresentException(this.getClass(),this.name());
        }
    }

}
