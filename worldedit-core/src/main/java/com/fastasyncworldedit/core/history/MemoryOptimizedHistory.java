package com.fastasyncworldedit.core.history;

import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.history.changeset.FaweStreamChangeSet;
import com.fastasyncworldedit.core.internal.io.FastByteArrayOutputStream;
import com.fastasyncworldedit.core.internal.io.FastByteArraysInputStream;
import com.fastasyncworldedit.core.internal.io.FaweInputStream;
import com.fastasyncworldedit.core.internal.io.FaweOutputStream;
import com.fastasyncworldedit.core.util.MainUtil;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.worldedit.world.World;

import java.io.IOException;

/**
 * ChangeSet optimized for low memory usage
 * - No disk usage
 * - High CPU usage
 * - Low memory usage
 */
public class MemoryOptimizedHistory extends FaweStreamChangeSet {

    private byte[][] ids;
    private FastByteArrayOutputStream idsStream;
    private volatile FaweOutputStream idsStreamZip;

    private byte[][] biomes;
    private FastByteArrayOutputStream biomeStream;
    private volatile FaweOutputStream biomeStreamZip;

    private byte[][] entC;
    private FastByteArrayOutputStream entCStream;
    private volatile NBTOutputStream entCStreamZip;

    private byte[][] entR;
    private FastByteArrayOutputStream entRStream;
    private volatile NBTOutputStream entRStreamZip;

    private byte[][] tileC;
    private FastByteArrayOutputStream tileCStream;
    private volatile NBTOutputStream tileCStreamZip;

    private byte[][] tileR;
    private FastByteArrayOutputStream tileRStream;
    private volatile NBTOutputStream tileRStreamZip;

    public MemoryOptimizedHistory(World world) {
        super(world);
    }

    @Override
    public void flush() {
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
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
        synchronized (this) {
            try {
                if (idsStream != null) {
                    idsStreamZip.close();
                    ids = idsStream.toByteArrays();
                    idsStream = null;
                    idsStreamZip = null;
                }
                if (biomeStream != null) {
                    biomeStreamZip.close();
                    biomes = biomeStream.toByteArrays();
                    biomeStream = null;
                    biomeStreamZip = null;
                }
                if (entCStream != null) {
                    entCStreamZip.close();
                    entC = entCStream.toByteArrays();
                    entCStream = null;
                    entCStreamZip = null;
                }
                if (entRStream != null) {
                    entRStreamZip.close();
                    entR = entRStream.toByteArrays();
                    entRStream = null;
                    entRStreamZip = null;
                }
                if (tileCStream != null) {
                    tileCStreamZip.close();
                    tileC = tileCStream.toByteArrays();
                    tileCStream = null;
                    tileCStreamZip = null;
                }
                if (tileRStream != null) {
                    tileRStreamZip.close();
                    tileR = tileRStream.toByteArrays();
                    tileRStream = null;
                    tileRStreamZip = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int getCompressedSize() {
        if (ids == null) {
            return 0;
        }
        int count = 0;
        for (byte[] array : ids) {
            count += 4 + array.length;
        }
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
            if (idsStreamZip != null) {
                return idsStreamZip;
            }
            setOrigin(x, z);
            // Build the buffer and its wrapper into locals and only publish both fields together
            // once construction has fully succeeded. Two things this guards against:
            //  1. If getCompressedOS()/writeHeader() throws, idsStream must not be left non-null
            //     while idsStreamZip stays null - flush()/close() gate on idsStream != null and
            //     then dereference idsStreamZip, which would NPE.
            //  2. Callers on the fast path read the volatile idsStreamZip without holding the
            //     lock, so publishing it before writeHeader has run would let them write block
            //     data ahead of the header.
            FastByteArrayOutputStream buffer = new FastByteArrayOutputStream(Settings.settings().HISTORY.BUFFER_SIZE);
            FaweOutputStream stream = getCompressedOS(buffer);
            writeHeader(stream, x, y, z);
            idsStream = buffer;
            idsStreamZip = stream;
            return stream;
        }
    }

    @Override
    public FaweInputStream getBiomeIS() throws IOException {
        if (biomes == null) {
            return null;
        }
        return MainUtil.getCompressedIS(new FastByteArraysInputStream(biomes));
    }

    @Override
    public FaweOutputStream getBiomeOS() throws IOException {
        if (biomeStreamZip != null) {
            return biomeStreamZip;
        }
        synchronized (this) {
            if (biomeStreamZip != null) {
                return biomeStreamZip;
            }
            // See getBlockOS() for why buffer/wrapper must be published together: an exception
            // from getCompressedOS() must not leave biomeStream non-null with biomeStreamZip null.
            FastByteArrayOutputStream buffer = new FastByteArrayOutputStream(Settings.settings().HISTORY.BUFFER_SIZE);
            FaweOutputStream stream = getCompressedOS(buffer);
            biomeStream = buffer;
            biomeStreamZip = stream;
            return stream;
        }
    }

    @Override
    public FaweInputStream getBlockIS() throws IOException {
        if (ids == null) {
            return null;
        }
        FaweInputStream result = MainUtil.getCompressedIS(new FastByteArraysInputStream(ids));
        readHeader(result);
        return result;
    }

    @Override
    public NBTOutputStream getEntityCreateOS() throws IOException {
        if (entCStreamZip != null) {
            return entCStreamZip;
        }
        synchronized (this) {
            if (entCStreamZip != null) {
                return entCStreamZip;
            }
            // See getBlockOS() for why buffer/wrapper must be published together.
            FastByteArrayOutputStream buffer = new FastByteArrayOutputStream(Settings.settings().HISTORY.BUFFER_SIZE);
            NBTOutputStream stream = new NBTOutputStream(getCompressedOS(buffer));
            entCStream = buffer;
            entCStreamZip = stream;
            return stream;
        }
    }

    @Override
    public NBTOutputStream getEntityRemoveOS() throws IOException {
        if (entRStreamZip != null) {
            return entRStreamZip;
        }
        synchronized (this) {
            if (entRStreamZip != null) {
                return entRStreamZip;
            }
            // See getBlockOS() for why buffer/wrapper must be published together.
            FastByteArrayOutputStream buffer = new FastByteArrayOutputStream(Settings.settings().HISTORY.BUFFER_SIZE);
            NBTOutputStream stream = new NBTOutputStream(getCompressedOS(buffer));
            entRStream = buffer;
            entRStreamZip = stream;
            return stream;
        }
    }

    @Override
    public NBTOutputStream getTileCreateOS() throws IOException {
        if (tileCStreamZip != null) {
            return tileCStreamZip;
        }
        synchronized (this) {
            if (tileCStreamZip != null) {
                return tileCStreamZip;
            }
            // See getBlockOS() for why buffer/wrapper must be published together.
            FastByteArrayOutputStream buffer = new FastByteArrayOutputStream(Settings.settings().HISTORY.BUFFER_SIZE);
            NBTOutputStream stream = new NBTOutputStream(getCompressedOS(buffer));
            tileCStream = buffer;
            tileCStreamZip = stream;
            return stream;
        }
    }

    @Override
    public NBTOutputStream getTileRemoveOS() throws IOException {
        if (tileRStreamZip != null) {
            return tileRStreamZip;
        }
        synchronized (this) {
            if (tileRStreamZip != null) {
                return tileRStreamZip;
            }
            // See getBlockOS() for why buffer/wrapper must be published together.
            FastByteArrayOutputStream buffer = new FastByteArrayOutputStream(Settings.settings().HISTORY.BUFFER_SIZE);
            NBTOutputStream stream = new NBTOutputStream(getCompressedOS(buffer));
            tileRStream = buffer;
            tileRStreamZip = stream;
            return stream;
        }
    }

    @Override
    public NBTInputStream getEntityCreateIS() throws IOException {
        return entC == null ? null : new NBTInputStream(MainUtil.getCompressedIS(new FastByteArraysInputStream(entC)));
    }

    @Override
    public NBTInputStream getEntityRemoveIS() throws IOException {
        return entR == null ? null : new NBTInputStream(MainUtil.getCompressedIS(new FastByteArraysInputStream(entR)));
    }

    @Override
    public NBTInputStream getTileCreateIS() throws IOException {
        return tileC == null ? null : new NBTInputStream(MainUtil.getCompressedIS(new FastByteArraysInputStream(tileC)));
    }

    @Override
    public NBTInputStream getTileRemoveIS() throws IOException {
        return tileR == null ? null : new NBTInputStream(MainUtil.getCompressedIS(new FastByteArraysInputStream(tileR)));
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
