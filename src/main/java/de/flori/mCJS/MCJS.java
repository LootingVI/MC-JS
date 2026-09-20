package de.flori.mCJS;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import de.flori.mCJS.api.EventAPI;
import de.flori.mCJS.api.InventoryAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import de.flori.mCJS.dashboard.DashboardService;

public final class MCJS extends JavaPlugin {

    private JSPluginManager jsPluginManager;
    private DashboardService dashboard;

    public JSPluginManager getJSPluginManager() {
        return jsPluginManager;
    }

    private void restartDashboard() {
        if (dashboard != null) {
            dashboard.close();
            dashboard = null;
        }
        if (getConfig().getBoolean("dashboard.enabled", true)) {
            try {
                dashboard = new DashboardService(this);
            } catch (Exception error) {
                getLogger().severe("Dashboard could not start: " + error.getMessage());
            }
        }
    }

    @Override
    public void onEnable() {

        getLogger().info("========================================");
        getLogger().info(Version.getVersionInfo() + " enabled!");
        getLogger().info("Using Rhino JavaScript Engine");
        getLogger().info("========================================");

        saveDefaultConfig();

        reloadConfig();

        try {
            jsPluginManager = new JSPluginManager(this);

            jsPluginManager.loadPlugins();

            int loadedCount = jsPluginManager.getLoadedPlugins().size();
            getLogger().info("Successfully loaded " + loadedCount + " JS plugin(s)");

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
            return;
        }
        restartDashboard();
    }

    @Override
    public void onDisable() {
        if (dashboard != null) {
            dashboard.close();
            dashboard = null;
        }

        if (jsPluginManager != null) {
            jsPluginManager.unloadPlugins();
        }
        EventAPI.reset(this);
        InventoryAPI.resetEventRegistration();
        getLogger().info("MC-JS Plugin disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("mjs")) {
            if (dashboard == null) {
                sender.sendMessage(Component.text("Dashboard unavailable. Check the dashboard settings in config.yml and the server console.", NamedTextColor.RED));
                return true;
            }
            return dashboard.command(sender, args);
        }
        if (command.getName().equalsIgnoreCase("jsreload")) {
            if (!sender.hasPermission("mcjs.admin")) {
                sender.sendMessage(Component.text("You don't have permission to use this command!").color(NamedTextColor.RED));
                return true;
            }

            if (args.length > 0) {

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

            reloadConfig();

            if (jsPluginManager != null) {
                jsPluginManager.unloadPlugins();
            }
            jsPluginManager = new JSPluginManager(this);
            jsPluginManager.loadPlugins();

            restartDashboard();

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
                restartDashboard();
                sender.sendMessage(Component.text("Configuration reloaded!").color(NamedTextColor.GREEN));
                getLogger().info("Configuration reloaded by " + sender.getName());
                return true;
            }

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

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("mjs")) {
            return args.length == 1 && sender.hasPermission("mcjs.dashboard") && "dashboard".startsWith(args[0].toLowerCase(java.util.Locale.ROOT))
                    ? java.util.List.of("dashboard") : java.util.List.of();
        }
        return null;
    }
}
