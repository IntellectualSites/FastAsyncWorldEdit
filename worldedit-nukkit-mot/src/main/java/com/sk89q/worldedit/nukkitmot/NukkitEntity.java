package com.sk89q.worldedit.nukkitmot;

import cn.nukkit.utils.Identifier;
import com.fastasyncworldedit.nukkitmot.NukkitNbtConverter;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.concurrency.LazyReference;
import com.sk89q.worldedit.world.NullWorld;
import com.sk89q.worldedit.world.entity.EntityType;
import com.sk89q.worldedit.world.entity.EntityTypes;
import org.enginehub.linbus.tree.LinCompoundTag;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;

/**
 * Adapts a Nukkit entity to a WorldEdit entity.
 */
public class NukkitEntity implements Entity {

    private final WeakReference<cn.nukkit.entity.Entity> entityRef;

    public NukkitEntity(cn.nukkit.entity.Entity entity) {
        this.entityRef = new WeakReference<>(entity);
    }

    @Override
    public Extent getExtent() {
        cn.nukkit.entity.Entity entity = entityRef.get();
        if (entity != null) {
            return NukkitAdapter.adapt(entity.getLevel());
        }
        return NullWorld.getInstance();
    }

    @Override
    public Location getLocation() {
        cn.nukkit.entity.Entity entity = entityRef.get();
        if (entity != null) {
            return new Location(
                    NukkitAdapter.adapt(entity.getLevel()),
                    entity.x, entity.y, entity.z,
                    (float) entity.yaw, (float) entity.pitch
            );
        }
        return new Location(NullWorld.getInstance());
    }

    @Override
    public boolean setLocation(Location location) {
        cn.nukkit.entity.Entity entity = entityRef.get();
        if (entity != null) {
            entity.teleport(NukkitAdapter.adapt(location));
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    public BaseEntity getState() {
        cn.nukkit.entity.Entity entity = entityRef.get();
        if (entity == null || entity instanceof cn.nukkit.Player) {
            return null;
        }

        Identifier identifier = entity.getIdentifier();
        if (identifier == null) {
            return null;
        }

        EntityType type = EntityTypes.get(identifier.toString());
        if (type == null) {
            return null;
        }

        cn.nukkit.nbt.tag.CompoundTag namedTag = entity.namedTag;
        if (namedTag != null) {
            return new BaseEntity(type, LazyReference.computed(
                    NukkitNbtConverter.toLinCompound(namedTag)
            ));
        }
        return new BaseEntity(type);
    }

    @Override
    public boolean remove() {
        cn.nukkit.entity.Entity entity = entityRef.get();
        if (entity != null) {
            entity.close();
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    public <T> T getFacet(Class<? extends T> cls) {
        return null;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof NukkitEntity that)) {
            return false;
        }
        cn.nukkit.entity.Entity self = this.entityRef.get();
        cn.nukkit.entity.Entity otherEntity = that.entityRef.get();
        if (self == null || otherEntity == null) {
            return false;
        }
        return self.getId() == otherEntity.getId();
    }

    @Override
    public int hashCode() {
        cn.nukkit.entity.Entity entity = entityRef.get();
        return entity != null ? Long.hashCode(entity.getId()) : 0;
    }

}
