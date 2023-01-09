package com.fastasyncworldedit.core.extent.clipboard;

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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DiskBasedClipboard implements Clipboard {

    private static final int INVALID_INDEX = -1;
    private static final BlockState AIR = Objects.requireNonNull(BlockTypes.AIR).getDefaultState();
    private static final BiomeType OCEAN = BiomeTypes.OCEAN;

    private final MemoryFile blockFile;
    private MemoryFile biomeFile;

    private final BlockVector3 dimensions;
    private final BlockVector3 offset;
    private final Path folder;
    private final Int2ReferenceMap<CompoundBinaryTag> nbt = new Int2ReferenceOpenHashMap<>();
    private BlockVector3 origin;

    DiskBasedClipboard(
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

    private static long requiredEntries(BlockVector3 dimensions) {
        return (long) dimensions.getX() * dimensions.getY() * dimensions.getZ();
    }

    private static long requiredBiomeEntries(BlockVector3 dimensions) {
        return requiredEntries(dimensions.divide(4));
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
            biomeFile = MemoryFile.create(folder.resolve("biomes.dc"), requiredBiomeEntries(this.dimensions), biomeCount);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        this.biomeFile = biomeFile;
        return biomeFile;
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
            this.blockFile.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void flush() {
        try {
            this.blockFile.flush();
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
        BinaryTagIO.writer().write(tiles, this.folder.resolve("entities.nbt"));
    }

    @Override
    public BlockVector3 getMinimumPoint() {
        return this.offset;
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return this.offset.add(this.dimensions.subtract(1, 1, 1));
    }

    private boolean inRegion(int x, int y, int z) {
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
        if (inRegion(lx, ly, lz)) {
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
        if (inRegion(lx, ly, lz)) {
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

}
