package com.github.david32768.jynxfor.code;

import java.util.HashMap;
import java.util.Map;

import static com.github.david32768.jynxfor.my.Message.M349;

import com.github.david32768.jynxfree.jynx.LogIllegalArgumentException;

public class VarIndexTranslator {

    private final Map<String,Integer> imap;
    private final Map<String,Integer> lmap;
    private final Map<String,Integer> fmap;
    private final Map<String,Integer> dmap;
    
    private final Map<String,Character> checker;

    public VarIndexTranslator() {
        this.imap = new HashMap<>();
        this.lmap = new HashMap<>();
        this.fmap = new HashMap<>();
        this.dmap = new HashMap<>();
        this.checker = new HashMap<>();
    }
    
    public int indexOf(char vt, String name) {
        char is = checker.computeIfAbsent(name, k -> vt);
        if (is != vt) {
            // "name %s: type was '%c' but accessed as '%c"
            throw new LogIllegalArgumentException(M349, name, is, vt);
        }
        return switch(vt) {
            case 'i' -> imap.computeIfAbsent(name, k -> imap.size());
            case 'l' -> lmap.computeIfAbsent(name, k -> lmap.size());
            case 'f' -> fmap.computeIfAbsent(name, k -> fmap.size());
            case 'd' -> dmap.computeIfAbsent(name, k -> dmap.size());
            default -> throw new AssertionError("vt = " + vt);
        };
    }
}
