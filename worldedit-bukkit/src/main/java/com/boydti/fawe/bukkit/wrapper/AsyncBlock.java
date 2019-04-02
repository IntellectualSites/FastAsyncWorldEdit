package com.boydti.fawe.bukkit.wrapper;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.bukkit.wrapper.state.AsyncSign;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.WorldEditException;
import java.util.Collection;
import java.util.List;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class AsyncBlock implements Block {

    public int z;
    public int y;
    public int x;
    public final FaweQueue queue;
    public final AsyncWorld world;

    public AsyncBlock(AsyncWorld world, FaweQueue queue, int x, int y, int z) {
        this.world = world;
        this.queue = queue;
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
        return (byte) (queue.getCachedCombinedId4Data(x, y, z, BlockTypes.AIR.getInternalId()) & 0xF);
    }

    public int getPropertyId() {
        return (queue.getCachedCombinedId4Data(x, y, z, BlockTypes.AIR.getInternalId()) >> BlockTypes.BIT_OFFSET);
    }

    public int getCombinedId() {
        return queue.getCachedCombinedId4Data(x, y, z, BlockTypes.AIR.getInternalId());
    }

    public int getTypeId() {
        return (queue.getCachedCombinedId4Data(x, y, z, BlockTypes.AIR.getInternalId()) & BlockTypes.BIT_MASK);
    }

    @Override
    public AsyncBlock getRelative(int modX, int modY, int modZ) {
        return new AsyncBlock(world, queue, x + modX, y + modY, z + modZ);
    }

    @Override
    public AsyncBlock getRelative(BlockFace face) {
        return this.getRelative(face.getModX(), face.getModY(), face.getModZ());
    }

    @Override
    public AsyncBlock getRelative(BlockFace face, int distance) {
        return this.getRelative(face.getModX() * distance, face.getModY() * distance, face.getModZ() * distance);
    }

    @Override
    public Material getType() {
        return getBlockData().getMaterial();
    }

    @Override
    public BlockData getBlockData() {
        return BukkitAdapter.getBlockData(queue.getCachedCombinedId4Data(x, y, z, BlockTypes.AIR.getInternalId()));
    }

    @Deprecated
    public boolean setTypeIdAndPropertyId(int id, int propertyId, boolean physics) {
        return setTypeIdAndPropertyId(id, propertyId);
    }

    @Deprecated
    public boolean setCombinedId(int combinedId) {
        return queue.setBlock(x, y, z, combinedId);
    }

    @Deprecated
    public boolean setTypeIdAndPropertyId(int id, int propertyId) {
        return setCombinedId(id + (propertyId << BlockTypes.BIT_OFFSET));
    }

    @Deprecated
    public boolean setTypeId(int typeId) {
        return queue.setBlock(x, y, z, BlockTypes.get(typeId).getDefaultState());
    }

    @Deprecated
    public boolean setPropertyId(int propertyId) {
        return setTypeIdAndPropertyId(getTypeId(), propertyId);
    }

    @Override
    public byte getLightLevel() {
        return (byte) queue.getLight(x, y, z);
    }

    @Override
    public byte getLightFromSky() {
        return (byte) queue.getSkyLight(x, y, z);
    }

    @Override
    public byte getLightFromBlocks() {
        return (byte) queue.getEmmittedLight(x, y, z);
    }

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
    public Location getLocation() {
        return new Location(world, x, y, z);
    }

    @Override
    public Location getLocation(Location loc) {
        if(loc != null) {
            loc.setWorld(this.getWorld());
            loc.setX((double)this.x);
            loc.setY((double)this.y);
            loc.setZ((double)this.z);
        }
        return loc;
    }

    @Override
    public AsyncChunk getChunk() {
        return world.getChunkAt(x >> 4, z >> 4);
    }

    @Override
    public void setBlockData(BlockData blockData) {
        try {
            queue.setBlock(x, y, z, BukkitAdapter.adapt(blockData));
        } catch (WorldEditException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setBlockData(BlockData blockData, boolean b) {
        setBlockData(blockData);
    }

    @Override
    public void setType(Material type) {
        try {
            queue.setBlock(x, y, z, BukkitAdapter.adapt(type).getDefaultState());
        } catch (WorldEditException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setType(Material type, boolean applyPhysics) {
        setType(type);
    }

    @Override
    public BlockFace getFace(Block block) {
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

    @Override
    public AsyncBlockState getState() {
        int combined = queue.getCombinedId4Data(x, y, z, 0);
        BlockType type = BlockTypes.getFromStateId(combined);
        String s = type.getResource().toUpperCase();
        if (type == BlockTypes.SIGN || type == BlockTypes.WALL_SIGN) {
            return new AsyncSign(this, combined);
        }
        return new AsyncBlockState(this, combined);
    }

    @Override
    public AsyncBlockState getState(boolean useSnapshot) {
        return getState();
    }

    @Override
    public Biome getBiome() {
        return world.getAdapter().getBiome(queue.getBiomeId(x, z));
    }

    @Override
    public void setBiome(Biome bio) {
        int id = world.getAdapter().getBiomeId(bio);
        queue.setBiome(x, z, FaweCache.getBiome(id));
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
    public boolean isBlockFacePowered(BlockFace face) {
        return false;
    }

    @Override
    public boolean isBlockFaceIndirectlyPowered(BlockFace face) {
        return false;
    }

    @Override
    public int getBlockPower(BlockFace face) {
        return 0;
    }

    @Override
    public int getBlockPower() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return getType().isEmpty();
    }

    @Override
    public boolean isLiquid() {
        int combined = queue.getCombinedId4Data(x, y, z, 0);
        BlockType type = BlockTypes.getFromStateId(combined);
        return type.getMaterial().isLiquid();
    }

    @Override
    public double getTemperature() {
        return this.getWorld().getTemperature(this.getX(), this.getZ());
    }

    @Override
    public double getHumidity() {
        return this.getWorld().getHumidity(this.getX(), this.getZ());
    }

    @Override
    public PistonMoveReaction getPistonMoveReaction() {
        return null;
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
    public boolean breakNaturally(ItemStack tool) {
        return TaskManager.IMP.sync(() -> getUnsafeBlock().breakNaturally(tool));
    }

    @Override
    public Collection<ItemStack> getDrops() {
        return TaskManager.IMP.sync(() -> getUnsafeBlock().getDrops());
    }

    @Override
    public Collection<ItemStack> getDrops(ItemStack tool) {
        return TaskManager.IMP.sync(() -> getUnsafeBlock().getDrops(tool));
    }

    @Override
    public void setMetadata(String metadataKey, MetadataValue newMetadataValue) {
        this.getUnsafeBlock().setMetadata(metadataKey, newMetadataValue);
    }

    @Override
    public List<MetadataValue> getMetadata(String metadataKey) {
        return this.getUnsafeBlock().getMetadata(metadataKey);
    }

    @Override
    public boolean hasMetadata(String metadataKey) {
        return this.getUnsafeBlock().hasMetadata(metadataKey);
    }

    @Override
    public void removeMetadata(String metadataKey, Plugin owningPlugin) {
        this.getUnsafeBlock().removeMetadata(metadataKey, owningPlugin);
    }

	@Override
	public boolean isPassable() {
		return this.getUnsafeBlock().isPassable();
	}

	@Override
	public RayTraceResult rayTrace(Location arg0, Vector arg1, double arg2, FluidCollisionMode arg3) {
		return this.getUnsafeBlock().rayTrace(arg0, arg1, arg2, arg3);
	}

	@Override
	public BoundingBox getBoundingBox() {
		return this.getUnsafeBlock().getBoundingBox();
	}
}
