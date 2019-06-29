package com.boydti.fawe.object;

import com.boydti.fawe.object.changeset.FaweChangeSet;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.history.changeset.ChangeSet;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Stores changes to a {@link ChangeSet}.
 */
public class HistoryExtent extends AbstractDelegateExtent {

    private FaweChangeSet changeSet;
    private final FaweQueue queue;
    private final EditSession session;

    /**
     * Create a new instance.
     *
     * @param extent    the extent
     * @param changeSet the change set
     */
    public HistoryExtent(final EditSession session, final Extent extent, final FaweChangeSet changeSet, FaweQueue queue) {
        super(extent);
        checkNotNull(changeSet);
        this.queue = queue;
        this.changeSet = changeSet;
        this.session = session;
    }

    public FaweChangeSet getChangeSet() {
        return changeSet;
    }

    public void setChangeSet(FaweChangeSet fcs) {
        this.changeSet = fcs;
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(int x, int y, int z, B block) throws WorldEditException {
        BaseBlock previous = queue.getFullBlock(mutable.setComponents(x, y, z)).toBaseBlock();
        if (previous.getInternalId() == block.getInternalId()) {
            if (!previous.hasNbtData() && (block instanceof BaseBlock && !((BaseBlock)block).hasNbtData())) {
                return false;
            }
        }
        this.changeSet.add(x, y, z, previous, block.toBaseBlock());
        return getExtent().setBlock(x, y, z, block);
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(final BlockVector3 location, final B block) throws WorldEditException {
        return setBlock(location.getBlockX(), location.getBlockY(), location.getBlockZ(), block);
    }

    @Nullable
    @Override
    public Entity createEntity(final Location location, final BaseEntity state) {
        final Entity entity = super.createEntity(location, state);
        if ((state != null)) {
            this.changeSet.addEntityCreate(state.getNbtData());
        }
        return entity;
    }

    @Override
    public List<? extends Entity> getEntities() {
        return this.wrapEntities(super.getEntities());
    }

    @Override
    public List<? extends Entity> getEntities(final Region region) {
        return this.wrapEntities(super.getEntities(region));
    }

    private List<? extends Entity> wrapEntities(final List<? extends Entity> entities) {
        final List<Entity> newList = new ArrayList<>(entities.size());
        for (final Entity entity : entities) {
            newList.add(new TrackedEntity(entity));
        }
        return newList;
    }

    @Override
    public boolean setBiome(BlockVector2 position, BiomeType newBiome) {
        BiomeType oldBiome = this.getBiome(position);
        if (oldBiome.getId() != newBiome.getId()) {
            this.changeSet.addBiomeChange(position.getBlockX(), position.getBlockZ(), oldBiome, newBiome);
            return getExtent().setBiome(position, newBiome);
        } else {
            return false;
        }
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType newBiome) {
        BiomeType oldBiome = this.getBiome(BlockVector2.at(x, z));
        if (oldBiome.getId() != newBiome.getId()) {
            this.changeSet.addBiomeChange(x, z, oldBiome, newBiome);
            return getExtent().setBiome(x, y, z, newBiome);
        } else {
            return false;
        }
    }

    public class TrackedEntity implements Entity {
        private final Entity entity;

        private TrackedEntity(final Entity entity) {
            this.entity = entity;
        }

        @Override
        public BaseEntity getState() {
            return this.entity.getState();
        }

        @Override
        public Location getLocation() {
            return this.entity.getLocation();
        }

        @Override
        public Extent getExtent() {
            return this.entity.getExtent();
        }

        @Override
        public boolean remove() {
            final Location location = this.entity.getLocation();
            final BaseEntity state = this.entity.getState();
            final boolean success = this.entity.remove();
            if ((state != null) && success) {
                HistoryExtent.this.changeSet.addEntityRemove(state.getNbtData());
            }
            return success;
        }

        @Nullable
        @Override
        public <T> T getFacet(final Class<? extends T> cls) {
            return this.entity.getFacet(cls);
        }

		@Override
		public boolean setLocation(Location location) {
			return this.entity.setLocation(location);
		}
    }
}
