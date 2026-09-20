package de.flori.mCJS.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayDeque;

public final class DebugAPI {
    private final ArrayDeque<JsonObject> entries = new ArrayDeque<>();
    private long sequence;
    private volatile long until;
    private String revision = "";

    public boolean isEnabled() { return System.currentTimeMillis() < until; }

    public synchronized void enable(boolean enabled) { until = enabled ? System.currentTimeMillis() + 300_000 : 0; }

    public synchronized void generation(String revision) { this.revision = revision; entries.clear(); }

    public synchronized void step(String blockId, String values) {
        if (!isEnabled()) return;
        append("step", blockId, "", values);
    }

    public synchronized void error(String blockId, String message, String values) {
        if (!isEnabled()) return;
        append("error", blockId, message, values);
    }

    private void append(String type, String blockId, String message, String values) {
        var entry = new JsonObject();
        entry.addProperty("sequence", ++sequence);
        entry.addProperty("time", System.currentTimeMillis());
        entry.addProperty("type", type);
        entry.addProperty("blockId", cut(blockId, 128));
        entry.addProperty("message", cut(message, 2000));
        if (values != null && values.length() <= 8192) {
            try { entry.add("values", JsonParser.parseString(values)); }
            catch (RuntimeException ignored) { entry.addProperty("values", "Snapshot unavailable"); }
        }
        entries.addLast(entry);
        while (entries.size() > 300) entries.removeFirst();
    }

    private String cut(String text, int max) { return text == null ? "" : text.substring(0, Math.min(text.length(), max)); }

    public synchronized JsonObject read(long after) {
        var result = new JsonObject();
        result.addProperty("enabled", isEnabled());
        result.addProperty("expiresAt", until);
        result.addProperty("revision", revision);
        result.addProperty("cursor", sequence);
        result.addProperty("dropped", !entries.isEmpty() && after > 0 && after < entries.getFirst().get("sequence").getAsLong() - 1);
        var events = new JsonArray();
        for (var entry : entries) if (entry.get("sequence").getAsLong() > after) events.add(entry.deepCopy());
        result.add("events", events);
        return result;
    }
}
