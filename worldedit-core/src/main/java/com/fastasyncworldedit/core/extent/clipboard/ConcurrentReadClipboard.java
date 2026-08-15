package com.fastasyncworldedit.core.extent.clipboard;

import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.List;

@ApiStatus.Experimental // Temporary wrapper
public class ConcurrentReadClipboard implements Clipboard {

    private final Clipboard parent;

    public static Clipboard tryWrap(Clipboard clipboard) {
        if (clipboard.supportsParallelAccess()) {
            return clipboard;
        }
        return new ConcurrentReadClipboard(clipboard);
    }

    private ConcurrentReadClipboard(Clipboard parent) {
        this.parent = parent;
    }

    @Override
    public synchronized BlockState getBlock(BlockVector3 position) {
        return parent.getBlock(position);
    }

    @Override
    public synchronized BlockState getBlock(int x, int y, int z) {
        return parent.getBlock(x, y, z);
    }

    @Override
    public synchronized BaseBlock getFullBlock(BlockVector3 position) {
        return parent.getFullBlock(position);
    }

    @Override
    public synchronized BaseBlock getFullBlock(int x, int y, int z) {
        return parent.getFullBlock(x, y, z);
    }

    @Override
    public BiomeType getBiomeType(int x, int y, int z) {
        return parent.getBiomeType(x, y, z);
    }

    @Override
    public BiomeType getBiome(BlockVector3 position) {
        return parent.getBiome(position);
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(int x, int y, int z, B block) {
        throw new UnsupportedOperationException("Wrapper not intended for writing");
    }

    @Override
    public boolean tile(int x, int y, int z, FaweCompoundTag tile) {
        throw new UnsupportedOperationException("Wrapper not intended for writing");
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        throw new UnsupportedOperationException("Wrapper not intended for writing");
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        throw new UnsupportedOperationException("Wrapper not intended for writing");
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
    public Region getRegion() {
        return parent.getRegion();
    }

    @Override
    public BlockVector3 getDimensions() {
        return parent.getDimensions();
    }

    @Override
    public BlockVector3 getOrigin() {
        return parent.getOrigin();
    }

    @Override
    public void setOrigin(BlockVector3 origin) {
        throw new UnsupportedOperationException("Wrapper not intended for writing");
    }

    @Override
    public boolean hasBiomes() {
        return parent.hasBiomes();
    }

    @Override
    public void removeEntity(Entity entity) {
        throw new UnsupportedOperationException("Wrapper not intended for writing");
    }

    @Override
    @Nonnull
    public Iterator<BlockVector3> iterator() {
        return parent.iterator();
    }

    @Override
    public boolean supportsParallelAccess() {
        return true;
    }

    @Override
    public void close() {
        parent.close();
    }

}
