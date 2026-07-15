package com.fastasyncworldedit.core.history;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.database.DBHandler;
import com.fastasyncworldedit.core.database.RollbackDatabase;
import com.fastasyncworldedit.core.history.changeset.FaweStreamChangeSet;
import com.fastasyncworldedit.core.history.changeset.SimpleChangeSetSummary;
import com.fastasyncworldedit.core.internal.io.FaweInputStream;
import com.fastasyncworldedit.core.internal.io.FaweOutputStream;
import com.fastasyncworldedit.core.math.IntPair;
import com.fastasyncworldedit.core.util.MainUtil;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.function.operation.ChangeSetExecutor;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Store the change on disk
 * - High disk usage
 * - Moderate CPU usage
 * - Minimal memory usage
 * - Slow
 */
public class DiskStorageHistory extends FaweStreamChangeSet {

    private static final Logger LOGGER = LogManagerCompat.getLogger();
    private static final Map<String, Map<UUID, Integer>> NEXT_INDEX = new ConcurrentHashMap<>();

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
    private volatile FaweOutputStream osBD;
    // biome
    private volatile FaweOutputStream osBIO;
    // NBT From
    private volatile NBTOutputStream osNBTF;
    // NBT To
    private volatile NBTOutputStream osNBTT;
    // Entity Create From
    private volatile NBTOutputStream osENTCF;
    // Entity Create To
    private volatile NBTOutputStream osENTCT;

    private int index;

    public DiskStorageHistory(World world, UUID uuid) {
        super(world);
        init(uuid, world.getName());
    }

    private void init(UUID uuid, String worldName) {
        final File folder = MainUtil.getFile(
                Fawe.platform().getDirectory(),
                Settings.settings().PATHS.HISTORY + File.separator + worldName + File.separator + uuid
        );

        final int max = NEXT_INDEX.computeIfAbsent(worldName, _worldName -> new ConcurrentHashMap<>())
                .compute(uuid, (_uuid, id) -> (id == null ? MainUtil.getMaxFileId(folder) : id) + 1) - 1;

        init(uuid, max);
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

    private void initFiles(File folder) {
        nbtfFile = new File(folder, index + ".nbtf");
        nbttFile = new File(folder, index + ".nbtt");
        entfFile = new File(folder, index + ".entf");
        enttFile = new File(folder, index + ".entt");
        //Switch file ending due to new (sort-of) format. (Added e for Extended height)
        bdFile = new File(folder, index + ".bd");
        bioFile = new File(folder, index + ".bio");
    }

    private void init(UUID uuid, int i) {
        this.uuid = uuid;
        this.index = i;
        File folder = MainUtil.getFile(
                Fawe.platform().getDirectory(),
                Settings.settings().PATHS.HISTORY + File.separator + getWorld().getName() + File.separator + uuid
        );
        initFiles(folder);
    }

    @Override
    public void delete() {
        deleteFiles();
        if (Settings.settings().HISTORY.USE_DATABASE) {
            RollbackDatabase db = DBHandler.dbHandler().getDatabase(getWorld());
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

    public void undo(Actor actor, Region[] regions) {
        try {
            close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        try (EditSession session = toEditSession(actor, regions)) {
            session.setBlocks(this, ChangeSetExecutor.Type.UNDO);
        }
    }

    public void undo(Actor actor) {
        undo(actor, null);
    }

    public void redo(Actor actor, Region[] regions) {
        try {
            close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        EditSession session = toEditSession(actor, regions);
        session.setBlocks(this, ChangeSetExecutor.Type.REDO);
    }

    public void redo(Actor actor) {
        redo(actor, null);
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
                if (osBD != null) {
                    osBD.flush();
                }
                if (osBIO != null) {
                    osBIO.flush();
                }
                if (osNBTF != null) {
                    osNBTF.flush();
                }
                if (osNBTT != null) {
                    osNBTT.flush();
                }
                if (osENTCF != null) {
                    osENTCF.flush();
                }
                if (osENTCT != null) {
                    osENTCT.flush();
                }
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

    /**
     * Closes {@code closeable} (if not null), suppressing rather than propagating or masking any
     * failure from the close itself onto {@code primary} - the original failure that triggered
     * the cleanup, and the one that must actually reach the caller. Used when a fallible resource
     * has to be closed before rethrowing, without losing the real cause if the close itself
     * fails too. No-op if {@code closeable} is null (e.g. construction failed before it was
     * created).
     */
    private static void closeQuietly(AutoCloseable closeable, Throwable primary) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Throwable suppressed) {
            // Throwable, not just Exception: a RuntimeException or Error from close() itself
            // must not propagate in place of primary either, or it would mask the original
            // failure exactly like the checked-exception case this method exists to prevent.
            // This still doesn't swallow anything - it's attached to primary, which callers
            // always rethrow.
            primary.addSuppressed(suppressed);
        }
    }

    @Override
    public FaweOutputStream getBlockOS(int x, int y, int z) throws IOException {
        if (osBD != null) {
            return osBD;
        }
        synchronized (this) {
            if (osBD != null) {
                return osBD;
            }
            bdFile.getParentFile().mkdirs();
            bdFile.createNewFile();
            // Write the header before publishing to the volatile field: osBD is not thread-safe
            // (see getCompressedOS()'s javadoc), so another thread's unsynchronized fast-path
            // read (`if (osBD != null) return osBD;`) must never observe the stream until it is
            // fully initialized, or it could start writing block data concurrently with the
            // header write here and corrupt the file.
            //
            // The raw FileOutputStream is kept in a local so it can be closed directly if
            // getCompressedOS() itself throws before returning anything to close; once wrapped,
            // closing the returned stream closes the FileOutputStream it wraps. Both catch
            // clauses below also cover unchecked RuntimeException/Error, not just IOException:
            // MainUtil.getCompressedOS()/writeHeader() can fail that way too (e.g. a linkage
            // error constructing a compression stream), and an unchecked failure after fos is
            // opened would otherwise leak the file descriptor since it's never published to osBD.
            FileOutputStream fos = new FileOutputStream(bdFile);
            FaweOutputStream stream;
            try {
                stream = getCompressedOS(fos);
            } catch (IOException | RuntimeException | Error e) {
                closeQuietly(fos, e);
                throw e;
            }
            try {
                writeHeader(stream, x, y, z);
            } catch (IOException | RuntimeException | Error e) {
                // Not yet published to osBD, so close() would never close this otherwise.
                closeQuietly(stream, e);
                throw e;
            }
            osBD = stream;
            return osBD;
        }
    }

    @Override
    public FaweOutputStream getBiomeOS() throws IOException {
        if (osBIO != null) {
            return osBIO;
        }
        synchronized (this) {
            if (osBIO != null) {
                return osBIO;
            }
            bioFile.getParentFile().mkdirs();
            bioFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(bioFile);
            try {
                osBIO = getCompressedOS(fos);
            } catch (IOException | RuntimeException | Error e) {
                closeQuietly(fos, e);
                throw e;
            }
            return osBIO;
        }
    }

    @Override
    public NBTOutputStream getEntityCreateOS() throws IOException {
        if (osENTCT != null) {
            return osENTCT;
        }
        synchronized (this) {
            if (osENTCT != null) {
                return osENTCT;
            }
            enttFile.getParentFile().mkdirs();
            enttFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(enttFile);
            try {
                osENTCT = new NBTOutputStream(getCompressedOS(fos));
            } catch (IOException | RuntimeException | Error e) {
                closeQuietly(fos, e);
                throw e;
            }
            return osENTCT;
        }
    }

    @Override
    public NBTOutputStream getEntityRemoveOS() throws IOException {
        if (osENTCF != null) {
            return osENTCF;
        }
        synchronized (this) {
            if (osENTCF != null) {
                return osENTCF;
            }
            entfFile.getParentFile().mkdirs();
            entfFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(entfFile);
            try {
                osENTCF = new NBTOutputStream(getCompressedOS(fos));
            } catch (IOException | RuntimeException | Error e) {
                closeQuietly(fos, e);
                throw e;
            }
            return osENTCF;
        }
    }

    @Override
    public NBTOutputStream getTileCreateOS() throws IOException {
        if (osNBTT != null) {
            return osNBTT;
        }
        synchronized (this) {
            if (osNBTT != null) {
                return osNBTT;
            }
            nbttFile.getParentFile().mkdirs();
            nbttFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(nbttFile);
            try {
                osNBTT = new NBTOutputStream(getCompressedOS(fos));
            } catch (IOException | RuntimeException | Error e) {
                closeQuietly(fos, e);
                throw e;
            }
            return osNBTT;
        }
    }

    @Override
    public NBTOutputStream getTileRemoveOS() throws IOException {
        if (osNBTF != null) {
            return osNBTF;
        }
        synchronized (this) {
            if (osNBTF != null) {
                return osNBTF;
            }
            nbtfFile.getParentFile().mkdirs();
            nbtfFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(nbtfFile);
            try {
                osNBTF = new NBTOutputStream(getCompressedOS(fos));
            } catch (IOException | RuntimeException | Error e) {
                closeQuietly(fos, e);
                throw e;
            }
            return osNBTF;
        }
    }

    @Override
    public FaweInputStream getBlockIS() throws IOException {
        if (!bdFile.exists()) {
            return null;
        }
        try {
            FaweInputStream is = MainUtil.getCompressedIS(new FileInputStream(bdFile));
            readHeader(is);
            return is;
        } catch (IOException e) {
            LOGGER.error("Could not load block history file {}", bdFile);
            throw e;
        }
    }

    @Override
    public FaweInputStream getBiomeIS() throws IOException {
        if (!bioFile.exists()) {
            return null;
        }
        try {
            return MainUtil.getCompressedIS(new FileInputStream(bioFile));
        } catch (IOException e) {
            LOGGER.error("Could not load biome history file {}", bdFile);
            throw e;
        }
    }

    @Override
    public NBTInputStream getEntityCreateIS() throws IOException {
        if (!enttFile.exists()) {
            return null;
        }
        try {
            return new NBTInputStream(MainUtil.getCompressedIS(new FileInputStream(enttFile)));
        } catch (IOException e) {
            LOGGER.error("Could not load entity create history file {}", bdFile);
            throw e;
        }
    }

    @Override
    public NBTInputStream getEntityRemoveIS() throws IOException {
        if (!entfFile.exists()) {
            return null;
        }
        try {
            return new NBTInputStream(MainUtil.getCompressedIS(new FileInputStream(entfFile)));
        } catch (IOException e) {
            LOGGER.error("Could not load entity remove history file {}", bdFile);
            throw e;
        }
    }

    @Override
    public NBTInputStream getTileCreateIS() throws IOException {
        if (!nbttFile.exists()) {
            return null;
        }
        try {
            return new NBTInputStream(MainUtil.getCompressedIS(new FileInputStream(nbttFile)));
        } catch (IOException e) {
            LOGGER.error("Could not load tile create history file {}", bdFile);
            throw e;
        }
    }

    @Override
    public NBTInputStream getTileRemoveIS() throws IOException {
        if (!nbtfFile.exists()) {
            return null;
        }
        try {
            return new NBTInputStream(MainUtil.getCompressedIS(new FileInputStream(nbtfFile)));
        } catch (IOException e) {
            LOGGER.error("Could not load tile remove history file {}", bdFile);
            throw e;
        }
    }

    @Override
    public SimpleChangeSetSummary summarize(Region region, boolean shallow) {
        if (bdFile.exists()) {
            return super.summarize(region, shallow);
        }
        return null;
    }

    public IntPair readHeader() {
        int ox = getOriginX();
        int oz = getOriginZ();
        if (ox == 0 && oz == 0 && bdFile.exists()) {
            try (FileInputStream fis = new FileInputStream(bdFile)) {
                final FaweInputStream gis = MainUtil.getCompressedIS(fis);
                // skip mode
                gis.skipFully(1);
                // skip version
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
        return new IntPair(ox, oz);
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
