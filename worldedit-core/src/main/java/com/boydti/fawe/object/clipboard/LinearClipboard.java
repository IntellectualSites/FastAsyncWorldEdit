package com.boydti.fawe.object.clipboard;

import com.boydti.fawe.jnbt.NBTStreamer;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Best used when clipboard selections are small, or using legacy formats
 * (Small being < Integer.MAX_VALUE/BLOCK_SIZE_BYTES blocks)
 */
public abstract class LinearClipboard implements Clipboard, Closeable {
    private final BlockVector3 size;
    private final int area;
    private final int volume;
    private BlockVector3 origin;

    public LinearClipboard(BlockVector3 dimensions) {
        this.size = dimensions;
        long longVolume = (long) getWidth() * (long) getHeight() * (long) getLength();
        if (longVolume >= Integer.MAX_VALUE >> 2) {
            throw new IllegalArgumentException("Dimensions are too large for this clipboard format.");
        }
        this.area = getWidth() * getLength();
        this.volume = (int) longVolume;
        this.origin = BlockVector3.ZERO;
    }

    public abstract <B extends BlockStateHolder<B>> boolean setBlock(int i, B block);

    public abstract BaseBlock getFullBlock(int i);

    public abstract void setBiome(int index, BiomeType biome);

    public abstract BiomeType getBiome(int index);

    public void setOrigin(BlockVector3 offset) {
        this.origin = offset;
    }

    @Override
    public BlockVector3 getOrigin() {
        return origin;
    }

    @Override
    public BlockVector3 getMinimumPoint() {
        return BlockVector3.ZERO;
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return size.subtract(BlockVector3.ONE);
    }

    @Override
    public Region getRegion() {
        return new CuboidRegion(BlockVector3.at(0, 0, 0), BlockVector3.at(getWidth() - 1, getHeight() - 1, getLength() - 1));
    }

    public final BlockVector3 getDimensions() {
        return size;
    }

    public final int getWidth() {
        return size.getBlockX();
    }

    public final int getHeight() {
        return size.getBlockY();
    }

    public final int getLength() {
        return size.getBlockZ();
    }

    public int getArea() {
        return area;
    }

    public int getVolume() {
        return volume;
    }

    /**
     * The locations provided are relative to the clipboard min
     *
     * @param task
     * @param air
     */
    public abstract void forEach(BlockReader task, boolean air);

    public interface BlockReader {
        <B extends BlockStateHolder<B>> void run(int x, int y, int z, B block);
    }

    public abstract void streamBiomes(NBTStreamer.ByteReader task);

    public void streamOrdinals(NBTStreamer.ByteReader task) {
        forEach(new BlockReader() {
            private int index;

            @Override
            public <B extends BlockStateHolder<B>> void run(int x, int y, int z, B block) {
                task.run(index++, block.getOrdinal());
            }
        }, true);
    }

    public List<CompoundTag> getTileEntities() {
        final List<CompoundTag> tiles = new ArrayList<>();
        forEach(new BlockReader() {

            @Override
            public <B extends BlockStateHolder<B>> void run(int x, int y, int z, B block) {
                if(!(block instanceof BaseBlock)) return;
                BaseBlock base = (BaseBlock)block;
                CompoundTag tag = base.getNbtData();
                if (tag != null) {
                    Map<String, Tag> values = ReflectionUtils.getMap(tag.getValue());
                    values.put("x", new IntTag(x));
                    values.put("y", new IntTag(y));
                    values.put("z", new IntTag(z));
                    tiles.add(tag);
                }
            }
        }, false);
        return tiles;
    }

    public void close() {}

    public void flush() {}

    @Override
    protected void finalize() {
        close();
    }
}
