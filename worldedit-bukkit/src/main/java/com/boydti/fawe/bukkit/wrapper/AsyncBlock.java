package com.boydti.fawe.bukkit.wrapper;

import com.boydti.fawe.bukkit.wrapper.state.AsyncSign;
import com.boydti.fawe.util.TaskManager;
import com.destroystokyo.paper.block.BlockSoundGroup;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockID;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class AsyncBlock implements Block {

    public int z;
    public int y;
    public int x;
    public final AsyncWorld world;

    public AsyncBlock(AsyncWorld world, int x, int y, int z) {
        this.world = world;
        this.x = x;
        this.y = Math.max(0, Math.min(255, y));
        this.z = z;
    }

    public void setPosition(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    @Deprecated
    public byte getData() {
        return (byte) getPropertyId();
    }

    public int getPropertyId() {
        return world.getBlock(x, y, z).getInternalId() >> BlockTypesCache.BIT_OFFSET;
    }

    public int getCombinedId() {
        return world.getBlock(x, y, z).getInternalId();
    }

    public int getTypeId() {
        return world.getBlock(x, y, z).getBlockType().getInternalId();
    }

    @Nonnull
    @Override
    public AsyncBlock getRelative(int modX, int modY, int modZ) {
        return new AsyncBlock(world, x + modX, y + modY, z + modZ);
    }

    @Nonnull
    @Override
    public AsyncBlock getRelative(BlockFace face) {
        return this.getRelative(face.getModX(), face.getModY(), face.getModZ());
    }

    @Nonnull
    @Override
    public AsyncBlock getRelative(BlockFace face, int distance) {
        return this.getRelative(face.getModX() * distance, face.getModY() * distance,
            face.getModZ() * distance);
    }

    @Nonnull
    @Override
    public Material getType() {
        return getBlockData().getMaterial();
    }

    @Nonnull
    @Override
    public BlockData getBlockData() {
        return BukkitAdapter.adapt(world.getBlock(x, y, z));
    }

    @Deprecated
    public boolean setTypeIdAndPropertyId(int id, int propertyId, boolean physics) {
        return setTypeIdAndPropertyId(id, propertyId);
    }

    @Deprecated
    public boolean setTypeIdAndPropertyId(int id, int propertyId) {
        return setCombinedId(id + (propertyId << BlockTypesCache.BIT_OFFSET));
    }

    @Deprecated
    public boolean setCombinedId(int combinedId) {
        return world.setBlock(x, y, z, BlockState.getFromInternalId(combinedId));
    }

    @Deprecated
    public boolean setTypeId(int typeId) {
        return world.setBlock(x, y, z, BlockTypes.get(typeId).getDefaultState());
    }

    @Deprecated
    public boolean setPropertyId(int propertyId) {
        return setTypeIdAndPropertyId(getTypeId(), propertyId);
    }

    @Override
    public byte getLightLevel() {
        return (byte) 15;
    }

    @Override
    public byte getLightFromSky() {
        return (byte) 15;
    }

    @Override
    public byte getLightFromBlocks() {
        return (byte) 15;
    }

    @Nonnull
    @Override
    public AsyncWorld getWorld() {
        return world;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public int getZ() {
        return z;
    }

    @Override
    public boolean isValidTool(@Nonnull ItemStack itemStack) {
        return getDrops(itemStack).size() !=0;
    }

    @Nonnull
    @Override
    public Location getLocation() {
        return new Location(world, x, y, z);
    }

    @Override
    public Location getLocation(Location loc) {
        if (loc != null) {
            loc.setWorld(this.getWorld());
            loc.setX(this.x);
            loc.setY(this.y);
            loc.setZ(this.z);
        }
        return loc;
    }

    @Nonnull
    @Override
    public AsyncChunk getChunk() {
        return world.getChunkAt(x >> 4, z >> 4);
    }

    @Override
    public void setBlockData(@Nonnull BlockData blockData) {
        try {
            world.setBlock(x, y, z, BukkitAdapter.adapt(blockData));
        } catch (WorldEditException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setBlockData(@Nonnull BlockData blockData, boolean b) {
        setBlockData(blockData);
    }

    @Override
    public void setType(@Nonnull Material type) {
        try {
            world.setBlock(x, y, z, BukkitAdapter.asBlockType(type).getDefaultState());
        } catch (WorldEditException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setType(@Nonnull Material type, boolean applyPhysics) {
        setType(type);
    }

    @Override
    public BlockFace getFace(@Nonnull Block block) {
        BlockFace[] directions = BlockFace.values();
        for (BlockFace face : directions) {
            if (this.getX() + face.getModX() == block.getX()
                && this.getY() + face.getModY() == block.getY()
                && this.getZ() + face.getModZ() == block.getZ()) {
                return face;
            }
        }
        return null;
    }

    @Nonnull
    @Override
    public AsyncBlockState getState() {
        BaseBlock state = world.getFullBlock(x, y, z);
        switch (state.getBlockType().getInternalId()) {
            case BlockID.ACACIA_SIGN:
            case BlockID.SPRUCE_SIGN:
            case BlockID.ACACIA_WALL_SIGN:
            case BlockID.BIRCH_SIGN:
            case BlockID.SPRUCE_WALL_SIGN:
            case BlockID.BIRCH_WALL_SIGN:
            case BlockID.DARK_OAK_SIGN:
            case BlockID.DARK_OAK_WALL_SIGN:
            case BlockID.JUNGLE_SIGN:
            case BlockID.JUNGLE_WALL_SIGN:
            case BlockID.OAK_SIGN:
            case BlockID.OAK_WALL_SIGN:
                return new AsyncSign(this, state);
            default:
                return new AsyncBlockState(this, state);
        }
    }

    @Override
    @Nonnull
    public AsyncBlockState getState(boolean useSnapshot) {
        return getState();
    }

    @Nonnull
    @Override
    public Biome getBiome() {
        return world.getAdapter().adapt(world.getBiomeType(x, y, z));
    }

    @Override
    public void setBiome(@Nonnull Biome bio) {
        BiomeType biome = world.getAdapter().adapt(bio);
        world.setBiome(x, 0, z, biome);
    }

    @Override
    public boolean isBlockPowered() {
        return false;
    }

    @Override
    public boolean isBlockIndirectlyPowered() {
        return false;
    }

    @Override
    public boolean isBlockFacePowered(@Nonnull BlockFace face) {
        return false;
    }

    @Override
    public boolean isBlockFaceIndirectlyPowered(@Nonnull BlockFace face) {
        return false;
    }

    @Override
    public int getBlockPower(@Nonnull BlockFace face) {
        return 0;
    }

    @Override
    public int getBlockPower() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        switch (getType()) {
            case AIR:
            case CAVE_AIR:
            case VOID_AIR:
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean isLiquid() {
        return world.getBlock(x, y, z).getMaterial().isLiquid();
    }

    @Override
    public boolean isBuildable() {
        return this.getUnsafeBlock().isBuildable();
    }

    @Override
    public boolean isBurnable() {
        return this.getType().isBurnable();
    }

    @Override
    public boolean isReplaceable() {
        return this.getUnsafeBlock().isReplaceable();
    }

    @Override
    public double getTemperature() {
        return this.getWorld().getTemperature(this.getX(), this.getZ());
    }

    @Override
    public double getHumidity() {
        return this.getWorld().getHumidity(this.getX(), this.getZ());
    }

    @Nonnull
    @Override
    public PistonMoveReaction getPistonMoveReaction() {
        return PistonMoveReaction.IGNORE;
    }

    @Deprecated
    private Block getUnsafeBlock() {
        return world.getBukkitWorld().getBlockAt(x, y, z);
    }

    @Override
    public boolean breakNaturally() {
        return TaskManager.IMP.sync(() -> getUnsafeBlock().breakNaturally());
    }

    @Override
    public boolean breakNaturally(@Nonnull ItemStack tool) {
        return TaskManager.IMP.sync(() -> getUnsafeBlock().breakNaturally(tool));
    }

    public boolean breakNaturally(@Nonnull ItemStack tool, boolean value) {
        return TaskManager.IMP.sync(() -> getUnsafeBlock().breakNaturally(tool));
    }

    @Nonnull
    @Override
    public Collection<ItemStack> getDrops() {
        return TaskManager.IMP.sync(() -> getUnsafeBlock().getDrops());
    }

    @Nonnull
    @Override
    public Collection<ItemStack> getDrops(@Nonnull ItemStack tool) {
        return TaskManager.IMP.sync(() -> getUnsafeBlock().getDrops(tool));
    }

    public Collection<ItemStack> getDrops(ItemStack tool, Entity entity) {
        return Collections.emptyList(); //todo
    }

    @Override
    public void setMetadata(@Nonnull String metadataKey, @Nonnull MetadataValue newMetadataValue) {
        this.getUnsafeBlock().setMetadata(metadataKey, newMetadataValue);
    }

    @Nonnull
    @Override
    public List<MetadataValue> getMetadata(@Nonnull String metadataKey) {
        return this.getUnsafeBlock().getMetadata(metadataKey);
    }

    @Override
    public boolean hasMetadata(@Nonnull String metadataKey) {
        return this.getUnsafeBlock().hasMetadata(metadataKey);
    }

    @Override
    public void removeMetadata(@Nonnull String metadataKey, @Nonnull Plugin owningPlugin) {
        this.getUnsafeBlock().removeMetadata(metadataKey, owningPlugin);
    }

    @Override
    public boolean isPassable() {
        return this.getUnsafeBlock().isPassable();
    }

    @Override
    public RayTraceResult rayTrace(@Nonnull Location arg0, @Nonnull Vector arg1, double arg2,
        @Nonnull FluidCollisionMode arg3) {
        return this.getUnsafeBlock().rayTrace(arg0, arg1, arg2, arg3);
    }

    public boolean applyBoneMeal(@Nonnull BlockFace face) {
        throw new UnsupportedOperationException("FAWE does not support this yet");
    }

    public String getTranslationKey() {
        throw new UnsupportedOperationException("FAWE does not support this yet");
    }

    @Nonnull
    @Override
    public float getDestroySpeed(@Nonnull ItemStack itemStack) {
        throw new UnsupportedOperationException("FAWE does not support this yet");
    }

    @Nonnull
    @Override
    public float getDestroySpeed(@Nonnull ItemStack itemStack, boolean considerEnchants) {
        throw new UnsupportedOperationException("FAWE does not support this yet");
    }

    @Nonnull
    @Override
    public BoundingBox getBoundingBox() {
        return this.getUnsafeBlock().getBoundingBox();
    }

    @Override
    @Nonnull
    public BlockSoundGroup getSoundGroup() {
        return TaskManager.IMP.sync(() -> getUnsafeBlock().getSoundGroup());
    }
    
    @Override
    @Nonnull
    public boolean isSolid() {
        return this.getType().isSolid();
    }

}
