package com.sk89q.worldedit.extent.transform;

import com.boydti.fawe.object.extent.ResettableExtent;
import com.boydti.fawe.util.ReflectionUtils;
import com.boydti.fawe.util.StringMan;
import com.sk89q.jnbt.ByteTag;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.internal.helper.MCDirections;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.registry.state.AbstractProperty;
import com.sk89q.worldedit.registry.state.DirectionalProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class BlockTransformExtent2 extends ResettableExtent {
    private Transform transform;
    private Transform transformInverse;
    private int[] BLOCK_ROTATION_BITMASK;
    private int[][] BLOCK_TRANSFORM;
    private int[][] BLOCK_TRANSFORM_INVERSE;
    private int[] ALL = new int[0];

    public BlockTransformExtent2(Extent parent) {
        this(parent, new AffineTransform());
    }

    public BlockTransformExtent2(Extent parent, Transform transform) {
        super(parent);
        this.transform = transform;
        this.transformInverse = this.transform.inverse();
        cache();
    }


    private int combine(Direction... directions) {
        int mask = 0;
        for (Direction dir : directions) {
            mask = mask & (1 << dir.ordinal());
        }
        return mask;
    }

    private int[] adapt(Direction... dirs) {
        int[] arr = new int[dirs.length];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = 1 << dirs[i].ordinal();
        }
        return arr;
    }

    private int[] adapt(int... dirs) {
        int[] arr = new int[dirs.length];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = dirs[i];
        }
        return arr;
    }

    private int[] getDirections(AbstractProperty property) {
        if (property instanceof DirectionalProperty) {
            DirectionalProperty directional = (DirectionalProperty) property;
            directional.getValues();
        } else {
            List values = property.getValues();
            switch (property.getKey()) {
                case HALF:
                    return adapt(Direction.UP, Direction.DOWN);
                case ROTATION: {
                    List<Direction> directions = new ArrayList<>();
                    for (Object value : values) {
                        directions.add(Direction.fromRotationIndex((Integer) value).get());
                    }
                    return adapt(directions.toArray(new Direction[0]));
                }
                case AXIS:
                    switch (property.getValues().size()) {
                        case 3:
                            return adapt(Direction.EAST, Direction.UP, Direction.SOUTH);
                        case 2:
                            return adapt(combine(Direction.EAST, Direction.WEST), combine(Direction.SOUTH, Direction.NORTH));
                        default:
                            System.out.println("Invalid " + property.getName() + " " + property.getValues());
                            return null;
                    }
                case FACING: {
                    List<Direction> directions = new ArrayList<>();
                    for (Object value : values) {
                        directions.add(Direction.valueOf(value.toString().toUpperCase()));
                    }
                    return adapt(directions.toArray(new Direction[0]));
                }
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
        Vector3 oldVector = oldDirection.toVector();
        Vector3 newVector = transform.apply(oldVector).subtract(transform.apply(Vector3.ZERO)).normalize();
        int newIndex = oldIndex;
        double closest = oldVector.normalize().dot(newVector);
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

    private boolean isDirectional(Property property) {
        if (property instanceof DirectionalProperty) {
            return true;
        }
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
                return true;
            default:
                return false;
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
            BlockType type = BlockTypes.get(i);
            int bitMask = 0;
            for (AbstractProperty property : (Collection<AbstractProperty>) (Collection) type.getProperties()) {
                if (isDirectional(property)) {
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
            return BlockState.getFromInternalId(newMaskedId | (internalId & (~mask)));
        }
        newMaskedId = state.getInternalId();

        BlockType type = state.getBlockType();
        for (AbstractProperty property : (Collection<AbstractProperty>) (Collection) type.getProperties()) {
            if (isDirectional(property)) {
                List<Direction> directions = getDirections(property);
                if (directions != null) {
                    Integer newIndex = getNewStateIndex(transform, directions, property.getIndex(state.getInternalId()));
                    if (newIndex != null) {
                        newMaskedId = property.modifyIndex(newMaskedId, newIndex);
                    }
                }
            }
        }
        arr[maskedId] = newMaskedId & mask;
        return BlockState.getFromInternalId(newMaskedId);
    }

    public final BaseBlock transformFast(BaseBlock block) {
        BlockState transformed = transformFast(block.toImmutableState());
        return transformFastWith(transformed, block.getNbtData(), transform);
    }

    public final BaseBlock transformInverse(BaseBlock block) {
        BlockState transformed = transformFastInverse(block.toImmutableState());
        return transformFastWith(transformed, block.getNbtData(), transformInverse);
    }

    public final BaseBlock transformFastWith(BlockState transformed, CompoundTag tag, Transform transform) {
        if (tag != null) {
            if (tag.containsKey("Rot")) {
                int rot = tag.asInt("Rot");

                Direction direction = MCDirections.fromRotation(rot);

                if (direction != null) {
                    Vector3 applyAbsolute = transform.apply(direction.toVector());
                    Vector3 applyOrigin = transform.apply(Vector3.ZERO);
                    applyAbsolute.mutX(applyAbsolute.getX() - applyOrigin.getX());
                    applyAbsolute.mutY(applyAbsolute.getY() - applyOrigin.getY());
                    applyAbsolute.mutZ(applyAbsolute.getZ() - applyOrigin.getZ());

                    Direction newDirection = Direction.findClosest(applyAbsolute, Direction.Flag.CARDINAL | Direction.Flag.ORDINAL | Direction.Flag.SECONDARY_ORDINAL);

                    if (newDirection != null) {
                        Map<String, Tag> values = ReflectionUtils.getMap(tag.getValue());
                        values.put("Rot", new ByteTag((byte) MCDirections.toRotation(newDirection)));
                    }
                }
                return new BaseBlock(transformed, tag);
            }
        }
        return transformed.toBaseBlock();
    }

    public final BlockState transformFast(BlockState block) {
        return transform(block, BLOCK_TRANSFORM, transform);
    }

    public final BlockState transformFastInverse(BlockState block) {
        return transform(block, BLOCK_TRANSFORM_INVERSE, transformInverse);
    }

    @Override
    public BlockState getLazyBlock(int x, int y, int z) {
        return transformFast(super.getLazyBlock(x, y, z));
    }

    @Override
    public BlockState getLazyBlock(BlockVector3 position) {
        return transformFast(super.getLazyBlock(position));
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        return transformFast(super.getBlock(position));
    }

    @Override
    public BiomeType getBiome(BlockVector2 position) {
        return super.getBiome(position);
    }

    @Override
    public boolean setBlock(int x, int y, int z, BlockStateHolder block) throws WorldEditException {
        return super.setBlock(x, y, z, transformFastInverse((BlockState) block));
    }


    @Override
    public boolean setBlock(BlockVector3 location, BlockStateHolder block) throws WorldEditException {
        return super.setBlock(location, transformFastInverse((BlockState) block));
    }


}