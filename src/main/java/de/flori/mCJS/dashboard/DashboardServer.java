package de.flori.mCJS.dashboard;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class DashboardServer implements AutoCloseable {
    private static final Gson JSON = new Gson();
    private static final String COOKIE = "mcjs_session";
    private final DashboardSessions sessions;
    private final PluginWorkspace workspace;
    private final DashboardAccess access;
    private final URI publicUri;
    private final String origin;
    private final HttpServer server;
    private final ThreadPoolExecutor executor;

    public DashboardServer(InetSocketAddress address, URI publicUri, DashboardSessions sessions,
                           PluginWorkspace workspace, DashboardAccess access) throws IOException {
        if (!List.of("http", "https").contains(publicUri.getScheme()) || publicUri.getHost() == null
                || publicUri.getUserInfo() != null || publicUri.getQuery() != null || publicUri.getFragment() != null
                || !(publicUri.getPath().isEmpty() || publicUri.getPath().equals("/"))) {
            throw new IllegalArgumentException("dashboard.public-url must be an http(s) origin without a path, query or fragment");
        }
        this.sessions = sessions;
        this.workspace = workspace;
        this.access = access;
        this.publicUri = publicUri;
        this.origin = publicUri.getScheme() + "://" + publicUri.getRawAuthority();
        this.server = HttpServer.create(address, 32);
        this.executor = new ThreadPoolExecutor(4, 4, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(32), runnable -> {
            Thread thread = new Thread(runnable, "MCJS-Dashboard");
            thread.setDaemon(true);
            return thread;
        });
        server.setExecutor(executor);
        server.createContext("/", this::handle);
    }

    public void start() {
        server.start();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    public String link(String token) {
        return origin + "/#token=" + token;
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            headers(exchange);
            if (!publicUri.getRawAuthority().equalsIgnoreCase(exchange.getRequestHeaders().getFirst("Host"))) {
                throw new DashboardException(403, "Unknown host. Open the configured dashboard address.");
            }
            String requestOrigin = exchange.getRequestHeaders().getFirst("Origin");
            if (requestOrigin != null && !origin.equalsIgnoreCase(requestOrigin)) {
                throw new DashboardException(403, "Cross-origin requests are not allowed.");
            }
            if ("cross-site".equals(exchange.getRequestHeaders().getFirst("Sec-Fetch-Site"))) {
                throw new DashboardException(403, "Cross-origin requests are not allowed.");
            }
            String path = exchange.getRequestURI().getPath();
            if (!path.startsWith("/api/")) {
                asset(exchange, path);
                return;
            }
            String method = exchange.getRequestMethod();
            boolean mutation = !method.equals("GET");
            if (mutation && !"application/json".equalsIgnoreCase(
                    String.valueOf(exchange.getRequestHeaders().getFirst("Content-Type")).split(";", 2)[0].trim())) {
                throw new DashboardException(415, "Expected a JSON request.");
            }
            if (path.equals("/api/session") && method.equals("POST")) {
                JsonObject body = body(exchange);
                DashboardSessions.Session session = sessions.exchange(string(body, "token"));
                exchange.getResponseHeaders().set("Set-Cookie", cookie(session.id(), false));
                send(exchange, 200, sessionInfo(session));
                return;
            }
            String id = sessionId(exchange);
            DashboardSessions.Session session = sessions.get(id);
            if (mutation && !DashboardSessions.equal(session.csrf(), exchange.getRequestHeaders().getFirst("X-MCJS-CSRF"))) {
                throw new DashboardException(403, "Invalid session verification.");
            }
            if (path.equals("/api/logout") && method.equals("POST")) {
                sessions.logout(id);
                exchange.getResponseHeaders().set("Set-Cookie", cookie("", true));
                send(exchange, 200, Map.of("ok", true));
                return;
            }
            if (path.equals("/api/session") && method.equals("GET")) {
                send(exchange, 200, authorized(id, false, () -> sessionInfo(sessions.get(id))));
                return;
            }
            JsonObject body = mutation ? body(exchange) : new JsonObject();
            Object response = authorized(id, true, () -> route(path, method, body, exchange, session));
            send(exchange, 200, response);
        } catch (Exception exception) {
            Throwable cause = exception;
            while (cause instanceof ExecutionException && cause.getCause() != null) cause = cause.getCause();
            if (cause instanceof DashboardException failure) {
                send(exchange, failure.status(), Map.of("error", failure.getMessage()));
            } else if (cause instanceof JsonParseException || cause instanceof IllegalStateException
                    || cause instanceof IllegalArgumentException) {
                send(exchange, 400, Map.of("error", "Invalid request."));
            } else if (cause instanceof java.util.concurrent.TimeoutException) {
                send(exchange, 503, Map.of("error", "The server has not responded yet. Check the plugin status before trying again."));
            } else {
                send(exchange, 500, Map.of("error", "Dashboard action failed. Check the server console and file permissions."));
            }
        } finally {
            exchange.close();
        }
    }

    private Object route(String path, String method, JsonObject body, HttpExchange exchange,
                         DashboardSessions.Session session) throws Exception {
        if (path.equals("/api/integrations") && method.equals("GET")) return access.integrations();
        if (path.equals("/api/library") && method.equals("GET")) return workspace.library();
        if (path.equals("/api/library") && method.equals("PUT")) {
            var result = workspace.saveLibrary(body.get("entries"), string(body, "revision"));
            access.audit(session.playerId(), "library-save", "library");
            return result;
        }
        if ((path.equals("/api/data") || path.equals("/api/debug")) && method.equals("GET")) {
            var params = query(exchange);
            String name = params.getOrDefault("name", "");
            workspace.read(name);
            if (path.equals("/api/data")) return access.inspect(name, params.getOrDefault("scope", ""), params.getOrDefault("q", ""), Math.max(0, Integer.parseInt(params.getOrDefault("offset", "0"))));
            return access.debug(name, Math.max(0, Long.parseLong(params.getOrDefault("after", "0"))), null);
        }
        if (path.equals("/api/debug") && method.equals("POST")) {
            String name = string(body, "name");
            workspace.read(name);
            boolean enabled = body.get("enabled").getAsBoolean();
            access.audit(session.playerId(), enabled ? "debug-start" : "debug-stop", name);
            return access.debug(name, 0, enabled);
        }
        if (path.equals("/api/plugins") && method.equals("GET")) {
            JsonArray plugins = new JsonArray();
            for (String name : workspace.list()) {
                JsonObject entry = access.describe(name);
                entry.addProperty("name", name);
                plugins.add(entry);
            }
            return Map.of("plugins", plugins);
        }
        if (path.equals("/api/plugin") && method.equals("GET")) {
            String query = exchange.getRequestURI().getRawQuery();
            if (query == null || !query.startsWith("name=") || query.contains("&")) {
                throw new DashboardException(400, "Plugin name is required.");
            }
            return workspace.read(URLDecoder.decode(query.substring(5), StandardCharsets.UTF_8));
        }
        if (path.equals("/api/plugin") && method.equals("PUT")) {
            String name = string(body, "name");
            JsonObject result = workspace.save(name, string(body, "source"), string(body, "revision"), body.get("workspace"));
            access.audit(session.playerId(), "save", name);
            return result;
        }
        if (path.equals("/api/validate") && method.equals("POST")) {
            PluginWorkspace.validate(string(body, "source"));
            return Map.of("ok", true);
        }
        if ((path.equals("/api/run") || path.equals("/api/stop") || path.equals("/api/plugin"))
                && (method.equals("POST") || method.equals("DELETE"))) {
            boolean delete = path.equals("/api/plugin") && method.equals("DELETE");
            if (!delete && !method.equals("POST") || path.equals("/api/plugin") && !delete) {
                throw new DashboardException(405, "Method not allowed.");
            }
            String name = string(body, "name");
            workspace.checkRevision(name, string(body, "revision"));
            workspace.read(name);
            if (path.equals("/api/run")) {
                PluginWorkspace.validate(workspace.read(name).get("source").getAsString());
                access.run(name);
            } else {
                access.stop(name);
                if (delete) workspace.delete(name, string(body, "revision"));
            }
            access.audit(session.playerId(), delete ? "delete" : path.substring(5), name);
            return Map.of("ok", true);
        }
        throw new DashboardException(404, "Action not found.");
    }

    private Map<String, String> query(HttpExchange exchange) {
        var result = new java.util.HashMap<String, String>();
        String query = exchange.getRequestURI().getRawQuery();
        if (query != null) for (String part : query.split("&")) {
            var pair = part.split("=", 2);
            if (pair.length == 2) result.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8), URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
        }
        return result;
    }

    private <T> T authorized(String id, boolean requireVerified, Callable<T> action) throws Exception {
        return access.call(() -> {
            DashboardSessions.Session current = sessions.get(id);
            if (requireVerified && !current.verified()) {
                throw new DashboardException(403, "First confirm access using the command shown in your browser.");
            }
            try {
                access.authorize(current.playerId());
            } catch (DashboardException denied) {
                sessions.logout(id);
                throw denied;
            }
            return action.call();
        });
    }

    private Object sessionInfo(DashboardSessions.Session session) {
        return Map.of("verified", session.verified(), "player", session.playerName(), "expiresAt", session.expiresAt(),
                "csrf", session.csrf(), "command", session.verified() ? "" : "/mjs dashboard " + session.code());
    }

    private String cookie(String id, boolean clear) {
        return COOKIE + "=" + id + "; Path=/; HttpOnly; SameSite=Strict"
                + (publicUri.getScheme().equals("https") ? "; Secure" : "") + (clear ? "; Max-Age=0" : "");
    }

    private String sessionId(HttpExchange exchange) {
        List<String> cookies = exchange.getRequestHeaders().get("Cookie");
        if (cookies != null) {
            for (String header : cookies) {
                for (String cookie : header.split(";")) {
                    String[] parts = cookie.trim().split("=", 2);
                    if (parts.length == 2 && parts[0].equals(COOKIE)) return parts[1];
                }
            }
        }
        throw new DashboardException(401, "Open the link provided by /mjs dashboard.");
    }

    private JsonObject body(HttpExchange exchange) throws IOException {
        byte[] bytes = exchange.getRequestBody().readNBytes(2 * 1024 * 1024 + 1);
        if (bytes.length > 2 * 1024 * 1024) throw new DashboardException(413, "Request is too large.");
        return JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static String string(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive() || !object.getAsJsonPrimitive(key).isString()) {
            throw new DashboardException(400, "Missing field: " + key);
        }
        return object.get(key).getAsString();
    }

    private void headers(HttpExchange exchange) {
        var headers = exchange.getResponseHeaders();
        headers.set("Cache-Control", "no-store");
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("Referrer-Policy", "no-referrer");
        headers.set("X-Frame-Options", "DENY");
        headers.set("Cross-Origin-Resource-Policy", "same-origin");
        headers.set("Content-Security-Policy", "default-src 'none'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
                + "img-src 'self' data:; font-src 'self'; connect-src 'self'; media-src 'self'; "
                + "frame-ancestors 'none'; base-uri 'none'; form-action 'none'");
    }

    private void asset(HttpExchange exchange, String path) throws IOException {
        if (!exchange.getRequestMethod().equals("GET")) throw new DashboardException(405, "Method not allowed.");
        String file = path.equals("/") ? "index.html" : path.substring(1);
        if (!List.of("index.html", "app.js", "app.css", "THIRD-PARTY-LICENSES.txt").contains(file)
                && !file.matches("media/[A-Za-z0-9_-]+\\.(svg|png|gif|mp3|wav|ogg)")) {
            throw new DashboardException(404, "File not found.");
        }
        try (var input = getClass().getResourceAsStream("/dashboard/" + file)) {
            if (input == null) throw new DashboardException(404, "Dashboard assets are missing. Build the frontend first.");
            String type = file.endsWith(".js") ? "text/javascript; charset=utf-8"
                    : file.endsWith(".css") ? "text/css; charset=utf-8"
                    : file.endsWith(".html") ? "text/html; charset=utf-8"
                    : file.endsWith(".svg") ? "image/svg+xml"
                    : file.endsWith(".png") ? "image/png"
                    : file.endsWith(".gif") ? "image/gif" : "text/plain; charset=utf-8";
            byte[] bytes = input.readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", type);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
        }
    }

    private void send(HttpExchange exchange, int status, Object result) throws IOException {
        byte[] bytes = JSON.toJson(result).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
        sessions.clear();
    }
}
