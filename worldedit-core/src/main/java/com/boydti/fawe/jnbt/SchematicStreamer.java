package com.boydti.fawe.jnbt;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweInputStream;
import com.boydti.fawe.object.FaweOutputStream;
import com.boydti.fawe.object.clipboard.CPUOptimizedClipboard;
import com.boydti.fawe.object.clipboard.DiskOptimizedClipboard;
import com.boydti.fawe.object.clipboard.FaweClipboard;
import com.boydti.fawe.object.clipboard.MemoryOptimizedClipboard;
import com.boydti.fawe.object.io.FastByteArrayOutputStream;
import com.boydti.fawe.object.io.FastByteArraysInputStream;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.StringTag;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.registry.state.PropertyKey;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.block.*;
import com.sk89q.worldedit.world.entity.EntityType;
import com.sk89q.worldedit.world.entity.EntityTypes;
import com.sk89q.worldedit.world.item.ItemTypes;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import com.sk89q.worldedit.world.registry.LegacyMapper;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;

import java.io.IOException;
import java.util.UUID;
import java.util.function.BiConsumer;

// TODO FIXME
public class SchematicStreamer extends NBTStreamer {
    private final UUID uuid;
    private FastByteArrayOutputStream idOut = new FastByteArrayOutputStream();
    private FastByteArrayOutputStream dataOut = new FastByteArrayOutputStream();
    private FastByteArrayOutputStream addOut;

    private FaweOutputStream ids;
    private FaweOutputStream datas;
    private FaweOutputStream adds;

    public SchematicStreamer(NBTInputStream stream, UUID uuid) {
        super(stream);
        this.uuid = uuid;
        clipboard = new BlockArrayClipboard(new CuboidRegion(BlockVector3.at(0, 0, 0), BlockVector3.at(0, 0, 0)), fc);
    }

    public void addBlockReaders() throws IOException {
        NBTStreamReader idInit = new NBTStreamReader<Integer, Integer>() {
            @Override
            public void accept(Integer length, Integer type) {
                setupClipboard(length);
                ids = new FaweOutputStream(new LZ4BlockOutputStream(idOut));
            }
        };
        NBTStreamReader dataInit = new NBTStreamReader<Integer, Integer>() {
            @Override
            public void accept(Integer length, Integer type) {
                setupClipboard(length);
                datas = new FaweOutputStream(new LZ4BlockOutputStream(dataOut));
            }
        };
        NBTStreamReader addInit = new NBTStreamReader<Integer, Integer>() {
            @Override
            public void accept(Integer length, Integer type) {
                setupClipboard(length*2);
                addOut = new FastByteArrayOutputStream();
                adds = new FaweOutputStream(new LZ4BlockOutputStream(addOut));
            }
        };

        addReader("Schematic.Blocks.?", idInit);
        addReader("Schematic.Data.?", dataInit);
        addReader("Schematic.AddBlocks.?", addInit);
        addReader("Schematic.Blocks.#", new ByteReader() {
            @Override
            public void run(int index, int value) {
                try {
                    ids.write(value);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        addReader("Schematic.Data.#", new ByteReader() {
            @Override
            public void run(int index, int value) {
                try {
                    datas.write(value);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        addReader("Schematic.AddBlocks.#", new ByteReader() {
            @Override
            public void run(int index, int value) {
                if (value != 0) {
                    int first = value & 0x0F;
                    int second = (value & 0xF0) >> 4;
                    int gIndex = index << 1;
                    try {
                        if (first != 0) adds.write(first);
                        if (second != 0) adds.write(second);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        ByteReader biomeReader = new ByteReader() {
            @Override
            public void run(int index, int value) {
                fc.setBiome(index, value);
            }
        };
        NBTStreamReader<Integer, Integer> initializer23 = new NBTStreamReader<Integer, Integer>() {
            @Override
            public void accept(Integer value1, Integer value2) {
                if (fc == null) setupClipboard(length * width * height);
            }
        };
        addReader("Schematic.AWEBiomes.?", initializer23);
        addReader("Schematic.Biomes.?", initializer23);
        addReader("Schematic.AWEBiomes.#", biomeReader); // AWE stores as an int[]
        addReader("Schematic.Biomes.#", biomeReader); // FAWE stores as a byte[] (4x smaller)

        // Tiles
        addReader("Schematic.TileEntities.#", (BiConsumer<Integer, CompoundTag>) (index, value) -> {
            if (fc == null) {
                setupClipboard(0);
            }
            int x = value.getInt("x");
            int y = value.getInt("y");
            int z = value.getInt("z");
            fc.setTile(x, y, z, value);
        });
        // Entities
        addReader("Schematic.Entities.#", (BiConsumer<Integer, CompoundTag>) (index, compound) -> {
            if (fc == null) {
                setupClipboard(0);
            }
            String id = compound.getString("id");
            if (id.isEmpty()) {
                return;
            }
            ListTag positionTag = compound.getListTag("Pos");
            ListTag directionTag = compound.getListTag("Rotation");
            EntityType type = EntityTypes.parse(id);
            if (type != null) {
                compound.getValue().put("Id", new StringTag(type.getId()));
                BaseEntity state = new BaseEntity(type, compound);
                fc.createEntity(clipboard, positionTag.asDouble(0), positionTag.asDouble(1), positionTag.asDouble(2), (float) directionTag.asDouble(0), (float) directionTag.asDouble(1), state);
            } else {
                Fawe.debug("Invalid entity: " + id);
            }
        });
    }

    @Override
    public void readFully() throws IOException {
        super.readFully();
        if (ids != null) ids.close();
        if (datas != null) datas.close();
        if (adds != null) adds.close();
        FaweInputStream idIn = new FaweInputStream(new LZ4BlockInputStream(new FastByteArraysInputStream(idOut.toByteArrays())));
        FaweInputStream dataIn = new FaweInputStream(new LZ4BlockInputStream(new FastByteArraysInputStream(dataOut.toByteArrays())));

        LegacyMapper remap = LegacyMapper.getInstance();
        BlockVector3 dimensions = fc.getDimensions();
        int length = dimensions.getBlockX() * dimensions.getBlockY() * dimensions.getBlockZ();
        if (adds == null) {
            for (int i = 0; i < length; i++) {
                fc.setBlock(i, remap.getBlockFromLegacyCombinedId(((idIn.read() & 0xFF) << 4) + (dataIn.read() & 0xF)));
            }
        } else {
            FaweInputStream addIn = new FaweInputStream(new LZ4BlockInputStream(new FastByteArraysInputStream(dataOut.toByteArrays())));
            for (int i = 0; i < length; i++) {
                fc.setBlock(i, remap.getBlockFromLegacyCombinedId(((addIn.read() & 0xFF) << 8) + ((idIn.read() & 0xFF) << 4) + (dataIn.read() & 0xF)));
            }
            addIn.close();
        }
        idIn.close();
        dataIn.close();
    }

    private void fixStates() {
        fc.forEach(new FaweClipboard.BlockReader() {
            @Override
            public <B extends BlockStateHolder<B>> void run(int x, int y, int z, B block) {
                BlockType type = block.getBlockType();
                switch (type.getResource().toUpperCase()) {
                    case "ACACIA_STAIRS":
                    case "BIRCH_STAIRS":
                    case "BRICK_STAIRS":
                    case "COBBLESTONE_STAIRS":
                    case "DARK_OAK_STAIRS":
                    case "DARK_PRISMARINE_STAIRS":
                    case "JUNGLE_STAIRS":
                    case "NETHER_BRICK_STAIRS":
                    case "OAK_STAIRS":
                    case "PRISMARINE_BRICK_STAIRS":
                    case "PRISMARINE_STAIRS":
                    case "PURPUR_STAIRS":
                    case "QUARTZ_STAIRS":
                    case "RED_SANDSTONE_STAIRS":
                    case "SANDSTONE_STAIRS":
                    case "SPRUCE_STAIRS":
                    case "STONE_BRICK_STAIRS":
                        Object half = block.getState(PropertyKey.HALF);
                        Direction facing = block.getState(PropertyKey.FACING);

                        BlockVector3 forward = facing.toBlockVector();
                        Direction left = facing.getLeft();
                        Direction right = facing.getRight();

                        BlockStateHolder forwardBlock = fc.getBlock(x + forward.getBlockX(), y + forward.getBlockY(), z + forward.getBlockZ());
                        BlockType forwardType = forwardBlock.getBlockType();
                        if (forwardType.hasProperty(PropertyKey.SHAPE) && forwardType.hasProperty(PropertyKey.FACING)) {
                            Direction forwardFacing = (Direction) forwardBlock.getState(PropertyKey.FACING);
                            if (forwardFacing == left) {
                                BlockStateHolder rightBlock = fc.getBlock(x + right.getBlockX(), y + right.getBlockY(), z + right.getBlockZ());
                                BlockType rightType = rightBlock.getBlockType();
                                if (!rightType.hasProperty(PropertyKey.SHAPE) || rightBlock.getState(PropertyKey.FACING) != facing) {
                                    fc.setBlock(x, y, z, block.with(PropertyKey.SHAPE, "inner_left"));
                                }
                                return;
                            } else if (forwardFacing == right) {
                                BlockStateHolder leftBlock = fc.getBlock(x + left.getBlockX(), y + left.getBlockY(), z + left.getBlockZ());
                                BlockType leftType = leftBlock.getBlockType();
                                if (!leftType.hasProperty(PropertyKey.SHAPE) || leftBlock.getState(PropertyKey.FACING) != facing) {
                                    fc.setBlock(x, y, z, block.with(PropertyKey.SHAPE, "inner_right"));
                                }
                                return;
                            }
                        }

                        BlockStateHolder backwardsBlock = fc.getBlock(x - forward.getBlockX(), y - forward.getBlockY(), z - forward.getBlockZ());
                        BlockType backwardsType = backwardsBlock.getBlockType();
                        if (backwardsType.hasProperty(PropertyKey.SHAPE) && backwardsType.hasProperty(PropertyKey.FACING)) {
                            Direction backwardsFacing = (Direction) backwardsBlock.getState(PropertyKey.FACING);
                            if (backwardsFacing == left) {
                                BlockStateHolder rightBlock = fc.getBlock(x + right.getBlockX(), y + right.getBlockY(), z + right.getBlockZ());
                                BlockType rightType = rightBlock.getBlockType();
                                if (!rightType.hasProperty(PropertyKey.SHAPE) || rightBlock.getState(PropertyKey.FACING) != facing) {
                                    fc.setBlock(x, y, z, block.with(PropertyKey.SHAPE, "outer_left"));
                                }
                                return;
                            } else if (backwardsFacing == right) {
                                BlockStateHolder leftBlock = fc.getBlock(x + left.getBlockX(), y + left.getBlockY(), z + left.getBlockZ());
                                BlockType leftType = leftBlock.getBlockType();
                                if (!leftType.hasProperty(PropertyKey.SHAPE) || leftBlock.getState(PropertyKey.FACING) != facing) {
                                    fc.setBlock(x, y, z, block.with(PropertyKey.SHAPE, "outer_right"));
                                }
                                return;
                            }
                        }
                        break;
                    default:
                        int group = group(type);
                        if (group == -1) return;
                        BlockStateHolder set = block;

                        if (set.getState(PropertyKey.NORTH) == Boolean.FALSE && merge(group, x, y, z - 1)) set = set.with(PropertyKey.NORTH, true);
                        if (set.getState(PropertyKey.EAST) == Boolean.FALSE && merge(group, x + 1, y, z)) set = set.with(PropertyKey.EAST, true);
                        if (set.getState(PropertyKey.SOUTH) == Boolean.FALSE && merge(group, x, y, z + 1)) set = set.with(PropertyKey.SOUTH, true);
                        if (set.getState(PropertyKey.WEST) == Boolean.FALSE && merge(group, x - 1, y, z)) set = set.with(PropertyKey.WEST, true);

                        if (group == 2) {
                            int ns = ((Boolean) set.getState(PropertyKey.NORTH) ? 1 : 0) + ((Boolean) set.getState(PropertyKey.SOUTH) ? 1 : 0);
                            int ew = ((Boolean) set.getState(PropertyKey.EAST) ? 1 : 0) + ((Boolean) set.getState(PropertyKey.WEST) ? 1 : 0);
                            if (Math.abs(ns - ew) != 2 || fc.getBlock(x, y + 1, z).getBlockType().getMaterial().isSolid()) {
                                set = set.with(PropertyKey.UP, true);
                            }
                        }

                        if (set != block) fc.setBlock(x, y, z, set);
                        break;
                }
            }
        }, false);
    }

    private BlockTypeSwitch<Boolean> fullCube = new BlockTypeSwitchBuilder<>(false).add(type -> {
        BlockMaterial mat = type.getMaterial();
        return (mat.isFullCube() && !mat.isFragileWhenPushed() && mat.getLightValue() == 0 && mat.isOpaque() && mat.isSolid() && !mat.isTranslucent());
    }, true).build();

    private boolean merge(int group, int x, int y, int z) {
        BlockStateHolder block = fc.getBlock(x, y, z);
        BlockType type = block.getBlockType();
        return group(type) == group || fullCube.apply(type);
    }

    private int group(BlockType type) {
        switch (type.getResource().toUpperCase()) {
            case "ACACIA_FENCE":
            case "BIRCH_FENCE":
            case "DARK_OAK_FENCE":
            case "JUNGLE_FENCE":
            case "OAK_FENCE":
            case "SPRUCE_FENCE":
                return 0;
            case "NETHER_BRICK_FENCE":
                return 1;
            case "COBBLESTONE_WALL":
            case "MOSSY_COBBLESTONE_WALL":
                return 2;
            case "IRON_BARS":
            case "BLACK_STAINED_GLASS_PANE":
            case "BLUE_STAINED_GLASS_PANE":
            case "BROWN_MUSHROOM_BLOCK":
            case "BROWN_STAINED_GLASS_PANE":
            case "CYAN_STAINED_GLASS_PANE":
            case "GLASS_PANE":
            case "GRAY_STAINED_GLASS_PANE":
            case "GREEN_STAINED_GLASS_PANE":
            case "LIGHT_BLUE_STAINED_GLASS_PANE":
            case "LIGHT_GRAY_STAINED_GLASS_PANE":
            case "LIME_STAINED_GLASS_PANE":
            case "MAGENTA_STAINED_GLASS_PANE":
            case "ORANGE_STAINED_GLASS_PANE":
            case "PINK_STAINED_GLASS_PANE":
            case "PURPLE_STAINED_GLASS_PANE":
            case "RED_STAINED_GLASS_PANE":
            case "WHITE_STAINED_GLASS_PANE":
            case "YELLOW_STAINED_GLASS_PANE":
                return 3;
            default:
                return -1;
        }
    }

    public void addDimensionReaders() {
        addReader("Schematic.Height",
            (BiConsumer<Integer, Short>) (index, value) -> height = (value));
        addReader("Schematic.Width", (BiConsumer<Integer, Short>) (index, value) -> width = (value));
        addReader("Schematic.Length",
            (BiConsumer<Integer, Short>) (index, value) -> length = (value));
        addReader("Schematic.WEOriginX",
            (BiConsumer<Integer, Integer>) (index, value) -> originX = (value));
        addReader("Schematic.WEOriginY",
            (BiConsumer<Integer, Integer>) (index, value) -> originY = (value));
        addReader("Schematic.WEOriginZ",
            (BiConsumer<Integer, Integer>) (index, value) -> originZ = (value));
        addReader("Schematic.WEOffsetX",
            (BiConsumer<Integer, Integer>) (index, value) -> offsetX = (value));
        addReader("Schematic.WEOffsetY",
            (BiConsumer<Integer, Integer>) (index, value) -> offsetY = (value));
        addReader("Schematic.WEOffsetZ",
            (BiConsumer<Integer, Integer>) (index, value) -> offsetZ = (value));
    }

    private int height;
    private int width;
    private int length;

    private int originX;
    private int originY;
    private int originZ;

    private int offsetX;
    private int offsetY;
    private int offsetZ;

    private BlockArrayClipboard clipboard;
    private FaweClipboard fc;

    private FaweClipboard setupClipboard(int size) {
        if (fc != null) {
            if (fc.getDimensions().getX() == 0) {
                fc.setDimensions(BlockVector3.at(size, 1, 1));
            }
            return fc;
        }
        if (Settings.IMP.CLIPBOARD.USE_DISK) {
            return fc = new DiskOptimizedClipboard(size, 1, 1, uuid);
        } else if (Settings.IMP.CLIPBOARD.COMPRESSION_LEVEL == 0) {
            return fc = new CPUOptimizedClipboard(size, 1, 1);
        } else {
            return fc = new MemoryOptimizedClipboard(size, 1, 1);
        }
    }

    public BlockVector3 getOrigin() {
        return BlockVector3.at(originX, originY, originZ);
    }

    public BlockVector3 getOffset() {
        return BlockVector3.at(offsetX, offsetY, offsetZ);
    }

    public BlockVector3 getDimensions() {
        return BlockVector3.at(width, height, length);
    }

    public void setClipboard(FaweClipboard clipboard) {
        this.fc = clipboard;
    }

    public Clipboard getClipboard() throws IOException {
        try {
        	setupClipboard(0);
            addDimensionReaders();
            addBlockReaders();
            readFully();
            BlockVector3 min = BlockVector3.at(originX, originY, originZ);
            BlockVector3 offset = BlockVector3.at(offsetX, offsetY, offsetZ);
            BlockVector3 origin = min.subtract(offset);
            BlockVector3 dimensions = BlockVector3.at(width, height, length);
            fc.setDimensions(dimensions);
            fixStates();
            CuboidRegion region = new CuboidRegion(min, min.add(width, height, length).subtract(BlockVector3.ONE));
            clipboard.init(region, fc);
            clipboard.setOrigin(origin);
            return clipboard;
        } catch (Throwable e) {
            if (fc != null) {
                fc.close();
            }
            throw e;
        }
    }
}
