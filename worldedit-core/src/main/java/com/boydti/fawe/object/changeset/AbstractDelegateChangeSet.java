package com.boydti.fawe.object.changeset;

import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.FaweQueue;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.history.change.BlockChange;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.history.change.EntityCreate;
import com.sk89q.worldedit.history.change.EntityRemove;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.Iterator;

public class AbstractDelegateChangeSet extends FaweChangeSet {
    public final FaweChangeSet parent;

    public AbstractDelegateChangeSet(FaweChangeSet parent) {
        super(parent.getWorld());
        this.parent = parent;
        this.waitingCombined = parent.waitingCombined;
        this.waitingAsync = parent.waitingAsync;
    }

    @Override
    public void addChangeTask(FaweQueue queue) {
        super.addChangeTask(queue);
    }

    @Override
    public boolean closeAsync() {
        return super.closeAsync();
    }

    @Override
    public boolean flush() {
        return parent.flush();
    }

    @Override
    public boolean close() {
        return super.close() && parent.close();
    }

    public final FaweChangeSet getParent() {
        return parent;
    }

    @Override
    public String getWorldName() {
        return parent.getWorldName();
    }

    @Override
    public World getWorld() {
        return parent.getWorld();
    }

    @Override
    @Deprecated
    public boolean flushAsync() {
        return parent.flushAsync();
    }

    @Override
    public void add(int x, int y, int z, int combinedFrom, int combinedTo) {
        parent.add(x, y, z, combinedFrom, combinedTo);
    }

    @Override
    public Iterator<Change> backwardIterator() {
        return parent.backwardIterator();
    }

    @Override
    public Iterator<Change> forwardIterator() {
        return parent.forwardIterator();
    }

    @Override
    public int size() {
        return parent.size();
    }

    @Override
    public void addBiomeChange(int x, int z, BiomeType from, BiomeType to) {
        parent.addBiomeChange(x, z, from, to);
    }

    @Override
    public void addTileCreate(CompoundTag tag) {
        parent.addTileCreate(tag);
    }

    @Override
    public void addTileRemove(CompoundTag tag) {
        parent.addTileRemove(tag);
    }

    @Override
    public void addEntityRemove(CompoundTag tag) {
        parent.addEntityRemove(tag);
    }

    @Override
    public void addEntityCreate(CompoundTag tag) {
        parent.addEntityCreate(tag);
    }

    @Override
    public Iterator<Change> getIterator(BlockBag blockBag, int mode, boolean redo) {
        return parent.getIterator(blockBag, mode, redo);
    }

    @Override
    public Iterator<Change> getIterator(boolean redo) {
        return parent.getIterator(redo);
    }

    @Override
    public void delete() {
        parent.delete();
    }

    @Override
    public EditSession toEditSession(FawePlayer player) {
        return parent.toEditSession(player);
    }

    @Override
    public void add(EntityCreate change) {
        parent.add(change);
    }

    @Override
    public void add(EntityRemove change) {
        parent.add(change);
    }

    @Override
    public void add(Change change) {
        parent.add(change);
    }

    @Override
    public void add(BlockChange change) {
        parent.add(change);
    }

    @Override
    public void add(BlockVector3 loc, BaseBlock from, BaseBlock to) {
        parent.add(loc, from, to);
    }

    @Override
    public void add(int x, int y, int z, BaseBlock from, BaseBlock to) {
        parent.add(x, y, z, from, to);
    }

    @Override
    public boolean isEmpty() {
        return parent.isEmpty();
    }

    @Override
    public void add(int x, int y, int z, int combinedFrom, BaseBlock to) {
        parent.add(x, y, z, combinedFrom, to);
    }

	@Override
	public boolean isRecordingChanges() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setRecordChanges(boolean recordChanges) {
		// TODO Auto-generated method stub
		
	}
}
