package com.boydti.fawe.object.extent;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.NbtValued;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

public class StripNBTExtent extends AbstractDelegateExtent {
    private final String[] strip;

    /**
     * Create a new instance.
     *
     * @param extent the extent
     */
    public StripNBTExtent(Extent extent, Set<String> strip) {
        super(extent);
        this.strip = strip.toArray(new String[0]);
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 location, B block) throws WorldEditException {
        return super.setBlock(location, stripBlockNBT(block));
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(int x, int y, int z, B block) throws WorldEditException {
        return super.setBlock(x, y, z, stripBlockNBT(block));
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity) {
        return super.createEntity(location, stripEntityNBT(entity));
    }

    public <B extends BlockStateHolder<B>> B stripBlockNBT(B block) {
        if(!(block instanceof BaseBlock)) return block;
        BaseBlock localBlock = (BaseBlock)block;
        if (!localBlock.hasNbtData()) return block;
        CompoundTag nbt = localBlock.getNbtData();
        Map<String, Tag> value = nbt.getValue();
        for (String key : strip) {
            value.remove(key);
        }
        return (B) localBlock;
    }

    public <T extends NbtValued> T stripEntityNBT(T entity) {
        if (!entity.hasNbtData()) return entity;
        CompoundTag nbt = entity.getNbtData();
        Map<String, Tag> value = nbt.getValue();
        for (String key : strip) {
            value.remove(key);
        }
        return entity;
    }
}
