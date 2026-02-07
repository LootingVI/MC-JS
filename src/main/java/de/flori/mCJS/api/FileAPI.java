package de.flori.mCJS.api;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * API module for file I/O operations (YAML, JSON, Text)
 */
public class FileAPI extends BaseAPI {
    
    public FileAPI(JavaPlugin plugin) {
        super(plugin);
    }
    
    /**
     * Check if file access is restricted and if the path is allowed
     */
    private boolean isFileAccessAllowed(String filePath) {
        boolean restrictAccess = plugin.getConfig().getBoolean("security.restrict-file-access", false);
        if (!restrictAccess) {
            return true; // No restrictions
        }
        
        java.util.List<String> allowedPaths = plugin.getConfig().getStringList("security.allowed-paths");
        if (allowedPaths == null || allowedPaths.isEmpty()) {
            return false; // Restricted but no allowed paths = deny all
        }
        
        String normalizedPath = filePath.replace("\\", "/");
        for (String allowedPath : allowedPaths) {
            String normalizedAllowed = allowedPath.replace("\\", "/");
            if (normalizedPath.startsWith(normalizedAllowed)) {
                return true;
            }
        }
        
        plugin.getLogger().warning("File access denied for: " + filePath + " (not in allowed paths)");
        return false;
    }
    
    // ===== YAML FILE MANAGEMENT =====
    public void saveYamlFile(String fileName, Map<String, Object> data) {
        try {
            File file = new File(plugin.getDataFolder(), fileName + ".yml");
            String filePath = file.getAbsolutePath();
            
            if (!isFileAccessAllowed(filePath)) {
                plugin.getLogger().warning("Access denied: Cannot save YAML file '" + fileName + "' (security restriction)");
                return;
            }
            
            org.bukkit.configuration.file.YamlConfiguration config = new org.bukkit.configuration.file.YamlConfiguration();

            // Convert Map to ConfigurationSection
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                config.set(entry.getKey(), entry.getValue());
            }

            config.save(file);
        } catch (Exception e) {
            plugin.getLogger().severe("Error saving YAML file '" + fileName + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Map<String, Object> loadYamlFile(String fileName) {
        try {
            File file = new File(plugin.getDataFolder(), fileName + ".yml");
            String filePath = file.getAbsolutePath();
            
            if (!isFileAccessAllowed(filePath)) {
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
        File file = new File(plugin.getDataFolder(), fileName + ".yml");
        return file.exists();
    }

    public void deleteYamlFile(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName + ".yml");
        if (file.exists()) {
            file.delete();
        }
    }

    // ===== JSON FILE MANAGEMENT =====
    public void saveJsonFile(String fileName, String jsonContent) {
        try {
            File file = new File(plugin.getDataFolder(), fileName + ".json");
            String filePath = file.getAbsolutePath();
            
            if (!isFileAccessAllowed(filePath)) {
                plugin.getLogger().warning("Access denied: Cannot save JSON file '" + fileName + "' (security restriction)");
                return;
            }
            
            java.nio.file.Files.write(file.toPath(), jsonContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            plugin.getLogger().severe("Error saving JSON file '" + fileName + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String loadJsonFile(String fileName) {
        try {
            File file = new File(plugin.getDataFolder(), fileName + ".json");
            String filePath = file.getAbsolutePath();
            
            if (!isFileAccessAllowed(filePath)) {
                plugin.getLogger().warning("Access denied: Cannot load JSON file '" + fileName + "' (security restriction)");
                return "{}";
            }
            
            if (!file.exists()) {
                return "{}";
            }

            return new String(java.nio.file.Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading JSON file '" + fileName + "': " + e.getMessage());
            e.printStackTrace();
            return "{}";
        }
    }

    public boolean jsonFileExists(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName + ".json");
        return file.exists();
    }

    public void deleteJsonFile(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName + ".json");
        if (file.exists()) {
            file.delete();
        }
    }

    // ===== TEXT FILE MANAGEMENT =====
    public void saveTextFile(String fileName, String content) {
        try {
            File file = new File(plugin.getDataFolder(), fileName + ".txt");
            String filePath = file.getAbsolutePath();
            
            if (!isFileAccessAllowed(filePath)) {
                plugin.getLogger().warning("Access denied: Cannot save text file '" + fileName + "' (security restriction)");
                return;
            }
            
            java.nio.file.Files.write(file.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            plugin.getLogger().severe("Error saving text file '" + fileName + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String loadTextFile(String fileName) {
        try {
            File file = new File(plugin.getDataFolder(), fileName + ".txt");
            String filePath = file.getAbsolutePath();
            
            if (!isFileAccessAllowed(filePath)) {
                plugin.getLogger().warning("Access denied: Cannot load text file '" + fileName + "' (security restriction)");
                return "";
            }
            
            if (!file.exists()) {
                return "";
            }

            return new String(java.nio.file.Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading text file '" + fileName + "': " + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Get a File object for a plugin file in js-plugins directory
     */
    public File getPluginFile(String fileName) {
        File pluginsDir = new File(plugin.getDataFolder(), "js-plugins");
        File pluginFile = new File(pluginsDir, fileName.endsWith(".js") ? fileName : fileName + ".js");
        return pluginFile;
    }
    
    /**
     * Check if a plugin file exists
     */
    public boolean pluginFileExists(String fileName) {
        return getPluginFile(fileName).exists();
    }
    
    public boolean textFileExists(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName + ".txt");
        return file.exists();
    }

    public void deleteTextFile(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName + ".txt");
        if (file.exists()) {
            file.delete();
        }
    }
}
