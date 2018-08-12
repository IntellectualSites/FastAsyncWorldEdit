/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.function.entity;

import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.ByteTag;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.FloatTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.EntityFunction;
import com.sk89q.worldedit.internal.helper.MCDirections;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.Direction.Flag;
import com.sk89q.worldedit.util.Location;
import java.util.Arrays;
import java.util.Map;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Copies entities provided to the function to the provided destination
 * {@code Extent}.
 */
public class ExtentEntityCopy implements EntityFunction {

    private final Extent destination;
    private final Vector from;
    private final Vector to;
    private final Transform transform;
    private boolean removing;

    /**
     * Create a new instance.
     *
     * @param from        the from position
     * @param destination the destination {@code Extent}
     * @param to          the destination position
     * @param transform   the transformation to apply to both position and orientation
     */
    public ExtentEntityCopy(Vector from, Extent destination, Vector to, Transform transform) {
        checkNotNull(from);
        checkNotNull(destination);
        checkNotNull(to);
        checkNotNull(transform);
        this.destination = destination;
        this.from = from;
        this.to = to;
        this.transform = transform;
    }

    /**
     * Return whether entities that are copied should be removed.
     *
     * @return true if removing
     */
    public boolean isRemoving() {
        return removing;
    }

    /**
     * Set whether entities that are copied should be removed.
     *
     * @param removing true if removing
     */
    public void setRemoving(boolean removing) {
        this.removing = removing;
    }

    @Override
    public boolean apply(Entity entity) throws WorldEditException {
        BaseEntity state = entity.getState();
        if (state != null) {
            Location newLocation;
            Location location = entity.getLocation();

            Vector pivot = from.round().add(0.5, 0.5, 0.5);
            Vector newPosition = transform.apply(location.toVector().subtract(pivot));
            Vector newDirection;
            if (transform.isIdentity()) {
                newDirection = entity.getLocation().getDirection();
                newLocation = new Location(destination, newPosition.add(to.round().add(0.5, 0.5, 0.5)), newDirection);
            } else {
                newDirection = new Vector(transform.apply(location.getDirection())).subtract(transform.apply(Vector.ZERO)).normalize();
                newLocation = new Location(destination, newPosition.add(to.round().add(0.5, 0.5, 0.5)), newDirection);
                state = transformNbtData(state);
            }

            boolean success = destination.createEntity(newLocation, state) != null;

            // Remove
            if (isRemoving()) {
                entity.remove();
            }

            return success;
        } else {
            return false;
        }
    }

    /**
     * Transform NBT data in the given entity state and return a new instance
     * if the NBT data needs to be transformed.
     *
     * @param state the existing state
     * @return a new state or the existing one
     */
    private BaseEntity transformNbtData(BaseEntity state) {
        CompoundTag tag = state.getNbtData();
        if (tag != null) {
            boolean changed = false;
            // Handle hanging entities (paintings, item frames, etc.)

            tag = tag.createBuilder().build();

            Map<String, Tag> values = ReflectionUtils.getMap(tag.getValue());

            boolean hasTilePosition = tag.containsKey("TileX") && tag.containsKey("TileY") && tag.containsKey("TileZ");
            boolean hasDirection = tag.containsKey("Direction");
            boolean hasLegacyDirection = tag.containsKey("Dir");
            boolean hasFacing = tag.containsKey("Facing");

            if (hasTilePosition) {
                changed = true;
                Vector tilePosition = new Vector(tag.asInt("TileX"), tag.asInt("TileY"), tag.asInt("TileZ"));
                Vector newTilePosition = transform.apply(tilePosition.subtract(from)).add(to);

                values.put("TileX", new IntTag(newTilePosition.getBlockX()));
                values.put("TileY", new IntTag(newTilePosition.getBlockY()));
                values.put("TileZ", new IntTag(newTilePosition.getBlockZ()));

                if (hasDirection || hasLegacyDirection || hasFacing) {
                    int d;
                    if (hasDirection) {
                        d = tag.asInt("Direction");
                    } else if (hasLegacyDirection) {
                        d = MCDirections.fromLegacyHanging((byte) tag.asInt("Dir"));
                    } else {
                        d = tag.asInt("Facing");
                    }

                    Direction direction = MCDirections.fromHanging(d);

                    if (direction != null) {
                        Vector vector = transform.apply(direction.toVector()).subtract(transform.apply(Vector.ZERO)).normalize();
                        Direction newDirection = Direction.findClosest(vector, Flag.CARDINAL);

                        if (newDirection != null) {
                            byte hangingByte = (byte) MCDirections.toHanging(newDirection);
                            values.put("Direction", new ByteTag(hangingByte));
                            values.put("Facing", new ByteTag(hangingByte));
                            values.put("Dir", new ByteTag(MCDirections.toLegacyHanging(MCDirections.toHanging(newDirection))));
                        }
                    }
                }
            }

            ListTag rotation = tag.getListTag("Rotation");
            if (rotation != null && rotation.getValue().size() >= 2) {
                changed = true;
                double yaw = Math.toRadians(rotation.getFloat(0));
                double pitch = Math.toRadians(rotation.getFloat(1));

                double xz = Math.cos(pitch);
                Vector direction = new Vector(-xz * Math.sin(yaw), -Math.sin(pitch), xz * Math.cos(yaw));
                direction = transform.apply(direction);
                FloatTag yawTag = new FloatTag(direction.toYaw());
                FloatTag pitchTag = new FloatTag(direction.toPitch());
                values.put("Rotation", new ListTag(FloatTag.class, Arrays.asList(yawTag, pitchTag)));
            }

            if (changed) {
                return new BaseEntity(state.getType(), tag);
            }
        }

        return state;
    }

    public static Class<?> inject() {
        return ExtentEntityCopy.class;
    }

}
