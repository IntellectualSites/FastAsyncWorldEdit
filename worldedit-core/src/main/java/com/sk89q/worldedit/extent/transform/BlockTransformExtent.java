/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.extent.transform;

import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.extent.ResettableExtent;
import com.fastasyncworldedit.core.registry.state.PropertyKey;
import com.fastasyncworldedit.core.registry.state.PropertyKeySet;
import com.google.common.collect.ImmutableMap;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.internal.helper.MCDirections;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.registry.state.AbstractProperty;
import com.sk89q.worldedit.registry.state.DirectionalProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import org.apache.logging.log4j.Logger;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinNumberTag;
import org.enginehub.linbus.tree.LinTag;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.sk89q.worldedit.util.Direction.ASCENDING_EAST;
import static com.sk89q.worldedit.util.Direction.ASCENDING_NORTH;
import static com.sk89q.worldedit.util.Direction.ASCENDING_SOUTH;
import static com.sk89q.worldedit.util.Direction.ASCENDING_WEST;
import static com.sk89q.worldedit.util.Direction.DESCENDING_EAST;
import static com.sk89q.worldedit.util.Direction.DESCENDING_NORTH;
import static com.sk89q.worldedit.util.Direction.DESCENDING_SOUTH;
import static com.sk89q.worldedit.util.Direction.DESCENDING_WEST;
import static com.sk89q.worldedit.util.Direction.DOWN;
import static com.sk89q.worldedit.util.Direction.EAST;
import static com.sk89q.worldedit.util.Direction.Flag;
import static com.sk89q.worldedit.util.Direction.NORTH;
import static com.sk89q.worldedit.util.Direction.NORTHEAST;
import static com.sk89q.worldedit.util.Direction.NORTHWEST;
import static com.sk89q.worldedit.util.Direction.SOUTH;
import static com.sk89q.worldedit.util.Direction.SOUTHEAST;
import static com.sk89q.worldedit.util.Direction.SOUTHWEST;
import static com.sk89q.worldedit.util.Direction.UP;
import static com.sk89q.worldedit.util.Direction.WEST;
import static com.sk89q.worldedit.util.Direction.findClosest;

/**
 * Transforms blocks themselves (but not their position) according to a
 * given transform.
 */
public class BlockTransformExtent extends ResettableExtent {

    //FAWE start
    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private static final Set<PropertyKey> directional = PropertyKeySet.of(
            PropertyKey.HALF,
            PropertyKey.ROTATION,
            PropertyKey.AXIS,
            PropertyKey.FACING,
            PropertyKey.FACE,
            PropertyKey.SHAPE,
            PropertyKey.NORTH,
            PropertyKey.EAST,
            PropertyKey.SOUTH,
            PropertyKey.WEST,
            PropertyKey.ORIENTATION
    );

    private static final Map<Direction, PropertyKey> directionMap = ImmutableMap.of(
            NORTH, PropertyKey.NORTH,
            EAST, PropertyKey.EAST,
            SOUTH, PropertyKey.SOUTH,
            WEST, PropertyKey.WEST
    );

    private final int[] ALL = new int[0];
    private Transform transform;
    private Transform transformInverse;
    private int[] BLOCK_ROTATION_BITMASK;
    private int[][] BLOCK_TRANSFORM;
    private int[][] BLOCK_TRANSFORM_INVERSE;

    public BlockTransformExtent(Extent parent) {
        this(parent, new AffineTransform());
    }
    //FAWE end

    /**
     * Create a new instance.
     *
     * @param extent the extent
     */
    public BlockTransformExtent(Extent extent, Transform transform) {
        super(extent);
        checkNotNull(transform);
        this.transform = transform;
        //FAWE start - cache this
        this.transformInverse = this.transform.inverse();
        cache();
        //FAWE end
    }


    //FAWE start
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

    private static long[] getDirections(AbstractProperty<?> property) {
        if (property instanceof final DirectionalProperty dir) {
            return adapt(dir.getValues().toArray(new Direction[0]));
        } else {
            List<?> values = property.getValues();
            PropertyKey key = property.getKey();
            switch (key.getName().toLowerCase()) {
                case "half": {
                    return adapt(UP, DOWN);
                }
                case "type": {
                    return adapt(combine(UP), combine(DOWN), 0L);
                }
                case "rotation": {
                    List<Direction> directions = new ArrayList<>();
                    for (Object value : values) {
                        directions.add(Direction.fromRotationIndex((Integer) value).orElseThrow());
                    }
                    return adapt(directions.toArray(new Direction[0]));
                }
                case "axis": {
                    return switch (property.getValues().size()) {
                        case 3 -> adapt(combine(EAST, WEST), combine(UP, DOWN), combine(SOUTH, NORTH));
                        case 2 -> adapt(combine(EAST, WEST), combine(SOUTH, NORTH));
                        default -> {
                            LOGGER.error("Invalid {} {}", property.getName(), property.getValues());
                            yield null;
                        }
                    };
                }
                case "facing": {
                    List<Direction> directions = new ArrayList<>();
                    for (Object value : values) {
                        directions.add(Direction.valueOf(value.toString().toUpperCase(Locale.ROOT)));
                    }
                    return adapt(directions.toArray(new Direction[0]));
                }
                case "face": {
                    if (values.size() == 3) {
                        return adapt(combine(UP), combine(NORTH, EAST, SOUTH, WEST), combine(DOWN));
                    }
                    return null;
                }
                case "hinge": {
                    return adapt(
                            combine(NORTHEAST, NORTHWEST, SOUTHEAST, SOUTHWEST),
                            combine(NORTHEAST, NORTHWEST, SOUTHEAST, SOUTHWEST)
                    );
                }
                case "shape": {
                    if (values.contains("left")) {
                        return adapt(combine(EAST, WEST), combine(NORTH, SOUTH));
                    }
                    if (values.contains("straight")) {
                        ArrayList<Long> result = new ArrayList<>();
                        for (Object value : values) {
                            // [straight, inner_left, inner_right, outer_left, outer_right]
                            switch (value.toString()) {
                                case "straight":
                                    result.add(combine(NORTH, EAST, SOUTH, WEST));
                                    continue;
                                case "inner_left", "inner_right":
                                    result.add(orIndex(
                                            combine(NORTHEAST, NORTHWEST, SOUTHWEST, SOUTHEAST),
                                            property.getIndexFor("outer_right"),
                                            property.getIndexFor("outer_left")
                                    ));
                                    continue;
                                case "outer_left", "outer_right":
                                    result.add(orIndex(
                                            combine(NORTHEAST, NORTHWEST, SOUTHWEST, SOUTHEAST),
                                            property.getIndexFor("inner_left"),
                                            property.getIndexFor("inner_right")
                                    ));
                                    continue;
                                default:
                                    LOGGER.warn("Unknown direction {}", value);
                                    result.add(0L);
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
                                    LOGGER.warn("Unknown direction {}", value);
                                    directions.add(0L);
                            }
                        }
                        return adapt(directions.toArray(new Long[0]));
                    }
                }
                case "orientiation": {
                    List<Long> directions = new ArrayList<>();
                    for (Object value : values) {
                        switch (value.toString()) {
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
                            case "descending_east":
                                directions.add(combine(DESCENDING_EAST));
                                break;
                            case "descending_west":
                                directions.add(combine(DESCENDING_WEST));
                                break;
                            case "descending_north":
                                directions.add(combine(DESCENDING_NORTH));
                                break;
                            case "descending_south":
                                directions.add(combine(DESCENDING_SOUTH));
                                break;
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

    private static long orIndex(long mask, int... indexes) {
        for (int index : indexes) {
            mask = mask | (1L << (index + Direction.values().length));
        }
        return mask;
    }

    private static boolean hasIndex(long mask, int index) {
        return ((mask >> Direction.values().length) & (1L << index)) == 0;
    }

    @Nullable
    private static Integer getNewStateIndex(Transform transform, long[] directions, int oldIndex) {
        long oldDirMask = directions[oldIndex];
        if (oldDirMask == 0) {
            return null;
        }
        Integer newIndex = null;

        for (Direction oldDirection : Direction.values()) {
            if (!hasDirection(oldDirMask, oldDirection)) {
                continue;
            }
            Vector3 oldVector = oldDirection.toVector();
            Vector3 newVector = transform.apply(oldVector).subtract(transform.apply(Vector3.ZERO)).normalize();
            boolean flip = false;

            if (transform instanceof AffineTransform) {
                flip = ((AffineTransform) transform).isScaled(oldVector);
            }

            // If we're flipping, it is possible for the old and new vectors to be equal
            if (!flip && oldVector.equalsFuzzy(newVector)) {
                continue;
            }

            double closest = oldVector.normalize().dot(newVector);
            for (int i = 0; i < directions.length; i++) {
                int j = (oldIndex + i) % directions.length;
                long newDirMask = directions[j];
                // Check if the old mask excludes it
                if (!hasIndex(oldDirMask, j)) {
                    continue;
                }
                for (Direction v : Direction.values()) {
                    // Check if it's one of the current directions
                    if (!hasDirection(newDirMask, v)) {
                        continue;
                    }
                    double dot = v.toVector().normalize().dot(newVector);
                    if (dot > closest || (flip && dot >= closest)) { //
                        closest = dot;
                        newIndex = j;
                    }
                }
            }
            if (newIndex != null) {
                return newIndex;
            }
        }
        return newIndex;
    }

    private static boolean isDirectional(Property<?> property) {
        if (property instanceof DirectionalProperty) {
            return true;
        }
        if (directional.contains(property.getKey())) {
            return true;
        }
        List<?> values = property.getValues();
        return (values.contains("top") || values.contains("left"));
    }

    private static BaseBlock transformBaseBlockNBT(BlockState transformed, @Nonnull LinCompoundTag tag, Transform transform) {
        Map<String, LinTag<?>> value = tag.value();
        if (value.get("Rot") instanceof LinNumberTag<?> rotTag) {
            int rot = rotTag.value().intValue();

            Direction direction = MCDirections.fromRotation(rot);

            if (direction != null) {
                Vector3 applyAbsolute = transform.apply(direction.toVector());
                Vector3 applyOrigin = transform.apply(Vector3.ZERO);
                applyAbsolute.mutX(applyAbsolute.x() - applyOrigin.x());
                applyAbsolute.mutY(applyAbsolute.y() - applyOrigin.y());
                applyAbsolute.mutZ(applyAbsolute.z() - applyOrigin.z());

                Direction newDirection = Direction.findClosest(
                        applyAbsolute,
                        Direction.Flag.CARDINAL | Direction.Flag.ORDINAL | Direction.Flag.SECONDARY_ORDINAL
                );

                if (newDirection != null) {
                    LinCompoundTag.Builder builder = tag.toBuilder();
                    builder.putByte("Rot", (byte) MCDirections.toRotation(newDirection));
                    return transformed.toBaseBlock(builder.build());
                }
            }
        }
        return transformed.toBaseBlock(tag);
    }

    private static int transformState(BlockState state, Transform transform) {
        int newMaskedId = state.getInternalId();

        BlockType type = state.getBlockType();
        // Rotate North, East, South, West
        if (type.hasProperty(PropertyKey.NORTH) && type.hasProperty(PropertyKey.EAST) && type.hasProperty(PropertyKey.SOUTH) && type
                .hasProperty(PropertyKey.WEST)) {

            BlockState tmp = state;
            for (Map.Entry<Direction, PropertyKey> entry : directionMap.entrySet()) {
                Direction newDir = findClosest(transform.apply(entry.getKey().toVector()), Flag.CARDINAL);
                if (newDir != null) {
                    Object dirState = state.getState(entry.getValue());
                    tmp = tmp.with(directionMap.get(newDir), dirState);
                }
            }

            newMaskedId = tmp.getInternalId();
        }
        // True if relying on two different "directions" for the result, e.g. stairs with both facing and shape
        for (AbstractProperty<?> property : (List<AbstractProperty<?>>) type.getProperties()) {
            if (isDirectional(property)) {
                long[] directions = getDirections(property);
                if (directions != null) {
                    int oldIndex = property.getIndex(state.getInternalId());
                    if (oldIndex >= directions.length) {
                        if (Settings.settings().ENABLED_COMPONENTS.DEBUG) {
                            LOGGER.warn(String.format(
                                    "Index outside direction array length found for block:{%s} property:{%s}",
                                    state.getBlockType().id(),
                                    property.getName()
                            ));
                        }
                        continue;
                    }
                    Integer newIndex = getNewStateIndex(transform, directions, oldIndex);
                    if (newIndex != null) {
                        newMaskedId = property.modifyIndex(newMaskedId, newIndex);
                    }
                }
            }
        }
        return newMaskedId;
    }


    private void cache() {
        BLOCK_ROTATION_BITMASK = new int[BlockTypes.size()];
        BLOCK_TRANSFORM = new int[BlockTypes.size()][];
        BLOCK_TRANSFORM_INVERSE = new int[BlockTypes.size()][];
        for (int i = 0; i < BLOCK_TRANSFORM.length; i++) {
            BLOCK_TRANSFORM[i] = ALL;
            BLOCK_TRANSFORM_INVERSE[i] = ALL;
            BlockType type = BlockTypes.get(i);
            int bitMask = 0;
            for (AbstractProperty<?> property : (Collection<AbstractProperty>) (Collection) type.getProperties()) {
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
    //FAWE end

    /**
     * Get the transform.
     *
     * @return the transform
     */
    public Transform getTransform() {
        return transform;
    }

    /**
     * Transform a block without making a copy.
     *
     * @param block   the block
     * @param reverse true to transform in the opposite direction
     * @return the same block
     */
    private <T extends BlockStateHolder<T>> T transformBlock(T block, boolean reverse) {
        return transform(block, reverse ? transform.inverse() : transform);
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        return transformBlock(super.getBlock(position), false).toImmutableState();
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return transformBlock(super.getBlock(x, y, z), false).toImmutableState();
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        return transformBlock(super.getFullBlock(position), false).toBaseBlock();
    }

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        return transformBlock(super.getFullBlock(x, y, z), false).toBaseBlock();
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 location, B block) throws WorldEditException {
        return super.setBlock(location, transformInverse(block));
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T block) throws WorldEditException {
        return super.setBlock(x, y, z, transformInverse(block));
    }

    public void setTransform(Transform affine) {
        this.transform = affine;
        //FAWE start - cache this
        this.transformInverse = this.transform.inverse();
        cache();
        //FAWE end
    }

    /**
     * Transform the given block using the given transform.
     *
     * <p>The provided block is <em>not</em> modified.</p>
     *
     * @param block     the block
     * @param transform the transform
     * @return the same block
     */
    public static <B extends BlockStateHolder<B>> B transform(@Nonnull B block, @Nonnull Transform transform) {
        //FAWE start - use own logic
        // performance critical
        BlockState state = block.toImmutableState();

        int transformedId = transformState(state, transform);
        BlockState transformed = BlockState.getFromInternalId(transformedId);
        boolean baseBlock = block instanceof BaseBlock;
        if (baseBlock && block.getNbt() != null) {
            return (B) transformBaseBlockNBT(transformed, block.getNbt(), transform);
        }
        return (B) (baseBlock? transformed.toBaseBlock() : transformed);
        //FAWE end
    }

    //FAWE start - use own logic
    private BlockState transform(BlockState state, int[][] transformArray, Transform transform) {
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
        int newMaskedId = arr[maskedId >> BlockTypesCache.BIT_OFFSET];
        if (newMaskedId != -1) {
            return BlockState.getFromInternalId(newMaskedId | (internalId & (~mask)));
        }

        newMaskedId = transformState(state, transform);

        arr[maskedId >> BlockTypesCache.BIT_OFFSET] = newMaskedId & mask;
        return BlockState.getFromInternalId(newMaskedId);
    }

    public final BaseBlock transform(BlockStateHolder<BaseBlock> block) {
        BlockState transformed = transform(block.toImmutableState());
        if (block.getNbt() != null) {
            return transformBaseBlockNBT(transformed, block.getNbt(), transform);
        }
        return transformed.toBaseBlock();
    }

    protected final BlockStateHolder transformInverse(BlockStateHolder block) {
        BlockState transformed = transformInverse(block.toImmutableState());
        if (block.getNbt() != null) {
            return transformBaseBlockNBT(transformed, block.getNbt(), transformInverse);
        }
        return transformed;
    }

    public final BlockState transform(BlockState block) {
        return transform(block, BLOCK_TRANSFORM, transform);
    }

    private BlockState transformInverse(BlockState block) {
        return transform(block, BLOCK_TRANSFORM_INVERSE, transformInverse);
    }
    //FAWE end
}
