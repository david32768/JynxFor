package jynx2asm;

import java.util.Arrays;
import java.util.EnumMap;

import static com.github.david32768.jynxfree.jynx.ReservedWord.*;

import static com.github.david32768.jynxfor.my.Message.M290;
import static com.github.david32768.jynxfor.my.Message.M291;
import static com.github.david32768.jynxfor.my.Message.M292;
import static com.github.david32768.jynxfor.my.Message.M326;
import static com.github.david32768.jynxfor.my.Message.M990;

import static com.github.david32768.jynxfree.jynx.Global.LOG;
import static com.github.david32768.jynxfree.jynx.Global.OPTION;

import com.github.david32768.jynxfor.code.JynxCodeNodeBuilder;
import com.github.david32768.jynxfor.instruction.LineInstruction;
import com.github.david32768.jynxfor.node.JynxInstructionNode;
import com.github.david32768.jynxfor.scan.Line;

import com.github.david32768.jynxfree.jynx.GlobalOption;
import com.github.david32768.jynxfree.jynx.ReservedWord;

public class InstList {

    private final JynxCodeNodeBuilder codeNode;
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
    
    public InstList(JynxCodeNodeBuilder codenode, StackLocals stacklocals,
            Line line, EnumMap<ReservedWord, Integer> options) {
        this.codeNode = codenode;
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

    public StackLocals getStackLocals() {
        return stackLocals;
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

    public void add(JynxInstructionNode insn) {
        if (addLineNumber && insn.canThrow()) {
            int lnum = line.getLinect() / 10;
            addInsn(new LineInstruction(lnum, line));    
            addLineNumber = false;
        }
        addInsn(insn);
    }

    private void addInsn(JynxInstructionNode insn) {
        if (expand) {
            LOG(M292,spacer,insn); // "%s  +%s"
        }
        boolean ok = stackLocals.visitInsn(insn, line);
        if (ok) {
            codeNode.addInstruction(insn);
            if (expand) {
                printStackLocals();
            }
        }
    }

    public void visitEnd() {
        if (!expand) {
            printStackLocals();
        }
    }
    
    public boolean isUnreachable() {
        return stackLocals.isUnreachable();
    }
    
}
