package com.thevoxelbox.voxelsniper.brush;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.brush.perform.PerformBrush;
import org.bukkit.ChatColor;

public class EllipsoidBrush extends PerformBrush {
    private double xRad;
    private double yRad;
    private double zRad;
    private boolean istrue;

    public EllipsoidBrush() {
        this.setName("Ellipsoid");
    }

    private void execute(final SnipeData v, AsyncBlock targetBlock) {
        this.current.perform(targetBlock);
        double istrueoffset = istrue ? 0.5 : 0;
        int blockPositionX = targetBlock.getX();
        int blockPositionY = targetBlock.getY();
        int blockPositionZ = targetBlock.getZ();

        for (double x = 0; x <= xRad; x++) {

            final double xSquared = (x / (xRad + istrueoffset)) * (x / (xRad + istrueoffset));

            for (double z = 0; z <= zRad; z++) {

                final double zSquared = (z / (zRad + istrueoffset)) * (z / (zRad + istrueoffset));

                for (double y = 0; y <= yRad; y++) {

                    final double ySquared = (y / (yRad + istrueoffset)) * (y / (yRad + istrueoffset));

                    if (xSquared + ySquared + zSquared <= 1) {
                        this.current.perform(this.clampY((int) (blockPositionX + x), (int) (blockPositionY + y), (int) (blockPositionZ + z)));
                        this.current.perform(this.clampY((int) (blockPositionX + x), (int) (blockPositionY + y), (int) (blockPositionZ - z)));
                        this.current.perform(this.clampY((int) (blockPositionX + x), (int) (blockPositionY - y), (int) (blockPositionZ + z)));
                        this.current.perform(this.clampY((int) (blockPositionX + x), (int) (blockPositionY - y), (int) (blockPositionZ - z)));
                        this.current.perform(this.clampY((int) (blockPositionX - x), (int) (blockPositionY + y), (int) (blockPositionZ + z)));
                        this.current.perform(this.clampY((int) (blockPositionX - x), (int) (blockPositionY + y), (int) (blockPositionZ - z)));
                        this.current.perform(this.clampY((int) (blockPositionX - x), (int) (blockPositionY - y), (int) (blockPositionZ + z)));
                        this.current.perform(this.clampY((int) (blockPositionX - x), (int) (blockPositionY - y), (int) (blockPositionZ - z)));
                    }

                }
            }
        }

        v.owner().storeUndo(this.current.getUndo());
    }

    @Override
    protected final void arrow(final SnipeData v) {
        this.execute(v, this.getTargetBlock());
    }

    @Override
    protected final void powder(final SnipeData v) {
        this.execute(v, this.getLastBlock());
    }

    @Override
    public final void info(final Message vm) {
        vm.brushName(this.getName());
        vm.custom(ChatColor.AQUA + "X-size set to: " + ChatColor.DARK_AQUA + this.xRad);
        vm.custom(ChatColor.AQUA + "Y-size set to: " + ChatColor.DARK_AQUA + this.yRad);
        vm.custom(ChatColor.AQUA + "Z-size set to: " + ChatColor.DARK_AQUA + this.zRad);
    }

    @Override
    public final void parameters(final String[] par, final com.thevoxelbox.voxelsniper.SnipeData v) {
        this.istrue = false;
        for (int i = 1; i < par.length; i++) {
            final String parameter = par[i];

            try {
                if (parameter.equalsIgnoreCase("info")) {
                    v.sendMessage(ChatColor.GOLD + "Ellipse brush parameters");
                    v.sendMessage(ChatColor.AQUA + "x[n]: Set X radius to n");
                    v.sendMessage(ChatColor.AQUA + "y[n]: Set Y radius to n");
                    v.sendMessage(ChatColor.AQUA + "z[n]: Set Z radius to n");
                    return;
                } else if (parameter.startsWith("x")) {
                    this.xRad = Integer.parseInt(par[i].replace("x", ""));
                    v.sendMessage(ChatColor.AQUA + "X radius set to: " + this.xRad);
                } else if (parameter.startsWith("y")) {
                    this.yRad = Integer.parseInt(par[i].replace("y", ""));
                    v.sendMessage(ChatColor.AQUA + "Y radius set to: " + this.yRad);
                } else if (parameter.startsWith("z")) {
                    this.zRad = Integer.parseInt(par[i].replace("z", ""));
                    v.sendMessage(ChatColor.AQUA + "Z radius set to: " + this.zRad);
                } else if (parameter.equalsIgnoreCase("true")) {
                    this.istrue = true;
                } else {
                    v.sendMessage(ChatColor.RED + "Invalid brush parameters! Use the \"info\" parameter to display parameter info.");
                }

            } catch (final Exception exception) {
                v.sendMessage(ChatColor.RED + "Incorrect parameter \"" + parameter + "\"; use the \"info\" parameter.");
            }
        }
    }

    @Override
    public String getPermissionNode() {
        return "voxelsniper.brush.ellipsoid";
    }
}
