package com.sk89q.worldedit.extent.transform;

import com.boydti.fawe.object.extent.ResettableExtent;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.ByteTag;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.internal.helper.MCDirections;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.registry.state.AbstractProperty;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Sets;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.registry.state.BooleanProperty;
import com.sk89q.worldedit.registry.state.DirectionalProperty;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
//    @Override
//    public BlockState getBlock(BlockVector3 position) {
//        return transformBlock(super.getBlock(position), false);
//    }
//
//    @Override
//    public BaseBlock getFullBlock(BlockVector3 position) {
//        return transformBlock(super.getFullBlock(position), false);
//    }

//    @Override
//    public boolean setBlock(BlockVector3 location, BlockStateHolder block) throws WorldEditException {
//        return super.setBlock(location, transformBlock(block, true));
//    }


//    /**
//     * Transform the given block using the given transform.
//     *
//     * <p>The provided block is modified.</p>
//     *
//     * @param block the block
//     * @param transform the transform
//     * @return the same block
//     */
//    public static <T extends BlockStateHolder> T transform(T block, Transform transform) {
//        return transform(block, transform, block);
//    }

    private static final Set<String> directionNames = Sets.newHashSet("north", "south", "east", "west");

//    /**
//     * Transform the given block using the given transform.
//     *
//     * @param block the block
//     * @param transform the transform
//     * @param changedBlock the block to change
//     * @return the changed block
//     */
//    private static <T extends BlockStateHolder> T transform(T block, Transform transform, T changedBlock) {
//        checkNotNull(block);
//        checkNotNull(transform);
//
//        List<? extends Property> properties = block.getBlockType().getProperties();
//
//        for (Property property : properties) {
//            if (property instanceof DirectionalProperty) {
//                Direction value = (Direction) block.getState(property);
//                if (value != null) {
//                    Vector3 newValue = getNewStateValue((DirectionalProperty) property, transform, value.toVector());
//                    if (newValue != null) {
//                        changedBlock = (T) changedBlock.with(property, Direction.findClosest(newValue, Direction.Flag.ALL));
//                    }
//                }
//            }
//        }



    @Nullable
//<<<<<<< HEAD
    private static Integer getNewStateIndex(Transform transform, List<Direction> directions, int oldIndex) {
        Direction oldDirection = directions.get(oldIndex);
        Vector3 oldVector = oldDirection.toVector();
        Vector3 newVector = transform.apply(oldVector).subtract(transform.apply(Vector3.ZERO)).normalize();
        int newIndex = oldIndex;
        double closest = oldVector.normalize().dot(newVector);
//=======
//    private static Vector3 getNewStateValue(DirectionalProperty state, Transform transform, Vector3 oldDirection) {
//        Vector3 newDirection = transform.apply(oldDirection).subtract(transform.apply(Vector3.ZERO)).normalize();
//        Vector3 newValue = null;
//        double closest = -2;
//>>>>>>> 399e0ad5... Refactor vector system to be cleaner
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
            return BlockState.getFromInternalId(newMaskedId | (internalId & (~mask)));
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
        return BlockState.getFromInternalId(newMaskedId);
    }

    public final BlockState transformFast(BlockState block) {
        BlockState transformed = transform(block, BLOCK_TRANSFORM, transform);
        if (block.hasNbtData()) {
            CompoundTag tag = block.getNbtData();
            if (tag.containsKey("Rot")) {
                int rot = tag.asInt("Rot");

                Direction direction = MCDirections.fromRotation(rot);

                if (direction != null) {
                    Vector3 applyAbsolute = transform.apply(direction.toVector());
                    Vector3 applyOrigin = transform.apply(Vector3.ZERO);

                    Direction newDirection = Direction.findClosest(applyAbsolute.subtract(applyOrigin), Direction.Flag.CARDINAL | Direction.Flag.ORDINAL | Direction.Flag.SECONDARY_ORDINAL);

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
                    Vector3 applyAbsolute = transformInverse.apply(direction.toVector());
                    Vector3 applyOrigin = transformInverse.apply(Vector3.ZERO);

                    Direction newDirection = Direction.findClosest(applyAbsolute.subtract(applyOrigin), Direction.Flag.CARDINAL | Direction.Flag.ORDINAL | Direction.Flag.SECONDARY_ORDINAL);

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
    public BlockState getLazyBlock(BlockVector3 position) {
        return transformFast(super.getLazyBlock(position));
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        return transformFast(super.getBlock(position));
    }

    @Override
    public BaseBiome getBiome(BlockVector2 position) {
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