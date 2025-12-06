package com.github.david32768.jynxfor.node;

import static com.github.david32768.jynxfor.my.Message.M217;
import static com.github.david32768.jynxfree.jynx.Global.LOG;

import com.github.david32768.jynxfor.scan.Line;

import jynx2asm.JynxLabel;

public interface LabelRange {
    public JynxLabel from();
    public JynxLabel to();
    public Line line();

    public static boolean isValid(LabelRange range) {
        var from = range.from();
        var to = range.to();
        if (from == null || to == null || from.isLessThan(to)) {
            return true;
        } else {
            var line = range.line();
            //"from label %s is not before to label %s"
            LOG(line.toString(), M217, from.name(), to.name());
            return false;
        }        
    }
}
