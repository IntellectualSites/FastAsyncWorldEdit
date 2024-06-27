/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.bukkit.adapter;

import com.fastasyncworldedit.bukkit.FaweBukkit;
import com.fastasyncworldedit.bukkit.adapter.IBukkitAdapter;
import com.fastasyncworldedit.bukkit.adapter.NMSRelighterFactory;
import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.extent.processor.lighting.RelighterFactory;
import com.fastasyncworldedit.core.queue.IBatchProcessor;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.implementation.packet.ChunkPacket;
import com.sk89q.jnbt.LinBusConverter;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.internal.wna.WorldNativeAccess;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.world.DataFixer;
import com.sk89q.worldedit.world.RegenOptions;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Biome;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinTag;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An interface for adapters of various Bukkit implementations.
 */
//FAWE start - Generic & extends IBukkitAdapter
public interface BukkitImplAdapter<T> extends IBukkitAdapter {
//FAWE end

    /**
     * Get a data fixer, or null if not supported.
     *
     * @return the data fixer
     */
    @Nullable
    DataFixer getDataFixer();

    /**
     * Check if this adapter supports the watchdog.
     *
     * @return {@code true} if {@link #tickWatchdog()} is implemented
     */
    default boolean supportsWatchdog() {
        return false;
    }

    /**
     * Tick the server watchdog, if possible.
     */
    default void tickWatchdog() {
    }

    /**
     * Get the block at the given location.
     *
     * @param location the location
     * @return the block
     */
    BlockState getBlock(Location location);

    /**
     * Get the block with NBT data at the given location.
     *
     * @param location the location
     * @return the block
     */
    BaseBlock getFullBlock(Location location);

    /**
     * Create a {@link WorldNativeAccess} for the given world reference.
     *
     * @param world the world reference
     * @return the native access object
     */
    WorldNativeAccess<?, ?, ?> createWorldNativeAccess(World world);

    /**
     * Get the state for the given entity.
     *
     * @param entity the entity
     * @return the state, or null
     */
    @Nullable
    BaseEntity getEntity(Entity entity);

    /**
     * Create the given entity.
     *
     * @param location the location
     * @param state    the state
     * @return the created entity or null
     */
    @Nullable
    Entity createEntity(Location location, BaseEntity state);

    /**
     * Gets the name for the given block.
     *
     * @param blockType the block
     * @return The name
     */
    Component getRichBlockName(BlockType blockType);

    /**
     * Gets the name for the given item.
     *
     * @param itemType the item
     * @return The name
     */
    Component getRichItemName(ItemType itemType);

    /**
     * Gets the name for the given item stack.
     *
     * @param itemStack the item stack
     * @return The name
     */
    Component getRichItemName(BaseItemStack itemStack);

    /**
     * Get a map of {@code string -> property}.
     *
     * @param blockType The block type
     * @return The properties map
     */
    Map<String, ? extends Property<?>> getProperties(BlockType blockType);

    /**
     * Send the given NBT data to the player.
     *
     * @param player  The player
     * @param pos     The position
     * @param nbtData The NBT Data
     */
    void sendFakeNBT(Player player, BlockVector3 pos, LinCompoundTag nbtData);

    /**
     * Make the client think it has operator status.
     * This does not give them any operator capabilities.
     *
     * @param player The player
     */
    void sendFakeOP(Player player);

    /**
     * Simulates a player using an item.
     *
     * @param world    the world
     * @param position the location
     * @param item     the item to be used
     * @param face     the direction in which to "face" when using the item
     * @return whether the usage was successful
     */
    default boolean simulateItemUse(World world, BlockVector3 position, BaseItem item, Direction face) {
        return false;
    }

    /**
     * Gets whether the given {@link BlockState} can be placed here.
     *
     * @param world      The world
     * @param position   The position
     * @param blockState The blockstate
     * @return If it can be placed
     */
    boolean canPlaceAt(World world, BlockVector3 position, BlockState blockState);

    /**
     * Create a Bukkit ItemStack with NBT, if available.
     *
     * @param item the WorldEdit BaseItemStack to adapt
     * @return the Bukkit ItemStack
     */
    ItemStack adapt(BaseItemStack item);

    /**
     * Create a WorldEdit ItemStack with NBT, if available.
     *
     * @param itemStack the Bukkit ItemStack to adapt
     * @return the WorldEdit BaseItemStack
     */
    BaseItemStack adapt(ItemStack itemStack);

    /**
     * Get the {@link SideEffect}s that this adapter supports.
     *
     * @return The side effects that are supported
     */
    Set<SideEffect> getSupportedSideEffects();

    default OptionalInt getInternalBlockStateId(BlockData data) {
        //FAWE start
        // return OptionalInt.empty();
        return getInternalBlockStateId(BukkitAdapter.adapt(data));
        //FAWE end
    }

    /**
     * Retrieve the internal ID for a given state, if possible.
     *
     * @param state The block state
     * @return the internal ID of the state
     */
    default OptionalInt getInternalBlockStateId(BlockState state) {
        return OptionalInt.empty();
    }

    /**
     * Regenerate a region in the given world, so it appears "as new".
     *
     * @param world   the world to regen in
     * @param region  the region to regen
     * @param extent  the extent to use for setting blocks
     * @param options the regeneration options
     * @return true on success, false on failure
     */
    default boolean regenerate(World world, Region region, Extent extent, RegenOptions options) throws Exception {
        throw new UnsupportedOperationException("This adapter does not support regeneration.");
    }

    /**
     * Clears the contents of a Clearable block.
     *
     * @param world The world
     * @param pt    The location
     * @return If a block was cleared
     */
    default boolean clearContainerBlockContents(World world, BlockVector3 pt) {
        throw new UnsupportedOperationException("This adapter does not support clearing block contents.");
    }

    /**
     * Set the biome at a location.
     *
     * @param location the location
     * @param biome    the new biome
     */
    default void setBiome(Location location, BiomeType biome) {
        throw new UnsupportedOperationException("This adapter does not support custom biomes.");
    }

    /**
     * Gets the current biome at a location.
     *
     * @param location the location
     * @return the biome
     */
    default BiomeType getBiome(Location location) {
        throw new UnsupportedOperationException("This adapter does not support custom biomes.");
    }

    /**
     * Initialize registries that require NMS access.
     */
    default void initializeRegistries() {

    }

    /**
     * Sends biome updates for the given chunks.
     *
     * <p>This doesn't modify biomes at all, it just sends the current state of the biomes
     * in the world to all of the nearby players, updating the visual representation of the
     * biomes on their clients.</p>
     *
     * @param world  the world
     * @param chunks a list of chunk coordinates to send biome updates for
     */
    default void sendBiomeUpdates(World world, Iterable<BlockVector2> chunks) {
    }

    //FAWE start
    default BlockMaterial getMaterial(BlockType blockType) {
        return getMaterial(blockType.getDefaultState());
    }

    default BlockMaterial getMaterial(BlockState blockState) {
        return null;
    }

    @Deprecated
    default Tag toNative(T foreign) {
        return LinBusConverter.toJnbtTag(toNativeLin(foreign));
    }

    default LinTag<?> toNativeLin(T foreign) {
        return toNative(foreign).toLinTag();
    }

    @Deprecated
    default T fromNative(Tag foreign) {
        if (foreign == null) {
            return null;
        }
        return fromNativeLin(foreign.toLinTag());
    }

    default T fromNativeLin(LinTag<?> foreign) {
        if (foreign == null) {
            return null;
        }
        return fromNative(LinBusConverter.toJnbtTag(foreign));
    }

    @Nullable
    default World createWorld(WorldCreator creator) {
        return ((FaweBukkit) Fawe.platform()).createWorldUnloaded(creator::createWorld);
    }

    /**
     * Send a fake chunk packet to a player.
     */
    default void sendFakeChunk(org.bukkit.World world, Player player, ChunkPacket packet) {
        throw new UnsupportedOperationException("Cannot send fake chunks");
    }

    default IChunkGet get(World world, int chunkX, int chunkZ) {
        throw new UnsupportedOperationException();
    }

    default int getInternalBiomeId(BiomeType biome) {
        return Biome.BADLANDS.ordinal();
    }

    /**
     * Returns an iterable of all biomes known to the server.
     *
     * @return all biomes known to the server.
     */
    default Iterable<NamespacedKey> getRegisteredBiomes() {
        return Arrays.stream(Biome.values())
                .map(Keyed::getKey)
                .collect(Collectors.toList());
    }

    default RelighterFactory getRelighterFactory() {
        return new NMSRelighterFactory(); // TODO implement in adapters instead
    }

    default Map<String, List<Property<?>>> getAllProperties() {
        return Collections.emptyMap();
    }

    /**
     * Returns an {@link IBatchProcessor} instance for post-processing of chunks to sort ticking of placed/existing blocks and
     * fluids if the plugin is configured to do so
     *
     * @since 2.1.0
     */
    default IBatchProcessor getTickingPostProcessor() {
        return null;
    }
    //FAWE end
}
