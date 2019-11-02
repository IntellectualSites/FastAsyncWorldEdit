/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.extent.clipboard.io;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.jnbt.streamer.InfoReader;
import com.boydti.fawe.jnbt.streamer.IntValueReader;
import com.boydti.fawe.jnbt.streamer.StreamDelegate;
import com.boydti.fawe.jnbt.streamer.ValueReader;
import com.boydti.fawe.object.FaweInputStream;
import com.boydti.fawe.object.FaweOutputStream;
import com.boydti.fawe.object.clipboard.LinearClipboard;
import com.boydti.fawe.object.io.FastByteArrayOutputStream;
import com.boydti.fawe.object.io.FastByteArraysInputStream;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.legacycompat.EntityNBTCompatibilityHandler;
import com.sk89q.worldedit.extent.clipboard.io.legacycompat.FlowerPotCompatibilityHandler;
import com.sk89q.worldedit.extent.clipboard.io.legacycompat.NBTCompatibilityHandler;
import com.sk89q.worldedit.extent.clipboard.io.legacycompat.NoteBlockCompatibilityHandler;
import com.sk89q.worldedit.extent.clipboard.io.legacycompat.Pre13HangingCompatibilityHandler;
import com.sk89q.worldedit.extent.clipboard.io.legacycompat.SignCompatibilityHandler;
import com.sk89q.worldedit.extent.clipboard.io.legacycompat.SkullBlockCompatibilityHandler;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.PropertyKey;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockCategories;
import com.sk89q.worldedit.world.block.BlockID;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypeSwitch;
import com.sk89q.worldedit.world.block.BlockTypeSwitchBuilder;
import com.sk89q.worldedit.world.entity.EntityType;
import com.sk89q.worldedit.world.entity.EntityTypes;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import com.sk89q.worldedit.world.registry.LegacyMapper;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Reads schematic files based that are compatible with MCEdit and other editors.
 */
public class SchematicReader implements ClipboardReader {

    private static final NBTCompatibilityHandler[] COMPATIBILITY_HANDLERS = {
        new SignCompatibilityHandler(),
        new FlowerPotCompatibilityHandler(),
        new NoteBlockCompatibilityHandler(),
        new SkullBlockCompatibilityHandler()
    };
    private static final EntityNBTCompatibilityHandler[] ENTITY_COMPATIBILITY_HANDLERS = {
        new Pre13HangingCompatibilityHandler()
    };

    private NBTInputStream inputStream;
    private InputStream rootStream;

//    private final DataFixer fixer; TODO

    private FastByteArrayOutputStream idOut = new FastByteArrayOutputStream();
    private FastByteArrayOutputStream dataOut = new FastByteArrayOutputStream();
    private FastByteArrayOutputStream addOut;
    private FastByteArrayOutputStream biomesOut;

    private FaweOutputStream ids;
    private FaweOutputStream datas;
    private FaweOutputStream adds;
    private FaweOutputStream biomes;

    private List<Map<String, Object>> tiles;
    private List<Map<String, Object>> entities;

    private int width, height, length;
    private int offsetX, offsetY, offsetZ;
    private int originX, originY, originZ;

    /**
     * Create a new instance.
     *
     * @param inputStream the input stream to read from
     */
    public SchematicReader(NBTInputStream inputStream) {
        checkNotNull(inputStream);
        this.inputStream = inputStream;
    }

    public void setUnderlyingStream(InputStream in) {
        this.rootStream = in;
    }

    public StreamDelegate createDelegate() {
        StreamDelegate root = new StreamDelegate();
        StreamDelegate schematic = root.add("Schematic");
        schematic.add("Width").withInt((i, v) -> width = v);
        schematic.add("Height").withInt((i, v) -> height = v);
        schematic.add("Length").withInt((i, v) -> length = v);

        schematic.add("WEOriginX").withInt((i, v) -> originX = v);
        schematic.add("WEOriginY").withInt((i, v) -> originY = v);
        schematic.add("WEOriginZ").withInt((i, v) -> originZ = v);

        StreamDelegate metadata = schematic.add("Metadata");
        metadata.add("WEOffsetX").withInt((i, v) -> offsetX = v);
        metadata.add("WEOffsetY").withInt((i, v) -> offsetY = v);
        metadata.add("WEOffsetZ").withInt((i, v) -> offsetZ = v);

        StreamDelegate blocksDelegate = schematic.add("Blocks");
        blocksDelegate.withInfo((length, type) -> ids = new FaweOutputStream(new LZ4BlockOutputStream(idOut)));
        blocksDelegate.withInt((index, value) -> ids.write(value));

        StreamDelegate dataDelegate = schematic.add("Data");
        dataDelegate.withInfo((length, type) -> datas = new FaweOutputStream(new LZ4BlockOutputStream(dataOut)));
        dataDelegate.withInt((index, value) -> datas.write(value));

        StreamDelegate addDelegate = schematic.add("AddBlocks");
        addDelegate.withInfo((length, type) -> {
            addOut = new FastByteArrayOutputStream();
            adds = new FaweOutputStream(new LZ4BlockOutputStream(addOut));
        });
        addDelegate.withInt((index, value) -> {
            if (value != 0) {
                int first = value & 0x0F;
                int second = (value & 0xF0) >> 4;
                adds.write(first);
                adds.write(second);
            } else {
                adds.write(0);
                adds.write(0);
            }
        });

        StreamDelegate biomesDelegate = schematic.add("Biomes");
        StreamDelegate aweBiomesDelegate = schematic.add("AWEBiomes");

        InfoReader biomesInfo = (l, t) -> {
            biomesOut = new FastByteArrayOutputStream();
            biomes = new FaweOutputStream(new LZ4BlockOutputStream(biomesOut));
        };
        biomesDelegate.withInfo(biomesInfo);
        aweBiomesDelegate.withInfo(biomesInfo);

        IntValueReader biomeReader = (index, value) -> biomes.write(value);
        biomesDelegate.withInt(biomeReader);


        StreamDelegate tilesDelegate = schematic.add("TileEntities");
        tilesDelegate.withInfo((length, type) -> tiles = new ArrayList<>(length));
        tilesDelegate.withElem(new ValueReader<Map<String, Object>>() {
            @Override
            public void apply(int index, Map<String, Object> tile) {
                tiles.add(tile);
            }
        });

        StreamDelegate entitiesDelegate = schematic.add("Entities");
        entitiesDelegate.withInfo((length, type) -> entities = new ArrayList<>(length));
        entitiesDelegate.withElem(new ValueReader<Map<String, Object>>() {
            @Override
            public void apply(int index, Map<String, Object> entity) {
                entities.add(entity);
            }
        });
        return root;
    }

    private int readCombined(InputStream idIn, InputStream dataIn) throws IOException {
        return ((idIn.read() & 0xFF) << 4) + (dataIn.read() & 0xF);
    }

    private int readCombined(InputStream idIn, InputStream dataIn, InputStream addIn) throws IOException {
        return ((addIn.read() & 0xFF) << 8) + readCombined(idIn, dataIn);
    }

    private BlockState getBlock(int combined) {
        BlockState state = LegacyMapper.getInstance().getBlockFromLegacyCombinedId(combined);
        return state;
    }

    private void write(int index, BlockState block, LinearClipboard clipboard) {
        clipboard.setBlock(index, block);
    }

    private void write(int x, int y, int z, BlockState block, Clipboard clipboard) {
        clipboard.setBlock(x, y, z, block);
    }

    private void readwrite(int index, InputStream idIn, InputStream dataIn, LinearClipboard out) throws IOException {
        readwrite(index, readCombined(idIn, dataIn), out);
    }

    private void readwrite(int x, int y, int z, InputStream idIn, InputStream dataIn, Clipboard out) throws IOException {
        readwrite(x, y, z, readCombined(idIn, dataIn), out);
    }

    private void readwrite(int index, InputStream idIn, InputStream dataIn, InputStream addIn, LinearClipboard out) throws IOException {
        readwrite(index, readCombined(idIn, dataIn, addIn), out);
    }

    private void readwrite(int x, int y, int z, InputStream idIn, InputStream dataIn, InputStream addIn, Clipboard out) throws IOException {
        readwrite(x, y, z, readCombined(idIn, dataIn, addIn), out);
    }

    private void readwrite(int index, int combined, LinearClipboard out) throws IOException {
        write(index, getBlock(combined), out);
    }

    private void readwrite(int x, int y, int z, int combined, Clipboard out) throws IOException {
        write(x, y, z, getBlock(combined), out);
    }

    @Override
    public Clipboard read(UUID uuid, Function<BlockVector3, Clipboard> createOutput) throws IOException {
        StreamDelegate root = createDelegate();
        inputStream.readNamedTagLazy(root);

        if (ids != null) ids.close();
        if (datas != null) datas.close();
        if (adds != null) adds.close();
        if (biomes != null) biomes.close();
        ids = null;
        datas = null;
        adds = null;
        biomes = null;

        BlockVector3 dimensions = BlockVector3.at(width, height, length);
        BlockVector3 origin = BlockVector3.at(originX, originY, originZ);
        if (offsetX != Integer.MIN_VALUE && offsetY != Integer.MIN_VALUE  && offsetZ != Integer.MIN_VALUE) {
            origin = origin.subtract(BlockVector3.at(offsetX, offsetY, offsetZ));
        }

        Clipboard clipboard = createOutput.apply(dimensions);
        try (InputStream dataIn = new LZ4BlockInputStream(new FastByteArraysInputStream(dataOut.toByteArrays()));InputStream idIn = new LZ4BlockInputStream(new FastByteArraysInputStream(idOut.toByteArrays()))) {
            if (addOut != null) {
                try (FaweInputStream addIn = new FaweInputStream(new LZ4BlockInputStream(new FastByteArraysInputStream(addOut.toByteArrays())))) {
                    if (clipboard instanceof LinearClipboard) {
                        LinearClipboard linear = (LinearClipboard) clipboard;
                        for (int y = 0, index = 0; y < height; y++) {
                            for (int z = 0; z < length; z++) {
                                for (int x = 0; x < width; x++, index++) {
                                    readwrite(index, idIn, dataIn, addIn, linear);
                                }
                            }
                        }
                    } else {
                        for (int y = 0; y < height; y++) {
                            for (int z = 0; z < length; z++) {
                                for (int x = 0; x < width; x++) {
                                    readwrite(x, y, z, idIn, dataIn, addIn, clipboard);
                                }
                            }
                        }
                    }
                }
            } else {
                if (clipboard instanceof LinearClipboard) {
                    LinearClipboard linear = (LinearClipboard) clipboard;
                    for (int y = 0, index = 0; y < height; y++) {
                        for (int z = 0; z < length; z++) {
                            for (int x = 0; x < width; x++, index++) {
                                readwrite(index, idIn, dataIn, linear);
                            }
                        }
                    }
                } else {
                    for (int y = 0; y < height; y++) {
                        for (int z = 0; z < length; z++) {
                            for (int x = 0; x < width; x++) {
                                readwrite(x, y, z, idIn, dataIn, clipboard);
                            }
                        }
                    }
                }
            }
        }

        if (biomes != null) {
            try (InputStream biomesIn = new LZ4BlockInputStream(new FastByteArraysInputStream(biomesOut.toByteArrays()))) {
                if (clipboard instanceof LinearClipboard) {
                    LinearClipboard linear = (LinearClipboard) clipboard;
                    int volume = width * length;
                    for (int index = 0; index < volume; index++) {
                        BiomeType biome = BiomeTypes.getLegacy(biomesIn.read());
                        if (biome != null) linear.setBiome(index, biome);
                    }
                } else {
                    for (int z = 0; z < length; z++) {
                        for (int x = 0; x < width; x++) {
                            BiomeType biome = BiomeTypes.getLegacy(biomesIn.read());
                            if (biome != null) clipboard.setBiome(x, 0, z, biome);
                        }
                    }
                }
            }
        }

        // tiles
        if (tiles != null && !tiles.isEmpty()) {
            outer:
            for (Map<String, Object> tileRaw : tiles) {
                CompoundTag tile = FaweCache.IMP.asTag(tileRaw);
                int x = (int) tileRaw.get("x");
                int y = (int) tileRaw.get("y");
                int z = (int) tileRaw.get("z");

                BlockState block = clipboard.getBlock(x, y, z);
                for (NBTCompatibilityHandler compat : COMPATIBILITY_HANDLERS) {
                    if (compat.isAffectedBlock(block)) {
                        block = compat.updateNBT(block, tile.getValue());
                        BaseBlock baseBlock = block.toBaseBlock(tile);
                        clipboard.setBlock(x, y, z, baseBlock);
                        continue outer;
                    }
                }
                clipboard.setTile(x, y, z, tile);
            }
        }

        // entities
        if (entities != null && !entities.isEmpty()) {
            for (Map<String, Object> entRaw : entities) {
                String id = (String) entRaw.get("id");
                if (id == null) {
                    continue;
                }
                entRaw.put("Id", id);
                EntityType type = EntityTypes.parse(id);
                if (type != null) {
                    CompoundTag ent = FaweCache.IMP.asTag(entRaw);
                    for (EntityNBTCompatibilityHandler compat : ENTITY_COMPATIBILITY_HANDLERS) {
                        if (compat.isAffectedEntity(type, ent)) {
                            ent = compat.updateNBT(type, ent);
                        }
                    }
                    BaseEntity state = new BaseEntity(type, ent);
                    Location loc = ent.getEntityLocation(clipboard);
                    clipboard.createEntity(loc, state);
                } else {
                    Fawe.debug("Invalid entity: " + id);
                }
            }
        }
        fixStates(clipboard);
        clipboard.setOrigin(origin);
        return clipboard;
    }

    private void fixStates(Clipboard fc) {
        for (BlockVector3 pos : fc) {
            BlockState block = pos.getBlock(fc);
            if (block.getMaterial().isAir()) continue;

            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();

            BlockType type = block.getBlockType();
            if (BlockCategories.STAIRS.contains(type)) {
                Direction facing = block.getState(PropertyKey.FACING);

                BlockVector3 forward = facing.toBlockVector();
                Direction left = facing.getLeft();
                Direction right = facing.getRight();

                BlockStateHolder forwardBlock = fc.getBlock(x + forward.getBlockX(), y + forward.getBlockY(), z + forward.getBlockZ());
                BlockType forwardType = forwardBlock.getBlockType();
                if (forwardType.hasProperty(PropertyKey.SHAPE) && forwardType.hasProperty(PropertyKey.FACING)) {
                    Direction forwardFacing = (Direction) forwardBlock.getState(PropertyKey.FACING);
                    if (forwardFacing == left) {
                        BlockState rightBlock = fc.getBlock(x + right.toBlockVector().getBlockX(), y + right.toBlockVector().getBlockY(), z + right.toBlockVector().getBlockZ());
                        BlockType rightType = rightBlock.getBlockType();
                        if (!rightType.hasProperty(PropertyKey.SHAPE) || rightBlock.getState(PropertyKey.FACING) != facing) {
                            pos.setBlock(fc, block.with(PropertyKey.SHAPE, "inner_left"));
                        }
                        return;
                    } else if (forwardFacing == right) {
                        BlockState leftBlock = fc.getBlock(x + left.toBlockVector().getBlockX(), y + left.toBlockVector().getBlockY(), z + left.toBlockVector().getBlockZ());
                        BlockType leftType = leftBlock.getBlockType();
                        if (!leftType.hasProperty(PropertyKey.SHAPE) || leftBlock.getState(PropertyKey.FACING) != facing) {
                            fc.setBlock(x, y, z, block.with(PropertyKey.SHAPE, "inner_right"));
                        }
                        return;
                    }
                }

                BlockState backwardsBlock = fc.getBlock(x - forward.getBlockX(), y - forward.getBlockY(), z - forward.getBlockZ());
                BlockType backwardsType = backwardsBlock.getBlockType();
                if (backwardsType.hasProperty(PropertyKey.SHAPE) && backwardsType.hasProperty(PropertyKey.FACING)) {
                    Direction backwardsFacing = (Direction) backwardsBlock.getState(PropertyKey.FACING);
                    if (backwardsFacing == left) {
                        BlockState rightBlock = fc.getBlock(x + right.toBlockVector().getBlockX(), y + right.toBlockVector().getBlockY(), z + right.toBlockVector().getBlockZ());
                        BlockType rightType = rightBlock.getBlockType();
                        if (!rightType.hasProperty(PropertyKey.SHAPE) || rightBlock.getState(PropertyKey.FACING) != facing) {
                            pos.setBlock(fc, block.with(PropertyKey.SHAPE, "outer_left"));
                        }
                        return;
                    } else if (backwardsFacing == right) {
                        BlockState leftBlock = fc.getBlock(x + left.toBlockVector().getBlockX(), y + left.toBlockVector().getBlockY(), z + left.toBlockVector().getBlockZ());
                        BlockType leftType = leftBlock.getBlockType();
                        if (!leftType.hasProperty(PropertyKey.SHAPE) || leftBlock.getState(PropertyKey.FACING) != facing) {
                            pos.setBlock(fc, block.with(PropertyKey.SHAPE, "outer_right"));
                        }
                        return;
                    }
                }
            } else {
                int group = group(type);
                if (group == -1) return;
                BlockState set = block;

                if (set.getState(PropertyKey.NORTH) == Boolean.FALSE && merge(fc, group, x, y, z - 1)) set = set.with(PropertyKey.NORTH, true);
                if (set.getState(PropertyKey.EAST) == Boolean.FALSE && merge(fc, group, x + 1, y, z)) set = set.with(PropertyKey.EAST, true);
                if (set.getState(PropertyKey.SOUTH) == Boolean.FALSE && merge(fc, group, x, y, z + 1)) set = set.with(PropertyKey.SOUTH, true);
                if (set.getState(PropertyKey.WEST) == Boolean.FALSE && merge(fc, group, x - 1, y, z)) set = set.with(PropertyKey.WEST, true);

                if (group == 2) {
                    int ns = ((Boolean) set.getState(PropertyKey.NORTH) ? 1 : 0) + ((Boolean) set.getState(PropertyKey.SOUTH) ? 1 : 0);
                    int ew = ((Boolean) set.getState(PropertyKey.EAST) ? 1 : 0) + ((Boolean) set.getState(PropertyKey.WEST) ? 1 : 0);
                    if (Math.abs(ns - ew) != 2 || fc.getBlock(x, y + 1, z).getBlockType().getMaterial().isSolid()) {
                        set = set.with(PropertyKey.UP, true);
                    }
                }

                if (set != block) pos.setBlock(fc, set);
            }
        }
    }


    private BlockTypeSwitch<Boolean> fullCube = new BlockTypeSwitchBuilder<>(false).add(type -> {
        BlockMaterial mat = type.getMaterial();
        return (mat.isFullCube() && !mat.isFragileWhenPushed() && mat.getLightValue() == 0 && mat.isOpaque() && mat.isSolid() && !mat.isTranslucent());
    }, true).build();

    private boolean merge(Clipboard fc, int group, int x, int y, int z) {
        BlockState block = fc.getBlock(x, y, z);
        BlockType type = block.getBlockType();
        return group(type) == group || fullCube.apply(type);
    }

    private int group(BlockType type) {
        switch (type.getInternalId()) {
            case BlockID.ACACIA_FENCE:
            case BlockID.BIRCH_FENCE:
            case BlockID.DARK_OAK_FENCE:
            case BlockID.JUNGLE_FENCE:
            case BlockID.OAK_FENCE:
            case BlockID.SPRUCE_FENCE:
                return 0;
            case BlockID.NETHER_BRICK_FENCE:
                return 1;
            case BlockID.COBBLESTONE_WALL:
            case BlockID.MOSSY_COBBLESTONE_WALL:
                return 2;
            case BlockID.IRON_BARS:
            case BlockID.BLACK_STAINED_GLASS_PANE:
            case BlockID.BLUE_STAINED_GLASS_PANE:
            case BlockID.BROWN_MUSHROOM_BLOCK:
            case BlockID.BROWN_STAINED_GLASS_PANE:
            case BlockID.CYAN_STAINED_GLASS_PANE:
            case BlockID.GLASS_PANE:
            case BlockID.GRAY_STAINED_GLASS_PANE:
            case BlockID.GREEN_STAINED_GLASS_PANE:
            case BlockID.LIGHT_BLUE_STAINED_GLASS_PANE:
            case BlockID.LIGHT_GRAY_STAINED_GLASS_PANE:
            case BlockID.LIME_STAINED_GLASS_PANE:
            case BlockID.MAGENTA_STAINED_GLASS_PANE:
            case BlockID.ORANGE_STAINED_GLASS_PANE:
            case BlockID.PINK_STAINED_GLASS_PANE:
            case BlockID.PURPLE_STAINED_GLASS_PANE:
            case BlockID.RED_STAINED_GLASS_PANE:
            case BlockID.WHITE_STAINED_GLASS_PANE:
            case BlockID.YELLOW_STAINED_GLASS_PANE:
                return 3;
            default:
                return -1;
        }
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}
