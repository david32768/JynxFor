package com.github.david32768.jynxfor.code;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.objectweb.asm.tree.LocalVariableAnnotationNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

import static com.github.david32768.jynxfor.my.Message.M335;
import static com.github.david32768.jynxfor.my.Message.M345;
import static com.github.david32768.jynxfor.my.Message.M346;
import static com.github.david32768.jynxfor.my.Message.M347;

import static com.github.david32768.jynxfree.jvm.StandardAttribute.LocalVariableTypeTable;
import static com.github.david32768.jynxfree.jvm.StandardAttribute.Signature;
import static com.github.david32768.jynxfree.jynx.Global.LOG;
import static com.github.david32768.jynxfree.jynx.ReservedWord.res_from;
import static com.github.david32768.jynxfree.jynx.ReservedWord.res_is;
import static com.github.david32768.jynxfree.jynx.ReservedWord.res_signature;
import static com.github.david32768.jynxfree.jynx.ReservedWord.res_to;


import com.github.david32768.jynxfor.instruction.LabelInstruction;
import com.github.david32768.jynxfor.node.JynxAnnotationsNode;
import com.github.david32768.jynxfor.node.JynxCatchNode;
import com.github.david32768.jynxfor.node.JynxCodeNode;
import com.github.david32768.jynxfor.node.JynxInstructionNode;
import com.github.david32768.jynxfor.node.JynxVarNode;
import com.github.david32768.jynxfor.node.LabelRange;
import com.github.david32768.jynxfor.ops.JvmOp;
import com.github.david32768.jynxfor.scan.Line;
import com.github.david32768.jynxfor.scan.Token;

import com.github.david32768.jynxfree.jvm.TypeRef;
import com.github.david32768.jynxfree.jynx.Global;
import com.github.david32768.jynxfree.jynx.NameDesc;

import jynx2asm.JynxLabel;
import jynx2asm.JynxLabelMap;
import jynx2asm.StackLocals;

public class JynxCodeNodeBuilder {
    
    private final List<JynxCatchNode> catches;
    private final List<JynxInstructionNode> instructionList;
    private final List<JynxVarNode> varList;    
    private final JynxAnnotationsNode varAnnotations;    
    
    public JynxCodeNodeBuilder() {
        this.catches = new ArrayList<>();
        this.instructionList = new ArrayList<>();
        this.varList = new ArrayList<>();
        this.varAnnotations = new JynxAnnotationsNode();
    }
    

    public void addCatch(JynxCatchNode jcatch) {
        catches.add(jcatch);
    }
    
    public void addInstruction(JynxInstructionNode insn) {
        instructionList.add(insn);
    }
    
    public void addCatchAnnotation(TypeAnnotationNode annotation, boolean visible) {
        int index = TypeRef.getIndexFrom(annotation.typeRef);
        assert index >= 0;
        if (index < catches.size()) {
            catches.get(index).addAnnotation(annotation, visible);
        } else {
            // "%d is not a current valid try index: current valid range is [0,%d]"
            LOG(M335, index, catches.size() - 1);
        }
    }
    
    public void addAnnotationToLastInstruction(TypeAnnotationNode annotation, boolean visible) {
        if (instructionList.isEmpty()) {
            // "no instruction preceding instruction annotation"
            LOG(M345);
        } else {
            instructionList.getLast().addTypeAnnotation(annotation, visible);
        }
    }        

    public void addVar(Line line, JynxLabelMap labelmap) {
        int varnum = line.nextToken().asUnsignedShort();
        String name = line.after(res_is);
        String desc = line.nextToken().asString();
        Optional<String> vsignature = line.optAfter(res_signature);
        JynxLabel fromref = null;
        JynxLabel toref = null;
        Token token = line.peekToken();
        if (!token.isEndToken()) {
            String fromname = line.after(res_from);
            fromref = labelmap.useOfJynxLabel(fromname, line);
            String toname = line.after(res_to);
            toref = labelmap.useOfJynxLabel(toname, line);
        }
        line.noMoreTokens();
        vsignature.ifPresent(sig -> {
                Global.CHECK_SUPPORTS(LocalVariableTypeTable);
                Global.CHECK_SUPPORTS(Signature);
                NameDesc.FIELD_SIGNATURE.validate(sig);
            });
        var jvar = new JynxVarNode(varnum, name, desc, vsignature.orElse(null), fromref, toref, line);
        varList.add(jvar);
    }
    
    public void addVar(int varnum, String name, String desc) {
        var jvar = new JynxVarNode(varnum, name, desc, null, null, null, Line.GENERATED);
        varList.add(jvar);
    }

    public void addVarAnnotation(LocalVariableAnnotationNode annotation, boolean visible) {
        varAnnotations.addAnnotation(annotation, visible);
    }
    
    public void addFrameToLabel(Object[] stackArray, Object[] localArray) {
        if (!instructionList.isEmpty() && instructionList.getLast() instanceof LabelInstruction labinst) {
            labinst.setFrame(stackArray, localArray);
        } else {
            //  "stack frame does not follow a label"
            LOG(M346);
        }
    }
    
    public JynxCodeNode build(StackLocals stacklocals) {
        int maxstack = stacklocals.stack().max();
        int maxlocal = stacklocals.locals().max();
        var validvars = varList.stream()
                .filter(LabelRange::isValid)
                .filter(stacklocals::visitVarDirective)
                .toList();
        var validcatches = catches.stream()
                .filter(LabelRange::isValid)
                .toList();
        boolean hasop = instructionList.stream()
                .map(inst -> inst.jvmop())
                .anyMatch(jvmop -> jvmop.opcode() >= 0);
        if (!hasop) {
            // "method has no instructions"
            LOG(M347);
            return null;
        }
        if (!(instructionList.getFirst() instanceof LabelInstruction)) {
            JynxLabel label = new JynxLabel(":start");
            LabelInstruction labinst = new LabelInstruction(JvmOp.xxx_label, label, Line.GENERATED);
            instructionList.addFirst(labinst);
        }
        if (!(instructionList.getLast() instanceof LabelInstruction)) {
            JynxLabel label = new JynxLabel(":end");
            LabelInstruction labinst = new LabelInstruction(JvmOp.xxx_label, label, Line.GENERATED);
            instructionList.addLast(labinst);            
        }
        return new JynxCodeNode(validcatches, instructionList,
                validvars, varAnnotations, maxstack, maxlocal);
    }
        
}
