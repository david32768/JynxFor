package com.github.david32768.jynxfor.ops;

import java.util.function.Predicate;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import static com.github.david32768.jynxfor.my.Message.M176;
import static com.github.david32768.jynxfor.my.Message.M243;
import static com.github.david32768.jynxfor.my.Message.M316;
import static com.github.david32768.jynxfor.my.Message.M318;

import static com.github.david32768.jynxfree.jynx.Global.ADD_OPTION;
import static com.github.david32768.jynxfree.jynx.Global.LOG;

import com.github.david32768.jynxfree.jynx.LogAssertionError;
import com.github.david32768.jynxfree.jynx.NameDesc;

public class JynxOps {

    private final Map<String, JynxOp> opmap;
    private final Map<String,MacroLib> macrolibs;
    
    private Predicate<String> labelTester;
    
    private final JynxTranslator translator;
    

    private JynxOps(JynxTranslator translator) {
        this.opmap = new HashMap<>(512);
        this.macrolibs = new HashMap<>();
        this.translator = translator;
    }

    public static JynxOps getInstance(JynxTranslator translator) {
        return new JynxOps(translator);
    }

    private static final int MAX_SIMPLE = 16;
    
    private void addOp(String name, JynxOp op) {
        if (!NameDesc.OP_ID.isValid(name)) {
            // "op %s is not a valid op name"
            throw new LogAssertionError(M318, name);
        }
        JynxOp before = opmap.putIfAbsent(name, op);
        if (before != null) {
            LOG(M243, name, op.getClass(), before.getClass()); // "%s op defined in %s has already been defined in %s"
        }
    }

    public JynxOp get(String jopstr) {
        JynxOp op =  JvmOp.getOp(jopstr);
        if (op == null) {
            op = opmap.get(jopstr);
            if (op == null) {
                return null;
            }
        }
        return op;
    }
    
    public MacroLib addMacroLib(String libname) {
        MacroLib result = macrolibs.get(libname);
        if (result != null) {
            return result;
        }
        ServiceLoader<MacroLib> libloader = ServiceLoader.load(MacroLib.class);
        for (MacroLib lib : libloader) {
            if (lib.name().equals(libname)) {
                lib.getMacros().entrySet().stream()
                        .forEach(me->addOp(me.getKey(), me.getValue()));
                macrolibs.put(libname, lib);
                translator.addParmTranslations(lib.parmTranslations());
                translator.addOwnerTranslations(lib.ownerTranslations());
                if (labelTester == null) {
                    labelTester = lib.labelTester();
                } else if (lib.labelTester() != null) {
                    // "only one label tester allowed"
                    LOG(M316);
                }
                for (MacroOption opt:lib.getOptions()) {
                    ADD_OPTION(opt.option());
                }
                result = lib;
                break;
            }
        }
        if (result == null) {
            LOG(M176,libname); // "%s not found as a macro library service"
        }
        return result;
    }
   
    public boolean isLabel(String labstr) {
        return labelTester != null && labelTester.test(labstr);
    }

}
