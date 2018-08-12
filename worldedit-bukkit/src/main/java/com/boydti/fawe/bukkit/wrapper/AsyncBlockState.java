package com.boydti.fawe.bukkit.wrapper;

import com.boydti.fawe.FaweCache;
import com.sk89q.jnbt.CompoundTag;
import java.util.List;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.material.MaterialData;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

public class AsyncBlockState implements BlockState {

    private int combinedId;
    private BlockData blockData;
    private CompoundTag nbt;
    private final AsyncBlock block;

    public AsyncBlockState(AsyncBlock block) {
        this(block, block.queue.getCombinedId4Data(block.x, block.y, block.z, 0));
    }

    public AsyncBlockState(AsyncBlock block, int combined) {
        this.combinedId = combined;
        this.block = block;
        this.blockData = BukkitAdapter.getBlockData(combined);
        if (BlockTypes.getFromStateId(combined).getMaterial().hasContainer()) {
            this.nbt = block.queue.getTileEntity(block.x, block.y, block.z);
        }
    }

    @Override
    public Block getBlock() {
        return block;
    }

    @Override
    public BlockData getBlockData() {
        return blockData;
    }

    @Override
    public MaterialData getData() {
        return new MaterialData(blockData.getMaterial());
    }

    @Override
    public Material getType() {
        return blockData.getMaterial();
    }

    @Override
    public byte getLightLevel() {
        return (byte) BlockTypes.getFromStateId(combinedId).getMaterial().getLightValue();
    }

    @Override
    public World getWorld() {
        return block.world;
    }

    @Override
    public int getX() {
        return block.x;
    }

    @Override
    public int getY() {
        return block.y;
    }

    @Override
    public int getZ() {
        return block.z;
    }

    @Override
    public Location getLocation() {
        return block.getLocation();
    }

    @Override
    public Location getLocation(Location loc) {
        return block.getLocation(loc);
    }

    @Override
    public Chunk getChunk() {
        return block.getChunk();
    }

    @Override
    public void setData(MaterialData data) {
        setBlockData(data.getItemType().createBlockData());
    }

    @Override
    public void setBlockData(BlockData blockData) {
        this.blockData = blockData;
        this.combinedId = BukkitAdapter.adapt(blockData).getInternalId();
    }

    @Override
    public void setType(Material type) {
        setBlockData(type.createBlockData());
    }

    @Override
    public boolean update() {
        return update(false);
    }

    @Override
    public boolean update(boolean force) {
        return update(force, true);
    }

    @Override
    public boolean update(boolean force, boolean applyPhysics) {
        try {
            boolean result = block.queue.setBlock(block.x, block.y, block.z, BukkitAdapter.adapt(blockData));
            if (nbt != null) {
                block.queue.setTile(block.x, block.y, block.z, nbt);
            }
            return result;
        } catch (WorldEditException e) {
            throw new RuntimeException(e);
        }
    }

    public CompoundTag getNbtData() {
        return nbt;
    }

    public void setNbtData(CompoundTag nbt) {
        this.nbt = nbt;
    }

    @Override
    public byte getRawData() {
        return (byte) (combinedId >> BlockTypes.BIT_OFFSET);
    }

    @Override
    public void setRawData(byte data) {
        this.combinedId = (combinedId & BlockTypes.BIT_MASK) + data;
        this.blockData = BukkitAdapter.getBlockData(this.combinedId);
    }

    @Override
    public boolean isPlaced() {
        return true;
    }

    @Override
    public void setMetadata(String key, MetadataValue value) {
        block.setMetadata(key, value);
    }

    @Override
    public List<MetadataValue> getMetadata(String key) {
        return block.getMetadata(key);
    }

    @Override
    public boolean hasMetadata(String key) {
        return block.hasMetadata(key);
    }

    @Override
    public void removeMetadata(String key, Plugin plugin) {
        block.removeMetadata(key, plugin);
    }
}
