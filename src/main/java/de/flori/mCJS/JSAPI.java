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
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
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
    
    /**
     * Check if debug mode is enabled in config
     */
    private boolean isDebugMode() {
        return plugin.getConfig().getBoolean("settings.debug-mode", false);
    }
    
    /**
     * Debug logging helper
     */
    private void debug(String message) {
        if (isDebugMode()) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }
    
    /**
     * Execute a runnable with timeout protection
     */
    private void executeWithTimeout(Runnable task, long timeoutMs, String context) {
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        java.util.concurrent.Future<?> future = executor.submit(task);
        try {
            future.get(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            plugin.getLogger().warning("Event handler execution timed out after " + timeoutMs + "ms for " + context);
            debug("Timeout occurred for: " + context);
        } catch (Exception e) {
            plugin.getLogger().severe("Error executing timed task for " + context + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
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
        debug("Registering command: " + name + " (with description and usage)");
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
        debug("Registering simple command: " + name);
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
        debug("Registering event: " + eventClassName);
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
            
            // If not found yet, try different event packages (skip base org.bukkit.event. package)
            // But only if eventClassName doesn't already contain a dot (which means it was already processed)
            if (eventClass == null && !eventClassName.contains(".")) {
                String[] packages = {
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
                        Class<?> foundClass = Class.forName(pkg + eventClassName);
                        // Double check it's not the base Event class
                        if (foundClass != Event.class && Event.class.isAssignableFrom(foundClass)) {
                            eventClass = foundClass;
                            break;
                        }
                    } catch (ClassNotFoundException e) {
                        // Try next package
                    }
                }
            }
            
            if (eventClass == null) {
                plugin.getLogger().severe("Event class not found: " + eventClassName);
                return;
            }
            
            // Final validation: ensure it's not the base Event class
            if (eventClass == Event.class) {
                plugin.getLogger().warning("Cannot register base Event class. Use a specific event type like 'player.PlayerJoinEvent'.");
                return;
            }
            
            // Ensure it's a valid event class
            if (Event.class.isAssignableFrom(eventClass)) {
                @SuppressWarnings("unchecked")
                Class<? extends Event> clazz = (Class<? extends Event>) eventClass;
                
                // Log the resolved event class for debugging
                plugin.getLogger().info("Registering event: " + eventClassName + " -> " + clazz.getName());
                debug("Resolved event class: " + clazz.getName() + " for " + eventClassName);
                
                registerEvent(clazz, handler);
            } else {
                plugin.getLogger().warning("Class '" + eventClassName + "' is not an Event class.");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error registering event '" + eventClassName + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    public <T extends Event> void registerEvent(Class<T> eventClass, Object handler) {
        registerEvent(eventClass, handler, EventPriority.NORMAL);
    }

    // Event handler storage - universal listener handles all events
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

    // Map to track which event classes have been registered
    private static final Set<Class<? extends Event>> registeredEventClasses = new HashSet<>();
    
    // Register a specific event class with PaperMC using reflection to find the right method
    private static void registerEventClass(Class<? extends Event> eventClass, JavaPlugin plugin) {
        synchronized (registeredEventClasses) {
            if (registeredEventClasses.contains(eventClass)) {
                return; // Already registered
            }
            
            // Log for debugging
            plugin.getLogger().info("Attempting to register event class: " + eventClass.getName() + " (is Event.class: " + (eventClass == Event.class) + ")");
            
            try {
                // Try to use PaperMC's registerEvent method with Consumer via reflection
                java.lang.reflect.Method registerEventMethod = null;
                try {
                    // Try the method signature: registerEvent(Class, Listener, EventPriority, Consumer, Plugin)
                    registerEventMethod = plugin.getServer().getPluginManager().getClass()
                        .getMethod("registerEvent", Class.class, Listener.class, EventPriority.class, 
                                  java.util.function.Consumer.class, Plugin.class);
                } catch (NoSuchMethodException e) {
                    // Method doesn't exist, try alternative approach
                }
                
                if (registerEventMethod != null) {
                    // Use the Consumer-based registration
                    Listener dummyListener = new Listener() {};
                    java.util.function.Consumer<Event> consumer = (event) -> {
                        // Use executeEventWrapper to prevent duplicate execution
                        executeEventWrapper(dummyListener, event);
                    };
                    
                    registerEventMethod.invoke(plugin.getServer().getPluginManager(), 
                        eventClass, dummyListener, EventPriority.NORMAL, consumer, plugin);
                } else {
                    // Fallback: Register event directly using HandlerList
                    registerEventViaHandlerList(eventClass, plugin);
                }
                
                registeredEventClasses.add(eventClass);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to register event class " + eventClass.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    // Register event directly by manipulating HandlerList
    private static <T extends Event> void registerEventViaHandlerList(Class<T> eventClass, JavaPlugin plugin) {
        try {
            // Get the HandlerList for this event class
            java.lang.reflect.Method getHandlerListMethod = eventClass.getMethod("getHandlerList");
            org.bukkit.event.HandlerList handlerList = (org.bukkit.event.HandlerList) getHandlerListMethod.invoke(null);
            
            // Create a listener that will handle this specific event class
            Listener dummyListener = new Listener() {};
            
            // Create an EventExecutor that will call our wrapper method
            org.bukkit.plugin.EventExecutor executor = new org.bukkit.plugin.EventExecutor() {
                @Override
                public void execute(Listener listener, Event event) throws EventException {
                    executeEventWrapper(listener, event);
                }
            };
            
            // Use the EventExecutor-based constructor: RegisteredListener(Listener, EventExecutor, EventPriority, Plugin, boolean)
            try {
                java.lang.reflect.Constructor<RegisteredListener> constructor = 
                    RegisteredListener.class.getConstructor(Listener.class, 
                        org.bukkit.plugin.EventExecutor.class, EventPriority.class, Plugin.class, boolean.class);
                
                // Register only once with NORMAL priority - we'll handle all priorities in dispatchEvent
                RegisteredListener registeredListener = constructor.newInstance(
                    dummyListener, executor, EventPriority.NORMAL, plugin, false
                );
                handlerList.register(registeredListener);
                
                plugin.getLogger().info("Successfully registered event class " + eventClass.getName() + " via HandlerList");
            } catch (NoSuchMethodException e) {
                // Fallback: try to use reflection to find the constructor
                plugin.getLogger().warning("Standard RegisteredListener constructor not found. Trying reflection...");
                java.lang.reflect.Constructor<?>[] constructors = RegisteredListener.class.getDeclaredConstructors();
                for (java.lang.reflect.Constructor<?> constructor : constructors) {
                    constructor.setAccessible(true);
                    Class<?>[] paramTypes = constructor.getParameterTypes();
                    if (paramTypes.length == 5 &&
                        paramTypes[0] == Listener.class &&
                        paramTypes[1] == org.bukkit.plugin.EventExecutor.class &&
                        paramTypes[2] == EventPriority.class &&
                        paramTypes[3] == Plugin.class &&
                        paramTypes[4] == boolean.class) {
                        // Register only once with NORMAL priority - we'll handle all priorities in dispatchEvent
                        RegisteredListener registeredListener = (RegisteredListener) constructor.newInstance(
                            dummyListener, executor, EventPriority.NORMAL, plugin, false
                        );
                        handlerList.register(registeredListener);
                        plugin.getLogger().info("Successfully registered event class " + eventClass.getName() + " via HandlerList (using reflection)");
                        return;
                    }
                }
                
                plugin.getLogger().severe("Cannot register events: No suitable RegisteredListener constructor found. Events will not work.");
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to register event via HandlerList for " + eventClass.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Track events that are currently being dispatched to prevent duplicate execution
    private static final Set<Event> eventsBeingDispatched = Collections.synchronizedSet(Collections.newSetFromMap(new java.util.IdentityHashMap<>()));
    
    // Wrapper method that gets the event class from the map and dispatches
    @SuppressWarnings("unused")
    private static void executeEventWrapper(Listener listener, Event event) {
        // Prevent duplicate execution if this event is already being dispatched
        // This can happen if multiple plugins register the same event class
        synchronized (eventsBeingDispatched) {
            if (eventsBeingDispatched.contains(event)) {
                // Event is already being dispatched, skip
                if (listenerPlugin != null && listenerPlugin.getConfig().getBoolean("settings.debug-mode", false)) {
                    listenerPlugin.getLogger().info("[DEBUG] Skipping duplicate event dispatch for " + event.getClass().getSimpleName());
                }
                return;
            }
            eventsBeingDispatched.add(event);
        }
        
        try {
            // Find which event class this handler is for by checking the method map
            // Since we can't bind parameters, we need to check all registered event classes
            synchronized (globalEventHandlers) {
                for (Class<? extends Event> eventClass : globalEventHandlers.keySet()) {
                    if (eventClass.isInstance(event)) {
                        // Dispatch once for all priorities - dispatchEvent will filter by priority
                        dispatchEventForAllPriorities(event);
                        break; // Only dispatch once per event
                    }
                }
            }
        } finally {
            // Remove from tracking set after dispatch completes
            synchronized (eventsBeingDispatched) {
                eventsBeingDispatched.remove(event);
            }
        }
    }
    
    // Dispatch event to all handlers regardless of priority (priority filtering happens inside)
    private static void dispatchEventForAllPriorities(Event event) {
        Class<? extends Event> actualEventClass = event.getClass();
        
        // Debug logging (only if any JSAPI instance has debug mode enabled)
        if (listenerPlugin != null && listenerPlugin.getConfig().getBoolean("settings.debug-mode", false)) {
            listenerPlugin.getLogger().info("[DEBUG] Dispatching event: " + actualEventClass.getSimpleName());
        }
        
        // Check all registered event classes to see if they match
        synchronized (globalEventHandlers) {
            for (Map.Entry<Class<? extends Event>, Map<JSAPI, List<EventHandlerInfo>>> entry : globalEventHandlers.entrySet()) {
                Class<? extends Event> registeredClass = entry.getKey();
                
                // Check if the actual event class matches or is a subclass of the registered class
                if (registeredClass.isAssignableFrom(actualEventClass)) {
                    Map<JSAPI, List<EventHandlerInfo>> handlersForClass = entry.getValue();
                    
                    for (Map.Entry<JSAPI, List<EventHandlerInfo>> apiEntry : handlersForClass.entrySet()) {
                        JSAPI api = apiEntry.getKey();
                        List<EventHandlerInfo> handlers = apiEntry.getValue();
                        
                        // Execute all handlers for this event (priority is stored in EventHandlerInfo)
                        for (EventHandlerInfo info : handlers) {
                            if (info.handler instanceof Function && api.scope != null) {
                                try {
                                    if (listenerPlugin != null && listenerPlugin.getConfig().getBoolean("settings.debug-mode", false)) {
                                        listenerPlugin.getLogger().info("[DEBUG] Executing event handler for " + actualEventClass.getSimpleName() + 
                                                                      " (priority: " + info.priority + ")");
                                    }
                                    
                                    // Get max execution time from config
                                    long maxExecutionTime = listenerPlugin != null ? 
                                        listenerPlugin.getConfig().getLong("performance.max-execution-time", 5000) : 5000;
                                    
                                    // Execute with timeout if configured
                                    if (maxExecutionTime > 0) {
                                        api.executeWithTimeout(() -> {
                                            org.mozilla.javascript.Context rhinoContext = org.mozilla.javascript.Context.enter();
                                            try {
                                                int optimizationLevel = listenerPlugin != null ? 
                                                    listenerPlugin.getConfig().getInt("performance.optimization-level", -1) : -1;
                                                rhinoContext.setOptimizationLevel(optimizationLevel);
                                                rhinoContext.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                                                Function func = (Function) info.handler;
                                                func.call(rhinoContext, api.scope, api.scope, new Object[]{event});
                                                if (listenerPlugin != null && listenerPlugin.getConfig().getBoolean("settings.debug-mode", false)) {
                                                    listenerPlugin.getLogger().info("[DEBUG] Event handler execution completed");
                                                }
                                            } finally {
                                                org.mozilla.javascript.Context.exit();
                                            }
                                        }, maxExecutionTime, actualEventClass.getSimpleName());
                                    } else {
                                        // No timeout
                                        org.mozilla.javascript.Context rhinoContext = org.mozilla.javascript.Context.enter();
                                        try {
                                            int optimizationLevel = listenerPlugin != null ? 
                                                listenerPlugin.getConfig().getInt("performance.optimization-level", -1) : -1;
                                            rhinoContext.setOptimizationLevel(optimizationLevel);
                                            rhinoContext.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                                            Function func = (Function) info.handler;
                                            func.call(rhinoContext, api.scope, api.scope, new Object[]{event});
                                            if (listenerPlugin != null && listenerPlugin.getConfig().getBoolean("settings.debug-mode", false)) {
                                                listenerPlugin.getLogger().info("[DEBUG] Event handler execution completed");
                                            }
                                        } finally {
                                            org.mozilla.javascript.Context.exit();
                                        }
                                    }
                                } catch (Exception e) {
                                    if (listenerPlugin != null) {
                                        listenerPlugin.getLogger().severe("Error in JS event handler for " + actualEventClass.getSimpleName() + ": " + e.getMessage());
                                        if (listenerPlugin.getConfig().getBoolean("settings.debug-mode", false)) {
                                            listenerPlugin.getLogger().info("[DEBUG] Event handler error details: " + e.getClass().getName());
                                        }
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    

    public <T extends Event> void registerEvent(Class<T> eventClass, Object handler, EventPriority priority) {
        debug("Registering event handler for " + eventClass.getName() + " with priority " + priority);
        // Check if Event is the base class (which doesn't have getHandlerList)
        if (eventClass == null || eventClass == Event.class) {
            plugin.getLogger().warning("Cannot register handler for base Event class. Use a specific event type.");
            debug("Attempted to register base Event class, rejected");
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
        
        // Register this specific event class with PaperMC
        registerEventClass(eventClass, plugin);
        
        // Store handler
        synchronized (globalEventHandlers) {
            globalEventHandlers.computeIfAbsent(eventClass, k -> new HashMap<>())
                              .computeIfAbsent(this, k -> new ArrayList<>())
                              .add(new EventHandlerInfo(handler, priority));
            debug("Stored event handler for " + eventClass.getName() + " (total handlers: " + 
                  globalEventHandlers.get(eventClass).get(this).size() + ")");
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
        return name != null ? Bukkit.getPlayer(name) : null;
    }

    public Player getPlayerExact(String name) {
        return name != null ? Bukkit.getPlayerExact(name) : null;
    }
    
    public Player getPlayerByUUID(String uuid) {
        if (uuid == null) {
            return null;
        }
        try {
            return Bukkit.getPlayer(java.util.UUID.fromString(uuid));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    public OfflinePlayer getOfflinePlayer(String name) {
        return name != null ? Bukkit.getOfflinePlayer(name) : null;
    }
    
    public OfflinePlayer getOfflinePlayerByUUID(String uuid) {
        if (uuid == null) {
            return null;
        }
        try {
            return Bukkit.getOfflinePlayer(java.util.UUID.fromString(uuid));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public Collection<? extends Player> getOnlinePlayers() {
        return Bukkit.getOnlinePlayers();
    }
    
    public boolean isPlayerOnline(String name) {
        return name != null && Bukkit.getPlayer(name) != null;
    }
    
    public boolean isPlayerOnline(Player player) {
        return player != null && player.isOnline();
    }
    
    public void kickPlayer(Player player, String reason) {
        if (player != null && reason != null) {
            player.kickPlayer(ChatColor.translateAlternateColorCodes('&', reason));
        }
    }
    
    public void banPlayer(String playerName, String reason) {
        if (playerName == null) {
            return;
        }
        try {
            Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(
                playerName,
                reason != null ? ChatColor.translateAlternateColorCodes('&', reason) : "Banned by an operator",
                null,
                null
            );
        } catch (Exception e) {
            plugin.getLogger().warning("Error banning player '" + playerName + "': " + e.getMessage());
        }
    }
    
    public void unbanPlayer(String playerName) {
        if (playerName != null) {
            try {
                Bukkit.getBanList(org.bukkit.BanList.Type.NAME).pardon(playerName);
            } catch (Exception e) {
                plugin.getLogger().warning("Error unbanning player '" + playerName + "': " + e.getMessage());
            }
        }
    }
    
    public boolean isBanned(String playerName) {
        return playerName != null && Bukkit.getBanList(org.bukkit.BanList.Type.NAME).isBanned(playerName);
    }
    
    public void addToWhitelist(String playerName) {
        if (playerName != null) {
            Bukkit.getWhitelistedPlayers().add(Bukkit.getOfflinePlayer(playerName));
        }
    }
    
    public void removeFromWhitelist(String playerName) {
        if (playerName != null) {
            Bukkit.getWhitelistedPlayers().remove(Bukkit.getOfflinePlayer(playerName));
        }
    }
    
    public boolean isWhitelisted(String playerName) {
        return playerName != null && Bukkit.getOfflinePlayer(playerName).isWhitelisted();
    }
    
    public void setWhitelistEnabled(boolean enabled) {
        Bukkit.setWhitelist(enabled);
    }
    
    public boolean isWhitelistEnabled() {
        return Bukkit.hasWhitelist();
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
    
    /**
     * Create a custom inventory with a simple holder
     */
    public Inventory createCustomInventory(int size, String title) {
        if (size % 9 != 0 || size < 9 || size > 54) {
            plugin.getLogger().warning("Invalid inventory size: " + size + ". Must be a multiple of 9 between 9 and 54.");
            size = Math.max(9, Math.min(54, (size / 9) * 9));
        }
        CustomInventoryHolder holder = new CustomInventoryHolder();
        Inventory inv = Bukkit.createInventory(holder, size, ChatColor.translateAlternateColorCodes('&', title));
        holder.setInventory(inv);
        return inv;
    }
    
    /**
     * Create a custom inventory with a specific size (rows)
     */
    public Inventory createCustomInventory(int rows, String title, boolean useRows) {
        if (useRows) {
            int size = rows * 9;
            if (size < 9 || size > 54) {
                plugin.getLogger().warning("Invalid inventory rows: " + rows + ". Must be between 1 and 6.");
                rows = Math.max(1, Math.min(6, rows));
                size = rows * 9;
            }
            return createCustomInventory(size, title);
        } else {
            return createCustomInventory(rows, title);
        }
    }
    
    /**
     * Create a custom inventory holder for advanced usage
     */
    public CustomInventoryHolder createInventoryHolder() {
        return new CustomInventoryHolder();
    }
    
    /**
     * Create inventory with custom holder
     */
    public Inventory createInventoryWithHolder(CustomInventoryHolder holder, int size, String title) {
        if (size % 9 != 0 || size < 9 || size > 54) {
            plugin.getLogger().warning("Invalid inventory size: " + size + ". Must be a multiple of 9 between 9 and 54.");
            size = Math.max(9, Math.min(54, (size / 9) * 9));
        }
        Inventory inv = Bukkit.createInventory(holder, size, ChatColor.translateAlternateColorCodes('&', title));
        holder.setInventory(inv);
        return inv;
    }
    
    /**
     * Custom Inventory Holder class for advanced inventory management
     */
    public static class CustomInventoryHolder implements InventoryHolder {
        private Inventory inventory;
        private final Map<String, Object> data = new HashMap<>();
        private volatile boolean closed = false;
        
        @Override
        public Inventory getInventory() {
            return inventory;
        }
        
        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }
        
        /**
         * Store custom data in the holder (thread-safe)
         */
        public synchronized void setData(String key, Object value) {
            if (!closed) {
                data.put(key, value);
            }
        }
        
        /**
         * Get custom data from the holder (thread-safe)
         */
        public synchronized Object getData(String key) {
            return data.get(key);
        }
        
        /**
         * Get all custom data (thread-safe)
         */
        public synchronized Map<String, Object> getAllData() {
            return new HashMap<>(data);
        }
        
        /**
         * Remove custom data (thread-safe)
         */
        public synchronized void removeData(String key) {
            data.remove(key);
        }
        
        /**
         * Clear all custom data (thread-safe)
         */
        public synchronized void clearData() {
            data.clear();
        }
        
        /**
         * Mark this inventory as closed
         */
        public synchronized void markClosed() {
            closed = true;
        }
        
        /**
         * Check if inventory is closed
         */
        public synchronized boolean isClosed() {
            return closed;
        }
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
        if (item == null || lore == null) {
            return item;
        }
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
    
    // ===== ENCHANTMENT METHODS =====
    public void addEnchantment(ItemStack item, String enchantmentName, int level) {
        if (item == null || enchantmentName == null) {
            return;
        }
        try {
            org.bukkit.enchantments.Enchantment enchantment = org.bukkit.enchantments.Enchantment.getByName(enchantmentName.toUpperCase());
            if (enchantment != null) {
                item.addUnsafeEnchantment(enchantment, level);
            } else {
                plugin.getLogger().warning("Enchantment '" + enchantmentName + "' not found");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error adding enchantment '" + enchantmentName + "': " + e.getMessage());
        }
    }
    
    public void removeEnchantment(ItemStack item, String enchantmentName) {
        if (item == null || enchantmentName == null) {
            return;
        }
        try {
            org.bukkit.enchantments.Enchantment enchantment = org.bukkit.enchantments.Enchantment.getByName(enchantmentName.toUpperCase());
            if (enchantment != null) {
                item.removeEnchantment(enchantment);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error removing enchantment '" + enchantmentName + "': " + e.getMessage());
        }
    }
    
    public boolean hasEnchantment(ItemStack item, String enchantmentName) {
        if (item == null || enchantmentName == null) {
            return false;
        }
        try {
            org.bukkit.enchantments.Enchantment enchantment = org.bukkit.enchantments.Enchantment.getByName(enchantmentName.toUpperCase());
            return enchantment != null && item.containsEnchantment(enchantment);
        } catch (Exception e) {
            return false;
        }
    }
    
    public int getEnchantmentLevel(ItemStack item, String enchantmentName) {
        if (item == null || enchantmentName == null) {
            return 0;
        }
        try {
            org.bukkit.enchantments.Enchantment enchantment = org.bukkit.enchantments.Enchantment.getByName(enchantmentName.toUpperCase());
            return enchantment != null ? item.getEnchantmentLevel(enchantment) : 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    public Map<String, Integer> getEnchantments(ItemStack item) {
        Map<String, Integer> enchantments = new HashMap<>();
        if (item == null) {
            return enchantments;
        }
        try {
            for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
                enchantments.put(entry.getKey().getName(), entry.getValue());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting enchantments: " + e.getMessage());
        }
        return enchantments;
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
        insertDataAndGetId(dbName, tableName, data);
    }

    public long insertDataAndGetId(String dbName, String tableName, Map<String, Object> data) {
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
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            stmt.executeUpdate();
            
            // Get the generated ID
            try (java.sql.ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }
            
            // Fallback: use last_insert_rowid()
            try (java.sql.Statement stmt2 = conn.createStatement();
                 java.sql.ResultSet rs = stmt2.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error inserting data: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
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
        if (sender == null || permission == null) {
            return false;
        }
        return sender.hasPermission(permission);
    }
    
    public boolean hasPermission(Player player, String permission) {
        if (player == null || permission == null) {
            return false;
        }
        return player.hasPermission(permission);
    }
    
    public void addPermission(Player player, String permission) {
        if (player == null || permission == null) {
            return;
        }
        try {
            org.bukkit.permissions.PermissionAttachment attachment = player.addAttachment(plugin);
            attachment.setPermission(permission, true);
        } catch (Exception e) {
            plugin.getLogger().warning("Error adding permission '" + permission + "' to player '" + player.getName() + "': " + e.getMessage());
        }
    }
    
    public void removePermission(Player player, String permission) {
        if (player == null || permission == null) {
            return;
        }
        try {
            org.bukkit.permissions.PermissionAttachment attachment = player.addAttachment(plugin);
            attachment.setPermission(permission, false);
        } catch (Exception e) {
            plugin.getLogger().warning("Error removing permission '" + permission + "' from player '" + player.getName() + "': " + e.getMessage());
        }
    }
    
    public boolean isOp(Player player) {
        return player != null && player.isOp();
    }
    
    public void setOp(Player player, boolean op) {
        if (player != null) {
            player.setOp(op);
        }
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
        if (world != null) {
            world.setStorm(storm);
        }
    }
    
    // ===== WORLD MANAGEMENT METHODS =====
    public void setWorldDifficulty(World world, String difficulty) {
        if (world == null || difficulty == null) {
            return;
        }
        try {
            Difficulty diff = Difficulty.valueOf(difficulty.toUpperCase());
            world.setDifficulty(diff);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid difficulty: " + difficulty);
        }
    }
    
    public String getWorldDifficulty(World world) {
        return world != null ? world.getDifficulty().name() : null;
    }
    
    public void setWorldPVP(World world, boolean pvp) {
        if (world != null) {
            world.setPVP(pvp);
        }
    }
    
    public boolean isWorldPVP(World world) {
        return world != null && world.getPVP();
    }
    
    public void setWorldSpawnLocation(World world, Location location) {
        if (world != null && location != null) {
            world.setSpawnLocation(location);
        }
    }
    
    public Location getWorldSpawnLocation(World world) {
        return world != null ? world.getSpawnLocation() : null;
    }
    
    public void setWorldKeepSpawnInMemory(World world, boolean keepLoaded) {
        if (world != null) {
            world.setKeepSpawnInMemory(keepLoaded);
        }
    }
    
    public boolean isWorldKeepSpawnInMemory(World world) {
        return world != null && world.getKeepSpawnInMemory();
    }
    
    public void setWorldAutoSave(World world, boolean autoSave) {
        if (world != null) {
            world.setAutoSave(autoSave);
        }
    }
    
    public boolean isWorldAutoSave(World world) {
        return world != null && world.isAutoSave();
    }
    
    public World.Environment getWorldEnvironment(World world) {
        return world != null ? world.getEnvironment() : null;
    }
    
    public long getWorldSeed(World world) {
        return world != null ? world.getSeed() : 0;
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
    private final Map<Inventory, Object> inventoryClickHandlers = Collections.synchronizedMap(new HashMap<>());
    private final Map<Inventory, Object> inventoryCloseHandlers = Collections.synchronizedMap(new HashMap<>());
    private static boolean inventoryClickEventRegistered = false;
    private static boolean inventoryCloseEventRegistered = false;

    public void registerInventoryClick(Inventory inventory, Object handler) {
        inventoryClickHandlers.put(inventory, handler);
        
        // Register the event globally if not already registered
        if (!inventoryClickEventRegistered) {
            inventoryClickEventRegistered = true;
            // Register a global handler that dispatches to the right inventory handler
            registerEvent("inventory.InventoryClickEvent", createInventoryClickWrapper());
        }
    }

    public void registerInventoryClose(Inventory inventory, Object handler) {
        inventoryCloseHandlers.put(inventory, handler);
        
        // Register the event globally if not already registered
        if (!inventoryCloseEventRegistered) {
            inventoryCloseEventRegistered = true;
            registerEvent("inventory.InventoryCloseEvent", createInventoryCloseWrapper());
        }
    }
    
    /**
     * Create a wrapper function for inventory click events
     * This will be called from the event system and dispatch to the right handlers
     */
    private Object createInventoryClickWrapper() {
        // Create a JavaScript function that calls handleInventoryClickForAll
        // We'll register it as a regular event handler that gets called by dispatchEventForAllPriorities
        org.mozilla.javascript.Context cx = org.mozilla.javascript.Context.enter();
        try {
            cx.setOptimizationLevel(-1);
            cx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
            Scriptable wrapperScope = cx.initStandardObjects();
            
            // Create a function that calls handleInventoryClickForAll
            // We'll use a simple approach: create a function that checks all JSAPI instances
            String functionCode = "function(event) { " +
                "var JSAPI = Packages.de.flori.mCJS.JSAPI; " +
                "JSAPI.handleInventoryClickForAll(event); " +
                "}";
            
            return cx.evaluateString(wrapperScope, functionCode, "<internal>", 1, null);
        } finally {
            org.mozilla.javascript.Context.exit();
        }
    }
    
    /**
     * Create a wrapper function for inventory close events
     */
    private Object createInventoryCloseWrapper() {
        org.mozilla.javascript.Context cx = org.mozilla.javascript.Context.enter();
        try {
            cx.setOptimizationLevel(-1);
            cx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
            Scriptable wrapperScope = cx.initStandardObjects();
            
            String functionCode = "function(event) { " +
                "var JSAPI = Packages.de.flori.mCJS.JSAPI; " +
                "JSAPI.handleInventoryCloseForAll(event); " +
                "}";
            
            return cx.evaluateString(wrapperScope, functionCode, "<internal>", 1, null);
        } finally {
            org.mozilla.javascript.Context.exit();
        }
    }
    
    /**
     * Handle inventory click for all registered inventories
     * Improved version with proper top/bottom inventory handling, shift-click, drag support
     */
    public static void handleInventoryClickForAll(InventoryClickEvent event) {
        Inventory clickedInv = event.getClickedInventory();
        Inventory topInv = event.getInventory();
        
        // Ignore clicks outside inventory
        if (clickedInv == null) {
            return;
        }
        
        // Only handle clicks in the top inventory (GUI), not player inventory
        // Exception: Shift-clicks from player inventory should also be handled
        boolean isTopInventory = clickedInv.equals(topInv);
        boolean isShiftClick = event.isShiftClick();
        
        // For custom inventories, we only care about top inventory clicks
        // Shift-clicks from player inventory are handled separately
        if (!isTopInventory && !isShiftClick) {
            return;
        }
        
        // Check all JSAPI instances for handlers
        synchronized (globalEventHandlers) {
            for (Map.Entry<Class<? extends Event>, Map<JSAPI, List<EventHandlerInfo>>> entry : globalEventHandlers.entrySet()) {
                if (entry.getKey() == InventoryClickEvent.class) {
                    Map<JSAPI, List<EventHandlerInfo>> handlersMap = entry.getValue();
                    for (JSAPI api : handlersMap.keySet()) {
                        Object handler = api.inventoryClickHandlers.get(topInv);
                        
                        // Check if it's a custom inventory with slot-specific handlers
                        if (topInv.getHolder() instanceof CustomInventoryHolder) {
                            CustomInventoryHolder holder = (CustomInventoryHolder) topInv.getHolder();
                            
                            // Skip if inventory is closed
                            if (holder.isClosed()) {
                                continue;
                            }
                            
                            @SuppressWarnings("unchecked")
                            Map<Integer, Object> slotHandlers = (Map<Integer, Object>) holder.getData("clickHandlers");
                            Object globalHandler = holder.getData("globalClickHandler");
                            Object allowRemovalObj = holder.getData("allowItemRemoval");
                            boolean allowItemRemoval = allowRemovalObj instanceof Boolean ? (Boolean) allowRemovalObj : false;
                            
                            // Handle drag events - cancel them if item removal is not allowed
                            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && !allowItemRemoval) {
                                event.setCancelled(true);
                                return;
                            }
                            
                            // If item removal is not allowed, cancel the event by default
                            // The handler can still call setCancelled(false) if needed
                            if (!allowItemRemoval && isTopInventory) {
                                // Only cancel if clicking in top inventory
                                // Shift-clicks from player inventory are handled by the handler
                                event.setCancelled(true);
                            }
                            
                            // Handle slot-specific or global handlers
                            if (slotHandlers != null || globalHandler != null) {
                                int slot = event.getSlot();
                                
                                // For shift-clicks from player inventory, slot is -999
                                // We should still call the global handler if available
                                Object slotHandler = null;
                                if (slot >= 0 && slotHandlers != null) {
                                    slotHandler = slotHandlers.get(slot);
                                }
                                
                                // Execute slot-specific handler first
                                if (slotHandler instanceof Function && api.scope != null && slot >= 0) {
                                    try {
                                        org.mozilla.javascript.Context cx = org.mozilla.javascript.Context.enter();
                                        try {
                                            cx.setOptimizationLevel(-1);
                                            cx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                                            ((Function) slotHandler).call(cx, api.scope, api.scope, new Object[]{event});
                                        } finally {
                                            org.mozilla.javascript.Context.exit();
                                        }
                                    } catch (Exception e) {
                                        if (listenerPlugin != null) {
                                            listenerPlugin.getLogger().severe("Error in inventory click handler: " + e.getMessage());
                                            if (listenerPlugin.getConfig().getBoolean("settings.debug-mode", false)) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                    return;
                                }
                                
                                // Execute global handler if no slot-specific handler or for shift-clicks
                                if (globalHandler instanceof Function && api.scope != null) {
                                    try {
                                        org.mozilla.javascript.Context cx = org.mozilla.javascript.Context.enter();
                                        try {
                                            cx.setOptimizationLevel(-1);
                                            cx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                                            ((Function) globalHandler).call(cx, api.scope, api.scope, new Object[]{event});
                                        } finally {
                                            org.mozilla.javascript.Context.exit();
                                        }
                                    } catch (Exception e) {
                                        if (listenerPlugin != null) {
                                            listenerPlugin.getLogger().severe("Error in inventory click handler: " + e.getMessage());
                                            if (listenerPlugin.getConfig().getBoolean("settings.debug-mode", false)) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                    return;
                                }
                            }
                        }
                        
                        // Fallback to regular handler
                        if (handler instanceof Function && api.scope != null) {
                            try {
                                org.mozilla.javascript.Context cx = org.mozilla.javascript.Context.enter();
                                try {
                                    cx.setOptimizationLevel(-1);
                                    cx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                                    ((Function) handler).call(cx, api.scope, api.scope, new Object[]{event});
                                } finally {
                                    org.mozilla.javascript.Context.exit();
                                }
                            } catch (Exception e) {
                                if (listenerPlugin != null) {
                                    listenerPlugin.getLogger().severe("Error in inventory click handler: " + e.getMessage());
                                    if (listenerPlugin.getConfig().getBoolean("settings.debug-mode", false)) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                        break; // Only process once
                    }
                    break;
                }
            }
        }
    }
    
    /**
     * Handle inventory close for all registered inventories
     * Improved version with proper cleanup to prevent memory leaks
     */
    public static void handleInventoryCloseForAll(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        
        // Check all JSAPI instances for handlers
        synchronized (globalEventHandlers) {
            for (Map.Entry<Class<? extends Event>, Map<JSAPI, List<EventHandlerInfo>>> entry : globalEventHandlers.entrySet()) {
                if (entry.getKey() == InventoryCloseEvent.class) {
                    Map<JSAPI, List<EventHandlerInfo>> handlersMap = entry.getValue();
                    for (JSAPI api : handlersMap.keySet()) {
                        Object handler = api.inventoryCloseHandlers.get(inv);
                        
                        // Check if it's a custom inventory with close handler in holder
                        if (inv.getHolder() instanceof CustomInventoryHolder) {
                            CustomInventoryHolder holder = (CustomInventoryHolder) inv.getHolder();
                            
                            // Mark inventory as closed
                            holder.markClosed();
                            
                            Object closeHandler = holder.getData("closeHandler");
                            if (closeHandler instanceof Function && api.scope != null) {
                                try {
                                    org.mozilla.javascript.Context cx = org.mozilla.javascript.Context.enter();
                                    try {
                                        cx.setOptimizationLevel(-1);
                                        cx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                                        ((Function) closeHandler).call(cx, api.scope, api.scope, new Object[]{event});
                                    } finally {
                                        org.mozilla.javascript.Context.exit();
                                    }
                                } catch (Exception e) {
                                    if (listenerPlugin != null) {
                                        listenerPlugin.getLogger().severe("Error in inventory close handler: " + e.getMessage());
                                        if (listenerPlugin.getConfig().getBoolean("settings.debug-mode", false)) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                            
                            // Cleanup: Remove handlers to prevent memory leaks
                            // Use a delayed task to ensure cleanup happens after event processing
                            Bukkit.getScheduler().runTaskLater(listenerPlugin, () -> {
                                synchronized (api.inventoryClickHandlers) {
                                    api.inventoryClickHandlers.remove(inv);
                                }
                                synchronized (api.inventoryCloseHandlers) {
                                    api.inventoryCloseHandlers.remove(inv);
                                }
                            }, 1L);
                            
                            return;
                        }
                        
                        // Fallback to regular handler
                        if (handler instanceof Function && api.scope != null) {
                            try {
                                org.mozilla.javascript.Context cx = org.mozilla.javascript.Context.enter();
                                try {
                                    cx.setOptimizationLevel(-1);
                                    cx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                                    ((Function) handler).call(cx, api.scope, api.scope, new Object[]{event});
                                } finally {
                                    org.mozilla.javascript.Context.exit();
                                }
                            } catch (Exception e) {
                                if (listenerPlugin != null) {
                                    listenerPlugin.getLogger().severe("Error in inventory close handler: " + e.getMessage());
                                    if (listenerPlugin.getConfig().getBoolean("settings.debug-mode", false)) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                        
                        // Cleanup regular handlers too
                        Bukkit.getScheduler().runTaskLater(listenerPlugin, () -> {
                            synchronized (api.inventoryClickHandlers) {
                                api.inventoryClickHandlers.remove(inv);
                            }
                            synchronized (api.inventoryCloseHandlers) {
                                api.inventoryCloseHandlers.remove(inv);
                            }
                        }, 1L);
                        
                        break; // Only process once
                    }
                    break;
                }
            }
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
        if (inventory == null || slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        inventory.setItem(slot, item);
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
    
    /**
     * Create a GUI builder pattern for easier inventory creation
     */
    public InventoryGUI createGUI(String title, int rows) {
        return new InventoryGUI(title, rows);
    }
    
    /**
     * GUI Builder class for creating custom inventories easily
     */
    public class InventoryGUI {
        private String title;
        private int rows;
        private int size;
        private Map<Integer, ItemStack> items = new HashMap<>();
        private Map<Integer, Object> clickHandlers = new HashMap<>();
        private Object globalClickHandler = null;
        private Object closeHandler = null;
        private ItemStack backgroundItem = null;
        private Inventory inventory = null;
        private boolean allowItemRemoval = false; // Default: items cannot be removed from GUI
        
        public InventoryGUI(String title, int rows) {
            this.title = title;
            this.rows = Math.max(1, Math.min(6, rows));
            this.size = this.rows * 9;
        }
        
        /**
         * Set an item at a specific slot
         */
        public InventoryGUI setItem(int slot, ItemStack item) {
            if (slot >= 0 && slot < size) {
                items.put(slot, item);
            }
            return this;
        }
        
        /**
         * Set an item at a specific slot with click handler
         */
        public InventoryGUI setItem(int slot, ItemStack item, Object clickHandler) {
            setItem(slot, item);
            if (clickHandler != null) {
                clickHandlers.put(slot, clickHandler);
            }
            return this;
        }
        
        /**
         * Set items in a range
         */
        public InventoryGUI setItems(int startSlot, int endSlot, ItemStack item) {
            for (int i = startSlot; i <= endSlot && i < size; i++) {
                items.put(i, item);
            }
            return this;
        }
        
        /**
         * Fill entire inventory with an item
         */
        public InventoryGUI fill(ItemStack item) {
            backgroundItem = item;
            for (int i = 0; i < size; i++) {
                if (!items.containsKey(i)) {
                    items.put(i, item);
                }
            }
            return this;
        }
        
        /**
         * Fill borders with an item
         */
        public InventoryGUI fillBorders(ItemStack item) {
            // Top row
            for (int i = 0; i < 9; i++) {
                items.put(i, item);
            }
            // Bottom row
            for (int i = size - 9; i < size; i++) {
                items.put(i, item);
            }
            // Left column
            for (int i = 0; i < size; i += 9) {
                items.put(i, item);
            }
            // Right column
            for (int i = 8; i < size; i += 9) {
                items.put(i, item);
            }
            return this;
        }
        
        /**
         * Set background item (used for empty slots)
         */
        public InventoryGUI setBackground(ItemStack item) {
            this.backgroundItem = item;
            return this;
        }
        
        /**
         * Set global click handler (called for all clicks)
         */
        public InventoryGUI onClick(Object handler) {
            this.globalClickHandler = handler;
            return this;
        }
        
        /**
         * Set close handler
         */
        public InventoryGUI onClose(Object handler) {
            this.closeHandler = handler;
            return this;
        }
        
        /**
         * Set whether items can be removed from the GUI
         * @param allow true to allow item removal, false to prevent it (default: false)
         */
        public InventoryGUI setAllowItemRemoval(boolean allow) {
            this.allowItemRemoval = allow;
            return this;
        }
        
        /**
         * Build and return the inventory
         */
        public Inventory build() {
            CustomInventoryHolder holder = new CustomInventoryHolder();
            inventory = Bukkit.createInventory(holder, size, ChatColor.translateAlternateColorCodes('&', title));
            holder.setInventory(inventory);
            
            // Create a copy of items map for storage in holder (for refresh functionality)
            Map<Integer, ItemStack> itemsCopy = new HashMap<>(items);
            
            // Set all items
            for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
                inventory.setItem(entry.getKey(), entry.getValue());
            }
            
            // Fill empty slots with background if set
            if (backgroundItem != null) {
                for (int i = 0; i < size; i++) {
                    if (inventory.getItem(i) == null) {
                        inventory.setItem(i, backgroundItem);
                        // Also add to itemsCopy for refresh
                        itemsCopy.put(i, backgroundItem.clone());
                    }
                }
            }
            
            // Store handlers and data in holder for later use
            holder.setData("clickHandlers", new HashMap<>(clickHandlers)); // Copy for thread safety
            holder.setData("globalClickHandler", globalClickHandler);
            holder.setData("closeHandler", closeHandler);
            holder.setData("allowItemRemoval", allowItemRemoval);
            holder.setData("items", itemsCopy); // Store items for refresh
            holder.setData("backgroundItem", backgroundItem != null ? backgroundItem.clone() : null);
            holder.setData("title", title);
            holder.setData("size", size);
            
            // Register click handlers (actual handling is done in handleInventoryClick)
            if (!clickHandlers.isEmpty() || globalClickHandler != null) {
                // Register with a dummy handler - the actual handling happens in handleInventoryClick
                // which checks the CustomInventoryHolder for slot-specific handlers
                registerInventoryClick(inventory, null); // null is fine, handleInventoryClick will check holder
            }
            
            // Register close handler
            if (closeHandler != null) {
                registerInventoryClose(inventory, closeHandler);
            }
            
            return inventory;
        }
        
        /**
         * Build and open for a player
         */
        public Inventory buildAndOpen(Player player) {
            Inventory inv = build();
            player.openInventory(inv);
            return inv;
        }
    }
    
    /**
     * Get the holder of an inventory (if it's a CustomInventoryHolder)
     */
    public CustomInventoryHolder getInventoryHolder(Inventory inventory) {
        InventoryHolder holder = inventory.getHolder();
        if (holder instanceof CustomInventoryHolder) {
            return (CustomInventoryHolder) holder;
        }
        return null;
    }
    
    /**
     * Check if inventory has a custom holder
     */
    public boolean isCustomInventory(Inventory inventory) {
        return inventory.getHolder() instanceof CustomInventoryHolder;
    }
    
    /**
     * Get click handler for a specific slot from a custom inventory
     */
    public Object getSlotClickHandler(Inventory inventory, int slot) {
        CustomInventoryHolder holder = getInventoryHolder(inventory);
        if (holder != null) {
            Map<Integer, Object> handlers = (Map<Integer, Object>) holder.getData("clickHandlers");
            if (handlers != null) {
                return handlers.get(slot);
            }
        }
        return null;
    }
    
    /**
     * Get global click handler from a custom inventory
     */
    public Object getGlobalClickHandler(Inventory inventory) {
        CustomInventoryHolder holder = getInventoryHolder(inventory);
        if (holder != null) {
            return holder.getData("globalClickHandler");
        }
        return null;
    }
    
    /**
     * Update an item in a custom inventory (thread-safe)
     */
    public boolean updateInventoryItem(Inventory inventory, int slot, ItemStack item) {
        if (inventory == null || slot < 0 || slot >= inventory.getSize()) {
            return false;
        }
        
        CustomInventoryHolder holder = getInventoryHolder(inventory);
        if (holder != null && holder.isClosed()) {
            return false;
        }
        
        inventory.setItem(slot, item);
        return true;
    }
    
    /**
     * Get an item from a custom inventory
     */
    public ItemStack getInventoryItem(Inventory inventory, int slot) {
        if (inventory == null || slot < 0 || slot >= inventory.getSize()) {
            return null;
        }
        return inventory.getItem(slot);
    }
    
    /**
     * Clear all items from a custom inventory
     */
    public void clearInventory(Inventory inventory) {
        if (inventory == null) {
            return;
        }
        
        CustomInventoryHolder holder = getInventoryHolder(inventory);
        if (holder != null && holder.isClosed()) {
            return;
        }
        
        inventory.clear();
    }
    
    /**
     * Refresh/rebuild a custom inventory (reapplies items from holder)
     */
    public boolean refreshInventory(Inventory inventory) {
        if (inventory == null) {
            return false;
        }
        
        CustomInventoryHolder holder = getInventoryHolder(inventory);
        if (holder == null || holder.isClosed()) {
            return false;
        }
        
        @SuppressWarnings("unchecked")
        Map<Integer, ItemStack> items = (Map<Integer, ItemStack>) holder.getData("items");
        ItemStack backgroundItem = (ItemStack) holder.getData("backgroundItem");
        
        if (items != null) {
            // Clear first
            inventory.clear();
            
            // Reapply items
            for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
                int slot = entry.getKey();
                if (slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, entry.getValue());
                }
            }
            
            // Reapply background
            if (backgroundItem != null) {
                for (int i = 0; i < inventory.getSize(); i++) {
                    if (inventory.getItem(i) == null) {
                        inventory.setItem(i, backgroundItem);
                    }
                }
            }
        }
        
        return true;
    }
    
    /**
     * Check if a slot in inventory is empty
     */
    public boolean isInventorySlotEmpty(Inventory inventory, int slot) {
        if (inventory == null || slot < 0 || slot >= inventory.getSize()) {
            return true;
        }
        ItemStack item = inventory.getItem(slot);
        return item == null || item.getType() == Material.AIR;
    }
    
    /**
     * Get first empty slot in inventory
     */
    public int getFirstEmptySlot(Inventory inventory) {
        if (inventory == null) {
            return -1;
        }
        return inventory.firstEmpty();
    }
    
    /**
     * Add item to inventory (finds first empty slot)
     */
    public boolean addItemToInventory(Inventory inventory, ItemStack item) {
        if (inventory == null || item == null) {
            return false;
        }
        
        CustomInventoryHolder holder = getInventoryHolder(inventory);
        if (holder != null && holder.isClosed()) {
            return false;
        }
        
        HashMap<Integer, ItemStack> leftover = inventory.addItem(item);
        return leftover.isEmpty();
    }
    
    /**
     * Remove item from inventory
     */
    public boolean removeItemFromInventory(Inventory inventory, ItemStack item) {
        if (inventory == null || item == null) {
            return false;
        }
        
        CustomInventoryHolder holder = getInventoryHolder(inventory);
        if (holder != null && holder.isClosed()) {
            return false;
        }
        
        return inventory.removeItem(item).isEmpty();
    }
    
    /**
     * Check if inventory contains item
     */
    public boolean inventoryContains(Inventory inventory, ItemStack item) {
        if (inventory == null || item == null) {
            return false;
        }
        return inventory.contains(item);
    }
    
    /**
     * Check if inventory contains at least amount of item
     */
    public boolean inventoryContainsAtLeast(Inventory inventory, ItemStack item, int amount) {
        if (inventory == null || item == null || amount <= 0) {
            return false;
        }
        return inventory.containsAtLeast(item, amount);
    }

    // ===== PLAYER DATA METHODS =====
    public String generateOfflineUUID(String playerName) {
        if (playerName == null) {
            return null;
        }
        // Generate a deterministic UUID based on player name
        // This ensures consistent UUIDs for offline players
        java.util.UUID uuid = java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes());
        return uuid.toString();
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
        if (entity == null || name == null) {
            return;
        }
        entity.setCustomName(ChatColor.translateAlternateColorCodes('&', name));
        entity.setCustomNameVisible(true);
    }

    public void setEntityGlowing(Entity entity, boolean glowing) {
        if (entity != null) {
            entity.setGlowing(glowing);
        }
    }

    public void setEntityGravity(Entity entity, boolean gravity) {
        if (entity != null) {
            entity.setGravity(gravity);
        }
    }

    public void setEntityInvulnerable(Entity entity, boolean invulnerable) {
        if (entity != null) {
            entity.setInvulnerable(invulnerable);
        }
    }
    
    public void setEntityAI(Entity entity, boolean ai) {
        if (entity != null && entity instanceof LivingEntity) {
            ((LivingEntity) entity).setAI(ai);
        }
    }
    
    public boolean hasEntityAI(Entity entity) {
        if (entity != null && entity instanceof LivingEntity) {
            return ((LivingEntity) entity).hasAI();
        }
        return false;
    }
    
    public void setEntitySilent(Entity entity, boolean silent) {
        if (entity != null) {
            entity.setSilent(silent);
        }
    }
    
    public boolean isEntitySilent(Entity entity) {
        return entity != null && entity.isSilent();
    }
    
    public void setEntityCollidable(Entity entity, boolean collidable) {
        if (entity != null && entity instanceof LivingEntity) {
            // Note: setCollidable is only available for LivingEntity in some versions
            try {
                java.lang.reflect.Method method = entity.getClass().getMethod("setCollidable", boolean.class);
                method.invoke(entity, collidable);
            } catch (Exception e) {
                // Method not available in this version
                debug("setCollidable not available for entity type: " + entity.getType());
            }
        }
    }
    
    public boolean isEntityCollidable(Entity entity) {
        if (entity == null || !(entity instanceof LivingEntity)) {
            return true; // Default
        }
        try {
            java.lang.reflect.Method method = entity.getClass().getMethod("isCollidable");
            return (Boolean) method.invoke(entity);
        } catch (Exception e) {
            return true; // Default if method not available
        }
    }
    
    public Location getEntityLocation(Entity entity) {
        return entity != null ? entity.getLocation() : null;
    }
    
    public void teleportEntity(Entity entity, Location location) {
        if (entity != null && location != null) {
            entity.teleport(location);
        }
    }
    
    public void teleportEntity(Entity entity, Entity target) {
        if (entity != null && target != null) {
            entity.teleport(target.getLocation());
        }
    }

    public void removeEntity(Entity entity) {
        if (entity != null) {
            entity.remove();
        }
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
    
    public boolean isValidItemStack(ItemStack item) {
        return item != null && item.getType() != Material.AIR;
    }
    
    public boolean isValidEntity(Entity entity) {
        return entity != null && !entity.isDead();
    }
    
    public boolean isValidInventory(Inventory inventory) {
        return inventory != null;
    }
    
    // ===== PLAYER DATA METHODS =====
    public void setPlayerMetadata(Player player, String key, Object value) {
        if (player != null && key != null) {
            player.setMetadata(key, new org.bukkit.metadata.FixedMetadataValue(plugin, value));
        }
    }
    
    public Object getPlayerMetadata(Player player, String key) {
        if (player == null || key == null) {
            return null;
        }
        List<org.bukkit.metadata.MetadataValue> values = player.getMetadata(key);
        return values.isEmpty() ? null : values.get(0).value();
    }
    
    public boolean hasPlayerMetadata(Player player, String key) {
        return player != null && key != null && !player.getMetadata(key).isEmpty();
    }
    
    public void removePlayerMetadata(Player player, String key) {
        if (player != null && key != null) {
            player.removeMetadata(key, plugin);
        }
    }
    
    // ===== BETTER ASYNC TASK MANAGEMENT =====
    public BukkitTask runTaskAsyncSafe(Object task, Object onError) {
        return plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (task instanceof Function && scope != null) {
                    executeFunction((Function) task);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error in async task: " + e.getMessage());
                if (onError instanceof Function && scope != null) {
                    try {
                        org.mozilla.javascript.Context cx = org.mozilla.javascript.Context.enter();
                        try {
                            cx.setOptimizationLevel(-1);
                            cx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                            ((Function) onError).call(cx, scope, scope, new Object[]{e.getMessage()});
                        } finally {
                            org.mozilla.javascript.Context.exit();
                        }
                    } catch (Exception errorHandlerException) {
                        plugin.getLogger().severe("Error in error handler: " + errorHandlerException.getMessage());
                    }
                } else {
                    e.printStackTrace();
                }
            }
        });
    }
    
    public BukkitTask runTaskSafe(Object task, Object onError) {
        return plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                if (task instanceof Function && scope != null) {
                    executeFunction((Function) task);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error in task: " + e.getMessage());
                if (onError instanceof Function && scope != null) {
                    try {
                        org.mozilla.javascript.Context cx = org.mozilla.javascript.Context.enter();
                        try {
                            cx.setOptimizationLevel(-1);
                            cx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                            ((Function) onError).call(cx, scope, scope, new Object[]{e.getMessage()});
                        } finally {
                            org.mozilla.javascript.Context.exit();
                        }
                    } catch (Exception errorHandlerException) {
                        plugin.getLogger().severe("Error in error handler: " + errorHandlerException.getMessage());
                    }
                } else {
                    e.printStackTrace();
                }
            }
        });
    }
}
