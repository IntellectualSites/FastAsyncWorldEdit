package com.fastasyncworldedit.core.extent.clipboard;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.internal.exception.FaweClipboardVersionMismatchException;
import com.fastasyncworldedit.core.internal.io.ByteBufferInputStream;
import com.fastasyncworldedit.core.jnbt.streamer.IntValueReader;
import com.fastasyncworldedit.core.math.IntTriple;
import com.fastasyncworldedit.core.util.MainUtil;
import com.fastasyncworldedit.core.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.DoubleTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A clipboard with disk backed storage. (lower memory + loads on crash)
 * - Uses an auto closable RandomAccessFile for getting / setting id / data
 * - I don't know how to reduce nbt / entities to O(2) complexity, so it is stored in memory.
 */
public class DiskOptimizedClipboard extends LinearClipboard {

    public static final int VERSION = 2;
    private static final Logger LOGGER = LogManagerCompat.getLogger();
    private static final int HEADER_SIZE = 27; // Current header size
    private static final int VERSION_1_HEADER_SIZE = 22; // Header size of "version 1"
    private static final int VERSION_2_HEADER_SIZE = 27; // Header size of "version 2" i.e. when NBT/entities could be saved
    private static final Map<String, LockHolder> LOCK_HOLDER_CACHE = new ConcurrentHashMap<>();

    private final HashMap<IntTriple, CompoundTag> nbtMap;
    private final File file;
    private final int headerSize;

    private RandomAccessFile braf;
    private MappedByteBuffer byteBuffer = null;

    private FileChannel fileChannel = null;
    private boolean hasBiomes = false;
    private boolean canHaveBiomes = true;
    private int nbtBytesRemaining;

    /**
     * Creates a new DiskOptimizedClipboard for the given region. Creates or overwrites a file using the given UUID as a name.
     */
    public DiskOptimizedClipboard(Region region, UUID uuid) {
        this(
                region.getDimensions(),
                MainUtil.getFile(
                        Fawe.instance() != null ? Fawe.platform().getDirectory() : new File("."),
                        Settings.settings().PATHS.CLIPBOARD + File.separator + uuid + ".bd"
                )
        );
        setOffset(region.getMinimumPoint());
        setOrigin(region.getMinimumPoint());
    }

    /**
     * Creates a new DiskOptimizedClipboard with the given dimensions. Creates a new file with a random UUID name.
     *
     * @deprecated Use {@link DiskOptimizedClipboard#DiskOptimizedClipboard(Region, UUID)} or
     *         {@link DiskOptimizedClipboard#DiskOptimizedClipboard(BlockVector3, File)} to avoid creating a large number of clipboard
     *         files that won't be cleaned until `clipboard.delete-after-days` and a server restart.
     */
    @Deprecated(forRemoval = true, since = "2.3.0")
    public DiskOptimizedClipboard(BlockVector3 dimensions) {
        this(
                dimensions,
                MainUtil.getFile(
                        Fawe.platform() != null ? Fawe.platform().getDirectory() : new File("."),
                        Settings.settings().PATHS.CLIPBOARD + File.separator + UUID.randomUUID() + ".bd"
                )
        );
    }

    /**
     * New DiskOptimizedClipboard. If the file specified exists, then it will be completely overwritten. To load an existing
     * clipboard, use {@link DiskOptimizedClipboard#DiskOptimizedClipboard(File)}.
     */
    public DiskOptimizedClipboard(BlockVector3 dimensions, File file) {
        super(dimensions, BlockVector3.ZERO);
        headerSize = HEADER_SIZE;
        if (headerSize + ((long) getVolume() << 1) >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "Dimensions too large for this clipboard format. Use //lazycopy for large selections.");
        } else if (headerSize + ((long) getVolume() << 1) + (long) ((getHeight() >> 2) + 1) * ((getLength() >> 2) + 1) * ((getWidth() >> 2) + 1) >= Integer.MAX_VALUE) {
            LOGGER.error("Dimensions are too large for biomes to be stored in a DiskOptimizedClipboard");
            canHaveBiomes = false;
        }
        nbtMap = new HashMap<>();
        try {
            this.file = file;
            try {
                if (!file.exists()) {
                    File parent = file.getParentFile();
                    if (parent != null) {
                        file.getParentFile().mkdirs();
                    }
                    file.createNewFile();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            this.braf = new RandomAccessFile(file, "rw");
            long fileLength = (long) (getVolume() << 1) + (long) headerSize;
            braf.setLength(0);
            braf.setLength(fileLength);
            this.nbtBytesRemaining = Integer.MAX_VALUE - (int) fileLength;
            init();
            // write getLength() etc
            byteBuffer.putChar(2, (char) (VERSION));
            byteBuffer.putChar(4, (char) getWidth());
            byteBuffer.putChar(6, (char) getHeight());
            byteBuffer.putChar(8, (char) getLength());
        } catch (IOException e) {
            close();
            throw new RuntimeException(e);
        } catch (Throwable t) {
            close();
            throw t;
        }
    }

    /**
     * Load an existing file as a DiskOptimizedClipboard. The file MUST exist and MUST be created as a DiskOptimizedClipboard
     * with data written to it.
     * @deprecated Will be made private, use {@link DiskOptimizedClipboard#loadFromFile(File)}
     */
    @Deprecated(forRemoval = true, since = "2.6.2")
    public DiskOptimizedClipboard(File file) {
        this(file, VERSION);
    }

    /**
     * Load an existing file as a DiskOptimizedClipboard. The file MUST exist and MUST be created as a DiskOptimizedClipboard
     * with data written to it.
     *
     * @param file            File to read from
     * @param versionOverride An override version to allow loading of older clipboards if required
     * @deprecated Will be made private, use {@link DiskOptimizedClipboard#loadFromFile(File)}
     */
    @Deprecated(forRemoval = true, since = "2.6.2")
    public DiskOptimizedClipboard(File file, int versionOverride) {
        super(readSize(file, versionOverride), BlockVector3.ZERO);
        headerSize = getHeaderSizeOverrideFromVersion(versionOverride);
        nbtMap = new HashMap<>();
        try {
            this.file = file;
            this.braf = new RandomAccessFile(file, "rw");
            braf.setLength(file.length());
            this.nbtBytesRemaining = Integer.MAX_VALUE - (int) file.length();
            init();

            int biomeLength = ((getHeight() >> 2) + 1) * ((getLength() >> 2) + 1) * ((getWidth() >> 2) + 1);
            canHaveBiomes = (long) headerSize + biomeLength < Integer.MAX_VALUE;

            if (headerSize >= VERSION_2_HEADER_SIZE) {
                readBiomeStatusFromHeader();
                int nbtCount = readNBTSavedCountFromHeader();
                int entitiesCount = readEntitiesSavedCountFromHeader();
                if (Settings.settings().CLIPBOARD.SAVE_CLIPBOARD_NBT_TO_DISK && (nbtCount + entitiesCount > 0)) {
                    loadNBTFromFileFooter(nbtCount, entitiesCount, biomeLength);
                }
            } else if (canHaveBiomes && braf.length() - headerSize == ((long) getVolume() << 1) + biomeLength) {
                hasBiomes = true;
            }
            getAndSetOffsetAndOrigin();
        } catch (IOException e) {
            close();
            throw new RuntimeException(e);
        } catch (Throwable t) {
            close();
            throw t;
        }
    }

    /**
     * Attempt to load a file into a new {@link DiskOptimizedClipboard} instance. Will attempt to recover on version mismatch
     * failure.
     *
     * @param file File to load
     * @return new {@link DiskOptimizedClipboard} instance.
     */
    public static DiskOptimizedClipboard loadFromFile(final File file) {
        DiskOptimizedClipboard doc;
        try {
            doc = new DiskOptimizedClipboard(file);
        } catch (FaweClipboardVersionMismatchException e) { // Attempt to recover
            int version = e.getClipboardVersion();
            doc = new DiskOptimizedClipboard(file, version);
        }
        return doc;
    }

    private static BlockVector3 readSize(File file, int expectedVersion) {
        try (DataInputStream is = new DataInputStream(new FileInputStream(file))) {
            is.skipBytes(2);
            int version = is.readChar();
            if (version != expectedVersion) {
                throw new FaweClipboardVersionMismatchException(expectedVersion, version);
            }
            return BlockVector3.at(is.readChar(), is.readChar(), is.readChar());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void loadNBTFromFileFooter(int nbtCount, int entitiesCount, long biomeLength) throws IOException {
        int biomeBlocksLength = headerSize + (getVolume() << 1) + (hasBiomes ? (int) biomeLength : 0);
        MappedByteBuffer tmp = fileChannel.map(FileChannel.MapMode.READ_ONLY, biomeBlocksLength, braf.length());
        try (NBTInputStream nbtIS = new NBTInputStream(MainUtil.getCompressedIS(new ByteBufferInputStream(tmp)))) {
            Iterator<CompoundTag> iter = nbtIS.toIterator();
            while (nbtCount > 0 && iter.hasNext()) { // TileEntities are stored "before" entities
                CompoundTag tag = iter.next();
                int x = tag.getInt("x");
                int y = tag.getInt("y");
                int z = tag.getInt("z");
                IntTriple pos = new IntTriple(x, y, z);
                nbtMap.put(pos, tag);
                nbtCount--;
            }
            while (entitiesCount > 0 && iter.hasNext()) {
                CompoundTag tag = iter.next();
                Tag posTag = tag.getValue().get("Pos");
                if (posTag == null) {
                    LOGGER.warn("Missing pos tag: {}", tag);
                    return;
                }
                List<DoubleTag> pos = (List<DoubleTag>) posTag.getValue();
                double x = pos.get(0).getValue();
                double y = pos.get(1).getValue();
                double z = pos.get(2).getValue();
                BaseEntity entity = new BaseEntity(tag);
                BlockArrayClipboard.ClipboardEntity clipboardEntity = new BlockArrayClipboard.ClipboardEntity(
                        this,
                        x,
                        y,
                        z,
                        0f,
                        0f,
                        entity
                );
                this.entities.add(clipboardEntity);
                entitiesCount--;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getHeaderSizeOverrideFromVersion(int versionOverride) {
        return switch (versionOverride) {
            case 1 -> VERSION_1_HEADER_SIZE;
            case 2 -> VERSION_2_HEADER_SIZE;
            default -> HEADER_SIZE;
        };
    }

    @Override
    public URI getURI() {
        return file.toURI();
    }

    public File getFile() {
        return file;
    }

    private void init() throws IOException {
        if (this.fileChannel == null) {
            this.fileChannel = braf.getChannel();
            if (Settings.settings().CLIPBOARD.LOCK_CLIPBOARD_FILE) {
                try {
                    FileLock lock = this.fileChannel.lock();
                    LOCK_HOLDER_CACHE.put(file.getName(), new LockHolder(lock));
                } catch (OverlappingFileLockException e) {
                    LockHolder existing = LOCK_HOLDER_CACHE.get(file.getName());
                    if (existing != null) {
                        long ms = System.currentTimeMillis() - existing.lockHeldSince;
                        LOGGER.error(
                                "Cannot lock clipboard file {} acquired by thread {}, {}ms ago",
                                file.getName(),
                                existing.thread,
                                ms
                        );
                    }
                    // Rethrow to prevent clipboard access
                    throw e;
                }
            }
            this.byteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, braf.length());
        }
    }

    private boolean initBiome() {
        if (!canHaveBiomes) {
            return false;
        }
        if (!hasBiomes) {
            try {
                hasBiomes = true;
                close();
                this.braf = new RandomAccessFile(file, "rw");
                // Since biomes represent a 4x4x4 cube, we store fewer biome bytes that volume at 1 byte per biome
                // +1 to each to allow for cubes that lie across the region boundary
                long length =
                        headerSize + ((long) getVolume() << 1) + (long) ((getHeight() >> 2) + 1) * ((getLength() >> 2) + 1) * ((getWidth() >> 2) + 1);
                this.braf.setLength(length);
                this.nbtBytesRemaining = Integer.MAX_VALUE - (int) length;
                init();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean hasBiomes() {
        return hasBiomes;
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        return setBiome(position.x(), position.y(), position.z(), biome);
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        setBiome(getBiomeIndex(x, y, z), biome);
        return true;
    }

    @Override
    public void setBiome(int index, BiomeType biome) {
        if (initBiome()) {
            try {
                byteBuffer.put(headerSize + (getVolume() << 1) + index, (byte) biome.getInternalId());
            } catch (IndexOutOfBoundsException e) {
                LOGGER.info((long) (getHeight() >> 2) * (getLength() >> 2) * (getWidth() >> 2));
                LOGGER.info(index);
                e.printStackTrace();
            }
        }
    }

    @Override
    public BiomeType getBiome(int index) {
        if (!hasBiomes()) {
            return null;
        }
        int biomeId = byteBuffer.get(headerSize + (getVolume() << 1) + index) & 0xFF;
        return BiomeTypes.get(biomeId);
    }

    @Override
    public void streamBiomes(IntValueReader task) {
        if (!hasBiomes()) {
            return;
        }
        int mbbIndex = headerSize + (getVolume() << 1);
        try {
            for (int y = 0; y < getHeight(); y++) {
                for (int z = 0; z < getLength(); z++) {
                    for (int x = 0; x < getWidth(); x++) {
                        int biome = byteBuffer.get(mbbIndex + getBiomeIndex(x, y, z)) & 0xFF;
                        task.applyInt(getIndex(x, y, z), biome);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public BiomeType getBiomeType(int x, int y, int z) {
        return getBiome(getBiomeIndex(x, y, z));
    }

    @Override
    public BiomeType getBiome(BlockVector3 position) {
        return getBiome(getBiomeIndex(position.x(), position.y(), position.z()));
    }

    public BlockArrayClipboard toClipboard() {
        try {
            Region region = getRegion();
            region.shift(offset);
            BlockArrayClipboard clipboard = new BlockArrayClipboard(region, this);
            clipboard.setOrigin(getOrigin().add(offset));
            return clipboard;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void setOrigin(BlockVector3 origin) {
        super.setOrigin(origin);
        origin = origin.subtract(offset);
        try {
            byteBuffer.putShort(10, (short) origin.x());
            byteBuffer.putShort(12, (short) origin.y());
            byteBuffer.putShort(14, (short) origin.z());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setOffset(BlockVector3 offset) {
        super.setOffset(offset);
        try {
            byteBuffer.putShort(16, (short) offset.x());
            byteBuffer.putShort(18, (short) offset.y());
            byteBuffer.putShort(20, (short) offset.z());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void getAndSetOffsetAndOrigin() {
        int x = byteBuffer.getShort(16);
        int y = byteBuffer.getShort(18);
        int z = byteBuffer.getShort(20);
        super.setOffset(BlockVector3.at(x, y, z));
        int ox = byteBuffer.getShort(10);
        int oy = byteBuffer.getShort(12);
        int oz = byteBuffer.getShort(14);
        super.setOrigin(BlockVector3.at(ox, oy, oz));
    }

    @Override
    public void flush() {
        byteBuffer.force();
    }

    private void closeDirectBuffer(ByteBuffer cb) {
        if (cb == null || !cb.isDirect()) {
            return;
        }
        ReflectionUtils.getUnsafe().invokeCleaner(cb);
    }

    private void writeBiomeStatusToHeader() {
        byteBuffer.put(22, (byte) (hasBiomes ? 1 : 0));
    }

    private void writeNBTSavedCountToHeader(int count) {
        byteBuffer.putChar(23, (char) count);
    }

    private void writeEntitiesSavedCountToHeader(int count) {
        byteBuffer.putChar(25, (char) count);
    }

    private boolean readBiomeStatusFromHeader() {
        return this.hasBiomes = byteBuffer.get(22) == 1;
    }

    private int readNBTSavedCountFromHeader() {
        return byteBuffer.getChar(23);
    }

    private int readEntitiesSavedCountFromHeader() {
        return byteBuffer.getChar(25);
    }

    @Override
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void close() {
        try {
            if (byteBuffer != null) {
                if (headerSize >= VERSION_2_HEADER_SIZE) {
                    if (Settings.settings().CLIPBOARD.SAVE_CLIPBOARD_NBT_TO_DISK) {
                        try {
                            writeNBTToDisk();
                        } catch (Exception e) {
                            LOGGER.error("Unable to save NBT data to disk.", e);
                        }
                    }
                    writeBiomeStatusToHeader();
                }
                byteBuffer.force();
                fileChannel.close();
                braf.close();
                file.setWritable(true);
                MappedByteBuffer tmpBuffer = byteBuffer;
                byteBuffer = null;
                closeDirectBuffer(tmpBuffer);
                fileChannel = null;
                braf = null;
            } else if (fileChannel != null) {
                fileChannel.close();
                fileChannel = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (fileChannel != null) {
                try {
                    fileChannel.close();
                    fileChannel = null;
                } catch (IOException ex) {
                    LOGGER.error("Could not close file channel on clipboard {}. If this belongs to a player, the server may " +
                            "need to be restarted for clipboard use to work.", getFile().getName(), ex);
                }
            }
        }
    }

    private void writeNBTToDisk() throws IOException {
        if (!nbtMap.isEmpty() || !entities.isEmpty()) {
            byte[] output = null;
            boolean entitiesFit = false;
            // Closing a BAOS does nothing
            ByteArrayOutputStream baOS = new ByteArrayOutputStream();
            try (NBTOutputStream nbtOS = new NBTOutputStream(MainUtil.getCompressedOS(
                    baOS,
                    Settings.settings().CLIPBOARD.COMPRESSION_LEVEL
            ))) {
                if (!nbtMap.isEmpty()) {
                    try {
                        for (CompoundTag tag : nbtMap.values()) {
                            nbtOS.writeTag(tag);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    nbtOS.flush();
                    if (baOS.size() > nbtBytesRemaining) {
                        LOGGER.warn(
                                "Clipboard file {} does not have enough remaining space to store NBT data on disk.",
                                file.getName()
                        );
                        writeNBTSavedCountToHeader(0);
                        writeEntitiesSavedCountToHeader(0);
                        return;
                    } else {
                        writeNBTSavedCountToHeader(nbtMap.size());
                        nbtBytesRemaining -= baOS.size();
                    }
                    output = baOS.toByteArray(); //Keep this in case entities are unable to fit.
                }

                if (!entities.isEmpty()) {
                    try {
                        for (BlockArrayClipboard.ClipboardEntity entity : entities) {
                            if (entity.getState() != null && entity.getState().getNbtData() != null) {
                                CompoundTag data = entity.getState().getNbtData();
                                HashMap<String, Tag<?, ?>> value = new HashMap<>(data.getValue());
                                List<DoubleTag> pos = new ArrayList<>(3);
                                pos.add(new DoubleTag(entity.getLocation().x()));
                                pos.add(new DoubleTag(entity.getLocation().x()));
                                pos.add(new DoubleTag(entity.getLocation().x()));
                                value.put("Pos", new ListTag(DoubleTag.class, pos));
                                nbtOS.writeTag(new CompoundTag(value));
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    nbtOS.flush();
                    if (baOS.size() > nbtBytesRemaining) {
                        LOGGER.warn(
                                "Clipboard file {} does not have enough remaining space to store entity data on disk.",
                                file.getName()
                        );
                        writeEntitiesSavedCountToHeader(0);
                    } else {
                        entitiesFit = true;
                        writeEntitiesSavedCountToHeader(entities.size());
                    }
                }
            }

            if (entitiesFit) {
                output = baOS.toByteArray();
            }

            if (output == null) {
                return;
            }

            long currentLength = this.braf.length();
            this.braf.setLength(currentLength + baOS.size());
            MappedByteBuffer tempBuffer = fileChannel.map(
                    FileChannel.MapMode.READ_WRITE,
                    currentLength,
                    baOS.size()
            );
            tempBuffer.put(output);
            tempBuffer.force();
            closeDirectBuffer(tempBuffer);
        } else {
            writeNBTSavedCountToHeader(0);
            writeEntitiesSavedCountToHeader(0);
        }
    }

    @Override
    public Collection<CompoundTag> getTileEntities() {
        return nbtMap.values();
    }

    public int getIndex(int x, int y, int z) {
        return x + y * getArea() + z * getWidth();
    }

    public int getBiomeIndex(int x, int y, int z) {
        return (x >> 2) + (y >> 2) * (getWidth() >> 2) * (getLength() >> 2) + (z >> 2) * (getWidth() >> 2);
    }

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        return toBaseBlock(getBlock(x, y, z), x, y, z);
    }

    private BaseBlock toBaseBlock(BlockState state, int i) {
        if (state.getMaterial().hasContainer() && !nbtMap.isEmpty()) {
            CompoundTag nbt;
            if (nbtMap.size() < 4) {
                nbt = null;
                for (Map.Entry<IntTriple, CompoundTag> entry : nbtMap.entrySet()) {
                    IntTriple key = entry.getKey();
                    int index = getIndex(key.x(), key.y(), key.z());
                    if (index == i) {
                        nbt = entry.getValue();
                        break;
                    }
                }
            } else {
                int y = i / getArea();
                int newI = i - y * getArea();
                int z = newI / getWidth();
                int x = newI - z * getWidth();
                nbt = nbtMap.get(new IntTriple(x, y, z));
            }
            return state.toBaseBlock(nbt);
        }
        return state.toBaseBlock();
    }

    private BaseBlock toBaseBlock(BlockState state, int x, int y, int z) {
        if (state.getMaterial().hasContainer() && !nbtMap.isEmpty()) {
            CompoundTag nbt = nbtMap.get(new IntTriple(x, y, z));
            return state.toBaseBlock(nbt);
        }
        return state.toBaseBlock();
    }

    @Override
    public BaseBlock getFullBlock(int i) {
        return toBaseBlock(getBlock(i), i);
    }

    @Override
    public BlockState getBlock(int index) {
        try {
            int diskIndex = headerSize + (index << 1);
            char ordinal = byteBuffer.getChar(diskIndex);
            return BlockState.getFromOrdinal(ordinal);
        } catch (IndexOutOfBoundsException ignored) {
        }
        return BlockTypes.AIR.getDefaultState();
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return getBlock(getIndex(x, y, z));
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tag) {
        final Map<String, Tag<?, ?>> values = new HashMap<>(tag.getValue());
        values.put("x", new IntTag(x));
        values.put("y", new IntTag(y));
        values.put("z", new IntTag(z));
        nbtMap.put(new IntTriple(x, y, z), new CompoundTag(values));
        return true;
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(int x, int y, int z, B block) {
        try {
            int index = headerSize + (getIndex(x, y, z) << 1);
            char ordinal = block.getOrdinalChar();
            if (ordinal == BlockTypesCache.ReservedIDs.__RESERVED__) {
                ordinal = BlockTypesCache.ReservedIDs.AIR;
            }
            byteBuffer.putChar(index, ordinal);
            boolean hasNbt = block instanceof BaseBlock && block.hasNbtData();
            if (hasNbt) {
                setTile(x, y, z, block.getNbtData());
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(int i, B block) {
        try {
            char ordinal = block.getOrdinalChar();
            int index = headerSize + (i << 1);
            byteBuffer.putChar(index, ordinal);
            boolean hasNbt = block instanceof BaseBlock && block.hasNbtData();
            if (hasNbt) {
                int y = i / getArea();
                int newI = i - y * getArea();
                int z = newI / getWidth();
                int x = newI - z * getWidth();
                setTile(x, y, z, block.getNbtData());
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static class LockHolder {

        final FileLock lock;
        final long lockHeldSince;
        final String thread;

        LockHolder(FileLock lock) {
            this.lock = lock;
            lockHeldSince = System.currentTimeMillis();
            this.thread = Thread.currentThread().getName();
        }
    }


}
