package de.flori.mCJS.dashboard;

import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class DashboardSessionsTest {
    static final class MutableClock extends Clock {
        long now = 1_000_000;
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return Instant.ofEpochMilli(now); }
    }

    @Test
    void linkIsSingleUseAndOnlyItsPlayerCanVerify() {
        var sessions = new DashboardSessions(Duration.ofMinutes(5), Duration.ofMinutes(30));
        UUID player = UUID.randomUUID();
        String token = sessions.issue(player, "Builder");
        assertEquals(43, token.length());
        var pending = sessions.exchange(token);
        assertFalse(pending.verified());
        assertThrows(DashboardException.class, () -> sessions.exchange(token));
        assertFalse(sessions.verify(UUID.randomUUID(), pending.code()));
        assertTrue(sessions.verify(player, pending.code().toLowerCase()));
        var verified = sessions.get(pending.id());
        assertTrue(verified.verified());
        assertEquals("", verified.code());
        assertFalse(sessions.verify(player, pending.code()));
    }

    @Test
    void newLinkRevokesOldBrowserAndLogoutRevokesSession() {
        var sessions = new DashboardSessions(Duration.ofMinutes(5), Duration.ofMinutes(30));
        UUID player = UUID.randomUUID();
        var first = sessions.exchange(sessions.issue(player, "Builder"));
        sessions.verify(player, first.code());
        String token = sessions.issue(player, "Builder");
        assertThrows(DashboardException.class, () -> sessions.get(first.id()));
        var second = sessions.exchange(token);
        sessions.logout(second.id());
        assertThrows(DashboardException.class, () -> sessions.get(second.id()));
    }

    @Test
    void ticketsPendingAndVerifiedSessionsExpire() {
        MutableClock clock = new MutableClock();
        var sessions = new DashboardSessions(clock, Duration.ofSeconds(10), Duration.ofSeconds(30));
        UUID player = UUID.randomUUID();
        String expired = sessions.issue(player, "Builder");
        clock.now += 10_000;
        assertThrows(DashboardException.class, () -> sessions.exchange(expired));
        var pending = sessions.exchange(sessions.issue(player, "Builder"));
        clock.now += 10_000;
        assertFalse(sessions.verify(player, pending.code()));
        var active = sessions.exchange(sessions.issue(player, "Builder"));
        assertTrue(sessions.verify(player, active.code()));
        clock.now += 29_999;
        assertTrue(sessions.get(active.id()).verified());
        clock.now++;
        assertThrows(DashboardException.class, () -> sessions.get(active.id()));
    }

    @Test
    void wrongCodesAreRateLimitedEvenWhenIssuingNewLinks() {
        MutableClock clock = new MutableClock();
        var sessions = new DashboardSessions(clock, Duration.ofMinutes(5), Duration.ofMinutes(30));
        UUID player = UUID.randomUUID();
        for (int i = 0; i < 5; i++) assertFalse(sessions.verify(player, "WRONG"));
        var pending = sessions.exchange(sessions.issue(player, "Builder"));
        assertEquals(429, assertThrows(DashboardException.class, () -> sessions.verify(player, pending.code())).status());
        clock.now += 60_000;
        assertTrue(sessions.verify(player, pending.code()));
        sessions.revoke(player);
        assertThrows(DashboardException.class, () -> sessions.get(pending.id()));
    }
}
