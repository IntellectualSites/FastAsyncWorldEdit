package com.boydti.fawe.object.clipboard;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Supplier;

public class WorldCutClipboard extends WorldCopyClipboard {
    public WorldCutClipboard(Supplier<Extent> supplier, Region region) {
        super(supplier, region);
    }

    public WorldCutClipboard(Supplier<Extent> supplier, Region region, boolean hasEntities, boolean hasBiomes) {
        super(supplier, region, hasEntities, hasBiomes);
    }

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        int xx = mx + x;
        int yy = my + y;
        int zz = mz + z;
        Extent extent = getExtent();
        BaseBlock block = extent.getFullBlock(xx, yy, zz);
        extent.setBlock(xx, yy, zz, BlockTypes.AIR.getDefaultState());
        return block;
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        int xx = mx + x;
        int yy = my + y;
        int zz = mz + z;
        Extent extent = getExtent();
        BlockState block = extent.getBlock(xx, yy, zz);
        extent.setBlock(x, y, z, BlockTypes.AIR.getDefaultState());
        return block;
    }

    @Override
    public void close() {
        Extent extent = getExtent();
        if (extent instanceof EditSession) {
            ((EditSession) extent).flushQueue();
        } else if (extent instanceof Closeable) {
            try {
                ((Closeable) extent).close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            extent.commit();
        }
    }
}
