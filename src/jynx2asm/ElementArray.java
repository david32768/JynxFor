package jynx2asm;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

import static com.github.david32768.jynxfor.my.Message.M276;

import com.github.david32768.jynxfree.jynx.LogIllegalArgumentException;
import com.github.david32768.jynxfree.jynx.ReservedWord;

class ElementArray implements TokenArray {

    private Line line;
    private Deque<Token> current;
    
    ElementArray(Line line) {
        assert Objects.nonNull(line);
        this.line = line;
        this.current = null;
    }

    @Override
    public Line line() {
        return line;
    }

    @Override
    public void close() {
        this.current = null;
        this.line = null;
    }

    private Deque<Token>  readArray(Line line) {
        Deque<Token> tokens = new ArrayDeque<>();

        while(true) {
            Token token = line.nextToken();
            if (token.isEndToken()) {
                break;
            }
            tokens.addLast(token);
        }

        if (tokens.isEmpty()) {
            // "empty element in  array"
            throw new LogIllegalArgumentException(M276);
        } else {
            tokens.addLast(Token.END_TOKEN);
        }
        return tokens;
    }
    
    @Override
    public boolean isMultiLine() {
        return false;
    }
    
    @Override
    public Token firstToken() {
        if (current != null) {
            noMoreTokens();
            line = null;
            return Token.getInstance(ReservedWord.right_array.externalName());
        }
        current = readArray(line);
        return current.removeFirst();
    }

    @Override
    public Deque<Token> getDeque() {
        return current;
    }
   
}
