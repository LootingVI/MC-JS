package de.flori.mCJS.api;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class SoundParticleAPI extends BaseAPI {

    public SoundParticleAPI(JavaPlugin plugin) {
        super(plugin);
    }

    private Sound getSound(String soundName) {
        try {

            String normalized = soundName.toLowerCase().replace("_", ":");
            NamespacedKey key = NamespacedKey.fromString(normalized);
            if (key != null) {
                Sound sound = Registry.SOUNDS.get(key);
                if (sound != null) {
                    return sound;
                }
            }

            try {
                NamespacedKey minecraftKey = NamespacedKey.minecraft(soundName.toLowerCase().replace("_", ""));
                Sound sound = Registry.SOUNDS.get(minecraftKey);
                if (sound != null) {
                    return sound;
                }
            } catch (Exception ex) {

            }

            @SuppressWarnings("deprecation")
            Sound fallback = Sound.valueOf(soundName.toUpperCase());
            return fallback;
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound type: " + soundName + ", using ENTITY_PLAYER_LEVELUP");
            return Sound.ENTITY_PLAYER_LEVELUP;
        }
    }

    public void playSound(Location location, Sound sound, float volume, float pitch) {
        location.getWorld().playSound(location, sound, volume, pitch);
    }

    public void playSound(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    public void playSound(Location location, Object soundName, float volume, float pitch) {
        if (soundName == null) return;
        playSound(location, getSound(soundName.toString()), volume, pitch);
    }

    public void playSound(Player player, Object soundName, float volume, float pitch) {
        if (soundName == null) return;
        playSound(player, getSound(soundName.toString()), volume, pitch);
    }

    private Particle getParticle(String particleName) {
        try {
            return Particle.valueOf(particleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid particle type: " + particleName + ", using FLAME");
            try {
                return Particle.FLAME;
            } catch (Exception ex) {

                return Particle.values()[0];
            }
        }
    }

    public void spawnParticle(Location location, Particle particle, int count) {
        location.getWorld().spawnParticle(particle, location, count);
    }

    public void spawnParticle(Location location, Particle particle, int count, double offsetX, double offsetY, double offsetZ) {
        location.getWorld().spawnParticle(particle, location, count, offsetX, offsetY, offsetZ);
    }

    public void spawnParticle(Location location, Particle particle, int count, double offsetX, double offsetY, double offsetZ, double extra) {
        location.getWorld().spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra);
    }

    public void spawnParticle(Location location, Object particleName, int count) {
        if (particleName == null) return;
        spawnParticle(location, getParticle(particleName.toString()), count);
    }

    public void spawnParticle(Location location, Object particleName, int count, double offsetX, double offsetY, double offsetZ) {
        if (particleName == null) return;
        spawnParticle(location, getParticle(particleName.toString()), count, offsetX, offsetY, offsetZ);
    }

    public void spawnParticle(Location location, Object particleName, int count, double offsetX, double offsetY, double offsetZ, double extra) {
        if (particleName == null) return;
        spawnParticle(location, getParticle(particleName.toString()), count, offsetX, offsetY, offsetZ, extra);
    }
}
