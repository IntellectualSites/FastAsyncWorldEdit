package com.boydti.fawe.jnbt.anvil;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.Trimable;
import com.boydti.fawe.beta.implementation.IChunkExtent;
import com.boydti.fawe.beta.implementation.processors.ExtentBatchProcessorHolder;
import com.boydti.fawe.object.RunnableVal4;
import com.boydti.fawe.object.io.BufferedRandomAccessFile;
import com.boydti.fawe.object.io.FastByteArrayInputStream;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MathMan;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.io.FastByteArrayOutputStream;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Chunk format: http://minecraft.gamepedia.com/Chunk_format#Entity_format
 * e.g.: `.Level.Entities.#` (Starts with a . as the root tag is unnamed)
 * Note: This class isn't thread safe. You can use it in an async thread, but not multiple at the same time
 */
public class MCAFile extends ExtentBatchProcessorHolder implements Trimable, IChunkExtent {

    private static Field fieldBuf2;

    static {
        try {
            fieldBuf2 = InflaterInputStream.class.getDeclaredField("buf");
            fieldBuf2.setAccessible(true);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private final ForkJoinPool pool;
    private final byte[] locations;
    private boolean readLocations;

    private File file;
    private RandomAccessFile raf;

    private boolean deleted;
    private int X, Z;
    private MCAChunk[] chunks;
    private boolean[] chunkInitialized;
    private Object[] locks;
    private Deflater deflater = new Deflater(1, false);

    public MCAFile(ForkJoinPool pool) {
        this.pool = pool;
        this.locations = new byte[4096];
        this.chunks = new MCAChunk[32 * 32];
        this.chunkInitialized = new boolean[this.chunks.length];
        this.locks = new Object[this.chunks.length];
        for (int i = 0; i < locks.length; i++) {
            locks[i] = new Object();
        }
    }

    @Override
    public boolean trim(boolean aggressive) {
        boolean hasChunk = false;
        for (int i = 0; i < chunkInitialized.length; i++) {
            if (!chunkInitialized[i]) {
                chunks[i] = null;
            } else {
                hasChunk = true;
            }
        }
        return !hasChunk;
    }

    public MCAFile init(File file) throws FileNotFoundException {
        String[] split = file.getName().split("\\.");
        int X = Integer.parseInt(split[1]);
        int Z = Integer.parseInt(split[2]);
        return init(file, X, Z);
    }

    public MCAFile init(File file, int mcrX, int mcrZ) throws FileNotFoundException {
        if (raf != null) {
            flush(true);
            for (int i = 0; i < 4096; i++) {
                locations[i] = 0;
            }
            try {
                raf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            raf = null;
        }
        deleted = false;
        Arrays.fill(chunkInitialized, false);
        readLocations = false;
        this.X = mcrX;
        this.Z = mcrZ;
        this.file = file;
        if (!file.exists()) {
            throw new FileNotFoundException(file.getName());
        }
        return this;
    }

    public MCAFile init(World world, int mcrX, int mcrZ) throws FileNotFoundException {
        return init(new File(world.getStoragePath().toFile(), File.separator + "regions" + File.separator + "r." + mcrX + "." + mcrZ + ".mca"));
    }

    @Override
    public BlockVector3 getMinimumPoint() {
        return BlockVector3.at(this.X << 9, 0, this.Z << 9);
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return BlockVector3.at((this.X << 9) + 511, FaweCache.IMP.WORLD_MAX_Y, (this.Z << 9) + 511);
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tile) throws WorldEditException {
//        final IChunk chunk = getChunk(x >> 4, z >> 4);
//        return chunk.setTile(x & 15, y, z & 15, tile);
        return false;
    }

    public int getIndex(int chunkX, int chunkZ) {
        return ((chunkX & 31) << 2) + ((chunkZ & 31) << 7);
    }


    private RandomAccessFile getRaf() throws FileNotFoundException {
        if (this.raf == null) {
            this.raf = new RandomAccessFile(file, "rw");
        }
        return this.raf;
    }

    private void readHeader() throws IOException {
        if (!readLocations) {
            readLocations = true;
            getRaf();
            if (raf.length() < 8192) {
                raf.setLength(8192);
            } else {
                raf.seek(0);
                raf.readFully(locations);
            }
        }
    }

    public void clear() {
        if (raf != null) {
            try {
                raf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        deleted = false;
        readLocations = false;
        Arrays.fill(chunkInitialized, false);
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isDeleted() {
        return deleted;
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
        int pair = getIndex(cx, cz);
        MCAChunk chunk = chunks[pair];
        if (chunk != null && chunkInitialized[pair]) {
            return chunk;
        }
        return null;
    }

    public void setChunk(MCAChunk chunk) {
        int cx = chunk.getX();
        int cz = chunk.getZ();
        int pair = getIndex(cx, cz);
        chunks[pair] = chunk;
    }

    @Override
    public MCAChunk getOrCreateChunk(int chunkX, int chunkZ) {
        try {
            return getChunk(chunkX, chunkZ);
        } catch (IOException e) {
            // TODO generate?
            return null;
        }
    }

    public MCAChunk getChunk(int cx, int cz) throws IOException {
        int pair = getIndex(cx, cz);
        MCAChunk chunk = chunks[pair];
        if (chunk == null) {
            Object lock = locks[pair];
            synchronized (lock) {
                chunk = chunks[pair];
                if (chunk == null) {
                    chunk = new MCAChunk();
                    chunk.setPosition(cx, cz);
                    chunks[pair] = chunk;
                }
            }
        } else if (chunkInitialized[pair]) {
            return chunk;
        }
        synchronized (chunk) {
            if (!chunkInitialized[pair]) {
                readChunk(chunk, pair);
                chunkInitialized[pair] = true;
            }
        }
        return chunk;
    }

    private MCAChunk readChunk(MCAChunk chunk, int i) throws IOException {
        int offset = (((locations[i] & 0xFF) << 16) + ((locations[i + 1] & 0xFF) << 8) + ((locations[i + 2] & 0xFF))) << 12;
        if (offset == 0) {
            return null;
        }
        int size = (locations[i + 3] & 0xFF) << 12;
        try (NBTInputStream nis = getChunkIS(offset)) {
            chunk.read(nis, false);
        }
        System.out.println("TODO multithreaded"); // TODO
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

    public void forEachChunk(Consumer<MCAChunk> onEach) {
        int i = 0;
        for (int z = 0; z < 32; z++) {
            for (int x = 0; x < 32; x++, i += 4) {
                int offset = (((locations[i] & 0xFF) << 16) + ((locations[i + 1] & 0xFF) << 8) + ((locations[i + 2] & 0xFF)));
                int size = locations[i + 3] & 0xFF;
                if (size != 0) {
                    try {
                        onEach.accept(getChunk(x, z));
                    } catch (Throwable ignore) {
                    }
                }
            }
        }
    }

    public int getOffset(int cx, int cz) {
        int i = getIndex(cx, cz);
        int offset = (((locations[i] & 0xFF) << 16) + ((locations[i + 1] & 0xFF) << 8) + ((locations[i + 2] & 0xFF)));
        return offset << 12;
    }

    public int getSize(int cx, int cz) {
        int i = getIndex(cx, cz);
        return (locations[i + 3] & 0xFF) << 12;
    }

    public FastByteArrayInputStream getChunkCompressedBytes(int offset) throws IOException {
        if (offset == 0) {
            return null;
        }
        synchronized (raf) {
            raf.seek(offset);
            int size = raf.readInt();
            int compression = raf.read();
            byte[] data = FaweCache.IMP.BYTE_BUFFER_VAR.get(size);
            raf.readFully(data, 0, size);
            FastByteArrayInputStream result = new FastByteArrayInputStream(data, 0, size);
            return result;
        }
    }

    private NBTInputStream getChunkIS(int offset) throws IOException {
        try {
            return getChunkIS(getChunkCompressedBytes(offset));
        } catch (IllegalAccessException unlikely) {
            unlikely.printStackTrace();
            return null;
        }
    }

    private NBTInputStream getChunkIS(InputStream is) throws IllegalAccessException {
        InflaterInputStream iis = new InflaterInputStream(is, new Inflater(), 1);
        fieldBuf2.set(iis, FaweCache.IMP.BYTE_BUFFER_8192.get());
        BufferedInputStream bis = new BufferedInputStream(iis);
        NBTInputStream nis = new NBTInputStream(bis);
        return nis;
    }

    /**
     * @param onEach chunk
     */
    public void forEachCachedChunk(Consumer<MCAChunk> onEach) {
        for (int i = 0; i < chunks.length; i++) {
            MCAChunk chunk = chunks[i];
            if (chunk != null && this.chunkInitialized[i]) {
                onEach.accept(chunk);
            }
        }
    }

    public List<MCAChunk> getCachedChunks() {
        int size = 0;
        for (int i = 0; i < chunks.length; i++) {
            if (chunks[i] != null && this.chunkInitialized[i]) size++;
        }
        ArrayList<MCAChunk> list = new ArrayList<>(size);
        for (int i = 0; i < chunks.length; i++) {
            MCAChunk chunk = chunks[i];
            if (chunk != null && this.chunkInitialized[i]) {
                list.add(chunk);
            }
        }
        return list;
    }

    private FastByteArrayOutputStream toBytes(MCAChunk chunk) throws IOException {
        if (chunk.isDeleted()) {
            return null;
        }
        byte[] writeBuffer = FaweCache.IMP.BYTE_BUFFER_VAR.get(4096);
        FastByteArrayOutputStream uncompressed = chunk.toBytes(writeBuffer);
        if (uncompressed.array.length > writeBuffer.length) {
            FaweCache.IMP.BYTE_BUFFER_VAR.set(uncompressed.array);
        }
        writeBuffer = uncompressed.array;
        byte[] buffer = FaweCache.IMP.BYTE_BUFFER_8192.get();
        int length = uncompressed.length;
        uncompressed.reset();
        // cheat, reusing the same buffer to read/write
        int compressedLength = MainUtil.compress(uncompressed.array, length, buffer, uncompressed, deflater);
        return uncompressed;
    }

    private void writeSafe(RandomAccessFile raf, int offset, byte[] data, int length) throws IOException {
        int len = length + 5;
        raf.seek(offset);
        if (raf.length() - offset < len) {
            raf.setLength(((offset + len + 4095) / 4096) * 4096);
        }
        // Length of remaining data
        raf.writeInt(length + 1);
        // Compression type
        raf.write(2);
        raf.write(data, 0, length);
    }

    private void writeHeader(RandomAccessFile raf, int cx, int cz, int offsetMedium, int sizeByte, boolean writeTime) throws IOException {
        int i = getIndex(cx, cz);
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

    public void close() {
        if (raf == null) return;
        synchronized (raf) {
            if (raf != null) {
                flush(true);
                try {
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                raf = null;
            }
        }
    }

    public boolean isModified() {
        if (isDeleted()) {
            return true;
        }
        for (int i = 0; i < chunks.length; i++) {
            MCAChunk chunk = chunks[i];
            if (chunk != null && this.chunkInitialized[i]) {
                if (chunk.isModified() || chunk.isDeleted()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Write the chunk to the file
     * @param wait - If the flush method needs to wait for the pool
     */
    public void flush(boolean wait) {
        synchronized (raf) {
            // If the file is marked as deleted, nothing is written
            if (isDeleted()) {
                clear();
                file.delete();
                return;
            }
            // Chunks that need to be relocated
            Int2ObjectOpenHashMap<byte[]> relocate = new Int2ObjectOpenHashMap<>();
            // The position of each chunk
            final Int2ObjectOpenHashMap<Integer> offsetMap = new Int2ObjectOpenHashMap<>(); // Offset -> <byte cx, byte cz, short size>
            // The data of each modified chunk
            final Int2ObjectOpenHashMap<Future<byte[]>> compressedMap = new Int2ObjectOpenHashMap<>();
            // The data of each chunk that needs to be moved
            final Int2ObjectOpenHashMap<Future<byte[]>> append = new Int2ObjectOpenHashMap<>();
            // Get the current time for the chunk timestamp
            long now = System.currentTimeMillis();

            // Load the chunks into the append or compressed map
            final ForkJoinPool finalPool = this.pool;


            boolean modified = false;
            for (int i = 0; i < chunks.length; i++) {
                if (this.chunkInitialized[i]) {
                    MCAChunk chunk = chunks[i];
                    if (chunk != null && chunk.isModified() && !chunk.isDeleted()) {
                        modified = true;
                        ForkJoinTask<byte[]> future = pool.submit(() -> {
                            FastByteArrayOutputStream compressed = toBytes(chunk);
                            return Arrays.copyOf(compressed.array, compressed.length);
                        });
                    }
                }
            }
//
//            forEachCachedChunk(chunk -> {
//                if (chunk.isModified() || chunk.isDeleted()) {
//                    modified[0] = true;
//                    chunk.setLastUpdate(now);
//                    if (!chunk.isDeleted()) {
//                        MCAFile.this.pool.submit(() -> {
//                            try {
//                                byte[] compressed = toBytes(chunk);
//                                int pair = MathMan.pair((short) (chunk.getX() & 31), (short) (chunk.getZ() & 31));
//                                Int2ObjectOpenHashMap map;
//                                if (getOffset(chunk.getX(), chunk.getZ()) == 0) {
//                                    map = append;
//                                } else {
//                                    map = compressedMap;
//                                }
//                                synchronized (map) {
//                                    map.put(pair, compressed);
//                                }
//                            } catch (Throwable e) {
//                                e.printStackTrace();
//                            }
//                        });
//                    }
//                }
//            });

            if (!modified) {
                // Not modified, do nothing
                return;
            }
            // If any changes were detected
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
                    int pair = getIndex(cx, cz);

                    Future<byte[]> future = null;
                    byte[] newBytes = relocate.get(pair);
                    int newBytesLength = 0;

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
                                future = compressedMap.get(pair);
                            }
                        } else {
                            // The chunk needs to be moved, fetch the data if necessary
                            future = compressedMap.get(pair);
                            if (future == null) {
                                if (cached == null || !cached.isDeleted()) {
                                    FastByteArrayInputStream result = getChunkCompressedBytes(getOffset(cx, cz));
                                    newBytes = result.array;
                                    newBytesLength = result.length;
                                }
                            }
                        }
                    } else {
                        newBytesLength = newBytes.length;
                    }
                    if (future != null) {
                        newBytes = future.get();
                        newBytesLength = newBytes.length;
                    }

                    if (newBytes == null) {
                        writeHeader(raf, cx, cz, 0, 0, false);
                        continue;
                    }

                    // The length to be written (compressed data + 5 byte chunk header)
                    int len = newBytesLength + 5;
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
                                FastByteArrayInputStream tmp = getChunkCompressedBytes(nextOffset2);
                                byte[] nextBytes = Arrays.copyOf(tmp.array, tmp.length);
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
                    writeSafe(raf, start, newBytes, newBytesLength);
                    // Write the location data (beginning of file)
                    writeHeader(raf, cx, cz, start >> 12, newSize, true);

                    written = start + newBytesLength + 5;
                    start += newSize << 12;
                }

                // Write all the chunks which need to be appended
                if (!append.isEmpty()) {
                    for (Int2ObjectMap.Entry<Future<byte[]>> entry : append.int2ObjectEntrySet()) {
                        int pair = entry.getIntKey();
                        short cx = MathMan.unpairX(pair);
                        short cz = MathMan.unpairY(pair);
                        byte[] bytes = entry.getValue().get();
                        int len = bytes.length + 5;
                        int newSize = (len + 4095) >> 12;
                        writeSafe(raf, start, bytes, bytes.length);
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
                pool.awaitQuiescence(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
            }
        }
    }
}