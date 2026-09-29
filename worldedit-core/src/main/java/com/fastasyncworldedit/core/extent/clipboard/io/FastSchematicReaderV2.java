package com.fastasyncworldedit.core.extent.clipboard.io;

import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.extent.clipboard.LinearClipboard;
import com.fastasyncworldedit.core.extent.clipboard.SimpleClipboard;
import com.fastasyncworldedit.core.internal.io.FastByteArrayOutputStream;
import com.fastasyncworldedit.core.internal.io.FastByteArraysInputStream;
import com.fastasyncworldedit.core.internal.io.FaweInputStream;
import com.fastasyncworldedit.core.internal.io.FaweOutputStream;
import com.fastasyncworldedit.core.jnbt.streamer.StreamDelegate;
import com.fastasyncworldedit.core.jnbt.streamer.ValueReader;
import com.sk89q.jnbt.LinBusConverter;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.NBTSchematicReader;
import com.sk89q.worldedit.internal.Constants;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.DataFixer;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import com.sk89q.worldedit.world.entity.EntityType;
import com.sk89q.worldedit.world.entity.EntityTypes;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Reads schematic files using the Sponge Schematic Specification.
 */
public class FastSchematicReaderV2 extends NBTSchematicReader {

    private static final Logger LOGGER = LogManagerCompat.getLogger();
    private final NBTInputStream inputStream;
    private final DataFixer fixer;
    private int dataVersion = -1;
    private int version = -1;
    private int faweWritten = -1;

    private FastByteArrayOutputStream blocksOut;
    private FaweOutputStream blocks;

    private FastByteArrayOutputStream biomesOut;
    private FaweOutputStream biomes;

    private List<Map<String, Object>> tiles;
    private List<Map<String, Object>> entities;
    private int width;
    private int height;
    private int length;
    private int offsetX;
    private int offsetY;
    private int offsetZ;
    private char[] palette;
    private char[] biomePalette;
    private BlockVector3 min = BlockVector3.ZERO;
    private boolean brokenEntities = false;
    private boolean isWorldEdit = false;


    /**
     * Create a new instance.
     *
     * @param inputStream the input stream to read from
     */
    public FastSchematicReaderV2(NBTInputStream inputStream) {
        checkNotNull(inputStream);
        this.inputStream = inputStream;
        this.fixer = WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.WORLD_EDITING).getDataFixer();
    }

    public void setBrokenEntities(boolean brokenEntities) {
        this.brokenEntities = brokenEntities;
    }

    private String fix(String palettePart) {
        if (fixer == null || dataVersion == -1) {
            return palettePart;
        }
        return fixer.fixUp(DataFixer.FixTypes.BLOCK_STATE, palettePart, dataVersion);
    }

    private CompoundTag fixBlockEntity(CompoundTag tag) {
        if (fixer == null || dataVersion == -1) {
            return tag;
        }
        return (CompoundTag) LinBusConverter.fromLinBus(fixer.fixUp(
                DataFixer.FixTypes.BLOCK_ENTITY,
                tag.toLinTag(),
                dataVersion
        ));
    }

    private CompoundTag fixEntity(CompoundTag tag) {
        if (fixer == null || dataVersion == -1) {
            return tag;
        }
        return (CompoundTag) LinBusConverter.fromLinBus(fixer.fixUp(
                DataFixer.FixTypes.ENTITY,
                tag.toLinTag(),
                dataVersion
        ));
    }

    private String fixBiome(String biomePalettePart) {
        if (fixer == null || dataVersion == -1) {
            return biomePalettePart;
        }
        return fixer.fixUp(DataFixer.FixTypes.BIOME, biomePalettePart, dataVersion);
    }

    public StreamDelegate createVersionDelegate() {
        StreamDelegate root = new StreamDelegate();
        StreamDelegate schematic = root.add("Schematic");
        schematic.add("DataVersion").withInt((i, v) -> dataVersion = v);
        schematic.add("Version").withInt((i, v) -> {
            version = v;
            if (v == 1 && dataVersion == -1) { // DataVersion might not be present, assume 1.13.2
                dataVersion = Constants.DATA_VERSION_MC_1_13_2;
            }
        });
        return root;
    }

    public StreamDelegate createDelegate() {
        StreamDelegate root = new StreamDelegate();
        StreamDelegate schematic = root.add("Schematic");
        schematic.add("Width").withInt((i, v) -> width = v);
        schematic.add("Height").withInt((i, v) -> height = v);
        schematic.add("Length").withInt((i, v) -> length = v);
        schematic.add("Offset").withValue((ValueReader<int[]>) (index, v) -> min = BlockVector3.at(v[0], v[1], v[2]));

        StreamDelegate metadata = schematic.add("Metadata");
        metadata.add("WEOffsetX").withInt((i, v) -> offsetX = v);
        metadata.add("WEOffsetY").withInt((i, v) -> offsetY = v);
        metadata.add("WEOffsetZ").withInt((i, v) -> offsetZ = v);
        metadata.add("FAWEVersion").withInt((i, v) -> faweWritten = v);

        StreamDelegate worldEditSection = metadata.add("WorldEdit");
        worldEditSection.withValue((ValueReader<Object>) (index, v) -> isWorldEdit = true);


        StreamDelegate paletteDelegate = schematic.add("Palette");
        paletteDelegate.withValue((ValueReader<Map<String, Object>>) (ignore, v) -> {
            palette = new char[v.size()];
            for (Entry<String, Object> entry : v.entrySet()) {
                BlockState state;
                String palettePart = fix(entry.getKey());
                try {
                    state = BlockState.get(palettePart);
                } catch (InputParseException ignored) {
                    LOGGER.warn("Invalid BlockState in palette: {}. Block will be replaced with air.", palettePart);
                    state = BlockTypes.AIR.getDefaultState();
                }
                int index = (int) entry.getValue();
                palette[index] = (char) state.getOrdinal();
            }
        });
        StreamDelegate blockData = schematic.add("BlockData");
        blockData.withInfo((length, type) -> {
            blocksOut = new FastByteArrayOutputStream();
            blocks = new FaweOutputStream(new LZ4BlockOutputStream(blocksOut));
        });
        blockData.withInt((index, value) -> blocks.write(value));

        StreamDelegate tilesDelegate = schematic.add("BlockEntities");
        tilesDelegate.withInfo((length, type) -> tiles = new ArrayList<>(length));
        tilesDelegate.withElem((ValueReader<Map<String, Object>>) (index, tile) -> tiles.add(tile));

        // Keep this here so schematics created with FAWE before TileEntities was fixed to BlockEntities still work
        StreamDelegate compatTilesDelegate = schematic.add("TileEntities");
        compatTilesDelegate.withInfo((length, type) -> tiles = new ArrayList<>(length));
        compatTilesDelegate.withElem((ValueReader<Map<String, Object>>) (index, tile) -> tiles.add(tile));

        StreamDelegate entitiesDelegate = schematic.add("Entities");
        entitiesDelegate.withInfo((length, type) -> entities = new ArrayList<>(length));
        entitiesDelegate.withElem((ValueReader<Map<String, Object>>) (index, entity) -> entities.add(entity));

        StreamDelegate biomePaletteDelegate = schematic.add("BiomePalette");
        biomePaletteDelegate.withValue((ValueReader<Map<String, Object>>) (ignore, v) -> {
            biomePalette = new char[v.size()];
            for (Entry<String, Object> entry : v.entrySet()) {
                BiomeType biome = null;
                try {
                    String biomePalettePart = fixBiome(entry.getKey());
                    biome = BiomeTypes.get(biomePalettePart);
                } catch (InputParseException e) {
                    e.printStackTrace();
                }
                int index = (int) entry.getValue();
                biomePalette[index] = (char) biome.getInternalId();
            }
        });
        StreamDelegate biomeData = schematic.add("BiomeData");
        biomeData.withInfo((length, type) -> {
            biomesOut = new FastByteArrayOutputStream();
            biomes = new FaweOutputStream(new LZ4BlockOutputStream(biomesOut));
        });
        biomeData.withInt((index, value) -> biomes.write(value));
        return root;
    }

    private BlockState getBlockState(int id) {
        return BlockTypesCache.states[palette[id]];
    }

    private BiomeType getBiomeType(FaweInputStream fis) throws IOException {
        char biomeId = biomePalette[fis.readVarInt()];
        return BiomeTypes.get(biomeId);
    }

    @Override
    public Clipboard read(UUID uuid, Function<BlockVector3, Clipboard> createOutput) throws IOException {
        StreamDelegate root = createDelegate();
        StreamDelegate versions = createVersionDelegate();
        inputStream.mark(Integer.MAX_VALUE);
        inputStream.readNamedTagLazy(versions);
        inputStream.reset();
        inputStream.readNamedTagLazy(root);

        if (version != 1 && version != 2) {
            throw new IOException("This schematic version is not supported; Version: " + version
                    + ", DataVersion: " + dataVersion + ". It's very likely your schematic has an invalid file extension," +
                    " if the schematic has been created on a version lower than 1.13.2, the extension MUST be `.schematic`," +
                    " elsewise the schematic can't be read properly. If you are using a litematica schematic, it is not supported!");
        }

        if (blocks != null) {
            blocks.close();
        }
        if (biomes != null) {
            biomes.close();
        }
        blocks = null;
        biomes = null;

        BlockVector3 dimensions = BlockVector3.at(width, height, length);
        BlockVector3 origin;
        if (offsetX != Integer.MIN_VALUE && offsetY != Integer.MIN_VALUE && offsetZ != Integer.MIN_VALUE) {
            origin = BlockVector3.at(-offsetX, -offsetY, -offsetZ);
        } else {
            origin = BlockVector3.ZERO;
        }

        Clipboard clipboard = createOutput.apply(dimensions);

        if (blocksOut != null && blocksOut.getSize() != 0) {
            try (FaweInputStream fis = new FaweInputStream(new LZ4BlockInputStream(new FastByteArraysInputStream(blocksOut.toByteArrays())))) {
                if (clipboard instanceof LinearClipboard linear) {
                    int volume = width * height * length;
                    if (palette.length < 128) {
                        for (int index = 0; index < volume; index++) {
                            int ordinal = fis.read();
                            linear.setBlock(index, getBlockState(ordinal));
                        }
                    } else {
                        for (int index = 0; index < volume; index++) {
                            int ordinal = fis.readVarInt();
                            linear.setBlock(index, getBlockState(ordinal));
                        }
                    }
                } else {
                    if (palette.length < 128) {
                        for (int y = 0; y < height; y++) {
                            for (int z = 0; z < length; z++) {
                                for (int x = 0; x < width; x++) {
                                    int ordinal = fis.read();
                                    clipboard.setBlock(x, y, z, getBlockState(ordinal));
                                }
                            }
                        }
                    } else {
                        for (int y = 0; y < height; y++) {
                            for (int z = 0; z < length; z++) {
                                for (int x = 0; x < width; x++) {
                                    int ordinal = fis.readVarInt();
                                    clipboard.setBlock(x, y, z, getBlockState(ordinal));
                                }
                            }
                        }
                    }
                }
            }
        }
        if (biomesOut != null && biomesOut.getSize() != 0 && biomePalette != null && biomePalette.length > 0) {
            try (FaweInputStream fis = new FaweInputStream(new LZ4BlockInputStream(new FastByteArraysInputStream(biomesOut.toByteArrays())))) {
                for (int z = 0; z < length; z++) {
                    for (int x = 0; x < width; x++) {
                        BiomeType biome = getBiomeType(fis);
                        for (int y = 0; y < height; y++) {
                            clipboard.setBiome(x, y, z, biome);
                        }
                    }
                }
            }
        }
        // tiles
        if (tiles != null && !tiles.isEmpty()) {
            for (Map<String, Object> tileRaw : tiles) {
                CompoundTag tile = FaweCache.INSTANCE.asTag(tileRaw);

                int[] pos = tile.getIntArray("Pos");
                int x;
                int y;
                int z;
                if (pos.length != 3) {
                    if (!tile.containsKey("x") || !tile.containsKey("y") || !tile.containsKey("z")) {
                        return null;
                    }
                    x = tile.getInt("x");
                    y = tile.getInt("y");
                    z = tile.getInt("z");
                } else {
                    x = pos[0];
                    y = pos[1];
                    z = pos[2];
                }
                Map<String, Tag<?, ?>> values = new HashMap<>(tile.getValue());
                Tag id = values.get("Id");
                if (id != null) {
                    values.put("x", new IntTag(x));
                    values.put("y", new IntTag(y));
                    values.put("z", new IntTag(z));
                    values.put("id", id);
                } else if (values.get("id") != null) {
                    values.put("x", new IntTag(x));
                    values.put("y", new IntTag(y));
                    values.put("z", new IntTag(z));
                }
                values.remove("Id");
                values.remove("Pos");

                clipboard.setTile(
                        x,
                        y,
                        z,
                        fixBlockEntity(new CompoundTag(values))
                );
            }
        }

        // entities
        if (entities != null && !entities.isEmpty()) {
            for (Map<String, Object> entRaw : entities) {
                Map<String, Tag<?, ?>> value = new HashMap<>(FaweCache.INSTANCE.asTag(entRaw).getValue());
                StringTag id = (StringTag) value.get("Id");
                if (id == null) {
                    id = (StringTag) value.get("id");
                    if (id == null) {
                        continue;
                    }
                }
                value.put("id", id);
                value.remove("Id");

                EntityType type = EntityTypes.parse(id.getValue());
                if (type != null) {
                    final CompoundTag ent = fixEntity(new CompoundTag(value));
                    BaseEntity state = new BaseEntity(type, ent);
                    Location loc = ent.getEntityLocation(clipboard);
                    if (brokenEntities) {
                        clipboard.createEntity(loc, state);
                        continue;
                    }
                    if (!isWorldEdit && faweWritten == -1) {
                        int locX = loc.getBlockX();
                        int locY = loc.getBlockY();
                        int locZ = loc.getBlockZ();
                        BlockVector3 max = min.add(dimensions).subtract(BlockVector3.ONE);
                        if (locX < min.x() || locY < min.y() || locZ < min.z()
                                || locX > max.x() || locY > max.y() || locZ > max.z()) {
                            for (Entity e : clipboard.getEntities()) {
                                clipboard.removeEntity(e);
                            }
                            LOGGER.error("Detected schematic entity outside clipboard region. FAWE will not load entities. "
                                    + "Please try loading the schematic with the format \"legacyentity\"");
                            break;
                        }
                    }
                    clipboard.createEntity(loc.setPosition(loc.subtract(min.toVector3())), state);
                } else {
                    LOGGER.error("Invalid entity: {}", id);
                }
            }
        }
        clipboard.setOrigin(origin);

        if (clipboard instanceof SimpleClipboard && !min.equals(BlockVector3.ZERO)) {
            clipboard = new BlockArrayClipboard((SimpleClipboard) clipboard, min);
        }

        return clipboard;
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }

}
