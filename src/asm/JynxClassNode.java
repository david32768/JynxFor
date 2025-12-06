package asm;

import java.io.PrintWriter;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import org.objectweb.asm.tree.analysis.Interpreter;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ModuleNode;
import org.objectweb.asm.tree.RecordComponentNode;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.TraceClassVisitor;

import static com.github.david32768.jynxfor.my.Message.*;
import static com.github.david32768.jynxfree.jynx.Global.*;
import static com.github.david32768.jynxfree.jynx.ClassType.RECORD;
import static com.github.david32768.jynxfree.jynx.ClassType.VALUE_RECORD;
import static com.github.david32768.jynxfree.jynx.GlobalOption.TRACE;

import com.github.david32768.jynxfor.node.JynxCodeNode;
import com.github.david32768.jynxfor.scan.Line;
import com.github.david32768.jynxfor.verify.Resolver;
import com.github.david32768.jynxfree.jvm.Feature;
import com.github.david32768.jynxfree.jynx.Access;
import com.github.david32768.jynxfree.jynx.ClassType;
import com.github.david32768.jynxfree.jynx.Directive;
import com.github.david32768.jynxfree.jynx.GlobalOption;

import jynx2asm.ClassChecker;
import jynx2asm.ObjectLine;

public abstract class JynxClassNode {

    private final ClassVisitor cv;

    private final Access accessName;
 
    private final ClassChecker checker;

    protected JynxClassNode(Access accessname, ClassVisitor basecv) {
        if (OPTION(TRACE)) {
            Printer printer = new ASMifier();
            PrintWriter pw = new PrintWriter(System.out);
            TraceClassVisitor tcv = new TraceClassVisitor(basecv, printer, pw);
            this.cv = new CheckClassAdapter(tcv, false);
        } else {
            this.cv = SUPPORTS(Feature.value)?
                    basecv:
                    new CheckClassAdapter(basecv, false);
        }
        this.accessName = accessname;
        this.checker = ClassChecker.getInstance(accessname);
    }
    
    public static JynxClassNode getInstance(Access accessname, Resolver resolver) {
        boolean usestack = OPTION(GlobalOption.USE_STACK_MAP);
        return ASMClassNode.getInstance(accessname, usestack, resolver);
    }

    public abstract byte[] toByteArray();

    public String getClassName() {
        return accessName.name();
    }
    
    public JynxClassHdr getJynxClassHdr(ObjectLine<String> source, String defaultsource, Resolver resolver) {
        return JynxClassHdr.getInstance(accessName, source, defaultsource, resolver);
    }
    
    public JynxMethodNode getJynxMethodNode(Line line) {
        return  JynxMethodNode.getInstance(line,checker);
    }

    public JynxFieldNode getJynxFieldNode(Line line) {
         return JynxFieldNode.getInstance(line,checker);
    }
    
    public JynxComponentNode getJynxComponentNode(Line line) {
        ClassType classtype = accessName.classType();
        if (classtype != RECORD && classtype != VALUE_RECORD) {
            LOG(M41);    // "component can only appear in a record"
        }
        JynxComponentNode jcn = JynxComponentNode.getInstance(line);
        checker.checkComponent(jcn);
        return jcn;
    }

    public void visitEnd() {
        checker.visitEnd();
        cv.visitEnd();
    }

    public void acceptClassHdr(JynxClassHdr jclasshdr) {
        ASMClassHeaderNode hdrnode = jclasshdr.endHeader();
        if (hdrnode != null) {
            if (hdrnode.interfaces != null) {
                checker.hasImplements();
            }
            checker.setSuper(hdrnode.superName);
            hdrnode.accept(cv);
        }
    }
    
    public void acceptComponent(JynxComponentNode jcompnode, Directive dir) {
        RecordComponentNode compnode = jcompnode.visitEnd(dir);
        if (compnode == null) {
            return;
        }
        compnode.accept(cv);
    }
    
    public void acceptField(JynxFieldNode jfieldnode, Directive dir) {
        FieldNode fnode = jfieldnode.visitEnd(dir);
        if (fnode == null) {
            return;
        }
        fnode.accept(cv);
    }
    
    public void acceptMethod(JynxMethodNode jmethodnode, JynxCodeNode codenode) {
        MethodNode mnode = jmethodnode.visitEnd(codenode);
        if (mnode == null) {
            return;
        }
        boolean verified = false;
        String verifiername;
        Interpreter<BasicValue> asmverifier = new BasicVerifier();
        verifiername = GlobalOption.BASIC_VERIFIER.name();
        Analyzer<BasicValue> analyzer = new Analyzer<>(asmverifier);
        try {
            analyzer.analyze(accessName.name(), mnode);
            verified =  true;
        } catch (AnalyzerException e) {
            String emsg = e.getMessage();
            var line = analyseExceptionMsg(emsg, codenode);
            if (line == null) {
                // "Method %s failed %s check:%n    %s"
                LOG(e, M75, mnode.name, verifiername, emsg);
            } else {
                // "Method %s failed %s check:%n    %s"
                LOG(e, line.toString(), M75, mnode.name, verifiername, emsg.substring(emsg.indexOf(':') + 1));                
            }
        } catch (IllegalArgumentException e) {
            String emsg = e.getMessage();
            // "Method %s failed %s check:%n    %s"
            LOG(e, M75, mnode.name, verifiername, emsg);
        }
        if (verified) {
            try {
                mnode.accept(cv);
            } catch (TypeNotPresentException ex) {
                LOG(M411,ex.typeName()); // "type %s not found"
            }
        }
    }
    
    private final static String INSTRUCTION = "instruction";
    
    private Line analyseExceptionMsg(String msg, JynxCodeNode codenode) {
        if (msg != null && codenode != null && msg.contains(INSTRUCTION)) {
            String rest = msg.substring(msg.indexOf(INSTRUCTION));
            String[] words = rest.replace(':', ' ').split(" ");
            assert words[0].equals(INSTRUCTION);
            try {
                int index = Integer.parseInt(words[1]);
                var insn = codenode.getInstruction(index);
                return insn.line();
            } catch (NumberFormatException | IndexOutOfBoundsException ex) {}
        }
        return null;
    }
    
    public void acceptModule(JynxModule jmodule) {
        ModuleNode modnode = jmodule.visitEnd();
        if (modnode == null) {
            return;
        }
        modnode.accept(cv);
    }

}
