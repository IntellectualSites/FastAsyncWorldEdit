package com.fastasyncworldedit.forge1710.world;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.implementation.packet.ChunkPacket;
import com.fastasyncworldedit.core.util.TaskManager;
import com.fastasyncworldedit.forge1710.registry.NativeBlockMapper;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.world.AbstractWorld;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.generation.TreeType;
import com.fastasyncworldedit.forge1710.Forge1710Adapter;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

public class Forge1710World extends AbstractWorld {

    private final WeakReference<WorldServer> worldRef;
    private final String name;

    public Forge1710World(WorldServer world) {
        this.worldRef = new WeakReference<>(world);
        this.name = worldName(world);
    }

    public static String worldName(WorldServer world) {
        String base = world.getWorldInfo().getWorldName();
        int dimension = world.provider.dimensionId;
        return dimension == 0 ? base : base + "_DIM" + dimension;
    }

    public WorldServer getWorld() {
        WorldServer world = worldRef.get();
        if (world == null) {
            throw new IllegalStateException("World '" + name + "' has been unloaded");
        }
        return world;
    }

    static <T> T onMainThread(Supplier<T> supplier) {
        if (Fawe.isMainThread()) {
            return supplier.get();
        }
        return TaskManager.taskManager().sync(supplier);
    }

    /**
     * Loaded chunks are read directly; unloaded chunks are loaded on the server thread (chunk providers are not
     * thread safe in 1.7.10).
     */
    public Chunk getChunk(int chunkX, int chunkZ) {
        WorldServer world = getWorld();
        if (Fawe.isMainThread() || world.theChunkProviderServer.chunkExists(chunkX, chunkZ)) {
            return world.getChunkFromChunkCoords(chunkX, chunkZ);
        }
        return onMainThread(() -> world.getChunkFromChunkCoords(chunkX, chunkZ));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getNameUnsafe() {
        return name;
    }

    @Override
    public String id() {
        return name.replace(" ", "_").toLowerCase(Locale.ROOT);
    }

    @Override
    public Path getStoragePath() {
        WorldServer world = getWorld();
        java.io.File base = world.getSaveHandler().getWorldDirectory();
        String folder = world.provider.getSaveFolder();
        return (folder == null ? base : new java.io.File(base, folder)).toPath();
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getMaxY() {
        return 255;
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        if (y < 0 || y > 255) {
            return BlockTypes.AIR.getDefaultState();
        }
        Chunk chunk = getChunk(x >> 4, z >> 4);
        Block block = chunk.getBlock(x & 15, y, z & 15);
        int meta = chunk.getBlockMetadata(x & 15, y, z & 15);
        return NativeBlockMapper.get().toState(block, meta);
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        return getBlock(position.x(), position.y(), position.z());
    }

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        return getBlock(x, y, z).toBaseBlock();
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        return getFullBlock(position.x(), position.y(), position.z());
    }

    @Override
    public BiomeType getBiomeType(int x, int y, int z) {
        BiomeGenBase biome = getWorld().getBiomeGenForCoords(x, z);
        BiomeType type = biome == null ? null : BiomeTypes.get(Forge1710Adapter.biomeId(biome));
        return type != null ? type : BiomeTypes.PLAINS;
    }

    @Override
    public BiomeType getBiome(BlockVector3 position) {
        return getBiomeType(position.x(), position.y(), position.z());
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        return false;
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        return false;
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 position, B block, SideEffectSet sideEffects)
            throws WorldEditException {
        int x = position.x();
        int y = position.y();
        int z = position.z();
        if (y < 0 || y > 255) {
            return false;
        }
        int nativeId = NativeBlockMapper.get().toNative(block.getOrdinal());
        if (nativeId < 0) {
            return false;
        }
        int flags = 2 | (sideEffects.shouldApply(SideEffect.NEIGHBORS) ? 1 : 0);
        return onMainThread(() -> getWorld().setBlock(x, y, z, Block.getBlockById(nativeId >> 4), nativeId & 15, flags));
    }

    @Override
    public Set<SideEffect> applySideEffects(BlockVector3 position, BlockState previousType, SideEffectSet sideEffectSet) {
        return Collections.emptySet();
    }

    @Override
    public boolean clearContainerBlockContents(BlockVector3 position) {
        return onMainThread(() -> {
            TileEntity tile = getWorld().getTileEntity(position.x(), position.y(), position.z());
            if (!(tile instanceof IInventory inventory)) {
                return false;
            }
            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                inventory.setInventorySlotContents(i, null);
            }
            return true;
        });
    }

    @Override
    public void dropItem(Vector3 position, BaseItemStack item) {
        net.minecraft.item.ItemStack stack = Forge1710Adapter.toNative(item);
        if (stack == null) {
            return;
        }
        onMainThread(() -> {
            WorldServer world = getWorld();
            EntityItem entity = new EntityItem(world, position.x(), position.y(), position.z(), stack);
            entity.delayBeforeCanPickup = 10;
            return world.spawnEntityInWorld(entity);
        });
    }

    @Override
    public void simulateBlockMine(BlockVector3 position) {
        onMainThread(() -> {
            WorldServer world = getWorld();
            int x = position.x();
            int y = position.y();
            int z = position.z();
            Block block = world.getBlock(x, y, z);
            int meta = world.getBlockMetadata(x, y, z);
            block.dropBlockAsItem(world, x, y, z, meta, 0);
            return world.setBlockToAir(x, y, z);
        });
    }

    @Override
    public boolean generateTree(TreeType type, EditSession editSession, BlockVector3 position) {
        return false;
    }

    @Override
    public BlockVector3 getSpawnPosition() {
        ChunkCoordinates spawn = getWorld().getSpawnPoint();
        return BlockVector3.at(spawn.posX, spawn.posY, spawn.posZ);
    }

    @Override
    public void refreshChunk(int chunkX, int chunkZ) {
        TaskManager.taskManager().task(() -> resendChunk(getWorld(), chunkX, chunkZ));
    }

    /**
     * Must be called on the server thread.
     */
    public static void resendChunk(WorldServer world, int chunkX, int chunkZ) {
        if (!world.theChunkProviderServer.chunkExists(chunkX, chunkZ)) {
            return;
        }
        Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
        S21PacketChunkData packet = null;
        for (Object o : world.playerEntities) {
            EntityPlayerMP player = (EntityPlayerMP) o;
            if (world.getPlayerManager().isPlayerWatchingChunk(player, chunkX, chunkZ)) {
                if (packet == null) {
                    // Non-full update of every allocated section; a full update would also resend biomes.
                    packet = new S21PacketChunkData(chunk, false, 0xFFFF);
                }
                player.playerNetServerHandler.sendPacket(packet);
            }
        }
    }

    @Override
    public IChunkGet get(int chunkX, int chunkZ) {
        return new Forge1710GetBlocks(this, chunkX, chunkZ);
    }

    @Override
    public void sendFakeChunk(@Nullable Player player, ChunkPacket packet) {
    }

    @Override
    public void flush() {
    }

    @Override
    public boolean tile(int x, int y, int z, FaweCompoundTag tile) {
        return false;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Forge1710World that && that.worldRef.get() == worldRef.get();
    }

    @Override
    public int hashCode() {
        WorldServer world = worldRef.get();
        return world == null ? 0 : System.identityHashCode(world);
    }

    @Override
    public String toString() {
        return "Forge1710World{" + name + "}";
    }

}
