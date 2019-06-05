package com.thevoxelbox.voxelsniper.brush;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.brush.perform.PerformBrush;
import org.bukkit.ChatColor;

import java.util.Random;

/**
 * @author Voxel
 */
public class SplatterDiscBrush extends PerformBrush {
    private static final int GROW_PERCENT_MIN = 1;
    private static final int GROW_PERCENT_DEFAULT = 1000;
    private static final int GROW_PERCENT_MAX = 9999;
    private static final int SEED_PERCENT_MIN = 1;
    private static final int SEED_PERCENT_DEFAULT = 1000;
    private static final int SEED_PERCENT_MAX = 9999;
    private static final int SPLATREC_PERCENT_MIN = 1;
    private static final int SPLATREC_PERCENT_DEFAULT = 3;
    private static final int SPLATREC_PERCENT_MAX = 10;
    private int seedPercent; // Chance block on first pass is made active
    private int growPercent; // chance block on recursion pass is made active
    private int splatterRecursions; // How many times you grow the seeds
    private Random generator = new Random();

    /**
     *
     */
    public SplatterDiscBrush() {
        this.setName("Splatter Disc");
    }

    private void splatterDisc(final SnipeData v, AsyncBlock targetBlock) {
        if (this.seedPercent < SEED_PERCENT_MIN || this.seedPercent > SEED_PERCENT_MAX) {
            v.sendMessage(ChatColor.BLUE + "Seed percent set to: 10%");
            this.seedPercent = SEED_PERCENT_DEFAULT;
        }
        if (this.growPercent < GROW_PERCENT_MIN || this.growPercent > GROW_PERCENT_MAX) {
            v.sendMessage(ChatColor.BLUE + "Growth percent set to: 10%");
            this.growPercent = GROW_PERCENT_DEFAULT;
        }
        if (this.splatterRecursions < SPLATREC_PERCENT_MIN || this.splatterRecursions > SPLATREC_PERCENT_MAX) {
            v.sendMessage(ChatColor.BLUE + "Recursions set to: 3");
            this.splatterRecursions = SPLATREC_PERCENT_DEFAULT;
        }
        final int[][] splat = new int[2 * v.getBrushSize() + 1][2 * v.getBrushSize() + 1];

        // Seed the array
        for (int x = 2 * v.getBrushSize(); x >= 0; x--) {
            for (int y = 2 * v.getBrushSize(); y >= 0; y--) {

                if (this.generator.nextInt(SEED_PERCENT_MAX + 1) <= this.seedPercent) {
                    splat[x][y] = 1;

                }
            }
        }
        // Grow the seeds
        final int gref = this.growPercent;
        int growcheck;
        final int[][] tempSplat = new int[2 * v.getBrushSize() + 1][2 * v.getBrushSize() + 1];
        for (int r = 0; r < this.splatterRecursions; r++) {

            this.growPercent = gref - ((gref / this.splatterRecursions) * (r));
            for (int x = 2 * v.getBrushSize(); x >= 0; x--) {
                for (int y = 2 * v.getBrushSize(); y >= 0; y--) {
                    tempSplat[x][y] = splat[x][y]; // prime tempsplat

                    growcheck = 0;
                    if (splat[x][y] == 0) {
                        if (x != 0 && splat[x - 1][y] == 1) {
                            growcheck++;
                        }
                        if (y != 0 && splat[x][y - 1] == 1) {
                            growcheck++;
                        }
                        if (x != 2 * v.getBrushSize() && splat[x + 1][y] == 1) {
                            growcheck++;
                        }
                        if (y != 2 * v.getBrushSize() && splat[x][y + 1] == 1) {
                            growcheck++;
                        }
                    }

                    if (growcheck >= 1 && this.generator.nextInt(GROW_PERCENT_MAX + 1) <= this.growPercent) {
                        tempSplat[x][y] = 1; // prevent bleed into splat
                    }
                }
            }

            // integrate tempsplat back into splat at end of iteration
            for (int x = 2 * v.getBrushSize(); x >= 0; x--) {
                if (2 * v.getBrushSize() + 1 >= 0)
                    System.arraycopy(tempSplat[x], 0, splat[x], 0, 2 * v.getBrushSize() + 1);
            }
        }
        this.growPercent = gref;
        // Fill 1x1 holes
        for (int x = 2 * v.getBrushSize(); x >= 0; x--) {
            for (int y = 2 * v.getBrushSize(); y >= 0; y--) {
                if (splat[Math.max(x - 1, 0)][y] == 1 && splat[Math.min(x + 1, 2 * v.getBrushSize())][y] == 1 && splat[x][Math.max(0, y - 1)] == 1 && splat[x][Math.min(2 * v.getBrushSize(), y + 1)] == 1) {
                    splat[x][y] = 1;
                }
            }
        }

        // Make the changes
        final double rSquared = Math.pow(v.getBrushSize() + 1, 2);
        for (int x = 2 * v.getBrushSize(); x >= 0; x--) {
            final double xSquared = Math.pow(x - v.getBrushSize() - 1, 2);
            for (int y = 2 * v.getBrushSize(); y >= 0; y--) {
                if (splat[x][y] == 1 && xSquared + Math.pow(y - v.getBrushSize() - 1, 2) <= rSquared) {
                    current.perform(targetBlock.getRelative(x - v.getBrushSize(), 0, y - v.getBrushSize()));
                }
            }
        }
        v.owner().storeUndo(this.current.getUndo());
    }

    @Override
    protected final void arrow(final SnipeData v) {
        this.splatterDisc(v, this.getTargetBlock());
    }

    @Override
    protected final void powder(final SnipeData v) {
        this.splatterDisc(v, this.getLastBlock());
    }

    @Override
    public final void info(final Message vm) {
        if (this.seedPercent < SEED_PERCENT_MIN || this.seedPercent > SEED_PERCENT_MAX) {
            this.seedPercent = SEED_PERCENT_DEFAULT;
        }
        if (this.growPercent < GROW_PERCENT_MIN || this.growPercent > GROW_PERCENT_MAX) {
            this.growPercent = GROW_PERCENT_DEFAULT;
        }
        if (this.splatterRecursions < SPLATREC_PERCENT_MIN || this.splatterRecursions > SPLATREC_PERCENT_MAX) {
            this.splatterRecursions = SPLATREC_PERCENT_DEFAULT;
        }
        vm.brushName("Splatter Disc");
        vm.size();
        vm.custom(ChatColor.BLUE + "Seed percent set to: " + this.seedPercent / 100 + "%");
        vm.custom(ChatColor.BLUE + "Growth percent set to: " + this.growPercent / 100 + "%");
        vm.custom(ChatColor.BLUE + "Recursions set to: " + this.splatterRecursions);
    }

    @Override
    public final void parameters(final String[] par, final SnipeData v) {
        for (int i = 1; i < par.length; i++) {
            final String parameter = par[i];

            if (parameter.equalsIgnoreCase("info")) {
                v.sendMessage(ChatColor.GOLD + "Splatter Disc brush Parameters:");
                v.sendMessage(ChatColor.AQUA + "/b sd s[int] -- set a seed percentage (1-9999). 100 = 1% Default is 1000");
                v.sendMessage(ChatColor.AQUA + "/b sd g[int] -- set a growth percentage (1-9999).  Default is 1000");
                v.sendMessage(ChatColor.AQUA + "/b sd r[int] -- set a recursion (1-10).  Default is 3");
                return;
            } else if (parameter.startsWith("s")) {
                final double temp = Integer.parseInt(parameter.replace("s", ""));
                if (temp >= SEED_PERCENT_MIN && temp <= SEED_PERCENT_MAX) {
                    v.sendMessage(ChatColor.AQUA + "Seed percent set to: " + temp / 100 + "%");
                    this.seedPercent = (int) temp;
                } else {
                    v.sendMessage(ChatColor.RED + "Seed percent must be an integer 1-9999!");
                }
            } else if (parameter.startsWith("g")) {
                final double temp = Integer.parseInt(parameter.replace("g", ""));
                if (temp >= GROW_PERCENT_MIN && temp <= GROW_PERCENT_MAX) {
                    v.sendMessage(ChatColor.AQUA + "Growth percent set to: " + temp / 100 + "%");
                    this.growPercent = (int) temp;
                } else {
                    v.sendMessage(ChatColor.RED + "Growth percent must be an integer 1-9999!");
                }
            } else if (parameter.startsWith("r")) {
                final int temp = Integer.parseInt(parameter.replace("r", ""));
                if (temp >= SPLATREC_PERCENT_MIN && temp <= SPLATREC_PERCENT_MAX) {
                    v.sendMessage(ChatColor.AQUA + "Recursions set to: " + temp);
                    this.splatterRecursions = temp;
                } else {
                    v.sendMessage(ChatColor.RED + "Recursions must be an integer 1-10!");
                }
            } else {
                v.sendMessage(ChatColor.RED + "Invalid brush parameters! Use the info parameter to display parameter info.");
            }
        }
    }

    @Override
    public String getPermissionNode() {
        return "voxelsniper.brush.splatterdisc";
    }
}
