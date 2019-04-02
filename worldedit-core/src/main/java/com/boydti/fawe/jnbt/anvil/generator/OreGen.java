package com.boydti.fawe.jnbt.anvil.generator;

import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.MutableBlockVector3;

import java.util.Random;

public class OreGen extends Resource {
    private final int maxSize;
    private final double maxSizeO8;
    private final double maxSizeO16;
    private final double sizeInverse;
    private final int minY;
    private final int maxY;
    private final Pattern pattern;
    private final Extent extent;
    private final Mask mask;
    private MutableBlockVector3 mutable = new MutableBlockVector3();

    private double ONE_2 = 1 / 2F;
    private double ONE_8 = 1 / 8F;
    private double ONE_16 = 1 / 16F;

    public int laced = 0;

    public OreGen(Extent extent, Mask mask, Pattern pattern, int size, int minY, int maxY) {
        this.maxSize = size;
        this.maxSizeO8 = size * ONE_8;
        this.maxSizeO16 = size * ONE_16;
        this.sizeInverse = 1.0 / size;
        this.minY = minY;
        this.maxY = maxY;
        this.mask = mask;
        this.pattern = pattern;
        this.extent = extent;
    }

    @Override
    public boolean spawn(Random rand, int x, int z) throws WorldEditException {
        int y = rand.nextInt(maxY - minY) + minY;
        if (!mask.test(mutable.setComponents(x, y, z))) {
            return false;
        }
        double f = rand.nextDouble() * Math.PI;

        int x8 = x;
        int z8 = z;
        double so8 = maxSizeO8;
        double so16 = maxSizeO16;
        double sf = MathMan.sinInexact(f) * so8;
        double cf = MathMan.cosInexact(f) * so8;
        double d1 = x8 + sf;
        double d2 = x8 - sf;
        double d3 = z8 + cf;
        double d4 = z8 - cf;

        double d5 = y + rand.nextInt(3) - 2;
        double d6 = y + rand.nextInt(3) - 2;

        double xd = (d2 - d1);
        double yd = (d6 - d5);
        double zd = (d4 - d3);

        double iFactor = 0;
        for (int i = 0; i < maxSize; i++, iFactor += sizeInverse) {
            double d7 = d1 + xd * iFactor;
            double d8 = d5 + yd * iFactor;
            double d9 = d3 + zd * iFactor;

            double d10 = rand.nextDouble() * so16;
            double sif = MathMan.sinInexact(Math.PI * iFactor);
            double d11 = (sif + 1.0) * d10 + 1.0;
            double d12 = (sif + 1.0) * d10 + 1.0;

            double d11o2 = d11 * ONE_2;
            double d12o2 = d12 * ONE_2;

            int minX = MathMan.floorZero(d7 - d11o2);
            int minY = Math.max(1, MathMan.floorZero(d8 - d12o2));
            int minZ = MathMan.floorZero(d9 - d11o2);

            int maxX = MathMan.floorZero(d7 + d11o2);
            int maxY = Math.min(255, MathMan.floorZero(d8 + d12o2));
            int maxZ = MathMan.floorZero(d9 + d11o2);

            double id11o2 = 1.0 / (d11o2);
            double id12o2 = 1.0 / (d12o2);

            for (int xx = minX; xx <= maxX; xx++) {
                double dx = (xx + 0.5D - d7) * id11o2;
                double dx2 = dx * dx;
                if (dx2 < 1) {
                    mutable.mutX(xx);
                    for (int yy = minY; yy <= maxY; yy++) {
                        double dy = (yy + 0.5D - d8) * id12o2;
                        double dxy2 = dx2 + dy * dy;
                        if (dxy2 < 1) {
                            mutable.mutY(yy);
                            for (int zz = minZ; zz <= maxZ; zz++) {
                                mutable.mutZ(zz);
                                double dz = (zz + 0.5D - d9) * id11o2;
                                double dxyz2 = dxy2 + dz * dz;
                                if ((dxyz2 < 1)) {
                                    if (mask.test(mutable))
                                        pattern.apply(extent, mutable, mutable);
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }
}