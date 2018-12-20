package com.boydti.fawe.object.changeset;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.jnbt.anvil.HeightMapMCAGenerator;
import com.boydti.fawe.object.change.CFIChange;
import com.boydti.fawe.util.MainUtil;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;

public class CFIChangeSet extends FaweChangeSet {

    private final File file;

    public CFIChangeSet(HeightMapMCAGenerator hmmg, UUID uuid) throws IOException {
        super(hmmg);
        File folder = MainUtil.getFile(Fawe.imp().getDirectory(), Settings.IMP.PATHS.HISTORY + File.separator + uuid + File.separator + "CFI" + File.separator + hmmg.getWorldName());
        int max = MainUtil.getMaxFileId(folder);
        this.file = new File(folder, Integer.toString(max) + ".cfi");
        File parent = this.file.getParentFile();
        if (!parent.exists()) this.file.getParentFile().mkdirs();
        if (!this.file.exists()) this.file.createNewFile();
        hmmg.flushChanges(file);
    }

    @Override
    public boolean close() {
        return true;
    }

    @Override
    public boolean closeAsync() {
        return true;
    }

    @Override
    public void add(int x, int y, int z, int combinedFrom, int combinedTo) {
        throw new UnsupportedOperationException("Only CFI operations are supported");
    }

    @Override
    public void addTileCreate(CompoundTag tag) {
        throw new UnsupportedOperationException("Only CFI operations are supported");
    }

    @Override
    public void addTileRemove(CompoundTag tag) {
        throw new UnsupportedOperationException("Only CFI operations are supported");
    }

    @Override
    public void addEntityRemove(CompoundTag tag) {
        throw new UnsupportedOperationException("Only CFI operations are supported");
    }

    @Override
    public void addEntityCreate(CompoundTag tag) {
        throw new UnsupportedOperationException("Only CFI operations are supported");
    }

    @Override
    public void addBiomeChange(int x, int z, BaseBiome from, BaseBiome to) {
        throw new UnsupportedOperationException("Only CFI operations are supported");
    }

    @Override
    public Iterator<Change> getIterator(boolean redo) {
        return Collections.<Change>singleton(new CFIChange(file)).iterator();
    }

    @Override
    public int size() {
        return 1;
    }
}
