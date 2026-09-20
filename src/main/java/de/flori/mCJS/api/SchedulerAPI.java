package de.flori.mCJS.api;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.mozilla.javascript.Function;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SchedulerAPI extends BaseAPI {
    private final APIHelper apiHelper;
    private final Set<OwnedTask> tasks = ConcurrentHashMap.newKeySet();
    private volatile boolean closed;

    public SchedulerAPI(JavaPlugin plugin, APIHelper apiHelper) {
        super(plugin);
        this.apiHelper = apiHelper;
    }

    public BukkitTask runTaskLater(long delay, Object task) {
        return schedule(task, null, false, runnable -> plugin.getServer().getScheduler().runTaskLater(plugin, runnable, delay));
    }

    public BukkitTask runTaskTimer(long delay, long period, Object task) {
        return schedule(task, null, true, runnable -> plugin.getServer().getScheduler().runTaskTimer(plugin, runnable, delay, period));
    }

    public BukkitTask runTask(Object task) {
        return schedule(task, null, false, runnable -> plugin.getServer().getScheduler().runTask(plugin, runnable));
    }

    public BukkitTask runOwnedTask(Runnable task) {
        return schedule(task, null, false, runnable -> plugin.getServer().getScheduler().runTask(plugin, runnable));
    }

    public BukkitTask runOwnedTaskLater(long delay, Runnable task) {
        return schedule(task, null, false, runnable -> plugin.getServer().getScheduler().runTaskLater(plugin, runnable, delay));
    }

    public BukkitTask runTaskAsync(Object task) {
        return schedule(task, null, false, runnable -> plugin.getServer().getScheduler().runTaskAsynchronously(plugin, runnable));
    }

    public BukkitTask runTaskLaterAsync(long delay, Object task) {
        return schedule(task, null, false, runnable -> plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay));
    }

    public BukkitTask runTaskSafe(Object task, Object onError) {
        return schedule(task, onError, false, runnable -> plugin.getServer().getScheduler().runTask(plugin, runnable));
    }

    public BukkitTask runTaskAsyncSafe(Object task, Object onError) {
        return schedule(task, onError, false, runnable -> plugin.getServer().getScheduler().runTaskAsynchronously(plugin, runnable));
    }

    private BukkitTask schedule(Object callback, Object onError, boolean repeat,
                                java.util.function.Function<Runnable, BukkitTask> register) {
        if (closed) throw new IllegalStateException("This plugin has been stopped");
        OwnedTask owned = new OwnedTask(callback, onError, repeat);
        tasks.add(owned);
        try {
            BukkitTask handle = register.apply(owned);
            owned.handle = handle;
            if (closed) {
                handle.cancel();
                tasks.remove(owned);
            }
            return handle;
        } catch (RuntimeException error) {
            tasks.remove(owned);
            throw error;
        }
    }

    public void cancelTask(BukkitTask task) {
        if (task != null) {
            task.cancel();
            tasks.removeIf(owned -> owned.handle == task);
        }
    }

    public void cancelAll() {
        closed = true;
        for (OwnedTask task : tasks) {
            if (task.handle != null) task.handle.cancel();
        }
        tasks.clear();
    }

    private final class OwnedTask implements Runnable {
        final Object callback;
        final Object onError;
        final boolean repeat;
        volatile BukkitTask handle;

        OwnedTask(Object callback, Object onError, boolean repeat) {
            this.callback = callback;
            this.onError = onError;
            this.repeat = repeat;
        }

        public void run() {
            try {
                if (!closed && callback instanceof Function function) apiHelper.executeFunction(function);
                else if (!closed && callback instanceof Runnable runnable) runnable.run();
            } catch (Exception error) {
                plugin.getLogger().severe("Error in scheduled task: " + error.getMessage());
                if (!closed && onError instanceof Function handler) {
                    try {
                        apiHelper.executeFunction(handler, error.getMessage());
                    } catch (Exception handlerError) {
                        plugin.getLogger().severe("Error in error handler: " + handlerError.getMessage());
                    }
                }
            } finally {
                if (!repeat) tasks.remove(this);
            }
        }
    }
}
