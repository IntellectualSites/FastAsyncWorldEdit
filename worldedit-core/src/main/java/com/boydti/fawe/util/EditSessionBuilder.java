package com.boydti.fawe.util;

import static com.google.common.base.Preconditions.checkNotNull;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.IBatchProcessor;
import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.beta.implementation.processors.LimitProcessor;
import com.boydti.fawe.beta.implementation.queue.ParallelQueueExtent;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.logging.LoggingChangeSet;
import com.boydti.fawe.logging.rollback.RollbackOptimizedHistory;
import com.boydti.fawe.object.FaweLimit;
import com.boydti.fawe.object.HistoryExtent;
import com.boydti.fawe.object.NullChangeSet;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.object.brush.visualization.VirtualWorld;
import com.boydti.fawe.object.changeset.BlockBagChangeSet;
import com.boydti.fawe.object.changeset.DiskStorageHistory;
import com.boydti.fawe.object.changeset.FaweChangeSet;
import com.boydti.fawe.object.changeset.MemoryOptimizedHistory;
import com.boydti.fawe.object.extent.FaweRegionExtent;
import com.boydti.fawe.object.extent.MultiRegionExtent;
import com.boydti.fawe.object.extent.NullExtent;
import com.boydti.fawe.object.extent.SingleRegionExtent;
import com.boydti.fawe.object.extent.SlowExtent;
import com.boydti.fawe.object.extent.StripNBTExtent;
import com.boydti.fawe.wrappers.WorldWrapper;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.eventbus.EventBus;
import com.sk89q.worldedit.world.World;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

public class EditSessionBuilder {
    private World world;
    private String worldName;
    private Player player;
    private FaweLimit limit;
    private FaweChangeSet changeSet;
    private Region[] allowedRegions;
    private Boolean autoQueue;
    private Boolean fastmode;
    private Boolean checkMemory;
    private Boolean combineStages;
    private EventBus eventBus;
    private BlockBag blockBag;
    private boolean threaded = true;
    private EditSessionEvent event;

    /**
     * An EditSession builder<br>
     * - Unset values will revert to their default<br>
     * <br>
     * player: The player doing the edit (defaults to to null)<br>
     * limit: Block/Entity/Action limit (defaults to unlimited)<br>
     * changeSet: Stores changes (defaults to config.yml value)<br>
     * allowedRegions: Allowed editable regions (defaults to player's allowed regions, or everywhere)<br>
     * autoQueue: Changes can occur before flushQueue() (defaults true)<br>
     * fastmode: bypasses history (defaults to player fastmode or config.yml console history)<br>
     * checkMemory: If low memory checks are enabled (defaults to player's fastmode or true)<br>
     * combineStages: If history is combined with dispatching
     *
     * @param world A world must be provided for all EditSession(s)
     */
    public EditSessionBuilder(@Nonnull World world) {
        checkNotNull(world);
        this.world = world;
        this.worldName = Fawe.imp().getWorldName(world);
    }

    public EditSessionBuilder(World world, String worldName) {
        if (world == null && worldName == null) throw new NullPointerException("Both world and worldname cannot be null");
        this.world = world;
        this.worldName = worldName;
    }

    public EditSessionBuilder(@Nonnull String worldName) {
        checkNotNull(worldName);
        this.worldName = worldName;
        this.world = FaweAPI.getWorld(worldName);
    }

    public EditSessionBuilder player(@Nullable Player player) {
        this.player = player;
        return setDirty();
    }

    public EditSessionBuilder limit(@Nullable FaweLimit limit) {
        this.limit = limit;
        return setDirty();
    }

    public EditSessionBuilder limitUnlimited() {
        return limit(FaweLimit.MAX.copy());
    }

    public EditSessionBuilder limitUnprocessed(@Nonnull Player player) {
        checkNotNull(player);
        limitUnlimited();
        FaweLimit tmp = player.getLimit();
        limit.INVENTORY_MODE = tmp.INVENTORY_MODE;
        return setDirty();
    }

    public EditSessionBuilder changeSet(@Nullable FaweChangeSet changeSet) {
        this.changeSet = changeSet;
        return setDirty();
    }

    public EditSessionBuilder changeSetNull() {
        return changeSet(world == null ? new NullChangeSet(worldName) : new NullChangeSet(world));
    }

    public EditSessionBuilder world(@Nonnull World world) {
        checkNotNull(world);
        this.world = world;
        this.worldName = world.getName();
        return setDirty();
    }

    /**
     * @param disk If it should be stored on disk
     * @param uuid The uuid to store it under (if on disk)
     * @param compression Compression level (0-9)
     * @return
     */
    public EditSessionBuilder changeSet(boolean disk, @Nullable UUID uuid, int compression) {
        if (world == null) {
            if (disk) {
                if (Settings.IMP.HISTORY.USE_DATABASE) {
                    this.changeSet = new RollbackOptimizedHistory(worldName, uuid);
                } else {
                    this.changeSet = new DiskStorageHistory(worldName, uuid);
                }
            } else {
                this.changeSet = new MemoryOptimizedHistory(worldName);
            }
        } else if (disk) {
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

    public EditSessionBuilder checkMemory(@Nullable Boolean checkMemory) {
        this.checkMemory = checkMemory;
        return setDirty();
    }

    public EditSessionBuilder combineStages(@Nullable Boolean combineStages) {
        this.combineStages = combineStages;
        return setDirty();
    }

    public EditSessionBuilder blockBag(@Nullable BlockBag blockBag) {
        this.blockBag = blockBag;
        return setDirty();
    }

    public EditSessionBuilder eventBus(@Nullable EventBus eventBus) {
        this.eventBus = eventBus;
        return setDirty();
    }

    public EditSessionBuilder event(@Nullable EditSessionEvent event) {
        this.event = event;
        return setDirty();
    }

    private EditSessionBuilder setDirty() {
        compiled = false;
        return this;
    }

    private Extent wrapExtent(final Extent extent, final EventBus eventBus, EditSessionEvent event, final EditSession.Stage stage) {
        event = event.clone(stage);
        event.setExtent(extent);
        eventBus.post(event);
        if (event.isCancelled()) {
            return new NullExtent(extent, FaweCache.MANUAL);
        }
        final Extent toReturn = event.getExtent();
        if(toReturn instanceof com.sk89q.worldedit.extent.NullExtent) {
            return new NullExtent(toReturn, FaweCache.MANUAL);
        }
//        if (!(toReturn instanceof AbstractDelegateExtent)) {
//            Fawe.debug("Extent " + toReturn + " must be AbstractDelegateExtent");
//            return extent;
//        }
        if (toReturn != extent) {
            String className = toReturn.getClass().getName().toLowerCase();
            for (String allowed : Settings.IMP.EXTENT.ALLOWED_PLUGINS) {
                if (className.contains(allowed.toLowerCase())) {
                    this.wrapped = true;
                    return toReturn;
                }
            }
            if (Settings.IMP.EXTENT.DEBUG && event.getActor() != null) {
                event.getActor().printDebug("Potentially unsafe extent blocked: " + toReturn.getClass().getName());
                event.getActor().printDebug(" - For area restrictions, it is recommended to use the FaweAPI");
                event.getActor().printDebug(" - For block logging, it is recommended to use use BlocksHub");
                event.getActor().printDebug(" - To allow this plugin add it to the FAWE `allowed-plugins` list");
                event.getActor().printDebug(" - To hide this message set `debug` to false in the FAWE config.yml");
                if (toReturn.getClass().getName().contains("CoreProtect")) {
                    event.getActor().printDebug("Note on CoreProtect: ");
                    event.getActor().printDebug(" - If you disable CP's WE logger (CP config) and this still shows, please update CP");
                    event.getActor().printDebug(" - Use BlocksHub and set `debug` false in the FAWE config");
                }
            }
        }
        return extent;
    }

    private FaweChangeSet changeTask;
    private int maxY;
    private Extent bypassHistory;
    private Extent bypassAll;
    private Extent extent;
    private boolean compiled;
    private boolean wrapped;

    public EditSessionBuilder compile() {
        if (compiled) return this;

        compiled = true;
        wrapped = false;
        if (world == null && !this.worldName.isEmpty()) {
            world = FaweAPI.getWorld(this.worldName);
        }
        if (eventBus == null) {
            eventBus = WorldEdit.getInstance().getEventBus();
        }
        if (event == null) {
            event = new EditSessionEvent(world, player, -1, null);
        }
        if (player == null && event.getActor() != null) {
            player = (Player) event.getActor();
        }
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
        if (checkMemory == null) {
            checkMemory = player != null && !this.fastmode;
        }
        if (checkMemory) {
            if (MemUtil.isMemoryLimitedSlow()) {
                if (Permission.hasPermission(player, "worldedit.fast")) {
                    player.print(BBC.WORLDEDIT_OOM_ADMIN.s());
                }
                throw FaweCache.LOW_MEMORY;
            }
        }
//        this.originalLimit = limit;
        this.blockBag = limit.INVENTORY_MODE != 0 ? blockBag : null;
        this.limit = limit.copy();

        if (extent == null) {
            IQueueExtent queue = null;
            World unwrapped = WorldWrapper.unwrap(world);
            boolean placeChunks = this.fastmode || this.limit.FAST_PLACEMENT;

            if (placeChunks) {
                if (unwrapped instanceof IQueueExtent) {
                    extent = queue = (IQueueExtent) unwrapped;
                } else if (Settings.IMP.QUEUE.PARALLEL_THREADS > 1 && threaded) {
                    ParallelQueueExtent parallel = new ParallelQueueExtent(Fawe.get().getQueueHandler(), world);
                    queue = parallel.getExtent();
                    extent = parallel;
                } else {
                    System.out.println("FAWE is in single threaded mode (performance reduced)");
                    extent = queue = Fawe.get().getQueueHandler().getQueue(world);
                }
            } else {
                extent = world;
            }
            Extent root = extent;
            if (combineStages == null) {
                combineStages =
                        // If it's enabled in the settings
                        Settings.IMP.HISTORY.COMBINE_STAGES
                                // If fast placement is disabled, it's slower to perform a copy on each chunk
                                && this.limit.FAST_PLACEMENT
                                // If the edit uses items from the inventory we can't use a delayed task
                                && this.blockBag == null;
            }
            if (!Settings.IMP.QUEUE.PROGRESS.DISPLAY.equalsIgnoreCase("false") && player != null) {
                System.out.println("TODO add progress display");
//                switch (Settings.IMP.QUEUE.PROGRESS.DISPLAY.toLowerCase()) {
//                    case "chat":
//                        this.queue.setProgressTask(new ChatProgressTracker(player));
//                        break;
//                    case "title":
//                    case "true":
//                    default:
//                        this.queue.setProgressTask(new DefaultProgressTracker(player));
//                }
            }
            extent = this.bypassAll = wrapExtent(extent, eventBus, event, EditSession.Stage.BEFORE_CHANGE);
            this.bypassHistory = (this.extent = wrapExtent(bypassAll, eventBus, event, EditSession.Stage.BEFORE_REORDER));
            if (!this.fastmode || changeSet != null) {
                if (changeSet == null) {
                    if (Settings.IMP.HISTORY.USE_DISK) {
                        UUID uuid = player == null ? EditSession.CONSOLE : player.getUniqueId();
                        if (Settings.IMP.HISTORY.USE_DATABASE) {
                            changeSet = new RollbackOptimizedHistory(world, uuid);
                        } else {
                            changeSet = new DiskStorageHistory(world, uuid);
                        }
//                    } else if (combineStages && Settings.IMP.HISTORY.COMPRESSION_LEVEL == 0) {
//                        changeSet = new CPUOptimizedChangeSet(world);
                    } else {
                        if (combineStages && Settings.IMP.HISTORY.COMPRESSION_LEVEL == 0) {
                            System.out.println("TODO add CPUOptimizedChangeSet");
                        }
                        changeSet = new MemoryOptimizedHistory(world);
                    }
                }
                if (this.limit.SPEED_REDUCTION > 0) {
                    this.extent = this.bypassHistory = new SlowExtent(this.bypassHistory, this.limit.SPEED_REDUCTION);
                }
                if (changeSet instanceof NullChangeSet && Fawe.imp().getBlocksHubApi() != null && player != null) {
                    changeSet = LoggingChangeSet.wrap(player, changeSet);
                }
                if (!(changeSet instanceof NullChangeSet)) {
                    if (!(changeSet instanceof LoggingChangeSet) && player != null && Fawe.imp().getBlocksHubApi() != null) {
                        changeSet = LoggingChangeSet.wrap(player, changeSet);
                    }
                    if (this.blockBag != null) {
                        System.out.println("TODO implement block bag as IBatchProcessor");
                        changeSet = new BlockBagChangeSet(changeSet, blockBag, limit.INVENTORY_MODE == 1);
                    }
                    if (combineStages) {
                        changeTask = changeSet;
                        this.extent = extent.enableHistory(changeSet);
                    } else {
                        this.extent = (new HistoryExtent(extent, changeSet));
//                        if (this.blockBag != null) {
//                            this.extent = new BlockBagExtent(this.extent, blockBag, limit.INVENTORY_MODE == 1);
//                        }
                    }
                }
            }
            if (allowedRegions == null) {
                if (player != null && !player.hasPermission("fawe.bypass") && !player.hasPermission("fawe.bypass.regions") && !(root instanceof VirtualWorld)) {
                    allowedRegions = player.getCurrentRegions();
                }
            }
            this.maxY = world == null ? 255 : world.getMaxY();
            FaweRegionExtent regionExtent = null;
            if (allowedRegions != null) {
                if (allowedRegions.length == 0) {
                    regionExtent = new NullExtent(this.extent, FaweCache.NO_REGION);
                } else {
//                    this.extent = new ProcessedWEExtent(this.extent, this.limit);
                    if (allowedRegions.length == 1) {
                        regionExtent = new SingleRegionExtent(this.extent, this.limit, allowedRegions[0]);
                    } else {
                        regionExtent = new MultiRegionExtent(this.extent, this.limit, allowedRegions);
                    }
                }
            } else {
//                this.extent = new HeightBoundExtent(this.extent, this.limit, 0, maxY);
            }
            IBatchProcessor limitProcessor = regionExtent;
            if (limit != null && !limit.isUnlimited()) {
                limitProcessor = new LimitProcessor(limit, limitProcessor);
            }
            if (regionExtent != null && queue != null && combineStages) {
                queue.addProcessor(limitProcessor);
            } else if (regionExtent != null) {
                this.extent = limitProcessor.construct(regionExtent.getExtent());
            }
            if (this.limit.STRIP_NBT != null && !this.limit.STRIP_NBT.isEmpty()) {
                System.out.println("TODO add batch processor for strip nbt");
                this.extent = new StripNBTExtent(this.extent, this.limit.STRIP_NBT);
            }
            this.extent = wrapExtent(this.extent, eventBus, event, EditSession.Stage.BEFORE_HISTORY);
        }
        return this;
    }

    public EditSession build() {
        if (eventBus == null) {
            eventBus = WorldEdit.getInstance().getEventBus();
        }
        return new EditSession(this);
    }

    public World getWorld() {
        return world;
    }

    public String getWorldName() {
        return worldName;
    }

    public Extent getExtent() {
        return extent != null ? extent : world;
    }

    public boolean isWrapped() {
        return wrapped;
    }

    public boolean hasFastMode() {
        return fastmode;
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

    public Player getPlayer() {
        return player;
    }

    public FaweChangeSet getChangeTask() {
        return changeTask;
    }

    public BlockBag getBlockBag() {
        return blockBag;
    }

    public int getMaxY() {
        return maxY;
    }
}
