package de.flori.mCJS.api;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class EntityAPI extends BaseAPI {

    public EntityAPI(JavaPlugin plugin) {
        super(plugin);
    }

    private EntityType getEntityType(String entityName) {
        try {
            return EntityType.valueOf(entityName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid entity type: " + entityName + ", using PIG");
            return EntityType.PIG;
        }
    }

    public Entity spawnEntity(Location location, EntityType type) {
        return location.getWorld().spawnEntity(location, type);
    }

    public Entity spawnEntity(Location location, String entityName) {
        return spawnEntity(location, getEntityType(entityName));
    }

    public List<Entity> getNearbyEntities(Location location, double x, double y, double z) {
        return (List<Entity>) location.getWorld().getNearbyEntities(location, x, y, z);
    }

    public void setEntityCustomName(Entity entity, String name) {
        if (entity == null || name == null) {
            return;
        }
        Component component = legacyToComponentWithAmpersand(name);
        entity.customName(component);
        entity.setCustomNameVisible(true);
    }

    public void setEntityGlowing(Entity entity, boolean glowing) {
        if (entity != null) {
            entity.setGlowing(glowing);
        }
    }

    public void setEntityGravity(Entity entity, boolean gravity) {
        if (entity != null) {
            entity.setGravity(gravity);
        }
    }

    public void setEntityInvulnerable(Entity entity, boolean invulnerable) {
        if (entity != null) {
            entity.setInvulnerable(invulnerable);
        }
    }

    public void setEntityAI(Entity entity, boolean ai) {
        if (entity != null && entity instanceof LivingEntity) {
            ((LivingEntity) entity).setAI(ai);
        }
    }

    public boolean hasEntityAI(Entity entity) {
        if (entity != null && entity instanceof LivingEntity) {
            return ((LivingEntity) entity).hasAI();
        }
        return false;
    }

    public void setEntitySilent(Entity entity, boolean silent) {
        if (entity != null) {
            entity.setSilent(silent);
        }
    }

    public boolean isEntitySilent(Entity entity) {
        return entity != null && entity.isSilent();
    }

    public void setEntityCollidable(Entity entity, boolean collidable) {
        if (entity != null && entity instanceof LivingEntity) {

            try {
                java.lang.reflect.Method method = entity.getClass().getMethod("setCollidable", boolean.class);
                method.invoke(entity, collidable);
            } catch (Exception e) {

                debug("setCollidable not available for entity type: " + entity.getType());
            }
        }
    }

    public boolean isEntityCollidable(Entity entity) {
        if (entity == null || !(entity instanceof LivingEntity)) {
            return true;
        }
        try {
            java.lang.reflect.Method method = entity.getClass().getMethod("isCollidable");
            return (Boolean) method.invoke(entity);
        } catch (Exception e) {
            return true;
        }
    }

    public Location getEntityLocation(Entity entity) {
        return entity != null ? entity.getLocation() : null;
    }

    public void teleportEntity(Entity entity, Location location) {
        if (entity != null && location != null) {
            entity.teleport(location);
        }
    }

    public void teleportEntity(Entity entity, Entity target) {
        if (entity != null && target != null) {
            entity.teleport(target.getLocation());
        }
    }

    public void removeEntity(Entity entity) {
        if (entity != null) {
            entity.remove();
        }
    }

    public void setEntityCustomNameVisible(Entity entity, boolean visible) {
        if (entity != null) {
            entity.setCustomNameVisible(visible);
        }
    }

    public boolean isEntityCustomNameVisible(Entity entity) {
        return entity != null && entity.isCustomNameVisible();
    }

    public Vector getEntityVelocity(Entity entity) {
        return entity != null ? entity.getVelocity() : null;
    }

    public void setEntityVelocity(Entity entity, Vector velocity) {
        if (entity != null && velocity != null) {
            entity.setVelocity(velocity);
        }
    }

    public boolean isEntityOnGround(Entity entity) {
        return entity != null && entity.isOnGround();
    }

    public List<Entity> getEntityPassengers(Entity entity) {
        return entity != null ? entity.getPassengers() : new ArrayList<>();
    }

    public boolean addEntityPassenger(Entity entity, Entity passenger) {
        if (entity != null && passenger != null) {
            return entity.addPassenger(passenger);
        }
        return false;
    }

    public boolean removeEntityPassenger(Entity entity, Entity passenger) {
        if (entity != null && passenger != null) {
            return entity.removePassenger(passenger);
        }
        return false;
    }
}
