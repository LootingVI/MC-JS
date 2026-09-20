package de.flori.mCJS.api;

import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.java.JavaPlugin;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EventAPI extends BaseAPI {
    private final APIHelper apiHelper;

    private static final Map<Class<? extends Event>, Map<EventHandlerContext, List<EventHandlerInfo>>> globalEventHandlers = new ConcurrentHashMap<>();
    private static JavaPlugin listenerPlugin;

    private static InventoryAPI inventoryAPI;

    private static class EventHandlerInfo {
        final Object handler;
        final EventPriority priority;

        EventHandlerInfo(Object handler, EventPriority priority) {
            this.handler = handler;
            this.priority = priority;
        }
    }

    private static final Set<Class<? extends Event>> registeredEventClasses = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public EventAPI(JavaPlugin plugin, APIHelper apiHelper) {
        super(plugin);
        this.apiHelper = apiHelper;

        synchronized (globalEventHandlers) {
            if (listenerPlugin == null) {
                listenerPlugin = plugin;
            }
        }
    }

    public void setInventoryAPI(InventoryAPI inventoryAPI) {
        EventAPI.inventoryAPI = inventoryAPI;
    }

    public void unregisterHandlers() {
        Scriptable scope = apiHelper.getScope();
        synchronized (globalEventHandlers) {
            globalEventHandlers.entrySet().removeIf(entry -> {
                entry.getValue().entrySet().removeIf(context ->
                    context.getKey().plugin == plugin && context.getKey().scope == scope);
                return entry.getValue().isEmpty();
            });
        }
    }

    public static void reset(JavaPlugin plugin) {
        HandlerList.unregisterAll(plugin);
        synchronized (globalEventHandlers) {
            globalEventHandlers.clear();
            registeredEventClasses.clear();
            listenerPlugin = null;
            inventoryAPI = null;
        }
    }

    public void registerEvent(String eventClassName, Object handler) {
        debug("Registering event: " + eventClassName);
        try {
            Class<?> eventClass = null;

            if (eventClassName.contains(".") && !eventClassName.startsWith("org.bukkit")) {

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

                        }
                    }
                }
            }

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

                        if (foundClass != Event.class && Event.class.isAssignableFrom(foundClass)) {
                            eventClass = foundClass;
                            break;
                        }
                    } catch (ClassNotFoundException e) {

                    }
                }
            }

            if (eventClass == null) {
                plugin.getLogger().severe("Event class not found: " + eventClassName);
                return;
            }

            if (eventClass == Event.class) {
                plugin.getLogger().warning("Cannot register base Event class. Use a specific event type like 'player.PlayerJoinEvent'.");
                return;
            }

            if (Event.class.isAssignableFrom(eventClass)) {
                @SuppressWarnings("unchecked")
                Class<? extends Event> clazz = (Class<? extends Event>) eventClass;

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

    private static void registerEventClass(Class<? extends Event> eventClass, JavaPlugin plugin) {
        synchronized (registeredEventClasses) {
            if (registeredEventClasses.contains(eventClass)) {
                return;
            }

            plugin.getLogger().info("Attempting to register event class: " + eventClass.getName() + " (is Event.class: " + (eventClass == Event.class) + ")");

            try {

                java.lang.reflect.Method registerEventMethod = null;
                try {

                    registerEventMethod = plugin.getServer().getPluginManager().getClass()
                        .getMethod("registerEvent", Class.class, Listener.class, EventPriority.class,
                                  java.util.function.Consumer.class, Plugin.class);
                } catch (NoSuchMethodException e) {

                }

                if (registerEventMethod != null) {

                    Listener dummyListener = new Listener() {};
                    java.util.function.Consumer<Event> consumer = (event) -> {

                        executeEventWrapper(dummyListener, event);
                    };

                    registerEventMethod.invoke(plugin.getServer().getPluginManager(),
                        eventClass, dummyListener, EventPriority.NORMAL, consumer, plugin);
                } else {

                    registerEventViaHandlerList(eventClass, plugin);
                }

                registeredEventClasses.add(eventClass);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to register event class " + eventClass.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static <T extends Event> void registerEventViaHandlerList(Class<T> eventClass, JavaPlugin plugin) {
        try {

            java.lang.reflect.Method getHandlerListMethod = eventClass.getMethod("getHandlerList");
            org.bukkit.event.HandlerList handlerList = (org.bukkit.event.HandlerList) getHandlerListMethod.invoke(null);

            Listener dummyListener = new Listener() {};

            org.bukkit.plugin.EventExecutor executor = new org.bukkit.plugin.EventExecutor() {
                @Override
                public void execute(Listener listener, Event event) throws EventException {
                    executeEventWrapper(listener, event);
                }
            };

            try {
                java.lang.reflect.Constructor<RegisteredListener> constructor =
                    RegisteredListener.class.getConstructor(Listener.class,
                        org.bukkit.plugin.EventExecutor.class, EventPriority.class, Plugin.class, boolean.class);

                RegisteredListener registeredListener = constructor.newInstance(
                    dummyListener, executor, EventPriority.NORMAL, plugin, false
                );
                handlerList.register(registeredListener);

                plugin.getLogger().info("Successfully registered event class " + eventClass.getName() + " via HandlerList");
            } catch (NoSuchMethodException e) {

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

    private static final Set<Event> eventsBeingDispatched = Collections.synchronizedSet(Collections.newSetFromMap(new java.util.IdentityHashMap<>()));

    @SuppressWarnings("unused")
    private static void executeEventWrapper(Listener listener, Event event) {

        synchronized (eventsBeingDispatched) {
            if (eventsBeingDispatched.contains(event)) {

                if (listenerPlugin != null && listenerPlugin.getConfig().getBoolean("settings.debug-mode", false)) {
                    listenerPlugin.getLogger().info("[DEBUG] Skipping duplicate event dispatch for " + event.getClass().getSimpleName());
                }
                return;
            }
            eventsBeingDispatched.add(event);
        }

        try {

            if (event instanceof InventoryClickEvent && inventoryAPI != null) {
                inventoryAPI.handleInventoryClick((InventoryClickEvent) event);
                return;
            }

            if (event instanceof InventoryCloseEvent && inventoryAPI != null) {
                inventoryAPI.handleInventoryClose((InventoryCloseEvent) event);
                return;
            }

            synchronized (globalEventHandlers) {
                for (Class<? extends Event> eventClass : globalEventHandlers.keySet()) {
                    if (eventClass.isInstance(event)) {

                        dispatchEventForAllPriorities(event);
                        break;
                    }
                }
            }
        } finally {

            synchronized (eventsBeingDispatched) {
                eventsBeingDispatched.remove(event);
            }
        }
    }

    private static void dispatchEventForAllPriorities(Event event) {
        Class<? extends Event> actualEventClass = event.getClass();

        if (listenerPlugin != null && listenerPlugin.getConfig().getBoolean("settings.debug-mode", false)) {
            listenerPlugin.getLogger().info("[DEBUG] Dispatching event: " + actualEventClass.getSimpleName());
        }

        synchronized (globalEventHandlers) {
            for (Map.Entry<Class<? extends Event>, Map<EventHandlerContext, List<EventHandlerInfo>>> entry : globalEventHandlers.entrySet()) {
                Class<? extends Event> registeredClass = entry.getKey();

                if (registeredClass.isAssignableFrom(actualEventClass)) {
                    Map<EventHandlerContext, List<EventHandlerInfo>> handlersForClass = entry.getValue();

                    for (Map.Entry<EventHandlerContext, List<EventHandlerInfo>> contextEntry : handlersForClass.entrySet()) {
                        EventHandlerContext context = contextEntry.getKey();
                        List<EventHandlerInfo> handlers = contextEntry.getValue();

                        for (EventHandlerInfo info : handlers) {
                            if (info.handler instanceof Function && context.scope != null) {
                                try {
                                    if (listenerPlugin != null && listenerPlugin.getConfig().getBoolean("settings.debug-mode", false)) {
                                        listenerPlugin.getLogger().info("[DEBUG] Executing event handler for " + actualEventClass.getSimpleName() +
                                                                      " (priority: " + info.priority + ")");
                                    }

                                    long maxExecutionTime = listenerPlugin != null ?
                                        listenerPlugin.getConfig().getLong("performance.max-execution-time", 5000) : 5000;

                                    long start = System.nanoTime();
                                    org.mozilla.javascript.Context rhinoContext = org.mozilla.javascript.Context.enter();
                                    try {
                                        int optimizationLevel = listenerPlugin != null
                                            ? listenerPlugin.getConfig().getInt("performance.optimization-level", -1) : -1;
                                        rhinoContext.setOptimizationLevel(optimizationLevel);
                                        rhinoContext.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                                        ((Function) info.handler).call(rhinoContext, context.scope, context.scope, new Object[]{event});
                                    } finally {
                                        org.mozilla.javascript.Context.exit();
                                    }
                                    long elapsedMs = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                                    if (maxExecutionTime > 0 && elapsedMs > maxExecutionTime && listenerPlugin != null) {
                                        listenerPlugin.getLogger().warning("Event handler exceeded " + maxExecutionTime + "ms for "
                                            + actualEventClass.getSimpleName() + " (took " + elapsedMs + "ms)");
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

        if (eventClass == null || eventClass == Event.class) {
            plugin.getLogger().warning("Cannot register handler for base Event class. Use a specific event type.");
            debug("Attempted to register base Event class, rejected");
            return;
        }

        try {
            java.lang.reflect.Method handlerListMethod = eventClass.getMethod("getHandlerList");

            if (!java.lang.reflect.Modifier.isStatic(handlerListMethod.getModifiers())) {
                plugin.getLogger().warning("Event class '" + eventClass.getName() + "' getHandlerList() is not static. Cannot register.");
                return;
            }
        } catch (NoSuchMethodException e) {
            plugin.getLogger().warning("Event class '" + eventClass.getName() + "' does not have getHandlerList() method. Cannot register.");
            return;
        }

        registerEventClass(eventClass, plugin);

        EventHandlerContext context = new EventHandlerContext(apiHelper.getScope(), plugin);

        synchronized (globalEventHandlers) {
            globalEventHandlers.computeIfAbsent(eventClass, k -> new HashMap<>())
                              .computeIfAbsent(context, k -> new ArrayList<>())
                              .add(new EventHandlerInfo(handler, priority));
            debug("Stored event handler for " + eventClass.getName() + " (total handlers: " +
                  globalEventHandlers.get(eventClass).get(context).size() + ")");
        }
    }
}
