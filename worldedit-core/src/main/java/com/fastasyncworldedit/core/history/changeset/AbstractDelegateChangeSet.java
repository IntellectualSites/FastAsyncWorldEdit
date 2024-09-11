package com.fastasyncworldedit.core.history.changeset;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.history.change.BlockChange;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.history.change.EntityCreate;
import com.sk89q.worldedit.history.change.EntityRemove;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.Future;

public class AbstractDelegateChangeSet extends AbstractChangeSet {

    public final AbstractChangeSet parent;

    public AbstractDelegateChangeSet(AbstractChangeSet parent) {
        super(parent.getWorld());
        this.parent = parent;
    }

    public final AbstractChangeSet getParent() {
        return parent;
    }

    @Override
    public World getWorld() {
        return parent.getWorld();
    }

    @Override
    public void closeAsync() {
        parent.closeAsync();
    }

    @Override
    public void flush() {
        parent.flush();
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
    public void addBiomeChange(int x, int y, int z, BiomeType from, BiomeType to) {
        parent.addBiomeChange(x, y, z, from, to);
    }

    @Override
    public Iterator<Change> getIterator(BlockBag blockBag, int mode, boolean redo) {
        return parent.getIterator(blockBag, mode, redo);
    }

    @Override
    public ChangeExchangeCoordinator getCoordinatedChanges(final BlockBag blockBag, final int mode, final boolean dir) {
        return parent.getCoordinatedChanges(blockBag, mode, dir);
    }

    @Override
    public Iterator<Change> getIterator(boolean redo) {
        return parent.getIterator(redo);
    }

    @Override
    public EditSession toEditSession(Actor actor) {
        return parent.toEditSession(actor);
    }

    @Override
    public EditSession toEditSession(Actor actor, Region[] regions) {
        return parent.toEditSession(actor, regions);
    }

    @Override
    public void add(int x, int y, int z, int combinedFrom, int combinedTo) {
        parent.add(x, y, z, combinedFrom, combinedTo);
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
    public void add(int x, int y, int z, int combinedFrom, BaseBlock to) {
        parent.add(x, y, z, combinedFrom, to);
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
    public void close() throws IOException {
        parent.close();
    }

    @Override
    public boolean isEmpty() {
        return parent.isEmpty();
    }

    @Override
    public Future<?> addWriteTask(Runnable writeTask) {
        return parent.addWriteTask(writeTask);
    }

    @Override
    public Future<?> addWriteTask(Runnable writeTask, boolean completeNow) {
        return parent.addWriteTask(writeTask, completeNow);
    }

    @Override
    public boolean isRecordingChanges() {
        return parent.isRecordingChanges();
    }

    @Override
    public void setRecordChanges(boolean recordChanges) {
        parent.setRecordChanges(recordChanges);
    }

    @Override
    public int size() {
        return parent.size();
    }

    @Override
    public long longSize() {
        return parent.longSize();
    }

    @Override
    public void delete() {
        parent.delete();
    }

    @Override
    public ChangeSetSummary summarize(Region region, boolean shallow) {
        return parent.summarize(region, shallow);
    }

}
