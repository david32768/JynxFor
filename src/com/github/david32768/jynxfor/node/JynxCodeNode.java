package com.github.david32768.jynxfor.node;

import java.util.List;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

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
    
    public JynxInstructionNode getInstruction(int index) {
        return instructionList.get(index);
    }
    
    public void accept(MethodVisitor mv) {
        mv.visitCode();
        for (var jcatch : catches) {
            jcatch.accept(mv);
        }
        Label startlabel = ((LabelInstruction)instructionList.getFirst()).jynxlab().asmlabel();
        for (var instruction : instructionList) {
            JynxInstructionNode.accept(instruction, mv);
        }
        Label endlabel = ((LabelInstruction)instructionList.getLast()).jynxlab().asmlabel();
        for (var jvar : varList) {
            jvar.accept(mv, startlabel, endlabel);
        }
        varAnnotations.accept(mv);
        // must be called to generate stackmap etc.
        mv.visitMaxs(maxStack, maxLocal);
    }
    
}
