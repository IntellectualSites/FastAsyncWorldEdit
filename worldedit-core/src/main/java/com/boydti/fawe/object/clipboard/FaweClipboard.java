package com.boydti.fawe.object.clipboard;

import com.boydti.fawe.jnbt.NBTStreamer;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;


import static com.google.common.base.Preconditions.checkNotNull;

public abstract class FaweClipboard {
    public abstract BaseBlock getBlock(int x, int y, int z);

    public abstract <B extends BlockStateHolder<B>> boolean setBlock(int index, B block);

    public abstract <B extends BlockStateHolder<B>> boolean setBlock(int x, int y, int z, B block);

    public abstract boolean hasBiomes();

    public abstract boolean setBiome(int x, int z, int biome);

    public abstract BaseBiome getBiome(int x, int z);

    public abstract BaseBiome getBiome(int index);

    public abstract BaseBlock getBlock(int index);

    public abstract void setBiome(int index, int biome);

    public abstract boolean setTile(int x, int y, int z, CompoundTag tag);

    public abstract Entity createEntity(Extent world, double x, double y, double z, float yaw, float pitch, BaseEntity entity);

    public abstract List<? extends Entity> getEntities();

    public abstract boolean remove(ClipboardEntity clipboardEntity);

    public void setOrigin(BlockVector3 offset) {
    } // Do nothing

    public abstract void setDimensions(BlockVector3 dimensions);

    public abstract BlockVector3 getDimensions();

    /**
     * The locations provided are relative to the clipboard min
     *
     * @param task
     * @param air
     */
    public abstract void forEach(BlockReader task, boolean air);

    public static abstract class BlockReader {
        public abstract <B extends BlockStateHolder<B>> void run(int x, int y, int z, B block);
    }

    public abstract void streamBiomes(final NBTStreamer.ByteReader task);

    public void streamCombinedIds(final NBTStreamer.ByteReader task) {
        forEach(new BlockReader() {
            private int index = 0;

            @Override
            public <B extends BlockStateHolder<B>> void run(int x, int y, int z, B block) {
                task.run(index++, block.getInternalId());
            }
        }, true);
    }

    public List<CompoundTag> getTileEntities() {
        final List<CompoundTag> tiles = new ArrayList<>();
        forEach(new BlockReader() {
            private int index = 0;

            @Override
            public <B extends BlockStateHolder<B>> void run(int x, int y, int z, B block) {
            	if(!(block instanceof BaseBlock)) return;
            	BaseBlock base = (BaseBlock)block;
                CompoundTag tag = base.getNbtData();
                if (tag != null) {
                    Map<String, Tag> values = ReflectionUtils.getMap(tag.getValue());
                    values.put("x", new IntTag(x));
                    values.put("y", new IntTag(y));
                    values.put("z", new IntTag(z));
                    tiles.add(tag);
                }
            }
        }, false);
        return tiles;
    }

    public void close() {}

    public void flush() {}

    /**
     * Stores entity data.
     */
    public class ClipboardEntity implements Entity {
        private final BaseEntity entity;
        private final Extent world;
        private final double x, y, z;
        private final float yaw, pitch;

        public ClipboardEntity(Extent world, double x, double y, double z, float yaw, float pitch, BaseEntity entity) {
            checkNotNull(entity);
            checkNotNull(world);
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.entity = new BaseEntity(entity);
        }

        @Override
        public boolean remove() {
            return FaweClipboard.this.remove(this);
        }

        @Nullable
        @Override
        public <T> T getFacet(Class<? extends T> cls) {
            return null;
        }

        /**
         * Get the entity state. This is not a copy.
         *
         * @return the entity
         */
        BaseEntity getEntity() {
            return entity;
        }

        @Override
        public BaseEntity getState() {
            return new BaseEntity(entity);
        }

        @Override
        public Location getLocation() {
            return new Location(world, x, y, z, yaw, pitch);
        }

        @Override
        public Extent getExtent() {
            return world;
        }

		@Override
		public boolean setLocation(Location location) {
			//Should not be teleporting this entity
			return false;
		}
    }
}
