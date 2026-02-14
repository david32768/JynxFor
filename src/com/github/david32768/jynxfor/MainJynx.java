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
import static com.github.david32768.jynxfor.my.Message.M651;
import static com.github.david32768.jynxfor.my.Message.M97;

import static com.github.david32768.jynxfree.jynx.Global.LOG;
import static com.github.david32768.jynxfree.jynx.Global.OPTION;
import static com.github.david32768.jynxfree.jynx.GlobalOption.SYSIN;
import static com.github.david32768.jynxfree.jynx.GlobalOption.VALIDATE_ONLY;

import com.github.david32768.jynxfor.scan.JynxScanner;

import com.github.david32768.jynxfree.jynx.MainConstants;
import com.github.david32768.jynxfree.jynx.MainOption;
import com.github.david32768.jynxfree.jynx.MainOptionService;
import com.github.david32768.jynxfree.utility.FileIO;

import jynx2asm.JynxClass;

public class MainJynx implements MainOptionService {

    @Override
    public MainOption main() {
        return MainOption.ASSEMBLY;
    }

    @Override
    public String version() {
        return "0.25.2";
    }
    
    @Override
    public boolean call(PrintWriter pw) {
        if (!OPTION(SYSIN)) {
            LOG(M222,SYSIN); // "either option %s is specified or file name is present but not both"
            return false;
        }
        return call(Optional.empty(), Optional.empty());
    }

    @Override
    public boolean call(PrintWriter pw, String arg) {
        if (OPTION(SYSIN)) {
            return call(Optional.empty(), Optional.of(arg));
        } else {
            return call(Optional.of(arg), Optional.empty());
        }
    }

    @Override
    public boolean call(PrintWriter pw, String fname, String dirname) {
        if (OPTION(SYSIN)) {
            LOG(M222,SYSIN); // "either option %s is specified or file name is present but not both"
            return false;
        }
        return call(Optional.of(fname), Optional.of(dirname));
    }

    private boolean call(Optional<String> optfname, Optional<String> optdirname) {
        String fname = optfname.orElse("SYSIN");
        if (optfname.isEmpty()) {
            LOG(M218); //"SYSIN will be used as input"
        } else if (!fname.endsWith(MainConstants.JX_SUFFIX)) {
            LOG(M97, fname, MainConstants.JX_SUFFIX); // "file(%s) does not have %s suffix"
            return false;
        }
        try {
            Optional<Path> optdirpath = optdirname.map(Path::of);
            if (optdirpath.isPresent() && !Files.isDirectory(optdirpath.get())) {
                // "%s is not a directory"
                LOG(M651, optdirname.get());
                return false;
            }
            JynxScanner scanner;
            if (optfname.isPresent()) {
                Path pathj = Paths.get(fname);
                scanner = JynxScanner.getInstance(pathj);
            } else {
                scanner = JynxScanner.getInstance(System.in);
            }
            byte[] ba = JynxClass.getBytes(fname,scanner);
            if (ba == null) {
                return false;
            }
            return writeOut(fname, ba, optdirpath);
        } catch (IOException ex) {
            LOG(ex);
            return false;
        }
    }
    
    @Override
    public byte[] callFromString(String classname, String code) {
        return JynxClass.getBytes(classname, null, JynxScanner.getInstance(code));
    }
    
    private static boolean writeOut(String fname, byte[] bytes, Optional<Path> optdirpath) throws IOException {
        if (OPTION(VALIDATE_ONLY)) {
            return true;
        }
        String cname = CLASS_NAME();
        if (optdirpath.isPresent()) {
            return FileIO.write(optdirpath.get(), cname, bytes);
        }
        int index = cname.lastIndexOf('/');
        String cfname = cname.substring(index + 1);
        cfname += ".class";
        Path pathc = Paths.get(cfname);
        if (!OPTION(SYSIN)) {
            pathc = Paths.get(fname).resolveSibling(pathc);
        }
        Files.write(pathc, bytes);
         // "%s created - size %d bytes"
        LOG(M116, pathc, bytes.length);
        return true;
    }
    
}
