package de.flori.mCJS.api;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * API module for utility methods (broadcast, player lookup, server info, etc.)
 */
public class UtilityAPI extends BaseAPI {
    
    public UtilityAPI(JavaPlugin plugin) {
        super(plugin);
    }
    
    // ===== BROADCAST METHODS =====
    public void broadcast(Object message) {
        if (message == null) return;
        String messageStr = message.toString();
        Component component = legacyToComponentWithAmpersand(messageStr);
        Bukkit.getServer().broadcast(component);
    }
    
    public void broadcast(Object message, Object permission) {
        if (message == null || permission == null) return;
        String messageStr = message.toString();
        String permissionStr = permission.toString();
        Component component = legacyToComponentWithAmpersand(messageStr);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(permissionStr)) {
                player.sendMessage(component);
            }
        }
    }
    
    // ===== WORLD METHODS =====
    public World getWorld(String name) {
        return name != null ? Bukkit.getWorld(name) : null;
    }
    
    public List<World> getWorlds() {
        return Bukkit.getWorlds();
    }
    
    // ===== COLOR METHODS =====
    public String colorize(String text) {
        Component component = legacyToComponentWithAmpersand(text);
        return LegacyComponentSerializer.legacySection().serialize(component);
    }
    
    public String stripColor(String text) {
        if (text == null) {
            return null;
        }
        Component component = legacyToComponentWithAmpersand(text);
        return LegacyComponentSerializer.legacySection().serialize(component).replaceAll("§[0-9a-fk-or]", "");
    }
    
    // ===== PLUGIN METHODS =====
    public boolean isPluginEnabled(String name) {
        var plugin = this.plugin.getServer().getPluginManager().getPlugin(name);
        return plugin != null && plugin.isEnabled();
    }
    
    public org.bukkit.plugin.Plugin getPlugin(String name) {
        return plugin.getServer().getPluginManager().getPlugin(name);
    }
    
    // ===== SERVER METHODS =====
    public String getServerVersion() {
        return plugin.getServer().getVersion();
    }
    
    public String getBukkitVersion() {
        return plugin.getServer().getBukkitVersion();
    }
    
    public int getMaxPlayers() {
        return plugin.getServer().getMaxPlayers();
    }
    
    @SuppressWarnings("deprecation")
    public String getMotd() {
        return plugin.getServer().getMotd();
    }
    
    // ===== CONSOLE METHODS =====
    public void executeCommand(String command) {
        if (command != null) {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
        }
    }
    
    // ===== LOGGING METHODS =====
    public void logInfo(String message) {
        plugin.getLogger().info(message);
    }
    
    public void logWarning(String message) {
        plugin.getLogger().warning(message);
    }
    
    public void logError(String message) {
        plugin.getLogger().severe(message);
    }
    
    // ===== RANDOM METHODS =====
    public int randomInt(int min, int max) {
        return new Random().nextInt(max - min + 1) + min;
    }
    
    public double randomDouble(double min, double max) {
        return min + (max - min) * new Random().nextDouble();
    }
    
    public boolean randomBoolean() {
        return new Random().nextBoolean();
    }
    
    // ===== STRING UTILITIES =====
    public String format(String format, Object... args) {
        return String.format(format, args);
    }
    
    public String join(String delimiter, String... elements) {
        return String.join(delimiter, elements);
    }
    
    // ===== MATH UTILITIES =====
    public double round(double value, int places) {
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }
    
    public double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
    
    public int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
    
    // ===== STRING ENCODING =====
    public String base64Encode(String input) {
        return java.util.Base64.getEncoder().encodeToString(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    
    public String base64Decode(String input) {
        try {
            byte[] decoded = java.util.Base64.getDecoder().decode(input);
            return new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            plugin.getLogger().severe("Error decoding base64: " + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }
    
    public String urlEncode(String input) {
        try {
            return java.net.URLEncoder.encode(input, "UTF-8");
        } catch (Exception e) {
            plugin.getLogger().severe("Error URL encoding: " + e.getMessage());
            e.printStackTrace();
            return input;
        }
    }
    
    public String urlDecode(String input) {
        try {
            return java.net.URLDecoder.decode(input, "UTF-8");
        } catch (Exception e) {
            plugin.getLogger().severe("Error URL decoding: " + e.getMessage());
            e.printStackTrace();
            return input;
        }
    }
    
    // ===== ADVANCED UTILITY METHODS =====
    public String replacePlaceholders(String text, Map<String, String> placeholders) {
        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return result;
    }
    
    public boolean isNumber(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public double parseDouble(String str) {
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    public int parseInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    public List<String> splitString(String str, String delimiter) {
        return Arrays.asList(str.split(delimiter));
    }
    
    public String repeatString(String str, int count) {
        return str.repeat(Math.max(0, count));
    }
    
    // ===== DISTANCE AND LOCATION METHODS =====
    public double getDistance(Location loc1, Location loc2) {
        return loc1 != null && loc2 != null ? loc1.distance(loc2) : 0.0;
    }
    
    public double getDistanceSquared(Location loc1, Location loc2) {
        return loc1 != null && loc2 != null ? loc1.distanceSquared(loc2) : 0.0;
    }
    
    public Location getMidpoint(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null || loc1.getWorld() == null) {
            return null;
        }
        return new Location(
            loc1.getWorld(),
            (loc1.getX() + loc2.getX()) / 2,
            (loc1.getY() + loc2.getY()) / 2,
            (loc1.getZ() + loc2.getZ()) / 2
        );
    }
    
    // ===== ARRAY/COLLECTION UTILITIES =====
    public List<String> filterList(List<String> list, String filter) {
        List<String> filtered = new ArrayList<>();
        if (list == null || filter == null) {
            return filtered;
        }
        for (String item : list) {
            if (item != null && item.toLowerCase().contains(filter.toLowerCase())) {
                filtered.add(item);
            }
        }
        return filtered;
    }
    
    public List<String> getPlayerNames() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return names;
    }
    
    public List<String> getWorldNames() {
        List<String> names = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            names.add(world.getName());
        }
        return names;
    }
    
    // ===== VALIDATION METHODS =====
    public boolean isValidPlayer(Player player) {
        return player != null && player.isOnline();
    }
    
    public boolean isValidLocation(Location location) {
        return location != null && location.getWorld() != null;
    }
    
    public boolean isValidWorld(World world) {
        return world != null;
    }
    
    /**
     * Check if a CommandSender is a Player
     */
    public boolean isPlayer(org.bukkit.command.CommandSender sender) {
        return sender instanceof Player;
    }
    
    /**
     * Convert CommandSender to Player if possible, otherwise return null
     */
    public Player getPlayerFromSender(org.bukkit.command.CommandSender sender) {
        return sender instanceof Player ? (Player) sender : null;
    }
    
    // ===== TIME METHODS =====
    public long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }
    
    public long getCurrentTime() {
        return System.currentTimeMillis();
    }
    
    // ===== DATE AND TIME METHODS =====
    public long getCurrentTimestamp() {
        return System.currentTimeMillis() / 1000;
    }
    
    public String formatDate(long timestamp) {
        return new java.util.Date(timestamp).toString();
    }
    
    public String formatDate(long timestamp, String format) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(format);
            return sdf.format(new java.util.Date(timestamp));
        } catch (Exception e) {
            return new java.util.Date(timestamp).toString();
        }
    }
    
    // ===== MATERIAL METHODS =====
    public org.bukkit.Material getMaterial(Object materialName) {
        if (materialName == null) {
            return null;
        }
        try {
            String materialStr = materialName.toString();
            return org.bukkit.Material.valueOf(materialStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material type: " + materialName + ", using STONE");
            return org.bukkit.Material.STONE;
        }
    }
    
    // ===== ENCRYPTION/HASHING METHODS =====
    public String md5(String input) {
        if (input == null) {
            return "";
        }
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            plugin.getLogger().severe("Error creating MD5 hash: " + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }

    public String sha256(String input) {
        if (input == null) {
            return "";
        }
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            plugin.getLogger().severe("Error creating SHA-256 hash: " + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }
}
