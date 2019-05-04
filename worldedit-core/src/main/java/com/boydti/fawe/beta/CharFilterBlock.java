package com.boydti.fawe.beta;

import com.boydti.fawe.beta.implementation.blocks.CharGetBlocks;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.registry.BlockMaterial;

import javax.annotation.Nullable;

import static com.sk89q.worldedit.world.block.BlockTypes.states;
public class CharFilterBlock implements FilterBlock {
    private IQueueExtent queue;
    private CharGetBlocks get;

    private ISetBlocks set;

    private char[] getArr;
    private @Nullable char[] setArr;
    private SetDelegate delegate;

    // local
    private int layer, index, x, y, z, xx, yy, zz, X, Z;

    @Override
    public final FilterBlock init(final IQueueExtent queue) {
        this.queue = queue;
        return this;
    }

    @Override
    public final FilterBlock init(final int X, final int Z, final IGetBlocks chunk) {
        this.get = (CharGetBlocks) chunk;
        this.X = X;
        this.Z = Z;
        this.xx = X << 4;
        this.zz = Z << 4;
        return this;
    }

    @Override
    public final void filter(final IGetBlocks iget, final ISetBlocks iset, final int layer, final Filter filter, final @Nullable Region region, BlockVector3 min, BlockVector3 max) {
        this.layer = layer;
        final CharGetBlocks get = (CharGetBlocks) iget;
        if (!get.hasSection(layer)) return;
        this.set = iset;
        getArr = get.sections[layer].get(get, layer);
        if (set.hasSection(layer)) {
            setArr = set.getArray(layer);
            delegate = FULL;
        } else {
            delegate = NULL;
            setArr = null;
        }
        this.yy = layer << 4;
        if (region == null) {
            if (min != null && max != null) {
                iterate(min, max, layer, filter);
            } else {
                iterate(filter);
            }
        } else {
            if (min != null && max != null) {
                iterate(region, min, max, layer, filter);
            } else {
                iterate(region, filter);
            }
        }
    }

    private void iterate(final Region region, final Filter filter) {
        for (y = 0, index = 0; y < 16; y++) {
            int absY = yy + y;
            for (z = 0; z < 16; z++) {
                int absZ = zz + z;
                for (x = 0; x < 16; x++, index++) {
                    int absX = xx + x;
                    if (region.contains(absX, absY, absZ)) {
                        filter.applyBlock(this);
                    }
                }
            }
        }
    }

    private void iterate(final Region region, BlockVector3 min, BlockVector3 max, int layer, final Filter filter) {
        int by = Math.max(min.getY(), layer << 4) & 15;
        int ty = Math.min(max.getY(), 15 + (layer << 4)) & 15;
        int bx = min.getX();
        int bz = min.getZ();
        int tx = max.getX();
        int tz = max.getZ();
        for (y = by; y <= ty; y++) {
            int yIndex = (y << 8);
            int absY = yy + y;
            for (z = bz; z <= tz; z++) {
                int zIndex = yIndex + ((z) << 4);
                int absZ = zz + z;
                for (x = bx; x <= tx; x++) {
                    index = zIndex + x;
                    int absX = xx + x;
                    if (region.contains(absX, absY, absZ)) {
                        filter.applyBlock(this);
                    }

                }
            }
        }
    }

    private void iterate(BlockVector3 min, BlockVector3 max, int layer, final Filter filter) {
        int by = Math.max(min.getY(), layer << 4) & 15;
        int ty = Math.min(max.getY(), 15 + (layer << 4)) & 15;
        int bx = min.getX();
        int bz = min.getZ();
        int tx = max.getX();
        int tz = max.getZ();
        for (y = by; y <= ty; y++) {
            int yIndex = (y << 8);
            for (z = bz; z <= tz; z++) {
                int zIndex = yIndex + ((z) << 4);
                for (x = bx; x <= tx; x++) {
                    index = zIndex + x;
                    filter.applyBlock(this);
                }
            }
        }
    }

    private final void iterate(final Filter filter) {
        for (y = 0, index = 0; y < 16; y++) {
            for (z = 0; z < 16; z++) {
                for (x = 0; x < 16; x++, index++) {
                    filter.applyBlock(this);
                }
            }
        }
    }

    @Override
    public void setOrdinal(final int ordinal) {
        delegate.set(this, (char) ordinal);
    }

    @Override
    public void setState(final BlockState state) {
        delegate.set(this, state.getOrdinalChar());
    }

    @Override
    public void setFullBlock(final BaseBlock block) {
        delegate.set(this, block.getOrdinalChar());
        final CompoundTag nbt = block.getNbtData();
        if (nbt != null) { // TODO optimize check via ImmutableBaseBlock
            set.setTile(x, yy + y, z, nbt);
        }
    }

    @Override
    public final int getX() {
        return xx + x;
    }

    @Override
    public final int getY() {
        return yy + y;
    }

    @Override
    public final int getZ() {
        return zz + z;
    }

    @Override
    public final int getLocalX() {
        return x;
    }

    @Override
    public final int getLocalY() {
        return y;
    }

    @Override
    public final int getLocalZ() {
        return z;
    }

    @Override
    public final int getChunkX() {
        return X;
    }

    @Override
    public final int getChunkZ() {
        return Z;
    }

    public final char getOrdinalChar() {
        return getArr[index];
    }

    @Override
    public final int getOrdinal() {
        return getArr[index];
    }

    @Override
    public final BlockState getState() {
        final int ordinal = getArr[index];
        return BlockTypes.states[ordinal];
    }

    @Override
    public final BaseBlock getBaseBlock() {
        final BlockState state = getState();
        final BlockMaterial material = state.getMaterial();
        if (material.hasContainer()) {
            final CompoundTag tag = get.getTag(x, y + (layer << 4), z);
            return state.toBaseBlock(tag);
        }
        return state.toBaseBlock();
    }

    @Override
    public final CompoundTag getTag() {
        return null;
    }

    public final BlockState getOrdinalBelow() {
        if (y > 0) {
            return states[getArr[index - 256]];
        }
        if (layer > 0) {
            final int newLayer = layer - 1;
            final CharGetBlocks chunk = this.get;
            return states[chunk.sections[newLayer].get(chunk, newLayer, index + 3840)];
        }
        return BlockTypes.__RESERVED__.getDefaultState();
    }

    public final BlockState getStateAbove() {
        if (y < 16) {
            return states[getArr[index + 256]];
        }
        if (layer < 16) {
            final int newLayer = layer + 1;
            final CharGetBlocks chunk = this.get;
            return states[chunk.sections[newLayer].get(chunk, newLayer, index - 3840)];
        }
        return BlockTypes.__RESERVED__.getDefaultState();
    }

    public final BlockState getStateRelativeY(final int y) {
        final int newY = this.y + y;
        final int layerAdd = newY >> 4;
        switch (layerAdd) {
            case 0:
                return states[getArr[this.index + (y << 8)]];
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15: {
                final int newLayer = layer + layerAdd;
                if (newLayer < 16) {
                    final int index = this.index + ((y & 15) << 8);
                    return states[get.sections[newLayer].get(get, newLayer, index)];
                }
                break;
            }
            case -1:
            case -2:
            case -3:
            case -4:
            case -5:
            case -6:
            case -7:
            case -8:
            case -9:
            case -10:
            case -11:
            case -12:
            case -13:
            case -14:
            case -15: {
                final int newLayer = layer + layerAdd;
                if (newLayer >= 0) {
                    final int index = this.index + ((y & 15) << 8);
                    return states[get.sections[newLayer].get(get, newLayer, index)];
                }
                break;
            }
        }
        return BlockTypes.__RESERVED__.getDefaultState();
    }

    public final BlockState getStateRelative(final int x, final int y, final int z) {
        final int newX = this.x + x;
        if (newX >> 4 == 0) {
            final int newZ = this.z + z;
            if (newZ >> 4 == 0) {
                final int newY = this.y + y;
                final int layerAdd = newY >> 4;
                switch (layerAdd) {
                    case 0:
                        return states[getArr[this.index + ((y << 8) + (z << 4) + x)]];
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                    case 8:
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                    case 13:
                    case 14:
                    case 15: {
                        final int newLayer = layer + layerAdd;
                        if (newLayer < 16) {
                            final int index = ((newY & 15) << 8) + (newZ << 4) + newX;
                            return states[get.sections[newLayer].get(get, newLayer, index)];
                        }
                        break;
                    }
                    case -1:
                    case -2:
                    case -3:
                    case -4:
                    case -5:
                    case -6:
                    case -7:
                    case -8:
                    case -9:
                    case -10:
                    case -11:
                    case -12:
                    case -13:
                    case -14:
                    case -15: {
                        final int newLayer = layer + layerAdd;
                        if (newLayer >= 0) {
                            final int index = ((newY & 15) << 8) + (newZ << 4) + newX;
                            return states[get.sections[newLayer].get(get, newLayer, index)];
                        }
                        break;
                    }
                }
                return BlockTypes.__RESERVED__.getDefaultState();
            }
        }
//        queue.get
        // TODO return normal get block
        final int newY = this.y + y + yy;
        if (newY >= 0 && newY <= 256) {
            return queue.getBlock(xx + newX,  newY, this.zz + this.z + z);
        }
        return BlockTypes.__RESERVED__.getDefaultState();
    }

    /*
    Set delegate
     */
    private SetDelegate initSet() {
        setArr = set.getArray(layer);
        return delegate = FULL;
    }

    private interface SetDelegate {
        void set(CharFilterBlock block, char value);
    }

    private static final SetDelegate NULL = new SetDelegate() {
        @Override
        public void set(final CharFilterBlock block, final char value) {
            block.initSet().set(block, value);
        }
    };

    private static final SetDelegate FULL = new SetDelegate() {
        @Override
        public final void set(final CharFilterBlock block, final char value) {
            block.setArr[block.index] = value;
        }
    };
}
