package com.boydti.fawe.wrappers;

import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.util.Location;

public class LocationMaskedPlayerWrapper extends PlayerWrapper {
    private final boolean allowTeleport;
    private Location position;

    public LocationMaskedPlayerWrapper(Player parent, Location position) {
        this(parent, position, false);
    }

    public LocationMaskedPlayerWrapper(Player parent, Location position, boolean allowTeleport) {
        super(parent);
        this.position = position;
        this.allowTeleport = allowTeleport;
    }

    @Override
    public Location getLocation() {
        return position;
    }

    @Override
    public void setPosition(Vector3 pos, float pitch, float yaw) {
        this.position = new Location(position.getExtent(), pos, pitch, yaw);
        if (allowTeleport) {
            super.setPosition(pos, pitch, yaw);
        }
    }
}