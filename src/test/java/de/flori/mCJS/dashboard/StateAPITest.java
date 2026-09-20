package de.flori.mCJS.dashboard;

import de.flori.mCJS.api.APIHelper;
import de.flori.mCJS.api.SchedulerAPI;
import de.flori.mCJS.api.StateAPI;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptableObject;
import org.mockito.ArgumentCaptor;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StateAPITest {
    JavaPlugin plugin;
    APIHelper helper;
    SchedulerAPI scheduler;
    StateAPI states;
    Player player;
    Function enter, exit, running, guard;
    @BeforeEach void setup() {
        plugin = mock(JavaPlugin.class); var server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server); when(server.getPluginManager()).thenReturn(mock(PluginManager.class));
        when(server.isPrimaryThread()).thenReturn(true);
        helper = mock(APIHelper.class); scheduler = mock(SchedulerAPI.class); player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID()); when(player.isOnline()).thenReturn(true);
        enter = mock(Function.class); exit = mock(Function.class); running = mock(Function.class); guard = mock(Function.class);
        states = new StateAPI(plugin, helper, scheduler);
    }
    StateAPI.Machine machine() {
        var machine = states.create("game", "Lobby"); machine.state("Lobby", enter, exit); machine.state("Running", running, null);
        machine.transition("Lobby", "next", "Running", guard); return machine;
    }
    @Test void playerAndGlobalInstancesAreIndependentAndGuardsRunBeforeExit() {
        machine(); states.start("game", player, "initial"); states.start("game", null, null);
        when(helper.callFunction(guard, player, "go")).thenReturn(false);
        assertFalse(states.send("game", player, "next", "go")); verify(helper, never()).executeFunction(exit, player, "go");
        when(helper.callFunction(guard, player, "go")).thenReturn(true);
        assertTrue(states.send("game", player, "next", "go"));
        var order = inOrder(helper); order.verify(helper).executeFunction(exit, player, "go"); order.verify(helper).executeFunction(running, player, "go");
        assertEquals("Running", states.current("game", player)); assertEquals("Lobby", states.current("game", null));
        assertThrows(IllegalStateException.class, () -> states.start("game", player, null));
    }
    @Test void transitionsInvalidateTimersAndReleaseSchedulerOwnership() {
        machine(); states.start("game", player, null);
        var task = mock(BukkitTask.class); when(scheduler.runOwnedTaskLater(eq(200L), any())).thenReturn(task);
        states.after("game", player, 10, "next", null);
        var action = ArgumentCaptor.forClass(Runnable.class); verify(scheduler).runOwnedTaskLater(eq(200L), action.capture());
        when(helper.callFunction(guard, player, (Object) null)).thenReturn(true);
        states.send("game", player, "next", null); verify(scheduler).cancelTask(task);
        clearInvocations(helper); action.getValue().run(); verifyNoInteractions(helper);
        states.stop("game", player, null); assertEquals("", states.current("game", player));
    }
    @Test void shutdownCancelsTimersAndPreventsStaleCallbacks() {
        machine(); states.start("game", null, null);
        var task = mock(BukkitTask.class); when(scheduler.runOwnedTaskLater(anyLong(), any())).thenReturn(task);
        states.after("game", null, 0, "next", null);
        var action = ArgumentCaptor.forClass(Runnable.class); verify(scheduler).runOwnedTaskLater(eq(1L), action.capture());
        states.close(); verify(scheduler).cancelTask(task); clearInvocations(helper);
        action.getValue().run(); verifyNoInteractions(helper);
        assertThrows(IllegalStateException.class, () -> states.start("game", null, null));
    }
    @Test void invalidGraphsAndRecursiveGuardTransitionsAreRejected() {
        var invalid = states.create("invalid", "Missing"); invalid.state("Lobby", null, null);
        assertThrows(IllegalStateException.class, () -> states.start("invalid", null, null));
        machine(); states.start("game", player, null);
        when(helper.callFunction(guard, player, (Object) null)).thenAnswer(call -> states.send("game", player, "next", null));
        assertThrows(IllegalStateException.class, () -> states.send("game", player, "next", null));
        assertEquals("Lobby", states.current("game", player));
        assertThrows(IllegalArgumentException.class, () -> states.after("game", player, Double.NaN, "next", null));
    }
    @Test void rhinoCallbacksCanPassDataAndImmediatelyEnterTheNextState() {
        try (var context = Context.enter()) {
            context.setOptimizationLevel(-1); context.setLanguageVersion(Context.VERSION_ES6);
            var scope = context.initStandardObjects(); var realHelper = new APIHelper(); realHelper.setScope(scope);
            var actual = new StateAPI(plugin, realHelper, scheduler);
            ScriptableObject.putProperty(scope, "states", Context.javaToJS(actual, scope));
            Object value = context.evaluateString(scope, "var seen = []; var machine = states.create('test', 'Lobby'); machine.state('Lobby', function(player,data) { states.send('test', player, 'go', data); }, null); machine.state('Running', function(player,data) { seen.push(data.score); }, null); machine.transition('Lobby', 'go', 'Running', function(player,data) { return data.score === 7; }); states.start('test', null, {score:7}); seen[0] + ':' + states.current('test', null);", "test", 1, null);
            assertEquals("7:Running", Context.toString(value)); actual.close();
        }
    }
}
