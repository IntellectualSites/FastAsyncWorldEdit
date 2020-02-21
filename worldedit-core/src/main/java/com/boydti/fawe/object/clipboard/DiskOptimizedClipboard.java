package com.boydti.fawe.object.clipboard;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.jnbt.streamer.IntValueReader;
import com.boydti.fawe.object.IntegerTrio;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * A clipboard with disk backed storage. (lower memory + loads on crash) - Uses an auto closable
 * RandomAccessFile for getting / setting id / data - I don't know how to reduce nbt / entities to
 * O(2) complexity, so it is stored in memory.
 */
public class DiskOptimizedClipboard extends LinearClipboard {

    private static int HEADER_SIZE = 14;

    private final HashMap<IntegerTrio, CompoundTag> nbtMap;
    private final File file;

    private RandomAccessFile braf;
    private MappedByteBuffer byteBuffer;

    private FileChannel fileChannel;
    private boolean hasBiomes;
    private int ylast;
    private int ylasti;
    private int zlast;
    private int zlasti;

    public DiskOptimizedClipboard(BlockVector3 dimensions, UUID uuid) {
        this(dimensions, MainUtil
            .getFile(Fawe.get() != null ? Fawe.imp().getDirectory() : new File("."),
                Settings.IMP.PATHS.CLIPBOARD + File.separator + uuid + ".bd"));
    }

    public DiskOptimizedClipboard(BlockVector3 dimensions) {
        this(dimensions, MainUtil
            .getFile(Fawe.imp() != null ? Fawe.imp().getDirectory() : new File("."),
                Settings.IMP.PATHS.CLIPBOARD + File.separator + UUID.randomUUID() + ".bd"));
    }

    public DiskOptimizedClipboard(BlockVector3 dimensions, File file) {
        super(dimensions);
        if (getWidth() > Character.MAX_VALUE || getHeight() > Character.MAX_VALUE
            || getLength() > Character.MAX_VALUE) {
            throw new IllegalArgumentException("Too large");
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
            byteBuffer.putChar(2, (char) getWidth());
            byteBuffer.putChar(4, (char) getHeight());
            byteBuffer.putChar(6, (char) getLength());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public DiskOptimizedClipboard(File file) {
        super(readSize(file));
        nbtMap = new HashMap<>();
        try {
            this.file = file;
            this.braf = new RandomAccessFile(file, "rw");
            braf.setLength(file.length());
            init();
            if (braf.length() - HEADER_SIZE == (getVolume() << 1) + getArea()) {
                hasBiomes = true;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static BlockVector3 readSize(File file) {
        try (DataInputStream is = new DataInputStream(new FileInputStream(file))) {
            is.skipBytes(2);
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
            this.byteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, file.length());
        }
    }

    private boolean initBiome() {
        if (!hasBiomes) {
            try {
                hasBiomes = true;
                close();
                this.braf = new RandomAccessFile(file, "rw");
                this.braf.setLength(HEADER_SIZE + (getVolume() << 1) + getArea());
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
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        setBiome(getIndex(x, 0, z), biome);
        return true;
    }

    @Override
    public void setBiome(int index, BiomeType biome) {
        if (initBiome()) {
            byteBuffer.put(HEADER_SIZE + (getVolume() << 1) + index, (byte) biome.getInternalId());
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
        int index = 0;
        int mbbIndex = HEADER_SIZE + (getVolume() << 1);
        try {
            for (int z = 0; z < getLength(); z++) {
                for (int x = 0; x < getWidth(); x++, index++, mbbIndex++) {
                    int biome = byteBuffer.get(mbbIndex) & 0xFF;
                    task.applyInt(index, biome);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public BiomeType getBiomeType(int x, int y, int z) {
        return getBiome(getIndex(x, 0, z));
    }

    public BlockArrayClipboard toClipboard() {
        try {
            CuboidRegion region = new CuboidRegion(BlockVector3.at(0, 0, 0),
                BlockVector3.at(getWidth() - 1, getHeight() - 1, getLength() - 1));
            int ox = byteBuffer.getShort(8);
            int oy = byteBuffer.getShort(10);
            int oz = byteBuffer.getShort(12);
            BlockArrayClipboard clipboard = new BlockArrayClipboard(region, this);
            clipboard.setOrigin(BlockVector3.at(ox, oy, oz));
            return clipboard;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void setOrigin(BlockVector3 offset) {
        super.setOrigin(offset);
        try {
            byteBuffer.putShort(8, (short) offset.getBlockX());
            byteBuffer.putShort(10, (short) offset.getBlockY());
            byteBuffer.putShort(12, (short) offset.getBlockZ());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void flush() {
        byteBuffer.force();
    }

    private void closeDirectBuffer(ByteBuffer cb) {
        if (cb == null || !cb.isDirect()) {
            return;
        }
        // we could use this type cast and call functions without reflection code,
        // but static import from sun.* package is risky for non-SUN virtual machine.
        //try { ((sun.nio.ch.DirectBuffer)cb).cleaner().clean(); } catch (Exception ex) { }
        try {
            Method cleaner = cb.getClass().getMethod("cleaner");
            cleaner.setAccessible(true);
            Method clean = Class.forName("sun.misc.Cleaner").getMethod("clean");
            clean.setAccessible(true);
            clean.invoke(cleaner.invoke(cb));
        } catch (Exception ex) {
            try {
                final Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
                final Field theUnsafeField = unsafeClass.getDeclaredField("theUnsafe");
                theUnsafeField.setAccessible(true);
                final Object theUnsafe = theUnsafeField.get(null);
                final Method invokeCleanerMethod = unsafeClass
                    .getMethod("invokeCleaner", ByteBuffer.class);
                invokeCleanerMethod.invoke(theUnsafe, cb);
            } catch (Exception e) {
                System.gc();
            }
        }
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
        return x + (ylast == y ? ylasti : (ylasti = (ylast = y) * getArea())) + (zlast == z
            ? zlasti : (zlasti = (zlast = z) * getWidth()));
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
                for (Map.Entry<IntegerTrio, CompoundTag> entry : nbtMap.entrySet()) {
                    IntegerTrio key = entry.getKey();
                    int index = getIndex(key.x, key.y, key.z);
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
                nbt = nbtMap.get(new IntegerTrio(x, y, z));
            }
            return state.toBaseBlock(nbt);
        }
        return state.toBaseBlock();
    }

    private BaseBlock toBaseBlock(BlockState state, int x, int y, int z) {
        if (state.getMaterial().hasContainer() && !nbtMap.isEmpty()) {
            CompoundTag nbt = nbtMap.get(new IntegerTrio(x, y, z));
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
        } catch (IndexOutOfBoundsException ignore) {
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
        nbtMap.put(new IntegerTrio(x, y, z), tag);
        Map<String, Tag> values = ReflectionUtils.getMap(tag.getValue());
        values.put("x", new IntTag(x));
        values.put("y", new IntTag(y));
        values.put("z", new IntTag(z));
        return true;
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(int x, int y, int z, B block) {
        try {
            int index = HEADER_SIZE + (getIndex(x, y, z) << 1);
            char ordinal = block.getOrdinalChar();
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
        BlockArrayClipboard.ClipboardEntity ret = new BlockArrayClipboard.ClipboardEntity(location,
            entity);
        entities.add(ret);
        return ret;
    }

    @Override
    public List<? extends Entity> getEntities() {
        return new ArrayList<>(entities);
    }

    @Override
    public void removeEntity(Entity entity) {
        this.entities.remove(entity);
    }

}
