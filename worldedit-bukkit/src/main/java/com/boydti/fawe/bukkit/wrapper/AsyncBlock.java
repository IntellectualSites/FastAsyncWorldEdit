package com.boydti.fawe.bukkit.wrapper;

import com.bekvon.bukkit.residence.commands.tool;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.bukkit.wrapper.state.AsyncSign;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.WorldEditException;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

public class AsyncBlock implements Block {

    public final int z;
    public final int y;
    public final int x;
    public final FaweQueue queue;
    public final AsyncWorld world;

    public AsyncBlock(AsyncWorld world, FaweQueue queue, int x, int y, int z) {
        this.world = world;
        this.queue = queue;
        this.x = x;
        this.y = Math.max(0, Math.min(255, y));
        this.z = z;
    }

    @Override
    @Deprecated
    public byte getData() {
        return (byte) (queue.getCachedCombinedId4Data(x, y, z, BlockTypes.AIR.getInternalId()) & 0xF);
    }

    @Override
    public Block getRelative(int modX, int modY, int modZ) {
        return new AsyncBlock(world, queue, x + modX, y + modY, z + modZ);
    }

    @Override
    public Block getRelative(BlockFace face) {
        return this.getRelative(face.getModX(), face.getModY(), face.getModZ());
    }

    @Override
    public Block getRelative(BlockFace face, int distance) {
        return this.getRelative(face.getModX() * distance, face.getModY() * distance, face.getModZ() * distance);
    }

    @Override
    public Material getType() {
        return getBlockData().getMaterial();
    }

    @Override
    public BlockData getBlockData() {
        return BukkitAdapter.getBlockData(queue.getCachedCombinedId4Data(x, y, z, BlockTypes.AIR.getInternalId()));
    }

    @Override
    public byte getLightLevel() {
        return (byte) queue.getLight(x, y, z);
    }

    @Override
    public byte getLightFromSky() {
        return (byte) queue.getSkyLight(x, y, z);
    }

    @Override
    public byte getLightFromBlocks() {
        return (byte) queue.getEmmittedLight(x, y, z);
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public int getZ() {
        return z;
    }

    @Override
    public Location getLocation() {
        return new Location(world, x, y, z);
    }

    @Override
    public Location getLocation(Location loc) {
        if(loc != null) {
            loc.setWorld(this.getWorld());
            loc.setX((double)this.x);
            loc.setY((double)this.y);
            loc.setZ((double)this.z);
        }
        return loc;
    }

    @Override
    public Chunk getChunk() {
        return world.getChunkAt(x >> 4, z >> 4);
    }

    @Override
    public void setBlockData(BlockData blockData) {
        try {
            queue.setBlock(x, y, z, BukkitAdapter.adapt(blockData));
        } catch (WorldEditException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setBlockData(BlockData blockData, boolean b) {
        setBlockData(blockData);
    }

    @Override
    public void setType(Material type) {
        try {
            queue.setBlock(x, y, z, BukkitAdapter.adapt(type).getDefaultState());
        } catch (WorldEditException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setType(Material type, boolean applyPhysics) {
        setType(type);
    }

    @Override
    public BlockFace getFace(Block block) {
        BlockFace[] directions = BlockFace.values();
        for(int i = 0; i < directions.length; ++i) {
            BlockFace face = directions[i];
            if(this.getX() + face.getModX() == block.getX() && this.getY() + face.getModY() == block.getY() && this.getZ() + face.getModZ() == block.getZ()) {
                return face;
            }
        }
        return null;
    }

    @Override
    public BlockState getState() {
        int combined = queue.getCombinedId4Data(x, y, z, 0);
        BlockTypes type = BlockTypes.getFromStateId(combined);
        switch (type) {
            case SIGN:
            case WALL_SIGN:
                return new AsyncSign(this, combined);
        }
        return new AsyncBlockState(this, combined);
    }

    @Override
    public BlockState getState(boolean useSnapshot) {
        return getState();
    }

    @Override
    public Biome getBiome() {
        return world.getAdapter().getBiome(queue.getBiomeId(x, z));
    }

    @Override
    public void setBiome(Biome bio) {
        int id = world.getAdapter().getBiomeId(bio);
        queue.setBiome(x, z, FaweCache.getBiome(id));
    }

    @Override
    public boolean isBlockPowered() {
        return false;
    }

    @Override
    public boolean isBlockIndirectlyPowered() {
        return false;
    }

    @Override
    public boolean isBlockFacePowered(BlockFace face) {
        return false;
    }

    @Override
    public boolean isBlockFaceIndirectlyPowered(BlockFace face) {
        return false;
    }

    @Override
    public int getBlockPower(BlockFace face) {
        return 0;
    }

    @Override
    public int getBlockPower() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        switch (getType()) {
            case AIR:
            case CAVE_AIR:
            case VOID_AIR:
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean isLiquid() {
        int combined = queue.getCombinedId4Data(x, y, z, 0);
        BlockTypes type = BlockTypes.getFromStateId(combined);
        return type.getMaterial().isLiquid();
    }

    @Override
    public double getTemperature() {
        return this.getWorld().getTemperature(this.getX(), this.getZ());
    }

    @Override
    public double getHumidity() {
        return this.getWorld().getHumidity(this.getX(), this.getZ());
    }

    @Override
    public PistonMoveReaction getPistonMoveReaction() {
        return null;
    }

    public Block getBukkitBlock() {
        return world.getBukkitWorld().getBlockAt(x, y, z);
    }

    @Override
    public boolean breakNaturally() {
        return TaskManager.IMP.sync(() -> getBukkitBlock().breakNaturally());
    }

    @Override
    public boolean breakNaturally(ItemStack tool) {
        return TaskManager.IMP.sync(() -> getBukkitBlock().breakNaturally(tool));
    }

    @Override
    public Collection<ItemStack> getDrops() {
        return TaskManager.IMP.sync(() -> getBukkitBlock().getDrops());
    }

    @Override
    public Collection<ItemStack> getDrops(ItemStack tool) {
        return TaskManager.IMP.sync(() -> getBukkitBlock().getDrops(tool));
    }

    @Override
    public void setMetadata(String metadataKey, MetadataValue newMetadataValue) {
        this.getBukkitBlock().setMetadata(metadataKey, newMetadataValue);
    }

    @Override
    public List<MetadataValue> getMetadata(String metadataKey) {
        return this.getBukkitBlock().getMetadata(metadataKey);
    }

    @Override
    public boolean hasMetadata(String metadataKey) {
        return this.getBukkitBlock().hasMetadata(metadataKey);
    }

    @Override
    public void removeMetadata(String metadataKey, Plugin owningPlugin) {
        this.getBukkitBlock().removeMetadata(metadataKey, owningPlugin);
    }
}
