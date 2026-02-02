package de.flori.mCJS.api;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.mozilla.javascript.Function;

/**
 * API module for task scheduling (sync and async)
 */
public class SchedulerAPI extends BaseAPI {
    private final APIHelper apiHelper;
    
    public SchedulerAPI(JavaPlugin plugin, APIHelper apiHelper) {
        super(plugin);
        this.apiHelper = apiHelper;
    }
    
    public BukkitTask runTaskLater(long delay, Object task) {
        return plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            try {
                if (task instanceof Function && apiHelper.getScope() != null) {
                    apiHelper.executeFunction((Function) task);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error in scheduled task: " + e.getMessage());
                e.printStackTrace();
            }
        }, delay);
    }
    
    public BukkitTask runTaskTimer(long delay, long period, Object task) {
        return plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            try {
                if (task instanceof Function && apiHelper.getScope() != null) {
                    apiHelper.executeFunction((Function) task);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error in scheduled task: " + e.getMessage());
                e.printStackTrace();
            }
        }, delay, period);
    }
    
    public BukkitTask runTask(Object task) {
        return plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                if (task instanceof Function && apiHelper.getScope() != null) {
                    apiHelper.executeFunction((Function) task);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error in task: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    public BukkitTask runTaskAsync(Object task) {
        return plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (task instanceof Function && apiHelper.getScope() != null) {
                    apiHelper.executeFunction((Function) task);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error in async task: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    public BukkitTask runTaskLaterAsync(long delay, Object task) {
        return plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            try {
                if (task instanceof Function && apiHelper.getScope() != null) {
                    apiHelper.executeFunction((Function) task);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error in async scheduled task: " + e.getMessage());
                e.printStackTrace();
            }
        }, delay);
    }
    
    public void cancelTask(BukkitTask task) {
        if (task != null) {
            task.cancel();
        }
    }
    
    public BukkitTask runTaskSafe(Object task, Object onError) {
        return plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                if (task instanceof Function && apiHelper.getScope() != null) {
                    apiHelper.executeFunction((Function) task);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error in task: " + e.getMessage());
                if (onError instanceof Function && apiHelper.getScope() != null) {
                    try {
                        org.mozilla.javascript.Context cx = org.mozilla.javascript.Context.enter();
                        try {
                            cx.setOptimizationLevel(-1);
                            cx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                            ((Function) onError).call(cx, apiHelper.getScope(), apiHelper.getScope(), new Object[]{e.getMessage()});
                        } finally {
                            org.mozilla.javascript.Context.exit();
                        }
                    } catch (Exception errorHandlerException) {
                        plugin.getLogger().severe("Error in error handler: " + errorHandlerException.getMessage());
                    }
                } else {
                    e.printStackTrace();
                }
            }
        });
    }
    
    public BukkitTask runTaskAsyncSafe(Object task, Object onError) {
        return plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (task instanceof Function && apiHelper.getScope() != null) {
                    apiHelper.executeFunction((Function) task);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error in async task: " + e.getMessage());
                if (onError instanceof Function && apiHelper.getScope() != null) {
                    try {
                        org.mozilla.javascript.Context cx = org.mozilla.javascript.Context.enter();
                        try {
                            cx.setOptimizationLevel(-1);
                            cx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                            ((Function) onError).call(cx, apiHelper.getScope(), apiHelper.getScope(), new Object[]{e.getMessage()});
                        } finally {
                            org.mozilla.javascript.Context.exit();
                        }
                    } catch (Exception errorHandlerException) {
                        plugin.getLogger().severe("Error in error handler: " + errorHandlerException.getMessage());
                    }
                } else {
                    e.printStackTrace();
                }
            }
        });
    }
}
