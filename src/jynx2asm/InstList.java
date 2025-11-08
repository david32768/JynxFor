package jynx2asm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;

import org.objectweb.asm.tree.MethodNode;

import static com.github.david32768.jynxfree.jynx.ReservedWord.*;

import static com.github.david32768.jynxfor.my.Message.M290;
import static com.github.david32768.jynxfor.my.Message.M291;
import static com.github.david32768.jynxfor.my.Message.M292;
import static com.github.david32768.jynxfor.my.Message.M326;
import static com.github.david32768.jynxfor.my.Message.M990;

import static com.github.david32768.jynxfree.jynx.Global.LOG;
import static com.github.david32768.jynxfree.jynx.Global.OPTION;

import com.github.david32768.jynxfor.instruction.JynxInstruction;
import com.github.david32768.jynxfor.instruction.LineInstruction;
import com.github.david32768.jynxfor.ops.JvmOp;
import com.github.david32768.jynxfor.scan.Line;
import com.github.david32768.jynxfor.scan.Token;

import com.github.david32768.jynxfree.jynx.GlobalOption;
import com.github.david32768.jynxfree.jynx.ReservedWord;

public class InstList {

    private final List<JynxInstruction> instructions;
    private final StackLocals stackLocals;
    private final Line line;
    private final String spacer;

    private final boolean expand;
    private final boolean stack;
    private final boolean locals;
    private final boolean offset;

    private boolean addLineNumber;
    
    private String stackb;
    private String localsb;
    
    public InstList(StackLocals stacklocals, Line line, EnumMap<ReservedWord, Integer> options) {
        this.instructions = new ArrayList<>();
        this.stackLocals = stacklocals;
        this.line = line;
        int indent = line.getIndent();
        char[] chars = new char[indent];
        Arrays.fill(chars, ' ');
        this.spacer = String.valueOf(chars);
        this.expand = options.containsKey(res_expand);
        this.stack = options.containsKey(res_stack);
        this.locals = options.containsKey(res_locals);
        this.offset = options.containsKey(res_offset);
        this.stackb = this.stack? stackLocals.stringStack(): "";
        this.localsb = this.locals? stackLocals.stringLocals(): "";
        if (!options.isEmpty()) {
            LOG(M990,line); // "%s"
        }
        this.addLineNumber = OPTION(GlobalOption.GENERATE_LINE_NUMBERS);
    }

    public Line getLine() {
        return line;
    }

    private void printStack() {
        String stacka = stackLocals.stringStack();
        LOG(M290, spacer,stackb,stacka); // ";%s  %s -> %s"
        stackb = stacka;
    }
    
    private void printLocals() {
        String localsa = stackLocals.stringLocals();
        if (!localsa.equals(localsb)) {
            LOG(M291,spacer,res_locals,localsa); // ";%s  %s = %s"
        }
        localsb = localsa;
    }

    private void printOffset() {
        // ";%s  offset = [%d,%d]"
        LOG(M326, spacer, stackLocals.getMinLength(),stackLocals.getMaxLength());
    }
    
    private void printStackLocals() {
        if (stack) {
            printStack();
        }
        if (locals) {
            printLocals();
        }
        if (offset) {
            printOffset();
        }
    }

    private void addInsn(JynxInstruction insn) {
        if (expand) {
            LOG(M292,spacer,insn); // "%s  +%s"
        }
        boolean ok = stackLocals.visitInsn(insn, line);
        if (ok) {
            instructions.add(insn);
            if (expand) {
                printStackLocals();
            }
        }
    }

    public void add(JynxInstruction insn) {
        if (addLineNumber && insn.needLineNumber()) {
            int lnum = line.getLinect();
            addInsn(new LineInstruction(lnum));    
            addLineNumber = false;
        }
        addInsn(insn);
    }

    public void accept(MethodNode mnode) {
        for (JynxInstruction in:instructions) {
            in.accept(mnode);
        }
        if (!expand) {
            printStackLocals();
        }
    }
    
    public FrameElement peekTOS() {
        return stackLocals.stack().peekTOS();
    }
    
    public FrameElement peekVarNum(Token token) {
        return stackLocals.locals().peekVarNumber(token);
    }
    
    public JvmOp getReturnOp() {
        return stackLocals.getReturnOp();
    }
    
    public boolean isUnreachable() {
        return stackLocals.isUnreachable();
    }
    
}
