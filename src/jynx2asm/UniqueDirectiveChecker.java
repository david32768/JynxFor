package jynx2asm;

import java.util.HashMap;
import java.util.Map;

import static com.github.david32768.jynxfor.my.Message.M31;

import com.github.david32768.jynxfree.jynx.Directive;
import com.github.david32768.jynxfree.jynx.LogIllegalStateException;

public class UniqueDirectiveChecker {

    private final Map<Directive,Line> directives;
    
    public UniqueDirectiveChecker() {
        this.directives = new HashMap<>();
    }

    public void checkUnique(Directive dir, Line line) {
        if (dir.isUniqueWithin()) {
            Line linex = directives.putIfAbsent(dir, line);
            if (linex != null) {
                throw new LogIllegalStateException(M31,this,linex); // "%s already set in line%n    %s"
            }
        }
    }
    
    public Line get(Directive dir) {
        return directives.get(dir);
    }
}
