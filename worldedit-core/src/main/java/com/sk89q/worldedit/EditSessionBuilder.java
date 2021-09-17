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
import com.fastasyncworldedit.core.extent.FaweRegionExtent;
import com.fastasyncworldedit.core.extent.HistoryExtent;
import com.fastasyncworldedit.core.extent.LimitExtent;
import com.fastasyncworldedit.core.extent.MultiRegionExtent;
import com.fastasyncworldedit.core.extent.NullExtent;
import com.fastasyncworldedit.core.extent.SingleRegionExtent;
import com.fastasyncworldedit.core.extent.SlowExtent;
import com.fastasyncworldedit.core.extent.StripNBTExtent;
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
import com.fastasyncworldedit.core.object.FaweLimit;
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
import com.sk89q.worldedit.util.Identifiable;
import com.sk89q.worldedit.util.eventbus.EventBus;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.world.World;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;

/**
 * A builder-style factory for {@link EditSession EditSessions}.
 */
public final class EditSessionBuilder extends com.fastasyncworldedit.core.util.EditSessionBuilder {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private final EventBus eventBus;
    public FaweLimit limit;
    public AbstractChangeSet changeSet;
    public Region[] allowedRegions;
    public Boolean autoQueue;
    public Boolean fastmode;
    public Boolean checkMemory;
    public Boolean combineStages;
    public EditSessionEvent event;
    public String command;
    public RelightMode relightMode;
    public Relighter relighter;
    public Boolean wnaMode;
    public AbstractChangeSet changeTask;
    public Extent bypassHistory;
    public Extent bypassAll;
    public Extent extent;
    public boolean compiled;
    public boolean wrapped;
    private @Nullable World world;
    private int maxBlocks = -1;
    @Nullable private Actor actor;
    @Nullable private BlockBag blockBag;
    private boolean tracing;

    EditSessionBuilder(EventBus eventBus) {
        super(null); //TODO - REMOVE THIS
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

    //TODO: Actor may need to be changed to player unless major refactoring can be done. -Matt

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

    // Extended methods
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
        //if (WorldEdit.getInstance().getConfiguration().traceUnflushedSessions) {
        //    return new TracedEditSession(eventBus, world, maxBlocks, blockBag, actor, tracing);
        //} TODO - check if needed and if so, optimize
        return new EditSession(this);
    }

    public EditSessionBuilder setDirty() {
        compiled = false;
        return this;
    }

    public EditSessionBuilder event(@Nullable EditSessionEvent event) {
        this.event = event;
        return setDirty();
    }
    public EditSessionBuilder limit(@Nullable FaweLimit limit) {
        this.limit = limit;
        return setDirty();
    }

    public EditSessionBuilder limitUnlimited() {
        return limit(FaweLimit.MAX.copy());
    }

    public EditSessionBuilder limitUnprocessed(@NotNull Actor player) {
        limitUnlimited();
        FaweLimit tmp = player.getLimit();
        limit.INVENTORY_MODE = tmp.INVENTORY_MODE;
        return setDirty();
    }

    public EditSessionBuilder changeSet(@Nullable AbstractChangeSet changeSet) {
        this.changeSet = changeSet;
        return setDirty();
    }

    public EditSessionBuilder changeSetNull() {
        return changeSet(new NullChangeSet(world));
    }

    public EditSessionBuilder command(String command) {
        this.command = command;
        return this;
    }

    public EditSessionBuilder changeSet(boolean disk, @Nullable UUID uuid, int compression) {
        if (disk) {
            if (Settings.IMP.HISTORY.USE_DATABASE) {
                this.changeSet = new RollbackOptimizedHistory(world, uuid);
            } else {
                this.changeSet = new DiskStorageHistory(world, uuid);
            }
        } else {
            this.changeSet = new MemoryOptimizedHistory(world);
        }
        return setDirty();
    }

    public EditSessionBuilder allowedRegions(@Nullable Region[] allowedRegions) {
        this.allowedRegions = allowedRegions;
        return setDirty();
    }

    @Deprecated
    public EditSessionBuilder allowedRegions(@Nullable RegionWrapper[] allowedRegions) {
        this.allowedRegions = allowedRegions;
        return setDirty();
    }

    public EditSessionBuilder allowedRegions(@Nullable RegionWrapper allowedRegion) {
        this.allowedRegions = allowedRegion == null ? null : allowedRegion.toArray();
        return setDirty();
    }

    public EditSessionBuilder allowedRegionsEverywhere() {
        return allowedRegions(new Region[]{RegionWrapper.GLOBAL()});
    }

    public EditSessionBuilder autoQueue(@Nullable Boolean autoQueue) {
        this.autoQueue = autoQueue;
        return setDirty();
    }

    public EditSessionBuilder fastmode(@Nullable Boolean fastmode) {
        this.fastmode = fastmode;
        return setDirty();
    }

    public EditSessionBuilder relightMode(@Nullable RelightMode relightMode) {
        this.relightMode = relightMode;
        return setDirty();
    }

    public EditSessionBuilder checkMemory(@Nullable Boolean checkMemory) {
        this.checkMemory = checkMemory;
        return setDirty();
    }

    public EditSessionBuilder combineStages(@Nullable Boolean combineStages) {
        this.combineStages = combineStages;
        return setDirty();
    }

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
        if (autoQueue == null) {
            autoQueue = true;
        }
        if (fastmode == null) {
            if (actor == null) {
                fastmode = !Settings.IMP.HISTORY.ENABLE_FOR_CONSOLE;
            } else {
                fastmode = actor.getSession().hasFastMode();
            }
        }
        if (checkMemory == null) {
            checkMemory = actor != null && !this.fastmode;
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
            boolean placeChunks = this.fastmode || this.limit.FAST_PLACEMENT;

            if (placeChunks && (wnaMode == null || !wnaMode)) {
                wnaMode = false;
                if (unwrapped instanceof IQueueExtent) {
                    extent = queue = (IQueueExtent) unwrapped;
                } else if (Settings.IMP.QUEUE.PARALLEL_THREADS > 1 && !Fawe.isMainThread()) {
                    ParallelQueueExtent parallel = new ParallelQueueExtent(Fawe.get().getQueueHandler(), world, fastmode);
                    queue = parallel.getExtent();
                    extent = parallel;
                } else {
                    extent = queue = Fawe.get().getQueueHandler().getQueue(world);
                }
            } else {
                wnaMode = true;
                extent = world;
            }
            if (combineStages == null) {
                combineStages =
                        // If it's enabled in the settings
                        Settings.IMP.HISTORY.COMBINE_STAGES
                                // If fast placement is disabled, it's slower to perform a copy on each chunk
                                && this.limit.FAST_PLACEMENT
                                // If the edit uses items from the inventory we can't use a delayed task
                                && this.blockBag == null;
            }
            extent = this.bypassAll = wrapExtent(extent, eventBus, event, EditSession.Stage.BEFORE_CHANGE);
            this.bypassHistory = this.extent = wrapExtent(bypassAll, eventBus, event, EditSession.Stage.BEFORE_REORDER);
            if (!this.fastmode || changeSet != null) {
                if (changeSet == null) {
                    if (Settings.IMP.HISTORY.USE_DISK) {
                        UUID uuid = actor == null ? Identifiable.CONSOLE : actor.getUniqueId();
                        if (Settings.IMP.HISTORY.USE_DATABASE) {
                            changeSet = new RollbackOptimizedHistory(world, uuid);
                        } else {
                            changeSet = new DiskStorageHistory(world, uuid);
                        }
                    } else {
                        if (combineStages && Settings.IMP.HISTORY.COMPRESSION_LEVEL == 0) {
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
                        changeTask = changeSet;
                        this.extent = extent.enableHistory(changeSet);
                    } else {
                        this.extent = new HistoryExtent(extent, changeSet);
                    }
                }
            }
            if (allowedRegions == null) {
                if (actor != null && !actor.hasPermission("fawe.bypass") && !actor.hasPermission("fawe.bypass.regions")) {
                    if (actor instanceof Player) {
                        Player player = (Player) actor;
                        allowedRegions = player.getCurrentRegions();
                    }
                }
            }
            FaweRegionExtent regionExtent = null;
            if (allowedRegions != null) {
                if (allowedRegions.length == 0) {
                    regionExtent = new NullExtent(this.extent, FaweCache.NO_REGION);
                } else {
                    if (allowedRegions.length == 1) {
                        regionExtent = new SingleRegionExtent(this.extent, this.limit, allowedRegions[0]);
                    } else {
                        regionExtent = new MultiRegionExtent(this.extent, this.limit, allowedRegions);
                    }
                }
            } else {
                allowedRegions = new Region[]{RegionWrapper.GLOBAL()};
//                this.extent = new HeightBoundExtent(this.extent, this.limit, 0, world.getMaxY());
            }
            // There's no need to do lighting (and it'll also just be a pain to implement) if we're not placing chunks
            if (placeChunks && ((relightMode != null && relightMode != RelightMode.NONE) || (relightMode == null && Settings.IMP.LIGHTING.MODE > 0))) {
                relighter = WorldEdit.getInstance().getPlatformManager()
                        .queryCapability(Capability.WORLD_EDITING)
                        .getRelighterFactory().createRelighter(relightMode, world, queue);
                extent.addProcessor(new RelightProcessor(relighter));
            } else {
                relighter = NullRelighter.INSTANCE;
            }
            extent.addProcessor(new HeightmapProcessor(world.getMinY(), world.getMaxY()));
            if (limit != null && !limit.isUnlimited() && regionExtent != null) {
                this.extent = new LimitExtent(regionExtent, limit);
            } else if (limit != null && !limit.isUnlimited()) {
                this.extent = new LimitExtent(this.extent, limit);
            } else if (regionExtent != null) {
                this.extent = regionExtent;
            }
            if (this.limit != null && this.limit.STRIP_NBT != null && !this.limit.STRIP_NBT.isEmpty()) {
                //TODO add batch processor for strip nbt
                this.extent = new StripNBTExtent(this.extent, this.limit.STRIP_NBT);
            }
            this.extent = wrapExtent(this.extent, eventBus, event, EditSession.Stage.BEFORE_HISTORY);
        }
        return this;
    }

    public Relighter getRelighter() {
        return relighter;
    }

    public boolean isWNAMode() {
        return wnaMode;
    }

    @Nullable
    public Region[] getAllowedRegions() {
        return allowedRegions;
    }

    public com.fastasyncworldedit.core.util.EditSessionBuilder forceWNA() {
        this.wnaMode = true;
        return setDirty();
    }

    public Extent wrapExtent(final Extent extent, final EventBus eventBus, EditSessionEvent event, final EditSession.Stage stage) {
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
            for (String allowed : Settings.IMP.EXTENT.ALLOWED_PLUGINS) {
                if (className.contains(allowed.toLowerCase(Locale.ROOT))) {
                    this.wrapped = true;
                    return toReturn;
                }
            }
            if (Settings.IMP.EXTENT.DEBUG) {
                if (event.getActor() != null) {
                    event.getActor().printDebug(TextComponent.of("Potentially unsafe extent blocked: " + toReturn.getClass().getName()));
                    event.getActor().printDebug(TextComponent.of(" - For area restrictions and block logging, it is recommended to use the FaweAPI"));
                    event.getActor().printDebug(TextComponent.of(" - To allow " + toReturn.getClass().getName() + ", add it to the FAWE `allowed-plugins` list in config.yml"));
                    event.getActor().printDebug(TextComponent.of(" - If you are unsure which plugin tries to use the extent, you can find some additional information below:"));
                    event.getActor().printDebug(TextComponent.of(" - " + toReturn.getClass().getClassLoader()));
                } else {
                    LOGGER.debug("Potentially unsafe extent blocked: " + toReturn.getClass().getName());
                    LOGGER.debug(" - For area restrictions and block logging, it is recommended to use the FaweAPI");
                    LOGGER.debug(" - To allow " + toReturn.getClass().getName() + ", add it to the FAWE `allowed-plugins` list in config.yml");
                    LOGGER.debug(" - If you are unsure which plugin tries to use the extent, you can find some additional information below:");
                    LOGGER.debug(" - " + toReturn.getClass().getClassLoader());
                }
            }
        }
        return extent;
    }

    public boolean isWrapped() {
        return wrapped;
    }

    public Extent getBypassHistory() {
        return bypassHistory;
    }

    public Extent getBypassAll() {
        return bypassAll;
    }

    @NotNull
    public FaweLimit getLimit() {
        return limit;
    }

    public AbstractChangeSet getChangeTask() {
        return changeTask;
    }

    public Extent getExtent() {
        return extent != null ? extent : world;
    }

}
