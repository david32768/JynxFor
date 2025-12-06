package jynx2asm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static com.github.david32768.jynxfor.my.Message.M106;
import static com.github.david32768.jynxfor.my.Message.M217;
import static com.github.david32768.jynxfor.my.Message.M284;
import static com.github.david32768.jynxfree.jynx.Global.LOG;

import com.github.david32768.jynxfor.node.JynxCatchNode;
import com.github.david32768.jynxfor.scan.Line;

import com.github.david32768.jynxfree.jynx.Directive;
import com.github.david32768.jynxfree.jynx.LogIllegalArgumentException;
import com.github.david32768.jynxfree.jynx.NameDesc;

public class JynxLabelMap {
    
    private final Map<String, JynxLabel> labelmap;
    private final Map<JynxCatchNode,Line> catches;
    
    public JynxLabelMap() {
        this.labelmap = new HashMap<>();
        this.catches = new HashMap<>();
    }

    public JynxLabel useOfJynxLabel(String name, Line line) {
        NameDesc.LABEL.validate(name);
        JynxLabel lab = labelmap.computeIfAbsent(name, k -> new JynxLabel(name));
        lab.addUsed(line);
        return lab;
    }
    
    public String printJynxlabelFrame(String name,Line line) {
        NameDesc.LABEL.validate(name);
        JynxLabel lab = labelmap.get(name);
        if (lab == null) {
            //"label %s is not (yet?) known"
            throw new LogIllegalArgumentException(M284,name);
        } else {
            return lab.printLabelFrame();
        }
    }

    public JynxLabel codeUseOfJynxLabel(String name, Line line) {
        NameDesc.LABEL.validate(name);
        JynxLabel lab = labelmap.computeIfAbsent(name, k -> new JynxLabel(name));
        lab.addCodeUsed(line);
        return lab;
    }

    public JynxLabel weakUseOfJynxLabel(JynxLabel lastlab, Line line) {
        lastlab.addWeakUsed(line);
        return lastlab;
    }

    public JynxLabel defineJynxLabel(String name, Line line) {
        NameDesc.LABEL.validate(name);
        JynxLabel lab = labelmap.computeIfAbsent(name, k -> new JynxLabel(name));
        lab.define(line);
        return lab;
    }

    public JynxLabel defineWeakJynxLabel(String name, Line line) {
        NameDesc.LABEL.validate(name);
        JynxLabel lab = labelmap.get(name);
        if (lab == null || lab.isDefined()) {
            return null;
        }
        return defineJynxLabel(name, line);
    }

    public void aliasJynxLabel(String alias, Line line, JynxLabel base) {
        NameDesc.LABEL.validate(alias);
        base.addWeakUsed(line);
        labelmap.put(alias,base);
    }

    public void checkCatch() {
        for (Map.Entry<JynxCatchNode,Line>  me:catches.entrySet()) {
            JynxCatchNode jcatch = me.getKey();
            Line line = me.getValue();
            JynxLabel from = getBase(jcatch.from());
            JynxLabel to = getBase(jcatch.to());
            if (!from.isLessThan(to)) {
                // "from label %s is not before to label %s"
                LOG(line.toString(), M217, from, to);
            }
        }
    }
   
    private JynxLabel getBase(JynxLabel label) {
        return labelmap.get(label.base());
    }
    
    public List<JynxCatchNode> getCatches(JynxLabel label) {
        label = getBase(label);
        List<JynxCatchNode> result = new ArrayList<>(); 
        for (Map.Entry<JynxCatchNode,Line>  me:catches.entrySet()) {
            JynxCatchNode jcatch = me.getKey();
            JynxLabel using = getBase(jcatch.usingLab());
            if (using.equals(label)) {
                result.add(jcatch);
            }
        }
        return result;
    }
    
    public List<JynxCatchNode.LabelRecord> startTry(JynxLabel label) {
        return catches.keySet().stream()
                .map(JynxCatchNode::labels)
                .filter(c -> c.from().equals(label))
                .toList();
    }
    
    public List<JynxCatchNode.LabelRecord> endTry(JynxLabel label) {
        return catches.keySet().stream()
                .map(JynxCatchNode::labels)
                .filter(c -> c.to().equals(label))
                .toList();
    }
    
    public boolean isEndTry(JynxLabel label) {
        return catches.keySet().stream()
                    .anyMatch(c -> c.to().equals(label));
    }
   
    public Set<JynxLabel> getThrowsTo(JynxLabel label) {
        label = getBase(label);
        Set<JynxLabel> result = new HashSet<>();
        for (Map.Entry<JynxCatchNode,Line>  me:catches.entrySet()) {
            JynxCatchNode jcatch = me.getKey();
            JynxLabel from = getBase(jcatch.from());
            JynxLabel to = getBase(jcatch.to());
            if (from.isLessThan(label) && !to.isLessThan(label) && (!to.isStartBlock() || !to.equals(label))) {
                JynxLabel using = getBase(jcatch.usingLab());
                result.add(using);
            }
        }
        return result;
    }
    
    public Stream<JynxLabel> stream() {
        return labelmap.entrySet().stream()
                .filter(me->me.getKey().equals(me.getValue().name())) // remove aliases
                .map(me->me.getValue());
    }

    public JynxCatchNode getCatch(String fromname, String  toname,
            String usingname, String exception, Line line) {
        JynxLabel fromref = useOfJynxLabel(fromname, line);
        JynxLabel toref = useOfJynxLabel(toname, line);
        JynxLabel usingref = codeUseOfJynxLabel(usingname, line);
        if (!fromref.isDefined() && !toref.isDefined() && !usingref.isDefined()) {
            JynxCatchNode jcatch = new JynxCatchNode(fromref, toref, usingref,exception,line);
            catches.put(jcatch,line);
            return jcatch;
        } else {
            LOG(M106,Directive.dir_catch); // "labels in %s must not be defined yet"
            return null;
        }
    }

}
