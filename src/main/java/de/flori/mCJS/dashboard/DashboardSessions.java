package de.flori.mCJS.dashboard;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class DashboardSessions {
    public record Session(String id, UUID playerId, String playerName, String code, String csrf,
                          boolean verified, long expiresAt) {}
    private record Ticket(UUID playerId, String playerName, long expiresAt) {}
    private record Attempts(int count, long resetAt) {}

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Ticket> tickets = new HashMap<>();
    private final Map<String, Session> sessions = new HashMap<>();
    private final Map<UUID, Attempts> attempts = new HashMap<>();
    private final Clock clock;
    private final long pendingMillis;
    private final long sessionMillis;

    public DashboardSessions(Duration pending, Duration session) {
        this(Clock.systemUTC(), pending, session);
    }

    DashboardSessions(Clock clock, Duration pending, Duration session) {
        this.clock = clock;
        this.pendingMillis = pending.toMillis();
        this.sessionMillis = session.toMillis();
    }

    public synchronized String issue(UUID playerId, String name) {
        prune();
        revoke(playerId);
        if (tickets.size() + sessions.size() >= 1024) {
            throw new DashboardException(429, "Too many dashboard sessions. Please try again later.");
        }
        String token = secret();
        tickets.put(token, new Ticket(playerId, name, clock.millis() + pendingMillis));
        return token;
    }

    public synchronized Session exchange(String token) {
        prune();
        Ticket ticket = tickets.remove(token);
        if (ticket == null) {
            throw new DashboardException(401, "This link has expired or has already been used. Run /mjs dashboard again.");
        }
        Session session = new Session(secret(), ticket.playerId(), ticket.playerName(), verificationCode(),
                secret(), false, ticket.expiresAt());
        sessions.put(session.id(), session);
        return session;
    }

    public synchronized Session get(String id) {
        prune();
        Session session = sessions.get(id);
        if (session == null) {
            throw new DashboardException(401, "Session expired. Request a new dashboard link in game.");
        }
        return session;
    }

    public synchronized boolean verify(UUID playerId, String code) {
        prune();
        Attempts previous = attempts.get(playerId);
        if (previous != null && previous.count() >= 5) {
            throw new DashboardException(429, "Too many incorrect codes. Wait one minute.");
        }
        for (Session session : sessions.values()) {
            if (session.playerId().equals(playerId) && !session.verified()
                    && equal(session.code(), code.toUpperCase(Locale.ROOT))) {
                sessions.put(session.id(), new Session(session.id(), playerId, session.playerName(), "",
                        session.csrf(), true, clock.millis() + sessionMillis));
                attempts.remove(playerId);
                return true;
            }
        }
        attempts.put(playerId, new Attempts(previous == null ? 1 : previous.count() + 1,
                previous == null ? clock.millis() + 60_000 : previous.resetAt()));
        return false;
    }

    public synchronized void logout(String id) {
        sessions.remove(id);
    }

    public synchronized void revoke(UUID playerId) {
        tickets.values().removeIf(ticket -> ticket.playerId().equals(playerId));
        sessions.values().removeIf(session -> session.playerId().equals(playerId));
    }

    public synchronized void clear() {
        tickets.clear();
        sessions.clear();
        attempts.clear();
    }

    private void prune() {
        long now = clock.millis();
        tickets.values().removeIf(ticket -> ticket.expiresAt() <= now);
        sessions.values().removeIf(session -> session.expiresAt() <= now);
        attempts.values().removeIf(attempt -> attempt.resetAt() <= now);
    }

    private String secret() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String verificationCode() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            code.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return code.toString();
    }

    public static boolean equal(String first, String second) {
        return first != null && second != null && MessageDigest.isEqual(
                first.getBytes(StandardCharsets.UTF_8), second.getBytes(StandardCharsets.UTF_8));
    }
}
