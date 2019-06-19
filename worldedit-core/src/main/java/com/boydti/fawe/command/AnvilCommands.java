package com.boydti.fawe.command;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.jnbt.anvil.*;
import com.boydti.fawe.jnbt.anvil.filters.*;
import com.boydti.fawe.jnbt.anvil.history.IAnvilHistory;
import com.boydti.fawe.jnbt.anvil.history.NullAnvilHistory;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.object.RunnableVal4;
import com.boydti.fawe.object.changeset.AnvilHistory;
import com.boydti.fawe.object.clipboard.remap.ClipboardRemapper;
import com.boydti.fawe.object.mask.FaweBlockMatcher;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.SetQueue;
import com.boydti.fawe.util.StringMan;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.pattern.RandomPattern;
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
import java.util.*;
import java.util.function.Consumer;


import static com.google.common.base.Preconditions.checkNotNull;

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
            aliases = {"replaceall", "rea", "repall"},
            usage = "<folder> [from-block] <to-block>",
            desc = "Replace all blocks in the selection with another",
            help = "Replace all blocks in the selection with another\n" +
                    "The -d flag disabled wildcard data matching\n",
            flags = "df",
            min = 2,
            max = 4
    )
    @CommandPermissions("worldedit.anvil.replaceall")
    public void replaceAll(Player player, String folder, @Optional String from, String to, @Switch('d') boolean useData) throws WorldEditException {
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
            aliases = {"remapall"},
            usage = "<folder>",
            help = "Remap the world between MCPE/PC values",
            desc = "Remap the world between MCPE/PC values",
            min = 1,
            max = 1
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
            aliases = {"deleteallunvisited", "delunvisited" },
            usage = "<folder> <age-ticks> [file-age=60000]",
            desc = "Delete all chunks which haven't been occupied",
            help = "Delete all chunks which haven't been occupied for `age-ticks` (20t = 1s) and \n" +
                    "Have not been accessed since `file-duration` (ms) after creation and\n" +
                    "Have not been used in the past `chunk-inactivity` (ms)" +
                    "The auto-save interval is the recommended value for `file-duration` and `chunk-inactivity`",
            min = 2,
            max = 3
    )
    @CommandPermissions("worldedit.anvil.deleteallunvisited")
    public void deleteAllUnvisited(Player player, String folder, int inhabitedTicks, @Optional("60000") int fileDurationMillis) throws WorldEditException {
        DeleteUninhabitedFilter filter = new DeleteUninhabitedFilter(fileDurationMillis, inhabitedTicks, fileDurationMillis);
        DeleteUninhabitedFilter result = runWithWorld(player, folder, filter, true);
        if (result != null) player.print(BBC.VISITOR_BLOCK.format(result.getTotal()));
    }

    @Command(
            aliases = {"deleteallunclaimed", "delallunclaimed" },
            usage = "<age-ticks> [file-age=60000]",
            desc = "(Supports: WG, P2, GP) Delete all chunks which haven't been occupied AND claimed",
            help = "(Supports: WG, P2, GP) Delete all chunks which aren't claimed AND haven't been occupied for `age-ticks` (20t = 1s) and \n" +
                    "Have not been accessed since `file-duration` (ms) after creation and\n" +
                    "Have not been used in the past `chunk-inactivity` (ms)" +
                    "The auto-save interval is the recommended value for `file-duration` and `chunk-inactivity`",
            min = 1,
            max = 3
    )
    @CommandPermissions("worldedit.anvil.deleteallunclaimed")
    public void deleteAllUnclaimed(Player player, int inhabitedTicks, @Optional("60000") int fileDurationMillis, @Switch('d') boolean debug) throws WorldEditException {
        String folder = player.getWorld().getName();
        DeleteUnclaimedFilter filter = new DeleteUnclaimedFilter(player.getWorld(), fileDurationMillis, inhabitedTicks, fileDurationMillis);
        if (debug) filter.enableDebug();
        DeleteUnclaimedFilter result = runWithWorld(player, folder, filter, true);
        if (result != null) player.print(BBC.VISITOR_BLOCK.format(result.getTotal()));
    }

    @Command(
            aliases = {"deleteunclaimed"},
            usage = "<age-ticks> [file-age=60000]",
            desc = "(Supports: WG, P2, GP) Delete all chunks which haven't been occupied AND claimed",
            help = "(Supports: WG, P2, GP) Delete all chunks which aren't claimed AND haven't been occupied for `age-ticks` (20t = 1s) and \n" +
                    "Have not been accessed since `file-duration` (ms) after creation and\n" +
                    "Have not been used in the past `chunk-inactivity` (ms)" +
                    "The auto-save interval is the recommended value for `file-duration` and `chunk-inactivity`",
            min = 1,
            max = 3
    )
    @CommandPermissions("worldedit.anvil.deleteunclaimed")
    public void deleteUnclaimed(Player player, EditSession editSession, @Selection Region selection, int inhabitedTicks, @Optional("60000") int fileDurationMillis, @Switch('d') boolean debug) throws WorldEditException {
        DeleteUnclaimedFilter filter = new DeleteUnclaimedFilter(player.getWorld(), fileDurationMillis, inhabitedTicks, fileDurationMillis);
        if (debug) filter.enableDebug();
        DeleteUnclaimedFilter result = runWithSelection(player, editSession, selection, filter);
        if (result != null) player.print(BBC.VISITOR_BLOCK.format(result.getTotal()));
    }

    @Command(
            aliases = {"deletealloldregions", "deloldreg" },
            usage = "<folder> <time>",
            desc = "Delete regions which haven't been accessed in a certain amount of time",
            help = "Delete regions which haven't been accessed in a certain amount of time\n" +
                    "You can use seconds (s), minutes (m), hours (h), days (d), weeks (w), years (y)\n" +
                    "(months are not a unit of time)\n" +
                    "E.g. 8h5m12s\n",
            min = 2,
            max = 3
    )
    @CommandPermissions("worldedit.anvil.deletealloldregions")
    public void deleteAllOldRegions(Player player, String folder, String time) throws WorldEditException {
        long duration = MainUtil.timeToSec(time) * 1000L;
        DeleteOldFilter filter = new DeleteOldFilter(duration);
        DeleteOldFilter result = runWithWorld(player, folder, filter, true);
        if (result != null) player.print(BBC.VISITOR_BLOCK.format(result.getTotal()));
    }

    @Command(
            aliases = {"trimallplots", },
            desc = "Trim chunks in a Plot World",
            help = "Trim chunks in a Plot World\n" +
                    "Unclaimed chunks will be deleted\n" +
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
            aliases = {"deletebiomechunks", },
            desc = "Delete chunks matching a specific biome"
    )
    @CommandPermissions("worldedit.anvil.trimallair")
    public void deleteBiome(Player player, String folder, BiomeType biome, @Switch('u') boolean unsafe) {
        DeleteBiomeFilterSimple filter = new DeleteBiomeFilterSimple(biome);
        DeleteBiomeFilterSimple result = runWithWorld(player, folder, filter, true, unsafe);
        if (result != null) player.print(BBC.VISITOR_BLOCK.format(result.getTotal()));
    }

    @Command(
            aliases = {"trimallair", },
            desc = "Trim all air in the world"
    )
    @CommandPermissions("worldedit.anvil.trimallair")
    public void trimAllAir(Player player, String folder, @Switch('u') boolean unsafe) throws WorldEditException {
        TrimAirFilter filter = new TrimAirFilter();
        TrimAirFilter result = runWithWorld(player, folder, filter, true, unsafe);
        if (result != null) player.print(BBC.VISITOR_BLOCK.format(result.getTotal()));
    }

    @Command(
            aliases = {"debugfixroads", },
            desc = "debug - do not use"
    )
    @CommandPermissions("worldedit.anvil.debugfixroads")
    public void debugfixroads(Player player, String folder) throws WorldEditException {
        DebugFixP2Roads filter = new DebugFixP2Roads();
        DebugFixP2Roads result = runWithWorld(player, folder, filter, true, true);
        if (result != null) player.print(BBC.VISITOR_BLOCK.format(result.getTotal()));
    }

    @Command(
            aliases = {"replaceallpattern", "reap", "repallpat"},
            usage = "<folder> [from-block] <to-pattern>",
            desc = "Replace all blocks in the selection with another",
            flags = "dm",
            min = 2,
            max = 4
    )
    @CommandPermissions("worldedit.anvil.replaceall")
    public void replaceAllPattern(Player player, String folder, @Optional String from, final Pattern to, @Switch('d') boolean useData, @Switch('m') boolean useMap) throws WorldEditException {
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
            aliases = {"countall"},
            usage = "<folder> [hasSky] <id>",
            desc = "Count all blocks in a world",
            flags = "d",
            min = 2,
            max = 3
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
            aliases = {"clear", "unset"},
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
            aliases = {"count"},
            usage = "<ids>",
            desc = "Count blocks in a selection",
            flags = "d",
            min = 1,
            max = 2
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
            aliases = {"distr"},
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
            aliases = {"replace", "r"},
            usage = "[from-block] <to-block>",
            desc = "Replace all blocks in the selection with another"
    )
    @CommandPermissions("worldedit.anvil.replace")
    public void replace(Player player, EditSession editSession, @Selection Region selection, @Optional String from, String to, @Switch('d') boolean useData) throws WorldEditException {
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
            aliases = {"replacepattern", "preplace", "rp"},
            usage = "[from-mask] <to-pattern>",
            desc = "Replace all blocks in the selection with a pattern"
    )
    @CommandPermissions("worldedit.anvil.replace")
    // Player player, String folder, @Optional String from, final Pattern to, @Switch('d') boolean useData, @Switch('m') boolean useMap
    public void replacePattern(Player player, EditSession editSession, @Selection Region selection, @Optional String from, final Pattern to, @Switch('d') boolean useData, @Switch('m') boolean useMap) throws WorldEditException {
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
            aliases = {"set"},
            usage = "<to-pattern>",
            desc = "Set all blocks in the selection with a pattern"
    )
    @CommandPermissions("worldedit.anvil.set")
    // Player player, String folder, @Optional String from, final Pattern to, @Switch('d') boolean useData, @Switch('m') boolean useMap
    public void set(Player player, EditSession editSession, @Selection Region selection, final Pattern to) throws WorldEditException {
        MCAFilterCounter filter = new SetPatternFilter(to);
        MCAFilterCounter result = runWithSelection(player, editSession, selection, filter);
        if (result != null) {
            player.print(BBC.VISITOR_BLOCK.format(result.getTotal()));
        }
    }

    @Command(
            aliases = {"removelayers"},
            usage = "<id>",
            desc = "Removes matching chunk layers",
            help = "Remove if all the selected layers in a chunk match the provided id"
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
            aliases = {"copy"},
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
            aliases = {"paste"},
            desc = "Paste chunks from your anvil clipboard",
            help =
                    "Paste the chunks from your anvil clipboard.\n" +
                            "The -c flag will align the paste to the chunks.",
            flags = "c"

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
