package com.boydti.fawe.util;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.logging.rollback.RollbackOptimizedHistory;
import com.boydti.fawe.object.*;
import com.boydti.fawe.object.changeset.DiskStorageHistory;
import com.boydti.fawe.object.changeset.FaweChangeSet;
import com.boydti.fawe.object.changeset.MemoryOptimizedHistory;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
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
        this.worldName = world.getName();
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
        this.worldName = world.getName();
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

    public EditSession build() {
            return new EditSession(worldName, world, queue, player, limit, changeSet, allowedRegions, autoQueue, fastmode, checkMemory, combineStages, blockBag, eventBus, event);
    }
}
