package com.boydti.fawe.object.clipboard;

import com.boydti.fawe.beta.IBatchProcessor;
import com.boydti.fawe.jnbt.NBTStreamer;
import com.boydti.fawe.object.changeset.FaweChangeSet;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.generator.GenBase;
import com.sk89q.worldedit.function.generator.Resource;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.Countable;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class DelegateClipboard implements Clipboard {
    private final Clipboard parent;

    public DelegateClipboard(Clipboard parent) {
        this.parent = parent;
    }

    public Clipboard getParent() {
        return parent;
    }

    @Override
    public URI getURI() {
        return parent.getURI();
    }

    @Override
    public void setOrigin(BlockVector3 offset) {
        parent.setOrigin(offset);
    }

    @Override
    public BlockVector3 getDimensions() {
        return parent.getDimensions();
    }

    @Override
    public Region getRegion() {
        return parent.getRegion();
    }

    @Override
    public BlockVector3 getOrigin() {
        return parent.getOrigin();
    }

    @Override
    public boolean hasBiomes() {
        return parent.hasBiomes();
    }

    @Override
    public void removeEntity(Entity entity) {
        parent.removeEntity(entity);
    }

    @Override
    public BlockVector3 getMinimumPoint() {
        return parent.getMinimumPoint();
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return parent.getMaximumPoint();
    }

    @Override
    public List<? extends Entity> getEntities(Region region) {
        return parent.getEntities(region);
    }

    @Override
    public List<? extends Entity> getEntities() {
        return parent.getEntities();
    }

    @Override
    @Nullable
    public Entity createEntity(Location location, BaseEntity entity) {
        return parent.createEntity(location, entity);
    }

    @Override
    @Nullable
    public void removeEntity(int x, int y, int z, UUID uuid) {
        parent.removeEntity(x, y, z, uuid);
    }

    @Override
    public boolean isWorld() {
        return parent.isWorld();
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        return parent.getBlock(position);
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return parent.getBlock(x, y, z);
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        return parent.getFullBlock(position);
    }

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        return parent.getFullBlock(x, y, z);
    }

    @Override
    public BiomeType getBiome(BlockVector2 position) {
        return parent.getBiome(position);
    }

    @Override
    public BiomeType getBiomeType(int x, int z) {
        return parent.getBiomeType(x, z);
    }

    @Override
    @Deprecated
    public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 position, T block) throws WorldEditException {
        return parent.setBlock(position, block);
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T block) throws WorldEditException {
        return parent.setBlock(x, y, z, block);
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tile) throws WorldEditException {
        return parent.setTile(x, y, z, tile);
    }

    @Override
    public boolean setBiome(BlockVector2 position, BiomeType biome) {
        return parent.setBiome(position, biome);
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        return parent.setBiome(x, y, z, biome);
    }
}
