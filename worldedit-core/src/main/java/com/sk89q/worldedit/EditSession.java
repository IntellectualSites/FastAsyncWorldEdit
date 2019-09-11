/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.example.MappedFaweQueue;
import com.boydti.fawe.jnbt.anvil.MCAQueue;
import com.boydti.fawe.jnbt.anvil.MCAWorld;
import com.boydti.fawe.logging.LoggingChangeSet;
import com.boydti.fawe.logging.rollback.RollbackOptimizedHistory;
import com.boydti.fawe.object.FaweLimit;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.HasFaweQueue;
import com.boydti.fawe.object.HistoryExtent;
import com.boydti.fawe.object.NullChangeSet;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.brush.visualization.VirtualWorld;
import com.boydti.fawe.object.changeset.BlockBagChangeSet;
import com.boydti.fawe.object.changeset.CPUOptimizedChangeSet;
import com.boydti.fawe.object.changeset.DiskStorageHistory;
import com.boydti.fawe.object.changeset.FaweChangeSet;
import com.boydti.fawe.object.changeset.MemoryOptimizedHistory;
import com.boydti.fawe.object.collection.LocalBlockVectorSet;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.object.extent.*;
import com.boydti.fawe.object.function.SurfaceRegionFunction;
import com.boydti.fawe.object.mask.ResettableMask;
import com.boydti.fawe.object.pattern.ExistingPattern;
import com.boydti.fawe.object.progress.ChatProgressTracker;
import com.boydti.fawe.object.progress.DefaultProgressTracker;
import com.boydti.fawe.util.ExtentTraverser;
import com.boydti.fawe.util.MaskTraverser;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.MemUtil;
import com.boydti.fawe.util.Perm;
import com.boydti.fawe.util.SetQueue;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.wrappers.WorldWrapper;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.base.Supplier;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.ChangeSetExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.MaskingExtent;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.extent.inventory.BlockBagExtent;
import com.sk89q.worldedit.extent.world.SurvivalModeExtent;
import com.sk89q.worldedit.function.GroundFunction;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.RegionMaskingFilter;
import com.sk89q.worldedit.function.block.BlockReplace;
import com.sk89q.worldedit.function.block.Naturalizer;
import com.sk89q.worldedit.function.generator.ForestGenerator;
import com.sk89q.worldedit.function.generator.GardenPatchGenerator;
import com.sk89q.worldedit.function.mask.*;
import com.sk89q.worldedit.function.operation.ChangeSetExecutor;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.BlockPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.pattern.WaterloggedRemover;
import com.sk89q.worldedit.function.util.RegionOffset;
import com.sk89q.worldedit.function.visitor.DirectionalVisitor;
import com.sk89q.worldedit.function.visitor.DownwardVisitor;
import com.sk89q.worldedit.function.visitor.FlatRegionVisitor;
import com.sk89q.worldedit.function.visitor.LayerVisitor;
import com.sk89q.worldedit.function.visitor.NonRisingVisitor;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
import com.sk89q.worldedit.function.visitor.RegionVisitor;
import com.sk89q.worldedit.history.UndoContext;
import com.sk89q.worldedit.history.change.BlockChange;
import com.sk89q.worldedit.history.changeset.ChangeSet;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.internal.expression.runtime.EvaluationException;
import com.sk89q.worldedit.internal.expression.runtime.ExpressionTimeoutException;
import com.sk89q.worldedit.internal.expression.runtime.RValue;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MathUtils;
import com.sk89q.worldedit.math.MutableBlockVector2;
import com.sk89q.worldedit.math.MutableBlockVector3;
import com.sk89q.worldedit.math.Vector2;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.interpolation.Interpolation;
import com.sk89q.worldedit.math.interpolation.KochanekBartelsInterpolation;
import com.sk89q.worldedit.math.interpolation.Node;
import com.sk89q.worldedit.math.noise.RandomNoise;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.EllipsoidRegion;
import com.sk89q.worldedit.regions.FlatRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.Regions;
import static com.sk89q.worldedit.regions.Regions.asFlatRegion;
import static com.sk89q.worldedit.regions.Regions.maximumBlockY;
import static com.sk89q.worldedit.regions.Regions.minimumBlockY;
import com.sk89q.worldedit.regions.shape.ArbitraryBiomeShape;
import com.sk89q.worldedit.regions.shape.ArbitraryShape;
import com.sk89q.worldedit.regions.shape.RegionShape;
import com.sk89q.worldedit.regions.shape.WorldEditExpressionEnvironment;
import com.sk89q.worldedit.util.Countable;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.TreeGenerator;
import com.sk89q.worldedit.util.eventbus.EventBus;
import com.sk89q.worldedit.world.SimpleWorld;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockCategories;
import com.sk89q.worldedit.world.block.BlockID;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.weather.WeatherType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * An {@link Extent} that handles history, {@link BlockBag}s, change limits,
 * block re-ordering, and much more. Most operations in WorldEdit use this class.
 *
 * <p>Most of the actual functionality is implemented with a number of other
 * {@link Extent}s that are chained together. For example, history is logged
 * using the {@link ChangeSetExtent}.</p>
 */
@SuppressWarnings({"FieldCanBeLocal"})
public class EditSession extends AbstractDelegateExtent implements HasFaweQueue, SimpleWorld, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(EditSession.class);

    /**
     * Used by {@link EditSession#setBlock(BlockVector3, BlockStateHolder, Stage)} to
     * determine which {@link Extent}s should be bypassed.
     */
    public enum Stage {
        BEFORE_HISTORY,
        BEFORE_REORDER,
        BEFORE_CHANGE
    }

    /**
     * Reorder mode for {@link EditSession#setReorderMode(ReorderMode)}.
     * NOT FUNCTIONAL IN FAWE AS OF June 3,2019
     *
     * MULTI_STAGE = Multi stage reorder, may not be great with mods.
     * FAST = Use the fast mode. Good for mods.
     * NONE = Place blocks without worrying about placement order.
     */
    public enum ReorderMode {
        MULTI_STAGE("multi"),
        FAST("fast"),
        NONE("none");

        private String displayName;

        ReorderMode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return this.displayName;
        }
    }

    @SuppressWarnings("ProtectedField")
    protected final World world;
    private String worldName;
    private FaweQueue queue;
    private boolean wrapped;
    private boolean fastMode;
    private AbstractDelegateExtent extent;
    private HistoryExtent history;
    private AbstractDelegateExtent bypassHistory;
    private AbstractDelegateExtent bypassAll;
    private FaweLimit originalLimit;
    private FaweLimit limit;
    private FawePlayer player;
    private FaweChangeSet changeTask;

    private MutableBlockVector3 mutablebv = new MutableBlockVector3();

    private int changes = 0;
    private BlockBag blockBag;

    private final int maxY;

    public static final UUID CONSOLE = UUID.fromString("1-1-3-3-7");

    @Deprecated
    public EditSession(@Nonnull World world, @Nullable FaweQueue queue, @Nullable FawePlayer player, @Nullable FaweLimit limit, @Nullable FaweChangeSet changeSet, @Nullable RegionWrapper[] allowedRegions, @Nullable Boolean autoQueue, @Nullable Boolean fastmode, @Nullable Boolean checkMemory, @Nullable Boolean combineStages, @Nullable BlockBag blockBag, @Nullable EventBus bus, @Nullable EditSessionEvent event) {
        this(null, world, queue, player, limit, changeSet, allowedRegions, autoQueue, fastmode, checkMemory, combineStages, blockBag, bus, event);
    }

    public EditSession(@Nullable String worldName, @Nullable World world, @Nullable FaweQueue queue, @Nullable FawePlayer player, @Nullable FaweLimit limit, @Nullable FaweChangeSet changeSet, @Nullable Region[] allowedRegions, @Nullable Boolean autoQueue, @Nullable Boolean fastmode, @Nullable Boolean checkMemory, @Nullable Boolean combineStages, @Nullable BlockBag blockBag, @Nullable EventBus bus, @Nullable EditSessionEvent event) {
        super(world);
        this.worldName = worldName == null ? world == null ? queue == null ? "" : queue.getWorldName() : world.getName() : worldName;
        if (world == null && this.worldName != null) world = FaweAPI.getWorld(this.worldName);

        this.world = world;

        if (bus == null) {
            bus = WorldEdit.getInstance().getEventBus();
        }
        if (event == null) {
            event = new EditSessionEvent(world, player == null ? null : (player.getPlayer()), -1, null);
        }
        event.setEditSession(this);
        if (player == null && event.getActor() != null) {
            player = FawePlayer.wrap(event.getActor());
        }
        this.player = player;
        if (limit == null) {
            if (player == null) {
                limit = FaweLimit.MAX;
            } else {
                limit = player.getLimit();
            }
        }
        if (autoQueue == null) {
            autoQueue = true;
        }
        if (fastmode == null) {
            if (player == null) {
                fastmode = !Settings.IMP.HISTORY.ENABLE_FOR_CONSOLE;
            } else {
                fastmode = player.getSession().hasFastMode();
            }
        }
        this.fastMode = fastmode;
        if (checkMemory == null) {
            checkMemory = player != null && !this.fastMode;
        }
        if (checkMemory) {
            if (MemUtil.isMemoryLimitedSlow()) {
                if (Perm.hasPermission(player, "worldedit.fast")) {
                    BBC.WORLDEDIT_OOM_ADMIN.send(player);
                }
                throw new FaweException(BBC.WORLDEDIT_CANCEL_REASON_LOW_MEMORY);
            }
        }
        this.originalLimit = limit;
        this.blockBag = limit.INVENTORY_MODE != 0 ? blockBag : null;
        this.limit = limit.copy();

        if (queue == null) {
            boolean placeChunks = this.fastMode || this.limit.FAST_PLACEMENT;
            World unwrapped = WorldWrapper.unwrap(world);
            if (unwrapped instanceof FaweQueue) {
                queue = (FaweQueue) unwrapped;
            } else if (unwrapped instanceof MCAWorld) {
                queue = ((MCAWorld) unwrapped).getQueue();
            } else if (player != null && world.equals(player.getWorld())) {
                queue = player.getFaweQueue(placeChunks, autoQueue);
            } else {
                queue = SetQueue.IMP.getNewQueue(world, placeChunks, autoQueue);
            }
        }
        if (combineStages == null) {
            combineStages =
                    // If it's enabled in the settings
                    Settings.IMP.HISTORY.COMBINE_STAGES
                    // If fast placement is disabled, it's slower to perform a copy on each chunk
                    && this.limit.FAST_PLACEMENT
                    // If the specific queue doesn't support it
                    && queue.supports(FaweQueue.Capability.CHANGE_TASKS)
                    // If the edit uses items from the inventory we can't use a delayed task
                    && this.blockBag == null;
        }
        if (Settings.IMP.EXPERIMENTAL.ANVIL_QUEUE_MODE && !(queue instanceof MCAQueue)) {
            queue = new MCAQueue(queue);
        }
        this.queue = queue;
        this.queue.addEditSession(this);
        if (!Settings.IMP.QUEUE.PROGRESS.DISPLAY.equalsIgnoreCase("false") && player != null) {
            switch (Settings.IMP.QUEUE.PROGRESS.DISPLAY.toLowerCase()) {
                case "chat":
                    this.queue.setProgressTask(new ChatProgressTracker(player));
                    break;
                case "title":
                case "true":
                default:
                    this.queue.setProgressTask(new DefaultProgressTracker(player));
            }
        }
        this.bypassAll = wrapExtent(new FastWorldEditExtent(world, queue), bus, event, Stage.BEFORE_CHANGE);
        this.bypassHistory = (this.extent = wrapExtent(bypassAll, bus, event, Stage.BEFORE_REORDER));
        if (!this.fastMode || changeSet != null) {
            if (changeSet == null) {
                if (Settings.IMP.HISTORY.USE_DISK) {
                    UUID uuid = player == null ? CONSOLE : player.getUUID();
                    if (Settings.IMP.HISTORY.USE_DATABASE) {
                        changeSet = new RollbackOptimizedHistory(world, uuid);
                    } else {
                        changeSet = new DiskStorageHistory(world, uuid);
                    }
                } else if (combineStages && Settings.IMP.HISTORY.COMPRESSION_LEVEL == 0 && !(queue instanceof MCAQueue)) {
                    changeSet = new CPUOptimizedChangeSet(world);
                } else {
                    changeSet = new MemoryOptimizedHistory(world);
                }
            }
            if (this.limit.SPEED_REDUCTION > 0) {
                this.bypassHistory = new SlowExtent(this.bypassHistory, this.limit.SPEED_REDUCTION);
            }
            if (changeSet instanceof NullChangeSet && Fawe.imp().getBlocksHubApi() != null && player != null) {
                changeSet = LoggingChangeSet.wrap(player, changeSet);
            }
            if (!(changeSet instanceof NullChangeSet)) {
                if (!(changeSet instanceof LoggingChangeSet) && player != null && Fawe.imp().getBlocksHubApi() != null) {
                    changeSet = LoggingChangeSet.wrap(player, changeSet);
                }
                if (this.blockBag != null) {
                    changeSet = new BlockBagChangeSet(changeSet, blockBag, limit.INVENTORY_MODE == 1);
                }
                if (combineStages) {
                    changeTask = changeSet;
                    changeSet.addChangeTask(queue);
                } else {
                    this.extent = (history = new HistoryExtent(this, bypassHistory, changeSet, queue));
//                    if (this.blockBag != null) {
//                        this.extent = new BlockBagExtent(this.extent, blockBag, limit.INVENTORY_MODE == 1);
//                    }
                }
            }
        }
        if (allowedRegions == null) {
            if (player != null && !player.hasPermission("fawe.bypass") && !player.hasPermission("fawe.bypass.regions") && !(queue instanceof VirtualWorld)) {
                allowedRegions = player.getCurrentRegions();
            }
        }
        this.maxY = getWorld() == null ? 255 : world.getMaxY();
        if (allowedRegions != null) {
            if (allowedRegions.length == 0) {
                this.extent = new NullExtent(this.extent, BBC.WORLDEDIT_CANCEL_REASON_NO_REGION);
            } else {
                this.extent = new ProcessedWEExtent(this.extent, this.limit);
                if (allowedRegions.length == 1) {
                    this.extent = new SingleRegionExtent(this.extent, this.limit, allowedRegions[0]);
                } else {
                    this.extent = new MultiRegionExtent(this.extent, this.limit, allowedRegions);
                }
            }
        } else {
            this.extent = new HeightBoundExtent(this.extent, this.limit, 0, maxY);
        }
        if (this.limit.STRIP_NBT != null && !this.limit.STRIP_NBT.isEmpty()) {
            this.extent = new StripNBTExtent(this.extent, this.limit.STRIP_NBT);
        }
        this.extent = wrapExtent(this.extent, bus, event, Stage.BEFORE_HISTORY);
        setExtent(this.extent);
    }

    /**
     * Construct the object with a maximum number of blocks and a block bag.
     *
     * @param eventBus the event bus
     * @param world the world
     * @param maxBlocks the maximum number of blocks that can be changed, or -1 to use no limit
     * @param blockBag an optional {@link BlockBag} to use, otherwise null
     * @param event the event to call with the extent
     */
    public EditSession(EventBus eventBus, World world, int maxBlocks, @Nullable BlockBag blockBag, EditSessionEvent event) {
        this(world, null, null, null, null, null, true, null, null, null, blockBag, eventBus, event);
    }

    /**
     * The limit for this specific edit (blocks etc)
     *
     * @return
     */
    public FaweLimit getLimit() {
        return originalLimit;
    }

    public void resetLimit() {
        this.limit.set(this.originalLimit);
        ExtentTraverser<ProcessedWEExtent> find = new ExtentTraverser<>(extent).find(ProcessedWEExtent.class);
        if (find != null && find.get() != null) {
            find.get().setLimit(this.limit);
        }
    }

    /**
     * Returns a new limit representing how much of this edit's limit has been used so far
     *
     * @return
     */
    public FaweLimit getLimitUsed() {
        FaweLimit newLimit = new FaweLimit();
        newLimit.MAX_ACTIONS = originalLimit.MAX_ACTIONS - limit.MAX_ACTIONS;
        newLimit.MAX_CHANGES = originalLimit.MAX_CHANGES - limit.MAX_CHANGES;
        newLimit.MAX_FAILS = originalLimit.MAX_FAILS - limit.MAX_FAILS;
        newLimit.MAX_CHECKS = originalLimit.MAX_CHECKS - limit.MAX_CHECKS;
        newLimit.MAX_ITERATIONS = originalLimit.MAX_ITERATIONS - limit.MAX_ITERATIONS;
        newLimit.MAX_BLOCKSTATES = originalLimit.MAX_BLOCKSTATES - limit.MAX_BLOCKSTATES;
        newLimit.MAX_ENTITIES = originalLimit.MAX_ENTITIES - limit.MAX_ENTITIES;
        newLimit.MAX_HISTORY = limit.MAX_HISTORY;
        return newLimit;
    }

    /**
     * Returns the remaining limits
     *
     * @return
     */
    public FaweLimit getLimitLeft() {
        return limit;
    }

    /**
     * The region extent restricts block placements to allowmaxYed regions
     *
     * @return FaweRegionExtent (may be null)
     */
    public FaweRegionExtent getRegionExtent() {
        ExtentTraverser<FaweRegionExtent> traverser = new ExtentTraverser<>(this.extent).find(FaweRegionExtent.class);
        return traverser == null ? null : traverser.get();
    }

    public Extent getBypassAll() {
        return bypassAll;
    }

    public Extent getBypassHistory() {
        return bypassHistory;
    }

    public Extent getExtent() {
        return extent;
    }

    public void setExtent(AbstractDelegateExtent extent) {
        this.extent = extent;
        new ExtentTraverser(this).setNext(extent);
    }

    /**
     * Get the FawePlayer or null
     *
     * @return
     */
    @Nullable
    public FawePlayer getPlayer() {
        return player;
    }

    public boolean cancel() {
        ExtentTraverser traverser = new ExtentTraverser(this.extent);
        NullExtent nullExtent = new NullExtent(world, BBC.WORLDEDIT_CANCEL_REASON_MANUAL);
        while (traverser != null) {
            ExtentTraverser next = traverser.next();
            Extent get = traverser.get();
            if (get instanceof AbstractDelegateExtent && !(get instanceof NullExtent)) {
                traverser.setNext(nullExtent);
            }
            traverser = next;
        }
        bypassHistory = nullExtent;
        this.extent = nullExtent;
        bypassAll = nullExtent;
        dequeue();
        if (!queue.isEmpty()) {
            if (Fawe.isMainThread()) {
                queue.clear();
            } else {
                SetQueue.IMP.addTask(() -> queue.clear());
            }
        }
        return true;
    }

    /**
     * Remove this EditSession from the queue<br>
     * - This doesn't necessarily stop it from being queued again
     */
    public void dequeue() {
        if (queue != null) {
            SetQueue.IMP.dequeue(queue);
        }
    }

    /**
     * Add a task to run when this EditSession is done dispatching
     *
     * @param whenDone
     */
    public void addNotifyTask(Runnable whenDone) {
        if (queue != null) {
            queue.addNotifyTask(whenDone);
        }
    }

    /**
     * Send a debug message to the Actor responsible for this EditSession (or Console)
     *
     * @param message
     * @param args
     */
    public void debug(BBC message, Object... args) {
        message.send(player, args);
    }

    /**
     * Get the FaweQueue this EditSession uses to queue the changes<br>
     * - Note: All implementation queues for FAWE are instances of NMSMappedFaweQueue
     *
     * @return
     */
    public FaweQueue getQueue() {
        return queue;
    }

    @Deprecated
    private AbstractDelegateExtent wrapExtent(Extent extent, EventBus eventBus, EditSessionEvent event, Stage stage) {
        event = event.clone(stage);
        event.setExtent(extent);
        eventBus.post(event);
        if (event.isCancelled()) {
            return new NullExtent(extent, BBC.WORLDEDIT_CANCEL_REASON_MANUAL);
        }
        final Extent toReturn = event.getExtent();
        if(toReturn instanceof com.sk89q.worldedit.extent.NullExtent) {
        	return new NullExtent(toReturn, null);
        }
        if (!(toReturn instanceof AbstractDelegateExtent)) {
            Fawe.debug("Extent " + toReturn + " must be AbstractDelegateExtent");
            return (AbstractDelegateExtent) extent;
        }
        if (toReturn != extent) {
            String className = toReturn.getClass().getName().toLowerCase();
            for (String allowed : Settings.IMP.EXTENT.ALLOWED_PLUGINS) {
                if (className.contains(allowed.toLowerCase())) {
                    this.wrapped = true;
                    return (AbstractDelegateExtent) toReturn;
                }
            }
            if (Settings.IMP.EXTENT.DEBUG) {
                Fawe.debug("&cPotentially unsafe extent blocked: " + toReturn.getClass().getName());
                Fawe.debug("&8 - &7For area restrictions, it is recommended to use the FaweAPI");
                Fawe.debug("&8 - &7For block logging, it is recommended to use use BlocksHub");
                Fawe.debug("&8 - &7To allow this plugin add it to the FAWE `allowed-plugins` list");
                Fawe.debug("&8 - &7To hide this message set `debug` to false in the FAWE config.yml");
                if (toReturn.getClass().getName().contains("CoreProtect")) {
                    Fawe.debug("Note on CoreProtect: ");
                    Fawe.debug(" - If you disable CP's WE logger (CP config) and this still shows, please update CP");
                    Fawe.debug(" - Use BlocksHub and set `debug` false in the FAWE config");
                }
            }
        }
        return (AbstractDelegateExtent) extent;
    }

    // pkg private for TracedEditSession only, may later become public API
    boolean commitRequired() {
        return false;
    }

    /**
     * Turns on specific features for a normal WorldEdit session, such as
     * {@link #setBatchingChunks(boolean)
     * chunk batching}.
     */
    public void enableStandardMode() {
        setBatchingChunks(true);
    }

    /**
     * Sets the {@link ReorderMode} of this EditSession, and flushes the session.
     *
     * @param reorderMode The reorder mode
     */
    public void setReorderMode(ReorderMode reorderMode) {
        //TODO Not working yet. - It shouldn't need to work. FAWE doesn't need reordering.
    }

    //TODO: Reorder mode.
    /**
     * Get the reorder mode.
     *
     * @return the reorder mode
     */
    public ReorderMode getReorderMode() {
        return null;
    }

    /**
     * Get the world.
     *
     * @return the world
     */
    public World getWorld() {
        return world;
    }

    /**
     * Get the underlying {@link ChangeSet}.
     *
     * @return the change set
     */
    public ChangeSet getChangeSet() {
        return changeTask != null ? changeTask : history != null ? history.getChangeSet() : null;
    }

    /**
     * Will be removed very soon. Use getChangeSet()
     */
    @Deprecated
    public FaweChangeSet getChangeTask() {
        return changeTask;
    }

    /**
     * Set the ChangeSet without hooking into any recording mechanism or triggering any actions.<br/>
     * Used internally to set the ChangeSet during completion to record custom changes which aren't normally recorded
     * @param set
     */
    public void setRawChangeSet(@Nullable FaweChangeSet set) {
        changeTask = set;
        changes++;
    }

    /**
     * Change the ChangeSet being used for this EditSession
     * - If history is disabled, no changeset can be set
     *
     * @param set (null = remove the changeset)
     */
    public void setChangeSet(@Nullable FaweChangeSet set) {
        if (set == null) {
            disableHistory(true);
        } else {
            if (history != null) {
                history.setChangeSet(set);
            } else {
                changeTask = set;
                set.addChangeTask(queue);
            }
        }
        changes++;
    }

    /**
     * Get the maximum number of blocks that can be changed. -1 will be returned
     * if it the limit disabled.
     *
     * @return the limit (&gt;= 0) or -1 for no limit
     * @see #getLimit()
     */
    @Deprecated
    public int getBlockChangeLimit() {
        return originalLimit.MAX_CHANGES;
    }

    /**
     * Set the maximum number of blocks that can be changed.
     *
     * @param limit the limit (&gt;= 0) or -1 for no limit
     */
    public void setBlockChangeLimit(int limit) {
        // Nothing
    }

    /**
     * Returns queue status.
     *
     * @return whether the queue is enabled
     * @deprecated Use {@link EditSession#getReorderMode()} with MULTI_STAGE instead.
     */
    @Deprecated
    public boolean isQueueEnabled() {
        return true;
    }

    /**
     * Queue certain types of block for better reproduction of those blocks.
     *
     * Uses {@link ReorderMode#MULTI_STAGE}
     * @deprecated Use {@link EditSession#setReorderMode(ReorderMode)} with MULTI_STAGE instead.
     */
    @Deprecated
    public void enableQueue() {
    }

    /**
     * Disable the queue. This will close the queue.
     */
    @Deprecated
    public void disableQueue() {
        if (isQueueEnabled()) {
            this.flushQueue();
        }
    }

    /**
     * Get the mask.
     *
     * @return mask, may be null
     */
    public Mask getMask() {
        ExtentTraverser<MaskingExtent> maskingExtent = new ExtentTraverser<>(this.extent).find(MaskingExtent.class);
        return maskingExtent != null ? maskingExtent.get().getMask() : null;
    }

    /**
     * Get the mask.
     *
     * @return mask, may be null
     */
    public Mask getSourceMask() {
        ExtentTraverser<SourceMaskExtent> maskingExtent = new ExtentTraverser<>(this.extent).find(SourceMaskExtent.class);
        return maskingExtent != null ? maskingExtent.get().getMask() : null;
    }

    public void addTransform(ResettableExtent transform) {
        wrapped = true;
        if (transform == null) {
            ExtentTraverser<ResettableExtent> traverser = new ExtentTraverser<>(this.extent).find(ResettableExtent.class);
            AbstractDelegateExtent next = extent;
            while (traverser != null && traverser.get() != null) {
                traverser = traverser.next();
                next = traverser.get();
            }
            this.extent = next;
        } else {
            this.extent = transform.setExtent(extent);
        }
    }

    public @Nullable ResettableExtent getTransform() {
        ExtentTraverser<ResettableExtent> traverser = new ExtentTraverser<>(this.extent).find(ResettableExtent.class);
        if (traverser != null) {
            return traverser.get();
        }
        return null;
    }

    /**
     * Set a mask.
     *
     * @param mask mask or null
     */
    public void setSourceMask(Mask mask) {
        if (mask == null) {
            mask = Masks.alwaysTrue();
        } else {
            new MaskTraverser(mask).reset(this);
        }
        ExtentTraverser<SourceMaskExtent> maskingExtent = new ExtentTraverser<>(this.extent).find(SourceMaskExtent.class);
        if (maskingExtent != null && maskingExtent.get() != null) {
            Mask oldMask = maskingExtent.get().getMask();
            if (oldMask instanceof ResettableMask) {
                ((ResettableMask) oldMask).reset();
            }
            maskingExtent.get().setMask(mask);
        } else if (mask != Masks.alwaysTrue()) {
            this.extent = new SourceMaskExtent(this.extent, mask);
        }
    }

    public void addSourceMask(Mask mask) {
        checkNotNull(mask);
        Mask existing = getSourceMask();
        if (existing != null) {
            if (existing instanceof MaskIntersection) {
                ((MaskIntersection) existing).add(mask);
                return;
            } else {
                MaskIntersection intersection = new MaskIntersection(existing);
                intersection.add(mask);
                mask = intersection;
            }
        }
        setSourceMask(mask);
    }

    /**
     * Set a mask.
     *
     * @param mask mask or null
     */
    public void setMask(Mask mask) {
        if (mask == null) {
            mask = Masks.alwaysTrue();
        } else {
            new MaskTraverser(mask).reset(this);
        }
        ExtentTraverser<MaskingExtent> maskingExtent = new ExtentTraverser<>(this.extent).find(MaskingExtent.class);
        if (maskingExtent != null && maskingExtent.get() != null) {
            Mask oldMask = maskingExtent.get().getMask();
            if (oldMask instanceof ResettableMask) {
                ((ResettableMask) oldMask).reset();
            }
            maskingExtent.get().setMask(mask);
        } else if (mask != Masks.alwaysTrue()) {
            this.extent = new MaskingExtent(this.extent, mask);
        }
    }

    /**
     * Get the {@link SurvivalModeExtent}.
     *
     * @return the survival simulation extent
     */
    public SurvivalModeExtent getSurvivalExtent() {
        ExtentTraverser<SurvivalModeExtent> survivalExtent = new ExtentTraverser<>(this.extent).find(SurvivalModeExtent.class);
        if (survivalExtent != null) {
            return survivalExtent.get();
        } else {
            AbstractDelegateExtent extent = this.extent;
            SurvivalModeExtent survival = new SurvivalModeExtent(extent.getExtent(), getWorld());
            new ExtentTraverser<>(extent).setNext(survival);
            return survival;
        }
    }

    /**
     * Set whether fast mode is enabled.
     *
     * <p>Fast mode may skip lighting checks or adjacent block
     * notification.</p>
     *
     * @param enabled true to enable
     */
    public void setFastMode(boolean enabled) {
        this.fastMode = enabled;
        disableHistory(enabled);
    }

    /**
     * Disable history (or re-enable)
     *
     * @param disableHistory
     */
    public void disableHistory(boolean disableHistory) {
        if (history == null) {
            return;
        }
        ExtentTraverser<HistoryExtent> traverseHistory = new ExtentTraverser<>(this.extent).find(HistoryExtent.class);
        if (disableHistory) {
            if (traverseHistory != null && traverseHistory.exists()) {
                ExtentTraverser<HistoryExtent> beforeHistory = traverseHistory.previous();
                ExtentTraverser<HistoryExtent> afterHistory = traverseHistory.next();
                if (beforeHistory != null && beforeHistory.exists()) {
                    beforeHistory.setNext(afterHistory.get());
                } else {
                    extent = afterHistory.get();
                }
            }
        } else if (traverseHistory == null || !traverseHistory.exists()) {
            ExtentTraverser<AbstractDelegateExtent> traverseBypass = new ExtentTraverser<>(this.extent).find(bypassHistory);
            if (traverseBypass != null) {
                ExtentTraverser<AbstractDelegateExtent> beforeHistory = traverseBypass.previous();
                beforeHistory.setNext(history);
            }
        }
    }

    /**
     * Return fast mode status.
     *
     * <p>Fast mode may skip lighting checks or adjacent block
     * notification.</p>
     *
     * @return true if enabled
     */
    public boolean hasFastMode() {
        return getChangeSet() == null;
    }

    /**
     * Get the {@link BlockBag} is used.
     *
     * @return a block bag or null
     */
    public BlockBag getBlockBag() {
        return this.blockBag;
    }

    /**
     * Set a {@link BlockBag} to use.
     *
     * @param blockBag the block bag to set, or null to use none
     */
    public void setBlockBag(BlockBag blockBag) {
        this.blockBag = blockBag;
    }

    @Override
    public String toString() {
        return super.toString() + ":" + extent;
    }

    /**
     * Gets the list of missing blocks and clears the list for the next
     * operation.
     *
     * @return a map of missing blocks
     */
    public Map<BlockType, Integer> popMissingBlocks() {
        BlockBag bag = getBlockBag();
        if (bag != null) {
            bag.flushChanges();

            Map<BlockType, Integer> missingBlocks;
            ChangeSet changeSet = getChangeSet();


            if (changeSet instanceof BlockBagChangeSet) {
                missingBlocks = ((BlockBagChangeSet) changeSet).popMissing();
            } else {
                ExtentTraverser<BlockBagExtent> find = new ExtentTraverser<>(extent).find(BlockBagExtent.class);
                if (find != null && find.get() != null) {
                    missingBlocks = find.get().popMissing();
                } else {
                    missingBlocks = null;
                }
            }

            if (missingBlocks != null && !missingBlocks.isEmpty()) {
                StringBuilder str = new StringBuilder();
                int size = missingBlocks.size();
                int i = 0;

                for (Map.Entry<BlockType, Integer> entry : missingBlocks.entrySet()) {
                    BlockType type = entry.getKey();
                    int amount = entry.getValue();
                    str.append((type.getName())).append((amount != 1 ? "x" + amount : ""));
                    ++i;
                    if (i != size) {
                        str.append(", ");
                    }
                }

                BBC.WORLDEDIT_SOME_FAILS_BLOCKBAG.send(player, str.toString());
            }
        }
        return Collections.emptyMap();
    }

    /**
     * Returns chunk batching status.
     *
     * @return whether chunk batching is enabled
     */
    public boolean isBatchingChunks() {
        return false;
    }

    /**
     * Enable or disable chunk batching. Disabling will
     * {@linkplain #flushSession() flush the session}.
     *
     * @param batchingChunks {@code true} to enable, {@code false} to disable
     */
    public void setBatchingChunks(boolean batchingChunks) {
    }

    /**
     * Disable all buffering extents.
     *
     * @see #setReorderMode(ReorderMode)
     * @see #setBatchingChunks(boolean)
     */
    public void disableBuffering() {
    }

    /**
     * Get the number of blocks changed, including repeated block changes.
     *
     * <p>This number may not be accurate.</p>
     *
     * @return the number of block changes
     */
    public int getBlockChangeCount() {
        return this.changes;
    }

    @Override
    public BiomeType getBiome(BlockVector2 position) {
        return this.extent.getBiome(position);
    }

    @Override
    public boolean setBiome(BlockVector2 position, BiomeType biome) {
        this.changes++;
        return this.extent.setBiome(position, biome);
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        this.changes++;
        return this.extent.setBiome(x, y, z, biome);
    }

    @Override
    public int getLight(int x, int y, int z) {
        return queue.getLight(x, y, z);
    }

    @Override
    public int getBlockLight(int x, int y, int z) {
        return queue.getEmmittedLight(x, y, z);
    }

    @Override
    public int getSkyLight(int x, int y, int z) {
        return queue.getSkyLight(x, y, z);
    }

    @Override
    public int getBrightness(int x, int y, int z) {
        return queue.getBrightness(x, y, z);
    }

    @Override
    public int getOpacity(int x, int y, int z) {
        return queue.getOpacity(x, y, z);
    }

    @Override
    public BlockState getLazyBlock(final BlockVector3 position) {
        return getLazyBlock(position.getBlockX(), position.getBlockY(), position.getBlockZ());
    }

    public BlockState getLazyBlock(int x, int y, int z) {
        return extent.getLazyBlock(x, y, z);
    }

    public BlockState getBlock(int x, int y, int z) {
        return getLazyBlock(x, y, z);
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        return extent.getBlock(position);
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        return extent.getFullBlock(position);
    }

    /**
     * Get a block type at the given position.
     *
     * @param position the position
     * @return the block type
     * @deprecated Use {@link #getLazyBlock(BlockVector3)} or {@link #getBlock(BlockVector3)}
     */
    @Deprecated
    public BlockType getBlockType(final BlockVector3 position) {
        return getBlockType(position.getBlockX(), position.getBlockY(), position.getBlockZ());
    }

    /**
     * Returns the highest solid 'terrain' block.
     *
     * @param x the X coordinate
     * @param z the Z coordinate
     * @param minY minimal height
     * @param maxY maximal height
     * @return height of highest block found or 'minY'
     */
    public int getHighestTerrainBlock(int x, int z, int minY, int maxY) {
        for (int y = maxY; y >= minY; --y) {
            if (getBlock(x, y, z).getBlockType().getMaterial().isMovementBlocker()) {
                return y;
            }
        }
        return minY;
    }

    /**
     * Returns the highest solid 'terrain' block.
     *
     * @param x the X coordinate
     * @param z the Z coordinate
     * @param minY minimal height
     * @param maxY maximal height
     * @param filter a mask of blocks to consider, or null to consider any solid (movement-blocking) block
     * @return height of highest block found or 'minY'
     */
    public int getHighestTerrainBlock(int x, int z, int minY, int maxY, Mask filter) {
        for (int y = maxY; y >= minY; --y) {
            if (filter.test(mutablebv.setComponents(x, y, z))) {
                return y;
            }
        }

        return minY;
    }

    public BlockType getBlockType(int x, int y, int z) {
        if (!limit.MAX_CHECKS()) {
            throw new FaweException(BBC.WORLDEDIT_CANCEL_REASON_MAX_CHECKS);
        }
        int combinedId4Data = queue.getCombinedId4DataDebug(x, y, z, BlockTypes.AIR.getInternalId(), this);
        return BlockTypes.getFromStateId(combinedId4Data);
    }

    /**
     * Set a block, bypassing both history and block re-ordering.
     *
     * @param position the position to set the block at
     * @param block the block
     * @param stage the level
     * @return whether the block changed
     * @throws WorldEditException thrown on a set error
     */
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 position, B block, Stage stage) throws WorldEditException {
        this.changes++;
        switch (stage) {
            case BEFORE_HISTORY:
                return this.extent.setBlock(position, block);
            case BEFORE_CHANGE:
                return this.bypassHistory.setBlock(position, block);
            case BEFORE_REORDER:
                return this.bypassAll.setBlock(position, block);
        }

        throw new RuntimeException("New enum entry added that is unhandled here");
    }

    /**
     * Set a block, bypassing both history and block re-ordering.
     *
     * @param position the position to set the block at
     * @param block the block
     * @return whether the block changed
     */
    public <B extends BlockStateHolder<B>> boolean rawSetBlock(BlockVector3 position, B block) {
        this.changes++;
        try {
            return this.bypassAll.setBlock(position, block);
        } catch (WorldEditException e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    /**
     * Set a block, bypassing history but still utilizing block re-ordering.
     *
     * @param position the position to set the block at
     * @param block the block
     * @return whether the block changed
     */
    public <B extends BlockStateHolder<B>> boolean smartSetBlock(BlockVector3 position, B block) {
        this.changes++;
        try {
            return setBlock(position, block, Stage.BEFORE_REORDER);
        } catch (WorldEditException e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 position, B block) throws MaxChangedBlocksException {
        this.changes++;
        try {
            return this.extent.setBlock(position, block);
        } catch (MaxChangedBlocksException e) {
            throw e;
        } catch (WorldEditException e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    public <B extends BlockStateHolder<B>> boolean setBlock(int x, int y, int z, B block) {
        this.changes++;
        try {
            return this.extent.setBlock(x, y, z, block);
        } catch (WorldEditException e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    public boolean setBlock(int x, int y, int z, Pattern pattern) {
        this.changes++;
        try {
            BlockVector3 bv = mutablebv.setComponents(x, y, z);
            return pattern.apply(extent, bv, bv);
        } catch (WorldEditException e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    public boolean setBlock(final BlockVector3 position, final Pattern pattern) {
        this.changes++;
        try {
            return pattern.apply(this.extent, position, position);
        } catch (WorldEditException e) {
            throw new RuntimeException(e);
        }
    }

    public int setBlocks(final Set<BlockVector3> vset, final Pattern pattern) {
        RegionVisitor visitor = new RegionVisitor(vset, new BlockReplace(extent, pattern), this);
        Operations.completeBlindly(visitor);
        changes += visitor.getAffected();
        return changes;
    }

    /**
     * Set a block (only if a previous block was not there) if {@link Math#random()}
     * returns a number less than the given probability.
     *
     * @param position the position
     * @param block the block
     * @param probability a probability between 0 and 1, inclusive
     * @return Whether the block changed -- not entirely dependable
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public boolean setChanceBlockIfAir(final BlockVector3 position, final BaseBlock block, final double probability) throws MaxChangedBlocksException {
        return (ThreadLocalRandom.current().nextInt(65536) <= (probability * 65536)) && this.setBlockIfAir(position, block);
    }

    /**
     * Set a block only if there's no block already there.
     *
     * @param position the position
     * @param block    the block to set
     * @return if block was changed
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     * @deprecated Use your own method
     */
    @Deprecated
    public boolean setBlockIfAir(final BlockVector3 position, final BlockStateHolder block) throws MaxChangedBlocksException {
        return this.getBlock(position).getBlockType().getMaterial().isAir() && this.setBlock(position, block);
    }

    @Override
    @Nullable
    public Entity createEntity(Location location, final BaseEntity entity) {
        return this.extent.createEntity(location, entity);
    }

    /**
     * Insert a contrived block change into the history.
     *
     * @param position the position
     * @param existing the previous block at that position
     * @param block    the new block
     * @deprecated Get the change set with {@link #getChangeSet()} and add the change with that
     */
    @Deprecated
    public void rememberChange(final BlockVector3 position, final BaseBlock existing, final BaseBlock block) {
        ChangeSet changeSet = getChangeSet();
        if (changeSet != null) {
            changeSet.add(new BlockChange(position, existing, block));
        }
    }

    /**
     * Restores all blocks to their initial state.
     *
     * @param editSession a new {@link EditSession} to perform the undo in
     */
    public void undo(EditSession editSession) {
        UndoContext context = new UndoContext();
        context.setExtent(editSession.bypassAll);
        ChangeSet changeSet = getChangeSet();
        editSession.getQueue().setChangeTask(null);
        Operations.completeBlindly(ChangeSetExecutor.create(changeSet, context, ChangeSetExecutor.Type.UNDO, editSession.getBlockBag(), editSession.getLimit().INVENTORY_MODE));
        flushQueue();
        editSession.changes = 1;
    }

    public void setBlocks(ChangeSet changeSet, ChangeSetExecutor.Type type) {
        final UndoContext context = new UndoContext();
        Extent bypass = (history == null) ? bypassAll : history;
        context.setExtent(bypass);
        Operations.completeBlindly(ChangeSetExecutor.create(changeSet, context, type, getBlockBag(), getLimit().INVENTORY_MODE));
        flushQueue();
        changes = 1;
    }

    /**
     * Sets to new state.
     *
     * @param editSession a new {@link EditSession} to perform the redo in
     */
    public void redo(EditSession editSession) {
        UndoContext context = new UndoContext();
        context.setExtent(editSession.bypassAll);
        ChangeSet changeSet = getChangeSet();
        editSession.getQueue().setChangeTask(null);
        Operations.completeBlindly(ChangeSetExecutor.create(changeSet, context, ChangeSetExecutor.Type.REDO, editSession.getBlockBag(), editSession.getLimit().INVENTORY_MODE));
        flushQueue();
        editSession.changes = 1;
    }

    /**
     * Get the number of changed blocks.
     *
     * @return the number of changes
     */
    public int size() {
        return getBlockChangeCount();
    }

    public void setSize(int size) {
        this.changes = size;
    }

    @Override
    public BlockVector3 getMinimumPoint() {
        if (extent != null) {
            return this.extent.getMinimumPoint();
        } else {
            return BlockVector3.at(-30000000, 0, -30000000);
        }
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        if (extent != null) {
            return this.extent.getMaximumPoint();
        } else {
            return BlockVector3.at(30000000, 255, 30000000);
        }
    }

    @Override
    public List<? extends Entity> getEntities(Region region) {
        return this.extent.getEntities(region);
    }

    @Override
    public List<? extends Entity> getEntities() {
        return this.extent.getEntities();
    }

    /**
     * Closing an EditSession {@linkplain #flushSession() flushes its buffers}.
     */
    @Override
    public void close() {
        flushSession();
    }

    /**
     * Communicate to the EditSession that all block changes are complete,
     * and that it should apply them to the world.
     */
    public void flushSession() {
        flushQueue();
    }

    @Override
    public @Nullable Operation commit() {
        return extent.commit();
    }

    /**
     * Finish off the queue.
     */
    public void flushQueue() {
        Operations.completeBlindly(commit());
        // Check fails
        FaweLimit used = getLimitUsed();
        if (used.MAX_FAILS > 0) {
            if (used.MAX_CHANGES > 0 || used.MAX_ENTITIES > 0) {
                BBC.WORLDEDIT_SOME_FAILS.send(player, used.MAX_FAILS);
            } else if (new ExtentTraverser<>(this).findAndGet(FaweRegionExtent.class) != null){
                BBC.WORLDEDIT_CANCEL_REASON_OUTSIDE_REGION.send(player);
            } else {
                BBC.WORLDEDIT_CANCEL_REASON_OUTSIDE_LEVEL.send(player);
            }
        }
        // Reset limit
        limit.set(originalLimit);
        // Enqueue it
        if (queue == null || queue.isEmpty()) {
            queue.dequeue();
            return;
        }
        if (Fawe.isMainThread()) {
            SetQueue.IMP.flush(queue);
        } else {
            queue.flush();
        }
        if (getChangeSet() != null) {
            if (Settings.IMP.HISTORY.COMBINE_STAGES) {
                ((FaweChangeSet) getChangeSet()).closeAsync();
            } else {
                ((FaweChangeSet) getChangeSet()).close();
            }
        }
    }

    /**
     * Count the number of blocks of a given list of types in a region.
     *
     * @param region    the region
     * @param searchIDs a list of IDs to search
     * @return the number of found blocks
     */
    public int countBlock(final Region region, final Set<BlockType> searchIDs) {
        if (searchIDs.isEmpty()) {
            return 0;
        }
        if (searchIDs.size() == 1) {
            final BlockType id = searchIDs.iterator().next();
            RegionVisitor visitor = new RegionVisitor(region, position -> getBlockType(position) == id, this);
            Operations.completeBlindly(visitor);
            return visitor.getAffected();
        }
        final boolean[] ids = new boolean[BlockTypes.size()];
        for (final BlockType id : searchIDs) {
            ids[id.getInternalId()] = true;
        }
        return this.countBlock(region, ids);
    }

    public int countBlock(final Region region, final boolean[] ids) {
        RegionVisitor visitor = new RegionVisitor(region, position -> ids[getBlockType(position).getInternalId()], this);
        Operations.completeBlindly(visitor);
        return visitor.getAffected();
    }

    public int countBlock(final Region region, final Mask mask) {
        RegionVisitor visitor = new RegionVisitor(region, mask::test, this);
        Operations.completeBlindly(visitor);
        return visitor.getAffected();
    }

    /**
     * Count the number of blocks of a list of types in a region.
     *
     * @param region the region
     * @param searchBlocks the list of blocks to search
     * @return the number of blocks that matched the pattern
     */
    public int countBlocks(Region region, Set<BlockStateHolder> searchBlocks) {
        Mask mask = new BlockMaskBuilder().addBlocks(searchBlocks).build(extent);
        RegionVisitor visitor = new RegionVisitor(region, mask::test, this);
        Operations.completeBlindly(visitor);
        return visitor.getAffected();
    }

    public int fall(final Region region, boolean fullHeight, final BlockStateHolder replace) {
        FlatRegion flat = asFlatRegion(region);
        final int startPerformY = region.getMinimumPoint().getBlockY();
        final int startCheckY = fullHeight ? 0 : startPerformY;
        final int endY = region.getMaximumPoint().getBlockY();
        RegionVisitor visitor = new RegionVisitor(flat, pos -> {
            int x = pos.getBlockX();
            int z = pos.getBlockZ();
            int freeSpot = startCheckY;
            for (int y = startCheckY; y <= endY; y++) {
                if (y < startPerformY) {
                    if (!getBlockType(x, y, z).getMaterial().isAir()) {
                        freeSpot = y + 1;
                    }
                    continue;
                }
                BlockType block = getBlockType(x, y, z);
                if (!block.getMaterial().isAir()) {
                    if (freeSpot != y) {
                        setBlock(x, freeSpot, z, block);
                        setBlock(x, y, z, replace);
                    }
                    freeSpot++;
                }
            }
            return true;
        }, this);
        Operations.completeBlindly(visitor);
        return this.changes;
    }

    /**
     * Fills an area recursively in the X/Z directions.
     *
     * @param origin    the location to start from
     * @param pattern     the block to fill with
     * @param radius    the radius of the spherical area to fill
     * @param depth     the maximum depth, starting from the origin
     * @param direction the direction to fill
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int fillDirection(final BlockVector3 origin, final Pattern pattern, final double radius, final int depth, BlockVector3 direction) {
        checkNotNull(origin);
        checkNotNull(pattern);
        checkArgument(radius >= 0, "radius >= 0");
        checkArgument(depth >= 1, "depth >= 1");
        if (direction.equals(BlockVector3.at(0, -1, 0))) {
            return fillXZ(origin, pattern, radius, depth, false);
        }
        final MaskIntersection mask = new MaskIntersection(new RegionMask(new EllipsoidRegion(null, origin, Vector3.at(radius, radius, radius))), Masks.negate(new ExistingBlockMask(EditSession.this)));

        // Want to replace blocks
        final BlockReplace replace = new BlockReplace(EditSession.this, pattern);

        // Pick how we're going to visit blocks
        RecursiveVisitor visitor = new DirectionalVisitor(mask, replace, origin, direction, (int) (radius * 2 + 1), this);

        // Start at the origin
        visitor.visit(origin);

        // Execute
        Operations.completeBlindly(visitor);
        return this.changes = visitor.getAffected();
    }

    /**
     * Fills an area recursively in the X/Z directions.
     *
     * @param origin the location to start from
     * @param block the block to fill with
     * @param radius the radius of the spherical area to fill
     * @param depth the maximum depth, starting from the origin
     * @param recursive whether a breadth-first search should be performed
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public <B extends BlockStateHolder<B>> int fillXZ(BlockVector3 origin, B block, double radius, int depth, boolean recursive) throws MaxChangedBlocksException {
        return fillXZ(origin, (block), radius, depth, recursive);
    }

    /**
     * Fills an area recursively in the X/Z directions.
     *
     * @param origin the origin to start the fill from
     * @param pattern the pattern to fill with
     * @param radius the radius of the spherical area to fill, with 0 as the smallest radius
     * @param depth the maximum depth, starting from the origin, with 1 as the smallest depth
     * @param recursive whether a breadth-first search should be performed
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int fillXZ(BlockVector3 origin, Pattern pattern, double radius, int depth, boolean recursive) throws MaxChangedBlocksException {
        checkNotNull(origin);
        checkNotNull(pattern);
        checkArgument(radius >= 0, "radius >= 0");
        checkArgument(depth >= 1, "depth >= 1");

        MaskIntersection mask = new MaskIntersection(
                new RegionMask(new EllipsoidRegion(null, origin, Vector3.at(radius, radius, radius))),
                new BoundedHeightMask(
                        Math.max(origin.getBlockY() - depth + 1, getMinimumPoint().getBlockY()),
                        Math.min(getMaximumPoint().getBlockY(), origin.getBlockY())),
                Masks.negate(new ExistingBlockMask(this)));

        // Want to replace blocks
        BlockReplace replace = new BlockReplace(this, pattern);

        // Pick how we're going to visit blocks
        RecursiveVisitor visitor;
        if (recursive) {
            visitor = new RecursiveVisitor(mask, replace, (int) (radius * 2 + 1), this);
        } else {
            visitor = new DownwardVisitor(mask, replace, origin.getBlockY(), (int) (radius * 2 + 1), this);
        }

        // Start at the origin
        visitor.visit(origin);

        // Execute
        Operations.completeBlindly(visitor);

        return this.changes = visitor.getAffected();
    }

    /**
     * Remove a cuboid above the given position with a given apothem and a given height.
     *
     * @param position base position
     * @param apothem an apothem of the cuboid (on the XZ plane), where the minimum is 1
     * @param height the height of the cuboid, where the minimum is 1
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int removeAbove(BlockVector3 position, int apothem, int height) throws MaxChangedBlocksException {
        checkNotNull(position);
        checkArgument(apothem >= 1, "apothem >= 1");
        checkArgument(height >= 1, "height >= 1");

        Region region = new CuboidRegion(
                getWorld(), // Causes clamping of Y range
                position.add(-apothem + 1, 0, -apothem + 1),
                position.add(apothem - 1, height - 1, apothem - 1));
        Pattern pattern = (BlockTypes.AIR.getDefaultState());
        return setBlocks(region, pattern);
    }

    /**
     * Remove a cuboid below the given position with a given apothem and a given height.
     *
     * @param position base position
     * @param apothem an apothem of the cuboid (on the XZ plane), where the minimum is 1
     * @param height the height of the cuboid, where the minimum is 1
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int removeBelow(BlockVector3 position, int apothem, int height) throws MaxChangedBlocksException {
        checkNotNull(position);
        checkArgument(apothem >= 1, "apothem >= 1");
        checkArgument(height >= 1, "height >= 1");

        Region region = new CuboidRegion(
                getWorld(), // Causes clamping of Y range
                position.add(-apothem + 1, 0, -apothem + 1),
                position.add(apothem - 1, -height + 1, apothem - 1));
        Pattern pattern = (BlockTypes.AIR.getDefaultState());
        return setBlocks(region, pattern);
    }

    /**
     * Remove blocks of a certain type nearby a given position.
     *
     * @param position center position of cuboid
     * @param mask the mask to match
     * @param apothem an apothem of the cuboid, where the minimum is 1
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int removeNear(final BlockVector3 position, Mask mask, final int apothem) {
        checkNotNull(position);
        checkArgument(apothem >= 1, "apothem >= 1");

        final BlockVector3 adjustment = BlockVector3.at(1, 1, 1).multiply(apothem - 1);
        final Region region = new CuboidRegion(this.getWorld(), // Causes clamping of Y range
                position.add(adjustment.multiply(-1)), position.add(adjustment));
        final Pattern pattern = BlockTypes.AIR.getDefaultState();
        return this.replaceBlocks(region, mask, pattern);
    }

    public boolean canBypassAll(Region region, boolean get, boolean set) {
        if (wrapped) return false;
        if (history != null) return false;
        FaweRegionExtent regionExtent = getRegionExtent();
        if (!(region instanceof CuboidRegion)) return false;
        if (regionExtent != null) {
            if (!(region instanceof CuboidRegion)) return false;
            BlockVector3 pos1 = region.getMinimumPoint();
            BlockVector3 pos2 = region.getMaximumPoint();
            boolean contains = false;
            for (Region current : regionExtent.getRegions()) {
                if (current.contains((int) pos1.getX(), pos1.getBlockY(), pos1.getBlockZ()) && current.contains(pos2.getBlockX(), pos2.getBlockY(), pos2.getBlockZ())) {
                    contains = true;
                    break;
                }
            }
            if (!contains) return false;
        }
        long area = region.getArea();
        FaweLimit left = getLimitLeft();
        if (!left.isUnlimited() && (((get || getChangeTask() != null) && left.MAX_CHECKS <= area) || (set && left.MAX_CHANGES <= area)))
            return false;
        if (getChangeTask() != getChangeSet()) return false;
        if (!Masks.isNull(getMask()) || !Masks.isNull(getSourceMask())) return false;
        if (getBlockBag() != null) return false;
        return true;
    }

    public boolean hasExtraExtents() {
        return wrapped || getMask() != null || getSourceMask() != null || history != null;
    }

    /**
     * Remove blocks of a certain type nearby a given position.
     *
     * @param position center position of cuboid
     * @param blockType the block type to match
     * @param apothem an apothem of the cuboid, where the minimum is 1
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int removeNear(BlockVector3 position, BlockType blockType, int apothem) throws MaxChangedBlocksException {
        checkNotNull(position);
        checkArgument(apothem >= 1, "apothem >= 1");

        Mask mask = new BlockTypeMask(this, blockType);
        BlockVector3 adjustment = BlockVector3.ONE.multiply(apothem - 1);
        Region region = new CuboidRegion(
                getWorld(), // Causes clamping of Y range
                position.add(adjustment.multiply(-1)),
                position.add(adjustment));
        Pattern pattern = (BlockTypes.AIR.getDefaultState());
        return replaceBlocks(region, mask, pattern);
    }

    /**
     * Sets all the blocks inside a region to a given block type.
     *
     * @param region the region
     * @param block the block
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public <B extends BlockStateHolder<B>> int setBlocks(Region region, B block) throws MaxChangedBlocksException {
        checkNotNull(region);
        checkNotNull(block);
        boolean hasNbt = block instanceof BaseBlock && ((BaseBlock)block).hasNbtData();

        if (canBypassAll(region, false, true) && !hasNbt) {
            return changes = queue.setBlocks((CuboidRegion) region, block.getInternalId());
        }
        try {
            if (hasExtraExtents()) {
                RegionVisitor visitor = new RegionVisitor(region, new BlockReplace(extent, (block)), this);
                Operations.completeBlindly(visitor);
                this.changes += visitor.getAffected();
            } else {
                for (BlockVector3 blockVector3 : region) {
                    if (this.extent.setBlock(blockVector3, block)) {
                        changes++;
                    }
                }
            }
        } catch (final WorldEditException e) {
            throw new RuntimeException("Unexpected exception", e);
        }
        return changes;
    }

    /**
     * Sets all the blocks inside a region to a given pattern.
     *
     * @param region the region
     * @param pattern the pattern that provides the replacement block
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int setBlocks(Region region, Pattern pattern) throws MaxChangedBlocksException {
        checkNotNull(region);
        checkNotNull(pattern);
        if (pattern instanceof BlockPattern) {
            return setBlocks(region, ((BlockPattern) pattern).getBlock());
        }
        if (pattern instanceof BlockStateHolder) {
            return setBlocks(region, (BlockStateHolder) pattern);
        }
        BlockReplace replace = new BlockReplace(this, pattern);
        RegionVisitor visitor = new RegionVisitor(region, replace, queue instanceof MappedFaweQueue ? (MappedFaweQueue) queue : null);
        Operations.completeBlindly(visitor);
        return this.changes = visitor.getAffected();
    }

    /**
     * Replaces all the blocks matching a given filter, within a given region, to a block
     * returned by a given pattern.
     *
     * @param region the region to replace the blocks within
     * @param filter a list of block types to match, or null to use {@link ExistingBlockMask}
     * @param replacement the replacement block
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public <B extends BlockStateHolder<B>> int replaceBlocks(Region region, Set<BaseBlock> filter, B replacement) throws MaxChangedBlocksException {
        return replaceBlocks(region, filter, (replacement));
    }

    /**
     * Replaces all the blocks matching a given filter, within a given region, to a block
     * returned by a given pattern.
     *
     * @param region the region to replace the blocks within
     * @param filter a list of block types to match, or null to use {@link ExistingBlockMask}
     * @param pattern the pattern that provides the new blocks
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int replaceBlocks(Region region, Set<BaseBlock> filter, Pattern pattern) throws MaxChangedBlocksException {
        Mask mask = filter == null ? new ExistingBlockMask(this) : new BlockMaskBuilder().addBlocks(filter).build(this);
        return replaceBlocks(region, mask, pattern);
    }

    /**
     * Replaces all the blocks matching a given mask, within a given region, to a block
     * returned by a given pattern.
     *
     * @param region the region to replace the blocks within
     * @param mask the mask that blocks must match
     * @param pattern the pattern that provides the new blocks
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int replaceBlocks(Region region, Mask mask, Pattern pattern) throws MaxChangedBlocksException {
        checkNotNull(region);
        checkNotNull(mask);
        checkNotNull(pattern);

        BlockReplace replace = new BlockReplace(this, pattern);
        RegionMaskingFilter filter = new RegionMaskingFilter(mask, replace);
        RegionVisitor visitor = new RegionVisitor(region, filter, queue instanceof MappedFaweQueue ? (MappedFaweQueue) queue : null);
        Operations.completeBlindly(visitor);
        return this.changes = visitor.getAffected();
    }

    /**
     * Sets the blocks at the center of the given region to the given pattern.
     * If the center sits between two blocks on a certain axis, then two blocks
     * will be placed to mark the center.
     *
     * @param region the region to find the center of
     * @param pattern the replacement pattern
     * @return the number of blocks placed
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int center(Region region, Pattern pattern) throws MaxChangedBlocksException {
        checkNotNull(region);
        checkNotNull(pattern);

        Vector3 center = region.getCenter();
        Region centerRegion = new CuboidRegion(
                getWorld(), // Causes clamping of Y range
                BlockVector3.at(((int) center.getX()), ((int) center.getY()), ((int) center.getZ())),
                BlockVector3.at(MathUtils.roundHalfUp(center.getX()),
                            center.getY(), MathUtils.roundHalfUp(center.getZ())));
        return setBlocks(centerRegion, pattern);
    }

    /**
     * Make the faces of the given region as if it was a {@link CuboidRegion}.
     *
     * @param region the region
     * @param block the block to place
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public <B extends BlockStateHolder<B>> int makeCuboidFaces(Region region, B block) throws MaxChangedBlocksException {
        return makeCuboidFaces(region, (block));
    }

    /**
     * Make the faces of the given region as if it was a {@link CuboidRegion}.
     *
     * @param region the region
     * @param pattern the pattern to place
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int makeCuboidFaces(Region region, Pattern pattern) throws MaxChangedBlocksException {
        checkNotNull(region);
        checkNotNull(pattern);

        CuboidRegion cuboid = CuboidRegion.makeCuboid(region);
        Region faces = cuboid.getFaces();
        return setBlocks(faces, pattern);
    }

    /**
     * Make the faces of the given region. The method by which the faces are found
     * may be inefficient, because there may not be an efficient implementation supported
     * for that specific shape.
     *
     * @param region the region
     * @param pattern the pattern to place
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int makeFaces(final Region region, Pattern pattern) throws MaxChangedBlocksException {
        checkNotNull(region);
        checkNotNull(pattern);

        if (region instanceof CuboidRegion) {
            return makeCuboidFaces(region, pattern);
        } else {
            return new RegionShape(region).generate(this, pattern, true);
        }
    }


    /**
     * Make the walls (all faces but those parallel to the X-Z plane) of the given region
     * as if it was a {@link CuboidRegion}.
     *
     * @param region the region
     * @param block the block to place
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public <B extends BlockStateHolder<B>> int makeCuboidWalls(Region region, B block) throws MaxChangedBlocksException {
        return makeCuboidWalls(region, (block));
    }

    /**
     * Make the walls (all faces but those parallel to the X-Z plane) of the given region
     * as if it was a {@link CuboidRegion}.
     *
     * @param region the region
     * @param pattern the pattern to place
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int makeCuboidWalls(Region region, Pattern pattern) throws MaxChangedBlocksException {
        checkNotNull(region);
        checkNotNull(pattern);

        CuboidRegion cuboid = CuboidRegion.makeCuboid(region);
        Region faces = cuboid.getWalls();
        return setBlocks(faces, pattern);
    }

    /**
     * Make the walls of the given region. The method by which the walls are found
     * may be inefficient, because there may not be an efficient implementation supported
     * for that specific shape.
     *
     * @param region the region
     * @param pattern the pattern to place
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int makeWalls(final Region region, Pattern pattern) throws MaxChangedBlocksException {
        checkNotNull(region);
        checkNotNull(pattern);

        if (region instanceof CuboidRegion) {
            return makeCuboidWalls(region, pattern);
        } else {
            for (BlockVector3 position : region) {
                int x = position.getBlockX();
                int y = position.getBlockY();
                int z = position.getBlockZ();
                if (!region.contains(x, z + 1) || !region.contains(x, z - 1) || !region.contains(x + 1, z) || !region.contains(x - 1, z)) {
                    setBlock(position, pattern);
                }
            }
        }
        return changes;
    }

    /**
     * Places a layer of blocks on top of ground blocks in the given region
     * (as if it were a cuboid).
     *
     * @param region the region
     * @param block the placed block
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public <B extends BlockStateHolder<B>> int overlayCuboidBlocks(Region region, B block) throws MaxChangedBlocksException {
        checkNotNull(block);

        return overlayCuboidBlocks(region, (block));
    }

    /**
     * Places a layer of blocks on top of ground blocks in the given region
     * (as if it were a cuboid).
     *
     * @param region the region
     * @param pattern the placed block pattern
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int overlayCuboidBlocks(Region region, Pattern pattern) throws MaxChangedBlocksException {
        checkNotNull(region);
        checkNotNull(pattern);

        BlockReplace replace = new BlockReplace(this, pattern);
        RegionOffset offset = new RegionOffset(BlockVector3.UNIT_Y, replace);
        int minY = region.getMinimumPoint().getBlockY();
        int maxY = Math.min(getMaximumPoint().getBlockY(), region.getMaximumPoint().getBlockY() + 1);
        SurfaceRegionFunction surface = new SurfaceRegionFunction(this, offset, minY, maxY);
        FlatRegionVisitor visitor = new FlatRegionVisitor(asFlatRegion(region), surface, this);
        Operations.completeBlindly(visitor);
        return this.changes = visitor.getAffected();
    }

    /**
     * Turns the first 3 layers into dirt/grass and the bottom layers
     * into rock, like a natural Minecraft mountain.
     *
     * @param region the region to affect
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int naturalizeCuboidBlocks(Region region) throws MaxChangedBlocksException {
        checkNotNull(region);

        Naturalizer naturalizer = new Naturalizer(this);
        FlatRegion flatRegion = Regions.asFlatRegion(region);
        LayerVisitor visitor = new LayerVisitor(flatRegion, minimumBlockY(region), maximumBlockY(region), naturalizer);
        Operations.completeBlindly(visitor);
        return this.changes = naturalizer.getAffected();
    }

    /**
     * Stack a cuboid region.
     *
     * @param region the region to stack
     * @param dir the direction to stack
     * @param count the number of times to stack
     * @param copyAir true to also copy air blocks
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int stackCuboidRegion(Region region, BlockVector3 dir, int count, boolean copyAir, boolean copyEntities, boolean copyBiomes) throws MaxChangedBlocksException {
        checkNotNull(region);
        checkNotNull(dir);
        checkArgument(count >= 1, "count >= 1 required");

        BlockVector3 size = region.getMaximumPoint().subtract(region.getMinimumPoint()).add(1, 1, 1);
        BlockVector3 to = region.getMinimumPoint();
        ForwardExtentCopy copy = new ForwardExtentCopy(this, region, this, to);
        copy.setCopyingEntities(copyEntities);
        copy.setCopyingBiomes(copyBiomes);
        copy.setRepetitions(count);
        copy.setTransform(new AffineTransform().translate(dir.multiply(size)));
        Mask sourceMask = getSourceMask();
        if (sourceMask != null) {
            new MaskTraverser(sourceMask).reset(EditSession.this);
            copy.setSourceMask(sourceMask);
            setSourceMask(null);
        }
        if (!copyAir) {
            copy.setSourceMask(new ExistingBlockMask(this));
        }
        Operations.completeBlindly(copy);
        return this.changes = copy.getAffected();
    }

    /**
     * Move the blocks in a region a certain direction.
     *
     * @param region the region to move
     * @param dir the direction
     * @param distance the distance to move
     * @param copyAir true to copy air blocks
     * @param replacement the replacement pattern to fill in after moving, or null to use air
     * @return number of blocks moved
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int moveRegion(Region region, BlockVector3 dir, int distance, boolean copyAir, boolean copyEntities, boolean copyBiomes, Pattern replacement) throws MaxChangedBlocksException {
        checkNotNull(region);
        checkNotNull(dir);
        checkArgument(distance >= 1, "distance >= 1 required");
        BlockVector3 to = region.getMinimumPoint().add(dir.multiply(distance));

        final BlockVector3 displace = dir.multiply(distance);
        final BlockVector3 size = region.getMaximumPoint().subtract(region.getMinimumPoint()).add(1, 1, 1);

        BlockVector3 disAbs = displace.abs();

        if (disAbs.getBlockX() < size.getBlockX() && disAbs.getBlockY() < size.getBlockY() && disAbs.getBlockZ() < size.getBlockZ()) {
            // Buffer if overlapping
            queue.dequeue();
        }

        ForwardExtentCopy copy = new ForwardExtentCopy(this, region, this, to);

        if (replacement == null) replacement = BlockTypes.AIR.getDefaultState();
        final BlockReplace remove = replacement instanceof ExistingPattern ? null : new BlockReplace(this, replacement);
        copy.setCopyingBiomes(copyBiomes);
        copy.setCopyingEntities(copyEntities);
        copy.setSourceFunction(remove); // Remove
        copy.setRemovingEntities(true);
        copy.setRepetitions(1);
        Mask sourceMask = getSourceMask();
        if (sourceMask != null) {
            new MaskTraverser(sourceMask).reset(this);
            copy.setSourceMask(sourceMask);
            setSourceMask(null);
        }
        if (!copyAir) {
            copy.setSourceMask(new ExistingBlockMask(this));
        }
        Operations.completeBlindly(copy);
        return this.changes = copy.getAffected();
    }

    /**
     * Move the blocks in a region a certain direction.
     *
     * @param region the region to move
     * @param dir the direction
     * @param distance the distance to move
     * @param copyAir true to copy air blocks
     * @param replacement the replacement pattern to fill in after moving, or null to use air
     * @return number of blocks moved
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int moveCuboidRegion(Region region, BlockVector3 dir, int distance, boolean copyAir, Pattern replacement) {
        return moveRegion(region, dir, distance, copyAir, true, false, replacement);
    }

    /**
     * Drain nearby pools of water or lava.
     *
     * @param origin the origin to drain from, which will search a 3x3 area
     * @param radius the radius of the removal, where a value should be 0 or greater
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int drainArea(BlockVector3 origin, double radius) throws MaxChangedBlocksException {
        return drainArea(origin, radius, false);
    }

    /**
     * Drain nearby pools of water or lava, optionally removed waterlogged states from blocks.
     *
     * @param origin the origin to drain from, which will search a 3x3 area
     * @param radius the radius of the removal, where a value should be 0 or greater
     * @param waterlogged true to make waterlogged blocks non-waterlogged as well
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int drainArea(BlockVector3 origin, double radius, boolean waterlogged) throws MaxChangedBlocksException {
        checkNotNull(origin);
        checkArgument(radius >= 0, "radius >= 0 required");

        Mask liquidMask;
        // Not thread safe, use hardcoded liquidmask
//        if (getWorld() != null) {
//            liquidMask = getWorld().createLiquidMask();
//        } else {
//        }
        liquidMask = new BlockTypeMask(this, BlockTypes.LAVA, BlockTypes.WATER);
        MaskIntersection mask = new MaskIntersection(
                new BoundedHeightMask(0, getWorld().getMaxY()),
                new RegionMask(new EllipsoidRegion(null, origin, Vector3.at(radius, radius, radius))),
                liquidMask);
        BlockReplace replace;
        if (waterlogged) {
            replace = new BlockReplace(this, new WaterloggedRemover(this));
        } else {
            replace = new BlockReplace(this, (BlockTypes.AIR.getDefaultState()));
        }
        RecursiveVisitor visitor = new RecursiveVisitor(mask, replace, (int) (radius * 2 + 1), this);

        // Around the origin in a 3x3 block
        for (BlockVector3 position : CuboidRegion.fromCenter(origin, 1)) {
            if (mask.test(position)) {
                visitor.visit(position);
            }
        }

        Operations.completeBlindly(visitor);

        return this.changes = visitor.getAffected();
    }

    /**
     * Fix liquids so that they turn into stationary blocks and extend outward.
     *
     * @param origin the original position
     * @param radius the radius to fix
     * @param fluid the type of the fluid
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int fixLiquid(BlockVector3 origin, double radius, BlockType fluid) throws MaxChangedBlocksException {
        checkNotNull(origin);
        checkArgument(radius >= 0, "radius >= 0 required");

        // Our origins can only be liquids
        Mask liquidMask = new BlockTypeMask(this, fluid);

        // But we will also visit air blocks
        MaskIntersection blockMask = new MaskUnion(liquidMask, Masks.negate(new ExistingBlockMask(this)));

        // There are boundaries that the routine needs to stay in
        MaskIntersection mask = new MaskIntersection(
                new BoundedHeightMask(0, Math.min(origin.getBlockY(), getWorld().getMaxY())),
                new RegionMask(new EllipsoidRegion(null, origin, Vector3.at(radius, radius, radius))),
                blockMask
        );

        BlockReplace replace = new BlockReplace(this, (fluid.getDefaultState()));
        NonRisingVisitor visitor = new NonRisingVisitor(mask, replace);

        // Around the origin in a 3x3 block
        for (BlockVector3 position : CuboidRegion.fromCenter(origin, 1)) {
            if (liquidMask.test(position)) {
                visitor.visit(position);
            }
        }

        Operations.completeLegacy(visitor);

        return visitor.getAffected();
    }

    /**
     * Makes a cylinder.
     *
     * @param pos Center of the cylinder
     * @param block The block pattern to use
     * @param radius The cylinder's radius
     * @param height The cylinder's up/down extent. If negative, extend downward.
     * @param filled If false, only a shell will be generated.
     * @return number of blocks changed
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int makeCylinder(BlockVector3 pos, Pattern block, double radius, int height, boolean filled) throws MaxChangedBlocksException {
        return makeCylinder(pos, block, radius, radius, height, filled);
    }

    /**
     * Makes a cylinder.
     *
     * @param pos Center of the cylinder
     * @param block The block pattern to use
     * @param radiusX The cylinder's largest north/south extent
     * @param radiusZ The cylinder's largest east/west extent
     * @param height The cylinder's up/down extent. If negative, extend downward.
     * @param filled If false, only a shell will be generated.
     * @return number of blocks changed
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int makeCylinder(BlockVector3 pos, Pattern block, double radiusX, double radiusZ, int height, boolean filled) throws MaxChangedBlocksException{
        return makeCylinder(pos, block, radiusX, radiusZ, height, 0, filled);
    }

    public int makeHollowCylinder(BlockVector3 pos, final Pattern block, double radiusX, double radiusZ, int height, double thickness) {
        return makeCylinder(pos, block, radiusX, radiusZ, height, thickness, false);
    }

    private int makeCylinder(BlockVector3 pos, final Pattern block, double radiusX, double radiusZ, int height, double thickness, final boolean filled) {
        int affected = 0;

        radiusX += 0.5;
        radiusZ += 0.5;

        MutableBlockVector3 posv = new MutableBlockVector3(pos);
        if (height == 0) {
            return 0;
        } else if (height < 0) {
            height = -height;
            posv.mutY(posv.getY() - height);
        }

        if (posv.getBlockY() < 0) {
            posv.mutY(0);
        } else if (((posv.getBlockY() + height) - 1) > maxY) {
            height = (maxY - posv.getBlockY()) + 1;
        }

        final double invRadiusX = 1 / (radiusX);
        final double invRadiusZ = 1 / (radiusZ);

        int px = posv.getBlockX();
        int py = posv.getBlockY();
        int pz = posv.getBlockZ();
        MutableBlockVector3 mutable = new MutableBlockVector3();

        final int ceilRadiusX = (int) Math.ceil(radiusX);
        final int ceilRadiusZ = (int) Math.ceil(radiusZ);
        double dx, dxz, dz;
        double nextXn = 0;

        if (thickness != 0) {
            double nextMinXn = 0;
            final double minInvRadiusX = 1 / (radiusX - thickness);
            final double minInvRadiusZ = 1 / (radiusZ - thickness);
            forX: for (int x = 0; x <= ceilRadiusX; ++x) {
                final double xn = nextXn;
                double dx2 = nextMinXn * nextMinXn;
                nextXn = (x + 1) * invRadiusX;
                nextMinXn = (x + 1) * minInvRadiusX;
                double nextZn = 0;
                double nextMinZn = 0;
                dx = xn * xn;
                forZ: for (int z = 0; z <= ceilRadiusZ; ++z) {
                    final double zn = nextZn;
                    double dz2 = nextMinZn * nextMinZn;
                    nextZn = (z + 1) * invRadiusZ;
                    nextMinZn = (z + 1) * minInvRadiusZ;
                    dz = zn * zn;
                    dxz = dx + dz;
                    if (dxz > 1) {
                        if (z == 0) {
                            break forX;
                        }
                        break forZ;
                    }

                    if ((dz2 + nextMinXn * nextMinXn <= 1) && (nextMinZn * nextMinZn + dx2 <= 1)) {
                        continue;
                    }

                    for (int y = 0; y < height; ++y) {
                        this.setBlock(mutable.setComponents(px + x, py + y, pz + z), block);
                        this.setBlock(mutable.setComponents(px - x, py + y, pz + z), block);
                        this.setBlock(mutable.setComponents(px + x, py + y, pz - z), block);
                        this.setBlock(mutable.setComponents(px - x, py + y, pz - z), block);
                    }
                }
            }
        } else {
            forX: for (int x = 0; x <= ceilRadiusX; ++x) {
                final double xn = nextXn;
                nextXn = (x + 1) * invRadiusX;
                double nextZn = 0;
                dx = xn * xn;
                forZ: for (int z = 0; z <= ceilRadiusZ; ++z) {
                    final double zn = nextZn;
                    nextZn = (z + 1) * invRadiusZ;
                    dz = zn * zn;
                    dxz = dx + dz;
                    if (dxz > 1) {
                        if (z == 0) {
                            break forX;
                        }
                        break forZ;
                    }

                    if (!filled) {
                        if ((dz + nextXn * nextXn <= 1) && (nextZn * nextZn + dx <= 1)) {
                            continue;
                        }
                    }

                    for (int y = 0; y < height; ++y) {
                        this.setBlock(mutable.setComponents(px + x, py + y, pz + z), block);
                        this.setBlock(mutable.setComponents(px - x, py + y, pz + z), block);
                        this.setBlock(mutable.setComponents(px + x, py + y, pz - z), block);
                        this.setBlock(mutable.setComponents(px - x, py + y, pz - z), block);
                    }
                }
            }
        }

        return this.changes;
    }

    public int makeCircle(BlockVector3 pos, final Pattern block, double radiusX, double radiusY, double radiusZ, boolean filled, Vector3 normal) {
        radiusX += 0.5;
        radiusY += 0.5;
        radiusZ += 0.5;

        normal = normal.normalize();
        double nx = normal.getX();
        double ny = normal.getY();
        double nz = normal.getZ();


        final double invRadiusX = 1 / radiusX;
        final double invRadiusY = 1 / radiusY;
        final double invRadiusZ = 1 / radiusZ;

        int px = pos.getBlockX();
        int py = pos.getBlockY();
        int pz = pos.getBlockZ();
        MutableBlockVector3 mutable = new MutableBlockVector3();

        final int ceilRadiusX = (int) Math.ceil(radiusX);
        final int ceilRadiusY = (int) Math.ceil(radiusY);
        final int ceilRadiusZ = (int) Math.ceil(radiusZ);

        double threshold = 0.5;

        LocalBlockVectorSet set = new LocalBlockVectorSet();

        double nextXn = 0;
        double dx, dy, dz, dxy, dxyz;
        forX:
        for (int x = 0; x <= ceilRadiusX; ++x) {
            final double xn = nextXn;
            dx = xn * xn;
            nextXn = (x + 1) * invRadiusX;
            double nextYn = 0;
            forY:
            for (int y = 0; y <= ceilRadiusY; ++y) {
                final double yn = nextYn;
                dy = yn * yn;
                dxy = dx + dy;
                nextYn = (y + 1) * invRadiusY;
                double nextZn = 0;
                forZ:
                for (int z = 0; z <= ceilRadiusZ; ++z) {
                    final double zn = nextZn;
                    dz = zn * zn;
                    dxyz = dxy + dz;
                    nextZn = (z + 1) * invRadiusZ;
                    if (dxyz > 1) {
                        if (z == 0) {
                            if (y == 0) {
                                break forX;
                            }
                            break forY;
                        }
                        break forZ;
                    }
                    if (!filled) {
                        if (nextXn * nextXn + dy + dz <= 1 && nextYn * nextYn + dx + dz <= 1 && nextZn * nextZn + dx + dy <= 1) {
                            continue;
                        }
                    }

                    if (Math.abs((x) * nx + (y) * ny + (z) * nz) < threshold)
                        setBlock(mutable.setComponents(px + x, py + y, pz + z), block);
                    if (Math.abs((-x) * nx + (y) * ny + (z) * nz) < threshold)
                        setBlock(mutable.setComponents(px - x, py + y, pz + z), block);
                    if (Math.abs((x) * nx + (-y) * ny + (z) * nz) < threshold)
                        setBlock(mutable.setComponents(px + x, py - y, pz + z), block);
                    if (Math.abs((x) * nx + (y) * ny + (-z) * nz) < threshold)
                        setBlock(mutable.setComponents(px + x, py + y, pz - z), block);
                    if (Math.abs((-x) * nx + (-y) * ny + (z) * nz) < threshold)
                        setBlock(mutable.setComponents(px - x, py - y, pz + z), block);
                    if (Math.abs((x) * nx + (-y) * ny + (-z) * nz) < threshold)
                        setBlock(mutable.setComponents(px + x, py - y, pz - z), block);
                    if (Math.abs((-x) * nx + (y) * ny + (-z) * nz) < threshold)
                        setBlock(mutable.setComponents(px - x, py + y, pz - z), block);
                    if (Math.abs((-x) * nx + (-y) * ny + (-z) * nz) < threshold)
                        setBlock(mutable.setComponents(px - x, py - y, pz - z), block);
                }
            }
        }

        return changes;
    }

    /**
    * Makes a sphere.
    *
    * @param pos Center of the sphere or ellipsoid
    * @param block The block pattern to use
    * @param radius The sphere's radius
    * @param filled If false, only a shell will be generated.
    * @return number of blocks changed
    * @throws MaxChangedBlocksException thrown if too many blocks are changed
    */
    public int makeSphere(BlockVector3 pos, Pattern block, double radius, boolean filled) throws MaxChangedBlocksException {
        return makeSphere(pos, block, radius, radius, radius, filled);
    }

    /**
     * Makes a sphere or ellipsoid.
     *
     * @param pos Center of the sphere or ellipsoid
     * @param block The block pattern to use
     * @param radiusX The sphere/ellipsoid's largest north/south extent
     * @param radiusY The sphere/ellipsoid's largest up/down extent
     * @param radiusZ The sphere/ellipsoid's largest east/west extent
     * @param filled If false, only a shell will be generated.
     * @return number of blocks changed
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int makeSphere(BlockVector3 pos, Pattern block, double radiusX, double radiusY, double radiusZ, boolean filled) throws MaxChangedBlocksException {
        int affected = 0;

        radiusX += 0.5;
        radiusY += 0.5;
        radiusZ += 0.5;

        final double invRadiusX = 1 / radiusX;
        final double invRadiusY = 1 / radiusY;
        final double invRadiusZ = 1 / radiusZ;

        int px = pos.getBlockX();
        int py = pos.getBlockY();
        int pz = pos.getBlockZ();

        final int ceilRadiusX = (int) Math.ceil(radiusX);
        final int ceilRadiusY = (int) Math.ceil(radiusY);
        final int ceilRadiusZ = (int) Math.ceil(radiusZ);

        double nextXn = invRadiusX;
        forX: for (int x = 0; x <= ceilRadiusX; ++x) {
            final double xn = nextXn;
            double dx = xn * xn;
            nextXn = (x + 1) * invRadiusX;
            double nextZn = invRadiusZ;
            forZ: for (int z = 0; z <= ceilRadiusZ; ++z) {
                final double zn = nextZn;
                double dz = zn * zn;
                double dxz = dx + dz;
                nextZn = (z + 1) * invRadiusZ;
                double nextYn = invRadiusY;

                forY: for (int y = 0; y <= ceilRadiusY; ++y) {
                    final double yn = nextYn;
                    double dy = yn * yn;
                    double dxyz = dxz + dy;
                    nextYn = (y + 1) * invRadiusY;

                    if (dxyz > 1) {
                        if (y == 0) {
                            if (z == 0) {
                                break forX;
                            }
                            break forZ;
                        }
                        break forY;
                    }

                    if (!filled) {
                        if (nextXn * nextXn + dy + dz <= 1 && nextYn * nextYn + dx + dz <= 1 && nextZn * nextZn + dx + dy <= 1) {
                            continue;
                        }
                    }

                    this.setBlock(px + x, py + y, pz + z, block);
                    if (x != 0) this.setBlock(px - x, py + y, pz + z, block);
                    if (z != 0) {
                        this.setBlock(px + x, py + y, pz - z, block);
                        if (x != 0) this.setBlock(px - x, py + y, pz - z, block);
                    }
                    if (y != 0) {
                        this.setBlock(px + x, py - y, pz + z, block);
                        if (x != 0) this.setBlock(px - x, py - y, pz + z, block);
                        if (z != 0) {
                            this.setBlock(px + x, py - y, pz - z, block);
                            if (x != 0) this.setBlock(px - x, py - y, pz - z, block);
                        }
                    }
                }
            }
        }

        return changes;
    }

    /**
     * Makes a pyramid.
     *
     * @param position a position
     * @param block a block
     * @param size size of pyramid
     * @param filled true if filled
     * @return number of blocks changed
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int makePyramid(BlockVector3 position, Pattern block, int size, boolean filled) throws MaxChangedBlocksException {
        int bx = position.getX();
        int by = position.getY();
        int bz = position.getZ();

        int height = size;

        for (int y = 0; y <= height; ++y) {
            size--;
            for (int x = 0; x <= size; ++x) {
                for (int z = 0; z <= size; ++z) {

                    if ((filled && z <= size && x <= size) || z == size || x == size) {
                        setBlock(x + bx, y + by, z + bz, block);
                        setBlock(-x + bx, y + by, z + bz, block);
                        setBlock(x + bx, y + by, -z + bz, block);
                        setBlock(-x + bx, y + by, -z + bz, block);
                    }
                }
            }
        }

        return changes;
    }

    /**
     * Thaw blocks in a radius.
     *
     * @param position the position
     * @param radius the radius
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int thaw(BlockVector3 position, double radius)
            throws MaxChangedBlocksException {
        int affected = 0;
        double radiusSq = radius * radius;

        int ox = position.getBlockX();
        int oy = position.getBlockY();
        int oz = position.getBlockZ();

        BlockState air = BlockTypes.AIR.getDefaultState();
        BlockState water = BlockTypes.WATER.getDefaultState();

        int ceilRadius = (int) Math.ceil(radius);
        for (int x = ox - ceilRadius; x <= ox + ceilRadius; ++x) {
            for (int z = oz - ceilRadius; z <= oz + ceilRadius; ++z) {
                if ((BlockVector3.at(x, oy, z)).distanceSq(position) > radiusSq) {
                    continue;
                }

                for (int y = maxY; y >= 1; --y) {
                    BlockType id = getBlock(x, y, z).getBlockType();

                    if (id == BlockTypes.ICE) {
                        if (setBlock(x, y, z, water)) {
                            ++affected;
                        }
                    } else if (id == BlockTypes.SNOW) {
                        if (setBlock(x, y, z, air)) {
                            ++affected;
                        }
                    } else if (id.getMaterial().isAir()) {
                        continue;
                    }

                    break;
                }
            }
        }

        return changes;
    }

    /**
     * Make snow in a radius.
     *
     * @param position a position
     * @param radius a radius
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int simulateSnow(BlockVector3 position, double radius) throws MaxChangedBlocksException {
        int affected = 0;
        double radiusSq = radius * radius;

        int ox = position.getBlockX();
        int oy = position.getBlockY();
        int oz = position.getBlockZ();

        BlockState ice = BlockTypes.ICE.getDefaultState();
        BlockState snow = BlockTypes.SNOW.getDefaultState();

        int ceilRadius = (int) Math.ceil(radius);
        for (int x = ox - ceilRadius; x <= ox + ceilRadius; ++x) {
            for (int z = oz - ceilRadius; z <= oz + ceilRadius; ++z) {
                if ((BlockVector3.at(x, oy, z)).distanceSq(position) > radiusSq) {
                    continue;
                }

                for (int y = maxY; y >= 1; --y) {
                    BlockVector3 pt = BlockVector3.at(x, y, z);
                    BlockType id = getBlock(pt).getBlockType();

                    if (id.getMaterial().isAir()) {
                        continue;
                    }

                    // Ice!
                    if (id == BlockTypes.WATER) {
                        if (setBlock(pt, ice)) {
                            ++affected;
                        }
                        break;
                    }

                    // Snow should not cover these blocks
                    if (id.getMaterial().isTranslucent()) {
                        // Add snow on leaves
                        if (!BlockCategories.LEAVES.contains(id)) {
                            break;
                        }
                    }

                    // Too high?
                    if (y == maxY) {
                        break;
                    }

                    // add snow cover
                    if (setBlock(pt.add(0, 1, 0), snow)) {
                        ++affected;
                    }
                    break;
                }
            }
        }

        return changes;
    }

    /**
     * Make dirt green.
     *
     * @param position a position
     * @param radius a radius
     * @param onlyNormalDirt only affect normal dirt (data value 0)
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int green(BlockVector3 position, double radius, final boolean onlyNormalDirt)
            throws MaxChangedBlocksException {
        final double radiusSq = radius * radius;

        final int ox = position.getBlockX();
        final int oz = position.getBlockZ();

        final BlockState grass = BlockTypes.GRASS_BLOCK.getDefaultState();

        final int ceilRadius = (int) Math.ceil(radius);
        for (int x = ox - ceilRadius; x <= ox + ceilRadius; ++x) {
            int dx = x - ox;
            int dx2 = dx * dx;
            for (int z = oz - ceilRadius; z <= (oz + ceilRadius); ++z) {
                int dz = z - oz;
                int dz2 = dz * dz;
                if (dx2 + dz2 > radiusSq) {
                    continue;
                }
                loop:
                for (int y = maxY; y >= 1; --y) {
                    BlockType block = getBlockType(x, y, z);
                    switch (block.getInternalId()) {
                        case BlockID.COARSE_DIRT:
                            if (onlyNormalDirt) break loop;
                        case BlockID.DIRT:
                            this.setBlock(x, y, z, BlockTypes.GRASS_BLOCK.getDefaultState());
                            break loop;
                        case BlockID.WATER:
                        case BlockID.LAVA:
                        default:
                            if (block.getMaterial().isMovementBlocker()) {
                                break loop;
                            }
                    }
                }
            }
        }

        return changes;
    }

    /**
     * Makes pumpkin patches randomly in an area around the given position.
     *
     * @param position the base position
     * @param apothem the apothem of the (square) area
     * @return number of patches created
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int makePumpkinPatches(final BlockVector3 position, final int apothem) {
        return makePumpkinPatches(position, apothem, 0.02);
    }

    public int makePumpkinPatches(final BlockVector3 position, final int apothem, double density) {
        // We want to generate pumpkins
        GardenPatchGenerator generator = new GardenPatchGenerator(this);
        generator.setPlant(GardenPatchGenerator.getPumpkinPattern());

        // In a region of the given radius
        FlatRegion region = new CuboidRegion(
                getWorld(), // Causes clamping of Y range
                position.add(-apothem, -5, -apothem),
                position.add(apothem, 10, apothem));

        GroundFunction ground = new GroundFunction(new ExistingBlockMask(this), generator);
        LayerVisitor visitor = new LayerVisitor(region, minimumBlockY(region), maximumBlockY(region), ground);
        visitor.setMask(new NoiseFilter2D(new RandomNoise(), density));
        Operations.completeBlindly(visitor);
        return this.changes = ground.getAffected();
    }

    /**
     * Makes a forest.
     *
     * @param basePosition a position
     * @param size a size
     * @param density between 0 and 1, inclusive
     * @param treeType the tree type
     * @return number of trees created
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int makeForest(BlockVector3 basePosition, int size, double density, TreeGenerator.TreeType treeType) throws MaxChangedBlocksException {
        try {
            for (int x = basePosition.getBlockX() - size; x <= (basePosition.getBlockX() + size); ++x) {
                for (int z = basePosition.getBlockZ() - size; z <= (basePosition.getBlockZ() + size); ++z) {
                    // Don't want to be in the ground
                    if (!this.getBlockType(x, basePosition.getBlockY(), z).getMaterial().isAir()) {
                        continue;
                    }
                    // The gods don't want a tree here
                    if (ThreadLocalRandom.current().nextInt(65536) >= (density * 65536)) {
                        continue;
                    } // def 0.05
                    this.changes++;
                    for (int y = basePosition.getBlockY(); y >= (basePosition.getBlockY() - 10); --y) {
                        BlockType type = getBlockType(x, y, z);
                        switch (type.getInternalId()) {
                            case BlockID.GRASS:
                            case BlockID.DIRT:
                                treeType.generate(this, BlockVector3.at(x, y + 1, z));
                                this.changes++;
                                break;
                            case BlockID.SNOW:
                                setBlock(BlockVector3.at(x, y, z), BlockTypes.AIR.getDefaultState());
                                break;
                        }
                    }
                }
            }
        } catch (MaxChangedBlocksException ignore) {}
        return this.changes;
    }

    /**
     * Makes a forest.
     *
     * @param region the region to generate trees in
     * @param density between 0 and 1, inclusive
     * @param treeType the tree type
     * @return number of trees created
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int makeForest(Region region, double density, TreeGenerator.TreeType treeType) throws MaxChangedBlocksException {
        ForestGenerator generator = new ForestGenerator(this, treeType);
        GroundFunction ground = new GroundFunction(new ExistingBlockMask(this), generator);
        LayerVisitor visitor = new LayerVisitor(asFlatRegion(region), minimumBlockY(region), maximumBlockY(region), ground);
        visitor.setMask(new NoiseFilter2D(new RandomNoise(), density));
        Operations.completeLegacy(visitor);
        return ground.getAffected();
    }

    /**
     * Get the block distribution inside a region.
     *
     * @param region a region
     * @return the results
     */
    public List<Countable<BlockState>> getBlockDistribution(Region region, boolean separateStates) {
        if (separateStates) return getBlockDistributionWithData(region);
        int[] counter = new int[BlockTypes.size()];

        if (region instanceof CuboidRegion) {
            // Doing this for speed
            final BlockVector3 min = region.getMinimumPoint();
            final BlockVector3 max = region.getMaximumPoint();

            final int minX = min.getBlockX();
            final int minY = min.getBlockY();
            final int minZ = min.getBlockZ();
            final int maxX = max.getBlockX();
            final int maxY = max.getBlockY();
            final int maxZ = max.getBlockZ();

            MutableBlockVector3 mutable = new MutableBlockVector3(minX, minY, minZ);
            for (int x = minX; x <= maxX; ++x) {
                for (int y = minY; y <= maxY; ++y) {
                    for (int z = minZ; z <= maxZ; ++z) {
                        BlockType type = getBlockType(x, y, z);
                        counter[type.getInternalId()]++;
                    }
                }
            }
        } else {
            for (final BlockVector3 pt : region) {
                BlockType type = getBlockType(pt);
                counter[type.getInternalId()]++;
            }
        }
        List<Countable<BlockState>> distribution = new ArrayList<>();
        for (int i = 0; i < counter.length; i++) {
            int count = counter[i];
            if (count != 0) {
                distribution.add(new Countable<>(BlockTypes.get(i).getDefaultState(), count));
            }
        }
        Collections.sort(distribution);
        return distribution;
    }

    /**
     * Generate a shape for the given expression.
     *
     * @param region the region to generate the shape in
     * @return number of blocks changed
     * @throws ExpressionException
     * @throws MaxChangedBlocksException
     */
    public List<Countable<BlockState>> getBlockDistributionWithData(final Region region) {
        int[][] counter = new int[BlockTypes.size()][];

        if (region instanceof CuboidRegion) {
            // Doing this for speed
            final BlockVector3 min = region.getMinimumPoint();
            final BlockVector3 max = region.getMaximumPoint();

            final int minX = min.getBlockX();
            final int minY = min.getBlockY();
            final int minZ = min.getBlockZ();
            final int maxX = max.getBlockX();
            final int maxY = max.getBlockY();
            final int maxZ = max.getBlockZ();

            for (int x = minX; x <= maxX; ++x) {
                for (int y = minY; y <= maxY; ++y) {
                    for (int z = minZ; z <= maxZ; ++z) {
                        BlockStateHolder blk = getBlock(x, y, z);
                        BlockType type = blk.getBlockType();
                        int[] stateCounter = counter[type.getInternalId()];
                        if (stateCounter == null) {
                            counter[type.getInternalId()] = stateCounter = new int[type.getMaxStateId() + 1];
                        }
                        stateCounter[blk.getInternalPropertiesId()]++;
                    }
                }
            }
        } else {
            for (final BlockVector3 pt : region) {
                BlockStateHolder blk = this.getBlock(pt);
                BlockType type = blk.getBlockType();
                int[] stateCounter = counter[type.getInternalId()];
                if (stateCounter == null) {
                    counter[type.getInternalId()] = stateCounter = new int[type.getMaxStateId() + 1];
                }
                stateCounter[blk.getInternalPropertiesId()]++;
            }
        }
        List<Countable<BlockState>> distribution = new ArrayList<>();
        for (int typeId = 0; typeId < counter.length; typeId++) {
            BlockType type = BlockTypes.get(typeId);
            int[] stateCount = counter[typeId];
            if (stateCount != null) {
                for (int propId = 0; propId < stateCount.length; propId++) {
                    int count = stateCount[propId];
                    if (count != 0) {
                        BlockState state = type.withPropertyId(propId);
                        distribution.add(new Countable<>(state, count));
                    }

                }
            }
        }
        // Collections.reverse(distribution);
        return distribution;
    }

    /**
     * Generate a shape for the given expression.
     *
     * @param region the region to generate the shape in
     * @param zero the coordinate origin for x/y/z variables
     * @param unit the scale of the x/y/z/ variables
     * @param pattern the default material to make the shape from
     * @param expressionString the expression defining the shape
     * @param hollow whether the shape should be hollow
     * @return number of blocks changed
     * @throws ExpressionException
     * @throws MaxChangedBlocksException
     */
    public int makeShape(final Region region, final Vector3 zero, final Vector3 unit,
                        final Pattern pattern, final String expressionString, final boolean hollow)
            throws ExpressionException, MaxChangedBlocksException {
        final Expression expression = Expression.compile(expressionString, "x", "y", "z", "type", "data");
        expression.optimize();

        final RValue typeVariable = expression.getVariable("type", false);
        final RValue dataVariable = expression.getVariable("data", false);

        final WorldEditExpressionEnvironment environment = new WorldEditExpressionEnvironment(this, unit, zero);
        expression.setEnvironment(environment);

        final int[] timedOut = {0};
        final ArbitraryShape shape = new ArbitraryShape(region) {
            @Override
            public BaseBlock getMaterial(final int x, final int y, final int z, final BaseBlock defaultMaterial) {
                //TODO Optimize - avoid vector creation (math)
//                final Vector3 current = mutablev.setComponents(x, y, z);
//            protected BlockStateHolder getMaterial(int x, int y, int z, BlockStateHolder defaultMaterial) {
                final Vector3 current = Vector3.at(x, y, z);
                environment.setCurrentBlock(current);
                final Vector3 scaled = current.subtract(zero).divide(unit);

                try {
                    if (expression.evaluate(scaled.getX(), scaled.getY(), scaled.getZ(), defaultMaterial.getBlockType().getInternalId(), defaultMaterial.getInternalPropertiesId()) <= 0) {
                        // TODO data
                        return null;
                    }

                    return BlockTypes.get((int) typeVariable.getValue()).withPropertyId((int) dataVariable.getValue()).toBaseBlock();
                } catch (Exception e) {
                    log.warn("Failed to create shape", e);
                    return null;
                }
            }
        };
        int changed = shape.generate(this, pattern, hollow);
        if (timedOut[0] > 0) {
            throw new ExpressionTimeoutException(
                    String.format("%d blocks changed. %d blocks took too long to evaluate (increase with //timeout).",
                            changed, timedOut[0]));
        }
        return changed;
    }

    public int deformRegion(final Region region, final Vector3 zero, final Vector3 unit, final String expressionString)
            throws ExpressionException, MaxChangedBlocksException {
        final Expression expression = Expression.compile(expressionString, "x", "y", "z");
        expression.optimize();

        final RValue x = expression.getVariable("x", false).optimize();
        final RValue y = expression.getVariable("y", false).optimize();
        final RValue z = expression.getVariable("z", false).optimize();
        final WorldEditExpressionEnvironment environment = new WorldEditExpressionEnvironment(this, unit, zero);
        expression.setEnvironment(environment);
        final Vector3 zero2 = zero.add(0.5, 0.5, 0.5);

        RegionVisitor visitor = new RegionVisitor(region, new RegionFunction() {
        private MutableBlockVector3 mutable = new MutableBlockVector3();

            @Override
            public boolean apply(BlockVector3 position) throws WorldEditException {
                try {
                    // offset, scale
                    double sx = (position.getX() - zero.getX()) / unit.getX();
                    double sy = (position.getY() - zero.getY()) / unit.getY();
                    double sz = (position.getZ() - zero.getZ()) / unit.getZ();
                    // transform
                    expression.evaluate(sx, sy, sz);
                    int xv = (int) (x.getValue() * unit.getX() + zero2.getX());
                    int yv = (int) (y.getValue() * unit.getY() + zero2.getY());
                    int zv = (int) (z.getValue() * unit.getZ() + zero2.getZ());
                    // read/write block from world
                    return setBlock(position, getBlock(xv, yv, zv));
                } catch (EvaluationException e) {
                    throw new RuntimeException(e);
                }
            }
        }, this);
        Operations.completeBlindly(visitor);
        changes += visitor.getAffected();
        return changes;
    }

    /**
     * Hollows out the region (Semi-well-defined for non-cuboid selections).
     *
     * @param region the region to hollow out.
     * @param thickness the thickness of the shell to leave (manhattan distance)
     * @param pattern The block pattern to use
     *
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int hollowOutRegion(Region region, int thickness, Pattern pattern) throws MaxChangedBlocksException {
    	try {
        final Set<BlockVector3> outside = new LocalBlockVectorSet();

        final BlockVector3 min = region.getMinimumPoint();
        final BlockVector3 max = region.getMaximumPoint();

        final int minX = min.getBlockX();
        final int minY = min.getBlockY();
        final int minZ = min.getBlockZ();
        final int maxX = max.getBlockX();
        final int maxY = max.getBlockY();
        final int maxZ = max.getBlockZ();

        for (int x = minX; x <= maxX; ++x) {
            for (int y = minY; y <= maxY; ++y) {
                recurseHollow(region, BlockVector3.at(x, y, minZ), outside);
                recurseHollow(region, BlockVector3.at(x, y, maxZ), outside);
            }
        }

        for (int y = minY; y <= maxY; ++y) {
            for (int z = minZ; z <= maxZ; ++z) {
                recurseHollow(region, BlockVector3.at(minX, y, z), outside);
                recurseHollow(region, BlockVector3.at(maxX, y, z), outside);
            }
        }

        for (int z = minZ; z <= maxZ; ++z) {
            for (int x = minX; x <= maxX; ++x) {
                recurseHollow(region, BlockVector3.at(x, minY, z), outside);
                recurseHollow(region, BlockVector3.at(x, maxY, z), outside);
            }
        }

        for (int i = 1; i < thickness; ++i) {
            final Set<BlockVector3> newOutside = new LocalBlockVectorSet();
            outer: for (BlockVector3 position : region) {
                for (BlockVector3 recurseDirection : recurseDirections) {
                    BlockVector3 neighbor = position.add(recurseDirection);

                    if (outside.contains(neighbor)) {
                        newOutside.add(position);
                        continue outer;
                    }
                }
            }

            outside.addAll(newOutside);
        }

        outer: for (BlockVector3 position : region) {
            for (BlockVector3 recurseDirection : recurseDirections) {
                BlockVector3 neighbor = position.add(recurseDirection);

                if (outside.contains(neighbor)) {
                    continue outer;
                    }
                }
                this.changes++;
                pattern.apply(this.extent, position, position);
            }
        } catch (WorldEditException e) {
            throw new RuntimeException(e);
        }

        return changes;
    }

    public int drawLine(final Pattern pattern, final BlockVector3 pos1, final BlockVector3 pos2, final double radius, final boolean filled) {
        return drawLine(pattern, pos1, pos2, radius, filled, false);
    }

    /**
     * Draws a line (out of blocks) between two vectors.
     *
     * @param pattern The block pattern used to draw the line.
     * @param pos1 One of the points that define the line.
     * @param pos2 The other point that defines the line.
     * @param radius The radius (thickness) of the line.
     * @param filled If false, only a shell will be generated.
     *
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int drawLine(Pattern pattern, BlockVector3 pos1, BlockVector3 pos2, double radius, boolean filled, boolean flat) {

        LocalBlockVectorSet vset = new LocalBlockVectorSet();
        boolean notdrawn = true;

        int x1 = pos1.getBlockX(), y1 = pos1.getBlockY(), z1 = pos1.getBlockZ();
        int x2 = pos2.getBlockX(), y2 = pos2.getBlockY(), z2 = pos2.getBlockZ();
        int tipx = x1, tipy = y1, tipz = z1;
        int dx = Math.abs(x2 - x1), dy = Math.abs(y2 - y1), dz = Math.abs(z2 - z1);

        if (dx + dy + dz == 0) {
            vset.add(BlockVector3.at(tipx, tipy, tipz));
            notdrawn = false;
        }

        if (Math.max(Math.max(dx, dy), dz) == dx && notdrawn) {
            for (int domstep = 0; domstep <= dx; domstep++) {
                tipx = x1 + domstep * (x2 - x1 > 0 ? 1 : -1);
                tipy = (int) Math.round(y1 + domstep * ((double) dy) / ((double) dx) * (y2 - y1 > 0 ? 1 : -1));
                tipz = (int) Math.round(z1 + domstep * ((double) dz) / ((double) dx) * (z2 - z1 > 0 ? 1 : -1));

                vset.add(BlockVector3.at(tipx, tipy, tipz));
            }
            notdrawn = false;
        }

        if (Math.max(Math.max(dx, dy), dz) == dy && notdrawn) {
            for (int domstep = 0; domstep <= dy; domstep++) {
                tipy = y1 + domstep * (y2 - y1 > 0 ? 1 : -1);
                tipx = (int) Math.round(x1 + domstep * ((double) dx) / ((double) dy) * (x2 - x1 > 0 ? 1 : -1));
                tipz = (int) Math.round(z1 + domstep * ((double) dz) / ((double) dy) * (z2 - z1 > 0 ? 1 : -1));

                vset.add(BlockVector3.at(tipx, tipy, tipz));
            }
            notdrawn = false;
        }

        if (Math.max(Math.max(dx, dy), dz) == dz && notdrawn) {
            for (int domstep = 0; domstep <= dz; domstep++) {
                tipz = z1 + domstep * (z2 - z1 > 0 ? 1 : -1);
                tipy = (int) Math.round(y1 + domstep * ((double) dy) / ((double) dz) * (y2-y1>0 ? 1 : -1));
                tipx = (int) Math.round(x1 + domstep * ((double) dx) / ((double) dz) * (x2-x1>0 ? 1 : -1));

                vset.add(BlockVector3.at(tipx, tipy, tipz));
            }
            notdrawn = false;
        }
        Set<BlockVector3> newVset;
        if (flat) {
            newVset = this.getStretched(vset, radius);
            if (!filled) {
                newVset = this.getOutline(newVset);
            }
        } else {
            newVset = this.getBallooned(vset, radius);
            if (!filled) {
                newVset = this.getHollowed(newVset);
            }
        }
        return setBlocks(newVset, pattern);
    }

    /**
     * Draws a spline (out of blocks) between specified vectors.
     *
     * @param pattern The block pattern used to draw the spline.
     * @param nodevectors The list of vectors to draw through.
     * @param tension The tension of every node.
     * @param bias The bias of every node.
     * @param continuity The continuity of every node.
     * @param quality The quality of the spline. Must be greater than 0.
     * @param radius The radius (thickness) of the spline.
     * @param filled If false, only a shell will be generated.
     *
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int drawSpline(Pattern pattern, List<BlockVector3> nodevectors, double tension, double bias,
                          double continuity, double quality, double radius, boolean filled)
            throws MaxChangedBlocksException {

        LocalBlockVectorSet vset = new LocalBlockVectorSet();
        List<Node> nodes = new ArrayList<>(nodevectors.size());

        Interpolation interpol = new KochanekBartelsInterpolation();

        for (BlockVector3 nodevector : nodevectors) {
            Node n = new Node(nodevector.toVector3());
            n.setTension(tension);
            n.setBias(bias);
            n.setContinuity(continuity);
            nodes.add(n);
        }

        interpol.setNodes(nodes);
        double splinelength = interpol.arcLength(0, 1);
        for (double loop = 0; loop <= 1; loop += 1D / splinelength / quality) {
            final BlockVector3 tipv = interpol.getPosition(loop).toBlockPoint();
            if (radius == 0) {
                pattern.apply(this, tipv, tipv);
            } else {
                vset.add(tipv);
            }
        }
        Set<BlockVector3> newVset;
        if (radius != 0) {
            newVset = this.getBallooned(vset, radius);
            if (!filled) {
                newVset = this.getHollowed(newVset);
            }
            return setBlocks(newVset, pattern);
        }
        return changes;
    }

    private Set<BlockVector3> getBallooned(Set<BlockVector3> vset, double radius) {
        if (radius < 1) {
            return vset;
        }
        LocalBlockVectorSet returnset = new LocalBlockVectorSet();
        int ceilrad = (int) Math.ceil(radius);

        for (BlockVector3 v : vset) {
            int tipx = v.getBlockX(), tipy = v.getBlockY(), tipz = v.getBlockZ();

            for (int loopx = tipx - ceilrad; loopx <= (tipx + ceilrad); loopx++) {
                for (int loopy = tipy - ceilrad; loopy <= (tipy + ceilrad); loopy++) {
                    for (int loopz = tipz - ceilrad; loopz <= (tipz + ceilrad); loopz++) {
                        if (MathMan.hypot(loopx - tipx, loopy - tipy, loopz - tipz) <= radius) {
                            returnset.add(loopx, loopy, loopz);
                        }
                    }
                }
            }
        }
        return returnset;
    }

    public Set<BlockVector3> getStretched(final Set<BlockVector3> vset, final double radius) {
        if (radius < 1) {
            return vset;
        }
        final LocalBlockVectorSet returnset = new LocalBlockVectorSet();
        final int ceilrad = (int) Math.ceil(radius);
        for (final BlockVector3 v : vset) {
            final int tipx = v.getBlockX(), tipy = v.getBlockY(), tipz = v.getBlockZ();
            for (int loopx = tipx - ceilrad; loopx <= (tipx + ceilrad); loopx++) {
                for (int loopz = tipz - ceilrad; loopz <= (tipz + ceilrad); loopz++) {
                    if (MathMan.hypot(loopx - tipx, 0, loopz - tipz) <= radius) {
                        returnset.add(loopx, v.getBlockY(), loopz);
                    }
                }
            }
        }
        return returnset;
    }

    public Set<BlockVector3> getOutline(final Set<BlockVector3> vset) {
        final LocalBlockVectorSet returnset = new LocalBlockVectorSet();
        final LocalBlockVectorSet newset = new LocalBlockVectorSet();
        newset.addAll(vset);
        for (final BlockVector3 v : newset) {
            final int x = v.getX(), y = v.getY(), z = v.getZ();
            if (!(newset.contains(x + 1, y, z)
                    && newset.contains(x - 1, y, z)
                    && newset.contains(x, y, z + 1) && newset.contains(x, y, z - 1))) {
                returnset.add(v);
            }
        }
        return returnset;
    }

    public Set<BlockVector3> getHollowed(final Set<BlockVector3> vset) {
        final Set returnset = new LocalBlockVectorSet();
        final LocalBlockVectorSet newset = new LocalBlockVectorSet();
        newset.addAll(vset);
        for (final BlockVector3 v : newset) {
            final int x = v.getX(), y = v.getY(), z = v.getZ();
            if (!(newset.contains(x + 1, y, z)
                    && newset.contains(x - 1, y, z)
                    && newset.contains(x, y + 1, z)
                    && newset.contains(x, y - 1, z)
                    && newset.contains(x, y, z + 1) && newset.contains(x, y, z - 1))) {
                returnset.add(v);
            }
        }
        return returnset;
    }

    private void recurseHollow(Region region, BlockVector3 origin, Set<BlockVector3> outside) {
        final LocalBlockVectorSet queue = new LocalBlockVectorSet();
        queue.add(origin);
        while (!queue.isEmpty()) {
        	BlockVector3 current = queue.getIndex(0);
        	queue.remove(current);
        	final BlockState block = getBlock(current);
        	if (block.getBlockType().getMaterial().isMovementBlocker()) {
        		continue;
        	}

        	if (!outside.add(current)) {
        		continue;
        	}

        	if (!region.contains(current)) {
        		continue;
        	}

        	for (BlockVector3 recurseDirection : recurseDirections) {
        		queue.add(current.add(recurseDirection));
        	}
        }
    }

    public int makeBiomeShape(final Region region, final Vector3 zero, final Vector3 unit, final BiomeType biomeType,
                              final String expressionString, final boolean hollow)
            throws ExpressionException, MaxChangedBlocksException {
        final Vector2 zero2D = zero.toVector2();
        final Vector2 unit2D = unit.toVector2();

        final Expression expression = Expression.compile(expressionString, "x", "z");
        expression.optimize();

        final EditSession editSession = this;
        final WorldEditExpressionEnvironment environment = new WorldEditExpressionEnvironment(editSession, unit, zero);
        expression.setEnvironment(environment);

        final ArbitraryBiomeShape shape = new ArbitraryBiomeShape(region) {
            @Override
            protected BiomeType getBiome(final int x, final int z, final BiomeType defaultBiomeType) {
                environment.setCurrentBlock(x, 0, z);
                double scaledX = (x - zero2D.getX()) / unit2D.getX();
                double scaledZ = (z - zero2D.getZ()) / unit2D.getZ();

                try {
                    if (expression.evaluate(scaledX, scaledZ) <= 0) {
                        return null;
                    }

                    // TODO: Allow biome setting via a script variable (needs BiomeType<->int mapping)
                    return defaultBiomeType;
                } catch (Exception e) {
                    log.warn("Failed to create shape", e);
                    return null;
                }
            }
        };
        int changed = shape.generate(this, biomeType, hollow);
        return changed;
    }

    private static final BlockVector3[] recurseDirections = {
            Direction.NORTH.toBlockVector(),
            Direction.EAST.toBlockVector(),
            Direction.SOUTH.toBlockVector(),
            Direction.WEST.toBlockVector(),
            Direction.UP.toBlockVector(),
            Direction.DOWN.toBlockVector(),
    };

    private static double lengthSq(double x, double y, double z) {
        return (x * x) + (y * y) + (z * z);
    }

    private static double lengthSq(double x, double z) {
        return (x * x) + (z * z);
    }


    @Override
    public String getName() {
        return worldName;
    }

    @Override
    public int getBlockLightLevel(BlockVector3 position) {
        return queue.getEmmittedLight(position.getBlockX(), position.getBlockY(), position.getBlockZ());
    }

    @Override
    public boolean clearContainerBlockContents(BlockVector3 pos) {
        BaseBlock block = getFullBlock(pos);
        CompoundTag nbt = block.getNbtData();
        if (nbt != null) {
            if (nbt.containsKey("items")) {
                block.setNbtData(null);
                return setBlock(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ(), block);
            }
        }
        return false;
    }

    public boolean regenerate(final Region region) {
        return regenerate(region, this);
    }

    @Override
    public boolean regenerate(final Region region, final EditSession session) {
        return session.regenerate(region, null, null);
    }

    private void setExistingBlocks(BlockVector3 pos1, BlockVector3 pos2) {
        for (int x = pos1.getX(); x <= pos2.getX(); x++) {
            for (int z = pos1.getBlockZ(); z <= pos2.getBlockZ(); z++) {
                for (int y = pos1.getY(); y <= pos2.getY(); y++) {
                    int from = queue.getCombinedId4Data(x, y, z);
                    queue.setBlock(x, y, z, from);
                    if (BlockTypes.getFromStateId(from).getMaterial().hasContainer()) {
                        CompoundTag tile = queue.getTileEntity(x, y, z);
                        if (tile != null) {
                            queue.setTile(x, y, z, tile);
                        }
                    }
                }
            }
        }
    }

    public boolean regenerate(final Region region, final BiomeType biome, final Long seed) {
        //TODO Optimize - avoid Vector2D creation (make mutable)
        final FaweQueue queue = this.getQueue();
        queue.setChangeTask(null);
        final FaweChangeSet fcs = (FaweChangeSet) this.getChangeSet();
        final FaweRegionExtent fe = this.getRegionExtent();
        final boolean cuboid = region instanceof CuboidRegion;
        if (fe != null && cuboid) {
        	BlockVector3 max = region.getMaximumPoint();
        	BlockVector3 min = region.getMinimumPoint();
            if (!fe.contains(max.getBlockX(), max.getBlockY(), max.getBlockZ()) && !fe.contains(min.getBlockX(), min.getBlockY(), min.getBlockZ())) {
                throw new FaweException(BBC.WORLDEDIT_CANCEL_REASON_OUTSIDE_REGION);
            }
        }
        final Set<BlockVector2> chunks = region.getChunks();
        MutableBlockVector3 mutable = new MutableBlockVector3();
        MutableBlockVector2 mutable2D = new MutableBlockVector2();
        for (BlockVector2 chunk : chunks) {
            final int cx = chunk.getBlockX();
            final int cz = chunk.getBlockZ();
            final int bx = cx << 4;
            final int bz = cz << 4;
            final BlockVector3 cmin = BlockVector3.at(bx, 0, bz);
            final BlockVector3 cmax = cmin.add(15, maxY, 15);
            final boolean containsBot1 = (fe == null || fe.contains(cmin.getBlockX(), cmin.getBlockY(), cmin.getBlockZ()));
            final boolean containsBot2 = region.contains(cmin);
            final boolean containsTop1 = (fe == null || fe.contains(cmax.getBlockX(), cmax.getBlockY(), cmax.getBlockZ()));
            final boolean containsTop2 = region.contains(cmax);
            if (((containsBot2 && containsTop2)) && !containsBot1 && !containsTop1) {
                continue;
            }
            boolean conNextX = chunks.contains(mutable2D.setComponents(cx + 1, cz));
            boolean conNextZ = chunks.contains(mutable2D.setComponents(cx, cz + 1));
            boolean containsAny = false;
            if (cuboid && containsBot1 && containsBot2 && containsTop1 && containsTop2 && conNextX && conNextZ) {
                containsAny = true;
                BlockVector3 mbv = mutable;
                if (fcs != null) {
                    for (int x = 0; x < 16; x++) {
                        int xx = x + bx;
                        for (int z = 0; z < 16; z++) {
                            int zz = z + bz;
                            for (int y = 0; y < maxY + 1; y++) {
                                BaseBlock block = getFullBlock(mutable.setComponents(xx, y, zz));
                                fcs.add(mbv, block, BlockTypes.AIR.getDefaultState().toBaseBlock());
                            }
                        }
                    }
                }
            } else {
                if (!conNextX) {
                    setExistingBlocks(BlockVector3.at(bx + 16, 0, bz), BlockVector3.at(bx + 31, maxY, bz + 15));
                }
                if (!conNextZ) {
                    setExistingBlocks(BlockVector3.at(bx, 0, bz + 16), BlockVector3.at(bx + 15, maxY, bz + 31));
                }
                if (!chunks.contains(mutable2D.setComponents(cx + 1, cz + 1)) && !conNextX && !conNextZ) {
                    setExistingBlocks(BlockVector3.at(bx + 16, 0, bz + 16), BlockVector3.at(bx + 31, maxY, bz + 31));
                }
                for (int x = 0; x < 16; x++) {
                    int xx = x + bx;
                    mutable.mutX(xx);
                    for (int z = 0; z < 16; z++) {
                        int zz = z + bz;
                        mutable.mutZ(zz);
                        for (int y = 0; y < maxY + 1; y++) {
                            mutable.mutY(y);
                            boolean contains = (fe == null || fe.contains(xx, y, zz)) && region.contains(mutable);
                            if (contains) {
                                containsAny = true;
                                if (fcs != null) {
                                    BaseBlock block = getFullBlock(mutable);
                                    fcs.add(mutable, block, BlockTypes.AIR.getDefaultState().toBaseBlock());
                                }
                            } else {
                                BlockStateHolder block = getFullBlock(mutable);
                                try {
                                    setBlock(mutable, block);
                                } catch (MaxChangedBlocksException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }
                }
            }
            if (containsAny) {
                changes++;
                TaskManager.IMP.sync(new RunnableVal<Object>() {
                    @Override
                    public void run(Object value) {
                        queue.regenerateChunk(cx, cz, biome, seed);
                    }
                });
            }
        }
        if (changes != 0) {
            flushQueue();
            return true;
        } else {
            this.queue.clear();
        }
        return false;
    }

//    public void dropItem(BlockVector3 position, BaseItemStack item) {
//        if (getWorld() != null) {
//            getWorld().dropItem(position, item);
//        }
//    }

    @Override
    public void simulateBlockMine(BlockVector3 position) {
        TaskManager.IMP.sync((Supplier<Object>) () -> {
            world.simulateBlockMine(position);
            return null;
        });
    }

    public boolean generateTree(TreeGenerator.TreeType type, BlockVector3 position) {
        return generateTree(type, this, position);
    }

    @Override
    public boolean generateTree(TreeGenerator.TreeType type, EditSession editSession, BlockVector3 position) {
        if (getWorld() != null) {
            try {
                return getWorld().generateTree(type, editSession, position);
            } catch (MaxChangedBlocksException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    @Override
    public WeatherType getWeather() {
        return world.getWeather();
    }

    @Override
    public long getRemainingWeatherDuration() {
        return world.getRemainingWeatherDuration();
    }

    @Override
    public void setWeather(WeatherType weatherType) {
        world.setWeather(weatherType);
    }

    @Override
    public void setWeather(WeatherType weatherType, long duration) {
        world.setWeather(weatherType, duration);
    }

	@Override
	public void dropItem(Vector3 position, BaseItemStack item) {
		world.dropItem(position, item);
	}

	@Override
	public boolean playEffect(Vector3 position, int type, int data) {
		return world.playEffect(position, type, data);
	}

	@Override
	public boolean notifyAndLightBlock(BlockVector3 position, BlockState previousType) throws WorldEditException {
		return world.notifyAndLightBlock(position, previousType);
	}

	@Override
	public BlockVector3 getSpawnPosition() {
		return world.getSpawnPosition();
	}

}
