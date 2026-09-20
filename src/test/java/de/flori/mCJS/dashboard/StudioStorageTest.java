package de.flori.mCJS.dashboard;

import com.google.gson.JsonParser;
import de.flori.mCJS.api.DebugAPI;
import de.flori.mCJS.api.FileAPI;
import de.flori.mCJS.api.SystemsAPI;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudioStorageTest {
    @TempDir Path directory;
    @Test void tracesAreOptInBoundedIncrementalAndTiedToSourceRevision() {
        var debug = new DebugAPI(); debug.step("ignored", "{}"); assertTrue(debug.read(0).getAsJsonArray("events").isEmpty());
        debug.enable(true); debug.generation("revision-one");
        for (int i = 0; i < 350; i++) debug.step("block-" + i, "{\"score\":" + i + "}");
        var result = debug.read(1); assertEquals(300, result.getAsJsonArray("events").size()); assertTrue(result.get("dropped").getAsBoolean());
        debug.error("broken", "Error message", "{\"score\":350}");
        assertEquals("broken", debug.read(350).getAsJsonArray("events").get(0).getAsJsonObject().get("blockId").getAsString());
        debug.generation("revision-two"); assertTrue(debug.read(0).getAsJsonArray("events").isEmpty());
        debug.step("new", "invalid json"); assertEquals("revision-two", debug.read(0).get("revision").getAsString());
        debug.enable(false); debug.step("ignored", "{}"); assertEquals(352, debug.read(0).get("cursor").getAsLong());
    }
    @Test void sharedLibraryUsesRevisionsAndCannotCollideWithAPluginNamedLibrary() throws Exception {
        var workspace = new PluginWorkspace(directory);
        var project = JsonParser.parseString("{\"blocks\":{\"blocks\":[]}}");
        workspace.save("library", "function onEnable() {}", "", project);
        String revision = workspace.library().get("revision").getAsString();
        var entries = JsonParser.parseString("[{\"id\":\"menu\",\"name\":\"Shared menu\",\"blocks\":[{\"type\":\"mjs_function\"}]}]");
        workspace.saveLibrary(entries, revision);
        assertEquals(project, workspace.read("library").get("workspace"));
        assertEquals(1, new PluginWorkspace(directory).library().getAsJsonArray("entries").size());
        assertEquals(409, assertThrows(DashboardException.class, () -> workspace.saveLibrary(entries, revision)).status());
        String latest = workspace.library().get("revision").getAsString();
        assertEquals(400, assertThrows(DashboardException.class, () -> workspace.saveLibrary(JsonParser.parseString("[{}]"), latest)).status());
        assertEquals(400, assertThrows(DashboardException.class, () -> workspace.saveLibrary(JsonParser.parseString("[1]"), latest)).status());
    }
    @Test void inspectorDecodesPlayerKeysSearchesValuesAndPaginatesWithoutChangingData() {
        var plugin = mock(JavaPlugin.class); when(plugin.getDataFolder()).thenReturn(directory.toFile()); when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        var files = new FileAPI(plugin); var systems = new SystemsAPI(plugin, files, null, null);
        systems.writeData("game", "player:alex", "quest.progress", "{\"mined\":7}");
        systems.claimCooldown("game", "player:alex", "daily", 300);
        for (int i = 0; i < 102; i++) systems.writeData("game", "global", "counter" + i, String.valueOf(i));
        var found = files.inspectSystemData("game", "alex", "mined", 0); assertEquals(1, found.get("total").getAsInt());
        var row = found.getAsJsonArray("rows").get(0).getAsJsonObject(); assertEquals("quest.progress", row.get("key").getAsString()); assertEquals(7, row.getAsJsonObject("value").get("mined").getAsInt());
        var cooldown = files.inspectSystemData("game", "cooldowns:", "daily", 0).getAsJsonArray("rows").get(0).getAsJsonObject();
        assertTrue(cooldown.get("remainingSeconds").getAsInt() > 290);
        assertEquals(100, files.inspectSystemData("game", "global", "", 0).getAsJsonArray("rows").size());
        assertEquals(2, files.inspectSystemData("game", "global", "", 100).getAsJsonArray("rows").size());
        assertEquals("{\"mined\":7}", systems.readData("game", "player:alex", "quest.progress", "null"));
        assertEquals(0, files.inspectSystemData("other", "", "", 0).get("total").getAsInt());
    }
}
