package com.boydti.fawe.bukkit.adapter.mc1_16_1;


import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.IBlocks;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.beta.implementation.lighting.HeightMapType;
import com.boydti.fawe.bukkit.adapter.mc1_16_1.nbt.LazyCompoundTag_1_16_1;
import com.google.common.base.Suppliers;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import net.minecraft.server.v1_16_R1.BiomeBase;
import net.minecraft.server.v1_16_R1.BiomeStorage;
import net.minecraft.server.v1_16_R1.Entity;
import net.minecraft.server.v1_16_R1.NBTTagCompound;
import net.minecraft.server.v1_16_R1.TileEntity;
import net.minecraft.server.v1_16_R1.WorldServer;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftBlock;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;

public class BukkitGetBlocks_1_16_1_Copy implements IChunkGet {

    private final Map<BlockVector3, CompoundTag> tiles = new HashMap<>();
    private final Set<CompoundTag> entities = new HashSet<>();
    private BiomeStorage biomeStorage;
    private final char[][] blocks = new char[16][];
    private final WorldServer world;

    protected BukkitGetBlocks_1_16_1_Copy(WorldServer world) {
        this.world = world;
    }

    protected void storeTile(TileEntity tile) {
        tiles.put(BlockVector3.at(tile.getPosition().getX(), tile.getPosition().getY(), tile.getPosition().getZ()),
            new LazyCompoundTag_1_16_1(Suppliers.memoize(() -> tile.save(new NBTTagCompound()))));
    }

    @Override
    public Map<BlockVector3, CompoundTag> getTiles() {
        return tiles;
    }

    @Override
    @Nullable
    public CompoundTag getTile(int x, int y, int z) {
        return tiles.get(BlockVector3.at(x, y, z));
    }

    protected void storeEntity(Entity entity) {
        BukkitImplAdapter adapter = WorldEditPlugin.getInstance().getBukkitImplAdapter();
        NBTTagCompound tag = new NBTTagCompound();
        entities.add((CompoundTag) adapter.toNative(entity.save(tag)));
    }

    @Override
    public Set<CompoundTag> getEntities() {
        return this.entities;
    }

    @Override
    public CompoundTag getEntity(UUID uuid) {
        for (CompoundTag tag : entities) {
            UUID tagUUID;
            if (tag.containsKey("UUID")) {
                int[] arr = tag.getIntArray("UUID");
                tagUUID = new UUID((long) arr[0] << 32 | (arr[1] & 0xFFFFFFFFL), (long) arr[2] << 32 | (arr[3] & 0xFFFFFFFFL));
            } else if (tag.containsKey("UUIDMost")) {
                tagUUID = new UUID(tag.getLong("UUIDMost"), tag.getLong("UUIDLeast"));
            } else if (tag.containsKey("PersistentIDMSB")) {
                tagUUID = new UUID(tag.getLong("PersistentIDMSB"), tag.getLong("PersistentIDLSB"));
            } else {
                return null;
            }
            if (uuid.equals(tagUUID)) {
                return tag;
            }
        }
        return null;
    }

    @Override
    public void setCreateCopy(boolean createCopy) {

    }

    @Override
    public boolean isCreateCopy() {
        return false;
    }

    @Override
    public void setLightingToGet(char[][] lighting) {}

    @Override
    public void setSkyLightingToGet(char[][] lighting) {}

    @Override
    public void setHeightmapToGet(HeightMapType type, int[] data) {}

    protected void storeBiomes(BiomeStorage biomeStorage) {
        this.biomeStorage = new BiomeStorage(BukkitAdapter_1_16_1.getBiomeArray(biomeStorage).clone());
    }

    @Override
    public BiomeType getBiomeType(int x, int y, int z) {
        BiomeBase base = null;
        if (y == -1) {
            for (y = 0; y < FaweCache.IMP.WORLD_HEIGHT; y++) {
                base = biomeStorage.getBiome(x >> 2, y >> 2, z >> 2);
                if (base != null) break;
            }
        } else {
            base = biomeStorage.getBiome(x >> 2, y >> 2, z >> 2);
        }
        return base != null ? BukkitAdapter.adapt(CraftBlock.biomeBaseToBiome(base)) : null;
    }

    @Override
    public void removeSectionLighting(int layer, boolean sky) {}

    @Override
    public boolean trim(boolean aggressive, int layer) {
        return false;
    }

    @Override
    public IBlocks reset() {
        return null;
    }

    protected void storeSection(int layer, char[] data) {
        blocks[layer] = data;
    }

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        BlockState state = BlockTypesCache.states[get(x, y, z)];
        return state.toBaseBlock(this, x, y, z);
    }

    @Override
    public boolean hasSection(@Range(from = 0, to = 15) int layer) {
        return blocks[layer] != null;
    }

    @Override
    public char[] load(int layer) {
        return blocks[layer];
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return BlockTypesCache.states[get(x, y, z)];
    }

    @Override
    public int getSkyLight(int x, int y, int z) {
        return 0;
    }

    @Override
    public int getEmmittedLight(int x, int y, int z) {
        return 0;
    }

    @Override
    public int[] getHeightMap(HeightMapType type) {
        return new int[0];
    }

    @Override
    public <T extends Future<T>> T call(IChunkSet set, Runnable finalize) {
        return null;
    }

    public char get(int x, int y, int z) {
        final int layer = y >> 4;
        final int index = (y & 15) << 8 | z << 4 | x;
        return blocks[layer][index];
    }


    @Override
    public boolean trim(boolean aggressive) {
        return false;
    }
}
