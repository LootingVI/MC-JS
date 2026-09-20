package de.flori.mCJS.dashboard;

import de.flori.mCJS.api.VaultAPI;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.permission.Permission;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VaultAPITest {
    JavaPlugin plugin;
    Server server;
    ServicesManager services;
    Economy economy;
    Player player, target;
    PlayerInventory inventory;
    ItemStack item;
    VaultAPI vault;

    @BeforeEach void setup() {
        plugin = mock(JavaPlugin.class); server = mock(Server.class); services = mock(ServicesManager.class);
        economy = mock(Economy.class); player = mock(Player.class); target = mock(Player.class);
        inventory = mock(PlayerInventory.class); item = mock(ItemStack.class);
        when(plugin.getServer()).thenReturn(server); when(plugin.getLogger()).thenReturn(mock(Logger.class));
        when(server.getServicesManager()).thenReturn(services); when(server.isPrimaryThread()).thenReturn(true);
        when(services.getRegistration(Economy.class)).thenReturn(new RegisteredServiceProvider<>(Economy.class, economy, ServicePriority.Normal, plugin));
        when(economy.isEnabled()).thenReturn(true); when(economy.getName()).thenReturn("Test Economy");
        when(player.getUniqueId()).thenReturn(UUID.randomUUID()); when(target.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getInventory()).thenReturn(inventory); when(inventory.getMaxStackSize()).thenReturn(64);
        when(inventory.getStorageContents()).thenAnswer(call -> new ItemStack[36]);
        when(inventory.addItem(any(ItemStack[].class))).thenReturn(new HashMap<>());
        var material = mock(Material.class); when(item.getType()).thenReturn(material);
        when(item.getAmount()).thenReturn(1); when(item.getMaxStackSize()).thenReturn(64); when(item.clone()).thenReturn(item);
        when(economy.getBalance(player)).thenReturn(50.0); when(economy.has(player, 10)).thenReturn(true);
        when(economy.withdrawPlayer(player, 10)).thenReturn(ok(10, 40));
        when(economy.depositPlayer(player, 10)).thenReturn(ok(10, 50));
        vault = new VaultAPI(plugin);
    }
    EconomyResponse ok(double amount, double balance) { return new EconomyResponse(amount, balance, EconomyResponse.ResponseType.SUCCESS, null); }
    EconomyResponse denied() { return new EconomyResponse(0, 50, EconomyResponse.ResponseType.FAILURE, "Transaction denied"); }

    @Test void usesRegisteredProviderAndKeepsRhinoResultsAccessible() {
        assertEquals("Test Economy", vault.getProviderName()); assertEquals(50, vault.balance(player));
        try (var context = Context.enter()) {
            var scope = context.initStandardObjects();
            ScriptableObject.putProperty(scope, "vault", Context.javaToJS(vault, scope));
            ScriptableObject.putProperty(scope, "player", Context.javaToJS(player, scope));
            assertEquals("ok:40:", Context.toString(context.evaluateString(scope, "var result = vault.withdraw(player, 10); result.getCode() + ':' + result.getBalance() + ':' + result.getError();", "test", 1, null)));
        }
        var permissions = mock(Permission.class); var chat = mock(Chat.class);
        when(services.getRegistration(Permission.class)).thenReturn(new RegisteredServiceProvider<>(Permission.class, permissions, ServicePriority.Normal, plugin));
        when(services.getRegistration(Chat.class)).thenReturn(new RegisteredServiceProvider<>(Chat.class, chat, ServicePriority.Normal, plugin));
        when(permissions.getPrimaryGroup(player)).thenReturn("member"); when(chat.getPlayerPrefix(player)).thenReturn("[Member]");
        assertEquals("member", vault.getPrimaryGroup(player)); assertEquals("[Member]", vault.getPrefix(player));
    }
    @Test void missingProvidersAndInvalidAmountsNeverPretendToSucceed() {
        when(services.getRegistration(Economy.class)).thenReturn(null);
        assertFalse(vault.isAvailable()); assertEquals("unavailable", vault.deposit(player, 10).getCode());
        assertThrows(IllegalStateException.class, () -> vault.balance(player));
        for (double invalid : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY}) assertThrows(IllegalArgumentException.class, () -> vault.withdraw(player, invalid));
        when(server.isPrimaryThread()).thenReturn(false);
        assertThrows(IllegalStateException.class, () -> vault.deposit(player, 10));
        verify(economy, never()).withdrawPlayer(any(Player.class), anyDouble());
    }
    @Test void purchaseRequiresCapacityAndFundsAndHonorsProviderRejection() {
        when(inventory.getStorageContents()).thenReturn(new ItemStack[0]);
        assertEquals("space", vault.purchase(player, 10, item).getCode());
        verify(economy, never()).withdrawPlayer(player, 10);
        when(inventory.getStorageContents()).thenReturn(new ItemStack[36]); when(economy.has(player, 10)).thenReturn(false);
        assertEquals("funds", vault.purchase(player, 10, item).getCode());
        when(economy.has(player, 10)).thenReturn(true); when(economy.withdrawPlayer(player, 10)).thenReturn(denied());
        assertFalse(vault.purchase(player, 10, item).isSuccess()); verify(inventory, never()).addItem(any(ItemStack[].class));
        when(economy.withdrawPlayer(player, 10)).thenReturn(ok(10, 40));
        assertTrue(vault.purchase(player, 10, item).isSuccess()); verify(inventory).addItem(any(ItemStack[].class));
    }
    @Test void partialInventoryInsertRestoresItemsAndRefundsActualDebit() {
        var leftover = new HashMap<Integer, ItemStack>(); leftover.put(0, item);
        when(inventory.addItem(any(ItemStack[].class))).thenReturn(leftover);
        when(economy.withdrawPlayer(player, 10)).thenReturn(ok(9.99, 40.01));
        when(economy.depositPlayer(player, 9.99)).thenReturn(ok(9.99, 50));
        assertEquals("space", vault.purchase(player, 10, item).getCode());
        verify(inventory).setStorageContents(any(ItemStack[].class)); verify(economy).depositPlayer(player, 9.99);
    }
    @Test void failedRecipientDepositRefundsSenderAndFailedRefundIsExplicit() {
        when(economy.depositPlayer(target, 10)).thenReturn(denied());
        assertEquals("failed", vault.transfer(player, target, 10).getCode()); verify(economy).depositPlayer(player, 10);
        when(economy.depositPlayer(player, 10)).thenReturn(denied());
        assertEquals("refund_failed", vault.transfer(player, target, 10).getCode()); verify(plugin.getLogger()).severe(contains(player.getUniqueId().toString()));
    }
    @Test void evenInventoryRestorationFailureStillAttemptsRefund() {
        when(inventory.addItem(any(ItemStack[].class))).thenThrow(new IllegalStateException("Insert failed"));
        doThrow(new IllegalStateException("Restore failed")).when(inventory).setStorageContents(any(ItemStack[].class));
        assertEquals("inventory_restore_failed", vault.purchase(player, 10, item).getCode()); verify(economy).depositPlayer(player, 10);
    }
}
