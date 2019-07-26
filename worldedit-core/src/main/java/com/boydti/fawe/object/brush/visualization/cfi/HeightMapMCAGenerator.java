package com.boydti.fawe.object.brush.visualization.cfi;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.object.FaweInputStream;
import com.boydti.fawe.object.FaweOutputStream;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.Metadatable;
import com.boydti.fawe.object.brush.visualization.VirtualWorld;
import com.boydti.fawe.object.change.StreamChange;
import com.boydti.fawe.object.changeset.CFIChangeSet;
import com.boydti.fawe.object.collection.DifferentialArray;
import com.boydti.fawe.object.collection.DifferentialBlockBuffer;
import com.boydti.fawe.object.collection.IterableThreadLocal;
import com.boydti.fawe.object.collection.LocalBlockVector2DSet;
import com.boydti.fawe.object.collection.SummedAreaTable;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.object.schematic.Schematic;
import com.boydti.fawe.util.CachedTextureUtil;
import com.boydti.fawe.util.RandomTextureUtil;
import com.boydti.fawe.util.ReflectionUtils;
import com.boydti.fawe.util.TextureUtil;
import com.boydti.fawe.util.image.Drawable;
import com.boydti.fawe.util.image.ImageViewer;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.registry.state.PropertyKey;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BlockID;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nullable;

// TODO FIXME
public class HeightMapMCAGenerator extends MCAWriter implements StreamChange, Drawable, VirtualWorld {
    private final MutableBlockVector3 mutable = new MutableBlockVector3();

    private final ThreadLocal<int[]> indexStore = new ThreadLocal<int[]>() {
        @Override
        protected int[] initialValue() {
            return new int[256];
        }
    };

    private final DifferentialBlockBuffer blocks;
    protected final DifferentialArray<byte[]> heights;
    protected final DifferentialArray<byte[]> biomes;
    protected final DifferentialArray<int[]> floor;
    protected final DifferentialArray<int[]> main;
    protected DifferentialArray<int[]> overlay;

    protected final CFIPrimtives primtives = new CFIPrimtives();
    private CFIPrimtives oldPrimitives = new CFIPrimtives();

    public final class CFIPrimtives implements Cloneable {
        protected int waterHeight = 0;
        protected int floorThickness = 0;
        protected int worldThickness = 0;
        protected boolean randomVariation = true;
        protected int biomePriority = 0;
        protected int waterId = BlockID.WATER;
        protected int bedrockId = BlockID.BEDROCK;
        protected boolean modifiedMain = false;

        @Override
        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof CFIPrimtives)) {
                return false;
            }
            try {
                for (Field field : CFIPrimtives.class.getDeclaredFields()) {
                    if (field.get(this) != field.get(obj)) return false;
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return true;
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }


    protected Metadatable metaData = new Metadatable();
    protected TextureUtil textureUtil;

    @Override
    public void flushChanges(FaweOutputStream out) throws IOException {
        heights.flushChanges(out);
        biomes.flushChanges(out);
        floor.flushChanges(out);
        main.flushChanges(out);
        out.writeBoolean(overlay != null);
        if (overlay != null) overlay.flushChanges(out);
        try {
            for (Field field : ReflectionUtils.sortFields(CFIPrimtives.class.getDeclaredFields())) {
                Object now = field.get(primtives);
                Object old = field.get(oldPrimitives);
                boolean diff = old != now;
                out.writeBoolean(diff);
                if (diff) {
                    out.writePrimitive(old);
                    out.writePrimitive(now);
                }
            }
            resetPrimtives();
        } catch (Throwable neverHappens) {
            neverHappens.printStackTrace();
        }

        blocks.flushChanges(out);
    }

    public boolean isModified() {
        return blocks.isModified() ||
                heights.isModified() ||
                biomes.isModified() ||
                (overlay != null && overlay.isModified()) ||
                !primtives.equals(oldPrimitives);
    }

    private void resetPrimtives() throws CloneNotSupportedException {
        oldPrimitives = (CFIPrimtives) primtives.clone();
    }

    @Override
    public void undoChanges(FaweInputStream in) throws IOException {
        heights.undoChanges(in);
        biomes.undoChanges(in);
        floor.undoChanges(in);
        main.undoChanges(in);
        if (in.readBoolean()) overlay.undoChanges(in);
        try {
            for (Field field : ReflectionUtils.sortFields(CFIPrimtives.class.getDeclaredFields())) {
                if (in.readBoolean()) {
                    field.set(primtives, in.readPrimitive(field.getType())); // old
                    in.readPrimitive(field.getType()); // new
                }
            }
            resetPrimtives();
        } catch (Throwable neverHappens) {
            neverHappens.printStackTrace();
        }
        blocks.undoChanges(in);
    }

    @Override
    public void redoChanges(FaweInputStream in) throws IOException {
        heights.redoChanges(in);
        biomes.redoChanges(in);
        floor.redoChanges(in);
        main.redoChanges(in);
        if (in.readBoolean()) overlay.redoChanges(in);

        try {
            for (Field field : ReflectionUtils.sortFields(CFIPrimtives.class.getDeclaredFields())) {
                if (in.readBoolean()) {
                    in.readPrimitive(field.getType()); // old
                    field.set(primtives, in.readPrimitive(field.getType())); // new
                }
            }
            resetPrimtives();
        } catch (Throwable neverHappens) {
            neverHappens.printStackTrace();
        }

        blocks.clearChanges(); // blocks.redoChanges(in); Unsupported
    }

//    @Override TODO NOT IMPLEMENTED
    public void addEditSession(EditSession session) {
        session.setFastMode(true);
        this.editSession = session;
    }

    // Used for visualizing the world on a map
    private ImageViewer viewer;
    // Used for visualizing the world by sending chunk packets
    // These three variables should be set together
//    private IQueueExtent packetQueue;
    private FawePlayer player;
    private BlockVector2 chunkOffset = BlockVector2.ZERO;
    private EditSession editSession;
    // end

    public HeightMapMCAGenerator(BufferedImage img, File regionFolder) {
        this(img.getWidth(), img.getHeight(), regionFolder);
        setHeight(img);
    }

    public HeightMapMCAGenerator(int width, int length, File regionFolder) {
        super(width, length, regionFolder);

        blocks = new DifferentialBlockBuffer(width, length);
        heights = new DifferentialArray(new byte[getArea()]);
        biomes = new DifferentialArray(new byte[getArea()]);
        floor = new DifferentialArray(new int[getArea()]);
        main = new DifferentialArray(new int[getArea()]);

        int stone = BlockID.STONE;
        int grass = BlockTypes.GRASS_BLOCK.getDefaultState().with(PropertyKey.SNOWY, false).getInternalId();
        Arrays.fill(main.getIntArray(), stone);
        Arrays.fill(floor.getIntArray(), grass);
    }

    public Metadatable getMetaData() {
        return metaData;
    }

    @Override
    public Vector3 getOrigin() {
        return Vector3.at(chunkOffset.getBlockX() << 4, 0, chunkOffset.getBlockZ() << 4);
    }

    public boolean hasPacketViewer() {
        return player != null;
    }

    public void setPacketViewer(FawePlayer player) {
        this.player = player;
        if (player != null) {
            Location pos = player.getLocation();
            this.chunkOffset = BlockVector2.at(1 + (pos.getBlockX() >> 4), 1 + (pos.getBlockZ() >> 4));
        }
    }

    public FawePlayer getOwner() {
        return player;
    }

    private int[][][] getChunkArray(int x, int z) {
        int[][][][][] blocksData = blocks.get();
        if (blocksData == null) return null;
        int[][][][] arr = blocksData[z];
        return arr != null ? arr[x] : null;
    }

    public void setImageViewer(ImageViewer viewer) {
        this.viewer = viewer;
    }

    public ImageViewer getImageViewer() {
        return viewer;
    }

    @Override
    public void update() {
        if (viewer != null) {
            viewer.view(this);
        }
//        if (chunkOffset != null && player != null) { TODO NOT IMPLEMENTED
//            IQueueExtent packetQueue = SetQueue.IMP.getNewQueue(player.getWorld(), true, false);
//
//            if (!packetQueue.supports(Capability.CHUNK_PACKETS)) {
//                return;
//            }
//
//            int lenCX = (getWidth() + 15) >> 4;
//            int lenCZ = (getLength() + 15) >> 4;
//
//            int OX = chunkOffset.getBlockX();
//            int OZ = chunkOffset.getBlockZ();
//
//            Location position = player.getLocation();
//            int pcx = (position.getBlockX() >> 4) - OX;
//            int pcz = (position.getBlockZ() >> 4) - OZ;
//
//            int scx = Math.max(0, pcx - 15);
//            int scz = Math.max(0, pcz - 15);
//            int ecx = Math.min(lenCX - 1, pcx + 15);
//            int ecz = Math.min(lenCZ - 1, pcz + 15);
//
//            for (int cz = scz; cz <= ecz; cz++) {
//                for (int cx = scx; cx <= ecx; cx++) {
//                    final int finalCX = cx;
//                    final int finalCZ = cz;
//                    TaskManager.IMP.getPublicForkJoinPool().submit(() -> {
//                        try {
//                            FaweChunk toSend = getSnapshot(finalCX, finalCZ);
//                            toSend.setLoc(HeightMapMCAGenerator.this, finalCX + OX, finalCZ + OZ);
//                            packetQueue.sendChunkUpdate(toSend, player);
//                        } catch (Throwable e) {
//                            e.printStackTrace();
//                        }
//                    });
//                }
//            }
//        }
    }

    public TextureUtil getRawTextureUtil() {
        if (textureUtil == null) {
            textureUtil = Fawe.get().getTextureUtil();
        }
        return this.textureUtil;
    }

    public TextureUtil getTextureUtil() {
        if (textureUtil == null) {
            textureUtil = Fawe.get().getTextureUtil();
        }
        try {
            if (primtives.randomVariation) {
                return new RandomTextureUtil(textureUtil);
            } else if (textureUtil instanceof CachedTextureUtil) {
                return textureUtil;
            } else {
                return new CachedTextureUtil(textureUtil);
            }
        } catch (FileNotFoundException neverHappens) {
            neverHappens.printStackTrace();
            return null;
        }
    }

    public void setBedrockId(int bedrockId) {
        this.primtives.bedrockId = bedrockId;
    }

    public void setFloorThickness(int floorThickness) {
        this.primtives.floorThickness = floorThickness;
    }

    public void setWorldThickness(int height) {
        this.primtives.worldThickness = height;
    }

    public void setWaterHeight(int waterHeight) {
        this.primtives.waterHeight = waterHeight;
    }

    public void setWaterId(int waterId) {
        this.primtives.waterId = waterId;
    }

    public void setTextureRandomVariation(boolean randomVariation) {
        this.primtives.randomVariation = randomVariation;
    }

    public boolean getTextureRandomVariation() {
        return this.primtives.randomVariation;
    }

    public void setTextureUtil(TextureUtil textureUtil) {
        this.textureUtil = textureUtil;
    }

    public void smooth(BufferedImage img, boolean white, int radius, int iterations) {
        smooth(img, null, white, radius, iterations);
    }

    public void smooth(Mask mask, int radius, int iterations) {
        smooth(null, mask, false, radius, iterations);
    }

    public void smooth(BlockVector2 min, BlockVector2 max, int radius, int iterations) {
        int snowLayer = BlockTypes.SNOW.getInternalId();
        int snowBlock = BlockTypes.SNOW_BLOCK.getInternalId();

        int[] floor = this.floor.get();
        byte[] heights = this.heights.get();

        int width = getWidth();
        int length = getLength();

        int minX = min.getBlockX();
        int minZ = min.getBlockZ();

        int maxX = max.getBlockX();
        int maxZ = max.getBlockZ();

        int tableWidth = (maxX - minX + 1);
        int tableLength = (maxZ - minZ + 1);
        int smoothArea = tableWidth * tableLength;

        long[] copy = new long[smoothArea];
        char[] layers = new char[smoothArea];

        SummedAreaTable table = new SummedAreaTable(copy, layers, tableWidth, radius);
        for (int j = 0; j < iterations; j++) {

            { // Copy to table
                int localIndex = 0;
                int zIndex = (minZ * getWidth());
                for (int z = minZ; z <= maxZ; z++, zIndex += getWidth()) {
                    int index = zIndex + minX;
                    for (int x = minX; x <= maxX; x++, index++, localIndex++) {
                        int combined = floor[index];
                        if (BlockTypes.getFromStateId(combined) == BlockTypes.SNOW) {
                            layers[localIndex] = (char) (((heights[index] & 0xFF) << 3) + (floor[index] >> BlockTypes.BIT_OFFSET) - 7);
                        } else {
                            layers[localIndex] = (char) (((heights[index] & 0xFF) << 3));
                        }
                    }
                }
            }
            // Process table
            table.processSummedAreaTable();
            { // Copy from table
                int localIndex = 0;
                int zIndex = (minZ * getWidth());
                for (int z = minZ, localZ = 0; z <= maxZ; z++, localZ++, zIndex += getWidth()) {
                    int index = zIndex + minX;
                    for (int x = minX, localX = 0; x <= maxX; x++, localX++, index++, localIndex++) {
                        int y = heights[index] & 0xFF;
                        int newHeight = table.average(localX, localZ, localIndex);
                        setLayerHeight(index, newHeight);
                    }
                }
            }
        }
    }

    private final void setLayerHeight(int index, int height) {
        int blockHeight = (height) >> 3;
        int layerHeight = (height) & 0x7;
        setLayerHeight(index, blockHeight, layerHeight);
    }

    private final void setLayerHeight(int index, int blockHeight, int layerHeight) {
        int floorState = floor.get()[index];
        BlockType type = BlockTypes.getFromStateId(floorState);
        switch (type.getInternalId()) {
            case BlockID.SNOW:
            case BlockID.SNOW_BLOCK:
                if (layerHeight != 0) {
                    this.heights.setByte(index, (byte) (blockHeight + 1));
                    this.floor.setInt(index, (BlockTypes.SNOW.getInternalId() + layerHeight));
                } else {
                    this.heights.setByte(index, (byte) (blockHeight));
                    this.floor.setInt(index, (BlockTypes.SNOW_BLOCK.getInternalId()));
                }
                break;
            default:
                this.heights.setByte(index, (byte) (blockHeight));
                break;
        }
    }

    private final void setLayerHeightRaw(int index, int height) {
        int blockHeight = (height) >> 3;
        int layerHeight = (height) & 0x7;
        setLayerHeightRaw(index, blockHeight, layerHeight);
    }

    private final void setLayerHeightRaw(int index, int blockHeight, int layerHeight) {
        int floorState = floor.get()[index];
        BlockType type = BlockTypes.getFromStateId(floorState);
        switch (type.getInternalId()) {
            case BlockID.SNOW:
            case BlockID.SNOW_BLOCK:
                if (layerHeight != 0) {
                    this.heights.getByteArray()[index] = (byte) (blockHeight + 1);
                    this.floor.getIntArray()[index] = (BlockTypes.SNOW.getInternalId() + layerHeight);
                } else {
                    this.heights.getByteArray()[index] = (byte) (blockHeight);
                    this.floor.getIntArray()[index] = (BlockTypes.SNOW_BLOCK.getInternalId());
                }
                break;
            default:
                this.heights.getByteArray()[index] = (byte) (blockHeight);
                break;
        }
    }

    private void smooth(BufferedImage img, Mask mask, boolean white, int radius, int iterations) {
        int[] floor = this.floor.get();
        byte[] heights = this.heights.get();

        long[] copy = new long[heights.length];
        char[] layers = new char[heights.length];

        this.floor.record(() -> HeightMapMCAGenerator.this.heights.record(() -> {
            int width = getWidth();
            int length = getLength();
            SummedAreaTable table = new SummedAreaTable(copy, layers, width, radius);
            for (int j = 0; j < iterations; j++) {
                for (int i = 0; i < heights.length; i++) {
                    int combined = floor[i];
                    if (BlockTypes.getFromStateId(combined) == BlockTypes.SNOW) {
                        layers[i] = (char) (((heights[i] & 0xFF) << 3) + (floor[i] >> BlockTypes.BIT_OFFSET) - 7);
                    } else {
                        layers[i] = (char) (((heights[i] & 0xFF) << 3));
                    }
                }
                int index = 0;
                table.processSummedAreaTable();
                if (img != null) {
                    for (int z = 0; z < getLength(); z++) {
                        for (int x = 0; x < getWidth(); x++, index++) {
                            int height = img.getRGB(x, z) & 0xFF;
                            if (height == 255 || height > 0 && !white && ThreadLocalRandom.current()
                                    .nextInt(256) <= height) {
                                int newHeight = table.average(x, z, index);
                                setLayerHeightRaw(index, newHeight);
                            }
                        }
                    }
                } else if (mask != null) {
                    for (int z = 0; z < getLength(); z++) {
                        mutable.mutZ(z);
                        for (int x = 0; x < getWidth(); x++, index++) {
                            int y = heights[index] & 0xFF;
                            mutable.mutX(x);
                            mutable.mutY(y);
                            if (mask.test(mutable)) {
                                int newHeight = table.average(x, z, index);
                                setLayerHeightRaw(index, newHeight);
                            }
                        }
                    }
                } else {
                    for (int z = 0; z < getLength(); z++) {
                        for (int x = 0; x < getWidth(); x++, index++) {
                            int newHeight = table.average(x, z, index);
                            setLayerHeightRaw(index, newHeight);
                        }
                    }
                }
            }
        }));
    }

    public void setHeight(BufferedImage img) {
        int index = 0;
        for (int z = 0; z < getLength(); z++) {
            for (int x = 0; x < getWidth(); x++, index++) {
                heights.setByte(index, (byte) (img.getRGB(x, z) >> 8));
            }
        }
    }

    public void addCaves() throws WorldEditException {
        CuboidRegion region = new CuboidRegion(BlockVector3.at(0, 0, 0), BlockVector3.at(getWidth() -1, 255, getLength() -1));
        addCaves(region);
    }

    @Deprecated
    public void addSchems(Mask mask, List<ClipboardHolder> clipboards, int rarity, boolean rotate) throws WorldEditException {
        CuboidRegion region = new CuboidRegion(BlockVector3.at(0, 0, 0), BlockVector3.at(getWidth() -1, 255, getLength() -1));
        addSchems(region, mask, clipboards, rarity, rotate);
    }

    public void addSchems(BufferedImage img, Mask mask, List<ClipboardHolder> clipboards, int rarity, int distance, boolean randomRotate) throws WorldEditException {
        if (img.getWidth() != getWidth() || img.getHeight() != getLength())
            throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
        double doubleRarity = rarity / 100d;
        int index = 0;
        AffineTransform identity = new AffineTransform();
        LocalBlockVector2DSet placed = new LocalBlockVector2DSet();
        for (int z = 0; z < getLength(); z++) {
            mutable.mutZ(z);
            for (int x = 0; x < getWidth(); x++, index++) {
                int y = heights.getByte(index) & 0xFF;
                int height = img.getRGB(x, z) & 0xFF;
                if (height == 0 || ThreadLocalRandom.current().nextInt(256) > height * doubleRarity) {
                    continue;
                }
                mutable.mutX(x);
                mutable.mutY(y);
                if (!mask.test(mutable)) {
                    continue;
                }
                if (placed.containsRadius(x, z, distance)) {
                    continue;
                }
                placed.add(x, z);
                ClipboardHolder holder = clipboards.get(ThreadLocalRandom.current().nextInt(clipboards.size()));
                if (randomRotate) {
                    int rotate = ThreadLocalRandom.current().nextInt(4) * 90;
                    if (rotate != 0) {
                        holder.setTransform(new AffineTransform().rotateY(ThreadLocalRandom.current().nextInt(4) * 90));
                    } else {
                        holder.setTransform(identity);
                    }
                }
                Clipboard clipboard = holder.getClipboard();
                Schematic schematic = new Schematic(clipboard);
                Transform transform = holder.getTransform();
                if (transform.isIdentity()) {
                    schematic.paste(this, mutable, false);
                } else {
                    schematic.paste(this, mutable, false, transform);
                }
                if (x + distance < getWidth()) {
                    x += distance;
                    index += distance;
                } else {
                    break;
                }
            }
        }
    }

    public void addSchems(Mask mask, List<ClipboardHolder> clipboards, int rarity, int distance, boolean randomRotate) throws WorldEditException {
        int scaledRarity = (256 * rarity) / 100;
        int index = 0;
        AffineTransform identity = new AffineTransform();
        LocalBlockVector2DSet placed = new LocalBlockVector2DSet();
        for (int z = 0; z < getLength(); z++) {
            mutable.mutZ(z);
            for (int x = 0; x < getWidth(); x++, index++) {
                int y = heights.getByte(index) & 0xFF;
                if (ThreadLocalRandom.current().nextInt(256) > scaledRarity) {
                    continue;
                }
                mutable.mutX(x);
                mutable.mutY(y);
                if (!mask.test(mutable)) {
                    continue;
                }
                if (placed.containsRadius(x, z, distance)) {
                    continue;
                }
                mutable.mutY(y + 1);
                placed.add(x, z);
                ClipboardHolder holder = clipboards.get(ThreadLocalRandom.current().nextInt(clipboards.size()));
                if (randomRotate) {
                    int rotate = ThreadLocalRandom.current().nextInt(4) * 90;
                    if (rotate != 0) {
                        holder.setTransform(new AffineTransform().rotateY(ThreadLocalRandom.current().nextInt(4) * 90));
                    } else {
                        holder.setTransform(identity);
                    }
                }
                Clipboard clipboard = holder.getClipboard();
                Schematic schematic = new Schematic(clipboard);
                Transform transform = holder.getTransform();
                if (transform.isIdentity()) {
                    schematic.paste(this, mutable, false);
                } else {
                    schematic.paste(this, mutable, false, transform);
                }
                if (x + distance < getWidth()) {
                    x += distance;
                    index += distance;
                } else {
                    break;
                }
            }
        }
    }

    public void addOre(Mask mask, Pattern material, int size, int frequency, int rarity, int minY, int maxY) throws WorldEditException {
        CuboidRegion region = new CuboidRegion(BlockVector3.at(0, 0, 0), BlockVector3.at(getWidth() -1, 255, getLength() -1));
        addOre(region, mask, material, size, frequency, rarity, minY, maxY);
    }

    public void addDefaultOres(Mask mask) throws WorldEditException {
        addOres(new CuboidRegion(BlockVector3.at(0, 0, 0), BlockVector3.at(getWidth() -1, 255, getLength() -1)), mask);
    }

    @Override
    public BlockVector3 getMinimumPoint() {
        return BlockVector3.at(0, 0, 0);
    }

    @Override
    public FawePlayer getPlayer() {
        return player;
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return BlockVector3.at(getWidth() - 1, 255, getLength() - 1);
    }

    @Override
    public boolean setBlock(BlockVector3 position, BlockStateHolder block) throws WorldEditException {
        return setBlock(position.getBlockX(), position.getBlockY(), position.getBlockZ(), block);
    }

    private boolean setBlock(int x, int y, int z, int combined) {
        int index = z * getWidth() + x;
        if (index < 0 || index >= getArea()) return false;
        int height = heights.getByte(index) & 0xFF;
        switch (y - height) {
            case 0:
                floor.setInt(index, combined);
                return true;
            case 1:
                int mainId = main.getInt(index);
                int floorId = floor.getInt(index);
                floor.setInt(index, combined);

                byte currentHeight = heights.getByte(index);
                currentHeight++;
                heights.setByte(index, currentHeight);
                if (mainId == floorId) return true;
                y--;
                combined = floorId;
            default:
                try {
                    blocks.set(x, y, z, combined);
                    return true;
                } catch (IndexOutOfBoundsException ignore) {
                    return false;
                }
        }
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        int index = z * getWidth() + x;
        if (index < 0 || index >= getArea()) return false;
        biomes.setByte(index, (byte) biome.getInternalId());
        return true;
    }

//    @Override TODO NOT IMPLEMENTED
//    public FaweChunk getFaweChunk(int chunkX, int chunkZ) {
//        return new SimpleIntFaweChunk(this, chunkX, chunkZ);
//    }
//
//    @Override
//    public FaweChunk getSnapshot(int chunkX, int chunkZ) {
//        return getSnapshot(null, chunkX, chunkZ);
//    }
//
//    private FaweChunk getSnapshot(final WritableMCAChunk chunk, int chunkX, int chunkZ) {
//        return new LazyFaweChunk<WritableMCAChunk>(this, chunkX, chunkZ) {
//            @Override
//            public WritableMCAChunk getChunk() {
//                WritableMCAChunk tmp = chunk;
//                if (tmp == null) {
//                    tmp = new WritableMCAChunk();
//                }
//                tmp.setLoc(HeightMapMCAGenerator.this, chunkX, chunkZ);
//                int cbx = chunkX << 4;
//                int cbz = chunkZ << 4;
//                int csx = Math.max(0, cbx);
//                int csz = Math.max(0, cbz);
//                int cex = Math.min(getWidth(), cbx + 15);
//                int cez = Math.min(getLength(), cbz + 15);
//                write(tmp, csx, cex, csz, cez);
//                tmp.setLoc(HeightMapMCAGenerator.this, getX(), getZ());
//                return tmp;
//            }
//
//            @Override
//            public void addToQueue() {
//                WritableMCAChunk cached = getCachedChunk();
//                if (cached != null) setChunk(cached);
//            }
//        };
//    }
//
//    @Override
//    public Collection<FaweChunk> getFaweChunks() {
//        return Collections.emptyList();
//    }
//
//    @Override
//    public void setChunk(FaweChunk chunk) {
//        int[][] src = chunk.getCombinedIdArrays();
//        for (int i = 0; i < src.length; i++) {
//            if (src[i] != null) {
//                int bx = chunk.getX() << 4;
//                int bz = chunk.getZ() << 4;
//                int by = i << 4;
//                for (int layer = i; layer < src.length; layer++) {
//                    int[] srcLayer = src[layer];
//                    if (srcLayer != null) {
//                        int index = 0;
//                        for (int y = 0; y < 16; y++) {
//                            int yy = by + y;
//                            for (int z = 0; z < 16; z++) {
//                                int zz = bz + z;
//                                for (int x = 0; x < 16; x++, index++) {
//                                    int combined = srcLayer[index];
//                                    if (combined != 0) {
//                                        setBlock(bx + x, yy, zz, combined);
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//                break;
//            }
//        }
//    }

    @Nullable
    @Override
    public Path getStoragePath() {
        return getFolder().toPath();
    }

    @Override
    public boolean regenerateChunk(int x, int z, @Nullable BiomeType biome, @Nullable Long seed) {
        // Unsupported
        return false;
    }

    @Nullable
    @Override
    public Operation commit() {
        EditSession curES = editSession;
        if (curES != null && isModified()) {
            try {
                update();
                FawePlayer esPlayer = curES.getPlayer();
                UUID uuid = esPlayer != null ? esPlayer.getUUID() : EditSession.CONSOLE;
                try {
                    curES.setRawChangeSet(new CFIChangeSet(this, uuid));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public void close(boolean update) {
        if (chunkOffset != null && player != null && update) {
            IQueueExtent packetQueue = Fawe.get().getQueueHandler().getQueue(player.getWorld());

            int lenCX = (getWidth() + 15) >> 4;
            int lenCZ = (getLength() + 15) >> 4;

            int OX = chunkOffset.getBlockX();
            int OZ = chunkOffset.getBlockZ();

            Location position = player.getLocation();
            int pcx = (position.getBlockX() >> 4) - OX;
            int pcz = (position.getBlockZ() >> 4) - OZ;

            int scx = Math.max(0, pcx - 10);
            int scz = Math.max(0, pcz - 10);
            int ecx = Math.min(lenCX - 1, pcx + 10);
            int ecz = Math.min(lenCZ - 1, pcz + 10);

            for (int cz = scz; cz <= ecz; cz++) {
                for (int cx = scx; cx <= ecx; cx++) {
                    packetQueue.sendChunk(cx + OX, cz + OZ, 0);
                }
            }
        }
        if (player != null) {
            player.deleteMeta("CFISettings");
            LocalSession session = player.getSession();
            session.clearHistory();
        }
        player = null;
        chunkOffset = null;
    }

    @Override
    public BiomeType getBiomeType(int x, int z) throws FaweException.FaweChunkLoadException {
        int index = z * getWidth() + x;
        if (index < 0 || index >= getArea()) index = Math.floorMod(index, getArea());
        return BiomeTypes.get(biomes.getByte(index));
    }

//    @Override
    public int getCombinedId4Data(int x, int y, int z) throws FaweException.FaweChunkLoadException {
        int index = z * getWidth() + x;
        if (y < 0) return 0;
        if (index < 0 || index >= getArea() || x < 0 || x >= getWidth()) return 0;
        int height = heights.getByte(index) & 0xFF;
        if (y > height) {
            if (y == height + 1) {
                return overlay != null ? overlay.getInt(index) : 0;
            }
            if (blocks != null) {
                short chunkX = (short) (x >> 4);
                short chunkZ = (short) (z >> 4);
                int[][][] map = getChunkArray(chunkX, chunkZ);
                if (map != null) {
                    int combined = get(map, x, y, z);
                    if (combined != 0) {
                        return combined;
                    }
                }
            }
            if (y <= primtives.waterHeight) {
                return primtives.waterId << 4;
            }
            return 0;
        } else if (y == height) {
            return floor.getInt(index);
        } else {
            if (blocks != null) {
                short chunkX = (short) (x >> 4);
                short chunkZ = (short) (z >> 4);
                int[][][] map = getChunkArray(chunkX, chunkZ);
                if (map != null) {
                    int combined = get(map, x, y, z);
                    if (combined != 0) {
                        return combined;
                    }
                }
            }
            return main.getInt(index);
        }
    }

    @Override
    public boolean setBlock(int x, int y, int z, BlockStateHolder block) throws WorldEditException {
        return this.setBlock(x, y, z, block.getInternalId());
    }

    @Override
    public BiomeType getBiome(BlockVector2 position) {
        return getBiomeType(position.getBlockX(), position.getBlockZ());
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        return getBlock(position.getX(), position.getY(), position.getZ());
    }

    public BlockState getFloor(int x, int z) {
        int index = z * getWidth() + x;
        return BlockState.getFromInternalId(floor.getInt(index));
    }

    public int getHeight(int x, int z) {
        int index = z * getWidth() + x;
        return heights.getByte(index) & 0xFF;
    }

    public int getHeight(int index) {
        return heights.getByte(index) & 0xFF;
    }

    public void setFloor(int x, int z, BlockStateHolder block) {
        int index = z * getWidth() + x;
        floor.setInt(index, block.getInternalId());
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return BlockState.getFromInternalId(getCombinedId4Data(x, y, z));
    }

    @Override
    public int getNearestSurfaceLayer(int x, int z, int y, int minY, int maxY) {
        int index = z * getWidth() + x;
        if (index < 0 || index >= getArea()) index = Math.floorMod(index, getArea());
        return ((heights.getByte(index) & 0xFF) << 3) + (floor.getInt(index) & 0xFF) + 1;
    }

    @Override
    public int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY) {
        int index = z * getWidth() + x;
        if (index < 0 || index >= getArea()) index = Math.floorMod(index, getArea());
        return heights.getByte(index) & 0xFF;
    }

    @Override
    public int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY, int failedMin, int failedMax) {
        int index = z * getWidth() + x;
        if (index < 0 || index >= getArea()) index = Math.floorMod(index, getArea());
        return heights.getByte(index) & 0xFF;
    }

    public void setBiome(BufferedImage img, BiomeType biome, boolean white) {
        if (img.getWidth() != getWidth() || img.getHeight() != getLength())
            throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
        byte biomeByte = (byte) biome.getInternalId();
        biomes.record(new Runnable() {
            @Override
            public void run() {
                byte[] biomeArr = biomes.get();
                int index = 0;
                for (int z = 0; z < getLength(); z++) {
                    for (int x = 0; x < getWidth(); x++, index++) {
                        int height = img.getRGB(x, z) & 0xFF;
                        if (height == 255 || height > 0 && !white && ThreadLocalRandom.current()
                                .nextInt(256) <= height) {
                            biomeArr[index] = biomeByte;
                        }
                    }
                }
            }
        });
    }

    public BufferedImage draw() {
        // TODO NOT IMPLEMENTED
//        return new HeightMapMCADrawer(this).draw();
        return null;
    }

    public void setBiomePriority(int value) {
        this.primtives.biomePriority = ((value * 65536) / 100) - 32768;
    }

    public int getBiomePriority() {
        return ((primtives.biomePriority + 32768) * 100) / 65536;
    }

    public void setBlockAndBiomeColor(BufferedImage img, Mask mask, BufferedImage imgMask, boolean whiteOnly) {
        if (mask == null && imgMask == null) {
            setBlockAndBiomeColor(img);
            return;
        }
        if (img.getWidth() != getWidth() || img.getHeight() != getLength())
            throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
        TextureUtil textureUtil = getTextureUtil();

        int widthIndex = img.getWidth() - 1;
        int heightIndex = img.getHeight() - 1;
        int maxIndex = getArea() - 1;

        biomes.record(() -> floor.record(() -> main.record(() -> {
            int[] mainArr = main.get();
            int[] floorArr = floor.get();
            byte[] biomesArr = biomes.get();

            int index = 0;
            int[] buffer = new int[2];
            for (int z = 0; z < img.getHeight(); z++) {
                mutable.mutZ(z);
                for (int x = 0; x < img.getWidth(); x++, index++) {
                    if (mask != null) {
                        mutable.mutX(z);
                        mutable.mutY(heights.getByte(index) & 0xFF);
                        if (!mask.test(mutable)) continue;
                    }
                    if (imgMask != null) {
                        int height = imgMask.getRGB(x, z) & 0xFF;
                        if (height != 255 && (height <= 0 || !whiteOnly || ThreadLocalRandom
                                .current().nextInt(256) > height)) continue;
                    }
                    int color = img.getRGB(x, z);
                    if (textureUtil.getIsBlockCloserThanBiome(buffer, color, primtives.biomePriority)) {
                        int combined = buffer[0];
                        mainArr[index] = combined;
                        floorArr[index] = combined;
                    }
                    biomesArr[index] = (byte) buffer[1];
                }
            }
        })));
    }

    public void setBlockAndBiomeColor(BufferedImage img) {
        if (img.getWidth() != getWidth() || img.getHeight() != getLength())
            throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
        TextureUtil textureUtil = getTextureUtil();
        int widthIndex = img.getWidth() - 1;
        int heightIndex = img.getHeight() - 1;
        int maxIndex = getArea() - 1;

        biomes.record(() -> floor.record(() -> main.record(() -> {
            int[] mainArr = main.get();
            int[] floorArr = floor.get();
            byte[] biomesArr = biomes.get();

            int[] buffer = new int[2];
            int index = 0;
            for (int y = 0; y < img.getHeight(); y++) {
                boolean yBiome = y > 0 && y < heightIndex;
                for (int x = 0; x < img.getWidth(); x++, index++) {
                    int color = img.getRGB(x, y);
                    if (textureUtil.getIsBlockCloserThanBiome(buffer, color, primtives.biomePriority)) {
                        int combined = buffer[0];
                        mainArr[index] = combined;
                        floorArr[index] = combined;
                    }
                    biomesArr[index] = (byte) buffer[1];
                }
            }
        })));
    }

    public void setBiomeColor(BufferedImage img) {
        if (img.getWidth() != getWidth() || img.getHeight() != getLength())
            throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
        TextureUtil textureUtil = getTextureUtil();

        biomes.record(new Runnable() {
            @Override
            public void run() {
                byte[] biomesArr = biomes.get();
                int index = 0;
                for (int y = 0; y < img.getHeight(); y++) {
                    for (int x = 0; x < img.getWidth(); x++) {
                        int color = img.getRGB(x, y);
                        TextureUtil.BiomeColor biome = textureUtil.getNearestBiome(color);
                        if (biome != null) {
                            biomesArr[index] = (byte) biome.id;
                        }
                        index++;
                    }
                }
            }
        });
    }

    public void setColor(BufferedImage img, BufferedImage mask, boolean white) {
        if (img.getWidth() != getWidth() || img.getHeight() != getLength())
            throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
        if (mask.getWidth() != getWidth() || mask.getHeight() != getLength())
            throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
        primtives.modifiedMain = true;
        TextureUtil textureUtil = getTextureUtil();

        floor.record(() -> main.record(() -> {
            int[] mainArr = main.get();
            int[] floorArr = floor.get();

            int index = 0;
            for (int z = 0; z < getLength(); z++) {
                for (int x = 0; x < getWidth(); x++, index++) {
                    int height = mask.getRGB(x, z) & 0xFF;
                    if (height == 255 || height > 0 && !white && ThreadLocalRandom.current()
                            .nextInt(256) <= height) {
                        int color = img.getRGB(x, z);
                        BlockType block = textureUtil.getNearestBlock(color);
                        if (block != null) {
                            int combined = block.getInternalId();
                            mainArr[index] = combined;
                            floorArr[index] = combined;
                        }
                    }
                }
            }
        }));
    }

    public void setColor(BufferedImage img, Mask mask) {
        if (img.getWidth() != getWidth() || img.getHeight() != getLength())
            throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
        primtives.modifiedMain = true;
        TextureUtil textureUtil = getTextureUtil();


        floor.record(() -> main.record(() -> {
            int[] mainArr = main.get();
            int[] floorArr = floor.get();

            int index = 0;
            for (int z = 0; z < getLength(); z++) {
                mutable.mutZ(z);
                for (int x = 0; x < getWidth(); x++, index++) {
                    mutable.mutX(x);
                    mutable.mutY(heights.getByte(index) & 0xFF);
                    if (mask.test(mutable)) {
                        int color = img.getRGB(x, z);
                        BlockType block = textureUtil.getNearestBlock(color);
                        if (block != null) {
                            int combined = block.getInternalId();
                            mainArr[index] = combined;
                            floorArr[index] = combined;
                        } else {
                            System.out.println("Block is null: " + color);
                        }
                    }
                }
            }
        }));
    }

    public void setColor(BufferedImage img) {
        if (img.getWidth() != getWidth() || img.getHeight() != getLength())
            throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
        primtives.modifiedMain = true;
        TextureUtil textureUtil = getTextureUtil();

        floor.record(() -> main.record(() -> {
            int[] mainArr = main.get();
            int[] floorArr = floor.get();

            int index = 0;
            for (int z = 0; z < img.getHeight(); z++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int color = img.getRGB(x, z);
                    BlockType block = textureUtil.getNearestBlock(color);
                    if (block != null) {
                        int combined = block.getInternalId();
                        mainArr[index] = combined;
                        floorArr[index] = combined;
                    } else {
                        System.out.println("Block is null " + color + " | " + textureUtil.getClass());
                    }
                    index++;
                }
            }
        }));
    }

    public void setColorWithGlass(BufferedImage img) {
        if (img.getWidth() != getWidth() || img.getHeight() != getLength())
            throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
        TextureUtil textureUtil = getTextureUtil();

        floor.record(() -> main.record(() -> {
            int[] mainArr = main.get();
            int[] floorArr = floor.get();

            int index = 0;
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int color = img.getRGB(x, y);
                    BlockType[] layer = textureUtil.getNearestLayer(color);
                    if (layer != null) {
                        floorArr[index] = layer[0].getInternalId();
                        mainArr[index] = layer[1].getInternalId();
                    }
                    index++;
                }
            }
        }));
    }

    public void setBiome(Mask mask, BiomeType biome) {
        int index = 0;
        byte biomeByte = (byte) biome.getInternalId();
        for (int z = 0; z < getLength(); z++) {
            mutable.mutZ(z);
            for (int x = 0; x < getWidth(); x++, index++) {
                int y = heights.getByte(index) & 0xFF;
                mutable.mutX(x);
                mutable.mutY(y);
                if (mask.test(mutable)) {
                    biomes.setByte(index, biomeByte);
                }
            }
        }
    }

    public void setOverlay(BufferedImage img, Pattern pattern, boolean white) {
        if (pattern instanceof BlockStateHolder) {
            setOverlay(img, ((BlockStateHolder) pattern).getInternalId(), white);
        } else if (pattern instanceof BlockType) {
            setOverlay(img, ((BlockType) pattern).getInternalId(), white);
        } else {
            if (img.getWidth() != getWidth() || img.getHeight() != getLength())
                throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
            if (overlay == null) {
                overlay = new DifferentialArray<>(new int[getArea()]);
            }

            overlay.record(() -> {
                int[] overlayArr = overlay.get();
                int index = 0;
                for (int z = 0; z < getLength(); z++) {
                    mutable.mutZ(z);
                    for (int x = 0; x < getWidth(); x++, index++) {
                        int height = img.getRGB(x, z) & 0xFF;
                        if (height == 255 || height > 0 && !white && ThreadLocalRandom.current()
                                .nextInt(256) <= height) {
                            mutable.mutX(x);
                            mutable.mutY(height);
                            overlayArr[index] = pattern.apply(mutable).getInternalId();
                        }
                    }
                }
            });

        }
    }

    public void setMain(BufferedImage img, Pattern pattern, boolean white) {
        if (pattern instanceof BlockStateHolder) {
            setMain(img, ((BlockStateHolder) pattern).getInternalId(), white);
        } else {
            if (img.getWidth() != getWidth() || img.getHeight() != getLength())
                throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
            primtives.modifiedMain = true;

            main.record(() -> {
                int[] mainArr = main.get();
                int index = 0;
                for (int z = 0; z < getLength(); z++) {
                    mutable.mutZ(z);
                    for (int x = 0; x < getWidth(); x++, index++) {
                        int height = img.getRGB(x, z) & 0xFF;
                        if (height == 255 || height > 0 && !white && ThreadLocalRandom.current()
                                .nextInt(256) <= height) {
                            mutable.mutX(x);
                            mutable.mutY(height);
                            mainArr[index] = pattern.apply(mutable).getInternalId();
                        }
                    }
                }
            });
        }
    }

    public void setFloor(BufferedImage img, Pattern pattern, boolean white) {
        if (pattern instanceof BlockStateHolder) {
            setFloor(img, ((BlockStateHolder) pattern).getInternalId(), white);
        } else {
            if (img.getWidth() != getWidth() || img.getHeight() != getLength())
                throw new IllegalArgumentException("Input image dimensions do not match the current height map!");

            floor.record(() -> {
                int[] floorArr = floor.get();
                int index = 0;
                for (int z = 0; z < getLength(); z++) {
                    mutable.mutZ(z);
                    for (int x = 0; x < getWidth(); x++, index++) {
                        int height = img.getRGB(x, z) & 0xFF;
                        if (height == 255 || height > 0 && !white && ThreadLocalRandom.current()
                                .nextInt(256) <= height) {
                            mutable.mutX(x);
                            mutable.mutY(height);
                            floorArr[index] = pattern.apply(mutable).getInternalId();
                        }
                    }
                }
            });
        }
    }

    public void setColumn(BufferedImage img, Pattern pattern, boolean white) {
        if (pattern instanceof BlockStateHolder) {
            setColumn(img, ((BlockStateHolder) pattern).getInternalId(), white);
        } else {
            if (img.getWidth() != getWidth() || img.getHeight() != getLength())
                throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
            primtives.modifiedMain = true;

            main.record(() -> floor.record(() -> {
                int[] floorArr = floor.get();
                int[] mainArr = main.get();
                int index = 0;
                for (int z = 0; z < getLength(); z++) {
                    mutable.mutZ(z);
                    for (int x = 0; x < getWidth(); x++, index++) {
                        int height = img.getRGB(x, z) & 0xFF;
                        if (height == 255 || height > 0 && !white && ThreadLocalRandom.current()
                                .nextInt(256) <= height) {
                            mutable.mutX(x);
                            mutable.mutY(height);
                            int combined = pattern.apply(mutable).getInternalId();
                            mainArr[index] = combined;
                            floorArr[index] = combined;
                        }
                    }
                }
            }));
        }
    }

    public void setOverlay(Mask mask, Pattern pattern) {
        if (pattern instanceof BlockStateHolder) {
            setOverlay(mask, ((BlockStateHolder) pattern).getInternalId());
        } else {
            int index = 0;
            if (overlay == null) overlay = new DifferentialArray<>(new int[getArea()]);
            for (int z = 0; z < getLength(); z++) {
                mutable.mutZ(z);
                for (int x = 0; x < getWidth(); x++, index++) {
                    int y = heights.getByte(index) & 0xFF;
                    mutable.mutX(x);
                    mutable.mutY(y);
                    if (mask.test(mutable)) {
                        overlay.setInt(index, pattern.apply(mutable).getInternalId());
                    }
                }
            }
        }
    }

    public void setFloor(Mask mask, Pattern pattern) {
        if (pattern instanceof BlockStateHolder) {
            setFloor(mask, ((BlockStateHolder) pattern).getInternalId());
        } else {
            int index = 0;
            for (int z = 0; z < getLength(); z++) {
                mutable.mutZ(z);
                for (int x = 0; x < getWidth(); x++, index++) {
                    int y = heights.getByte(index) & 0xFF;
                    mutable.mutX(x);
                    mutable.mutY(y);
                    if (mask.test(mutable)) {
                        floor.setInt(index, pattern.apply(mutable).getInternalId());
                    }
                }
            }
        }
    }

    public void setMain(Mask mask, Pattern pattern) {
        if (pattern instanceof BlockStateHolder) {
            setMain(mask, ((BlockStateHolder) pattern).getInternalId());
        } else {
            primtives.modifiedMain = true;
            int index = 0;
            for (int z = 0; z < getLength(); z++) {
                mutable.mutZ(z);
                for (int x = 0; x < getWidth(); x++, index++) {
                    int y = heights.getByte(index) & 0xFF;
                    mutable.mutX(x);
                    mutable.mutY(y);
                    if (mask.test(mutable)) {
                        main.setInt(index, pattern.apply(mutable).getInternalId());
                    }
                }
            }
        }
    }

    public void setColumn(Mask mask, Pattern pattern) {
        if (pattern instanceof BlockStateHolder) {
            setColumn(mask, ((BlockStateHolder) pattern).getInternalId());
        } else {
            primtives.modifiedMain = true;
            int index = 0;
            for (int z = 0; z < getLength(); z++) {
                mutable.mutZ(z);
                for (int x = 0; x < getWidth(); x++, index++) {
                    int y = heights.getByte(index) & 0xFF;
                    mutable.mutX(x);
                    mutable.mutY(y);
                    if (mask.test(mutable)) {
                        int combined = pattern.apply(mutable).getInternalId();
                        floor.setInt(index, combined);
                        main.setInt(index, combined);
                    }
                }
            }
        }
    }

    public void setBiome(BiomeType biome) {
        biomes.record(() -> Arrays.fill(biomes.get(), (byte) biome.getInternalId()));
    }

    public void setFloor(Pattern value) {
        if (value instanceof BlockStateHolder) {
            setFloor(((BlockStateHolder) value).getInternalId());
        } else {
            floor.record(() -> {
                int[] floorArr = floor.get();
                int index = 0;
                for (int z = 0; z < getLength(); z++) {
                    mutable.mutZ(z);
                    for (int x = 0; x < getWidth(); x++, index++) {
                        int y = heights.getByte(index) & 0xFF;
                        mutable.mutX(x);
                        mutable.mutY(y);
                        floorArr[index] = value.apply(mutable).getInternalId();
                    }
                }
            });
        }
    }

    public void setColumn(Pattern value) {
        if (value instanceof BlockStateHolder) {
            setColumn(((BlockStateHolder) value).getInternalId());
        } else {
            main.record(() -> floor.record(() -> {
                int[] floorArr = floor.get();
                int[] mainArr = main.get();
                int index = 0;
                for (int z = 0; z < getLength(); z++) {
                    mutable.mutZ(z);
                    for (int x = 0; x < getWidth(); x++, index++) {
                        int y = heights.getByte(index) & 0xFF;
                        mutable.mutX(x);
                        mutable.mutY(y);
                        int combined = value.apply(mutable).getInternalId();
                        mainArr[index] = combined;
                        floorArr[index] = combined;
                    }
                }
            }));
        }
    }

    public void setMain(Pattern value) {
        if (value instanceof BlockStateHolder) {
            setMain(((BlockStateHolder) value).getInternalId());
        } else {
            main.record(() -> {
                int[] mainArr = main.get();
                int index = 0;
                for (int z = 0; z < getLength(); z++) {
                    mutable.mutZ(z);
                    for (int x = 0; x < getWidth(); x++, index++) {
                        int y = heights.getByte(index) & 0xFF;
                        mutable.mutX(x);
                        mutable.mutY(y);
                        mainArr[index] = value.apply(mutable).getInternalId();
                    }
                }
            });
        }
    }

    public void setOverlay(Pattern value) {
        if (overlay == null) overlay = new DifferentialArray<>(new int[getArea()]);
        if (value instanceof BlockStateHolder) {
            setOverlay(((BlockStateHolder) value).getInternalId());
        } else {
            overlay.record(() -> {
                int[] overlayArr = overlay.get();
                int index = 0;
                for (int z = 0; z < getLength(); z++) {
                    mutable.mutZ(z);
                    for (int x = 0; x < getWidth(); x++, index++) {
                        int y = heights.getByte(index) & 0xFF;
                        mutable.mutX(x);
                        mutable.mutY(y);
                        overlayArr[index] = value.apply(mutable).getInternalId();
                    }
                }
            });
        }
    }

    public void setHeight(int x, int z, int height) {
        int index = z * getWidth() + x;
        if (index < 0 || index >= getArea()) return;
        heights.setByte(index, (byte) height);
    }

    public void setHeight(int index, int height) {
        heights.setByte(index, (byte) height);
    }

    public void setHeights(int value) {
        heights.record(() -> {
            Arrays.fill(heights.get(), (byte) value);
        });
    }

    @Override
    public boolean shouldWrite(int chunkX, int chunkZ) {
        return true;
    }

    @Override
    public WritableMCAChunk write(WritableMCAChunk chunk, int csx, int cex, int csz, int cez) {
        byte[] heights = this.heights.get();
        byte[] biomes = this.biomes.get();
        int[] main = this.main.get();
        int[] floor = this.floor.get();
        int[] overlay = this.overlay != null ? this.overlay.get() : null;
        try {
            int[] indexes = indexStore.get();

            int index;
            int maxY = 0;
            int minY = Integer.MAX_VALUE;
            int[] heightMap = chunk.biomes;
            int globalIndex;
            for (int z = csz; z <= cez; z++) {
                globalIndex = z * getWidth() + csx;
                index = (z & 15) << 4;
                for (int x = csx; x <= cex; x++, index++, globalIndex++) {
                    indexes[index] = globalIndex;
                    int height = heights[globalIndex] & 0xFF;
                    heightMap[index] = height;
                    maxY = Math.max(maxY, height);
                    minY = Math.min(minY, height);
                }
            }
            boolean hasOverlay = this.overlay != null;
            if (hasOverlay) {
                maxY++;
            }
            int maxLayer = maxY >> 4;
            for (int layer = 0; layer <= maxLayer; layer++) {
                chunk.hasSections[layer] = true;
            }
            if (primtives.waterHeight != 0) {
                int maxIndex = (primtives.waterHeight) << 8;
                Arrays.fill(chunk.blocks, 0, maxIndex, primtives.waterId);
            }

            if (primtives.modifiedMain) { // If the main block is modified, we can't short circuit this
                for (int z = csz; z <= cez; z++) {
                    index = (z & 15) << 4;
                    for (int x = csx; x <= cex; x++, index++) {
                        globalIndex = indexes[index];
                        int mainCombined = main[globalIndex];
                        for (int y = 0; y < minY; y++) {
                            chunk.blocks[index + (y << 8)] = mainCombined;
                        }
                    }
                }
            } else {
                int maxIndex = minY << 8;
                Arrays.fill(chunk.blocks, 0, maxIndex, BlockID.STONE);
            }

            final boolean hasFloorThickness = primtives.floorThickness != 0 || primtives.worldThickness != 0;
            if (primtives.worldThickness != 0) {
                int endLayer = ((minY - primtives.worldThickness + 1) >> 4);
                for (int layer = 0; layer < endLayer; layer++) {
                    chunk.hasSections[layer] = false;
                }
            }

            for (int z = csz; z <= cez; z++) {
                index = (z & 15) << 4;
                for (int x = csx; x <= cex; x++, index++) {
                    globalIndex = indexes[index];
                    int height = heightMap[index];
                    int maxMainY = height;
                    int minMainY = minY;

                    int mainCombined = main[globalIndex];

                    int floorCombined = floor[globalIndex];
                    if (hasFloorThickness) {
                        if (x > 0) maxMainY = Math.min(heights[globalIndex - 1] & 0xFF, maxMainY);
                        if (x < getWidth() - 1) maxMainY = Math.min(heights[globalIndex + 1] & 0xFF, maxMainY);
                        if (z > 0) maxMainY = Math.min(heights[globalIndex - getWidth()] & 0xFF, maxMainY);
                        if (z < getLength() - 1) maxMainY = Math.min(heights[globalIndex + getWidth()] & 0xFF, maxMainY);

                        int min = maxMainY;

                        if (primtives.floorThickness != 0) {
                            maxMainY = Math.max(0, maxMainY - (primtives.floorThickness - 1));
                            for (int y = maxMainY; y <= height; y++) {
                                chunk.blocks[index + (y << 8)] = floorCombined;
                            }
                        }
                        else {
                            chunk.blocks[index + ((height) << 8)] = floorCombined;
                        }

                        if (primtives.worldThickness != 0) {
                            minMainY = Math.max(minY, min - primtives.worldThickness + 1);
                            for (int y = minY; y < minMainY; y++) {
                                chunk.blocks[index + (y << 8)] = BlockID.AIR;
                            }
                        }

                    } else {
                        chunk.blocks[index + ((height) << 8)] = floorCombined;
                    }

                    for (int y = minMainY; y < maxMainY; y++) {
                        chunk.blocks[index + (y << 8)] = mainCombined;
                    }

                    if (hasOverlay) {
                        int overlayCombined = overlay[globalIndex];
                        int overlayIndex = index + ((height + 1) << 8);
                        chunk.blocks[overlayIndex] = overlayCombined;
                    }

                    if (primtives.bedrockId != 0) {
                        chunk.blocks[index] = primtives.bedrockId;
                    }
                }
            }

            int[][][] localBlocks = getChunkArray(chunk.getX(), chunk.getZ());
            if (localBlocks != null) {
                index = 0;
                for (int layer = 0; layer < 16; layer++) {
                    int by = layer << 4;
                    int ty = by + 15;
                    for (int y = by; y <= ty; y++, index += 256) {
                        int[][] yBlocks = localBlocks[y];
                        if (yBlocks != null) {
                            chunk.hasSections[layer] = true;
                            for (int z = 0; z < yBlocks.length; z++) {
                                int[] zBlocks = yBlocks[z];
                                if (zBlocks != null) {
                                    int zIndex = index + (z << 4);
                                    for (int x = 0; x < zBlocks.length; x++, zIndex++) {
                                        int combined = zBlocks[x];
                                        if (combined == 0) continue;
                                        chunk.blocks[zIndex] = combined;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            for (int i = 0; i < 256; i++) {
                chunk.biomes[i] = biomes[indexes[i]];
            }


        } catch (Throwable e) {
            e.printStackTrace();
        }
        return chunk;
    }

    private void setUnsafe(int[][][] map, int combined, int x, int y, int z) {
        int[][] yMap = map[y];
        if (yMap == null) {
            map[y] = yMap = new int[16][];
        }
        int[] zMap = yMap[z];
        if (zMap == null) {
            yMap[z] = zMap = new int[16];
        }
        zMap[x] = combined;
    }

    private int get(int[][][] map, int x, int y, int z) {
        int[][] yMap = map[y];
        if (yMap == null) {
            return 0;
        }
        int[] zMap = yMap[z & 15];
        if (zMap == null) {
            return 0;
        }
        return zMap[x & 15];
    }

    private void setOverlay(Mask mask, int combined) {
        int index = 0;
        if (overlay == null) overlay = new DifferentialArray<>(new int[getArea()]);
        for (int z = 0; z < getLength(); z++) {
            mutable.mutZ(z);
            for (int x = 0; x < getWidth(); x++, index++) {
                int y = heights.getByte(index) & 0xFF;
                mutable.mutX(x);
                mutable.mutY(y);
                if (mask.test(mutable)) {
                    overlay.setInt(index, combined);
                }
            }
        }
    }

    private void setFloor(Mask mask, int combined) {
        int index = 0;
        for (int z = 0; z < getLength(); z++) {
            mutable.mutZ(z);
            for (int x = 0; x < getWidth(); x++, index++) {
                int y = heights.getByte(index) & 0xFF;
                mutable.mutX(x);
                mutable.mutY(y);
                if (mask.test(mutable)) {
                    floor.setInt(index, combined);
                }
            }
        }
    }

    private void setMain(Mask mask, int combined) {
        primtives.modifiedMain = true;
        int index = 0;
        for (int z = 0; z < getLength(); z++) {
            mutable.mutZ(z);
            for (int x = 0; x < getWidth(); x++, index++) {
                int y = heights.getByte(index) & 0xFF;
                mutable.mutX(x);
                mutable.mutY(y);
                if (mask.test(mutable)) {
                    main.setInt(index, combined);
                }
            }
        }
    }

    private void setColumn(Mask mask, int combined) {
        primtives.modifiedMain = true;
        int index = 0;
        for (int z = 0; z < getLength(); z++) {
            mutable.mutZ(z);
            for (int x = 0; x < getWidth(); x++, index++) {
                int y = heights.getByte(index) & 0xFF;
                mutable.mutX(x);
                mutable.mutY(y);
                if (mask.test(mutable)) {
                    floor.setInt(index, combined);
                    main.setInt(index, combined);
                }
            }
        }
    }

    private void setFloor(int value) {
        floor.record(() -> Arrays.fill(floor.get(), value));
    }

    private void setColumn(int value) {
        setFloor(value);
        setMain(value);
    }

    private void setMain(int value) {
        primtives.modifiedMain = true;
        main.record(() -> Arrays.fill(main.get(), value));
    }

    private void setOverlay(int value) {
        if (overlay == null) overlay = new DifferentialArray<>(new int[getArea()]);
        overlay.record(() -> Arrays.fill(overlay.get(), value));
    }

    private void setOverlay(BufferedImage img, int combined, boolean white) {
        if (img.getWidth() != getWidth() || img.getHeight() != getLength())
            throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
        if (overlay == null) overlay = new DifferentialArray<>(new int[getArea()]);

        overlay.record(() -> {
            int index = 0;
            for (int z = 0; z < getLength(); z++) {
                for (int x = 0; x < getWidth(); x++, index++) {
                    int height = img.getRGB(x, z) & 0xFF;
                    if (height == 255 || height > 0 && white && ThreadLocalRandom.current()
                            .nextInt(256) <= height) {
                        overlay.get()[index] = combined;
                    }
                }
            }
        });
    }

    private void setMain(BufferedImage img, int combined, boolean white) {
        if (img.getWidth() != getWidth() || img.getHeight() != getLength())
            throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
        primtives.modifiedMain = true;

        main.record(() -> {
            int index = 0;
            for (int z = 0; z < getLength(); z++) {
                for (int x = 0; x < getWidth(); x++, index++) {
                    int height = img.getRGB(x, z) & 0xFF;
                    if (height == 255 || height > 0 && !white && ThreadLocalRandom.current()
                            .nextInt(256) <= height) {
                        main.get()[index] = combined;
                    }
                }
            }
        });
    }

    private void setFloor(BufferedImage img, int combined, boolean white) {
        if (img.getWidth() != getWidth() || img.getHeight() != getLength())
            throw new IllegalArgumentException("Input image dimensions do not match the current height map!");

        floor.record(() -> {
            int index = 0;
            for (int z = 0; z < getLength(); z++) {
                for (int x = 0; x < getWidth(); x++, index++) {
                    int height = img.getRGB(x, z) & 0xFF;
                    if (height == 255 || height > 0 && !white && ThreadLocalRandom.current()
                            .nextInt(256) <= height) {
                        floor.get()[index] = combined;
                    }
                }
            }
        });
    }

    private void setColumn(BufferedImage img, int combined, boolean white) {
        if (img.getWidth() != getWidth() || img.getHeight() != getLength())
            throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
        primtives.modifiedMain = true;

        main.record(() -> floor.record(() -> {
            int index = 0;
            for (int z = 0; z < getLength(); z++) {
                for (int x = 0; x < getWidth(); x++, index++) {
                    int height = img.getRGB(x, z) & 0xFF;
                    if (height == 255 || height > 0 && !white && ThreadLocalRandom.current()
                            .nextInt(256) <= height) {
                        main.get()[index] = combined;
                        floor.get()[index] = combined;
                    }
                }
            }
        }));
    }

    @Override
    protected void finalize() throws Throwable {
        IterableThreadLocal.clean(indexStore);
        super.finalize();
    }

    @Override
    public int getMaxY() {
        return 255;
    }

    @Override
    public String getName() {
        File folder = getFolder();
        if (folder != null) {
            String name = folder.getName();
            if (name.equalsIgnoreCase("region")) return folder.getParentFile().getName();
            return name;
        }
        return Integer.toString(hashCode());
    }

    @Override
    public boolean setBlock(BlockVector3 position, BlockStateHolder block, boolean notifyAndLight) throws WorldEditException {
        return setBlock(position, block);
    }

    // These aren't implemented yet...
    @Override
    public int getBlockLightLevel(BlockVector3 position) {
        return 0;
    }

    @Override
    public boolean clearContainerBlockContents(BlockVector3 position) {
        return false;
    }

    @Override
    public boolean regenerate(Region region, EditSession editSession) {
        return false;
    }

    @Override
    public void dropItem(Vector3 position, BaseItemStack item) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean playEffect(Vector3 position, int type, int data) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean notifyAndLightBlock(BlockVector3 position, BlockState previousType) throws WorldEditException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public BlockVector3 getSpawnPosition() {
        // TODO Auto-generated method stub
        return null;
    }
}
