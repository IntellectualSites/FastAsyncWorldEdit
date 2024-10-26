package com.fastasyncworldedit.core.history.changeset;

import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.history.change.ChangePopulator;
import com.fastasyncworldedit.core.history.change.MutableBiomeChange;
import com.fastasyncworldedit.core.history.change.MutableBlockChange;
import com.fastasyncworldedit.core.history.change.MutableEntityChange;
import com.fastasyncworldedit.core.history.change.MutableFullBlockChange;
import com.fastasyncworldedit.core.history.change.MutableTileChange;
import com.fastasyncworldedit.core.history.change.BlockPositionChange;
import com.fastasyncworldedit.core.internal.exception.FaweSmallEditUnsupportedException;
import com.fastasyncworldedit.core.internal.io.FaweInputStream;
import com.fastasyncworldedit.core.internal.io.FaweOutputStream;
import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.fastasyncworldedit.core.util.MainUtil;
import com.fastasyncworldedit.core.util.MathMan;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.Exchanger;
import java.util.function.BiConsumer;

/**
 * FAWE stream ChangeSet offering support for extended-height worlds
 */
@ApiStatus.Internal
public abstract class FaweStreamChangeSet extends AbstractChangeSet {

    public static final int HEADER_SIZE = 9;
    private static final int VERSION = 2;
    // equivalent to Short#MIN_VALUE three times stored with [(x) & 0xff, ((rx) >> 8) & 0xff]
    private static final byte[] MAGIC_NEW_RELATIVE = new byte[]{0, (byte) 128, 0, (byte) 128, 0, (byte) 128};
    private int mode;
    private final int compression;
    private final int minY;

    protected long blockSize;
    private int originX;
    private int originZ;
    private int version;

    protected FaweStreamIdDelegate idDel;
    protected FaweStreamPositionDelegate posDel;

    public FaweStreamChangeSet(World world) {
        this(world, Settings.settings().HISTORY.COMPRESSION_LEVEL, Settings.settings().HISTORY.STORE_REDO, Settings.settings().HISTORY.SMALL_EDITS);
    }

    public FaweStreamChangeSet(World world, int compression, boolean storeRedo, boolean smallLoc) {
        super(world);
        this.compression = compression;
        this.minY = world.getMinY();
        init(storeRedo, smallLoc);
    }

    private void init(boolean storeRedo, boolean smallLoc) {
        if (storeRedo) {
            if (smallLoc) {
                mode = 4;
            } else {
                mode = 3;
            }
        } else if (smallLoc) {
            mode = 1;
        } else {
            mode = 2;
        }
    }

    interface FaweStreamPositionDelegate {

        void write(OutputStream out, int x, int y, int z) throws IOException;

        void read(FaweInputStream in, BlockPositionChange change) throws IOException;

    }

    interface FaweStreamIdDelegate {

        void writeChange(FaweOutputStream out, int from, int to) throws IOException;

        void readCombined(FaweInputStream in, MutableBlockChange change, boolean dir) throws IOException;

        void readCombined(FaweInputStream in, MutableFullBlockChange change) throws IOException;

    }

    protected void setupStreamDelegates(int mode) {
        this.mode = mode;
        if (mode == 3 || mode == 4) {
            idDel = new FaweStreamIdDelegate() {
                @Override
                public void writeChange(FaweOutputStream stream, int combinedFrom, int combinedTo) throws IOException {
                    stream.writeVarInt(combinedFrom);
                    stream.writeVarInt(combinedTo);
                }

                @Override
                public void readCombined(FaweInputStream is, MutableBlockChange change, boolean dir) throws IOException {
                    if (dir) {
                        is.readVarInt();
                        change.ordinal = is.readVarInt();
                    } else {
                        change.ordinal = is.readVarInt();
                        is.readVarInt();
                    }
                }

                @Override
                public void readCombined(FaweInputStream is, MutableFullBlockChange change) throws IOException {
                    change.from = is.readVarInt();
                    change.to = is.readVarInt();
                }
            };
        } else {
            idDel = new FaweStreamIdDelegate() {
                @Override
                public void writeChange(FaweOutputStream stream, int combinedFrom, int to) throws IOException {
                    stream.writeVarInt(combinedFrom);
                }

                @Override
                public void readCombined(FaweInputStream in, MutableBlockChange change, boolean dir) throws IOException {
                    change.ordinal = in.readVarInt();
                }

                @Override
                public void readCombined(FaweInputStream is, MutableFullBlockChange change) throws IOException {
                    change.from = is.readVarInt();
                    change.to = BlockTypes.AIR.getInternalId();
                }
            };
        }
        if (mode == 1 || mode == 4) { // small
            posDel = new FaweStreamPositionDelegate() {
                final byte[] buffer = new byte[4];
                int lx;
                int ly;
                int lz;

                @Override
                public void write(OutputStream out, int x, int y, int z) throws IOException {
                    if (y < 0 || y > 255) {
                        throw new FaweSmallEditUnsupportedException();
                    }
                    int rx = -lx + (lx = x);
                    int ry = -ly + (ly = y);
                    int rz = -lz + (lz = z);
                    byte b1 = (byte) (ry);
                    byte b2 = (byte) (rx);
                    byte b3 = (byte) (rz);
                    int x16 = (rx >> 8) & 0xF;
                    int z16 = (rz >> 8) & 0xF;
                    byte b4 = MathMan.pair16(x16, z16);
                    out.write(b1);
                    out.write(b2);
                    out.write(b3);
                    out.write(b4);
                }

                @Override
                public void read(final FaweInputStream in, final BlockPositionChange change) throws IOException {
                    in.readFully(buffer);
                    change.x = lx = lx + ((((buffer[1] & 0xFF) | ((MathMan.unpair16x(buffer[3])) << 8)) << 20) >> 20);
                    change.y = (ly = ly + buffer[0]) & 0xFF;
                    change.z = lz = lz + ((((buffer[2] & 0xFF) | ((MathMan.unpair16y(buffer[3])) << 8)) << 20) >> 20);
                }

            };
        } else {
            posDel = new FaweStreamPositionDelegate() {
                final byte[] buffer = new byte[6];
                int lx;
                int ly;
                int lz;

                @Override
                public void write(OutputStream stream, int x, int y, int z) throws IOException {
                    int rx = -lx + (lx = x);
                    int ry = -ly + (ly = y);
                    int rz = -lz + (lz = z);
                    // Use LE/GE to ensure we don't accidentally write MAGIC_NEW_RELATIVE
                    if (rx >= Short.MAX_VALUE || rz >= Short.MAX_VALUE || rx <= Short.MIN_VALUE || rz <= Short.MIN_VALUE) {
                        stream.write(MAGIC_NEW_RELATIVE);
                        stream.write((byte) (x >> 24));
                        stream.write((byte) (x >> 16));
                        stream.write((byte) (x >> 8));
                        stream.write((byte) (x));
                        stream.write((byte) (z >> 24));
                        stream.write((byte) (z >> 16));
                        stream.write((byte) (z >> 8));
                        stream.write((byte) (z));
                        rx = 0;
                        rz = 0;
                    }
                    stream.write((rx) & 0xff);
                    stream.write(((rx) >> 8) & 0xff);
                    stream.write((rz) & 0xff);
                    stream.write(((rz) >> 8) & 0xff);
                    stream.write((ry) & 0xff);
                    stream.write(((ry) >> 8) & 0xff);
                }

                @Override
                public void read(final FaweInputStream in, final BlockPositionChange change) throws IOException {
                    in.readFully(buffer);
                    change.x = lx = lx + ((buffer[0] & 0xFF) | (buffer[1] << 8));
                    change.z = lz = lz + ((buffer[2] & 0xFF) | (buffer[3]) << 8);
                    change.y = ly = ly + ((buffer[4] & 0xFF) | (buffer[5]) << 8);
                }
            };
        }
    }

    public void writeHeader(OutputStream os, int x, int y, int z) throws IOException {
        os.write(mode);
        // Allows for version detection of history in case of changes to format.
        os.write(VERSION);
        setOrigin(x, z);
        os.write((byte) (x >> 24));
        os.write((byte) (x >> 16));
        os.write((byte) (x >> 8));
        os.write((byte) (x));
        os.write((byte) (z >> 24));
        os.write((byte) (z >> 16));
        os.write((byte) (z >> 8));
        os.write((byte) (z));
        setupStreamDelegates(mode);
    }

    public void readHeader(InputStream is) throws IOException {
        // skip mode
        int mode = is.read();
        version = is.read();
        if (version != 1 && version != VERSION) { // version 1 is fine
            throw new UnsupportedOperationException(String.format("Version %s history not supported!", version));
        }
        // origin
        int x = ((is.read() << 24) + (is.read() << 16) + (is.read() << 8) + is.read());
        int z = ((is.read() << 24) + (is.read() << 16) + (is.read() << 8) + is.read());
        setOrigin(x, z);
        setupStreamDelegates(mode);
    }

    public FaweOutputStream getCompressedOS(OutputStream os) throws IOException {
        return MainUtil.getCompressedOS(os, compression);
    }

    @Override
    public boolean isEmpty() {
        if (blockSize > 0) {
            return false;
        }
        if (!super.isEmpty()) {
            return false;
        }
        flush();
        return blockSize == 0;
    }

    @Override
    public long longSize() {
        // Flush so we can accurately get the size
        flush();
        return blockSize;
    }

    @Override
    public int size() {
        return (int) longSize();
    }

    public abstract int getCompressedSize();

    public abstract long getSizeInMemory();

    public long getSizeOnDisk() {
        return 0;
    }

    public abstract FaweOutputStream getBlockOS(int x, int y, int z) throws IOException;

    public abstract FaweOutputStream getBiomeOS() throws IOException;

    public abstract NBTOutputStream getEntityCreateOS() throws IOException;

    public abstract NBTOutputStream getEntityRemoveOS() throws IOException;

    public abstract NBTOutputStream getTileCreateOS() throws IOException;

    public abstract NBTOutputStream getTileRemoveOS() throws IOException;

    public abstract FaweInputStream getBlockIS() throws IOException;

    public abstract FaweInputStream getBiomeIS() throws IOException;

    public abstract NBTInputStream getEntityCreateIS() throws IOException;

    public abstract NBTInputStream getEntityRemoveIS() throws IOException;

    public abstract NBTInputStream getTileCreateIS() throws IOException;

    public abstract NBTInputStream getTileRemoveIS() throws IOException;

    public void setOrigin(int x, int z) {
        originX = x;
        originZ = z;
    }

    public int getOriginX() {
        return originX;
    }

    public int getOriginZ() {
        return originZ;
    }

    @Override
    public void add(int x, int y, int z, int combinedFrom, int combinedTo) {
        blockSize++;
        try {
            FaweOutputStream stream = getBlockOS(x, y, z);
            //x
            posDel.write(stream, x - originX, y, z - originZ);
            idDel.writeChange(stream, combinedFrom, combinedTo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addBiomeChange(int bx, int by, int bz, BiomeType from, BiomeType to) {
        blockSize++;
        try {
            int x = bx >> 2;
            int y = by >> 2;
            int z = bz >> 2;
            FaweOutputStream os = getBiomeOS();
            os.write((byte) (x >> 24));
            os.write((byte) (x >> 16));
            os.write((byte) (x >> 8));
            os.write((byte) (x));
            os.write((byte) (z >> 24));
            os.write((byte) (z >> 16));
            os.write((byte) (z >> 8));
            os.write((byte) (z));
            // only need to store biomes in the 4x4x4 chunks so only need one byte for y still (signed byte -128 -> 127)
            //  means -512 -> 508. Add 128 to avoid negative value casting.
            os.write((byte) (y + 128));
            os.writeVarInt(from.getInternalId());
            os.writeVarInt(to.getInternalId());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addTileCreate(final FaweCompoundTag tag) {
        blockSize++;
        try {
            NBTOutputStream nbtos = getTileCreateOS();
            nbtos.writeTag(new CompoundTag(tag.linTag()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addTileRemove(final FaweCompoundTag tag) {
        blockSize++;
        try {
            NBTOutputStream nbtos = getTileRemoveOS();
            nbtos.writeTag(new CompoundTag(tag.linTag()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addEntityRemove(final FaweCompoundTag tag) {
        blockSize++;
        try {
            NBTOutputStream nbtos = getEntityRemoveOS();
            nbtos.writeTag(new CompoundTag(tag.linTag()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addEntityCreate(final FaweCompoundTag tag) {
        blockSize++;
        try {
            NBTOutputStream nbtos = getEntityCreateOS();
            nbtos.writeTag(new CompoundTag(tag.linTag()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Iterator<MutableBlockChange> getBlockIterator(final boolean dir) throws IOException {
        final FaweInputStream is = getBlockIS();
        if (is == null) {
            return Collections.emptyIterator();
        }
        final MutableBlockChange change = new MutableBlockChange(0, 0, 0, BlockTypes.AIR.getInternalId());
        return new Iterator<MutableBlockChange>() {
            private MutableBlockChange last = read();

            public MutableBlockChange read() {
                try {
                    posDel.read(is, change);
                    change.x += originX;
                    change.z += originZ;
                    idDel.readCombined(is, change, dir);
                    return change;
                } catch (EOFException ignored) {
                } catch (Exception e) {
                    e.printStackTrace();
                    e.printStackTrace();
                }
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public boolean hasNext() {
                return last != null || ((last = read()) != null);
            }

            @Override
            public MutableBlockChange next() {
                MutableBlockChange tmp = last;
                if (tmp == null) {
                    tmp = read();
                }
                last = null;
                return tmp;
            }

            @Override
            public void remove() {
                throw new IllegalArgumentException("CANNOT REMOVE");
            }
        };
    }

    public Iterator<MutableBiomeChange> getBiomeIterator(final boolean dir) throws IOException {
        final FaweInputStream is = getBiomeIS();
        if (is == null) {
            return Collections.emptyIterator();
        }
        final MutableBiomeChange change = new MutableBiomeChange();
        return new Iterator<MutableBiomeChange>() {
            private MutableBiomeChange last = new MutableBiomeChange();

            public MutableBiomeChange read() {
                try {
                    int int1 = is.read();
                    if (int1 != -1) {
                        int x = ((int1 << 24) + (is.read() << 16) + (is.read() << 8) + is.read()) << 2;
                        int z = ((is.read() << 24) + (is.read() << 16) + (is.read() << 8) + is.read()) << 2;
                        int y = (is.read() - 128) << 2;
                        int from = is.readVarInt();
                        int to = is.readVarInt();
                        change.setBiome(x, y, z, from, to);
                        return change;
                    }
                } catch (EOFException ignored) {
                } catch (Exception e) {
                    e.printStackTrace();
                    e.printStackTrace();
                }
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public boolean hasNext() {
                return last != null || ((last = read()) != null);
            }

            @Override
            public MutableBiomeChange next() {
                MutableBiomeChange tmp = last;
                if (tmp == null) {
                    tmp = read();
                }
                last = null;
                return tmp;
            }

            @Override
            public void remove() {
                throw new IllegalArgumentException("CANNOT REMOVE");
            }
        };
    }

    @Override
    public Iterator<Change> getIterator(BlockBag blockBag, int mode, boolean redo) {
        if (blockBag != null && mode > 0) {
            try {
                return (Iterator<Change>) (Iterator<?>) getFullBlockIterator(blockBag, mode, redo);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return getIterator(redo);
    }

    public Iterator<MutableFullBlockChange> getFullBlockIterator(BlockBag blockBag, int inventory, final boolean dir) throws
            IOException {
        final FaweInputStream is = new FaweInputStream(getBlockIS());
        final MutableFullBlockChange change = new MutableFullBlockChange(blockBag, inventory, dir);
        return new Iterator<MutableFullBlockChange>() {
            private MutableFullBlockChange last = read();

            public MutableFullBlockChange read() {
                try {
                    posDel.read(is, change);
                    change.x += originX;
                    change.z += originZ;
                    idDel.readCombined(is, change);
                    return change;
                } catch (EOFException ignored) {
                } catch (Exception e) {
                    e.printStackTrace();
                    e.printStackTrace();
                }
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public boolean hasNext() {
                return last != null || ((last = read()) != null);
            }

            @Override
            public MutableFullBlockChange next() {
                MutableFullBlockChange tmp = last;
                if (tmp == null) {
                    tmp = read();
                }
                last = null;
                return tmp;
            }

            @Override
            public void remove() {
                throw new IllegalArgumentException("CANNOT REMOVE");
            }
        };
    }

    public Iterator<MutableEntityChange> getEntityIterator(final NBTInputStream is, final boolean create) {
        if (is == null) {
            return Collections.emptyIterator();
        }
        final MutableEntityChange change = new MutableEntityChange(null, create);
        try {
            return new Iterator<MutableEntityChange>() {
                private MutableEntityChange last = read();

                public MutableEntityChange read() {
                    try {
                        change.tag = (CompoundTag) is.readTag();
                        return change;
                    } catch (Exception ignored) {
                    }
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }

                @Override
                public boolean hasNext() {
                    return last != null || ((last = read()) != null);
                }

                @Override
                public MutableEntityChange next() {
                    MutableEntityChange tmp = last;
                    if (tmp == null) {
                        tmp = read();
                    }
                    last = null;
                    return tmp;
                }

                @Override
                public void remove() {
                    throw new IllegalArgumentException("CANNOT REMOVE");
                }
            };
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Iterator<MutableTileChange> getTileIterator(final NBTInputStream is, final boolean create) {
        if (is == null) {
            return Collections.emptyIterator();
        }
        final MutableTileChange change = new MutableTileChange(null, create);
        try {
            return new Iterator<MutableTileChange>() {
                private MutableTileChange last = read();

                public MutableTileChange read() {
                    try {
                        change.tag = (CompoundTag) is.readTag();
                        return change;
                    } catch (Exception ignored) {
                    }
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }

                @Override
                public boolean hasNext() {
                    return last != null || ((last = read()) != null);
                }

                @Override
                public MutableTileChange next() {
                    MutableTileChange tmp = last;
                    if (tmp == null) {
                        tmp = read();
                    }
                    last = null;
                    return tmp;
                }

                @Override
                public void remove() {
                    throw new IllegalArgumentException("CANNOT REMOVE");
                }
            };
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public ChangeExchangeCoordinator getCoordinatedChanges(BlockBag blockBag, int mode, boolean dir) {
        try {
            return coordinatedChanges(blockBag, mode, dir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ChangeExchangeCoordinator coordinatedChanges(final BlockBag blockBag, final int mode, boolean dir) throws IOException {
        close();
        var tileCreate = tileChangePopulator(getTileCreateIS(), true);
        var tileRemove = tileChangePopulator(getTileRemoveIS(), false);

        var entityCreate = entityChangePopulator(getEntityCreateIS(), true);
        var entityRemove = entityChangePopulator(getEntityRemoveIS(), false);

        var blockChange = blockBag != null && mode > 0 ? fullBlockChangePopulator(blockBag, mode, dir) : blockChangePopulator(dir);

        var biomeChange = biomeChangePopulator(dir);

        Queue<ChangePopulator<?>> populators = new ArrayDeque<>(List.of(
                tileCreate,
                tileRemove,
                entityCreate,
                entityRemove,
                blockChange,
                biomeChange
        ));
        BiConsumer<Exchanger<Change[]>, Change[]> task = (exchanger, array) -> {
            while (fillArray(array, populators)) {
                try {
                    array = exchanger.exchange(array);
                } catch (InterruptedException e) {
                    return;
                }
            }
        };
        return new ChangeExchangeCoordinator(task);
    }

    private boolean fillArray(Change[] changes, Queue<ChangePopulator<?>> populators) {
        ChangePopulator<?> populator = populators.peek();
        if (populator == null) {
            return false;
        }
        for (int i = 0; i < changes.length; i++) {
            Change change = changes[i];
            do {
                change = populator.updateOrCreate(change);
                if (change == null) {
                    populators.remove();
                    populator = populators.peek();
                    if (populator == null) {
                        changes[i] = null; // mark end
                        return true; // still needs to consume the elements of the current round
                    }
                } else {
                    break;
                }
            } while (true);
            changes[i] = change;
        }
        return true;
    }

    private static abstract class CompoundTagPopulator<C extends Change> implements ChangePopulator<C> {
        private final NBTInputStream inputStream;

        private CompoundTagPopulator(final NBTInputStream stream) {
            inputStream = stream;
        }

        @Override
        public @Nullable C populate(final @Nonnull C change) {
            try {
                write(change, (CompoundTag) inputStream.readTag());
                return change;
            } catch (Exception ignored) {
            }
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        protected abstract void write(C change, CompoundTag tag);
    }

    private ChangePopulator<MutableTileChange> tileChangePopulator(NBTInputStream is, boolean create) {
        if (is == null) {
            return ChangePopulator.empty();
        }
        class Populator extends CompoundTagPopulator<MutableTileChange> {

            private Populator() {
                super(is);
            }

            @Override
            public @Nonnull MutableTileChange create() {
                return new MutableTileChange(null, create);
            }

            @Override
            protected void write(final MutableTileChange change, final CompoundTag tag) {
                change.tag = tag;
            }

            @Override
            public boolean accepts(final Change change) {
                return change instanceof MutableTileChange;
            }

        }
        return new Populator();
    }
    private ChangePopulator<MutableEntityChange> entityChangePopulator(NBTInputStream is, boolean create) {
        if (is == null) {
            return ChangePopulator.empty();
        }
        class Populator extends CompoundTagPopulator<MutableEntityChange> {

            private Populator() {
                super(is);
            }

            @Override
            public @Nonnull MutableEntityChange create() {
                return new MutableEntityChange(null, create);
            }

            @Override
            protected void write(final MutableEntityChange change, final CompoundTag tag) {
                change.tag = tag;
            }

            @Override
            public boolean accepts(final Change change) {
                return change instanceof MutableTileChange;
            }

        }
        return new Populator();
    }

    private ChangePopulator<MutableFullBlockChange> fullBlockChangePopulator(BlockBag blockBag, int mode, boolean dir) throws
            IOException {
        final FaweInputStream is = getBlockIS();
        if (is == null) {
            return ChangePopulator.empty();
        }
        class Populator implements ChangePopulator<MutableFullBlockChange> {

            @Override
            public @Nonnull MutableFullBlockChange create() {
                return new MutableFullBlockChange(blockBag, mode, dir);
            }

            @Override
            public @Nullable MutableFullBlockChange populate(@Nonnull final MutableFullBlockChange change) {
                try {
                    posDel.read(is, change);
                    idDel.readCombined(is, change);
                    change.x += originX;
                    change.z += originZ;
                    return change;
                } catch (EOFException ignored) {
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public boolean accepts(final Change change) {
                return change instanceof MutableFullBlockChange;
            }

        }
        return new Populator();

    }

    private ChangePopulator<MutableBlockChange> blockChangePopulator(boolean dir) throws IOException {
        final FaweInputStream is = getBlockIS();
        if (is == null) {
            return ChangePopulator.empty();
        }
        class Populator implements ChangePopulator<MutableBlockChange> {

            @Override
            public @Nonnull MutableBlockChange create() {
                return new MutableBlockChange(0, 0, 0, BlockTypes.AIR.getInternalId());
            }

            @Override
            public @Nullable MutableBlockChange populate(@Nonnull final MutableBlockChange change) {
                try {
                    posDel.read(is, change);
                    idDel.readCombined(is, change, dir);
                    change.x += originX;
                    change.z += originZ;
                    return change;
                } catch (EOFException ignored) {
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public boolean accepts(final Change change) {
                return change instanceof MutableBlockChange;
            }

        }
        return new Populator();
    }

    private ChangePopulator<MutableBiomeChange> biomeChangePopulator(boolean dir) throws IOException {
        final FaweInputStream is = getBiomeIS();
        if (is == null) {
            return ChangePopulator.empty();
        }
        class Populator implements ChangePopulator<MutableBiomeChange> {

            @Override
            public @Nonnull MutableBiomeChange create() {
                return new MutableBiomeChange();
            }

            @Override
            public @Nullable MutableBiomeChange populate(@Nonnull final MutableBiomeChange change) {
                try {
                    int int1 = is.read();
                    if (int1 != -1) {
                        int x = ((int1 << 24) + (is.read() << 16) + (is.read() << 8) + is.read()) << 2;
                        int z = ((is.read() << 24) + (is.read() << 16) + (is.read() << 8) + is.read()) << 2;
                        int y = (is.read() - 128) << 2;
                        int from = is.readVarInt();
                        int to = is.readVarInt();
                        change.setBiome(x, y, z, from, to);
                        return change;
                    }
                } catch (EOFException ignored) {
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public boolean accepts(final Change change) {
                return change instanceof MutableBiomeChange;
            }

        }
        return new Populator();
    }

    @Override
    public Iterator<Change> getIterator(final boolean dir) {
        try {
            close();
            final Iterator<MutableTileChange> tileCreate = getTileIterator(getTileCreateIS(), true);
            final Iterator<MutableTileChange> tileRemove = getTileIterator(getTileRemoveIS(), false);

            final Iterator<MutableEntityChange> entityCreate = getEntityIterator(getEntityCreateIS(), true);
            final Iterator<MutableEntityChange> entityRemove = getEntityIterator(getEntityRemoveIS(), false);

            final Iterator<MutableBlockChange> blockChange = getBlockIterator(dir);

            final Iterator<MutableBiomeChange> biomeChange = getBiomeIterator(dir);

            return new Iterator<>() {
                final Iterator<Change>[] iterators = new Iterator[]{tileCreate, tileRemove, entityCreate, entityRemove, blockChange, biomeChange};
                int i = 0;
                Iterator<Change> current = iterators[0];

                @Override
                public boolean hasNext() {
                    if (current.hasNext()) {
                        return true;
                    } else if (i >= iterators.length - 1) {
                        return false;
                    } else {
                        current = iterators[++i];
                    }
                    return hasNext();
                }

                @Override
                public void remove() {
                    current.remove();
                }

                @Override
                public Change next() {
                    try {
                        return current.next();
                    } catch (Throwable ignored) {
                        if (i >= iterators.length - 1) {
                            throw new NoSuchElementException("End of iterator");
                        }
                        current = iterators[++i];
                        return next();
                    }
                }
            };
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyIterator();
    }

    @Override
    public Iterator<Change> backwardIterator() {
        return getIterator(false);
    }

    @Override
    public Iterator<Change> forwardIterator() {
        return getIterator(true);
    }

    protected SimpleChangeSetSummary summarizeShallow() {
        return new SimpleChangeSetSummary(getOriginX(), getOriginZ());
    }

    @Override
    public SimpleChangeSetSummary summarize(Region region, boolean shallow) {
        int ox = getOriginX();
        int oz = getOriginZ();
        SimpleChangeSetSummary summary = summarizeShallow();
        if (region != null && !region.contains(ox, oz)) {
            return summary;
        }
        try (FaweInputStream fis = getBlockIS()) {
            if (!shallow) {
                int amount = (Settings.settings().HISTORY.BUFFER_SIZE - HEADER_SIZE) / 9;
                MutableFullBlockChange change = new MutableFullBlockChange(null, 0, false);
                for (int i = 0; i < amount; i++) {
                    posDel.read(fis, change);
                    idDel.readCombined(fis, change);
                    summary.add(change.x + ox, change.z + oz, change.to);
                }
            }
        } catch (EOFException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
        }
        return summary;
    }

}
