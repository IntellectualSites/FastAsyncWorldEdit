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
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.registry.state.PropertyKey;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BlockID;
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
        NBTStreamReader<? extends Integer, ? extends Integer> idInit = new NBTStreamReader<Integer, Integer>() {
            @Override
            public void accept(Integer length, Integer type) {
                setupClipboard(length);
                ids = new FaweOutputStream(new LZ4BlockOutputStream(idOut));
            }
        };
        NBTStreamReader<? extends Integer, ? extends Integer> dataInit = new NBTStreamReader<Integer, Integer>() {
            @Override
            public void accept(Integer length, Integer type) {
                setupClipboard(length);
                datas = new FaweOutputStream(new LZ4BlockOutputStream(dataOut));
            }
        };
        NBTStreamReader<? extends Integer, ? extends Integer> addInit = new NBTStreamReader<Integer, Integer>() {
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
                BiomeType biome = BiomeTypes.getLegacy(value);
                if (biome != null) {
                    fc.setBiome(index, biome);
                }
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
                switch (type.getInternalId()) {
                    case BlockID.ACACIA_STAIRS:
                    case BlockID.BIRCH_STAIRS:
                    case BlockID.BRICK_STAIRS:
                    case BlockID.COBBLESTONE_STAIRS:
                    case BlockID.DARK_OAK_STAIRS:
                    case BlockID.DARK_PRISMARINE_STAIRS:
                    case BlockID.JUNGLE_STAIRS:
                    case BlockID.NETHER_BRICK_STAIRS:
                    case BlockID.OAK_STAIRS:
                    case BlockID.PRISMARINE_BRICK_STAIRS:
                    case BlockID.PRISMARINE_STAIRS:
                    case BlockID.PURPUR_STAIRS:
                    case BlockID.QUARTZ_STAIRS:
                    case BlockID.RED_SANDSTONE_STAIRS:
                    case BlockID.SANDSTONE_STAIRS:
                    case BlockID.SPRUCE_STAIRS:
                    case BlockID.STONE_BRICK_STAIRS:
                        Object half = block.getState(PropertyKey.HALF);
                        Direction facing = block.getState(PropertyKey.FACING);

                        BlockVector3 forward = facing.toBlockVector();
                        Direction left = facing.getLeft();
                        Direction right = facing.getRight();

                        BlockStateHolder<com.sk89q.worldedit.world.block.BaseBlock> forwardBlock = fc.getBlock(x + forward.getBlockX(), y + forward.getBlockY(), z + forward.getBlockZ());
                        BlockType forwardType = forwardBlock.getBlockType();
                        if (forwardType.hasProperty(PropertyKey.SHAPE) && forwardType.hasProperty(PropertyKey.FACING)) {
                            Direction forwardFacing = (Direction) forwardBlock.getState(PropertyKey.FACING);
                            if (forwardFacing == left) {
                                BlockStateHolder<com.sk89q.worldedit.world.block.BaseBlock> rightBlock = fc.getBlock(x + right.toBlockVector().getBlockX(), y + right.toBlockVector().getBlockY(), z + right.toBlockVector().getBlockZ());
                                BlockType rightType = rightBlock.getBlockType();
                                if (!rightType.hasProperty(PropertyKey.SHAPE) || rightBlock.getState(PropertyKey.FACING) != facing) {
                                    fc.setBlock(x, y, z, block.with(PropertyKey.SHAPE, "inner_left"));
                                }
                                return;
                            } else if (forwardFacing == right) {
                                BlockStateHolder<com.sk89q.worldedit.world.block.BaseBlock> leftBlock = fc.getBlock(x + left.toBlockVector().getBlockX(), y + left.toBlockVector().getBlockY(), z + left.toBlockVector().getBlockZ());
                                BlockType leftType = leftBlock.getBlockType();
                                if (!leftType.hasProperty(PropertyKey.SHAPE) || leftBlock.getState(PropertyKey.FACING) != facing) {
                                    fc.setBlock(x, y, z, block.with(PropertyKey.SHAPE, "inner_right"));
                                }
                                return;
                            }
                        }

                        BlockStateHolder<com.sk89q.worldedit.world.block.BaseBlock> backwardsBlock = fc.getBlock(x - forward.getBlockX(), y - forward.getBlockY(), z - forward.getBlockZ());
                        BlockType backwardsType = backwardsBlock.getBlockType();
                        if (backwardsType.hasProperty(PropertyKey.SHAPE) && backwardsType.hasProperty(PropertyKey.FACING)) {
                            Direction backwardsFacing = (Direction) backwardsBlock.getState(PropertyKey.FACING);
                            if (backwardsFacing == left) {
                                BlockStateHolder<com.sk89q.worldedit.world.block.BaseBlock> rightBlock = fc.getBlock(x + right.toBlockVector().getBlockX(), y + right.toBlockVector().getBlockY(), z + right.toBlockVector().getBlockZ());
                                BlockType rightType = rightBlock.getBlockType();
                                if (!rightType.hasProperty(PropertyKey.SHAPE) || rightBlock.getState(PropertyKey.FACING) != facing) {
                                    fc.setBlock(x, y, z, block.with(PropertyKey.SHAPE, "outer_left"));
                                }
                                return;
                            } else if (backwardsFacing == right) {
                                BlockStateHolder<com.sk89q.worldedit.world.block.BaseBlock> leftBlock = fc.getBlock(x + left.toBlockVector().getBlockX(), y + left.toBlockVector().getBlockY(), z + left.toBlockVector().getBlockZ());
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
        BlockStateHolder<com.sk89q.worldedit.world.block.BaseBlock> block = fc.getBlock(x, y, z);
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
