package de.flori.mCJS.api;

import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.java.JavaPlugin;
import org.mozilla.javascript.Function;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API module for event registration and handling
 * Uses EventHandlerContext instead of JSAPI instances
 */
public class EventAPI extends BaseAPI {
    private final APIHelper apiHelper;
    
    // Event handler storage - universal listener handles all events
    // Use ConcurrentHashMap for thread-safety during plugin reloads
    private static final Map<Class<? extends Event>, Map<EventHandlerContext, List<EventHandlerInfo>>> globalEventHandlers = new ConcurrentHashMap<>();
    private static JavaPlugin listenerPlugin;
    
    // Reference to InventoryAPI for inventory event handling
    private static InventoryAPI inventoryAPI;

    private static class EventHandlerInfo {
        final Object handler;
        final EventPriority priority;
        
        EventHandlerInfo(Object handler, EventPriority priority) {
            this.handler = handler;
            this.priority = priority;
        }
    }

    // Map to track which event classes have been registered
    // Use ConcurrentHashMap-backed set for thread-safety
    private static final Set<Class<? extends Event>> registeredEventClasses = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    public EventAPI(JavaPlugin plugin, APIHelper apiHelper) {
        super(plugin);
        this.apiHelper = apiHelper;
        
        // Initialize listener plugin reference
        synchronized (globalEventHandlers) {
            if (listenerPlugin == null) {
                listenerPlugin = plugin;
            }
        }
    }
    
    /**
     * Set the InventoryAPI reference for inventory event handling
     */
    public void setInventoryAPI(InventoryAPI inventoryAPI) {
        EventAPI.inventoryAPI = inventoryAPI;
    }
    
    // ===== EVENT REGISTRATION METHODS =====
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
            // Special handling for inventory events - these are handled by InventoryAPI
            if (event instanceof InventoryClickEvent && inventoryAPI != null) {
                inventoryAPI.handleInventoryClick((InventoryClickEvent) event);
                return;
            }
            
            if (event instanceof InventoryCloseEvent && inventoryAPI != null) {
                inventoryAPI.handleInventoryClose((InventoryCloseEvent) event);
                return;
            }
            
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
        
        // Debug logging (only if any EventAPI instance has debug mode enabled)
        if (listenerPlugin != null && listenerPlugin.getConfig().getBoolean("settings.debug-mode", false)) {
            listenerPlugin.getLogger().info("[DEBUG] Dispatching event: " + actualEventClass.getSimpleName());
        }
        
        // Check all registered event classes to see if they match
        synchronized (globalEventHandlers) {
            for (Map.Entry<Class<? extends Event>, Map<EventHandlerContext, List<EventHandlerInfo>>> entry : globalEventHandlers.entrySet()) {
                Class<? extends Event> registeredClass = entry.getKey();
                
                // Check if the actual event class matches or is a subclass of the registered class
                if (registeredClass.isAssignableFrom(actualEventClass)) {
                    Map<EventHandlerContext, List<EventHandlerInfo>> handlersForClass = entry.getValue();
                    
                    for (Map.Entry<EventHandlerContext, List<EventHandlerInfo>> contextEntry : handlersForClass.entrySet()) {
                        EventHandlerContext context = contextEntry.getKey();
                        List<EventHandlerInfo> handlers = contextEntry.getValue();
                        
                        // Execute all handlers for this event (priority is stored in EventHandlerInfo)
                        for (EventHandlerInfo info : handlers) {
                            if (info.handler instanceof Function && context.scope != null) {
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
                                        executeWithTimeout(() -> {
                                            org.mozilla.javascript.Context rhinoContext = org.mozilla.javascript.Context.enter();
                                            try {
                                                int optimizationLevel = listenerPlugin != null ? 
                                                    listenerPlugin.getConfig().getInt("performance.optimization-level", -1) : -1;
                                                rhinoContext.setOptimizationLevel(optimizationLevel);
                                                rhinoContext.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                                                Function func = (Function) info.handler;
                                                func.call(rhinoContext, context.scope, context.scope, new Object[]{event});
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
                                            func.call(rhinoContext, context.scope, context.scope, new Object[]{event});
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
    
    /**
     * Execute a runnable with timeout protection
     */
    private static void executeWithTimeout(Runnable task, long timeoutMs, String context) {
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        java.util.concurrent.Future<?> future = executor.submit(task);
        try {
            future.get(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            if (listenerPlugin != null) {
                listenerPlugin.getLogger().warning("Event handler execution timed out after " + timeoutMs + "ms for " + context);
            }
        } catch (Exception e) {
            if (listenerPlugin != null) {
                listenerPlugin.getLogger().severe("Error executing timed task for " + context + ": " + e.getMessage());
                e.printStackTrace();
            }
        } finally {
            executor.shutdown();
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
        
        // Register this specific event class with PaperMC
        registerEventClass(eventClass, plugin);
        
        // Create EventHandlerContext for this registration
        EventHandlerContext context = new EventHandlerContext(apiHelper.getScope(), plugin);
        
        // Store handler
        synchronized (globalEventHandlers) {
            globalEventHandlers.computeIfAbsent(eventClass, k -> new HashMap<>())
                              .computeIfAbsent(context, k -> new ArrayList<>())
                              .add(new EventHandlerInfo(handler, priority));
            debug("Stored event handler for " + eventClass.getName() + " (total handlers: " + 
                  globalEventHandlers.get(eventClass).get(context).size() + ")");
        }
    }
}
