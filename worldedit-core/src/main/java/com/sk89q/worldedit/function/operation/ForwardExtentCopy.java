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

package com.sk89q.worldedit.function.operation;

import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.extent.BlockTranslateExtent;
import com.fastasyncworldedit.core.extent.PositionTransformExtent;
import com.fastasyncworldedit.core.function.RegionMaskTestFunction;
import com.fastasyncworldedit.core.function.block.BiomeCopy;
import com.fastasyncworldedit.core.function.block.CombinedBlockCopy;
import com.fastasyncworldedit.core.function.block.SimpleBlockCopy;
import com.fastasyncworldedit.core.function.visitor.IntersectRegionFunction;
import com.fastasyncworldedit.core.queue.implementation.ParallelQueueExtent;
import com.fastasyncworldedit.core.util.ExtentTraverser;
import com.fastasyncworldedit.core.util.MaskTraverser;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.entity.metadata.EntityProperties;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.CombinedRegionFunction;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.RegionMaskingFilter;
import com.sk89q.worldedit.function.entity.ExtentEntityCopy;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.visitor.EntityVisitor;
import com.sk89q.worldedit.function.visitor.RegionVisitor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Identity;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.FlatRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;

import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Makes a copy of a portion of one extent to another extent or another point.
 *
 * <p>This is a forward extent copy, meaning that it iterates over the blocks
 * in the source extent, and will copy as many blocks as there are in the
 * source. Therefore, interpolation will not occur to fill in the gaps.</p>
 */
public class ForwardExtentCopy implements Operation {

    private final Extent source;
    private final Extent destination;
    private final Region region;
    private final BlockVector3 from;
    private final BlockVector3 to;
    private int repetitions = 1;
    private Mask sourceMask = Masks.alwaysTrue();
    private boolean removingEntities;
    private boolean copyingEntities = true; // default to true for backwards compatibility, sort of
    private boolean copyingBiomes;
    private RegionFunction sourceFunction = null;
    private Transform transform = new Identity();
    private Transform currentTransform = null;

    private RegionFunction filterFunction;
    private RegionVisitor lastBiomeVisitor;
    private EntityVisitor lastEntityVisitor;

    private int affectedBlocks;
    private int affectedBiomeCols;
    private int affectedEntities;

    /**
     * Create a new copy using the region's lowest minimum point as the
     * "from" position.
     *
     * @param source      the source extent
     * @param region      the region to copy
     * @param destination the destination extent
     * @param to          the destination position
     * @see #ForwardExtentCopy(Extent, Region, BlockVector3, Extent, BlockVector3) the main constructor
     */
    public ForwardExtentCopy(Extent source, Region region, Extent destination, BlockVector3 to) {
        this(source, region, region.getMinimumPoint(), destination, to);
    }

    /**
     * Create a new copy.
     *
     * @param source      the source extent
     * @param region      the region to copy
     * @param from        the source position
     * @param destination the destination extent
     * @param to          the destination position
     */
    public ForwardExtentCopy(Extent source, Region region, BlockVector3 from, Extent destination, BlockVector3 to) {
        checkNotNull(source);
        checkNotNull(region);
        checkNotNull(from);
        checkNotNull(destination);
        checkNotNull(to);
        this.source = source;
        this.destination = destination;
        this.region = region;
        this.from = from;
        this.to = to;
    }

    /**
     * Get the transformation that will occur on every point.
     *
     * <p>The transformation will stack with each repetition.</p>
     *
     * @return a transformation
     */
    public Transform getTransform() {
        return transform;
    }

    /**
     * Set the transformation that will occur on every point.
     *
     * @param transform a transformation
     * @see #getTransform()
     */
    public void setTransform(Transform transform) {
        checkNotNull(transform);
        this.transform = transform;
    }

    /**
     * Get the mask that gets applied to the source extent.
     *
     * <p>This mask can be used to filter what will be copied from the source.</p>
     *
     * @return a source mask
     */
    public Mask getSourceMask() {
        return sourceMask;
    }

    /**
     * Set a mask that gets applied to the source extent.
     *
     * @param sourceMask a source mask
     * @see #getSourceMask()
     */
    public void setSourceMask(Mask sourceMask) {
        checkNotNull(sourceMask);
        this.sourceMask = sourceMask;
    }

    //FAWE start
    public void setFilterFunction(RegionFunction filterFunction) {
        this.filterFunction = filterFunction;
    }
    //FAWE end

    /**
     * Get the function that gets applied to all source blocks <em>after</em>
     * the copy has been made.
     *
     * @return a source function, or null if none is to be applied
     */
    public RegionFunction getSourceFunction() {
        return sourceFunction;
    }

    /**
     * Set the function that gets applied to all source blocks <em>after</em>
     * the copy has been made.
     *
     * @param function a source function, or null if none is to be applied
     */
    public void setSourceFunction(RegionFunction function) {
        this.sourceFunction = function;
    }

    /**
     * Get the number of repetitions left.
     *
     * @return the number of repetitions
     */
    public int getRepetitions() {
        return repetitions;
    }

    /**
     * Set the number of repetitions left.
     *
     * @param repetitions the number of repetitions
     */
    public void setRepetitions(int repetitions) {
        checkArgument(repetitions >= 0, "number of repetitions must be non-negative");
        this.repetitions = repetitions;
    }

    /**
     * Return whether entities should be copied along with blocks.
     *
     * @return true if copying
     */
    public boolean isCopyingEntities() {
        return copyingEntities;
    }

    /**
     * Set whether entities should be copied along with blocks.
     *
     * @param copyingEntities true if copying
     */
    public void setCopyingEntities(boolean copyingEntities) {
        this.copyingEntities = copyingEntities;
    }

    /**
     * Return whether entities that are copied should be removed.
     *
     * @return true if removing
     */
    public boolean isRemovingEntities() {
        return removingEntities;
    }

    /**
     * Set whether entities that are copied should be removed.
     *
     * @param removingEntities true if removing
     */
    public void setRemovingEntities(boolean removingEntities) {
        this.removingEntities = removingEntities;
    }

    /**
     * Return whether biomes should be copied along with blocks.
     *
     * @return true if copying biomes
     */
    public boolean isCopyingBiomes() {
        return copyingBiomes;
    }

    /**
     * Set whether biomes should be copies along with blocks.
     *
     * @param copyingBiomes true if copying
     */
    public void setCopyingBiomes(boolean copyingBiomes) {
        //FAWE start - FlatRegion
        if (copyingBiomes && !(region instanceof FlatRegion)) {
            throw new UnsupportedOperationException("Can't copy biomes from region that doesn't implement FlatRegion");
        }
        //FAWE end
        this.copyingBiomes = copyingBiomes;
    }

    /**
     * Get the number of affected objects.
     *
     * @return the number of affected
     */
    public int getAffected() {
        return affectedBlocks + affectedBiomeCols + affectedEntities;
    }

    @Override
    public Operation resume(RunContext run) throws WorldEditException {
        //FAWE start
        if (currentTransform == null) {
            currentTransform = transform;
        }
        //FAWE end
        if (lastBiomeVisitor != null) {
            affectedBiomeCols += lastBiomeVisitor.getAffected();
            lastBiomeVisitor = null;
        }
        if (lastEntityVisitor != null) {
            affectedEntities += lastEntityVisitor.getAffected();
            lastEntityVisitor = null;
        }

        //FAWE start
        Extent finalDest = destination;
        BlockVector3 translation = to.subtract(from);

        if (!translation.equals(BlockVector3.ZERO)) {
            finalDest = new BlockTranslateExtent(
                    finalDest,
                    translation.x(),
                    translation.y(),
                    translation.z()
            );
        }
        //FAWE end

        //FAWE start - RegionVisitor > ExtentBlockCopy
        RegionFunction copy;
        RegionVisitor blockCopy = null;
        PositionTransformExtent transExt = null;
        if (!currentTransform.isIdentity()) {
            if (!(currentTransform instanceof AffineTransform) || ((AffineTransform) currentTransform).isOffAxis()) {
                transExt = new PositionTransformExtent(source, currentTransform.inverse());
                transExt.setOrigin(from);
                copy = new SimpleBlockCopy(transExt, finalDest);
                if (this.filterFunction != null) {
                    copy = new IntersectRegionFunction(filterFunction, copy);
                }
                if (sourceFunction != null) {
                    copy = CombinedRegionFunction.combine(copy, sourceFunction);
                }
                if (sourceMask != Masks.alwaysTrue()) {
                    new MaskTraverser(sourceMask).reset(transExt);
                    copy = new RegionMaskingFilter(sourceMask, copy);
                }
                if (copyingBiomes && (source.isWorld() || region instanceof FlatRegion)) {
                    copy = CombinedRegionFunction.combine(copy, new BiomeCopy(source, finalDest));
                }
                blockCopy = new BackwardsExtentBlockCopy(region, from, transform, copy);
            } else {
                transExt = new PositionTransformExtent(finalDest, currentTransform);
                transExt.setOrigin(from);
                finalDest = transExt;
            }
        }

        if (blockCopy == null) {
            RegionFunction maskFunc = null;

            if (sourceFunction != null) {
                BlockVector3 disAbs = translation.abs();
                BlockVector3 size = region.getMaximumPoint().subtract(region.getMinimumPoint()).add(1, 1, 1);
                boolean overlap = (disAbs.x() < size.x() && disAbs.y() < size.y() && disAbs.z() < size
                        .z());

                RegionFunction copySrcFunc = sourceFunction;
                if (overlap && translation.length() != 0) {

                    int x = translation.x();
                    int y = translation.y();
                    int z = translation.z();

                    maskFunc = position -> {
                        BlockVector3 bv = BlockVector3.at(
                                position.x() + x,
                                position.y() + y,
                                position.z() + z
                        );
                        if (region.contains(bv)) {
                            return sourceFunction.apply(bv);
                        }
                        return false;
                    };

                    copySrcFunc = position -> {
                        BlockVector3 bv = BlockVector3.at(
                                position.x() - x,
                                position.y() - y,
                                position.z() - z
                        );
                        if (!region.contains(bv)) {
                            return sourceFunction.apply(position);
                        }
                        return false;
                    };
                }
                copy = new CombinedBlockCopy(source, finalDest, copySrcFunc);
            } else {
                copy = new SimpleBlockCopy(source, finalDest);
            }
            if (this.filterFunction != null) {
                copy = new IntersectRegionFunction(filterFunction, copy);
            }
            if (sourceMask != Masks.alwaysTrue()) {
                if (maskFunc != null) {
                    copy = new RegionMaskTestFunction(sourceMask, copy, maskFunc);
                } else {
                    copy = new RegionMaskingFilter(sourceMask, copy);
                }
            }
            if (copyingBiomes && (source.isWorld() || region instanceof FlatRegion)) {
                copy = CombinedRegionFunction.combine(copy, new BiomeCopy(source, finalDest));
            }
            ExtentTraverser<ParallelQueueExtent> queueTraverser = new ExtentTraverser<>(finalDest).find(ParallelQueueExtent.class);
            Extent preloader = queueTraverser != null ? queueTraverser.get() : source;
            blockCopy = new RegionVisitor(region, copy, preloader);
        }

        List<? extends Entity> entities;
        if (copyingEntities) {
            // filter players since they can't be copied
            entities = source.getEntities(region);
            entities.removeIf(entity -> {
                EntityProperties properties = entity.getFacet(EntityProperties.class);
                return properties != null && !properties.isPasteable();
            });
        } else {
            entities = Collections.emptyList();
        }

        for (int i = 0; i < repetitions; i++) {
            Operations.completeBlindly(blockCopy);

            if (!entities.isEmpty()) {
                ExtentEntityCopy entityCopy = new ExtentEntityCopy(
                        source,
                        from.toVector3(),
                        destination,
                        to.toVector3(),
                        currentTransform
                );
                entityCopy.setRemoving(removingEntities);
                List<? extends Entity> entities2 = Lists.newArrayList(source.getEntities(region));
                entities2.removeIf(entity -> {
                    EntityProperties properties = entity.getFacet(EntityProperties.class);
                    return properties != null && !properties.isPasteable();
                });
                EntityVisitor entityVisitor = new EntityVisitor(entities.iterator(), entityCopy);
                Operations.completeBlindly(entityVisitor);
                affectedEntities += entityVisitor.getAffected();
            }

            if (transExt != null) {
                currentTransform = currentTransform.combine(transform);
                transExt.setTransform(currentTransform);
            }

        }
        affectedBlocks += blockCopy.getAffected();
        if (copyingBiomes) {
            // We know biomes will have happened unless something else has gone wrong. Just calculate it.
            affectedBiomeCols += source.fullySupports3DBiomes() ? (getAffected() >> 2) : (region.getWidth() * region.getLength());
        }
        //FAWE end
        return null;
    }

    @Override
    public void cancel() {
    }

    @Override
    public Iterable<Component> getStatusMessages() {
        return ImmutableList.of(
                Caption.of(
                        "worldedit.operation.affected.block",
                        TextComponent.of(affectedBlocks)
                ),
                Caption.of(
                        "worldedit.operation.affected.biome",
                        TextComponent.of(affectedBiomeCols)
                ),
                Caption.of(
                        "worldedit.operation.affected.entity",
                        TextComponent.of(affectedEntities)
                )
        );
    }

}
