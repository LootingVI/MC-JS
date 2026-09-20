package de.flori.mCJS.dashboard;

import de.flori.mCJS.api.APIHelper;
import de.flori.mCJS.api.FileAPI;
import de.flori.mCJS.api.SchedulerAPI;
import de.flori.mCJS.JSPluginManager;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mockito.ArgumentCaptor;
import java.nio.file.Path;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PluginRuntimeTest {
    @TempDir Path directory;

    @Test
    void failedOnEnableDoesNotLeavePluginLoadedOrKeepItsTimer() throws Exception {
        var plugin = mock(JavaPlugin.class);
        var server = mock(Server.class);
        var scheduler = mock(BukkitScheduler.class);
        var handle = mock(BukkitTask.class);
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("MCJS-Test"));
        when(server.getScheduler()).thenReturn(scheduler);
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), eq(0L), eq(20L))).thenReturn(handle);
        Files.createDirectories(directory.resolve("js-plugins"));
        Files.writeString(directory.resolve("js-plugins/broken.js"),
                "function onEnable() { api.runTaskTimer(0, 20, function() {}); throw new Error('broken startup'); }");
        var manager = new JSPluginManager(plugin);
        var error = assertThrows(java.io.IOException.class, () -> manager.activatePlugin("broken"));
        assertTrue(error.getMessage().contains("broken startup"), error.getMessage());
        assertTrue(manager.getLoadedPlugins().isEmpty());
        verify(handle).cancel();
    }

    @Test
    void stoppingOnePluginCancelsItsTimerButNotOtherPlugins() {
        var plugin = mock(JavaPlugin.class);
        var server = mock(Server.class);
        var scheduler = mock(BukkitScheduler.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(scheduler);
        var firstHandle = mock(BukkitTask.class);
        var secondHandle = mock(BukkitTask.class);
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), eq(0L), eq(20L))).thenReturn(firstHandle, secondHandle);
        var helper = mock(APIHelper.class);
        var first = new SchedulerAPI(plugin, helper);
        var second = new SchedulerAPI(plugin, helper);
        var callback = mock(Function.class);
        first.runTaskTimer(0, 20, callback);
        second.runTaskTimer(0, 20, callback);
        var captured = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler, times(2)).runTaskTimer(eq(plugin), captured.capture(), eq(0L), eq(20L));
        first.cancelAll();
        verify(firstHandle).cancel();
        verify(secondHandle, never()).cancel();
        captured.getAllValues().get(0).run();
        verifyNoInteractions(helper);
        captured.getAllValues().get(1).run();
        verify(helper).executeFunction(callback);
        assertThrows(IllegalStateException.class, () -> first.runTaskTimer(0, 20, callback));
    }

    @Test
    void finishedOneShotTasksAreNotRetainedForLaterCancellation() {
        var plugin = mock(JavaPlugin.class);
        var server = mock(Server.class);
        var scheduler = mock(BukkitScheduler.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(scheduler);
        var handle = mock(BukkitTask.class);
        when(scheduler.runTask(eq(plugin), any(Runnable.class))).thenReturn(handle);
        var api = new SchedulerAPI(plugin, mock(APIHelper.class));
        api.runTask(mock(Function.class));
        var captured = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).runTask(eq(plugin), captured.capture());
        captured.getValue().run();
        api.cancelAll();
        verify(handle, never()).cancel();
    }

    @Test
    void rhinoBlockValuesPersistAcrossPluginInstances() {
        var plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        var api = new FileAPI(plugin);
        try (Context context = Context.enter()) {
            context.setLanguageVersion(Context.VERSION_ES6);
            var scope = context.initStandardObjects();
            Object array = context.evaluateString(scope, "[0, false, 'text', {nested: 42}]", "test", 1, null);
            api.setPluginConfigValue("demo", "saved", array);
        }
        var reloaded = new FileAPI(plugin);
        Object value = reloaded.getPluginConfigValue("demo", "saved", "missing");
        assertInstanceOf(java.util.List.class, value);
        var list = (java.util.List<?>) value;
        assertEquals(0.0, ((Number) list.get(0)).doubleValue());
        assertEquals(false, list.get(1));
        assertEquals("text", list.get(2));
        assertEquals(42.0, ((Number) ((java.util.Map<?, ?>) list.get(3)).get("nested")).doubleValue());
        assertEquals("fallback", reloaded.getPluginConfigValue("demo", "unknown", "fallback"));
        assertThrows(DashboardException.class, () -> reloaded.setPluginConfigValue("../config", "x", 1));
    }
}
