package com.boydti.fawe.beta;

import com.boydti.fawe.beta.implementation.blocks.CharGetBlocks;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import static com.sk89q.worldedit.world.block.BlockTypes.states;
public class CharFilterBlock implements FilterBlock {
    private IQueueExtent queue;
    private CharGetBlocks chunk;
    private char[] section;

    @Override
    public final void init(IQueueExtent queue) {
        this.queue = queue;
    }

    @Override
    public final void init(int X, int Z, IGetBlocks chunk) {
        this.chunk = (CharGetBlocks) chunk;
        this.X = X;
        this.Z = Z;
        this.xx = X << 4;
        this.zz = Z << 4;
    }

    // local
    private int layer, index, x, y, z, xx, yy, zz, X, Z;

    public final void filter(CharGetBlocks blocks, Filter filter) {
        for (int layer = 0; layer < 16; layer++) {
            if (!blocks.hasSection(layer)) continue;
            char[] arr = blocks.sections[layer].get(blocks, layer);

            this.section = arr;
            this.layer = layer;
            this.yy = layer << 4;

            for (y = 0, index = 0; y < 16; y++) {
                for (z = 0; z < 16; z++) {
                    for (x = 0; x < 16; x++, index++) {
                        filter.applyBlock(this);
                    }
                }
            }
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
        return section[index];
    }

    @Override
    public final int getOrdinal() {
        return section[index];
    }

    @Override
    public final BlockState getState() {
        int ordinal = section[index];
        return BlockTypes.states[ordinal];
    }

    @Override
    public final BaseBlock getBaseBlock() {
        BlockState state = getState();
        BlockMaterial material = state.getMaterial();
        if (material.hasContainer()) {
            CompoundTag tag = chunk.getTag(x, y + (layer << 4), z);
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
            return states[section[index - 256]];
        }
        if (layer > 0) {
            final int newLayer = layer - 1;
            final CharGetBlocks chunk = this.chunk;
            return states[chunk.sections[newLayer].get(chunk, newLayer, index + 3840)];
        }
        return BlockTypes.__RESERVED__.getDefaultState();
    }

    public final BlockState getStateAbove() {
        if (y < 16) {
            return states[section[index + 256]];
        }
        if (layer < 16) {
            final int newLayer = layer + 1;
            final CharGetBlocks chunk = this.chunk;
            return states[chunk.sections[newLayer].get(chunk, newLayer, index - 3840)];
        }
        return BlockTypes.__RESERVED__.getDefaultState();
    }

    public final BlockState getStateRelativeY(int y) {
        int newY = this.y + y;
        int layerAdd = newY >> 4;
        switch (layerAdd) {
            case 0:
                return states[section[this.index + (y << 8)]];
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
                int newLayer = layer + layerAdd;
                if (newLayer < 16) {
                    int index = this.index + ((y & 15) << 8);
                    return states[chunk.sections[newLayer].get(chunk, newLayer, index)];
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
                int newLayer = layer + layerAdd;
                if (newLayer >= 0) {
                    int index = this.index + ((y & 15) << 8);
                    return states[chunk.sections[newLayer].get(chunk, newLayer, index)];
                }
                break;
            }
        }
        return BlockTypes.__RESERVED__.getDefaultState();
    }

    public final BlockState getStateRelative(final int x, final int y, final int z) {
        int newX = this.x + x;
        if (newX >> 4 == 0) {
            int newZ = this.z + z;
            if (newZ >> 4 == 0) {
                int newY = this.y + y;
                int layerAdd = newY >> 4;
                switch (layerAdd) {
                    case 0:
                        return states[section[this.index + ((y << 8) + (z << 4) + x)]];
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
                        int newLayer = layer + layerAdd;
                        if (newLayer < 16) {
                            int index = ((newY & 15) << 8) + (newZ << 4) + newX;
                            return states[chunk.sections[newLayer].get(chunk, newLayer, index)];
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
                        int newLayer = layer + layerAdd;
                        if (newLayer >= 0) {
                            int index = ((newY & 15) << 8) + (newZ << 4) + newX;
                            return states[chunk.sections[newLayer].get(chunk, newLayer, index)];
                        }
                        break;
                    }
                }
                return BlockTypes.__RESERVED__.getDefaultState();
            }
        }
//        queue.get
        // TODO return normal get block
        int newY = this.y + y + yy;
        if (newY >= 0 && newY <= 256) {
            return queue.getBlock(xx + newX,  newY, this.zz + this.z + z);
        }
        return BlockTypes.__RESERVED__.getDefaultState();
    }
}
