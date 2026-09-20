package de.flori.mCJS.api;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import java.lang.reflect.InvocationTargetException;

public final class VaultAPI extends BaseAPI {
    public VaultAPI(JavaPlugin plugin) { super(plugin); }

    public record Result(boolean success, String code, String error, double amount, double balance) {
        public boolean isSuccess() { return success; }
        public String getCode() { return code; }
        public String getError() { return error; }
        public double getAmount() { return amount; }
        public double getBalance() { return balance; }
    }

    private record Service(Class<?> type, Object provider) {
        Object call(String name, Class<?>[] types, Object... args) {
            try { return type.getMethod(name, types).invoke(provider, args); }
            catch (ReflectiveOperationException error) {
                Throwable cause = error instanceof InvocationTargetException target ? target.getCause() : error;
                throw new IllegalStateException("Vault provider failed: " + cause.getMessage(), cause);
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Service service(String type) {
        try {
            var api = Class.forName("net.milkbowl.vault." + type, false, plugin.getClass().getClassLoader());
            var registration = plugin.getServer().getServicesManager().getRegistration((Class) api);
            return registration == null ? null : new Service(api, registration.getProvider());
        } catch (ClassNotFoundException | LinkageError ignored) { return null; }
    }

    private Service economy() {
        var service = service("economy.Economy");
        if (service == null || !Boolean.TRUE.equals(service.call("isEnabled", new Class<?>[0]))) {
            throw new IllegalStateException("Vault economy is unavailable. Install Vault and an enabled economy provider.");
        }
        return service;
    }

    public boolean isAvailable() {
        try { economy(); return true; } catch (IllegalStateException ignored) { return false; }
    }

    public String getProviderName() { return isAvailable() ? String.valueOf(economy().call("getName", new Class<?>[0])) : "Unavailable"; }
    public double balance(OfflinePlayer player) { requirePlayer(player); return ((Number) economy().call("getBalance", new Class<?>[]{OfflinePlayer.class}, player)).doubleValue(); }
    public String format(double amount) { finite(amount); return String.valueOf(economy().call("format", new Class<?>[]{double.class}, amount)); }
    public boolean has(OfflinePlayer player, double amount) { requirePlayer(player); amount(amount); return Boolean.TRUE.equals(economy().call("has", new Class<?>[]{OfflinePlayer.class, double.class}, player, amount)); }
    public Result deposit(OfflinePlayer player, double amount) { return transact("depositPlayer", player, amount); }
    public Result withdraw(OfflinePlayer player, double amount) { return transact("withdrawPlayer", player, amount); }

    private Result transact(String operation, OfflinePlayer player, double amount) {
        requirePlayer(player);
        amount(amount);
        if (!isAvailable()) return new Result(false, "unavailable", "Install Vault and an enabled economy provider.", 0, 0);
        Object response = economy().call(operation, new Class<?>[]{OfflinePlayer.class, double.class}, player, amount);
        try {
            Class<?> type = response.getClass();
            boolean success = Boolean.TRUE.equals(type.getMethod("transactionSuccess").invoke(response));
            Object message = type.getField("errorMessage").get(response);
            return new Result(success, success ? "ok" : "failed", message == null ? "" : message.toString(),
                    ((Number) type.getField("amount").get(response)).doubleValue(), ((Number) type.getField("balance").get(response)).doubleValue());
        } catch (ReflectiveOperationException error) { throw new IllegalStateException("Invalid Vault economy response", error); }
    }

    public Result transfer(OfflinePlayer from, OfflinePlayer to, double amount) {
        requirePlayer(from);
        requirePlayer(to);
        amount(amount);
        if (from.getUniqueId().equals(to.getUniqueId())) return new Result(false, "same_player", "Choose another player.", 0, balance(from));
        var debit = withdraw(from, amount);
        if (!debit.success()) return debit;
        try {
            var credit = deposit(to, debit.amount());
            if (credit.success()) return credit;
            return refund(from, debit.amount(), credit.code(), credit.error());
        } catch (RuntimeException error) { return refund(from, debit.amount(), "failed", error.getMessage()); }
    }

    public Result purchase(Player player, double cost, ItemStack item) {
        requirePlayer(player);
        amount(cost);
        if (item == null || item.getType().isAir() || item.getAmount() < 1) throw new IllegalArgumentException("A valid item is required");
        var inventory = player.getInventory();
        var before = inventory.getStorageContents();
        int space = 0;
        for (int index = 0; index < before.length; index++) {
            var slot = before[index];
            int maximum = Math.min(inventory.getMaxStackSize(), item.getMaxStackSize());
            if (slot == null || slot.getType().isAir()) space += maximum;
            else {
                before[index] = slot.clone();
                if (slot.isSimilar(item)) space += Math.max(0, maximum - slot.getAmount());
            }
        }
        if (!isAvailable()) return new Result(false, "unavailable", "Vault economy is unavailable.", 0, 0);
        if (space < item.getAmount()) return new Result(false, "space", "Make room in your inventory.", 0, balance(player));
        if (!has(player, cost)) return new Result(false, "funds", "Insufficient funds.", 0, balance(player));
        var debit = withdraw(player, cost);
        if (!debit.success()) return debit;
        try {
            if (inventory.addItem(item.clone()).isEmpty()) return debit;
        } catch (RuntimeException error) {
            return restoreAndRefund(player, before, debit.amount(), "failed", error.getMessage());
        }
        return restoreAndRefund(player, before, debit.amount(), "space", "Make room in your inventory.");
    }

    private Result restoreAndRefund(Player player, ItemStack[] before, double amount, String code, String message) {
        boolean restored = true;
        try { player.getInventory().setStorageContents(before); }
        catch (RuntimeException error) {
            restored = false;
            plugin.getLogger().severe("Inventory restoration failed for " + player.getUniqueId() + ": " + error.getMessage());
        }
        var refund = refund(player, amount, code, message);
        if (!restored && !refund.code().equals("refund_failed")) return new Result(false, "inventory_restore_failed", "Money refunded, but inventory restoration failed. Contact a server administrator.", 0, refund.balance());
        return refund;
    }

    private Result refund(OfflinePlayer player, double amount, String code, String message) {
        try {
            var result = deposit(player, amount);
            if (result.success()) return new Result(false, code, message, 0, result.balance());
        } catch (RuntimeException ignored) {}
        String error = "Refund failed for " + player.getUniqueId() + " (" + amount + "). Contact a server administrator.";
        plugin.getLogger().severe(error);
        return new Result(false, "refund_failed", error, amount, 0);
    }

    public String getPrimaryGroup(Player player) {
        requirePlayer(player);
        var permissions = service("permission.Permission");
        if (permissions == null) throw new IllegalStateException("A Vault permission provider is required");
        return String.valueOf(permissions.call("getPrimaryGroup", new Class<?>[]{Player.class}, player));
    }

    public String getPrefix(Player player) {
        requirePlayer(player);
        var chat = service("chat.Chat");
        if (chat == null) throw new IllegalStateException("A Vault chat provider is required");
        return String.valueOf(chat.call("getPlayerPrefix", new Class<?>[]{Player.class}, player));
    }

    private void requirePlayer(OfflinePlayer player) {
        if (player == null) throw new IllegalArgumentException("A player is required");
        if (!plugin.getServer().isPrimaryThread()) throw new IllegalStateException("Use Vault on the server thread");
    }
    private void finite(double amount) { if (!Double.isFinite(amount)) throw new IllegalArgumentException("Amount must be finite"); }
    private void amount(double amount) { finite(amount); if (amount < 0) throw new IllegalArgumentException("Amount must not be negative"); }
}
