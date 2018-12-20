package com.boydti.fawe.jnbt.anvil;

import com.boydti.fawe.object.extent.FastWorldEditExtent;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.SimpleWorld;
import com.sk89q.worldedit.world.biome.BaseBiome;
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
    public boolean setBlock(Vector position, BlockStateHolder block) throws WorldEditException {
        return extent.setBlock(position, block);
    }

    @Override
    public int getBlockLightLevel(Vector position) {
        return queue.getEmmittedLight(position.getBlockX(), position.getBlockY(), position.getBlockZ());
    }

    @Override
    public boolean clearContainerBlockContents(Vector position) {
        BlockStateHolder block = extent.getLazyBlock(position);
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
    public void dropItem(Vector position, BaseItemStack item) {

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
    public BlockState getBlock(Vector position) {
        return extent.getLazyBlock(position);
    }

    @Override
    public BaseBiome getBiome(Vector2D position) {
        return extent.getBiome(position);
    }

    @Override
    public boolean setBiome(Vector2D position, BaseBiome biome) {
        return extent.setBiome(position, biome);
    }
}
