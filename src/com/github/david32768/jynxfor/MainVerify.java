package com.github.david32768.jynxfor;

import java.io.IOException;
import java.io.PrintWriter;

import static com.github.david32768.jynxfree.jynx.Global.LOG;

import com.github.david32768.jynxfor.verify.Resolver;
import com.github.david32768.jynxfor.verify.Verifier;
import com.github.david32768.jynxfree.jynx.ClassUtil;
import com.github.david32768.jynxfree.jynx.MainOption;
import com.github.david32768.jynxfree.jynx.MainOptionService;

public class MainVerify implements MainOptionService {
    
    @Override
    public MainOption main() {
        return MainOption.VERIFY;
    }

    @Override
    public boolean call(PrintWriter pw, String fname) {
        return call(fname, new Verifier(new Resolver()));
    }

    @Override
    public boolean call(PrintWriter pw, String fname, String hints) {
        var verifier = new Verifier(Resolver.ofFile(hints));
        return call(fname, verifier);
    }

    private boolean call(String fname, Verifier verifier) {
        try {
            byte[] ba = ClassUtil.getClassBytes(fname);
            return verifier.verify(ba);
        } catch (IOException ex) {
            LOG(ex);
            return false;
        }
    }

}
