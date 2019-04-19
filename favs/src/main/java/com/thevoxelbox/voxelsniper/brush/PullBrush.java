package com.thevoxelbox.voxelsniper.brush;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import org.bukkit.ChatColor;

import java.util.HashSet;

/**
 * @author Piotr
 */
public class PullBrush extends Brush {
    private final HashSet<BlockWrapper> surface = new HashSet<>();
    private int vh;
    private double c1 = 1;
    private double c2 = 0;

    /**
     * Default Constructor.
     */
    public PullBrush() {
        this.setName("Soft Selection");
    }

    @Override
    public final void info(final Message vm) {
        vm.brushName(this.getName());
        vm.size();
        vm.height();
        vm.custom(ChatColor.AQUA + "Pinch " + (-this.c1 + 1));
        vm.custom(ChatColor.AQUA + "Bubble " + this.c2);
    }

    @Override
    public final void parameters(final String[] par, final SnipeData v) {
        try {
            final double pinch = Double.parseDouble(par[1]);
            final double bubble = Double.parseDouble(par[2]);
            this.c1 = 1 - pinch;
            this.c2 = bubble;
        } catch (final Exception exception) {
            v.sendMessage(ChatColor.RED + "Invalid brush parameters!");
        }
    }

    /**
     * @param t
     * @return double
     */
    private double getStr(final double t) {
        final double lt = 1 - t;
        return (lt * lt * lt) + 3 * (lt * lt) * t * this.c1 + 3 * lt * (t * t) * this.c2; // My + (t * ((By + (t * ((c2 + (t * (0 - c2))) - By))) - My));
    }

    /**
     * @param v
     */
    private void getSurface(final SnipeData v) {
        this.surface.clear();

        final double bSquared = Math.pow(v.getBrushSize() + 0.5, 2);
        for (int z = -v.getBrushSize(); z <= v.getBrushSize(); z++) {
            final double zSquared = Math.pow(z, 2);
            final int actualZ = this.getTargetBlock().getZ() + z;
            for (int x = -v.getBrushSize(); x <= v.getBrushSize(); x++) {
                final double xSquared = Math.pow(x, 2);
                final int actualX = this.getTargetBlock().getX() + x;
                for (int y = -v.getBrushSize(); y <= v.getBrushSize(); y++) {
                    final double volume = (xSquared + Math.pow(y, 2) + zSquared);
                    if (volume <= bSquared) {
                        if (this.isSurface(actualX, this.getTargetBlock().getY() + y, actualZ)) {
                            this.surface.add(new BlockWrapper(this.clampY(actualX, this.getTargetBlock().getY() + y, actualZ), this.getStr(((volume / bSquared)))));
                        }
                    }
                }
            }
        }
    }

    /**
     * @param x
     * @param y
     * @param z
     * @return boolean
     */
    private boolean isSurface(final int x, final int y, final int z) {
        return !this.getBlockAt(x, y, z).isEmpty() && ((this.getBlockAt(x, y - 1, z).isEmpty()) || (this.getBlockAt(x, y + 1, z).isEmpty()) || (this.getBlockAt(x + 1, y, z).isEmpty()) || (this.getBlockAt(x - 1, y, z).isEmpty()) || (this.getBlockAt(x, y, z + 1).isEmpty()) || (this.getBlockAt(x, y, z - 1).isEmpty()));

    }

    @SuppressWarnings("deprecation")
    private void setBlock(final BlockWrapper block) {
        final AsyncBlock currentBlock = this.clampY(block.getX(), block.getY() + (int) (this.vh * block.getStr()), block.getZ());
        if (this.getBlockAt(block.getX(), block.getY() - 1, block.getZ()).isEmpty()) {
            currentBlock.setTypeId(block.getId());
            currentBlock.setPropertyId(block.getD());
            for (int y = block.getY(); y < currentBlock.getY(); y++) {
                this.setBlockIdAt(block.getZ(), block.getX(), y, BlockTypes.AIR.getInternalId());
            }
        } else {
            currentBlock.setTypeId(block.getId());
            currentBlock.setPropertyId(block.getD());
            for (int y = block.getY() - 1; y < currentBlock.getY(); y++) {
                final AsyncBlock current = this.clampY(block.getX(), y, block.getZ());
                current.setTypeId(block.getId());
                current.setPropertyId(block.getD());
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void setBlockDown(final BlockWrapper block) {
        final AsyncBlock currentBlock = this.clampY(block.getX(), block.getY() + (int) (this.vh * block.getStr()), block.getZ());
        currentBlock.setTypeId(block.getId());
        currentBlock.setPropertyId(block.getD());
        for (int y = block.getY(); y > currentBlock.getY(); y--) {
            this.setBlockIdAt(block.getZ(), block.getX(), y, BlockTypes.AIR.getInternalId());
        }
        // }
    }

    @Override
    protected final void arrow(final SnipeData v) {
        this.vh = v.getVoxelHeight();
        this.getSurface(v);

        if (this.vh > 0) {
            for (final BlockWrapper block : this.surface) {
                this.setBlock(block);
            }
        } else if (this.vh < 0) {
            for (final BlockWrapper block : this.surface) {
                this.setBlockDown(block);
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected final void powder(final SnipeData v) {
        this.vh = v.getVoxelHeight();

        this.surface.clear();

        int lastY;
        int newY;
        int lastStr;
        double str;
        final double brushSizeSquared = Math.pow(v.getBrushSize() + 0.5, 2);

        int id;

        // Are we pulling up ?
        if (this.vh > 0) {

            // Z - Axis
            for (int z = -v.getBrushSize(); z <= v.getBrushSize(); z++) {

                final int zSquared = z * z;
                final int actualZ = this.getTargetBlock().getZ() + z;

                // X - Axis
                for (int x = -v.getBrushSize(); x <= v.getBrushSize(); x++) {

                    final int xSquared = x * x;
                    final int actualX = this.getTargetBlock().getX() + x;

                    // Down the Y - Axis
                    for (int y = v.getBrushSize(); y >= -v.getBrushSize(); y--) {

                        final double volume = zSquared + xSquared + (y * y);

                        // Is this in the range of the brush?
                        if (volume <= brushSizeSquared && !this.getWorld().getBlockAt(actualX, this.getTargetBlock().getY() + y, actualZ).isEmpty()) {

                            int actualY = this.getTargetBlock().getY() + y;

                            // Starting strength and new Position
                            str = this.getStr(volume / brushSizeSquared);
                            lastStr = (int) (this.vh * str);
                            lastY = actualY + lastStr;

                            this.clampY(actualX, lastY, actualZ).setTypeId(this.getWorld().getBlockAt(actualX, actualY, actualZ).getTypeId());

                            if (str == 1) {
                                str = 0.8;
                            }

                            while (lastStr > 0) {
                                if (actualY < this.getTargetBlock().getY()) {
                                    str = str * str;
                                }
                                lastStr = (int) (this.vh * str);
                                newY = actualY + lastStr;
                                id = this.getWorld().getBlockAt(actualX, actualY, actualZ).getTypeId();
                                for (int i = newY; i < lastY; i++) {
                                    this.clampY(actualX, i, actualZ).setTypeId(id);
                                }
                                lastY = newY;
                                actualY--;
                            }
                            break;
                        }
                    }
                }
            }
        } else {
            for (int z = -v.getBrushSize(); z <= v.getBrushSize(); z++) {
                final double zSquared = Math.pow(z, 2);
                final int actualZ = this.getTargetBlock().getZ() + z;
                for (int x = -v.getBrushSize(); x <= v.getBrushSize(); x++) {
                    final double xSquared = Math.pow(x, 2);
                    final int actualX = this.getTargetBlock().getX() + x;
                    for (int y = -v.getBrushSize(); y <= v.getBrushSize(); y++) {
                        double volume = (xSquared + Math.pow(y, 2) + zSquared);
                        if (volume <= brushSizeSquared && !this.getWorld().getBlockAt(actualX, this.getTargetBlock().getY() + y, actualZ).isEmpty()) {
                            final int actualY = this.getTargetBlock().getY() + y;
                            lastY = actualY + (int) (this.vh * this.getStr(volume / brushSizeSquared));
                            this.clampY(actualX, lastY, actualZ).setTypeId(this.getWorld().getBlockAt(actualX, actualY, actualZ).getTypeId());
                            y++;
                            volume = (xSquared + Math.pow(y, 2) + zSquared);
                            while (volume <= brushSizeSquared) {
                                final int blockY = this.getTargetBlock().getY() + y + (int) (this.vh * this.getStr(volume / brushSizeSquared));
                                final int blockId = this.getWorld().getBlockAt(actualX, this.getTargetBlock().getY() + y, actualZ).getTypeId();
                                for (int i = blockY; i < lastY; i++) {
                                    this.clampY(actualX, i, actualZ).setTypeId(blockId);
                                }
                                lastY = blockY;
                                y++;
                                volume = (xSquared + Math.pow(y, 2) + zSquared);
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public String getPermissionNode() {
        return "voxelsniper.brush.pull";
    }

    /**
     * @author Piotr
     */
    private final class BlockWrapper {

        private final int id;
        private final int d;
        private final double str;
        private final int x;
        private final int y;
        private final int z;

        /**
         * @param block
         * @param st
         */
        @SuppressWarnings("deprecation")
        public BlockWrapper(final AsyncBlock block, final double st) {
            this.id = block.getTypeId();
            this.d = block.getPropertyId();
            this.x = block.getX();
            this.y = block.getY();
            this.z = block.getZ();
            this.str = st;
        }

        /**
         * @return the d
         */
        public int getD() {
            return this.d;
        }

        /**
         * @return the id
         */
        public int getId() {
            return this.id;
        }

        /**
         * @return the str
         */
        public double getStr() {
            return this.str;
        }

        /**
         * @return the x
         */
        public int getX() {
            return this.x;
        }

        /**
         * @return the y
         */
        public int getY() {
            return this.y;
        }

        /**
         * @return the z
         */
        public int getZ() {
            return this.z;
        }
    }
}
