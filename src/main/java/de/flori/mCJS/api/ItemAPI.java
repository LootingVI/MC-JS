package de.flori.mCJS.api;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API module for item and enchantment management
 */
public class ItemAPI extends BaseAPI {
    
    public ItemAPI(JavaPlugin plugin) {
        super(plugin);
    }
    
    // ===== ITEM CREATION METHODS =====
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

    public ItemStack setItemDisplayName(ItemStack item, Object name) {
        if (item == null || name == null) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String nameStr = name.toString();
            Component component = legacyToComponentWithAmpersand(nameStr);
            meta.displayName(component);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack setItemLore(ItemStack item, Object lore) {
        if (item == null || lore == null) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<Component> componentLore = new ArrayList<>();
            
            // Handle both List and JavaScript arrays
            if (lore instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<Object> loreList = (java.util.List<Object>) lore;
                for (Object line : loreList) {
                    if (line != null) {
                        componentLore.add(legacyToComponentWithAmpersand(line.toString()));
                    }
                }
            } else if (lore instanceof org.mozilla.javascript.Scriptable) {
                // Handle JavaScript arrays
                org.mozilla.javascript.Scriptable array = (org.mozilla.javascript.Scriptable) lore;
                Object length = array.get("length", array);
                if (length instanceof Number) {
                    int len = ((Number) length).intValue();
                    for (int i = 0; i < len; i++) {
                        Object line = array.get(i, array);
                        if (line != null) {
                            componentLore.add(legacyToComponentWithAmpersand(line.toString()));
                        }
                    }
                }
            }
            
            meta.lore(componentLore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    // ===== ENCHANTMENT METHODS =====
    // Helper method to get enchantment by name using registry
    private org.bukkit.enchantments.Enchantment getEnchantment(String enchantmentName) {
        if (enchantmentName == null) {
            return null;
        }
        try {
            // Try registry lookup first (new API)
            String normalized = enchantmentName.toLowerCase().replace("_", "");
            NamespacedKey key = NamespacedKey.minecraft(normalized);
            // Use Registry.ENCHANTMENTS instead of Registry.ENCHANTMENT
            @SuppressWarnings("deprecation")
            org.bukkit.enchantments.Enchantment enchantment = org.bukkit.enchantments.Enchantment.getByKey(key);
            if (enchantment != null) {
                return enchantment;
            }
            // Fallback to old API for backwards compatibility
            @SuppressWarnings("deprecation")
            org.bukkit.enchantments.Enchantment fallback = org.bukkit.enchantments.Enchantment.getByKey(key);
            if (fallback != null) {
                return fallback;
            }
        } catch (Exception e) {
            // Try old API as fallback
            try {
                @SuppressWarnings("deprecation")
                org.bukkit.enchantments.Enchantment oldApi = org.bukkit.enchantments.Enchantment.getByName(enchantmentName.toUpperCase());
                return oldApi;
            } catch (Exception ex) {
                return null;
            }
        }
        return null;
    }
    
    public void addEnchantment(ItemStack item, String enchantmentName, int level) {
        if (item == null || enchantmentName == null) {
            return;
        }
        try {
            org.bukkit.enchantments.Enchantment enchantment = getEnchantment(enchantmentName);
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
            org.bukkit.enchantments.Enchantment enchantment = getEnchantment(enchantmentName);
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
            org.bukkit.enchantments.Enchantment enchantment = getEnchantment(enchantmentName);
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
            org.bukkit.enchantments.Enchantment enchantment = getEnchantment(enchantmentName);
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
                // Use getKey() method instead of getName()
                NamespacedKey key = entry.getKey().getKey();
                enchantments.put(key.toString(), entry.getValue());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting enchantments: " + e.getMessage());
        }
        return enchantments;
    }
}
