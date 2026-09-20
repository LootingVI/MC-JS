package de.flori.mCJS.api;

import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

public class APIHelper {
    private volatile Scriptable scope;

    public void setScope(Scriptable scope) {
        this.scope = scope;
    }

    public Scriptable getScope() {
        return scope;
    }

    public void executeFunction(Function func, Object... args) {
        callFunction(func, args);
    }

    public Object callFunction(Function func, Object... args) {
        Scriptable currentScope = scope;
        if (currentScope == null) {
            return null;
        }

        org.mozilla.javascript.Context rhinoContext = org.mozilla.javascript.Context.enter();
        try {
            rhinoContext.setOptimizationLevel(-1);
            rhinoContext.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
            return func.call(rhinoContext, currentScope, currentScope, args);
        } finally {
            org.mozilla.javascript.Context.exit();
        }
    }
}
