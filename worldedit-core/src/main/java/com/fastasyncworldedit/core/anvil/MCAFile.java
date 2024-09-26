package com.fastasyncworldedit.core.anvil;

import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.internal.exception.FaweException;
import com.fastasyncworldedit.core.math.FastBitSet;
import com.fastasyncworldedit.core.util.task.RunnableVal4;
import com.plotsquared.core.util.task.RunnableVal;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.io.FastByteArrayInputStream;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

@SuppressWarnings({"removal"})
public class MCAFile implements Closeable, Flushable {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private static final int CHUNK_HEADER_SIZE = 5;
    private static final byte VERSION_GZIP = 1;
    private static final byte VERSION_DEFLATE = 2;
    private static final byte VERSION_UNCOMPRESSED = 3;
    private static final int SECTOR_BYTES = 4096;
    private static final int SECTOR_INTS = SECTOR_BYTES / 4;

    private final Int2IntOpenHashMap offsetMap;
    private final Path file;
    private RandomAccessFile raf;
    private int[] offsets;
    private boolean deleted;
    private final int X, Z;
    private final Int2ObjectOpenHashMap<MCAChunk> chunks = new Int2ObjectOpenHashMap<>();
    private FastBitSet sectorFree;
    private boolean closed = false;
    private volatile boolean init = false;

    private MCAFile(int mcrX, int mcrZ, Path file) {
        this.file = file;
        X = mcrX;
        Z = mcrZ;
        offsetMap = new Int2IntOpenHashMap();
        offsetMap.defaultReturnValue(Integer.MAX_VALUE);
    }

    /**
     * Create a new mca file
     *
     * @param mcrX      region x
     * @param mcrZ      region z
     * @param directory directory to create mca file in
     * @return new mca file
     * @throws IOException on io error
     */
    public static MCAFile create(int mcrX, int mcrZ, Path directory) throws IOException {
        Files.createDirectories(directory);
        Path file = directory.resolve("r." + mcrX + "." + mcrZ + ".mca");
        Files.createFile(file);
        MCAFile mca = new MCAFile(mcrX, mcrZ, file);
        mca.initEmpty();
        return mca;
    }

    /**
     * Load an mca file
     *
     * @param mcrX      region x
     * @param mcrZ      region z
     * @param directory directory to load mca file from
     * @return loaded mca file
     * @throws IOException on io error
     */
    @Nullable
    public static MCAFile load(int mcrX, int mcrZ, Path directory) throws IOException {
        Path file = directory.resolve("r." + mcrX + "." + mcrZ + ".mca");
        if (!Files.exists(file)) {
            return null;
        }
        MCAFile mca = new MCAFile(mcrX, mcrZ, file);
        mca.init();
        return mca;
    }

    /**
     * Load or create an mca file
     *
     * @param mcrX      region x
     * @param mcrZ      region z
     * @param directory directory to load or create mca file in
     * @return mca file
     * @throws IOException on io error
     */
    public static MCAFile loadOrCreate(int mcrX, int mcrZ, Path directory) throws IOException {
        Path file = directory.resolve("r." + mcrX + "." + mcrZ + ".mca");
        MCAFile mca = new MCAFile(mcrX, mcrZ, file);
        if (!Files.exists(file)) {
            Files.createFile(file);
            mca.initEmpty();
        } else {
            mca.init();
        }
        return mca;
    }

    public void clear() {
        if (raf != null) {
            try {
                raf.close();
            } catch (IOException e) {
                LOGGER.error("Error clearing MCAFile", e);
            }
        }
        synchronized (chunks) {
            chunks.clear();
        }
        offsetMap.clear();
        offsets = null;
    }

    /**
     * Set if the file should be delete
     */
    public void setDeleted(boolean deleted) {
        checkInit();
        this.deleted = deleted;
    }

    /**
     * Get if the file has been set to be deleted
     */
    public boolean isDeleted() {
        return deleted;
    }

    protected void checkInit() {
        if (!init) {
            try {
                init();
            } catch (IOException e) {
                throw new FaweException("Error initialising MCA file", FaweException.Type.ANVIL_IO, false, e);
            }
        }
    }

    /**
     * Initialises the RandomAccessFile and loads the location header from disk if not done yet
     */
    private synchronized void init() throws IOException {
        if (raf == null) {
            this.offsets = new int[SECTOR_INTS];
            if (file != null) {
                this.raf = new RandomAccessFile(file.toFile(), "rw");
                final int nSectors = (int) Math.round(Math.ceil((double) raf.length() / SECTOR_BYTES));
                sectorFree = new FastBitSet(nSectors);
                sectorFree.setAll();
                sectorFree.set(0, false);
                sectorFree.set(1, false);
                if (raf.length() < 8192) {
                    raf.setLength(8192);
                } else {
                    if ((raf.length() & 0xFFF) != 0) {
                        raf.setLength(((raf.length() + 0xFFF) >> 12) << 12);
                    }
                    raf.seek(0);
                    for (int i = 0; i < SECTOR_INTS; i++) {
                        final int offset = raf.readInt();
                        offsets[i] = offset;
                        int sectorStart = offset >> 8;
                        int numSectors = offset & 0xFF;
                        if (offset != 0 && sectorStart + numSectors <= sectorFree.size()) {
                            offsetMap.put(offset, i);
                            for (int sectorNum = 0; sectorNum < (offset & 0xFF); sectorNum++) {
                                sectorFree.set((offset >> 8) + sectorNum, false);
                            }
                        }
                    }
                }
            }
            init = true;
            closed = false;
        }
    }

    private synchronized void initEmpty() throws IOException {
        this.offsets = new int[SECTOR_INTS];
        this.raf = new RandomAccessFile(file.toFile(), "rw");

        // Initialises with all zeroes
        this.raf.setLength(8192L);
        this.raf.seek(0);
        this.raf.write(FaweCache.INSTANCE.BYTE_BUFFER_8192.get());
        sectorFree = new FastBitSet(2);
        init = true;
        closed = false;
    }

    /**
     * Get the region file X
     */
    public int getX() {
        return X;
    }

    /**
     * Get the region file Z
     */
    public int getZ() {
        return Z;
    }

    /**
     * Get the RandomAccessFile of the MCA region file
     */
    public RandomAccessFile getRandomAccessFile() {
        return raf;
    }

    /**
     * Get the MCA region file
     */
    public Path getFile() {
        return file;
    }

    /**
     * Gets a cached {@link MCAChunk} if present else returns null
     */
    @Nullable
    public MCAChunk getCachedChunk(int cx, int cz) {
        checkInit();
        short pair = (short) ((cx & 31) + ((cz & 31) << 5));
        synchronized (chunks) {
            return chunks.get(pair);
        }
    }

    /**
     * Create a new empty {@link MCAChunk}.
     */
    public MCAChunk newChunk(int cx, int cz) {
        checkInit();
        short pair = (short) ((cx & 31) + ((cz & 31) << 5));
        MCAChunk chunk;
        synchronized (chunks) {
            chunks.put(pair, chunk = new MCAChunk(this, cx, cx));
        }
        return chunk;
    }

    /**
     * Insert a {@link MCAChunk} into the cache.
     */
    public void setChunk(MCAChunk chunk) {
        checkInit();
        int cx = chunk.getX();
        int cz = chunk.getZ();
        short pair = (short) ((cx & 31) + ((cz & 31) << 5));
        synchronized (chunks) {
            chunks.put(pair, chunk);
        }
    }

    /**
     * Load data from the mca region into the given {@link MCAChunk}.
     */
    public void loadIntoChunkFromFile(MCAChunk chunk) throws IOException {
        checkInit();
        int cx = chunk.getX();
        int cz = chunk.getZ();
        int i = (cx & 31) + ((cz & 31) << 5);
        int offset = offsets[i];
        synchronized (this) {
            if (offset == 0) {
                chunk.setEmpty(true);
                return;
            }
            chunk.loadFromNIS(new NBTInputStream(getChunkBIS(offset >> 8)), false);
        }
    }

    @Nonnull
    public MCAChunk getChunk(int cx, int cz) {
        checkInit();
        MCAChunk cached = getCachedChunk(cx, cz);
        if (cached != null) {
            return cached;
        } else {
            return readChunk(cx, cz);
        }
    }

    public MCAChunk readChunk(int cx, int cz) {
        checkInit();
        int i = (cx & 31) + ((cz & 31) << 5);
        int offset = offsets[i];
        if (offset == 0) {
            return newChunk(cx, cz);
        }
        try {
            MCAChunk chunk;
            synchronized (this) {
                NBTInputStream nis = new NBTInputStream(getChunkBIS(offset >> 8));
                chunk = new MCAChunk(this, nis, cx, cz, false);
            }
            short pair = (short) ((cx & 31) + ((cz & 31) << 5));
            synchronized (chunks) {
                chunks.put(pair, chunk);
            }
            return chunk;
        } catch (Exception e) {
            throw new RuntimeException("Error attempting to read chunk locally located at `" + (cx & 31) + "," + (cz & 31) + "`" +
                    " in file `" + file.getFileName() + "` at offset: `" + (offset >> 8) + "`", e);
        }
    }

    /**
     * @param onEach cx, cz, sector, sectorsAllocated (in kB)
     */
    public void forEachChunk(RunnableVal4<Integer, Integer, Integer, Integer> onEach) {
        checkInit();
        int i = 0;
        for (int z = 0; z < 32; z++) {
            for (int x = 0; x < 32; x++, i += 4) {
                int offset = offsets[x + (z << 5)];
                if (offset != 0) {
                    int size = offset & 0xFF;
                    onEach.run(x, z, offset >> 8, size);
                }
            }
        }
    }

    public void forEachChunk(RunnableVal<MCAChunk> onEach) {
        checkInit();
        int rx = X << 5;
        int rz = Z << 5;
        for (int z = 0; z < 32; z++) {
            for (int x = 0; x < 32; x++) {
                int offset = offsets[x + (z << 5)];
                if (offset != 0) {
                    try {
                        onEach.run(getChunk(rx + x, rz + z));
                    } catch (Throwable ignore) {
                    }
                }
            }
        }
    }

    @Nullable
    protected BufferedInputStream getChunkIS(int cx, int cz) throws IOException {
        checkInit();
        int offset = offsets[(cx & 31) + ((cz & 31) << 5)];
        return getChunkBIS(offset);
    }

    @Nullable
    private BufferedInputStream getChunkBIS(long offset) throws IOException {
        if (offset == 0) {
            return null;
        }
        byte version;
        byte[] data;
        synchronized (this) {
            raf.seek(offset << 12);
            int length = raf.readInt();
            version = raf.readByte();
            data = new byte[length - 1];
            raf.read(data);
        }
        FastByteArrayInputStream bais = new FastByteArrayInputStream(data);
        return switch (version) {
            case VERSION_GZIP -> new BufferedInputStream(new GZIPInputStream(bais));
            case VERSION_DEFLATE -> new BufferedInputStream(new InflaterInputStream(bais));
            case VERSION_UNCOMPRESSED -> new BufferedInputStream(bais);
            default -> throw new IllegalStateException("Unexpected compression version: " + version);
        };
    }

    public byte[] getFullChunkBytes(int cx, int cz) throws IOException {
        checkInit();
        int offset = offsets[(cx & 31) + ((cz & 31) << 5)];
        int sectorNumber = offset >> 8;
        int sectorsAllocated = offset & 0xFF;
        byte[] data = new byte[sectorsAllocated << 12];
        raf.seek((long) sectorNumber << 12);
        raf.read(data);
        return data;
    }

    /**
     * Writes chunk bytes directly to the MCAFile and returns the next sector after the chunk
     *
     * @param cx           chunk x
     * @param cz           chunk z
     * @param sectorNumber sector to write chunk from
     * @param bytes        byte to write
     * @return next sector after the chunk
     * @throws IOException on IO error
     */
    public synchronized int writeFullChunkBytes(int cx, int cz, int sectorNumber, byte[] bytes) throws IOException {
        checkInit();
        int allocate = (bytes.length >> 12) + 1;
        int offset = (sectorNumber << 8) | (allocate & 0xFF);
        setOffset(cx, cz, offset);
        raf.seek((long) sectorNumber << 12);
        raf.write(bytes);
        int newSector = sectorNumber + allocate;
        sectorFree.expandTo(newSector, false);
        return newSector;
    }

    public synchronized void replaceChunkBytes(int cx, int cz, byte[] bytes) throws IOException {
        checkInit();
        int currOffset = offsets[(cx & 31) + ((cz & 31) << 5)];
        int allocate = (bytes.length >> 12) + 1;
        int sectorNumber = sectorFree.size() + 1;
        if (currOffset != 0) {
            int currSector = currOffset >> 8;
            int currAllocate = currOffset & 0xFF;
            sectorFree.setRange(currSector, currSector + currAllocate);
            if (allocate <= currAllocate) {
                sectorNumber = currSector;
                sectorFree.clearRange(currSector, currSector + allocate);
            }
            System.out.println(sectorNumber);
        }
        int offset = (sectorNumber << 8) | (allocate & 0xFF);
        setOffset(cx, cz, offset);
        raf.seek((long) sectorNumber << 12);
        raf.write(bytes);
        int newSector = sectorNumber + allocate;
        sectorFree.expandTo(newSector, false);
    }

    public List<MCAChunk> getCachedChunks() {
        synchronized (chunks) {
            return new ArrayList<>(chunks.values());
        }
    }

    public void uncache(int cx, int cz) {
        int pair = (cx & 31) + ((cz & 31) << 5);
        synchronized (chunks) {
            chunks.remove(pair);
        }
    }

    public synchronized void close(boolean flush) throws IOException {
        if (raf == null || closed) {
            return;
        }
        if (flush) {
            flush();
        }
        try {
            raf.close();
        } catch (IOException e) {
            LOGGER.error("Error closing MCAFile", e);
        }
        raf = null;
        offsets = null;
        offsetMap.clear();
        closed = true;
        init = false;
    }

    @Override
    public synchronized void close() throws IOException {
        close(true);
    }

    public boolean isModified() {
        if (isDeleted()) {
            return true;
        }
        synchronized (chunks) {
            for (Int2ObjectMap.Entry<MCAChunk> entry : chunks.int2ObjectEntrySet()) {
                MCAChunk chunk = entry.getValue();
                if (chunk.isModified()) {
                    return true;
                }
            }
        }
        return false;
    }

    public int getOffset(int cx, int cz) {
        return offsets[cx + (cz << 5)];
    }

    public synchronized void setOffset(int cx, int cz, final int offset)
            throws IOException {
        int i = (cx & 31) + ((cz & 31) << 5);
        if (offset == 0) {
            offsetMap.remove(offsets[i]);
        } else {
            offsetMap.put(offset, i);
        }
        offsets[cx + (cz << 5)] = offset;
        raf.seek((long) i << 2);
        raf.writeInt(offset);
    }

    /**
     * Write the chunk to the file
     */
    @Override
    public synchronized void flush() throws IOException {
        System.out.println(sectorFree);
        boolean delete = true;
        int currentSector = 2;
        Queue<Integer> offsets =
                new LinkedBlockingDeque<>(Arrays.stream(this.offsets).boxed().sorted(Comparator.comparingInt(i -> (i >> 8))).toList());
        int offset;
        int count = 0;
        while (offsets.peek() != null) {
            count++;
            offset = offsets.poll();
            if (offset == 0) {
                continue;
            }
            delete = false;
            int pair = offsetMap.get(offset);
            int sectorNumber = offset >> 8;
            int sectorsAllocated = offset & 0xFF;
            if (sectorNumber < 2) {
                throw new IllegalStateException("Sector number cannot be < 2!");
            }
            if (pair == Integer.MAX_VALUE) {
                sectorFree.setRange(sectorNumber, sectorNumber + sectorsAllocated);
                continue;
            }
            MCAChunk chunk = chunks.remove(pair);

            byte[] data;
            int sectorsNeeded;
            boolean writeChunkHeader;

            if (currentSector <= sectorNumber) { // Only set free if we definitely won't be overwriting
                sectorFree.setRange(sectorNumber, sectorNumber + sectorsAllocated);
            } else {
                throw new IllegalStateException("Current sector number being written to cannot exceed sector number of chunk to" +
                        " be written!");
            }
            if (chunk == null) {
                if (currentSector != sectorNumber) {
                    writeChunkHeader = false;
                    data = new byte[sectorsAllocated << 12];
                    sectorsNeeded = sectorsAllocated;
                    raf.seek((long) sectorNumber << 12);
                    raf.read(data);
                } else {
                    sectorFree.clearRange(currentSector, currentSector + sectorsAllocated);
                    currentSector += sectorsAllocated;
                    continue;
                }
            } else if (chunk.isDeleted()) {
                int x = pair & 31;
                int z = (pair >> 5) & 31;
                setOffset(x, z, 0);
                continue;
            } else {
                data = chunk.toBytes(null);
                writeChunkHeader = true;
                sectorsNeeded = ((data.length + CHUNK_HEADER_SIZE) >> 12) + 1;
            }

            boolean hasSpace = sectorsNeeded <= sectorsAllocated;
            long position = (long) currentSector << 12;
            if (!hasSpace) {
                hasSpace = true;
                for (int i = currentSector; i < currentSector + sectorsNeeded; i++) {
                    if (!sectorFree.get(i) || i > sectorFree.size()) {
                        hasSpace = false;
                        break;
                    }
                }
                if (!hasSpace) {
                    if (sectorNumber > 2500 || count > 30000) {
                        throw new RuntimeException();
                    }
                    sectorNumber = sectorFree.size() + 1;
                    offset = (sectorNumber << 8) | (sectorsNeeded & 0xFF);
                    setOffset(pair & 31, (pair >> 5) & 31, offset);
                    position = (long) sectorNumber << 12;
                    raf.setLength((long) (sectorNumber + sectorsNeeded + 1) << 12);
                    sectorFree.expandTo(sectorNumber + sectorsNeeded, true);
                    sectorFree.clearRange(sectorNumber, sectorNumber + sectorsNeeded);
                    offsets.add(offset); // Come back later to clean up.
                }
            }
            raf.seek(position);
            if (writeChunkHeader) {
                raf.writeInt(data.length + 1);
                raf.writeByte(VERSION_DEFLATE);
            }
            raf.write(data);
            if (hasSpace) {
                offset = (currentSector << 8) | (sectorsNeeded & 0xFF);
                setOffset(pair & 31, (pair >> 5) & 31, offset);
                sectorFree.clearRange(currentSector, currentSector + sectorsNeeded);
                currentSector += sectorsNeeded;
            }
        }
        int size = 0;
        for (int i = sectorFree.size(); i > 0; i--) {
            if (!sectorFree.get(i)) {
                size = i + 1; // + 1 to fully encompass
                break;
            }
        }
        System.out.println(sectorFree);
        raf.setLength((long) size << 12);
        if (delete || size < 3) {
            clear();
            Files.delete(file);
        }
        synchronized (chunks) {
            chunks.clear();
        }
    }

}
