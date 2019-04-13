package com.boydti.fawe.object;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.command.CFICommands;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.brush.visualization.VirtualWorld;
import com.boydti.fawe.object.clipboard.DiskOptimizedClipboard;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.object.task.SimpleAsyncNotifyQueue;
import com.boydti.fawe.regions.FaweMaskManager;
import com.boydti.fawe.util.*;
import com.boydti.fawe.wrappers.FakePlayer;
import com.boydti.fawe.wrappers.LocationMaskedPlayerWrapper;
import com.boydti.fawe.wrappers.PlayerWrapper;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.worldedit.*;

import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.extension.platform.*;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.*;
import com.sk89q.worldedit.regions.selector.ConvexPolyhedralRegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.regions.selector.CylinderRegionSelector;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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
        if (obj == null || (obj instanceof String && obj.equals("*"))) {
            return FakePlayer.getConsole().toFawePlayer();
        }
        if (obj instanceof FawePlayer) {
            return (FawePlayer<V>) obj;
        }
        if (obj instanceof FakePlayer) {
            return ((FakePlayer) obj).toFawePlayer();
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
            FakePlayer fake = new FakePlayer(actor.getName(), actor.getUniqueId(), actor);
            return fake.toFawePlayer();
        }
        if (obj != null && obj.getClass().getName().contains("CraftPlayer") && !Fawe.imp().getPlatform().equals("bukkit")) {
            try {
                Method methodGetHandle = obj.getClass().getDeclaredMethod("getHandle");
                obj = methodGetHandle.invoke(obj);
            } catch (Throwable e) {
                e.printStackTrace();
                return null;
            }
        }
        return Fawe.imp().wrap(obj);
    }

    @Deprecated
    public FawePlayer(final T parent) {
        this.parent = parent;
        Fawe.get().register(this);
        if (Settings.IMP.CLIPBOARD.USE_DISK) {
            loadClipboardFromDisk();
        }
    }

    public int cancel(boolean close) {
        Collection<FaweQueue> queues = SetQueue.IMP.getAllQueues();
        int cancelled = 0;
        clearActions();
        for (FaweQueue queue : queues) {
            Collection<EditSession> sessions = queue.getEditSessions();
            for (EditSession session : sessions) {
                FawePlayer currentPlayer = session.getPlayer();
                if (currentPlayer == this) {
                    if (session.cancel()) {
                        cancelled++;
                    }
                }
            }
        }
        VirtualWorld world = getSession().getVirtualWorld();
        if (world != null) {
            if (close) {
                try {
                    world.close(false);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else world.clear();
        }
        return cancelled;
    }

    private void setConfirmTask(@Nullable Runnable task, CommandContext context, String command) {
        if (task != null) {
            Runnable newTask = () -> CommandManager.getInstance().handleCommandTask(() -> {
                task.run();
                return null;
            }, context.getLocals());
            setMeta("cmdConfirm", newTask);
        } else {
            setMeta("cmdConfirm", new CommandEvent(getPlayer(), command));
        }
    }

    public void checkConfirmation(@Nullable Runnable task, String command, int times, int limit, CommandContext context) throws RegionOperationException {
        if (command != null && !getMeta("cmdConfirmRunning", false)) {
            if (times > limit) {
                setConfirmTask(task, context, command);
                String volume = "<unspecified>";
                throw new RegionOperationException(BBC.WORLDEDIT_CANCEL_REASON_CONFIRM.f(0, times, command, volume));
            }
        }
        if (task != null) task.run();
    }

    public void checkConfirmationRadius(@Nullable Runnable task, String command, int radius, CommandContext context) throws RegionOperationException {
        if (command != null && !getMeta("cmdConfirmRunning", false)) {
            if (radius > 0) {
                if (radius > 448) {
                    setConfirmTask(task, context, command);
                    long volume = (long) (Math.PI * ((double) radius * radius));
                    throw new RegionOperationException(BBC.WORLDEDIT_CANCEL_REASON_CONFIRM.f(0, radius, command, NumberFormat.getNumberInstance().format(volume)));
                }
            }
        }
        if (task != null) task.run();
    }

    public void checkConfirmationStack(@Nullable Runnable task, String command, Region region, int times, CommandContext context) throws RegionOperationException {
        if (command != null && !getMeta("cmdConfirmRunning", false)) {
            if (region != null) {
            	BlockVector3 min = region.getMinimumPoint();
            	BlockVector3 max = region.getMaximumPoint();
                long area = (long) ((max.getX() - min.getX()) * (max.getZ() - min.getZ() + 1)) * times;
                if (area > 2 << 18) {
                    setConfirmTask(task, context, command);
                    BlockVector3 base = max.subtract(min).add(BlockVector3.ONE);
                    long volume = (long) base.getX() * base.getZ() * base.getY() * times;
                    throw new RegionOperationException(BBC.WORLDEDIT_CANCEL_REASON_CONFIRM.f(min, max, command, NumberFormat.getNumberInstance().format(volume)));
                }
            }
        }
        if (task != null) task.run();
    }

    public void checkConfirmationRegion(@Nullable Runnable task, String command, Region region, CommandContext context) throws RegionOperationException {
        if (command != null && !getMeta("cmdConfirmRunning", false)) {
            if (region != null) {
            	BlockVector3 min = region.getMinimumPoint();
            	BlockVector3 max = region.getMaximumPoint();
                long area = (long) ((max.getX() - min.getX()) * (max.getZ() - min.getZ() + 1));
                if (area > 2 << 18) {
                    setConfirmTask(task, context, command);
                    BlockVector3 base = max.subtract(min).add(BlockVector3.ONE);
                    long volume = (long) base.getX() * base.getZ() * base.getY();
                    throw new RegionOperationException(BBC.WORLDEDIT_CANCEL_REASON_CONFIRM.f(min, max, command, NumberFormat.getNumberInstance().format(volume)));
                }
            }
        }
        if (task != null) task.run();
    }

    public synchronized boolean confirm() {
        Runnable confirm = deleteMeta("cmdConfirm");
        if (!(confirm instanceof Runnable)) {
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
            throw new FaweException(BBC.WORLDEDIT_CANCEL_REASON_NO_REGION);
        } else if (!WEManager.IMP.regionContains(wrappedSelection, allowedSet)) {
            throw new FaweException(BBC.WORLDEDIT_CANCEL_REASON_OUTSIDE_REGION);
        }
    }

    public boolean toggle(String perm) {
        if (this.hasPermission(perm)) {
            this.setPermission(perm, false);
            return false;
        } else {
            this.setPermission(perm, true);
            return true;
        }
    }

    /**
     * Queue an action to run async
     * @param run
     */
    public void queueAction(final Runnable run) {
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
            sendMessage(BBC.getPrefix() + e.getLocalizedMessage());
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
    public boolean runAction(final Runnable ifFree, boolean checkFree, boolean async) {
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
                } catch (EmptyClipboardException e) {
                }
                if (player != null) {
                    Clipboard clip = doc.toClipboard();
                    ClipboardHolder holder = new ClipboardHolder(clip);
                    getSession().setClipboard(holder);
                }
            }
        } catch (Exception event) {
            Fawe.debug("====== INVALID CLIPBOARD ======");
            MainUtil.handleError(event, false);
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
        return FaweAPI.getWorld(getLocation().world);
    }

    public FaweQueue getFaweQueue(boolean autoQueue) {
        return getFaweQueue(true, autoQueue);
    }

    public FaweQueue getFaweQueue(boolean fast, boolean autoQueue) {
        CFICommands.CFISettings settings = this.getMeta("CFISettings");
        if (settings != null && settings.hasGenerator()) {
            return settings.getGenerator();
        } else {
            return SetQueue.IMP.getNewQueue(getWorld(), true, autoQueue);
        }
    }

    public FaweQueue getMaskedFaweQueue(boolean autoQueue) {
        FaweQueue queue = getFaweQueue(autoQueue);
        Region[] allowedRegions = getCurrentRegions();
        if (allowedRegions.length == 1 && allowedRegions[0].isGlobal()) {
            return queue;
        }
        return new MaskedFaweQueue(queue, allowedRegions);
    }

    /**
     * Load all the undo EditSession's from disk for a world <br>
     * - Usually already called when necessary
     *
     * @param world
     */
    public void loadSessionsFromDisk(final World world) {
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
    public abstract boolean hasPermission(final String perm);

    /**
     * Set a permission (requires Vault)
     *
     * @param perm
     * @param flag
     */
    public abstract void setPermission(final String perm, final boolean flag);

    /**
     * Send a message to the player
     *
     * @param message
     */
    public abstract void sendMessage(final String message);

    /**
     * Have the player execute a command
     *
     * @param substring
     */
    public abstract void executeCommand(final String substring);

    /**
     * Get the player's location
     *
     * @return
     */
    public abstract FaweLocation getLocation();

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
        } catch (final IncompleteRegionException e) {
            return null;
        }
    }

    /**
     * Get the player's current LocalSession
     *
     * @return
     */
    public LocalSession getSession() {
        return (this.session != null || this.getPlayer() == null || Fawe.get() == null) ? this.session : (session = Fawe.get().getWorldEdit().getSessionManager().get(this.getPlayer()));
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
    public void setSelection(final RegionWrapper region) {
        final Player player = this.getPlayer();
        BlockVector3 top = region.getMaximumPoint();
        top.withY(getWorld().getMaxY());
        final RegionSelector selector = new CuboidRegionSelector(player.getWorld(), region.getMinimumPoint(), top);
        this.getSession().setRegionSelector(player.getWorld(), selector);
    }

    public void setSelection(Region region) {
        RegionSelector selector;
        switch (region.getClass().getName()) {
            case "ConvexPolyhedralRegion":
                selector = new ConvexPolyhedralRegionSelector((ConvexPolyhedralRegion) region);
                break;
            case "CylinderRegion":
                selector = new CylinderRegionSelector((CylinderRegion) region);
                break;
            case "Polygonal2DRegion":
                selector = new com.sk89q.worldedit.regions.selector.Polygonal2DRegionSelector((Polygonal2DRegion) region);
                break;
            default:
                selector = new CuboidRegionSelector(null, region.getMinimumPoint(), region.getMaximumPoint());
                break;
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
    public void setSelection(final RegionSelector selector) {
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
        for (final Region region : this.getCurrentRegions()) {
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


    /**
     * Get the tracked EditSession(s) for this player<br>
     * - Queued or autoqueued EditSessions are considered tracked
     *
     * @param requiredStage
     * @return
     */
    public Map<EditSession, SetQueue.QueueStage> getTrackedSessions(SetQueue.QueueStage requiredStage) {
        Map<EditSession, SetQueue.QueueStage> map = new ConcurrentHashMap<>(8, 0.9f, 1);
        if (requiredStage == null || requiredStage == SetQueue.QueueStage.ACTIVE) {
            for (FaweQueue queue : SetQueue.IMP.getActiveQueues()) {
                Collection<EditSession> sessions = queue.getEditSessions();
                for (EditSession session : sessions) {
                    FawePlayer currentPlayer = session.getPlayer();
                    if (currentPlayer == this) {
                        map.put(session, SetQueue.QueueStage.ACTIVE);
                    }
                }
            }
        }
        if (requiredStage == null || requiredStage == SetQueue.QueueStage.INACTIVE) {
            for (FaweQueue queue : SetQueue.IMP.getInactiveQueues()) {
                Collection<EditSession> sessions = queue.getEditSessions();
                for (EditSession session : sessions) {
                    FawePlayer currentPlayer = session.getPlayer();
                    if (currentPlayer == this) {
                        map.put(session, SetQueue.QueueStage.INACTIVE);
                    }
                }
            }
        }
        return map;
    }
}
