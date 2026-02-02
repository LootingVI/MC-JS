package de.flori.mCJS.api;

import org.mozilla.javascript.Scriptable;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Context for event handlers - stores scope and plugin reference
 * Used instead of JSAPI instances in EventAPI
 */
public class EventHandlerContext {
    public final Scriptable scope;
    public final JavaPlugin plugin;
    
    public EventHandlerContext(Scriptable scope, JavaPlugin plugin) {
        this.scope = scope;
        this.plugin = plugin;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        EventHandlerContext that = (EventHandlerContext) obj;
        return scope == that.scope && plugin.equals(that.plugin);
    }
    
    @Override
    public int hashCode() {
        return scope.hashCode() * 31 + plugin.hashCode();
    }
}
