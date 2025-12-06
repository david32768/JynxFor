package com.github.david32768.jynxfor.instruction;

import java.util.Collection;
import java.util.stream.Collectors;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import com.github.david32768.jynxfor.ops.JvmOp;
import com.github.david32768.jynxfor.scan.Line;
import com.github.david32768.jynxfree.jynx.ReservedWord;

import jynx2asm.JynxLabel;

public class TableInstruction extends SwitchInstruction {

    private final int min;
    private final int max;
    private final JynxLabel dflt;
    private final Collection<JynxLabel> labels;

    public TableInstruction(int min, int max, JynxLabel dflt, Collection<JynxLabel> labels, Line line) {
        super(JvmOp.asm_tableswitch, minsize(labels.size()), line);
        assert min <= max;
        this.min = min;
        this.max = max;
        this.dflt = dflt;
        this.labels = labels;
    }

    @Override
    public Collection<JynxLabel> labels() {
        return labels;
    }
    
    @Override
    public JynxLabel dfltLabel() {
        return dflt;
    }

    private static final int OVERHEAD = 1 + 4 + 4 + 4 ;
    
    public static final long minsize(long labelct) {
        return OVERHEAD + 4*labelct;
    }
    
    @Override
    public void accept(MethodVisitor mv) {
        Label[] asmlabels = labels.stream()
            .map(JynxLabel::asmlabel)
            .toArray(Label[]::new);
        mv.visitTableSwitchInsn(min, max, dflt.asmlabel(), asmlabels);
    }

    @Override
    public String toString() {
        String brlabels = labels.stream()
                .map(JynxLabel::name)
                .collect(Collectors.joining(" , "));
        return String.format("%s %d default %s %s %s %s",
                jvmop,min, dflt,ReservedWord.left_array,brlabels,ReservedWord.right_array);
    }

}
