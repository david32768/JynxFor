package com.github.david32768.jynxfor;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static com.github.david32768.jynxfor.my.JynxGlobal.CLASS_NAME;
import static com.github.david32768.jynxfor.my.Message.M116;
import static com.github.david32768.jynxfor.my.Message.M218;
import static com.github.david32768.jynxfor.my.Message.M222;
import static com.github.david32768.jynxfor.my.Message.M97;
import static com.github.david32768.jynxfree.jynx.Global.LOG;
import static com.github.david32768.jynxfree.jynx.Global.OPTION;
import static com.github.david32768.jynxfree.jynx.GlobalOption.SYSIN;
import static com.github.david32768.jynxfree.jynx.GlobalOption.VALIDATE_ONLY;
import com.github.david32768.jynxfree.jynx.MainConstants;

import com.github.david32768.jynxfree.jynx.MainOption;
import com.github.david32768.jynxfree.jynx.MainOptionService;

import jynx2asm.JynxClass;
import jynx2asm.JynxScanner;

public class MainJynx implements MainOptionService {

    @Override
    public MainOption main() {
        return MainOption.ASSEMBLY;
    }

    @Override
    public boolean call(PrintWriter pw) {
        return call(Optional.empty());
    }

    @Override
    public boolean call(PrintWriter pw, String arg) {
        return call(Optional.of(arg));
    }

    public boolean call(Optional<String> optfname) {
        if (optfname.isPresent() == OPTION(SYSIN)) {
            LOG(M222,SYSIN); // "either option %s is specified or file name is present but not both"
            return false;
        }
        if (optfname.isEmpty()) {
            LOG(M218); //"SYSIN will be used as input"
        }
        String fname = optfname.orElse("SYSIN");
        try {
            JynxScanner scanner;
            if (optfname.isPresent()) {
                if (!fname.endsWith(MainConstants.JX_SUFFIX)) {
                    LOG(M97, fname, MainConstants.JX_SUFFIX); // "file(%s) does not have %s suffix"
                    return false;
                }
                Path pathj = Paths.get(fname);
                scanner = JynxScanner.getInstance(pathj);
            } else {
                scanner = JynxScanner.getInstance(System.in);
            }
            return assemble(fname, scanner);
        } catch (IOException ex) {
            LOG(ex);
            return false;
        }
    }
    
    @Override
    public byte[] callFromString(String classname, String code) {
        return JynxClass.getBytes(classname, null, JynxScanner.getInstance(code));
    }
    
    private static boolean assemble(String fname, JynxScanner scanner) throws IOException {
        byte[] ba = JynxClass.getBytes(fname,scanner);
        if (ba == null) {
            return false;
        }
        if (OPTION(VALIDATE_ONLY)) {
            return true;
        }
        String cname = CLASS_NAME();
        int index = cname.lastIndexOf('/');
        String cfname = cname.substring(index + 1);
        cfname += ".class";
        Path pathc = Paths.get(cfname);
        if (!OPTION(SYSIN)) {
            Path parent = Paths.get(fname).getParent();
            if (parent != null) {
                pathc = parent.resolve(pathc);
            }
        }
        Files.write(pathc, ba);
        LOG(M116,pathc,ba.length); // "%s created - size %d bytes"
        return true;
    }
    
}
