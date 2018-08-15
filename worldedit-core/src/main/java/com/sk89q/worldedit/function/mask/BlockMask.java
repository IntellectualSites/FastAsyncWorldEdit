package com.sk89q.worldedit.function.mask;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.collection.FastBitSet;
import com.boydti.fawe.util.MainUtil;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.registry.state.AbstractProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A mask that checks whether blocks at the given positions are matched by
 * a block in a list.
 * <p>
 * <p>This mask checks for both an exact block ID and data value match, as well
 * for a block with the same ID but a data value of -1.</p>
 */
public class BlockMask extends AbstractExtentMask {

    private final long[][] bitSets;
    protected final static long[] ALL = new long[0];

    @Deprecated
    public BlockMask(Extent extent, BaseBlock... blocks) {
        super(extent);
        MainUtil.warnDeprecated(BlockMaskBuilder.class);
        this.bitSets = new BlockMaskBuilder().addBlocks(blocks).optimize().getBits();
    }

    protected BlockMask(Extent extent, long[][] bitSets) {
        super(extent);
        this.bitSets = bitSets;
    }

    @Override
    public Mask optimize() {
        Map<Object, Integer> states = new HashMap<>();
        int indexFound = -1;
        {
            int indexNull = -1;
            int indexAll = -1;
            for (int i = 0; i < bitSets.length; i++) {
                long[] bs = bitSets[i];
                if (bs == null) {
                    indexNull = i;
                    states.put(null, states.getOrDefault(null, 0) + 1);
                } else if (bs.length == 0) {
                    indexAll = i;
                    states.put(ALL, states.getOrDefault(ALL, 0) + 1);
                } else if (indexFound == -1) {
                    indexFound = i;
                } else {
                    return this;
                }
            }
            // Only types, no states
            if (indexFound == -1) {
                if (states.size() == 1) {
                    return states.keySet().iterator().next() == null ? Masks.alwaysFalse() : Masks.alwaysTrue();
                }
                if (states.get(ALL) == 1) return new SingleBlockTypeMask(getExtent(), BlockTypes.get(indexAll));
                if (states.get(null) == 1)
                    return new SingleBlockTypeMask(getExtent(), BlockTypes.get(indexNull)).inverse();

                boolean[] types = new boolean[BlockTypes.size()];
                for (int i = 0; i < bitSets.length; i++) {
                    if (bitSets[i].length == 0) types[i] = true;
                }
                return new BlockTypeMask(getExtent(), types);
            }
        }
        BlockType type = BlockTypes.get(indexFound);
        {
            Mask mask = getOptimizedMask(type, bitSets[indexFound]);
            if (mask == null) { // Try with inverse
                long[] newBitSet = bitSets[indexFound];
                for (int i = 0; i < newBitSet.length; i++) newBitSet[i] = ~newBitSet[i];
                mask = getOptimizedMask(type, bitSets[indexFound]);
                if (mask != null) mask = mask.inverse();
            }
            return mask;
        }
    }

    private Mask getOptimizedMask(BlockType type, long[] bitSet) {
        boolean single = true;
        int and = type.getInternalId();
        List<? extends Property> properties = type.getProperties();
        for (AbstractProperty prop : (List<AbstractProperty>) type.getProperties()) {
            List values = prop.getValues();
            int numSet = 0;
            for (int i = 0; i < values.size(); i++) {
                int localI = i << prop.getBitOffset();
                if (FastBitSet.get(bitSet, localI)) {
                    numSet++;
                    and |= prop.modify(and, i);
                }
            }
            // Cannot optimize multiple property values - use current mask (null)
            if (numSet != values.size() && numSet != 1) {
                return null;
            }
            single = single && numSet == 1;
        }
        if (single)
            return new SingleBlockStateMask(getExtent(), BlockState.get(and));
        return new SingleBlockStateBitMask(getExtent(), and);
    }

    @Override
    public Mask and(Mask other) {
        if (other instanceof BlockMask) {
            long[][] otherBitSets = ((BlockMask) other).bitSets;
            for (int i = 0; i < otherBitSets.length; i++) {
                long[] otherBitSet = otherBitSets[i];
                long[] bitSet = bitSets[i];
                if (otherBitSet == null) bitSets[i] = null;
                else if (otherBitSet.length == 0) continue;
                else if (bitSet == null) continue;
                else if (bitSet.length == 0) bitSets[i] = otherBitSet;
                else for (int j = 0; j < otherBitSet.length; j++) bitSet[j] &= otherBitSet[j];
            }
            return this;
        }
        if (other instanceof SingleBlockStateMask) {
            return new BlockMaskBuilder(bitSets).filter(((SingleBlockStateMask) other).getBlockState()).build(getExtent());
        }
        if (other instanceof SingleBlockTypeMask) {
            return new BlockMaskBuilder(bitSets).filter(((SingleBlockTypeMask) other).getBlockType()).build(getExtent());
        }
        return null;
    }

    @Override
    public Mask or(Mask other) {
        if (other instanceof BlockMask) {
            long[][] otherBitSets = ((BlockMask) other).bitSets;
            for (int i = 0; i < otherBitSets.length; i++) {
                long[] otherBitSet = otherBitSets[i];
                long[] bitSet = bitSets[i];
                if (otherBitSet == null) continue;
                else if (otherBitSet.length == 0) bitSets[i] = ALL;
                else if (bitSet == null) bitSets[i] = otherBitSet;
                else if (bitSet.length == 0) continue;
                else for (int j = 0; j < otherBitSet.length; j++) bitSet[j] |= otherBitSet[j];
            }
            return this;
        }
        if (other instanceof SingleBlockStateMask) {
            return new BlockMaskBuilder(bitSets).add(((SingleBlockStateMask) other).getBlockState()).build(getExtent());
        }
        if (other instanceof SingleBlockTypeMask) {
            return new BlockMaskBuilder(bitSets).add(((SingleBlockTypeMask) other).getBlockType()).build(getExtent());
        }
        return null;
    }

    @Override
    public Mask inverse() {
        for (int i = 0; i < bitSets.length; i++) {
            if (bitSets[i] == null) bitSets[i] = ALL;
            else if (bitSets[i] == ALL) bitSets[i] = null;
            else {
                for (int j = 0; j < bitSets[i].length; j++)
                    bitSets[i][j] = ~bitSets[i][j];
            }
        }
        return this;
    }

    @Override
    public boolean test(Vector vector) {
        BlockStateHolder block = getExtent().getBlock(vector);
        long[] bitSet = bitSets[block.getInternalBlockTypeId()];
        if (bitSet == null) return false;
        if (bitSet.length == 0) return true;
        return FastBitSet.get(bitSet, block.getInternalPropertiesId());
    }

    @Nullable
    @Override
    public Mask2D toMask2D() {
        return null;
    }


}