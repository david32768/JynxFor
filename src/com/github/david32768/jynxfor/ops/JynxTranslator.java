package com.github.david32768.jynxfor.ops;

import java.util.HashMap;
import java.util.Map;

import static com.github.david32768.jynxfor.my.Message.M314;
import static com.github.david32768.jynxfor.my.Message.M315;
import static com.github.david32768.jynxfor.my.Message.M324;
import static com.github.david32768.jynxfor.my.Message.M325;

import static com.github.david32768.jynxfree.jynx.Global.LOG;

import com.github.david32768.jynxfree.jynx.NameDesc;

import jynx2asm.ConstType;

public class JynxTranslator {

    private final Map<String,String> ownerTranslations;
    private final Map<String,String> parmTranslations;
    

    private JynxTranslator() {
        this.ownerTranslations = new HashMap<>();
        this.parmTranslations = new HashMap<>();
    }

    public static JynxTranslator getInstance() {
        return new JynxTranslator();
    }

    public void addOwnerTranslations(Map<String, String> add) {
        addTranslations(ownerTranslations, add, NameDesc.OWNER_VALUE_NAME);
    }
    
    public void addParmTranslations(Map<String, String> add) {
        addTranslations(parmTranslations, add, NameDesc.PARM_VALUE_NAME);
    }
    
    private void addTranslations(Map<String, String> map, Map<String, String> add, NameDesc valuetype) {
        for (Map.Entry<String, String> entry : add.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (!NameDesc.KEY_NAME.isValid(key)) {
                // "translation %s -> %s is invalid; %s is not a valid key"
                LOG(M324, key, value, key);
            } else if (!valuetype.isValid(value)) {
                // "translation %s -> %s is invalid; %s is not a valid value"
                LOG(M325, key, value, value);
            } else {
                map.put(key, value);
            }
        }
    }
    
    public String translateType(String classname, String parm, boolean semi) {
        int last = parm.lastIndexOf('[');
        String type = parm.substring(last + 1); // OK if last == -1
        String prefix = parm.substring(0, last + 1); // OK if last == -1
        if (ConstType.isPrimitiveType(type)) {
            return parm;
        }
        int indexsemi = type.indexOf(';');
        boolean owner = type.contains("/") || indexsemi >= 0;
        if (owner) {
            if (indexsemi >= 0) {
                if (!type.startsWith("L") || indexsemi != type.length() - 1) {
                    return parm; // error
                }
                type = type.substring(1, indexsemi);
            }
            String trans = translateOwner(classname, type);
            return construct(prefix, trans, semi);
        } else {
            String trans = parmTranslations.get(type);
            if (trans == null) {
                trans = type;
            }
            type = construct(prefix, trans, semi);
            //"parameter type %s changed to %s"
            LOG(M314, parm, type);
            return type;
        }
    }

    private String construct(String prefix, String type, boolean semi) {
        if (!prefix.isEmpty() || semi) { 
            return prefix + 'L' + type + ';';
        }
        return prefix + type;
    }
    
    public String translateDesc(String classname, String str) {
        int indexto = str.indexOf("->");
        if (indexto < 0) { 
            return str;
        }
        String parm = str.substring(0, indexto);
        String rtype = str.substring(indexto + 2);
        if (rtype.equals("()")) {
            rtype = "V";
        } else {
            rtype = translateParm(classname, rtype, true);
        }
        parm = translateParms(classname, parm);
        return parm + rtype;
    }
    
    public String translateParms(String classname, String parms) {
        if (parmTranslations.isEmpty() || parms.charAt(0) != '(' || parms.charAt(parms.length() - 1) != ')') { 
            return parms;
        }
        String[] parmarr = parms.substring(1, parms.length() - 1).split(",");
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (String p:parmarr) {
            sb.append(translateParm(classname, p, true));
        }
        sb.append(')');
        return sb.toString();
    }
    
    public String translateOwner(String classname, String str) {
        if (str == null || str.equals("/")) {
            return classname;
        }
        String result = ownerTranslations.get(str);
        if (result == null) {
            return str;
        }
        // "owner %s translated to %s"
        LOG(M315, str, result);
        return result;
    }
    
    private String translateParm(String classname, String parm, boolean semi) {
        int last = parm.lastIndexOf('[');
        String type = parm.substring(last + 1); // OK if last == -1
        String prefix = parm.substring(0, last + 1); // OK if last == -1
        if (ConstType.isPrimitiveType(type)) {
            return parm;
        }
        int indexsemi = type.indexOf(';');
        boolean owner = type.contains("/") || indexsemi >= 0;
        if (owner) {
            if (indexsemi >= 0) {
                if (!type.startsWith("L") || indexsemi != type.length() - 1) {
                    return parm; // error
                }
                type = type.substring(1, indexsemi);
            }
            String trans = translateOwner(classname, type);
            return construct(prefix, trans, semi);
        } else {
            String trans = parmTranslations.get(type);
            if (trans == null) {
                return parm;
            }
            owner = trans.contains("/");
            if (owner) {
                type = construct(prefix, trans, semi);
            } else {
                type = prefix + trans;
            }
            //"parameter type %s changed to %s"
            LOG(M314, parm, type);
            return type;
        }
    }

}
