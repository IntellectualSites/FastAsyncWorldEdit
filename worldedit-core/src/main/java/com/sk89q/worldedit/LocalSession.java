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
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweInputStream;
import com.boydti.fawe.object.FaweLimit;
import com.boydti.fawe.object.FaweOutputStream;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.brush.visualization.VirtualWorld;
import com.boydti.fawe.object.changeset.AnvilHistory;
import com.boydti.fawe.object.changeset.DiskStorageHistory;
import com.boydti.fawe.object.changeset.FaweChangeSet;
import com.boydti.fawe.object.clipboard.MultiClipboardHolder;
import com.boydti.fawe.object.collection.SparseBitSet;
import com.boydti.fawe.object.extent.ResettableExtent;
import com.boydti.fawe.util.*;
import com.boydti.fawe.wrappers.WorldWrapper;
import com.sk89q.jchronic.Chronic;
import com.sk89q.jchronic.Options;
import com.sk89q.jchronic.utils.Span;
import com.sk89q.jchronic.utils.Time;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.command.tool.*;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.ChangeSetExecutor;
import com.sk89q.worldedit.history.changeset.ChangeSet;
import com.sk89q.worldedit.internal.cui.CUIEvent;
import com.sk89q.worldedit.internal.cui.CUIRegion;
import com.sk89q.worldedit.internal.cui.SelectionShapeEvent;
import com.sk89q.worldedit.internal.cui.ServerCUIHandler;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.regions.selector.RegionSelectorType;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.session.request.Request;
import com.sk89q.worldedit.util.HandSide;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.item.ItemTypes;
import com.sk89q.worldedit.world.snapshot.Snapshot;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkNotNull;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Stores session information.
 */
public class LocalSession implements TextureHolder {

    public transient static int MAX_HISTORY_SIZE = 15;

    // Non-session related fields
    private transient LocalConfiguration config;
    private transient final AtomicBoolean dirty = new AtomicBoolean();
    private transient int failedCuiAttempts = 0;

    // Session related
    private transient RegionSelector selector = new CuboidRegionSelector();
    private transient boolean placeAtPos1 = false;
    private transient List<Object> history = Collections.synchronizedList(new LinkedList<Object>() {
        @Override
        public Object get(int index) {
            Object value = super.get(index);
            if (value instanceof Integer) {
                value = getChangeSet(value);
                set(index, value);
            }
            return value;
        }

        @Override
        public Object remove(int index) {
            return getChangeSet(super.remove(index));
        }
    });
    private transient volatile Integer historyNegativeIndex;
    private transient ClipboardHolder clipboard;
    private transient boolean toolControl = true;
    private transient boolean superPickaxe = false;
    private transient BlockTool pickaxeMode = new SinglePickaxe();
    private transient boolean hasTool = false;
    private transient Tool[] tools = new Tool[ItemTypes.size()];
    private transient int maxBlocksChanged = -1;
    private transient int maxTimeoutTime;
    private transient boolean useInventory;
    private transient Snapshot snapshot;
    private transient boolean hasCUISupport = false;
    private transient int cuiVersion = -1;
    private transient boolean fastMode = false;
    private transient Mask mask;
    private transient Mask sourceMask;
    private transient TextureUtil texture;
    private transient ResettableExtent transform = null;
    private transient TimeZone timezone = TimeZone.getDefault();
    private transient World currentWorld;
    private transient UUID uuid;
    private transient volatile long historySize = 0;

    private transient VirtualWorld virtual;
    private transient BlockVector3 cuiTemporaryBlock;

    // Saved properties
    private String lastScript;
    private RegionSelectorType defaultSelector;
    private boolean useServerCUI = false; // Save this to not annoy players.

    /**
     * Construct the object.
     *
     * <p>{@link #setConfiguration(LocalConfiguration)} should be called
     * later with configuration.</p>
     */
    public LocalSession() {
    }

    /**
     * Construct the object.
     *
     * @param config the configuration
     */
    public LocalSession(@Nullable LocalConfiguration config) {
        this.config = config;
    }

    /**
     * Set the configuration.
     *
     * @param config the configuration
     */
    public void setConfiguration(LocalConfiguration config) {
        checkNotNull(config);
        this.config = config;
    }

    /**
     * Called on post load of the session from persistent storage.
     */
    public void postLoad() {
        if (defaultSelector != null) {
            this.selector = defaultSelector.createSelector();
        }
    }

    /**
     * @param uuid
     * @param world
     * @return If any loading occured
     */
    public boolean loadSessionHistoryFromDisk(UUID uuid, World world) {
        if (world == null || uuid == null) {
            return false;
        }
        if (Settings.IMP.HISTORY.USE_DISK) {
            MAX_HISTORY_SIZE = Integer.MAX_VALUE;
        }
        world = WorldWrapper.unwrap(world);
        if (!world.equals(currentWorld)) {
            this.uuid = uuid;
            // Save history
            saveHistoryNegativeIndex(uuid, currentWorld);
            history.clear();
            currentWorld = world;
            // Load history
            if (loadHistoryChangeSets(uuid, currentWorld)) {
                loadHistoryNegativeIndex(uuid, currentWorld);
                return true;
            }
            historyNegativeIndex = 0;
        }
        return false;
    }

    private boolean loadHistoryChangeSets(UUID uuid, World world) {
        SparseBitSet set = new SparseBitSet();
        final File folder = MainUtil.getFile(Fawe.imp().getDirectory(), Settings.IMP.PATHS.HISTORY + File.separator + world.getName() + File.separator + uuid);
        if (folder.isDirectory()) {
            folder.listFiles(pathname -> {
                String name = pathname.getName();
                Integer val = null;
                if (pathname.isDirectory()) {
                    val = StringMan.toInteger(name, 0, name.length());

                } else {
                    int i = name.lastIndexOf('.');
                    if (i != -1) val = StringMan.toInteger(name, 0, i);
                }
                if (val != null) set.set(val);
                return false;
            });
        }
        if (!set.isEmpty()) {
            historySize = MainUtil.getTotalSize(folder.toPath());
            for (int index = set.nextSetBit(0); index != -1; index = set.nextSetBit(index + 1)) {
                history.add(index);
            }
        } else {
            historySize = 0;
        }
        return !set.isEmpty();
    }

    private void loadHistoryNegativeIndex(UUID uuid, World world) {
        if (!Settings.IMP.HISTORY.USE_DISK) {
            return;
        }
        File file = MainUtil.getFile(Fawe.imp().getDirectory(), Settings.IMP.PATHS.HISTORY + File.separator + world.getName() + File.separator + uuid + File.separator + "index");
        if (file.exists()) {
            try (FaweInputStream is = new FaweInputStream(new FileInputStream(file))) {
                historyNegativeIndex = Math.min(Math.max(0, is.readInt()), history.size());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            historyNegativeIndex = 0;
        }
    }

    private void saveHistoryNegativeIndex(UUID uuid, World world) {
        if (world == null || !Settings.IMP.HISTORY.USE_DISK) {
            return;
        }
        File file = MainUtil.getFile(Fawe.imp().getDirectory(), Settings.IMP.PATHS.HISTORY + File.separator + world.getName() + File.separator + uuid + File.separator + "index");
        if (getHistoryNegativeIndex() != 0) {
            try {
                if (!file.exists()) {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                }
                try (FaweOutputStream os = new FaweOutputStream(new FileOutputStream(file))) {
                    os.writeInt(getHistoryNegativeIndex());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (file.exists()) {
            file.delete();
        }
    }

    /**
     * Get whether this session is "dirty" and has changes that needs to
     * be committed.
     *
     * @return true if dirty
     */
    public boolean isDirty() {
        return dirty.get();
    }

    /**
     * Set this session as dirty.
     */
    private void setDirty() {
        dirty.set(true);
    }

    public int getHistoryIndex() {
        return history.size() - 1 - (historyNegativeIndex == null ? 0 : historyNegativeIndex);
    }

    public int getHistoryNegativeIndex() {
        return (historyNegativeIndex == null ? historyNegativeIndex = 0 : historyNegativeIndex);
    }

    public void setHistoryIndex(int value) {
        historyNegativeIndex = history.size() - value - 1;
    }

    public boolean save() {
        saveHistoryNegativeIndex(uuid, currentWorld);
        if (defaultSelector == RegionSelectorType.CUBOID) {
            defaultSelector = null;
        }
        if (lastScript != null || defaultSelector != null) {
            return true;
        }
        return false;
    }

    /**
     * Get whether this session is "dirty" and has changes that needs to
     * be committed, and reset it to {@code false}.
     *
     * @return true if the dirty value was {@code true}
     */
    public boolean compareAndResetDirty() {
        return dirty.compareAndSet(true, false);
    }

    /**
     * Get the session's timezone.
     *
     * @return the timezone
     */
    public TimeZone getTimeZone() {
        return timezone;
    }

    /**
     * Set the session's timezone.
     *
     * @param timezone the user's timezone
     */
    public void setTimezone(TimeZone timezone) {
        checkNotNull(timezone);
        this.timezone = timezone;
    }

    /**
     * Clear history.
     */
    public void clearHistory() {
        history.clear();
        historyNegativeIndex = 0;
        historySize = 0;
        currentWorld = null;
    }

    /**
     * Remember an edit session for the undo history. If the history maximum
     * size is reached, old edit sessions will be discarded.
     *
     * @param editSession the edit session
     */
    public void remember(EditSession editSession) {
        checkNotNull(editSession);

        FawePlayer fp = editSession.getPlayer();
        int limit = fp == null ? Integer.MAX_VALUE : fp.getLimit().MAX_HISTORY;
        remember(editSession, true, limit);
    }

    private FaweChangeSet getChangeSet(Object o) {
        if (o instanceof FaweChangeSet) {
            FaweChangeSet cs = (FaweChangeSet) o;
            cs.close();
            return cs;
        }
        if (o instanceof Integer) {
            File folder = MainUtil.getFile(Fawe.imp().getDirectory(), Settings.IMP.PATHS.HISTORY + File.separator + currentWorld.getName() + File.separator + uuid);
            File specific = new File(folder, o.toString());
            if (specific.isDirectory()) {
                return new AnvilHistory(currentWorld.getName(), specific);
            } else {
                return new DiskStorageHistory(currentWorld, this.uuid, (Integer) o);
            }
        }
        return null;
    }

    public synchronized void remember(Player player, World world, ChangeSet changeSet, FaweLimit limit) {
        if (Settings.IMP.HISTORY.USE_DISK) {
            LocalSession.MAX_HISTORY_SIZE = Integer.MAX_VALUE;
        }
        if (changeSet.size() == 0) {
            return;
        }
        loadSessionHistoryFromDisk(player.getUniqueId(), world);
        if (changeSet instanceof FaweChangeSet) {
            int size = getHistoryNegativeIndex();
            ListIterator<Object> iter = history.listIterator();
            int i = 0;
            int cutoffIndex = history.size() - getHistoryNegativeIndex();
            while (iter.hasNext()) {
                Object item = iter.next();
                if (++i > cutoffIndex) {
                    FaweChangeSet oldChangeSet;
                    if (item instanceof FaweChangeSet) {
                        oldChangeSet = (FaweChangeSet) item;
                    } else {
                        oldChangeSet = getChangeSet(item);
                    }
                    historySize -= MainUtil.getSize(oldChangeSet);
                    iter.remove();
                }
            }
        }
        historySize += MainUtil.getSize(changeSet);
        history.add(changeSet);
        if (getHistoryNegativeIndex() != 0) {
            setDirty();
            historyNegativeIndex = 0;
        }
        if (limit != null) {
            int limitMb = limit.MAX_HISTORY;
            while (((!Settings.IMP.HISTORY.USE_DISK && history.size() > MAX_HISTORY_SIZE) || (historySize >> 20) > limitMb) && history.size() > 1) {
                FaweChangeSet item = (FaweChangeSet) history.remove(0);
                item.delete();
                long size = MainUtil.getSize(item);
                historySize -= size;
            }
        }
    }

    public synchronized void remember(final EditSession editSession, final boolean append, int limitMb) {
        if (Settings.IMP.HISTORY.USE_DISK) {
            LocalSession.MAX_HISTORY_SIZE = Integer.MAX_VALUE;
        }
        // It should have already been flushed, but just in case!
        editSession.flushQueue();
        if (editSession.getChangeSet() == null || limitMb == 0 || historySize >> 20 > limitMb && !append) {
            return;
        }
        // Don't store anything if no changes were made
        if (editSession.size() == 0) {
            return;
        }
        FaweChangeSet changeSet = (FaweChangeSet) editSession.getChangeSet();
        if (changeSet.isEmpty()) {
            return;
        }

        FawePlayer fp = editSession.getPlayer();
        if (fp != null) {
            loadSessionHistoryFromDisk(fp.getUUID(), editSession.getWorld());
        }
        // Destroy any sessions after this undo point
        if (append) {
            int size = getHistoryNegativeIndex();
            ListIterator<Object> iter = history.listIterator();
            int i = 0;
            int cutoffIndex = history.size() - getHistoryNegativeIndex();
            while (iter.hasNext()) {
                Object item = iter.next();
                if (++i > cutoffIndex) {
                    FaweChangeSet oldChangeSet;
                    if (item instanceof FaweChangeSet) {
                        oldChangeSet = (FaweChangeSet) item;
                    } else {
                        oldChangeSet = getChangeSet(item);
                    }
                    historySize -= MainUtil.getSize(oldChangeSet);
                    iter.remove();
                }
            }
        }

        historySize += MainUtil.getSize(changeSet);
        if (append) {
            history.add(changeSet);
            if (getHistoryNegativeIndex() != 0) {
                setDirty();
                historyNegativeIndex = 0;
            }
        } else {
            history.add(0, changeSet);
        }
        while (((!Settings.IMP.HISTORY.USE_DISK && history.size() > MAX_HISTORY_SIZE) || (historySize >> 20) > limitMb) && history.size() > 1) {
            FaweChangeSet item = (FaweChangeSet) history.remove(0);
            item.delete();
            long size = MainUtil.getSize(item);
            historySize -= size;
        }
    }

    /**
     * Performs an undo.
     *
     * @param newBlockBag a new block bag
     * @param player the player
     * @return whether anything was undone
     */
    public EditSession undo(@Nullable BlockBag newBlockBag, Player player) {
        checkNotNull(player);
        FawePlayer fp = FawePlayer.wrap(player);
        loadSessionHistoryFromDisk(player.getUniqueId(), fp.getWorldForEditing());
        if (getHistoryNegativeIndex() < history.size()) {
            FaweChangeSet changeSet = getChangeSet(history.get(getHistoryIndex()));
            EditSession newEditSession = new EditSessionBuilder(changeSet.getWorld())
                    .allowedRegionsEverywhere()
                    .checkMemory(false)
                    .changeSetNull()
                    .fastmode(false)
                    .limitUnprocessed(fp)
                    .player(fp)
                    .blockBag(getBlockBag(player))
                    .build();
            newEditSession.setBlocks(changeSet, ChangeSetExecutor.Type.UNDO);
            setDirty();
            historyNegativeIndex++;
            return newEditSession;
        } else {
            int size = history.size();
            if (getHistoryNegativeIndex() != size) {
                historyNegativeIndex = history.size();
                setDirty();
            }
            return null;
        }
    }

    /**
     * Performs a redo
     *
     * @param newBlockBag a new block bag
     * @param player the player
     * @return whether anything was redone
     */
    public EditSession redo(@Nullable BlockBag newBlockBag, Player player) {
        checkNotNull(player);
        FawePlayer fp = FawePlayer.wrap(player);
        loadSessionHistoryFromDisk(player.getUniqueId(), fp.getWorldForEditing());
        if (getHistoryNegativeIndex() > 0) {
            setDirty();
            historyNegativeIndex--;
            FaweChangeSet changeSet = getChangeSet(history.get(getHistoryIndex()));
            EditSession newEditSession = new EditSessionBuilder(changeSet.getWorld())
                    .allowedRegionsEverywhere()
                    .checkMemory(false)
                    .changeSetNull()
                    .fastmode(false)
                    .limitUnprocessed(fp)
                    .player(fp)
                    .blockBag(getBlockBag(player))
                    .build();
            newEditSession.setBlocks(changeSet, ChangeSetExecutor.Type.REDO);
            return newEditSession;
        }

        return null;
    }

    public void unregisterTools(Player player) {
        for (Tool tool : tools) {
            if (tool instanceof BrushTool) {
                ((BrushTool) tool).clear(player);
            }
        }
    }

    public int getSize() {
        return history.size();
    }

    /**
     * Get the default region selector.
     *
     * @return the default region selector
     */
    public RegionSelectorType getDefaultRegionSelector() {
        return defaultSelector;
    }

    /**
     * Set the default region selector.
     *
     * @param defaultSelector the default region selector
     */
    public void setDefaultRegionSelector(RegionSelectorType defaultSelector) {
        checkNotNull(defaultSelector);
        this.defaultSelector = defaultSelector;
        setDirty();
    }

    /**
     * Get the region selector for defining the selection. If the selection
     * was defined for a different world, the old selection will be discarded.
     *
     * @param world the world
     * @return position the position
     */
    public RegionSelector getRegionSelector(World world) {
        checkNotNull(world);
        try {
            if (selector.getWorld() == null || !selector.getWorld().equals(world)) {
                selector.setWorld(world);
                selector.clear();
            }
        } catch (Throwable ignore) {
            selector.clear();
        }
        return selector;
    }

    /**
     * Set the region selector.
     *
     * @param world the world
     * @param selector the selector
     */
    public void setRegionSelector(World world, RegionSelector selector) {
        checkNotNull(world);
        checkNotNull(selector);
        selector.setWorld(world);
        this.selector = selector;
    }

    /**
     * Returns true if the region is fully defined for the specified world.
     *
     * @param world the world
     * @return true if a region selection is defined
     */
    public boolean isSelectionDefined(World world) {
        checkNotNull(world);
        if (selector.getIncompleteRegion().getWorld() == null || !selector.getIncompleteRegion().getWorld().equals(world)) {
            return false;
        }
        return selector.isDefined();
    }

    /**
     * Get the selection region. If you change the region, you should
     * call learnRegionChanges().  If the selection is defined in
     * a different world, the {@code IncompleteRegionException}
     * exception will be thrown.
     *
     * @param world the world
     * @return a region
     * @throws IncompleteRegionException if no region is selected
     */
    public Region getSelection(World world) throws IncompleteRegionException {
        checkNotNull(world);
        if (selector.getIncompleteRegion().getWorld() == null || !selector.getIncompleteRegion().getWorld().equals(world)) {
            throw new IncompleteRegionException();
        }
        return selector.getRegion();
    }

    public @Nullable VirtualWorld getVirtualWorld() {
        synchronized (dirty) {
            return virtual;
        }
    }

    public void setVirtualWorld(@Nullable VirtualWorld world) {
        VirtualWorld tmp;
        synchronized (dirty) {
            tmp = this.virtual;
            this.virtual = world;
        }
        if (tmp != null) {
            try {
                tmp.close(world == null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (world != null) {
            Fawe.imp().registerPacketListener();
            world.update();
        }
    }

    /**
     * Get the selection world.
     *
     * @return the the world of the selection
     */
    public World getSelectionWorld() {
        World world = selector.getIncompleteRegion().getWorld();
        if (world instanceof WorldWrapper) {
            return ((WorldWrapper) world).getParent();
        }
        return world;
    }

    /**
     * Get the world selection.
     *
     * @return the current selection
     */
    public Region getWorldSelection() throws IncompleteRegionException {
        return getSelection(getSelectionWorld());
    }

    /**
     * This is an alias for {@link #getSelection(World)}.
     * It enables CraftScripts to get a world selection as it is
     * not possible to use getSelection which have two default
     * implementations.
     *
     * @return Get the selection region in the world.
     */
    public Region getWorldSelection(World world) throws IncompleteRegionException {
        return getSelection(world);
    }

    /**
     * Gets the clipboard.
     *
     * @return clipboard
     * @throws EmptyClipboardException thrown if no clipboard is set
     */
    public ClipboardHolder getClipboard() throws EmptyClipboardException {
        if (clipboard == null) {
            throw new EmptyClipboardException();
        }
        return clipboard;
    }

    @Nullable
    public ClipboardHolder getExistingClipboard() {
        return clipboard;
    }

    public void addClipboard(@Nonnull MultiClipboardHolder toAppend) {
        checkNotNull(toAppend);
        ClipboardHolder existing = getExistingClipboard();
        MultiClipboardHolder multi;
        if (existing instanceof MultiClipboardHolder) {
            multi = (MultiClipboardHolder) existing;
            for (ClipboardHolder holder : toAppend.getHolders()) {
                multi.add(holder);
            }
        } else  {
            multi = toAppend;
            if (existing != null) {
                multi.add(existing);
            }
        }
        setClipboard(multi);
    }

    /**
     * Sets the clipboard.
     *
     * <p>Pass {@code null} to clear the clipboard.</p>
     *
     * @param clipboard the clipboard, or null if the clipboard is to be cleared
     */
    public void setClipboard(@Nullable ClipboardHolder clipboard) {
        if (this.clipboard == clipboard) return;

        if (this.clipboard != null) {
            if (clipboard == null || !clipboard.contains(this.clipboard.getClipboard())) {
                this.clipboard.close();
            }
        }
        this.clipboard = clipboard;
    }

    /**
     * See if tool control is enabled.
     *
     * @return true if enabled
     */
    public boolean isToolControlEnabled() {
        return toolControl;
    }

    /**
     * Change tool control setting.
     *
     * @param toolControl true to enable tool control
     */
    public void setToolControl(boolean toolControl) {
        this.toolControl = toolControl;
    }

    /**
     * Get the maximum number of blocks that can be changed in an edit session.
     *
     * @return block change limit
     */
    public int getBlockChangeLimit() {
        return maxBlocksChanged;
    }

    /**
     * Set the maximum number of blocks that can be changed.
     *
     * @param maxBlocksChanged the maximum number of blocks changed
     */
    public void setBlockChangeLimit(int maxBlocksChanged) {
        this.maxBlocksChanged = maxBlocksChanged;
    }

    /**
     * Get the maximum time allowed for certain executions to run before cancelling them, such as expressions.
     *
     * @return timeout time, in milliseconds
     */
    public int getTimeout() {
        return maxTimeoutTime;
    }

    /**
     * Set the maximum number of blocks that can be changed.
     *
     * @param timeout the time, in milliseconds, to limit certain executions to, or -1 to disable
     */
    public void setTimeout(int timeout) {
        this.maxTimeoutTime = timeout;
    }

    /**
     * Checks whether the super pick axe is enabled.
     *
     * @return status
     */
    public boolean hasSuperPickAxe() {
        return superPickaxe;
    }

    /**
     * Enable super pick axe.
     */
    public void enableSuperPickAxe() {
        superPickaxe = true;
    }

    /**
     * Disable super pick axe.
     */
    public void disableSuperPickAxe() {
        superPickaxe = false;
    }

    /**
     * Toggle the super pick axe.
     *
     * @return whether the super pick axe is now enabled
     */
    public boolean toggleSuperPickAxe() {
        superPickaxe = !superPickaxe;
        return superPickaxe;
    }

    /**
     * Get the position use for commands that take a center point
     * (i.e. //forestgen, etc.).
     *
     * @param player the player
     * @return the position to use
     * @throws IncompleteRegionException thrown if a region is not fully selected
     */
    public BlockVector3 getPlacementPosition(Player player) throws IncompleteRegionException {
        checkNotNull(player);
        if (!placeAtPos1) {
            return player.getBlockIn().toVector().toBlockPoint();
        }

        return selector.getPrimaryPosition();
    }

    /**
     * Toggle placement position.
     *
     * @return whether "place at position 1" is now enabled
     */
    public boolean togglePlacementPosition() {
        placeAtPos1 = !placeAtPos1;
        return placeAtPos1;
    }

    /**
     * Get a block bag for a player.
     *
     * @param player the player to get the block bag for
     * @return a block bag
     */
    @Nullable
    public BlockBag getBlockBag(Player player) {
        checkNotNull(player);
        if (!useInventory && FawePlayer.wrap(player).getLimit().INVENTORY_MODE == 0) {
            return null;
        }
        return player.getInventoryBlockBag();
    }

    /**
     * Get the snapshot that has been selected.
     *
     * @return the snapshot
     */
    @Nullable
    public Snapshot getSnapshot() {
        return snapshot;
    }

    /**
     * Select a snapshot.
     *
     * @param snapshot a snapshot
     */
    public void setSnapshot(@Nullable Snapshot snapshot) {
        this.snapshot = snapshot;
    }

    /**
     * Get the assigned block tool.
     *
     * @return the super pickaxe tool mode
     */
    public BlockTool getSuperPickaxe() {
        return pickaxeMode;
    }

    /**
     * Set the super pick axe tool.
     *
     * @param tool the tool to set
     */
    public void setSuperPickaxe(BlockTool tool) {
        checkNotNull(tool);
        this.pickaxeMode = tool;
    }

    /**
     * Get the tool assigned to the item.
     *
     * @param item the item type
     * @return the tool, which may be {@code null}
     */
    @Nullable
    public Tool getTool(ItemType item) {
        return tools[item.getInternalId()];
    }

    @Nullable
    public Tool getTool(Player player) {
        if (!Settings.IMP.EXPERIMENTAL.PERSISTENT_BRUSHES && !hasTool) {
            return null;
        }
        BaseItem item = player.getItemInHand(HandSide.MAIN_HAND);
        return getTool(item, player);
    }

    public Tool getTool(BaseItem item, Player player) {
        if (Settings.IMP.EXPERIMENTAL.PERSISTENT_BRUSHES && item.getNativeItem() != null) {
            BrushTool tool = BrushCache.getTool(player, this, item);
            if (tool != null) return tool;
        }
        return getTool(item.getType());
    }

    /**
     * Get the brush tool assigned to the item. If there is no tool assigned
     * or the tool is not assigned, the slot will be replaced with the
     * brush tool.
     *
     * @param item the item type
     * @return the tool, or {@code null}
     * @throws InvalidToolBindException if the item can't be bound to that item
     */
    public BrushTool getBrushTool(ItemType item) throws InvalidToolBindException {
        return getBrushTool(item.getDefaultState(), null, true);
    }

    public BrushTool getBrushTool(Player player) throws InvalidToolBindException {
        return getBrushTool(player, true);
    }

    public BrushTool getBrushTool(Player player, boolean create) throws InvalidToolBindException {
        BaseItem item = player.getItemInHand(HandSide.MAIN_HAND);
        return getBrushTool(item, player, create);
    }


    public BrushTool getBrushTool(BaseItem item, Player player, boolean create) throws InvalidToolBindException {
        Tool tool = getTool(item, player);
        if (!(tool instanceof BrushTool)) {
            if (create) {
                tool = new BrushTool();
                setTool(item, tool, player);
            } else {
                return null;
            }
        }

        return (BrushTool) tool;
    }

    /**
     * Set the tool.
     *
     * @param item the item type
     * @param tool the tool to set, which can be {@code null}
     * @throws InvalidToolBindException if the item can't be bound to that item
     */
    public void setTool(ItemType item, @Nullable Tool tool) throws InvalidToolBindException {
        if (item.hasBlockType()) {
            throw new InvalidToolBindException(item, "Blocks can't be used");
        } else if (item.getId().equalsIgnoreCase(config.wandItem)) {
            throw new InvalidToolBindException(item, "Already used for the wand");
        } else if (item.getId().equalsIgnoreCase(config.navigationWand)) {
            throw new InvalidToolBindException(item, "Already used for the navigation wand");
        }
        setTool(item.getDefaultState(), tool, null);
    }

    public void setTool(@Nullable Tool tool, Player player) throws InvalidToolBindException {
        BaseItemStack item = player.getItemInHand(HandSide.MAIN_HAND);
        setTool(item, tool, player);
    }

    public void setTool(BaseItem item, @Nullable Tool tool, Player player) throws InvalidToolBindException {
        ItemType type = item.getType();
        if (type.hasBlockType() && type.getBlockType().getMaterial().isAir()) {
            throw new InvalidToolBindException(type, "Blocks can't be used");
        } else if (type.getId().equalsIgnoreCase(config.wandItem)) {
            throw new InvalidToolBindException(type, "Already used for the wand");
        } else if (type.getId().equalsIgnoreCase(config.navigationWand)) {
            throw new InvalidToolBindException(type, "Already used for the navigation wand");
        }
        Tool previous;
        if (player != null && (tool instanceof BrushTool || tool == null) && Settings.IMP.EXPERIMENTAL.PERSISTENT_BRUSHES && item.getNativeItem() != null) {
            previous = BrushCache.getCachedTool(item);
            BrushCache.setTool(item, (BrushTool) tool);
            if (tool != null) {
                ((BrushTool) tool).setHolder(item);
            } else {
                this.tools[type.getInternalId()] = null;
            }
        } else {
            previous = this.tools[type.getInternalId()];
            this.tools[type.getInternalId()] = tool;
            if (tool != null) {
                hasTool = true;
            } else {
                hasTool = false;
                for (Tool i : this.tools) if (i != null) {
                    hasTool = true;
                    break;
                }
            }
        }
        if (previous != null && player != null && previous instanceof BrushTool) {
            BrushTool brushTool = (BrushTool) previous;
            brushTool.clear(player);
        }
    }

    /**
     * Returns whether inventory usage is enabled for this session.
     *
     * @return if inventory is being used
     */
    public boolean isUsingInventory() {
        return useInventory;
    }

    /**
     * Set the state of inventory usage.
     *
     * @param useInventory if inventory is to be used
     */
    public void setUseInventory(boolean useInventory) {
        this.useInventory = useInventory;
    }

    /**
     * Get the last script used.
     *
     * @return the last script's name
     */
    @Nullable
    public String getLastScript() {
        return lastScript;
    }

    /**
     * Set the last script used.
     *
     * @param lastScript the last script's name
     */
    public void setLastScript(@Nullable String lastScript) {
        this.lastScript = lastScript;
        setDirty();
    }

    /**
     * Tell the player the WorldEdit version.
     *
     * @param player the player
     */
    public void tellVersion(Actor player) {
    }

    public boolean shouldUseServerCUI() {
        return this.useServerCUI;
    }

    public void setUseServerCUI(boolean useServerCUI) {
        this.useServerCUI = useServerCUI;
        setDirty();
    }

    /**
     * Update server-side WorldEdit CUI.
     *
     * @param actor The player
     */
    public void updateServerCUI(Actor actor) {
        if (!actor.isPlayer()) {
            return; // This is for players only.
        }

        if (!config.serverSideCUI) {
            return; // Disabled in config.
        }

        Player player = (Player) actor;

        if (!useServerCUI || hasCUISupport) {
            if (cuiTemporaryBlock != null) {
                player.sendFakeBlock(cuiTemporaryBlock, null);
                cuiTemporaryBlock = null;
            }
            return; // If it's not enabled, ignore this.
        }

        BaseBlock block = ServerCUIHandler.createStructureBlock(player);
        if (block != null) {
            // If it's null, we don't need to do anything. The old was already removed.
            Map<String, Tag> tags = block.getNbtData().getValue();
            BlockVector3 tempCuiTemporaryBlock = BlockVector3.at(
                    ((IntTag) tags.get("x")).getValue(),
                    ((IntTag) tags.get("y")).getValue(),
                    ((IntTag) tags.get("z")).getValue()
            );
            if (cuiTemporaryBlock != null && !tempCuiTemporaryBlock.equals(cuiTemporaryBlock)) {
                // Update the existing block if it's the same location
                player.sendFakeBlock(cuiTemporaryBlock, null);
            }
            cuiTemporaryBlock = tempCuiTemporaryBlock;
            player.sendFakeBlock(cuiTemporaryBlock, block);
        } else if (cuiTemporaryBlock != null) {
            // Remove the old block
            player.sendFakeBlock(cuiTemporaryBlock, null);
            cuiTemporaryBlock = null;
        }
    }

    /**
     * Dispatch a CUI event but only if the actor has CUI support.
     *
     * @param actor the actor
     * @param event the event
     */
    public void dispatchCUIEvent(Actor actor, CUIEvent event) {
        checkNotNull(actor);
        checkNotNull(event);

        if (hasCUISupport) {
            actor.dispatchCUIEvent(event);
        } else if (useServerCUI) {
            updateServerCUI(actor);
        }
    }

    /**
     * Dispatch the initial setup CUI messages.
     *
     * @param actor the actor
     */
    public void dispatchCUISetup(Actor actor) {
        if (selector != null) {
            dispatchCUISelection(actor);
        }
    }

    /**
     * Send the selection information.
     *
     * @param actor the actor
     */
    public void dispatchCUISelection(Actor actor) {
        checkNotNull(actor);

        if (!hasCUISupport && useServerCUI) {
            updateServerCUI(actor);
            return;
        }

        if (selector instanceof CUIRegion) {
            CUIRegion tempSel = (CUIRegion) selector;

            if (tempSel.getProtocolVersion() > cuiVersion) {
                actor.dispatchCUIEvent(new SelectionShapeEvent(tempSel.getLegacyTypeID()));
                tempSel.describeLegacyCUI(this, actor);
            } else {
                actor.dispatchCUIEvent(new SelectionShapeEvent(tempSel.getTypeID()));
                tempSel.describeCUI(this, actor);
            }

        }
    }

    /**
     * Describe the selection to the CUI actor.
     *
     * @param actor the actor
     */
    public void describeCUI(Actor actor) {
        checkNotNull(actor);

        if (!hasCUISupport) {
            return;
        }

        if (selector instanceof CUIRegion) {
            CUIRegion tempSel = (CUIRegion) selector;

            if (tempSel.getProtocolVersion() > cuiVersion) {
                tempSel.describeLegacyCUI(this, actor);
            } else {
                tempSel.describeCUI(this, actor);
            }

        }
    }

    /**
     * Handle a CUI initialization message.
     *
     * @param text the message
     */
    public void handleCUIInitializationMessage(String text) {
        checkNotNull(text);
        if (this.failedCuiAttempts > 3) {
            return;
        }

        String[] split = text.split("\\|", 2);
        if (split.length > 1 && split[0].equalsIgnoreCase("v")) { // enough fields and right message
            if (split[1].length() > 4) {
                this.failedCuiAttempts ++;
                return;
            }
            setCUISupport(true);
            try {
                setCUIVersion(Integer.parseInt(split[1]));
            } catch (NumberFormatException e) {
                WorldEdit.logger.warn("Error while reading CUI init message: " + e.getMessage());
                this.failedCuiAttempts ++;
            }
        }
    }

    /**
     * Gets the status of CUI support.
     *
     * @return true if CUI is enabled
     */
    public boolean hasCUISupport() {
        return hasCUISupport;
    }

    /**
     * Sets the status of CUI support.
     *
     * @param support true if CUI is enabled
     */
    public void setCUISupport(boolean support) {
        hasCUISupport = support;
    }

    /**
     * Gets the client's CUI protocol version
     *
     * @return the CUI version
     */
    public int getCUIVersion() {
        return cuiVersion;
    }

    /**
     * Sets the client's CUI protocol version
     *
     * @param cuiVersion the CUI version
     */
    public void setCUIVersion(int cuiVersion) {
        this.cuiVersion = cuiVersion;
    }

    /**
     * Detect date from a user's input.
     *
     * @param input the input to parse
     * @return a date
     */
    @Nullable
    public Calendar detectDate(String input) {
        checkNotNull(input);

        Time.setTimeZone(getTimeZone());
        Options opt = new com.sk89q.jchronic.Options();
        opt.setNow(Calendar.getInstance(getTimeZone()));
        Span date = Chronic.parse(input, opt);
        if (date == null) {
            return null;
        } else {
            return date.getBeginCalendar();
        }
    }

    /**
     * Construct a new edit session.
     *
     * @param player the player
     * @return an edit session
     */
    public EditSession createEditSession(Player player) {
        checkNotNull(player);

        BlockBag blockBag = getBlockBag(player);

        World world = player.getWorld();
        boolean isPlayer = player.isPlayer();
        EditSessionBuilder builder = new EditSessionBuilder(world);
        if (player.isPlayer()) builder.player(FawePlayer.wrap(player));
        builder.blockBag(blockBag);
        builder.fastmode(fastMode);

        EditSession editSession = builder.build();

        Request.request().setEditSession(editSession);
        if (mask != null) {
            editSession.setMask(mask);
        }
        if (sourceMask != null) {
            editSession.setSourceMask(sourceMask);
        }
        if (transform != null) {
            editSession.addTransform(transform);
        }
        return editSession;
    }

    /**
     * Checks if the session has fast mode enabled.
     *
     * @return true if fast mode is enabled
     */
    public boolean hasFastMode() {
        return fastMode;
    }

    /**
     * Set fast mode.
     *
     * @param fastMode true if fast mode is enabled
     */
    public void setFastMode(boolean fastMode) {
        this.fastMode = fastMode;
    }

//    /**
//     * Gets the reorder mode of the session.
//     *
//     * @return The reorder mode
//     */
//    public EditSession.ReorderMode getReorderMode() {
//        return reorderMode;
//    }
//
//    /**
//     * Sets the reorder mode of the session.
//     *
//     * @param reorderMode The reorder mode
//     */
//    public void setReorderMode(EditSession.ReorderMode reorderMode) {
//        this.reorderMode = reorderMode;
//    }

    /**
     * Get the mask.
     *
     * @return mask, may be null
     */
    public Mask getMask() {
        return mask;
    }

    /**
     * Get the mask.
     *
     * @return mask, may be null
     */
    public Mask getSourceMask() {
        return sourceMask;
    }

    /**
     * Set a mask.
     *
     * @param mask mask or null
     */
    public void setMask(Mask mask) {
        this.mask = mask;
    }

    /**
     * Set a mask.
     *
     * @param mask mask or null
     */
    public void setSourceMask(Mask mask) {
        this.sourceMask = mask;
    }

    public void setTextureUtil(TextureUtil texture) {
        synchronized (this) {
            this.texture = texture;
        }
    }

    /**
     * Get the TextureUtil currently being used
     * @return
     */
    @Override
    public TextureUtil getTextureUtil() {
        TextureUtil tmp = texture;
        if (tmp == null) {
            synchronized (this) {
                tmp = Fawe.get().getCachedTextureUtil(true, 0, 100);
                this.texture = tmp;
            }
        }
        return tmp;
    }

    public ResettableExtent getTransform() {
        return transform;
    }

    public void setTransform(ResettableExtent transform) {
        this.transform = transform;
    }


}
