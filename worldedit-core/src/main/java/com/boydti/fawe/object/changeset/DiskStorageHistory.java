package com.boydti.fawe.object.changeset;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.database.DBHandler;
import com.boydti.fawe.database.RollbackDatabase;
import com.boydti.fawe.object.FaweInputStream;
import com.boydti.fawe.object.FaweOutputStream;
import com.boydti.fawe.object.IntegerPair;
import com.boydti.fawe.object.change.MutableFullBlockChange;
import com.boydti.fawe.util.MainUtil;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Store the change on disk
 * - High disk usage
 * - Moderate CPU usage
 * - Minimal memory usage
 * - Slow
 */
public class DiskStorageHistory extends FaweStreamChangeSet {

    private UUID uuid;
    private File bdFile;
    private File bioFile;
    private File nbtfFile;
    private File nbttFile;
    private File entfFile;
    private File enttFile;

    /*
     * Block data
     *
     * [header]
     * {int origin x, int origin z}
     *
     * [contents]...
     * { short rel x, short rel z, unsigned byte y, short combinedFrom, short combinedTo }
     */
    private FaweOutputStream osBD;
    // biome
    private FaweOutputStream osBIO;
    // NBT From
    private NBTOutputStream osNBTF;
    // NBT To
    private NBTOutputStream osNBTT;
    // Entity Create From
    private NBTOutputStream osENTCF;
    // Entity Create To
    private NBTOutputStream osENTCT;

    private int index;

    public DiskStorageHistory(World world, UUID uuid) {
        super(world);
        init(uuid, world.getName());
    }

    public DiskStorageHistory(String world, UUID uuid) {
        super(world);
        init(uuid, world);
    }

    private void init(UUID uuid, String worldName) {
        File folder = MainUtil.getFile(Fawe.imp().getDirectory(), Settings.IMP.PATHS.HISTORY + File.separator + worldName + File.separator + uuid);
        int max = MainUtil.getMaxFileId(folder);
        init(uuid, max);
    }

    public DiskStorageHistory(String world, UUID uuid, int index) {
        super(world);
        init(uuid, index);
    }

    public DiskStorageHistory(World world, UUID uuid, int index) {
        super(world);
        init(uuid, index);
    }

    public DiskStorageHistory(File folder, World world, UUID uuid, int i) {
        super(world);
        this.uuid = uuid;
        this.index = i;
        initFiles(folder);
    }

    public DiskStorageHistory(File folder, String world, UUID uuid, int i) {
        super(world);
        this.uuid = uuid;
        this.index = i;
        initFiles(folder);
    }

    private void initFiles(File folder) {
        nbtfFile = new File(folder, index + ".nbtf");
        nbttFile = new File(folder, index + ".nbtt");
        entfFile = new File(folder, index + ".entf");
        enttFile = new File(folder, index + ".entt");
        bdFile = new File(folder, index + ".bd");
        bioFile = new File(folder, index + ".bio");
    }

    private void init(UUID uuid, int i) {
        this.uuid = uuid;
        this.index = i;
        File folder = MainUtil.getFile(Fawe.imp().getDirectory(), Settings.IMP.PATHS.HISTORY + File.separator + getWorld().getName() + File.separator + uuid);
        initFiles(folder);
    }

    @Override
    public void delete() {
//        Fawe.debug("Deleting history: " + getWorld().getName() + "/" + uuid + "/" + index);
        deleteFiles();
        if (Settings.IMP.HISTORY.USE_DATABASE) {
            RollbackDatabase db = DBHandler.IMP.getDatabase(getWorld());
            db.delete(uuid, index);
        }
    }

    public void deleteFiles() {
        bdFile.delete();
        nbtfFile.delete();
        nbttFile.delete();
        entfFile.delete();
        enttFile.delete();
    }

    public void undo(Player player, Region[] regions) {
        EditSession session = toEditSession(player, regions);
        session.undo(session);
        deleteFiles();
    }

    public void undo(Player player) {
        undo(player, null);
    }

    public void redo(Player player, Region[] regions) {
        EditSession session = toEditSession(player, regions);
        session.redo(session);
    }

    public void redo(Player player) {
        undo(player, null);
    }

    public UUID getUUID() {
        return uuid;
    }

    public File getBDFile() {
        return bdFile;
    }

    public File getNbtfFile() {
        return nbtfFile;
    }

    public File getNbttFile() {
        return nbttFile;
    }

    public File getEntfFile() {
        return entfFile;
    }

    public File getEnttFile() {
        return enttFile;
    }

    public File getBioFile() {
        return bioFile;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public void flush() {
        super.flush();
        synchronized (this) {
            try {
                if (osBD != null) osBD.flush();
                if (osBIO != null) osBIO.flush();
                if (osNBTF != null) osNBTF.flush();
                if (osNBTT != null) osNBTT.flush();
                if (osENTCF != null) osENTCF.flush();
                if (osENTCT != null) osENTCT.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
        synchronized (this) {
            try {
                if (osBD != null) {
                    osBD.close();
                    osBD = null;
                }
                if (osBIO != null) {
                    osBIO.close();
                    osBIO = null;
                }
                if (osNBTF != null) {
                    osNBTF.close();
                    osNBTF = null;
                }
                if (osNBTT != null) {
                    osNBTT.close();
                    osNBTT = null;
                }
                if (osENTCF != null) {
                    osENTCF.close();
                    osENTCF = null;
                }
                if (osENTCT != null) {
                    osENTCT.close();
                    osENTCT = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int getCompressedSize() {
        return bdFile.exists() ? (int) bdFile.length() : 0;
    }

    @Override
    public long getSizeInMemory() {
        return 80;
    }

    @Override
    public long getSizeOnDisk() {
        int total = 0;
        if (bdFile.exists()) {
            total += bdFile.length();
        }
        if (bioFile.exists()) {
            total += bioFile.length();
        }
        if (nbtfFile.exists()) {
            total += entfFile.length();
        }
        if (nbttFile.exists()) {
            total += entfFile.length();
        }
        if (entfFile.exists()) {
            total += entfFile.length();
        }
        if (enttFile.exists()) {
            total += entfFile.length();
        }
        return total;
    }

    @Override
    public FaweOutputStream getBlockOS(int x, int y, int z) throws IOException {
        if (osBD != null) {
            return osBD;
        }
        synchronized (this) {
            bdFile.getParentFile().mkdirs();
            bdFile.createNewFile();
            osBD = getCompressedOS(new FileOutputStream(bdFile));
            writeHeader(osBD, x, y, z);
            return osBD;
        }
    }

    @Override
    public FaweOutputStream getBiomeOS() throws IOException {
        if (osBIO != null) {
            return osBIO;
        }
        synchronized (this) {
            bioFile.getParentFile().mkdirs();
            bioFile.createNewFile();
            osBIO = getCompressedOS(new FileOutputStream(bioFile));
            return osBIO;
        }
    }

    @Override
    public NBTOutputStream getEntityCreateOS() throws IOException {
        if (osENTCT != null) {
            return osENTCT;
        }
        enttFile.getParentFile().mkdirs();
        enttFile.createNewFile();
        osENTCT = new NBTOutputStream(getCompressedOS(new FileOutputStream(enttFile)));
        return osENTCT;
    }

    @Override
    public NBTOutputStream getEntityRemoveOS() throws IOException {
        if (osENTCF != null) {
            return osENTCF;
        }
        entfFile.getParentFile().mkdirs();
        entfFile.createNewFile();
        osENTCF = new NBTOutputStream(getCompressedOS(new FileOutputStream(entfFile)));
        return osENTCF;
    }

    @Override
    public NBTOutputStream getTileCreateOS() throws IOException {
        if (osNBTT != null) {
            return osNBTT;
        }
        nbttFile.getParentFile().mkdirs();
        nbttFile.createNewFile();
        osNBTT = new NBTOutputStream(getCompressedOS(new FileOutputStream(nbttFile)));
        return osNBTT;
    }

    @Override
    public NBTOutputStream getTileRemoveOS() throws IOException {
        if (osNBTF != null) {
            return osNBTF;
        }
        nbtfFile.getParentFile().mkdirs();
        nbtfFile.createNewFile();
        osNBTF = new NBTOutputStream(getCompressedOS(new FileOutputStream(nbtfFile)));
        return osNBTF;
    }

    @Override
    public FaweInputStream getBlockIS() throws IOException {
        if (!bdFile.exists()) {
            return null;
        }
        FaweInputStream is = MainUtil.getCompressedIS(new FileInputStream(bdFile));
        readHeader(is);
        return is;
    }

    @Override
    public FaweInputStream getBiomeIS() throws IOException {
        if (!bioFile.exists()) {
            return null;
        }
        return MainUtil.getCompressedIS(new FileInputStream(bioFile));
    }

    @Override
    public NBTInputStream getEntityCreateIS() throws IOException {
        if (!enttFile.exists()) {
            return null;
        }
        return new NBTInputStream(MainUtil.getCompressedIS(new FileInputStream(enttFile)));
    }

    @Override
    public NBTInputStream getEntityRemoveIS() throws IOException {
        if (!entfFile.exists()) {
            return null;
        }
        return new NBTInputStream(MainUtil.getCompressedIS(new FileInputStream(entfFile)));
    }

    @Override
    public NBTInputStream getTileCreateIS() throws IOException {
        if (!nbttFile.exists()) {
            return null;
        }
        return new NBTInputStream(MainUtil.getCompressedIS(new FileInputStream(nbttFile)));
    }

    @Override
    public NBTInputStream getTileRemoveIS() throws IOException {
        if (!nbtfFile.exists()) {
            return null;
        }
        return new NBTInputStream(MainUtil.getCompressedIS(new FileInputStream(nbtfFile)));
    }

    @Override
    public SimpleChangeSetSummary summarize(Region region, boolean shallow) {
        if (bdFile.exists()) {
            return super.summarize(region, shallow);
        }
        return null;
    }

    public IntegerPair readHeader() {
        int ox = getOriginX();
        int oz = getOriginZ();
        if (ox == 0 && oz == 0 && bdFile.exists()) {
            try (FileInputStream fis = new FileInputStream(bdFile)) {
                final FaweInputStream gis = MainUtil.getCompressedIS(fis);
                // skip mode
                gis.skipFully(1);
                // origin
                ox = ((gis.read() << 24) + (gis.read() << 16) + (gis.read() << 8) + gis.read());
                oz = ((gis.read() << 24) + (gis.read() << 16) + (gis.read() << 8) + gis.read());
                setOrigin(ox, oz);
                fis.close();
                gis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new IntegerPair(ox, oz);
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
