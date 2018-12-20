package com.boydti.fawe.object;

import com.boydti.fawe.object.changeset.FaweChangeSet;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.util.ArrayList;
import java.util.Iterator;

public class NullChangeSet extends FaweChangeSet {
    public NullChangeSet(World world) {
        super(world);
    }

    public NullChangeSet(String world) {
        super(world);
    }

    @Override
    public final boolean close() {
        return false;
    }

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
    public void addBiomeChange(int x, int z, BaseBiome from, BaseBiome to) {

    }

    @Override
    public void addChangeTask(FaweQueue queue) {

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
}
