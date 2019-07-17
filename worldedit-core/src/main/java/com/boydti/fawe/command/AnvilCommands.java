package com.boydti.fawe.command;

import static com.google.common.base.Preconditions.checkNotNull;

import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.jnbt.anvil.MCAClipboard;
import com.boydti.fawe.jnbt.anvil.MCAFile;
import com.boydti.fawe.jnbt.anvil.MCAFilter;
import com.boydti.fawe.jnbt.anvil.MCAFilterCounter;
import com.boydti.fawe.jnbt.anvil.MCAQueue;
import com.boydti.fawe.jnbt.anvil.filters.DebugFixP2Roads;
import com.boydti.fawe.jnbt.anvil.filters.DeleteBiomeFilterSimple;
import com.boydti.fawe.jnbt.anvil.filters.DeleteOldFilter;
import com.boydti.fawe.jnbt.anvil.filters.DeleteUnclaimedFilter;
import com.boydti.fawe.jnbt.anvil.filters.DeleteUninhabitedFilter;
import com.boydti.fawe.jnbt.anvil.filters.PlotTrimFilter;
import com.boydti.fawe.jnbt.anvil.filters.RemapFilter;
import com.boydti.fawe.jnbt.anvil.filters.RemoveLayerFilter;
import com.boydti.fawe.jnbt.anvil.filters.SetPatternFilter;
import com.boydti.fawe.jnbt.anvil.filters.TrimAirFilter;
import com.boydti.fawe.jnbt.anvil.history.IAnvilHistory;
import com.boydti.fawe.jnbt.anvil.history.NullAnvilHistory;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.object.RunnableVal4;
import com.boydti.fawe.object.changeset.AnvilHistory;
import com.boydti.fawe.object.clipboard.remap.ClipboardRemapper;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.SetQueue;
import org.enginehub.piston.annotation.Command;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.annotation.Selection;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.command.binding.Switch;
import com.sk89q.worldedit.util.command.parametric.Optional;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.function.Consumer;

@Command(aliases = {"/anvil"}, desc = "Manipulate billions of blocks: [More Info](https://github.com/boy0001/FastAsyncWorldedit/wiki/Anvil-API)")
public class AnvilCommands {

    private final WorldEdit worldEdit;

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public AnvilCommands(WorldEdit worldEdit) {
        checkNotNull(worldEdit);
        this.worldEdit = worldEdit;
    }

    /**
     * Run safely on an unloaded world (no selection)
     *
     * @param player
     * @param folder
     * @param filter
     * @param <G>
     * @param <T>
     * @return
     */
    @Deprecated
    public static <G, T extends MCAFilter<G>> T runWithWorld(Player player, String folder, T filter, boolean force) {
        return runWithWorld(player, folder, filter, force, false);
    }


    @Deprecated
    public static <G, T extends MCAFilter<G>> T runWithWorld(Player player, String folder, T filter, boolean force, boolean unsafe) {
        boolean copy = false;
        if (FaweAPI.getWorld(folder) != null) {
            if (!force) {
                BBC.WORLD_IS_LOADED.send(player);
                return null;
            }
            copy = true;
        }
        FaweQueue defaultQueue = SetQueue.IMP.getNewQueue(folder, true, false);
        MCAQueue queue = new MCAQueue(defaultQueue);
        if (copy && !unsafe) {
            return queue.filterCopy(filter, RegionWrapper.GLOBAL());
        } else {
            return queue.filterWorld(filter);
        }
    }

    /**
     * Run safely on an existing world within a selection
     *
     * @param player
     * @param editSession
     * @param selection
     * @param filter
     * @param <G>
     * @param <T>
     * @return
     */
    @Deprecated
    public static <G, T extends MCAFilter<G>> T runWithSelection(Player player, EditSession editSession, Region selection, T filter) {
        if (!(selection instanceof CuboidRegion)) {
            BBC.NO_REGION.send(player);
            return null;
        }
        CuboidRegion cuboid = (CuboidRegion) selection;
        RegionWrapper wrappedRegion = new RegionWrapper(cuboid.getMinimumPoint(), cuboid.getMaximumPoint());
        String worldName = editSession.getWorld().getName();
        FaweQueue tmp = SetQueue.IMP.getNewQueue(worldName, true, false);
        MCAQueue queue = new MCAQueue(tmp);
        FawePlayer<Object> fp = FawePlayer.wrap(player);
        fp.checkAllowedRegion(selection);
        recordHistory(fp, editSession.getWorld(), iAnvilHistory -> {
            queue.filterCopy(filter, wrappedRegion, iAnvilHistory);
        });
        return filter;
    }

    public static void recordHistory(FawePlayer fp, World world, Consumer<IAnvilHistory> run) {
        LocalSession session = fp.getSession();
        if (session == null || session.hasFastMode()) {
            run.accept(new NullAnvilHistory());
        } else {
            AnvilHistory history = new AnvilHistory(world.getName(), fp.getUUID());
            run.accept(history);
            session.remember(fp.getPlayer(), world, history, fp.getLimit());
        }
    }

    @Command(
            name = "replaceall",
            aliases = {"rea", "repall"},
            desc = "Replace all blocks in the selection with another",
            descFooter = "The -d flag disabled wildcard data matching\n"
)
    @CommandPermissions("worldedit.anvil.replaceall")
    public void replaceAll(Player player, String folder, @Arg(name = "from", desc = "String", def = "") String from, String to, @Switch('d') boolean useData) throws WorldEditException {
//        final FaweBlockMatcher matchFrom;
//        if (from == null) {
//            matchFrom = FaweBlockMatcher.NOT_AIR;
//        } else {
//            if (from.contains(":")) {
//                useData = true; //override d flag, if they specified data they want it
//            }
//            matchFrom = FaweBlockMatcher.fromBlocks(worldEdit.getBlocks(player, from, true), useData);
//        }
//        final FaweBlockMatcher matchTo = FaweBlockMatcher.setBlocks(worldEdit.getBlocks(player, to, true));
//        ReplaceSimpleFilter filter = new ReplaceSimpleFilter(matchFrom, matchTo);
//        ReplaceSimpleFilter result = runWithWorld(player, folder, filter, true);
//        if (result != null) player.print(BBC.VISITOR_BLOCK.format(result.getTotal()));
    }

    @Command(
            name = "remapall",
            descFooter = "Remap the world between MCPE/PC values",
            desc = "Remap the world between MCPE/PC values"
    )
    @CommandPermissions("worldedit.anvil.remapall")
    public void remapall(Player player, String folder) throws WorldEditException {
        ClipboardRemapper mapper;
        ClipboardRemapper.RemapPlatform from;
        ClipboardRemapper.RemapPlatform to;
        from = ClipboardRemapper.RemapPlatform.PE;
        to = ClipboardRemapper.RemapPlatform.PC;
        RemapFilter filter = new RemapFilter(from, to);
        RemapFilter result = runWithWorld(player, folder, filter, true);
        if (result != null) player.print(BBC.VISITOR_BLOCK.format(result.getTotal()));
    }


    @Command(
            name = "deleteallunvisited",
            aliases = {"delunvisited" },
            desc = "Delete all chunks which haven't been occupied",
            descFooter = "occupied for `age-ticks` (20t = 1s) and \n" +
                    "Have not been accessed since `file-duration` (ms) after creation and\n" +
                    "Have not been used in the past `chunk-inactivity` (ms)" +
                    "The auto-save interval is the recommended value for `file-duration` and `chunk-inactivity`"
    )
    @CommandPermissions("worldedit.anvil.deleteallunvisited")
    public void deleteAllUnvisited(Player player, String folder, int inhabitedTicks, @Arg(name = "filedurationmillis", desc = "int", def = "60000") int fileDurationMillis) throws WorldEditException {
        DeleteUninhabitedFilter filter = new DeleteUninhabitedFilter(fileDurationMillis, inhabitedTicks, fileDurationMillis);
        DeleteUninhabitedFilter result = runWithWorld(player, folder, filter, true);
        if (result != null) player.print(BBC.VISITOR_BLOCK.format(result.getTotal()));
    }

    @Command(
            name = "deleteallunclaimed",
            aliases = {"delallunclaimed" },
            desc = "Delete all chunks which haven't been occupied",
            descFooter = "Supports: WG, P2, GP:\n" +
                    "Delete all chunks which aren't claimed AND haven't been occupied for `age-ticks` (20t = 1s) and \n" +
                    "Have not been accessed since `file-duration` (ms) after creation and\n" +
                    "Have not been used in the past `chunk-inactivity` (ms)" +
                    "The auto-save interval is the recommended value for `file-duration` and `chunk-inactivity`"
)
    @CommandPermissions("worldedit.anvil.deleteallunclaimed")
    public void deleteAllUnclaimed(Player player, int inhabitedTicks, @Arg(name = "filedurationmillis", desc = "int", def = "60000") int fileDurationMillis, @Switch('d') boolean debug) throws WorldEditException {
        String folder = player.getWorld().getName();
        DeleteUnclaimedFilter filter = new DeleteUnclaimedFilter(player.getWorld(), fileDurationMillis, inhabitedTicks, fileDurationMillis);
        if (debug) filter.enableDebug();
        DeleteUnclaimedFilter result = runWithWorld(player, folder, filter, true);
        if (result != null) player.print(BBC.VISITOR_BLOCK.format(result.getTotal()));
    }

    @Command(
            name = "deleteunclaimed",
            desc = "Delete all chunks which haven't been occupied",
            descFooter = "(Supports: WG, P2, GP):\n" +
                    "Is not claimed\n" +
                    "Has not been occupied for `age-ticks` (20t = 1s) and \n" +
                    "Have not been accessed since `file-duration` (ms) after creation and\n" +
                    "Have not been used in the past `chunk-inactivity` (ms)" +
                    "The auto-save interval is the recommended value for `file-duration` and `chunk-inactivity`"
)
    @CommandPermissions("worldedit.anvil.deleteunclaimed")
    public void deleteUnclaimed(Player player, EditSession editSession, @Selection Region selection, int inhabitedTicks, @Arg(name = "filedurationmillis", desc = "int", def = "60000") int fileDurationMillis, @Switch('d') boolean debug) throws WorldEditException {
        DeleteUnclaimedFilter filter = new DeleteUnclaimedFilter(player.getWorld(), fileDurationMillis, inhabitedTicks, fileDurationMillis);
        if (debug) filter.enableDebug();
        DeleteUnclaimedFilter result = runWithSelection(player, editSession, selection, filter);
        if (result != null) player.print(BBC.VISITOR_BLOCK.format(result.getTotal()));
    }

    @Command(
            name = "deletealloldregions",
            aliases = {"deloldreg" },
            desc = "Delete regions which haven't been accessed in a certain amount of time",
            descFooter = "You can use seconds (s), minutes (m), hours (h), days (d), weeks (w), years (y)\n" +
                    "(months are not a unit of time)\n" +
                    "E.g. 8h5m12s\n"
    )
    @CommandPermissions("worldedit.anvil.deletealloldregions")
    public void deleteAllOldRegions(Player player, String folder, String time) throws WorldEditException {
        long duration = MainUtil.timeToSec(time) * 1000L;
        DeleteOldFilter filter = new DeleteOldFilter(duration);
        DeleteOldFilter result = runWithWorld(player, folder, filter, true);
        if (result != null) player.print(BBC.VISITOR_BLOCK.format(result.getTotal()));
    }

    @Command(
            name = "trimallplots",
            desc = "Trim chunks in a Plot World",
            descFooter = "Unclaimed chunks will be deleted\n" +
                    "Unmodified chunks will be deleted\n" +
                    "Use -v to also delete unvisited chunks\n"
    )
    @CommandPermissions("worldedit.anvil.trimallplots")
    public void trimAllPlots(Player player, @Switch('v') boolean deleteUnvisited) throws WorldEditException {
        String folder = player.getWorld().getName();
        int visitTime = deleteUnvisited ? 1 : -1;
        PlotTrimFilter filter = new PlotTrimFilter(player.getWorld(), 0, visitTime, 600000);
//        PlotTrimFilter result = runWithWorld(player, folder, filter, true);
        FaweQueue defaultQueue = SetQueue.IMP.getNewQueue(folder, true, false);
        MCAQueue queue = new MCAQueue(defaultQueue);
        PlotTrimFilter result = queue.filterWorld(filter);
        if (result != null) player.print(BBC.VISITOR_BLOCK.format(result.getTotal()));
    }

    @Command(
            name = "deletebiomechunks",
            desc = "Delete chunks matching a specific biome"
    )
    @CommandPermissions("worldedit.anvil.trimallair")
    public void deleteBiome(Player player, String folder, BiomeType biome, @Switch('u') boolean unsafe) {
        DeleteBiomeFilterSimple filter = new DeleteBiomeFilterSimple(biome);
        DeleteBiomeFilterSimple result = runWithWorld(player, folder, filter, true, unsafe);
        if (result != null) player.print(BBC.VISITOR_BLOCK.format(result.getTotal()));
    }

    @Command(
            name = "trimallair",
            desc = "Trim all air in the world"
    )
    @CommandPermissions("worldedit.anvil.trimallair")
    public void trimAllAir(Player player, String folder, @Switch('u') boolean unsafe) throws WorldEditException {
        TrimAirFilter filter = new TrimAirFilter();
        TrimAirFilter result = runWithWorld(player, folder, filter, true, unsafe);
        if (result != null) player.print(BBC.VISITOR_BLOCK.format(result.getTotal()));
    }

    @Command(
            name = "debugfixroads",
            desc = "debug - do not use"
    )
    @CommandPermissions("worldedit.anvil.debugfixroads")
    public void debugfixroads(Player player, String folder) throws WorldEditException {
        DebugFixP2Roads filter = new DebugFixP2Roads();
        DebugFixP2Roads result = runWithWorld(player, folder, filter, true, true);
        if (result != null) player.print(BBC.VISITOR_BLOCK.format(result.getTotal()));
    }

    @Command(
            name = "replaceallpattern",
            aliases = {"reap", "repallpat"},
            desc = "Replace all blocks in the selection with another"
)
    @CommandPermissions("worldedit.anvil.replaceall")
    public void replaceAllPattern(Player player, String folder, @Arg(name = "from", desc = "String", def = "") String from, final Pattern to, @Switch('d') boolean useData, @Switch('m') boolean useMap) throws WorldEditException {
//        MCAFilterCounter filter;
//        if (useMap) {
//            if (to instanceof RandomPattern) {
//                List<String> split = StringMan.split(from, ',');
//                filter = new MappedReplacePatternFilter(from, (RandomPattern) to, useData);
//            } else {
//                player.print("Must be a pattern list!");
//                return;
//            }
//        } else {
//            final FaweBlockMatcher matchFrom;
//            if (from == null) {
//                matchFrom = FaweBlockMatcher.NOT_AIR;
//            } else {
//                matchFrom = FaweBlockMatcher.fromBlocks(worldEdit.getBlocks(player, from, true), useData || from.contains(":"));
//            }
//            filter = new ReplacePatternFilter(matchFrom, to);
//        }
//        MCAFilterCounter result = runWithWorld(player, folder, filter, true);
//        if (result != null) player.print(BBC.VISITOR_BLOCK.format(result.getTotal()));
    }
//
    @Command(
            name = "countall",
            desc = "Count all blocks in a world"
)
    @CommandPermissions("worldedit.anvil.countall")
    public void countAll(Player player, EditSession editSession, String folder, String arg, @Switch('d') boolean useData) throws WorldEditException {
//        Set<BaseBlock> searchBlocks = worldEdit.getBlocks(player, arg, true);
//        MCAFilterCounter filter;
//        if (useData || arg.contains(":")) { // Optimize for both cases
//            CountFilter counter = new CountFilter();
//            searchBlocks.forEach(counter::addBlock);
//            filter = counter;
//        } else {
//            CountIdFilter counter = new CountIdFilter();
//            searchBlocks.forEach(counter::addBlock);
//            filter = counter;
//        }
//        MCAFilterCounter result = runWithWorld(player, folder, filter, true);
//        if (result != null) player.print(BBC.SELECTION_COUNT.format(result.getTotal()));
    }

    @Command(
            name = "clear",
            aliases = {"unset"},
            desc = "Clear the chunks in a selection (delete without defrag)"
    )
    @CommandPermissions("worldedit.anvil.clear")
    public void unset(Player player, EditSession editSession, @Selection Region selection) throws WorldEditException {
        BlockVector3 bot = selection.getMinimumPoint();
        BlockVector3 top = selection.getMaximumPoint();
        RegionWrapper region = new RegionWrapper(bot, top);

        MCAFilterCounter filter = new MCAFilterCounter() {
            @Override
            public MCAFile applyFile(MCAFile file) {
                int X = file.getX();
                int Z = file.getZ();
                int bcx = X << 5;
                int bcz = Z << 5;
                int bx = X << 9;
                int bz = Z << 9;
                if (region.isIn(bx, bz) && region.isIn(bx + 511, bz + 511)) {
                    file.setDeleted(true);
                    get().add(512 * 512 * 256);
                } else if (region.isInMCA(X, Z)) {
                    file.init();
                    final byte[] empty = new byte[4];
                    RandomAccessFile raf = file.getRandomAccessFile();
                    file.forEachChunk(new RunnableVal4<Integer, Integer, Integer, Integer>() {
                        @Override
                        public void run(Integer cx, Integer cz, Integer offset, Integer size) {
                            if (region.isInChunk(bcx + cx, bcz + cz)) {
                                int index = ((cx & 31) << 2) + ((cz & 31) << 7);
                                try {
                                    raf.seek(index);
                                    raf.write(empty);
                                    get().add(16 * 16 * 256);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                    file.clear();
                }
                return null;
            }
        };
        MCAFilterCounter result = runWithSelection(player, editSession, selection, filter);
        if (result != null) player.print(BBC.VISITOR_BLOCK.format(result.getTotal()));
    }

    @Command(
            name = "count",
            desc = "Count blocks in a selection"
)
    @CommandPermissions("worldedit.anvil.count")
    public void count(Player player, EditSession editSession, @Selection Region selection, String arg, @Switch('d') boolean useData) throws WorldEditException {
//        Set<BaseBlock> searchBlocks = worldEdit.getBlocks(player, arg, true);
//        MCAFilterCounter filter;
//        if (useData || arg.contains(":")) { // Optimize for both cases
//            CountFilter counter = new CountFilter();
//            searchBlocks.forEach(counter::addBlock);
//            filter = counter;
//        } else {
//            CountIdFilter counter = new CountIdFilter();
//            searchBlocks.forEach(counter::addBlock);
//            filter = counter;
//        }
//        MCAFilterCounter result = runWithSelection(player, editSession, selection, filter);
//        if (result != null) player.print(BBC.SELECTION_COUNT.format(result.getTotal()));
    }
//
    @Command(
            name = "distr",
            desc = "Replace all blocks in the selection with another"
    )
    @CommandPermissions("worldedit.anvil.distr")
    public void distr(Player player, EditSession editSession, @Selection Region selection, @Switch('d') boolean useData) throws WorldEditException {
//        long total = 0;
//        long[] count;
//        MCAFilter<long[]> counts;
//        if (useData) {
//            counts = runWithSelection(player, editSession, selection, new MCAFilter<long[]>() {
//                @Override
//                public void applyBlock(int x, int y, int z, BaseBlock block, long[] counts) {
//                    counts[block.getCombined()]++;
//                }
//
//                @Override
//                public long[] init() {
//                    return new long[Character.MAX_VALUE + 1];
//                }
//            });
//            count = new long[Character.MAX_VALUE + 1];
//        } else {
//            counts = runWithSelection(player, editSession, selection, new MCAFilter<long[]>() {
//                @Override
//                public void applyBlock(int x, int y, int z, BaseBlock block, long[] counts) {
//                    counts[block.getId()]++;
//                }
//
//                @Override
//                public long[] init() {
//                    return new long[4096];
//                }
//            });
//            count = new long[4096];
//        }
//        for (long[] value : counts) {
//            for (int i = 0; i < value.length; i++) {
//                count[i] += value[i];
//                total += value[i];
//            }
//        }
//        ArrayList<long[]> map = new ArrayList<>();
//        for (int i = 0; i < count.length; i++) {
//            if (count[i] != 0) map.add(new long[]{i, count[i]});
//        }
//        Collections.sort(map, new Comparator<long[]>() {
//            @Override
//            public int compare(long[] a, long[] b) {
//                long vA = a[1];
//                long vB = b[1];
//                return (vA < vB) ? -1 : ((vA == vB) ? 0 : 1);
//            }
//        });
//        if (useData) {
//            for (long[] c : map) {
//                BaseBlock block = FaweCache.CACHE_BLOCK[(int) c[0]];
//                String name = BlockType.fromID(block.getId()).getName();
//                String str = String.format("%-7s (%.3f%%) %s #%d:%d",
//                        String.valueOf(c[1]),
//                        ((c[1] * 10000) / total) / 100d,
//                        name == null ? "Unknown" : name,
//                        block.getType(), block.getData());
//                player.print(str);
//            }
//        } else {
//            for (long[] c : map) {
//                BlockType block = BlockType.fromID((int) c[0]);
//                String str = String.format("%-7s (%.3f%%) %s #%d",
//                        String.valueOf(c[1]),
//                        ((c[1] * 10000) / total) / 100d,
//                        block == null ? "Unknown" : block.getName(), c[0]);
//                player.print(str);
//            }
//        }
    }
//
    @Command(
            name = "replace",
            aliases = {"r"},
            desc = "Replace all blocks in the selection with another"
    )
    @CommandPermissions("worldedit.anvil.replace")
    public void replace(Player player, EditSession editSession, @Selection Region selection, @Arg(name = "from", desc = "String", def = "") String from, String to, @Switch('d') boolean useData) throws WorldEditException {
//        final FaweBlockMatcher matchFrom;
//        if (from == null) {
//            matchFrom = FaweBlockMatcher.NOT_AIR;
//        } else {
//            matchFrom = FaweBlockMatcher.fromBlocks(worldEdit.getBlocks(player, from, true), useData || from.contains(":"));
//        }
//        final FaweBlockMatcher matchTo = FaweBlockMatcher.setBlocks(worldEdit.getBlocks(player, to, true));
//        ReplaceSimpleFilter filter = new ReplaceSimpleFilter(matchFrom, matchTo);
//        MCAFilterCounter result = runWithSelection(player, editSession, selection, filter);
//        if (result != null) {
//            player.print(BBC.VISITOR_BLOCK.format(result.getTotal()));
//        }
    }
//
    @Command(
            name = "replacepattern",
            aliases = {"preplace", "rp"},
            desc = "Replace all blocks in the selection with a pattern"
    )
    @CommandPermissions("worldedit.anvil.replace")
    // Player player, String folder, @Arg(name = "from", desc = "String", def = "") String from, final Pattern to, @Switch('d') boolean useData, @Switch('m') boolean useMap
    public void replacePattern(Player player, EditSession editSession, @Selection Region selection, @Arg(name = "from", desc = "String", def = "") String from, final Pattern to, @Switch('d') boolean useData, @Switch('m') boolean useMap) throws WorldEditException {
//        MCAFilterCounter filter;
//        if (useMap) {
//            if (to instanceof RandomPattern) {
//                List<String> split = StringMan.split(from, ',');
//                filter = new MappedReplacePatternFilter(from, (RandomPattern) to, useData);
//            } else {
//                player.print("Must be a pattern list!");
//                return;
//            }
//        } else {
//            final FaweBlockMatcher matchFrom;
//            if (from == null) {
//                matchFrom = FaweBlockMatcher.NOT_AIR;
//            } else {
//                matchFrom = FaweBlockMatcher.fromBlocks(worldEdit.getBlocks(player, from, true), useData || from.contains(":"));
//            }
//            filter = new ReplacePatternFilter(matchFrom, to);
//        }
//        MCAFilterCounter result = runWithSelection(player, editSession, selection, filter);
//        if (result != null) {
//            player.print(BBC.VISITOR_BLOCK.format(result.getTotal()));
//        }
    }

    @Command(
            name = "set",
            desc = "Set all blocks in the selection with a pattern"
    )
    @CommandPermissions("worldedit.anvil.set")
    // Player player, String folder, @Arg(name = "from", desc = "String", def = "") String from, final Pattern to, @Switch('d') boolean useData, @Switch('m') boolean useMap
    public void set(Player player, EditSession editSession, @Selection Region selection, final Pattern to) throws WorldEditException {
        MCAFilterCounter filter = new SetPatternFilter(to);
        MCAFilterCounter result = runWithSelection(player, editSession, selection, filter);
        if (result != null) {
            player.print(BBC.VISITOR_BLOCK.format(result.getTotal()));
        }
    }

    @Command(
            name = "removelayers",
            desc = "Removes matching chunk layers",
            descFooter = "Only if a chunk matches the provided id"
    )
    @CommandPermissions("worldedit.anvil.removelayer")
    public void removeLayers(Player player, EditSession editSession, @Selection Region selection, int id) throws WorldEditException {
        BlockVector3 min = selection.getMinimumPoint();
        BlockVector3 max = selection.getMaximumPoint();
        int minY = min.getBlockY();
        int maxY = max.getBlockY();
        RemoveLayerFilter filter = new RemoveLayerFilter(minY, maxY, id);
        MCAFilterCounter result = runWithSelection(player, editSession, selection, filter);
        if (result != null) {
            player.print(BBC.VISITOR_BLOCK.format(result.getTotal()));
        }
    }


    @Command(
            name = "copy",
            desc = "Lazily copy chunks to your anvil clipboard"
    )
    @CommandPermissions("worldedit.anvil.copychunks")
    public void copy(Player player, LocalSession session, EditSession editSession, @Selection Region selection) throws WorldEditException {
        if (!(selection instanceof CuboidRegion)) {
            BBC.NO_REGION.send(player);
            return;
        }
        CuboidRegion cuboid = (CuboidRegion) selection;
        String worldName = editSession.getWorld().getName();
        FaweQueue tmp = SetQueue.IMP.getNewQueue(worldName, true, false);
        MCAQueue queue = new MCAQueue(tmp);
        BlockVector3 origin = session.getPlacementPosition(player);
        MCAClipboard clipboard = new MCAClipboard(queue, cuboid, origin);
        FawePlayer fp = FawePlayer.wrap(player);
        fp.setMeta(FawePlayer.METADATA_KEYS.ANVIL_CLIPBOARD, clipboard);
        BBC.COMMAND_COPY.send(player, selection.getArea());
    }

    @Command(
            name = "paste",
            desc = "Paste chunks from your anvil clipboard",
            descFooter =
                    "Paste the chunks from your anvil clipboard.\n" +
                            "The -c flag will align the paste to the chunks.",

    )
    @CommandPermissions("worldedit.anvil.pastechunks")
    public void paste(Player player, LocalSession session, EditSession editSession, @Switch('c') boolean alignChunk) throws WorldEditException, IOException {
//        FawePlayer fp = FawePlayer.wrap(player);
//        MCAClipboard clipboard = fp.getMeta(FawePlayer.METADATA_KEYS.ANVIL_CLIPBOARD);
//        if (clipboard == null) {
//            fp.sendMessage("You must first use `//anvil copy`");
//            return;
//        }
//        CuboidRegion cuboid = clipboard.getRegion();
//        RegionWrapper copyRegion = new RegionWrapper(cuboid.getMinimumPoint(), cuboid.getMaximumPoint());
//        final Vector offset = player.getPosition().subtract(clipboard.getOrigin());
//        if (alignChunk) {
//            offset.setComponents((offset.getBlockX() >> 4) << 4, offset.getBlockY(), (offset.getBlockZ() >> 4) << 4);
//        }
//        int oX = offset.getBlockX();
//        int oZ = offset.getBlockZ();
//        RegionWrapper pasteRegion = new RegionWrapper(copyRegion.minX + oX, copyRegion.maxX + oX, copyRegion.minZ + oZ, copyRegion.maxZ + oZ);
//        String pasteWorldName = Fawe.imp().getWorldName(editSession.getWorld());
//        FaweQueue tmpTo = SetQueue.IMP.getNewQueue(pasteWorldName, true, false);
//        MCAQueue copyQueue = clipboard.getQueue();
//        MCAQueue pasteQueue = new MCAQueue(tmpTo);
//
//        fp.checkAllowedRegion(pasteRegion);
//        recordHistory(fp, editSession.getWorld(), iAnvilHistory -> {
//            try {
//                pasteQueue.pasteRegion(copyQueue, copyRegion, offset, iAnvilHistory);
//            } catch (IOException e) { throw new RuntimeException(e); }
//        });
//        BBC.COMMAND_PASTE.send(player, player.getPosition().toBlockVector());
    }
}
