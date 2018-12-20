package com.thevoxelbox.voxelsniper.brush;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.Undo;
import org.bukkit.block.Block;

import java.util.Random;

/**
 * http://www.voxelwiki.com/minecraft/Voxelsniper#The_Random-Erode_Brush
 *
 * @author Piotr
 * @author Giltwist (Randomized blockPositionY)
 */
public class RandomErodeBrush extends Brush
{
    private final double trueCircle = 0.5;
    private BlockWrapper[][][] snap;
    private BlockWrapper[][][] firstSnap;
    private int bsize;
    private int erodeFace;
    private int fillFace;
    private int brushSize;
    private int erodeRecursion = 1;
    private int fillRecursion = 1;
    private Random generator = new Random();

    /**
     *
     */
    public RandomErodeBrush()
    {
        this.setName("RandomErode");
    }

    private boolean erode(final int x, final int y, final int z)
    {
        if (this.snap[x][y][z].isSolid())
        {
            int d = 0;
            if (!this.snap[x + 1][y][z].isSolid())
            {
                d++;
            }
            if (!this.snap[x - 1][y][z].isSolid())
            {
                d++;
            }
            if (!this.snap[x][y + 1][z].isSolid())
            {
                d++;
            }
            if (!this.snap[x][y - 1][z].isSolid())
            {
                d++;
            }
            if (!this.snap[x][y][z + 1].isSolid())
            {
                d++;
            }
            if (!this.snap[x][y][z - 1].isSolid())
            {
                d++;
            }
            return (d >= this.erodeFace);
        }
        else
        {
            return false;
        }
    }

    @SuppressWarnings("deprecation")
	private boolean fill(final int x, final int y, final int z)
    {
        if (this.snap[x][y][z].isSolid())
        {
            return false;
        }
        else
        {
            int d = 0;
            if (this.snap[x + 1][y][z].isSolid())
            {
                this.snap[x][y][z].setId(this.snap[x + 1][y][z].getNativeBlock().getTypeId());
                d++;
            }
            if (this.snap[x - 1][y][z].isSolid())
            {
                this.snap[x][y][z].setId(this.snap[x - 1][y][z].getNativeBlock().getTypeId());
                d++;
            }
            if (this.snap[x][y + 1][z].isSolid())
            {
                this.snap[x][y][z].setId(this.snap[x][y + 1][z].getNativeBlock().getTypeId());
                d++;
            }
            if (this.snap[x][y - 1][z].isSolid())
            {
                this.snap[x][y][z].setId(this.snap[x][y - 1][z].getNativeBlock().getTypeId());
                d++;
            }
            if (this.snap[x][y][z + 1].isSolid())
            {
                this.snap[x][y][z].setId(this.snap[x][y][z + 1].getNativeBlock().getTypeId());
                d++;
            }
            if (this.snap[x][y][z - 1].isSolid())
            {
                this.snap[x][y][z].setId(this.snap[x][y][z - 1].getNativeBlock().getTypeId());
                d++;
            }
            return (d >= this.fillFace);
        }
    }

    private void getMatrix()
    {
        this.brushSize = ((this.bsize + 1) * 2) + 1;

        if (this.snap.length == 0)
        {
            this.snap = new BlockWrapper[this.brushSize][this.brushSize][this.brushSize];

            int sx = this.getTargetBlock().getX() - (this.bsize + 1);
            int sy = this.getTargetBlock().getY() - (this.bsize + 1);
            int sz = this.getTargetBlock().getZ() - (this.bsize + 1);

            for (int x = 0; x < this.snap.length; x++)
            {
                sz = this.getTargetBlock().getZ() - (this.bsize + 1);
                for (int z = 0; z < this.snap.length; z++)
                {
                    sy = this.getTargetBlock().getY() - (this.bsize + 1);
                    for (int y = 0; y < this.snap.length; y++)
                    {
                        this.snap[x][y][z] = new BlockWrapper(this.clampY(sx, sy, sz));
                        sy++;
                    }
                    sz++;
                }
                sx++;
            }
            this.firstSnap = this.snap.clone();
        }
        else
        {
            this.snap = new BlockWrapper[this.brushSize][this.brushSize][this.brushSize];

            int sx = this.getTargetBlock().getX() - (this.bsize + 1);
            int sy = this.getTargetBlock().getY() - (this.bsize + 1);
            int sz = this.getTargetBlock().getZ() - (this.bsize + 1);

            for (int x = 0; x < this.snap.length; x++)
            {
                sz = this.getTargetBlock().getZ() - (this.bsize + 1);
                for (int z = 0; z < this.snap.length; z++)
                {
                    sy = this.getTargetBlock().getY() - (this.bsize + 1);
                    for (int y = 0; y < this.snap.length; y++)
                    {
                        this.snap[x][y][z] = new BlockWrapper(this.clampY(sx, sy, sz));
                        sy++;
                    }
                    sz++;
                }
                sx++;
            }
        }
    }

    @SuppressWarnings("deprecation")
	private void rerosion(final SnipeData v)
    {
        final Undo undo = new Undo();

        if (this.erodeFace >= 0 && this.erodeFace <= 6)
        {
            for (int currentErodeRecursion = 0; currentErodeRecursion < this.erodeRecursion; currentErodeRecursion++)
            {
                this.getMatrix();

                final double brushSizeSquared = Math.pow(this.bsize + this.trueCircle, 2);
                for (int z = 1; z < this.snap.length - 1; z++)
                {

                    final double zSquared = Math.pow(z - (this.bsize + 1), 2);
                    for (int x = 1; x < this.snap.length - 1; x++)
                    {

                        final double xSquared = Math.pow(x - (this.bsize + 1), 2);
                        for (int y = 1; y < this.snap.length - 1; y++)
                        {

                            if (((xSquared + Math.pow(y - (this.bsize + 1), 2) + zSquared) <= brushSizeSquared))
                            {
                                if (this.erode(x, y, z))
                                {
                                    this.snap[x][y][z].getNativeBlock().setTypeId(BlockTypes.AIR.getInternalId());
                                }
                            }
                        }
                    }
                }
            }
        }
        if (this.fillFace >= 0 && this.fillFace <= 6)
        {
            final double brushSizeSquared = Math.pow(this.bsize + 0.5, 2);

            for (int currentFillRecursion = 0; currentFillRecursion < this.fillRecursion; currentFillRecursion++)
            {
                this.getMatrix();

                for (int z = 1; z < this.snap.length - 1; z++)
                {

                    final double zSquared = Math.pow(z - (this.bsize + 1), 2);
                    for (int x = 1; x < this.snap.length - 1; x++)
                    {

                        final double xSquared = Math.pow(x - (this.bsize + 1), 2);
                        for (int y = 1; y < this.snap.length - 1; y++)
                        {

                            if (((xSquared + Math.pow(y - (this.bsize + 1), 2) + zSquared) <= brushSizeSquared))
                            {
                                if (this.fill(x, y, z))
                                {
                                    this.snap[x][y][z].getNativeBlock().setTypeId(this.snap[x][y][z].getId());
                                }
                            }
                        }
                    }
                }
            }
        }

        for (BlockWrapper[][] firstSnapSlice : this.firstSnap)
        {
            for (BlockWrapper[] firstSnapString : firstSnapSlice)
            {
                for (final BlockWrapper block : firstSnapString)
                {
                    if (block.getI() != block.getNativeBlock().getTypeId())
                    {
                        undo.put(block.getNativeBlock());
                    }
                }
            }
        }

        v.owner().storeUndo(undo);
    }

    @SuppressWarnings("deprecation")
	private void rfilling(final SnipeData v)
    {
        final Undo undo = new Undo();

        if (this.fillFace >= 0 && this.fillFace <= 6)
        {
            final double bSquared = Math.pow(this.bsize + 0.5, 2);

            for (int currentFillRecursion = 0; currentFillRecursion < this.fillRecursion; currentFillRecursion++)
            {
                this.getMatrix();

                for (int z = 1; z < this.snap.length - 1; z++)
                {
                    final double zSquared = Math.pow(z - (this.bsize + 1), 2);
                    for (int x = 1; x < this.snap.length - 1; x++)
                    {
                        final double xSquared = Math.pow(x - (this.bsize + 1), 2);
                        for (int y = 1; y < this.snap.length - 1; y++)
                        {
                            if (((xSquared + Math.pow(y - (this.bsize + 1), 2) + zSquared) <= bSquared))
                            {
                                if (this.fill(x, y, z))
                                {
                                    this.snap[x][y][z].getNativeBlock().setTypeId(this.snap[x][y][z].getId());
                                }
                            }
                        }
                    }
                }
            }
        }
        if (this.erodeFace >= 0 && this.erodeFace <= 6)
        {
            final double bSquared = Math.pow(this.bsize + this.trueCircle, 2);

            for (int currentErodeRecursion = 0; currentErodeRecursion < this.erodeRecursion; currentErodeRecursion++)
            {
                this.getMatrix();

                for (int z = 1; z < this.snap.length - 1; z++)
                {

                    final double zSquared = Math.pow(z - (this.bsize + 1), 2);
                    for (int x = 1; x < this.snap.length - 1; x++)
                    {

                        final double xSquared = Math.pow(x - (this.bsize + 1), 2);
                        for (int y = 1; y < this.snap.length - 1; y++)
                        {

                            if (((xSquared + Math.pow(y - (this.bsize + 1), 2) + zSquared) <= bSquared))
                            {
                                if (this.erode(x, y, z))
                                {
                                    this.snap[x][y][z].getNativeBlock().setTypeId(BlockTypes.AIR.getInternalId());
                                }
                            }
                        }
                    }
                }
            }
        }

        for (BlockWrapper[][] firstSnapSlice : this.firstSnap)
        {
            for (BlockWrapper[] firstSnapString : firstSnapSlice)
            {
                for (final BlockWrapper block : firstSnapString)
                {
                    if (block.getI() != block.getNativeBlock().getTypeId())
                    {
                        undo.put(block.getNativeBlock());
                    }
                }
            }
        }

        v.owner().storeUndo(undo);
    }

    @Override
    protected final void arrow(final SnipeData v)
    {
        this.bsize = v.getBrushSize();

        this.snap = new BlockWrapper[0][0][0];

        this.erodeFace = this.generator.nextInt(5) + 1;
        this.fillFace = this.generator.nextInt(3) + 3;
        this.erodeRecursion = this.generator.nextInt(3);
        this.fillRecursion = this.generator.nextInt(3);

        if (this.fillRecursion == 0 && this.erodeRecursion == 0)
        { // if they are both zero, it will lead to a null pointer exception. Still want to give them a
            // chance to be zero though, for more interestingness -Gav
            this.erodeRecursion = this.generator.nextInt(2) + 1;
            this.fillRecursion = this.generator.nextInt(2) + 1;
        }

        this.rerosion(v);
    }

    @Override
    protected final void powder(final SnipeData v)
    {
        this.bsize = v.getBrushSize();

        this.snap = new BlockWrapper[0][0][0];

        this.erodeFace = this.generator.nextInt(3) + 3;
        this.fillFace = this.generator.nextInt(5) + 1;
        this.erodeRecursion = this.generator.nextInt(3);
        this.fillRecursion = this.generator.nextInt(3);
        if (this.fillRecursion == 0 && this.erodeRecursion == 0)
        { // if they are both zero, it will lead to a null pointer exception. Still want to give them a
            // chance to be zero though, for more interestingness -Gav
            this.erodeRecursion = this.generator.nextInt(2) + 1;
            this.fillRecursion = this.generator.nextInt(2) + 1;
        }

        this.rfilling(v);
    }

    @Override
    public final void info(final Message vm)
    {
        vm.brushName(this.getName());
        vm.size();
    }

    /**
     * @author unknown
     */
    private class BlockWrapper
    {
        private boolean solid;
        private AsyncBlock nativeBlock;
        private int id;
        private int i;

        /**
         * @param bl
         */
        @SuppressWarnings("deprecation")
		public BlockWrapper(final AsyncBlock bl)
        {
            this.setNativeBlock(bl);
            this.setI(bl.getTypeId());
            switch (bl.getType())
            {
                case AIR:
                    this.setSolid(false);
                    break;
                case WATER:
                    this.setSolid(false);
                    break;
                case LAVA:
                    this.setSolid(false);
                    break;
                default:
                    this.setSolid(true);
            }
        }

        public boolean isSolid()
        {
            return solid;
        }

        public void setSolid(boolean solid)
        {
            this.solid = solid;
        }

        public AsyncBlock getNativeBlock()
        {
            return nativeBlock;
        }

        public void setNativeBlock(AsyncBlock nativeBlock)
        {
            this.nativeBlock = nativeBlock;
        }

        public int getId()
        {
            return id;
        }

        public void setId(int id)
        {
            this.id = id;
        }

        public int getI()
        {
            return i;
        }

        public void setI(int i)
        {
            this.i = i;
        }
    }

    @Override
    public String getPermissionNode()
    {
        return "voxelsniper.brush.randomerode";
    }
}
