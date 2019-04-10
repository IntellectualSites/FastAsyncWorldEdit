package com.boydti.fawe.jnbt.anvil.filters;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.jnbt.anvil.MCAChunk;
import com.boydti.fawe.jnbt.anvil.MCAFile;
import com.boydti.fawe.object.RunnableVal4;
import com.boydti.fawe.object.collection.LongHashSet;
import com.github.intellectualsites.plotsquared.plot.PlotSquared;
import com.github.intellectualsites.plotsquared.plot.generator.HybridGen;
import com.github.intellectualsites.plotsquared.plot.generator.HybridPlotWorld;
import com.github.intellectualsites.plotsquared.plot.generator.IndependentPlotGenerator;
import com.github.intellectualsites.plotsquared.plot.object.Location;
import com.github.intellectualsites.plotsquared.plot.object.Plot;
import com.github.intellectualsites.plotsquared.plot.object.PlotArea;
import com.github.intellectualsites.plotsquared.plot.util.expiry.ExpireManager;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.registry.LegacyMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.concurrent.ForkJoinPool;

public class PlotTrimFilter extends DeleteUninhabitedFilter {
    private final HybridPlotWorld hpw;
    private final HybridGen hg;
    private final MCAChunk reference;
    private final LongHashSet occupiedRegions;
    private final LongHashSet unoccupiedChunks;
    private boolean referenceIsVoid;

    public static boolean shouldSuggest(PlotArea area) {
        IndependentPlotGenerator gen = area.getGenerator();
        if (area instanceof HybridPlotWorld && gen instanceof HybridGen) {
            HybridPlotWorld hpw = (HybridPlotWorld) area;
            return hpw.PLOT_BEDROCK && !hpw.PLOT_SCHEMATIC && hpw.MAIN_BLOCK.getBlocks().size() == 1 && hpw.TOP_BLOCK.getBlocks().size() == 1;
        }
        return false;
    }

    public PlotTrimFilter(World world, long fileDuration, long inhabitedTicks, long chunkInactivity) {
        super(fileDuration, inhabitedTicks, chunkInactivity);
        Fawe.debug("Initializing Plot trim...");

        String worldName = Fawe.imp().getWorldName(world);
        PlotArea area = PlotSquared.get().getPlotAreaByString(worldName);
        IndependentPlotGenerator gen = area.getGenerator();
        if (!(area instanceof HybridPlotWorld) || !(gen instanceof HybridGen)) {
            throw new UnsupportedOperationException("Trim does not support non hybrid plot worlds");
        }
        this.hg = (HybridGen) gen;
        this.hpw = (HybridPlotWorld) area;
        if (hpw.PLOT_SCHEMATIC || hpw.MAIN_BLOCK.getBlocks().size() != 1 || hpw.TOP_BLOCK.getBlocks().size() != 1) {
            throw new UnsupportedOperationException("WIP - will implement later");
        }
        this.occupiedRegions = new LongHashSet();
        this.unoccupiedChunks = new LongHashSet();

        this.reference = calculateReference();
        
        Fawe.debug(" - calculating claims");
        this.calculateClaimedArea();
    }

    private MCAChunk calculateReference() {
        MCAChunk reference = new MCAChunk(null, 0, 0);
        if (hpw.PLOT_BEDROCK) {
            reference.fillCuboid(0, 15, 0, 0, 0, 15, BlockTypes.BEDROCK.getInternalId());
        } else if (hpw.MAIN_BLOCK.hasSingleItem() && hpw.MAIN_BLOCK.getBlock().isAir() && hpw.TOP_BLOCK.hasSingleItem() && hpw.TOP_BLOCK.getBlock().isAir()) {
            referenceIsVoid = true;
        }
        reference.fillCuboid(0, 15, 1, hpw.PLOT_HEIGHT - 1, 0, 15, LegacyMapper.getInstance().getBaseBlockFromPlotBlock(hpw.MAIN_BLOCK.getBlock()).getInternalBlockTypeId());
        reference.fillCuboid(0, 15, hpw.PLOT_HEIGHT, hpw.PLOT_HEIGHT, 0, 15, LegacyMapper.getInstance().getBaseBlockFromPlotBlock(hpw.TOP_BLOCK.getBlock()).getInternalBlockTypeId());
        return reference;
    }

    private void calculateClaimedArea() {
        ArrayList<Plot> plots = new ArrayList<>(hpw.getPlots());
        if (ExpireManager.IMP != null) {
            plots.removeAll(ExpireManager.IMP.getPendingExpired());
        }
        for (Plot plot : plots) {
            Location pos1 = plot.getBottom();
            Location pos2 = plot.getTop();
            int ccx1 = pos1.getX() >> 9;
            int ccz1 = pos1.getZ() >> 9;
            int ccx2 = pos2.getX() >> 9;
            int ccz2 = pos2.getZ() >> 9;
            for (int x = ccx1; x <= ccx2; x++) {
                for (int z = ccz1; z <= ccz2; z++) {
                    if (!occupiedRegions.containsKey(x, z)) {
                        occupiedRegions.add(x, z);
                        int bcx = x << 5;
                        int bcz = z << 5;
                        int tcx = bcx + 32;
                        int tcz = bcz + 32;
                        for (int cz = bcz; cz < tcz; cz++) {
                            for (int cx = bcx; cx < tcx; cx++) {
                                unoccupiedChunks.add(cx, cz);
                            }
                        }
                    }
                }
            }
            int cx1 = pos1.getX() >> 4;
            int cz1 = pos1.getZ() >> 4;
            int cx2 = pos2.getX() >> 4;
            int cz2 = pos2.getZ() >> 4;
            for (int cz = cz1; cz <= cz2; cz++) {
                for (int cx = cx1; cx <= cx2; cx++) {
                    unoccupiedChunks.remove(cx, cz);
                }
            }
        }
    }

    @Override
    public boolean shouldDelete(File file, BasicFileAttributes attr, int mcaX, int mcaZ) throws IOException {
        Fawe.debug("Apply file: " + file);
        return !occupiedRegions.containsKey(mcaX, mcaZ) || super.shouldDelete(file, attr, mcaX, mcaZ);
    }

    @Override
    public boolean shouldDeleteChunk(MCAFile mca, int cx, int cz) {
        return unoccupiedChunks.containsKey(cx, cz);
    }

    @Override
    public void filter(MCAFile mca, ForkJoinPool pool) throws IOException {
        if (reference == null) {
            super.filter(mca, pool);
            return;
        }
        Path file = mca.getFile().toPath();
        BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
        long creationDate = attr.creationTime().toMillis();

        mca.forEachSortedChunk(new RunnableVal4<Integer, Integer, Integer, Integer>() {
            @Override
            public void run(Integer x, Integer z, Integer offset, Integer size) {
                int bx = mca.getX() << 5;
                int bz = mca.getZ() << 5;
                int cx = bx + x;
                int cz = bz + z;
                if (shouldDeleteChunk(mca, cx, cz)) {
                    MCAChunk chunk = new MCAChunk(null, x, z);
                    chunk.setDeleted(true);
                    synchronized (mca) {
                        mca.setChunk(chunk);
                    }
                    get().add(16 * 16 * 256);
                    return;
                }
                Runnable task = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            MCAChunk chunk = mca.getChunk(x, z);
                            if (chunk.getInhabitedTime() <= getInhabitedTicks()) {
                                chunk.setDeleted(true);
                                get().add(16 * 16 * 256);
                                return;
                            }
                            if (referenceIsVoid) {
                                for (int i = 0; i < chunk.ids.length; i++) {
                                    int[] arr = chunk.ids[i];
                                    if (arr != null) {
                                        for (int b : arr) {
                                            if (!BlockTypes.getFromStateId(b).getMaterial().isAir()) return;
                                        }
                                    }
                                }
                            }
                            else if (!reference.idsEqual(chunk, false)) {
                                return;
                            }
                            chunk.setDeleted(true);
                            get().add(16 * 16 * 256);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                };
                pool.submit(task);
            }
        });
    }
}