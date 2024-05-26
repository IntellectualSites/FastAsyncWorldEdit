package com.fastasyncworldedit.core.extent.processor;

import com.fastasyncworldedit.core.extent.filter.block.FilterBlock;
import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.fastasyncworldedit.core.math.MutableVector3;
import com.fastasyncworldedit.core.queue.Filter;
import com.fastasyncworldedit.core.queue.IBatchProcessor;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.NBTUtils;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.NullExtent;
import com.sk89q.worldedit.function.mask.BlockTypeMask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockCategories;
import com.sk89q.worldedit.world.block.BlockCategory;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import org.enginehub.linbus.tree.LinCompoundTag;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class PlacementStateProcessor extends AbstractDelegateExtent implements IBatchProcessor, Filter, Pattern {

    private static final Direction[] NESW = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private static final boolean SETUP = false;
    private static BlockTypeMask DEFAULT_MASK = null;

    protected final Extent extent;
    protected final BlockTypeMask mask;
    protected final boolean includeUnedited;
    private final MutableVector3 clickPos = new MutableVector3();
    private final MutableBlockVector3 clickedBlock = new MutableBlockVector3();

    public PlacementStateProcessor(Extent extent, BlockTypeMask mask, boolean includeUnedited) {
        super(extent);
        // Required here as child classes are located within adapters and will therefore be statically accessed on startup,
        // meaning we attempt to access BlockTypes class before it is correctly initialised.
        if (!SETUP) {
            synchronized (PlacementStateProcessor.class) {
                if (!SETUP) {
                    setup();
                }
            }
        }
        this.extent = extent;
        this.mask = mask == null ? DEFAULT_MASK : mask;
        this.includeUnedited = includeUnedited;
    }

    private static void setup() {
        DEFAULT_MASK = new BlockTypeMask(new NullExtent());
        DEFAULT_MASK.add(
                BlockTypes.IRON_BARS,
                BlockTypes.GLASS_PANE,
                BlockTypes.BLACK_STAINED_GLASS_PANE,
                BlockTypes.BLUE_STAINED_GLASS_PANE,
                BlockTypes.BROWN_STAINED_GLASS_PANE,
                BlockTypes.LIGHT_BLUE_STAINED_GLASS_PANE,
                BlockTypes.PINK_STAINED_GLASS_PANE,
                BlockTypes.LIGHT_GRAY_STAINED_GLASS_PANE,
                BlockTypes.GRAY_STAINED_GLASS_PANE,
                BlockTypes.CYAN_STAINED_GLASS_PANE,
                BlockTypes.PURPLE_STAINED_GLASS_PANE,
                BlockTypes.GREEN_STAINED_GLASS_PANE,
                BlockTypes.LIME_STAINED_GLASS_PANE,
                BlockTypes.MAGENTA_STAINED_GLASS_PANE,
                BlockTypes.YELLOW_STAINED_GLASS_PANE,
                BlockTypes.ORANGE_STAINED_GLASS_PANE,
                BlockTypes.RED_STAINED_GLASS_PANE,
                BlockTypes.WHITE_STAINED_GLASS_PANE,
                BlockTypes.CHORUS_PLANT,
                BlockTypes.DRIPSTONE_BLOCK,
                BlockTypes.POINTED_DRIPSTONE,
                BlockTypes.BIG_DRIPLEAF,
                BlockTypes.BIG_DRIPLEAF_STEM,
                BlockTypes.CAMPFIRE,
                BlockTypes.CHEST,
                BlockTypes.TRAPPED_CHEST,
                BlockTypes.CRAFTER,
                BlockTypes.MUSHROOM_STEM,
                BlockTypes.BROWN_MUSHROOM_BLOCK,
                BlockTypes.RED_MUSHROOM_BLOCK,
                BlockTypes.TRIPWIRE,
                BlockTypes.TWISTING_VINES_PLANT,
                BlockTypes.CAVE_VINES_PLANT,
                BlockTypes.WEEPING_VINES_PLANT,
                BlockTypes.VINE,
                BlockTypes.REDSTONE_WIRE
        );
        BlockCategory[] categories = new BlockCategory[]{BlockCategories.FENCES, BlockCategories.FENCE_GATES, BlockCategories.STAIRS, BlockCategories.WALLS, BlockCategories.BAMBOO_BLOCKS, BlockCategories.CAVE_VINES, BlockCategories.TALL_FLOWERS};
        for (BlockCategory category : categories) {
            if (category != null) {
                DEFAULT_MASK.add(category.getAll());
            }
        }
    }

    @Override
    public IChunkSet processSet(IChunk iChunk, IChunkGet iChunkGet, IChunkSet iChunkSet) {
        int chunkX = iChunk.getX() << 4;
        int chunkZ = iChunk.getZ() << 4;
        for (int layer = iChunkGet.getMinSectionPosition(); layer <= iChunkGet.getMaxSectionPosition(); layer++) {
            int layerY = layer << 4;
            char[] set = iChunkSet.loadIfPresent(layer);
            char[] get = null;
            if (set == null) {
                if (!includeUnedited) {
                    continue;
                }
            }
            for (int y = 0, i = 0; y < 16; y++) {
                int blockY = layerY + y;
                for (int z = 0; z < 16; z++) {
                    int blockZ = chunkZ + z;
                    for (int x = 0; x < 16; x++, i++) {
                        int blockX = chunkX + x;
                        char ordinal = set == null ? BlockTypesCache.ReservedIDs.__RESERVED__ : set[i];
                        if (ordinal == BlockTypesCache.ReservedIDs.__RESERVED__) {
                            if (!includeUnedited) {
                                continue;
                            }
                            if (get == null) {
                                get = iChunkGet.load(layer);
                            }
                            ordinal = get[i];
                        }
                        BlockState state = BlockTypesCache.states[ordinal];
                        if (!mask.test(state.getBlockType())) {
                            continue;
                        }
                        char newOrdinal = getBlockOrdinal(blockX, blockY, blockZ, state);
                        if (set == null) {
                            set = iChunkSet.load(layer);
                        }
                        set[i] = newOrdinal;
                    }
                }
            }
        }
        return iChunkSet;
    }

    @Override
    public ProcessorScope getScope() {
        return ProcessorScope.CHANGING_BLOCKS;
    }

    @Override
    public abstract PlacementStateProcessor fork();

    // Require block type to avoid duplicate lookup
    protected abstract char getStateAtFor(
            int x, int y, int z, BlockState state, Vector3 clickPos, Direction clickedFaceDirection, BlockVector3 clickedBlock
    );

    private char getBlockOrdinal(
            final int blockX,
            final int blockY,
            final int blockZ,
            final BlockState state
    ) {
        EnumSet<Direction> dirs = Direction.getDirections(state);
        Direction clickedFaceDirection = null; // This should be always be set by the below.
        Set<String> states = state.getStates().keySet().stream().map(Property::getName).collect(Collectors.toSet());
        if (dirs.isEmpty() || states.contains("NORTH") && states.contains("EAST")) {
            clickPos.setComponents(blockX + 0.5d, blockY, blockZ + 0.5d);
            clickedFaceDirection = Direction.UP;
            clickedBlock.setComponents(blockX, blockY - 1, blockZ);
        } else {
            boolean hadNesw = false;
            for (Direction dir : NESW) {
                if (dirs.contains(dir)) {
                    clickedFaceDirection = dir.getLeft().getLeft(); // opposite
                    clickPos.setComponents(
                            (double) blockX + 0.5 * (1 + dir.getBlockX()),
                            (double) blockY + 0.2,
                            (double) blockZ + 0.5 * (1 + dir.getBlockZ())
                    );
                    clickedBlock.setComponents(blockX, blockY, blockZ).add(dir.toBlockVector());
                    hadNesw = true;
                    break;
                }
            }
            if (hadNesw) {
                if (dirs.contains(Direction.UP)) {
                    clickPos.mutY(blockY + 0.5);
                }
            } else if (dirs.contains(Direction.UP)) {
                clickedFaceDirection = Direction.DOWN;
                clickPos.setComponents(blockX + 0.5d, blockY + 1d, blockZ + 0.5d);
                clickedBlock.setComponents(blockX, blockY + 1, blockZ);
            } else if (dirs.contains(Direction.DOWN)) {
                clickedFaceDirection = Direction.UP;
                clickPos.setComponents(blockX + 0.5d, blockY - 1d, blockZ + 0.5d);
                clickedBlock.setComponents(blockX, blockY - 1, blockZ);
            }
        }
        return getStateAtFor(blockX, blockY, blockZ, state, clickPos, clickedFaceDirection, clickedBlock);
    }

    @Override
    public void applyBlock(FilterBlock block) {
        BlockState state = BlockTypesCache.states[block.getOrdinal()];
        if (!mask.test(state.getBlockType())) {
            return;
        }
        char ordinal = (char) block.getOrdinal();
        char newOrdinal = getBlockOrdinal(block.x(), block.y(), block.z(), block.getBlock());
        if (ordinal != newOrdinal) {
            block.setBlock(BlockTypesCache.states[newOrdinal]);
        }
    }

    @Override
    public boolean apply(Extent orDefault, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        if (orDefault == null) {
            orDefault = extent;
        }
        BaseBlock block = orDefault.getFullBlock(get);
        if (!mask.test(block.getBlockType())) {
            return false;
        }
        char newOrdinal = getBlockOrdinal(set.x(), set.y(), set.z(), block.toBlockState());
        if (block.getOrdinalChar() != newOrdinal) {
            BlockState newState = BlockTypesCache.states[newOrdinal];
            orDefault.setBlock(set, newState);
            LinCompoundTag nbt = block.getNbt();
            if (nbt != null && newState.getBlockType() == block.getBlockType()) {
                orDefault.setTile(set.x(), set.y(), set.z(), new CompoundTag(nbt));
            }
            return true;
        }
        return false;
    }

    @Override
    public BaseBlock applyBlock(final BlockVector3 position) {
        BaseBlock block = extent.getFullBlock(position);
        if (!mask.test(block.getBlockType())) {
            return null;
        }
        char newOrdinal = getBlockOrdinal(position.x(), position.y(), position.z(), block.toBlockState());
        if (block.getOrdinalChar() != newOrdinal) {
            BlockState state = BlockTypesCache.states[newOrdinal];
            LinCompoundTag nbt = block.getNbt();
            if (nbt != null && state.getBlockType() == block.getBlockType()) {
                return state.toBaseBlock(nbt);
            }
            return state.toBaseBlock();
        }
        return null;
    }

}
