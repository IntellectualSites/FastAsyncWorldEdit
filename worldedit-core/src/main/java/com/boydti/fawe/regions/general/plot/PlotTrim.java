package com.boydti.fawe.regions.general.plot;

import com.boydti.fawe.jnbt.anvil.MCAChunk;
import com.boydti.fawe.jnbt.anvil.MCAFile;
import com.boydti.fawe.jnbt.anvil.MCAFilter;
import com.boydti.fawe.jnbt.anvil.MCAQueue;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.SetQueue;
import com.github.intellectualsites.plotsquared.plot.PlotSquared;
import com.github.intellectualsites.plotsquared.plot.object.ChunkLoc;
import com.github.intellectualsites.plotsquared.plot.object.Location;
import com.github.intellectualsites.plotsquared.plot.object.Plot;
import com.github.intellectualsites.plotsquared.plot.object.PlotArea;
import com.github.intellectualsites.plotsquared.plot.object.PlotPlayer;
import com.github.intellectualsites.plotsquared.plot.util.expiry.ExpireManager;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


import static com.google.common.base.Preconditions.checkNotNull;

public class PlotTrim {
    private final MCAQueue queue;
    private final PlotArea area;
    private final PlotPlayer player;
    private final MCAQueue originalQueue;
    private final File root;
    private final File originalRoot;
    private int[][] ids;
    private boolean deleteUnowned = true;

    public PlotTrim(PlotPlayer player, PlotArea area, String worldName, boolean deleteUnowned) {
        FaweQueue tmpQueue = SetQueue.IMP.getNewQueue(worldName, true, false);
        File saveFolder = tmpQueue.getSaveFolder();
        this.root = new File(saveFolder.getParentFile().getParentFile(), worldName + "-Copy" + File.separator + "region");
        this.originalRoot = saveFolder;
        this.originalQueue = new MCAQueue(worldName, originalRoot, true);
        this.queue = new MCAQueue(worldName + "-Copy", root, true);
        this.area = area;
        this.player = player;
        this.deleteUnowned = deleteUnowned;
    }

    public void setChunk(int[][] ids) {
        checkNotNull(ids);
        this.ids = ids;
    }

    public void setChunk(int x, int z) {
        this.ids = ((MCAChunk) originalQueue.getFaweChunk(x, z)).ids;
    }

    private Map<Long, Object> chunks = new ConcurrentHashMap<>();
    private Object PRESENT = new Object();

    private void removeChunks(Plot plot) {
        Location pos1 = plot.getBottom();
        Location pos2 = plot.getTop();
        int ccx1 = pos1.getX() >> 4;
        int ccz1 = pos1.getZ() >> 4;
        int ccx2 = pos2.getX() >> 4;
        int ccz2 = pos2.getZ() >> 4;
        for (int x = ccx1; x <= ccx2; x++) {
            for (int z = ccz1; z <= ccz2; z++) {
                long pair = MathMan.pairInt(x, z);
                chunks.remove(pair);
            }
        }
    }

    public void run() {
        final Set<ChunkLoc> mcas = new HashSet<>();
        if (deleteUnowned && area != null) {
            originalQueue.filterWorld(new MCAFilter() {
                @Override
                public boolean appliesFile(int mcaX, int mcaZ) {
                    mcas.add(new ChunkLoc(mcaX, mcaZ));
                    return false;
                }
            });
            ArrayList<Plot> plots = new ArrayList<>(PlotSquared.get().getPlots(area));
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
                        ChunkLoc loc = new ChunkLoc(x, z);
                        mcas.remove(loc);
                    }
                }
            }
            for (ChunkLoc mca : mcas) {
                int bx = mca.x << 5;
                int bz = mca.z << 5;
                for (int x = 0; x < 32; x++) {
                    for (int z = 0; z < 32; z++) {
                        long pair = MathMan.pairInt(bx + x, bz + z);
                        chunks.put(pair, PRESENT);
                    }
                }
            }
            for (Plot plot : plots) {
                removeChunks(plot);
            }
        }
        originalQueue.filterWorld(new MCAFilter() {
            @Override
            public boolean appliesFile(int mcaX, int mcaZ) {
                ChunkLoc loc = new ChunkLoc(mcaX, mcaZ);
                return !mcas.contains(loc);
            }

            @Override
            public MCAFile applyFile(MCAFile mca) {
                int mcaX = mca.getX();
                int mcaZ = mca.getZ();
                ChunkLoc loc = new ChunkLoc(mcaX, mcaZ);
                if (mcas.contains(loc)) {
                    player.sendMessage("Delete MCA " + mca);
                    mca.setDeleted(true);
                    return null;
                }
                try {
                    File copy = new File(root, mca.getFile().getName());
                    if (!copy.exists()) {
                        copy = MainUtil.copyFile(mca.getFile(), copy);
                        player.sendMessage("Filter copy -> " + copy);
                    } else {
                        player.sendMessage("Filter existing: " + mcaX + "," + mcaZ);
                    }
                    return new MCAFile(mca.getParent(), copy);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }

            @Override
            public boolean appliesChunk(int cx, int cz) {
                return true;
            }

            @Override
            public MCAChunk applyChunk(MCAChunk chunk, Object ignore) {
                long pair = MathMan.pairInt(chunk.getX(), chunk.getZ());
                if (chunks.containsKey(pair)) {
                    chunk.setDeleted(true);
                    return null;
                }
                if (ids != null) {
                    for (int i = 0; i < ids.length; i++) {
                        if (!isEqual(ids[i], chunk.ids[i])) {
                            return null;
                        }
                    }
                    chunk.setDeleted(true);
                }
                return null;
            }
        });
        player.sendMessage("Done!");
    }

    private int count = 0;

    private boolean isEqual(int[] a, int[] b) {
        if (a == b) {
            return true;
        }
        if (a != null) {
            if (b != null) {
                return Arrays.equals(a, b);
            }
            return isEmpty(a);
        }
        return isEmpty(b);
    }

    private boolean isEmpty(int[] a) {
        for (int b : a) {
            if (!BlockTypes.getFromStateId(b).getMaterial().isAir()) {
                return false;
            }
        }
        return true;
    }
}
