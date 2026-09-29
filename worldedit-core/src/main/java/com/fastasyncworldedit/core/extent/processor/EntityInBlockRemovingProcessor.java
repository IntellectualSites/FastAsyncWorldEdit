package com.fastasyncworldedit.core.extent.processor;

import com.fastasyncworldedit.core.queue.IBatchProcessor;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

/**
 * Processor that removes existing entities that would not be in air after the edit
 *
 * @since 2.7.0
 */
public class EntityInBlockRemovingProcessor implements IBatchProcessor {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    @Override
    public IChunkSet processSet(final IChunk chunk, final IChunkGet get, final IChunkSet set) {
        try {
            for (CompoundTag tag : get.getEntities()) {
                // Empty tags for seemingly non-existent entities can exist?
                if (tag.getList("Pos").size() == 0) {
                    continue;
                }
                BlockVector3 pos = tag.getEntityPosition().toBlockPoint();
                int x = pos.x() & 15;
                int y = pos.y();
                int z = pos.z() & 15;
                if (!set.hasSection(y >> 4)) {
                    continue;
                }
                if (set.getBlock(x, y, z).getBlockType() != BlockTypes.__RESERVED__ && !set
                        .getBlock(x, y, z)
                        .getBlockType()
                        .getMaterial()
                        .isAir()) {
                    set.removeEntity(tag.getUUID());
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Could not remove entities in blocks in chunk {},{}", chunk.getX(), chunk.getZ(), e);
        }
        return set;
    }

    @Nullable
    @Override
    public Extent construct(final Extent child) {
        throw new UnsupportedOperationException("Processing only");
    }

    @Override
    public ProcessorScope getScope() {
        // After block removal but before history
        return ProcessorScope.CUSTOM;
    }

}
