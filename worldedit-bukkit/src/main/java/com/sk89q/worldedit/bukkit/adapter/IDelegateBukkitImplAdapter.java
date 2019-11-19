package com.sk89q.worldedit.bukkit.adapter;

import com.boydti.fawe.beta.implementation.packet.ChunkPacket;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.DataFixer;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.gamemode.GameMode;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Biome;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.OptionalInt;

public interface IDelegateBukkitImplAdapter<T> extends BukkitImplAdapter<T> {
    BukkitImplAdapter<T> getParent();

    @Override
    default int getDataVersion() {
        return getParent().getDataVersion();
    }

    @Override
    @Nullable
    default DataFixer getDataFixer() {
        return getParent().getDataFixer();
    }

    @Override
    default boolean supportsWatchdog() {
        return getParent().supportsWatchdog();
    }

    @Override
    default void tickWatchdog() {
        getParent().tickWatchdog();
    }

    @Override
    default BaseBlock getBlock(Location location) {
        return getParent().getBlock(location);
    }

    default boolean setBlock(Location location, BlockStateHolder state, boolean notifyAndLight) {
        return getParent().setBlock(location, state, notifyAndLight);
    }

    @Override
    default void notifyAndLightBlock(Location position, BlockState previousType) {
        getParent().notifyAndLightBlock(position, previousType);
    }

    @Override
    @Nullable
    default BaseEntity getEntity(Entity entity) {
        return getParent().getEntity(entity);
    }

    @Override
    @Nullable
    default Entity createEntity(Location location, BaseEntity state) {
        return getParent().createEntity(location, state);
    }

    @Override
    default Map<String, ? extends Property<?>> getProperties(BlockType blockType) {
        return getParent().getProperties(blockType);
    }

    @Override
    default void sendFakeNBT(Player player, BlockVector3 pos, CompoundTag nbtData) {
        getParent().sendFakeNBT(player, pos, nbtData);
    }

    @Override
    default void sendFakeOP(Player player) {
        getParent().sendFakeOP(player);
    }

    @Override
    default boolean simulateItemUse(World world, BlockVector3 position, BaseItem item, Direction face) {
        return getParent().simulateItemUse(world, position, item, face);
    }

    @Override
    default ItemStack adapt(BaseItemStack item) {
        return getParent().adapt(item);
    }

    @Override
    default BaseItemStack adapt(ItemStack itemStack) {
        return getParent().adapt(itemStack);
    }

    @Override
    default OptionalInt getInternalBlockStateId(BlockData data) {
        return getParent().getInternalBlockStateId(data);
    }

    @Override
    default OptionalInt getInternalBlockStateId(BlockState state) {
        return getParent().getInternalBlockStateId(state);
    }

    @Override
    default BlockMaterial getMaterial(BlockType blockType) {
        return getParent().getMaterial(blockType);
    }

    @Override
    default BlockMaterial getMaterial(BlockState blockState) {
        return getParent().getMaterial(blockState);
    }

    default Tag toNative(T foreign) {
        return getParent().toNative(foreign);
    }

    @Override
    default T fromNative(Tag foreign) {
        return getParent().fromNative(foreign);
    }

    @Override
    @Nullable
    default World createWorld(WorldCreator creator) {
        return getParent().createWorld(creator);
    }

    @Override
    default void sendFakeChunk(World world, Player player, ChunkPacket packet) {
        getParent().sendFakeChunk(world, player, packet);
    }

    @Override
    default BukkitWorld asBukkitWorld(com.sk89q.worldedit.world.World world) {
        return getParent().asBukkitWorld(world);
    }

    @Override
    default World adapt(com.sk89q.worldedit.world.World world) {
        return getParent().adapt(world);
    }

    @Override
    default Location adapt(World world, Vector3 position) {
        return getParent().adapt(world, position);
    }

    @Override
    default Location adapt(World world, BlockVector3 position) {
        return getParent().adapt(world, position);
    }

    @Override
    default Location adapt(World world, com.sk89q.worldedit.util.Location location) {
        return getParent().adapt(world, location);
    }

    @Override
    default Vector3 asVector(Location location) {
        return getParent().asVector(location);
    }

    @Override
    default BlockVector3 asBlockVector(Location location) {
        return getParent().asBlockVector(location);
    }

    @Override
    default com.sk89q.worldedit.entity.Entity adapt(Entity entity) {
        return getParent().adapt(entity);
    }

    @Override
    default Material adapt(ItemType itemType) {
        return getParent().adapt(itemType);
    }

    @Override
    default Material adapt(BlockType blockType) {
        return getParent().adapt(blockType);
    }

    @Override
    default EntityType adapt(com.sk89q.worldedit.world.entity.EntityType entityType) {
        return getParent().adapt(entityType);
    }

    @Override
    default BlockType asBlockType(Material material) {
        return getParent().asBlockType(material);
    }

    @Override
    default ItemType asItemType(Material material) {
        return getParent().asItemType(material);
    }

    @Override
    default BlockState adapt(BlockData blockData) {
        return getParent().adapt(blockData);
    }

    @Override
    default BlockData adapt(BlockStateHolder block) {
        return getParent().adapt(block);
    }

    @Override
    default BukkitPlayer adapt(Player player) {
        return getParent().adapt(player);
    }

    @Override
    default Player adapt(com.sk89q.worldedit.entity.Player player) {
        return getParent().adapt(player);
    }

    @Override
    default Biome adapt(BiomeType biomeType) {
        return getParent().adapt(biomeType);
    }

    @Override
    default BiomeType adapt(Biome biome) {
        return getParent().adapt(biome);
    }

    @Override
    default boolean equals(BlockType blockType, Material type) {
        return getParent().equals(blockType, type);
    }

    @Override
    default com.sk89q.worldedit.world.World adapt(World world) {
        return getParent().adapt(world);
    }

    @Override
    default GameMode adapt(org.bukkit.GameMode gameMode) {
        return getParent().adapt(gameMode);
    }

    @Override
    default com.sk89q.worldedit.world.entity.EntityType adapt(EntityType entityType) {
        return getParent().adapt(entityType);
    }

    @Override
    default BlockState asBlockState(ItemStack itemStack) {
        return getParent().asBlockState(itemStack);
    }
}
