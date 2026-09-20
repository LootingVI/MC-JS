package de.flori.mCJS.api;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class FileAPI extends BaseAPI {

    public FileAPI(JavaPlugin plugin) {
        super(plugin);
    }

    public Object getPluginConfigValue(String name, String key, Object fallback) {
        synchronized (plugin) {
            File file = pluginConfigFile(name);
            return org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file).get(key, fallback);
        }
    }

    public void setPluginConfigValue(String name, String key, Object value) {
        synchronized (plugin) {
            File file = pluginConfigFile(name);
            var config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
            config.set(key, persistentValue(value));
            try {
                Files.createDirectories(file.toPath().getParent());
                config.save(file);
            } catch (java.io.IOException error) {
                throw new IllegalStateException("Could not save plugin config: " + name, error);
            }
        }
    }

    public com.google.gson.JsonObject inspectSystemData(String name, String scopeFilter, String query, int offset) {
        synchronized (plugin) {
            File file = pluginConfigFile(name);
            if (Files.isSymbolicLink(file.toPath()) || Files.isSymbolicLink(file.toPath().getParent())) throw new IllegalStateException("Config must not be a symbolic link");
            if (file.length() > 4 * 1024 * 1024) throw new IllegalArgumentException("Config exceeds the inspector limit of 4 MiB");
            var config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
            var root = config.getConfigurationSection("systems");
            var rows = new java.util.ArrayList<com.google.gson.JsonObject>();
            if (root != null) for (String encodedScope : root.getKeys(false)) {
                String scope;
                try { scope = new String(java.util.Base64.getUrlDecoder().decode(encodedScope), java.nio.charset.StandardCharsets.UTF_8); }
                catch (IllegalArgumentException ignored) { continue; }
                if (!scopeFilter.isBlank() && !scope.toLowerCase(java.util.Locale.ROOT).contains(scopeFilter.toLowerCase(java.util.Locale.ROOT))) continue;
                var section = root.getConfigurationSection(encodedScope);
                if (section == null) continue;
                for (String encodedKey : section.getKeys(false)) {
                    String key;
                    try { key = new String(java.util.Base64.getUrlDecoder().decode(encodedKey), java.nio.charset.StandardCharsets.UTF_8); }
                    catch (IllegalArgumentException ignored) { continue; }
                    String raw = section.getString(encodedKey, "null");
                    if (!(scope + " " + key + " " + raw).toLowerCase(java.util.Locale.ROOT).contains(query.toLowerCase(java.util.Locale.ROOT))) continue;
                    var row = new com.google.gson.JsonObject();
                    row.addProperty("scope", scope); row.addProperty("key", key);
                    row.addProperty("truncated", raw.length() > 8192);
                    if (raw.length() > 8192) row.addProperty("value", raw.substring(0, 8192));
                    else try { row.add("value", com.google.gson.JsonParser.parseString(raw)); }
                    catch (RuntimeException ignored) { row.addProperty("value", raw); }
                    if (scope.startsWith("cooldowns:")) try { row.addProperty("remainingSeconds", Math.max(0, Math.ceil((Double.parseDouble(raw) - System.currentTimeMillis()) / 1000))); } catch (NumberFormatException ignored) {}
                    rows.add(row);
                }
            }
            rows.sort(java.util.Comparator.comparing(row -> row.get("scope").getAsString() + "\0" + row.get("key").getAsString()));
            var result = new com.google.gson.JsonObject();
            result.addProperty("total", rows.size()); result.addProperty("offset", offset);
            var page = new com.google.gson.JsonArray();
            rows.stream().skip(Math.max(0, offset)).limit(100).forEach(page::add);
            result.add("rows", page);
            return result;
        }
    }

    private File pluginConfigFile(String name) {
        de.flori.mCJS.dashboard.PluginWorkspace.validateName(name);
        File file = resolveDataFile("configs/" + name, ".yml");
        if (!isFileAccessAllowed(file)) throw new IllegalStateException("Plugin config access denied");
        return file;
    }

    private Object persistentValue(Object value) {
        if (value instanceof org.mozilla.javascript.Wrapper wrapper) return persistentValue(wrapper.unwrap());
        if (value == null || value == org.mozilla.javascript.Undefined.instance) return null;
        if (value instanceof CharSequence text) return text.toString();
        if (value instanceof Number || value instanceof Boolean) return value;
        if (value instanceof java.util.List<?> list) return list.stream().map(this::persistentValue).toList();
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new java.util.LinkedHashMap<>();
            map.forEach((key, entry) -> result.put(key.toString(), persistentValue(entry)));
            return result;
        }
        throw new IllegalArgumentException("Only strings, numbers, booleans, lists and maps can be stored");
    }

    private File resolveDataFile(String fileName, String extension) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File name cannot be blank");
        }

        Path dataFolder = plugin.getDataFolder().toPath().toAbsolutePath().normalize();
        Path file = dataFolder.resolve(fileName + extension).normalize();
        if (!file.startsWith(dataFolder)) {
            throw new IllegalArgumentException("File path must remain inside the MC-JS data folder");
        }
        return file.toFile();
    }

    private boolean isFileAccessAllowed(File file) {
        boolean restrictAccess = plugin.getConfig().getBoolean("security.restrict-file-access", false);
        if (!restrictAccess) {
            return true;
        }

        java.util.List<String> allowedPaths = plugin.getConfig().getStringList("security.allowed-paths");
        if (allowedPaths == null || allowedPaths.isEmpty()) {
            return false;
        }

        Path dataFolder = plugin.getDataFolder().toPath().toAbsolutePath().normalize();
        Path pluginsFolder = dataFolder.getParent();
        Path serverFolder = pluginsFolder != null && pluginsFolder.getParent() != null
            ? pluginsFolder.getParent()
            : dataFolder;
        Path normalizedPath = file.toPath().toAbsolutePath().normalize();
        for (String allowedPath : allowedPaths) {
            Path configuredPath = Path.of(allowedPath);
            Path normalizedAllowed = configuredPath.isAbsolute()
                ? configuredPath.toAbsolutePath().normalize()
                : serverFolder.resolve(configuredPath).normalize();
            if (normalizedPath.startsWith(normalizedAllowed)) {
                return true;
            }
        }

        plugin.getLogger().warning("File access denied for: " + normalizedPath + " (not in allowed paths)");
        return false;
    }

    public void saveYamlFile(String fileName, Map<String, Object> data) {
        try {
            File file = resolveDataFile(fileName, ".yml");

            if (!isFileAccessAllowed(file)) {
                plugin.getLogger().warning("Access denied: Cannot save YAML file '" + fileName + "' (security restriction)");
                return;
            }

            org.bukkit.configuration.file.YamlConfiguration config = new org.bukkit.configuration.file.YamlConfiguration();

            for (Map.Entry<String, Object> entry : data.entrySet()) {
                config.set(entry.getKey(), entry.getValue());
            }

            Files.createDirectories(file.toPath().getParent());
            config.save(file);
        } catch (Exception e) {
            plugin.getLogger().severe("Error saving YAML file '" + fileName + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Map<String, Object> loadYamlFile(String fileName) {
        try {
            File file = resolveDataFile(fileName, ".yml");

            if (!isFileAccessAllowed(file)) {
                plugin.getLogger().warning("Access denied: Cannot load YAML file '" + fileName + "' (security restriction)");
                return new HashMap<>();
            }

            if (!file.exists()) {
                return new HashMap<>();
            }

            org.bukkit.configuration.file.YamlConfiguration config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
            Map<String, Object> data = new HashMap<>();

            for (String key : config.getKeys(true)) {
                data.put(key, config.get(key));
            }

            return data;
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading YAML file '" + fileName + "': " + e.getMessage());
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    public boolean yamlFileExists(String fileName) {
        File file = resolveDataFile(fileName, ".yml");
        return isFileAccessAllowed(file) && file.exists();
    }

    public void deleteYamlFile(String fileName) {
        File file = resolveDataFile(fileName, ".yml");
        if (isFileAccessAllowed(file) && file.exists()) {
            file.delete();
        }
    }

    public void saveJsonFile(String fileName, String jsonContent) {
        try {
            File file = resolveDataFile(fileName, ".json");

            if (!isFileAccessAllowed(file)) {
                plugin.getLogger().warning("Access denied: Cannot save JSON file '" + fileName + "' (security restriction)");
                return;
            }

            Files.createDirectories(file.toPath().getParent());
            Files.write(file.toPath(), jsonContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            plugin.getLogger().severe("Error saving JSON file '" + fileName + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String loadJsonFile(String fileName) {
        try {
            File file = resolveDataFile(fileName, ".json");

            if (!isFileAccessAllowed(file)) {
                plugin.getLogger().warning("Access denied: Cannot load JSON file '" + fileName + "' (security restriction)");
                return "{}";
            }

            if (!file.exists()) {
                return "{}";
            }

            return new String(Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading JSON file '" + fileName + "': " + e.getMessage());
            e.printStackTrace();
            return "{}";
        }
    }

    public boolean jsonFileExists(String fileName) {
        File file = resolveDataFile(fileName, ".json");
        return isFileAccessAllowed(file) && file.exists();
    }

    public void deleteJsonFile(String fileName) {
        File file = resolveDataFile(fileName, ".json");
        if (isFileAccessAllowed(file) && file.exists()) {
            file.delete();
        }
    }

    public void saveTextFile(String fileName, String content) {
        try {
            File file = resolveDataFile(fileName, ".txt");

            if (!isFileAccessAllowed(file)) {
                plugin.getLogger().warning("Access denied: Cannot save text file '" + fileName + "' (security restriction)");
                return;
            }

            Files.createDirectories(file.toPath().getParent());
            Files.write(file.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            plugin.getLogger().severe("Error saving text file '" + fileName + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String loadTextFile(String fileName) {
        try {
            File file = resolveDataFile(fileName, ".txt");

            if (!isFileAccessAllowed(file)) {
                plugin.getLogger().warning("Access denied: Cannot load text file '" + fileName + "' (security restriction)");
                return "";
            }

            if (!file.exists()) {
                return "";
            }

            return new String(Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading text file '" + fileName + "': " + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }

    public File getPluginFile(String fileName) {
        return resolveDataFile("js-plugins/" + fileName, fileName.endsWith(".js") ? "" : ".js");
    }

    public boolean pluginFileExists(String fileName) {
        File file = getPluginFile(fileName);
        return isFileAccessAllowed(file) && file.exists();
    }

    public boolean textFileExists(String fileName) {
        File file = resolveDataFile(fileName, ".txt");
        return isFileAccessAllowed(file) && file.exists();
    }

    public void deleteTextFile(String fileName) {
        File file = resolveDataFile(fileName, ".txt");
        if (isFileAccessAllowed(file) && file.exists()) {
            file.delete();
        }
    }
}
