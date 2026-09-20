package de.flori.mCJS.dashboard;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import static org.junit.jupiter.api.Assertions.*;

class DashboardServerTest {
    @TempDir Path directory;
    DashboardSessions sessions;
    DashboardServer server;
    URI origin;
    UUID player = UUID.randomUUID();
    String cookie, csrf;
    boolean allowed = true;
    Set<String> running = new HashSet<>();
    HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    @BeforeEach
    void start() throws Exception {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) { port = socket.getLocalPort(); }
        origin = URI.create("http://127.0.0.1:" + port);
        sessions = new DashboardSessions(Duration.ofMinutes(5), Duration.ofMinutes(30));
        server = new DashboardServer(new InetSocketAddress("127.0.0.1", port), origin, sessions,
                new PluginWorkspace(directory), new DashboardAccess() {
            public <T> T call(Callable<T> action) throws Exception { return action.call(); }
            public void authorize(UUID uuid) { if (!allowed) throw new DashboardException(403, "Permission removed"); }
            public JsonObject describe(String name) { var result = new JsonObject(); result.addProperty("running", running.contains(name)); return result; }
            public void run(String name) { running.add(name); }
            public void stop(String name) { running.remove(name); }
            public void audit(UUID uuid, String action, String name) {}
        });
        server.start();
    }

    @AfterEach
    void stop() { server.close(); }

    HttpResponse<String> request(String path, String method, String data, String requestOrigin, String requestCsrf) throws Exception {
        var builder = HttpRequest.newBuilder(origin.resolve(path)).timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json");
        if (cookie != null) builder.header("Cookie", cookie);
        if (requestOrigin != null) builder.header("Origin", requestOrigin);
        if (requestCsrf != null) builder.header("X-MCJS-CSRF", requestCsrf);
        return client.send(builder.method(method, data == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(data)).build(), HttpResponse.BodyHandlers.ofString());
    }

    HttpResponse<String> request(String path, String method, String data) throws Exception {
        return request(path, method, data, origin.toString(), csrf);
    }

    JsonObject login(boolean verify) throws Exception {
        String token = sessions.issue(player, "Builder");
        var response = request("/api/session", "POST", "{\"token\":\"" + token + "\"}");
        assertEquals(200, response.statusCode());
        String setCookie = response.headers().firstValue("Set-Cookie").orElseThrow();
        assertTrue(setCookie.contains("HttpOnly"));
        assertTrue(setCookie.contains("SameSite=Strict"));
        cookie = setCookie.split(";", 2)[0];
        var data = JsonParser.parseString(response.body()).getAsJsonObject();
        csrf = data.get("csrf").getAsString();
        if (verify) assertTrue(sessions.verify(player, data.get("command").getAsString().substring("/mjs dashboard ".length())));
        return data;
    }

    @Test
    void pendingBrowserCannotReadOrMutatePluginsAndTokensCannotBeReused() throws Exception {
        assertEquals(401, request("/api/plugins", "GET", null).statusCode());
        var pending = login(false);
        assertFalse(pending.get("verified").getAsBoolean());
        assertEquals(403, request("/api/plugins", "GET", null).statusCode());
        assertEquals(403, request("/api/plugin", "PUT", "{\"name\":\"x\",\"source\":\"var x=1;\",\"revision\":\"\"}").statusCode());
        assertEquals(403, request("/api/run", "POST", "{}").statusCode());
        assertEquals(200, request("/api/session", "GET", null).statusCode());
        assertEquals(401, request("/api/session", "POST", "{\"token\":\"invalid\"}").statusCode());
    }

    @Test
    void verifiedWorkflowCreatesValidatesStartsStopsAndDeletes() throws Exception {
        login(true);
        assertEquals(200, request("/api/plugins", "GET", null).statusCode());
        var saved = request("/api/plugin", "PUT", "{\"name\":\"welcome\",\"source\":\"function onEnable() {}\",\"revision\":\"\"}");
        assertEquals(200, saved.statusCode(), saved.body());
        String revision = JsonParser.parseString(saved.body()).getAsJsonObject().get("revision").getAsString();
        String body = "{\"name\":\"welcome\",\"revision\":\"" + revision + "\"}";
        assertEquals(200, request("/api/plugin?name=welcome", "GET", null).statusCode());
        assertEquals(422, request("/api/validate", "POST", "{\"source\":\"function (\"}").statusCode());
        assertEquals(200, request("/api/run", "POST", body).statusCode());
        assertTrue(running.contains("welcome"));
        assertEquals(200, request("/api/stop", "POST", body).statusCode());
        assertFalse(running.contains("welcome"));
        assertEquals(200, request("/api/plugin", "DELETE", body).statusCode());
        assertEquals(404, request("/api/plugin?name=welcome", "GET", null).statusCode());
    }

    @Test
    void csrfForeignOriginsAndPathTraversalAreRejected() throws Exception {
        login(true);
        assertEquals(403, request("/api/validate", "POST", "{\"source\":\"1;\"}", origin.toString(), null).statusCode());
        assertEquals(403, request("/api/plugins", "GET", null, "https://evil.example", csrf).statusCode());
        assertEquals(400, request("/api/plugin?name=..%2Fconfig", "GET", null).statusCode());
        assertEquals(404, request("/config.yml", "GET", null).statusCode());
        assertEquals(400, request("/api/plugin", "PUT", "{broken").statusCode());
        assertEquals(413, request("/api/plugin", "PUT", "x".repeat(2 * 1024 * 1024 + 1)).statusCode());
        assertEquals(200, request("/api/logout", "POST", "{}").statusCode());
        assertEquals(401, request("/api/plugins", "GET", null).statusCode());
    }

    @Test
    void revokedPermissionsInvalidateAnAlreadyVerifiedBrowser() throws Exception {
        login(true);
        allowed = false;
        assertEquals(403, request("/api/plugins", "GET", null).statusCode());
        allowed = true;
        assertEquals(401, request("/api/plugins", "GET", null).statusCode());
    }

    @Test
    void staticDashboardIsPackagedWithRestrictiveHeaders() throws Exception {
        var page = request("/", "GET", null);
        assertEquals(200, page.statusCode());
        assertTrue(page.body().contains("Plugin Studio"));
        assertEquals("no-store", page.headers().firstValue("Cache-Control").orElseThrow());
        assertEquals("no-referrer", page.headers().firstValue("Referrer-Policy").orElseThrow());
        assertTrue(page.headers().firstValue("Content-Security-Policy").orElseThrow().contains("frame-ancestors 'none'"));
        assertEquals(200, request("/app.js", "GET", null).statusCode());
    }
}
