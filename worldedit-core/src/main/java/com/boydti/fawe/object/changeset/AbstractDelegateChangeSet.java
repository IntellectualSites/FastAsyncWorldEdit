package com.boydti.fawe.object.changeset;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.entity.Player;
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
import java.util.UUID;
import java.util.concurrent.Future;

public class AbstractDelegateChangeSet extends FaweChangeSet {
    public final FaweChangeSet parent;

    public static FaweChangeSet getDefaultChangeSet(World world, UUID uuid) {
        return FaweChangeSet.getDefaultChangeSet(world, uuid);
    }

    public AbstractDelegateChangeSet(FaweChangeSet parent) {
        super(parent.getWorld());
        this.parent = parent;
        this.waitingCombined = parent.waitingCombined;
        this.waitingAsync = parent.waitingAsync;
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
    public void close() throws IOException {
        parent.close();
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
    public EditSession toEditSession(Player player) {
        return parent.toEditSession(player);
    }

    @Override
    public EditSession toEditSession(Player player, Region[] regions) {
        return parent.toEditSession(player, regions);
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
}
