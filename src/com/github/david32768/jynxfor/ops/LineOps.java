package com.github.david32768.jynxfor.ops;

import com.github.david32768.jynxfor.scan.Line;
import com.github.david32768.jynxfor.scan.Token;
import com.github.david32768.jynxfor.scan.TokenArray;
import com.github.david32768.jynxfree.jynx.LogUnexpectedEnumValueException;

import com.github.david32768.jynxfree.jynx.ReservedWord;
import com.github.david32768.jynxfree.jynx.StringUtil;


public enum LineOps implements LineOp {

    line_num,
    
    tok_skip,
    tok_skipall,
    tok_swap,
    tok_dup,
    tok_multi,
    tok_print, // for debugging
    
    ;    
    
    private LineOps() {}

    @Override
    public void adjustLine(CurrentState state) {
        lineop(state.line());
    }

    private void lineop(Line line) {
        Token token;
        switch(this) {
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
            case tok_multi -> {
                Token next = line.peekToken().checkNotEnd();
                if (next.is(ReservedWord.dot_array)) {
                    String str = TokenArray.multiLineString(line);
                    str = StringUtil.enquote(str);
                    line.nextToken();
                    line.insert(str);
                }
            }
            default -> throw new LogUnexpectedEnumValueException(this);
        }
    }

}
