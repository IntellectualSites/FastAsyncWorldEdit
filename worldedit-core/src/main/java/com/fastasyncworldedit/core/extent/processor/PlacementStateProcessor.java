package com.fastasyncworldedit.core.extent.processor;

import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.extent.NullExtent;
import com.fastasyncworldedit.core.extent.filter.block.FilterBlock;
import com.fastasyncworldedit.core.function.mask.AdjacentAny2DMask;
import com.fastasyncworldedit.core.math.BlockVector3ChunkMap;
import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.fastasyncworldedit.core.math.MutableVector3;
import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.fastasyncworldedit.core.queue.IBatchProcessor;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.registry.state.PropertyKey;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.BlockCategoryMask;
import com.sk89q.worldedit.function.mask.BlockTypeMask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.Region;
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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Processor/pattern that uses Minecraft internal methods to determine the shape of blocks, e.g. stairs and fences
 *
 * @since 2.12.3
 */
public abstract class PlacementStateProcessor extends AbstractDelegateExtent implements IBatchProcessor, Pattern {

    private static final Direction[] NESW = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private static final int CHUNK_BLOCK_POS_MASK = -1 << 4;

    private static volatile boolean SETUP = false;
    private static BlockTypeMask DEFAULT_MASK = null;
    private static BlockTypeMask IN_FIRST_PASS = null;
    private static BlockTypeMask REQUIRES_SECOND_PASS = null;
    private static BlockTypeMask IN_FIRST_PASS_WITHOUT_SECOND = null;
    private static AdjacentAny2DMask ADJACENT_STAIR_MASK = null;

    protected final Extent extent;
    protected final BlockTypeMask mask;
    protected final Region region;
    protected final Map<SecondPass, Character> postCompleteSecondPasses;
    protected final ThreadLocal<PlacementStateProcessor> threadProcessors;
    protected final AtomicBoolean finished;
    private final MutableVector3 clickPos = new MutableVector3();
    private final MutableBlockVector3 clickedBlock = new MutableBlockVector3();
    private final MutableBlockVector3 placedBlock = new MutableBlockVector3(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

    private IChunkGet processChunkGet = null;
    private IChunkSet processChunkSet = null;
    private int processChunkX;
    private int processChunkZ;

    /**
     * Processor/pattern for performing block updates, e.g. stair shape and glass pane connections
     *
     * @param extent Extent to use
     * @param mask   Mask of blocks to perform updates on
     * @since 2.12.3
     */
    public PlacementStateProcessor(Extent extent, BlockTypeMask mask, Region region) {
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
        this.region = region;
        this.postCompleteSecondPasses = new ConcurrentHashMap<>();
        this.threadProcessors = ThreadLocal.withInitial(this::fork);
        this.finished = new AtomicBoolean();
    }

    protected PlacementStateProcessor(
            Extent extent,
            BlockTypeMask mask,
            Map<SecondPass, Character> crossChunkSecondPasses,
            ThreadLocal<PlacementStateProcessor> threadProcessors,
            Region region,
            AtomicBoolean finished
    ) {
        super(extent);
        this.extent = extent;
        this.mask = mask;
        this.region = region;
        this.postCompleteSecondPasses = crossChunkSecondPasses;
        this.threadProcessors = threadProcessors;
        this.finished = finished;
    }

    private static void setup() {
        NullExtent nullExtent = new NullExtent(
                com.sk89q.worldedit.extent.NullExtent.INSTANCE,
                Caption.of("PlacementStateProcessor fell through to null extent")
        );

        IN_FIRST_PASS = new BlockTypeMask(nullExtent);
        IN_FIRST_PASS.add(
                BlockTypes.CHEST,
                BlockTypes.TRAPPED_CHEST
        );
        IN_FIRST_PASS.add(BlockCategories.STAIRS.getAll());

        IN_FIRST_PASS_WITHOUT_SECOND = new BlockTypeMask(nullExtent);
        IN_FIRST_PASS_WITHOUT_SECOND.add(BlockCategories.STAIRS.getAll());

        REQUIRES_SECOND_PASS = new BlockTypeMask(nullExtent);
        REQUIRES_SECOND_PASS.add(
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
                BlockTypes.TRIPWIRE,
                BlockTypes.TWISTING_VINES_PLANT,
                BlockTypes.CAVE_VINES_PLANT,
                BlockTypes.WEEPING_VINES_PLANT,
                BlockTypes.VINE,
                BlockTypes.REDSTONE_WIRE
        );
        BlockCategory[] categories = new BlockCategory[]{BlockCategories.FENCES, BlockCategories.FENCE_GATES, BlockCategories.WALLS, BlockCategories.CAVE_VINES};
        for (BlockCategory category : categories) {
            if (category != null) {
                REQUIRES_SECOND_PASS.add(category.getAll());
            }
        }

        DEFAULT_MASK = REQUIRES_SECOND_PASS.copy();
        DEFAULT_MASK.add(
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
                BlockTypes.RED_MUSHROOM_BLOCK
        );
        categories = new BlockCategory[]{BlockCategories.STAIRS, BlockCategories.BAMBOO_BLOCKS, BlockCategories.TALL_FLOWERS};
        for (BlockCategory category : categories) {
            if (category != null) {
                DEFAULT_MASK.add(category.getAll());
            }
        }

        ADJACENT_STAIR_MASK = new AdjacentAny2DMask(new BlockCategoryMask(nullExtent, BlockCategories.STAIRS), false);
        SETUP = true;
    }

    @Override
    public IChunkSet processSet(IChunk iChunk, IChunkGet chunkGet, IChunkSet chunkSet) {
        if (finished.get()) {
            return chunkSet;
        }
        PlacementStateProcessor threadProcessor = threadProcessors.get();
        try {
            threadProcessor.initProcess(iChunk, chunkGet, chunkSet);
            return threadProcessor.process();
        } finally {
            threadProcessor.uninit();
        }
    }

    private void initProcess(IChunk iChunk, IChunkGet chunkGet, IChunkSet chunkSet) {
        this.processChunkX = iChunk.getX() << 4;
        this.processChunkZ = iChunk.getZ() << 4;
        this.processChunkGet = chunkGet;
        this.processChunkSet = chunkSet;
    }

    private void uninit() {
        this.processChunkGet = null;
        this.processChunkSet = null;
    }

    private IChunkSet process() {
        Map<BlockVector3, FaweCompoundTag> setTiles = processChunkSet.tiles();
        for (int layer = processChunkGet.getMinSectionPosition(); layer <= processChunkGet.getMaxSectionPosition(); layer++) {
            int layerY = layer << 4;
            char[] set = processChunkSet.loadIfPresent(layer);
            if (set == null) {
                continue;
            }
            for (int y = 0, i = 0; y < 16; y++, i += 256) {
                int blockY = layerY + y;
                checkAndPerformUpdate(setTiles, set, i, blockY, true);
                checkAndPerformUpdate(setTiles, set, i, blockY, false);
            }
        }
        return processChunkSet;
    }

    private void checkAndPerformUpdate(
            Map<BlockVector3, FaweCompoundTag> setTiles,
            char[] set,
            int index,
            int blockY,
            boolean firstPass
    ) {
        for (int z = 0; z < 16; z++) {
            int blockZ = processChunkZ + z;
            for (int x = 0; x < 16; x++, index++) {
                int blockX = processChunkX + x;
                char ordinal = set[index];
                BlockState state = BlockTypesCache.states[ordinal];
                if (firstPass) {
                    if (!IN_FIRST_PASS.test(state)) {
                        continue;
                    }
                } else if (IN_FIRST_PASS_WITHOUT_SECOND.test(state)) {
                    continue;
                }
                if (!mask.test(state)) {
                    continue;
                }
                boolean atEdge = x == 0 || x == 15 || z == 0 || z == 15;
                if (!firstPass && atEdge && REQUIRES_SECOND_PASS.test(state)) {
                    postCompleteSecondPasses.put(new SecondPass(
                            blockX,
                            blockY,
                            blockZ,
                            setTiles.isEmpty() ? null : ((BlockVector3ChunkMap<FaweCompoundTag>) setTiles).remove(x, blockY, z)
                    ), ordinal);
                    set[index] = BlockTypesCache.ReservedIDs.__RESERVED__;
                    continue;
                }
                if (state.getBlockType().equals(BlockTypes.CHEST) || state.getBlockType().equals(BlockTypes.TRAPPED_CHEST)) {
                    placedBlock.setComponents(blockX, blockY, blockZ);
                } else {
                    placedBlock.setComponents(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
                }
                char newOrdinal = getBlockOrdinal(blockX, blockY, blockZ, state);
                if (newOrdinal == ordinal) {
                    continue;
                }
                set[index] = newOrdinal;
            }
        }
    }

    @Override
    public ProcessorScope getScope() {
        return ProcessorScope.CHANGING_BLOCKS;
    }

    @Override
    public void finish() {
        flush();
    }

    @Override
    public void flush() {
        finished.set(true);
        for (Map.Entry<SecondPass, Character> entry : postCompleteSecondPasses.entrySet()) {
            BlockState state;
            char ordinal = entry.getValue();
            SecondPass secondPass = entry.getKey();
            if (ordinal != 0) {
                state = BlockTypesCache.states[ordinal];
            } else {
                state = extent.getBlock(secondPass.x, secondPass.y, secondPass.z);
            }
            char newOrdinal = getBlockOrdinal(secondPass.x, secondPass.y, secondPass.z, state);
            if (newOrdinal == state.getOrdinalChar() && ordinal == 0) {
                continue;
            }
            if (secondPass.tile != null) {
                extent.tile(secondPass.x, secondPass.y, secondPass.z, secondPass.tile);
            }
            extent.setBlock(secondPass.x, secondPass.y, secondPass.z, BlockTypesCache.states[newOrdinal]);
        }
        postCompleteSecondPasses.clear();
    }

    @Override
    public abstract PlacementStateProcessor fork();

    protected abstract char getStateAtFor(
            int x,
            int y,
            int z,
            BlockState state,
            Vector3 clickPos,
            Direction clickedFaceDirection,
            BlockVector3 clickedBlock
    );

    public BlockState getBlockStateAt(int x, int y, int z) {
        Character ord = postCompleteSecondPasses.get(new SecondPass(x, y, z, null));
        if (ord != null && ord != 0) {
            return BlockTypesCache.states[ord];
        }
        if (processChunkSet == null || (x & CHUNK_BLOCK_POS_MASK) != processChunkX || (z & CHUNK_BLOCK_POS_MASK) != processChunkZ) {
            return extent.getBlock(x, y, z);
        }
        char[] set = processChunkSet.loadIfPresent(y >> 4);
        if (set == null) {
            return processChunkGet.getBlock(x & 15, y, z & 15);
        }
        char ordinal = set[(y & 15) << 8 | (z & 15) << 4 | (x & 15)];
        if (ordinal == BlockTypesCache.ReservedIDs.__RESERVED__) {
            return processChunkGet.getBlock(x & 15, y, z & 15);
        }
        BlockState state = BlockTypesCache.states[ordinal];
        // "Hack" for chests as internal server methods will only accept "single" chests for joining
        if (state.getBlockType().equals(BlockTypes.CHEST) || state.getBlockType().equals(BlockTypes.TRAPPED_CHEST)) {
            String shape =  state.getState(PropertyKey.TYPE).toString();
            if (shape.equals("right")) {
                Direction facing = state.getState(PropertyKey.FACING);
                Direction left = facing.getLeft();
                int testX = x + left.getBlockX();
                int testZ = z + left.getBlockZ();
                if (placedBlock.isAt(testX, y, testZ)) {
                    return state.with(PropertyKey.TYPE, "single");
                }
            } else if(shape.equals("left")) {
                Direction facing = state.getState(PropertyKey.FACING);
                Direction right = facing.getRight();
                int testX = x + right.getBlockX();
                int testZ = z + right.getBlockZ();
                if (placedBlock.isAt(testX, y, testZ)) {
                    return state.with(PropertyKey.TYPE, "single");
                }
            }
        }
        return state;
    }

    public LinCompoundTag getTileAt(int x, int y, int z) {
        SecondPass secondPass = new SecondPass(x, y, z, null);
        Character ord = postCompleteSecondPasses.get(secondPass);
        if (ord != null && ord != 0) {
            // This should be rare enough...
            for (SecondPass pass : postCompleteSecondPasses.keySet()) {
                if (pass.hashCode() == secondPass.hashCode()) {
                    return pass.tile != null ? pass.tile.linTag() : BlockTypesCache.states[ord].getNbt();
                }
            }
            return BlockTypesCache.states[ord].getNbt();
        }
        if (processChunkSet == null || (x & CHUNK_BLOCK_POS_MASK) != processChunkX || (z & CHUNK_BLOCK_POS_MASK) != processChunkZ) {
            return extent.getFullBlock(x, y, z).getNbt();
        }
        FaweCompoundTag tile = processChunkSet.tile(x & 15, y, z & 15);
        if (tile != null) {
            return tile.linTag();
        }
        char[] set = processChunkSet.loadIfPresent(y >> 4);
        if (set == null) {
            return processChunkGet.getFullBlock(x & 15, y, z & 15).getNbt();
        }
        char ordinal = set[(y & 15) << 8 | (z & 15) << 4 | (x & 15)];
        if (ordinal == BlockTypesCache.ReservedIDs.__RESERVED__) {
            return processChunkGet.getFullBlock(x & 15, y, z & 15).getNbt();
        }
        return BlockTypesCache.states[ordinal].getNbt();
    }

    private char getBlockOrdinal(int blockX, int blockY, int blockZ, BlockState state) {
        char override = getOverrideBlockOrdinal(blockX, blockY, blockZ, state);
        if (override != BlockTypesCache.ReservedIDs.__RESERVED__) {
            return override;
        }
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
                    clickedFaceDirection = dir.getLeft().getLeft();
                    if (state.getBlockType() == BlockTypes.CHEST || state.getBlockType() == BlockTypes.TRAPPED_CHEST) {
                        Direction tmp = clickedFaceDirection;
                        clickedFaceDirection = dir;
                        dir = tmp;
                    }
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
                    clickPos.mutY(blockY + 0.6);
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

    protected char getOverrideBlockOrdinal(int blockX, int blockY, int blockZ, BlockState state) {
        if (BlockCategories.TALL_FLOWERS.contains(state)) {
            PropertyKey propertyKey = PropertyKey.HALF;
            BlockState plantState = extent.getBlock(blockX, blockY - 1, blockZ).getBlockType().equals(state.getBlockType())
                    ? state.with(propertyKey, "upper")
                    : state.with(propertyKey, "lower");
            return plantState.getOrdinalChar();
        }
        return BlockTypesCache.ReservedIDs.__RESERVED__;
    }

    @Override
    public void applyBlock(FilterBlock block) {
        if (finished.get()) {
            return;
        }
        BlockState state = BlockTypesCache.states[block.getOrdinal()];
        if (!mask.test(state)) {
            return;
        }
        if (REQUIRES_SECOND_PASS.test(block.getBlock()) && ADJACENT_STAIR_MASK.test(extent, block)) {
            postCompleteSecondPasses.put(new SecondPass(block), (char) 0);
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
        if (!mask.test(block)) {
            return false;
        }
        if (REQUIRES_SECOND_PASS.test(block) && ADJACENT_STAIR_MASK.test(extent, set)) {
            postCompleteSecondPasses.put(new SecondPass(set), (char) 0);
            return false;
        }
        char newOrdinal = getBlockOrdinal(set.x(), set.y(), set.z(), block.toBlockState());
        if (block.getOrdinalChar() != newOrdinal) {
            BlockState newState = BlockTypesCache.states[newOrdinal];
            orDefault.setBlock(set.x(), set.y(), set.z(), newState);
            LinCompoundTag nbt = block.getNbt();
            if (nbt != null && newState.getBlockType() == block.getBlockType()) {
                orDefault.tile(set.x(), set.y(), set.z(), FaweCompoundTag.of(nbt));
            }
            return true;
        }
        return false;
    }

    @Override
    public BaseBlock applyBlock(BlockVector3 position) {
        if (finished.get()) {
            return null;
        }
        BaseBlock block = extent.getFullBlock(position);
        if (!mask.test(block)) {
            return null;
        }
        if (REQUIRES_SECOND_PASS.test(block) && ADJACENT_STAIR_MASK.test(extent, position)) {
            postCompleteSecondPasses.put(new SecondPass(position), (char) 0);
            return null;
        }
        char newOrdinal = getBlockOrdinal(position.x(), position.y(), position.z(), block.toBlockState());
        if (block.getOrdinalChar() != newOrdinal) {
            BlockState state = BlockTypesCache.states[newOrdinal];
            LinCompoundTag nbt = block.getNbt();
            if (nbt != null && state.getBlockType() == block.getBlockType()) {
                state.toBaseBlock(nbt);
            }
            return state.toBaseBlock();
        }
        return null;
    }

    protected record SecondPass(int x, int y, int z, FaweCompoundTag tile) {

        private SecondPass(BlockVector3 pos) {
            this(pos.x(), pos.y(), pos.z(), null);
        }

        @Override
        public int hashCode() {
            return (x ^ (z << 12)) ^ (y << 24);
        }

    }

}
