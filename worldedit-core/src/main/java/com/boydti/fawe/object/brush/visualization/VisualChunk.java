package com.boydti.fawe.object.brush.visualization;

import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.collection.SparseBitSet;
import com.boydti.fawe.object.visitor.FaweChunkVisitor;
import com.boydti.fawe.util.MathMan;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * FAWE visualizations display glass (20) as a placeholder
 * - Using a non transparent block can cause FPS lag
 */
public class VisualChunk extends FaweChunk<FaweChunk> {

    public static BlockStateHolder VISUALIZE_BLOCK = BlockTypes.BLACK_STAINED_GLASS.getDefaultState();

    private SparseBitSet add;
    private SparseBitSet remove;

    /**
     * A FaweSections object represents a chunk and the blocks that you wish to change in it.
     *
     * @param x
     * @param z
     */
    public VisualChunk(int x, int z) {
        super(null, x, z);
        this.add = new SparseBitSet();
        this.remove = new SparseBitSet();
    }

    protected VisualChunk(int x, int z, SparseBitSet add, SparseBitSet remove) {
        super(null, x, z);
        this.add = add;
        this.remove = remove;
    }

    public int size() {
        return add.cardinality() + remove.cardinality();
    }

    private final int getIndex(int x, int y, int z) {
        return MathMan.tripleBlockCoordChar(x, y, z);
    }

    @Override
    public int getBitMask() {
        return 0;
    }

    @Override
    public int getBlockCombinedId(int x, int y, int z) {
        int index = getIndex(x, y, z);
        if (add.get(index)) {
            return VISUALIZE_BLOCK.getInternalId();
        } else if (remove.get(index)) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public void forEachQueuedBlock(FaweChunkVisitor onEach) {
        int index = -1;
        while ((index = add.nextSetBit(index + 1)) != -1) {
            int x = MathMan.untripleBlockCoordX(index);
            int y = MathMan.untripleBlockCoordY(index);
            int z = MathMan.untripleBlockCoordZ(index);
            onEach.run(x, y, z, VISUALIZE_BLOCK.getInternalId());
        }
        index = -1;
        while ((index = remove.nextSetBit(index + 1)) != -1) {
            int x = MathMan.untripleBlockCoordX(index);
            int y = MathMan.untripleBlockCoordY(index);
            int z = MathMan.untripleBlockCoordZ(index);
            onEach.run(x, y, z, 1);
        }
    }

    @Override
    public BiomeType[] getBiomeArray() {
        return new BiomeType[256];
    }

    @Override
    public FaweChunk getChunk() {
        return this;
    }

    @Override
    public void setTile(int x, int y, int z, CompoundTag tile) {
        // Unsupported
    }

    @Override
    public void setEntity(CompoundTag entity) {
        // Unsupported
    }

    @Override
    public void removeEntity(UUID uuid) {
        // Unsupported
    }

    @Override
    public void setBlock(int x, int y, int z, int combinedId) {
        int index = getIndex(x, y, z);
        try {
            if (BlockTypes.getFromStateId(combinedId).getMaterial().isAir()) {
                add.clear(index);
                remove.set(index);
            } else {
                remove.clear(index);
                add.set(index);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void unset(int x, int y, int z) {
        int index = getIndex(x, y, z);
        remove.clear(index);
        add.clear(index);
    }

    @Override
    public Set<CompoundTag> getEntities() {
        return new HashSet<>();
    }

    @Override
    public Set<UUID> getEntityRemoves() {
        return new HashSet<>();
    }

    @Override
    public Map<Short, CompoundTag> getTiles() {
        return new HashMap<>();
    }

    @Override
    public CompoundTag getTile(int x, int y, int z) {
        return null;
    }

    @Override
    public void setBiome(int x, int z, BiomeType biome) {
        // Unsupported
    }

    @Override
    public FaweChunk copy(boolean shallow) {
        if (shallow) {
            return new VisualChunk(getX(), getZ(), add, remove);
        } else {
            return new VisualChunk(getX(), getZ(), add.clone(), remove.clone());
        }
    }

    @Override
    public FaweChunk call() {
        return this;
    }
}
