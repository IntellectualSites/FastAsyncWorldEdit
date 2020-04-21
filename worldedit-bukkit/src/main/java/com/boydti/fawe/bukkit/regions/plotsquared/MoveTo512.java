package com.boydti.fawe.bukkit.regions.plotsquared;


import com.plotsquared.core.command.CommandCategory;
import com.plotsquared.core.command.CommandDeclaration;
import com.plotsquared.core.command.RequiredType;

@CommandDeclaration(
        command = "moveto512",
        permission = "plots.moveto512",
        category = CommandCategory.DEBUG,
        requiredType = RequiredType.CONSOLE,
        description = "Move plots to a 512 sized region",
        usage = "/plots moveto512 [world]"
)
// TODO FIXME
public class MoveTo512 /*extends Command*/ {

//    public MoveTo512() {
//        super(MainCommand.getInstance(), true);
//    }
//
//    private MCAChunk emptyPlot(MCAChunk chunk, HybridPlotWorld hpw) {
//        int maxLayer = (hpw.PLOT_HEIGHT) >> 4;
//        for (int i = maxLayer + 1; i < chunk.ids.length; i++) {
//            chunk.ids[i] = null;
//            chunk.data[i] = null;
//        }
//        for (int layer = 0; layer <= maxLayer; layer++) {
//            byte[] ids = chunk.ids[layer];
//            if (ids == null) {
//                ids = chunk.ids[layer] = new byte[4096];
//                chunk.data[layer] = new byte[2048];
//                chunk.skyLight[layer] = new byte[2048];
//                chunk.blockLight[layer] = new byte[2048];
//            } else {
//                Arrays.fill(ids, (byte) 0);
//                Arrays.fill(chunk.data[layer], (byte) 0);
//                Arrays.fill(chunk.skyLight[layer], (byte) 0);
//                Arrays.fill(chunk.blockLight[layer], (byte) 0);
//            }
//            if (layer == maxLayer) {
//                int yMax = hpw.PLOT_HEIGHT & 15;
//                for (int y = yMax + 1; y < 15; y++) {
//                    Arrays.fill(chunk.skyLight[layer], y << 7, (y << 7) + 128, (byte) 255);
//                }
//                if (layer == 0) {
//                    Arrays.fill(ids, 0, 256, (byte) 7);
//                    for (int y = 1; y < yMax; y++) {
//                        int y8 = y << 8;
//                        Arrays.fill(ids, y8, y8 + 256, (byte) 3);
//                    }
//                } else {
//                    for (int y = 0; y < yMax; y++) {
//                        int y8 = y << 8;
//                        Arrays.fill(ids, y8, y8 + 256, (byte) 3);
//                    }
//                }
//                int yMax15 = yMax & 15;
//                int yMax158 = yMax15 << 8;
//                Arrays.fill(ids, yMax158, yMax158 + 256, (byte) 2);
//                if (yMax != 15) {
//                    Arrays.fill(ids, yMax158 + 256, 4096, (byte) 0);
//                }
//            } else if (layer == 0){
//                Arrays.fill(ids, 256, 4096, (byte) 3);
//                Arrays.fill(ids, 0, 256, (byte) 7);
//            } else {
//                Arrays.fill(ids, (byte) 3);
//            }
//        }
//        return chunk;
//    }
//
//    private MCAChunk emptyRoad(MCAChunk chunk, HybridPlotWorld hpw) {
//        int maxLayer = (hpw.ROAD_HEIGHT) >> 4;
//        for (int i = maxLayer + 1; i < chunk.ids.length; i++) {
//            chunk.ids[i] = null;
//            chunk.data[i] = null;
//        }
//        for (int layer = 0; layer <= maxLayer; layer++) {
//            byte[] ids = chunk.ids[layer];
//            if (ids == null) {
//                ids = chunk.ids[layer] = new byte[4096];
//                chunk.data[layer] = new byte[2048];
//                chunk.skyLight[layer] = new byte[2048];
//                chunk.blockLight[layer] = new byte[2048];
//            } else {
//                Arrays.fill(ids, (byte) 0);
//                Arrays.fill(chunk.data[layer], (byte) 0);
//                Arrays.fill(chunk.skyLight[layer], (byte) 0);
//                Arrays.fill(chunk.blockLight[layer], (byte) 0);
//            }
//            if (layer == maxLayer) {
//                int yMax = hpw.ROAD_HEIGHT & 15;
//                for (int y = yMax + 1; y < 15; y++) {
//                    Arrays.fill(chunk.skyLight[layer], y << 7, (y << 7) + 128, (byte) 255);
//                }
//                if (layer == 0) {
//                    Arrays.fill(ids, 0, 256, (byte) 7);
//                    for (int y = 1; y <= yMax; y++) {
//                        int y8 = y << 8;
//                        Arrays.fill(ids, y8, y8 + 256, (byte) hpw.ROAD_BLOCK.id);
//                    }
//                } else {
//                    for (int y = 0; y <= yMax; y++) {
//                        int y8 = y << 8;
//                        Arrays.fill(ids, y8, y8 + 256, (byte) hpw.ROAD_BLOCK.id);
//                    }
//                }
//                if (yMax != 15) {
//                    int yMax15 = yMax & 15;
//                    int yMax158 = yMax15 << 8;
//                    Arrays.fill(ids, yMax158 + 256, 4096, (byte) 0);
//                }
//            } else if (layer == 0){
//                Arrays.fill(ids, 256, 4096, (byte) hpw.ROAD_BLOCK.id);
//                Arrays.fill(ids, 0, 256, (byte) 7);
//            } else {
//                Arrays.fill(ids, (byte) hpw.ROAD_BLOCK.id);
//            }
//        }
//        return chunk;
//    }

//    @Override
//    public void execute(PlotPlayer player, String[] args, RunnableVal3<Command, Runnable, Runnable> confirm, RunnableVal2<Command, CommandResult> whenDone) throws CommandException {
//        checkTrue(args.length == 1, Captions.COMMAND_SYNTAX, getUsage());
//        PlotArea area = player.getPlotAreaAbs();
//        check(area, Captions.COMMAND_SYNTAX, getUsage());
//        checkTrue(area instanceof HybridPlotWorld, Captions.NOT_VALID_HYBRID_PLOT_WORLD);
//
//        WorldUtil.IMP.saveWorld(area.worldname);
//
//        IQueueExtent defaultQueue = SetQueue.IMP.getNewQueue(area.worldname, true, false);
//        MCAQueue queueFrom = new MCAQueue(area.worldname, defaultQueue.getSaveFolder(), defaultQueue.hasSky());
//
//        String world = args[0];
//        File folder = new File(PS.imp().getWorldContainer(), world + File.separator + "region");
//        checkTrue(!folder.exists(), Captions.SETUP_WORLD_TAKEN, world);
//
//        HybridPlotWorld hpw = (HybridPlotWorld) area;
//        int minRoad = 7;
//        int pLen = Math.min(hpw.PLOT_WIDTH, 512 - minRoad);
//        int roadWidth = 512 - pLen;
//        int roadPosLower;
//        if ((roadWidth & 1) == 0) {
//            roadPosLower = (short) (Math.floor(roadWidth / 2) - 1);
//        } else {
//            roadPosLower = (short) Math.floor(roadWidth / 2);
//        }
//        int roadPosUpper = 512 - roadWidth + roadPosLower + 1;
//
//        final ThreadLocal<boolean[]> roadCache = new ThreadLocal<boolean[]>() {
//            @Override
//            protected boolean[] initialValue() {
//                return new boolean[64];
//            }
//        };
//
//        MCAChunk reference = new MCAChunk(null, 0, 0);
//        {
//            reference.fillCuboid(0, 15, 0, 0, 0, 15, 7, (byte) 0);
//            reference.fillCuboid(0, 15, 1, hpw.PLOT_HEIGHT - 1, 0, 15, 3, (byte) 0);
//            reference.fillCuboid(0, 15, hpw.PLOT_HEIGHT, hpw.PLOT_HEIGHT, 0, 15, 2, (byte) 0);
//        }
//
//        Map<PlotId, Plot> rawPlots = area.getPlotsRaw();
//        ArrayList<Plot> plots = new ArrayList(rawPlots.values());
//        int size = plots.size();
//
//        PlotId nextId = new PlotId(0, 0);
//
//        long start = System.currentTimeMillis();
//
//        int percent = 0;
//        for (Plot plot : plots) {
//            Fawe.debug(((percent += 100) / size) + "% complete!");
//
//            Location bot = plot.getBottomAbs();
//            Location top = plot.getTopAbs();
//
//            int oX = roadPosLower - bot.getX() + 1;
//            int oZ = roadPosLower - bot.getZ() + 1;
//
//            { // Move
//                PlotId id = plot.getId();
//                Fawe.debug("Moving " + plot.getId() + " to " + nextId);
//                id.x = nextId.x;
//                id.y = nextId.y;
//                id.recalculateHash();
//            }
//
//            MCAWriter writer = new MCAWriter(512, 512, folder) {
//
//                @Override
//                public boolean shouldWrite(int chunkX, int chunkZ) {
//                    int bx = chunkX << 4;
//                    int bz = chunkZ << 4;
//                    int tx = bx + 15;
//                    int tz = bz + 15;
//                    return !(tx < roadPosLower || tz < roadPosLower || bx > roadPosUpper || bz > roadPosUpper);
//                }
//
//                @Override
//                public MCAChunk write(MCAChunk newChunk, int bx, int tx, int bz, int tz) {
//                    Arrays.fill(newChunk.biomes, (byte) 4);
//                    if (!newChunk.tiles.isEmpty()) newChunk.tiles.clear();
//                    if (tx < roadPosLower || tz < roadPosLower || bx > roadPosUpper || bz > roadPosUpper) {
//                        return emptyRoad(newChunk, hpw);
//                    } else {
//                        boolean partRoad = (bx <= roadPosLower || bz <= roadPosLower || tx >= roadPosUpper || tz >= roadPosUpper);
//
//                        boolean changed = false;
//                        emptyPlot(newChunk, hpw);
//
//                        int obx = bx - oX;
//                        int obz = bz - oZ;
//                        int otx = tx - oX;
//                        int otz = tz - oZ;
//                        int otherBCX = (obx) >> 4;
//                        int otherBCZ = (obz) >> 4;
//                        int otherTCX = (otx) >> 4;
//                        int otherTCZ = (otz) >> 4;
//                        int cx = newChunk.getX();
//                        int cz = newChunk.getZ();
//                        int cbx = (cx << 4) - oX;
//                        int cbz = (cz << 4) - oZ;
//
//                        for (int otherCZ = otherBCZ; otherCZ <= otherTCZ; otherCZ++) {
//                            for (int otherCX = otherBCX; otherCX <= otherTCX; otherCX++) {
//                                FaweChunk chunk;
//                                synchronized (queueFrom) {
//                                    chunk = queueFrom.getFaweChunk(otherCX, otherCZ);
//                                }
//                                if (!(chunk instanceof NullFaweChunk)) {
//                                    changed = true;
//                                    MCAChunk other = (MCAChunk) chunk;
//                                    int ocbx = otherCX << 4;
//                                    int ocbz = otherCZ << 4;
//                                    int octx = ocbx + 15;
//                                    int octz = ocbz + 15;
//                                    int offsetY = 0;
//                                    int minX = obx > ocbx ? (obx - ocbx) & 15 : 0;
//                                    int maxX = otx < octx ? (otx - ocbx) : 15;
//                                    int minZ = obz > ocbz ? (obz - ocbz) & 15 : 0;
//                                    int maxZ = otz < octz ? (otz - ocbz) : 15;
//                                    int offsetX = ocbx - cbx;
//                                    int offsetZ = ocbz - cbz;
//                                    newChunk.copyFrom(other, minX, maxX, 0, 255, minZ, maxZ, offsetX, offsetY, offsetZ);
//                                }
//                            }
//                        }
//                        if (!changed || reference.idsEqual(newChunk, false)) {
//                            return null;
//                        }
//                        if (partRoad) {
//                            boolean[] rwp = roadCache.get();
//                            for (short i = 0; i < 16; i++) {
//                                int vx = bx + i;
//                                int vz = bz + i;
//                                rwp[i] = vx < roadPosLower || vx > roadPosUpper;
//                                rwp[i + 32] = vx == roadPosLower || vx == roadPosUpper;
//                                rwp[i + 16] = vz < roadPosLower || vz > roadPosUpper;
//                                rwp[i + 48] = vz == roadPosLower || vz == roadPosUpper;
//                            }
//                            for (int z = 0; z < 16; z++) {
//                                final boolean rwpz16 = rwp[z + 16];
//                                final boolean rwpz48 = rwp[z + 48];
//                                for (int x = 0; x < 16; x++) {
//                                    if (rwpz16 || rwp[x]) {
//                                        for (int y = 1; y <= hpw.ROAD_HEIGHT; y++) {
//                                            newChunk.setBlock(x, y, z, hpw.ROAD_BLOCK.id, hpw.ROAD_BLOCK.data);
//                                        }
//                                        for (int y = hpw.ROAD_HEIGHT + 1; y < 256; y++) {
//                                            newChunk.setBlock(x, y, z, 0, 0);
//                                        }
//                                    } else if (rwpz48 || rwp[x + 32]) {
//                                        for (int y = 1; y <= hpw.WALL_HEIGHT; y++) {
//                                            newChunk.setBlock(x, y, z, hpw.WALL_FILLING.id, hpw.WALL_FILLING.data);
//                                        }
//                                        for (int y = hpw.WALL_HEIGHT + 2; y < 256; y++) {
//                                            newChunk.setBlock(x, y, z, 0, 0);
//                                        }
//                                        newChunk.setBlock(x, hpw.WALL_HEIGHT + 1, z, hpw.CLAIMED_WALL_BLOCK.id, hpw.CLAIMED_WALL_BLOCK.data);
//                                    }
//                                }
//                            }
//                        }
//                    }
//                    return newChunk;
//                }
//            };
//            writer.setMCAOffset(nextId.x - 1, nextId.y - 1);
//            try {
//                writer.generate();
//                System.gc();
//            } catch (IOException e) {
//                e.printStackTrace();
//                return;
//            }
//            queueFrom.clear();
//            nextId = nextId.getNextId(1);
//        }
//        Fawe.debug("Anvil copy completed in " + ((System.currentTimeMillis() - start) / 1000d) + "s");
//        Fawe.debug("Updating database, please wait...");
//        rawPlots.clear();
//        for (Plot plot : plots) {
//            rawPlots.put(plot.getId(), plot);
//            DBFunc.movePlot(plot, plot);
//        }
//        SQLManager db = (SQLManager) DBFunc.dbManager;
//        db.addNotifyTask(new Runnable() {
//            @Override
//            public void run() {
//                Fawe.debug("Instructions");
//                Fawe.debug(" - Stop the server");
//                Fawe.debug(" - Rename the folder for the new world to the current world");
//                Fawe.debug(" - Change the plot size to " + pLen);
//                Fawe.debug(" - Change the road size to " + roadWidth);
//                Fawe.debug(" - Start the server");
//            }
//        });
//
//        ConfigurationSection section = PS.get().worlds.getConfigurationSection("worlds." + world);
//        if (section == null) section = PS.get().worlds.createSection("worlds." + world);
//        area.saveConfiguration(section);
//        section.set("plot.size", pLen);
//        section.set("road.width", roadWidth);
//        try {
//            PS.get().worlds.save(PS.get().worldsFile);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        final SetupObject object = new SetupObject();
//        object.world = world;
//        object.plotManager = PS.imp().getPluginName();
//        object.setupGenerator = PS.imp().getPluginName();
//        String created = SetupUtils.manager.setupWorld(object);
//    }
}
