package com.boydti.fawe.object.clipboard;

import com.boydti.fawe.object.change.MutableBlockChange;
import com.boydti.fawe.object.change.MutableTileChange;
import com.boydti.fawe.object.changeset.MemoryOptimizedHistory;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.Iterator;

public class ResizableClipboardBuilder extends MemoryOptimizedHistory {

    private int minX = Integer.MAX_VALUE;
    private int minY = Integer.MAX_VALUE;
    private int minZ = Integer.MAX_VALUE;

    private int maxX = Integer.MIN_VALUE;
    private int maxY = Integer.MIN_VALUE;
    private int maxZ = Integer.MIN_VALUE;


    public ResizableClipboardBuilder(World world) {
        super(world);
    }

    @Override
    public void add(int x, int y, int z, int combinedFrom, int combinedTo) {
        super.add(x, y, z, combinedFrom, combinedTo);
        if (x < minX) {
            if (maxX == Integer.MIN_VALUE) {
                maxX = x;
            }
            minX = x;
        } else if (x > maxX) {
            maxX = x;
        }
        if (y < minY) {
            if (maxY == Integer.MIN_VALUE) {
                maxY = y;
            }
            minY = y;
        } else if (y > maxY) {
            maxY = y;
        }
        if (z < minZ) {
            if (maxZ == Integer.MIN_VALUE) {
                maxZ = z;
            }
            minZ = z;
        } else if (z > maxZ) {
            maxZ = z;
        }
    }

    public Clipboard build() {
        Vector pos1 = new Vector(minX, minY, minZ);
        Vector pos2 = new Vector(maxX, maxY, maxZ);
        CuboidRegion region = new CuboidRegion(pos1, pos2);
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        Iterator<Change> iter = getIterator(true);
        try {
            while (iter.hasNext()) {
                Change change = iter.next();
                if (change instanceof MutableBlockChange) {
                    MutableBlockChange blockChange = (MutableBlockChange) change;
                    BlockStateHolder block = BlockState.getFromInternalId(blockChange.combinedId);
                    clipboard.setBlock(blockChange.x, blockChange.y, blockChange.z, block);
                } else if (change instanceof MutableTileChange) {
                    MutableTileChange tileChange = (MutableTileChange) change;
                    int x = tileChange.tag.getInt("x");
                    int y = tileChange.tag.getInt("y");
                    int z = tileChange.tag.getInt("z");
                    clipboard.setTile(x, y, z, tileChange.tag);
                }
            }
        } catch (WorldEditException e) {
            e.printStackTrace();
        }

        return clipboard;
    }
}
