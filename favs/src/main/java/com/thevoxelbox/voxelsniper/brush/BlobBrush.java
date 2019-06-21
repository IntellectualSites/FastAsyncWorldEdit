package com.thevoxelbox.voxelsniper.brush;

import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.brush.perform.PerformBrush;
import org.bukkit.ChatColor;

import java.util.Random;

public class BlobBrush extends PerformBrush {
    private static final int GROW_PERCENT_DEFAULT = 1000;
    private static final int GROW_PERCENT_MIN = 1;
    private static final int GROW_PERCENT_MAX = 9999;

    private Random randomGenerator = new Random();
    private int growPercent = GROW_PERCENT_DEFAULT; // chance block on recursion pass is made active

    public BlobBrush() {
        this.setName("Blob");
    }

    private void checkValidGrowPercent(final SnipeData v) {
        if (this.growPercent < GROW_PERCENT_MIN || this.growPercent > GROW_PERCENT_MAX) {
            this.growPercent = GROW_PERCENT_DEFAULT;
            v.sendMessage(ChatColor.BLUE + "Growth percent set to: 10%");
        }
    }

    private void digBlob(final SnipeData v) {
        final int brushSize = v.getBrushSize();
        final int brushSizeDoubled = 2 * brushSize;
        final int[][][] splat = new int[brushSizeDoubled + 1][brushSizeDoubled + 1][brushSizeDoubled + 1];
        final int[][][] tempSplat = new int[brushSizeDoubled + 1][brushSizeDoubled + 1][brushSizeDoubled + 1];

        this.checkValidGrowPercent(v);

        // Seed the array
        for (int x = brushSizeDoubled; x >= 0; x--) {
            for (int y = brushSizeDoubled; y >= 0; y--) {
                for (int z = brushSizeDoubled; z >= 0; z--) {
                    if ((x == 0 || y == 0 | z == 0 || x == brushSizeDoubled || y == brushSizeDoubled || z == brushSizeDoubled) && this.randomGenerator.nextInt(GROW_PERCENT_MAX + 1) <= this.growPercent) {
                        splat[x][y][z] = 0;
                    } else {
                        splat[x][y][z] = 1;
                    }
                }
            }
        }

        // Grow the seed
        for (int r = 0; r < brushSize; r++) {
            for (int x = brushSizeDoubled; x >= 0; x--) {
                for (int y = brushSizeDoubled; y >= 0; y--) {
                    for (int z = brushSizeDoubled; z >= 0; z--) {
                        tempSplat[x][y][z] = splat[x][y][z];
                        double growCheck = 0;
                        if (splat[x][y][z] == 1) {
                            if (x != 0 && splat[x - 1][y][z] == 0) {
                                growCheck++;
                            }
                            if (y != 0 && splat[x][y - 1][z] == 0) {
                                growCheck++;
                            }
                            if (z != 0 && splat[x][y][z - 1] == 0) {
                                growCheck++;
                            }
                            if (x != 2 * brushSize && splat[x + 1][y][z] == 0) {
                                growCheck++;
                            }
                            if (y != 2 * brushSize && splat[x][y + 1][z] == 0) {
                                growCheck++;
                            }
                            if (z != 2 * brushSize && splat[x][y][z + 1] == 0) {
                                growCheck++;
                            }
                        }

                        if (growCheck >= 1 && this.randomGenerator.nextInt(GROW_PERCENT_MAX + 1) <= this.growPercent) {
                            tempSplat[x][y][z] = 0; // prevent bleed into splat
                        }
                    }
                }
            }

            // shouldn't this just be splat = tempsplat;? -Gavjenks
            // integrate tempsplat back into splat at end of iteration
            for (int x = brushSizeDoubled; x >= 0; x--) {
                for (int y = brushSizeDoubled; y >= 0; y--) {
                    System.arraycopy(tempSplat[x][y], 0, splat[x][y], 0, brushSizeDoubled + 1);
                }
            }
        }

        final double rSquared = Math.pow(brushSize + 1, 2);

        // Make the changes
        for (int x = brushSizeDoubled; x >= 0; x--) {
            final double xSquared = Math.pow(x - brushSize - 1, 2);

            for (int y = brushSizeDoubled; y >= 0; y--) {
                final double ySquared = Math.pow(y - brushSize - 1, 2);

                for (int z = brushSizeDoubled; z >= 0; z--) {
                    if (splat[x][y][z] == 1 && xSquared + ySquared + Math.pow(z - brushSize - 1, 2) <= rSquared) {
                        this.current.perform(this.clampY(this.getTargetBlock().getX() - brushSize + x, this.getTargetBlock().getY() - brushSize + z, this.getTargetBlock().getZ() - brushSize + y));
                    }
                }
            }
        }

        v.owner().storeUndo(this.current.getUndo());
    }

    private void growBlob(final SnipeData v) {
        final int brushSize = v.getBrushSize();
        final int brushSizeDoubled = 2 * brushSize;
        final int[][][] splat = new int[brushSizeDoubled + 1][brushSizeDoubled + 1][brushSizeDoubled + 1];
        final int[][][] tempSplat = new int[brushSizeDoubled + 1][brushSizeDoubled + 1][brushSizeDoubled + 1];

        this.checkValidGrowPercent(v);

        // Seed the array
        splat[brushSize][brushSize][brushSize] = 1;

        // Grow the seed
        for (int r = 0; r < brushSize; r++) {

            for (int x = brushSizeDoubled; x >= 0; x--) {
                for (int y = brushSizeDoubled; y >= 0; y--) {
                    for (int z = brushSizeDoubled; z >= 0; z--) {
                        tempSplat[x][y][z] = splat[x][y][z];
                        int growCheck = 0;
                        if (splat[x][y][z] == 0) {
                            if (x != 0 && splat[x - 1][y][z] == 1) {
                                growCheck++;
                            }
                            if (y != 0 && splat[x][y - 1][z] == 1) {
                                growCheck++;
                            }
                            if (z != 0 && splat[x][y][z - 1] == 1) {
                                growCheck++;
                            }
                            if (x != 2 * brushSize && splat[x + 1][y][z] == 1) {
                                growCheck++;
                            }
                            if (y != 2 * brushSize && splat[x][y + 1][z] == 1) {
                                growCheck++;
                            }
                            if (z != 2 * brushSize && splat[x][y][z + 1] == 1) {
                                growCheck++;
                            }
                        }

                        if (growCheck >= 1 && this.randomGenerator.nextInt(GROW_PERCENT_MAX + 1) <= this.growPercent) {
                            // prevent bleed into splat
                            tempSplat[x][y][z] = 1;
                        }
                    }
                }
            }

            // integrate tempsplat back into splat at end of iteration
            for (int x = brushSizeDoubled; x >= 0; x--) {
                for (int y = brushSizeDoubled; y >= 0; y--) {
                    System.arraycopy(tempSplat[x][y], 0, splat[x][y], 0, brushSizeDoubled + 1);
                }
            }
        }

        final double rSquared = Math.pow(brushSize + 1, 2);

        // Make the changes
        for (int x = brushSizeDoubled; x >= 0; x--) {
            final double xSquared = Math.pow(x - brushSize - 1, 2);

            for (int y = brushSizeDoubled; y >= 0; y--) {
                final double ySquared = Math.pow(y - brushSize - 1, 2);

                for (int z = brushSizeDoubled; z >= 0; z--) {
                    if (splat[x][y][z] == 1 && xSquared + ySquared + Math.pow(z - brushSize - 1, 2) <= rSquared) {
                        this.current.perform(this.clampY(this.getTargetBlock().getX() - brushSize + x, this.getTargetBlock().getY() - brushSize + z, this.getTargetBlock().getZ() - brushSize + y));
                    }
                }
            }
        }

        v.owner().storeUndo(this.current.getUndo());
    }

    @Override
    protected final void arrow(final SnipeData v) {
        this.growBlob(v);
    }

    @Override
    protected final void powder(final SnipeData v) {
        this.digBlob(v);
    }

    @Override
    public final void info(final Message vm) {
        this.checkValidGrowPercent(null);

        vm.brushName(this.getName());
        vm.size();
        vm.custom(ChatColor.BLUE + "Growth percent set to: " + this.growPercent / 100 + "%");
    }

    @Override
    public final void parameters(final String[] par, final SnipeData v) {
        for (int i = 1; i < par.length; i++) {
            final String parameter = par[i];

            if (parameter.equalsIgnoreCase("info")) {
                v.sendMessage(ChatColor.GOLD + "Blob brush Parameters:");
                v.sendMessage(ChatColor.AQUA + "/b blob g[int] -- set a growth percentage (" + GROW_PERCENT_MIN + "-" + GROW_PERCENT_MAX + ").  Default is " + GROW_PERCENT_DEFAULT);
                return;
            }
            if (parameter.startsWith("g")) {
                final int temp = Integer.parseInt(parameter.replace("g", ""));
                if (temp >= GROW_PERCENT_MIN && temp <= GROW_PERCENT_MAX) {
                    v.sendMessage(ChatColor.AQUA + "Growth percent set to: " + (float) temp / 100 + "%");
                    this.growPercent = temp;
                } else {
                    v.sendMessage(ChatColor.RED + "Growth percent must be an integer " + GROW_PERCENT_MIN + "-" + GROW_PERCENT_MAX + "!");
                }
            } else {
                v.sendMessage(ChatColor.RED + "Invalid brush parameters! Use the info parameter to display parameter info.");
            }
        }
    }

    @Override
    public String getPermissionNode() {
        return "voxelsniper.brush.blob";
    }
}
