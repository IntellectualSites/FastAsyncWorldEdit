package com.boydti.fawe.bukkit.wrapper;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.material.MaterialData;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AsyncBlockState implements BlockState {

    private BaseBlock state;
    private BlockData blockData;
    private final AsyncBlock block;

    public AsyncBlockState(AsyncBlock block, BaseBlock state) {
        this.state = state;
        this.block = block;
        this.blockData = BukkitAdapter.adapt(state);
    }

    public int getTypeId() {
        return state.getBlockType().getInternalId();
    }

    public int getPropertyId() {
        return state.getInternalId() >> BlockTypesCache.BIT_OFFSET;
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
        CompoundTag nbt = this.getNbtData();
        BlockType oldType = state.getBlockType();
        com.sk89q.worldedit.world.block.BlockState newState = BukkitAdapter.adapt(blockData);
        if (nbt != null && newState.getBlockType() == oldType) {
            this.setNbtData(nbt);
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

    /**
     * Returns the (unmodifiable) tag compound that belongs to this block state.
     * If the block state is null, this will return null.
     *
     * @return NBT data
     */
    public synchronized @Nullable CompoundTag getNbtData() {
        if (this.state == null) {
            return null;
        }
        return state.getNbtData();
    }

    /**
     * Clone the NBT {@link CompoundTag} into a new {@link Map}.
     *
     * @return Modifiable clone of NBT data
     */
    public @NotNull Map<String, Tag> cloneNbtMap() {
        return Optional.ofNullable(this.getNbtData()).map(CompoundTag::getValue)
            .map(HashMap::new).orElse(new HashMap<>());
    }

    /**
     * Set the NBT data of the block.
     *
     * @param nbt New NBT data
     */
    public synchronized void setNbtData(@Nullable final CompoundTag nbt) {
        state = this.state.toBaseBlock(nbt);
    }

    /**
     * Set the NBT data of the block.
     *
     * @param map New NBT data
     */
    public void setNbtData(@NotNull final Map<String, Tag> map) {
        this.setNbtData(new CompoundTag(map));
    }

    @Override
    public byte getRawData() {
        return (byte) (state.getInternalId() >> BlockTypesCache.BIT_OFFSET);
    }

    @Override
    public void setRawData(byte data) {
        int combinedId = getTypeId() + (data << BlockTypesCache.BIT_OFFSET);
        state = com.sk89q.worldedit.world.block.BlockState.getFromInternalId(combinedId)
            .toBaseBlock(this.getNbtData());
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
