package com.boydti.fawe.object.brush.visualization.cfi;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.IBlocks;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.beta.implementation.packet.ChunkPacket;
import com.boydti.fawe.beta.implementation.blocks.FallbackChunkGet;
import com.boydti.fawe.jnbt.anvil.MCAChunk;
import com.boydti.fawe.object.FaweInputStream;
import com.boydti.fawe.object.FaweOutputStream;
import com.boydti.fawe.object.Metadatable;
import com.boydti.fawe.object.brush.visualization.VirtualWorld;
import com.boydti.fawe.object.change.StreamChange;
import com.boydti.fawe.object.changeset.CFIChangeSet;
import com.boydti.fawe.object.collection.DifferentialArray;
import com.boydti.fawe.object.collection.DifferentialBlockBuffer;
import com.boydti.fawe.object.collection.LocalBlockVector2DSet;
import com.boydti.fawe.object.collection.SummedAreaTable;
import com.boydti.fawe.object.exception.FaweChunkLoadException;
import com.boydti.fawe.util.CachedTextureUtil;
import com.boydti.fawe.util.RandomTextureUtil;
import com.boydti.fawe.util.ReflectionUtils;
import com.boydti.fawe.util.TextureUtil;
import com.boydti.fawe.util.image.Drawable;
import com.boydti.fawe.util.image.ImageViewer;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.entity.Player;
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
import com.sk89q.worldedit.util.TreeGenerator;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BlockID;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;

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
import java.util.function.Supplier;
import javax.annotation.Nullable;

public class HeightMapMCAGenerator extends MCAWriter implements StreamChange, Drawable, VirtualWorld {
    private final MutableBlockVector3 mutable = new MutableBlockVector3();

    private final DifferentialBlockBuffer blocks;
    protected final DifferentialArray<byte[]> heights;
    protected final DifferentialArray<byte[]> biomes;
    protected final DifferentialArray<char[]> floor;
    protected final DifferentialArray<char[]> main;
    protected DifferentialArray<char[]> overlay;

    protected final CFIPrimitives primitives = new CFIPrimitives();
    private CFIPrimitives oldPrimitives = new CFIPrimitives();

    public final class CFIPrimitives implements Cloneable {
        int waterHeight;
        int floorThickness;
        int worldThickness;
        boolean randomVariation = true;
        int biomePriority;
        char waterOrdinal = BlockID.WATER;
        char bedrockOrdinal = BlockID.BEDROCK;
        boolean modifiedMain;

        @Override
        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof CFIPrimitives)) {
                return false;
            }
            try {
                for (Field field : CFIPrimitives.class.getDeclaredFields()) {
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
            for (Field field : ReflectionUtils.sortFields(CFIPrimitives.class.getDeclaredFields())) {
                Object now = field.get(primitives);
                Object old = field.get(oldPrimitives);
                boolean diff = old != now;
                out.writeBoolean(diff);
                if (diff) {
                    out.writePrimitive(old);
                    out.writePrimitive(now);
                }
            }
            resetPrimitives();
        } catch (Throwable neverHappens) {
            neverHappens.printStackTrace();
        }

        blocks.flushChanges(out);
    }

    public boolean isModified() {
        return blocks.isModified() ||
                heights.isModified() ||
                biomes.isModified() ||
            overlay != null && overlay.isModified() ||
                !primitives.equals(oldPrimitives);
    }

    private void resetPrimitives() throws CloneNotSupportedException {
        oldPrimitives = (CFIPrimitives) primitives.clone();
    }

    @Override
    public void undoChanges(FaweInputStream in) throws IOException {
        heights.undoChanges(in);
        biomes.undoChanges(in);
        floor.undoChanges(in);
        main.undoChanges(in);
        if (in.readBoolean()) overlay.undoChanges(in);
        try {
            for (Field field : ReflectionUtils.sortFields(CFIPrimitives.class.getDeclaredFields())) {
                if (in.readBoolean()) {
                    field.set(primitives, in.readPrimitive(field.getType())); // old
                    in.readPrimitive(field.getType()); // new
                }
            }
            resetPrimitives();
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
            for (Field field : ReflectionUtils.sortFields(CFIPrimitives.class.getDeclaredFields())) {
                if (in.readBoolean()) {
                    in.readPrimitive(field.getType()); // old
                    field.set(primitives, in.readPrimitive(field.getType())); // new
                }
            }
            resetPrimitives();
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
    private Player player;
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
        heights = new DifferentialArray<>(new byte[getArea()]);
        biomes = new DifferentialArray<>(new byte[getArea()]);
        floor = new DifferentialArray<>(new char[getArea()]);
        main = new DifferentialArray<>(new char[getArea()]);

        char stone = BlockID.STONE;
        char grass = BlockTypes.GRASS_BLOCK.getDefaultState().with(PropertyKey.SNOWY, false).getOrdinalChar();
        Arrays.fill(overlay.getCharArray(), stone);
        Arrays.fill(overlay.getCharArray(), grass);
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

    public void setPacketViewer(Player player) {
        this.player = player;
        if (player != null) {
            Location pos = player.getLocation();
            this.chunkOffset = BlockVector2.at(1 + (pos.getBlockX() >> 4), 1 + (pos.getBlockZ() >> 4));
        }
    }

    public Player getOwner() {
        return player;
    }

    private char[][][] getChunkArray(int x, int z) {
        char[][][][][] blocksData = blocks.get();
        if (blocksData == null) return null;
        char[][][][] arr = blocksData[z];
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
            World world = player.getWorld();

            int lenCX = (getWidth() + 15) >> 4;
            int lenCZ = (getLength() + 15) >> 4;


            Location position = player.getLocation();
            int pcx = (position.getBlockX() >> 4) - chunkOffset.getBlockX();
            int pcz = (position.getBlockZ() >> 4) - chunkOffset.getBlockZ();

            int scx = Math.max(0, pcx - 15);
            int scz = Math.max(0, pcz - 15);
            int ecx = Math.min(lenCX - 1, pcx + 15);
            int ecz = Math.min(lenCZ - 1, pcz + 15);

            for (int chunkZ = scz; chunkZ <= ecz; chunkZ++) {
                for (int chunkX = scx; chunkX <= ecx; chunkX++) {

                    refreshChunk(world, chunkX, chunkZ);
                }
            }
        }
    }

    public void refreshChunk(World world, int chunkX, int chunkZ) {
        Supplier<IBlocks> blocksSupplier = () -> getChunk(chunkX, chunkZ);

        int realChunkX = chunkX + chunkOffset.getBlockX();
        int realChunkZ = chunkZ + chunkOffset.getBlockZ();

        ChunkPacket packet = new ChunkPacket(realChunkX, realChunkZ, blocksSupplier, true);
        world.sendFakeChunk(player, packet);
    }

    @Override
    public void sendFakeChunk(@Nullable Player player, ChunkPacket packet) {
        if (this.player != null) {
            player.getWorld().sendFakeChunk(player, packet);
        }
    }

    @Override
    public void refreshChunk(int chunkX, int chunkZ) {
        if (chunkOffset != null && player != null) {
            refreshChunk(player.getWorld(), chunkX, chunkZ);
        }
    }

    public IChunkSet getChunk(int chunkX, int chunkZ) {
        // TODO don't generate new Writeable MCA chunk
        System.out.println("TODO don't generate new Writeable MCA chunk");
        MCAChunk tmp = new MCAChunk();
        int bx = chunkX << 4;
        int bz = chunkZ << 4;
        write(tmp, bx, bx + 15, bz, bz + 15);
        return tmp;
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
            if (primitives.randomVariation) {
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

    public void setBedrock(BlockState bedrock) {
        this.primitives.bedrockOrdinal = bedrock.getOrdinalChar();
    }

    public void setFloorThickness(int floorThickness) {
        this.primitives.floorThickness = floorThickness;
    }

    public void setWorldThickness(int height) {
        this.primitives.worldThickness = height;
    }

    public void setWaterHeight(int waterHeight) {
        this.primitives.waterHeight = waterHeight;
    }

    public void setWater(BlockState water) {
        this.primitives.waterOrdinal = water.getOrdinalChar();
    }

    public void setTextureRandomVariation(boolean randomVariation) {
        this.primitives.randomVariation = randomVariation;
    }

    public boolean getTextureRandomVariation() {
        return this.primitives.randomVariation;
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
        int snowLayer = BlockTypes.SNOW.getDefaultState().getOrdinalChar();
        int snowBlock = BlockTypes.SNOW_BLOCK.getDefaultState().getOrdinalChar();

        char[] floor = this.floor.get();
        byte[] heights = this.heights.get();

        int width = getWidth();
        int length = getLength();

        int minX = min.getBlockX();
        int minZ = min.getBlockZ();

        int maxX = max.getBlockX();
        int maxZ = max.getBlockZ();

        int tableWidth = maxX - minX + 1;
        int tableLength = maxZ - minZ + 1;
        int smoothArea = tableWidth * tableLength;

        long[] copy = new long[smoothArea];
        char[] layers = new char[smoothArea];

        SummedAreaTable table = new SummedAreaTable(copy, layers, tableWidth, radius);
        for (int j = 0; j < iterations; j++) {
            { // Copy to table
                int localIndex = 0;
                int zIndex = minZ * getWidth();
                for (int z = minZ; z <= maxZ; z++, zIndex += getWidth()) {
                    int index = zIndex + minX;
                    for (int x = minX; x <= maxX; x++, index++, localIndex++) {
                        int combined = floor[index];
                        if (BlockTypes.getFromStateOrdinal(combined) == BlockTypes.SNOW) {
                            layers[localIndex] = (char) (((heights[index] & 0xFF) << 3) + (floor[index] >> BlockTypesCache.BIT_OFFSET) - 7);
                        } else {
                            layers[localIndex] = (char) ((heights[index] & 0xFF) << 3);
                        }
                    }
                }
            }
            // Process table
            table.processSummedAreaTable();
            // Copy from table
            int localIndex = 0;
            int zIndex = minZ * getWidth();
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

    private final void setLayerHeight(int index, int height) {
        int blockHeight = height >> 3;
        int layerHeight = height & 0x7;
        setLayerHeight(index, blockHeight, layerHeight);
    }

    private final void setLayerHeight(int index, int blockHeight, int layerHeight) {
        int floorState = floor.get()[index];
        switch (floorState) {
            case BlockID.SNOW:
            case BlockID.SNOW_BLOCK:
                if (layerHeight != 0) {
                    this.heights.setByte(index, (byte) (blockHeight + 1));
                    this.floor.setInt(index, BlockTypes.SNOW.getDefaultState().getOrdinalChar() + layerHeight);
                } else {
                    this.heights.setByte(index, (byte) blockHeight);
                    this.floor.setInt(index, BlockTypes.SNOW_BLOCK.getDefaultState().getOrdinalChar());
                }
                break;
            default:
                this.heights.setByte(index, (byte) blockHeight);
                break;
        }
    }

    private final void setLayerHeightRaw(int index, int height) {
        int blockHeight = height >> 3;
        int layerHeight = height & 0x7;
        setLayerHeightRaw(index, blockHeight, layerHeight);
    }

    private final void setLayerHeightRaw(int index, int blockHeight, int layerHeight) {
        int floorState = floor.get()[index];
        switch (floorState) {
            case BlockID.SNOW:
            case BlockID.SNOW_BLOCK:
                if (layerHeight != 0) {
                    this.heights.getByteArray()[index] = (byte) (blockHeight + 1);
                    this.overlay.getCharArray()[index] = (char) (BlockTypes.SNOW.getDefaultState().getOrdinalChar() + layerHeight);
                } else {
                    this.heights.getByteArray()[index] = (byte) blockHeight;
                    this.overlay.getCharArray()[index] = BlockTypes.SNOW_BLOCK.getDefaultState().getOrdinalChar();
                }
                break;
            default:
                this.heights.getByteArray()[index] = (byte) blockHeight;
                break;
        }
    }

    private void smooth(BufferedImage img, Mask mask, boolean white, int radius, int iterations) {
        char[] floor = this.floor.get();
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
                    if (BlockTypes.getFromStateOrdinal(combined) == BlockTypes.SNOW) {
                        layers[i] = (char) (((heights[i] & 0xFF) << 3) + (floor[i] >> BlockTypesCache.BIT_OFFSET) - 7);
                    } else {
                        layers[i] = (char) ((heights[i] & 0xFF) << 3);
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
                Transform transform = holder.getTransform();
                if (transform.isIdentity()) {
                    clipboard.paste(this, mutable, false);
                } else {
                    clipboard.paste(this, mutable, false, transform);
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
        int scaledRarity = 256 * rarity / 100;
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
                Transform transform = holder.getTransform();
                if (transform.isIdentity()) {
                    clipboard.paste(this, mutable, false);
                } else {
                    clipboard.paste(this, mutable, false, transform);
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
    public Player getPlayer() {
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

    private boolean setBlock(int x, int y, int z, char combined) {
        int index = z * getWidth() + x;
        if (index < 0 || index >= getArea()) return false;
        int height = heights.getByte(index) & 0xFF;
        switch (y - height) {
            case 0:
                floor.setInt(index, combined);
                return true;
            case 1:
                char mainId = overlay.getChar(index);
                char floorId = overlay.getChar(index);
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
//    private FaweChunk getSnapshot(final MCAChunk chunk, int chunkX, int chunkZ) {
//        return new LazyFaweChunk<MCAChunk>(this, chunkX, chunkZ) {
//            @Override
//            public MCAChunk getChunk() {
//                MCAChunk tmp = chunk;
//                if (tmp == null) {
//                    tmp = new MCAChunk();
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
//                MCAChunk cached = getCachedChunk();
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
                Player esPlayer = curES.getPlayer();
                UUID uuid = esPlayer != null ? esPlayer.getUniqueId() : EditSession.CONSOLE;
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
            World world = player.getWorld();

            int lenCX = getWidth() + 15 >> 4;
            int lenCZ = getLength() + 15 >> 4;

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
                    world.refreshChunk(cx + OX, cz + OZ);
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
    public BiomeType getBiomeType(int x, int z) throws FaweChunkLoadException {
        int index = z * getWidth() + x;
        if (index < 0 || index >= getArea()) index = Math.floorMod(index, getArea());
        return BiomeTypes.get(biomes.getByte(index));
    }

    public int getOrdinal(int x, int y, int z) throws FaweChunkLoadException {
        int index = z * getWidth() + x;
        if (y < 0) return 0;
        if (index < 0 || index >= getArea() || x < 0 || x >= getWidth()) return 0;
        int height = heights.getByte(index) & 0xFF;
        if (y > height) {
            if (y == height + 1) {
                return overlay != null ? overlay.getChar(index) : 0;
            }
            if (blocks != null) {
                short chunkX = (short) (x >> 4);
                short chunkZ = (short) (z >> 4);
                char[][][] map = getChunkArray(chunkX, chunkZ);
                if (map != null) {
                    int combined = get(map, x, y, z);
                    if (combined != 0) {
                        return combined;
                    }
                }
            }
            if (y <= primitives.waterHeight) {
                return primitives.waterOrdinal;
            }
            return 0;
        } else if (y == height) {
            return overlay.getChar(index);
        } else {
            if (blocks != null) {
                short chunkX = (short) (x >> 4);
                short chunkZ = (short) (z >> 4);
                char[][][] map = getChunkArray(chunkX, chunkZ);
                if (map != null) {
                    int combined = get(map, x, y, z);
                    if (combined != 0) {
                        return combined;
                    }
                }
            }
            return overlay.getChar(index);
        }
    }

    @Override
    public boolean setBlock(int x, int y, int z, BlockStateHolder block) throws WorldEditException {
        return this.setBlock(x, y, z, block.getOrdinalChar());
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
        return BlockState.getFromOrdinal(overlay.getChar(index));
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
        floor.setInt(index, block.getOrdinalChar());
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return BlockState.getFromOrdinal(getOrdinal(x, y, z));
    }

    @Override
    public int getNearestSurfaceLayer(int x, int z, int y, int minY, int maxY) {
        int index = z * getWidth() + x;
        if (index < 0 || index >= getArea()) index = Math.floorMod(index, getArea());
        return ((heights.getByte(index) & 0xFF) << 3) + (overlay.getChar(index) & 0xFF) + 1;
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

    @Override
    public BufferedImage draw() {
        return new CFIDrawer(this).draw();
    }

    public void setBiomePriority(int value) {
        this.primitives.biomePriority = value * 65536 / 100 - 32768;
    }

    public int getBiomePriority() {
        return (primitives.biomePriority + 32768) * 100 / 65536;
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
            char[] mainArr = main.get();
            char[] floorArr = floor.get();
            byte[] biomesArr = biomes.get();

            int index = 0;
            char[] buffer = new char[2];
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
                    if (textureUtil.getIsBlockCloserThanBiome(buffer, color, primitives.biomePriority)) {
                        char combined = buffer[0];
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
        int heightIndex = img.getHeight() - 1;

        biomes.record(() -> floor.record(() -> main.record(() -> {
            char[] mainArr = main.get();
            char[] floorArr = floor.get();
            byte[] biomesArr = biomes.get();

            char[] buffer = new char[2];
            int index = 0;
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++, index++) {
                    int color = img.getRGB(x, y);
                    if (textureUtil.getIsBlockCloserThanBiome(buffer, color, primitives.biomePriority)) {
                        char combined = buffer[0];
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

        biomes.record(() -> {
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
        });
    }

    public void setColor(BufferedImage img, BufferedImage mask, boolean white) {
        if (img.getWidth() != getWidth() || img.getHeight() != getLength())
            throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
        if (mask.getWidth() != getWidth() || mask.getHeight() != getLength())
            throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
        primitives.modifiedMain = true;
        TextureUtil textureUtil = getTextureUtil();

        floor.record(() -> main.record(() -> {
            char[] mainArr = main.get();
            char[] floorArr = floor.get();

            int index = 0;
            for (int z = 0; z < getLength(); z++) {
                for (int x = 0; x < getWidth(); x++, index++) {
                    int height = mask.getRGB(x, z) & 0xFF;
                    if (height == 255 || height > 0 && !white && ThreadLocalRandom.current()
                            .nextInt(256) <= height) {
                        int color = img.getRGB(x, z);
                        BlockType block = textureUtil.getNearestBlock(color);
                        if (block != null) {
                            char combined = block.getDefaultState().getOrdinalChar();
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
        primitives.modifiedMain = true;
        TextureUtil textureUtil = getTextureUtil();


        floor.record(() -> main.record(() -> {
            char[] mainArr = main.get();
            char[] floorArr = floor.get();

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
                            char combined = block.getDefaultState().getOrdinalChar();
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
        primitives.modifiedMain = true;
        TextureUtil textureUtil = getTextureUtil();

        floor.record(() -> main.record(() -> {
            char[] mainArr = main.get();
            char[] floorArr = floor.get();

            int index = 0;
            for (int z = 0; z < img.getHeight(); z++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int color = img.getRGB(x, z);
                    BlockType block = textureUtil.getNearestBlock(color);
                    if (block != null) {
                        char combined = block.getDefaultState().getOrdinalChar();
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
            char[] mainArr = main.get();
            char[] floorArr = floor.get();

            int index = 0;
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int color = img.getRGB(x, y);
                    BlockType[] layer = textureUtil.getNearestLayer(color);
                    if (layer != null) {
                        floorArr[index] = layer[0].getDefaultState().getOrdinalChar();
                        mainArr[index] = layer[1].getDefaultState().getOrdinalChar();
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
            setOverlay(img, ((BlockStateHolder) pattern).getOrdinalChar(), white);
        } else if (pattern instanceof BlockType) {
            setOverlay(img, ((BlockType) pattern).getDefaultState().getOrdinalChar(), white);
        } else {
            if (img.getWidth() != getWidth() || img.getHeight() != getLength())
                throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
            if (overlay == null) {
                overlay = new DifferentialArray<>(new char[getArea()]);
            }

            overlay.record(() -> {
                char[] overlayArr = overlay.get();
                int index = 0;
                for (int z = 0; z < getLength(); z++) {
                    mutable.mutZ(z);
                    for (int x = 0; x < getWidth(); x++, index++) {
                        int height = img.getRGB(x, z) & 0xFF;
                        if (height == 255 || height > 0 && !white && ThreadLocalRandom.current()
                                .nextInt(256) <= height) {
                            mutable.mutX(x);
                            mutable.mutY(height);
                            overlayArr[index] = pattern.apply(mutable).getOrdinalChar();
                        }
                    }
                }
            });

        }
    }

    public void setMain(BufferedImage img, Pattern pattern, boolean white) {
        if (pattern instanceof BlockStateHolder) {
            setMain(img, ((BlockStateHolder) pattern).getOrdinalChar(), white);
        } else {
            if (img.getWidth() != getWidth() || img.getHeight() != getLength())
                throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
            primitives.modifiedMain = true;

            main.record(() -> {
                char[] mainArr = main.get();
                int index = 0;
                for (int z = 0; z < getLength(); z++) {
                    mutable.mutZ(z);
                    for (int x = 0; x < getWidth(); x++, index++) {
                        int height = img.getRGB(x, z) & 0xFF;
                        if (height == 255 || height > 0 && !white && ThreadLocalRandom.current()
                                .nextInt(256) <= height) {
                            mutable.mutX(x);
                            mutable.mutY(height);
                            mainArr[index] = pattern.apply(mutable).getOrdinalChar();
                        }
                    }
                }
            });
        }
    }

    public void setFloor(BufferedImage img, Pattern pattern, boolean white) {
        if (pattern instanceof BlockStateHolder) {
            setFloor(img, ((BlockStateHolder) pattern).getOrdinalChar(), white);
        } else {
            if (img.getWidth() != getWidth() || img.getHeight() != getLength())
                throw new IllegalArgumentException("Input image dimensions do not match the current height map!");

            floor.record(() -> {
                char[] floorArr = floor.get();
                int index = 0;
                for (int z = 0; z < getLength(); z++) {
                    mutable.mutZ(z);
                    for (int x = 0; x < getWidth(); x++, index++) {
                        int height = img.getRGB(x, z) & 0xFF;
                        if (height == 255 || height > 0 && !white && ThreadLocalRandom.current()
                                .nextInt(256) <= height) {
                            mutable.mutX(x);
                            mutable.mutY(height);
                            floorArr[index] = pattern.apply(mutable).getOrdinalChar();
                        }
                    }
                }
            });
        }
    }

    public void setColumn(BufferedImage img, Pattern pattern, boolean white) {
        if (pattern instanceof BlockStateHolder) {
            setColumn(img, ((BlockStateHolder) pattern).getOrdinalChar(), white);
        } else {
            if (img.getWidth() != getWidth() || img.getHeight() != getLength())
                throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
            primitives.modifiedMain = true;

            main.record(() -> floor.record(() -> {
                char[] floorArr = floor.get();
                char[] mainArr = main.get();
                int index = 0;
                for (int z = 0; z < getLength(); z++) {
                    mutable.mutZ(z);
                    for (int x = 0; x < getWidth(); x++, index++) {
                        int height = img.getRGB(x, z) & 0xFF;
                        if (height == 255 || height > 0 && !white && ThreadLocalRandom.current()
                                .nextInt(256) <= height) {
                            mutable.mutX(x);
                            mutable.mutY(height);
                            char combined = pattern.apply(mutable).getOrdinalChar();
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
            setOverlay(mask, ((BlockStateHolder) pattern).getOrdinalChar());
        } else {
            int index = 0;
            if (overlay == null) overlay = new DifferentialArray<>(new char[getArea()]);
            for (int z = 0; z < getLength(); z++) {
                mutable.mutZ(z);
                for (int x = 0; x < getWidth(); x++, index++) {
                    int y = heights.getByte(index) & 0xFF;
                    mutable.mutX(x);
                    mutable.mutY(y);
                    if (mask.test(mutable)) {
                        overlay.setInt(index, pattern.apply(mutable).getOrdinalChar());
                    }
                }
            }
        }
    }

    public void setFloor(Mask mask, Pattern pattern) {
        if (pattern instanceof BlockStateHolder) {
            setFloor(mask, ((BlockStateHolder) pattern).getOrdinalChar());
        } else {
            int index = 0;
            for (int z = 0; z < getLength(); z++) {
                mutable.mutZ(z);
                for (int x = 0; x < getWidth(); x++, index++) {
                    int y = heights.getByte(index) & 0xFF;
                    mutable.mutX(x);
                    mutable.mutY(y);
                    if (mask.test(mutable)) {
                        floor.setInt(index, pattern.apply(mutable).getOrdinalChar());
                    }
                }
            }
        }
    }

    public void setMain(Mask mask, Pattern pattern) {
        if (pattern instanceof BlockStateHolder) {
            setMain(mask, ((BlockStateHolder) pattern).getOrdinalChar());
        } else {
            primitives.modifiedMain = true;
            int index = 0;
            for (int z = 0; z < getLength(); z++) {
                mutable.mutZ(z);
                for (int x = 0; x < getWidth(); x++, index++) {
                    int y = heights.getByte(index) & 0xFF;
                    mutable.mutX(x);
                    mutable.mutY(y);
                    if (mask.test(mutable)) {
                        main.setInt(index, pattern.apply(mutable).getOrdinalChar());
                    }
                }
            }
        }
    }

    public void setColumn(Mask mask, Pattern pattern) {
        if (pattern instanceof BlockStateHolder) {
            setColumn(mask, ((BlockStateHolder) pattern).getOrdinalChar());
        } else {
            primitives.modifiedMain = true;
            int index = 0;
            for (int z = 0; z < getLength(); z++) {
                mutable.mutZ(z);
                for (int x = 0; x < getWidth(); x++, index++) {
                    int y = heights.getByte(index) & 0xFF;
                    mutable.mutX(x);
                    mutable.mutY(y);
                    if (mask.test(mutable)) {
                        int combined = pattern.apply(mutable).getOrdinalChar();
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
            setFloor(((BlockStateHolder) value).getOrdinalChar());
        } else {
            floor.record(() -> {
                char[] floorArr = floor.get();
                int index = 0;
                for (int z = 0; z < getLength(); z++) {
                    mutable.mutZ(z);
                    for (int x = 0; x < getWidth(); x++, index++) {
                        int y = heights.getByte(index) & 0xFF;
                        mutable.mutX(x);
                        mutable.mutY(y);
                        floorArr[index] = value.apply(mutable).getOrdinalChar();
                    }
                }
            });
        }
    }

    public void setColumn(Pattern value) {
        if (value instanceof BlockStateHolder) {
            setColumn(((BlockStateHolder) value).getOrdinalChar());
        } else {
            main.record(() -> floor.record(() -> {
                char[] floorArr = floor.get();
                char[] mainArr = main.get();
                int index = 0;
                for (int z = 0; z < getLength(); z++) {
                    mutable.mutZ(z);
                    for (int x = 0; x < getWidth(); x++, index++) {
                        int y = heights.getByte(index) & 0xFF;
                        mutable.mutX(x);
                        mutable.mutY(y);
                        char combined = value.apply(mutable).getOrdinalChar();
                        mainArr[index] = combined;
                        floorArr[index] = combined;
                    }
                }
            }));
        }
    }

    public void setMain(Pattern value) {
        if (value instanceof BlockStateHolder) {
            setMain(((BlockStateHolder) value).getOrdinalChar());
        } else {
            main.record(() -> {
                char[] mainArr = main.get();
                int index = 0;
                for (int z = 0; z < getLength(); z++) {
                    mutable.mutZ(z);
                    for (int x = 0; x < getWidth(); x++, index++) {
                        int y = heights.getByte(index) & 0xFF;
                        mutable.mutX(x);
                        mutable.mutY(y);
                        mainArr[index] = value.apply(mutable).getOrdinalChar();
                    }
                }
            });
        }
    }

    public void setOverlay(Pattern value) {
        if (overlay == null) overlay = new DifferentialArray<>(new char[getArea()]);
        if (value instanceof BlockStateHolder) {
            setOverlay(((BlockStateHolder) value).getOrdinalChar());
        } else {
            overlay.record(() -> {
                char[] overlayArr = overlay.get();
                int index = 0;
                for (int z = 0; z < getLength(); z++) {
                    mutable.mutZ(z);
                    for (int x = 0; x < getWidth(); x++, index++) {
                        int y = heights.getByte(index) & 0xFF;
                        mutable.mutX(x);
                        mutable.mutY(y);
                        overlayArr[index] = value.apply(mutable).getOrdinalChar();
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
        byte[] heights = this.heights.get();
        byte[] biomes = this.biomes.get();
        char[] main = this.main.get();
        char[] floor = this.floor.get();
        char[] overlay = this.overlay != null ? this.overlay.get() : null;
        try {
            int[] indexes = FaweCache.IMP.INDEX_STORE.get();

            int index;
            int maxY = 0;
            int minY = Integer.MAX_VALUE;
            byte[] heightMap = chunk.biomes;
            int globalIndex;
            for (int z = csz; z <= cez; z++) {
                globalIndex = z * getWidth() + csx;
                index = (z & 15) << 4;
                for (int x = csx; x <= cex; x++, index++, globalIndex++) {
                    indexes[index] = globalIndex;
                    int height = heights[globalIndex] & 0xFF;
                    heightMap[index] = (byte) height;
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
            if (primitives.waterHeight != 0) {
                int maxIndex = primitives.waterHeight << 8;
                Arrays.fill(chunk.blocks, 0, maxIndex, primitives.waterOrdinal);
            }

            if (primitives.modifiedMain) { // If the main block is modified, we can't short circuit this
                for (int z = csz; z <= cez; z++) {
                    index = (z & 15) << 4;
                    for (int x = csx; x <= cex; x++, index++) {
                        globalIndex = indexes[index];
                        char mainCombined = main[globalIndex];
                        for (int y = 0; y < minY; y++) {
                            chunk.blocks[index + (y << 8)] = mainCombined;
                        }
                    }
                }
            } else {
                int maxIndex = minY << 8;
                Arrays.fill(chunk.blocks, 0, maxIndex, (char) BlockID.STONE);
            }

            final boolean hasFloorThickness = primitives.floorThickness != 0 || primitives.worldThickness != 0;
            if (primitives.worldThickness != 0) {
                int endLayer = minY - primitives.worldThickness + 1 >> 4;
                for (int layer = 0; layer < endLayer; layer++) {
                    chunk.hasSections[layer] = false;
                }
            }

            for (int z = csz; z <= cez; z++) {
                index = (z & 15) << 4;
                for (int x = csx; x <= cex; x++, index++) {
                    globalIndex = indexes[index];
                    int height = heightMap[index] & 0xFF;
                    int maxMainY = height;
                    int minMainY = minY;

                    char mainCombined = main[globalIndex];

                    char floorCombined = floor[globalIndex];
                    if (hasFloorThickness) {
                        if (x > 0) maxMainY = Math.min(heights[globalIndex - 1] & 0xFF, maxMainY);
                        if (x < getWidth() - 1) maxMainY = Math.min(heights[globalIndex + 1] & 0xFF, maxMainY);
                        if (z > 0) maxMainY = Math.min(heights[globalIndex - getWidth()] & 0xFF, maxMainY);
                        if (z < getLength() - 1) maxMainY = Math.min(heights[globalIndex + getWidth()] & 0xFF, maxMainY);

                        int min = maxMainY;

                        if (primitives.floorThickness != 0) {
                            maxMainY = Math.max(0, maxMainY - (primitives.floorThickness - 1));
                            for (int y = maxMainY; y <= height; y++) {
                                chunk.blocks[index + (y << 8)] = floorCombined;
                            }
                        }
                        else {
                            chunk.blocks[index + (height << 8)] = floorCombined;
                        }

                        if (primitives.worldThickness != 0) {
                            minMainY = Math.max(minY, min - primitives.worldThickness + 1);
                            for (int y = minY; y < minMainY; y++) {
                                chunk.blocks[index + (y << 8)] = BlockID.AIR;
                            }
                        }

                    } else {
                        chunk.blocks[index + (height << 8)] = floorCombined;
                    }

                    for (int y = minMainY; y < maxMainY; y++) {
                        chunk.blocks[index + (y << 8)] = mainCombined;
                    }

                    if (hasOverlay) {
                        char overlayCombined = overlay[globalIndex];
                        int overlayIndex = index + (height + 1 << 8);
                        chunk.blocks[overlayIndex] = overlayCombined;
                    }

                    if (primitives.bedrockOrdinal != 0) {
                        chunk.blocks[index] = primitives.bedrockOrdinal;
                    }
                }
            }

            char[][][] localBlocks = getChunkArray(chunk.getX(), chunk.getZ());
            if (localBlocks != null) {
                index = 0;
                for (int layer = 0; layer < 16; layer++) {
                    int by = layer << 4;
                    int ty = by + 15;
                    for (int y = by; y <= ty; y++, index += 256) {
                        char[][] yBlocks = localBlocks[y];
                        if (yBlocks != null) {
                            chunk.hasSections[layer] = true;
                            for (int z = 0; z < yBlocks.length; z++) {
                                char[] zBlocks = yBlocks[z];
                                if (zBlocks != null) {
                                    int zIndex = index + (z << 4);
                                    for (int x = 0; x < zBlocks.length; x++, zIndex++) {
                                        char combined = zBlocks[x];
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

    private void setUnsafe(char[][][] map, char combined, int x, int y, int z) {
        char[][] yMap = map[y];
        if (yMap == null) {
            map[y] = yMap = new char[16][];
        }
        char[] zMap = yMap[z];
        if (zMap == null) {
            yMap[z] = zMap = new char[16];
        }
        zMap[x] = combined;
    }

    private int get(char[][][] map, int x, int y, int z) {
        char[][] yMap = map[y];
        if (yMap == null) {
            return 0;
        }
        char[] zMap = yMap[z & 15];
        if (zMap == null) {
            return 0;
        }
        return zMap[x & 15];
    }

    private void setOverlay(Mask mask, int combined) {
        int index = 0;
        if (overlay == null) overlay = new DifferentialArray<>(new char[getArea()]);
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
        primitives.modifiedMain = true;
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
        primitives.modifiedMain = true;
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

    private void setFloor(char value) {
        floor.record(() -> Arrays.fill(floor.get(), value));
    }

    private void setColumn(char value) {
        setFloor(value);
        setMain(value);
    }

    private void setMain(char value) {
        primitives.modifiedMain = true;
        main.record(() -> Arrays.fill(main.get(), value));
    }

    private void setOverlay(char value) {
        if (overlay == null) overlay = new DifferentialArray<>(new char[getArea()]);
        overlay.record(() -> Arrays.fill(overlay.get(), value));
    }

    private void setOverlay(BufferedImage img, char combined, boolean white) {
        if (img.getWidth() != getWidth() || img.getHeight() != getLength())
            throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
        if (overlay == null) overlay = new DifferentialArray<>(new char[getArea()]);

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

    private void setMain(BufferedImage img, char combined, boolean white) {
        if (img.getWidth() != getWidth() || img.getHeight() != getLength())
            throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
        primitives.modifiedMain = true;

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

    private void setFloor(BufferedImage img, char combined, boolean white) {
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

    private void setColumn(BufferedImage img, char combined, boolean white) {
        if (img.getWidth() != getWidth() || img.getHeight() != getLength())
            throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
        primitives.modifiedMain = true;

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
    public boolean setTile(int x, int y, int z, CompoundTag tile) throws WorldEditException {
        return false;
    }

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
    public boolean generateTree(TreeGenerator.TreeType type, EditSession editSession, BlockVector3 position) throws MaxChangedBlocksException {
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

    @Override
    public IChunkGet get(int x, int z) {
        Fawe.debug("Should not be using buffering with HMMG");
        return new FallbackChunkGet(this, x, z);
    }
}
