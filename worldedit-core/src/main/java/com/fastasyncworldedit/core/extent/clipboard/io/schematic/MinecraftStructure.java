package com.fastasyncworldedit.core.extent.clipboard.io.schematic;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.extent.clipboard.io.sponge.ReaderUtil;
import com.sk89q.worldedit.extent.clipboard.io.sponge.VersionedDataFixer;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.concurrency.LazyReference;
import com.sk89q.worldedit.world.DataFixer;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.entity.EntityTypes;
import com.sk89q.worldedit.world.storage.NBTConversions;
import it.unimi.dsi.fastutil.chars.Char2IntArrayMap;
import it.unimi.dsi.fastutil.chars.Char2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.apache.logging.log4j.Logger;
import org.enginehub.linbus.stream.LinBinaryIO;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinDoubleTag;
import org.enginehub.linbus.tree.LinFloatTag;
import org.enginehub.linbus.tree.LinIntTag;
import org.enginehub.linbus.tree.LinListTag;
import org.enginehub.linbus.tree.LinRootEntry;
import org.enginehub.linbus.tree.LinTag;
import org.enginehub.linbus.tree.LinTagType;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class MinecraftStructure implements ClipboardReader, ClipboardWriter {

    private static final Logger LOGGER = LogManagerCompat.getLogger();
    private static final Platform PLATFORM = WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.WORLD_EDITING);

    private static final int WARN_SIZE = 32;

    private DataInput in;
    private DataOutput out;

    public MinecraftStructure(@Nonnull DataInput in) {
        this.in = in;
    }

    public MinecraftStructure(@Nonnull DataOutput out) {
        this.out = out;
    }

    @SuppressWarnings("removal")
    @Deprecated(since = "2.12.3")
    public MinecraftStructure(@Nonnull com.sk89q.jnbt.NBTInputStream inputStream) {
        this.in = inputStream.dataInputStream();
    }

    @SuppressWarnings("removal")
    @Deprecated(since = "2.12.3")
    public MinecraftStructure(@Nonnull com.sk89q.jnbt.NBTOutputStream out) {
        this.out = out;
    }

    @Override
    public Clipboard read(UUID clipboardId) throws IOException {
        final LinRootEntry rootEntry = LinRootEntry.readFrom(LinBinaryIO.read(this.in));

        // MC structures are all unnamed, but this doesn't seem to be necessary? might remove this later
        if (!rootEntry.name().isEmpty()) {
            throw new IOException("Root tag has name - are you sure this is a structure?");
        }
        final LinCompoundTag parent = rootEntry.value();
        final VersionedDataFixer dataFixer = ReaderUtil.getVersionedDataFixer(
                parent.getTag("DataVersion", LinTagType.intTag()).valueAsInt(),
                PLATFORM, PLATFORM.getDataVersion()
        );

        final List<LinIntTag> size = parent.getListTag("size", LinTagType.intTag()).value();
        if (size.size() != 3) {
            throw new IOException("Expected 'size' to contain 3 integers, but got " + size.size());
        }
        final CuboidRegion region = new CuboidRegion(BlockVector3.ZERO, BlockVector3.at(
                size.get(0).valueAsInt(), size.get(1).valueAsInt(), size.get(2).value()
        ).subtract(BlockVector3.ONE));
        final Clipboard clipboard = new BlockArrayClipboard(region, clipboardId);

        // Palette
        final List<LinCompoundTag> paletteEntry = parent.getListTag("palette", LinTagType.compoundTag()).value();
        final BlockState[] palette = new BlockState[paletteEntry.size()];
        for (int i = 0; i < palette.length; i++) {
            final LinCompoundTag entry = paletteEntry.get(i);
            final BlockType blockType = BlockTypes.get(dataFixer.fixUp(DataFixer.FixTypes.BLOCK_STATE, entry.getTag("Name", LinTagType.stringTag()).value()));
            if (blockType == null) {
                throw new IOException("Unknown block type: " + entry.getTag("Name", LinTagType.stringTag()).value());
            }
            BlockState block = blockType.getDefaultState();
            LinCompoundTag properties = entry.findTag("Properties", LinTagType.compoundTag());
            if (properties != null) {
                for (final Map.Entry<String, LinTag<?>> propertyPair : properties.value().entrySet()) {
                    final Property<Object> property = blockType.getProperty(propertyPair.getKey());
                    if (property == null) {
                        continue;
                    }
                    final String value = LinTagType.stringTag().cast(propertyPair.getValue()).value();
                    block = block.with(property, property.getValueFor(value));

                }
            }
            palette[i] = block;
        }

        // Blocks
        final List<LinCompoundTag> blocks = parent.getListTag("blocks", LinTagType.compoundTag()).value();
        for (final LinCompoundTag block : blocks) {
            int state = block.getTag("state", LinTagType.intTag()).valueAsInt();
            if (state >= palette.length) {
                throw new IOException("state index exceeds palette length");
            }
            List<LinIntTag> pos = block.getTag("pos", LinTagType.listTag()).asTypeChecked(LinTagType.intTag()).value();
            if (size.size() != 3) {
                throw new IOException("Expected 'pos' to contain 3 integers, but got " + size.size());
            }
            LinCompoundTag nbt = block.findTag("nbt", LinTagType.compoundTag());
            if (nbt == null) {
                clipboard.setBlock(pos.get(0).valueAsInt(), pos.get(1).valueAsInt(), pos.get(2).valueAsInt(), palette[state]);
                continue;
            }
            clipboard.setBlock(
                    pos.get(0).valueAsInt(), pos.get(1).valueAsInt(), pos.get(2).valueAsInt(),
                    palette[state].toBaseBlock(nbt)
            );
        }

        // Entities
        LinListTag<@org.jetbrains.annotations.NotNull LinCompoundTag> entities = parent.findListTag("entities", LinTagType.compoundTag());
        if (entities == null) {
            return clipboard;
        }
        for (final LinCompoundTag entity : entities.value()) {
            final LinCompoundTag nbt = entity.getTag("nbt", LinTagType.compoundTag());
            clipboard.createEntity(
                    NBTConversions.toLocation(clipboard,
                            entity.getListTag("pos", LinTagType.doubleTag()),
                            nbt.getListTag("Rotation", LinTagType.floatTag())
                    ),
                    new BaseEntity(
                            EntityTypes.get(nbt.getTag("id", LinTagType.stringTag()).value()),
                            LazyReference.computed(dataFixer.fixUp(DataFixer.FixTypes.ENTITY, nbt))
                    )
            );
        }
        return clipboard;
    }


    /**
     * @deprecated owner is not used anymore, use {@link #write(Clipboard)}
     */
    @Deprecated(since = "2.12.3")
    public void write(Clipboard clipboard, @SuppressWarnings("unused") String owner) throws IOException {
        this.write(clipboard);
    }

    @Override
    public void write(Clipboard clipboard) throws IOException {
        clipboard.flush();
        Region region = clipboard.getRegion();
        int width = region.getWidth();
        int height = region.getHeight();
        int length = region.getLength();
        BlockVector3 min = region.getMinimumPoint();
        if (width > WARN_SIZE || height > WARN_SIZE || length > WARN_SIZE) {
            LOGGER.info("A structure longer than 32 is unsupported by minecraft (but probably still works)");
        }
        LinCompoundTag.Builder root = LinCompoundTag.builder();
        root.putInt("DataVersion", PLATFORM.getDataVersion());
        root.put("size", LinListTag.of(LinTagType.intTag(), List.of(
            LinIntTag.of(width), LinIntTag.of(height), LinIntTag.of(length)
        )));

        // Block Palette
        Char2IntMap ordinals = new Char2IntArrayMap();
        LinListTag.Builder<@org.jetbrains.annotations.NotNull LinCompoundTag> palette = LinListTag.builder(LinTagType.compoundTag());
        Int2ObjectMap<BlockState> paletteIndexes = new Int2ObjectArrayMap<>();
        for (final BlockVector3 pos : clipboard) {
            final BlockState block = clipboard.getBlock(pos);
            if (block.getBlockType() == BlockTypes.STRUCTURE_VOID || ordinals.containsKey(block.getOrdinalChar())) {
                continue;
            }
            ordinals.put(block.getOrdinalChar(), paletteIndexes.size());
            paletteIndexes.put(block.getOrdinalChar(), block);
            final LinCompoundTag.Builder entry = LinCompoundTag.builder()
                    .putString("Name", block.getBlockType().id());
            if (block.getInternalId() != block.getBlockType().getInternalId()) {
                final LinCompoundTag.Builder properties = LinCompoundTag.builder();
                block.getStates().forEach((property, value) -> properties.putString(
                        property.getName(),
                        value.toString().toLowerCase(Locale.ROOT)
                ));
                entry.put("Properties", properties.build());
            }
            palette.add(entry.build());
        }

        // Blocks
        LinListTag.Builder<@org.jetbrains.annotations.NotNull LinCompoundTag> blocks = LinListTag.builder(LinTagType.compoundTag());
        for (final BlockVector3 pos : clipboard) {
            final BlockState block = clipboard.getBlock(pos);
            LinCompoundTag.Builder entry = LinCompoundTag.builder()
                    .putInt("state", ordinals.get(block.getOrdinalChar()))
                    .put("pos", LinListTag.of(LinTagType.intTag(), List.of(
                            LinIntTag.of(pos.x() - min.x()),
                            LinIntTag.of(pos.y() - min.y()),
                            LinIntTag.of(pos.z() - min.z())
                    )));
            final BaseBlock baseBlock = clipboard.getFullBlock(pos);
            if (baseBlock != null) {
                final LinCompoundTag nbt = baseBlock.getNbt();
                if (nbt != null) {
                    entry.put("nbt", nbt.toBuilder().remove("x").remove("y").remove("z").build());
                }
            }
            blocks.add(entry.build());
        }

        // Entities
        LinListTag.Builder<@org.jetbrains.annotations.NotNull LinCompoundTag> entities = LinListTag.builder(LinTagType.compoundTag());
        for (final Entity entity : clipboard.getEntities()) {
            final Location location = entity.getLocation();
            final Vector3 exactPosition = location.subtract(min.x(), min.y(), min.z());
            final BlockVector3 blockPosition = entity.getBlockLocation().toBlockPoint().subtract(min);
            final BaseEntity baseEntity = entity.getState();
            LinCompoundTag.Builder nbt = null;
            if (baseEntity != null) {
                final LinCompoundTag contained = baseEntity.getNbt();
                if (contained != null) {
                    nbt = contained.toBuilder();
                }
            }
            if (nbt == null) {
                nbt = LinCompoundTag.builder();
            }
            entities.add(LinCompoundTag.builder()
                    .put("pos", LinListTag.of(LinTagType.doubleTag(), List.of(
                            LinDoubleTag.of(exactPosition.x()),
                            LinDoubleTag.of(exactPosition.y()),
                            LinDoubleTag.of(exactPosition.z())
                    )))
                    .put("blockPos", LinListTag.of(LinTagType.intTag(), List.of(
                            LinIntTag.of(blockPosition.x()),
                            LinIntTag.of(blockPosition.y()),
                            LinIntTag.of(blockPosition.z())
                    )))
                    .put("nbt", nbt
                            .putString("id", entity.getType().id())
                            .put("Rotation", LinListTag.of(LinTagType.floatTag(), List.of(
                                    LinFloatTag.of(location.getYaw()),
                                    LinFloatTag.of(location.getPitch())
                            )))
                            .build()
                    )
                    .build());
        }
        root.put("palette", palette.build())
                .put("blocks", blocks.build())
                .put("entities", entities.build());
        LinBinaryIO.write(this.out, new LinRootEntry("", root.build()));
        close();
    }

    @Override
    public void close() throws IOException {
        if (in != null && in instanceof Closeable closeable) {
            closeable.close();
        }
        if (out != null && out instanceof Closeable closeable) {
            closeable.close();
        }
    }

}
