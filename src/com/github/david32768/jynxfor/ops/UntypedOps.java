package com.github.david32768.jynxfor.ops;


import static com.github.david32768.jynxfor.ops.JvmOp.*;

import static com.github.david32768.jynxfor.my.Message.M282;

import com.github.david32768.jynxfor.scan.Line;
import com.github.david32768.jynxfor.scan.Token;

import com.github.david32768.jynxfree.jynx.LogIllegalArgumentException;
import com.github.david32768.jynxfree.jynx.LogUnexpectedEnumValueException;

import jynx2asm.FrameElement;

public enum UntypedOps implements SelectOp {
    
    xxx_xreturn(Type.RETURN),
    
    xxx_xload(Type.LOCAL,asm_iload,asm_lload,asm_fload,asm_dload,asm_aload),
    xxx_xstore(Type.STACK,asm_istore,asm_lstore,asm_fstore,asm_dstore,asm_astore),
    ;
        
    private final Select selector;

    private UntypedOps(UntypedOps.Type type, JynxOp... ops) {
        this.selector = new Select(type, ops);
    }

    @Override
    public JynxOp getOp(CurrentState state) {
        return selector.getOp(state);
    }

    @Override
    public String toString() {
        return name().substring(4);
    }

    private static enum Type {
        SLOT_LENGTH,
        STACK,
        LOCAL,
        RETURN,
        ;
    }

    public static SelectOp of12(JynxOp oplen1, JynxOp oplen2) {
        return new Select(Type.SLOT_LENGTH, oplen1, oplen2);
    }

    public static SelectOp stackILFDA(JynxOp iop, JynxOp jop, JynxOp fop, JynxOp dop, JynxOp aop) {
        return new Select(Type.STACK, iop, jop, fop, dop, aop);
    }

    private static class Select implements SelectOp {

        private final Type type;
        private final JynxOp[] ops;

        private Select(UntypedOps.Type type, JynxOp... ops) {
            this.type = type;
            this.ops = ops;
        }

        @Override
        public JynxOp getOp(CurrentState state) {
            Line line = state.line();
            return switch (type) {
                case RETURN -> state.getReturnOp();
                case SLOT_LENGTH -> getSlotsOfTOS(state);
                case STACK -> getTypeIndex(state.peekTOS());
                case LOCAL -> getLocal(line, state);
                default -> throw new LogUnexpectedEnumValueException(type);
            };
        }

        private JynxOp getSlotsOfTOS(CurrentState state) {
            FrameElement fe = state.peekTOS();
            return ops[fe.slots() - 1];
        }

        private final static String ILFDA = "ilfda";
        
        private JynxOp getTypeIndex(FrameElement fe) {
            char fetype = fe.instLetter();
            int index = ILFDA.indexOf(fetype);
            if (index < 0 || index >= ops.length) {
                String valid = ILFDA.substring(0,Math.min(ILFDA.length(),ops.length));
                // "instruction type '%c' (%s) of element at top of stack is not one of %s"
                throw new LogIllegalArgumentException(M282,fetype,fe,valid);
            }
            assert ops[index].toString().charAt(0) == fetype:
                    String.format("%s %c", ops[index],fetype);
            return ops[index];
        }
        
        private JynxOp getLocal(Line line, CurrentState state) {
            Token token = line.peekToken();
            return getTypeIndex(state.peekVarNum(token));
        }
        
    }

}
