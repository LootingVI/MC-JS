package de.flori.mCJS;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class MCJS extends JavaPlugin {

    private JSPluginManager jsPluginManager;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("========================================");
        getLogger().info("MC-JS Plugin v" + getDescription().getVersion() + " enabled!");
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
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                return true;
            }

            if (args.length > 0) {
                // Reload specific plugin
                String pluginName = args[0];
                if (jsPluginManager != null && jsPluginManager.getLoadedPlugins().containsKey(pluginName)) {
                    jsPluginManager.reloadPlugin(pluginName);
                    sender.sendMessage(ChatColor.GREEN + "Reloaded JS plugin: " + pluginName);
                    getLogger().info("Reloaded JS plugin '" + pluginName + "' by " + sender.getName());
                } else {
                    sender.sendMessage(ChatColor.RED + "Plugin '" + pluginName + "' not found!");
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

            sender.sendMessage(ChatColor.GREEN + "Reloaded " + jsPluginManager.getLoadedPlugins().size() + " JS plugin(s)!");
            getLogger().info("Reloaded JS plugins by " + sender.getName());
            return true;
        } else if (command.getName().equalsIgnoreCase("jslist")) {
            if (!sender.hasPermission("mcjs.admin")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                return true;
            }

            if (jsPluginManager != null) {
                var plugins = jsPluginManager.getLoadedPlugins();
                if (plugins.isEmpty()) {
                    sender.sendMessage(ChatColor.YELLOW + "No JS plugins loaded.");
                } else {
                    sender.sendMessage(ChatColor.GREEN + "Loaded JS plugins (" + plugins.size() + "):");
                    for (String pluginName : plugins.keySet()) {
                        JSPluginManager.PluginMetadata metadata = jsPluginManager.getMetadata(pluginName);
                        if (metadata != null && metadata.getVersion() != null) {
                            sender.sendMessage(ChatColor.GRAY + "  - " + ChatColor.WHITE + pluginName + 
                                ChatColor.GRAY + " v" + metadata.getVersion());
                        } else {
                            sender.sendMessage(ChatColor.GRAY + "  - " + ChatColor.WHITE + pluginName);
                        }
                    }
                }
            }
            return true;
        } else if (command.getName().equalsIgnoreCase("jsconfig")) {
            if (!sender.hasPermission("mcjs.admin")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                return true;
            }

            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "Configuration reloaded!");
                getLogger().info("Configuration reloaded by " + sender.getName());
                return true;
            }

            // Show config info
            sender.sendMessage(ChatColor.GREEN + "=== MC-JS Configuration ===");
            sender.sendMessage(ChatColor.YELLOW + "Example plugin enabled: " + 
                ChatColor.WHITE + getConfig().getBoolean("settings.enable-example-plugin", true));
            sender.sendMessage(ChatColor.YELLOW + "Debug mode: " + 
                ChatColor.WHITE + getConfig().getBoolean("settings.debug-mode", false));
            sender.sendMessage(ChatColor.YELLOW + "Auto-reload: " + 
                ChatColor.WHITE + getConfig().getBoolean("settings.auto-reload", false));
            sender.sendMessage(ChatColor.YELLOW + "Disabled plugins: " + 
                ChatColor.WHITE + getConfig().getStringList("plugins.disabled-plugins").toString());
            sender.sendMessage(ChatColor.GRAY + "Use /jsconfig reload to reload the config file");
            return true;
        }
        return false;
    }
}
