package com.boydti.fawe.object.changeset;

import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweInputStream;
import com.boydti.fawe.object.FaweOutputStream;
import com.boydti.fawe.object.io.FastByteArrayInputStream;
import com.boydti.fawe.object.io.FastByteArrayOutputStream;
import com.boydti.fawe.util.MainUtil;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.worldedit.world.World;
import java.io.IOException;

/**
 * An implementation of {@link com.sk89q.worldedit.history.changeset.ChangeSet} optimized for low
 * memory usage but is heavy on CPU usage. This changeset does not usage any disk storage.
 */
public class MemoryOptimizedHistory extends FaweStreamChangeSet {

    private byte[] ids;
    private FastByteArrayOutputStream idsStream;
    private FaweOutputStream idsStreamZip;

    private byte[] biomes;
    private FastByteArrayOutputStream biomeStream;
    private FaweOutputStream biomeStreamZip;

    private byte[] entC;
    private FastByteArrayOutputStream entCStream;
    private NBTOutputStream entCStreamZip;

    private byte[] entR;
    private FastByteArrayOutputStream entRStream;
    private NBTOutputStream entRStreamZip;

    private byte[] tileC;
    private FastByteArrayOutputStream tileCStream;
    private NBTOutputStream tileCStreamZip;

    private byte[] tileR;
    private FastByteArrayOutputStream tileRStream;
    private NBTOutputStream tileRStreamZip;

    public MemoryOptimizedHistory(World world) {
        super(world);
    }

    public MemoryOptimizedHistory(String world) {
        super(world);
    }

    @Override
    public boolean flush() {
        super.flush();
        synchronized (this) {
            try {
                if (idsStream != null) {
                    idsStreamZip.flush();
                }
                if (biomeStream != null) {
                    biomeStreamZip.flush();
                }
                if (entCStream != null) {
                    entCStreamZip.flush();
                }
                if (entRStream != null) {
                    entRStreamZip.flush();
                }
                if (tileCStream != null) {
                    tileCStreamZip.flush();
                }
                if (tileRStream != null) {
                    tileRStreamZip.flush();
                }
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public boolean close() {
        super.close();
        synchronized (this) {
            try {
                if (idsStream != null) {
                    idsStreamZip.close();
                    ids = idsStream.toByteArray();
                    idsStream = null;
                    idsStreamZip = null;
                }
                if (biomeStream != null) {
                    biomeStreamZip.close();
                    biomes = biomeStream.toByteArray();
                    biomeStream = null;
                    biomeStreamZip = null;
                }
                if (entCStream != null) {
                    entCStreamZip.close();
                    entC = entCStream.toByteArray();
                    entCStream = null;
                    entCStreamZip = null;
                }
                if (entRStream != null) {
                    entRStreamZip.close();
                    entR = entRStream.toByteArray();
                    entRStream = null;
                    entRStreamZip = null;
                }
                if (tileCStream != null) {
                    tileCStreamZip.close();
                    tileC = tileCStream.toByteArray();
                    tileCStream = null;
                    tileCStreamZip = null;
                }
                if (tileRStream != null) {
                    tileRStreamZip.close();
                    tileR = tileRStream.toByteArray();
                    tileRStream = null;
                    tileRStreamZip = null;
                }
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public int getCompressedSize() {
        if (ids == null) {
            return 0;
        }
        int count = 4 + ids.length;
        return count;
    }

    @Override
    public long getSizeInMemory() {
        return 92 + getCompressedSize();
    }

    @Override
    public FaweOutputStream getBlockOS(int x, int y, int z) throws IOException {
        if (idsStreamZip != null) {
            return idsStreamZip;
        }
        synchronized (this) {
            setOrigin(x, z);
            idsStream = new FastByteArrayOutputStream(Settings.IMP.HISTORY.BUFFER_SIZE);
            idsStreamZip = getCompressedOS(idsStream);
            writeHeader(idsStreamZip, x, y, z);
            return idsStreamZip;
        }
    }

    @Override
    public FaweInputStream getBiomeIS() throws IOException {
        if (biomes == null) {
            return null;
        }
        return MainUtil.getCompressedIS(new FastByteArrayInputStream(biomes));
    }

    @Override
    public FaweOutputStream getBiomeOS() throws IOException {
        if (biomeStreamZip != null) {
            return biomeStreamZip;
        }
        synchronized (this) {
            biomeStream = new FastByteArrayOutputStream(Settings.IMP.HISTORY.BUFFER_SIZE);
            biomeStreamZip = getCompressedOS(biomeStream);
            return biomeStreamZip;
        }
    }

    @Override
    public FaweInputStream getBlockIS() throws IOException {
        if (ids == null) {
            return null;
        }
        FaweInputStream result = MainUtil.getCompressedIS(new FastByteArrayInputStream(ids));
        readHeader(result);
        return result;
    }

    @Override
    public NBTOutputStream getEntityCreateOS() throws IOException {
        if (entCStreamZip != null) {
            return entCStreamZip;
        }
        entCStream = new FastByteArrayOutputStream(Settings.IMP.HISTORY.BUFFER_SIZE);
        return entCStreamZip = new NBTOutputStream(getCompressedOS(entCStream));
    }

    @Override
    public NBTOutputStream getEntityRemoveOS() throws IOException {
        if (entRStreamZip != null) {
            return entRStreamZip;
        }
        entRStream = new FastByteArrayOutputStream(Settings.IMP.HISTORY.BUFFER_SIZE);
        return entRStreamZip = new NBTOutputStream(getCompressedOS(entRStream));
    }

    @Override
    public NBTOutputStream getTileCreateOS() throws IOException {
        if (tileCStreamZip != null) {
            return tileCStreamZip;
        }
        tileCStream = new FastByteArrayOutputStream(Settings.IMP.HISTORY.BUFFER_SIZE);
        return tileCStreamZip = new NBTOutputStream(getCompressedOS(tileCStream));
    }

    @Override
    public NBTOutputStream getTileRemoveOS() throws IOException {
        if (tileRStreamZip != null) {
            return tileRStreamZip;
        }
        tileRStream = new FastByteArrayOutputStream(Settings.IMP.HISTORY.BUFFER_SIZE);
        return tileRStreamZip = new NBTOutputStream(getCompressedOS(tileRStream));
    }

    @Override
    public NBTInputStream getEntityCreateIS() throws IOException {
        return entC == null ? null
            : new NBTInputStream(MainUtil.getCompressedIS(new FastByteArrayInputStream(entC)));
    }

    @Override
    public NBTInputStream getEntityRemoveIS() throws IOException {
        return entR == null ? null
            : new NBTInputStream(MainUtil.getCompressedIS(new FastByteArrayInputStream(entR)));
    }

    @Override
    public NBTInputStream getTileCreateIS() throws IOException {
        return tileC == null ? null
            : new NBTInputStream(MainUtil.getCompressedIS(new FastByteArrayInputStream(tileC)));
    }

    @Override
    public NBTInputStream getTileRemoveIS() throws IOException {
        return tileR == null ? null
            : new NBTInputStream(MainUtil.getCompressedIS(new FastByteArrayInputStream(tileR)));
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
