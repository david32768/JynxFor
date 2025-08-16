package asm;

import static com.github.david32768.jynxfree.jvm.AccessFlag.*;

import com.github.david32768.jynxfree.jvm.AccessFlag;

public interface HasAccessFlags {

    public boolean is(AccessFlag flag);
    
    public default boolean isStatic() {
        return is(acc_static);
    }

    public default boolean isFinal() {
        return is(acc_final);
    }    

    public default boolean isPrivate() {
        return is(acc_private);
    }

    public default boolean isAbstract() {
        return is(acc_abstract);
    }

    public default boolean isNative() {
        return is(acc_native);
    }

    public default boolean isAbstractOrNative() {
        return isAbstract() || isNative();
    }

    public default boolean isComponent() {
        return is(xxx_component);
    }
}
