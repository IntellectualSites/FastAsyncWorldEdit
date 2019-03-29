package com.thevoxelbox.voxelsniper.brush;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;

public class WallSider extends Brush{
	
	private static String[] facings = new String[] { "north", "east", "south", "west", "relative to player" };
	private short c;
    private short d;
    private double e;
    private boolean f;
    private boolean g;
    private boolean h;
    
    public WallSider() {
        this.c = 4;
        this.d = 1;
        this.e = 0.0;
        this.setName("WallSider");
    }
    
    private void a(final SnipeData snipeData, final Block block, final boolean b) {
        final double n = (snipeData.getBrushSize() + this.e) * (snipeData.getBrushSize() + this.e);
        final Vector vector;
        final Vector clone = (vector = block.getLocation().toVector()).clone();
        int c;
        if (this.c == 4) {
            double n2;
            if ((n2 = (snipeData.owner().getPlayer().getLocation().getYaw() - 90.0f) % 360.0f) < 0.0) {
                n2 += 360.0;
            }
            c = ((0.0 >= n2 && n2 < 45.0) ? 2 : ((45.0 >= n2 && n2 < 135.0) ? 3 : ((135.0 >= n2 && n2 < 225.0) ? 0 : ((225.0 >= n2 && n2 < 315.0) ? 1 : ((315.0 >= n2 && n2 < 360.0) ? 2 : -1)))));
        }
        else {
            c = this.c;
        }
        int n3 = c;
        if (b) {
            n3 = (short)((n3 + 2) % 4);
        }
        int n4 = 98;
        if (n3 == 0 || n3 == 2) {
            n4 = 97;
        }
        for (int i = -snipeData.getBrushSize(); i <= snipeData.getBrushSize(); ++i) {
            if (n4 == 97) {
                clone.setX(vector.getX() + i);
            }
            else {
                clone.setZ(vector.getZ() + i);
            }
            for (int j = -snipeData.getBrushSize(); j <= snipeData.getBrushSize(); ++j) {
                clone.setY(vector.getY() + j);
                if (vector.distanceSquared(clone) <= n) {
                    for (short n5 = 0; n5 < this.d; ++n5) {
                        if (n4 == 97) {
                            clone.setZ(vector.getZ() + ((n3 == 2) ? n5 : (-n5)));
                        }
                        else {
                            clone.setX(vector.getX() + ((n3 == 1) ? n5 : (-n5)));
                        }
                        final AsyncBlock block2 = this.getWorld().getBlockAt(clone.getBlockX(), clone.getBlockY(), clone.getBlockZ());
                        if ((this.f && block2.getTypeId() == snipeData.getReplaceId()) || (!this.f && (block2.getTypeId() != 0 || this.g))) {
                            block2.setTypeId(snipeData.getVoxelId());
                        }
                    }
                    if (n4 == 97) {
                        clone.setZ(vector.getZ());
                    }
                    else {
                        clone.setX(vector.getX());
                    }
                }
            }
        }
    }
    
    @Override
    protected final void arrow(final SnipeData snipeData) {
    	this.a(snipeData, this.getTargetBlock(), false);
    }
    
    @Override
    protected final void powder(final SnipeData snipeData) {
    	this.a(snipeData, this.getTargetBlock(), true);
    }
    
    public final void parameters(final String[] array, final SnipeData snipeData) {
        for (int i = 1; i < array.length; ++i) {
            final String lowerCase;
            if ((lowerCase = array[i].toLowerCase()).startsWith("d")) {
                this.d = (short)Integer.parseInt(lowerCase.replace("d", ""));
                snipeData.sendMessage(ChatColor.AQUA + "Depth set to " + this.d + " blocks");
            }
            else if (lowerCase.startsWith("s")) {
                this.c = (short)Integer.parseInt(lowerCase.replace("s", ""));
                if (this.c > 4 || this.c < 0) {
                    this.c = 4;
                }
                snipeData.sendMessage(ChatColor.AQUA + "Orientation set to " + facings[this.c]);
            }
            else if (lowerCase.startsWith("true")) {
                this.e = 0.5;
                snipeData.sendMessage(ChatColor.AQUA + "True circle mode ON.");
            }
            else if (lowerCase.startsWith("false")) {
                this.e = 0.0;
                snipeData.sendMessage(ChatColor.AQUA + "True circle mode OFF.");
            }
            else if (lowerCase.startsWith("air")) {
                this.g = true;
                snipeData.sendMessage(ChatColor.AQUA + "Including air.");
            }
            else if (lowerCase.startsWith("mm")) {
                this.f = true;
                snipeData.sendMessage(ChatColor.AQUA + "Replacing block.");
            }
        }
    }

	@Override
	public String getPermissionNode() {
		return "voxelsniper.brush.wallsider";
	}

	@Override
	public void info(Message vm) {
		// TODO Auto-generated method stub
		
	}

}
