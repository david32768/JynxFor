package jynx2asm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.objectweb.asm.Label;

import static com.github.david32768.jynxfor.my.Message.*;
import static com.github.david32768.jynxfree.jynx.Global.*;

import jynx2asm.frame.JynxLabelFrame;
import jynx2asm.frame.LocalFrame;
import jynx2asm.frame.OperandStackFrame;

public class JynxLabel {

    private final String name;
    private final ArrayList<Line> usedList;
    private final ArrayList<Line> weakList;
    private final Label asmlab;
    private final Map<Integer,Integer> usedAtMap;

    private Line defined;
    private boolean usedInCode;
    private JynxLabelFrame jlf;
    private boolean startBlock;
    
    private int minLimit;
    private int maxLimit;
    
    public JynxLabel(String name) {
        this.name = name;
        this.defined = null;
        this.usedInCode = false;
        this.usedList = new ArrayList<>();
        this.weakList = new ArrayList<>();
        this.asmlab = new Label();
        this.jlf = new JynxLabelFrame(name);
        this.minLimit = Integer.MAX_VALUE;
        this.maxLimit = Integer.MIN_VALUE;
        this.usedAtMap = new HashMap<>();
        this.startBlock = false;
    }
    
    public boolean isDefined() {
        return defined != null;
    }

    public boolean isUnused() {
        return isDefined() && usedList.isEmpty() && weakList.isEmpty();
    }

    public boolean isUsedInCode() {
        return usedInCode;
    }

    public boolean isLessThan(JynxLabel after) {
        return this.isDefined() && after.isDefined() && this.definedLine().getLinect() < after.definedLine().getLinect();
    }

    public boolean isStartBlock() {
        return startBlock;
    }

    public void setStartBlock() {
        this.startBlock = true;
    }
    
    public void setOffset(int minoffset, int maxoffset) {
        assert !hasOffset();
        assert minoffset >= 0 && maxoffset >= minoffset;
        this.minLimit = Math.max(0, maxoffset - Short.MAX_VALUE);
        this.maxLimit = Math.max(0 - Short.MIN_VALUE, minoffset - Short.MIN_VALUE);
    }
    
    public void define(Line line) {
        if (isDefined()) {
            LOG(M36,line);   // "label already defined in line:%n  %s"
            return;
        }
        defined = line;
    }

    public boolean hasOffset() {
        return this.maxLimit > 0;
    }
    
    public boolean isDefinitelyNotWide(int minoffset, int maxoffset) {
        return hasOffset() && maxoffset <= maxLimit && minoffset >= minLimit;
    }
    
    public boolean isDefinitelyWide(int minoffset, int maxoffset) {
        return hasOffset() && (minoffset > maxLimit || maxoffset < minLimit);
    }
    
    public Line definedLine() {
        return defined;
    }
    
    public void addUsed(Line line) {
        usedList.add(line);
    }
    
    public void addCodeUsed(Line line) {
        addUsed(line);
        usedInCode = true;
    }
    
    public void addWeakUsed(Line line) {
        weakList.add(line);
    }
    
    public void usedAt(int minoffset, int maxoffset, int adjust) {
        if (!hasOffset() && adjust != 0) {
            Integer before = usedAtMap.put(minoffset, adjust);
            assert before == null;
        }
    }

    public int forwardBranchAdjustment() {
        int adjust = 0;
        for (Map.Entry<Integer,Integer> me:usedAtMap.entrySet()) {
            int minoffset = me.getKey();
            if (isDefinitelyNotWide(minoffset, minoffset)) {
                adjust += me.getValue();
            }
        }
        usedAtMap.clear();
        return adjust;
    }
    
    public String name() {
        return name;
    }

    public String base() {
        return jlf.name();
    }
    
    public Label asmlabel() {
        return asmlab;
    }
    
    public Stream<Line> used() {
        return usedList.stream();
    }
    
    public void visitCatch(List<JynxCatch> catchlist) {
        for (JynxCatch jcatch:catchlist) {
            JynxLabel fromref = jcatch.fromLab();
            JynxLabel toref = jcatch.toLab();
            if (!this.equals(fromref) ) {
                updateLocal(fromref.getLocals());
            }
            updateLocal(toref.getLocalsBefore());
        }
    }
    
    public void aliasOf(JynxLabel base) {
        jlf = base.jlf.merge(jlf);
        assert base.isDefined() && isDefined();
        base.usedInCode |= usedInCode;
        base.usedList.addAll(usedList);
        base.weakList.addAll(weakList);
    }
    
    public void updateLocal(LocalFrame osfx) {
        jlf.updateLocal(osfx);
    }

    public LocalFrame getLocals() {
        return jlf.locals();
    }

    public OperandStackFrame getStack() {
        return jlf.stack();
    }

    public void updateStack(OperandStackFrame osf) {
        jlf.updateStack(osf);
    }
    
    public void setLocalsFrame(LocalFrame osfx) {
        jlf.setLocalsFrame(osfx);
    }

    public void setLocalsBefore(LocalFrame osfLocalsBefore) {
        jlf.setLocalsBefore(osfLocalsBefore);
    }

    public LocalFrame getLocalsBefore() {
        return jlf.localsBefore();
    }
    
    public void load(FrameElement fe, int num) {
        jlf.load(fe, num);
    }
    
    public void store(FrameElement fe, int num) {
        jlf.store(fe, num);
    }
    
    public void freeze() {
        assert isDefined();
        jlf.freeze();
    }

    public String printLabelFrame() {
        return jlf.print();
    }

    @Override
    public String toString() {
        return jlf.getNameAliases();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof JynxLabel that) {
            return this.name.equals(that.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
    
}
