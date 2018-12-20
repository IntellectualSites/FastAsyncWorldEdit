package com.boydti.fawe.jnbt.anvil;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.example.SimpleIntFaweChunk;
import com.boydti.fawe.object.*;
import com.boydti.fawe.object.brush.visualization.VirtualWorld;
import com.boydti.fawe.object.change.StreamChange;
import com.boydti.fawe.object.changeset.CFIChangeSet;
import com.boydti.fawe.object.collection.*;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.object.queue.LazyFaweChunk;
import com.boydti.fawe.object.schematic.Schematic;
import com.boydti.fawe.util.*;
import com.boydti.fawe.util.image.Drawable;
import com.boydti.fawe.util.image.ImageViewer;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.TreeGenerator;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;
import javax.annotation.Nullable;

// TODO FIXME
public class HeightMapMCAGenerator extends MCAWriter implements StreamChange, Drawable, VirtualWorld {
    private final MutableBlockVector mutable = new MutableBlockVector();

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
        protected int waterId = BlockTypes.WATER.getInternalId();
        protected int bedrockId = 7;
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
        } catch (Throwable neverHappens) { neverHappens.printStackTrace(); }

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
        } catch (Throwable neverHappens) { neverHappens.printStackTrace(); }
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
        } catch (Throwable neverHappens) { neverHappens.printStackTrace(); }

        blocks.clearChanges(); // blocks.redoChanges(in); Unsupported
    }

    @Override
    public void addEditSession(EditSession session) {
        session.setFastMode(true);
        this.editSession = session;
    }

    @Override
    public boolean supports(Capability capability) {
        return false;
    }

    // Used for visualizing the world on a map
    private ImageViewer viewer;
    // Used for visualizing the world by sending chunk packets
    // These three variables should be set together
//    private FaweQueue packetQueue;
    private FawePlayer player;
    private Vector2D chunkOffset = Vector2D.ZERO;
    private EditSession editSession;
    // end

    public HeightMapMCAGenerator(BufferedImage img, File regionFolder) {
        this(img.getWidth(), img.getHeight(), regionFolder);
        setHeight(img);
    }

    public HeightMapMCAGenerator(int width, int length, File regionFolder) {
        super(width, length, regionFolder);
        int area = getArea();

        blocks = new DifferentialBlockBuffer(width, length);
        heights = new DifferentialArray(new byte[getArea()]);
        biomes = new DifferentialArray(new byte[getArea()]);
        floor = new DifferentialArray(new int[getArea()]);
        main = new DifferentialArray(new int[getArea()]);

        int stone = BlockTypes.STONE.getInternalId();
        int grass = BlockTypes.GRASS_BLOCK.getInternalId();
        Arrays.fill(main.getIntArray(), stone);
        Arrays.fill(floor.getIntArray(), grass);
    }

    public Metadatable getMetaData() {
        return metaData;
    }

    @Override
    public FaweQueue getQueue() {
        throw new UnsupportedOperationException("Not supported: Queue is not backed by a real world");
    }

    @Override
    public Vector getOrigin() {
        return new BlockVector(chunkOffset.getBlockX() << 4, 0, chunkOffset.getBlockZ() << 4);
    }

    public boolean hasPacketViewer() {
        return player != null;
    }

    public void setPacketViewer(FawePlayer player) {
        this.player = player;
        if (player != null) {
            FaweLocation pos = player.getLocation();
            this.chunkOffset = new Vector2D(1 + (pos.x >> 4), 1 + (pos.z >> 4));
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
        if (chunkOffset != null && player != null) {
            FaweQueue packetQueue = SetQueue.IMP.getNewQueue(player.getWorld(), true, false);

            if (!packetQueue.supports(Capability.CHUNK_PACKETS)) {
                return;
            }

            int lenCX = (getWidth() + 15) >> 4;
            int lenCZ = (getLength() + 15) >> 4;

            int OX = chunkOffset.getBlockX();
            int OZ = chunkOffset.getBlockZ();

            FaweLocation position = player.getLocation();
            int pcx = (position.x >> 4) - OX;
            int pcz = (position.z >> 4) - OZ;

            int scx = Math.max(0, pcx - 15);
            int scz = Math.max(0, pcz - 15);
            int ecx = Math.min(lenCX - 1, pcx + 15);
            int ecz = Math.min(lenCZ - 1, pcz + 15);

            MCAChunk chunk = new MCAChunk(this, 0, 0);
            for (int cz = scz; cz <= ecz; cz++) {
                for (int cx = scx; cx <= ecx; cx++) {
                    final int finalCX = cx;
                    final int finalCZ = cz;
                    TaskManager.IMP.getPublicForkJoinPool().submit((Runnable) () -> {
                        try {
                            FaweChunk toSend = getSnapshot(finalCX, finalCZ);
                            toSend.setLoc(HeightMapMCAGenerator.this, finalCX + OX, finalCZ + OZ);
                            packetQueue.sendChunkUpdate(toSend, player);
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
        }
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

    public void smooth(Vector2D min, Vector2D max, int radius, int iterations) {
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
        switch (BlockTypes.getFromStateId(floorState)) {
            case SNOW:
            case SNOW_BLOCK:
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
        switch (BlockTypes.getFromStateId(floorState)) {
            case SNOW:
            case SNOW_BLOCK:
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
                            if (height == 255 || height > 0 && !white && PseudoRandom.random.nextInt(256) <= height) {
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
        CuboidRegion region = new CuboidRegion(new Vector(0, 0, 0), new Vector(getWidth() -1, 255, getLength() -1));
        addCaves(region);
    }

    @Deprecated
    public void addSchems(Mask mask, List<ClipboardHolder> clipboards, int rarity, boolean rotate) throws WorldEditException {
        CuboidRegion region = new CuboidRegion(new Vector(0, 0, 0), new Vector(getWidth() -1, 255, getLength() -1));
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
                if (height == 0 || PseudoRandom.random.nextInt(256) > height * doubleRarity) {
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
                ClipboardHolder holder = clipboards.get(PseudoRandom.random.random(clipboards.size()));
                if (randomRotate) {
                    int rotate = PseudoRandom.random.random(4) * 90;
                    if (rotate != 0) {
                        holder.setTransform(new AffineTransform().rotateY(PseudoRandom.random.random(4) * 90));
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
                if (PseudoRandom.random.nextInt(256) > scaledRarity) {
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
                ClipboardHolder holder = clipboards.get(PseudoRandom.random.random(clipboards.size()));
                if (randomRotate) {
                    int rotate = PseudoRandom.random.random(4) * 90;
                    if (rotate != 0) {
                        holder.setTransform(new AffineTransform().rotateY(PseudoRandom.random.random(4) * 90));
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
        CuboidRegion region = new CuboidRegion(new Vector(0, 0, 0), new Vector(getWidth() -1, 255, getLength() -1));
        addOre(region, mask, material, size, frequency, rarity, minY, maxY);
    }

    public void addDefaultOres(Mask mask) throws WorldEditException {
        addOres(new CuboidRegion(new Vector(0, 0, 0), new Vector(getWidth() -1, 255, getLength() -1)), mask);
    }

    @Override
    public Vector getMinimumPoint() {
        return new Vector(0, 0, 0);
    }

    @Override
    public FawePlayer getPlayer() {
        return player;
    }

    @Override
    public Vector getMaximumPoint() {
        return new Vector(getWidth() - 1, 255, getLength() - 1);
    }

    @Override
    public boolean setBlock(Vector position, BlockStateHolder block) throws WorldEditException {
        return setBlock(position.getBlockX(), position.getBlockY(), position.getBlockZ(), block);
    }

    @Override
    public boolean setBiome(Vector2D position, BaseBiome biome) {
        return this.setBiome(position.getBlockX(), position.getBlockZ(), biome);
    }

    @Override
    public boolean setBlock(int x, int y, int z, int combined) {
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
                    short chunkX = (short) (x >> 4);
                    short chunkZ = (short) (z >> 4);
                    blocks.set(x, y, z, combined);
                    return true;
                } catch (IndexOutOfBoundsException ignore) {
                    return false;
                }
        }
    }

    @Override
    public void setTile(int x, int y, int z, CompoundTag tag) {
        // Not implemented
    }

    @Override
    public void setEntity(int x, int y, int z, CompoundTag tag) {
        // Not implemented
    }

    @Override
    public void removeEntity(int x, int y, int z, UUID uuid) {
        // Not implemented
    }

    @Override
    public boolean setBiome(int x, int z, BaseBiome biome) {
        int index = z * getWidth() + x;
        if (index < 0 || index >= getArea()) return false;
        biomes.setByte(index, (byte) biome.getId());
        return true;
    }

    @Override
    public FaweChunk getFaweChunk(int chunkX, int chunkZ) {
        return new SimpleIntFaweChunk(this, chunkX, chunkZ);
    }

    @Override
    public FaweChunk getSnapshot(int chunkX, int chunkZ) {
        return getSnapshot(null, chunkX, chunkZ);
    }

    private FaweChunk getSnapshot(final MCAChunk chunk, int chunkX, int chunkZ) {
        return new LazyFaweChunk<MCAChunk>(this, chunkX, chunkZ) {
            @Override
            public MCAChunk getChunk() {
                MCAChunk tmp = chunk;
                if (tmp == null) {
                    tmp = new MCAChunk(HeightMapMCAGenerator.this, chunkX, chunkZ);
                } else {
                    tmp.setLoc(HeightMapMCAGenerator.this, chunkX, chunkZ);
                }
                int cbx = chunkX << 4;
                int cbz = chunkZ << 4;
                int csx = Math.max(0, cbx);
                int csz = Math.max(0, cbz);
                int cex = Math.min(getWidth(), cbx + 15);
                int cez = Math.min(getLength(), cbz + 15);
                write(tmp, csx, cex, csz, cez);
                tmp.setLoc(HeightMapMCAGenerator.this, getX(), getZ());
                return tmp;
            }

            @Override
            public void addToQueue() {
                MCAChunk cached = getCachedChunk();
                if (cached != null) setChunk(cached);
            }
        };
    }

    @Override
    public Collection<FaweChunk> getFaweChunks() {
        return Collections.emptyList();
    }

    @Override
    public void setChunk(FaweChunk chunk) {
        int[][] src = chunk.getCombinedIdArrays();
        for (int i = 0; i < src.length; i++) {
            if (src[i] != null) {
                int bx = chunk.getX() << 4;
                int bz = chunk.getZ() << 4;
                int by = i << 4;
                for (int layer = i; layer < src.length; layer++) {
                    int[] srcLayer = src[layer];
                    if (srcLayer != null) {
                        int index = 0;
                        for (int y = 0; y < 16; y++) {
                            int yy = by + y;
                            for (int z = 0; z < 16; z++) {
                                int zz = bz + z;
                                for (int x = 0; x < 16; x++, index++) {
                                    int combined = srcLayer[index];
                                    if (combined != 0) {
                                        setBlock(bx + x, yy, zz, combined);
                                    }
                                }
                            }
                        }
                    }
                }
                break;
            }
        }
    }

    @Override
    public File getSaveFolder() {
        return getFolder();
    }

    @Override
    public boolean regenerateChunk(int x, int z, @Nullable BaseBiome biome, @Nullable Long seed) {
        // Unsupported
        return false;
    }

    @Override
    public void sendBlockUpdate(FaweChunk chunk, FawePlayer... players) {

    }

    @Override
    public void flush(int time) {
        next(0, time);
    }

    @Override
    public boolean next(int amount, long time) {
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
        clear();
        return false;
    }

    @Override
    public void sendChunk(FaweChunk chunk) {
    }

    @Override
    public void sendChunk(int x, int z, int bitMask) {
    }

    @Override
    public void clear() {
        this.editSession = null;
    }

    @Override
    public void close(boolean update) {
        clear();
        if (chunkOffset != null && player != null && update) {
            FaweQueue packetQueue = SetQueue.IMP.getNewQueue(player.getWorld(), true, false);

            int lenCX = (getWidth() + 15) >> 4;
            int lenCZ = (getLength() + 15) >> 4;

            int OX = chunkOffset.getBlockX();
            int OZ = chunkOffset.getBlockZ();

            FaweLocation position = player.getLocation();
            int pcx = (position.x >> 4) - OX;
            int pcz = (position.z >> 4) - OZ;

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
    public void addNotifyTask(int x, int z, Runnable runnable) {
        if (runnable != null) runnable.run();
    }

    @Override
    public int getBiomeId(int x, int z) throws FaweException.FaweChunkLoadException {
        int index = z * getWidth() + x;
        if (index < 0 || index >= getArea()) index = Math.floorMod(index, getArea());
        return biomes.getByte(index) & 0xFF;
    }

    @Override
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
    public int getCombinedId4Data(int x, int y, int z, int def) {
        return getCombinedId4Data(x, y, z);
    }

    @Override
    public int getCachedCombinedId4Data(int x, int y, int z) throws FaweException.FaweChunkLoadException {
        return getCombinedId4Data(x, y, z);
    }

    @Override
    public boolean hasSky() {
        return true;
    }

    @Override
    public int getSkyLight(int x, int y, int z) {
        return getNearestSurfaceTerrainBlock(x, z, y, 0, 255) < y ? 15 : 0;
    }

    @Override
    public int getEmmittedLight(int x, int y, int z) {
        return 0;
    }

    @Override
    public CompoundTag getTileEntity(int x, int y, int z) throws FaweException.FaweChunkLoadException {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean setBlock(int x, int y, int z, BlockStateHolder block) throws WorldEditException {
        return this.setBlock(x, y, z, block.getInternalId());
    }

    @Override
    public BaseBiome getBiome(Vector2D position) {
        return FaweCache.CACHE_BIOME[getBiomeId(position.getBlockX(), position.getBlockZ())];
    }

    @Override
    public BlockState getBlock(Vector position) {
        return getLazyBlock(position);
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
    public BlockState getLazyBlock(Vector position) {
        return getLazyBlock(position.getBlockX(), position.getBlockY(), position.getBlockZ());
    }

    @Override
    public BlockState getLazyBlock(int x, int y, int z) {
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

    public void setBiome(BufferedImage img, byte biome, boolean white) {
        if (img.getWidth() != getWidth() || img.getHeight() != getLength())
            throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
        biomes.record(new Runnable() {
            @Override
            public void run() {
                byte[] biomeArr = biomes.get();
                int index = 0;
                for (int z = 0; z < getLength(); z++) {
                    for (int x = 0; x < getWidth(); x++, index++) {
                        int height = img.getRGB(x, z) & 0xFF;
                        if (height == 255 || height > 0 && !white && PseudoRandom.random.nextInt(256) <= height) {
                            biomeArr[index] = biome;
                        }
                    }
                }
            }
        });
    }

    public BufferedImage draw() {
        return new HeightMapMCADrawer(this).draw();
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
                        if (height != 255 && (height <= 0 || !whiteOnly || PseudoRandom.random.nextInt(256) > height)) continue;
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
                    if (height == 255 || height > 0 && !white && PseudoRandom.random.nextInt(256) <= height) {
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

    public void setBiome(Mask mask, byte biome) {
        int index = 0;
        for (int z = 0; z < getLength(); z++) {
            mutable.mutZ(z);
            for (int x = 0; x < getWidth(); x++, index++) {
                int y = heights.getByte(index) & 0xFF;
                mutable.mutX(x);
                mutable.mutY(y);
                if (mask.test(mutable)) {
                    biomes.setByte(index, biome);
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
                        if (height == 255 || height > 0 && !white && PseudoRandom.random.nextInt(256) <= height) {
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
                        if (height == 255 || height > 0 && !white && PseudoRandom.random.nextInt(256) <= height) {
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
                        if (height == 255 || height > 0 && !white && PseudoRandom.random.nextInt(256) <= height) {
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
                        if (height == 255 || height > 0 && !white && PseudoRandom.random.nextInt(256) <= height) {
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

    public void setBiome(int biome) {
        biomes.record(() -> Arrays.fill(biomes.get(), (byte) biome));
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
    public MCAChunk write(MCAChunk chunk, int csx, int cex, int csz, int cez) {
        // TODO FIXME
//        byte[] heights = this.heights.get();
//        byte[] biomes = this.biomes.get();
//        int[] main = this.main.get();
//        int[] floor = this.floor.get();
//        int[] overlay = this.overlay != null ? this.overlay.get() : null;
//        try {
//            int[] indexes = indexStore.get();
//            for (int i = 0; i < chunk.ids.length; i++) {
//                byte[] idsArray = chunk.ids[i];
//                if (idsArray != null) {
//                    Arrays.fill(idsArray, (byte) 0);
//                    Arrays.fill(chunk.data[i], (byte) 0);
//                }
//            }
//            int index;
//            int maxY = 0;
//            int minY = Integer.MAX_VALUE;
//            int[] heightMap = chunk.getHeightMapArray();
//            int globalIndex;
//            for (int z = csz; z <= cez; z++) {
//                globalIndex = z * getWidth() + csx;
//                index = (z & 15) << 4;
//                for (int x = csx; x <= cex; x++, index++, globalIndex++) {
//                    indexes[index] = globalIndex;
//                    int height = heights[globalIndex] & 0xFF;
//                    heightMap[index] = height;
//                    maxY = Math.max(maxY, height);
//                    minY = Math.min(minY, height);
//                }
//            }
//            boolean hasOverlay = this.overlay != null;
//            if (hasOverlay) {
//                maxY++;
//            }
//            int maxLayer = maxY >> 4;
//            int fillLayers = Math.max(0, (minY - 1)) >> 4;
//            for (int layer = 0; layer <= maxLayer; layer++) {
//                if (chunk.ids[layer] == null) {
//                    chunk.ids[layer] = new byte[4096];
//                    chunk.data[layer] = new byte[2048];
//                    chunk.skyLight[layer] = new byte[2048];
//                    chunk.blockLight[layer] = new byte[2048];
//                }
//            }
//            if (primtives.waterHeight != 0) {
//                maxY = Math.max(maxY, primtives.waterHeight);
//                int maxWaterLayer = ((primtives.waterHeight + 15) >> 4);
//                for (int layer = 0; layer < maxWaterLayer; layer++) {
//                    boolean fillAll = (layer << 4) + 15 <= primtives.waterHeight;
//                    byte[] ids = chunk.ids[layer];
//                    if (ids == null) {
//                        chunk.ids[layer] = ids = new byte[4096];
//                        chunk.data[layer] = new byte[2048];
//                        chunk.skyLight[layer] = new byte[2048];
//                        chunk.blockLight[layer] = new byte[2048];
//                        Arrays.fill(chunk.skyLight[layer], (byte) 255);
//                    }
//                    if (fillAll) {
//                        Arrays.fill(ids, primtives.waterId);
//                    } else {
//                        int maxIndex = (primtives.waterHeight & 15) << 8;
//                        Arrays.fill(ids, 0, maxIndex, primtives.waterId);
//                    }
//                }
//            }
//
//            if (primtives.modifiedMain) { // If the main block is modified, we can't short circuit this
//                for (int layer = 0; layer < fillLayers; layer++) {
//                    byte[] layerIds = chunk.ids[layer];
//                    byte[] layerDatas = chunk.data[layer];
//                    for (int z = csz; z <= cez; z++) {
//                        index = (z & 15) << 4;
//                        for (int x = csx; x <= cex; x++, index++) {
//                            globalIndex = indexes[index];
//                            char mainCombined = main[globalIndex];
//                            byte id = (byte) FaweCache.getId(mainCombined);
//                            int data = FaweCache.getData(mainCombined);
//                            if (data != 0) {
//                                for (int y = 0; y < 16; y++) {
//                                    int mainIndex = index + (y << 8);
//                                    chunk.setNibble(mainIndex, layerDatas, data);
//                                }
//                            }
//                            for (int y = 0; y < 16; y++) {
//                                layerIds[index + (y << 8)] = id;
//                            }
//                        }
//                    }
//                }
//            } else {
//                for (int layer = 0; layer < fillLayers; layer++) {
//                    Arrays.fill(chunk.ids[layer], (byte) 1);
//                }
//            }
//
//            for (int layer = fillLayers; layer <= maxLayer; layer++) {
//                Arrays.fill(chunk.skyLight[layer], (byte) 255);
//                byte[] layerIds = chunk.ids[layer];
//                byte[] layerDatas = chunk.data[layer];
//                int startY = layer << 4;
//                int endY = startY + 15;
//                for (int z = csz; z <= cez; z++) {
//                    index = (z & 15) << 4;
//                    for (int x = csx; x <= cex; x++, index++) {
//                        globalIndex = indexes[index];
//                        int height = heightMap[index];
//                        int diff;
//                        if (height > endY) {
//                            diff = 16;
//                        } else if (height >= startY) {
//                            diff = height - startY;
//                            char floorCombined = floor[globalIndex];
//                            int id = FaweCache.getId(floorCombined);
//                            int floorIndex = index + ((height & 15) << 8);
//                            layerIds[floorIndex] = (byte) id;
//                            int data = FaweCache.getData(floorCombined);
//                            if (data != 0) {
//                                chunk.setNibble(floorIndex, layerDatas, data);
//                            }
//                            if (hasOverlay && height >= startY - 1 && height < endY) {
//                                char overlayCombined = overlay[globalIndex];
//                                id = FaweCache.getId(overlayCombined);
//                                int overlayIndex = index + (((height + 1) & 15) << 8);
//                                layerIds[overlayIndex] = (byte) id;
//                                data = FaweCache.getData(overlayCombined);
//                                if (data != 0) {
//                                    chunk.setNibble(overlayIndex, layerDatas, data);
//                                }
//                            }
//                        } else if (hasOverlay && height == startY - 1) {
//                            char overlayCombined = overlay[globalIndex];
//                            int id = FaweCache.getId(overlayCombined);
//                            int overlayIndex = index + (((height + 1) & 15) << 8);
//                            layerIds[overlayIndex] = (byte) id;
//                            int data = FaweCache.getData(overlayCombined);
//                            if (data != 0) {
//                                chunk.setNibble(overlayIndex, layerDatas, data);
//                            }
//                            continue;
//                        } else {
//                            continue;
//                        }
//                        char mainCombined = main[globalIndex];
//                        byte id = (byte) FaweCache.getId(mainCombined);
//                        int data = FaweCache.getData(mainCombined);
//                        if (data != 0) {
//                            for (int y = 0; y < diff; y++) {
//                                int mainIndex = index + (y << 8);
//                                chunk.setNibble(mainIndex, layerDatas, data);
//                            }
//                        }
//                        for (int y = 0; y < diff; y++) {
//                            layerIds[index + (y << 8)] = id;
//                        }
//                    }
//                }
//            }
//
//            int maxYMod = 15 + (maxLayer << 4);
//            for (int layer = (maxY >> 4) + 1; layer < 16; layer++) {
//                chunk.ids[layer] = null;
//                chunk.data[layer] = null;
//            }
//
//            if (primtives.bedrockId != 0) { // Bedrock
//                byte[] layerIds = chunk.ids[0];
//                for (int z = csz; z <= cez; z++) {
//                    index = (z & 15) << 4;
//                    for (int x = csx; x <= cex; x++) {
//                        layerIds[index++] = primtives.bedrockId;
//                    }
//                }
//            }
//
//            char[][][] localBlocks = getChunkArray(chunk.getX(), chunk.getZ());
//            if (localBlocks != null) {
//                for (int layer = 0; layer < 16; layer++) {
//                    int by = layer << 4;
//                    int ty = by + 15;
//                    index = 0;
//                    for (int y = by; y <= ty; y++, index += 256) {
//                        char[][] yBlocks = localBlocks[y];
//                        if (yBlocks != null) {
//                            if (chunk.ids[layer] == null) {
//                                chunk.ids[layer] = new byte[4096];
//                                chunk.data[layer] = new byte[2048];
//                                chunk.skyLight[layer] = new byte[2048];
//                                chunk.blockLight[layer] = new byte[2048];
//                                Arrays.fill(chunk.skyLight[layer], (byte) 255);
//                            }
//                            byte[] idsLayer = chunk.ids[layer];
//                            byte[] dataLayer = chunk.data[layer];
//                            for (int z = 0; z < yBlocks.length; z++) {
//                                char[] zBlocks = yBlocks[z];
//                                if (zBlocks != null) {
//                                    int zIndex = index + (z << 4);
//                                    for (int x = 0; x < zBlocks.length; x++, zIndex++) {
//                                        char combined = zBlocks[x];
//                                        if (combined == 0) continue;
//                                        int id = FaweCache.getId(combined);
//                                        int data = FaweCache.getData(combined);
//                                        if (data == 0) {
//                                            chunk.setIdUnsafe(idsLayer, zIndex, (byte) id);
//                                        } else {
//                                            chunk.setBlockUnsafe(idsLayer, dataLayer, zIndex, (byte) id, FaweCache.getData(combined));
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//
//            if (primtives.floorThickness != 0 || primtives.worldThickness != 0) {
//                // Use biomes array as temporary buffer
//                byte[] minArr = chunk.biomes;
//                for (int z = csz; z <= cez; z++) {
//                    index = (z & 15) << 4;
//                    for (int x = csx; x <= cex; x++, index++) {
//                        int gi = indexes[index];
//                        int height = heightMap[index];
//                        int min = height;
//                        if (x > 0) min = Math.min(heights[gi - 1] & 0xFF, min);
//                        if (x < getWidth() - 1) min = Math.min(heights[gi + 1] & 0xFF, min);
//                        if (z > 0) min = Math.min(heights[gi - getWidth()] & 0xFF, min);
//                        if (z < getLength() - 1) min = Math.min(heights[gi + getWidth()] & 0xFF, min);
//                        minArr[index] = (byte) min;
//                    }
//                }
//
//                int minLayer = Math.max(0, (minY - primtives.floorThickness) >> 4);
//
//                if (primtives.floorThickness != 0) {
//                    for (int layer = minLayer; layer <= maxLayer; layer++) {
//                        byte[] layerIds = chunk.ids[layer];
//                        byte[] layerDatas = chunk.data[layer];
//                        int startY = layer << 4;
//                        int endY = startY + 15;
//                        for (int z = csz; z <= cez; z++) {
//                            index = (z & 15) << 4;
//                            for (int x = csx; x <= cex; x++, index++) {
//                                globalIndex = indexes[index];
//                                int height = heightMap[index];
//
//                                int min = (minArr[index] & 0xFF) - primtives.floorThickness;
//                                int localMin = min - startY;
//
//                                int max = height + 1;
//                                if (min < startY) min = startY;
//                                if (max > endY) max = endY + 1;
//
//
//                                if (min < max) {
//                                    char floorCombined = floor[globalIndex];
//                                    final byte id = (byte) FaweCache.getId(floorCombined);
//                                    final int data = FaweCache.getData(floorCombined);
//                                    for (int y = min; y < max; y++) {
//                                        int floorIndex = index + ((y & 15) << 8);
//                                        layerIds[floorIndex] = id;
//                                        if (data != 0) {
//                                            chunk.setNibble(floorIndex, layerDatas, data);
//                                        }
//                                    }
//                                }
//
//                            }
//                        }
//                    }
//                }
//                if (primtives.worldThickness != 0) {
//                    for (int layer = 0; layer < minLayer; layer++) {
//                        chunk.ids[layer] = null;
//                        chunk.data[layer] = null;
//                    }
//                    for (int layer = minLayer; layer <= maxLayer; layer++) {
//                        byte[] layerIds = chunk.ids[layer];
//                        byte[] layerDatas = chunk.data[layer];
//                        int startY = layer << 4;
//                        int endY = startY + 15;
//                        for (int z = csz; z <= cez; z++) {
//                            index = (z & 15) << 4;
//                            for (int x = csx; x <= cex; x++, index++) {
//                                globalIndex = indexes[index];
//                                int height = heightMap[index];
//
//                                int min = (minArr[index] & 0xFF) - primtives.worldThickness;
//                                int localMin = min - startY;
//                                if (localMin > 0) {
//                                    char floorCombined = floor[globalIndex];
//                                    final byte id = (byte) FaweCache.getId(floorCombined);
//                                    final int data = FaweCache.getData(floorCombined);
//
//                                    for (int y = 0; y < localMin; y++) {
//                                        int floorIndex = index + ((y & 15) << 8);
//                                        layerIds[floorIndex] = 0;
//                                        if (data != 0) {
//                                            chunk.setNibble(floorIndex, layerDatas, 0);
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//
//                for (int layer = fillLayers; layer <= maxLayer; layer++) {
//                    Arrays.fill(chunk.skyLight[layer], (byte) 255);
//
//                }
//            }
//
//            for (int i = 0; i < 256; i++) {
//                chunk.biomes[i] = biomes[indexes[i]];
//            }
//
//
//        } catch (Throwable e) {
//            e.printStackTrace();
//        }
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
                    if (height == 255 || height > 0 && white && PseudoRandom.random.nextInt(256) <= height) {
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
                    if (height == 255 || height > 0 && !white && PseudoRandom.random.nextInt(256) <= height) {
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
                    if (height == 255 || height > 0 && !white && PseudoRandom.random.nextInt(256) <= height) {
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
                    if (height == 255 || height > 0 && !white && PseudoRandom.random.nextInt(256) <= height) {
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
    public void setWorld(String world) {

    }

    @Override
    public World getWEWorld() {
        return this;
    }

    @Override
    public String getWorldName() {
        return getName();
    }

    @Override
    public long getModified() {
        return 0;
    }

    @Override
    public void setModified(long modified) {
        // Unsupported
    }

    @Override
    public RunnableVal2<ProgressType, Integer> getProgressTask() {
        return null;
    }

    @Override
    public void setProgressTask(RunnableVal2<ProgressType, Integer> progressTask) {

    }

    @Override
    public void setChangeTask(RunnableVal2<FaweChunk, FaweChunk> changeTask) {

    }

    @Override
    public RunnableVal2<FaweChunk, FaweChunk> getChangeTask() {
        return null;
    }

    @Override
    public SetQueue.QueueStage getStage() {
        return SetQueue.QueueStage.NONE;
    }

    @Override
    public void setStage(SetQueue.QueueStage stage) {
        // Not supported
    }

    @Override
    public void addNotifyTask(Runnable runnable) {
        runnable.run();
    }

    @Override
    public void runTasks() {

    }

    @Override
    public void addTask(Runnable whenFree) {
        whenFree.run();
    }

    @Override
    public boolean isEmpty() {
        return !isModified();
    }

    @Nullable
    @Override
    public Operation commit() {
        return null;
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
    public boolean setBlock(Vector position, BlockStateHolder block, boolean notifyAndLight) throws WorldEditException {
        return setBlock(position, block);
    }

    // These aren't implemented yet...
    @Override
    public int getBlockLightLevel(Vector position) {
        return 0;
    }

    @Override
    public boolean clearContainerBlockContents(Vector position) {
        return false;
    }

    @Override
    public void dropItem(Vector position, BaseItemStack item) {

    }

    @Override
    public boolean regenerate(Region region, EditSession editSession) {
        return false;
    }

    @Override
    public boolean generateTree(TreeGenerator.TreeType type, EditSession editSession, Vector position) throws MaxChangedBlocksException {
        return false;
    }
}
