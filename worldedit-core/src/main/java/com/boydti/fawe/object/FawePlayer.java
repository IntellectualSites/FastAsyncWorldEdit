package com.boydti.fawe.object;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.brush.visualization.VirtualWorld;
import com.boydti.fawe.object.clipboard.DiskOptimizedClipboard;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.object.task.SimpleAsyncNotifyQueue;
import com.boydti.fawe.regions.FaweMaskManager;
import com.boydti.fawe.util.EditSessionBuilder;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.util.WEManager;
import com.boydti.fawe.wrappers.LocationMaskedPlayerWrapper;
import com.boydti.fawe.wrappers.PlayerWrapper;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.PlatformCommandManager;
import com.sk89q.worldedit.extension.platform.PlatformManager;
import com.sk89q.worldedit.extension.platform.PlayerProxy;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.ConvexPolyhedralRegion;
import com.sk89q.worldedit.regions.CylinderRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionOperationException;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.ConvexPolyhedralRegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.regions.selector.CylinderRegionSelector;
import com.sk89q.worldedit.regions.selector.Polygonal2DRegionSelector;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import java.io.File;
import java.lang.reflect.Field;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.enginehub.piston.inject.InjectedValueAccess;
import org.jetbrains.annotations.NotNull;

public abstract class FawePlayer<T> extends Metadatable {

    public final T parent;
    private LocalSession session;

    public static final class METADATA_KEYS {
        public static final String ANVIL_CLIPBOARD = "anvil-clipboard";
        public static final String ROLLBACK = "rollback";
    }

    /**
     * Wrap some object into a FawePlayer<br>
     * - org.bukkit.entity.Player
     * - org.spongepowered.api.entity.living.player
     * - com.sk89q.worldedit.entity.Player
     * - String (name)
     * - UUID (player UUID)
     *
     * @param obj
     * @param <V>
     * @return
     */
    public static <V> FawePlayer<V> wrap(Object obj) {
        if (obj instanceof FawePlayer) {
            return (FawePlayer<V>) obj;
        }
        if (obj instanceof Player) {
            Player actor = LocationMaskedPlayerWrapper.unwrap((Player) obj);
            if (obj instanceof PlayerProxy) {
                Player player = ((PlayerProxy) obj).getBasePlayer();
                FawePlayer<Object> result = wrap(player);
                return (FawePlayer<V>) (result == null ? wrap(player.getName()) : result);
            } else if (obj instanceof PlayerWrapper) {
                return wrap(((PlayerWrapper) obj).getParent());
            } else {
                try {
                    Field fieldPlayer = actor.getClass().getDeclaredField("player");
                    fieldPlayer.setAccessible(true);
                    return wrap(fieldPlayer.get(actor));
                } catch (Throwable ignore) {
                }
            }
        }
        if (obj instanceof Actor) {
            Actor actor = (Actor) obj;
            FawePlayer existing = Fawe.get().getCachedPlayer(actor.getName());
            if (existing != null) {
                return existing;
            }
            }
        obj.getClass().getName();
        return Fawe.imp().wrap(obj);
    }

    @Deprecated
    public FawePlayer(T parent) {
        this.parent = parent;
        Fawe.get().register(this);
        if (Settings.IMP.CLIPBOARD.USE_DISK) {
            loadClipboardFromDisk();
        }
    }

    public int cancel(boolean close) {
//        Collection<IQueueExtent> queues = SetQueue.IMP.getAllQueues(); TODO NOT IMPLEMENTED
        int cancelled = 0;
//        clearActions();
//        for (IQueueExtent queue : queues) {
//            Collection<EditSession> sessions = queue.getEditSessions();
//            for (EditSession session : sessions) {
//                FawePlayer currentPlayer = session.getPlayer();
//                if (currentPlayer == this) {
//                    if (session.cancel()) {
//                        cancelled++;
//                    }
//                }
//            }
//        }
//        VirtualWorld world = getSession().getVirtualWorld();
//        if (world != null) {
//            if (close) {
//                try {
//                    world.close(false);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            else world.clear();
//        }
        return cancelled;
    }

    private void setConfirmTask(@NotNull Runnable task, InjectedValueAccess context, String command) {
        CommandEvent event = new CommandEvent(getPlayer(), command);
        Runnable newTask = () -> PlatformCommandManager.getInstance().handleCommandTask(() -> {
            task.run();
            return null;
        }, context, getSession(), event);
        setMeta("cmdConfirm", newTask);
    }

    public void checkConfirmation(@NotNull Runnable task, String command, int times, int limit, InjectedValueAccess context) throws RegionOperationException {
        if (command != null && !getMeta("cmdConfirmRunning", false)) {
            if (times > limit) {
                setConfirmTask(task, context, command);
                String volume = "<unspecified>";
                throw new RegionOperationException(
                    BBC.WORLDEDIT_CANCEL_REASON_CONFIRM.format(0, times, command, volume));
            }
        }
        task.run();
    }

    public void checkConfirmationRadius(@NotNull Runnable task, String command, int radius, InjectedValueAccess context) throws RegionOperationException {
        if (command != null && !getMeta("cmdConfirmRunning", false)) {
            if (radius > 0) {
                if (radius > 448) {
                    setConfirmTask(task, context, command);
                    long volume = (long) (Math.PI * ((double) radius * radius));
                    throw new RegionOperationException(BBC.WORLDEDIT_CANCEL_REASON_CONFIRM
                        .format(0, radius, command,
                            NumberFormat.getNumberInstance().format(volume)));
                }
            }
        }
        task.run();
    }

    public void checkConfirmationStack(@NotNull Runnable task, String command, Region region, int times, InjectedValueAccess context) throws RegionOperationException {
        if (command != null && !getMeta("cmdConfirmRunning", false)) {
            if (region != null) {
                BlockVector3 min = region.getMinimumPoint();
                BlockVector3 max = region.getMaximumPoint();
                long area = (long) ((max.getX() - min.getX()) * (max.getZ() - min.getZ() + 1)) * times;
                if (area > 2 << 18) {
                    setConfirmTask(task, context, command);
                    BlockVector3 base = max.subtract(min).add(BlockVector3.ONE);
                    long volume = (long) base.getX() * base.getZ() * base.getY() * times;
                    throw new RegionOperationException(BBC.WORLDEDIT_CANCEL_REASON_CONFIRM
                        .format(min, max, command, NumberFormat.getNumberInstance().format(volume)));
                }
            }
        }
        task.run();
    }

    public void checkConfirmationRegion(@NotNull Runnable task, String command, Region region, InjectedValueAccess context) throws RegionOperationException {
        if (command != null && !getMeta("cmdConfirmRunning", false)) {
            if (region != null) {
                BlockVector3 min = region.getMinimumPoint();
                BlockVector3 max = region.getMaximumPoint();
                long area = (max.getX() - min.getX()) * (max.getZ() - min.getZ() + 1);
                if (area > 2 << 18) {
                    setConfirmTask(task, context, command);
                    BlockVector3 base = max.subtract(min).add(BlockVector3.ONE);
                    long volume = (long) base.getX() * base.getZ() * base.getY();
                    throw new RegionOperationException(BBC.WORLDEDIT_CANCEL_REASON_CONFIRM
                        .format(min, max, command, NumberFormat.getNumberInstance().format(volume)));
                }
            }
        }
        task.run();
    }

    public synchronized boolean confirm() {
        Runnable confirm = deleteMeta("cmdConfirm");
        if (confirm == null) {
            return false;
        }
        queueAction(() -> {
            setMeta("cmdConfirmRunning", true);
            try {
                confirm.run();
            } finally {
                setMeta("cmdConfirmRunning", false);
            }
        });
        return true;
    }

    public void checkAllowedRegion(Region wrappedSelection) {
        Region[] allowed = WEManager.IMP.getMask(this, FaweMaskManager.MaskType.OWNER);
        HashSet<Region> allowedSet = new HashSet<>(Arrays.asList(allowed));
        if (allowed.length == 0) {
            throw FaweException.NO_REGION;
        } else if (!WEManager.IMP.regionContains(wrappedSelection, allowedSet)) {
            throw FaweException.OUTSIDE_REGION;
        }
    }

    /**
     * Queue an action to run async
     * @param run
     */
    public void queueAction(Runnable run) {
        runAction(run, false, true);
    }

    public void clearActions() {
        asyncNotifyQueue.clear();
    }

    public boolean runAsyncIfFree(Runnable r) {
        return runAction(r, true, true);
    }

    public boolean runIfFree(Runnable r) {
        return runAction(r, true, false);
    }

    // Queue for async tasks
    private AtomicInteger runningCount = new AtomicInteger();
    private SimpleAsyncNotifyQueue asyncNotifyQueue = new SimpleAsyncNotifyQueue((t, e) -> {
        while (e.getCause() != null) {
            e = e.getCause();
        }
        if (e instanceof WorldEditException) {
            sendMessage(e.getLocalizedMessage());
        } else {
            FaweException fe = FaweException.get(e);
            if (fe != null) {
                sendMessage(fe.getMessage());
            } else {
                e.printStackTrace();
            }
        }
    });

    /**
     * Run a task either async, or on the current thread
     * @param ifFree
     * @param checkFree Whether to first check if a task is running
     * @param async
     * @return false if the task was ran or queued
     */
    public boolean runAction(Runnable ifFree, boolean checkFree, boolean async) {
        if (checkFree) {
            if (runningCount.get() != 0) return false;
        }
        Runnable wrapped = () -> {
            try {
                runningCount.addAndGet(1);
                ifFree.run();
            } finally {
                runningCount.decrementAndGet();
            }
        };
        if (async) {
            asyncNotifyQueue.queue(wrapped);
        } else {
            TaskManager.IMP.taskNow(wrapped, false);
        }
        return true;
    }

    public boolean checkAction() {
        long time = getMeta("faweActionTick", Long.MIN_VALUE);
        long tick = Fawe.get().getTimer().getTick();
        setMeta("faweActionTick", tick);
        return tick > time;
    }

    /**
     * Loads any history items from disk:
     * - Should already be called if history on disk is enabled
     */
    public void loadClipboardFromDisk() {
        File file = MainUtil.getFile(Fawe.imp().getDirectory(), Settings.IMP.PATHS.CLIPBOARD + File.separator + getUUID() + ".bd");
        try {
            if (file.exists() && file.length() > 5) {
                DiskOptimizedClipboard doc = new DiskOptimizedClipboard(file);
                Player player = toWorldEditPlayer();
                LocalSession session = getSession();
                try {
                    if (session.getClipboard() != null) {
                        return;
                    }
                } catch (EmptyClipboardException ignored) {
                }
                if (player != null) {
                    Clipboard clip = doc.toClipboard();
                    ClipboardHolder holder = new ClipboardHolder(clip);
                    getSession().setClipboard(holder);
                }
            }
        } catch (Exception event) {
            Fawe.debug("====== INVALID CLIPBOARD ======");
            event.printStackTrace();
            Fawe.debug("===============---=============");
            Fawe.debug("This shouldn't result in any failure");
            Fawe.debug("File: " + file.getName() + " (len:" + file.length() + ")");
            Fawe.debug("===============---=============");
        }
    }

    /**
     * Get the current World
     *
     * @return
     */
    public World getWorld() {
        return getPlayer().getWorld();
    }

    /**
     * Load all the undo EditSession's from disk for a world <br>
     * - Usually already called when necessary
     *
     * @param world
     */
    public void loadSessionsFromDisk(World world) {
        if (world == null) {
            return;
        }
        getSession().loadSessionHistoryFromDisk(getUUID(), world);
    }

    /**
     * Send a title
     *
     * @param head
     * @param sub
     */
    public abstract void sendTitle(String head, String sub);

    /**
     * Remove the title
     */
    public abstract void resetTitle();

    /**
     * Get the player's limit
     *
     * @return
     */
    public FaweLimit getLimit() {
        return Settings.IMP.getLimit(this);
    }

    /**
     * Get the player's name
     *
     * @return
     */
    public abstract String getName();

    /**
     * Get the player's UUID
     *
     * @return
     */
    public abstract UUID getUUID();


    public boolean isSneaking() {
        return false;
    }

    /**
     * Check the player's permission
     *
     * @param perm
     * @return
     */
    public abstract boolean hasPermission(String perm);

    /**
     * Send a message to the player
     *
     * @param message
     */
    public abstract void sendMessage(String message);

    /**
     * Print a WorldEdit error.
     *
     * @param msg The error message text
     */
    public abstract void printError(String msg);

    /**
     * Have the player execute a command
     *
     * @param substring
     */
    public abstract void executeCommand(String substring);

    /**
     * Get the player's location
     *
     * @return
     */
    public Location getLocation() {
        return getPlayer().getLocation();
    }

    /**
     * Get the WorldEdit player object
     *
     * @return
     */
    public abstract Player toWorldEditPlayer();

    private Player cachedWorldEditPlayer;

    public Player getPlayer() {
        if (cachedWorldEditPlayer == null) {
            cachedWorldEditPlayer = toWorldEditPlayer();
        }
        return cachedWorldEditPlayer;
    }

    /**
     * Get the player's current selection (or null)
     *
     * @return
     */
    public Region getSelection() {
        try {
            return this.getSession().getSelection(this.getPlayer().getWorld());
        } catch (IncompleteRegionException e) {
            return null;
        }
    }

    /**
     * Get the player's current LocalSession
     *
     * @return
     */
    public LocalSession getSession() {
        if (this.session != null || this.getPlayer() == null || Fawe.get() == null) return this.session;
        else return session = Fawe.get().getWorldEdit().getSessionManager().get(this.getPlayer());
    }

    /**
     * Get the player's current allowed WorldEdit regions
     *
     * @return
     */
    @Deprecated
    public Region[] getCurrentRegions() {
        return WEManager.IMP.getMask(this);
    }

    @Deprecated
    public Region[] getCurrentRegions(FaweMaskManager.MaskType type) {
        return WEManager.IMP.getMask(this, type);
    }

    /**
     * Set the player's WorldEdit selection to the following CuboidRegion
     *
     * @param region
     */
    @Deprecated
    public void setSelection(RegionWrapper region) {
        final Player player = this.getPlayer();
        BlockVector3 top = region.getMaximumPoint();
        top.withY(getWorld().getMaxY());
        final RegionSelector selector = new CuboidRegionSelector(player.getWorld(), region.getMinimumPoint(), top);
        this.getSession().setRegionSelector(player.getWorld(), selector);
    }

    public void setSelection(Region region) {
        RegionSelector selector;
        if (region instanceof ConvexPolyhedralRegion) {
            selector = new ConvexPolyhedralRegionSelector((ConvexPolyhedralRegion) region);
        } else if (region instanceof CylinderRegion) {
            selector = new CylinderRegionSelector((CylinderRegion) region);
        } else if (region instanceof Polygonal2DRegion) {
            selector = new Polygonal2DRegionSelector((Polygonal2DRegion) region);
        } else {
            selector = new CuboidRegionSelector(null, region.getMinimumPoint(), region.getMaximumPoint());
        }
        selector.setWorld(region.getWorld());

        final Player player = this.getPlayer();
        this.getSession().setRegionSelector(player.getWorld(), selector);
    }

    /**
     * Set the player's WorldEdit selection
     *
     * @param selector
     */
    public void setSelection(RegionSelector selector) {
        this.getSession().setRegionSelector(toWorldEditPlayer().getWorld(), selector);
    }

    /**
     * Get the largest region in the player's allowed WorldEdit region
     *
     * @return
     */
    public Region getLargestRegion() {
        int area = 0;
        Region max = null;
        for (Region region : this.getCurrentRegions()) {
            final int tmp = region.getArea();
            if (tmp > area) {
                area = tmp;
                max = region;
            }
        }
        return max;
    }

    @Override
    public String toString() {
        return this.getName();
    }

    /**
     * Check if the player has WorldEdit bypass enabled
     *
     * @return
     */
    public boolean hasWorldEditBypass() {
        return this.hasPermission("fawe.bypass");
    }

    /**
     * Unregister this player (delets all metadata etc)
     * - Usually called on logout
     */
    public void unregister() {
        cancel(true);
        if (Settings.IMP.HISTORY.DELETE_ON_LOGOUT) {
            session = getSession();
            session.setClipboard(null);
            session.clearHistory();
            session.unregisterTools(getPlayer());
        }
        Fawe.get().unregister(getName());
    }

    /**
     * Get a new EditSession from this player
     */
    public EditSession getNewEditSession() {
        return new EditSessionBuilder(getWorld()).player(this).build();
    }

    public void setVirtualWorld(VirtualWorld world) {
        getSession().setVirtualWorld(world);
    }

    /**
     * Get the World the player is editing in (may not match the world they are in)<br/>
     * - e.g. If they are editing a CFI world.<br/>
     * @return Editing world
     */
    public World getWorldForEditing() {
        VirtualWorld virtual = getSession().getVirtualWorld();
        if (virtual != null) {
            return virtual;
        }
//        CFICommands.CFISettings cfi = getMeta("CFISettings");
//        if (cfi != null && cfi.hasGenerator() && cfi.getGenerator().hasPacketViewer()) {
//            return cfi.getGenerator();
//        }
        return WorldEdit.getInstance().getPlatformManager().getWorldForEditing(getWorld());
    }

    public PlayerProxy createProxy() {
        Player player = getPlayer();
        World world = getWorldForEditing();

        PlatformManager platformManager = WorldEdit.getInstance().getPlatformManager();

        Player permActor = platformManager.queryCapability(Capability.PERMISSIONS).matchPlayer(player);
        if (permActor == null) {
            permActor = player;
        }

        Player cuiActor = platformManager.queryCapability(Capability.WORLDEDIT_CUI).matchPlayer(player);
        if (cuiActor == null) {
            cuiActor = player;
        }

        PlayerProxy proxy = new PlayerProxy(player, permActor, cuiActor, world);
        if (world instanceof VirtualWorld) {
            proxy.setOffset(Vector3.ZERO.subtract(((VirtualWorld) world).getOrigin()));
        }
        return proxy;
    }
}
