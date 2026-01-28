package de.flori.mCJS;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;
import org.bukkit.util.Vector;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import java.io.File;
import java.net.URI;
import java.util.*;

public class JSAPI {
    private final JavaPlugin plugin;
    Scriptable scope; // Package-private for access from inner classes

    public JSAPI(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // Internal method to set Rhino scope (called by JSPluginManager)
    // Note: Context is thread-local, so we create a new one for each callback
    public void setRhinoScope(Scriptable scope) {
        this.scope = scope;
    }

    // Helper method to execute a function with a new context
    private void executeFunction(Function func, Object... args) {
        if (scope == null) {
            plugin.getLogger().warning("No scope available for JavaScript execution");
            return;
        }

        // Create a new context for this thread
        org.mozilla.javascript.Context rhinoContext = org.mozilla.javascript.Context.enter();
        try {
            rhinoContext.setOptimizationLevel(-1);
            rhinoContext.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
            func.call(rhinoContext, scope, scope, args);
        } finally {
            org.mozilla.javascript.Context.exit();
        }
    }

    // ===== COMMAND REGISTRATION =====
    public void registerCommand(String name, String description, String usage, Object executor) {
        registerCommand(name, description, usage, executor, null);
    }

    // Register command without tab completer (internal)
    private void registerCommandInternal(String name, String description, String usage, Object executor) {
        try {
            // Try to get existing command first
        PluginCommand command = plugin.getCommand(name);
            
            // If command doesn't exist in plugin.yml, register it dynamically
            if (command == null) {
                // Use reflection to access CommandMap and register command dynamically
                try {
                    java.lang.reflect.Field commandMapField = plugin.getServer().getClass().getDeclaredField("commandMap");
                    commandMapField.setAccessible(true);
                    org.bukkit.command.CommandMap commandMap = (org.bukkit.command.CommandMap) commandMapField.get(plugin.getServer());
                    
                    // Create a simple Command object
                    org.bukkit.command.Command dynamicCommand = new org.bukkit.command.Command(name) {
                        @Override
                        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                            try {
                                if (executor instanceof Function && scope != null) {
                                    org.mozilla.javascript.Context rhinoContext = org.mozilla.javascript.Context.enter();
                                    try {
                                        rhinoContext.setOptimizationLevel(-1);
                                        rhinoContext.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                                        Function func = (Function) executor;
                                        Object result = func.call(rhinoContext, scope, scope, new Object[]{sender, args});
                                        return result instanceof Boolean ? (Boolean) result : true;
                                    } finally {
                                        org.mozilla.javascript.Context.exit();
                                    }
                                }
                                return true;
                            } catch (Exception e) {
                                plugin.getLogger().severe("Error executing JS command '" + name + "': " + e.getMessage());
                                e.printStackTrace();
                                return false;
                            }
                        }
                    };
                    
                    dynamicCommand.setDescription(description != null ? description : "");
                    dynamicCommand.setUsage(usage != null ? usage : "/" + name);
                    
                    // Register the command
                    commandMap.register(name, plugin.getName().toLowerCase(), dynamicCommand);
                    plugin.getLogger().info("Dynamically registered command: /" + name);
                    return;
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to register command '" + name + "' dynamically: " + e.getMessage());
                    e.printStackTrace();
                    return;
                }
            }
            
            // Set executor for existing command
            command.setExecutor((sender, cmd, label, args) -> {
                try {
                    if (executor instanceof Function && scope != null) {
                        org.mozilla.javascript.Context rhinoContext = org.mozilla.javascript.Context.enter();
                        try {
                            rhinoContext.setOptimizationLevel(-1);
                            rhinoContext.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                            Function func = (Function) executor;
                            Object result = func.call(rhinoContext, scope, scope, new Object[]{sender, args});
                        return result instanceof Boolean ? (Boolean) result : true;
                        } finally {
                            org.mozilla.javascript.Context.exit();
                        }
                    }
                    return true;
                } catch (Exception e) {
                    plugin.getLogger().severe("Error executing JS command '" + name + "': " + e.getMessage());
                    e.printStackTrace();
                    return false;
                }
            });
            
            if (description != null && !description.isEmpty()) {
            command.setDescription(description);
            }
            if (usage != null && !usage.isEmpty()) {
            command.setUsage(usage);
            }
            
            plugin.getLogger().info("Registered JS command: /" + name);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to register command '" + name + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void registerCommand(String name, Object executor) {
        registerCommand(name, "", "/" + name, executor, null);
    }

    // Register command with tab completer
    public void registerCommand(String name, String description, String usage, Object executor, Object tabCompleter) {
        registerCommandInternal(name, description, usage, executor);
        
        // Set tab completer if provided
        if (tabCompleter != null) {
            try {
                PluginCommand command = plugin.getCommand(name);
                if (command == null) {
                    // Try to get from CommandMap
                    try {
                        java.lang.reflect.Field commandMapField = plugin.getServer().getClass().getDeclaredField("commandMap");
                        commandMapField.setAccessible(true);
                        org.bukkit.command.CommandMap commandMap = (org.bukkit.command.CommandMap) commandMapField.get(plugin.getServer());
                        org.bukkit.command.Command cmd = commandMap.getCommand(name);
                        if (cmd instanceof PluginCommand) {
                            command = (PluginCommand) cmd;
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                }
                
                if (command != null && tabCompleter instanceof Function) {
                    command.setTabCompleter((sender, cmd, alias, args) -> {
                        try {
                            org.mozilla.javascript.Context rhinoContext = org.mozilla.javascript.Context.enter();
                            try {
                                rhinoContext.setOptimizationLevel(-1);
                                rhinoContext.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                                Function func = (Function) tabCompleter;
                                Object result = func.call(rhinoContext, scope, scope, new Object[]{sender, args});
                                
                                if (result instanceof java.util.List) {
                                    return (java.util.List<String>) result;
                                } else if (result instanceof Object[]) {
                                    return java.util.Arrays.asList((String[]) result);
                                } else if (result instanceof Scriptable) {
                                    Scriptable array = (Scriptable) result;
                                    java.util.List<String> completions = new java.util.ArrayList<>();
                                    Object length = array.get("length", array);
                                    if (length instanceof Number) {
                                        int len = ((Number) length).intValue();
                                        for (int i = 0; i < len; i++) {
                                            Object item = array.get(i, array);
                                            if (item != null) {
                                                completions.add(item.toString());
                                            }
                                        }
                                    }
                                    return completions;
                                }
                            } finally {
                                org.mozilla.javascript.Context.exit();
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning("Error in tab completer for command '" + name + "': " + e.getMessage());
                        }
                        return null;
                    });
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to set tab completer for command '" + name + "': " + e.getMessage());
            }
        }
    }

    // ===== EVENT REGISTRATION =====
    public void registerEvent(String eventClassName, Object handler) {
        try {
            Class<?> eventClass = null;
            
            // If the event name already contains a package (e.g., "player.PlayerJoinEvent")
            if (eventClassName.contains(".") && !eventClassName.startsWith("org.bukkit")) {
                // Map package prefixes to full package names
                String[] parts = eventClassName.split("\\.", 2);
                if (parts.length == 2) {
                    String prefix = parts[0];
                    String className = parts[1];
                    
                    String fullPackage = null;
                    switch (prefix.toLowerCase()) {
                        case "player":
                            fullPackage = "org.bukkit.event.player.";
                            break;
                        case "block":
                            fullPackage = "org.bukkit.event.block.";
                            break;
                        case "entity":
                            fullPackage = "org.bukkit.event.entity.";
                            break;
                        case "inventory":
                            fullPackage = "org.bukkit.event.inventory.";
                            break;
                        case "server":
                            fullPackage = "org.bukkit.event.server.";
                            break;
                        case "world":
                            fullPackage = "org.bukkit.event.world.";
                            break;
                        case "vehicle":
                            fullPackage = "org.bukkit.event.vehicle.";
                            break;
                        case "hanging":
                            fullPackage = "org.bukkit.event.hanging.";
                            break;
                        case "enchantment":
                            fullPackage = "org.bukkit.event.enchantment.";
                            break;
                        case "painting":
                            fullPackage = "org.bukkit.event.painting.";
                            break;
                    }
                    
                    if (fullPackage != null) {
                        try {
                            eventClass = Class.forName(fullPackage + className);
                        } catch (ClassNotFoundException e) {
                            // Try without prefix
                        }
                    }
                }
            }
            
            // If not found yet, try different event packages
            if (eventClass == null) {
                String[] packages = {
                    "org.bukkit.event.",
                    "org.bukkit.event.player.",
                    "org.bukkit.event.block.",
                    "org.bukkit.event.entity.",
                    "org.bukkit.event.inventory.",
                    "org.bukkit.event.server.",
                    "org.bukkit.event.world.",
                    "org.bukkit.event.vehicle.",
                    "org.bukkit.event.hanging.",
                    "org.bukkit.event.enchantment.",
                    "org.bukkit.event.painting."
                };
                
                for (String pkg : packages) {
                    try {
                        eventClass = Class.forName(pkg + eventClassName);
                        break;
                    } catch (ClassNotFoundException e) {
                        // Try next package
                    }
                }
            }
            
            if (eventClass == null) {
                plugin.getLogger().severe("Event class not found: " + eventClassName);
                return;
            }
            
            // Ensure it's a valid event class (not the base Event class)
            if (Event.class.isAssignableFrom(eventClass) && eventClass != Event.class) {
                @SuppressWarnings("unchecked")
                Class<? extends Event> clazz = (Class<? extends Event>) eventClass;
                registerEvent(clazz, handler);
            } else {
                plugin.getLogger().warning("Invalid event class: " + eventClassName + " (cannot use base Event class)");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error registering event '" + eventClassName + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    public <T extends Event> void registerEvent(Class<T> eventClass, Object handler) {
        registerEvent(eventClass, handler, EventPriority.NORMAL);
    }

    // Event handler storage - one listener per event class
    private static final Map<Class<? extends Event>, Listener> eventListeners = new HashMap<>();
    private static final Map<Class<? extends Event>, Map<JSAPI, List<EventHandlerInfo>>> globalEventHandlers = new HashMap<>();
    private static JavaPlugin listenerPlugin;

    private static class EventHandlerInfo {
        final Object handler;
        final EventPriority priority;
        
        EventHandlerInfo(Object handler, EventPriority priority) {
            this.handler = handler;
            this.priority = priority;
        }
    }

    // Create a dynamic listener for a specific event class
    private static <T extends Event> Listener createEventListener(Class<T> eventClass, JavaPlugin plugin) {
        return new Listener() {
            @EventHandler(priority = EventPriority.LOWEST)
            public void onEventLowest(T event) {
                handleEvent(event, EventPriority.LOWEST);
            }

            @EventHandler(priority = EventPriority.LOW)
            public void onEventLow(T event) {
                handleEvent(event, EventPriority.LOW);
            }

            @EventHandler(priority = EventPriority.NORMAL)
            public void onEventNormal(T event) {
                handleEvent(event, EventPriority.NORMAL);
            }

            @EventHandler(priority = EventPriority.HIGH)
            public void onEventHigh(T event) {
                handleEvent(event, EventPriority.HIGH);
            }

            @EventHandler(priority = EventPriority.HIGHEST)
            public void onEventHighest(T event) {
                handleEvent(event, EventPriority.HIGHEST);
            }

            @EventHandler(priority = EventPriority.MONITOR)
            public void onEventMonitor(T event) {
                handleEvent(event, EventPriority.MONITOR);
            }

            @SuppressWarnings("unchecked")
            private void handleEvent(Event event, EventPriority currentPriority) {
                Class<? extends Event> actualEventClass = event.getClass();
                
                // Check direct class match
                Map<JSAPI, List<EventHandlerInfo>> handlersForClass = globalEventHandlers.get(eventClass);
                
                // If no direct match, check if the registered class is assignable from the actual event class
                if (handlersForClass == null) {
                    for (Map.Entry<Class<? extends Event>, Map<JSAPI, List<EventHandlerInfo>>> entry : globalEventHandlers.entrySet()) {
                        if (entry.getKey().isAssignableFrom(actualEventClass)) {
                            handlersForClass = entry.getValue();
                            break;
                        }
                    }
                }
                
                if (handlersForClass != null) {
                    for (Map.Entry<JSAPI, List<EventHandlerInfo>> apiEntry : handlersForClass.entrySet()) {
                        JSAPI api = apiEntry.getKey();
                        List<EventHandlerInfo> handlers = apiEntry.getValue();
                        
                        for (EventHandlerInfo info : handlers) {
                            if (info.priority == currentPriority && info.handler instanceof Function && api.scope != null) {
                                try {
                                    org.mozilla.javascript.Context rhinoContext = org.mozilla.javascript.Context.enter();
                                    try {
                                        rhinoContext.setOptimizationLevel(-1);
                                        rhinoContext.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                                        Function func = (Function) info.handler;
                                        func.call(rhinoContext, api.scope, api.scope, new Object[]{event});
                                    } finally {
                                        org.mozilla.javascript.Context.exit();
                }
            } catch (Exception e) {
                                    if (listenerPlugin != null) {
                                        listenerPlugin.getLogger().severe("Error in JS event handler for " + actualEventClass.getSimpleName() + ": " + e.getMessage());
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        };
    }

    public <T extends Event> void registerEvent(Class<T> eventClass, Object handler, EventPriority priority) {
        // Check if Event is the base class (which doesn't have getHandlerList)
        if (eventClass == null || eventClass == Event.class) {
            plugin.getLogger().warning("Cannot register handler for base Event class. Use a specific event type.");
            return;
        }
        
        // Additional check: ensure the event class has static getHandlerList method
        try {
            java.lang.reflect.Method handlerListMethod = eventClass.getMethod("getHandlerList");
            // Check if it's static
            if (!java.lang.reflect.Modifier.isStatic(handlerListMethod.getModifiers())) {
                plugin.getLogger().warning("Event class '" + eventClass.getName() + "' getHandlerList() is not static. Cannot register.");
                return;
            }
        } catch (NoSuchMethodException e) {
            plugin.getLogger().warning("Event class '" + eventClass.getName() + "' does not have getHandlerList() method. Cannot register.");
            return;
        }
        
        // Initialize listener plugin reference
        synchronized (globalEventHandlers) {
            if (listenerPlugin == null) {
                listenerPlugin = plugin;
            }
        }
        
        // Create and register listener for this event class if not already registered
        synchronized (eventListeners) {
            if (!eventListeners.containsKey(eventClass)) {
                try {
                    Listener listener = createEventListener(eventClass, plugin);
                    eventListeners.put(eventClass, listener);
                    plugin.getServer().getPluginManager().registerEvents(listener, plugin);
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to register event listener for " + eventClass.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                    return;
                }
            }
        }
        
        // Store handler
        synchronized (globalEventHandlers) {
            globalEventHandlers.computeIfAbsent(eventClass, k -> new HashMap<>())
                              .computeIfAbsent(this, k -> new ArrayList<>())
                              .add(new EventHandlerInfo(handler, priority));
        }
    }

    // ===== SCHEDULER METHODS =====
    public BukkitTask runTaskLater(long delay, Object task) {
        return plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            try {
                if (task instanceof Function && scope != null) {
                    executeFunction((Function) task);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error in scheduled task: " + e.getMessage());
                e.printStackTrace();
            }
        }, delay);
    }

    public BukkitTask runTaskTimer(long delay, long period, Object task) {
        return plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            try {
                if (task instanceof Function && scope != null) {
                    executeFunction((Function) task);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error in scheduled task: " + e.getMessage());
                e.printStackTrace();
            }
        }, delay, period);
    }

    public BukkitTask runTask(Object task) {
        return plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                if (task instanceof Function && scope != null) {
                    executeFunction((Function) task);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error in task: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public BukkitTask runTaskAsync(Object task) {
        return plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (task instanceof Function && scope != null) {
                    executeFunction((Function) task);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error in async task: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public BukkitTask runTaskLaterAsync(long delay, Object task) {
        return plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            try {
                if (task instanceof Function && scope != null) {
                    executeFunction((Function) task);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error in async scheduled task: " + e.getMessage());
                e.printStackTrace();
            }
        }, delay);
    }

    public void cancelTask(BukkitTask task) {
        if (task != null) {
            task.cancel();
        }
    }

    // ===== UTILITY METHODS =====
    public void broadcast(String message) {
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    public void broadcast(String message, String permission) {
        String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(permission)) {
                player.sendMessage(coloredMessage);
            }
        }
    }

    public Player getPlayer(String name) {
        return Bukkit.getPlayer(name);
    }

    public Player getPlayerExact(String name) {
        return Bukkit.getPlayerExact(name);
    }

    public Collection<? extends Player> getOnlinePlayers() {
        return Bukkit.getOnlinePlayers();
    }

    public World getWorld(String name) {
        return Bukkit.getWorld(name);
    }

    public List<World> getWorlds() {
        return Bukkit.getWorlds();
    }

    // ===== INVENTORY METHODS =====
    public Inventory createInventory(InventoryHolder holder, int size, String title) {
        return Bukkit.createInventory(holder, size, ChatColor.translateAlternateColorCodes('&', title));
    }

    public Inventory createInventory(InventoryHolder holder, InventoryType type, String title) {
        return Bukkit.createInventory(holder, type, ChatColor.translateAlternateColorCodes('&', title));
    }

    // ===== ITEM METHODS =====
    public ItemStack createItemStack(Material material, int amount) {
        return new ItemStack(material, amount);
    }

    public ItemStack createItemStack(Material material) {
        return new ItemStack(material);
    }

    public ItemMeta getItemMeta(ItemStack item) {
        return item.getItemMeta();
    }

    public void setItemMeta(ItemStack item, ItemMeta meta) {
        item.setItemMeta(meta);
    }

    public ItemStack setItemDisplayName(ItemStack item, String name) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack setItemLore(ItemStack item, List<String> lore) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ===== LOCATION METHODS =====
    public Location createLocation(World world, double x, double y, double z) {
        return new Location(world, x, y, z);
    }

    public Location createLocation(World world, double x, double y, double z, float yaw, float pitch) {
        return new Location(world, x, y, z, yaw, pitch);
    }

    // ===== VECTOR METHODS =====
    public Vector createVector(double x, double y, double z) {
        return new Vector(x, y, z);
    }

    // ===== POTION EFFECTS =====
    public PotionEffect createPotionEffect(PotionEffectType type, int duration, int amplifier) {
        return new PotionEffect(type, duration, amplifier);
    }

    public PotionEffect createPotionEffect(PotionEffectType type, int duration, int amplifier, boolean ambient) {
        return new PotionEffect(type, duration, amplifier, ambient);
    }

    // ===== SCOREBOARD METHODS =====
    public Scoreboard getMainScoreboard() {
        return Bukkit.getScoreboardManager().getMainScoreboard();
    }

    public Scoreboard createScoreboard() {
        return Bukkit.getScoreboardManager().getNewScoreboard();
    }

    // ===== SOUND METHODS =====
    // Helper method to convert string to Sound enum
    private Sound getSound(String soundName) {
        try {
            return Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound type: " + soundName + ", using ENTITY_PLAYER_LEVELUP");
            return Sound.ENTITY_PLAYER_LEVELUP;
        }
    }

    // Overloads that accept Sound enum
    public void playSound(Location location, Sound sound, float volume, float pitch) {
        location.getWorld().playSound(location, sound, volume, pitch);
    }

    public void playSound(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    // Overloads that accept String (for JavaScript compatibility)
    public void playSound(Location location, String soundName, float volume, float pitch) {
        playSound(location, getSound(soundName), volume, pitch);
    }

    public void playSound(Player player, String soundName, float volume, float pitch) {
        playSound(player, getSound(soundName), volume, pitch);
    }

    // ===== PARTICLE METHODS =====
    // Helper method to convert string to Particle enum
    private Particle getParticle(String particleName) {
        try {
            return Particle.valueOf(particleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid particle type: " + particleName + ", using FLAME");
            try {
                return Particle.FLAME;
            } catch (Exception ex) {
                // Fallback to first available particle
                return Particle.values()[0];
            }
        }
    }

    // Overloads that accept Particle enum
    public void spawnParticle(Location location, Particle particle, int count) {
        location.getWorld().spawnParticle(particle, location, count);
    }

    public void spawnParticle(Location location, Particle particle, int count, double offsetX, double offsetY, double offsetZ) {
        location.getWorld().spawnParticle(particle, location, count, offsetX, offsetY, offsetZ);
    }

    public void spawnParticle(Location location, Particle particle, int count, double offsetX, double offsetY, double offsetZ, double extra) {
        location.getWorld().spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra);
    }

    // Overloads that accept String (for JavaScript compatibility)
    public void spawnParticle(Location location, String particleName, int count) {
        spawnParticle(location, getParticle(particleName), count);
    }

    public void spawnParticle(Location location, String particleName, int count, double offsetX, double offsetY, double offsetZ) {
        spawnParticle(location, getParticle(particleName), count, offsetX, offsetY, offsetZ);
    }

    public void spawnParticle(Location location, String particleName, int count, double offsetX, double offsetY, double offsetZ, double extra) {
        spawnParticle(location, getParticle(particleName), count, offsetX, offsetY, offsetZ, extra);
    }

    // ===== ENTITY METHODS =====
    // Helper method to convert string to EntityType enum
    private EntityType getEntityType(String entityName) {
        try {
            return EntityType.valueOf(entityName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid entity type: " + entityName + ", using PIG");
            return EntityType.PIG;
        }
    }

    // Overload that accepts EntityType enum
    public Entity spawnEntity(Location location, EntityType type) {
        return location.getWorld().spawnEntity(location, type);
    }

    // Overload that accepts String (for JavaScript compatibility)
    public Entity spawnEntity(Location location, String entityName) {
        return spawnEntity(location, getEntityType(entityName));
    }

    public List<Entity> getNearbyEntities(Location location, double x, double y, double z) {
        return (List<Entity>) location.getWorld().getNearbyEntities(location, x, y, z);
    }

    // ===== CONFIGURATION METHODS =====
    public void saveConfig() {
        plugin.saveConfig();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
    }

    public Object getConfig(String path) {
        return plugin.getConfig().get(path);
    }

    public Object getConfig(String path, Object defaultValue) {
        return plugin.getConfig().get(path, defaultValue);
    }

    public void setConfig(String path, Object value) {
        plugin.getConfig().set(path, value);
        plugin.saveConfig();
    }

    // ===== YAML FILE MANAGEMENT =====
    public void saveYamlFile(String fileName, Map<String, Object> data) {
        try {
            File file = new File(plugin.getDataFolder(), fileName + ".yml");
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
            java.nio.file.Files.write(file.toPath(), jsonContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            plugin.getLogger().severe("Error saving JSON file '" + fileName + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String loadJsonFile(String fileName) {
        try {
            File file = new File(plugin.getDataFolder(), fileName + ".json");
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
            java.nio.file.Files.write(file.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            plugin.getLogger().severe("Error saving text file '" + fileName + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String loadTextFile(String fileName) {
        try {
            File file = new File(plugin.getDataFolder(), fileName + ".txt");
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

    // ===== DATABASE METHODS (SQLite) =====
    private java.sql.Connection getDatabaseConnection(String dbName) throws java.sql.SQLException {
        File dbFile = new File(plugin.getDataFolder(), dbName + ".db");
        return java.sql.DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
    }

    public void executeSQL(String dbName, String sql) {
        try (java.sql.Connection conn = getDatabaseConnection(dbName);
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (Exception e) {
            plugin.getLogger().severe("Error executing SQL '" + sql + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<Map<String, Object>> querySQL(String dbName, String sql) {
        List<Map<String, Object>> results = new ArrayList<>();
        try (java.sql.Connection conn = getDatabaseConnection(dbName);
             java.sql.Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(sql)) {

            java.sql.ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnName(i), rs.getObject(i));
                }
                results.add(row);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error querying SQL '" + sql + "': " + e.getMessage());
            e.printStackTrace();
        }
        return results;
    }

    public void createTable(String dbName, String tableName, Map<String, String> columns) {
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS " + tableName + " (");
        boolean first = true;
        for (Map.Entry<String, String> column : columns.entrySet()) {
            if (!first) sql.append(", ");
            sql.append(column.getKey()).append(" ").append(column.getValue());
            first = false;
        }
        sql.append(")");
        executeSQL(dbName, sql.toString());
    }

    public void insertData(String dbName, String tableName, Map<String, Object> data) {
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        List<Object> params = new ArrayList<>();

        boolean first = true;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!first) {
                columns.append(", ");
                values.append(", ");
            }
            columns.append(entry.getKey());
            values.append("?");
            params.add(entry.getValue());
            first = false;
        }

        String sql = "INSERT OR REPLACE INTO " + tableName + " (" + columns + ") VALUES (" + values + ")";

        try (java.sql.Connection conn = getDatabaseConnection(dbName);
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            stmt.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().severe("Error inserting data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ===== HTTP REQUESTS =====
    public String httpGet(String url) {
        try {
            URI uri = new URI(url);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line).append("\n");
                }
                return response.toString().trim();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error making HTTP GET request to '" + url + "': " + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }

    public String httpPost(String url, String data) {
        try {
            URI uri = new URI(url);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            try (java.io.OutputStream os = conn.getOutputStream()) {
                byte[] input = data.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line).append("\n");
                }
                return response.toString().trim();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error making HTTP POST request to '" + url + "': " + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }

    // ===== ENCRYPTION METHODS =====
    public String md5(String input) {
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

    // ===== DATE AND TIME METHODS =====
    public String formatDate(long timestamp) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(timestamp));
    }

    public String formatDate(long timestamp, String pattern) {
        return new java.text.SimpleDateFormat(pattern).format(new java.util.Date(timestamp));
    }

    public long parseDate(String dateString) {
        try {
            return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(dateString).getTime();
        } catch (Exception e) {
            plugin.getLogger().severe("Error parsing date '" + dateString + "': " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    public long parseDate(String dateString, String pattern) {
        try {
            return new java.text.SimpleDateFormat(pattern).parse(dateString).getTime();
        } catch (Exception e) {
            plugin.getLogger().severe("Error parsing date '" + dateString + "': " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    public long getCurrentTime() {
        return System.currentTimeMillis();
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

    // ===== PERMISSION METHODS =====
    public boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission(permission);
    }

    // ===== MATERIAL AND BLOCK METHODS =====
    public Material getMaterial(String name) {
        try {
        return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Material '" + name + "' not found");
            return Material.AIR;
        }
    }

    public boolean isBlock(Material material) {
        return material.isBlock();
    }

    public boolean isItem(Material material) {
        return material.isItem();
    }

    // ===== TIME METHODS =====
    public long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    public long getWorldTime(World world) {
        return world.getTime();
    }

    public void setWorldTime(World world, long time) {
        world.setTime(time);
    }

    // ===== WEATHER METHODS =====
    public boolean isThundering(World world) {
        return world.isThundering();
    }

    public void setThundering(World world, boolean thundering) {
        world.setThundering(thundering);
    }

    public boolean hasStorm(World world) {
        return world.hasStorm();
    }

    public void setStorm(World world, boolean storm) {
        world.setStorm(storm);
    }

    // ===== GAMEMODE METHODS =====
    public void setGameMode(Player player, GameMode mode) {
        player.setGameMode(mode);
    }

    public GameMode getGameMode(Player player) {
        return player.getGameMode();
    }

    // ===== TELEPORTATION =====
    public void teleport(Player player, Location location) {
        player.teleport(location);
    }

    public void teleport(Entity entity, Location location) {
        entity.teleport(location);
    }

    // ===== HEALTH AND DAMAGE =====
    public void setHealth(Player player, double health) {
        try {
            org.bukkit.attribute.AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                double maxHealth = maxHealthAttr.getValue();
                player.setHealth(Math.min(health, maxHealth));
            } else {
                player.setHealth(Math.min(health, player.getHealthScale()));
            }
        } catch (Exception e) {
            // Fallback for older API versions
            player.setHealth(Math.min(health, player.getHealthScale()));
        }
    }

    public double getHealth(Player player) {
        return player.getHealth();
    }

    public double getMaxHealth(Player player) {
        try {
            org.bukkit.attribute.AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                return maxHealthAttr.getValue();
            } else {
                return player.getHealthScale();
            }
        } catch (Exception e) {
            // Fallback for older API versions
            return player.getHealthScale();
        }
    }

    public void damage(Player player, double damage) {
        player.damage(damage);
    }

    // ===== FOOD AND SATURATION =====
    public void setFoodLevel(Player player, int level) {
        player.setFoodLevel(level);
    }

    public int getFoodLevel(Player player) {
        return player.getFoodLevel();
    }

    public void setSaturation(Player player, float saturation) {
        player.setSaturation(saturation);
    }

    public float getSaturation(Player player) {
        return player.getSaturation();
    }

    // ===== INVENTORY MANAGEMENT =====
    public void giveItem(Player player, ItemStack item) {
        player.getInventory().addItem(item);
    }

    public void clearInventory(Player player) {
        player.getInventory().clear();
    }

    public ItemStack getItemInMainHand(Player player) {
        return player.getInventory().getItemInMainHand();
    }

    public void setItemInMainHand(Player player, ItemStack item) {
        player.getInventory().setItemInMainHand(item);
    }

    public ItemStack getItemInOffHand(Player player) {
        return player.getInventory().getItemInOffHand();
    }

    public void setItemInOffHand(Player player, ItemStack item) {
        player.getInventory().setItemInOffHand(item);
    }

    // ===== CHAT AND MESSAGES =====
    public void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    public void sendTitle(Player player, String title, String subtitle) {
        player.sendTitle(
            ChatColor.translateAlternateColorCodes('&', title),
            ChatColor.translateAlternateColorCodes('&', subtitle),
            10, 70, 20
        );
    }

    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(
            ChatColor.translateAlternateColorCodes('&', title),
            ChatColor.translateAlternateColorCodes('&', subtitle),
            fadeIn, stay, fadeOut
        );
    }

    public void sendActionBar(Player player, String message) {
        player.sendActionBar(ChatColor.translateAlternateColorCodes('&', message));
    }

    // ===== ECONOMY INTEGRATION (if Vault is present) =====
    public boolean hasEconomy() {
        return plugin.getServer().getPluginManager().getPlugin("Vault") != null;
    }

    // ===== WORLD BORDER METHODS =====
    public WorldBorder getWorldBorder(World world) {
        return world.getWorldBorder();
    }

    public void setWorldBorderSize(World world, double size) {
        world.getWorldBorder().setSize(size);
    }

    public void setWorldBorderCenter(World world, double x, double z) {
        world.getWorldBorder().setCenter(x, z);
    }

    // ===== ADVANCEMENT METHODS =====
    public void grantAdvancement(Player player, String advancementKey) {
        var advancement = Bukkit.getAdvancement(NamespacedKey.minecraft(advancementKey));
        if (advancement != null) {
            player.getAdvancementProgress(advancement).awardCriteria("impossible");
        }
    }

    public void revokeAdvancement(Player player, String advancementKey) {
        var advancement = Bukkit.getAdvancement(NamespacedKey.minecraft(advancementKey));
        if (advancement != null) {
            player.getAdvancementProgress(advancement).revokeCriteria("impossible");
        }
    }

    // ===== TEAM METHODS =====
    public Team getTeam(Scoreboard scoreboard, String name) {
        return scoreboard.getTeam(name);
    }

    public Team createTeam(Scoreboard scoreboard, String name) {
        return scoreboard.registerNewTeam(name);
    }

    // ===== OBJECTIVE METHODS =====
    public Objective getObjective(Scoreboard scoreboard, String name) {
        return scoreboard.getObjective(name);
    }

    public Objective createObjective(Scoreboard scoreboard, String name, String criteria, String displayName) {
        Objective objective = scoreboard.registerNewObjective(name, criteria, ChatColor.translateAlternateColorCodes('&', displayName));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        return objective;
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

    // ===== COLOR METHODS =====
    public String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public String stripColor(String text) {
        return ChatColor.stripColor(text);
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

    public String getMotd() {
        return plugin.getServer().getMotd();
    }

    // ===== CONSOLE METHODS =====
    public void executeCommand(String command) {
        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
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

    // ===== BLOCK METHODS =====
    public Block getBlock(Location location) {
        return location.getBlock();
    }

    public Material getBlockType(Location location) {
        return location.getBlock().getType();
    }

    public void setBlockType(Location location, Material material) {
        location.getBlock().setType(material);
    }

    // ===== PLAYER EXPERIENCE =====
    public void setExp(Player player, float exp) {
        player.setExp(exp);
    }

    public float getExp(Player player) {
        return player.getExp();
    }

    public void setLevel(Player player, int level) {
        player.setLevel(level);
    }

    public int getLevel(Player player) {
        return player.getLevel();
    }

    public void giveExp(Player player, int amount) {
        player.giveExp(amount);
    }

    // ===== INVENTORY CLICK EVENTS =====
    private final Map<Inventory, Object> inventoryClickHandlers = new HashMap<>();
    private final Map<Inventory, Object> inventoryCloseHandlers = new HashMap<>();

    public void registerInventoryClick(Inventory inventory, Object handler) {
        inventoryClickHandlers.put(inventory, handler);
        // Register the event if not already registered
        registerEvent(InventoryClickEvent.class, new java.util.function.Consumer<InventoryClickEvent>() {
            @Override
            public void accept(InventoryClickEvent event) {
                if (event.getInventory().equals(inventory)) {
                    handleInventoryClick(event);
                }
            }
        });
    }

    public void registerInventoryClose(Inventory inventory, Object handler) {
        inventoryCloseHandlers.put(inventory, handler);
        // Register the event if not already registered
        registerEvent(InventoryCloseEvent.class, new java.util.function.Consumer<InventoryCloseEvent>() {
            @Override
            public void accept(InventoryCloseEvent event) {
                if (event.getInventory().equals(inventory)) {
                    handleInventoryClose(event);
                }
            }
        });
    }

    // Internal method called by event system
    private void handleInventoryClick(InventoryClickEvent event) {
        Object handler = inventoryClickHandlers.get(event.getInventory());
        if (handler instanceof Function && scope != null) {
            executeFunction((Function) handler, event);
        }
    }

    private void handleInventoryClose(InventoryCloseEvent event) {
        Object handler = inventoryCloseHandlers.get(event.getInventory());
        if (handler instanceof Function && scope != null) {
            executeFunction((Function) handler, event);
        }
    }

    // ===== BOSSBAR METHODS =====
    public org.bukkit.boss.BossBar createBossBar(String title, org.bukkit.boss.BarColor color, org.bukkit.boss.BarStyle style) {
        return Bukkit.createBossBar(
            ChatColor.translateAlternateColorCodes('&', title),
            color != null ? color : org.bukkit.boss.BarColor.PURPLE,
            style != null ? style : org.bukkit.boss.BarStyle.SOLID
        );
    }

    public org.bukkit.boss.BossBar createBossBar(String title, String colorName, String styleName) {
        org.bukkit.boss.BarColor color = org.bukkit.boss.BarColor.PURPLE;
        org.bukkit.boss.BarStyle style = org.bukkit.boss.BarStyle.SOLID;
        
        try {
            if (colorName != null) {
                color = org.bukkit.boss.BarColor.valueOf(colorName.toUpperCase());
            }
        } catch (Exception e) {
            // Use default
        }
        
        try {
            if (styleName != null) {
                style = org.bukkit.boss.BarStyle.valueOf(styleName.toUpperCase());
            }
        } catch (Exception e) {
            // Use default
        }
        
        return createBossBar(title, color, style);
    }

    // ===== COOLDOWN SYSTEM =====
    private final Map<String, Map<String, Long>> cooldowns = new HashMap<>();

    public boolean hasCooldown(String playerName, String cooldownKey) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerName);
        if (playerCooldowns == null) return false;
        
        Long cooldownEnd = playerCooldowns.get(cooldownKey);
        if (cooldownEnd == null) return false;
        
        return System.currentTimeMillis() < cooldownEnd;
    }

    public void setCooldown(String playerName, String cooldownKey, long durationMillis) {
        cooldowns.computeIfAbsent(playerName, k -> new HashMap<>())
                 .put(cooldownKey, System.currentTimeMillis() + durationMillis);
    }

    public long getCooldownRemaining(String playerName, String cooldownKey) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerName);
        if (playerCooldowns == null) return 0;
        
        Long cooldownEnd = playerCooldowns.get(cooldownKey);
        if (cooldownEnd == null) return 0;
        
        long remaining = cooldownEnd - System.currentTimeMillis();
        return remaining > 0 ? remaining : 0;
    }

    public void removeCooldown(String playerName, String cooldownKey) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerName);
        if (playerCooldowns != null) {
            playerCooldowns.remove(cooldownKey);
            if (playerCooldowns.isEmpty()) {
                cooldowns.remove(playerName);
            }
        }
    }

    public void clearCooldowns(String playerName) {
        cooldowns.remove(playerName);
    }

    // ===== EXTENDED DATABASE OPERATIONS =====
    public void updateData(String dbName, String tableName, Map<String, Object> data, String whereClause) {
        StringBuilder setClause = new StringBuilder();
        List<Object> params = new ArrayList<>();
        
        boolean first = true;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!first) setClause.append(", ");
            setClause.append(entry.getKey()).append(" = ?");
            params.add(entry.getValue());
            first = false;
        }
        
        String sql = "UPDATE " + tableName + " SET " + setClause + " WHERE " + whereClause;
        
        try (java.sql.Connection conn = getDatabaseConnection(dbName);
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            stmt.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().severe("Error updating data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void deleteData(String dbName, String tableName, String whereClause) {
        String sql = "DELETE FROM " + tableName + " WHERE " + whereClause;
        executeSQL(dbName, sql);
    }

    public int countRows(String dbName, String tableName) {
        return countRows(dbName, tableName, null);
    }

    public int countRows(String dbName, String tableName, String whereClause) {
        String sql = "SELECT COUNT(*) FROM " + tableName;
        if (whereClause != null && !whereClause.isEmpty()) {
            sql += " WHERE " + whereClause;
        }
        
        List<Map<String, Object>> results = querySQL(dbName, sql);
        if (!results.isEmpty()) {
            Object count = results.get(0).get("COUNT(*)");
            if (count instanceof Number) {
                return ((Number) count).intValue();
            }
        }
        return 0;
    }

    // ===== CONFIG SYSTEM FOR JS PLUGINS =====
    public Map<String, Object> getPluginConfig(String pluginName) {
        File configFile = new File(plugin.getDataFolder(), "js-plugins/" + pluginName + ".config.yml");
        if (!configFile.exists()) {
            return new HashMap<>();
        }
        
        try {
            org.bukkit.configuration.file.YamlConfiguration config = 
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile);
            Map<String, Object> data = new HashMap<>();
            
            for (String key : config.getKeys(true)) {
                data.put(key, config.get(key));
            }
            
            return data;
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading config for plugin '" + pluginName + "': " + e.getMessage());
            return new HashMap<>();
        }
    }

    public void savePluginConfig(String pluginName, Map<String, Object> data) {
        File configFile = new File(plugin.getDataFolder(), "js-plugins/" + pluginName + ".config.yml");
        try {
            org.bukkit.configuration.file.YamlConfiguration config = 
                new org.bukkit.configuration.file.YamlConfiguration();
            
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                config.set(entry.getKey(), entry.getValue());
            }
            
            config.save(configFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Error saving config for plugin '" + pluginName + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Object getPluginConfigValue(String pluginName, String path) {
        return getPluginConfigValue(pluginName, path, null);
    }

    public Object getPluginConfigValue(String pluginName, String path, Object defaultValue) {
        File configFile = new File(plugin.getDataFolder(), "js-plugins/" + pluginName + ".config.yml");
        if (!configFile.exists()) {
            return defaultValue;
        }
        
        try {
            org.bukkit.configuration.file.YamlConfiguration config = 
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile);
            return config.get(path, defaultValue);
        } catch (Exception e) {
            plugin.getLogger().severe("Error reading config value for plugin '" + pluginName + "': " + e.getMessage());
            return defaultValue;
        }
    }

    public void setPluginConfigValue(String pluginName, String path, Object value) {
        File configFile = new File(plugin.getDataFolder(), "js-plugins/" + pluginName + ".config.yml");
        try {
            org.bukkit.configuration.file.YamlConfiguration config = 
                configFile.exists() ? 
                    org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile) :
                    new org.bukkit.configuration.file.YamlConfiguration();
            
            config.set(path, value);
            config.save(configFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Error setting config value for plugin '" + pluginName + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ===== ADVANCED INVENTORY METHODS =====
    public void setInventoryItem(Inventory inventory, int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }

    public ItemStack getInventoryItem(Inventory inventory, int slot) {
        return inventory.getItem(slot);
    }

    public void fillInventory(Inventory inventory, ItemStack item) {
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, item);
        }
    }

    public void fillInventoryRange(Inventory inventory, ItemStack item, int start, int end) {
        for (int i = start; i <= end && i < inventory.getSize(); i++) {
            inventory.setItem(i, item);
        }
    }

    // ===== PLAYER DATA METHODS =====
    public void kickPlayer(Player player, String reason) {
        player.kickPlayer(ChatColor.translateAlternateColorCodes('&', reason));
    }

    public void banPlayer(String playerName, String reason) {
        Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(
            playerName,
            ChatColor.translateAlternateColorCodes('&', reason),
            null,
            null
        );
    }

    public void unbanPlayer(String playerName) {
        Bukkit.getBanList(org.bukkit.BanList.Type.NAME).pardon(playerName);
    }

    public boolean isBanned(String playerName) {
        return Bukkit.getBanList(org.bukkit.BanList.Type.NAME).isBanned(playerName);
    }

    // ===== WORLD METHODS =====
    public void createExplosion(Location location, float power) {
        location.getWorld().createExplosion(location, power);
    }

    public void createExplosion(Location location, float power, boolean setFire) {
        location.getWorld().createExplosion(location, power, setFire);
    }

    public void strikeLightning(Location location) {
        location.getWorld().strikeLightning(location);
    }

    public void strikeLightningEffect(Location location) {
        location.getWorld().strikeLightningEffect(location);
    }

    // ===== ENTITY METHODS =====
    public void setEntityCustomName(Entity entity, String name) {
        entity.setCustomName(ChatColor.translateAlternateColorCodes('&', name));
        entity.setCustomNameVisible(true);
    }

    public void setEntityGlowing(Entity entity, boolean glowing) {
        entity.setGlowing(glowing);
    }

    public void setEntityGravity(Entity entity, boolean gravity) {
        entity.setGravity(gravity);
    }

    public void setEntityInvulnerable(Entity entity, boolean invulnerable) {
        entity.setInvulnerable(invulnerable);
    }

    public void removeEntity(Entity entity) {
        entity.remove();
    }

    // ===== BLOCK METHODS =====
    public void breakBlock(Location location) {
        location.getBlock().breakNaturally();
    }

    public void breakBlock(Location location, boolean dropItems) {
        location.getBlock().breakNaturally(new ItemStack(Material.AIR));
    }

    public Block getBlockAt(World world, int x, int y, int z) {
        return world.getBlockAt(x, y, z);
    }

    public List<Block> getBlocksInRadius(Location center, double radius) {
        List<Block> blocks = new ArrayList<>();
        int radiusInt = (int) Math.ceil(radius);
        
        for (int x = -radiusInt; x <= radiusInt; x++) {
            for (int y = -radiusInt; y <= radiusInt; y++) {
                for (int z = -radiusInt; z <= radiusInt; z++) {
                    Location loc = center.clone().add(x, y, z);
                    if (loc.distance(center) <= radius) {
                        blocks.add(loc.getBlock());
                    }
                }
            }
        }
        
        return blocks;
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
        return loc1.distance(loc2);
    }

    public double getDistanceSquared(Location loc1, Location loc2) {
        return loc1.distanceSquared(loc2);
    }

    public Location getMidpoint(Location loc1, Location loc2) {
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
        for (String item : list) {
            if (item.toLowerCase().contains(filter.toLowerCase())) {
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
}
