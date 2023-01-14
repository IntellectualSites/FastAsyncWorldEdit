package com.fastasyncworldedit.core.extent.clipboard;

import com.fastasyncworldedit.core.util.MathMan;
import com.fastasyncworldedit.core.util.io.MemoryFile;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.nbt.BinaryTagIO;
import com.sk89q.worldedit.util.nbt.CompoundBinaryTag;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public sealed class DiskBasedClipboard implements Clipboard {

    private static final int INVALID_INDEX = -1;
    private static final BlockState AIR = Objects.requireNonNull(BlockTypes.AIR).getDefaultState();
    private static final BiomeType OCEAN = BiomeTypes.OCEAN;

    private final MemoryFile blockFile;
    private @MonotonicNonNull MemoryFile biomeFile;

    private final BlockVector3 dimensions;
    private final BlockVector3 offset;
    private final Path folder;
    private final Int2ReferenceMap<CompoundBinaryTag> nbt = new Int2ReferenceOpenHashMap<>();
    private BlockVector3 origin;

    static DiskBasedClipboard create(
            final Region region,
            final BlockVector3 origin,
            final Path folder
    ) {
        if (region instanceof CuboidRegion) {
            return new DiskBasedClipboard(region.getDimensions(), region.getMinimumPoint(), origin, folder);
        }
        return new NonCuboidRegionDiskBasedClipboard(region, origin, folder);
    }

    private DiskBasedClipboard(
            final BlockVector3 dimensions,
            final BlockVector3 offset,
            final BlockVector3 origin,
            final Path folder
    ) {
        this.dimensions = dimensions;
        this.offset = offset;
        this.origin = origin;
        this.folder = folder;
        long entries = requiredEntries(dimensions);
        try {
            this.blockFile = MemoryFile.create(folder.resolve("blocks.dc"), entries, BlockTypesCache.states.length);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    DiskBasedClipboard(final Path folder) throws IOException {
        this.folder = folder;
        final Metadata metadata = Metadata.read(folder);
        this.dimensions = metadata.dimensions();
        this.offset = metadata.offset();
        this.origin = metadata.origin();
        this.blockFile = MemoryFile.load(folder.resolve("blocks.dc"), BlockTypesCache.states.length);
        loadBiomeFileIfPresent();
    }

    private static long requiredEntries(BlockVector3 dimensions) {
        return (long) dimensions.getX() * dimensions.getY() * dimensions.getZ();
    }

    private static long requiredBiomeEntries(BlockVector3 dimensions) {
        // biomes are based on 4*4*4, so we can divide the dimensions of the region by 4
        BlockVector3 biomeDimensions = BlockVector3.at(
                MathMan.ceilDiv(dimensions.getX(), 4),
                MathMan.ceilDiv(dimensions.getY(), 4),
                MathMan.ceilDiv(dimensions.getZ(), 4)
        );
        return requiredEntries(biomeDimensions);
    }

    @Override
    public BaseBlock getFullBlock(final int x, final int y, final int z) {
        final int index = toLocalIndex(x, y, z);
        if (index != INVALID_INDEX) {
            BlockState state = getBlock(index);
            CompoundBinaryTag tag = this.nbt.get(index);
            return state.toBaseBlock(tag); // passing null is fine
        }
        return AIR.toBaseBlock();
    }

    @Override
    public BiomeType getBiomeType(final int x, final int y, final int z) {
        MemoryFile memoryFile = ensureBiomeFile();
        final int index = toLocalBiomeIndex(x, y, z);
        if (index != INVALID_INDEX) {
            return BiomeTypes.get(memoryFile.getValue(index));
        }
        return OCEAN; // as per documentation in InputExtent
    }

    @Override
    public BlockState getBlock(final int x, final int y, final int z) {
        final int index = toLocalIndex(x, y, z);
        if (index != INVALID_INDEX) {
            return getBlock(index);
        }
        return AIR;
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(final int x, final int y, final int z, final B block) throws
            WorldEditException {
        final int index = toLocalIndex(x, y, z);
        if (index != INVALID_INDEX) {
            this.blockFile.setValue(index, getId(block));
            if (block instanceof BaseBlock bb) {
                dealWithNbt(bb, index);
            }
            return true;
        }
        return false;
    }

    private void dealWithNbt(BaseBlock bb, int index) {
        final CompoundBinaryTag value = bb.getNbt();
        if (value != null) {
            nbt.put(index, value);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean setTile(final int x, final int y, final int z, final CompoundTag tile) throws WorldEditException {
        final int index = toLocalIndex(x, y, z);
        if (index != INVALID_INDEX) {
            this.nbt.put(index, tile.asBinaryTag());
            return true;
        }
        return false;
    }

    @Override
    public boolean fullySupports3DBiomes() {
        return true;
    }

    @Override
    public boolean setBiome(final int x, final int y, final int z, final BiomeType biome) {
        MemoryFile biomeFile = ensureBiomeFile();
        final int index = toLocalBiomeIndex(x, y, z);
        if (index != INVALID_INDEX) {
            biomeFile.setValue(index, biome.getInternalId());
            return true;
        }
        return false;
    }

    private MemoryFile ensureBiomeFile() {
        MemoryFile biomeFile = this.biomeFile;
        if (biomeFile != null) {
            return biomeFile;
        }
        int biomeCount = BiomeTypes.getMaxId();
        try {
            biomeFile = MemoryFile.create(this.folder.resolve("biomes.dc"), requiredBiomeEntries(this.dimensions), biomeCount);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        this.biomeFile = biomeFile;
        return biomeFile;
    }

    private void loadBiomeFileIfPresent() throws IOException {
        Path biomeFilePath = this.folder.resolve("biomes.dc");
        if (Files.isRegularFile(biomeFilePath)) {
            this.biomeFile = MemoryFile.load(biomeFilePath, BiomeTypes.getMaxId());
        }
    }

    @Override
    public Region getRegion() {
        return new CuboidRegion(getMinimumPoint(), getMaximumPoint());
    }

    @Override
    public BlockVector3 getDimensions() {
        return this.dimensions;
    }

    @Override
    public BlockVector3 getOrigin() {
        return this.origin;
    }

    @Override
    public void setOrigin(final BlockVector3 origin) {
        this.origin = origin;
    }

    @Override
    public boolean hasBiomes() {
        return this.biomeFile != null;
    }

    @Override
    public void removeEntity(final Entity entity) {

    }

    @Override
    public void close() {
        try {
            Metadata.write(new Metadata(this.offset, this.dimensions, this.origin), this.folder);
            this.blockFile.close();
            if (this.biomeFile != null) {
                this.biomeFile.flush();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void flush() {
        try {
            this.blockFile.flush();
            if (this.biomeFile != null) {
                this.biomeFile.close();
            }
            writeNbt();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeNbt() throws IOException {
        if (this.nbt.isEmpty()) {
            return;
        }
        Map<String, CompoundBinaryTag> converted = new HashMap<>();
        for (final Int2ReferenceMap.Entry<CompoundBinaryTag> entry : this.nbt.int2ReferenceEntrySet()) {
            converted.put(String.valueOf(entry.getIntKey()), entry.getValue());
        }
        CompoundBinaryTag tiles = CompoundBinaryTag.from(converted);
        BinaryTagIO.writer().write(tiles, this.folder.resolve("blockEntities.nbt"));
    }

    @Override
    public BlockVector3 getMinimumPoint() {
        return this.offset;
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return this.offset.add(this.dimensions.subtract(1, 1, 1));
    }

    protected boolean inRegionNoOffset(int x, int y, int z) {
        return (x | y | z) >= 0 && y < this.dimensions.getY() && x < this.dimensions.getX() && z < this.dimensions.getZ();
    }

    @SuppressWarnings("deprecation")
    private <B extends BlockStateHolder<B>> int getId(B b) {
        return b.getOrdinal();
    }

    private int toLocalIndex(int x, int y, int z) {
        int lx = x - this.offset.getX();
        int ly = y - this.offset.getY();
        int lz = z - this.offset.getZ();
        if (inRegionNoOffset(lx, ly, lz)) {
            return toIndexUnchecked(lx, ly, lz);
        }
        return INVALID_INDEX;
    }

    // l = local
    private int toIndexUnchecked(int lx, int ly, int lz) {
        // chosen to correspond to iteration order in CuboidRegion#iterator()
        // to minimize cache misses/page faults
        return lx + this.dimensions.getX() * (lz + ly * this.dimensions.getZ());
    }

    private int toLocalBiomeIndex(int x, int y, int z) {
        int lx = x - this.offset.getX();
        int ly = y - this.offset.getY();
        int lz = z - this.offset.getZ();
        if (inRegionNoOffset(lx, ly, lz)) {
            return toBiomeIndexUnchecked(lx >> 2, ly >> 2, lz >> 2);
        }
        return INVALID_INDEX;
    }

    // b = biome, l = local
    private int toBiomeIndexUnchecked(int blx, int bly, int blz) {
        // chosen to correspond to iteration order in CuboidRegion#iterator()
        // to minimize cache misses/page faults
        return blx + (this.dimensions.getX() >> 2) * (blz + bly * (this.dimensions.getZ() >> 2));
    }

    private BlockState getBlock(final int i) {
        return BlockTypesCache.states[this.blockFile.getValue(i)];
    }

    private record Metadata(
            BlockVector3 offset,
            BlockVector3 dimensions,
            BlockVector3 origin
    ) {

        private static final int REQUIRED_BYTES = Integer.BYTES * 3 /* vectors */ * 3 /* ints per vector*/;

        static Metadata read(Path folder) throws IOException {
            Path file = getFile(folder);
            byte[] bytes = Files.readAllBytes(file);
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            BlockVector3 offset = getBlockVector3(buffer);
            BlockVector3 dimension = getBlockVector3(buffer);
            BlockVector3 origin = getBlockVector3(buffer);
            return new Metadata(offset, dimension, origin);
        }

        private static BlockVector3 getBlockVector3(ByteBuffer buffer) {
            return BlockVector3.at(buffer.getInt(), buffer.getInt(), buffer.getInt());
        }

        static void write(Metadata metadata, Path folder) throws IOException {
            Path file = getFile(folder);
            byte[] raw = new byte[REQUIRED_BYTES];
            ByteBuffer buffer = ByteBuffer.wrap(raw);
            putBlockVector3(buffer, metadata.offset);
            putBlockVector3(buffer, metadata.dimensions);
            putBlockVector3(buffer, metadata.origin);
            Files.write(file, raw);
        }

        @NotNull
        private static Path getFile(Path folder) {
            return folder.resolve(".metadata");
        }

        static void putBlockVector3(ByteBuffer buffer, BlockVector3 vector) {
            buffer.putInt(vector.getX());
            buffer.putInt(vector.getY());
            buffer.putInt(vector.getZ());
        }

    }

    // for regions other than CuboidRegion, we fall back to a slower check
    static final class NonCuboidRegionDiskBasedClipboard extends DiskBasedClipboard {

        private final Region region;

        public NonCuboidRegionDiskBasedClipboard(Region region, BlockVector3 origin, Path folder) {
            super(region.getDimensions(), region.getMinimumPoint(), origin, folder);
            this.region = region;
        }

        @Override
        protected boolean inRegionNoOffset(final int x, final int y, final int z) {
            final BlockVector3 minimumPoint = this.region.getMinimumPoint();
            return this.region.contains(minimumPoint.getX() + x, minimumPoint.getY() + y, minimumPoint.getZ() + z);
        }

        @Override
        public Region getRegion() {
            return this.region;
        }

    }

}
