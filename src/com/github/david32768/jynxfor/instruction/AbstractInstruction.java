package com.github.david32768.jynxfor.instruction;


import com.github.david32768.jynxfor.ops.JvmOp;
import com.github.david32768.jynxfor.scan.Line;

public abstract class AbstractInstruction implements JynxInstruction {

    protected final JvmOp jvmop;
    private final Line line;

    protected AbstractInstruction(JvmOp jvmop, Line line) {
        assert jvmop != null;
        this.jvmop = jvmop;
        this.line = line;
    }

    @Override
    public JvmOp jvmop() {
        return jvmop;
    }

    @Override
    public Line line() {
        return line;
    }

}
