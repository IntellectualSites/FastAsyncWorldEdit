package com.boydti.fawe.object.brush;

import com.boydti.fawe.object.random.SimplexNoise;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import java.util.concurrent.ThreadLocalRandom;

public class RockBrush implements Brush {
    private final double amplitude;
    private final double frequency;
    private final Vector3 radius;

    public RockBrush(Vector3 radius, double frequency, double amplitude) {
        this.frequency = frequency;
        this.amplitude = amplitude;
        this.radius = radius;
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) throws MaxChangedBlocksException {
        double seedX = ThreadLocalRandom.current().nextDouble();
        double seedY = ThreadLocalRandom.current().nextDouble();
        double seedZ = ThreadLocalRandom.current().nextDouble();

        int px = position.getBlockX();
        int py = position.getBlockY();
        int pz = position.getBlockZ();

        double distort = this.frequency / size;

        double modX = 1d/radius.getX();
        double modY = 1d/radius.getY();
        double modZ = 1d/radius.getZ();

        int radiusSqr = (int) (size * size);
        int sizeInt = (int) size * 2;
        for (int x = -sizeInt; x <= sizeInt; x++) {
            double nx = seedX + x * distort;
            double d1 = x * x * modX;
            for (int y = -sizeInt; y <= sizeInt; y++) {
                double d2 = d1 + y * y * modY;
                double ny = seedY + y * distort;
                for (int z = -sizeInt; z <= sizeInt; z++) {
                    double nz = seedZ + z * distort;
                    double distance = d2 + z * z * modZ;
                    double noise = this.amplitude * SimplexNoise.noise(nx, ny, nz);
                    if (distance + distance * noise < radiusSqr) {
                        editSession.setBlock(px + x, py + y, pz + z, pattern);
                    }
                }
            }
        }
    }
}
