package de.flori.mCJS.dashboard;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

public final class PluginWorkspace {
    public static final int MAX_SOURCE_BYTES = 512 * 1024;
    private final Path root;
    private final Path projects;

    public PluginWorkspace(Path root) throws IOException {
        Files.createDirectories(root);
        this.root = root.toRealPath();
        this.projects = this.root.resolve(".dashboard");
        if (Files.isSymbolicLink(projects)) {
            throw new IOException("Dashboard workspace must not be a symbolic link");
        }
        Files.createDirectories(projects);
    }

    public synchronized List<String> list() throws IOException {
        try (var paths = Files.list(root)) {
            return paths.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,63}\\.js"))
                    .map(name -> name.substring(0, name.length() - 3)).sorted().toList();
        }
    }

    public synchronized JsonObject library() throws IOException {
        Path file = projects.resolve(".library.json");
        if (Files.isSymbolicLink(projects) || Files.isSymbolicLink(file)) throw new DashboardException(400, "Library must not be a symbolic link.");
        String source = Files.exists(file) ? readLimited(file, 1024 * 1024) : "[]";
        var result = new JsonObject();
        result.addProperty("revision", revision(source));
        result.add("entries", JsonParser.parseString(source));
        return result;
    }

    public synchronized JsonObject saveLibrary(JsonElement entries, String expected) throws IOException {
        if (!library().get("revision").getAsString().equals(expected)) throw new DashboardException(409, "Library changed. Refresh it before saving.");
        if (entries == null || !entries.isJsonArray() || entries.getAsJsonArray().size() > 100) throw new DashboardException(400, "Library supports up to 100 entries.");
        var names = new java.util.HashSet<String>();
        for (var item : entries.getAsJsonArray()) {
            if (!item.isJsonObject()) throw new DashboardException(400, "Invalid library entry.");
            var entry = item.getAsJsonObject();
            if (!entry.has("id") || !entry.get("id").isJsonPrimitive() || !entry.getAsJsonPrimitive("id").isString()
                    || !entry.has("name") || !entry.get("name").isJsonPrimitive() || !entry.getAsJsonPrimitive("name").isString()
                    || !entry.has("blocks") || !entry.get("blocks").isJsonArray()) throw new DashboardException(400, "Invalid library entry.");
            String id = entry.get("id").getAsString();
            validateName(id);
            if (!names.add(id) || entry.get("name").getAsString().isBlank() || entry.get("name").getAsString().length() > 80
                    || entry.getAsJsonArray("blocks").isEmpty() || entry.getAsJsonArray("blocks").size() > 200) throw new DashboardException(400, "Invalid library entry.");
        }
        String source = entries.toString();
        if (source.getBytes(StandardCharsets.UTF_8).length > 1024 * 1024) throw new DashboardException(413, "Library exceeds 1 MiB.");
        atomicWrite(projects.resolve(".library.json"), source);
        return library();
    }

    public synchronized JsonObject read(String name) throws IOException {
        Path file = sourcePath(name);
        if (!Files.exists(file)) throw new DashboardException(404, "Plugin not found.");
        String source = readLimited(file, MAX_SOURCE_BYTES);
        JsonObject result = new JsonObject();
        result.addProperty("name", name);
        result.addProperty("source", source);
        result.addProperty("revision", revision(source));
        result.addProperty("mode", "code");
        Path project = projectPath(name);
        if (Files.exists(project)) {
            try {
                JsonObject data = JsonParser.parseString(readLimited(project, 1024 * 1024)).getAsJsonObject();
                if (data.has("revision") && revision(source).equals(data.get("revision").getAsString())
                        && data.has("workspace") && data.get("workspace").isJsonObject()) {
                    result.addProperty("mode", "blocks");
                    result.add("workspace", data.get("workspace"));
                }
            } catch (IllegalStateException | com.google.gson.JsonParseException ignored) {
            }
        }
        return result;
    }

    public synchronized JsonObject save(String name, String source, String expectedRevision,
                                        JsonElement workspace) throws IOException {
        Path file = sourcePath(name);
        checkRevision(name, expectedRevision);
        validate(source);
        String revision = revision(source);
        Path project = projectPath(name);
        if (workspace != null && !workspace.isJsonNull()) {
            if (!workspace.isJsonObject()) throw new DashboardException(400, "Invalid block workspace data.");
            JsonObject data = new JsonObject();
            data.addProperty("revision", revision);
            data.add("workspace", workspace);
            if (data.toString().getBytes(StandardCharsets.UTF_8).length > 1024 * 1024) {
                throw new DashboardException(413, "The block project is too large.");
            }
            atomicWrite(project, data.toString());
        } else {
            Files.deleteIfExists(project);
        }
        atomicWrite(file, source);
        return read(name);
    }

    public synchronized void checkRevision(String name, String expected) throws IOException {
        Path path = sourcePath(name);
        String current = Files.exists(path) ? revision(readLimited(path, MAX_SOURCE_BYTES)) : "";
        if (expected == null || !DashboardSessions.equal(current, expected)) {
            throw new DashboardException(409, "This plugin has changed since you opened it. Export your draft and reload the plugin.");
        }
    }

    public synchronized void delete(String name, String expected) throws IOException {
        checkRevision(name, expected);
        Files.deleteIfExists(sourcePath(name));
        Files.deleteIfExists(projectPath(name));
    }

    public Path sourcePath(String name) {
        validateName(name);
        Path path = root.resolve(name + ".js");
        if (Files.isSymbolicLink(path)) throw new DashboardException(400, "Symbolic links are not allowed.");
        return path;
    }

    private Path projectPath(String name) {
        validateName(name);
        Path path = projects.resolve(name + ".json");
        if (Files.isSymbolicLink(projects) || Files.isSymbolicLink(path)) {
            throw new DashboardException(400, "Symbolic links are not allowed.");
        }
        return path;
    }

    public static void validateName(String name) {
        if (name == null || !name.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,63}")
                || name.matches("(?i)(CON|PRN|AUX|NUL|COM[0-9]|LPT[0-9])")) {
            throw new DashboardException(400, "Name: 1–64 letters, numbers, hyphens or underscores; reserved filenames are not allowed.");
        }
    }

    public static void validate(String source) {
        if (source == null || source.isBlank()) throw new DashboardException(422, "The plugin source is empty.");
        if (source.getBytes(StandardCharsets.UTF_8).length > MAX_SOURCE_BYTES) {
            throw new DashboardException(413, "Source code must not exceed 512 KiB.");
        }
        try (Context context = Context.enter()) {
            context.setOptimizationLevel(-1);
            context.setLanguageVersion(Context.VERSION_ES6);
            context.compileString(source, "dashboard.js", 1, null);
        } catch (EvaluatorException error) {
            throw new DashboardException(422, "Line " + error.lineNumber() + ": " + error.details());
        }
    }

    private String readLimited(Path path, int max) throws IOException {
        try (var input = Files.newInputStream(path)) {
            byte[] bytes = input.readNBytes(max + 1);
            if (bytes.length > max) throw new DashboardException(413, "This file is too large for the editor.");
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private void atomicWrite(Path destination, String contents) throws IOException {
        Path temp = Files.createTempFile(destination.getParent(), ".mcjs-", ".tmp");
        try {
            Files.writeString(temp, contents, StandardCharsets.UTF_8);
            try {
                Files.move(temp, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    public static String revision(String source) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
