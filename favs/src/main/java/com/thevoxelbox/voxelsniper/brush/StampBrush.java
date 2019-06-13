package com.thevoxelbox.voxelsniper.brush;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.Undo;
import org.bukkit.ChatColor;

import java.util.HashSet;

public class StampBrush extends Brush {
    protected HashSet<BlockWrapper> clone = new HashSet<>();
    protected HashSet<BlockWrapper> fall = new HashSet<>();
    protected HashSet<BlockWrapper> drop = new HashSet<>();
    protected HashSet<BlockWrapper> solid = new HashSet<>();
    protected Undo undo;
    protected boolean sorted = false;
    protected StampType stamp = StampType.DEFAULT;

    public StampBrush() {
        this.setName("Stamp");
    }


    public final void reSort() {
        this.sorted = false;
    }

    /**
     * @param id
     * @return
     */
    protected final boolean falling(final int id) {
        return (id > 7 && id < 14);
    }

    /**
     * @param id
     * @return
     */
    protected final boolean fallsOff(final int id) {
        return (BlockTypes.get(id).getMaterial().isFragileWhenPushed());
    }

    /**
     * @param cb
     */
    @SuppressWarnings("deprecation")
    protected final void setBlock(final BlockWrapper cb) {
        final AsyncBlock block = this.clampY(this.getTargetBlock().getX() + cb.x, this.getTargetBlock().getY() + cb.y, this.getTargetBlock().getZ() + cb.z);
        this.undo.put(block);
        block.setTypeId(cb.id);
        block.setPropertyId(cb.d);
    }

    /**
     * @param cb
     */
    @SuppressWarnings("deprecation")
    protected final void setBlockFill(final BlockWrapper cb) {
        final AsyncBlock block = this.clampY(this.getTargetBlock().getX() + cb.x, this.getTargetBlock().getY() + cb.y, this.getTargetBlock().getZ() + cb.z);
        if (block.isEmpty()) {
            this.undo.put(block);
            block.setTypeId(cb.id);
            block.setPropertyId(cb.d);
        }
    }

    /**
     * @param type
     */
    protected final void setStamp(final StampType type) {
        this.stamp = type;
    }

    /**
     * @param v
     */
    protected final void stamp(final SnipeData v) {
        this.undo = new Undo();

        if (this.sorted) {
            for (final BlockWrapper block : this.solid) {
                this.setBlock(block);
            }
            for (final BlockWrapper block : this.drop) {
                this.setBlock(block);
            }
            for (final BlockWrapper block : this.fall) {
                this.setBlock(block);
            }
        } else {
            this.fall.clear();
            this.drop.clear();
            this.solid.clear();
            for (final BlockWrapper block : this.clone) {
                if (this.fallsOff(block.id)) {
                    this.fall.add(block);
                } else if (this.falling(block.id)) {
                    this.drop.add(block);
                } else {
                    this.solid.add(block);
                    this.setBlock(block);
                }
            }
            for (final BlockWrapper block : this.drop) {
                this.setBlock(block);
            }
            for (final BlockWrapper block : this.fall) {
                this.setBlock(block);
            }
            this.sorted = true;
        }

        v.owner().storeUndo(this.undo);
    }

    /**
     * @param v
     */
    protected final void stampFill(final SnipeData v) {

        this.undo = new Undo();

        if (this.sorted) {
            for (final BlockWrapper block : this.solid) {
                this.setBlockFill(block);
            }
            for (final BlockWrapper block : this.drop) {
                this.setBlockFill(block);
            }
            for (final BlockWrapper block : this.fall) {
                this.setBlockFill(block);
            }
        } else {
            this.fall.clear();
            this.drop.clear();
            this.solid.clear();
            for (final BlockWrapper block : this.clone) {
                if (this.fallsOff(block.id)) {
                    this.fall.add(block);
                } else if (this.falling(block.id)) {
                    this.drop.add(block);
                } else if (block.id != 0) {
                    this.solid.add(block);
                    this.setBlockFill(block);
                }
            }
            for (final BlockWrapper block : this.drop) {
                this.setBlockFill(block);
            }
            for (final BlockWrapper block : this.fall) {
                this.setBlockFill(block);
            }
            this.sorted = true;
        }

        v.owner().storeUndo(this.undo);
    }

    /**
     * @param v
     */
    protected final void stampNoAir(final SnipeData v) {

        this.undo = new Undo();

        if (this.sorted) {
            for (final BlockWrapper block : this.solid) {
                this.setBlock(block);
            }
            for (final BlockWrapper block : this.drop) {
                this.setBlock(block);
            }
            for (final BlockWrapper block : this.fall) {
                this.setBlock(block);
            }
        } else {
            this.fall.clear();
            this.drop.clear();
            this.solid.clear();
            for (final BlockWrapper block : this.clone) {
                if (this.fallsOff(block.id)) {
                    this.fall.add(block);
                } else if (this.falling(block.id)) {
                    this.drop.add(block);
                } else if (block.id != 0) {
                    this.solid.add(block);
                    this.setBlock(block);
                }
            }
            for (final BlockWrapper block : this.drop) {
                this.setBlock(block);
            }
            for (final BlockWrapper block : this.fall) {
                this.setBlock(block);
            }
            this.sorted = true;
        }

        v.owner().storeUndo(this.undo);
    }

    @Override
    protected final void arrow(final SnipeData v) {
        switch (this.stamp) {
            case DEFAULT:
                this.stamp(v);
                break;

            case NO_AIR:
                this.stampNoAir(v);
                break;

            case FILL:
                this.stampFill(v);
                break;

            default:
                v.sendMessage(ChatColor.DARK_RED + "Error while stamping! Report");
                break;
        }
    }

    @Override
    protected void powder(final SnipeData v) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void info(final Message vm) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getPermissionNode() {
        return "voxelsniper.brush.stamp";
    }

    /**
     * @author Monofraps
     */
    protected enum StampType {
        NO_AIR, FILL, DEFAULT
    }

    /**
     * @author Voxel
     */
    protected class BlockWrapper {
        public int id;
        public int x;
        public int y;
        public int z;
        public int d;

        /**
         * @param b
         * @param blx
         * @param bly
         * @param blz
         */
        @SuppressWarnings("deprecation")
        public BlockWrapper(final AsyncBlock b, final int blx, final int bly, final int blz) {
            this.id = b.getTypeId();
            this.d = b.getPropertyId();
            this.x = blx;
            this.y = bly;
            this.z = blz;
        }
    }
}
