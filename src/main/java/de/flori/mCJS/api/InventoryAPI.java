package de.flori.mCJS.api;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.mozilla.javascript.Function;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InventoryAPI extends BaseAPI {
    private final APIHelper apiHelper;

    private final Map<Inventory, Object> inventoryClickHandlers = Collections.synchronizedMap(new HashMap<>());
    private final Map<Inventory, Object> inventoryCloseHandlers = Collections.synchronizedMap(new HashMap<>());
    private static boolean inventoryClickEventRegistered = false;
    private static boolean inventoryCloseEventRegistered = false;

    public InventoryAPI(JavaPlugin plugin, APIHelper apiHelper) {
        super(plugin);
        this.apiHelper = apiHelper;
    }

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

        public synchronized void setData(String key, Object value) {
            if (!closed) {
                data.put(key, value);
            }
        }

        public synchronized Object getData(String key) {
            return data.get(key);
        }

        public synchronized Map<String, Object> getAllData() {
            return new HashMap<>(data);
        }

        public synchronized void removeData(String key) {
            data.remove(key);
        }

        public synchronized void clearData() {
            data.clear();
        }

        public synchronized void markClosed() {
            closed = true;
        }

        public synchronized boolean isClosed() {
            return closed;
        }
    }

    public class InventoryGUI {
        private String title;
        private int rows;
        private int size;

        private Map<Integer, ItemStack> items = new ConcurrentHashMap<>();
        private Map<Integer, Object> clickHandlers = new ConcurrentHashMap<>();
        private volatile Object globalClickHandler = null;
        private Object closeHandler = null;
        private ItemStack backgroundItem = null;
        private Inventory inventory = null;
        private boolean allowItemRemoval = false;

        public InventoryGUI(String title, int rows) {
            this.title = title;
            this.rows = Math.max(1, Math.min(6, rows));
            this.size = this.rows * 9;
        }

        public InventoryGUI setItem(int slot, ItemStack item) {
            if (slot >= 0 && slot < size) {
                items.put(slot, item);
            }
            return this;
        }

        public InventoryGUI setItem(int slot, ItemStack item, Object clickHandler) {
            setItem(slot, item);
            if (clickHandler != null) {
                clickHandlers.put(slot, clickHandler);
            }
            return this;
        }

        public InventoryGUI setItems(int startSlot, int endSlot, ItemStack item) {
            for (int i = startSlot; i <= endSlot && i < size; i++) {
                items.put(i, item);
            }
            return this;
        }

        public InventoryGUI fill(ItemStack item) {
            backgroundItem = item;
            for (int i = 0; i < size; i++) {
                if (!items.containsKey(i)) {
                    items.put(i, item);
                }
            }
            return this;
        }

        public InventoryGUI fillBorders(ItemStack item) {

            for (int i = 0; i < 9; i++) {
                items.put(i, item);
            }

            for (int i = size - 9; i < size; i++) {
                items.put(i, item);
            }

            for (int i = 0; i < size; i += 9) {
                items.put(i, item);
            }

            for (int i = 8; i < size; i += 9) {
                items.put(i, item);
            }
            return this;
        }

        public InventoryGUI setBackground(ItemStack item) {
            this.backgroundItem = item;
            return this;
        }

        public InventoryGUI onClick(Object handler) {
            this.globalClickHandler = handler;
            return this;
        }

        public InventoryGUI onClose(Object handler) {
            this.closeHandler = handler;
            return this;
        }

        public InventoryGUI setAllowItemRemoval(boolean allow) {
            this.allowItemRemoval = allow;
            return this;
        }

        public Inventory build() {
            CustomInventoryHolder holder = new CustomInventoryHolder();
            Component component = legacyToComponentWithAmpersand(title);
            inventory = Bukkit.createInventory(holder, size, component);
            holder.setInventory(inventory);

            Map<Integer, ItemStack> itemsCopy = new HashMap<>(items);

            for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
                inventory.setItem(entry.getKey(), entry.getValue());
            }

            if (backgroundItem != null) {
                for (int i = 0; i < size; i++) {
                    if (inventory.getItem(i) == null) {
                        inventory.setItem(i, backgroundItem);

                        itemsCopy.put(i, backgroundItem.clone());
                    }
                }
            }

            holder.setData("clickHandlers", new HashMap<>(clickHandlers));
            holder.setData("globalClickHandler", globalClickHandler);
            holder.setData("closeHandler", closeHandler);
            holder.setData("allowItemRemoval", allowItemRemoval);
            holder.setData("items", itemsCopy);
            holder.setData("backgroundItem", backgroundItem != null ? backgroundItem.clone() : null);
            holder.setData("title", title);
            holder.setData("size", size);

            if (!clickHandlers.isEmpty() || globalClickHandler != null) {

                registerInventoryClick(inventory, null);
            }

            if (closeHandler != null) {
                registerInventoryClose(inventory, closeHandler);
            }

            return inventory;
        }

        public Inventory buildAndOpen(Player player) {
            Inventory inv = build();
            player.openInventory(inv);
            return inv;
        }

        public Inventory open(Player player) {
            return buildAndOpen(player);
        }
    }

    public Inventory createInventory(InventoryHolder holder, int size, String title) {
        Component component = legacyToComponentWithAmpersand(title);
        return Bukkit.createInventory(holder, size, component);
    }

    public Inventory createInventory(InventoryHolder holder, InventoryType type, String title) {
        Component component = legacyToComponentWithAmpersand(title);
        return Bukkit.createInventory(holder, type, component);
    }

    public Inventory createCustomInventory(int size, String title) {
        if (size % 9 != 0 || size < 9 || size > 54) {
            plugin.getLogger().warning("Invalid inventory size: " + size + ". Must be a multiple of 9 between 9 and 54.");
            size = Math.max(9, Math.min(54, (size / 9) * 9));
        }
        CustomInventoryHolder holder = new CustomInventoryHolder();
        Component component = legacyToComponentWithAmpersand(title);
        Inventory inv = Bukkit.createInventory(holder, size, component);
        holder.setInventory(inv);
        return inv;
    }

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

    public CustomInventoryHolder createInventoryHolder() {
        return new CustomInventoryHolder();
    }

    public Inventory createInventoryWithHolder(CustomInventoryHolder holder, int size, String title) {
        if (size % 9 != 0 || size < 9 || size > 54) {
            plugin.getLogger().warning("Invalid inventory size: " + size + ". Must be a multiple of 9 between 9 and 54.");
            size = Math.max(9, Math.min(54, (size / 9) * 9));
        }
        Component component = legacyToComponentWithAmpersand(title);
        Inventory inv = Bukkit.createInventory(holder, size, component);
        holder.setInventory(inv);
        return inv;
    }

    public InventoryGUI createGUI(String title, int rows) {
        return new InventoryGUI(title, rows);
    }

    private EventAPI eventAPI;

    public void setEventAPI(EventAPI eventAPI) {
        this.eventAPI = eventAPI;
    }

    public void clearHandlers() {
        inventoryClickHandlers.clear();
        inventoryCloseHandlers.clear();
    }

    public static void resetEventRegistration() {
        inventoryClickEventRegistered = false;
        inventoryCloseEventRegistered = false;
    }

    public void registerInventoryClick(Inventory inventory, Object handler) {
        inventoryClickHandlers.put(inventory, handler);

        if (!inventoryClickEventRegistered && eventAPI != null) {
            inventoryClickEventRegistered = true;

            eventAPI.registerEvent("inventory.InventoryClickEvent", new Object() {

            });
            debug("Registered InventoryClickEvent through EventAPI");
        }
    }

    public void registerInventoryClose(Inventory inventory, Object handler) {
        inventoryCloseHandlers.put(inventory, handler);

        if (!inventoryCloseEventRegistered && eventAPI != null) {
            inventoryCloseEventRegistered = true;

            eventAPI.registerEvent("inventory.InventoryCloseEvent", new Object() {

            });
            debug("Registered InventoryCloseEvent through EventAPI");
        }
    }

    public void handleInventoryClick(InventoryClickEvent event) {
        Inventory clickedInv = event.getClickedInventory();
        Inventory topInv = event.getInventory();

        if (clickedInv == null) {
            return;
        }

        boolean isTopInventory = clickedInv.equals(topInv);
        boolean isShiftClick = event.isShiftClick();

        if (!isTopInventory && !isShiftClick) {
            return;
        }

        Object handler = inventoryClickHandlers.get(topInv);

        if (topInv.getHolder() instanceof CustomInventoryHolder) {
            CustomInventoryHolder holder = (CustomInventoryHolder) topInv.getHolder();

            if (holder.isClosed()) {
                return;
            }

            @SuppressWarnings("unchecked")
            Map<Integer, Object> slotHandlers = (Map<Integer, Object>) holder.getData("clickHandlers");
            Object globalHandler = holder.getData("globalClickHandler");
            Object allowRemovalObj = holder.getData("allowItemRemoval");
            boolean allowItemRemoval = allowRemovalObj instanceof Boolean ? (Boolean) allowRemovalObj : false;

            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && !allowItemRemoval) {
                event.setCancelled(true);
                return;
            }

            if (!allowItemRemoval && isTopInventory) {

                event.setCancelled(true);
            }

            if (slotHandlers != null || globalHandler != null) {
                int slot = event.getSlot();

                Object slotHandler = null;
                if (slot >= 0 && slotHandlers != null) {
                    slotHandler = slotHandlers.get(slot);
                }

                if (slotHandler instanceof Function && apiHelper.getScope() != null && slot >= 0) {
                    try {
                        org.mozilla.javascript.Context cx = org.mozilla.javascript.Context.enter();
                        try {
                            cx.setOptimizationLevel(-1);
                            cx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                            ((Function) slotHandler).call(cx, apiHelper.getScope(), apiHelper.getScope(), new Object[]{event});
                        } finally {
                            org.mozilla.javascript.Context.exit();
                        }
                    } catch (Exception e) {
                        plugin.getLogger().severe("Error in inventory click handler: " + e.getMessage());
                        if (isDebugMode()) {
                            e.printStackTrace();
                        }
                    }
                    return;
                }

                if (globalHandler instanceof Function && apiHelper.getScope() != null) {
                    try {
                        org.mozilla.javascript.Context cx = org.mozilla.javascript.Context.enter();
                        try {
                            cx.setOptimizationLevel(-1);
                            cx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                            ((Function) globalHandler).call(cx, apiHelper.getScope(), apiHelper.getScope(), new Object[]{event});
                        } finally {
                            org.mozilla.javascript.Context.exit();
                        }
                    } catch (Exception e) {
                        plugin.getLogger().severe("Error in inventory click handler: " + e.getMessage());
                        if (isDebugMode()) {
                            e.printStackTrace();
                        }
                    }
                    return;
                }
            }
        }

        if (handler instanceof Function && apiHelper.getScope() != null) {
            try {
                org.mozilla.javascript.Context cx = org.mozilla.javascript.Context.enter();
                try {
                    cx.setOptimizationLevel(-1);
                    cx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                    ((Function) handler).call(cx, apiHelper.getScope(), apiHelper.getScope(), new Object[]{event});
                } finally {
                    org.mozilla.javascript.Context.exit();
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error in inventory click handler: " + e.getMessage());
                if (isDebugMode()) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void handleInventoryClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        Object handler = inventoryCloseHandlers.get(inv);

        if (inv.getHolder() instanceof CustomInventoryHolder) {
            CustomInventoryHolder holder = (CustomInventoryHolder) inv.getHolder();

            holder.markClosed();

            Object closeHandler = holder.getData("closeHandler");
            if (closeHandler instanceof Function && apiHelper.getScope() != null) {
                try {
                    org.mozilla.javascript.Context cx = org.mozilla.javascript.Context.enter();
                    try {
                        cx.setOptimizationLevel(-1);
                        cx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                        ((Function) closeHandler).call(cx, apiHelper.getScope(), apiHelper.getScope(), new Object[]{event});
                    } finally {
                        org.mozilla.javascript.Context.exit();
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Error in inventory close handler: " + e.getMessage());
                    if (isDebugMode()) {
                        e.printStackTrace();
                    }
                }
            }

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                synchronized (inventoryClickHandlers) {
                    inventoryClickHandlers.remove(inv);
                }
                synchronized (inventoryCloseHandlers) {
                    inventoryCloseHandlers.remove(inv);
                }
            }, 1L);

            return;
        }

        if (handler instanceof Function && apiHelper.getScope() != null) {
            try {
                org.mozilla.javascript.Context cx = org.mozilla.javascript.Context.enter();
                try {
                    cx.setOptimizationLevel(-1);
                    cx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                    ((Function) handler).call(cx, apiHelper.getScope(), apiHelper.getScope(), new Object[]{event});
                } finally {
                    org.mozilla.javascript.Context.exit();
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error in inventory close handler: " + e.getMessage());
                if (isDebugMode()) {
                    e.printStackTrace();
                }
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            synchronized (inventoryClickHandlers) {
                inventoryClickHandlers.remove(inv);
            }
            synchronized (inventoryCloseHandlers) {
                inventoryCloseHandlers.remove(inv);
            }
        }, 1L);
    }

    public CustomInventoryHolder getInventoryHolder(Inventory inventory) {
        InventoryHolder holder = inventory.getHolder();
        if (holder instanceof CustomInventoryHolder) {
            return (CustomInventoryHolder) holder;
        }
        return null;
    }

    public boolean isCustomInventory(Inventory inventory) {
        return inventory.getHolder() instanceof CustomInventoryHolder;
    }

    public Object getSlotClickHandler(Inventory inventory, int slot) {
        CustomInventoryHolder holder = getInventoryHolder(inventory);
        if (holder != null) {
            @SuppressWarnings("unchecked")
            Map<Integer, Object> handlers = (Map<Integer, Object>) holder.getData("clickHandlers");
            if (handlers != null) {
                return handlers.get(slot);
            }
        }
        return null;
    }

    public Object getGlobalClickHandler(Inventory inventory) {
        CustomInventoryHolder holder = getInventoryHolder(inventory);
        if (holder != null) {
            return holder.getData("globalClickHandler");
        }
        return null;
    }

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

    public ItemStack getInventoryItem(Inventory inventory, int slot) {
        if (inventory == null || slot < 0 || slot >= inventory.getSize()) {
            return null;
        }
        return inventory.getItem(slot);
    }

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

            inventory.clear();

            for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
                int slot = entry.getKey();
                if (slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, entry.getValue());
                }
            }

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

    public boolean isInventorySlotEmpty(Inventory inventory, int slot) {
        if (inventory == null || slot < 0 || slot >= inventory.getSize()) {
            return true;
        }
        ItemStack item = inventory.getItem(slot);
        return item == null || item.getType() == Material.AIR;
    }

    public int getFirstEmptySlot(Inventory inventory) {
        if (inventory == null) {
            return -1;
        }
        return inventory.firstEmpty();
    }

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

    public boolean inventoryContains(Inventory inventory, ItemStack item) {
        if (inventory == null || item == null) {
            return false;
        }
        return inventory.contains(item);
    }

    public boolean inventoryContainsAtLeast(Inventory inventory, ItemStack item, int amount) {
        if (inventory == null || item == null || amount <= 0) {
            return false;
        }
        return inventory.containsAtLeast(item, amount);
    }

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
}
