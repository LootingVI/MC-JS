package de.flori.mCJS.api;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;

/**
 * API module for Player-related operations
 */
public class PlayerAPI extends BaseAPI {
    
    public PlayerAPI(JavaPlugin plugin) {
        super(plugin);
    }
    
    // ===== PLAYER LOOKUP =====
    public Player getPlayer(Object name) {
        if (name == null) return null;
        String nameStr = name.toString();
        return nameStr != null && !nameStr.isEmpty() ? Bukkit.getPlayer(nameStr) : null;
    }
    
    public Player getPlayerExact(Object name) {
        if (name == null) return null;
        String nameStr = name.toString();
        return nameStr != null && !nameStr.isEmpty() ? Bukkit.getPlayerExact(nameStr) : null;
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
    
    public org.bukkit.OfflinePlayer getOfflinePlayer(String name) {
        return name != null ? Bukkit.getOfflinePlayer(name) : null;
    }
    
    public org.bukkit.OfflinePlayer getOfflinePlayerByUUID(String uuid) {
        if (uuid == null) {
            return null;
        }
        try {
            return Bukkit.getOfflinePlayer(java.util.UUID.fromString(uuid));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    public java.util.Collection<? extends Player> getOnlinePlayers() {
        return Bukkit.getOnlinePlayers();
    }
    
    public boolean isPlayerOnline(String name) {
        return name != null && Bukkit.getPlayer(name) != null;
    }
    
    public boolean isPlayerOnline(Player player) {
        return player != null && player.isOnline();
    }
    
    // ===== PLAYER MANAGEMENT =====
    public void kickPlayer(Player player, String reason) {
        if (player != null && reason != null) {
            Component component = legacyToComponentWithAmpersand(reason);
            player.kick(component);
        }
    }
    
    public void banPlayer(String playerName, String reason) {
        if (playerName == null) {
            return;
        }
        try {
            org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
            @SuppressWarnings({"deprecation", "rawtypes"})
            org.bukkit.BanList banList = Bukkit.getBanList(org.bukkit.BanList.Type.NAME);
            String banReason = reason != null ? reason : "Banned by an operator";
            banList.addBan(playerName, banReason, (java.util.Date) null, (String) null);
            if (offlinePlayer.isOnline()) {
                Player player = offlinePlayer.getPlayer();
                if (player != null) {
                    Component component = legacyToComponentWithAmpersand(banReason);
                    player.kick(component);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error banning player '" + playerName + "': " + e.getMessage());
        }
    }
    
    public void unbanPlayer(String playerName) {
        if (playerName != null) {
            try {
                @SuppressWarnings({"deprecation", "rawtypes"})
                org.bukkit.BanList banList = Bukkit.getBanList(org.bukkit.BanList.Type.NAME);
                banList.pardon(playerName);
            } catch (Exception e) {
                plugin.getLogger().warning("Error unbanning player '" + playerName + "': " + e.getMessage());
            }
        }
    }
    
    public boolean isBanned(String playerName) {
        if (playerName == null) {
            return false;
        }
        try {
            @SuppressWarnings({"deprecation", "rawtypes"})
            org.bukkit.BanList banList = Bukkit.getBanList(org.bukkit.BanList.Type.NAME);
            @SuppressWarnings("deprecation")
            boolean banned = banList.isBanned(playerName);
            return banned;
        } catch (Exception e) {
            return false;
        }
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
    
    // ===== GAMEMODE METHODS =====
    public void setGameMode(Player player, GameMode mode) {
        if (player != null && mode != null) {
            player.setGameMode(mode);
        }
    }
    
    public GameMode getGameMode(Player player) {
        return player != null ? player.getGameMode() : null;
    }
    
    // ===== TELEPORTATION =====
    public void teleport(Player player, Location location) {
        if (player != null && location != null) {
            player.teleport(location);
        }
    }
    
    // ===== HEALTH AND DAMAGE =====
    public void setHealth(Player player, double health) {
        if (player == null) return;
        try {
            org.bukkit.attribute.AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                double maxHealth = maxHealthAttr.getValue();
                player.setHealth(Math.min(health, maxHealth));
            } else {
                player.setHealth(Math.min(health, player.getHealthScale()));
            }
        } catch (Exception e) {
            player.setHealth(Math.min(health, player.getHealthScale()));
        }
    }
    
    public double getHealth(Player player) {
        return player != null ? player.getHealth() : 0.0;
    }
    
    public double getMaxHealth(Player player) {
        if (player == null) return 0.0;
        try {
            org.bukkit.attribute.AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                return maxHealthAttr.getValue();
            } else {
                return player.getHealthScale();
            }
        } catch (Exception e) {
            return player.getHealthScale();
        }
    }
    
    public void damage(Player player, double damage) {
        if (player != null) {
            player.damage(damage);
        }
    }
    
    // ===== FOOD AND SATURATION =====
    public void setFoodLevel(Player player, int level) {
        if (player != null) {
            player.setFoodLevel(level);
        }
    }
    
    public int getFoodLevel(Player player) {
        return player != null ? player.getFoodLevel() : 0;
    }
    
    public void setSaturation(Player player, float saturation) {
        if (player != null) {
            player.setSaturation(saturation);
        }
    }
    
    public float getSaturation(Player player) {
        return player != null ? player.getSaturation() : 0.0f;
    }
    
    // ===== INVENTORY MANAGEMENT =====
    public void giveItem(Player player, ItemStack item) {
        if (player != null && item != null) {
            player.getInventory().addItem(item);
        }
    }
    
    public void clearInventory(Player player) {
        if (player != null) {
            player.getInventory().clear();
        }
    }
    
    public ItemStack getItemInMainHand(Player player) {
        return player != null ? player.getInventory().getItemInMainHand() : null;
    }
    
    public void setItemInMainHand(Player player, ItemStack item) {
        if (player != null && item != null) {
            player.getInventory().setItemInMainHand(item);
        }
    }
    
    public ItemStack getItemInOffHand(Player player) {
        return player != null ? player.getInventory().getItemInOffHand() : null;
    }
    
    public void setItemInOffHand(Player player, ItemStack item) {
        if (player != null && item != null) {
            player.getInventory().setItemInOffHand(item);
        }
    }
    
    // ===== CHAT AND MESSAGES =====
    public void sendMessage(CommandSender sender, Object message) {
        if (sender != null && message != null) {
            String messageStr = message.toString();
            Component component = legacyToComponentWithAmpersand(messageStr);
            sender.sendMessage(component);
        }
    }
    
    public void sendTitle(Player player, Object title, Object subtitle) {
        if (player == null) return;
        String titleStr = title != null ? title.toString() : "";
        String subtitleStr = subtitle != null ? subtitle.toString() : "";
        Component titleComponent = legacyToComponentWithAmpersand(titleStr);
        Component subtitleComponent = legacyToComponentWithAmpersand(subtitleStr);
        Title titleObj = Title.title(
            titleComponent,
            subtitleComponent,
            Title.Times.times(Duration.ofMillis(10 * 50), Duration.ofMillis(70 * 50), Duration.ofMillis(20 * 50))
        );
        player.showTitle(titleObj);
    }
    
    public void sendTitle(Player player, Object title, Object subtitle, int fadeIn, int stay, int fadeOut) {
        if (player == null) return;
        String titleStr = title != null ? title.toString() : "";
        String subtitleStr = subtitle != null ? subtitle.toString() : "";
        Component titleComponent = legacyToComponentWithAmpersand(titleStr);
        Component subtitleComponent = legacyToComponentWithAmpersand(subtitleStr);
        Title titleObj = Title.title(
            titleComponent,
            subtitleComponent,
            Title.Times.times(Duration.ofMillis(fadeIn * 50), Duration.ofMillis(stay * 50), Duration.ofMillis(fadeOut * 50))
        );
        player.showTitle(titleObj);
    }
    
    public void sendActionBar(Player player, Object message) {
        if (player != null && message != null) {
            String messageStr = message.toString();
            Component component = legacyToComponentWithAmpersand(messageStr);
            player.sendActionBar(component);
        }
    }
    
    // ===== PLAYER EXPERIENCE =====
    public void setExp(Player player, float exp) {
        if (player != null) {
            player.setExp(exp);
        }
    }
    
    public float getExp(Player player) {
        return player != null ? player.getExp() : 0.0f;
    }
    
    public void setLevel(Player player, int level) {
        if (player != null) {
            player.setLevel(level);
        }
    }
    
    public int getLevel(Player player) {
        return player != null ? player.getLevel() : 0;
    }
    
    public void giveExp(Player player, int amount) {
        if (player != null) {
            player.giveExp(amount);
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
    
    // ===== ADVANCEMENT METHODS =====
    public void grantAdvancement(Player player, String advancementKey) {
        if (player == null || advancementKey == null) return;
        var advancement = Bukkit.getAdvancement(org.bukkit.NamespacedKey.minecraft(advancementKey));
        if (advancement != null) {
            player.getAdvancementProgress(advancement).awardCriteria("impossible");
        }
    }
    
    public void revokeAdvancement(Player player, String advancementKey) {
        if (player == null || advancementKey == null) return;
        var advancement = Bukkit.getAdvancement(org.bukkit.NamespacedKey.minecraft(advancementKey));
        if (advancement != null) {
            player.getAdvancementProgress(advancement).revokeCriteria("impossible");
        }
    }
    
    // ===== ADVANCED PLAYER UTILITIES =====
    public int getPlayerPing(Player player) {
        return player != null ? player.getPing() : 0;
    }
    
    public String getPlayerLocale(Player player) {
        return player != null ? player.locale().toString() : "en_US";
    }
    
    public boolean isPlayerSleeping(Player player) {
        return player != null && player.isSleeping();
    }
    
    public boolean isPlayerSneaking(Player player) {
        return player != null && player.isSneaking();
    }
    
    public void setPlayerSneaking(Player player, boolean sneaking) {
        if (player != null) {
            player.setSneaking(sneaking);
        }
    }
    
    public boolean isPlayerSprinting(Player player) {
        return player != null && player.isSprinting();
    }
    
    public void setPlayerSprinting(Player player, boolean sprinting) {
        if (player != null) {
            player.setSprinting(sprinting);
        }
    }
    
    public Location getPlayerCompassTarget(Player player) {
        return player != null ? player.getCompassTarget() : null;
    }
    
    public void setPlayerCompassTarget(Player player, Location location) {
        if (player != null && location != null) {
            player.setCompassTarget(location);
        }
    }
    
    public Location getPlayerBedSpawnLocation(Player player) {
        return player != null ? player.getBedSpawnLocation() : null;
    }
    
    public double getPlayerLastDamage(Player player) {
        return player != null ? player.getLastDamage() : 0.0;
    }
    
    public String getPlayerLastDamageCause(Player player) {
        if (player != null && player.getLastDamageCause() != null) {
            return player.getLastDamageCause().getCause().name();
        }
        return null;
    }
    
    // ===== PLAYER DATA METHODS =====
    public void setPlayerMetadata(Player player, String key, Object value) {
        if (player != null && key != null) {
            @SuppressWarnings("deprecation")
            org.bukkit.metadata.FixedMetadataValue metadataValue = new org.bukkit.metadata.FixedMetadataValue(plugin, value);
            player.setMetadata(key, metadataValue);
        }
    }
    
    public Object getPlayerMetadata(Player player, String key) {
        if (player == null || key == null) {
            return null;
        }
        @SuppressWarnings("deprecation")
        java.util.List<org.bukkit.metadata.MetadataValue> values = player.getMetadata(key);
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
    
    public String generateOfflineUUID(String playerName) {
        if (playerName == null) {
            return null;
        }
        java.util.UUID uuid = java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes());
        return uuid.toString();
    }
}
