package com.fastasyncworldedit.core.extent.clipboard;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.internal.exception.FaweClipboardVersionMismatchException;
import com.fastasyncworldedit.core.jnbt.streamer.IntValueReader;
import com.fastasyncworldedit.core.math.IntTriple;
import com.fastasyncworldedit.core.util.MainUtil;
import com.fastasyncworldedit.core.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A clipboard with disk backed storage. (lower memory + loads on crash)
 * - Uses an auto closable RandomAccessFile for getting / setting id / data
 * - I don't know how to reduce nbt / entities to O(2) complexity, so it is stored in memory.
 */
public class DiskOptimizedClipboard extends LinearClipboard implements Closeable {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private static final int VERSION = 1;
    private static final int HEADER_SIZE = 22;

    private final HashMap<IntTriple, CompoundTag> nbtMap;
    private final File file;

    private RandomAccessFile braf;
    private MappedByteBuffer byteBuffer;

    private FileChannel fileChannel;
    private boolean hasBiomes;
    private boolean canHaveBiomes = true;

    public DiskOptimizedClipboard(Region region, UUID uuid) {
        this(
                region.getDimensions(),
                MainUtil.getFile(
                        Fawe.get() != null ? Fawe.imp().getDirectory() : new File("."),
                        Settings.settings().PATHS.CLIPBOARD + File.separator + uuid + ".bd"
                )
        );
        setOffset(region.getMinimumPoint());
        setOrigin(region.getMinimumPoint());
    }

    public DiskOptimizedClipboard(BlockVector3 dimensions) {
        this(
                dimensions,
                MainUtil.getFile(
                        Fawe.imp() != null ? Fawe.imp().getDirectory() : new File("."),
                        Settings.settings().PATHS.CLIPBOARD + File.separator + UUID.randomUUID() + ".bd"
                )
        );
    }

    public DiskOptimizedClipboard(BlockVector3 dimensions, File file) {
        super(dimensions, BlockVector3.ZERO);
        if (HEADER_SIZE + ((long) getVolume() << 1) >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "Dimensions too large for this clipboard format. Use //lazycopy for large selections.");
        } else if (HEADER_SIZE + ((long) getVolume() << 1) + (long) ((getHeight() >> 2) + 1) * ((getLength() >> 2) + 1) * ((getWidth() >> 2) + 1) >= Integer.MAX_VALUE) {
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
            long fileLength = (long) getVolume() * 2L + (long) HEADER_SIZE;
            braf.setLength(0);
            braf.setLength(fileLength);
            init();
            // write getLength() etc
            byteBuffer.putChar(2, (char) (VERSION));
            byteBuffer.putChar(4, (char) getWidth());
            byteBuffer.putChar(6, (char) getHeight());
            byteBuffer.putChar(8, (char) getLength());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public DiskOptimizedClipboard(File file) {
        super(readSize(file), BlockVector3.ZERO);
        nbtMap = new HashMap<>();
        try {
            this.file = file;
            this.braf = new RandomAccessFile(file, "rw");
            braf.setLength(file.length());
            init();
            if (braf.length() - HEADER_SIZE == ((long) getVolume() << 1) + (long) ((getHeight() >> 2) + 1) * ((getLength() >> 2) + 1) * ((getWidth() >> 2) + 1)) {
                hasBiomes = true;
            }
            getOffset();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static BlockVector3 readSize(File file) {
        try (DataInputStream is = new DataInputStream(new FileInputStream(file))) {
            is.skipBytes(2);
            int version = is.readChar();
            if (version != VERSION) {
                throw new FaweClipboardVersionMismatchException();
            }
            return BlockVector3.at(is.readChar(), is.readChar(), is.readChar());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
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
            this.fileChannel.lock();
            this.byteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, file.length());
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
                // +1 to each too allow for cubes that lie across the region boundary
                this.braf.setLength(HEADER_SIZE + ((long) getVolume() << 1) + (long) ((getHeight() >> 2) + 1) * ((getLength() >> 2) + 1) * ((getWidth() >> 2) + 1));
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
        return setBiome(position.getX(), position.getY(), position.getZ(), biome);
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
                byteBuffer.put(HEADER_SIZE + (getVolume() << 1) + index, (byte) biome.getInternalId());
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
        int biomeId = byteBuffer.get(HEADER_SIZE + (getVolume() << 1) + index) & 0xFF;
        return BiomeTypes.get(biomeId);
    }

    @Override
    public void streamBiomes(IntValueReader task) {
        if (!hasBiomes()) {
            return;
        }
        int mbbIndex = HEADER_SIZE + (getVolume() << 1);
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
        return getBiome(getBiomeIndex(position.getX(), position.getY(), position.getZ()));
    }

    public BlockArrayClipboard toClipboard() {
        try {
            CuboidRegion region = new CuboidRegion(
                    BlockVector3.at(0, 0, 0),
                    BlockVector3.at(getWidth() - 1, getHeight() - 1, getLength() - 1)
            );
            int offsetX = byteBuffer.getShort(16);
            int offsetY = byteBuffer.getShort(18);
            int offsetZ = byteBuffer.getShort(20);
            region.shift(BlockVector3.at(offsetX, offsetY, offsetZ));
            BlockArrayClipboard clipboard = new BlockArrayClipboard(region, this);
            clipboard.setOrigin(getOrigin());
            return clipboard;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public BlockVector3 getOrigin() {
        int ox = byteBuffer.getShort(10);
        int oy = byteBuffer.getShort(12);
        int oz = byteBuffer.getShort(14);
        return BlockVector3.at(ox, oy, oz);
    }

    @Override
    public void setOrigin(BlockVector3 offset) {
        super.setOrigin(offset);
        try {
            byteBuffer.putShort(10, (short) offset.getBlockX());
            byteBuffer.putShort(12, (short) offset.getBlockY());
            byteBuffer.putShort(14, (short) offset.getBlockZ());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setOffset(BlockVector3 offset) {
        super.setOffset(offset);
        try {
            byteBuffer.putShort(16, (short) offset.getBlockX());
            byteBuffer.putShort(18, (short) offset.getBlockY());
            byteBuffer.putShort(20, (short) offset.getBlockZ());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void getOffset() {
        int x = byteBuffer.getShort(16);
        int y = byteBuffer.getShort(18);
        int z = byteBuffer.getShort(20);
        super.setOffset(BlockVector3.at(x, y, z));
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

    @Override
    public void close() {
        try {
            if (byteBuffer != null) {
                byteBuffer.force();
                fileChannel.close();
                braf.close();
                //noinspection ResultOfMethodCallIgnored
                file.setWritable(true);
                closeDirectBuffer(byteBuffer);
                byteBuffer = null;
                fileChannel = null;
                braf = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
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
                    int index = getIndex(key.getX(), key.getY(), key.getZ());
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
            int diskIndex = HEADER_SIZE + (index << 1);
            char ordinal = byteBuffer.getChar(diskIndex);
            return BlockState.getFromOrdinal(ordinal);
        } catch (IndexOutOfBoundsException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
        }
        return BlockTypes.AIR.getDefaultState();
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return getBlock(getIndex(x, y, z));
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tag) {
        final Map<String, Tag> values = new HashMap<>(tag.getValue());
        values.put("x", new IntTag(x));
        values.put("y", new IntTag(y));
        values.put("z", new IntTag(z));
        nbtMap.put(new IntTriple(x, y, z), new CompoundTag(values));
        return true;
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(int x, int y, int z, B block) {
        try {
            int index = HEADER_SIZE + (getIndex(x, y, z) << 1);
            char ordinal = block.getOrdinalChar();
            if (ordinal == 0) {
                ordinal = 1;
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
            int index = HEADER_SIZE + (i << 1);
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

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity) {
        BlockArrayClipboard.ClipboardEntity ret = new BlockArrayClipboard.ClipboardEntity(location, entity);
        entities.add(ret);
        return ret;
    }

    @Override
    public List<? extends Entity> getEntities() {
        return new ArrayList<>(entities);
    }

    @Override
    public List<? extends Entity> getEntities(Region region) {
        return new ArrayList<>(entities
                .stream()
                .filter(e -> region.contains(e.getLocation().toBlockPoint()))
                .collect(Collectors.toList()));
    }

    @Override
    public void removeEntity(Entity entity) {
        if (!(entity instanceof BlockArrayClipboard.ClipboardEntity)) {
            Location loc = entity.getLocation();
            removeEntity(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), entity.getState().getNbtData().getUUID());
        } else {
            this.entities.remove(entity);
        }
    }

    @Override
    public void removeEntity(int x, int y, int z, UUID uuid) {
        Iterator<BlockArrayClipboard.ClipboardEntity> iter = this.entities.iterator();
        while (iter.hasNext()) {
            BlockArrayClipboard.ClipboardEntity entity = iter.next();
            UUID entUUID = entity.getState().getNbtData().getUUID();
            if (uuid.equals(entUUID)) {
                iter.remove();
                return;
            }
        }
    }

}
