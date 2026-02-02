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

/**
 * API module for inventory management and GUI creation
 */
public class InventoryAPI extends BaseAPI {
    private final APIHelper apiHelper;
    
    // Inventory click/close handlers
    private final Map<Inventory, Object> inventoryClickHandlers = Collections.synchronizedMap(new HashMap<>());
    private final Map<Inventory, Object> inventoryCloseHandlers = Collections.synchronizedMap(new HashMap<>());
    private static boolean inventoryClickEventRegistered = false;
    private static boolean inventoryCloseEventRegistered = false;
    
    public InventoryAPI(JavaPlugin plugin, APIHelper apiHelper) {
        super(plugin);
        this.apiHelper = apiHelper;
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
    
    /**
     * GUI Builder class for creating custom inventories easily
     */
    public class InventoryGUI {
        private String title;
        private int rows;
        private int size;
        // Use ConcurrentHashMap for thread-safety
        private Map<Integer, ItemStack> items = new ConcurrentHashMap<>();
        private Map<Integer, Object> clickHandlers = new ConcurrentHashMap<>();
        private volatile Object globalClickHandler = null;
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
            Component component = legacyToComponentWithAmpersand(title);
            inventory = Bukkit.createInventory(holder, size, component);
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
        
        /**
         * Open the GUI for a player (alias for buildAndOpen)
         */
        public Inventory open(Player player) {
            return buildAndOpen(player);
        }
    }
    
    // ===== INVENTORY CREATION METHODS =====
    public Inventory createInventory(InventoryHolder holder, int size, String title) {
        Component component = legacyToComponentWithAmpersand(title);
        return Bukkit.createInventory(holder, size, component);
    }

    public Inventory createInventory(InventoryHolder holder, InventoryType type, String title) {
        Component component = legacyToComponentWithAmpersand(title);
        return Bukkit.createInventory(holder, type, component);
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
        Component component = legacyToComponentWithAmpersand(title);
        Inventory inv = Bukkit.createInventory(holder, size, component);
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
        Component component = legacyToComponentWithAmpersand(title);
        Inventory inv = Bukkit.createInventory(holder, size, component);
        holder.setInventory(inv);
        return inv;
    }
    
    /**
     * Create a GUI builder pattern for easier inventory creation
     */
    public InventoryGUI createGUI(String title, int rows) {
        return new InventoryGUI(title, rows);
    }
    
    // ===== INVENTORY HANDLER REGISTRATION =====
    private EventAPI eventAPI;
    
    /**
     * Set the EventAPI reference for automatic event registration
     */
    public void setEventAPI(EventAPI eventAPI) {
        this.eventAPI = eventAPI;
    }
    
    public void registerInventoryClick(Inventory inventory, Object handler) {
        inventoryClickHandlers.put(inventory, handler);
        
        // Register the event globally if not already registered
        // Note: EventAPI.executeEventWrapper will automatically call handleInventoryClick
        // when InventoryClickEvent is fired, so we just need to ensure the event is registered
        if (!inventoryClickEventRegistered && eventAPI != null) {
            inventoryClickEventRegistered = true;
            // Register a dummy handler - EventAPI will route to handleInventoryClick automatically
            eventAPI.registerEvent("inventory.InventoryClickEvent", new Object() {
                // Dummy handler - actual handling is done in EventAPI.executeEventWrapper
            });
            debug("Registered InventoryClickEvent through EventAPI");
        }
    }

    public void registerInventoryClose(Inventory inventory, Object handler) {
        inventoryCloseHandlers.put(inventory, handler);
        
        // Register the event globally if not already registered
        // Note: EventAPI.executeEventWrapper will automatically call handleInventoryClose
        // when InventoryCloseEvent is fired, so we just need to ensure the event is registered
        if (!inventoryCloseEventRegistered && eventAPI != null) {
            inventoryCloseEventRegistered = true;
            // Register a dummy handler - EventAPI will route to handleInventoryClose automatically
            eventAPI.registerEvent("inventory.InventoryCloseEvent", new Object() {
                // Dummy handler - actual handling is done in EventAPI.executeEventWrapper
            });
            debug("Registered InventoryCloseEvent through EventAPI");
        }
    }
    
    /**
     * Handle inventory click for all registered inventories
     * This method should be called from EventAPI
     */
    public void handleInventoryClick(InventoryClickEvent event) {
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
        
        Object handler = inventoryClickHandlers.get(topInv);
        
        // Check if it's a custom inventory with slot-specific handlers
        if (topInv.getHolder() instanceof CustomInventoryHolder) {
            CustomInventoryHolder holder = (CustomInventoryHolder) topInv.getHolder();
            
            // Skip if inventory is closed
            if (holder.isClosed()) {
                return;
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
                
                // Execute global handler if no slot-specific handler or for shift-clicks
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
        
        // Fallback to regular handler
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
    
    /**
     * Handle inventory close for all registered inventories
     * This method should be called from EventAPI
     */
    public void handleInventoryClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        Object handler = inventoryCloseHandlers.get(inv);
        
        // Check if it's a custom inventory with close handler in holder
        if (inv.getHolder() instanceof CustomInventoryHolder) {
            CustomInventoryHolder holder = (CustomInventoryHolder) inv.getHolder();
            
            // Mark inventory as closed
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
            
            // Cleanup: Remove handlers to prevent memory leaks
            // Use a delayed task to ensure cleanup happens after event processing
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
        
        // Fallback to regular handler
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
        
        // Cleanup regular handlers too
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            synchronized (inventoryClickHandlers) {
                inventoryClickHandlers.remove(inv);
            }
            synchronized (inventoryCloseHandlers) {
                inventoryCloseHandlers.remove(inv);
            }
        }, 1L);
    }
    
    // ===== INVENTORY UTILITY METHODS =====
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
            @SuppressWarnings("unchecked")
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
    
    /**
     * Set inventory item
     */
    public void setInventoryItem(Inventory inventory, int slot, ItemStack item) {
        if (inventory == null || slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        inventory.setItem(slot, item);
    }

    /**
     * Fill inventory with item
     */
    public void fillInventory(Inventory inventory, ItemStack item) {
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, item);
        }
    }

    /**
     * Fill inventory range with item
     */
    public void fillInventoryRange(Inventory inventory, ItemStack item, int start, int end) {
        for (int i = start; i <= end && i < inventory.getSize(); i++) {
            inventory.setItem(i, item);
        }
    }
}
