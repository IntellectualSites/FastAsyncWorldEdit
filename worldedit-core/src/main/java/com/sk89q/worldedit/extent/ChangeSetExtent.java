/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.extent;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.history.change.BiomeChange3D;
import com.sk89q.worldedit.history.change.BlockChange;
import com.sk89q.worldedit.history.change.EntityCreate;
import com.sk89q.worldedit.history.change.EntityRemove;
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
public class ChangeSetExtent extends AbstractDelegateExtent {

    private final ChangeSet changeSet;
    private boolean enabled;

    /**
     * Create a new instance.
     *
     * @param extent    the extent
     * @param changeSet the change set
     */
    public ChangeSetExtent(Extent extent, ChangeSet changeSet) {
        this(extent, changeSet, true);
    }

    /**
     * Create a new instance.
     *
     * @param extent the extent
     * @param changeSet the change set
     * @param enabled if the extent is enabled
     */
    public ChangeSetExtent(Extent extent, ChangeSet changeSet, boolean enabled) {
        super(extent);
        checkNotNull(changeSet);
        this.changeSet = changeSet;
        this.enabled = true;
    }

    /**
     * If this extent is enabled and should perform change tracking.
     *
     * @return if enabled
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Sets whether this extent is enabled and should perform change tracking.
     *
     * @param enabled whether to enable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 location, B block) throws WorldEditException {
        if (enabled) {
            BaseBlock previous = getFullBlock(location);
            changeSet.add(new BlockChange(location, previous, block));
        }
        return super.setBlock(location, block);
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        if (enabled) {
            BiomeType previous = getBiome(position);
            changeSet.add(new BiomeChange3D(position, previous, biome));
        }
        return super.setBiome(position, biome);
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity state) {
        Entity entity = super.createEntity(location, state);
        if (enabled && entity != null) {
            changeSet.add(new EntityCreate(location, state, entity));
        }
        return entity;
    }

    //FAWE start
    @Override
    @Nullable
    public Entity createEntity(Location location, BaseEntity state, UUID uuid) {
        Entity entity = super.createEntity(location, state, uuid);
        if (enabled && entity != null) {
            changeSet.add(new EntityCreate(location, entity.getState(), entity));
        }
        return entity;
    }
    //FAWE end

    @Override
    public List<? extends Entity> getEntities() {
        return wrapEntities(super.getEntities());
    }

    @Override
    public List<? extends Entity> getEntities(Region region) {
        return wrapEntities(super.getEntities(region));
    }

    private List<? extends Entity> wrapEntities(List<? extends Entity> entities) {
        if (!enabled) {
            return entities;
        }
        List<Entity> newList = new ArrayList<>(entities.size());
        for (Entity entity : entities) {
            newList.add(new TrackedEntity(entity));
        }
        return newList;
    }

    private class TrackedEntity implements Entity {

        private final Entity entity;

        private TrackedEntity(Entity entity) {
            this.entity = entity;
        }

        @Override
        public BaseEntity getState() {
            return entity.getState();
        }

        @Override
        public Location getLocation() {
            return entity.getLocation();
        }

        @Override
        public boolean setLocation(Location location) {
            // TODO Add a changeset for this.
            return entity.setLocation(location);
        }

        @Override
        public Extent getExtent() {
            return entity.getExtent();
        }

        @Override
        public boolean remove() {
            Location location = entity.getLocation();
            BaseEntity state = entity.getState();
            boolean success = entity.remove();
            if (state != null && success) {
                changeSet.add(new EntityRemove(location, state));
            }
            return success;
        }

        @Nullable
        @Override
        public <T> T getFacet(Class<? extends T> cls) {
            return entity.getFacet(cls);
        }

    }

}
