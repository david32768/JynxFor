package jynx2asm.frame;

import java.util.ArrayList;
import java.util.List;

import static com.github.david32768.jynxfor.my.Message.*;
import static com.github.david32768.jynxfree.jynx.Global.*;

import com.github.david32768.jynxfree.jynx.LogIllegalArgumentException;
import com.github.david32768.jynxfree.jynx.ReservedWord;

import jynx2asm.FrameElement;

public class JynxLabelFrame {

    private final String name;
    private final List<String> aliasList;
    
    private LocalFrame locals;
    private OperandStackFrame stack;

    private LocalFrame localsInFrame;

    private LocalFrame localsBefore;
    private AfterFrameArray afterframe;

    public JynxLabelFrame(String name) {
        this.name = name;
        this.locals = null;
        this.localsInFrame = null;
        this.localsBefore = null;
        this.aliasList = new ArrayList<>();
        this.afterframe = null;
    }

    public JynxLabelFrame merge(JynxLabelFrame  alias) {
        assert alias.localsInFrame == null;
        assert alias.localsBefore == null;
        assert alias.afterframe == null;
        updateLocal(alias.locals);
        updateStack(alias.stack);
        aliasList.add(alias.name);
        assert alias.aliasList.isEmpty();
        return this;
    }

    public String name() {
        return name;
    }
    
    public LocalFrame locals() {
        return locals;
    }

    public OperandStackFrame stack() {
        return stack;
    }
    
    public String getNameAliases() {
        if (aliasList.isEmpty()) {
            return name;
        }
        return String.format("%s ( alias %s )",name,aliasList);
    }
    
    public void updateLocal(LocalFrame osfx) {
        if (osfx == null) {
            return;
        }
        if (isFrozen()) {
            LocalFrame.checkLabel(locals, osfx, afterframe.asLocalFrame());
        } else {
            locals = LocalFrame.combine(locals, osfx);
        }
        if (localsInFrame != null && locals != null && !localsInFrame.isCompatibleWith(locals)) {
            // "frame locals %s incompatible with current locals %s"
            throw new LogIllegalArgumentException(M216,localsInFrame,locals);
        }
    }

    private boolean isFrozen() {
        return afterframe != null;
    }
    
    public void updateStack(OperandStackFrame osf) {
        if (osf == null) {
            return;
        }
        if (stack == null) {
            stack = osf;
        } else if (!stack.isEquivalent(osf)) {
            LOG(M185,ReservedWord.res_stack,name,stack,osf); // "%s required for label %s is %s but currently is %s"
        }
    }
    
    public void setLocalsFrame(LocalFrame osfx) {
        localsInFrame = osfx;
        updateLocal(osfx);
    }

    public void setLocalsBefore(LocalFrame localsBefore) {
        this.localsBefore = localsBefore;
    }

    public LocalFrame localsBefore() {
        return localsBefore;
    }
    
    public void load(FrameElement fe, int num) {
        assert isFrozen();
        afterframe.set(num, fe);
    }
    
    public void store(FrameElement fe, int num) {
        assert isFrozen();
        afterframe.set(num, FrameElement.ANY);
        if (fe.isTwo()) {
            afterframe.set(num+1,FrameElement.ANY);
        }
    }
    
    public void freeze() {
        afterframe = new AfterFrameArray(locals.size());
    }
    
    public String print() {
        return String.format("label %s locals = %s stack = %s%n", name,locals,stack);
    }
}
