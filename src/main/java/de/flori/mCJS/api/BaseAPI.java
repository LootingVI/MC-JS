package de.flori.mCJS.api;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Base class for all API modules providing common utility methods
 */
public abstract class BaseAPI {
    protected final JavaPlugin plugin;
    
    public BaseAPI(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Check if debug mode is enabled in config
     */
    protected boolean isDebugMode() {
        return plugin.getConfig().getBoolean("settings.debug-mode", false);
    }
    
    /**
     * Debug logging helper
     */
    protected void debug(String message) {
        if (isDebugMode()) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }
    
    /**
     * Helper method to convert legacy color codes to Adventure Component (with & support)
     */
    protected Component legacyToComponentWithAmpersand(String text) {
        if (text == null) {
            return Component.empty();
        }
        // Replace & with § for legacy serializer
        String converted = text.replace('&', '§');
        return LegacyComponentSerializer.legacySection().deserialize(converted);
    }
}
