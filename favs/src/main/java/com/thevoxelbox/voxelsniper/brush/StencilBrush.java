package com.thevoxelbox.voxelsniper.brush;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.GZIPInputStream;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.boydti.fawe.object.FaweInputStream;
import com.boydti.fawe.object.FaweOutputStream;
import com.boydti.fawe.object.io.PGZIPOutputStream;
import com.google.common.io.Files;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.Undo;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;

/**
 * This is paste only currently. Assumes files exist, and thus has no usefulness until I add in saving stencils later. Uses sniper-exclusive stencil format: 3
 * shorts for X,Z,Y size of cuboid 3 shorts for X,Z,Y offsets from the -X,-Z,-Y corner. This is the reference point for pasting, corresponding to where you
 * click your brush. 1 long integer saying how many runs of blocks are in the schematic (data is compressed into runs) 1 per run: ( 1 boolean: true = compressed
 * line ahead, false = locally unique block ahead. This wastes a bit instead of a byte, and overall saves space, as long as at least 1/8 of all RUNS are going
 * to be size 1, which in Minecraft is almost definitely true. IF boolean was true, next unsigned byte stores the number of consecutive blocks of the same type,
 * up to 256. IF boolean was false, there is no byte here, goes straight to ID and data instead, which applies to just one block. 2 bytes to identify type of
 * block. First byte is ID, second is data. This applies to every one of the line of consecutive blocks if boolean was true. )
 * 
 * TODO: Make limit a config option
 *
 * @author Gavjenks
 */
public class StencilBrush extends Brush
{
    private byte pasteOption = 1; // 0 = full, 1 = fill, 2 = replace
    private String filename = "NoFileLoaded";
    private short x;
    private short z;
    private short y;
    private short xRef;
    private short zRef;
    private short yRef;
    private byte pasteParam = 0;
    private int[] firstPoint = new int[3];
    private int[] secondPoint = new int[3];
    private int[] pastePoint = new int[3];
    private byte point = 1;

    /**
     *
     */
    public StencilBrush()
    {
        this.setName("Stencil");
    }

    @SuppressWarnings("deprecation")
	private void stencilPaste(final SnipeData v)
    {
        if (this.filename.matches("NoFileLoaded"))
        {
            v.sendMessage(ChatColor.RED + "You did not specify a filename.  This is required.");
            return;
        }

        final Undo undo = new Undo();
        final File file = new File("plugins/VoxelSniper/stencils/" + this.filename + ".vstencil");

        if (file.exists())
        {
            try
            {
                final FaweInputStream in = new FaweInputStream(new DataInputStream(new GZIPInputStream(new BufferedInputStream(new FileInputStream(file)))));

                this.x = in.readShort();
                this.z = in.readShort();
                this.y = in.readShort();

                this.xRef = in.readShort();
                this.zRef = in.readShort();
                this.yRef = in.readShort();

                final int numRuns = in.readInt();

                int currX = -this.xRef; // so if your ref point is +5 x, you want to start pasting -5 blocks from the clicked point (the reference) to get the
                // corner, for example.
                int currZ = -this.zRef;
                int currY = -this.yRef;
                int id;
                int blockPositionX = getTargetBlock().getX();
                int blockPositionY = getTargetBlock().getY();
                int blockPositionZ = getTargetBlock().getZ();
                if (this.pasteOption == 0)
                {
                    for (int i = 1; i < numRuns + 1; i++)
                    {
                        if (in.readBoolean())
                        {
                            final int numLoops = in.readByte() + 128;
                            id = in.readVarInt();
                            for (int j = 0; j < numLoops; j++)
                            {
                                undo.put(this.clampY(blockPositionX + currX, blockPositionY + currY, blockPositionZ + currZ));
                                this.clampY(blockPositionX + currX, blockPositionY + currY, blockPositionZ + currZ).setCombinedId(id);
                                currX++;
                                if (currX == this.x - this.xRef)
                                {
                                    currX = -this.xRef;
                                    currZ++;
                                    if (currZ == this.z - this.zRef)
                                    {
                                        currZ = -this.zRef;
                                        currY++;
                                    }
                                }
                            }
                        }
                        else
                        {
                            undo.put(this.clampY(blockPositionX + currX, blockPositionY + currY, blockPositionZ + currZ));
                            int combined = in.readVarInt();
                            this.clampY(blockPositionX + currX, blockPositionY + currY, blockPositionZ + currZ).setCombinedId(combined);
                            currX++;
                            if (currX == this.x - this.xRef)
                            {
                                currX = -this.xRef;
                                currZ++;
                                if (currZ == this.z - this.zRef)
                                {
                                    currZ = -this.zRef;
                                    currY++;
                                }
                            }
                        }
                    }
                }
                else if (this.pasteOption == 1)
                {
                    for (int i = 1; i < numRuns + 1; i++)
                    {
                        if (in.readBoolean())
                        {
                            final int numLoops = in.readByte() + 128;
                            id = (in.readVarInt());
                            for (int j = 0; j < numLoops; j++)
                            {

                                if (!BlockTypes.getFromStateId(id).getMaterial().isAir() && this.clampY(blockPositionX + currX, blockPositionY + currY, blockPositionZ + currZ).isEmpty())
                                {
                                    undo.put(this.clampY(blockPositionX + currX, blockPositionY + currY, blockPositionZ + currZ));
                                    this.clampY(blockPositionX + currX, blockPositionY + currY, blockPositionZ + currZ).setCombinedId(id);
                                }
                                currX++;
                                if (currX == this.x - this.xRef)
                                {
                                    currX = -this.xRef;
                                    currZ++;
                                    if (currZ == this.z - this.zRef)
                                    {
                                        currZ = -this.zRef;
                                        currY++;
                                    }
                                }
                            }
                        }
                        else
                        {
                            id = (in.readVarInt());
                            if (!BlockTypes.getFromStateId(id).getMaterial().isAir() && this.clampY(blockPositionX + currX, blockPositionY + currY, blockPositionZ + currZ).isEmpty())
                            {
                                undo.put(this.clampY(blockPositionX + currX, blockPositionY + currY, blockPositionZ + currZ));
                                // v.sendMessage("currX:" + currX + " currZ:"+currZ + " currY:" + currY + " id:" + id + " data:" + data);
                                this.clampY(blockPositionX + currX, blockPositionY + currY, blockPositionZ + currZ).setCombinedId(id);
                            }
                            currX++;
                            if (currX == this.x - this.xRef)
                            {
                                currX = -this.xRef;
                                currZ++;
                                if (currZ == this.z - this.zRef)
                                {
                                    currZ = -this.zRef;
                                    currY++;
                                }
                            }
                        }
                    }
                }
                else
                { // replace
                    for (int i = 1; i < numRuns + 1; i++)
                    {
                        if (in.readBoolean())
                        {
                            final int numLoops = in.readByte() + 128;
                            id = (in.readVarInt());
                            for (int j = 0; j < (numLoops); j++)
                            {
                                if (!BlockTypes.getFromStateId(id).getMaterial().isAir())
                                {
                                    undo.put(this.clampY(blockPositionX + currX, blockPositionY + currY, blockPositionZ + currZ));
                                    this.clampY(blockPositionX + currX, blockPositionY + currY, blockPositionZ + currZ).setCombinedId(id);
                                }
                                currX++;
                                if (currX == this.x - this.xRef)
                                {
                                    currX = -this.xRef;
                                    currZ++;
                                    if (currZ == this.z - this.zRef)
                                    {
                                        currZ = -this.zRef;
                                        currY++;
                                    }
                                }
                            }
                        }
                        else
                        {
                            id = (in.readVarInt());
                            if (id != 0)
                            {
                                undo.put(this.clampY(blockPositionX + currX, blockPositionY + currY, blockPositionZ + currZ));
                                this.clampY(blockPositionX + currX, blockPositionY + currY, blockPositionZ + currZ).setCombinedId(id);
                            }
                            currX++;
                            if (currX == this.x)
                            {
                                currX = 0;
                                currZ++;
                                if (currZ == this.z)
                                {
                                    currZ = 0;
                                    currY++;
                                }
                            }
                        }
                    }
                }
                in.close();
                v.owner().storeUndo(undo);

            }
            catch (final Exception exception)
            {
                v.sendMessage(ChatColor.RED + "Something went wrong.");
                exception.printStackTrace();
            }
        }
        else
        {
            v.sendMessage(ChatColor.RED + "You need to type a stencil name / your specified stencil does not exist.");
        }
    }

    @SuppressWarnings("deprecation")
	private void stencilSave(final SnipeData v)
    {

        final File file = new File("plugins/VoxelSniper/stencils/" + this.filename + ".vstencil");
        try
        {
            this.x = (short) (Math.abs((this.firstPoint[0] - this.secondPoint[0])) + 1);
            this.z = (short) (Math.abs((this.firstPoint[1] - this.secondPoint[1])) + 1);
            this.y = (short) (Math.abs((this.firstPoint[2] - this.secondPoint[2])) + 1);
            this.xRef = (short) ((this.firstPoint[0] > this.secondPoint[0]) ? (this.pastePoint[0] - this.secondPoint[0]) : (this.pastePoint[0] - this.firstPoint[0]));
            this.zRef = (short) ((this.firstPoint[1] > this.secondPoint[1]) ? (this.pastePoint[1] - this.secondPoint[1]) : (this.pastePoint[1] - this.firstPoint[1]));
            this.yRef = (short) ((this.firstPoint[2] > this.secondPoint[2]) ? (this.pastePoint[2] - this.secondPoint[2]) : (this.pastePoint[2] - this.firstPoint[2]));

            if ((this.x * this.y * this.z) > 50000)
            {
                v.sendMessage(ChatColor.AQUA + "Volume exceeds maximum limit.");
                return;
            }

            Files.createParentDirs(file);
            file.createNewFile();
            final FaweOutputStream out = new FaweOutputStream(new DataOutputStream(new PGZIPOutputStream(new BufferedOutputStream(new FileOutputStream(file)))));
            int blockPositionX = (this.firstPoint[0] > this.secondPoint[0]) ? this.secondPoint[0] : this.firstPoint[0];
            int blockPositionZ = (this.firstPoint[1] > this.secondPoint[1]) ? this.secondPoint[1] : this.firstPoint[1];
            int blockPositionY = (this.firstPoint[2] > this.secondPoint[2]) ? this.secondPoint[2] : this.firstPoint[2];
            out.writeShort(this.x);
            out.writeShort(this.z);
            out.writeShort(this.y);
            out.writeShort(this.xRef);
            out.writeShort(this.zRef);
            out.writeShort(this.yRef);

            v.sendMessage(ChatColor.AQUA + "Volume: " + this.x * this.z * this.y + " blockPositionX:" + blockPositionX + " blockPositionZ:" + blockPositionZ + " blockPositionY:" + blockPositionY);

            int[] blockArray = new int[this.x * this.z * this.y];
            byte[] runSizeArray = new byte[this.x * this.z * this.y];

            int lastId = (this.getWorld().getBlockAt(blockPositionX, blockPositionY, blockPositionZ).getCombinedId());
            int thisId;
            int counter = 0;
            int arrayIndex = 0;
            for (int y = 0; y < this.y; y++)
            {
                for (int z = 0; z < this.z; z++)
                {
                    for (int x = 0; x < this.x; x++)
                    {
                        AsyncBlock currentBlock = getWorld().getBlockAt(blockPositionX + x, blockPositionY + y, blockPositionZ + z);
                        thisId = (currentBlock.getCombinedId());
                        if (thisId != lastId || counter == 255)
                        {
                            blockArray[arrayIndex] = lastId;
                            runSizeArray[arrayIndex] = (byte) (counter - 128);
                            arrayIndex++;
                            counter = 1;
                            lastId = thisId;
                        }
                        else
                        {
                            counter++;
                            lastId = thisId;
                        }
                    }
                }
            }
            blockArray[arrayIndex] = lastId; // saving last run, which will always be left over.
            runSizeArray[arrayIndex] = (byte) (counter - 128);

            out.writeInt(arrayIndex + 1);
            // v.sendMessage("number of runs = " + arrayIndex);
            for (int i = 0; i < arrayIndex + 1; i++)
            {
                if (runSizeArray[i] > -127)
                {
                    out.writeBoolean(true);
                    out.writeByte(runSizeArray[i]);
                    out.writeVarInt(blockArray[i]);
                }
                else
                {
                    out.writeBoolean(false);
                    out.writeVarInt(blockArray[i]);
                }
            }

            v.sendMessage(ChatColor.BLUE + "Saved as '" + this.filename + "'.");
            out.close();

        }
        catch (final Exception exception)
        {
            v.sendMessage(ChatColor.RED + "Something went wrong.");
            exception.printStackTrace();
        }
    }

    @Override
    protected final void arrow(final SnipeData v)
    { // will be used to copy/save later on?
        if (this.point == 1)
        {
            this.firstPoint[0] = this.getTargetBlock().getX();
            this.firstPoint[1] = this.getTargetBlock().getZ();
            this.firstPoint[2] = this.getTargetBlock().getY();
            v.sendMessage(ChatColor.GRAY + "First point");
            v.sendMessage("X:" + this.firstPoint[0] + " Z:" + this.firstPoint[1] + " Y:" + this.firstPoint[2]);
            this.point = 2;
        }
        else if (this.point == 2)
        {
            this.secondPoint[0] = this.getTargetBlock().getX();
            this.secondPoint[1] = this.getTargetBlock().getZ();
            this.secondPoint[2] = this.getTargetBlock().getY();
            if ((Math.abs(this.firstPoint[0] - this.secondPoint[0]) * Math.abs(this.firstPoint[1] - this.secondPoint[1]) * Math.abs(this.firstPoint[2] - this.secondPoint[2])) > 5000000)
            {
                v.sendMessage(ChatColor.DARK_RED + "Area selected is too large. (Limit is 5,000,000 blocks)");
                this.point = 1;
            }
            else
            {
                v.sendMessage(ChatColor.GRAY + "Second point");
                v.sendMessage("X:" + this.secondPoint[0] + " Z:" + this.secondPoint[1] + " Y:" + this.secondPoint[2]);
                this.point = 3;
            }
        }
        else if (this.point == 3)
        {
            this.pastePoint[0] = this.getTargetBlock().getX();
            this.pastePoint[1] = this.getTargetBlock().getZ();
            this.pastePoint[2] = this.getTargetBlock().getY();
            v.sendMessage(ChatColor.GRAY + "Paste Reference point");
            v.sendMessage("X:" + this.pastePoint[0] + " Z:" + this.pastePoint[1] + " Y:" + this.pastePoint[2]);
            this.point = 1;

            this.stencilSave(v);
        }
    }

    @Override
    protected final void powder(final SnipeData v)
    { // will be used to paste later on
        this.stencilPaste(v);
    }

    @Override
    public final void info(final Message vm)
    {
        vm.brushName(this.getName());
        vm.custom("File loaded: " + this.filename);
    }

    @Override
    public final void parameters(final String[] par, final SnipeData v)
    {
        if (par[1].equalsIgnoreCase("info"))
        {
            v.sendMessage(ChatColor.GOLD + "Stencil brush Parameters:");
            v.sendMessage(ChatColor.AQUA + "/b schem [optional: 'full' 'fill' or 'replace', with fill as default] [name] -- Loads the specified schematic.  Allowed size of schematic is based on rank.  Full/fill/replace must come first.  Full = paste all blocks, fill = paste only into air blocks, replace = paste full blocks in only, but replace anything in their way.");
            v.sendMessage(ChatColor.BLUE + "Size of the stencils you are allowed to paste depends on rank (member / lite, sniper, curator, admin)");
            return;
        }
        else if (par[1].equalsIgnoreCase("full"))
        {
            this.pasteOption = 0;
            this.pasteParam = 1;
        }
        else if (par[1].equalsIgnoreCase("fill"))
        {
            this.pasteOption = 1;
            this.pasteParam = 1;
        }
        else if (par[1].equalsIgnoreCase("replace"))
        {
            this.pasteOption = 2;
            this.pasteParam = 1;
        }
        try
        {
            this.filename = par[1 + this.pasteParam];
            final File file = new File("plugins/VoxelSniper/stencils/" + this.filename + ".vstencil");
            if (file.exists())
            {
                v.sendMessage(ChatColor.RED + "Stencil '" + this.filename + "' exists and was loaded.  Make sure you are using powder if you do not want any chance of overwriting the file.");
            }
            else
            {
                v.sendMessage(ChatColor.AQUA + "Stencil '" + this.filename + "' does not exist.  Ready to be saved to, but cannot be pasted.");
            }
        }
        catch (final Exception exception)
        {
            v.sendMessage(ChatColor.RED + "You need to type a stencil name.");
        }
    }

    @Override
    public String getPermissionNode()
    {
        return "voxelsniper.brush.stencil";
    }
}
