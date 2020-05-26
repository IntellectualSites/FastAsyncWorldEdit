package com.boydti.fawe.object.schematic;

import static org.slf4j.LoggerFactory.getLogger;

import com.boydti.fawe.FaweCache;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.jnbt.NamedTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.registry.state.AbstractProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.entity.EntityTypes;
import com.sk89q.worldedit.world.storage.NBTConversions;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public class MinecraftStructure implements ClipboardReader, ClipboardWriter {
    private static final int WARN_SIZE = 32;

    private NBTInputStream inputStream;
    private NBTOutputStream out;

    public MinecraftStructure(@NotNull NBTInputStream inputStream) {
        this.inputStream = inputStream;
    }

    public MinecraftStructure(NBTOutputStream out) {
        this.out = out;
    }

    @Override
    public Clipboard read() throws IOException {
        return read(UUID.randomUUID());
    }

    @Override
    public Clipboard read(UUID clipboardId) throws IOException {
        NamedTag rootTag = inputStream.readNamedTag();

        // MC structures are all unnamed, but this doesn't seem to be necessary? might remove this later
        if (!rootTag.getName().isEmpty()) {
            throw new IOException("Root tag has name - are you sure this is a structure?");
        }

        Map<String, Tag> tags = ((CompoundTag) rootTag.getTag()).getValue();

        ListTag size = (ListTag) tags.get("size");
        int width = size.getInt(0);
        int height = size.getInt(1);
        int length = size.getInt(2);

        // Init clipboard
        BlockVector3 origin = BlockVector3.at(0, 0, 0);
        CuboidRegion region = new CuboidRegion(origin, origin.add(width, height, length).subtract(BlockVector3.ONE));
        Clipboard clipboard = new BlockArrayClipboard(region, clipboardId);
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
                            BaseBlock block = state.toBaseBlock(nbt);
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
            getLogger(MinecraftStructure.class).debug("A structure longer than 32 is unsupported by minecraft (but probably still works)");
        }
        Map<String, Object> structure = FaweCache.IMP.asMap("version", 1, "author", owner);
        // ignored: version / owner
        MutableBlockVector3 mutable = new MutableBlockVector3(0, 0, 0);
        Int2ObjectArrayMap<Integer> indexes = new Int2ObjectArrayMap<>();
        // Size
        structure.put("size", Arrays.asList(width, height, length));
        // Palette
        ArrayList<HashMap<String, Object>> palette = new ArrayList<>();
        for (BlockVector3 point : region) {
            BlockState block = clipboard.getBlock(point);
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
        // Blocks
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
                    blocks.add(FaweCache.IMP.asMap("state", index, "pos", pos));
                } else {
                    blocks.add(
                        FaweCache.IMP.asMap("state", index, "pos", pos, "nbt", block.getNbtData()));
                }
            }
        }
        if (!blocks.isEmpty()) {
            structure.put("blocks", blocks);
        }
        // Entities
        ArrayList<Map<String, Object>> entities = new ArrayList<>();
        for (Entity entity : clipboard.getEntities()) {
            Location loc = entity.getLocation();
            List<Double> pos = Arrays.asList(loc.getX(), loc.getY(), loc.getZ());
            List<Integer> blockPos = Arrays.asList(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            BaseEntity state = entity.getState();
            if (state != null) {
                CompoundTag nbt = state.getNbtData();
                Map<String, Tag> nbtMap = nbt.getValue();
                // Replace rotation data
                nbtMap.put("Rotation", writeRotation(entity.getLocation()));
                nbtMap.put("id", new StringTag(state.getType().getId()));
                Map<String, Object> entityMap = FaweCache.IMP.asMap("pos", pos, "blockPos", blockPos, "nbt", nbt);
                entities.add(entityMap);
            }
        }
        if (!entities.isEmpty()) {
            structure.put("entities", entities);
        }
        out.writeNamedTag("", FaweCache.IMP.asTag(structure));
        close();
    }

    @Override
    public void close() throws IOException {
        if (inputStream != null) {
            inputStream.close();
        }
        if (out != null) {
            out.close();
        }
    }

}
