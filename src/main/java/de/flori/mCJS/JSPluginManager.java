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

public class JSPluginManager {
    private final JavaPlugin plugin;
    private final Map<String, Scriptable> pluginScopes;
    private final Map<String, PluginMetadata> pluginMetadata;

    public JSPluginManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.pluginScopes = new HashMap<>();
        this.pluginMetadata = new HashMap<>();
    }

    public void loadPlugins() {
        File jsPluginsDir = new File(plugin.getDataFolder(), "js-plugins");
        if (!jsPluginsDir.exists()) {
            jsPluginsDir.mkdirs();
            plugin.getLogger().info("Created js-plugins directory: " + jsPluginsDir.getAbsolutePath());
            
            // Copy example plugin if it doesn't exist and is enabled in config
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

        // Load plugins from directory
        plugin.getLogger().info("Scanning for JS plugins in: " + jsPluginsDir.getAbsolutePath());
        File[] pluginFiles = jsPluginsDir.listFiles((dir, name) -> name.endsWith(".js"));
        
        if (pluginFiles == null || pluginFiles.length == 0) {
            plugin.getLogger().info("No JS plugins found in js-plugins directory");
            return;
        }

        plugin.getLogger().info("Found " + pluginFiles.length + " JS plugin file(s)");
        for (File pluginFile : pluginFiles) {
            try {
                String pluginName = pluginFile.getName().replace(".js", "");
                
                // Check if plugin is disabled in config
                if (isPluginDisabled(pluginName)) {
                    plugin.getLogger().info("Skipping disabled plugin: " + pluginFile.getName());
                    continue;
                }
                
                plugin.getLogger().info("Loading JS plugin: " + pluginFile.getName());
                loadPlugin(pluginFile);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load JS plugin: " + pluginFile.getName() + " - " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void loadPlugin(File pluginFile) throws IOException {
        String pluginName = pluginFile.getName().replace(".js", "");
        String script = Files.readString(Path.of(pluginFile.toURI()));

        if (script == null || script.trim().isEmpty()) {
            plugin.getLogger().warning("Plugin file " + pluginFile.getName() + " is empty, skipping");
            return;
        }

        // Create a new Rhino context for this plugin (isolated execution)
        org.mozilla.javascript.Context rhinoContext = org.mozilla.javascript.Context.enter();
        int optimizationLevel = plugin.getConfig().getInt("performance.optimization-level", -1);
        rhinoContext.setOptimizationLevel(optimizationLevel);
        rhinoContext.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);

        try {
            // Create a new scope for this plugin
            Scriptable scope = rhinoContext.initStandardObjects();
            
            // Initialize API
            JSAPI api = new JSAPI(plugin);
            api.setRhinoScope(scope);
            ScriptableObject.putProperty(scope, "api", org.mozilla.javascript.Context.javaToJS(api, scope));
            ScriptableObject.putProperty(scope, "server", org.mozilla.javascript.Context.javaToJS(plugin.getServer(), scope));
            ScriptableObject.putProperty(scope, "plugin", org.mozilla.javascript.Context.javaToJS(plugin, scope));
            ScriptableObject.putProperty(scope, "logger", org.mozilla.javascript.Context.javaToJS(plugin.getLogger(), scope));
            ScriptableObject.putProperty(scope, "scheduler", org.mozilla.javascript.Context.javaToJS(plugin.getServer().getScheduler(), scope));
            ScriptableObject.putProperty(scope, "Bukkit", org.mozilla.javascript.Context.javaToJS(plugin.getServer(), scope));
            ScriptableObject.putProperty(scope, "Java", org.mozilla.javascript.Context.javaToJS(java.lang.System.class, scope));

            // Execute the script
            rhinoContext.evaluateString(scope, script, pluginFile.getName(), 1, null);

            // Store scope (we don't need to store context - it's thread-local and we'll create new ones as needed)
            pluginScopes.put(pluginName, scope);

            // Extract metadata if available
            PluginMetadata metadata = extractMetadata(scope, pluginName);
            pluginMetadata.put(pluginName, metadata);

            // Call onEnable if it exists (BEFORE exiting the context!)
            // In Rhino, functions defined with 'function onEnable()' are available directly in scope
            // And 'this.onEnable = onEnable' also makes it available as a property
            Object onEnableObj = null;
            try {
                // Try to get the function from scope
                onEnableObj = scope.get("onEnable", scope);
                
                // If not a function, try ScriptableObject.getProperty
                if (!(onEnableObj instanceof Function)) {
                    try {
                        onEnableObj = ScriptableObject.getProperty(scope, "onEnable");
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().fine("Could not find onEnable function: " + e.getMessage());
            }
            
            if (onEnableObj instanceof Function) {
                try {
                    Function onEnable = (Function) onEnableObj;
                    onEnable.call(rhinoContext, scope, scope, new Object[0]);
                    plugin.getLogger().info("Loaded JS plugin: " + pluginName + 
                        (metadata.getVersion() != null ? " v" + metadata.getVersion() : ""));
                } catch (Exception e) {
                    plugin.getLogger().warning("Error calling onEnable for plugin " + pluginName + ": " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                // Log what we found for debugging
                if (onEnableObj != null) {
                    plugin.getLogger().info("onEnable found but is not a Function: " + onEnableObj.getClass().getName());
                }
                plugin.getLogger().info("Loaded JS plugin: " + pluginName + 
                    (metadata.getVersion() != null ? " v" + metadata.getVersion() : "") + " (no onEnable function found)");
            }
            
            // Exit the context AFTER onEnable has been called
            // We'll create new contexts for callbacks as needed
            org.mozilla.javascript.Context.exit();
        } catch (Exception e) {
            // Make sure to exit context even on error
            try {
                org.mozilla.javascript.Context.exit();
            } catch (Exception ex) {
                // Ignore if context was already exited
            }
            throw new IOException("Failed to execute script: " + e.getMessage(), e);
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
            // Metadata extraction failed, use defaults
        }
        return metadata;
    }

    public void unloadPlugins() {
        for (Map.Entry<String, Scriptable> entry : pluginScopes.entrySet()) {
            try {
                Scriptable scope = entry.getValue();
                
                if (scope != null) {
                    // Create a new context for onDisable
                    org.mozilla.javascript.Context context = org.mozilla.javascript.Context.enter();
                    try {
                        context.setOptimizationLevel(-1);
                        context.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                        
                        Object onDisableObj = scope.get("onDisable", scope);
                        if (onDisableObj == null || !(onDisableObj instanceof Function)) {
                            // Try to get from 'this'
                            Object thisObj = scope.get("this", scope);
                            if (thisObj instanceof Scriptable) {
                                Scriptable thisScope = (Scriptable) thisObj;
                                onDisableObj = thisScope.get("onDisable", thisScope);
                            }
                        }
                        
                        if (onDisableObj instanceof Function) {
                            try {
                                Function onDisable = (Function) onDisableObj;
                                onDisable.call(context, scope, scope, new Object[0]);
                            } catch (Exception e) {
                                plugin.getLogger().warning("Error calling onDisable for plugin " + entry.getKey() + ": " + e.getMessage());
                            }
                        }
                    } finally {
                        org.mozilla.javascript.Context.exit();
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error disabling JS plugin: " + entry.getKey() + " - " + e.getMessage());
                e.printStackTrace();
            }
        }
        pluginScopes.clear();
        pluginMetadata.clear();
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

    public void reloadPlugin(String pluginName) {
        Scriptable scope = pluginScopes.get(pluginName);
        
        if (scope != null) {
            try {
                // Create a new context for onDisable
                org.mozilla.javascript.Context context = org.mozilla.javascript.Context.enter();
                try {
                    context.setOptimizationLevel(-1);
                    context.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                    
                    Object onDisableObj = scope.get("onDisable", scope);
                    if (onDisableObj instanceof Function) {
                        Function onDisable = (Function) onDisableObj;
                        onDisable.call(context, scope, scope, new Object[0]);
                    }
                } finally {
                    org.mozilla.javascript.Context.exit();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error disabling plugin during reload: " + e.getMessage());
            }
            pluginScopes.remove(pluginName);
            pluginMetadata.remove(pluginName);
        }

        // Reload the plugin file
        File pluginFile = new File(plugin.getDataFolder(), "js-plugins/" + pluginName + ".js");
        if (pluginFile.exists()) {
            try {
                loadPlugin(pluginFile);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to reload plugin " + pluginName + ": " + e.getMessage());
            }
        }
    }

    /**
     * Check if a plugin is disabled in the config
     */
    private boolean isPluginDisabled(String pluginName) {
        // Check if example plugin is disabled
        if (pluginName.equalsIgnoreCase("example")) {
            return !isExamplePluginEnabled();
        }
        
        // Check disabled-plugins list
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
    
    /**
     * Check if example plugin is enabled in config
     */
    private boolean isExamplePluginEnabled() {
        return plugin.getConfig().getBoolean("settings.enable-example-plugin", true);
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
