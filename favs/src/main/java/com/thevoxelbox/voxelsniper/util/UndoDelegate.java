package com.thevoxelbox.voxelsniper.util;

import com.thevoxelbox.voxelsniper.Undo;
import org.bukkit.BlockChangeDelegate;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

public class UndoDelegate implements BlockChangeDelegate {
    private final World targetWorld;
    private Undo currentUndo;

    public UndoDelegate(World targetWorld) {
        this.targetWorld = targetWorld;
        this.currentUndo = new Undo();
    }

    public Undo getUndo() {
        final Undo pastUndo = currentUndo;
        currentUndo = new Undo();
        return pastUndo;
    }

    @SuppressWarnings("deprecation")
    public boolean setBlock(Block b) {
        this.currentUndo.put(this.targetWorld.getBlockAt(b.getLocation()));
        this.targetWorld.getBlockAt(b.getLocation()).setBlockData(b.getBlockData());
        return true;
    }

    @Override
    public boolean setBlockData(int x, int y, int z, BlockData blockData) {
        this.currentUndo.put(this.targetWorld.getBlockAt(x, y, z));
        this.targetWorld.getBlockAt(x, y, z).setBlockData(blockData);
        return true;
    }

    @Override
    public BlockData getBlockData(int x, int y, int z) {
        return this.targetWorld.getBlockAt(x, y, z).getBlockData();
    }

    @Override
    public int getHeight() {
        return this.targetWorld.getMaxHeight();
    }

    @Override
    public boolean isEmpty(int x, int y, int z) {
        return this.targetWorld.getBlockAt(x, y, z).isEmpty();
    }
}
