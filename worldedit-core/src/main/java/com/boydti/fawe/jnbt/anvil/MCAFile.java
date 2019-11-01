package com.boydti.fawe.jnbt.anvil;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.jnbt.streamer.StreamDelegate;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.RunnableVal4;
import com.boydti.fawe.object.collection.CleanableThreadLocal;
import com.boydti.fawe.object.io.BufferedRandomAccessFile;
import com.boydti.fawe.object.io.FastByteArrayInputStream;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MathMan;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.worldedit.world.World;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Chunk format: http://minecraft.gamepedia.com/Chunk_format#Entity_format
 * e.g.: `.Level.Entities.#` (Starts with a . as the root tag is unnamed)
 */
public class MCAFile {

    private static Field fieldBuf2;
    private static Field fieldBuf3;

    static {
        try {
            fieldBuf2 = InflaterInputStream.class.getDeclaredField("buf");
            fieldBuf2.setAccessible(true);
            fieldBuf3 = NBTInputStream.class.getDeclaredField("buf");
            fieldBuf3.setAccessible(true);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private final World world;
    private final File file;
    private RandomAccessFile raf;
    private byte[] locations;
    private boolean deleted;
    private final int X, Z;
    private final Int2ObjectOpenHashMap<MCAChunk> chunks = new Int2ObjectOpenHashMap<>();

    final ThreadLocal<byte[]> byteStore1 = new ThreadLocal<byte[]>() {
        @Override
        protected byte[] initialValue() {
            return new byte[4096];
        }
    };
    final ThreadLocal<byte[]> byteStore2 = new ThreadLocal<byte[]>() {
        @Override
        protected byte[] initialValue() {
            return new byte[4096];
        }
    };
    final ThreadLocal<byte[]> byteStore3 = new ThreadLocal<byte[]>() {
        @Override
        protected byte[] initialValue() {
            return new byte[1024];
        }
    };

    public MCAFile(World world, File file) throws FileNotFoundException {
        this.world = world;
        this.file = file;
        if (!file.exists()) {
            throw new FileNotFoundException(file.getName());
        }
        String[] split = file.getName().split("\\.");
        X = Integer.parseInt(split[1]);
        Z = Integer.parseInt(split[2]);
    }

    public MCAFile(World world, int mcrX, int mcrZ) {
        this(world, mcrX, mcrZ, new File(world.getStoragePath().toFile(), "r." + mcrX + "." + mcrZ + ".mca"));
    }

    public MCAFile(World world, int mcrX, int mcrZ, File file) {
        this.world = world;
        this.file = file;
        X = mcrX;
        Z = mcrZ;
    }

    public void clear() {
        if (raf != null) {
            try {
                raf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        synchronized (chunks) {
            chunks.clear();
        }
        locations = null;
        CleanableThreadLocal.clean(byteStore1);
        CleanableThreadLocal.clean(byteStore2);
        CleanableThreadLocal.clean(byteStore3);
    }

    @Override
    protected void finalize() throws Throwable {
        CleanableThreadLocal.clean(byteStore1);
        CleanableThreadLocal.clean(byteStore2);
        CleanableThreadLocal.clean(byteStore3);
        super.finalize();
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public World getWorld() {
        return world;
    }

    /**
     * Loads the location header from disk
     */
    public void init() {
        try {
            if (raf == null) {
                this.locations = new byte[4096];
                if (file != null) {
                    this.raf = new RandomAccessFile(file, "rw");
                    if (raf.length() < 8192) {
                        raf.setLength(8192);
                    } else {
                        raf.seek(0);
                        raf.readFully(locations);
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public int getX() {
        return X;
    }

    public int getZ() {
        return Z;
    }

    public RandomAccessFile getRandomAccessFile() {
        return raf;
    }

    public File getFile() {
        return file;
    }

    public MCAChunk getCachedChunk(int cx, int cz) {
        int pair = MathMan.pair((short) (cx & 31), (short) (cz & 31));
        synchronized (chunks) {
            return chunks.get(pair);
        }
    }

    public void setChunk(MCAChunk chunk) {
        int cx = chunk.getX();
        int cz = chunk.getZ();
        int pair = MathMan.pair((short) (cx & 31), (short) (cz & 31));
        synchronized (chunks) {
            chunks.put(pair, chunk);
        }
    }

    public MCAChunk getChunk(int cx, int cz) throws IOException {
        MCAChunk cached = getCachedChunk(cx, cz);
        if (cached != null) {
            return cached;
        } else {
            return readChunk(cx, cz);
        }
    }

    public MCAChunk readChunk(int cx, int cz) throws IOException {
        int i = ((cx & 31) << 2) + ((cz & 31) << 7);
        int offset = (((locations[i] & 0xFF) << 16) + ((locations[i + 1] & 0xFF) << 8) + ((locations[i + 2] & 0xFF))) << 12;
        int size = (locations[i + 3] & 0xFF) << 12;
        if (offset == 0) {
            return null;
        }
        NBTInputStream nis = getChunkIS(offset);
        MCAChunk chunk = new MCAChunk(nis, cx, cz, false);
        nis.close();
        int pair = MathMan.pair((short) (cx & 31), (short) (cz & 31));
        synchronized (chunks) {
            chunks.put(pair, chunk);
        }
        return chunk;
    }

    /**
     * CX, CZ, OFFSET, SIZE
     *
     * @param onEach
     * @throws IOException
     */
    public void forEachSortedChunk(RunnableVal4<Integer, Integer, Integer, Integer> onEach) throws IOException {
        char[] offsets = new char[(int) (raf.length() / 4096) - 2];
        Arrays.fill(offsets, Character.MAX_VALUE);
        char i = 0;
        for (int z = 0; z < 32; z++) {
            for (int x = 0; x < 32; x++, i += 4) {
                int offset = (((locations[i] & 0xFF) << 16) + ((locations[i + 1] & 0xFF) << 8) + ((locations[i + 2] & 0xFF))) - 2;
                int size = locations[i + 3] & 0xFF;
                if (size != 0) {
                    if (offset < offsets.length) {
                        offsets[offset] = i;
                    } else {
                        Fawe.debug("Ignoring invalid offset " + offset);
                    }
                }
            }
        }
        for (i = 0; i < offsets.length; i++) {
            int index = offsets[i];
            if (index != Character.MAX_VALUE) {
                int offset = i + 2;
                int size = locations[index + 3] & 0xFF;
                int index2 = index >> 2;
                int x = (index2) & 31;
                int z = (index2) >> 5;
                onEach.run(x, z, offset << 12, size << 12);
            }
        }
    }

    /**
     * @param onEach cx, cz, offset, size
     */
    public void forEachChunk(RunnableVal4<Integer, Integer, Integer, Integer> onEach) {
        int i = 0;
        for (int z = 0; z < 32; z++) {
            for (int x = 0; x < 32; x++, i += 4) {
                int offset = (((locations[i] & 0xFF) << 16) + ((locations[i + 1] & 0xFF) << 8) + ((locations[i + 2] & 0xFF)));
                int size = locations[i + 3] & 0xFF;
                if (size != 0) {
                    onEach.run(x, z, offset << 12, size << 12);
                }
            }
        }
    }

    public void forEachChunk(RunnableVal<MCAChunk> onEach) {
        int i = 0;
        for (int z = 0; z < 32; z++) {
            for (int x = 0; x < 32; x++, i += 4) {
                int offset = (((locations[i] & 0xFF) << 16) + ((locations[i + 1] & 0xFF) << 8) + ((locations[i + 2] & 0xFF)));
                int size = locations[i + 3] & 0xFF;
                if (size != 0) {
                    try {
                        onEach.run(getChunk(x, z));
                    } catch (Throwable ignore) {
                    }
                }
            }
        }
    }

    public int getOffset(int cx, int cz) {
        int i = ((cx & 31) << 2) + ((cz & 31) << 7);
        int offset = (((locations[i] & 0xFF) << 16) + ((locations[i + 1] & 0xFF) << 8) + ((locations[i + 2] & 0xFF)));
        return offset << 12;
    }

    public int getSize(int cx, int cz) {
        int i = ((cx & 31) << 2) + ((cz & 31) << 7);
        return (locations[i + 3] & 0xFF) << 12;
    }

    public List<Integer> getChunks() {
        final List<Integer> values;
        synchronized (chunks) {
            values = new ArrayList<>(chunks.size());
        }
        for (int i = 0; i < locations.length; i += 4) {
            int offset = (((locations[i] & 0xFF) << 16) + ((locations[i + 1] & 0xFF) << 8) + ((locations[i + 2] & 0xFF)));
            values.add(offset);
        }
        return values;
    }

    public byte[] getChunkCompressedBytes(int offset) throws IOException {
        if (offset == 0) {
            return null;
        }
        synchronized (raf) {
            raf.seek(offset);
            int size = raf.readInt();
            int compression = raf.read();
            byte[] data = new byte[size];
            raf.readFully(data);
            return data;
        }
    }

    private NBTInputStream getChunkIS(int offset) throws IOException {
        try {
            byte[] data = getChunkCompressedBytes(offset);
            FastByteArrayInputStream bais = new FastByteArrayInputStream(data);
            InflaterInputStream iis = new InflaterInputStream(bais, new Inflater(), 1);
            fieldBuf2.set(iis, byteStore2.get());
            BufferedInputStream bis = new BufferedInputStream(iis);
            NBTInputStream nis = new NBTInputStream(bis);
            fieldBuf3.set(nis, byteStore3.get());
            return nis;
        } catch (IllegalAccessException unlikely) {
            unlikely.printStackTrace();
            return null;
        }
    }

    public void streamChunk(int cx, int cz, StreamDelegate delegate) throws IOException {
        streamChunk(getOffset(cx, cz), delegate);
    }

    public void streamChunk(int offset, StreamDelegate delegate) throws IOException {
        byte[] data = getChunkCompressedBytes(offset);
        streamChunk(data, delegate);
    }

    public void streamChunk(byte[] data, StreamDelegate delegate) throws IOException {
        if (data != null) {
            try {
                FastByteArrayInputStream bais = new FastByteArrayInputStream(data);
                InflaterInputStream iis = new InflaterInputStream(bais, new Inflater(), 1);
                fieldBuf2.set(iis, byteStore2.get());
                BufferedInputStream bis = new BufferedInputStream(iis);
                NBTInputStream nis = new NBTInputStream(bis);
                fieldBuf3.set(nis, byteStore3.get());
                nis.readNamedTagLazy(delegate);
            } catch (IllegalAccessException unlikely) {
                unlikely.printStackTrace();
            }
        }
    }

    /**
     * @param onEach chunk
     */
    public void forEachCachedChunk(RunnableVal<MCAChunk> onEach) {
        synchronized (chunks) {
            for (Map.Entry<Integer, MCAChunk> entry : chunks.entrySet()) {
                onEach.run(entry.getValue());
            }
        }
    }

    public List<MCAChunk> getCachedChunks() {
        synchronized (chunks) {
            return new ArrayList<>(chunks.values());
        }
    }

    public void uncache(int cx, int cz) {
        int pair = MathMan.pair((short) (cx & 31), (short) (cz & 31));
        synchronized (chunks) {
            chunks.remove(pair);
        }
    }

    private byte[] toBytes(MCAChunk chunk) throws Exception {
        if (chunk.isDeleted()) {
            return null;
        }
        byte[] uncompressed = chunk.toBytes(byteStore3.get());
        byte[] compressed = MainUtil.compress(uncompressed, byteStore2.get(), null);
        return compressed;
    }

    private byte[] getChunkBytes(int cx, int cz) throws Exception {
        MCAChunk mca = getCachedChunk(cx, cz);
        if (mca == null) {
            int offset = getOffset(cx, cz);
            if (offset == 0) {
                return null;
            }
            return getChunkCompressedBytes(offset);
        }
        return toBytes(mca);
    }


    private void writeSafe(RandomAccessFile raf, int offset, byte[] data) throws IOException {
        int len = data.length + 5;
        raf.seek(offset);
        if (raf.length() - offset < len) {
            raf.setLength(((offset + len + 4095) / 4096) * 4096);
        }
        // Length of remaining data
        raf.writeInt(data.length + 1);
        // Compression type
        raf.write(2);
        raf.write(data);
    }

    private void writeHeader(RandomAccessFile raf, int cx, int cz, int offsetMedium, int sizeByte, boolean writeTime) throws IOException {
        int i = ((cx & 31) << 2) + ((cz & 31) << 7);
        locations[i] = (byte) (offsetMedium >> 16);
        locations[i + 1] = (byte) (offsetMedium >> 8);
        locations[i + 2] = (byte) (offsetMedium);
        locations[i + 3] = (byte) sizeByte;
        raf.seek(i);
        raf.write((offsetMedium >> 16));
        raf.write((offsetMedium >> 8));
        raf.write((offsetMedium >> 0));
        raf.write(sizeByte);
        raf.seek(i + 4096);
        if (offsetMedium == 0 && sizeByte == 0) {
            raf.writeInt(0);
        } else {
            raf.writeInt((int) (System.currentTimeMillis() / 1000L));
        }
    }

    public void close(ForkJoinPool pool) {
        if (raf == null) return;
        synchronized (raf) {
            if (raf != null) {
                flush(pool);
                try {
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                raf = null;
                locations = null;
            }
        }
    }

    public boolean isModified() {
        if (isDeleted()) {
            return true;
        }
        synchronized (chunks) {
            for (Int2ObjectMap.Entry<MCAChunk> entry : chunks.int2ObjectEntrySet()) {
                MCAChunk chunk = entry.getValue();
                if (chunk.isModified() || chunk.isDeleted()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Write the chunk to the file
     * @param pool
     */
    public void flush(ForkJoinPool pool) {
        synchronized (raf) {
            // If the file is marked as deleted, nothing is written
            if (isDeleted()) {
                clear();
                file.delete();
                return;
            }

            boolean wait; // If the flush method needs to wait for the pool
            if (pool == null) {
                wait = true;
                pool = new ForkJoinPool();
            } else wait = false;

            // Chunks that need to be relocated
            Int2ObjectOpenHashMap<byte[]> relocate = new Int2ObjectOpenHashMap<>();
            // The position of each chunk
            final Int2ObjectOpenHashMap<Integer> offsetMap = new Int2ObjectOpenHashMap<>(); // Offset -> <byte cx, byte cz, short size>
            // The data of each modified chunk
            final Int2ObjectOpenHashMap<byte[]> compressedMap = new Int2ObjectOpenHashMap<>();
            // The data of each chunk that needs to be moved
            final Int2ObjectOpenHashMap<byte[]> append = new Int2ObjectOpenHashMap<>();
            boolean modified = false;
            // Get the current time for the chunk timestamp
            long now = System.currentTimeMillis();

            // Load the chunks into the append or compressed map
            for (MCAChunk chunk : getCachedChunks()) {
                if (chunk.isModified() || chunk.isDeleted()) {
                    modified = true;
                    chunk.setLastUpdate(now);
                    if (!chunk.isDeleted()) {
                        pool.submit(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    byte[] compressed = toBytes(chunk);
                                    int pair = MathMan.pair((short) (chunk.getX() & 31), (short) (chunk.getZ() & 31));
                                    Int2ObjectOpenHashMap map;
                                    if (getOffset(chunk.getX(), chunk.getZ()) == 0) {
                                        map = append;
                                    } else {
                                        map = compressedMap;
                                    }
                                    synchronized (map) {
                                        map.put(pair, compressed);
                                    }
                                } catch (Throwable e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                }
            }

            // If any changes were detected
            if (modified) {
                file.setLastModified(now);

                // Load the offset data into the offset map
                forEachChunk(new RunnableVal4<Integer, Integer, Integer, Integer>() {
                    @Override
                    public void run(Integer cx, Integer cz, Integer offset, Integer size) {
                        short pair1 = MathMan.pairByte((byte) (cx & 31), (byte) (cz & 31));
                        short pair2 = (short) (size >> 12);
                        offsetMap.put((int) offset, (Integer) MathMan.pair(pair1, pair2));
                    }
                });
                // Wait for previous tasks
                pool.awaitQuiescence(Long.MAX_VALUE, TimeUnit.MILLISECONDS);


                int start = 8192;
                int written = start;
                int end = 8192;
                int nextOffset = 8192;
                try {
                    for (int count = 0; count < offsetMap.size(); count++) {
                        // Get the previous position of the next chunk
                        Integer loc = offsetMap.get(nextOffset);
                        while (loc == null) {
                            nextOffset += 4096;
                            loc = offsetMap.get(nextOffset);
                        }
                        int offset = nextOffset;

                        // Get the x/z from the paired location
                        short cxz = MathMan.unpairX(loc);
                        int cx = MathMan.unpairShortX(cxz);
                        int cz = MathMan.unpairShortY(cxz);

                        // Get the size from the pair
                        int size = MathMan.unpairY(loc) << 12;

                        nextOffset += size;
                        end = Math.min(start + size, end);
                        int pair = MathMan.pair((short) (cx & 31), (short) (cz & 31));
                        byte[] newBytes = relocate.get(pair);

                        // newBytes is null if the chunk isn't modified or marked for moving
                        if (newBytes == null) {
                            MCAChunk cached = getCachedChunk(cx, cz);
                            // If the previous offset marks the current write position (start) then we only write the header
                            if (offset == start) {
                                if (cached == null || !cached.isModified()) {
                                    writeHeader(raf, cx, cz, start >> 12, size >> 12, true);
                                    start += size;
                                    written = start + size;
                                    continue;
                                } else {
                                    newBytes = compressedMap.get(pair);
                                }
                            } else {
                                // The chunk needs to be moved, fetch the data if necessary
                                newBytes = compressedMap.get(pair);
                                if (newBytes == null) {
                                    if (cached == null || !cached.isDeleted()) {
                                        newBytes = getChunkCompressedBytes(getOffset(cx, cz));
                                    }
                                }
                            }
                        }

                        if (newBytes == null) {
                            writeHeader(raf, cx, cz, 0, 0, false);
                            continue;
                        }

                        // The length to be written (compressed data + 5 byte chunk header)
                        int len = newBytes.length + 5;
                        int oldSize = (size + 4095) >> 12;
                        int newSize = (len + 4095) >> 12;
                        int nextOffset2 = end;

                        // If the current write position (start) + length of data to write (len) are longer than the position of the next chunk, we need to move the next chunks
                        while (start + len > end) {
                            Integer nextLoc = offsetMap.get(nextOffset2);
                            if (nextLoc != null) {
                                short nextCXZ = MathMan.unpairX(nextLoc);
                                int nextCX = MathMan.unpairShortX(nextCXZ);
                                int nextCZ = MathMan.unpairShortY(nextCXZ);
                                MCAChunk cached = getCachedChunk(nextCX, nextCZ);
                                if (cached == null || !cached.isModified()) {
                                    byte[] nextBytes = getChunkCompressedBytes(nextOffset2);
                                    relocate.put(MathMan.pair((short) (nextCX & 31), (short) (nextCZ & 31)), nextBytes);
                                }
                                int nextSize = MathMan.unpairY(nextLoc) << 12;
                                end += nextSize;
                                nextOffset2 += nextSize;
                            } else {
                                end += 4096;
                                nextOffset2 += 4096;
                            }
                        }
                        // Write the chunk + chunk header
                        writeSafe(raf, start, newBytes);
                        // Write the location data (beginning of file)
                        writeHeader(raf, cx, cz, start >> 12, newSize, true);

                        written = start + newBytes.length + 5;
                        start += newSize << 12;
                    }

                    // Write all the chunks which need to be appended
                    if (!append.isEmpty()) {
                        for (Int2ObjectMap.Entry<byte[]> entry : append.int2ObjectEntrySet()) {
                            int pair = entry.getIntKey();
                            short cx = MathMan.unpairX(pair);
                            short cz = MathMan.unpairY(pair);
                            byte[] bytes = entry.getValue();
                            int len = bytes.length + 5;
                            int newSize = (len + 4095) >> 12;
                            writeSafe(raf, start, bytes);
                            writeHeader(raf, cx, cz, start >> 12, newSize, true);
                            written = start + bytes.length + 5;
                            start += newSize << 12;
                        }
                    }
                    // Round the file length, since the vanilla server doesn't like it for some reason
                    raf.setLength(4096 * ((written + 4095) / 4096));
                    if (raf instanceof BufferedRandomAccessFile) {
                        ((BufferedRandomAccessFile) raf).flush();
                    }
                    raf.close();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                if (wait) {
                    pool.shutdown();
                    pool.awaitQuiescence(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                }
            }
        }
        CleanableThreadLocal.clean(byteStore1);
        CleanableThreadLocal.clean(byteStore2);
        CleanableThreadLocal.clean(byteStore3);
    }
}