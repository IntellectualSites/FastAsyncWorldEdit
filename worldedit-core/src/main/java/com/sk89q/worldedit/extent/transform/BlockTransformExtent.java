package com.sk89q.worldedit.extent.transform;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.extent.ResettableExtent;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.ByteTag;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.internal.helper.MCDirections;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.registry.state.AbstractProperty;
import com.sk89q.worldedit.registry.state.DirectionalProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.block.*;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.registry.BlockRegistry;
import jdk.nashorn.internal.ir.Block;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Transforms blocks themselves (but not their position) according to a
 * given transform.
 */
public class BlockTransformExtent extends ResettableExtent {
    private Transform transform;
    private Transform transformInverse;
    private int[] BLOCK_ROTATION_BITMASK;
    private int[][] BLOCK_TRANSFORM;
    private int[][] BLOCK_TRANSFORM_INVERSE;
    private int[] ALL = new int[0];

    public BlockTransformExtent(Extent parent) {
        this(parent, new AffineTransform());
    }

    public BlockTransformExtent(Extent parent, Transform transform) {
        super(parent);
        this.transform = transform;
        this.transformInverse = this.transform.inverse();
        cache();
    }

    private List<Direction> getDirections(AbstractProperty property) {
        if (property instanceof DirectionalProperty) {
            DirectionalProperty directional = (DirectionalProperty) property;
            directional.getValues();
        } else {
            switch (property.getKey()) {
                case HALF:

                case ROTATION:

                case AXIS:

                case FACING:

                case SHAPE:

                case NORTH:
                case EAST:
                case SOUTH:
                case WEST:
            }
        }
        return null;
    }

    @Nullable
    private static Integer getNewStateIndex(Transform transform, List<Direction> directions, int oldIndex) {
        Direction oldDirection = directions.get(oldIndex);
        Vector oldVector = oldDirection.toVector();
        Vector newVector = transform.apply(oldVector).subtract(transform.apply(Vector.ZERO)).normalize();
        int newIndex = oldIndex;
        double closest = oldVector.toVector().normalize().dot(newVector);
        boolean found = false;

        for (int i = 0; i < directions.size(); i++) {
            Direction v = directions.get(i);
            double dot = v.toVector().normalize().dot(newVector);
            if (dot > closest) {
                closest = dot;
                newIndex = i;
                found = true;
            }
        }

        if (found) {
            return newIndex;
        } else {
            return null;
        }
    }

    private void cache() {
        BLOCK_ROTATION_BITMASK = new int[BlockTypes.size()];
        BLOCK_TRANSFORM = new int[BlockTypes.size()][];
        BLOCK_TRANSFORM_INVERSE = new int[BlockTypes.size()][];
        outer:
        for (int i = 0; i < BLOCK_TRANSFORM.length; i++) {
            BLOCK_TRANSFORM[i] = ALL;
            BLOCK_TRANSFORM_INVERSE[i] = ALL;
            BlockTypes type = BlockTypes.get(i);
            int bitMask = 0;
            for (AbstractProperty property : (Collection<AbstractProperty>) type.getProperties()) {
                Collection<Direction> directions = getDirections(property);
                if (directions != null) {
                    BLOCK_TRANSFORM[i] = null;
                    BLOCK_TRANSFORM_INVERSE[i] = null;
                    bitMask |= property.getBitMask();
                }
            }
            if (bitMask != 0) {
                BLOCK_ROTATION_BITMASK[i] = bitMask;
            }
        }
    }

    @Override
    public ResettableExtent setExtent(Extent extent) {
        return super.setExtent(extent);
    }

    public Transform getTransform() {
        return transform;
    }

    public void setTransform(Transform affine) {
        this.transform = affine;
        this.transformInverse = this.transform.inverse();
        cache();
    }

    private final BlockState transform(BlockState state, int[][] transformArray, Transform transform) {
        int typeId = state.getInternalBlockTypeId();
        int[] arr = transformArray[typeId];
        if (arr == ALL) return state;
        if (arr == null) {
            arr = transformArray[typeId] = new int[state.getBlockType().getMaxStateId() + 1];
            Arrays.fill(arr, -1);
        }
        int mask = BLOCK_ROTATION_BITMASK[typeId];
        int internalId = state.getInternalId();

        int maskedId = internalId & mask;
        int newMaskedId = arr[maskedId];
        if (newMaskedId != -1) {
            return BlockState.get(newMaskedId | (internalId & (~mask)));
        }
        newMaskedId = state.getInternalId();

        BlockTypes type = state.getBlockType();
        for (AbstractProperty property : (Collection<AbstractProperty>) type.getProperties()) {
            List<Direction> directions = getDirections(property);
            if (directions != null) {
                Integer newIndex = getNewStateIndex(transform, directions, property.getIndex(state.getInternalId()));
                if (newIndex != null) {
                    newMaskedId = property.modifyIndex(newMaskedId, newIndex);
                }
            }
        }
        arr[maskedId] = newMaskedId & mask;
        return BlockState.get(newMaskedId);
    }

    public final BlockState transformFast(BlockState block) {
        BlockState transformed = transform(block, BLOCK_TRANSFORM, transform);
        if (block.hasNbtData()) {
            CompoundTag tag = block.getNbtData();
            if (tag.containsKey("Rot")) {
                int rot = tag.asInt("Rot");

                Direction direction = MCDirections.fromRotation(rot);

                if (direction != null) {
                    Vector applyAbsolute = transform.apply(direction.toVector());
                    Vector applyOrigin = transform.apply(Vector.ZERO);
                    applyAbsolute.mutX(applyAbsolute.getX() - applyOrigin.getX());
                    applyAbsolute.mutY(applyAbsolute.getY() - applyOrigin.getY());
                    applyAbsolute.mutZ(applyAbsolute.getZ() - applyOrigin.getZ());

                    Direction newDirection = Direction.findClosest(applyAbsolute, Direction.Flag.CARDINAL | Direction.Flag.ORDINAL | Direction.Flag.SECONDARY_ORDINAL);

                    if (newDirection != null) {
                        Map<String, Tag> values = ReflectionUtils.getMap(tag.getValue());
                        values.put("Rot", new ByteTag((byte) MCDirections.toRotation(newDirection)));
                    }
                }
                transformed = new BaseBlock(transformed, tag);
            }
        }
        return transformed;
    }

    public final BlockState transformFastInverse(BlockState block) {
        BlockState transformed = transform(block, BLOCK_TRANSFORM_INVERSE, transformInverse);
        if (block.hasNbtData()) {
            CompoundTag tag = block.getNbtData();
            if (tag.containsKey("Rot")) {
                int rot = tag.asInt("Rot");

                Direction direction = MCDirections.fromRotation(rot);

                if (direction != null) {
                    Vector applyAbsolute = transformInverse.apply(direction.toVector());
                    Vector applyOrigin = transformInverse.apply(Vector.ZERO);
                    applyAbsolute.mutX(applyAbsolute.getX() - applyOrigin.getX());
                    applyAbsolute.mutY(applyAbsolute.getY() - applyOrigin.getY());
                    applyAbsolute.mutZ(applyAbsolute.getZ() - applyOrigin.getZ());

                    Direction newDirection = Direction.findClosest(applyAbsolute, Direction.Flag.CARDINAL | Direction.Flag.ORDINAL | Direction.Flag.SECONDARY_ORDINAL);

                    if (newDirection != null) {
                        Map<String, Tag> values = ReflectionUtils.getMap(tag.getValue());
                        values.put("Rot", new ByteTag((byte) MCDirections.toRotation(newDirection)));
                    }
                }
            }
            transformed = new BaseBlock(transformed, tag);
        }
        return transformed;
    }

    @Override
    public BlockState getLazyBlock(int x, int y, int z) {
        return transformFast(super.getLazyBlock(x, y, z));
    }

    @Override
    public BlockState getLazyBlock(Vector position) {
        return transformFast(super.getLazyBlock(position));
    }

    @Override
    public BlockState getBlock(Vector position) {
        return transformFast(super.getBlock(position));
    }

    @Override
    public BaseBiome getBiome(Vector2D position) {
        return super.getBiome(position);
    }

    @Override
    public boolean setBlock(int x, int y, int z, BlockStateHolder block) throws WorldEditException {
        return super.setBlock(x, y, z, transformFastInverse((BlockState) block));
    }


    @Override
    public boolean setBlock(Vector location, BlockStateHolder block) throws WorldEditException {
        return super.setBlock(location, transformFastInverse((BlockState) block));
    }

    public static Class<?> inject() {
        return BlockTransformExtent.class;
    }
}