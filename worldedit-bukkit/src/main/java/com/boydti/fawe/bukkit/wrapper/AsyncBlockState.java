package com.boydti.fawe.bukkit.wrapper;

import com.boydti.fawe.FaweCache;
import com.sk89q.jnbt.CompoundTag;
import java.util.List;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockType;
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
import org.jetbrains.annotations.NotNull;

public class AsyncBlockState implements BlockState {

    private BaseBlock state;
    private BlockData blockData;
    private final AsyncBlock block;

    public AsyncBlockState(AsyncBlock block) {
        this(block, block.world.getFullBlock(block.x, block.y, block.z));
    }

    public AsyncBlockState(AsyncBlock block, BaseBlock state) {
        this.state = state;
        this.block = block;
        this.blockData = BukkitAdapter.adapt(state);
    }

    public int getTypeId() {
        return state.getBlockType().getInternalId();
    }

    public int getPropertyId() {
        return state.getInternalId() >> BlockTypes.BIT_OFFSET;
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
        return (byte) state.getMaterial().getLightValue();
    }

    @Override
    public AsyncWorld getWorld() {
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

    @NotNull
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
        CompoundTag nbt = state.getNbtData();
        BlockType oldType = state.getBlockType();
        com.sk89q.worldedit.world.block.BlockState newState = BukkitAdapter.adapt(blockData);
        if (nbt != null && newState.getBlockType() == oldType) {
            state = newState.toBaseBlock(nbt);
        } else {
            state = newState.toBaseBlock();
        }
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
            return block.world.setBlock(block.x, block.y, block.z, state);
        } catch (WorldEditException e) {
            throw new RuntimeException(e);
        }
    }

    public CompoundTag getNbtData() {
        return state.getNbtData();
    }

    public void setNbtData(CompoundTag nbt) {
        state = this.state.toBaseBlock(nbt);
    }

    @Override
    public byte getRawData() {
        return (byte) (state.getInternalId() >> BlockTypes.BIT_OFFSET);
    }

    @Override
    public void setRawData(byte data) {
        int combinedId = getTypeId() + (data << BlockTypes.BIT_OFFSET);
        state = com.sk89q.worldedit.world.block.BlockState.getFromInternalId(combinedId).toBaseBlock(state.getNbtData());
        this.blockData = BukkitAdapter.adapt(state);
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
