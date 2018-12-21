package com.sk89q.worldedit.bukkit;

import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.util.Location;

import javax.annotation.Nullable;

public class DelegateEntity implements Entity {
    private final Entity parent;

    public DelegateEntity(Entity parent) {
        this.parent = parent;
    }

    public Entity getParent() {
        return parent;
    }

    @Override
    @Nullable
    public BaseEntity getState() {
        return parent.getState();
    }

    @Override
    public Location getLocation() {
        return parent.getLocation();
    }

    @Override
    public Extent getExtent() {
        return parent.getExtent();
    }

    @Override
    public boolean remove() {
        return parent.remove();
    }

    @Override
    @Nullable
    public <T> T getFacet(Class<? extends T> cls) {
        return parent.getFacet(cls);
    }

	@Override
	public boolean setLocation(Location location) {
		return parent.setLocation(location);
	}
}
