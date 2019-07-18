package com.boydti.fawe;

import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.object.changeset.DiskStorageHistory;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.object.extent.LightingExtent;
import com.boydti.fawe.object.schematic.Schematic;
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
import com.sk89q.worldedit.extension.factory.DefaultTransformParser;
import com.sk89q.worldedit.extension.factory.parser.mask.DefaultMaskParser;
import com.sk89q.worldedit.extension.factory.parser.pattern.DefaultPatternParser;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.PlatformCommandManager;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.internal.registry.AbstractFactory;
import com.sk89q.worldedit.internal.registry.InputParser;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The FaweAPI class offers a few useful functions.<br>
 * - This class is not intended to replace the WorldEdit API<br>
 * - With FAWE installed, you can use the EditSession and other WorldEdit classes from an async thread.<br>
 * <br>
 * FaweAPI.[some method]
 */
public class FaweAPI {
    /**
     * Offers a lot of options for building an EditSession
     *
     * @param world
     * @return A new EditSessionBuilder
     * @see EditSessionBuilder
     */
    public static EditSessionBuilder getEditSessionBuilder(World world) {
        return new EditSessionBuilder(world);
    }

    /**
     * The TaskManager has some useful methods for doing things asynchronously
     *
     * @return TaskManager
     */
    public static TaskManager getTaskManager() {
        return TaskManager.IMP;
    }

    /**
     * Add a custom mask for use in e.g {@literal //mask #id:<input>}
     *
     * @param methods The class with a bunch of mask methods
     * @return true if the mask was registered
     * @see com.sk89q.worldedit.command.MaskCommands
     */
    public static boolean registerMasks(Object methods) {
        DefaultMaskParser parser = getParser(DefaultMaskParser.class);
        if (parser != null) parser.register(methods);
        return parser != null;
    }

    /**
     * Add a custom material for use in e.g {@literal //material #id:<input>}
     *
     * @param methods The class with a bunch of pattern methods
     * @return true if the mask was registered
     * @see com.sk89q.worldedit.command.PatternCommands
     */
    public static boolean registerPatterns(Object methods) {
        DefaultPatternParser parser = getParser(DefaultPatternParser.class);
        if (parser != null) parser.register(methods);
        return parser != null;
    }

    /**
     * Add a custom transform for use in
     *
     * @param methods The class with a bunch of transform methods
     * @return true if the transform was registered
     * @see com.sk89q.worldedit.command.TransformCommands
     */
    public static boolean registerTransforms(Object methods) {
        DefaultTransformParser parser = Fawe.get().getTransformParser();
        if (parser != null) parser.register(methods);
        return parser != null;
    }

    public static <T> T getParser(Class<T> parserClass) {
        try {
            Field field = AbstractFactory.class.getDeclaredField("parsers");
            field.setAccessible(true);
            ArrayList<InputParser> parsers = new ArrayList<>();
            parsers.addAll((List<InputParser>) field.get(WorldEdit.getInstance().getMaskFactory()));
            parsers.addAll((List<InputParser>) field.get(WorldEdit.getInstance().getBlockFactory()));
            parsers.addAll((List<InputParser>) field.get(WorldEdit.getInstance().getItemFactory()));
            parsers.addAll((List<InputParser>) field.get(WorldEdit.getInstance().getPatternFactory()));
            for (InputParser parser : parsers) {
                if (parserClass.isAssignableFrom(parser.getClass())) {
                    return (T) parser;
                }
            }
            return null;
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a command with the provided aliases and register all methods of the class as sub commands.<br>
     * - You should try to register commands during startup
     * - If no aliases are specified, all methods become root commands
     *
     * @param clazz   The class containing all the sub command methods
     * @param aliases The aliases to give the command (or none)
     */
    public static void registerCommands(Object clazz, String... aliases) {
//        PlatformCommandManager.getInstance().registerCommands(clazz, aliases); TODO NOT IMPLEMENTED
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
     * @return
     */
    public static FawePlayer wrapPlayer(Object obj) {
        return FawePlayer.wrap(obj);
    }

    /**
     * You can either use a IQueueExtent or an EditSession to change blocks<br>
     * - The IQueueExtent skips a bit of overhead so it's marginally faster<br>
     * - The WorldEdit EditSession can do a lot more<br>
     * Remember to commit when you're done!<br>
     *
     * @param world     The name of the world
     * @param autoqueue If it should start dispatching before you enqueue it.
     * @return
     * @see FaweQueue#enqueue()
     */
    public static IQueueExtent createQueue(World world, boolean autoqueue) {
        IQueueExtent queue = Fawe.get().getQueueHandler().getQueue(world);
        if (!autoqueue) {
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
     * Upload the clipboard to the configured web interface
     *
     * @param clipboard The clipboard (may not be null)
     * @param format    The format to use (some formats may not be supported)
     * @return The download URL or null
     */
    public static URL upload(final Clipboard clipboard, final ClipboardFormat format) {
        return format.uploadAnonymous(clipboard);
    }

    /**
     * Just forwards to ClipboardFormat.SCHEMATIC.load(file)
     *
     * @param file
     * @return
     * @see ClipboardFormat
     * @see Schematic
     */
    public static Schematic load(File file) throws IOException {
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
     * Check if the server has more than the configured low memory threshold
     *
     * @return True if the server has limited memory
     */
    public static boolean isMemoryLimited() {
        return MemUtil.isMemoryLimited();
    }

    /**
     * Get a player's allowed WorldEdit region
     *
     * @param player
     * @return
     */
    public static Region[] getRegions(FawePlayer player) {
        return WEManager.IMP.getMask(player);
    }

    /**
     * Cancel the edit with the following extent<br>
     * - The extent must be the one being used by an EditSession, otherwise an error may be thrown <br>
     * - Insert an extent into the EditSession using the EditSessionEvent: http://wiki.sk89q.com/wiki/WorldEdit/API/Hooking_EditSession <br>
     *
     * @param extent
     * @param reason
     * @see EditSession#getRegionExtent() To get the FaweExtent for an EditSession
     */
    public static void cancelEdit(Extent extent, BBC reason) {
        try {
            WEManager.IMP.cancelEdit(extent, new FaweException(reason));
        } catch (WorldEditException ignore) {
        }
    }

    public static void addMaskManager(FaweMaskManager maskMan) {
        WEManager.IMP.managers.add(maskMan);
    }

    /**
     * Get the DiskStorageHistory object representing a File
     *
     * @param file
     * @return
     */
    public static DiskStorageHistory getChangeSetFromFile(File file) {
        if (!file.exists() || file.isDirectory()) {
            throw new IllegalArgumentException("Not a file!");
        }
        if (!file.getName().toLowerCase().endsWith(".bd")) {
            throw new IllegalArgumentException("Not a BD file!");
        }
        if (Settings.IMP.HISTORY.USE_DISK) {
            throw new IllegalArgumentException("History on disk not enabled!");
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
        DiskStorageHistory history = new DiskStorageHistory(world, uuid, Integer.parseInt(file.getName().split("\\.")[0]));
        return history;
    }

    /**
     * Used in the RollBack to generate a list of DiskStorageHistory objects<br>
     * - Note: An edit outside the radius may be included if it overlaps with an edit inside that depends on it.
     *
     * @param origin   - The origin location
     * @param user     - The uuid (may be null)
     * @param radius   - The radius from the origin of the edit
     * @param timediff - The max age of the file in milliseconds
     * @param shallow  - If shallow is true, FAWE will only read the first Settings.IMP.BUFFER_SIZE bytes to obtain history info<br>
     *                 Reading only part of the file will result in unreliable bounds info for large edits
     * @return
     */
    public static List<DiskStorageHistory> getBDFiles(Location origin, UUID user, int radius, long timediff, boolean shallow) {
        Extent extent = origin.getExtent();
        if (!(extent instanceof World)) {
            throw new IllegalArgumentException("Origin is not a valid world");
        }
        World world = (World) extent;
        File history = MainUtil.getFile(Fawe.imp().getDirectory(), Settings.IMP.PATHS.HISTORY + File.separator + Fawe.imp().getWorldName(world));
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
            DiskStorageHistory.DiskStorageSummary summary = dsh.summarize(boundsPlus, shallow);
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
     * @param world
     * @param uuid
     * @param index
     * @return
     * @see DiskStorageHistory#toEditSession(FawePlayer)
     */
    public static DiskStorageHistory getChangeSetFromDisk(World world, UUID uuid, int index) {
        return new DiskStorageHistory(world, uuid, index);
    }

    /**
     * Compare two versions
     *
     * @param version
     * @param major
     * @param minor
     * @param minor2
     * @return true if version is >= major, minor, minor2
     */
    public static boolean checkVersion(final int[] version, final int major, final int minor, final int minor2) {
        return (version[0] > major) || ((version[0] == major) && (version[1] > minor)) || ((version[0] == major) && (version[1] == minor) && (version[2] >= minor2));
    }


    @Deprecated
    public static int fixLighting(World world, Region selection) {
        return fixLighting(world, selection, null);
    }

    /**
     * Fix the lighting in a selection<br>
     * - First removes all lighting, then relights
     * - Relights in parallel (if enabled) for best performance<br>
     * - Also resends chunks<br>
     *
     * @param world
     * @param selection (assumes cuboid)
     * @return
     */
    public static int fixLighting(World world, Region selection, @Nullable IQueueExtent queue) {
        final BlockVector3 bot = selection.getMinimumPoint();
        final BlockVector3 top = selection.getMaximumPoint();

        final int minX = bot.getBlockX() >> 4;
        final int minZ = bot.getBlockZ() >> 4;

        final int maxX = top.getBlockX() >> 4;
        final int maxZ = top.getBlockZ() >> 4;

        int count = 0;
        if (queue == null) queue = createQueue(world, false);
        // Remove existing lighting first
        if (queue instanceof LightingExtent) {
            LightingExtent relighter = (LightingExtent) queue;
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    relighter.relightChunk(x, z);
                    count++;
                }
            }
        } else {
            throw new UnsupportedOperationException("Queue is not " + LightingExtent.class);
        }
        return count;
    }

    /**
     * Have a task run when the server is low on memory (configured threshold)
     *
     * @param run
     */
    public static void addMemoryLimitedTask(Runnable run) {
        MemUtil.addMemoryLimitedTask(run);
    }

    /**
     * Have a task run when the server is no longer low on memory (configured threshold)
     *
     * @param run
     */
    public static void addMemoryPlentifulTask(Runnable run) {
        MemUtil.addMemoryPlentifulTask(run);
    }

    /**
     * @return
     * @see BBC
     */
    public static BBC[] getTranslations() {
        return BBC.values();
    }

}
