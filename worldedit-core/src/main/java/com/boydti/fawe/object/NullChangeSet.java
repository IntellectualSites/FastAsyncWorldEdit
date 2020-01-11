package com.boydti.fawe.object;

import com.boydti.fawe.object.changeset.AbstractChangeSet;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import java.util.ArrayList;
import java.util.Iterator;

public class NullChangeSet extends AbstractChangeSet {
    public NullChangeSet(World world) {
        super(world);
    }

    public NullChangeSet(String world) {
        super(world);
    }

    @Override
    public final void close() {}

    @Override
    public final void add(int x, int y, int z, int combinedFrom, int combinedTo) {

    }

    @Override
    public final void addTileCreate(CompoundTag tag) {

    }

    @Override
    public final void addTileRemove(CompoundTag tag) {

    }

    @Override
    public final void addEntityRemove(CompoundTag tag) {

    }

    @Override
    public final void addEntityCreate(CompoundTag tag) {

    }

    @Override
    public void addBiomeChange(int x, int z, BiomeType from, BiomeType to) {

    }

    @Override
    public Iterator<Change> getIterator(BlockBag blockBag, int mode, boolean redo) {
        return getIterator(redo);
    }

    @Override
    public final Iterator<Change> getIterator(boolean undo) {
        return new ArrayList<Change>().iterator();
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
