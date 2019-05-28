package com.boydti.fawe.object.extent;

import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.HasFaweQueue;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.*;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.*;

public class FastWorldEditExtent extends AbstractDelegateExtent implements HasFaweQueue {

    private final World world;
    private FaweQueue queue;
    private final int maxY;

    public FastWorldEditExtent(final World world, FaweQueue queue) {
        super(queue);
        this.world = world;
        this.queue = queue;
        this.maxY = world.getMaxY();
    }

    public FaweQueue getQueue() {
        return queue;
    }

    @Override
    public int getMaxY() {
        return queue.getMaxY();
    }

    @Override
    public int getLight(int x, int y, int z) {
        return queue.getLight(x, y, z);
    }

    @Override
    public int getBlockLight(int x, int y, int z) {
        return queue.getEmmittedLight(x, y, z);
    }

    @Override
    public int getSkyLight(int x, int y, int z) {
        return queue.getSkyLight(x, y, z);
    }

    @Override
    public int getBrightness(int x, int y, int z) {
        return queue.getBrightness(x, y, z);
    }

    @Override
    public int getOpacity(int x, int y, int z) {
        return queue.getOpacity(x, y, z);
    }

    @Override
    public Entity createEntity(final Location loc, final BaseEntity entity) {
        if (entity != null) {
            CompoundTag tag = entity.getNbtData();
            if (tag == null) {
                HashMap<String, Tag> map = new HashMap<>();
                tag = new CompoundTag(map);
            }
            Map<String, Tag> map = ReflectionUtils.getMap(tag.getValue());
            map.put("Id", new StringTag(entity.getType().getId()));
            ListTag pos = (ListTag) map.get("Pos");
            if (pos == null) {
                map.put("Pos", new ListTag(DoubleTag.class, Arrays.asList(new DoubleTag(loc.getX()), new DoubleTag(loc.getY()), new DoubleTag(loc.getZ()))));
            } else {
                List<Tag> posList = ReflectionUtils.getList(pos.getValue());
                posList.set(0, new DoubleTag(loc.getX()));
                posList.set(1, new DoubleTag(loc.getY()));
                posList.set(2, new DoubleTag(loc.getZ()));
            }
            ListTag rot = (ListTag) map.get("Rotation");
            if (rot == null) {
                map.put("Rotation", new ListTag(FloatTag.class, Arrays.asList(new FloatTag(loc.getYaw()), new DoubleTag(loc.getPitch()))));
            }
            queue.setEntity(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), tag);
        }
        return null;
    }

    @Override
    public String toString() {
        return super.toString() + ":" + queue + "(" + getExtent() + ")";
    }

    @Override
    public BiomeType getBiome(final BlockVector2 position) {
        return queue.getBiomeType(position.getBlockX(), position.getBlockZ());
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(final BlockVector3 location, final B block) throws WorldEditException {
        return setBlock(location.getBlockX(), location.getBlockY(), location.getBlockZ(), block);
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(int x, int y, int z, final B block) throws WorldEditException {
        return queue.setBlock(x, y, z, block);
    }

    @Override
    public BlockState getBlock(BlockVector3 location) {
        return getBlock(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        int combinedId4Data = queue.getCombinedId4Data(x, y, z, 0);
        BlockType type = BlockTypes.getFromStateId(combinedId4Data);
        BlockState state = type.withStateId(combinedId4Data);
        return state;
    }
    
    @Override
    public BaseBlock getFullBlock(BlockVector3 pos) {
        int combinedId4Data = queue.getCombinedId4Data(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ(), 0);
        BlockType type = BlockTypes.getFromStateId(combinedId4Data);
        BaseBlock base = type.withStateId(combinedId4Data).toBaseBlock();
        if (type.getMaterial().hasContainer()) {
            CompoundTag tile = queue.getTileEntity(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ());
            if (tile != null) {
                return base.toBaseBlock(tile);
            }
        }
        return base;
    }

    @Override
    public List<? extends Entity> getEntities() {
        return world.getEntities();
    }

    @Override
    public List<? extends Entity> getEntities(final Region region) {
        return world.getEntities(region);
    }

    @Override
    public boolean setBiome(final BlockVector2 position, final BiomeType biome) {
        queue.setBiome(position.getBlockX(), position.getBlockZ(), biome);
        return true;
    }
}
