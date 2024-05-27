package com.fastasyncworldedit.core.command.tool.brush;

import com.fastasyncworldedit.core.math.random.SimplexNoise;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;

import java.util.concurrent.ThreadLocalRandom;

public record RockBrush(double amplitude, double frequency, Vector3 radius) implements Brush {

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) throws
            MaxChangedBlocksException {
        double seedX = ThreadLocalRandom.current().nextDouble();
        double seedY = ThreadLocalRandom.current().nextDouble();
        double seedZ = ThreadLocalRandom.current().nextDouble();

        int px = position.x();
        int py = position.y();
        int pz = position.z();

        double distort = this.frequency / size;

        double modX = 1D / radius.x();
        double modY = 1D / radius.y();
        double modZ = 1D / radius.z();

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
