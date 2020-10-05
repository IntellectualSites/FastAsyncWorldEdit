package com.boydti.fawe;

import com.boydti.fawe.beta.IQueueChunk;
import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.beta.implementation.lighting.NMSRelighter;
import com.boydti.fawe.beta.implementation.queue.ParallelQueueExtent;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.object.RelightMode;
import com.boydti.fawe.object.changeset.DiskStorageHistory;
import com.boydti.fawe.object.changeset.SimpleChangeSetSummary;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.regions.FaweMaskManager;
import com.boydti.fawe.util.EditSessionBuilder;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MemUtil;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.util.WEManager;
import com.boydti.fawe.wrappers.WorldWrapper;
import com.google.common.collect.Sets;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.world.World;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * The FaweAPI class offers a few useful functions.<br>
 * - This class is not intended to replace the WorldEdit API<br>
 * - With FAWE installed, you can use the EditSession and other WorldEdit classes from an async thread.<br>
 * <br>
 * FaweAPI.[some method]
 */
public class FaweAPI {

    /**
     * Offers a lot of options for building an EditSession.
     *
     * @return A new EditSessionBuilder
     * @see EditSessionBuilder
     */
    @Deprecated
    public static EditSessionBuilder getEditSessionBuilder(World world) {
        return new EditSessionBuilder(world);
    }

    /**
     * The TaskManager has some useful methods for doing things asynchronously.
     *
     * @return TaskManager
     */
    public static TaskManager getTaskManager() {
        return TaskManager.IMP;
    }

    /**
     * You can either use a {@code IQueueExtent} or an {@code EditSession} to change blocks.
     *
     * <p>
     * The {@link IQueueExtent} skips a bit of overhead, so it is marginally faster. {@link
     * EditSession} can do a lot more. Remember to commit when you are done!
     * </p>
     *
     * @param world The name of the world
     * @param autoQueue If it should start dispatching before you enqueue it.
     * @return the queue extent
     */
    public static IQueueExtent<IQueueChunk> createQueue(World world, boolean autoQueue) {
        IQueueExtent<IQueueChunk> queue = Fawe.get().getQueueHandler().getQueue(world);
        if (!autoQueue) {
            queue.disableQueue();
        }
        return queue;
    }

    public static World getWorld(String worldName) {
        Platform platform = WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.WORLD_EDITING);
        List<? extends World> worlds = platform.getWorlds();
        for (World current : worlds) {
            if (current.getName().equals(worldName)) {
                return WorldWrapper.wrap(current);
            }
        }
        return null;
    }

    /**
     * Upload the clipboard to the configured web interface.
     *
     * @param clipboard The clipboard (may not be null)
     * @param format The format to use (some formats may not be supported)
     * @return The download URL or null
     */
    public static URL upload(final Clipboard clipboard, final ClipboardFormat format) {
        return format.upload(clipboard);
    }

    /**
     * Just forwards to ClipboardFormat.SCHEMATIC.load(file).
     *
     * @param file the file to load
     * @return a clipboard containing the schematic
     * @see ClipboardFormat
     */
    public static Clipboard load(File file) throws IOException {
        return ClipboardFormats.findByFile(file).load(file);
    }

    /**
     * Get a list of supported protection plugin masks.
     *
     * @return Set of FaweMaskManager
     */
    public static Set<FaweMaskManager> getMaskManagers() {
        return new HashSet<>(WEManager.IMP.managers);
    }

    /**
     * Check if the server has more than the configured low memory threshold.
     *
     * @return True if the server has limited memory
     */
    public static boolean isMemoryLimited() {
        return MemUtil.isMemoryLimited();
    }

    /**
     * Get a player's allowed WorldEdit region.
     */
    public static Region[] getRegions(Player player) {
        return WEManager.IMP.getMask(player);
    }

    /**
     * Cancel the edit with the following extent.
     *
     * <p>
     * The extent must be the one being used by an EditSession, otherwise an error will be thrown.
     * Insert an extent into the EditSession using the EditSessionEvent.
     * </p>
     *
     * @see EditSession#getRegionExtent() How to get the FaweExtent for an EditSession
     */
    public static void cancelEdit(AbstractDelegateExtent extent, Component reason) {
        try {
            WEManager.IMP.cancelEdit(extent, new FaweException(reason));
        } catch (WorldEditException ignored) {
        }
    }

    public static void addMaskManager(FaweMaskManager maskMan) {
        WEManager.IMP.managers.add(maskMan);
    }

    /**
     * Get the DiskStorageHistory object representing a File.
     */
    public static DiskStorageHistory getChangeSetFromFile(File file) {
        if (!file.exists() || file.isDirectory()) {
            throw new IllegalArgumentException("Not a file!");
        }
        if (Settings.IMP.HISTORY.USE_DISK) {
            throw new IllegalArgumentException("History on disk not enabled!");
        }
        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".bd")) {
            throw new IllegalArgumentException("Not a BD file!");
        }
        String[] path = file.getPath().split(File.separator);
        if (path.length < 3) {
            throw new IllegalArgumentException("Not in history directory!");
        }
        String worldName = path[path.length - 3];
        String uuidString = path[path.length - 2];
        World world = getWorld(worldName);
        if (world == null) {
            throw new IllegalArgumentException("Corresponding world does not exist: " + worldName);
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID from file path: " + uuidString);
        }
        return new DiskStorageHistory(world, uuid, Integer.parseInt(file.getName().split("\\.")[0]));
    }

    /**
     * Used in the rollback to generate a list of {@link DiskStorageHistory} objects.
     *
     * @param origin - The origin location
     * @param user - The uuid (may be null)
     * @param radius - The radius from the origin of the edit
     * @param timediff - The max age of the file in milliseconds
     * @param shallow - If shallow is true, FAWE will only read the first {@link
     *     Settings.HISTORY#BUFFER_SIZE} bytes to obtain history info
     * @return a list of DiskStorageHistory Objects
     * @apiNote An edit outside the radius may be included if it overlaps with an edit inside
     *     that depends on it. Reading only part of the file will result in unreliable bounds info
     *     for large edits.
     */
    public static List<DiskStorageHistory> getBDFiles(Location origin, UUID user, int radius, long timediff, boolean shallow) {
        Extent extent = origin.getExtent();
        if (!(extent instanceof World)) {
            throw new IllegalArgumentException("Origin is not a valid world");
        }
        World world = (World) extent;
        File history = MainUtil.getFile(Fawe.imp().getDirectory(), Settings.IMP.PATHS.HISTORY + File.separator + world.getName());
        if (!history.exists()) {
            return new ArrayList<>();
        }
        long now = System.currentTimeMillis();
        ArrayList<File> files = new ArrayList<>();
        for (File userFile : history.listFiles()) {
            if (!userFile.isDirectory()) {
                continue;
            }
            UUID userUUID;
            try {
                userUUID = UUID.fromString(userFile.getName());
            } catch (IllegalArgumentException e) {
                continue;
            }
            if (user != null && !userUUID.equals(user)) {
                continue;
            }
            ArrayList<Integer> ids = new ArrayList<>();
            for (File file : userFile.listFiles()) {
                if (file.getName().endsWith(".bd")) {
                    if (timediff >= Integer.MAX_VALUE || now - file.lastModified() <= timediff) {
                        files.add(file);
                        if (files.size() > 2048) {
                            return null;
                        }
                    }
                }
            }
        }
        files.sort((a, b) -> {
            String aName = a.getName();
            String bName = b.getName();
            int aI = Integer.parseInt(aName.substring(0, aName.length() - 3));
            int bI = Integer.parseInt(bName.substring(0, bName.length() - 3));
            long value = aI - bI;
            return value == 0 ? 0 : value < 0 ? -1 : 1;
        });
        RegionWrapper bounds = new RegionWrapper(origin.getBlockX() - radius, origin.getBlockX() + radius, origin.getBlockZ() - radius, origin.getBlockZ() + radius);
        RegionWrapper boundsPlus = new RegionWrapper(bounds.minX - 64, bounds.maxX + 512, bounds.minZ - 64, bounds.maxZ + 512);
        HashSet<RegionWrapper> regionSet = Sets.<RegionWrapper>newHashSet(bounds);
        ArrayList<DiskStorageHistory> result = new ArrayList<>();
        for (File file : files) {
            UUID uuid = UUID.fromString(file.getParentFile().getName());
            DiskStorageHistory dsh = new DiskStorageHistory(world, uuid, Integer.parseInt(file.getName().split("\\.")[0]));
            SimpleChangeSetSummary summary = dsh.summarize(boundsPlus, shallow);
            RegionWrapper region = new RegionWrapper(summary.minX, summary.maxX, summary.minZ, summary.maxZ);
            boolean encompassed = false;
            boolean isIn = false;
            for (RegionWrapper allowed : regionSet) {
                isIn = isIn || allowed.intersects(region);
                if (encompassed = allowed.isIn(region.minX, region.maxX) && allowed.isIn(region.minZ, region.maxZ)) {
                    break;
                }
            }
            if (isIn) {
                result.add(0, dsh);
                if (!encompassed) {
                    regionSet.add(region);
                }
                if (shallow && result.size() > 64) {
                    return result;
                }
            }
        }
        return result;
    }

    /**
     * The DiskStorageHistory class is what FAWE uses to represent the undo on disk.
     *
     */
    public static DiskStorageHistory getChangeSetFromDisk(World world, UUID uuid, int index) {
        return new DiskStorageHistory(world, uuid, index);
    }

    /**
     * Fix the lighting in a selection. This is a multi-step process as outlined below.
     *
     * <ol>
     *     <li>Removes all lighting, then relights.</li>
     *     <li>Relights in parallel (if enabled) for best performance.</li>
     *     <li>Resends the chunks to the client.</li>
     * </ol>
     *
     * @param world World to relight in
     * @param selection Region to relight
     * @param queue Queue to relight in/from
     * @param mode The mode to relight with
     * @return Chunks changed
     */
    public static int fixLighting(World world, Region selection, @Nullable IQueueExtent<IQueueChunk> queue, final RelightMode mode) {
        final BlockVector3 bot = selection.getMinimumPoint();
        final BlockVector3 top = selection.getMaximumPoint();

        final int minX = bot.getBlockX() >> 4;
        final int minZ = bot.getBlockZ() >> 4;

        final int maxX = top.getBlockX() >> 4;
        final int maxZ = top.getBlockZ() >> 4;

        int count = 0;

        if (queue == null) {
            World unwrapped = WorldWrapper.unwrap(world);
            if (unwrapped instanceof IQueueExtent) {
                queue = (IQueueExtent) unwrapped;
            } else if (Settings.IMP.QUEUE.PARALLEL_THREADS > 1) {
                ParallelQueueExtent parallel =
                    new ParallelQueueExtent(Fawe.get().getQueueHandler(), world, true);
                queue = parallel.getExtent();
            } else {
                queue = Fawe.get().getQueueHandler().getQueue(world);
            }
        }

        NMSRelighter relighter = new NMSRelighter(queue, Settings.IMP.LIGHTING.DO_HEIGHTMAPS);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                relighter.addChunk(x, z, null, 65535);
                count++;
            }
        }
        if (mode != RelightMode.NONE) {
            if (Settings.IMP.LIGHTING.REMOVE_FIRST) {
                relighter.removeAndRelight(true);
            } else {
                relighter.fixSkyLighting();
                relighter.fixBlockLighting();
            }
        } else {
            relighter.removeLighting();
        }
        relighter.sendChunks();
        return count;
    }

    /**
     * Runs a task when the server is low on memory.
     */
    public static void addMemoryLimitedTask(Runnable run) {
        MemUtil.addMemoryLimitedTask(run);
    }

    /**
     * Runs a task when the server is no longer low on memory.
     */
    public static void addMemoryPlentifulTask(Runnable run) {
        MemUtil.addMemoryPlentifulTask(run);
    }

    public static Map<String, String> getTranslations(Locale locale) {
        return WorldEdit.getInstance().getTranslationManager().getTranslationMap(locale);
    }

}
