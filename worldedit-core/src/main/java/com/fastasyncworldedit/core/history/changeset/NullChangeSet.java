package com.fastasyncworldedit.core.history.changeset;

import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;

import java.util.Collections;
import java.util.Iterator;

public class NullChangeSet extends AbstractChangeSet {

    public NullChangeSet(World world) {
        super(world);
    }

    @Override
    public final void close() {
    }

    @Override
    public final void add(int x, int y, int z, int combinedFrom, int combinedTo) {

    }

    @Override
    public void addTileCreate(final FaweCompoundTag tag) {

    }

    @Override
    public void addTileRemove(final FaweCompoundTag tag) {

    }

    @Override
    public void addEntityRemove(final FaweCompoundTag tag) {

    }

    @Override
    public void addEntityCreate(final FaweCompoundTag tag) {

    }

    @Override
    public void addBiomeChange(int x, int y, int z, BiomeType from, BiomeType to) {

    }

    @Override
    public Iterator<Change> getIterator(BlockBag blockBag, int mode, boolean redo) {
        return getIterator(redo);
    }

    @Override
    public ChangeExchangeCoordinator getCoordinatedChanges(final BlockBag blockBag, final int mode, final boolean dir) {
        return new ChangeExchangeCoordinator(((exchanger, changes) -> {
            try {
                exchanger.exchange(null);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }));
    }

    @Override
    public final Iterator<Change> getIterator(boolean undo) {
        return Collections.emptyIterator();
    }

    @Override
    public final int size() {
        return 0;
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
