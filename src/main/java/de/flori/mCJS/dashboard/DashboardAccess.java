package de.flori.mCJS.dashboard;

import com.google.gson.JsonObject;
import java.util.UUID;
import java.util.concurrent.Callable;

public interface DashboardAccess {
    <T> T call(Callable<T> action) throws Exception;
    void authorize(UUID playerId);
    JsonObject describe(String name);
    void run(String name) throws Exception;
    void stop(String name);
    void audit(UUID playerId, String action, String name);
    default JsonObject inspect(String name, String scope, String query, int offset) { throw new DashboardException(503, "Data inspector is unavailable."); }
    default JsonObject debug(String name, long after, Boolean enabled) { throw new DashboardException(503, "Debugger is unavailable."); }
    default JsonObject integrations() { var result = new JsonObject(); result.addProperty("vault", false); result.addProperty("provider", "Unavailable"); return result; }
}
