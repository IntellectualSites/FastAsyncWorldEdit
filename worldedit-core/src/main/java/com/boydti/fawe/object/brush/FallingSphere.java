package com.boydti.fawe.object.brush;

import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;

public class FallingSphere implements Brush {
    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) throws MaxChangedBlocksException {
        int px = position.getBlockX();
        int py = position.getBlockY();
        int pz = position.getBlockZ();
        int maxY = editSession.getMaxY();

        int lastY = py;

        int radius = (int) Math.round(size);
        int radiusSqr = (int) Math.round(size * size);
        for (int z = -radius; z <= radius; z++) {
            int zz = z * z;
            int az = pz + z;

            int remaining = radiusSqr - zz;
            int xRadius = MathMan.usqrt(remaining);


            for (int x = -xRadius; x <= xRadius; x++) {
                int xx = x * x;
                int ax = px + x;

                int remainingY = remaining - xx;
                if (remainingY < 0) continue;

                int yRadius = MathMan.usqrt(remainingY);
                int startY = Math.max(0, py - yRadius);
                int endY = Math.min(maxY, py + yRadius);

                int heightY = editSession.getHighestTerrainBlock(ax, az, 0, endY);
                if (heightY < startY) {
                    int diff = startY - heightY;
                    startY -= diff;
                    endY -= diff;
                }

                for (int y = startY; y <= heightY; y++) {
                    editSession.setBlock(ax, y, az, pattern);
                }
                for (int y = heightY + 1; y <= endY; y++) {
                    editSession.setBlock(ax, y, az, pattern);
                }
            }
        }
    }
}
