package de.flori.mCJS;

import de.flori.mCJS.api.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.mozilla.javascript.Scriptable;

public class MCJSAPI {
    private final JavaPlugin plugin;
    private final APIHelper apiHelper;

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
    public final SystemsAPI systems;
    public final VaultAPI vault;
    public final DebugAPI debug;
    public final StateAPI states;

    public MCJSAPI(JavaPlugin plugin) {
        this(plugin, new DebugAPI());
    }

    public MCJSAPI(JavaPlugin plugin, DebugAPI debugger) {
        this.plugin = plugin;
        this.debug = debugger;
        this.apiHelper = new APIHelper();

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
        this.systems = new SystemsAPI(plugin, this.file, this.apiHelper, this.scheduler);
        this.vault = new VaultAPI(plugin);
        this.states = new StateAPI(plugin, apiHelper, scheduler);

        this.event.setInventoryAPI(this.inventory);
        this.inventory.setEventAPI(this.event);
    }

    public void setRhinoScope(Scriptable scope) {
        this.apiHelper.setScope(scope);
    }

    public APIHelper getAPIHelper() {
        return apiHelper;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public void unload() {
        states.close();
        systems.close();
        scheduler.cancelAll();
        event.unregisterHandlers();
        command.unregisterCommands();
        inventory.clearHandlers();
        apiHelper.setScope(null);
    }

    public Object getPluginConfigValue(String name, String key) {
        return file.getPluginConfigValue(name, key, null);
    }

    public Object getPluginConfigValue(String name, String key, Object fallback) {
        return file.getPluginConfigValue(name, key, fallback);
    }

    public void setPluginConfigValue(String name, String key, Object value) {
        file.setPluginConfigValue(name, key, value);
    }

    public void registerCommand(String name, String description, String usage, Object executor) {
        command.registerCommand(name, description, usage, executor);
    }

    public void registerCommand(String name, Object executor) {
        command.registerCommand(name, executor);
    }

    public void registerCommand(String name, String description, String usage, Object executor, Object tabCompleter) {
        command.registerCommand(name, description, usage, executor, tabCompleter);
    }

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

    public void broadcast(Object message) {
        utility.broadcast(message);
    }

    public void broadcast(Object message, Object permission) {
        utility.broadcast(message, permission);
    }

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

    public org.bukkit.World getWorld(String name) {
        return utility.getWorld(name);
    }

    public java.util.List<org.bukkit.World> getWorlds() {
        return utility.getWorlds();
    }

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

    public de.flori.mCJS.api.InventoryAPI.InventoryGUI createGUI(String title, int rows) {
        return inventory.createGUI(title, rows);
    }

    public org.bukkit.Material getMaterial(Object materialName) {
        return utility.getMaterial(materialName);
    }

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

    public void createTable(String dbName, String tableName, java.util.Map<String, String> columns) {
        database.createTable(dbName, tableName, columns);
    }

    public void insertData(String dbName, String tableName, java.util.Map<String, Object> data) {
        database.insertData(dbName, tableName, data);
    }

    public java.util.List<java.util.Map<String, Object>> querySQL(String dbName, String sql) {
        return database.querySQL(dbName, sql);
    }

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

    public boolean isPlayer(org.bukkit.command.CommandSender sender) {
        return utility.isPlayer(sender);
    }

    public org.bukkit.entity.Player getPlayerFromSender(org.bukkit.command.CommandSender sender) {
        return utility.getPlayerFromSender(sender);
    }

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
