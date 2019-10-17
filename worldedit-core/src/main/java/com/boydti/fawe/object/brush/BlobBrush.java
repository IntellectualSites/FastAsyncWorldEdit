package com.boydti.fawe.object.brush;

import com.boydti.fawe.object.random.SimplexNoise;
import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import java.util.concurrent.ThreadLocalRandom;

public class BlobBrush implements Brush {
    private final double amplitude;
    private final double frequency;
    private final Vector3 radius;
    private final double sphericity;

    public BlobBrush(Vector3 radius, double frequency, double amplitude, double sphericity) {
        this.frequency = frequency;
        this.amplitude = amplitude;
        this.radius = radius;
        this.sphericity = sphericity;
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

        double modX = 1d / radius.getX();
        double modY = 1d / radius.getY();
        double modZ = 1d / radius.getZ();
        int radius = (int) size;
        int radiusSqr = (int) (size * size);
        int sizeInt = (int) size * 2;

        if (sphericity == 1) {
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
        } else {
            AffineTransform transform = new AffineTransform()
            .rotateX(ThreadLocalRandom.current().nextInt(360))
            .rotateY(ThreadLocalRandom.current().nextInt(360))
            .rotateZ(ThreadLocalRandom.current().nextInt(360));

            double manScaleX = 1.25 + seedX * 0.5;
            double manScaleY = 1.25 + seedY * 0.5;
            double manScaleZ = 1.25 + seedZ * 0.5;

            MutableVector3 mutable = new MutableVector3();
            double roughness = 1 - sphericity;
            for (int xr = -sizeInt; xr <= sizeInt; xr++) {
                mutable.mutX(xr);
                for (int yr = -sizeInt; yr <= sizeInt; yr++) {
                    mutable.mutY(yr);
                    for (int zr = -sizeInt; zr <= sizeInt; zr++) {
                        mutable.mutZ(zr);
                        Vector3 pt = transform.apply(mutable);
                        int x = MathMan.roundInt(pt.getX());
                        int y = MathMan.roundInt(pt.getY());
                        int z = MathMan.roundInt(pt.getZ());

                        double xScaled = Math.abs(x) * modX;
                        double yScaled = Math.abs(y) * modY;
                        double zScaled = Math.abs(z) * modZ;
                        double manDist = xScaled + yScaled + zScaled;
                        double distSqr = x * x * modX + z * z * modZ + y * y * modY;

                        double distance =
                        Math.sqrt(distSqr) * sphericity +
                        MathMan.max(manDist, xScaled * manScaleX, yScaled * manScaleY, zScaled * manScaleZ) * roughness;

                        double noise = this.amplitude * SimplexNoise.noise(seedX + x * distort, seedZ + z * distort, seedZ + z * distort);
                        if (distance + distance * noise < radius) {
                            editSession.setBlock(px + xr, py + yr, pz + zr, pattern);
                        }
                    }
                }
            }
        }
    }
}
