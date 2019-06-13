package com.thevoxelbox.voxelsniper.brush;

import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.SmallFireball;
import org.bukkit.util.Vector;

/**
 * @author Gavjenks Heavily revamped from ruler brush blockPositionY
 * @author Giltwist
 * @author Monofraps (Merged Meteor brush)
 */
public class CometBrush extends Brush {
    private boolean useBigBalls = false;

    public CometBrush() {
        this.setName("Comet");
    }

    private void doFireball(final SnipeData v) {
        final Vector targetCoords = new Vector(this.getTargetBlock().getX() + .5 * this.getTargetBlock().getX() / Math.abs(this.getTargetBlock().getX()), this.getTargetBlock().getY() + .5, this.getTargetBlock().getZ() + .5 * this.getTargetBlock().getZ() / Math.abs(this.getTargetBlock().getZ()));
        final Location playerLocation = v.owner().getPlayer().getEyeLocation();
        final Vector slope = targetCoords.subtract(playerLocation.toVector());

        if (useBigBalls) {
            v.owner().getPlayer().launchProjectile(LargeFireball.class).setVelocity(slope.normalize());
        } else {
            v.owner().getPlayer().launchProjectile(SmallFireball.class).setVelocity(slope.normalize());
        }
    }

    @Override
    public final void parameters(final String[] par, final SnipeData v) {
        for (int i = 0; i < par.length; ++i) {
            String parameter = par[i];

            if (parameter.equalsIgnoreCase("info")) {
                v.sendMessage("Parameters:");
                v.sendMessage("balls [big|small]  -- Sets your ball size.");
            }
            if (parameter.equalsIgnoreCase("balls")) {
                if (i + 1 >= par.length) {
                    v.sendMessage("The balls parameter expects a ball size after it.");
                }

                String newBallSize = par[++i];
                if (newBallSize.equalsIgnoreCase("big")) {
                    useBigBalls = true;
                    v.sendMessage("Your balls are " + ChatColor.DARK_RED + ("BIG"));
                } else if (newBallSize.equalsIgnoreCase("small")) {
                    useBigBalls = false;
                    v.sendMessage("Your balls are " + ChatColor.DARK_RED + ("small"));
                } else {
                    v.sendMessage("Unknown ball size.");
                }
            }
        }
    }

    @Override
    protected final void arrow(final SnipeData v) {
        this.doFireball(v);
    }

    @Override
    protected final void powder(final SnipeData v) {
        this.doFireball(v);
    }

    @Override
    public final void info(final Message vm) {
        vm.brushName(this.getName());
        vm.voxel();
        vm.custom("Your balls are " + ChatColor.DARK_RED + (useBigBalls ? "BIG" : "small"));
    }

    @Override
    public String getPermissionNode() {
        return "voxelsniper.brush.comet";
    }
}
