package de.flori.mCJS.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.mozilla.javascript.Function;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public final class SystemsAPI extends BaseAPI implements Listener {
    private final FileAPI files;
    private final APIHelper helper;
    private final SchedulerAPI scheduler;
    private final Map<Inventory, Menu> menus = new IdentityHashMap<>();
    private boolean listening;
    private boolean menuListening;
    private volatile boolean closed;

    public SystemsAPI(JavaPlugin plugin, FileAPI files, APIHelper helper, SchedulerAPI scheduler) {
        super(plugin);
        this.files = files;
        this.helper = helper;
        this.scheduler = scheduler;
    }

    private String path(String scope, String key) {
        if (scope == null || key == null || scope.length() > 256 || key.isBlank() || key.length() > 256) {
            throw new IllegalArgumentException("Data scope and key must be valid strings of at most 256 characters");
        }
        var encoder = Base64.getUrlEncoder().withoutPadding();
        return "systems." + encoder.encodeToString(scope.getBytes(StandardCharsets.UTF_8))
                + "." + encoder.encodeToString(key.getBytes(StandardCharsets.UTF_8));
    }

    public String readData(String name, String scope, String key, String fallback) {
        synchronized (plugin) {
            Object value = files.getPluginConfigValue(name, path(scope, key), fallback);
            return value == null ? "null" : value.toString();
        }
    }

    public void writeData(String name, String scope, String key, String json) {
        if (json == null || json.getBytes(StandardCharsets.UTF_8).length > 131072) {
            throw new IllegalArgumentException("A saved value must not exceed 128 KiB");
        }
        String canonical = JsonParser.parseString(json).toString();
        synchronized (plugin) {
            files.setPluginConfigValue(name, path(scope, key), canonical);
        }
    }

    public void deleteData(String name, String scope, String key) {
        synchronized (plugin) {
            files.setPluginConfigValue(name, path(scope, key), null);
        }
    }

    public double changeNumber(String name, String scope, String key, double delta) {
        synchronized (plugin) {
            double result = JsonParser.parseString(readData(name, scope, key, "0")).getAsDouble() + delta;
            if (!Double.isFinite(result)) throw new IllegalArgumentException("Stored numbers must be finite");
            writeData(name, scope, key, Double.toString(result));
            return result;
        }
    }

    public boolean claimCooldown(String name, String scope, String key, double seconds) {
        synchronized (plugin) {
            if (!Double.isFinite(seconds) || seconds < 0 || seconds > 31_536_000) {
                throw new IllegalArgumentException("Cooldown must be between 0 seconds and one year");
            }
            if (cooldownRemaining(name, scope, key) > 0) return false;
            writeData(name, "cooldowns:" + scope, key, Long.toString(System.currentTimeMillis() + (long) (seconds * 1000)));
            return true;
        }
    }

    public double cooldownRemaining(String name, String scope, String key) {
        long until = JsonParser.parseString(readData(name, "cooldowns:" + scope, key, "0")).getAsLong();
        return Math.max(0, Math.ceil((until - System.currentTimeMillis()) / 1000.0));
    }

    public String grantKit(String name, Player player, String key, double seconds, Object items) {
        if (player == null || !Double.isFinite(seconds) || seconds < 0 || seconds > 31_536_000) {
            throw new IllegalArgumentException("Invalid kit or cooldown");
        }
        if (!(items instanceof java.util.List<?> list) || list.isEmpty() || list.size() > 54) {
            throw new IllegalArgumentException("A kit requires a list of 1–54 items");
        }
        java.util.List<ItemStack> rewards = new java.util.ArrayList<>();
        for (Object value : list) {
            if (value instanceof org.mozilla.javascript.Wrapper wrapper) value = wrapper.unwrap();
            if (!(value instanceof ItemStack item) || item.getType().isAir() || item.getAmount() < 1) {
                throw new IllegalArgumentException("Invalid kit item");
            }
            rewards.add(item.clone());
        }
        synchronized (plugin) {
            String scope = "player:" + player.getUniqueId();
            if (cooldownRemaining(name, scope, key) > 0) return "cooldown";
            ItemStack[] before = player.getInventory().getStorageContents();
            for (int i = 0; i < before.length; i++) if (before[i] != null) before[i] = before[i].clone();
            try {
                if (!player.getInventory().addItem(rewards.toArray(ItemStack[]::new)).isEmpty()) {
                    player.getInventory().setStorageContents(before);
                    return "space";
                }
                writeData(name, "cooldowns:" + scope, key, Long.toString(System.currentTimeMillis() + (long) (seconds * 1000)));
                return "ok";
            } catch (RuntimeException error) {
                player.getInventory().setStorageContents(before);
                throw error;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void registerEvent(String eventName, boolean observe, Object callback) {
        if (closed || !(callback instanceof Function function)) throw new IllegalArgumentException("An active callback is required");
        try {
            Class<?> type = Class.forName("org.bukkit.event." + eventName, false, plugin.getClass().getClassLoader());
            if (!Event.class.isAssignableFrom(type)) throw new IllegalArgumentException("Not a Bukkit event");
            plugin.getServer().getPluginManager().registerEvent((Class<? extends Event>) type, this,
                    observe ? org.bukkit.event.EventPriority.MONITOR : org.bukkit.event.EventPriority.NORMAL,
                    (listener, event) -> {
                        if (!closed && type.isInstance(event)) helper.executeFunction(function, event);
                    }, plugin, observe);
            listening = true;
        } catch (ClassNotFoundException error) {
            throw new IllegalArgumentException("Unknown event: " + eventName, error);
        }
    }

    public String purchase(String name, Player player, String currency, double cost, ItemStack item) {
        if (player == null || item == null || item.getType().isAir() || item.getAmount() < 1
                || !Double.isFinite(cost) || cost < 0) throw new IllegalArgumentException("Invalid purchase");
        synchronized (plugin) {
            String scope = "player:" + player.getUniqueId();
            double balance = JsonParser.parseString(readData(name, scope, currency, "0")).getAsDouble();
            if (!Double.isFinite(balance) || balance < cost) return "funds";
            ItemStack[] before = player.getInventory().getStorageContents();
            int capacity = 0;
            for (int i = 0; i < before.length; i++) {
                ItemStack slot = before[i];
                if (slot == null || slot.getType().isAir()) capacity += Math.min(item.getMaxStackSize(), player.getInventory().getMaxStackSize());
                else {
                    before[i] = slot.clone();
                    if (slot.isSimilar(item)) capacity += Math.max(0, Math.min(slot.getMaxStackSize(), player.getInventory().getMaxStackSize()) - slot.getAmount());
                }
            }
            if (capacity < item.getAmount()) return "space";
            writeData(name, scope, currency, Double.toString(balance - cost));
            try {
                if (!player.getInventory().addItem(item.clone()).isEmpty()) {
                    player.getInventory().setStorageContents(before);
                    writeData(name, scope, currency, Double.toString(balance));
                    return "space";
                }
            } catch (RuntimeException error) {
                player.getInventory().setStorageContents(before);
                writeData(name, scope, currency, Double.toString(balance));
                throw error;
            }
            return "ok";
        }
    }

    public Player eventPlayer(Event event) {
        if (event instanceof PlayerEvent playerEvent) return playerEvent.getPlayer();
        if (event instanceof InventoryEvent inventoryEvent && inventoryEvent.getView().getPlayer() instanceof Player player) return player;
        if (event instanceof EntityEvent entityEvent && entityEvent.getEntity() instanceof Player player) return player;
        try {
            Object player = event.getClass().getMethod("getPlayer").invoke(event);
            return player instanceof Player found ? found : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    public String locationToJson(Location location) {
        if (location == null || location.getWorld() == null) return "null";
        JsonObject result = new JsonObject();
        result.addProperty("world", location.getWorld().getName());
        result.addProperty("x", location.getX());
        result.addProperty("y", location.getY());
        result.addProperty("z", location.getZ());
        result.addProperty("yaw", location.getYaw());
        result.addProperty("pitch", location.getPitch());
        return result.toString();
    }

    public Location locationFromJson(String json) {
        if (json == null || json.equals("null")) return null;
        var data = JsonParser.parseString(json).getAsJsonObject();
        var world = plugin.getServer().getWorld(data.get("world").getAsString());
        if (world == null) return null;
        return new Location(world, data.get("x").getAsDouble(), data.get("y").getAsDouble(), data.get("z").getAsDouble(),
                data.has("yaw") ? data.get("yaw").getAsFloat() : 0, data.has("pitch") ? data.get("pitch").getAsFloat() : 0);
    }

    public Menu createMenu(String title, int rows) {
        if (closed) throw new IllegalStateException("This plugin has stopped");
        if (rows < 1 || rows > 6) throw new IllegalArgumentException("Menus require 1–6 rows");
        if (!menuListening) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            listening = true;
            menuListening = true;
        }
        return new Menu(title, rows);
    }

    public final class Menu {
        private final Inventory inventory;
        private final Map<Integer, Object> buttons = new HashMap<>();
        private Object closeHandler;

        private Menu(String title, int rows) {
            inventory = plugin.getServer().createInventory(null, rows * 9, legacyToComponentWithAmpersand(title));
        }

        public void button(int slot, ItemStack item, Object callback) {
            if (slot < 0 || slot >= inventory.getSize()) throw new IllegalArgumentException("Menu slot is out of range");
            inventory.setItem(slot, item);
            buttons.put(slot, callback);
        }

        public void fill(ItemStack item) {
            for (int slot = 0; slot < inventory.getSize(); slot++) {
                if (inventory.getItem(slot) == null) inventory.setItem(slot, item);
            }
        }

        public void onClose(Object callback) { closeHandler = callback; }

        public void open(Player player) {
            if (closed || player == null || !player.isOnline()) return;
            menus.put(inventory, this);
            player.openInventory(inventory);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Menu menu = menus.get(event.getView().getTopInventory());
        if (menu == null) return;
        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= menu.inventory.getSize()) return;
        Object callback = menu.buttons.get(event.getRawSlot());
        if (callback instanceof Function function) {
            scheduler.runOwnedTask(() -> {
                if (!closed && menus.get(menu.inventory) == menu && event.getWhoClicked() instanceof Player player && player.isOnline()) {
                    helper.executeFunction(function, player, event);
                }
            });
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (menus.containsKey(event.getView().getTopInventory())) event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Menu menu = menus.remove(event.getInventory());
        if (menu != null && menu.closeHandler instanceof Function function) {
            scheduler.runOwnedTask(() -> { if (!closed) helper.executeFunction(function, event.getPlayer(), event); });
        }
    }

    public void close() {
        closed = true;
        if (listening) HandlerList.unregisterAll(this);
        for (Inventory inventory : java.util.List.copyOf(menus.keySet())) {
            for (var viewer : java.util.List.copyOf(inventory.getViewers())) viewer.closeInventory();
        }
        menus.clear();
    }
}
