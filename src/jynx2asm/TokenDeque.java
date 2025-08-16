package jynx2asm;

import java.util.Deque;
import java.util.EnumSet;
import java.util.Optional;

import static com.github.david32768.jynxfor.my.Message.M140;
import static com.github.david32768.jynxfor.my.Message.M402;
import static com.github.david32768.jynxfor.my.Message.M90;
import static com.github.david32768.jynxfree.jynx.Global.LOG;

import com.github.david32768.jynxfree.jvm.AccessFlag;
import com.github.david32768.jynxfree.jynx.LogIllegalStateException;
import com.github.david32768.jynxfree.jynx.ReservedWord;

public interface TokenDeque {

    public Token firstToken();

    public Deque<Token> getDeque();
    
    public default Token peekToken() {
        Token token = getDeque().peekFirst();
        if (token == null) {
            throw new LogIllegalStateException(M140);  // "reading next token after reaching last"
        } else {
            return token;
        }
    }

    public default Token nextToken() {
        Deque<Token> tokens = getDeque();
        if (tokens.isEmpty()) {
            throw new LogIllegalStateException(M140);  // "reading next token after reaching last"
        }
        return tokens.removeFirst();
    }

    public default void insert(Token insert) {
        if (insert.isEndToken()) {
            throw new LogIllegalStateException(M402);  // "cannot insert end_token"
        }
        getDeque().addFirst(insert);
    }

    public default void insert(String str) {
        getDeque().addFirst(Token.getInstance(str));
    }

    public default void insert(ReservedWord res) {
        getDeque().addFirst(Token.getInstance(res));
    }

    public default Token nextTokenSplitIfStart(ReservedWord... res) {
        Token token = nextToken();
        String tokstr = token.asString();
        for (ReservedWord rw:res) {
            String rwname = rw.externalName();
            if (!token.isEndToken() && !token.is(rw) && tokstr.startsWith(rwname)) {
                insert(Token.getInstance(tokstr.substring(rwname.length())));
                return Token.getInstance(rw);
            }
        }
        return token;
    }
    
    public default Token nextTokenSplitIfEnd(ReservedWord... res) {
        Token token = nextToken();
        String tokstr = token.asString();
        for (ReservedWord rw:res) {
            String rwname = rw.externalName();
            if (!token.isEndToken() && !token.is(rw) && tokstr.endsWith(rwname)) {
                insert(Token.getInstance(rw));
                return Token.getInstance(tokstr.substring(0, tokstr.length() - rwname.length()));
            }
        }
        return token;
    }
    
    public default void noMoreTokens() {
        Token token = getDeque().peekFirst();
        if (token == null || token.isEndToken()) {
        } else {
            LOG(M90,token);    // "unused tokens - starting at %s"
            skipTokens();
        }
    }

    public default void skipTokens() {
        getDeque().clear();
    }
    
    public default Token lastToken()  {
        Token token = nextToken();
        noMoreTokens();
        return token;
    }

    public default String after(ReservedWord rw)   {
        Token token = nextToken();
        token.mustBe(rw);
        return nextToken().asReservedWordType(rw.rwtype());
    }

    public default Optional<String> optAfter(ReservedWord rw) {
        assert rw.isOptional();
        Token token = peekToken();
        if (token.is(rw)) {
            nextToken();
            return Optional.of(nextToken().asReservedWordType(rw.rwtype()));
        }
        return Optional.empty();
    }

    public default EnumSet<AccessFlag> getAccFlags()  {
          EnumSet<AccessFlag> accflags = EnumSet.noneOf(AccessFlag.class);
          while (true) {
              Token token = peekToken();
              if (token.isEndToken()) {
                  break;
              }
              Optional<AccessFlag> afopt = AccessFlag.fromString(token.asString());
              if (afopt.isPresent()) {
                  nextToken();
                  accflags.add(afopt.get());
              } else {
                  break;
              }
          }
          return accflags;
    }

}
