package com.github.david32768.jynxfor.node;

import java.util.List;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import com.github.david32768.jynxfor.instruction.JynxInstruction;
import com.github.david32768.jynxfor.instruction.LabelInstruction;

public class JynxCodeNode {

    private final List<JynxCatchNode> catches;
    private final List<JynxInstructionNode> instructionList;
    private final List<JynxVarNode> varList;    
    private final JynxAnnotationsNode varAnnotations;    
    private final int maxStack;
    private final int maxLocal;

    public JynxCodeNode(List<JynxCatchNode> catches,
            List<JynxInstructionNode> instructionList,
            List<JynxVarNode> varList,
            JynxAnnotationsNode varAnnotations,
            int maxStack, int maxLocal) {
        this.catches = List.copyOf(catches);
        this.instructionList = List.copyOf(instructionList);
        this.varList = List.copyOf(varList);
        this.varAnnotations = varAnnotations;
        this.maxLocal = maxLocal;
        this.maxStack = maxStack;
    }
    
    public JynxInstruction getInstruction(int index) {
        return instructionList.get(index).instruction();
    }
    
    public void accept(MethodVisitor mv) {
        mv.visitCode();
        for (var jcatch : catches) {
            jcatch.accept(mv);
        }
        Label startlabel = ((LabelInstruction)instructionList.getFirst().instruction()).jynxlab().asmlabel();
        for (var instruction : instructionList) {
            instruction.accept(mv);
        }
        Label endlabel = ((LabelInstruction)instructionList.getLast().instruction()).jynxlab().asmlabel();
        for (var jvar : varList) {
            jvar.accept(mv, startlabel, endlabel);
        }
        varAnnotations.accept(mv);
        // must be called to generate stackmap etc.
        mv.visitMaxs(maxStack, maxLocal);
    }
    
}
