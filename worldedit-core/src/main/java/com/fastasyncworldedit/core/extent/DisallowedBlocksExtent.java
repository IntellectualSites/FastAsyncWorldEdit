package com.fastasyncworldedit.core.extent;

import com.fastasyncworldedit.core.extent.processor.ProcessorScope;
import com.fastasyncworldedit.core.limit.PropertyRemap;
import com.fastasyncworldedit.core.queue.IBatchProcessor;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.util.ExtentTraverser;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extension.factory.parser.DefaultBlockParser;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import com.sk89q.worldedit.world.block.FuzzyBlockState;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class DisallowedBlocksExtent extends AbstractDelegateExtent implements IBatchProcessor {

    private static final BlockState RESERVED = BlockTypes.__RESERVED__.getDefaultState();
    private final Set<PropertyRemap<?>> remaps;
    private Set<FuzzyBlockState> blockedStates = null;
    private Set<String> blockedBlocks = null;

    /**
     * Create a new instance.
     *
     * @param extent        the extent
     * @param blockedBlocks block types to disallow
     * @param remaps        property remaps to apply, e.g. waterlogged true -> false
     */
    public DisallowedBlocksExtent(Extent extent, Set<String> blockedBlocks, Set<PropertyRemap<?>> remaps) {
        super(extent);
        this.remaps = remaps;
        if (blockedBlocks != null && !blockedBlocks.isEmpty()) {
            blockedBlocks = blockedBlocks.stream()
                    .map(s -> s.contains(":") ? s.toLowerCase(Locale.ROOT) : ("minecraft:" + s).toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());
            this.blockedBlocks = new HashSet<>();
            for (String block : blockedBlocks) {
                if (block.indexOf('[') == -1 || block.indexOf(']') == -1) {
                    this.blockedBlocks.add(block);
                    continue;
                }
                String[] properties = block.substring(block.indexOf('[') + 1, block.indexOf(']')).split(",");
                if (properties.length == 0) {
                    continue;
                }
                BlockType type = BlockTypes.get(block.substring(0, block.indexOf('[')));
                Map<Property<?>, Object> values =
                        DefaultBlockParser.parseProperties(type, properties, null, true);
                if (values == null || values.isEmpty()) {
                    continue;
                }
                if (blockedStates == null) {
                    blockedStates = new HashSet<>();
                }
                blockedStates.add(new FuzzyBlockState(type.getDefaultState(), values));
            }
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

    @SuppressWarnings("unchecked")
    private <B extends BlockStateHolder<B>> B checkBlock(B block) {
        if (blockedBlocks != null) {
            if (blockedBlocks.contains(block.getBlockType().id())) {
                return (B) (block instanceof BlockState ? RESERVED : RESERVED.toBaseBlock()); // set to reserved/empty
            }
        }
        if (blockedStates == null) {
            return block;
        }
        for (FuzzyBlockState state : blockedStates) {
            if (state.equalsFuzzy(block)) {
                return (B) (block instanceof BlockState ? RESERVED : RESERVED.toBaseBlock());
            }
        }
        if (remaps == null || remaps.isEmpty()) {
            return block;
        }
        for (PropertyRemap<?> remap : remaps) {
            block = remap.apply(block);
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
            it:
            for (int i = 0; i < blocks.length; i++) {
                char block = blocks[i];
                if (block == BlockTypesCache.ReservedIDs.__RESERVED__) {
                    continue;
                }
                BlockState state = BlockTypesCache.states[block];
                if (blockedBlocks != null) {
                    if (blockedBlocks.contains(state.getBlockType().id())) {
                        blocks[i] = BlockTypesCache.ReservedIDs.__RESERVED__;
                        continue;
                    }
                }
                if (blockedStates == null) {
                    continue;
                }
                for (FuzzyBlockState fuzzy : blockedStates) {
                    if (fuzzy.equalsFuzzy(state)) {
                        blocks[i] = BlockTypesCache.ReservedIDs.__RESERVED__;
                        continue it;
                    }
                }
                if (remaps == null || remaps.isEmpty()) {
                    blocks[i] = block;
                    continue;
                }
                for (PropertyRemap<?> remap : remaps) {
                    state = remap.apply(state);
                }
                blocks[i] = state.getOrdinalChar();
            }
        }
        return set;
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
        return ProcessorScope.REMOVING_BLOCKS;
    }

}
