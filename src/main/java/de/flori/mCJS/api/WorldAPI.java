package de.flori.mCJS.api;

import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * API module for World-related operations
 */
public class WorldAPI extends BaseAPI {
    
    public WorldAPI(JavaPlugin plugin) {
        super(plugin);
    }
    
    // ===== WORLD LOOKUP =====
    public World getWorld(String name) {
        return name != null ? Bukkit.getWorld(name) : null;
    }
    
    public java.util.List<World> getWorlds() {
        return Bukkit.getWorlds();
    }
    
    // ===== TIME METHODS =====
    public long getWorldTime(World world) {
        return world != null ? world.getTime() : 0L;
    }
    
    public void setWorldTime(World world, long time) {
        if (world != null) {
            world.setTime(time);
        }
    }
    
    public long getWorldFullTime(World world) {
        return world != null ? world.getFullTime() : 0L;
    }
    
    public void setWorldFullTime(World world, long time) {
        if (world != null) {
            world.setFullTime(time);
        }
    }
    
    // ===== WEATHER METHODS =====
    public boolean isThundering(World world) {
        return world != null && world.isThundering();
    }
    
    public void setThundering(World world, boolean thundering) {
        if (world != null) {
            world.setThundering(thundering);
        }
    }
    
    public boolean hasStorm(World world) {
        return world != null && world.hasStorm();
    }
    
    public void setStorm(World world, boolean storm) {
        if (world != null) {
            world.setStorm(storm);
        }
    }
    
    public boolean hasWorldStorm(World world) {
        return world != null && world.hasStorm();
    }
    
    public void setWorldStorm(World world, boolean storm) {
        if (world != null) {
            world.setStorm(storm);
        }
    }
    
    public int getWorldStormDuration(World world) {
        return world != null ? world.getWeatherDuration() : 0;
    }
    
    public void setWorldStormDuration(World world, int duration) {
        if (world != null) {
            world.setWeatherDuration(duration);
        }
    }
    
    public boolean hasWorldThunder(World world) {
        return world != null && world.isThundering();
    }
    
    public void setWorldThunder(World world, boolean thunder) {
        if (world != null) {
            world.setThundering(thunder);
        }
    }
    
    public int getWorldThunderDuration(World world) {
        return world != null ? world.getThunderDuration() : 0;
    }
    
    public void setWorldThunderDuration(World world, int duration) {
        if (world != null) {
            world.setThunderDuration(duration);
        }
    }
    
    // ===== WORLD MANAGEMENT METHODS =====
    public void setWorldDifficulty(World world, String difficulty) {
        if (world == null || difficulty == null) {
            return;
        }
        try {
            Difficulty diff = Difficulty.valueOf(difficulty.toUpperCase());
            world.setDifficulty(diff);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid difficulty: " + difficulty);
        }
    }
    
    public String getWorldDifficulty(World world) {
        return world != null ? world.getDifficulty().name() : null;
    }
    
    public void setWorldPVP(World world, boolean pvp) {
        if (world != null) {
            try {
                java.lang.reflect.Method setPVPMethod = world.getClass().getMethod("setPVPEnabled", boolean.class);
                setPVPMethod.invoke(world, pvp);
            } catch (NoSuchMethodException e) {
                // Fallback to deprecated method
                try {
                    @SuppressWarnings("deprecation")
                    java.lang.reflect.Method deprecatedSetPVP = world.getClass().getMethod("setPVP", boolean.class);
                    deprecatedSetPVP.invoke(world, pvp);
                } catch (Exception ex) {
                    // Ignore
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error setting PVP for world: " + e.getMessage());
                // Fallback to deprecated method
                try {
                    @SuppressWarnings("deprecation")
                    java.lang.reflect.Method deprecatedSetPVP = world.getClass().getMethod("setPVP", boolean.class);
                    deprecatedSetPVP.invoke(world, pvp);
                } catch (Exception ex) {
                    // Ignore
                }
            }
        }
    }
    
    public boolean isWorldPVP(World world) {
        if (world == null) {
            return false;
        }
        try {
            java.lang.reflect.Method getPVPMethod = world.getClass().getMethod("isPVPEnabled");
            return (Boolean) getPVPMethod.invoke(world);
        } catch (NoSuchMethodException e) {
            @SuppressWarnings("deprecation")
            boolean result = world.getPVP();
            return result;
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting PVP status for world: " + e.getMessage());
            @SuppressWarnings("deprecation")
            boolean result = world.getPVP();
            return result;
        }
    }
    
    public void setWorldSpawnLocation(World world, Location location) {
        if (world != null && location != null) {
            world.setSpawnLocation(location);
        }
    }
    
    public Location getWorldSpawnLocation(World world) {
        return world != null ? world.getSpawnLocation() : null;
    }
    
    public void setWorldKeepSpawnInMemory(World world, boolean keepLoaded) {
        if (world != null) {
            try {
                world.setKeepSpawnInMemory(keepLoaded);
            } catch (Exception e) {
                plugin.getLogger().warning("setKeepSpawnInMemory is deprecated and may not work in future versions: " + e.getMessage());
            }
        }
    }
    
    public boolean isWorldKeepSpawnInMemory(World world) {
        if (world == null) {
            return false;
        }
        try {
            @SuppressWarnings("deprecation")
            boolean result = world.getKeepSpawnInMemory();
            return result;
        } catch (Exception e) {
            plugin.getLogger().warning("getKeepSpawnInMemory is deprecated and may not work in future versions: " + e.getMessage());
            return false;
        }
    }
    
    public void setWorldAutoSave(World world, boolean autoSave) {
        if (world != null) {
            world.setAutoSave(autoSave);
        }
    }
    
    public boolean isWorldAutoSave(World world) {
        return world != null && world.isAutoSave();
    }
    
    public World.Environment getWorldEnvironment(World world) {
        return world != null ? world.getEnvironment() : null;
    }
    
    public long getWorldSeed(World world) {
        return world != null ? world.getSeed() : 0L;
    }
    
    public int getWorldMaxHeight(World world) {
        return world != null ? world.getMaxHeight() : 256;
    }
    
    public int getWorldMinHeight(World world) {
        return world != null ? world.getMinHeight() : 0;
    }
    
    // ===== WORLD BORDER METHODS =====
    public WorldBorder getWorldBorder(World world) {
        return world != null ? world.getWorldBorder() : null;
    }
    
    public void setWorldBorderSize(World world, double size) {
        if (world != null) {
            world.getWorldBorder().setSize(size);
        }
    }
    
    public void setWorldBorderCenter(World world, double x, double z) {
        if (world != null) {
            world.getWorldBorder().setCenter(x, z);
        }
    }
    
    // ===== WORLD EFFECTS =====
    public void createExplosion(Location location, float power) {
        if (location != null && location.getWorld() != null) {
            location.getWorld().createExplosion(location, power);
        }
    }
    
    public void createExplosion(Location location, float power, boolean setFire) {
        if (location != null && location.getWorld() != null) {
            location.getWorld().createExplosion(location, power, setFire);
        }
    }
    
    public void strikeLightning(Location location) {
        if (location != null && location.getWorld() != null) {
            location.getWorld().strikeLightning(location);
        }
    }
    
    public void strikeLightningEffect(Location location) {
        if (location != null && location.getWorld() != null) {
            location.getWorld().strikeLightningEffect(location);
        }
    }
}
