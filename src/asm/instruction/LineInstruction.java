package asm.instruction;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static com.github.david32768.jynxfor.my.Message.M34;
import static com.github.david32768.jynxfor.my.Message.M800;
import static com.github.david32768.jynxfree.jynx.Global.LOG;

import com.github.david32768.jynxfor.ops.JvmOp;
import com.github.david32768.jynxfree.jvm.NumType;

import jynx2asm.StackLocals;

public class LineInstruction extends Instruction {

    private final int lineNum;

    private static final int LINE_NUMBER_MOD = 50000; // 50000 for easy human calculation
    
    public LineInstruction(int linenum) {
        super((JvmOp)null);
        if (linenum == 0) {
            // "line number 0 changed to 1; ASM Issue #317989"
            LOG(M800);
            linenum = 1;
        }
        if (!NumType.t_short.isInUnsignedRange(linenum)) {
            // "some line numbers have been reduced mod %d as exceed unsigned short max"
            LOG(M34,LINE_NUMBER_MOD);
            linenum = linenum%LINE_NUMBER_MOD;
            if (linenum == 0) {
                linenum = LINE_NUMBER_MOD;
            }
        }
        this.lineNum = linenum;
    }

    @Override
    public void adjust(StackLocals stackLocals) {}

    @Override
    public void accept(MethodVisitor mv) {
        Label label = new Label();  //  to get multiple line numbers. eg in jdk3/ArtificialStructures
        mv.visitLabel(label);
        mv.visitLineNumber(lineNum, label);
    }

    @Override
    public String toString() {
        return String.format("line %d",lineNum);
    }

}
