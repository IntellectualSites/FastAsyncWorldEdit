package com.fastasyncworldedit.bukkit.adapter;

import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;

@ApiStatus.Internal
public abstract class BukkitBlockMaterial<B, BS> implements BlockMaterial {

    protected final B block;
    protected final BS blockState;
    private final BlockData blockData;
    private final Material craftMaterial;
    private final FaweCompoundTag tile;

    public BukkitBlockMaterial(B block, BS blockState, BlockData blockData) {
        this.block = block;
        this.blockState = blockState;
        this.blockData = blockData;
        this.craftMaterial = this.blockData.getMaterial();
        this.tile = tileForBlock(block);
    }

    protected abstract FaweCompoundTag tileForBlock(B block);

    public B getBlock() {
        return this.block;
    }

    public BS getState() {
        return this.blockState;
    }

    public BlockData getBlockData() {
        return this.blockData;
    }

    @Override
    public boolean isBurnable() {
        return this.craftMaterial.isBurnable();
    }

    @Override
    public @Nullable FaweCompoundTag defaultTile() {
        return this.tile;
    }

    @Override
    public boolean hasContainer() {
        return this.tile != null;
    }

    @Override
    public boolean isTile() {
        return this.tile != null;
    }

    @Override
    public boolean isToolRequired() {
        // Removed in 1.16.1, this is not present in higher versions
        return false;
    }

}
