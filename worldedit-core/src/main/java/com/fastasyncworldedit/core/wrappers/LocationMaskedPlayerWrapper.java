package com.fastasyncworldedit.core.wrappers;

import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.util.Location;

public class LocationMaskedPlayerWrapper extends AsyncPlayer {

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
    public void setPosition(final Vector3 pos) {
        if (allowTeleport) {
            super.setPosition(pos);
            update();
        }
    }

    private void update() {
        this.position = super.getLocation();
    }

    @Override
    public void setPosition(Vector3 pos, float pitch, float yaw) {
        if (allowTeleport) {
            super.setPosition(pos, pitch, yaw);
            update();
        }
    }

    @Override
    public void findFreePosition(Location searchPos) {
        if (allowTeleport) {
            super.findFreePosition(searchPos);
            update();
        }
    }

    @Override
    public void setOnGround(Location searchPos) {
        if (allowTeleport) {
            super.setOnGround(searchPos);
            update();
        }
    }

    @Override
    public void findFreePosition() {
        if (allowTeleport) {
            super.findFreePosition();
            update();
        }
    }

    @Override
    public boolean ascendLevel() {
        if (allowTeleport && super.ascendLevel()) {
            update();
            return true;
        }
        return false;
    }

    @Override
    public boolean descendLevel() {
        if (allowTeleport && super.descendLevel()) {
            update();
            return true;
        }
        return false;
    }

    @Override
    public boolean ascendToCeiling(int clearance) {
        if (allowTeleport && super.ascendToCeiling(clearance)) {
            update();
            return true;
        }
        return false;
    }

    @Override
    public boolean ascendToCeiling(int clearance, boolean alwaysGlass) {
        if (allowTeleport && super.ascendToCeiling(clearance, alwaysGlass)) {
            update();
            return true;
        }
        return false;
    }

    @Override
    public boolean ascendUpwards(int distance) {
        if (allowTeleport && super.ascendUpwards(distance)) {
            update();
            return true;
        }
        return false;
    }

    @Override
    public boolean ascendUpwards(int distance, boolean alwaysGlass) {
        if (allowTeleport && super.ascendUpwards(distance, alwaysGlass)) {
            update();
            return true;
        }
        return false;
    }

    @Override
    public void floatAt(int x, int y, int z, boolean alwaysGlass) {
        if (allowTeleport) {
            super.floatAt(x, y, z, alwaysGlass);
            update();
        }
    }

}
