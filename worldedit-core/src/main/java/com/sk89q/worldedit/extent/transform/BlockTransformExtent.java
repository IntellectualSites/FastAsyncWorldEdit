package com.sk89q.worldedit.extent.transform;

import com.boydti.fawe.object.extent.ResettableExtent;
import com.boydti.fawe.util.ReflectionUtils;
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
import com.sk89q.worldedit.registry.state.PropertyKey;
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
import java.util.List;
import java.util.Map;

import static com.sk89q.worldedit.util.Direction.*;

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


    private static long combine(Direction... directions) {
        long mask = 0;
        for (Direction dir : directions) {
            mask = mask | (1L << dir.ordinal());
        }
        return mask;
    }

    private static long[] adapt(Direction... dirs) {
        long[] arr = new long[dirs.length];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = 1L << dirs[i].ordinal();
        }
        return arr;
    }

    private static long[] adapt(Long... dirs) {
        long[] arr = new long[dirs.length];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = dirs[i];
        }
        return arr;
    }

    private static long[] getDirections(AbstractProperty property) {
        if (property instanceof DirectionalProperty) {
            DirectionalProperty directional = (DirectionalProperty) property;
            return adapt(directional.getValues().toArray(new Direction[0]));
        } else {
            List values = property.getValues();
            switch (property.getKey()) {
                case HALF:
                    return adapt(UP, DOWN);
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
                            return adapt(EAST, UP, SOUTH);
                        case 2:
                            return adapt(combine(EAST, WEST), combine(SOUTH, NORTH));
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
                    if (values.contains("straight")) {
                        ArrayList<Long> result = new ArrayList<>();
                        for (Object value : values) {
                            // [straight, inner_left, inner_right, outer_left, outer_right]
                            switch (value.toString()) {
                                case "straight":
                                    result.add(combine(NORTH, EAST, SOUTH, WEST));
                                    continue;
                                case "inner_left":
                                    result.add(notIndex(combine(NORTHEAST, NORTHWEST, SOUTHWEST, SOUTHEAST), property.getIndexFor("outer_right"), property.getIndexFor("outer_left")));
                                    continue;
                                case "inner_right":
                                    result.add(notIndex(combine(NORTHEAST, NORTHWEST, SOUTHWEST, SOUTHEAST), property.getIndexFor("outer_right"), property.getIndexFor("outer_left")));
                                    continue;
                                case "outer_left":
                                    result.add(notIndex(combine(NORTHEAST, NORTHWEST, SOUTHWEST, SOUTHEAST), property.getIndexFor("inner_left"), property.getIndexFor("inner_right")));
                                    continue;
                                case "outer_right":
                                    result.add(notIndex(combine(NORTHEAST, NORTHWEST, SOUTHWEST, SOUTHEAST), property.getIndexFor("inner_left"), property.getIndexFor("inner_right")));
                                    continue;
                                default:
                                    System.out.println("Unknown direction " + value);
                                    result.add(0l);
                            }
                        }
                        return adapt(result.toArray(new Long[0]));
                    } else {
                        List<Long> directions = new ArrayList<>();
                        for (Object value : values) {
                            switch (value.toString()) {
                                case "north_south":
                                    directions.add(combine(NORTH, SOUTH));
                                    break;
                                case "east_west":
                                    directions.add(combine(EAST, WEST));
                                    break;
                                case "ascending_east":
                                    directions.add(combine(ASCENDING_EAST));
                                    break;
                                case "ascending_west":
                                    directions.add(combine(ASCENDING_WEST));
                                    break;
                                case "ascending_north":
                                    directions.add(combine(ASCENDING_NORTH));
                                    break;
                                case "ascending_south":
                                    directions.add(combine(ASCENDING_SOUTH));
                                    break;
                                case "south_east":
                                    directions.add(combine(SOUTHEAST));
                                    break;
                                case "south_west":
                                    directions.add(combine(SOUTHWEST));
                                    break;
                                case "north_west":
                                    directions.add(combine(NORTHWEST));
                                    break;
                                case "north_east":
                                    directions.add(combine(NORTHEAST));
                                    break;
                                default:
                                    System.out.println("Unknown direction " + value);
                                    directions.add(0l);
                            }
                        }
                        return adapt(directions.toArray(new Long[0]));
                    }
            }
        }
        return null;
    }

    private static boolean hasDirection(long mask, Direction dir) {
        return (mask & (1L << dir.ordinal())) != 0;
    }

    private static long notIndex(long mask, int... indexes) {
        for (int index : indexes) {
            mask = mask | (1L << (index + values().length));
        }
        return mask;
    }

    private static boolean hasIndex(long mask, int index) {
        return ((mask >> values().length) & (1 << index)) == 0;
    }

    @Nullable
    private static Integer getNewStateIndex(Transform transform, long[] directions, int oldIndex) {
        long oldDirMask = directions[oldIndex];
        if (oldDirMask == 0) {
            return null;
        }
        Integer newIndex = null;

        for (Direction oldDirection : values()) {
            if (!hasDirection(oldDirMask, oldDirection)) {
                continue;
            }
            Vector3 oldVector = oldDirection.toVector();
            Vector3 newVector = transform.apply(oldVector).subtract(transform.apply(Vector3.ZERO)).normalize();
            boolean flip = false;

            if (transform instanceof AffineTransform) {
                flip = ((AffineTransform) transform).isScaled(oldVector);
            }

            if (oldVector.equals(newVector)) {
                continue;
            }

            double closest = oldVector.normalize().dot(newVector);
            for (int i = 0; i < directions.length; i++) {
                int j = (oldIndex + i) % directions.length;
                long newDirMask = directions[j];
                if (!hasIndex(oldDirMask, j)) {
                    continue;
                }
                for (Direction v : Direction.values()) {
                    // Check if it's one of the current directions
                    if (!hasDirection(newDirMask, v)) {
                        continue;
                    }
                    // Check if the old mask excludes it
                    double dot = v.toVector().normalize().dot(newVector);
                    if (dot > closest || (flip && dot >= closest)) { //
                        closest = dot;
                        newIndex = j;
                    }
                }
            }
            if (newIndex != null) return newIndex;
        }
        return newIndex != null ? newIndex : null;
    }

    private static boolean isDirectional(Property property) {
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

    private static final BaseBlock transformBaseBlockNBT(BlockState transformed, CompoundTag tag, Transform transform) {
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

    private static int transformState(BlockState state, Transform transform) {
        int newMaskedId = state.getInternalId();

        BlockType type = state.getBlockType();
        // Rotate North, East, South, West
        if (type.hasProperty(PropertyKey.NORTH) && type.hasProperty(PropertyKey.EAST) && type.hasProperty(PropertyKey.SOUTH) && type.hasProperty(PropertyKey.WEST)) {
            Direction newNorth = findClosest(transform.apply(NORTH.toVector()), Flag.CARDINAL);
            Direction newEast = findClosest(transform.apply(EAST.toVector()), Flag.CARDINAL);
            Direction newSouth = findClosest(transform.apply(SOUTH.toVector()), Flag.CARDINAL);
            Direction newWest = findClosest(transform.apply(WEST.toVector()), Flag.CARDINAL);

            BlockState tmp = state;

            Object northState = tmp.getState(PropertyKey.NORTH);
            Object eastState = tmp.getState(PropertyKey.EAST);
            Object southState = tmp.getState(PropertyKey.SOUTH);
            Object westState = tmp.getState(PropertyKey.WEST);

            tmp = tmp.with(PropertyKey.valueOf(newNorth.name().toUpperCase()), northState);
            tmp = tmp.with(PropertyKey.valueOf(newEast.name().toUpperCase()), eastState);
            tmp = tmp.with(PropertyKey.valueOf(newSouth.name().toUpperCase()), southState);
            tmp = tmp.with(PropertyKey.valueOf(newWest.name().toUpperCase()), westState);

            newMaskedId = tmp.getInternalId();
        }

        for (AbstractProperty property : (Collection<AbstractProperty>) (Collection) type.getProperties()) {
            if (isDirectional(property)) {
                long[] directions = getDirections(property);
                if (directions != null) {
                    Integer newIndex = getNewStateIndex(transform, directions, property.getIndex(state.getInternalId()));
                    if (newIndex != null) {
                        newMaskedId = property.modifyIndex(newMaskedId, newIndex);
                    }
                }
            }
        }
        return newMaskedId;
    }

    /**
     * @deprecated Slow - does not cache results
     * @param block
     * @param transform
     * @param <B>
     * @return
     */
    @Deprecated
    public static <B extends BlockStateHolder<B>> B transform(B block, Transform transform) {
        BlockState state = block.toImmutableState();

        int transformedId = transformState(state, transform);
        BlockState transformed = BlockState.getFromInternalId(transformedId);
        if (block.hasNbtData()) {
            return (B) transformBaseBlockNBT(transformed, block.getNbtData(), transform);
        }
        return (B) (block instanceof BaseBlock ? transformed.toBaseBlock() : transformed);
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
        if (arr == ALL) {
            return state;
        }
        if (arr == null) {
            arr = transformArray[typeId] = new int[state.getBlockType().getMaxStateId() + 1];
            Arrays.fill(arr, -1);
        }
        int mask = BLOCK_ROTATION_BITMASK[typeId];
        int internalId = state.getInternalId();

        int maskedId = internalId & mask;
        int newMaskedId = arr[maskedId >> BlockTypes.BIT_OFFSET];
        if (newMaskedId != -1) {
            return BlockState.getFromInternalId(newMaskedId | (internalId & (~mask)));
        }

        int newMaskId = transformState(state, transform);
        arr[maskedId >> BlockTypes.BIT_OFFSET] = newMaskedId & mask;
        return BlockState.getFromInternalId(newMaskedId);
    }

    public final BaseBlock transform(BlockStateHolder block) {
        BlockState transformed = transform(block.toImmutableState());
        if (block.hasNbtData()) {
            return transformBaseBlockNBT(transformed, block.getNbtData(), transform);
        }
        return transformed.toBaseBlock();
    }

    public final BlockStateHolder transformInverse(BlockStateHolder block) {
        BlockState transformed = transformInverse(block.toImmutableState());
        if (block.hasNbtData()) {
            return transformBaseBlockNBT(transformed, block.getNbtData(), transformInverse);
        }
        return transformed;
    }

    public final BlockState transform(BlockState block) {
        return transform(block, BLOCK_TRANSFORM, transform);
    }

    public final BlockState transformInverse(BlockState block) {
        return transform(block, BLOCK_TRANSFORM_INVERSE, transformInverse);
    }

    @Override
    public BlockState getLazyBlock(int x, int y, int z) {
        return transform(super.getLazyBlock(x, y, z));
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        return transform(super.getFullBlock(position));
    }

    @Override
    public BlockState getLazyBlock(BlockVector3 position) {
        return transform(super.getLazyBlock(position));
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        return transform(super.getBlock(position));
    }

    @Override
    public BiomeType getBiome(BlockVector2 position) {
        return super.getBiome(position);
    }

    @Override
    public boolean setBlock(int x, int y, int z, BlockStateHolder block) throws WorldEditException {
        return super.setBlock(x, y, z, transformInverse(block));
    }


    @Override
    public boolean setBlock(BlockVector3 location, BlockStateHolder block) throws WorldEditException {
        return super.setBlock(location, transformInverse(block));
    }


}