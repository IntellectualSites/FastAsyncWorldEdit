package com.fastasyncworldedit.nukkit.adapter;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.entity.Entity;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.generic.BaseFullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import com.fastasyncworldedit.nukkit.mapping.ItemMapping.NukkitItemData;
import com.fastasyncworldedit.nukkit.mapping.NukkitBlockData;
import org.cloudburstmc.nbt.NbtMap;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class TestNukkitImplAdapter implements NukkitImplAdapter {

    @Override
    public String getPlayerLanguageCode(Player player) {
        return null;
    }

    @Override
    public int getBlockRuntimeId(Player player, int fullId) {
        return fullId;
    }

    @Override
    public String getEntityIdentifier(Entity entity) {
        return null;
    }

    @Override
    public List<NbtMap> loadBlockPalette() {
        return List.of();
    }

    @Override
    @Nullable
    public NukkitBlockData createBlockData(NbtMap nbtState) {
        return null;
    }

    @Override
    public int getFullBlockId(Object chunk, int x, int y, int z, int layer) {
        return layer == 0 ? ((BaseFullChunk) chunk).getFullBlock(x, y, z) : getAirFullId();
    }

    @Override
    public void setFullBlockId(Object chunk, int x, int y, int z, int layer, int fullId) {
    }

    @Override
    public Object getChunk(Level level, int chunkX, int chunkZ) {
        return level.getChunk(chunkX, chunkZ, true);
    }

    @Override
    public int getChunkBiomeId(Object chunk, int x, int y, int z) {
        return ((BaseFullChunk) chunk).getBiomeId(x, z);
    }

    @Override
    public void setChunkBiomeId(Object chunk, int x, int y, int z, int biomeId) {
        ((BaseFullChunk) chunk).setBiomeId(x, z, (byte) biomeId);
    }

    @Override
    public int getLevelBiomeId(Level level, int x, int y, int z) {
        return level.getBiomeId(x, z);
    }

    @Override
    public void setLevelBiomeId(Level level, int x, int y, int z, int biomeId) {
        level.setBiomeId(x, z, (byte) biomeId);
    }

    @Override
    public Map<Long, BlockEntity> getBlockEntities(Object chunk) {
        return ((BaseFullChunk) chunk).getBlockEntities();
    }

    @Override
    @Nullable
    public BlockEntity getTile(Object chunk, int x, int y, int z) {
        return ((BaseFullChunk) chunk).getTile(x, y, z);
    }

    @Override
    public int getBlockSkyLight(Object chunk, int x, int y, int z) {
        return ((BaseFullChunk) chunk).getBlockSkyLight(x, y, z);
    }

    @Override
    public int getBlockLight(Object chunk, int x, int y, int z) {
        return ((BaseFullChunk) chunk).getBlockLight(x, y, z);
    }

    @Override
    public void setChunkChanged(Object chunk, boolean changed) {
        ((BaseFullChunk) chunk).setChanged(changed);
    }

    @Override
    @Nullable
    public BlockEntity createBlockEntity(String id, Object chunk, CompoundTag nbt) {
        return null;
    }

    @Override
    @Nullable
    public Entity createEntity(String id, Object chunk, CompoundTag nbt) {
        return null;
    }

    @Override
    public Map<Long, Entity> getChunkEntities(Level level, int chunkX, int chunkZ) {
        return level.getChunkEntities(chunkX, chunkZ);
    }

    @Override
    public int getFullBlockId(Block block) {
        return 0;
    }

    @Override
    public Block getBlock(int fullId) {
        return Block.get(0, 0);
    }

    @Override
    public boolean isWaterFullId(int fullId) {
        return false;
    }

    @Override
    public int getAirFullId() {
        return 0;
    }

    @Override
    public int getStillWaterFullId() {
        return 9 << getBlockDataBits();
    }

    @Override
    @Nullable
    public NukkitItemData createItemData(String bedrockId, int metadata) {
        return null;
    }

    @Override
    public String getItemMappingKey(Item item) {
        return item.getId() + ":" + item.getDamage();
    }

    @Override
    public String getItemMappingKey(NukkitItemData data) {
        return data.itemId() + ":" + data.metadata();
    }

    @Override
    public Item getItem(NukkitItemData data, int amount) {
        return Item.get(data.itemId(), data.metadata(), amount);
    }

    @Override
    public Item getAirItem() {
        return Item.get(Item.AIR);
    }

    @Override
    public boolean matchesItem(Item item, NukkitItemData data) {
        return item.getId() == data.itemId() && item.getDamage() == data.metadata();
    }

    @Override
    public boolean isAirItem(Item item) {
        return item.getId() == Item.AIR || item.isNull();
    }

    @Override
    public UUID getEntityUUID(Entity entity) {
        return new UUID(0, 0);
    }

    @Override
    public void recalculateLight(Object chunk) {
        // No-op for tests; BaseFullChunk light arrays are not exercised here.
    }

    @Override
    public boolean growTree(
            Level level,
            com.sk89q.worldedit.util.TreeGenerator.TreeType type,
            int x, int y, int z
    ) {
        return false;
    }

}
