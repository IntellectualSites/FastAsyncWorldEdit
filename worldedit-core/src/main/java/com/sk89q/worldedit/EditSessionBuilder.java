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

package com.sk89q.worldedit;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.extent.DisallowedBlocksExtent;
import com.fastasyncworldedit.core.extent.FaweRegionExtent;
import com.fastasyncworldedit.core.extent.HistoryExtent;
import com.fastasyncworldedit.core.extent.LimitExtent;
import com.fastasyncworldedit.core.extent.MultiRegionExtent;
import com.fastasyncworldedit.core.extent.NullExtent;
import com.fastasyncworldedit.core.extent.SingleRegionExtent;
import com.fastasyncworldedit.core.extent.SlowExtent;
import com.fastasyncworldedit.core.extent.StripNBTExtent;
import com.fastasyncworldedit.core.extent.processor.EntityInBlockRemovingProcessor;
import com.fastasyncworldedit.core.extent.processor.heightmap.HeightmapProcessor;
import com.fastasyncworldedit.core.extent.processor.lighting.NullRelighter;
import com.fastasyncworldedit.core.extent.processor.lighting.RelightMode;
import com.fastasyncworldedit.core.extent.processor.lighting.RelightProcessor;
import com.fastasyncworldedit.core.extent.processor.lighting.Relighter;
import com.fastasyncworldedit.core.history.DiskStorageHistory;
import com.fastasyncworldedit.core.history.MemoryOptimizedHistory;
import com.fastasyncworldedit.core.history.RollbackOptimizedHistory;
import com.fastasyncworldedit.core.history.changeset.AbstractChangeSet;
import com.fastasyncworldedit.core.history.changeset.BlockBagChangeSet;
import com.fastasyncworldedit.core.history.changeset.NullChangeSet;
import com.fastasyncworldedit.core.limit.FaweLimit;
import com.fastasyncworldedit.core.limit.PropertyRemap;
import com.fastasyncworldedit.core.queue.IBatchProcessor;
import com.fastasyncworldedit.core.queue.IQueueChunk;
import com.fastasyncworldedit.core.queue.IQueueExtent;
import com.fastasyncworldedit.core.queue.implementation.ParallelQueueExtent;
import com.fastasyncworldedit.core.regions.RegionWrapper;
import com.fastasyncworldedit.core.util.MemUtil;
import com.fastasyncworldedit.core.util.Permission;
import com.fastasyncworldedit.core.wrappers.WorldWrapper;
import com.google.common.base.Preconditions;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.Locatable;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionIntersection;
import com.sk89q.worldedit.util.Identifiable;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.util.eventbus.EventBus;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.world.World;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * A builder-style factory for {@link EditSession EditSessions}.
 */
public final class EditSessionBuilder {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    // Keep heightmaps to maintain behavior and use configured lighting mode
    private static final SideEffectSet FAST_SIDE_EFFECTS = SideEffectSet.none()
            .with(SideEffect.HEIGHTMAPS)
            // apply default value to respect config setting `lighting.mode`
            .with(SideEffect.LIGHTING, SideEffect.LIGHTING.getDefaultValue());

    private final EventBus eventBus;
    private FaweLimit limit;
    private AbstractChangeSet changeSet;
    private Region[] allowedRegions;
    private Region[] disallowedRegions;
    private Boolean fastMode;
    private Boolean checkMemory;
    private Boolean combineStages;
    private EditSessionEvent event;
    private String command;
    private RelightMode relightMode;
    private Relighter relighter;
    private Boolean wnaMode;
    private Extent bypassHistory;
    private Extent bypassAll;
    private Extent extent;
    private boolean compiled;
    private boolean wrapped;
    private SideEffectSet sideEffectSet = null;

    private @Nullable
    World world;
    private int maxBlocks = -1;
    @Nullable
    private Actor actor;
    @Nullable
    private BlockBag blockBag;
    private boolean tracing;

    EditSessionBuilder(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * Set the world for the {@link EditSession}.
     *
     * @param world the world
     * @return this builder
     */
    public EditSessionBuilder world(@Nullable World world) {
        this.world = world;
        return setDirty();
    }

    /**
     * Get the world to be edited if present or null
     */
    @Nullable
    public World getWorld() {
        return world;
    }

    /**
     * Get the maximum number of block changes allowed
     */
    public int getMaxBlocks() {
        return maxBlocks;
    }

    /**
     * Set the maximum blocks to change for the {@link EditSession}.
     *
     * @param maxBlocks the maximum blocks to change
     * @return this builder
     */
    public EditSessionBuilder maxBlocks(int maxBlocks) {
        this.maxBlocks = maxBlocks;
        return setDirty();
    }

    /**
     * Get the actor associated with the edit if present or null
     */
    @Nullable
    public Actor getActor() {
        return actor;
    }

    /**
     * Set the actor who owns the {@link EditSession}.
     *
     * @param actor the actor
     * @return this builder
     */
    public EditSessionBuilder actor(@Nullable Actor actor) {
        this.actor = actor;
        return setDirty();
    }

    /**
     * Get the {@link BlockBag} associated with the edit if present or null
     */
    @Nullable
    public BlockBag getBlockBag() {
        return blockBag;
    }

    /**
     * Set the block bag for the {@link EditSession}.
     *
     * @param blockBag the block bag
     * @return this builder
     */
    public EditSessionBuilder blockBag(@Nullable BlockBag blockBag) {
        this.blockBag = blockBag;
        return setDirty();
    }

    /**
     * Check if tracing is enabled.
     *
     * <em>Internal use only.</em>
     */
    public boolean isTracing() {
        return tracing;
    }

    /**
     * Set tracing enabled/disabled.
     *
     * <em>Internal use only.</em>
     */
    public EditSessionBuilder tracing(boolean tracing) {
        this.tracing = tracing;
        return setDirty();
    }

    /**
     * Set the actor to one with a location/extent associated. Sets both the actor and the world.
     */
    public <A extends Actor & Locatable> EditSessionBuilder locatableActor(A locatable) {
        Extent extent = locatable.getExtent();
        Preconditions.checkArgument(extent instanceof World, "%s is not located in a World", locatable);
        return world(((World) extent)).actor(locatable);
    }

    /**
     * Build the {@link EditSession} using properties described in this builder.
     *
     * @return the new EditSession
     */
    public EditSession build() {
        // TracedEditSession does nothing at the moment.
        //if (WorldEdit.getInstance().getConfiguration().traceUnflushedSessions) {
        //    return new TracedEditSession(this);
        //}
        return new EditSession(this);
    }

    /**
     * Set builder as changed and requiring (re-)compilation
     */
    private EditSessionBuilder setDirty() {
        compiled = false;
        return this;
    }

    /**
     * Set the {@link EditSessionEvent} instance to be used for firing at different stages of preparation
     */
    public EditSessionBuilder event(@Nullable EditSessionEvent event) {
        this.event = event;
        return setDirty();
    }

    /**
     * Set the limit(s) for the edit to use
     */
    public EditSessionBuilder limit(@Nullable FaweLimit limit) {
        this.limit = limit;
        return setDirty();
    }

    /**
     * Set the edit to be able to edit everywhere, and for any number of blocks
     */
    public EditSessionBuilder limitUnlimited() {
        return limit(FaweLimit.MAX.copy());
    }

    /**
     * Unlimited in regions/block changes, but uses the given {@link Actor}'s inventory mode.
     */
    public EditSessionBuilder limitUnprocessed(@Nonnull Actor player) {
        limitUnlimited();
        FaweLimit tmp = player.getLimit();
        limit.INVENTORY_MODE = tmp.INVENTORY_MODE;
        return setDirty();
    }

    /**
     * Set the changeset to be used for history
     */
    public EditSessionBuilder changeSet(@Nullable AbstractChangeSet changeSet) {
        this.changeSet = changeSet;
        return setDirty();
    }

    /**
     * Do not process any history
     */
    public EditSessionBuilder changeSetNull() {
        return changeSet(new NullChangeSet(world));
    }

    /**
     * Set the command used that created this edit. Used in {@link RollbackOptimizedHistory}
     */
    public EditSessionBuilder command(String command) {
        this.command = command;
        return this;
    }

    /**
     * Create a new changeset to be used for the edit's history.
     *
     * @param disk If disk should be used for history storage
     * @param uuid UUID to be used for the history or null if unneeded.
     */
    public EditSessionBuilder changeSet(boolean disk, @Nullable UUID uuid) {
        if (disk) {
            if (Settings.settings().HISTORY.USE_DATABASE) {
                this.changeSet = new RollbackOptimizedHistory(world, uuid);
            } else {
                this.changeSet = new DiskStorageHistory(world, uuid);
            }
        } else {
            this.changeSet = new MemoryOptimizedHistory(world);
        }
        return setDirty();
    }

    /**
     * Set the regions the edit is allowed to operate in. Set to null for the regions to be calculated based on the actor if
     * present
     */
    public EditSessionBuilder allowedRegions(@Nullable Region[] allowedRegions) {
        this.allowedRegions = allowedRegions;
        return setDirty();
    }

    /**
     * Set the regions the edit is allowed to operate in. Set to null for the regions to be calculated based on the actor if
     * present
     */
    @Deprecated
    public EditSessionBuilder allowedRegions(@Nullable RegionWrapper[] allowedRegions) {
        this.allowedRegions = allowedRegions;
        return setDirty();
    }

    /**
     * Set the region the edit is allowed to operate in. Set to null for the regions to be calculated based on the actor if
     * present
     */
    public EditSessionBuilder allowedRegions(@Nullable RegionWrapper allowedRegion) {
        this.allowedRegions = allowedRegion == null ? null : allowedRegion.toArray();
        return setDirty();
    }

    /**
     * Set the regions the edit is allowed to operate in. Set to null for the regions to be calculated based on the actor if
     * present
     */
    public EditSessionBuilder disallowedRegions(@Nullable Region[] disallowedRegions) {
        this.disallowedRegions = disallowedRegions;
        return setDirty();
    }

    /**
     * Set the regions the edit is allowed to operate in. Set to null for the regions to be calculated based on the actor if
     * present
     */
    @Deprecated
    public EditSessionBuilder disallowedRegions(@Nullable RegionWrapper[] disallowedRegions) {
        this.disallowedRegions = disallowedRegions;
        return setDirty();
    }

    /**
     * Set the region the edit is allowed to operate in. Set to null for the regions to be calculated based on the actor if
     * present
     */
    public EditSessionBuilder disallowedRegions(@Nullable RegionWrapper disallowedRegion) {
        this.disallowedRegions = disallowedRegion == null ? null : disallowedRegion.toArray();
        return setDirty();
    }

    /**
     * Set the edit to be allowed to edit everywhere
     */
    public EditSessionBuilder allowedRegionsEverywhere() {
        return allowedRegions(new Region[]{RegionWrapper.GLOBAL()});
    }

    /**
     * Set fast mode. Use null to set to actor's fast mode setting. Also set to true by default if history for console disabled
     */
    public EditSessionBuilder fastMode(@Nullable Boolean fastMode) {
        this.fastMode = fastMode;
        return setDirty();
    }

    /**
     * Set the {@link RelightMode}
     */
    public EditSessionBuilder relightMode(@Nullable RelightMode relightMode) {
        this.relightMode = relightMode;
        return setDirty();
    }

    /**
     * Override if memory usage should be checked during editsession compilation. By default, checks memory if fastmode is not
     * enabled and actor is not null.
     */
    public EditSessionBuilder checkMemory(@Nullable Boolean checkMemory) {
        this.checkMemory = checkMemory;
        return setDirty();
    }

    /**
     * Record history with dispatching:,
     * - Much faster as it avoids duplicate block checks,
     * - Slightly worse compression since dispatch order is different.
     */
    public EditSessionBuilder combineStages(@Nullable Boolean combineStages) {
        this.combineStages = combineStages;
        return setDirty();
    }

    /**
     * Set the side effects to be used with this edit
     *
     * @since 2.12.3
     */
    public EditSessionBuilder setSideEffectSet(@Nullable SideEffectSet sideEffectSet) {
        this.sideEffectSet = sideEffectSet;
        return setDirty();
    }

    /**
     * Compile the builder to the settings given. Prepares history, limits, lighting, etc.
     */
    public EditSessionBuilder compile() {
        if (compiled) {
            return this;
        }

        compiled = true;
        wrapped = false;
        if (event == null) {
            event = new EditSessionEvent(world, actor, -1, null);
        }
        if (actor == null && event.getActor() != null) {
            actor = event.getActor();
        }
        if (limit == null) {
            if (actor == null) {
                limit = FaweLimit.MAX;
            } else {
                limit = actor.getLimit();
            }
        }
        if (fastMode == null) {
            if (actor == null) {
                fastMode = !Settings.settings().HISTORY.ENABLE_FOR_CONSOLE;
            } else {
                fastMode = actor.getSession().hasFastMode();
            }
        }
        if (sideEffectSet == null) {
            sideEffectSet = fastMode ? FAST_SIDE_EFFECTS : SideEffectSet.defaults();
        }
        sideEffectSet = sideEffectSet.with(
                SideEffect.ENTITY_EVENTS,
                fastMode || limit.SKIP_ENTITY_SPAWN_EVENTS ? SideEffect.State.OFF : SideEffect.State.ON
        );
        if (checkMemory == null) {
            checkMemory = actor != null && !this.fastMode;
        }
        if (checkMemory) {
            if (MemUtil.isMemoryLimitedSlow()) {
                if (Permission.hasPermission(actor, "worldedit.fast")) {
                    actor.print(Caption.of("fawe.info.worldedit.oom.admin"));
                }
                throw FaweCache.LOW_MEMORY;
            }
        }
//        this.originalLimit = limit;
        this.blockBag = limit.INVENTORY_MODE != 0 ? blockBag : null;
        this.limit = limit.copy();

        if (extent == null) {
            IQueueExtent<IQueueChunk> queue = null;
            World unwrapped = WorldWrapper.unwrap(world);
            boolean placeChunks = (this.fastMode || this.limit.FAST_PLACEMENT) && (wnaMode == null || !wnaMode);

            if (placeChunks) {
                wnaMode = false;
                if (unwrapped instanceof IQueueExtent) {
                    extent = queue = (IQueueExtent) unwrapped;
                } else if (Settings.settings().QUEUE.PARALLEL_THREADS > 1 && !Fawe.isMainThread()) {
                    ParallelQueueExtent parallel = new ParallelQueueExtent(
                            Fawe.instance().getQueueHandler(),
                            world,
                            fastMode,
                            sideEffectSet
                    );
                    queue = parallel.getExtent();
                    extent = parallel;
                } else {
                    extent = queue = Fawe.instance().getQueueHandler().getQueue(world);
                }
                queue.setSideEffectSet(sideEffectSet);
            } else {
                wnaMode = true;
                extent = world;
            }
            if (combineStages == null) {
                combineStages =
                        // If it's enabled in the settings
                        Settings.settings().HISTORY.COMBINE_STAGES
                                // If fast placement is disabled, it's slower to perform a copy on each chunk
                                && this.limit.FAST_PLACEMENT
                                // If the edit uses items from the inventory we can't use a delayed task
                                && this.blockBag == null;
            }
            extent = this.bypassAll = wrapExtent(extent, eventBus, event, EditSession.Stage.BEFORE_CHANGE);
            this.bypassHistory = this.extent = wrapExtent(bypassAll, eventBus, event, EditSession.Stage.BEFORE_REORDER);
            if (!this.fastMode || this.sideEffectSet.shouldApply(SideEffect.HISTORY) || changeSet != null) {
                if (changeSet == null) {
                    if (Settings.settings().HISTORY.USE_DISK) {
                        UUID uuid = actor == null ? Identifiable.CONSOLE : actor.getUniqueId();
                        if (Settings.settings().HISTORY.USE_DATABASE) {
                            changeSet = new RollbackOptimizedHistory(world, uuid);
                        } else {
                            changeSet = new DiskStorageHistory(world, uuid);
                        }
//                    } else if (combineStages && Settings.settings().HISTORY.COMPRESSION_LEVEL == 0) {
//                        changeSet = new CPUOptimizedChangeSet(world);
                    } else {
                        if (combineStages && Settings.settings().HISTORY.COMPRESSION_LEVEL == 0) {
                            //TODO add CPUOptimizedChangeSet
                        }
                        changeSet = new MemoryOptimizedHistory(world);
                    }
                }
                if (this.limit.SPEED_REDUCTION > 0) {
                    this.extent = this.bypassHistory = new SlowExtent(this.bypassHistory, this.limit.SPEED_REDUCTION);
                }
                if (command != null && changeSet instanceof RollbackOptimizedHistory) {
                    ((RollbackOptimizedHistory) changeSet).setCommand(this.command);
                }
                if (!(changeSet instanceof NullChangeSet)) {
                    if (this.blockBag != null) {
                        //TODO implement block bag as IBatchProcessor
                        changeSet = new BlockBagChangeSet(changeSet, blockBag, limit.INVENTORY_MODE == 1);
                    }
                    if (combineStages) {
                        this.extent = extent.enableHistory(changeSet);
                    } else {
                        this.extent = new HistoryExtent(extent, changeSet);
//                        if (this.blockBag != null) {
//                            this.extent = new BlockBagExtent(this.extent, blockBag, limit.INVENTORY_MODE == 1);
//                        }
                    }
                }
            }
            if (allowedRegions == null && Settings.settings().REGION_RESTRICTIONS) {
                if (actor != null && !actor.hasPermission("fawe.bypass.regions")) {
                    if (actor instanceof Player player) {
                        allowedRegions = player.getAllowedRegions();
                    }
                }
            }
            if (disallowedRegions == null && Settings.settings().REGION_RESTRICTIONS && Settings.settings().REGION_RESTRICTIONS_OPTIONS.ALLOW_BLACKLISTS) {
                if (actor != null && !actor.hasPermission("fawe.bypass.regions")) {
                    if (actor instanceof Player player) {
                        disallowedRegions = player.getDisallowedRegions();
                    }
                }
            }
            // There's no need to do the below (and it'll also just be a pain to implement) if we're not placing chunks
            if (placeChunks) {
                if (this.sideEffectSet.shouldApply(SideEffect.LIGHTING) || (relightMode != null && relightMode != RelightMode.NONE)) {
                    relighter = WorldEdit
                            .getInstance()
                            .getPlatformManager()
                            .queryCapability(Capability.WORLD_EDITING)
                            .getRelighterFactory()
                            .createRelighter(relightMode, world, queue);
                    queue.addProcessor(new RelightProcessor(relighter));
                }
                if (this.sideEffectSet.shouldApply(SideEffect.HEIGHTMAPS)) {
                    queue.addProcessor(new HeightmapProcessor(world.getMinY(), world.getMaxY()));
                }
                if (this.sideEffectSet.shouldApply(SideEffect.NEIGHBORS)) {
                    Region region = allowedRegions == null || allowedRegions.length == 0
                            ? null
                            : allowedRegions.length == 1 ? allowedRegions[0] : new RegionIntersection(allowedRegions);
                    queue.addProcessor(WorldEdit
                            .getInstance()
                            .getPlatformManager()
                            .queryCapability(Capability.WORLD_EDITING)
                            .getPlatformPlacementProcessor(extent, null, region));
                }

                if (!Settings.settings().EXPERIMENTAL.KEEP_ENTITIES_IN_BLOCKS) {
                    queue.addProcessor(new EntityInBlockRemovingProcessor());
                }

                IBatchProcessor platformProcessor = WorldEdit
                        .getInstance()
                        .getPlatformManager()
                        .queryCapability(Capability.WORLD_EDITING)
                        .getPlatformProcessor(fastMode);
                if (platformProcessor != null) {
                    queue.addProcessor(platformProcessor);
                }
                IBatchProcessor platformPostProcessor = WorldEdit
                        .getInstance()
                        .getPlatformManager()
                        .queryCapability(Capability.WORLD_EDITING)
                        .getPlatformPostProcessor(fastMode);
                if (platformPostProcessor != null) {
                    queue.addPostProcessor(platformPostProcessor);
                }
            } else {
                relighter = NullRelighter.INSTANCE;
            }
            if (this.limit != null && this.limit.STRIP_NBT != null && !this.limit.STRIP_NBT.isEmpty()) {
                StripNBTExtent ext = new StripNBTExtent(this.extent, this.limit.STRIP_NBT);
                if (placeChunks) {
                    queue.addProcessor(ext);
                }
                if (!placeChunks || !combineStages) {
                    this.extent = ext;
                }
            }
            if (this.limit != null && !this.limit.isUnlimited()) {
                Set<String> limitBlocks = new HashSet<>();
                if (getActor() != null && !getActor().hasPermission("worldedit.anyblock") && this.limit.UNIVERSAL_DISALLOWED_BLOCKS) {
                    limitBlocks.addAll(WorldEdit.getInstance().getConfiguration().disallowedBlocks);
                }
                if (this.limit.DISALLOWED_BLOCKS != null && !this.limit.DISALLOWED_BLOCKS.isEmpty()) {
                    limitBlocks.addAll(this.limit.DISALLOWED_BLOCKS);
                }
                Set<PropertyRemap<?>> remaps = this.limit.REMAP_PROPERTIES;
                if (!limitBlocks.isEmpty() || (remaps != null && !remaps.isEmpty())) {
                    DisallowedBlocksExtent ext = new DisallowedBlocksExtent(this.extent, limitBlocks, remaps);
                    if (placeChunks) {
                        queue.addProcessor(ext);
                    }
                    if (!placeChunks || !combineStages) {
                        this.extent = ext;
                    }
                }
            }

            FaweRegionExtent regionExtent = null;
            // Always use MultiRegionExtent if we have blacklist regions
            if (allowedRegions != null && allowedRegions.length == 0) {
                regionExtent = new NullExtent(this.extent, FaweCache.NO_REGION);
            } else if (disallowedRegions != null && disallowedRegions.length != 0) {
                regionExtent = new MultiRegionExtent(this.extent, this.limit, allowedRegions, disallowedRegions);
            } else if (allowedRegions == null) {
                allowedRegions = new Region[]{RegionWrapper.GLOBAL()};
            } else if (allowedRegions.length == 1) {
                regionExtent = new SingleRegionExtent(this.extent, this.limit, allowedRegions[0]);
            } else {
                regionExtent = new MultiRegionExtent(this.extent, this.limit, allowedRegions, null);
            }
            if (regionExtent != null) {
                if (placeChunks) {
                    queue.addProcessor(regionExtent);
                }
                if (!placeChunks || !combineStages) {
                    this.extent = regionExtent;
                }
            }
            Consumer<Component> onErrorMessage;
            if (getActor() != null) {
                onErrorMessage = c -> getActor().print(Caption.of("fawe.error.occurred-continuing", c));
            } else {
                onErrorMessage = c -> {
                };
            }
            if (limit != null && !limit.isUnlimited()) {
                this.extent = new LimitExtent(this.extent, limit, onErrorMessage, placeChunks && combineStages);
                // Only process if we're not necessarily going to catch tiles via Extent#setBlock, e.g. because using PQE methods
                if (placeChunks && combineStages) {
                    queue.addProcessor((LimitExtent) this.extent);
                }
            }
            this.extent = wrapExtent(this.extent, eventBus, event, EditSession.Stage.BEFORE_HISTORY);
        }
        return this;
    }

    /**
     * Get the relight engine to be used
     */
    public Relighter getRelighter() {
        return relighter;
    }

    /**
     * If the edit will force using WNA
     */
    public boolean isWNAMode() {
        return wnaMode;
    }

    /**
     * get the allowed regions associated with the edit's restricttions
     */
    @Nullable
    public Region[] getAllowedRegions() {
        return allowedRegions;
    }

    /**
     * Force WNA to be used instead of FAWE's queue system. Will use more memory, be slower, and more likely to cause issues.
     */
    public EditSessionBuilder forceWNA() {
        this.wnaMode = true;
        return setDirty();
    }

    /**
     * If an {@link EditSessionEvent} has been fired yet
     */
    public boolean isWrapped() {
        return wrapped;
    }

    /**
     * Get the base extent that blocks are set to, bypassing any restrictions, limits and history. All extents up to and including
     * {@link com.sk89q.worldedit.EditSession.Stage#BEFORE_REORDER}
     */
    public Extent getBypassHistory() {
        return bypassHistory;
    }

    /**
     * Get the base extent that blocks are set to, bypassing any restrictions, limits and history. All extents up to and including
     * {@link com.sk89q.worldedit.EditSession.Stage#BEFORE_CHANGE}
     */
    public Extent getBypassAll() {
        return bypassAll;
    }

    /**
     * Get the edit's limits
     */
    @Nonnull
    public FaweLimit getLimit() {
        return limit;
    }

    /**
     * Get the change set that will be used for history
     */
    public AbstractChangeSet getChangeTask() {
        return changeSet;
    }

    /**
     * Get the SideEffectSet that will be used
     *
     * @since 2.12.3
     */
    public SideEffectSet getSideEffectSet() {
        return sideEffectSet;
    }

    /**
     * Get the ultimate resultant extent
     */
    public Extent getExtent() {
        return extent != null ? extent : world;
    }

    /**
     * Fire an {@link EditSessionEvent}. Fired after each stage of preparation, allows other plugins to add/alter extents.
     */
    private Extent wrapExtent(
            final Extent extent,
            final EventBus eventBus,
            EditSessionEvent event,
            final EditSession.Stage stage
    ) {
        event = event.clone(stage);
        event.setExtent(extent);
        eventBus.post(event);
        if (event.isCancelled()) {
            return new NullExtent(extent, FaweCache.MANUAL);
        }
        final Extent toReturn = event.getExtent();
        if (toReturn instanceof com.sk89q.worldedit.extent.NullExtent) {
            return new NullExtent(toReturn, FaweCache.MANUAL);
        }
        if (toReturn != extent) {
            String className = toReturn.getClass().getName().toLowerCase(Locale.ROOT);
            for (String allowed : Settings.settings().EXTENT.ALLOWED_PLUGINS) {
                if (className.contains(allowed.toLowerCase(Locale.ROOT))) {
                    this.wrapped = true;
                    return toReturn;
                }
            }
            if (Settings.settings().EXTENT.DEBUG) {
                if (event.getActor() != null) {
                    event.getActor().printDebug(TextComponent.of("Potentially unsafe extent blocked: " + toReturn
                            .getClass()
                            .getName()));
                    event.getActor().print(TextComponent.of(
                            "- For area restrictions and block logging, it is recommended that third party plugins use the FAWE" +
                                    " API"));
                    event.getActor().print(TextComponent.of("- Add the following line to the `allowed-plugins` list in the " +
                            "FAWE config.yml to let FAWE recognize the extent:"));
                    event.getActor().print(toReturn.getClass().getName());
                } else {
                    LOGGER.warn("Potentially unsafe extent blocked: {}", toReturn.getClass().getName());
                    LOGGER.warn(
                            " - For area restrictions and block logging, it is recommended that third party plugins use the FAWE API");
                    LOGGER.warn(
                            " - Add the following classpath to the `allowed-plugins` list in the FAWE config.yml to let FAWE " +
                                    "recognize the extent:");
                    LOGGER.warn(toReturn.getClass().getName());
                }
            }
        }
        return extent;
    }

}
