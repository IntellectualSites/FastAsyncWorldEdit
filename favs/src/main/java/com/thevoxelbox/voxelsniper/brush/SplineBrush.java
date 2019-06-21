package com.thevoxelbox.voxelsniper.brush;

import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.brush.perform.PerformBrush;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;

import java.util.ArrayList;

/**
 * FOR ANY BRUSH THAT USES A SPLINE, EXTEND THAT BRUSH FROM THIS BRUSH!!! That way, the spline calculations are already there. Also, the UI for
 * the splines will be included.
 *
 * @author psanker
 */
public class SplineBrush extends PerformBrush {
    private final ArrayList<Block> endPts = new ArrayList<>();
    private final ArrayList<Block> ctrlPts = new ArrayList<>();
    protected ArrayList<Point> spline = new ArrayList<>();
    protected boolean set;
    protected boolean ctrl;
    protected String[] sparams = {"ss", "sc", "clear"};

    public SplineBrush() {
        this.setName("Spline");
    }

    public final void addToSet(final SnipeData v, final boolean ep, Block targetBlock) {
        if (ep) {
            if (this.endPts.contains(targetBlock) || this.endPts.size() == 2) {
                return;
            }

            this.endPts.add(targetBlock);
            v.sendMessage(ChatColor.GRAY + "Added block " + ChatColor.RED + "(" + targetBlock.getX() + ", " + targetBlock.getY() + ", " + targetBlock.getZ() + ") " + ChatColor.GRAY + "to endpoint selection");
            return;
        }

        if (this.ctrlPts.contains(targetBlock) || this.ctrlPts.size() == 2) {
            return;
        }

        this.ctrlPts.add(targetBlock);
        v.sendMessage(ChatColor.GRAY + "Added block " + ChatColor.RED + "(" + targetBlock.getX() + ", " + targetBlock.getY() + ", " + targetBlock.getZ() + ") " + ChatColor.GRAY
                              + "to control point selection");
    }

    public final void removeFromSet(final SnipeData v, final boolean ep, Block targetBlock) {
        if (ep) {
            if (!this.endPts.contains(targetBlock)) {
                v.sendMessage(ChatColor.RED + "That block is not in the endpoint selection set.");
                return;
            }

            this.endPts.add(targetBlock);
            v.sendMessage(ChatColor.GRAY + "Removed block " + ChatColor.RED + "(" + targetBlock.getX() + ", " + targetBlock.getY() + ", " + targetBlock.getZ() + ") " + ChatColor.GRAY
                                  + "from endpoint selection");
            return;
        }

        if (!this.ctrlPts.contains(targetBlock)) {
            v.sendMessage(ChatColor.RED + "That block is not in the control point selection set.");
            return;
        }

        this.ctrlPts.remove(targetBlock);
        v.sendMessage(ChatColor.GRAY + "Removed block " + ChatColor.RED + "(" + targetBlock.getX() + ", " + targetBlock.getY() + ", " + targetBlock.getZ() + ") " + ChatColor.GRAY
                              + "from control point selection");
    }

    public final boolean spline(final Point start, final Point end, final Point c1, final Point c2, final SnipeData v) {
        this.spline.clear();

        try {
            final Point c = (c1.subtract(start)).multiply(3);
            final Point b = ((c2.subtract(c1)).multiply(3)).subtract(c);
            final Point a = ((end.subtract(start)).subtract(c)).subtract(b);

            for (double t = 0.0; t < 1.0; t += 0.01) {
                final int px = (int) Math.round((a.getX() * (t * t * t)) + (b.getX() * (t * t)) + (c.getX() * t) + this.endPts.get(0).getX());
                final int py = (int) Math.round((a.getY() * (t * t * t)) + (b.getY() * (t * t)) + (c.getY() * t) + this.endPts.get(0).getY());
                final int pz = (int) Math.round((a.getZ() * (t * t * t)) + (b.getZ() * (t * t)) + (c.getZ() * t) + this.endPts.get(0).getZ());

                if (!this.spline.contains(new Point(px, py, pz))) {
                    this.spline.add(new Point(px, py, pz));
                }
            }

            return true;
        } catch (final Exception exception) {
            v.sendMessage(ChatColor.RED + "Not enough points selected; " + this.endPts.size() + " endpoints, " + this.ctrlPts.size() + " control points");
            return false;
        }
    }

    protected final void render(final SnipeData v) {
        if (this.spline.isEmpty()) {
            return;
        }

        for (final Point point : this.spline) {
            this.current.perform(this.clampY(point.getX(), point.getY(), point.getZ()));
        }

        v.owner().storeUndo(this.current.getUndo());
    }

    @Override
    protected final void arrow(final SnipeData v) {
        if (this.set) {
            this.removeFromSet(v, true, this.getTargetBlock());
        } else if (this.ctrl) {
            this.removeFromSet(v, false, this.getTargetBlock());
        }
    }

    protected final void clear(final SnipeData v) {
        this.spline.clear();
        this.ctrlPts.clear();
        this.endPts.clear();
        v.sendMessage(ChatColor.GRAY + "Bezier curve cleared.");
    }

    @Override
    protected final void powder(final SnipeData v) {
        if (this.set) {
            this.addToSet(v, true, this.getTargetBlock());
        }
        if (this.ctrl) {
            this.addToSet(v, false, this.getTargetBlock());
        }
    }

    @Override
    public final void info(final Message vm) {
        vm.brushName(this.getName());

        if (this.set) {
            vm.custom(ChatColor.GRAY + "Endpoint selection mode ENABLED.");
        } else if (this.ctrl) {
            vm.custom(ChatColor.GRAY + "Control point selection mode ENABLED.");
        } else {
            vm.custom(ChatColor.AQUA + "No selection mode enabled.");
        }
    }

    @Override
    public final void parameters(final String[] par, final SnipeData v) {
        for (int i = 1; i < par.length; i++) {
            if (par[i].equalsIgnoreCase("info")) {
                v.sendMessage(ChatColor.GOLD + "Spline brush parameters");
                v.sendMessage(ChatColor.AQUA + "ss: Enable endpoint selection mode for desired curve");
                v.sendMessage(ChatColor.AQUA + "sc: Enable control point selection mode for desired curve");
                v.sendMessage(ChatColor.AQUA + "clear: Clear out the curve selection");
                v.sendMessage(ChatColor.AQUA + "ren: Render curve from control points");
                return;
            }
            if (par[i].equalsIgnoreCase("sc")) {
                if (!this.ctrl) {
                    this.set = false;
                    this.ctrl = true;
                    v.sendMessage(ChatColor.GRAY + "Control point selection mode ENABLED.");
                } else {
                    this.ctrl = false;
                    v.sendMessage(ChatColor.AQUA + "Control point selection mode disabled.");
                }
            } else if (par[i].equalsIgnoreCase("ss")) {
                if (!this.set) {
                    this.set = true;
                    this.ctrl = false;
                    v.sendMessage(ChatColor.GRAY + "Endpoint selection mode ENABLED.");
                } else {
                    this.set = false;
                    v.sendMessage(ChatColor.AQUA + "Endpoint selection mode disabled.");
                }
            } else if (par[i].equalsIgnoreCase("clear")) {
                this.clear(v);
            } else if (par[i].equalsIgnoreCase("ren")) {
                if (this.spline(new Point(this.endPts.get(0)), new Point(this.endPts.get(1)), new Point(this.ctrlPts.get(0)), new Point(this.ctrlPts.get(1)), v)) {
                    this.render(v);
                }
            } else {
                v.sendMessage(ChatColor.RED + "Invalid brush parameters! Use the info parameter to display parameter info.");
            }
        }
    }

    @Override
    public String getPermissionNode() {
        return "voxelsniper.brush.spline";
    }

    // Vector class for splines
    protected class Point {
        private int x;
        private int y;
        private int z;

        public Point(final Block b) {
            this.setX(b.getX());
            this.setY(b.getY());
            this.setZ(b.getZ());
        }

        public Point(final int x, final int y, final int z) {
            this.setX(x);
            this.setY(y);
            this.setZ(z);
        }

        public final Point add(final Point p) {
            return new Point(this.getX() + p.getX(), this.getY() + p.getY(), this.getZ() + p.getZ());
        }

        public final Point multiply(final int scalar) {
            return new Point(this.getX() * scalar, this.getY() * scalar, this.getZ() * scalar);
        }

        public final Point subtract(final Point p) {
            return new Point(this.getX() - p.getX(), this.getY() - p.getY(), this.getZ() - p.getZ());
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getZ() {
            return z;
        }

        public void setZ(int z) {
            this.z = z;
        }
    }
}
