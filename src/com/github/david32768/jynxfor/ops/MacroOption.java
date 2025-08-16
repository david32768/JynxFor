package com.github.david32768.jynxfor.ops;

import com.github.david32768.jynxfree.jynx.GlobalOption;

public enum MacroOption {
    
    STRUCTURED_LABELS(GlobalOption.__STRUCTURED_LABELS),
    INDENT(GlobalOption.__WARN_INDENT),
    ;
    
    private final GlobalOption option;

    private MacroOption(GlobalOption option) {
        this.option = option;
    }

    public GlobalOption option() {
        return option;
    }
        
}
