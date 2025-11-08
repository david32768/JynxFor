package com.github.david32768.jynxfor.verify;

import java.io.IOException;
import java.lang.classfile.ClassFile.ClassHierarchyResolverOption;
import java.lang.classfile.ClassHierarchyResolver;
import java.lang.constant.ClassDesc;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.github.david32768.jynxfor.my.Message.M403;
import static com.github.david32768.jynxfor.my.Message.M404;
import static com.github.david32768.jynxfor.my.Message.M617;
import static com.github.david32768.jynxfor.my.Message.M622;
import static com.github.david32768.jynxfor.my.Message.M623;

import static com.github.david32768.jynxfree.jynx.Global.LOG;
import static com.github.david32768.jynxfree.jynx.Global.OPTION;
import static com.github.david32768.jynxfree.jynx.ReservedWord.res_extends;
import static com.github.david32768.jynxfree.jynx.ReservedWord.res_interface;
import static com.github.david32768.jynxfree.jynx.ReservedWord.right_array;

import com.github.david32768.jynxfor.scan.JynxScanner;
import com.github.david32768.jynxfor.scan.Line;
import com.github.david32768.jynxfor.scan.Token;
import com.github.david32768.jynxfor.scan.TokenArray;

import com.github.david32768.jynxfree.jynx.Directive;
import com.github.david32768.jynxfree.jynx.GlobalOption;
import com.github.david32768.jynxfree.jynx.NameDesc;
import com.github.david32768.jynxfree.jynx.ReservedWord;

public class Resolver {

    private final Set<String> interfaces;
    private final Map<String, String> classToSuperClass;
    private final Map<String, LinkedHashSet<String>> classHierarchy;
    private final ClassLoader loader;
    
    public Resolver() {
        this.interfaces = new HashSet<>();
        this.classToSuperClass = new HashMap<>();
        this.classHierarchy = new HashMap<>();
        this.loader = OPTION(GlobalOption.VERIFIER_PLATFORM)?
                ClassLoader.getPlatformClassLoader():
                ClassLoader.getSystemClassLoader();
    }
    
    private static final String HINTS_SUFFIX = ".hints";
    
    public static Resolver ofFile(String hints) {
        var resolver = new Resolver();
        Path path = Path.of(hints);
        if (!path.toString().endsWith(HINTS_SUFFIX)) {
            // "hints file does not have %s suffix"
            LOG(M622,HINTS_SUFFIX);
            return resolver;
        }
        try {
            var js = JynxScanner.getInstance(path);
            Line line = js.next();
            if (line.isDirective() && line.firstToken().asDirective() == Directive.dir_hints) {
                try (TokenArray dotarray = line.getTokenArray()) {
                    resolver.addResolver(dotarray);                    
                }
            } else {
                // "first directive in hints file is not %s"
                LOG(M623,Directive.dir_hints);
            }
        } catch (IOException ex) {
            LOG(ex);
        }
        return resolver;
    }

    public ClassLoader getLoader() {
        return loader;
    }
   
    public void addResolver(TokenArray dotarray) {
        createResolver(dotarray);
        createHierarchy();
    }

    private void createResolver(TokenArray dotarray) {
        while (true) {
            Token token = dotarray.firstToken();
            if (token.is(right_array)) {
                break;
            }
            String class1 = token.asString();
            NameDesc.CLASS_NAME.validate(class1);
            ReservedWord rw = dotarray.nextToken()
                    .expectOneOf(ReservedWord.res_interface, ReservedWord.res_extends);
            switch(rw) {
                case res_interface -> {
                    if (interfaces.contains(class1)) {
                        // "duplicate %s hint for %s"
                        LOG(M403, ReservedWord.res_interface, class1);
                        break;
                    }
                    interfaces.add(class1);
                }
                case res_extends -> {
                    String class2 = dotarray.nextToken().asString();
                    NameDesc.CLASS_NAME.validate(class2);
                    var last = classToSuperClass.putIfAbsent(class1, class2);
                    if (last != null) {
                        if (last.equals(class2)) {
                            // "duplicate %s hint for %s"
                            LOG(M403, ReservedWord.res_extends, class1);
                        } else {
                            // "different %s hint for %s: %s and %s"
                            LOG(M404, ReservedWord.res_extends, class1, class2, last);
                        }
                    }
                }
                default -> throw new AssertionError();
            }
            dotarray.noMoreTokens();
        }
    }
   
    public void createHierarchy() {
        classHierarchy.clear();
        for (var klass : classToSuperClass.keySet()) {
            classHierarchy.put(klass, new LinkedHashSet<>());
            classHierarchy.get(klass).add(klass);
            String current = klass;
            while (true) {
                String next = classToSuperClass.get(current);
                if (next == null) {
                    break;
                }
                boolean added = classHierarchy.get(klass).add(next);
                if (!added) {
                    // "circular hint: %s extends %s"
                    LOG(M617, klass, next);
                    classToSuperClass.clear();
                    classHierarchy.clear();
                    return;
                }
                current = next;
            }
        }
    }
   
    public Optional<String> getCommonSuperClass(String class1 , String class2) {
        var c1list = classHierarchy.get(class1);
        var c2list = classHierarchy.get(class2);
        if (c1list == null || c2list == null) {
            return Optional.empty();
        }
        for (var next : c1list) {
            if (c2list.contains(next)) {
                return Optional.of(next);
            }
        }
        return Optional.empty();
    }
    
    public boolean isInterface(String class1) {
        return interfaces.contains(class1);
    }
    
    public Optional<String> getSuperClass(String class1) {
        return Optional.ofNullable(classToSuperClass.get(class1));
    }
    
    public boolean isSubTypeOf(String value, String expected) {
        var supers = classHierarchy.get(value);
        if (supers == null) {
            return false;
        }
        return supers.contains(expected);
    }
    
    private List<ClassDesc> getInterfaces() {
        return interfaces.stream()
                .map(this::classDescOf)
                .toList();
    }
    
    private Map<ClassDesc, ClassDesc> getSuperMap() {
        Map<ClassDesc, ClassDesc>  result = new HashMap<>();
        for (var me : classToSuperClass.entrySet()) {
            result.put(classDescOf(me.getKey()), classDescOf(me.getValue()));
        }
        return result;
    }
    
    private ClassDesc classDescOf(String klass) {
        return ClassDesc.of(klass.replace('/','.'));
    }

    public ClassHierarchyResolverOption getResolverOption() {
        ClassHierarchyResolver resolver = ClassHierarchyResolver.ofClassLoading(loader);
        var hints = ClassHierarchyResolver.of(getInterfaces(), getSuperMap());
        return ClassHierarchyResolverOption.of(hints.orElse(resolver));
    }
    
}
