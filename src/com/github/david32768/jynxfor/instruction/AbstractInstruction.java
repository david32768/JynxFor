package com.github.david32768.jynxfor.instruction;

import org.objectweb.asm.MethodVisitor;

import com.github.david32768.jynxfor.node.JynxAnnotationsNode;
import com.github.david32768.jynxfor.node.JynxInstructionNode;
import com.github.david32768.jynxfor.ops.JvmOp;
import com.github.david32768.jynxfor.scan.Line;

public abstract class AbstractInstruction implements JynxInstructionNode {

    protected final JvmOp jvmop;
    private final Line line;
    private final JynxAnnotationsNode annotations;

    protected AbstractInstruction(JvmOp jvmop, Line line) {
        assert jvmop != null;
        this.jvmop = jvmop;
        this.line = line;
        this.annotations = new JynxAnnotationsNode();
    }

    @Override
    public JvmOp jvmop() {
        return jvmop;
    }

    @Override
    public Line line() {
        return line;
    }

    @Override
    public final JynxAnnotationsNode annotations() {
        return annotations;
    }

    public abstract void accept(MethodVisitor mv);
    
}
