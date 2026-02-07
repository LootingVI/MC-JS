package de.flori.mCJS;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class MCJS extends JavaPlugin {

    private JSPluginManager jsPluginManager;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("========================================");
        getLogger().info(Version.getVersionInfo() + " enabled!");
        getLogger().info("Using Rhino JavaScript Engine");
        getLogger().info("========================================");

        // Save default config if it doesn't exist
        saveDefaultConfig();
        
        // Reload config to ensure we have the latest values
        reloadConfig();

        // Initialize JS Plugin Manager
        try {
            jsPluginManager = new JSPluginManager(this);

            // Load JS plugins
            jsPluginManager.loadPlugins();

            int loadedCount = jsPluginManager.getLoadedPlugins().size();
            getLogger().info("Successfully loaded " + loadedCount + " JS plugin(s)");
            
            // Display plugin metadata
            for (String pluginName : jsPluginManager.getPluginMetadata().keySet()) {
                JSPluginManager.PluginMetadata metadata = jsPluginManager.getMetadata(pluginName);
                if (metadata != null && metadata.getVersion() != null) {
                    getLogger().info("  - " + pluginName + " v" + metadata.getVersion());
                }
            }
        } catch (Exception e) {
            getLogger().severe("Failed to initialize JS Plugin Manager: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (jsPluginManager != null) {
            jsPluginManager.unloadPlugins();
        }
        getLogger().info("MC-JS Plugin disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("jsreload")) {
            if (!sender.hasPermission("mcjs.admin")) {
                sender.sendMessage(Component.text("You don't have permission to use this command!").color(NamedTextColor.RED));
                return true;
            }

            if (args.length > 0) {
                // Reload specific plugin
                String pluginName = args[0];
                if (jsPluginManager != null && jsPluginManager.getLoadedPlugins().containsKey(pluginName)) {
                    jsPluginManager.reloadPlugin(pluginName);
                    sender.sendMessage(Component.text("Reloaded JS plugin: " + pluginName).color(NamedTextColor.GREEN));
                    getLogger().info("Reloaded JS plugin '" + pluginName + "' by " + sender.getName());
                } else {
                    sender.sendMessage(Component.text("Plugin '" + pluginName + "' not found!").color(NamedTextColor.RED));
                }
                return true;
            }

            // Reload config before reloading plugins
            reloadConfig();
            
            // Reload all JS plugins
            if (jsPluginManager != null) {
                jsPluginManager.unloadPlugins();
            }
            jsPluginManager = new JSPluginManager(this);
            jsPluginManager.loadPlugins();

            sender.sendMessage(Component.text("Reloaded " + jsPluginManager.getLoadedPlugins().size() + " JS plugin(s)!").color(NamedTextColor.GREEN));
            getLogger().info("Reloaded JS plugins by " + sender.getName());
            return true;
        } else if (command.getName().equalsIgnoreCase("jslist")) {
            if (!sender.hasPermission("mcjs.admin")) {
                sender.sendMessage(Component.text("You don't have permission to use this command!").color(NamedTextColor.RED));
                return true;
            }

            if (jsPluginManager != null) {
                var plugins = jsPluginManager.getLoadedPlugins();
                if (plugins.isEmpty()) {
                    sender.sendMessage(Component.text("No JS plugins loaded.").color(NamedTextColor.YELLOW));
                } else {
                    sender.sendMessage(Component.text("Loaded JS plugins (" + plugins.size() + "):").color(NamedTextColor.GREEN));
                    for (String pluginName : plugins.keySet()) {
                        JSPluginManager.PluginMetadata metadata = jsPluginManager.getMetadata(pluginName);
                        if (metadata != null && metadata.getVersion() != null) {
                            sender.sendMessage(Component.text("  - ").color(net.kyori.adventure.text.format.NamedTextColor.GRAY)
                                .append(Component.text(pluginName).color(net.kyori.adventure.text.format.NamedTextColor.WHITE))
                                .append(Component.text(" v" + metadata.getVersion()).color(net.kyori.adventure.text.format.NamedTextColor.GRAY)));
                        } else {
                            sender.sendMessage(Component.text("  - ").color(net.kyori.adventure.text.format.NamedTextColor.GRAY)
                                .append(Component.text(pluginName).color(net.kyori.adventure.text.format.NamedTextColor.WHITE)));
                        }
                    }
                }
            }
            return true;
        } else if (command.getName().equalsIgnoreCase("jsconfig")) {
            if (!sender.hasPermission("mcjs.admin")) {
                sender.sendMessage(Component.text("You don't have permission to use this command!").color(NamedTextColor.RED));
                return true;
            }

            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                sender.sendMessage(Component.text("Configuration reloaded!").color(NamedTextColor.GREEN));
                getLogger().info("Configuration reloaded by " + sender.getName());
                return true;
            }

            // Show config info
            sender.sendMessage(Component.text("=== MC-JS Configuration ===").color(NamedTextColor.GREEN));
            sender.sendMessage(Component.text("Example plugin enabled: ").color(NamedTextColor.YELLOW)
                .append(Component.text(String.valueOf(getConfig().getBoolean("settings.enable-example-plugin", true))).color(net.kyori.adventure.text.format.NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("Debug mode: ").color(NamedTextColor.YELLOW)
                .append(Component.text(String.valueOf(getConfig().getBoolean("settings.debug-mode", false))).color(net.kyori.adventure.text.format.NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("Auto-reload: ").color(NamedTextColor.YELLOW)
                .append(Component.text(String.valueOf(getConfig().getBoolean("settings.auto-reload", false))).color(net.kyori.adventure.text.format.NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("Disabled plugins: ").color(NamedTextColor.YELLOW)
                .append(Component.text(getConfig().getStringList("plugins.disabled-plugins").toString()).color(net.kyori.adventure.text.format.NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("Load order: ").color(NamedTextColor.YELLOW)
                .append(Component.text(getConfig().getStringList("plugins.load-order").isEmpty() ? "none" : 
                getConfig().getStringList("plugins.load-order").toString()).color(net.kyori.adventure.text.format.NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("Max execution time: ").color(NamedTextColor.YELLOW)
                .append(Component.text(getConfig().getLong("performance.max-execution-time", 5000) + "ms").color(net.kyori.adventure.text.format.NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("Restrict file access: ").color(NamedTextColor.YELLOW)
                .append(Component.text(String.valueOf(getConfig().getBoolean("security.restrict-file-access", false))).color(net.kyori.adventure.text.format.NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("Use /jsconfig reload to reload the config file").color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
            return true;
        }
        return false;
    }
}
