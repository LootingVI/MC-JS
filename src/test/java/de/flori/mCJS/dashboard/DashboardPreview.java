package de.flori.mCJS.dashboard;

import com.google.gson.JsonObject;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

public final class DashboardPreview {
    public static void main(String[] args) throws Exception {
        UUID player = UUID.randomUUID();
        var sessions = new DashboardSessions(Duration.ofMinutes(15), Duration.ofMinutes(30));
        var workspace = new PluginWorkspace(Path.of(".tools", "preview-plugins"));
        Set<String> running = new HashSet<>();
        try (var server = new DashboardServer(new InetSocketAddress("127.0.0.1", 18765), URI.create("http://localhost:18765"),
                sessions, workspace, new DashboardAccess() {
            public <T> T call(Callable<T> action) throws Exception { return action.call(); }
            public void authorize(UUID id) {}
            public JsonObject describe(String name) { var result = new JsonObject(); result.addProperty("running", running.contains(name)); return result; }
            public void run(String name) { running.add(name); }
            public void stop(String name) { running.remove(name); }
            public void audit(UUID id, String action, String name) { System.out.println(action + " " + name); }
        })) {
            server.start();
            System.out.println("TEST PREVIEW: Minecraft execution is simulated.");
            System.out.println(server.link(sessions.issue(player, "PreviewBuilder")));
            var scanner = new Scanner(System.in);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.equals("exit")) break;
                if (line.equals("link")) System.out.println(server.link(sessions.issue(player, "PreviewBuilder")));
                else System.out.println("Verified: " + sessions.verify(player, line.replace("/mjs dashboard ", "")));
            }
        }
    }
}
