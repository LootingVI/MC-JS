package de.flori.mCJS.api;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class BaseAPI {
    protected final JavaPlugin plugin;

    public BaseAPI(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    protected boolean isDebugMode() {
        return plugin.getConfig().getBoolean("settings.debug-mode", false);
    }

    protected void debug(String message) {
        if (isDebugMode()) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }

    protected Component legacyToComponentWithAmpersand(String text) {
        if (text == null) {
            return Component.empty();
        }

        String converted = text.replace('&', '§');
        return LegacyComponentSerializer.legacySection().deserialize(converted);
    }
}
