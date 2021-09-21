package com.fastasyncworldedit.core.extent;

import com.fastasyncworldedit.core.extent.processor.ProcessorScope;
import com.fastasyncworldedit.core.queue.IBatchProcessor;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.util.ExtentTraverser;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static com.sk89q.worldedit.world.block.BlockTypesCache.states;

public class DisallowedBlocksExtent extends AbstractDelegateExtent implements IBatchProcessor {

    private static final BlockState RESERVED = BlockTypes.__RESERVED__.getDefaultState();
    private Set<Property<?>> blockedStates = null;
    private Set<String> blockedBlocks = null;

    /**
     * Create a new instance.
     *
     * @param extent         the extent
     * @param blockedBlocks  block types to disallow
     * @param blockedStates  block states/properties to disallow
     */
    public DisallowedBlocksExtent(Extent extent, Set<String> blockedBlocks, Set<String> blockedStates) {
        super(extent);
        if (blockedBlocks != null && !blockedBlocks.isEmpty()) {
            this.blockedBlocks = blockedBlocks.stream()
                    .map(s -> s.contains(":") ? s.toLowerCase(Locale.ROOT) : ("minecraft:" + s).toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());
        }
        if (blockedStates != null && !blockedStates.isEmpty()) {
            this.blockedStates = blockedStates
                    .stream()
                    .flatMap(s -> BlockTypesCache.getAllProperties().get(s.toLowerCase(Locale.ROOT)).stream())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toUnmodifiableSet());
        }
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 location, B block) throws WorldEditException {
        if (block instanceof BaseBlock || block instanceof BlockState) {
            B newBlock = checkBlock(block);
            if (newBlock.getBlockType() == BlockTypes.__RESERVED__) {
                return false;
            }
            return super.setBlock(location, newBlock);
        }
        return super.setBlock(location, block);
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(int x, int y, int z, B block) throws WorldEditException {
        if (block instanceof BaseBlock || block instanceof BlockState) {
            B newBlock = checkBlock(block);
            if (newBlock.getBlockType() == BlockTypes.__RESERVED__) {
                return false;
            }
            return super.setBlock(x, y, z, newBlock);
        }
        return super.setBlock(x, y, z, block);
    }

    private <B extends BlockStateHolder<B>> B checkBlock(B block) {
        if (blockedBlocks != null) {
            if (blockedBlocks.contains(block.getBlockType().getId())) {
                return (B) (block instanceof BlockState ? RESERVED : RESERVED.toBaseBlock()); // set to reserved/empty
            }
        }
        if (blockedStates == null) {
            return block;
        }
        // ignore blocks in default state.
        if (block.getOrdinalChar() == block.getBlockType().getDefaultState().getOrdinalChar()) {
            return block;
        }
        BlockState def = block.getBlockType().getDefaultState();
        for (Property<?> property : blockedStates) {
            Object value = def.getState(property);
            if (block.getState(property) != value) {
                block = block.with(property.getKey(), (Object) def.getState(property));
            }
        }
        return block;
    }

    @Override
    public IChunkSet processSet(final IChunk chunk, final IChunkGet get, final IChunkSet set) {
        if (blockedStates == null && blockedBlocks == null) { // Shouldn't be possible, but make sure
            return set;
        }
        for (int layer = set.getMinSectionPosition(); layer <= set.getMaxSectionPosition(); layer++) {
            if (!set.hasSection(layer)) {
                continue;
            }
            char[] blocks = Objects.requireNonNull(set.loadIfPresent(layer));
            for (int i = 0; i < blocks.length; i++) {
                char block = blocks[i];
                BlockState state = states[block];
                if (blockedBlocks != null) {
                    if (blockedBlocks.contains(state.getBlockType().getId())) {
                        blocks[i] = 0; // set to reserved/empty
                    }
                }
                if (blockedStates == null) {
                    continue;
                }
                // ignore blocks in default state.
                if (block == state.getBlockType().getDefaultState().getOrdinalChar()) {
                    continue;
                }
                BlockState def = state.getBlockType().getDefaultState();
                for (Property<?> property : blockedStates) {
                    Object value = def.getState(property);
                    if (state.getState(property) != value) {
                        block = state.with(property.getKey(), (Object) def.getState(property)).getOrdinalChar();
                    }
                }
                blocks[i] = block;
            }
        }
        return set;
    }

    @Override
    public Future<IChunkSet> postProcessSet(final IChunk chunk, final IChunkGet get, final IChunkSet set) {
        return CompletableFuture.completedFuture(set);
    }

    @Nullable
    @Override
    public Extent construct(final Extent child) {
        if (getExtent() != child) {
            new ExtentTraverser<Extent>(this).setNext(child);
        }
        return this;
    }

    @Override
    public ProcessorScope getScope() {
        return ProcessorScope.CHANGING_BLOCKS;
    }

}
