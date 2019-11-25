package com.boydti.fawe.jnbt.anvil;

import com.boydti.fawe.object.extent.FastWorldEditExtent;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.SimpleWorld;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public class MCAWorld implements SimpleWorld {

    private final String name;
    private final MCAQueue queue;
    private final FastWorldEditExtent extent;

    public MCAWorld(String name, File saveFolder, boolean hasSky) {
        this.name = name;
        this.queue = new MCAQueue(name, saveFolder, hasSky);
        this.extent = new FastWorldEditExtent(this, queue);
    }

    public MCAQueue getQueue() {
        return queue;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean setBlock(BlockVector3 position, BlockStateHolder block) throws WorldEditException {
        return extent.setBlock(position, block);
    }

    @Override
    public int getBlockLightLevel(BlockVector3 position) {
        return queue.getEmmittedLight(position.getBlockX(), position.getBlockY(), position.getBlockZ());
    }

    @Override
    public boolean clearContainerBlockContents(BlockVector3 position) {
        BaseBlock block = extent.getFullBlock(position);
        if (block.hasNbtData()) {
            Map<String, Tag> nbt = ReflectionUtils.getMap(block.getNbtData().getValue());
            if (nbt.containsKey("Items")) {
                nbt.put("Items", new ListTag(CompoundTag.class, new ArrayList<CompoundTag>()));
                try {
                    extent.setBlock(position, block);
                } catch (WorldEditException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    @Override
    public void dropItem(Vector3 position, BaseItemStack item) {

    }

    @Override
    public boolean regenerate(Region region, EditSession editSession) {
        return false;
    }

    @Override
    public List<? extends Entity> getEntities(Region region) {
        return new ArrayList<>();
    }

    @Override
    public List<? extends Entity> getEntities() {
        return new ArrayList<>();
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity) {
        return extent.createEntity(location, entity);
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        return extent.getBlock(position);
    }

    @Override
    public BiomeType getBiome(BlockVector2 position) {
        return extent.getBiome(position);
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T block)
        throws WorldEditException {
        return extent.setBlock(x, y, z, block);
    }

    @Override
    public void setTile(int x, int y, int z, CompoundTag tile) throws WorldEditException {
        extent.setTile(x, y, z, tile);
    }

    @Override
    public boolean setBiome(BlockVector2 position, BiomeType biome) {
        return extent.setBiome(position, biome);
    }

	@Override
	public boolean playEffect(Vector3 position, int type, int data) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean notifyAndLightBlock(BlockVector3 position, BlockState previousType) throws WorldEditException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public BlockVector3 getSpawnPosition() {
		return queue.getWEWorld().getSpawnPosition();
	}
}
