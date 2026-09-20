package de.flori.mCJS;

import org.bukkit.plugin.java.JavaPlugin;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Function;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JSPluginManager {
    private final JavaPlugin plugin;
    private final Map<String, Scriptable> pluginScopes;
    private final Map<String, PluginMetadata> pluginMetadata;
    private final Map<String, MCJSAPI> pluginApis;
    private final Map<String, de.flori.mCJS.api.DebugAPI> debuggers = new ConcurrentHashMap<>();

    public de.flori.mCJS.api.DebugAPI debugger(String name) {
        de.flori.mCJS.dashboard.PluginWorkspace.validateName(name);
        return debuggers.computeIfAbsent(name, key -> new de.flori.mCJS.api.DebugAPI());
    }

    public JSPluginManager(JavaPlugin plugin) {
        this.plugin = plugin;

        this.pluginScopes = new ConcurrentHashMap<>();
        this.pluginMetadata = new ConcurrentHashMap<>();
        this.pluginApis = new ConcurrentHashMap<>();
    }

    private boolean isDebugMode() {
        return plugin.getConfig().getBoolean("settings.debug-mode", false);
    }

    private void debug(String message) {
        if (isDebugMode()) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }

    public void loadPlugins() {
        debug("Starting plugin loading process...");
        File jsPluginsDir = new File(plugin.getDataFolder(), "js-plugins");
        if (!jsPluginsDir.exists()) {
            jsPluginsDir.mkdirs();
            plugin.getLogger().info("Created js-plugins directory: " + jsPluginsDir.getAbsolutePath());
            debug("Created js-plugins directory: " + jsPluginsDir.getAbsolutePath());

            if (isExamplePluginEnabled()) {
                try {
                    File exampleFile = new File(jsPluginsDir, "example.js");
                    if (!exampleFile.exists()) {
                        plugin.saveResource("js-plugins/example.js", false);
                        plugin.getLogger().info("Copied example.js to js-plugins directory");
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Could not copy example plugin: " + e.getMessage());
                }
            } else {
                plugin.getLogger().info("Example plugin is disabled in config, skipping copy");
            }
        }

        plugin.getLogger().info("Scanning for JS plugins in: " + jsPluginsDir.getAbsolutePath());
        debug("Scanning directory: " + jsPluginsDir.getAbsolutePath());
        File[] pluginFiles = jsPluginsDir.listFiles((dir, name) -> name.endsWith(".js"));

        if (pluginFiles == null || pluginFiles.length == 0) {
            plugin.getLogger().info("No JS plugins found in js-plugins directory");
            debug("No .js files found in directory");
            return;
        }

        plugin.getLogger().info("Found " + pluginFiles.length + " JS plugin file(s)");
        debug("Found " + pluginFiles.length + " plugin file(s) to process");

        java.util.List<File> sortedPlugins = sortPluginsByLoadOrder(pluginFiles);

        for (File pluginFile : sortedPlugins) {
            try {
                String pluginName = pluginFile.getName().replace(".js", "");
                debug("Processing plugin file: " + pluginFile.getName() + " (name: " + pluginName + ")");

                if (isPluginDisabled(pluginName)) {
                    plugin.getLogger().info("Skipping disabled plugin: " + pluginFile.getName());
                    debug("Plugin " + pluginName + " is disabled in config, skipping");
                    continue;
                }

                plugin.getLogger().info("Loading JS plugin: " + pluginFile.getName());
                debug("Starting load process for plugin: " + pluginName);
                loadPlugin(pluginFile);
                debug("Successfully loaded plugin: " + pluginName);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load JS plugin: " + pluginFile.getName() + " - " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void loadPlugin(File pluginFile) throws IOException {
        String pluginName = pluginFile.getName().replace(".js", "");
        String script = Files.readString(pluginFile.toPath());
        if (script.isBlank()) throw new IOException("Plugin is empty: " + pluginName);
        var debugger = debugger(pluginName);
        debugger.generation(de.flori.mCJS.dashboard.PluginWorkspace.revision(script));
        MCJSAPI api = new MCJSAPI(plugin, debugger);
        try (org.mozilla.javascript.Context context = org.mozilla.javascript.Context.enter()) {
            context.setOptimizationLevel(plugin.getConfig().getInt("performance.optimization-level", -1));
            context.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
            Scriptable scope = context.initStandardObjects();
            api.setRhinoScope(scope);
            Map<String, Object> globals = new java.util.LinkedHashMap<>();
            globals.put("api", api);
            globals.put("server", plugin.getServer());
            globals.put("plugin", plugin);
            globals.put("logger", plugin.getLogger());
            globals.put("scheduler", plugin.getServer().getScheduler());
            globals.put("Bukkit", plugin.getServer());
            globals.put("Java", java.lang.System.class);
            for (Class<?> type : new Class<?>[]{org.bukkit.entity.Player.class, org.bukkit.entity.Entity.class,
                    org.bukkit.Material.class, org.bukkit.ChatColor.class, org.bukkit.Location.class, org.bukkit.World.class,
                    org.bukkit.block.Block.class, org.bukkit.inventory.ItemStack.class, org.bukkit.event.inventory.InventoryType.class,
                    org.bukkit.GameMode.class, org.bukkit.potion.PotionEffectType.class, org.bukkit.Sound.class,
                    org.bukkit.event.EventPriority.class, org.bukkit.command.CommandSender.class, org.bukkit.OfflinePlayer.class}) {
                globals.put(type.getSimpleName(), type);
            }
            globals.forEach((key, value) -> ScriptableObject.putProperty(scope, key,
                    org.mozilla.javascript.Context.javaToJS(value, scope)));
            context.evaluateString(scope, script, pluginFile.getName(), 1, null);
            Object onEnable = ScriptableObject.getProperty(scope, "onEnable");
            if (onEnable instanceof Function function) function.call(context, scope, scope, new Object[0]);
            pluginScopes.put(pluginName, scope);
            pluginApis.put(pluginName, api);
            pluginMetadata.put(pluginName, extractMetadata(scope, pluginName));
            plugin.getLogger().info("Loaded JS plugin: " + pluginName);
        } catch (Exception error) {
            try {
                api.unload();
            } catch (Exception cleanup) {
                error.addSuppressed(cleanup);
            }
            throw new IOException("Failed to execute " + pluginName + ": " + error.getMessage(), error);
        }
    }

    private PluginMetadata extractMetadata(Scriptable scope, String pluginName) {
        PluginMetadata metadata = new PluginMetadata(pluginName);
        try {
            Object pluginInfoObj = scope.get("pluginInfo", scope);
            if (pluginInfoObj instanceof Scriptable) {
                Scriptable pluginInfo = (Scriptable) pluginInfoObj;
                Object nameObj = ScriptableObject.getProperty(pluginInfo, "name");
                if (nameObj != null) {
                    metadata.setName(nameObj.toString());
                }
                Object versionObj = ScriptableObject.getProperty(pluginInfo, "version");
                if (versionObj != null) {
                    metadata.setVersion(versionObj.toString());
                }
                Object authorObj = ScriptableObject.getProperty(pluginInfo, "author");
                if (authorObj != null) {
                    metadata.setAuthor(authorObj.toString());
                }
                Object descObj = ScriptableObject.getProperty(pluginInfo, "description");
                if (descObj != null) {
                    metadata.setDescription(descObj.toString());
                }
            }
        } catch (Exception e) {

        }
        return metadata;
    }

    public void unloadPlugins() {
        debug("Starting plugin unload process for " + pluginScopes.size() + " plugin(s)");
        for (Map.Entry<String, Scriptable> entry : pluginScopes.entrySet()) {
            try {
                String pluginName = entry.getKey();
                debug("Unloading plugin: " + pluginName);
                Scriptable scope = entry.getValue();

                if (scope != null) {

                    org.mozilla.javascript.Context context = org.mozilla.javascript.Context.enter();
                    try {
                        context.setOptimizationLevel(-1);
                        context.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);

                        Object onDisableObj = scope.get("onDisable", scope);
                        if (onDisableObj == null || !(onDisableObj instanceof Function)) {

                            Object thisObj = scope.get("this", scope);
                            if (thisObj instanceof Scriptable) {
                                Scriptable thisScope = (Scriptable) thisObj;
                                onDisableObj = thisScope.get("onDisable", thisScope);
                            }
                        }

                        if (onDisableObj instanceof Function) {
                            try {
                                debug("Calling onDisable() for " + pluginName);
                                Function onDisable = (Function) onDisableObj;
                                onDisable.call(context, scope, scope, new Object[0]);
                                debug("onDisable() completed for " + pluginName);
                            } catch (Exception e) {
                                plugin.getLogger().warning("Error calling onDisable for plugin " + entry.getKey() + ": " + e.getMessage());
                                debug("Error in onDisable() for " + pluginName + ": " + e.getMessage());
                            }
                        } else {
                            debug("No onDisable function found for " + pluginName);
                        }
                    } finally {
                        org.mozilla.javascript.Context.exit();
                    }
                }
                MCJSAPI api = pluginApis.remove(pluginName);
                if (api != null) {
                    api.unload();
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error disabling JS plugin: " + entry.getKey() + " - " + e.getMessage());
                e.printStackTrace();
            }
        }
        pluginScopes.clear();
        pluginMetadata.clear();
        pluginApis.clear();
    }

    public Scriptable getPluginScope(String name) {
        return pluginScopes.get(name);
    }

    public Map<String, Scriptable> getLoadedPlugins() {
        return new HashMap<>(pluginScopes);
    }

    public Map<String, PluginMetadata> getPluginMetadata() {
        return new HashMap<>(pluginMetadata);
    }

    public PluginMetadata getMetadata(String pluginName) {
        return pluginMetadata.get(pluginName);
    }

    public void unloadPlugin(String pluginName) {
        Scriptable scope = pluginScopes.remove(pluginName);
        try {
            if (scope != null) {
                try (org.mozilla.javascript.Context context = org.mozilla.javascript.Context.enter()) {
                    context.setOptimizationLevel(-1);
                    context.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                    Object onDisable = ScriptableObject.getProperty(scope, "onDisable");
                    if (onDisable instanceof Function function) function.call(context, scope, scope, new Object[0]);
                }
            }
        } catch (Exception error) {
            plugin.getLogger().warning("Error disabling " + pluginName + ": " + error.getMessage());
        } finally {
            MCJSAPI api = pluginApis.remove(pluginName);
            try {
                if (api != null) api.unload();
            } finally {
                pluginMetadata.remove(pluginName);
            }
        }
    }

    public void activatePlugin(String pluginName) throws IOException {
        de.flori.mCJS.dashboard.PluginWorkspace.validateName(pluginName);
        if (isPluginDisabled(pluginName)) throw new IOException("Plugin is disabled in config: " + pluginName);
        File file = new File(plugin.getDataFolder(), "js-plugins/" + pluginName + ".js");
        if (!file.isFile() || Files.isSymbolicLink(file.toPath())) throw new IOException("Plugin file not found");
        de.flori.mCJS.dashboard.PluginWorkspace.validate(Files.readString(file.toPath()));
        unloadPlugin(pluginName);
        loadPlugin(file);
    }

    public void reloadPlugin(String pluginName) {
        try {
            activatePlugin(pluginName);
        } catch (Exception error) {
            plugin.getLogger().severe("Failed to reload plugin " + pluginName + ": " + error.getMessage());
        }
    }

    public boolean isPluginDisabled(String pluginName) {

        if (pluginName.equalsIgnoreCase("example") && !isExamplePluginEnabled()) {
            return true;
        }

        java.util.List<String> disabledPlugins = plugin.getConfig().getStringList("plugins.disabled-plugins");
        if (disabledPlugins != null) {
            for (String disabled : disabledPlugins) {
                if (disabled.equalsIgnoreCase(pluginName)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isExamplePluginEnabled() {
        return plugin.getConfig().getBoolean("settings.enable-example-plugin", true);
    }

    private java.util.List<File> sortPluginsByLoadOrder(File[] pluginFiles) {
        java.util.List<String> loadOrder = plugin.getConfig().getStringList("plugins.load-order");

        if (loadOrder == null || loadOrder.isEmpty()) {

            return java.util.Arrays.asList(pluginFiles);
        }

        java.util.List<File> sorted = new java.util.ArrayList<>();
        java.util.List<File> remaining = new java.util.ArrayList<>(java.util.Arrays.asList(pluginFiles));

        for (String orderedName : loadOrder) {
            for (File file : remaining) {
                String fileName = file.getName().replace(".js", "");
                if (fileName.equalsIgnoreCase(orderedName)) {
                    sorted.add(file);
                    remaining.remove(file);
                    break;
                }
            }
        }

        sorted.addAll(remaining);

        debug("Plugin load order: " + sorted.stream()
            .map(f -> f.getName().replace(".js", ""))
            .collect(java.util.stream.Collectors.joining(", ")));

        return sorted;
    }

    public static class PluginMetadata {
        private String name;
        private String version;
        private String author;
        private String description;

        public PluginMetadata(String name) {
            this.name = name;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
