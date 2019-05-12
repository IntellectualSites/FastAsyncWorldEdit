package com.boydti.fawe.util;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.jnbt.anvil.MCAQueue;
import com.boydti.fawe.jnbt.anvil.MCAWorld;
import com.boydti.fawe.logging.LoggingChangeSet;
import com.boydti.fawe.logging.rollback.RollbackOptimizedHistory;
import com.boydti.fawe.object.*;
import com.boydti.fawe.object.brush.visualization.VirtualWorld;
import com.boydti.fawe.object.changeset.BlockBagChangeSet;
import com.boydti.fawe.object.changeset.CPUOptimizedChangeSet;
import com.boydti.fawe.object.changeset.DiskStorageHistory;
import com.boydti.fawe.object.changeset.FaweChangeSet;
import com.boydti.fawe.object.changeset.MemoryOptimizedHistory;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.object.extent.FastWorldEditExtent;
import com.boydti.fawe.object.extent.HeightBoundExtent;
import com.boydti.fawe.object.extent.MultiRegionExtent;
import com.boydti.fawe.object.extent.NullExtent;
import com.boydti.fawe.object.extent.ProcessedWEExtent;
import com.boydti.fawe.object.extent.SingleRegionExtent;
import com.boydti.fawe.object.extent.SlowExtent;
import com.boydti.fawe.object.extent.StripNBTExtent;
import com.boydti.fawe.object.progress.ChatProgressTracker;
import com.boydti.fawe.object.progress.DefaultProgressTracker;
import com.boydti.fawe.wrappers.WorldWrapper;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.eventbus.EventBus;
import com.sk89q.worldedit.world.World;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

public class EditSessionBuilder {
    private World world;
    private String worldName;
    private FaweQueue queue;
    private FawePlayer player;
    private FaweLimit limit;
    private FaweChangeSet changeSet;
    private Region[] allowedRegions;
    private Boolean autoQueue;
    private Boolean fastmode;
    private Boolean checkMemory;
    private Boolean combineStages;
    private EventBus eventBus;
    private BlockBag blockBag;
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

    public EditSessionBuilder(@Nonnull String worldName) {
        checkNotNull(worldName);
        this.worldName = worldName;
        this.world = FaweAPI.getWorld(worldName);
    }

    public EditSessionBuilder player(@Nullable FawePlayer player) {
        this.player = player;
        return this;
    }

    public EditSessionBuilder limit(@Nullable FaweLimit limit) {
        this.limit = limit;
        return this;
    }

    public EditSessionBuilder limitUnlimited() {
        return limit(FaweLimit.MAX.copy());
    }

    public EditSessionBuilder limitUnprocessed(@Nonnull FawePlayer fp) {
        checkNotNull(fp);
        limitUnlimited();
        FaweLimit tmp = fp.getLimit();
        limit.INVENTORY_MODE = tmp.INVENTORY_MODE;
        return this;
    }

    public EditSessionBuilder changeSet(@Nullable FaweChangeSet changeSet) {
        this.changeSet = changeSet;
        return this;
    }

    public EditSessionBuilder changeSetNull() {
        return changeSet(world == null ? new NullChangeSet(worldName) : new NullChangeSet(world));
    }

    public EditSessionBuilder world(@Nonnull World world) {
        checkNotNull(world);
        this.world = world;
        this.worldName = Fawe.imp().getWorldName(world);
        return this;
    }

    /**
     * @param disk        If it should be stored on disk
     * @param uuid        The uuid to store it under (if on disk)
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
        return this;
    }

    public EditSessionBuilder allowedRegions(@Nullable Region[] allowedRegions) {
        this.allowedRegions = allowedRegions;
        return this;
    }

    @Deprecated
    public EditSessionBuilder allowedRegions(@Nullable RegionWrapper[] allowedRegions) {
        this.allowedRegions = allowedRegions;
        return this;
    }

    public EditSessionBuilder allowedRegions(@Nullable RegionWrapper allowedRegion) {
        this.allowedRegions = allowedRegion == null ? null : allowedRegion.toArray();
        return this;
    }

    public EditSessionBuilder allowedRegionsEverywhere() {
        return allowedRegions(new Region[]{RegionWrapper.GLOBAL()});
    }

    public EditSessionBuilder autoQueue(@Nullable Boolean autoQueue) {
        this.autoQueue = autoQueue;
        return this;
    }

    public EditSessionBuilder fastmode(@Nullable Boolean fastmode) {
        this.fastmode = fastmode;
        return this;
    }

    public EditSessionBuilder checkMemory(@Nullable Boolean checkMemory) {
        this.checkMemory = checkMemory;
        return this;
    }

    public EditSessionBuilder combineStages(@Nullable Boolean combineStages) {
        this.combineStages = combineStages;
        return this;
    }

    public EditSessionBuilder queue(@Nullable FaweQueue queue) {
        this.queue = queue;
        return this;
    }

    public EditSessionBuilder blockBag(@Nullable BlockBag blockBag) {
        this.blockBag = blockBag;
        return this;
    }

    public EditSessionBuilder eventBus(@Nullable EventBus eventBus) {
        this.eventBus = eventBus;
        return this;
    }

    public EditSessionBuilder event(@Nullable EditSessionEvent event) {
        this.event = event;
        return this;
    }

    private boolean wrapped;

    private AbstractDelegateExtent wrapExtent(final AbstractDelegateExtent extent, final EventBus eventBus, EditSessionEvent event, final EditSession.Stage stage) {
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
            return extent;
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
        return extent;
    }

    public EditSessionBuilder commit() {
        // reset
        wrapped = false;
        //
        if (worldName == null) {
            if (world == null) {
                if (queue == null) {
                    worldName = "";
                } else {
                    worldName = queue.getWorldName();
                }
            } else {
                worldName = Fawe.imp().getWorldName(world);
            }
        }
        if (world == null && !this.worldName.isEmpty()) {
            world = FaweAPI.getWorld(this.worldName);
        }
        if (eventBus == null) {
            eventBus = WorldEdit.getInstance().getEventBus();
        }
        if (event == null) {
            event = new EditSessionEvent(world, player == null ? null : (player.getPlayer()), -1, null);
        }
        if (player == null && event.getActor() != null) {
            player = FawePlayer.wrap(event.getActor());
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
                if (Perm.hasPermission(player, "worldedit.fast")) {
                    BBC.WORLDEDIT_OOM_ADMIN.send(player);
                }
                throw new FaweException(BBC.WORLDEDIT_CANCEL_REASON_LOW_MEMORY);
            }
        }
//        this.originalLimit = limit;
        this.blockBag = limit.INVENTORY_MODE != 0 ? blockBag : null;
//        this.limit = limit.copy();

        if (queue == null) {
            boolean placeChunks = this.fastmode || this.limit.FAST_PLACEMENT;
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
//        if (Settings.IMP.EXPERIMENTAL.ANVIL_QUEUE_MODE && !(queue instanceof MCAQueue)) {
//            queue = new MCAQueue(queue);
//        }
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
//        this.bypassAll = wrapExtent(new FastWorldEditExtent(world, queue), eventBus, event, EditSession.Stage.BEFORE_CHANGE);
//        this.bypassHistory = (this.extent = wrapExtent(bypassAll, eventBus, event, EditSession.Stage.BEFORE_REORDER));
//        if (!this.fastMode || changeSet != null) {
//            if (changeSet == null) {
//                if (Settings.IMP.HISTORY.USE_DISK) {
//                    UUID uuid = player == null ? EditSession.CONSOLE : player.getUUID();
//                    if (Settings.IMP.HISTORY.USE_DATABASE) {
//                        changeSet = new RollbackOptimizedHistory(world, uuid);
//                    } else {
//                        changeSet = new DiskStorageHistory(world, uuid);
//                    }
//                } else if (combineStages && Settings.IMP.HISTORY.COMPRESSION_LEVEL == 0 && !(queue instanceof MCAQueue)) {
//                    changeSet = new CPUOptimizedChangeSet(world);
//                } else {
//                    changeSet = new MemoryOptimizedHistory(world);
//                }
//            }
//            if (this.limit.SPEED_REDUCTION > 0) {
//                this.bypassHistory = new SlowExtent(this.bypassHistory, this.limit.SPEED_REDUCTION);
//            }
//            if (changeSet instanceof NullChangeSet && Fawe.imp().getBlocksHubApi() != null && player != null) {
//                changeSet = LoggingChangeSet.wrap(player, changeSet);
//            }
//            if (!(changeSet instanceof NullChangeSet)) {
//                if (!(changeSet instanceof LoggingChangeSet) && player != null && Fawe.imp().getBlocksHubApi() != null) {
//                    changeSet = LoggingChangeSet.wrap(player, changeSet);
//                }
//                if (this.blockBag != null) {
//                    changeSet = new BlockBagChangeSet(changeSet, blockBag, limit.INVENTORY_MODE == 1);
//                }
//                if (combineStages) {
//                    changeTask = changeSet;
//                    changeSet.addChangeTask(queue);
//                } else {
//                    this.extent = (history = new HistoryExtent(this, bypassHistory, changeSet, queue));
////                    if (this.blockBag != null) {
////                        this.extent = new BlockBagExtent(this.extent, blockBag, limit.INVENTORY_MODE == 1);
////                    }
//                }
//            }
//        }
//        if (allowedRegions == null) {
//            if (player != null && !player.hasPermission("fawe.bypass") && !player.hasPermission("fawe.bypass.regions") && !(queue instanceof VirtualWorld)) {
//                allowedRegions = player.getCurrentRegions();
//            }
//        }
//        this.maxY = getWorld() == null ? 255 : world.getMaxY();
//        if (allowedRegions != null) {
//            if (allowedRegions.length == 0) {
//                this.extent = new NullExtent(this.extent, BBC.WORLDEDIT_CANCEL_REASON_NO_REGION);
//            } else {
//                this.extent = new ProcessedWEExtent(this.extent, this.limit);
//                if (allowedRegions.length == 1) {
//                    Region region = allowedRegions[0];
//                    this.extent = new SingleRegionExtent(this.extent, this.limit, allowedRegions[0]);
//                } else {
//                    this.extent = new MultiRegionExtent(this.extent, this.limit, allowedRegions);
//                }
//            }
//        } else {
//            this.extent = new HeightBoundExtent(this.extent, this.limit, 0, maxY);
//        }
//        if (this.limit.STRIP_NBT != null && !this.limit.STRIP_NBT.isEmpty()) {
//            this.extent = new StripNBTExtent(this.extent, this.limit.STRIP_NBT);
//        }
//        this.extent = wrapExtent(this.extent, bus, event, Stage.BEFORE_HISTORY);
        return this;
    }

    public EditSession build() {
        return new EditSession(worldName, world, queue, player, limit, changeSet, allowedRegions, autoQueue, fastmode, checkMemory, combineStages, blockBag, eventBus, event);
    }
}
