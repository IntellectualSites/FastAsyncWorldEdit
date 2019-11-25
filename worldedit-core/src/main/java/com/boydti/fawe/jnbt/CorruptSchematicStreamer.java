package com.boydti.fawe.jnbt;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.clipboard.CPUOptimizedClipboard;
import com.boydti.fawe.object.clipboard.DiskOptimizedClipboard;
import com.boydti.fawe.object.clipboard.LinearClipboard;
import com.boydti.fawe.object.clipboard.MemoryOptimizedClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorruptSchematicStreamer {

    private final Logger log = LoggerFactory.getLogger(CorruptSchematicStreamer.class);
    private final InputStream stream;
    private final UUID uuid;
    private LinearClipboard fc;
    final AtomicInteger volume = new AtomicInteger();
    final AtomicInteger width = new AtomicInteger();
    final AtomicInteger height = new AtomicInteger();
    final AtomicInteger length = new AtomicInteger();
    final AtomicInteger offsetX = new AtomicInteger();
    final AtomicInteger offsetY = new AtomicInteger();
    final AtomicInteger offsetZ = new AtomicInteger();
    final AtomicInteger originX = new AtomicInteger();
    final AtomicInteger originY = new AtomicInteger();
    final AtomicInteger originZ = new AtomicInteger();

    public CorruptSchematicStreamer(InputStream rootStream, UUID uuid) {
        this.stream = rootStream;
        this.uuid = uuid;
    }

    public void match(String matchTag, CorruptReader reader) {
        try {
            stream.reset();
            stream.mark(Integer.MAX_VALUE);
            DataInputStream dataInput = new DataInputStream(new BufferedInputStream(new GZIPInputStream(stream)));
            byte[] match = matchTag.getBytes();
            int[] matchValue = new int[match.length];
            int matchIndex = 0;
            int read;
            while ((read = dataInput.read()) != -1) {
                int expected = match[matchIndex];
                if (expected == -1) {
                    if (++matchIndex == match.length) {
                        break;
                    }
                } else if (read == expected) {
                    if (++matchIndex == match.length) {
                        reader.run(dataInput);
                        break;
                    }
                } else {
                    if (matchIndex == 2)
                        matchIndex = 0;
                }
            }
            log.debug(" - Recover " + matchTag + " = success");
        } catch (Throwable e) {
            log.error(" - Recover " + matchTag + " = partial failure", e);
        }
    }

    public LinearClipboard setupClipboard() {
        if (fc != null) {
            return fc;
        }
        BlockVector3 dimensions = guessDimensions(volume.get(), width.get(), height.get(), length.get());
        if (width.get() == 0 || height.get() == 0 || length.get() == 0) {
            log.debug("No dimensions found! Estimating based on factors:" + dimensions);
        }
        if (Settings.IMP.CLIPBOARD.USE_DISK) {
            fc = new DiskOptimizedClipboard(dimensions, uuid);
        } else if (Settings.IMP.CLIPBOARD.COMPRESSION_LEVEL == 0) {
            fc = new CPUOptimizedClipboard(dimensions);
        } else {
            fc = new MemoryOptimizedClipboard(dimensions);
        }
        return fc;
    }

    public Clipboard recover() {
        // TODO FIXME
        throw new UnsupportedOperationException("TODO FIXME");
//        try {
//            if (stream == null || !stream.markSupported()) {
//                throw new IllegalArgumentException("Can only recover from a marked and resettable stream!");
//            }
//            match("Width", new CorruptSchematicStreamer.CorruptReader() {
//                @Override
//                public void run(DataInputStream in) throws IOException {
//                    width.set(in.readShort());
//                }
//            });
//            match("Height", new CorruptSchematicStreamer.CorruptReader() {
//                @Override
//                public void run(DataInputStream in) throws IOException {
//                    height.set(in.readShort());
//                }
//            });
//            match("Length", new CorruptSchematicStreamer.CorruptReader() {
//                @Override
//                public void run(DataInputStream in) throws IOException {
//                    length.set(in.readShort());
//                }
//            });
//            match("WEOffsetX", new CorruptSchematicStreamer.CorruptReader() {
//                @Override
//                public void run(DataInputStream in) throws IOException {
//                    offsetX.set(in.readInt());
//                }
//            });
//            match("WEOffsetY", new CorruptSchematicStreamer.CorruptReader() {
//                @Override
//                public void run(DataInputStream in) throws IOException {
//                    offsetY.set(in.readInt());
//                }
//            });
//            match("WEOffsetZ", new CorruptSchematicStreamer.CorruptReader() {
//                @Override
//                public void run(DataInputStream in) throws IOException {
//                    offsetZ.set(in.readInt());
//                }
//            });
//            match("WEOriginX", new CorruptSchematicStreamer.CorruptReader() {
//                @Override
//                public void run(DataInputStream in) throws IOException {
//                    originX.set(in.readInt());
//                }
//            });
//            match("WEOriginY", new CorruptSchematicStreamer.CorruptReader() {
//                @Override
//                public void run(DataInputStream in) throws IOException {
//                    originY.set(in.readInt());
//                }
//            });
//            match("WEOriginZ", new CorruptSchematicStreamer.CorruptReader() {
//                @Override
//                public void run(DataInputStream in) throws IOException {
//                    originZ.set(in.readInt());
//                }
//            });
//            match("Blocks", new CorruptSchematicStreamer.CorruptReader() {
//                @Override
//                public void run(DataInputStream in) throws IOException {
//                    int length = in.readInt();
//                    volume.set(length);
//                    setupClipboard();
//                    for (int i = 0; i < length; i++) {
//                        fc.setId(i, in.read());
//                    }
//                }
//            });
//            match("Data", new CorruptSchematicStreamer.CorruptReader() {
//                @Override
//                public void run(DataInputStream in) throws IOException {
//                    int length = in.readInt();
//                    volume.set(length);
//                    setupClipboard();
//                    for (int i = 0; i < length; i++) {
//                        fc.setData(i, in.read());
//                    }
//                }
//            });
//            match("AddBlocks", new CorruptSchematicStreamer.CorruptReader() {
//                @Override
//                public void run(DataInputStream in) throws IOException {
//                    int length = in.readInt();
//                    int expected = volume.get();
//                    if (expected == 0) {
//                        expected = length * 2;
//                        volume.set(expected);
//                    }
//                    setupClipboard();
//                    if (expected == length * 2) {
//                        for (int i = 0; i < length; i++) {
//                            int value = in.read();
//                            int first = value & 0x0F;
//                            int second = (value & 0xF0) >> 4;
//                            int gIndex = i << 1;
//                            if (first != 0) fc.setAdd(gIndex, first);
//                            if (second != 0) fc.setAdd(gIndex + 1, second);
//                        }
//                    } else {
//                        for (int i = 0; i < length; i++) {
//                            int value = in.read();
//                            if (value != 0) fc.setAdd(i, value);
//                        }
//                    }
//                }
//            });
//            match("Biomes", new CorruptSchematicStreamer.CorruptReader() {
//                @Override
//                public void run(DataInputStream in) throws IOException {
//                    int length = in.readInt();
//                    for (int i = 0; i < length; i++) {
//                        fc.setBiome(i, in.read());
//                    }
//                }
//            });
//            Vector dimensions = guessDimensions(volume.get(), width.get(), height.get(), length.get());
//            Vector min = new Vector(originX.get(), originY.get(), originZ.get());
//            Vector offset = new Vector(offsetX.get(), offsetY.get(), offsetZ.get());
//            Vector origin = min.subtract(offset);
//            CuboidRegion region = new CuboidRegion(min, min.add(dimensions.getBlockX(), dimensions.getBlockY(), dimensions.getBlockZ()).subtract(Vector.ONE));
//            fc.setOrigin(offset);
//            final BlockArrayClipboard clipboard = new BlockArrayClipboard(region, fc);
//            match("TileEntities", new CorruptSchematicStreamer.CorruptReader() {
//                @Override
//                public void run(DataInputStream in) throws IOException {
//                    int childType = in.readByte();
//                    int length = in.readInt();
//                    NBTInputStream nis = new NBTInputStream(in);
//                    for (int i = 0; i < length; ++i) {
//                        CompoundTag tag = (CompoundTag) nis.readTagPayload(childType, 1);
//                        int x = tag.getInt("x");
//                        int y = tag.getInt("y");
//                        int z = tag.getInt("z");
//                        fc.setTile(x, y, z, tag);
//                    }
//                }
//            });
//            match("Entities", new CorruptSchematicStreamer.CorruptReader() {
//                @Override
//                public void run(DataInputStream in) throws IOException {
//                    int childType = in.readByte();
//                    int length = in.readInt();
//                    NBTInputStream nis = new NBTInputStream(in);
//                    for (int i = 0; i < length; ++i) {
//                        CompoundTag tag = (CompoundTag) nis.readTagPayload(childType, 1);
//                        int x = tag.getInt("x");
//                        int y = tag.getInt("y");
//                        int z = tag.getInt("z");
//                        String id = tag.getString("id");
//                        if (id.isEmpty()) {
//                            return;
//                        }
//                        ListTag positionTag = tag.getListTag("Pos");
//                        ListTag directionTag = tag.getListTag("Rotation");
//                        BaseEntity state = new BaseEntity(id, tag);
//                        fc.createEntity(clipboard, positionTag.asDouble(0), positionTag.asDouble(1), positionTag.asDouble(2), (float) directionTag.asDouble(0), (float) directionTag.asDouble(1), state);
//                    }
//                }
//            });
//            return clipboard;
//        } catch (Throwable e) {
//            if (fc != null) fc.close();
//            throw e;
//        }
    }

    private BlockVector3 guessDimensions(int volume, int width, int height, int length) {
        if (volume == 0) {
            return BlockVector3.at(width, height, length);
        }
        if (volume == width * height * length) {
            return BlockVector3.at(width, height, length);
        }
        if (width == 0 && height != 0 && length != 0 && volume % (height * length) == 0 && height * length <= volume) {
            return BlockVector3.at(volume / (height * length), height, length);
        }
        if (height == 0 && width != 0 && length != 0 && volume % (width * length) == 0 && width * length <= volume) {
            return BlockVector3.at(width, volume / (width * length), length);
        }
        if (length == 0 && height != 0 && width != 0 && volume % (height * width) == 0 && height * width <= volume) {
            return BlockVector3.at(width, height, volume / (width * height));
        }
        List<Integer> factors = new ArrayList<>();
        for (int i = (int) Math.sqrt(volume); i > 0; i--) {
            if (volume % i == 0) {
                factors.add(i);
                factors.add(volume / i);
            }
        }
        int min = Integer.MAX_VALUE;
        int vx = 0, vy = 0, vz = 0;
        for (int x = 0; x < factors.size(); x++) {
            int xValue = factors.get(x);
            for (int yValue : factors) {
                long area = xValue * yValue;
                if (volume % area == 0) {
                    int z = (int) (volume / area);
                    int max = Math.max(Math.max(xValue, yValue), z);
                    if (max < min) {
                        min = max;
                        vx = xValue;
                        vz = z;
                        vy = yValue;
                    }
                }
            }
        }
        return BlockVector3.at(vx, vz, vy);
    }

    public interface CorruptReader {
        void run(DataInputStream in) throws IOException;
    }
}
