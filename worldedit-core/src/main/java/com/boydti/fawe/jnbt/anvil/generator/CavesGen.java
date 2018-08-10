package com.boydti.fawe.jnbt.anvil.generator;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.PseudoRandom;
import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

public class CavesGen extends GenBase {

    private boolean evenCaveDistribution = false;
    private int caveFrequency = 40;
    private int caveRarity = 7;
    private int caveMinAltitude = 8;
    private int caveMaxAltitude = 127;
    private int caveSystemFrequency = 1;
    private int individualCaveRarity = 25;
    private int caveSystemPocketChance = 0;
    private int caveSystemPocketMinSize = 0;
    private int caveSystemPocketMaxSize = 3;

    public CavesGen(int caveSize) {
        super(caveSize);
    }

    public CavesGen(int caveSize, int caveFrequency, int caveRarity, int caveMinAltitude, int caveMaxAltitude, int caveSystemFrequency, int individualCaveRarity, int caveSystemPocketChance, int caveSystemPocketMinSize, int caveSystemPocketMaxSize) {
        super(caveSize);
        this.caveFrequency = caveFrequency;
        this.caveRarity = caveRarity;
        this.caveMinAltitude = caveMinAltitude;
        this.caveMaxAltitude = caveMaxAltitude;
        this.caveSystemFrequency = caveSystemFrequency;
        this.individualCaveRarity = individualCaveRarity;
        this.caveSystemPocketChance = caveSystemPocketChance;
        this.caveSystemPocketMinSize = caveSystemPocketMinSize;
        this.caveSystemPocketMaxSize = caveSystemPocketMaxSize;
    }

    protected void generateLargeCaveNode(long seed, BlockVector2 pos, Extent chunk, double x, double y, double z) throws WorldEditException {
        generateCaveNode(seed, pos, chunk, x, y, z, 1.0F + PseudoRandom.random.nextDouble() * 6.0F, 0.0F, 0.0F, -1, -1, 0.5D);
    }

    protected void generateCaveNode(long seed, BlockVector2 chunkPos, Extent chunk, double x, double y, double z, double paramdouble1, double paramdouble2, double paramdouble3, int angle, int maxAngle, double paramDouble4) throws WorldEditException {
        int bx = (chunkPos.getBlockX() << 4);
        int bz = (chunkPos.getBlockZ() << 4);
        double real_x = bx + 7;
        double real_z = bz + 7;

        double f1 = 0.0F;
        double f2 = 0.0F;

        PseudoRandom localRandom = new PseudoRandom(seed);

        if (maxAngle <= 0) {
            int checkAreaSize = this.getCheckAreaSize() * 16 - 16;
            maxAngle = checkAreaSize - localRandom.nextInt(checkAreaSize / 4);
        }
        boolean isLargeCave = false;

        if (angle == -1) {
            angle = maxAngle / 2;
            isLargeCave = true;
        }

        int j = localRandom.nextInt(maxAngle / 2) + maxAngle / 4;
        int k = localRandom.nextInt(6) == 0 ? 1 : 0;

        for (; angle < maxAngle; angle++) {
            double d3 = 1.5D + MathMan.sinInexact(angle * 3.141593F / maxAngle) * paramdouble1 * 1.0F;
            double d4 = d3 * paramDouble4;

            double f3 = MathMan.cosInexact(paramdouble3);
            double f4 = MathMan.sinInexact(paramdouble3);
            x += MathMan.cosInexact(paramdouble2) * f3;
            y += f4;
            z += MathMan.sinInexact(paramdouble2) * f3;

            if (k != 0)
                paramdouble3 *= 0.92F;
            else {
                paramdouble3 *= 0.7F;
            }
            paramdouble3 += f2 * 0.1F;
            paramdouble2 += f1 * 0.1F;

            f2 *= 0.9F;
            f1 *= 0.75F;
            f2 += (localRandom.nextDouble() - localRandom.nextDouble()) * localRandom.nextDouble() * 2.0F;
            f1 += (localRandom.nextDouble() - localRandom.nextDouble()) * localRandom.nextDouble() * 4.0F;

            if ((!isLargeCave) && (angle == j) && (paramdouble1 > 1.0F) && (maxAngle > 0)) {
                generateCaveNode(localRandom.nextLong(), chunkPos, chunk, x, y, z, localRandom.nextDouble() * 0.5F + 0.5F, paramdouble2 - 1.570796F, paramdouble3 / 3.0F, angle, maxAngle, 1.0D);
                generateCaveNode(localRandom.nextLong(), chunkPos, chunk, x, y, z, localRandom.nextDouble() * 0.5F + 0.5F, paramdouble2 + 1.570796F, paramdouble3 / 3.0F, angle, maxAngle, 1.0D);
                return;
            }
            if ((!isLargeCave) && (localRandom.nextInt(4) == 0)) {
                continue;
            }

            // Check if distance to working point (x and z) too larger than working radius (maybe ??)
            double d5 = x - real_x;
            double d6 = z - real_z;
            double d7 = maxAngle - angle;
            double d8 = paramdouble1 + 2.0F + 16.0F;
            if (d5 * d5 + d6 * d6 - d7 * d7 > d8 * d8) {
                return;
            }

            //Boundaries check.
            if ((x < real_x - 16.0D - d3 * 2.0D) || (z < real_z - 16.0D - d3 * 2.0D) || (x > real_x + 16.0D + d3 * 2.0D) || (z > real_z + 16.0D + d3 * 2.0D))
                continue;


            int m = (int) (x - d3) - bx - 1;
            int n = (int) (x + d3) - bx + 1;

            int i1 = (int) (y - d4) - 1;
            int i2 = (int) (y + d4) + 1;

            int i3 = (int) (z - d3) - bz - 1;
            int i4 = (int) (z + d3) - bz + 1;

            if (m < 0)
                m = 0;
            if (n > 16)
                n = 16;

            if (i1 < 1)
                i1 = 1;
            if (i2 > 256 - 8) {
                i2 = 256 - 8;
            }
            if (i3 < 0)
                i3 = 0;
            if (i4 > 16)
                i4 = 16;

            // Search for water
            boolean waterFound = false;
            for (int local_x = m; (!waterFound) && (local_x < n); local_x++) {
                for (int local_z = i3; (!waterFound) && (local_z < i4); local_z++) {
                    for (int local_y = i2 + 1; (!waterFound) && (local_y >= i1 - 1); local_y--) {
                        if (local_y >= 0 && local_y < 255) {
                            BlockStateHolder material = chunk.getLazyBlock(bx + local_x, local_y, bz + local_z);
                            if (material.getBlockType() == BlockTypes.WATER) {
                                waterFound = true;
                            }
                            if ((local_y != i1 - 1) && (local_x != m) && (local_x != n - 1) && (local_z != i3) && (local_z != i4 - 1))
                                local_y = i1;
                        }
                    }
                }
            }
            if (waterFound) {
                continue;
            }

            // Generate cave
            for (int local_x = m; local_x < n; local_x++) {
                double d9 = (local_x + bx + 0.5D - x) / d3;
                for (int local_z = i3; local_z < i4; local_z++) {
                    double d10 = (local_z + bz + 0.5D - z) / d3;
                    boolean grassFound = false;
                    if (d9 * d9 + d10 * d10 < 1.0D) {
                        for (int local_y = i2; local_y > i1; local_y--) {
                            double d11 = ((local_y - 1) + 0.5D - y) / d4;
                            if ((d11 > -0.7D) && (d9 * d9 + d11 * d11 + d10 * d10 < 1.0D)) {
                                BlockStateHolder material = chunk.getLazyBlock(bx + local_x, local_y, bz + local_z);
                                BlockStateHolder materialAbove = chunk.getLazyBlock(bx + local_x, local_y + 1, bz + local_z);
                                switch (material.getBlockType()) {
                                    case GRASS:
                                    case MYCELIUM:
                                        grassFound = true;
                                        break;
                                }
                                if (this.isSuitableBlock(material, materialAbove)) {
                                    if (local_y - 1 < 10) {
                                        chunk.setBlock(bx + local_x, local_y, bz + local_z, BlockTypes.LAVA.getDefaultState());
                                    } else {
                                        chunk.setBlock(bx + local_x, local_y, bz + local_z, BlockTypes.CAVE_AIR.getDefaultState());

                                        // If grass was just deleted, try to
                                        // move it down
                                        if (grassFound) {
                                            BlockStateHolder block = chunk.getLazyBlock(bx + local_x, local_y - 1, bz + local_z);
                                            if (block.getBlockType() == BlockTypes.DIRT) {
                                                chunk.setBlock(bx + local_x, local_y - 1, bz + local_z, BlockTypes.STONE.getDefaultState());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (isLargeCave)
                break;
        }
    }

    protected boolean isSuitableBlock(BlockStateHolder material, BlockStateHolder materialAbove) {
        switch (material.getBlockType()) {
            case AIR:
            case CAVE_AIR:
            case VOID_AIR:
            case WATER:
            case LAVA:
            case BEDROCK:
                return false;
            default:
                return true;
        }
    }

    @Override
    public void generateChunk(int chunkX, int chunkZ, BlockVector2 originChunk, Extent chunk) throws WorldEditException {
        PseudoRandom random = getRandom();
        int i = random.nextInt(random.nextInt(random.nextInt(this.caveFrequency) + 1) + 1);
        if (this.evenCaveDistribution)
            i = this.caveFrequency;
        if (random.nextInt(100) >= this.caveRarity)
            i = 0;

        for (int j = 0; j < i; j++) {
            double x = (chunkX << 4) + random.nextInt(16);

            double y;

            if (this.evenCaveDistribution)
                y = random.nextInt(this.caveMinAltitude, this.caveMaxAltitude);
            else
                y = random.nextInt(random.nextInt(this.caveMaxAltitude - this.caveMinAltitude + 1) + 1) + this.caveMinAltitude;

            double z = (chunkZ << 4) + random.nextInt(16);

            int count = this.caveSystemFrequency;
            boolean largeCaveSpawned = false;
            if (random.nextInt(100) <= this.individualCaveRarity) {
                generateLargeCaveNode(random.nextLong(), originChunk, chunk, x, y, z);
                largeCaveSpawned = true;
            }

            if ((largeCaveSpawned) || (random.nextInt(100) <= this.caveSystemPocketChance - 1)) {
                count += random.nextInt(this.caveSystemPocketMinSize, this.caveSystemPocketMaxSize);
            }
            while (count > 0) {
                count--;
                double f1 = random.nextDouble() * 3.141593F * 2.0F;
                double f2 = (random.nextDouble() - 0.5F) * 2.0F / 8.0F;
                double f3 = random.nextDouble() * 2.0F + random.nextDouble();
                generateCaveNode(random.nextLong(), originChunk, chunk, x, y, z, f3, f1, f2, 0, 0, 1.0D);
            }
        }
    }
}