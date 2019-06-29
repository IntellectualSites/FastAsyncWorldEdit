package com.thevoxelbox.voxelsniper.brush;

import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.Undo;
import org.bukkit.ChatColor;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Scanner;

public class StencilListBrush extends Brush {
    private byte pasteOption = 1; // 0 = full, 1 = fill, 2 = replace
    private String filename = "NoFileLoaded";
    private short x;
    private short z;
    private short y;
    private short xRef;
    private short zRef;
    private short yRef;
    private byte pasteParam = 0;
    private HashMap<Integer, String> stencilList = new HashMap<>();

    public StencilListBrush() {
        this.setName("StencilList");
    }

    private String readRandomStencil() {
        double rand = Math.random() * (this.stencilList.size());
        final int choice = (int) rand;
        return this.stencilList.get(choice);
    }

    private void readStencilList() {
        final File file = new File("plugins/VoxelSniper/stencilLists/" + this.filename + ".txt");
        if (file.exists()) {
            try {
                final Scanner scanner = new Scanner(file);
                int counter = 0;
                while (scanner.hasNext()) {
                    this.stencilList.put(counter, scanner.nextLine());
                    counter++;
                }
                scanner.close();
            } catch (final Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void stencilPaste(final SnipeData v) {
        if (this.filename.matches("NoFileLoaded")) {
            v.sendMessage(ChatColor.RED + "You did not specify a filename for the list.  This is required.");
            return;
        }

        final String stencilName = this.readRandomStencil();
        v.sendMessage(stencilName);

        final Undo undo = new Undo();
        final File file = new File("plugins/VoxelSniper/stencils/" + stencilName + ".vstencil");

        if (file.exists()) {
            try (final DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))){
                this.x = in.readShort();
                this.z = in.readShort();
                this.y = in.readShort();

                this.xRef = in.readShort();
                this.zRef = in.readShort();
                this.yRef = in.readShort();

                final int numRuns = in.readInt();
                // Something here that checks ranks using sanker'world thingie he added to Sniper and boots you out with error message if too big.
                final int volume = this.x * this.y * this.z;
                v.owner().getPlayer().sendMessage(ChatColor.AQUA + this.filename + " pasted.  Volume is " + volume + " blocks.");

                int currX = -this.xRef; // so if your ref point is +5 x, you want to start pasting -5 blocks from the clicked point (the reference) to get the
                // corner, for example.
                int currZ = -this.zRef;
                int currY = -this.yRef;
                int id;
                int data;
                if (this.pasteOption == 0) {
                    for (int i = 1; i < numRuns + 1; i++) {
                        if (in.readBoolean()) {
                            final int numLoops = in.readByte() + 128;
                            id = (in.readByte() + 128);
                            data = (in.readByte() + 128);
                            for (int j = 0; j < numLoops; j++) {
                                undo.put(this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ));
                                this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ).setTypeIdAndPropertyId(id, data, false);
                                currX++;
                                if (currX == this.x - this.xRef) {
                                    currX = -this.xRef;
                                    currZ++;
                                    if (currZ == this.z - this.zRef) {
                                        currZ = -this.zRef;
                                        currY++;
                                    }
                                }
                            }
                        } else {
                            undo.put(this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ));
                            this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ).setTypeIdAndPropertyId((in.readByte() + 128), (in.readByte() + 128), false);
                            currX++;
                            if (currX == this.x - this.xRef) {
                                currX = -this.xRef;
                                currZ++;
                                if (currZ == this.z - this.zRef) {
                                    currZ = -this.zRef;
                                    currY++;
                                }
                            }
                        }
                    }
                } else if (this.pasteOption == 1) {
                    for (int i = 1; i < numRuns + 1; i++) {
                        if (in.readBoolean()) {
                            final int numLoops = in.readByte() + 128;
                            id = (in.readByte() + 128);
                            data = (in.readByte() + 128);
                            for (int j = 0; j < numLoops; j++) {
                                if (id != 0 && this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ).isEmpty()) {
                                    undo.put(this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ));
                                    this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ).setTypeIdAndPropertyId(id, (data), false);
                                }
                                currX++;
                                if (currX == this.x - this.xRef) {
                                    currX = -this.xRef;
                                    currZ++;
                                    if (currZ == this.z - this.zRef) {
                                        currZ = -this.zRef;
                                        currY++;
                                    }
                                }
                            }
                        } else {
                            id = (in.readByte() + 128);
                            data = (in.readByte() + 128);
                            if (id != 0 && this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ).isEmpty()) {
                                undo.put(this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ));
                                this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ).setTypeIdAndPropertyId(id, (data), false);
                            }
                            currX++;
                            if (currX == this.x - this.xRef) {
                                currX = -this.xRef;
                                currZ++;
                                if (currZ == this.z - this.zRef) {
                                    currZ = -this.zRef;
                                    currY++;
                                }
                            }
                        }
                    }
                } else { // replace
                    for (int i = 1; i < numRuns + 1; i++) {
                        if (in.readBoolean()) {
                            final int numLoops = in.readByte() + 128;
                            id = (in.readByte() + 128);
                            data = (in.readByte() + 128);
                            for (int j = 0; j < (numLoops); j++) {
                                if (id != 0) {
                                    undo.put(this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ));
                                    this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ).setTypeIdAndPropertyId(id, data, false);
                                }
                                currX++;
                                if (currX == this.x - this.xRef) {
                                    currX = -this.xRef;
                                    currZ++;
                                    if (currZ == this.z - this.zRef) {
                                        currZ = -this.zRef;
                                        currY++;
                                    }
                                }
                            }
                        } else {
                            id = (in.readByte() + 128);
                            data = (in.readByte() + 128);
                            if (id != 0) {
                                undo.put(this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ));
                                this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ).setTypeIdAndPropertyId(id, data, false);
                            }
                            currX++;
                            if (currX == this.x) {
                                currX = 0;
                                currZ++;
                                if (currZ == this.z) {
                                    currZ = 0;
                                    currY++;
                                }
                            }
                        }
                    }
                }
                in.close();
                v.owner().storeUndo(undo);

            } catch (final Exception exception) {
                v.owner().getPlayer().sendMessage(ChatColor.RED + "Something went wrong.");
                exception.printStackTrace();
            }
        } else {
            v.owner().getPlayer().sendMessage(ChatColor.RED + "You need to type a stencil name / your specified stencil does not exist.");
        }
    }

    @SuppressWarnings("deprecation")
    private void stencilPaste180(final SnipeData v) {
        if (this.filename.matches("NoFileLoaded")) {
            v.owner().getPlayer().sendMessage(ChatColor.RED + "You did not specify a filename for the list.  This is required.");
            return;
        }

        final String stencilName = this.readRandomStencil();

        final Undo undo = new Undo();
        final File file = new File("plugins/VoxelSniper/stencils/" + stencilName + ".vstencil");

        if (file.exists()) {
            try (final DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {

                this.x = in.readShort();
                this.z = in.readShort();
                this.y = in.readShort();

                this.xRef = in.readShort();
                this.zRef = in.readShort();
                this.yRef = in.readShort();

                final int numRuns = in.readInt();
                // Something here that checks ranks using sanker'world thingie he added to Sniper and boots you out with error message if too big.
                final int volume = this.x * this.y * this.z;
                v.owner().getPlayer().sendMessage(ChatColor.AQUA + this.filename + " pasted.  Volume is " + volume + " blocks.");

                int currX = +this.xRef; // so if your ref point is +5 x, you want to start pasting -5 blocks from the clicked point (the reference) to get the
                // corner, for example.
                int currZ = +this.zRef;
                int currY = -this.yRef;
                int id;
                int data;
                if (this.pasteOption == 0) {
                    for (int i = 1; i < numRuns + 1; i++) {
                        if (in.readBoolean()) {
                            final int numLoops = in.readByte() + 128;
                            id = (in.readByte() + 128);
                            data = (in.readByte() + 128);
                            for (int j = 0; j < numLoops; j++) {
                                undo.put(this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ));
                                this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ).setTypeIdAndPropertyId(id, data, false);
                                currX--;
                                if (currX == -this.x + this.xRef) {
                                    currX = this.xRef;
                                    currZ--;
                                    if (currZ == -this.z + this.zRef) {
                                        currZ = +this.zRef;
                                        currY++;
                                    }
                                }
                            }
                        } else {
                            undo.put(this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ));
                            this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ).setTypeIdAndPropertyId((in.readByte() + 128), (in.readByte() + 128), false);
                            currX--;
                            if (currX == -this.x + this.xRef) {
                                currX = this.xRef;
                                currZ--;
                                if (currZ == -this.z + this.zRef) {
                                    currZ = +this.zRef;
                                    currY++;
                                }
                            }
                        }
                    }
                } else if (this.pasteOption == 1) {
                    for (int i = 1; i < numRuns + 1; i++) {
                        if (in.readBoolean()) {
                            final int numLoops = in.readByte() + 128;
                            id = (in.readByte() + 128);
                            data = (in.readByte() + 128);
                            for (int j = 0; j < numLoops; j++) {
                                if (id != 0 && this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ).isEmpty()) {
                                    undo.put(this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ));
                                    this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ).setTypeIdAndPropertyId(id, (data), false);
                                }
                                currX--;
                                if (currX == -this.x + this.xRef) {
                                    currX = this.xRef;
                                    currZ--;
                                    if (currZ == -this.z + this.zRef) {
                                        currZ = +this.zRef;
                                        currY++;
                                    }
                                }
                            }
                        } else {
                            id = (in.readByte() + 128);
                            data = (in.readByte() + 128);
                            if (id != 0 && this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ).isEmpty()) {
                                undo.put(this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ));
                                this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ).setTypeIdAndPropertyId(id, (data), false);
                            }
                            currX--;
                            if (currX == -this.x + this.xRef) {
                                currX = this.xRef;
                                currZ--;
                                if (currZ == -this.z + this.zRef) {
                                    currZ = +this.zRef;
                                    currY++;
                                }
                            }
                        }
                    }
                } else { // replace
                    for (int i = 1; i < numRuns + 1; i++) {
                        if (in.readBoolean()) {
                            final int numLoops = in.readByte() + 128;
                            id = (in.readByte() + 128);
                            data = (in.readByte() + 128);
                            for (int j = 0; j < (numLoops); j++) {
                                if (id != 0) {
                                    undo.put(this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ));
                                    this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ).setTypeIdAndPropertyId(id, data, false);
                                }
                                currX--;
                                if (currX == -this.x + this.xRef) {
                                    currX = this.xRef;
                                    currZ--;
                                    if (currZ == -this.z + this.zRef) {
                                        currZ = +this.zRef;
                                        currY++;
                                    }
                                }
                            }
                        } else {
                            id = (in.readByte() + 128);
                            data = (in.readByte() + 128);
                            if (id != 0) {
                                undo.put(this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ));
                                this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ).setTypeIdAndPropertyId(id, data, false);
                            }
                            currX--;
                            if (currX == -this.x + this.xRef) {
                                currX = this.xRef;
                                currZ--;
                                if (currZ == -this.z + this.zRef) {
                                    currZ = +this.zRef;
                                    currY++;
                                }
                            }
                        }
                    }
                }
                in.close();
                v.owner().storeUndo(undo);

            } catch (final Exception exception) {
                v.owner().getPlayer().sendMessage(ChatColor.RED + "Something went wrong.");
                exception.printStackTrace();
            }
        } else {
            v.owner().getPlayer().sendMessage(ChatColor.RED + "You need to type a stencil name / your specified stencil does not exist.");
        }
    }

    @SuppressWarnings("deprecation")
    private void stencilPaste270(final SnipeData v) {
        if (this.filename.matches("NoFileLoaded")) {
            v.owner().getPlayer().sendMessage(ChatColor.RED + "You did not specify a filename for the list.  This is required.");
            return;
        }

        final String stencilName = this.readRandomStencil();

        final Undo undo = new Undo();
        final File file = new File("plugins/VoxelSniper/stencils/" + stencilName + ".vstencil");

        if (file.exists()) {
            try (final DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {

                this.x = in.readShort();
                this.z = in.readShort();
                this.y = in.readShort();

                this.xRef = in.readShort();
                this.zRef = in.readShort();
                this.yRef = in.readShort();

                final int numRuns = in.readInt();
                // Something here that checks ranks using sanker'world thingie he added to Sniper and boots you out with error message if too big.
                final int volume = this.x * this.y * this.z;
                v.owner().getPlayer().sendMessage(ChatColor.AQUA + this.filename + " pasted.  Volume is " + volume + " blocks.");

                int currX = +this.zRef; // so if your ref point is +5 x, you want to start pasting -5 blocks from the clicked point (the reference) to get the
                // corner, for example.
                int currZ = -this.xRef;
                int currY = -this.yRef;
                int id;
                int data;
                if (this.pasteOption == 0) {
                    for (int i = 1; i < numRuns + 1; i++) {
                        if (in.readBoolean()) {
                            final int numLoops = in.readByte() + 128;
                            id = (in.readByte() + 128);
                            data = (in.readByte() + 128);
                            for (int j = 0; j < numLoops; j++) {
                                undo.put(this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ));
                                this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ).setTypeIdAndPropertyId(id, data, false);
                                currZ++;
                                if (currZ == this.x - this.xRef) {
                                    currZ = -this.xRef;
                                    currX--;
                                    if (currX == -this.z + this.zRef) {
                                        currX = +this.zRef;
                                        currY++;
                                    }
                                }
                            }
                        } else {
                            undo.put(this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ));
                            this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ).setTypeIdAndPropertyId((in.readByte() + 128), (in.readByte() + 128), false);
                            currZ++;
                            currZ++;
                            if (currZ == this.x - this.xRef) {
                                currZ = -this.xRef;
                                currX--;
                                if (currX == -this.z + this.zRef) {
                                    currX = +this.zRef;
                                    currY++;
                                }
                            }
                        }
                    }
                } else if (this.pasteOption == 1) {
                    for (int i = 1; i < numRuns + 1; i++) {
                        if (in.readBoolean()) {
                            final int numLoops = in.readByte() + 128;
                            id = (in.readByte() + 128);
                            data = (in.readByte() + 128);
                            for (int j = 0; j < numLoops; j++) {
                                if (id != 0 && this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ).isEmpty()) { // no reason to paste air over
                                    // air, and it prevents us
                                    // most of the time from
                                    // having to even check the
                                    // block.
                                    undo.put(this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ));
                                    this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ).setTypeIdAndPropertyId(id, (data), false);
                                }
                                currZ++;
                                if (currZ == this.x - this.xRef) {
                                    currZ = -this.xRef;
                                    currX--;
                                    if (currX == -this.z + this.zRef) {
                                        currX = +this.zRef;
                                        currY++;
                                    }
                                }
                            }
                        } else {
                            id = (in.readByte() + 128);
                            data = (in.readByte() + 128);
                            if (id != 0 && this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ).isEmpty()) { // no reason to paste air over
                                // air, and it prevents us most of
                                // the time from having to even
                                // check the block.
                                undo.put(this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ));
                                this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ).setTypeIdAndPropertyId(id, (data), false);
                            }
                            currZ++;
                            if (currZ == this.x - this.xRef) {
                                currZ = -this.xRef;
                                currX--;
                                if (currX == -this.z + this.zRef) {
                                    currX = +this.zRef;
                                    currY++;
                                }
                            }
                        }
                    }
                } else { // replace
                    for (int i = 1; i < numRuns + 1; i++) {
                        if (in.readBoolean()) {
                            final int numLoops = in.readByte() + 128;
                            id = (in.readByte() + 128);
                            data = (in.readByte() + 128);
                            for (int j = 0; j < (numLoops); j++) {
                                if (id != 0) {
                                    undo.put(this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ));
                                    this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ).setTypeIdAndPropertyId(id, data, false);
                                }
                                currZ++;
                                if (currZ == this.x - this.xRef) {
                                    currZ = -this.xRef;
                                    currX--;
                                    if (currX == -this.z + this.zRef) {
                                        currX = +this.zRef;
                                        currY++;
                                    }
                                }
                            }
                        } else {
                            id = (in.readByte() + 128);
                            data = (in.readByte() + 128);
                            if (id != 0) {
                                undo.put(this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ));
                                this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ).setTypeIdAndPropertyId(id, data, false);
                            }
                            currZ++;
                            if (currZ == this.x - this.xRef) {
                                currZ = -this.xRef;
                                currX--;
                                if (currX == -this.z + this.zRef) {
                                    currX = +this.zRef;
                                    currY++;
                                }
                            }
                        }
                    }
                }
                in.close();
                v.owner().storeUndo(undo);

            } catch (final Exception exception) {
                v.owner().getPlayer().sendMessage(ChatColor.RED + "Something went wrong.");
                exception.printStackTrace();
            }
        } else {
            v.owner().getPlayer().sendMessage(ChatColor.RED + "You need to type a stencil name / your specified stencil does not exist.");
        }
    }

    @SuppressWarnings("deprecation")
    private void stencilPaste90(final SnipeData v) {
        if (this.filename.matches("NoFileLoaded")) {
            v.sendMessage(ChatColor.RED + "You did not specify a filename for the list.  This is required.");
            return;
        }

        final String stencilName = this.readRandomStencil();

        final Undo undo = new Undo();
        final File file = new File("plugins/VoxelSniper/stencils/" + stencilName + ".vstencil");

        if (file.exists()) {
            try {
                final DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));

                this.x = in.readShort();
                this.z = in.readShort();
                this.y = in.readShort();

                this.xRef = in.readShort();
                this.zRef = in.readShort();
                this.yRef = in.readShort();

                final int numRuns = in.readInt();
                // Something here that checks ranks using sanker'world thingie he added to Sniper and boots you out with error message if too big.
                final int volume = this.x * this.y * this.z;
                v.sendMessage(ChatColor.AQUA + this.filename + " pasted.  Volume is " + volume + " blocks.");

                int currX = -this.zRef; // so if your ref point is +5 x, you want to start pasting -5 blocks from the clicked point (the reference) to get the
                // corner, for example.
                int currZ = +this.xRef;
                int currY = -this.yRef;
                int id;
                int data;
                if (this.pasteOption == 0) {
                    for (int i = 1; i < numRuns + 1; i++) {
                        if (in.readBoolean()) {
                            final int numLoops = in.readByte() + 128;
                            id = (in.readByte() + 128);
                            data = (in.readByte() + 128);
                            for (int j = 0; j < numLoops; j++) {
                                undo.put(this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ));
                                this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ).setTypeIdAndPropertyId(id, data, false);
                                currZ--;
                                if (currZ == -this.x + this.xRef) {
                                    currZ = this.xRef;
                                    currX++;
                                    if (currX == this.z - this.zRef) {
                                        currX = -this.zRef;
                                        currY++;
                                    }
                                }
                            }
                        } else {
                            undo.put(this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ));
                            this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ).setTypeIdAndPropertyId((in.readByte() + 128), (in.readByte() + 128), false);
                            currZ--;
                            if (currZ == -this.x + this.xRef) {
                                currZ = this.xRef;
                                currX++;
                                if (currX == this.z - this.zRef) {
                                    currX = -this.zRef;
                                    currY++;
                                }
                            }
                        }
                    }
                } else if (this.pasteOption == 1) {
                    for (int i = 1; i < numRuns + 1; i++) {
                        if (in.readBoolean()) {
                            final int numLoops = in.readByte() + 128;
                            id = (in.readByte() + 128);
                            data = (in.readByte() + 128);
                            for (int j = 0; j < numLoops; j++) {
                                if (id != 0 && this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ).isEmpty()) {
                                    undo.put(this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ));
                                    this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ).setTypeIdAndPropertyId(id, (data), false);
                                }
                                currZ--;
                                if (currZ == -this.x + this.xRef) {
                                    currZ = this.xRef;
                                    currX++;
                                    if (currX == this.z - this.zRef) {
                                        currX = -this.zRef;
                                        currY++;
                                    }
                                }
                            }
                        } else {
                            id = (in.readByte() + 128);
                            data = (in.readByte() + 128);
                            if (id != 0 && this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ).isEmpty()) {
                                undo.put(this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ));
                                this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ).setTypeIdAndPropertyId(id, (data), false);
                            }
                            currZ--;
                            if (currZ == -this.x + this.xRef) {
                                currZ = this.xRef;
                                currX++;
                                if (currX == this.z - this.zRef) {
                                    currX = -this.zRef;
                                    currY++;
                                }
                            }
                        }
                    }
                } else { // replace
                    for (int i = 1; i < numRuns + 1; i++) {
                        if (in.readBoolean()) {
                            final int numLoops = in.readByte() + 128;
                            id = (in.readByte() + 128);
                            data = (in.readByte() + 128);
                            for (int j = 0; j < (numLoops); j++) {
                                if (id != 0) {
                                    undo.put(this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ));
                                    this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ).setTypeIdAndPropertyId(id, data, false);
                                }
                                currZ--;
                                if (currZ == -this.x + this.xRef) {
                                    currZ = this.xRef;
                                    currX++;
                                    if (currX == this.z - this.zRef) {
                                        currX = -this.zRef;
                                        currY++;
                                    }
                                }
                            }
                        } else {
                            id = (in.readByte() + 128);
                            data = (in.readByte() + 128);
                            if (id != 0) {
                                undo.put(this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ));
                                this.clampY(this.getTargetBlock().getX() + currX, this.getTargetBlock().getY() + currY, this.getTargetBlock().getZ() + currZ).setTypeIdAndPropertyId(id, data, false);
                            }
                            currZ--;
                            if (currZ == -this.x + this.xRef) {
                                currZ = this.xRef;
                                currX++;
                                if (currX == this.z - this.zRef) {
                                    currX = -this.zRef;
                                    currY++;
                                }
                            }
                        }
                    }
                }
                in.close();
                v.owner().storeUndo(undo);

            } catch (final Exception exception) {
                v.sendMessage(ChatColor.RED + "Something went wrong.");
                exception.printStackTrace();
            }
        } else {
            v.owner().getPlayer().sendMessage(ChatColor.RED + "You need to type a stencil name / your specified stencil does not exist.");
        }
    }

    private void stencilPasteRotation(final SnipeData v) {
        // just randomly chooses a rotation and then calls stencilPaste.
        this.readStencilList();
        final double random = Math.random();
        if (random < 0.26) {
            this.stencilPaste(v);
        } else if (random < 0.51) {
            this.stencilPaste90(v);
        } else if (random < 0.76) {
            this.stencilPaste180(v);
        } else {
            this.stencilPaste270(v);
        }

    }

    @Override
    protected final void arrow(final SnipeData v) {
        this.stencilPaste(v);
    }

    @Override
    protected final void powder(final SnipeData v) {
        this.stencilPasteRotation(v);
    }


    @Override
    public final void info(final Message vm) {
        vm.brushName(this.getName());
        vm.custom("File loaded: " + this.filename);
    }

    @Override
    public final void parameters(final String[] par, final SnipeData v) {
        if (par[1].equalsIgnoreCase("info")) {
            v.sendMessage(ChatColor.GOLD + "Stencil List brush Parameters:");
            v.sendMessage(ChatColor.AQUA + "/b schem [optional: 'full' 'fill' or 'replace', with fill as default] [name] -- Loads the specified stencil list.  Full/fill/replace must come first.  Full = paste all blocks, fill = paste only into air blocks, replace = paste full blocks in only, but replace anything in their way.");
            return;
        } else if (par[1].equalsIgnoreCase("full")) {
            this.pasteOption = 0;
            this.pasteParam = 1;
        } else if (par[1].equalsIgnoreCase("fill")) {
            this.pasteOption = 1;
            this.pasteParam = 1;
        } else if (par[1].equalsIgnoreCase("replace")) {
            this.pasteOption = 2;
            this.pasteParam = 1;
        }
        try {
            this.filename = par[1 + this.pasteParam];
            final File file = new File("plugins/VoxelSniper/stencilLists/" + this.filename + ".txt");
            if (file.exists()) {
                v.sendMessage(ChatColor.RED + "Stencil List '" + this.filename + "' exists and was loaded.");
                this.readStencilList();
            } else {
                v.sendMessage(ChatColor.AQUA + "Stencil List '" + this.filename + "' does not exist.  This brush will not function without a valid stencil list.");
                this.filename = "NoFileLoaded";
            }
        } catch (final Exception exception) {
            v.sendMessage(ChatColor.RED + "You need to type a stencil name.");
        }
    }

    @Override
    public String getPermissionNode() {
        return "voxelsniper.brush.stencillist";
    }
}
