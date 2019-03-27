package com.boydti.fawe.object.schematic;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.DoubleTag;
import com.sk89q.jnbt.FloatTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.jnbt.NamedTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.registry.state.AbstractProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.entity.EntityTypes;
import com.sk89q.worldedit.world.registry.BundledBlockData;
import com.sk89q.worldedit.world.storage.NBTConversions;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StructureFormat implements ClipboardReader, ClipboardWriter {
    private static final int WARN_SIZE = 32;

    private NBTInputStream in;
    private NBTOutputStream out;

    public StructureFormat(NBTInputStream in) {
        this.in = in;
    }

    public StructureFormat(NBTOutputStream out) {
        this.out = out;
    }

    @Override
    public Clipboard read() throws IOException {
        return read(UUID.randomUUID());
    }

    public Clipboard read(UUID clipboardId) throws IOException {
        NamedTag rootTag = in.readNamedTag();
        if (!rootTag.getName().equals("")) {
            throw new IOException("Root tag does not exist or is not first");
        }
        Map<String, Tag> tags = ((CompoundTag) rootTag.getTag()).getValue();

        ListTag size = (ListTag) tags.get("size");
        int width = size.getInt(0);
        int height = size.getInt(1);
        int length = size.getInt(2);

        // Init clipboard
        BlockVector3 origin = BlockVector3.at(0, 0, 0);
        CuboidRegion region = new CuboidRegion(origin, origin.add(width, height, length).subtract(BlockVector3.ONE));
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region, clipboardId);
        // Blocks
        ListTag blocks = (ListTag) tags.get("blocks");
        if (blocks != null) {
            // Palette
            List<CompoundTag> palette = (List<CompoundTag>) tags.get("palette").getValue();
            BlockState[] combinedArray = new BlockState[palette.size()];
            for (int i = 0; i < palette.size(); i++) {
                CompoundTag compound = palette.get(i);
                Map<String, Tag> map = compound.getValue();
                String name = ((StringTag) map.get("Name")).getValue();
                BlockType type = BlockTypes.get(name);
                BlockState state = type.getDefaultState();
                if (type == null) {
                    Fawe.debug("Unknown block: " + name);
                    continue;
                }
                CompoundTag properties = (CompoundTag) map.get("Properties");
                if (properties != null) {
                    for (Map.Entry<String, Tag> entry : properties.getValue().entrySet()) {
                        String key = entry.getKey();
                        String value = ((StringTag) entry.getValue()).getValue();
                        Property property = type.getProperty(key);
                        state = state.with(property, property.getValueFor(value));
                    }
                }
                combinedArray[i] = state;
            }
            // Populate blocks
            List<CompoundTag> blocksList = (List<CompoundTag>) tags.get("blocks").getValue();
            try {
                for (CompoundTag compound : blocksList) {
                    Map<String, Tag> blockMap = compound.getValue();
                    IntTag stateTag = (IntTag) blockMap.get("state");
                    ListTag posTag = (ListTag) blockMap.get("pos");
                    BlockState state = combinedArray[stateTag.getValue()];
                    int x = posTag.getInt(0);
                    int y = posTag.getInt(1);
                    int z = posTag.getInt(2);

                    if (state.getBlockType().getMaterial().hasContainer()) {
                        CompoundTag nbt = (CompoundTag) blockMap.get("nbt");
                        if (nbt != null) {
                            BaseBlock block = new BaseBlock(state, nbt);
                            clipboard.setBlock(x, y, z, block);
                            continue;
                        }
                    }
                    clipboard.setBlock(x, y, z, state);

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // Entities
        ListTag entities = (ListTag) tags.get("entities");
        if (entities != null) {
            List<CompoundTag> entityList = (List<CompoundTag>) (List<?>) entities.getValue();
            for (CompoundTag entityEntry : entityList) {
                Map<String, Tag> entityEntryMap = entityEntry.getValue();
                ListTag posTag = (ListTag) entityEntryMap.get("pos");
                CompoundTag nbtTag = (CompoundTag) entityEntryMap.get("nbt");
                String id = nbtTag.getString("Id");
                Location location = NBTConversions.toLocation(clipboard, posTag, nbtTag.getListTag("Rotation"));
                if (!id.isEmpty()) {
                    BaseEntity state = new BaseEntity(EntityTypes.get(id), nbtTag);
                    clipboard.createEntity(location, state);
                }
            }
        }
        return clipboard;
    }

    @Override
    public void write(Clipboard clipboard) throws IOException {
        write(clipboard, "FAWE");
    }

    public void write(Clipboard clipboard, String owner) throws IOException {
        Region region = clipboard.getRegion();
        int width = region.getWidth();
        int height = region.getHeight();
        int length = region.getLength();
        if (width > WARN_SIZE || height > WARN_SIZE || length > WARN_SIZE) {
            Fawe.debug("A structure longer than 32 is unsupported by minecraft (but probably still works)");
        }
        Map<String, Object> structure = FaweCache.asMap("version", 1, "author", owner);
        // ignored: version / owner
        MutableBlockVector mutable = new MutableBlockVector(0, 0, 0);
        Int2ObjectArrayMap<Integer> indexes = new Int2ObjectArrayMap<>();
        // Size
        structure.put("size", Arrays.asList(width, height, length));
        // Palette
        {
            ArrayList<HashMap<String, Object>> palette = new ArrayList<>();
            for (BlockVector3 point : region) {
                BlockStateHolder block = clipboard.getBlock(point);
                int combined = block.getInternalId();
                BlockType type = block.getBlockType();

                if (type == BlockTypes.STRUCTURE_VOID || indexes.containsKey(combined)) {
                    continue;
                }

                indexes.put(combined, (Integer) palette.size());
                HashMap<String, Object> paletteEntry = new HashMap<>();
                paletteEntry.put("Name", type.getId());
                if (block.getInternalId() != type.getInternalId()) {
                    Map<String, Object> properties = null;
                    for (AbstractProperty property : (List<AbstractProperty<?>>) type.getProperties()) {
                        int propIndex = property.getIndex(block.getInternalId());
                        if (propIndex != 0) {
                            if (properties == null) properties = new HashMap<>();
                            Object value = property.getValues().get(propIndex);
                            properties.put(property.getName(), value.toString());
                        }
                    }
                    if (properties != null) {
                        paletteEntry.put("Properties", properties);
                    }
                }
                palette.add(paletteEntry);
            }
            if (!palette.isEmpty()) {
                structure.put("palette", palette);
            }
        }
        // Blocks
        {
            ArrayList<Map<String, Object>> blocks = new ArrayList<>();
            BlockVector3 min = region.getMinimumPoint();
            for (BlockVector3 point : region) {
                BaseBlock block = clipboard.getFullBlock(point);
                if (block.getBlockType() != BlockTypes.STRUCTURE_VOID) {
                    int combined = block.getInternalId();
                    int index = indexes.get(combined);
                    List<Integer> pos = Arrays.asList(point.getX() - min.getX(),
                        point.getY() - min.getY(), point.getZ() - min.getZ());
                    if (!block.hasNbtData()) {
                        blocks.add(FaweCache.asMap("state", index, "pos", pos));
                    } else {
                        blocks.add(
                            FaweCache.asMap("state", index, "pos", pos, "nbt", block.getNbtData()));
                    }
                }
            }
            if (!blocks.isEmpty()) {
                structure.put("blocks", blocks);
            }
        }
        // Entities
        {
            ArrayList<Map<String, Object>> entities = new ArrayList<>();
            for (Entity entity : clipboard.getEntities()) {
                Location loc = entity.getLocation();
                List<Double> pos = Arrays.asList(loc.getX(), loc.getY(), loc.getZ());
                List<Integer> blockPos = Arrays.asList(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                BaseEntity state = entity.getState();
                if (state != null) {
                    CompoundTag nbt = state.getNbtData();
                    Map<String, Tag> nbtMap = ReflectionUtils.getMap(nbt.getValue());
                    // Replace rotation data
                    nbtMap.put("Rotation", writeRotation(entity.getLocation(), "Rotation"));
                    nbtMap.put("id", new StringTag(state.getType().getId()));
                    Map<String, Object> entityMap = FaweCache.asMap("pos", pos, "blockPos", blockPos, "nbt", nbt);
                    entities.add(entityMap);
                }
            }
            if (!entities.isEmpty()) {
                structure.put("entities", entities);
            }
        }
        out.writeNamedTag("", FaweCache.asTag(structure));
        close();
    }

    @Override
    public void close() throws IOException {
        if (in != null) {
            in.close();
        }
        if (out != null) {
            out.close();
        }
    }

    private Tag writeVector(BlockVector3 vector, String name) {
        List<DoubleTag> list = new ArrayList<>();
        list.add(new DoubleTag(vector.getX()));
        list.add(new DoubleTag(vector.getY()));
        list.add(new DoubleTag(vector.getZ()));
        return new ListTag(DoubleTag.class, list);
    }

    private Tag writeRotation(Location location, String name) {
        List<FloatTag> list = new ArrayList<>();
        list.add(new FloatTag(location.getYaw()));
        list.add(new FloatTag(location.getPitch()));
        return new ListTag(FloatTag.class, list);
    }
}
