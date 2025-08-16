package jynx2asm;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.function.Function;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static com.github.david32768.jynxfor.my.Message.*;
import static com.github.david32768.jynxfree.jynx.Global.*;

import com.github.david32768.jynxfree.jynx.Directive;
import com.github.david32768.jynxfree.jynx.LogIllegalArgumentException;

public class JynxScanner implements Iterator<Line> {

    private int linect;
    private Line line;
    private boolean reread;
    private int precomments;

    private final BufferedReader lines;
    private final Function<Line,TokenArray> arrayfn;

    private JynxScanner(BufferedReader  lines) {
        this.lines = lines;

        this.linect = 0;
        this.line = Line.EMPTY;
        this.reread = false;
        this.precomments = 0;
        this.arrayfn = (linex) -> TokenArray.getInstance(this, linex);
    }

    public int getPreCommentsCount() {
        return precomments;
    }

    public static JynxScanner getInstance(InputStream in) {
        JynxScanner js =  new JynxScanner(new BufferedReader(new InputStreamReader(in)));
        js.skipPreComments();
        return js;
    }
    
    public static JynxScanner getInstance(String str) {
        return new JynxScanner(new BufferedReader(new StringReader(str)));
    }
    
    public static JynxScanner getInstance(Path path) throws IOException {
        JynxScanner js = new JynxScanner(Files.newBufferedReader(path));
        js.skipPreComments();
        return js;
    }
    
    private String readLine() {
        try {
            String linestr = lines.readLine();
            if (linestr == null) {
                lines.close();
            }
            return linestr;
        } catch (IOException ioex) {
            throw new AssertionError(ioex);
        }
    }
    
    private void  skipPreComments() {
        String linestr;
        do {
            linestr = readLine();
            if (linestr == null) {
                // "no Jynx directives in file!"
                throw new LogIllegalArgumentException(M273);
            }
            ++linect;
            ++precomments;
        } while (!linestr.trim().startsWith(".")); // ignore lines until directive
        --precomments;
        line = Line.tokenise(linestr, linect, arrayfn);
        LOGGER().setLine(line.toString());
        reread = true;
    }
    
    public Line getLine() {
        return line;
    }

    private void nextLine() {
        assert line != null:M79.format(); // "Trying to read beyond end of file"
        line.noMoreTokens();
        String linestr;
        do {
            linestr = readLine();
            if (linestr == null) {
                line = null;
                return;
            }
            ++linect;
        } while (linestr.trim().length() == 0 || linestr.trim().startsWith(";")); // ignore empty lines and comments
        line = Line.tokenise(linestr, linect, arrayfn);
        LOGGER().setLine(line.toString());
    }

    @Override
    public boolean hasNext() {
        return line != null;
    }
    
    @Override
    public Line next() {
        if (line == null) {
            throw new NoSuchElementException();
        }
        if (reread) {
            LOGGER().setLine(line.toString());
            reread = false;
            return line;
        }
        nextLine();
        if (line == null) {
            return Line.tokenise(Directive.end_class.externalName(), Integer.MAX_VALUE, arrayfn);
        }
        return line;
    }
    
    public Line nextLineNotEnd(Directive enddir) {
        assert enddir.isEndDirective();
        nextLine();
        if (!line.isDirective()) {
            return line;
        }
        Token first = line.peekToken();
        Directive dir = first.asDirective();
        if (dir == enddir) {
            line.firstToken();
            line.noMoreTokens();
            return null;
        }
        LOG(M127,dir,enddir);    // "directive %s reached before %s"
        reread = true;
        return null;
    }
    
    public void skipNested(Directive startdir,Directive enddir, EnumSet<Directive> allowed) {
        assert enddir.isEndDirective();
        int nest = 1;
        while (nest > 0) {
            nextLine();
            if (!line.isDirective()) {
                skipTokens();
                continue;
            }
            Token first = line.peekToken();
            Directive dir = first.asDirective();
            if (dir == startdir) {
                ++nest;
            } else if (dir == enddir) {
                --nest;
            } else if (!allowed.contains(dir)) {
                LOG(M127,dir,enddir);    // "directive %s reached before %s"
                reread = true;
                return;
            }
            skipTokens();
        }
    }
  
    public void skipTokens() {
        line.skipTokens();
    }
    
}
