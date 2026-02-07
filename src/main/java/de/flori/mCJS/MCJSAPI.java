package de.flori.mCJS;

import de.flori.mCJS.api.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.mozilla.javascript.Scriptable;

/**
 * Main API facade that combines all API modules
 * This replaces JSAPI.java and provides a unified interface for JavaScript plugins
 */
public class MCJSAPI {
    private final JavaPlugin plugin;
    private final APIHelper apiHelper;
    
    // API Modules
    public final CommandAPI command;
    public final SchedulerAPI scheduler;
    public final PlayerAPI player;
    public final UtilityAPI utility;
    public final WorldAPI world;
    public final EventAPI event;
    public final InventoryAPI inventory;
    public final ItemAPI item;
    public final EntityAPI entity;
    public final BlockAPI block;
    public final FileAPI file;
    public final DatabaseAPI database;
    public final NetworkAPI network;
    public final ScoreboardAPI scoreboard;
    public final SoundParticleAPI soundParticle;
    public final PluginBrowserAPI pluginBrowser;
    
    public MCJSAPI(JavaPlugin plugin) {
        this.plugin = plugin;
        this.apiHelper = new APIHelper();
        
        // Initialize API modules
        this.command = new CommandAPI(plugin, apiHelper);
        this.scheduler = new SchedulerAPI(plugin, apiHelper);
        this.player = new PlayerAPI(plugin);
        this.utility = new UtilityAPI(plugin);
        this.world = new WorldAPI(plugin);
        this.event = new EventAPI(plugin, apiHelper);
        this.inventory = new InventoryAPI(plugin, apiHelper);
        this.item = new ItemAPI(plugin);
        this.entity = new EntityAPI(plugin);
        this.block = new BlockAPI(plugin);
        this.file = new FileAPI(plugin);
        this.database = new DatabaseAPI(plugin);
        this.network = new NetworkAPI(plugin);
        this.scoreboard = new ScoreboardAPI(plugin);
        this.soundParticle = new SoundParticleAPI(plugin);
        this.pluginBrowser = new PluginBrowserAPI(plugin, this.network);
        
        // Set up cross-module references
        this.event.setInventoryAPI(this.inventory);
        this.inventory.setEventAPI(this.event);
    }
    
    /**
     * Set the Rhino scope for JavaScript execution
     */
    public void setRhinoScope(Scriptable scope) {
        this.apiHelper.setScope(scope);
    }
    
    /**
     * Get the API helper (for internal use)
     */
    public APIHelper getAPIHelper() {
        return apiHelper;
    }
    
    /**
     * Get the plugin instance
     */
    public JavaPlugin getPlugin() {
        return plugin;
    }
    
    // ===== Convenience methods that delegate to modules =====
    // These provide backward compatibility and convenience
    
    // Command methods
    public void registerCommand(String name, String description, String usage, Object executor) {
        command.registerCommand(name, description, usage, executor);
    }
    
    public void registerCommand(String name, Object executor) {
        command.registerCommand(name, executor);
    }
    
    public void registerCommand(String name, String description, String usage, Object executor, Object tabCompleter) {
        command.registerCommand(name, description, usage, executor, tabCompleter);
    }
    
    // Scheduler methods
    public org.bukkit.scheduler.BukkitTask runTaskLater(long delay, Object task) {
        return scheduler.runTaskLater(delay, task);
    }
    
    public org.bukkit.scheduler.BukkitTask runTaskTimer(long delay, long period, Object task) {
        return scheduler.runTaskTimer(delay, period, task);
    }
    
    public org.bukkit.scheduler.BukkitTask runTask(Object task) {
        return scheduler.runTask(task);
    }
    
    public org.bukkit.scheduler.BukkitTask runTaskAsync(Object task) {
        return scheduler.runTaskAsync(task);
    }
    
    public org.bukkit.scheduler.BukkitTask runTaskLaterAsync(long delay, Object task) {
        return scheduler.runTaskLaterAsync(delay, task);
    }
    
    public void cancelTask(org.bukkit.scheduler.BukkitTask task) {
        scheduler.cancelTask(task);
    }
    
    // Utility methods
    public void broadcast(Object message) {
        utility.broadcast(message);
    }
    
    public void broadcast(Object message, Object permission) {
        utility.broadcast(message, permission);
    }
    
    // Player methods (most common ones)
    public org.bukkit.entity.Player getPlayer(String name) {
        return player.getPlayer(name);
    }
    
    public org.bukkit.entity.Player getPlayerExact(String name) {
        return player.getPlayerExact(name);
    }
    
    public org.bukkit.entity.Player getPlayerByUUID(String uuid) {
        return player.getPlayerByUUID(uuid);
    }
    
    public java.util.Collection<? extends org.bukkit.entity.Player> getOnlinePlayers() {
        return player.getOnlinePlayers();
    }
    
    public void kickPlayer(org.bukkit.entity.Player player, String reason) {
        this.player.kickPlayer(player, reason);
    }
    
    // World methods
    public org.bukkit.World getWorld(String name) {
        return utility.getWorld(name);
    }
    
    public java.util.List<org.bukkit.World> getWorlds() {
        return utility.getWorlds();
    }
    
    // ===== Player Message Methods =====
    public void sendMessage(org.bukkit.command.CommandSender sender, Object message) {
        player.sendMessage(sender, message);
    }
    
    public void sendTitle(org.bukkit.entity.Player player, Object title, Object subtitle) {
        this.player.sendTitle(player, title, subtitle);
    }
    
    public void sendTitle(org.bukkit.entity.Player player, Object title, Object subtitle, int fadeIn, int stay, int fadeOut) {
        this.player.sendTitle(player, title, subtitle, fadeIn, stay, fadeOut);
    }
    
    public void sendActionBar(org.bukkit.entity.Player player, Object message) {
        this.player.sendActionBar(player, message);
    }
    
    // ===== Player Inventory Methods =====
    public org.bukkit.inventory.ItemStack getItemInMainHand(org.bukkit.entity.Player player) {
        return this.player.getItemInMainHand(player);
    }
    
    public void giveItem(org.bukkit.entity.Player player, org.bukkit.inventory.ItemStack item) {
        this.player.giveItem(player, item);
    }
    
    public double getMaxHealth(org.bukkit.entity.Player player) {
        return this.player.getMaxHealth(player);
    }
    
    public void clearInventory(org.bukkit.entity.Player player) {
        this.player.clearInventory(player);
    }
    
    public void setSaturation(org.bukkit.entity.Player player, float saturation) {
        this.player.setSaturation(player, saturation);
    }
    
    // ===== Item Methods =====
    public org.bukkit.inventory.ItemStack createItemStack(org.bukkit.Material material, int amount) {
        return item.createItemStack(material, amount);
    }
    
    public org.bukkit.inventory.ItemStack createItemStack(org.bukkit.Material material) {
        return item.createItemStack(material);
    }
    
    public org.bukkit.inventory.ItemStack setItemDisplayName(org.bukkit.inventory.ItemStack item, Object name) {
        return this.item.setItemDisplayName(item, name);
    }
    
    public org.bukkit.inventory.ItemStack setItemLore(org.bukkit.inventory.ItemStack item, Object lore) {
        return this.item.setItemLore(item, lore);
    }
    
    // ===== Inventory/GUI Methods =====
    public de.flori.mCJS.api.InventoryAPI.InventoryGUI createGUI(String title, int rows) {
        return inventory.createGUI(title, rows);
    }
    
    // ===== Material Methods =====
    public org.bukkit.Material getMaterial(Object materialName) {
        return utility.getMaterial(materialName);
    }
    
    // ===== Sound and Particle Methods =====
    public void playSound(org.bukkit.Location location, Object soundName, float volume, float pitch) {
        soundParticle.playSound(location, soundName, volume, pitch);
    }
    
    public void playSound(org.bukkit.entity.Player player, Object soundName, float volume, float pitch) {
        soundParticle.playSound(player, soundName, volume, pitch);
    }
    
    public void spawnParticle(org.bukkit.Location location, Object particleName, int count) {
        soundParticle.spawnParticle(location, particleName, count);
    }
    
    public void spawnParticle(org.bukkit.Location location, Object particleName, int count, double offsetX, double offsetY, double offsetZ) {
        soundParticle.spawnParticle(location, particleName, count, offsetX, offsetY, offsetZ);
    }
    
    public void spawnParticle(org.bukkit.Location location, Object particleName, int count, double offsetX, double offsetY, double offsetZ, double extra) {
        soundParticle.spawnParticle(location, particleName, count, offsetX, offsetY, offsetZ, extra);
    }
    
    // ===== File Methods =====
    public void saveYamlFile(String fileName, java.util.Map<String, Object> data) {
        file.saveYamlFile(fileName, data);
    }
    
    public java.util.Map<String, Object> loadYamlFile(String fileName) {
        return file.loadYamlFile(fileName);
    }
    
    public void saveJsonFile(String fileName, String jsonContent) {
        file.saveJsonFile(fileName, jsonContent);
    }
    
    public String loadJsonFile(String fileName) {
        return file.loadJsonFile(fileName);
    }
    
    public java.io.File getPluginFile(String fileName) {
        return file.getPluginFile(fileName);
    }
    
    public boolean pluginFileExists(String fileName) {
        return file.pluginFileExists(fileName);
    }
    
    // ===== Database Methods =====
    public void createTable(String dbName, String tableName, java.util.Map<String, String> columns) {
        database.createTable(dbName, tableName, columns);
    }
    
    public void insertData(String dbName, String tableName, java.util.Map<String, Object> data) {
        database.insertData(dbName, tableName, data);
    }
    
    public java.util.List<java.util.Map<String, Object>> querySQL(String dbName, String sql) {
        return database.querySQL(dbName, sql);
    }
    
    // ===== Utility Methods =====
    public long getCurrentTimeMillis() {
        return utility.getCurrentTimeMillis();
    }
    
    public String getMCJSVersion() {
        return utility.getMCJSVersion();
    }
    
    public String getServerVersion() {
        return utility.getServerVersion();
    }
    
    public int getMaxPlayers() {
        return utility.getMaxPlayers();
    }
    
    public String formatDate(long timestamp) {
        return utility.formatDate(timestamp);
    }
    
    public String formatDate(long timestamp, String format) {
        return utility.formatDate(timestamp, format);
    }
    
    public String md5(String input) {
        return utility.md5(input);
    }
    
    public String sha256(String input) {
        return utility.sha256(input);
    }
    
    public String base64Encode(String input) {
        return utility.base64Encode(input);
    }
    
    public String base64Decode(String input) {
        return utility.base64Decode(input);
    }
    
    // ===== Validation Methods =====
    public boolean isPlayer(org.bukkit.command.CommandSender sender) {
        return utility.isPlayer(sender);
    }
    
    public org.bukkit.entity.Player getPlayerFromSender(org.bukkit.command.CommandSender sender) {
        return utility.getPlayerFromSender(sender);
    }
    
    // ===== Plugin Browser Methods =====
    public java.util.List<java.util.Map<String, Object>> searchPlugins(String query, String category) {
        return pluginBrowser.searchPlugins(query, category);
    }
    
    public java.util.Map<String, Object> getPluginDetails(int pluginId) {
        return pluginBrowser.getPluginDetails(pluginId);
    }
    
    public boolean installPlugin(int pluginId, String fileName) {
        return pluginBrowser.installPlugin(pluginId, fileName);
    }
    
    public boolean submitReview(int pluginId, String author, int rating, String comment) {
        return pluginBrowser.submitReview(pluginId, author, rating, comment);
    }
    
    public boolean reportPlugin(int pluginId, String reporter, String reason) {
        return pluginBrowser.reportPlugin(pluginId, reporter, reason);
    }
    
    public java.util.List<java.util.Map<String, Object>> getPluginReviews(int pluginId) {
        return pluginBrowser.getPluginReviews(pluginId);
    }
    
    public void setBrowserUrl(String url) {
        pluginBrowser.setBrowserUrl(url);
    }
    
    public String getBrowserUrl() {
        return pluginBrowser.getBrowserUrl();
    }
    
    public java.util.Map<String, Object> uploadPlugin(java.io.File pluginFile, String name, String version, String author, String description, String category, String uploader, String uploaderName) {
        return pluginBrowser.uploadPlugin(pluginFile, name, version, author, description, category, uploader, uploaderName);
    }
    
    public java.util.Map<String, Object> extractPluginMetadata(java.io.File pluginFile) {
        return pluginBrowser.extractPluginMetadata(pluginFile);
    }
    
    // ===== Event Methods =====
    public void registerEvent(String eventClassName, Object handler) {
        event.registerEvent(eventClassName, handler);
    }
    
    public <T extends org.bukkit.event.Event> void registerEvent(Class<T> eventClass, Object handler) {
        event.registerEvent(eventClass, handler);
    }
    
    public <T extends org.bukkit.event.Event> void registerEvent(Class<T> eventClass, Object handler, org.bukkit.event.EventPriority priority) {
        event.registerEvent(eventClass, handler, priority);
    }
}
