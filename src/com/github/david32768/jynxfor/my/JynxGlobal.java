package com.github.david32768.jynxfor.my;

import com.github.david32768.jynxfor.ops.JynxTranslator;

public class JynxGlobal {
    
    private String classname;
    private JynxTranslator translator;
    
    private JynxGlobal() {
        this.classname = null;
        this.translator = null;
    }

    private static JynxGlobal global;
    
    public static void set() {
        global = new JynxGlobal();
    }
    
    public static void setTranslator(JynxTranslator translator) {
        assert global.translator == null;
        global.translator = translator;
    }
    
    public static void setClassName(String classname) {
        assert global.classname == null;
        global.classname = classname;
    }
    
    public static String CLASS_NAME() {
        return global.classname;
    }

    public static String TRANSLATE_DESC(String str) {
        return global.translator.translateDesc(CLASS_NAME(),str);
    }
    
    public static String TRANSLATE_PARMS(String str) {
        return global.translator.translateParms(CLASS_NAME(),str);
    }
    
    public static String TRANSLATE_TYPE(String str, boolean semi) {
        return global.translator.translateType(CLASS_NAME(),str, semi);
    }
    
    public static String TRANSLATE_OWNER(String str) {
        return global.translator.translateOwner(CLASS_NAME(),str);
    }
}
