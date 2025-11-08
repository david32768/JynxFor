package jynx2asm;

import java.io.File;
import java.util.EnumSet;
import java.util.Optional;

import static com.github.david32768.jynxfor.my.Message.*;
import static com.github.david32768.jynxfree.jynx.Global.*;

import static com.github.david32768.jynxfree.jynx.ClassType.MODULE_CLASS;
import static com.github.david32768.jynxfree.jynx.ClassType.PACKAGE;
import static com.github.david32768.jynxfree.jynx.Directive.dir_comment;
import static com.github.david32768.jynxfree.jynx.Directive.dir_module;
import static com.github.david32768.jynxfree.jynx.Directive.end_comment;
import static com.github.david32768.jynxfree.jynx.NameDesc.CLASS_NAME;

import com.github.david32768.jynxfor.my.JynxGlobal;
import com.github.david32768.jynxfor.ops.JynxOps;
import com.github.david32768.jynxfor.ops.JynxTranslator;
import com.github.david32768.jynxfor.scan.JynxScanner;
import com.github.david32768.jynxfor.scan.Line;
import com.github.david32768.jynxfor.scan.Token;
import com.github.david32768.jynxfor.verify.Resolver;
import com.github.david32768.jynxfor.verify.Verifier;

import com.github.david32768.jynxfree.jvm.AccessFlag;
import com.github.david32768.jynxfree.jvm.Constants;
import com.github.david32768.jynxfree.jvm.Feature;
import com.github.david32768.jynxfree.jvm.JvmVersion;
import com.github.david32768.jynxfree.jynx.Access;
import com.github.david32768.jynxfree.jynx.ClassType;
import com.github.david32768.jynxfree.jynx.Directive;
import com.github.david32768.jynxfree.jynx.DirectiveConsumer;
import com.github.david32768.jynxfree.jynx.Global;
import com.github.david32768.jynxfree.jynx.GlobalOption;
import com.github.david32768.jynxfree.jynx.LogAssertionError;
import com.github.david32768.jynxfree.jynx.MainOption;
import com.github.david32768.jynxfree.jynx.SevereError;
import com.github.david32768.jynxfree.jynx.State;

import asm.ContextDependent;
import asm.JynxClassHdr;
import asm.JynxClassNode;
import asm.JynxCodeHdr;
import asm.JynxComponentNode;
import asm.JynxFieldNode;
import asm.JynxMethodNode;
import asm.JynxModule;

public class JynxClass implements ContextDependent,DirectiveConsumer {

    private final JynxScanner js;
    private final String file_source;
    private final String defaultSource;
    
    private JvmVersion jvmVersion;
    private ObjectLine<String> source;

    private State state;

    private JynxClassNode jclassnode;
    private JynxClassHdr jclasshdr;
    private JynxComponentNode jcompnode;
    private JynxFieldNode jfieldnode;
    private JynxMethodNode jmethodnode;
    private JynxCodeHdr jcodehdr;
    private JynxModule jmodule;
    
    private ContextDependent sd;
    
    private final UniqueDirectiveChecker unique_checker;
    private JynxOps opmap;
    
    private final Resolver resolver;
    

    private JynxClass(String file_source, String default_source, JynxScanner js) {
        this.js = js;
        this.file_source = file_source;
        this.defaultSource = default_source;
        this.source = null;
        this.unique_checker = new UniqueDirectiveChecker();
        this.sd = this;
        this.resolver = new Resolver();
    }

    public static byte[] getBytes(String file_source, JynxScanner lines) {
        int index = file_source.lastIndexOf(File.separatorChar);
        String default_source = file_source.substring(index + 1);
        return getBytes(file_source, default_source, lines);
    }
    
    public static byte[] getBytes(String source, String default_source, JynxScanner lines) {
        try {
            JynxClass jclass =  new JynxClass(source, default_source, lines);
            boolean ok = jclass.assemble();
            if (!ok) {
                return null;
            }
            return jclass.toByteArray();
        } catch (RuntimeException rtex) {
            LOG(rtex);
            // "%s of %s failed because of %s"
            LOG(M123, MainOption.ASSEMBLY.name().toLowerCase(), source, rtex);
            return null;
        }
    }
    
    private boolean assemble() {
        int instct = 0;
        int labct = 0;
        int dirct = 0;
        while (js.hasNext()) {
            try {
                Line line = js.next();
                Directive dir;
                if (line.isDirective()) {
                    Token token = line.firstToken();
                    dir = token.asDirective();
                    dirct++;
                } else {
                    dir = Directive.state_opcode;
                    if (line.isLabel()) {
                        ++labct;
                    } else {
                        instct++;
                    }
                }
                state = dir.visit(this,state);
            } catch (IllegalArgumentException ex) {
                LOG(ex);
                js.skipTokens();    // use js as may not be original line
            } catch (SevereError | IllegalStateException ex) {
                LOG(ex);
                return false;
            } catch (RuntimeException ex) {
                LOG(ex);
                throw ex;
            }
        }
        // "instructions = %d labels = %d directives = %d pre_comments = %d"
        LOG(M111, instct, labct, dirct - 1,js.getPreCommentsCount());
        // dirct - 1 as .end class is internal
        assert state == State.END_CLASS;
        boolean success = END_MESSAGES(jclassnode.getClassName());
        return success;
    }

    private void visitJvmVersion(JvmVersion jvmversion) {
        if (this.jvmVersion != null) {
            throw new IllegalStateException();
        }
        this.jvmVersion = jvmversion;
        Global.setJvmVersion(jvmversion);
        JynxTranslator translator = JynxTranslator.getInstance();
        JynxGlobal.set();
        JynxGlobal.setTranslator(translator);
        this.opmap = JynxOps.getInstance(jvmVersion, translator);
    }
    
    private void setOptions(Line line) {
        while (true) {
            Token token = line.nextToken();
            if (token.isEndToken()) {
                break;
            }
            Optional<GlobalOption> option = GlobalOption.optInstance(token.asString());
            if (option.isPresent()
                    && MainOption.ASSEMBLY.usesOption(option.get()) && option.get() != GlobalOption.SYSIN) {
                ADD_OPTION(option.get());
            } else {
                LOG(M105,token); // "unknown option %s - ignored"
            }
        }
        Global.printAddedOptions();
    }
    
    private void setJvmVersion(Line line) {
        String verstr = line.nextToken().asString();
        setOptions(line);
        visitJvmVersion(JvmVersion.getVersionInstance(verstr));
    }

    @Override
    public void defaultVersion(Directive dir) {
        // "%s %s assumed"
        LOG(M143,Directive.dir_version,JvmVersion.DEFAULT_VERSION);
        visitJvmVersion(JvmVersion.DEFAULT_VERSION);
    }

    @Override
    public void setSource(Line line) {
        if (jclasshdr != null) {
            throw new IllegalStateException();
        }
        this.source = new ObjectLine<>(line.lastToken().asString(),line);
    }

    private void setMacroLib(Line line) {
        String libname = line.nextToken().asString();
        line.noMoreTokens();
        opmap.addMacroLib(libname);
    }
    
    @Override
    public void setStart(Directive dir) {
        Line line = js.getLine();
        unique_checker.checkUnique(dir, line);
        switch(dir) {
            case dir_version -> setJvmVersion(line);
            case dir_source -> setSource(line);
            case dir_macrolib -> setMacroLib(line);
            default -> // "unknown directive %s for context %s"
                throw new LogAssertionError(M907,dir,State.START_BLOCK);
        }
    }

    private Access getAccess(Line line, EnumSet<AccessFlag> flags, ClassType classtype, JvmVersion jvmversion) {
        String cname;
        switch (classtype) {
            case MODULE_CLASS -> {
                flags = EnumSet.noneOf(AccessFlag.class); // read in JynxModule
                cname = Constants.MODULE_CLASS_NAME.stringValue();
            }
            case PACKAGE -> {
                cname = line.nextToken().asName();
                CLASS_NAME.validate(cname);
                cname += "/" + Constants.PACKAGE_INFO_NAME.stringValue();
                jvmversion.checkSupports(Feature.package_info);
            }
            default -> {
                cname = line.nextToken().asName();
                CLASS_NAME.validate(cname);
            }
        }
        line.noMoreTokens();
        flags.addAll(classtype.getMustHave4Class(jvmversion)); 
        Access accessname = Access.getInstance(flags, jvmversion, cname, classtype);
        accessname.check4Class();
        return accessname;
    }
    
    @Override
    public void setClass(Directive dir) {
        Line line = js.getLine();
        var flags = line.getAccFlags();
        ClassType classtype = ClassType.ofDir(dir);
        LOG(M89, file_source,jvmVersion); // "file = %s version = %s"
        Access accessname = getAccess(line, flags, classtype, jvmVersion);
        jclassnode = JynxClassNode.getInstance(accessname, resolver);
        jclasshdr = jclassnode.getJynxClassHdr(source, defaultSource, resolver);
        JynxGlobal.setClassName(jclasshdr.getClassName());
        state = State.getState(classtype);
        sd = jclasshdr;
    }

    @Override
    public void visitDirective(Directive dir, JynxScanner js) {
        Line line = js.getLine();
        unique_checker.checkUnique(dir, line);
        visitCommonDirective(dir, line, js);
    }

    @Override
    public void setCommon(Directive dir) {
        if (dir == dir_comment) {
            LOGGER().pushContext();
            js.skipNested(dir_comment, end_comment,EnumSet.noneOf(Directive.class));
            LOGGER().popContext();
        } else {
            sd.visitDirective(dir, js);
        }
    }
    
    @Override
    public void setHeader(Directive dir) {
        jclasshdr.visitDirective(dir, js);
    }
    
    @Override
    public void endHeader(Directive dir) {
        assert dir == null;
        jclassnode.acceptClassHdr(jclasshdr);
        jclasshdr = null;
        sd = null;
    }

    @Override
    public void setComponent(Directive dir) {
        assert dir == Directive.dir_component;
        jcompnode = jclassnode.getJynxComponentNode(js.getLine());
        sd = jcompnode;
        LOGGER().pushContext();
    }
    
    @Override
    public void endComponent(Directive dir) {
        if (jcompnode == null) {
            throw new IllegalStateException();
        }
        jclassnode.acceptComponent(jcompnode, dir);
        jcompnode = null;
        sd = null;
        LOGGER().popContext();
    }
    
    @Override
    public void setField(Directive dir) {
        assert dir == Directive.dir_field;
        jfieldnode = jclassnode.getJynxFieldNode(js.getLine());
        sd = jfieldnode;
        LOGGER().pushContext();
    }

    @Override
    public void endField(Directive dir) {
        if (jfieldnode == null) {
            throw new IllegalStateException();
        }
        jclassnode.acceptField(jfieldnode,dir);
        jfieldnode = null;
        sd = null;
        LOGGER().popContext();
    }


    @Override
    public void setMethod(Directive dir) {
        switch(dir) {
            case dir_method -> {
                if (jmethodnode != null) {
                    throw new IllegalStateException();
                }
                Line line = js.getLine();
                jmethodnode = jclassnode.getJynxMethodNode(line);
                sd = jmethodnode;
                LOGGER().pushContext();
            }
            default -> jmethodnode.visitDirective(dir, js);
        }
    }

    @Override
    public void setCode(Directive dir) {
        if (jcodehdr == null) {
            jcodehdr = jmethodnode.getJynxCodeHdr(js,opmap);
            if (jcodehdr == null) {
                js.skipTokens();
                return;
            }
            jcodehdr.visitCode();
            sd = jcodehdr;
        }
        if (dir == null) {
            return;
        }
        jcodehdr.visitDirective(dir, js);
    }
    
    @Override
    public void endMethod(Directive dir) {
        if (dir == null) {
            LOG(M270, Directive.end_method); // "%s directive missing but assumed"
        }
        boolean ok;
        if (jmethodnode.isAbstractOrNative()) {
            ok = true;
        } else {
            if (jcodehdr == null) {
                LOG(M46,jmethodnode.getName()); // "method %s has no body"
                ok = false;
            } else {
                ok = jcodehdr.visitEnd();
            }
        }
        if (ok) {
            jclassnode.acceptMethod(jmethodnode);
        }
        jmethodnode = null;
        jcodehdr = null;
        sd = null;
        LOGGER().popContext();
    }

    @Override
    public void setModule(Directive dir) {
        Line line = js.getLine();
        if (dir == dir_module) {
            jmodule = JynxModule.getInstance(line,jvmVersion);
        } else {
            jmodule.visitDirective(dir, line);
        }
    }
    
    @Override
    public void endModule(Directive dir) {
        assert dir == Directive.end_module;
        assert jmodule != null;
        jclassnode.acceptModule(jmodule);
    }
    
    @Override
    public void endClass(Directive dir) {
        if (js.getLine() != null) {
            LOG(M240,Directive.end_class); // "%s is for internal use only"
        }
        int errct = LOGGER().numErrors();
        if (errct != 0) {
            return;
        }
        jclassnode.visitEnd();
    }
    
    private byte[] toByteArray() {
        if (LOGGER().numErrors() != 0) {
            return null;
        }
        byte[] bytes = jclassnode.toByteArray();
        if (OPTION(GlobalOption.BASIC_VERIFIER)) {
            return bytes;
        }
        Global.pushGlobal(MainOption.VERIFY);
        Verifier verifier = new Verifier(resolver);
        boolean ok = verifier.verify(bytes);
        Global.popGlobal();
        return ok? bytes: null;
    }
    
}
