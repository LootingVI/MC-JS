package de.flori.mCJS.api;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * API module for block manipulation
 */
public class BlockAPI extends BaseAPI {
    
    public BlockAPI(JavaPlugin plugin) {
        super(plugin);
    }
    
    // ===== BASIC BLOCK METHODS =====
    public void breakBlock(Location location) {
        location.getBlock().breakNaturally();
    }

    public void breakBlock(Location location, boolean dropItems) {
        location.getBlock().breakNaturally(new ItemStack(Material.AIR));
    }

    public Block getBlockAt(World world, int x, int y, int z) {
        return world.getBlockAt(x, y, z);
    }

    public List<Block> getBlocksInRadius(Location center, double radius) {
        List<Block> blocks = new ArrayList<>();
        int radiusInt = (int) Math.ceil(radius);
        
        for (int x = -radiusInt; x <= radiusInt; x++) {
            for (int y = -radiusInt; y <= radiusInt; y++) {
                for (int z = -radiusInt; z <= radiusInt; z++) {
                    Location loc = center.clone().add(x, y, z);
                    if (loc.distance(center) <= radius) {
                        blocks.add(loc.getBlock());
                    }
                }
            }
        }
        
        return blocks;
    }
    
    public Block getBlock(Location location) {
        return location.getBlock();
    }

    public Material getBlockType(Location location) {
        return location.getBlock().getType();
    }

    public void setBlockType(Location location, Material material) {
        location.getBlock().setType(material);
    }
    
    // ===== ADVANCED BLOCK UTILITIES =====
    /**
     * Get block power relative to face
     */
    public int getBlockPower(Block block, BlockFace face) {
        return block != null && face != null ? block.getBlockPower(face) : 0;
    }
    
    /**
     * Get block temperature
     */
    public double getBlockTemperature(Block block) {
        return block != null ? block.getTemperature() : 0.0;
    }
    
    /**
     * Get block humidity
     */
    public double getBlockHumidity(Block block) {
        return block != null ? block.getHumidity() : 0.0;
    }
    
    /**
     * Get block light level from sky
     */
    public int getBlockLightLevelFromSky(Block block) {
        return block != null ? block.getLightFromSky() : 0;
    }
    
    /**
     * Get block light level from blocks
     */
    public int getBlockLightLevelFromBlocks(Block block) {
        return block != null ? block.getLightFromBlocks() : 0;
    }
    
    /**
     * Break block naturally
     */
    public void breakBlockNaturally(Block block) {
        if (block != null) {
            block.breakNaturally();
        }
    }
    
    /**
     * Break block with item
     */
    public void breakBlockNaturally(Block block, ItemStack tool) {
        if (block != null) {
            block.breakNaturally(tool);
        }
    }
    
    /**
     * Get block relative
     */
    public Block getBlockRelative(Block block, BlockFace face) {
        return block != null && face != null ? block.getRelative(face) : null;
    }
    
    /**
     * Get block relative with offset
     */
    public Block getBlockRelative(Block block, int x, int y, int z) {
        return block != null ? block.getRelative(x, y, z) : null;
    }
}
