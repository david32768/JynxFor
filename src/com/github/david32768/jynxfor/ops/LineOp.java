package com.github.david32768.jynxfor.ops;

import static com.github.david32768.jynxfree.jynx.Global.CHECK_SUPPORTS;

import com.github.david32768.jynxfree.jvm.JvmVersioned;

@FunctionalInterface
public interface LineOp extends JynxOp {

    public void adjustLine(CurrentState state);

    public static LineOp checkVersion(JvmVersioned version) {
        return _ -> CHECK_SUPPORTS(version);
    }
}
