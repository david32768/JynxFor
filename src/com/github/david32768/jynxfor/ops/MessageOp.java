package com.github.david32768.jynxfor.ops;

import static com.github.david32768.jynxfor.my.Message.M320;
import static com.github.david32768.jynxfor.my.Message.M322;

import static com.github.david32768.jynxfree.jynx.Global.LOG;

import com.github.david32768.jynxfree.jynx.JynxMessage;

public class MessageOp implements JynxOp {
    
    private static enum MessageType {
       IGNORE(M320), // "occurences of %s have been ignored: %s"
       UNSUPPORTED(M322), // "use of %s is not supported: %s"
       ;
       
       private final JynxMessage msg;

        private MessageType(JynxMessage msg) {
            this.msg = msg;
        }
       
    }
   
    private final MessageType type;
    private final String aux;

    private MessageOp(MessageType type, String aux) {
        this.type = type;
        this.aux = aux;
    }

    public void logMessage(MacroOp macroop){
        LOG(type.msg,macroop,aux);
    }

    @Override
    public String toString() {
        return String.format("*%s %s", type, aux);
    }

    public static MessageOp ignoreMacro(String msg) {
        return new MessageOp(MessageOp.MessageType.IGNORE,msg);
    }
    
    public static MessageOp unsupportedMacro(String msg) {
        return new MessageOp(MessageOp.MessageType.UNSUPPORTED,msg);
    }
    
}
