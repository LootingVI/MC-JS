package de.flori.mCJS.api;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StateAPI extends BaseAPI implements Listener {
    private final APIHelper helper;
    private final SchedulerAPI scheduler;
    private final Map<String, Machine> machines = new LinkedHashMap<>();
    private final Map<String, Instance> instances = new LinkedHashMap<>();
    private boolean listening;
    private boolean closed;
    private int depth;

    public StateAPI(JavaPlugin plugin, APIHelper helper, SchedulerAPI scheduler) {
        super(plugin); this.helper = helper; this.scheduler = scheduler;
    }

    public final class Machine {
        private final String name;
        private final String initial;
        private final Map<String, State> states = new LinkedHashMap<>();
        private final List<Transition> transitions = new ArrayList<>();
        private Machine(String name, String initial) { this.name = name; this.initial = initial; }
        public void state(String name, Object enter, Object exit) {
            valid(name);
            if (states.putIfAbsent(name, new State(enter, exit)) != null) throw new IllegalArgumentException("Duplicate state: " + name);
        }
        public void transition(String from, String event, String to, Object guard) {
            valid(from); valid(event); valid(to);
            transitions.add(new Transition(from, event, to, guard));
        }
        private void validate() {
            if (!states.containsKey(initial)) throw new IllegalStateException("Initial state does not exist: " + initial);
            for (var transition : transitions) if (!states.containsKey(transition.from) || !states.containsKey(transition.to)) throw new IllegalStateException("Transition refers to a missing state");
        }
    }

    private record State(Object enter, Object exit) {}
    private record Transition(String from, String event, String to, Object guard) {}
    private final class Instance {
        final Machine machine;
        final Player player;
        final List<BukkitTask> tasks = new ArrayList<>();
        String state;
        long generation;
        boolean transitioning;
        Instance(Machine machine, Player player) { this.machine = machine; this.player = player; this.state = machine.initial; }
        void cancelTimers() { tasks.forEach(scheduler::cancelTask); tasks.clear(); generation++; }
    }

    public Machine create(String name, String initial) {
        active(); valid(name); valid(initial);
        if (machines.containsKey(name)) throw new IllegalArgumentException("Duplicate state machine: " + name);
        if (!listening) { plugin.getServer().getPluginManager().registerEvents(this, plugin); listening = true; }
        var machine = new Machine(name, initial);
        machines.put(name, machine);
        return machine;
    }

    public void start(String name, Player player, Object data) {
        active();
        var machine = machines.get(name);
        if (machine == null) throw new IllegalArgumentException("Unknown state machine: " + name);
        machine.validate();
        String key = key(name, player);
        if (instances.containsKey(key)) throw new IllegalStateException("State machine is already running. Stop it before restarting.");
        var instance = new Instance(machine, player);
        instances.put(key, instance);
        try { invoke(machine.states.get(instance.state).enter, player, data); }
        catch (RuntimeException error) { if (instances.remove(key, instance)) instance.cancelTimers(); throw error; }
    }

    public boolean send(String name, Player player, String event, Object data) {
        active();
        var instance = instances.get(key(name, player));
        if (instance == null) return false;
        if (instance.transitioning) throw new IllegalStateException("Do not send transitions from an exit action or guard");
        if (++depth > 64) { depth--; throw new IllegalStateException("Too many immediate state transitions"); }
        try {
            for (var transition : instance.machine.transitions) {
                if (!transition.from.equals(instance.state) || !transition.event.equals(event)) continue;
                instance.transitioning = true;
                try {
                    if (transition.guard instanceof Function guard && !Context.toBoolean(helper.callFunction(guard, player, data))) continue;
                    invoke(instance.machine.states.get(instance.state).exit, player, data);
                    instance.cancelTimers();
                    instance.state = transition.to;
                } finally { instance.transitioning = false; }
                invoke(instance.machine.states.get(instance.state).enter, player, data);
                return true;
            }
            return false;
        } finally { depth--; }
    }

    public String current(String name, Player player) {
        active(); var instance = instances.get(key(name, player)); return instance == null ? "" : instance.state;
    }

    public void after(String name, Player player, double seconds, String event, Object data) {
        active();
        if (!Double.isFinite(seconds) || seconds < 0 || seconds > 86400) throw new IllegalArgumentException("State timers require 0–86400 seconds");
        var instance = instances.get(key(name, player));
        if (instance == null) throw new IllegalStateException("Start the state machine first");
        long generation = instance.generation;
        var handle = new BukkitTask[1];
        handle[0] = scheduler.runOwnedTaskLater(Math.max(1, Math.round(seconds * 20)), () -> {
            instance.tasks.remove(handle[0]);
            if (!closed && instances.get(key(name, player)) == instance && instance.generation == generation
                    && (player == null || player.isOnline())) send(name, player, event, data);
        });
        instance.tasks.add(handle[0]);
    }

    public void stop(String name, Player player, Object data) {
        active();
        String key = key(name, player);
        var instance = instances.get(key);
        if (instance == null) return;
        if (instance.transitioning) throw new IllegalStateException("Do not stop a machine from an exit action or guard");
        instances.remove(key);
        instance.cancelTimers();
        invoke(instance.machine.states.get(instance.state).exit, player, data);
    }

    private void invoke(Object callback, Player player, Object data) { if (callback instanceof Function function) helper.executeFunction(function, player, data); }
    private String key(String name, Player player) { valid(name); return name + ":" + (player == null ? "global" : player.getUniqueId()); }
    private void valid(String name) { if (name == null || !name.matches("[A-Za-z][A-Za-z0-9_-]{0,47}")) throw new IllegalArgumentException("Names require 1–48 letters, numbers, _ or - and must start with a letter"); }
    private void active() {
        if (closed) throw new IllegalStateException("This plugin has stopped");
        if (!plugin.getServer().isPrimaryThread()) throw new IllegalStateException("Use state machines on the server thread");
    }
    @EventHandler public void onQuit(PlayerQuitEvent event) {
        instances.values().removeIf(instance -> { if (instance.player != null && instance.player.getUniqueId().equals(event.getPlayer().getUniqueId())) { instance.cancelTimers(); return true; } return false; });
    }
    public void close() {
        closed = true;
        instances.values().forEach(Instance::cancelTimers);
        instances.clear(); machines.clear();
        if (listening) HandlerList.unregisterAll(this);
    }
}
