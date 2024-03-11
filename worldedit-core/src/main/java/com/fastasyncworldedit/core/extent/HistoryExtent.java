package com.fastasyncworldedit.core.extent;

import com.fastasyncworldedit.core.history.changeset.AbstractChangeSet;
import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.history.changeset.ChangeSet;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Stores changes to a {@link ChangeSet}.
 */
public final class HistoryExtent extends AbstractDelegateExtent {

    private final MutableBlockVector3 mutable = new MutableBlockVector3();
    private AbstractChangeSet changeSet;

    /**
     * Create a new instance.
     *
     * @param extent    the extent
     * @param changeSet the change set
     */
    public HistoryExtent(Extent extent, AbstractChangeSet changeSet) {
        super(extent);
        checkNotNull(changeSet);
        this.changeSet = changeSet;
    }

    public AbstractChangeSet getChangeSet() {
        return changeSet;
    }

    @Override
    public void setChangeSet(AbstractChangeSet fcs) {
        this.changeSet = fcs;
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(int x, int y, int z, B block) throws WorldEditException {
        BaseBlock previous = getFullBlock(x, y, z);
        if (previous.getInternalId() == block.getInternalId()) {
            if (!previous.hasNbtData() && block instanceof BaseBlock && !block.hasNbtData()) {
                return false;
            }
        }
        this.changeSet.add(x, y, z, previous, block.toBaseBlock());
        return getExtent().setBlock(x, y, z, block);
    }


    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 location, B block) throws WorldEditException {
        return setBlock(location.getBlockX(), location.getBlockY(), location.getBlockZ(), block);
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity state) {
        final Entity entity = super.createEntity(location, state);
        if (state != null) {
            this.changeSet.addEntityCreate(state.getNbtData());
        }
        return entity;
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity state, UUID uuid) {
        final Entity entity = super.createEntity(location, state, uuid);
        if (state != null) {
            this.changeSet.addEntityCreate(state.getNbtData());
        }
        return entity;
    }

    @Override
    public List<? extends Entity> getEntities() {
        return this.wrapEntities(super.getEntities());
    }

    @Override
    public List<? extends Entity> getEntities(Region region) {
        return this.wrapEntities(super.getEntities(region));
    }

    private List<? extends Entity> wrapEntities(List<? extends Entity> entities) {
        final List<Entity> newList = new ArrayList<>(entities.size());
        for (Entity entity : entities) {
            newList.add(new TrackedEntity(entity));
        }
        return newList;
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType newBiome) {
        BiomeType oldBiome = this.getBiome(position);
        if (!oldBiome.getId().equals(newBiome.getId())) {
            this.changeSet.addBiomeChange(position.getBlockX(), position.getBlockY(), position.getBlockZ(), oldBiome, newBiome);
            return getExtent().setBiome(position, newBiome);
        } else {
            return false;
        }
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType newBiome) {
        BiomeType oldBiome = this.getBiome(mutable.setComponents(x, y, z));
        if (!oldBiome.getId().equals(newBiome.getId())) {
            this.changeSet.addBiomeChange(x, y, z, oldBiome, newBiome);
            return getExtent().setBiome(x, y, z, newBiome);
        } else {
            return false;
        }
    }

    public class TrackedEntity implements Entity {

        private final Entity entity;

        private TrackedEntity(Entity entity) {
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
            if (state != null && success) {
                HistoryExtent.this.changeSet.addEntityRemove(state.getNbtData());
            }
            return success;
        }

        @Nullable
        @Override
        public <T> T getFacet(Class<? extends T> cls) {
            return this.entity.getFacet(cls);
        }

        @Override
        public boolean setLocation(Location location) {
            return this.entity.setLocation(location);
        }

    }

    @Override
    public Extent disableHistory() {
        return getExtent();
    }

}
