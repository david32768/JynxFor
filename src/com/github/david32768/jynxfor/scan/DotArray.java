package com.github.david32768.jynxfor.scan;

import java.util.Deque;
import java.util.Objects;

import static com.github.david32768.jynxfree.jynx.Global.LOGGER;

import com.github.david32768.jynxfree.jynx.Directive;
import com.github.david32768.jynxfree.jynx.ReservedWord;

class DotArray implements TokenArray {

    private Line line;
    private final JynxScanner js;
    
    DotArray(JynxScanner js, Line line) {
        assert Objects.nonNull(line);
        assert Objects.nonNull(js);
        line.nextToken().mustBe(ReservedWord.dot_array);
        line.noMoreTokens();
        this.js = js;
        this.line = line;
        LOGGER().pushContext();
    }

    @Override
    public Line line() {
        return line;
    }

    @Override
    public boolean isMultiLine() {
        return true;
    }
    
    @Override
    public Token firstToken() {
        assert Objects.nonNull(line);
        line = js.nextLineNotEnd(Directive.end_array);
        if (line == null) {
            return Token.getInstance(ReservedWord.right_array.externalName());
        }
        return line.firstToken();
    }

    @Override
    public Deque<Token> getDeque() {
        Objects.nonNull(line);
        return line.getDeque();
    }

    @Override
    public void close() {
        if (line != null) {
            LOGGER().pushCurrent();
            while((line = js.nextLineNotEnd(Directive.end_array)) != null) {
                line.skipTokens();
            }
            LOGGER().popCurrent();
        }
        LOGGER().popContext();
    }
    
    
}
