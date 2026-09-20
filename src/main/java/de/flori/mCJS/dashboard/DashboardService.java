package de.flori.mCJS.dashboard;

import com.google.gson.JsonObject;
import de.flori.mCJS.MCJS;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public final class DashboardService implements Listener, AutoCloseable {
    private final MCJS plugin;
    private final DashboardSessions sessions;
    private final DashboardServer server;

    public DashboardService(MCJS plugin) throws IOException {
        this.plugin = plugin;
        var config = plugin.getConfig();
        sessions = new DashboardSessions(
                Duration.ofSeconds(Math.clamp(config.getLong("dashboard.verify-timeout-seconds", 300), 30, 900)),
                Duration.ofMinutes(Math.clamp(config.getLong("dashboard.session-minutes", 30), 1, 240)));
        server = new DashboardServer(new InetSocketAddress(config.getString("dashboard.bind", "127.0.0.1"),
                config.getInt("dashboard.port", 8765)), URI.create(config.getString("dashboard.public-url", "http://localhost:8765")),
                sessions, new PluginWorkspace(plugin.getDataFolder().toPath().resolve("js-plugins")), new DashboardAccess() {
            @Override
            public <T> T call(Callable<T> action) throws Exception {
                if (!plugin.isEnabled()) throw new DashboardException(503, "MC-JS is shutting down.");
                var future = plugin.getServer().getScheduler().callSyncMethod(plugin, action);
                try {
                    return future.get(15, TimeUnit.SECONDS);
                } finally {
                    if (!future.isDone()) future.cancel(false);
                }
            }

            @Override
            public void authorize(UUID playerId) {
                Player player = plugin.getServer().getPlayer(playerId);
                if (player == null || !player.isOnline() || !player.hasPermission("mcjs.dashboard")) {
                    throw new DashboardException(403, "You must be online and have the mcjs.dashboard permission.");
                }
            }

            @Override
            public JsonObject describe(String name) {
                JsonObject result = new JsonObject();
                var manager = plugin.getJSPluginManager();
                result.addProperty("running", manager.getPluginScope(name) != null);
                result.addProperty("disabled", manager.isPluginDisabled(name));
                var metadata = manager.getMetadata(name);
                if (metadata != null) {
                    result.addProperty("version", metadata.getVersion());
                    result.addProperty("description", metadata.getDescription());
                }
                return result;
            }

            @Override
            public void run(String name) throws IOException {
                if (plugin.getJSPluginManager().isPluginDisabled(name)) {
                    throw new DashboardException(409, "This plugin is disabled in config.yml.");
                }
                try {
                    plugin.getJSPluginManager().activatePlugin(name);
                } catch (IOException error) {
                    plugin.getLogger().warning("Dashboard plugin start failed for " + name + ": " + error.getMessage());
                    throw new DashboardException(422, "Could not start plugin: " + error.getMessage());
                }
            }

            @Override
            public void stop(String name) {
                plugin.getJSPluginManager().unloadPlugin(name);
            }

            @Override
            public void audit(UUID playerId, String action, String name) {
                plugin.getLogger().info("Dashboard " + action + " '" + name + "' by " + playerId);
            }

            @Override public JsonObject inspect(String name, String scope, String query, int offset) {
                return new de.flori.mCJS.api.FileAPI(plugin).inspectSystemData(name, scope, query, offset);
            }
            @Override public JsonObject debug(String name, long after, Boolean enabled) {
                var debugger = plugin.getJSPluginManager().debugger(name);
                if (enabled != null) debugger.enable(enabled);
                return debugger.read(after);
            }
            @Override public JsonObject integrations() {
                var vault = new de.flori.mCJS.api.VaultAPI(plugin);
                var result = new JsonObject();
                result.addProperty("vault", vault.isAvailable()); result.addProperty("provider", vault.getProviderName());
                return result;
            }
        });
        server.start();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("Dashboard listening on " + config.getString("dashboard.bind", "127.0.0.1") + ":" + server.port());
    }

    public boolean command(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Run this command in game.", NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("mcjs.dashboard")) {
            player.sendMessage(Component.text("You do not have the mcjs.dashboard permission.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0 || !args[0].equalsIgnoreCase("dashboard") || args.length > 2) {
            player.sendMessage(Component.text("/mjs dashboard [verify-code]", NamedTextColor.YELLOW));
            return true;
        }
        try {
            if (args.length == 2) {
                boolean verified = sessions.verify(player.getUniqueId(), args[1]);
                player.sendMessage(Component.text(verified ? "Dashboard access granted. Continue in your browser."
                        : "Invalid or expired code. Use the code shown in your dashboard.",
                        verified ? NamedTextColor.GREEN : NamedTextColor.RED));
            } else {
                String link = server.link(sessions.issue(player.getUniqueId(), player.getName()));
                player.sendMessage(Component.text("Open dashboard ↗", NamedTextColor.AQUA)
                        .clickEvent(ClickEvent.openUrl(link))
                        .hoverEvent(Component.text("Open the one-time dashboard link in your browser")));
                player.sendMessage(Component.text(link, NamedTextColor.GRAY).clickEvent(ClickEvent.openUrl(link)));
                player.sendMessage(Component.text("Then confirm access by running the command shown in your browser here in game.", NamedTextColor.YELLOW));
            }
        } catch (DashboardException error) {
            player.sendMessage(Component.text(error.getMessage(), NamedTextColor.RED));
        }
        return true;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sessions.revoke(event.getPlayer().getUniqueId());
    }

    @Override
    public void close() {
        org.bukkit.event.HandlerList.unregisterAll(this);
        server.close();
    }
}
