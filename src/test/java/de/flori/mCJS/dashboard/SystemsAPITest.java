package de.flori.mCJS.dashboard;

import de.flori.mCJS.api.APIHelper;
import de.flori.mCJS.api.FileAPI;
import de.flori.mCJS.api.SchedulerAPI;
import de.flori.mCJS.api.SystemsAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptableObject;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SystemsAPITest {
    @TempDir Path directory;
    JavaPlugin plugin;
    Server server;
    PluginManager manager;
    FileAPI files;
    APIHelper helper;
    SchedulerAPI scheduler;
    SystemsAPI systems;
    Player player;
    PlayerInventory inventory;

    @BeforeEach
    void setup() {
        plugin = mock(JavaPlugin.class);
        server = mock(Server.class);
        manager = mock(PluginManager.class);
        helper = mock(APIHelper.class);
        scheduler = mock(SchedulerAPI.class);
        player = mock(Player.class);
        inventory = mock(PlayerInventory.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(manager);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isOnline()).thenReturn(true);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getMaxStackSize()).thenReturn(64);
        when(inventory.getStorageContents()).thenAnswer(call -> new ItemStack[36]);
        when(inventory.addItem(any(ItemStack[].class))).thenReturn(new HashMap<>());
        files = new FileAPI(plugin);
        systems = new SystemsAPI(plugin, files, helper, scheduler);
    }

    ItemStack item(Material material, int amount) {
        var result = mock(ItemStack.class);
        var type = mock(Material.class, material.name());
        when(type.isAir()).thenReturn(false);
        when(result.getType()).thenReturn(type);
        when(result.getAmount()).thenReturn(amount);
        when(result.getMaxStackSize()).thenReturn(64);
        when(result.clone()).thenReturn(result);
        return result;
    }

    String scope() { return "player:" + player.getUniqueId(); }

    double balance() { return Double.parseDouble(systems.readData("shop", scope(), "coins", "0")); }

    @Test
    void savedRecordsAreIsolatedByPluginAndPlayerAndSurviveReload() {
        systems.writeData("shop", scope(), "profile.with.dots", "{\"coins\":42,\"claimed\":false}");
        var reloaded = new SystemsAPI(plugin, new FileAPI(plugin), helper, scheduler);
        assertEquals("{\"coins\":42,\"claimed\":false}", reloaded.readData("shop", scope(), "profile.with.dots", "null"));
        assertEquals("null", reloaded.readData("other", scope(), "profile.with.dots", "null"));
        assertEquals("null", reloaded.readData("shop", "player:other", "profile.with.dots", "null"));
        systems.changeNumber("shop", scope(), "coins", 12);
        assertEquals(15, reloaded.changeNumber("shop", scope(), "coins", 3));
        systems.deleteData("shop", scope(), "coins");
        assertEquals(0, balance());
        assertThrows(IllegalArgumentException.class, () -> systems.changeNumber("shop", scope(), "coins", Double.NaN));
    }

    @Test
    void cooldownPersistsAndCannotBeClaimedTwice() {
        assertTrue(systems.claimCooldown("kit", scope(), "daily", 86400));
        var reloaded = new SystemsAPI(plugin, new FileAPI(plugin), helper, scheduler);
        assertFalse(reloaded.claimCooldown("kit", scope(), "daily", 86400));
        assertTrue(reloaded.cooldownRemaining("kit", scope(), "daily") > 86390);
        assertTrue(reloaded.claimCooldown("kit", "player:other", "daily", 86400));
        assertThrows(IllegalArgumentException.class, () -> systems.claimCooldown("kit", scope(), "daily", -1));
    }

    @Test
    void purchasesCheckFundsAndCapacityAndDebitOnlySuccessfulRewards() {
        var diamond = item(Material.DIAMOND, 1);
        assertEquals("funds", systems.purchase("shop", player, "coins", 10, diamond));
        verify(inventory, never()).addItem(any(ItemStack[].class));
        systems.writeData("shop", scope(), "coins", "20");
        var full = item(Material.DIRT, 64);
        when(inventory.getStorageContents()).thenReturn(new ItemStack[]{full});
        assertEquals("space", systems.purchase("shop", player, "coins", 10, diamond));
        assertEquals(20, balance());
        when(inventory.getStorageContents()).thenReturn(new ItemStack[36]);
        assertEquals("ok", systems.purchase("shop", player, "coins", 10, diamond));
        assertEquals(10, balance());
        var leftovers = new HashMap<Integer, ItemStack>();
        leftovers.put(0, diamond);
        when(inventory.addItem(any(ItemStack[].class))).thenReturn(leftovers);
        assertEquals("space", systems.purchase("shop", player, "coins", 10, diamond));
        assertEquals(10, balance());
        verify(inventory).setStorageContents(any(ItemStack[].class));
    }

    @Test
    void kitRollsBackPartialItemsAndLeavesCooldownAvailable() {
        var bread = item(Material.BREAD, 16);
        var leftovers = new HashMap<Integer, ItemStack>();
        leftovers.put(0, bread);
        when(inventory.addItem(any(ItemStack[].class))).thenReturn(leftovers);
        assertEquals("space", systems.grantKit("kit", player, "daily", 86400, List.of(bread)));
        assertEquals(0, systems.cooldownRemaining("kit", scope(), "daily"));
        verify(inventory).setStorageContents(any(ItemStack[].class));
        when(inventory.addItem(any(ItemStack[].class))).thenReturn(new HashMap<>());
        assertEquals("ok", systems.grantKit("kit", player, "daily", 86400, List.of(bread)));
        assertEquals("cooldown", systems.grantKit("kit", player, "daily", 86400, List.of(bread)));
        verify(inventory, times(2)).addItem(any(ItemStack[].class));
    }

    @Test
    void failedKitPersistenceRestoresTheInventory() {
        var failingFiles = spy(files);
        doThrow(new IllegalStateException("Disk is full")).when(failingFiles).setPluginConfigValue(anyString(), anyString(), any());
        var failingSystems = new SystemsAPI(plugin, failingFiles, helper, scheduler);
        assertThrows(IllegalStateException.class, () -> failingSystems.grantKit("kit", player, "daily", 86400, List.of(item(Material.BREAD, 16))));
        verify(inventory).setStorageContents(any(ItemStack[].class));
    }

    @Test
    void observationEventsUseMonitorPriorityAndIgnoreCancelledEvents() throws Exception {
        var callback = mock(Function.class);
        systems.registerEvent("block.BlockBreakEvent", true, callback);
        var executor = ArgumentCaptor.forClass(EventExecutor.class);
        verify(manager).registerEvent(eq(BlockBreakEvent.class), eq(systems), eq(EventPriority.MONITOR), executor.capture(), eq(plugin), eq(true));
        var event = mock(BlockBreakEvent.class);
        when(event.getPlayer()).thenReturn(player);
        assertSame(player, systems.eventPlayer(event));
        executor.getValue().execute(systems, event);
        verify(helper).executeFunction(callback, event);
        systems.close();
        clearInvocations(helper);
        executor.getValue().execute(systems, event);
        verifyNoInteractions(helper);
    }

    @Test
    void menuEventsOnlyAffectOwnedInventoriesAndCallbacksStopOnUnload() {
        var top = mock(Inventory.class);
        when(top.getSize()).thenReturn(27);
        when(server.createInventory(isNull(), eq(27), any(Component.class))).thenReturn(top);
        var queued = new ArrayList<Runnable>();
        when(scheduler.runOwnedTask(any())).thenAnswer(call -> { queued.add(call.getArgument(0)); return mock(BukkitTask.class); });
        systems.registerEvent("block.BlockBreakEvent", true, mock(Function.class));
        var menu = systems.createMenu("Shop", 3);
        verify(manager).registerEvents(systems, plugin);
        var callback = mock(Function.class);
        menu.button(11, item(Material.DIAMOND, 1), callback);
        menu.open(player);
        var view = mock(InventoryView.class);
        when(view.getTopInventory()).thenReturn(top);
        var click = mock(InventoryClickEvent.class);
        when(click.getView()).thenReturn(view);
        when(click.getWhoClicked()).thenReturn(player);
        when(click.getRawSlot()).thenReturn(11);
        systems.onClick(click);
        verify(click).setCancelled(true);
        verifyNoInteractions(helper);
        queued.removeFirst().run();
        verify(helper).executeFunction(callback, player, click);
        clearInvocations(helper);
        when(click.getRawSlot()).thenReturn(40);
        systems.onClick(click);
        assertTrue(queued.isEmpty());
        var drag = mock(InventoryDragEvent.class);
        when(drag.getView()).thenReturn(view);
        systems.onDrag(drag);
        verify(drag).setCancelled(true);
        var other = new SystemsAPI(plugin, files, helper, scheduler);
        clearInvocations(click);
        other.onClick(click);
        verify(click, never()).setCancelled(anyBoolean());
        when(click.getRawSlot()).thenReturn(11);
        systems.onClick(click);
        systems.close();
        queued.removeFirst().run();
        verifyNoInteractions(helper);
    }

    @Test
    void closingMenuDiscardsQueuedClicks() {
        var top = mock(Inventory.class);
        when(top.getSize()).thenReturn(27);
        when(server.createInventory(isNull(), eq(27), any(Component.class))).thenReturn(top);
        var callback = mock(Function.class);
        var menu = systems.createMenu("Shop", 3);
        menu.button(0, item(Material.DIAMOND, 1), callback);
        menu.open(player);
        var view = mock(InventoryView.class);
        when(view.getTopInventory()).thenReturn(top);
        var click = mock(InventoryClickEvent.class);
        when(click.getView()).thenReturn(view);
        systems.onClick(click);
        var task = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).runOwnedTask(task.capture());
        var close = mock(InventoryCloseEvent.class);
        when(close.getInventory()).thenReturn(top);
        systems.onClose(close);
        task.getValue().run();
        verifyNoInteractions(helper);
    }

    @Test
    void locationRecordsKeepWorldCoordinatesAndFacing() {
        var world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(server.getWorld("world")).thenReturn(world);
        var original = new Location(world, 12.25, 70, -40.75, 90, 15);
        var restored = systems.locationFromJson(systems.locationToJson(original));
        assertEquals(original, restored);
        when(server.getWorld("world")).thenReturn(null);
        assertNull(systems.locationFromJson(systems.locationToJson(original)));
    }

    @Test
    void rhinoAcceptsNativeItemListsAndJavaEventArguments() {
        try (var context = Context.enter()) {
            context.setLanguageVersion(Context.VERSION_ES6);
            context.setOptimizationLevel(-1);
            var scope = context.initStandardObjects();
            ScriptableObject.putProperty(scope, "systems", Context.javaToJS(systems, scope));
            ScriptableObject.putProperty(scope, "player", Context.javaToJS(player, scope));
            ScriptableObject.putProperty(scope, "item", Context.javaToJS(item(Material.BREAD, 16), scope));
            assertEquals("ok", Context.toString(context.evaluateString(scope, "systems.grantKit('kit', player, 'daily', 86400, [item])", "test", 1, null)));
            var realHelper = new APIHelper();
            realHelper.setScope(scope);
            var callback = (Function) context.evaluateString(scope, "(function(event) { systems.writeData('kit', 'global', 'player', JSON.stringify(String(event.getPlayer().getUniqueId()))); })", "test", 1, null);
            var event = mock(BlockBreakEvent.class);
            when(event.getPlayer()).thenReturn(player);
            realHelper.executeFunction(callback, event);
            assertEquals("\"" + player.getUniqueId() + "\"", systems.readData("kit", "global", "player", "null"));
        }
    }

    @Test
    void generatedTemplatesCompileWithTheServerRhinoVersion() throws Exception {
        try (var context = Context.enter()) {
            context.setLanguageVersion(Context.VERSION_ES6);
            context.setOptimizationLevel(-1);
            for (String name : List.of("shop", "kit", "quest", "homes", "vaultshop")) {
                try (var source = getClass().getResourceAsStream("/dashboard-systems/" + name + ".js")) {
                    assertNotNull(source, "Run npm run build to update system fixtures");
                    assertNotNull(context.compileString(new String(source.readAllBytes(), StandardCharsets.UTF_8), name, 1, null));
                }
            }
        }
    }
}
