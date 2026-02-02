package de.flori.mCJS.api;

import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

/**
 * Helper class that provides scope and function execution capabilities
 * to API modules that need to execute JavaScript functions
 */
public class APIHelper {
    private Scriptable scope;
    
    public void setScope(Scriptable scope) {
        this.scope = scope;
    }
    
    public Scriptable getScope() {
        return scope;
    }
    
    /**
     * Execute a JavaScript function with a new Rhino context
     */
    public void executeFunction(Function func, Object... args) {
        if (scope == null) {
            return;
        }
        
        // Create a new context for this thread
        org.mozilla.javascript.Context rhinoContext = org.mozilla.javascript.Context.enter();
        try {
            rhinoContext.setOptimizationLevel(-1);
            rhinoContext.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
            func.call(rhinoContext, scope, scope, args);
        } finally {
            org.mozilla.javascript.Context.exit();
        }
    }
}
