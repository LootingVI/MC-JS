package de.flori.mCJS.dashboard;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class PluginWorkspaceTest {
    @TempDir Path directory;

    @Test
    void savesRoundTripBlocksAndRejectsStaleWrites() throws Exception {
        var workspace = new PluginWorkspace(directory);
        var blocks = new JsonObject();
        blocks.add("blocks", new JsonObject());
        var saved = workspace.save("welcome", "function onEnable() {}", "", blocks);
        assertEquals("blocks", saved.get("mode").getAsString());
        assertEquals(blocks, workspace.read("welcome").get("workspace"));
        assertEquals(1, workspace.list().size());
        assertEquals(409, assertThrows(DashboardException.class,
                () -> workspace.save("welcome", "var changed = 1;", "", null)).status());
        String revision = saved.get("revision").getAsString();
        var code = workspace.save("welcome", "var changed = 1;", revision, null);
        assertEquals("code", code.get("mode").getAsString());
        assertThrows(DashboardException.class, () -> workspace.delete("welcome", revision));
        workspace.delete("welcome", code.get("revision").getAsString());
        assertTrue(workspace.list().isEmpty());
    }

    @Test
    void syntaxErrorsNeverOverwriteSavedCodeOrExecuteIt() throws Exception {
        var workspace = new PluginWorkspace(directory);
        var saved = workspace.save("test", "throw new Error('must never execute during save');", "", null);
        String revision = saved.get("revision").getAsString();
        assertEquals(422, assertThrows(DashboardException.class,
                () -> workspace.save("test", "function onEnable( {", revision, null)).status());
        assertEquals(revision, workspace.read("test").get("revision").getAsString());
    }

    @Test
    void rejectsTraversalReservedNamesAndOversizedSources() throws Exception {
        var workspace = new PluginWorkspace(directory);
        for (String name : new String[]{"../config", "/tmp/payload", "a/b", "a\\b", "a.js", "", "CON", "a:b"}) {
            assertThrows(DashboardException.class, () -> workspace.save(name, "var x = 1;", "", null));
        }
        assertEquals(413, assertThrows(DashboardException.class,
                () -> PluginWorkspace.validate(" ".repeat(PluginWorkspace.MAX_SOURCE_BYTES) + "1;")).status());
        assertEquals(422, assertThrows(DashboardException.class, () -> PluginWorkspace.validate("  ")).status());
    }

    @Test
    void externallyEditedSourceCannotResurrectStaleBlocks() throws Exception {
        var workspace = new PluginWorkspace(directory);
        var saved = workspace.save("test", "var x = 1;", "", new JsonObject());
        Files.writeString(directory.resolve("test.js"), "var x = 2;");
        assertEquals("code", workspace.read("test").get("mode").getAsString());
        assertThrows(DashboardException.class, () -> workspace.save("test", "var x = 3;", saved.get("revision").getAsString(), null));
    }
}
