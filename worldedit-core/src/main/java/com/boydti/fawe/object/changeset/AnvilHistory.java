package com.boydti.fawe.object.changeset;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.jnbt.anvil.history.IAnvilHistory;
import com.boydti.fawe.util.MainUtil;
import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.world.biome.BiomeType;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

public class AnvilHistory extends FaweChangeSet implements IAnvilHistory {
    private final File folder;
    private int size;

    public AnvilHistory(String world, File folder) {
        super(world);
        this.folder = folder;
        size = -1;
    }

    public AnvilHistory(String world, UUID uuid) {
        super(world);
        File history = MainUtil.getFile(Fawe.imp().getDirectory(), Settings.IMP.PATHS.HISTORY + File.separator + world + File.separator + uuid);
        File destFolder = new File(history, Integer.toString(MainUtil.getMaxFileId(history)));
        if (!destFolder.exists()) {
            destFolder.mkdirs();
        }
        this.folder = destFolder;
        this.size = 0;
    }

    @Override
    public boolean addFileChange(File originalMCAFile) {
        try {
            Files.move(originalMCAFile.toPath(), Paths.get(folder.getPath(), originalMCAFile.getName()), StandardCopyOption.ATOMIC_MOVE);
            if (size != -1)  size++;
        } catch (IOException e) {
            e.printStackTrace();
            originalMCAFile.delete();
        }
        return false;
    }

    @Override
    public void add(int x, int y, int z, int combinedFrom, int combinedTo) {
        throw new UnsupportedOperationException("Only anvil operations are supported");
    }

    @Override
    public void addTileCreate(CompoundTag tag) {
        throw new UnsupportedOperationException("Only anvil operations are supported");
    }

    @Override
    public void addTileRemove(CompoundTag tag) {
        throw new UnsupportedOperationException("Only anvil operations are supported");
    }

    @Override
    public void addEntityRemove(CompoundTag tag) {
        throw new UnsupportedOperationException("Only anvil operations are supported");
    }

    @Override
    public void addEntityCreate(CompoundTag tag) {
        throw new UnsupportedOperationException("Only anvil operations are supported");
    }

    @Override
    public void addBiomeChange(int x, int z, BiomeType from, BiomeType to) {
        throw new UnsupportedOperationException("Only anvil operations are supported");
    }

    @Override
    public Iterator<Change> getIterator(boolean redo) {
        if (redo) throw new UnsupportedOperationException("Only undo operations are supported");
        List<File> files = Arrays.asList(folder.listFiles());
        final MutableAnvilChange change = new MutableAnvilChange();
        return Iterators.transform(files.iterator(), new Function<File, MutableAnvilChange>() {
            @Nullable
            @Override
            public MutableAnvilChange apply(@Nullable File input) {
                change.setSource(input.toPath());
                return change;
            }
        });
    }

    @Override
    public int size() {
        return size == -1 ? folder.listFiles().length : size;
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
