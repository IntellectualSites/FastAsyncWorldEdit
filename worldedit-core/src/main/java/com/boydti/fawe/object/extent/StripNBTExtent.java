package com.boydti.fawe.object.extent;

import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.NbtValued;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class StripNBTExtent extends AbstractDelegateExtent {
    private final String[] strip;

    /**
     * Create a new instance.
     *
     * @param extent the extent
     */
    public StripNBTExtent(Extent extent, Set<String> strip) {
        super(extent);
        this.strip = strip.toArray(new String[strip.size()]);
    }

    @Override
    public boolean setBlock(Vector location, BlockStateHolder block) throws WorldEditException {
        return super.setBlock(location, stripNBT(block));
    }

    @Override
    public boolean setBlock(int x, int y, int z, BlockStateHolder block) throws WorldEditException {
        return super.setBlock(x, y, z, stripNBT(block));
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity) {
        return super.createEntity(location, stripNBT(entity));
    }

    public <T extends NbtValued> T stripNBT(T block) {
        if (!block.hasNbtData()) return block;
        CompoundTag nbt = block.getNbtData();
        Map<String, Tag> value = nbt.getValue();
        for (String key : strip) {
            value.remove(key);
        }
        return block;
    }
}
