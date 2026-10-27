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

package com.sk89q.worldedit.bukkit;

import com.fastasyncworldedit.core.util.FoliaSupport;
import com.fastasyncworldedit.core.util.TaskManager;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.entity.metadata.EntityProperties;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.NullWorld;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An adapter to adapt a Bukkit entity into a WorldEdit one.
 */
//FAWE start - made class public
public class BukkitEntity implements Entity {
//FAWE end

    private final WeakReference<org.bukkit.entity.Entity> entityRef;
    //FAWE start
    private final EntityType type;
    //FAWE end

    /**
     * Create a new instance.
     *
     * @param entity the entity
     */
    public BukkitEntity(org.bukkit.entity.Entity entity) {
        checkNotNull(entity);
        //FAWE start
        this.type = entity.getType();
        //FAWE end
        this.entityRef = new WeakReference<>(entity);
    }

    @Override
    public Extent getExtent() {
        org.bukkit.entity.Entity entity = entityRef.get();
        if (entity != null) {
            return BukkitAdapter.adapt(entity.getWorld());
        } else {
            return NullWorld.getInstance();
        }
    }

    @Override
    public Location getLocation() {
        org.bukkit.entity.Entity entity = entityRef.get();
        if (entity != null) {
            return BukkitAdapter.adapt(entity.getLocation());
        } else {
            return new Location(NullWorld.getInstance());
        }
    }

    @Override
    public boolean setLocation(Location location) {
        org.bukkit.entity.Entity entity = entityRef.get();
        if (entity != null) {
            return entity.teleport(BukkitAdapter.adapt(location));
        } else {
            return false;
        }
    }

    @Override
    public BaseEntity getState() {
        org.bukkit.entity.Entity entity = entityRef.get();
        if (entity == null || entity instanceof Player) {
            return null;
        }

        BukkitImplAdapter adapter = WorldEditPlugin.getInstance().getBukkitImplAdapter();
        if (adapter == null) {
            return null;
        }
        return syncEntity(entity, () -> adapter.getEntity(entity));
    }

    @Override
    public boolean remove() {
        org.bukkit.entity.Entity entity = entityRef.get();
        if (entity == null) {
            return true;
        }
        Boolean result = syncEntity(entity, () -> {
            try {
                entity.remove();
            } catch (UnsupportedOperationException e) {
                return false;
            }
            return entity.isDead();
        });
        return result != null ? result : true;
    }

    private static <T> T syncEntity(org.bukkit.entity.Entity entity, Supplier<T> supplier) {
        if (FoliaSupport.isFolia()) {
            if (Bukkit.isOwnedByCurrentRegion(entity)) {
                return supplier.get();
            }
            FutureTask<T> task = new FutureTask<>(supplier::get);
            entity.getScheduler().execute(WorldEditPlugin.getInstance(), task, () -> task.cancel(false), 0);
            try {
                return task.get();
            } catch (CancellationException e) {
                return null;
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        } else {
            return TaskManager.taskManager().sync(supplier);
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T getFacet(Class<? extends T> cls) {
        org.bukkit.entity.Entity entity = entityRef.get();
        if (entity != null && EntityProperties.class.isAssignableFrom(cls)) {
            return (T) new BukkitEntityProperties(entity);
        } else {
            return null;
        }
    }

}
