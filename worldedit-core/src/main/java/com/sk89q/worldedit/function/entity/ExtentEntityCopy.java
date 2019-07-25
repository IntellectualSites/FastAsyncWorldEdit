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

import static com.google.common.base.Preconditions.checkNotNull;

import com.sk89q.jnbt.ByteTag;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.FloatTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.EntityFunction;
import com.sk89q.worldedit.internal.helper.MCDirections;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.Direction.Flag;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.entity.EntityTypes;

import java.util.Arrays;
import java.util.Map;

/**
 * Copies entities provided to the function to the provided destination
 * {@code Extent}.
 */
public class ExtentEntityCopy implements EntityFunction {

    private final Extent destination;
    private final Vector3 from;
    private final Vector3 to;
    private final Transform transform;
    private boolean removing;

    /**
     * Create a new instance.
     *
     * @param from the from position
     * @param destination the destination {@code Extent}
     * @param to the destination position
     * @param transform the transformation to apply to both position and orientation
     */
    public ExtentEntityCopy(Vector3 from, Extent destination, Vector3 to, Transform transform) {
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
        if (state != null && state.getType() != EntityTypes.PLAYER) {
            Location newLocation;
            Location location = entity.getLocation();
            // If the entity has stored the location in the NBT data, we use that location
            CompoundTag tag = state.getNbtData();
            boolean hasTilePosition = tag != null && tag.containsKey("TileX") && tag.containsKey("TileY") && tag.containsKey("TileZ");
            if (hasTilePosition) {
                location = location.setPosition(Vector3.at(tag.asInt("TileX"), tag.asInt("TileY"), tag.asInt("TileZ")).add(0.5, 0.5, 0.5));
            }

            Vector3 pivot = from.round().add(0.5, 0.5, 0.5);
            Vector3 newPosition = transform.apply(location.toVector().subtract(pivot));
            if (hasTilePosition) {
                newPosition = newPosition.subtract(0.5, 0.5, 0.5);
            }
            Vector3 newDirection;

            newDirection = transform.isIdentity() ?
                    entity.getLocation().getDirection()
                    : transform.apply(location.getDirection()).subtract(transform.apply(Vector3.ZERO)).normalize();
            newLocation = new Location(destination, newPosition.add(to.round().add(0.5, 0.5, 0.5)), newDirection);

            // Some entities store their position data in NBT
            state = transformNbtData(state);

            boolean success = destination.createEntity(newLocation, state) != null;

            // Remove
            if (isRemoving() && success) {
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
            // Handle leashed entities
            Tag leashTag = tag.getValue().get("Leash");
            if (leashTag instanceof CompoundTag) {
                CompoundTag leashCompound = (CompoundTag) leashTag;
                if (leashCompound.containsKey("X")) { // leashed to a fence
                    Vector3 tilePosition = Vector3.at(leashCompound.asInt("X"), leashCompound.asInt("Y"), leashCompound.asInt("Z"));
                    BlockVector3 newLeash = transform.apply(tilePosition.subtract(from)).add(to).toBlockPoint();
                    return new BaseEntity(state.getType(), tag.createBuilder()
                            .put("Leash", leashCompound.createBuilder()
                                .putInt("X", newLeash.getBlockX())
                                .putInt("Y", newLeash.getBlockY())
                                .putInt("Z", newLeash.getBlockZ())
                                .build()
                            ).build());
                }
            }

            boolean changed = false;
            // Handle hanging entities (paintings, item frames, etc.)
            boolean hasTilePosition = tag.containsKey("TileX") && tag.containsKey("TileY") && tag.containsKey("TileZ");
            boolean hasFacing = tag.containsKey("Facing");
            tag = tag.createBuilder().build();

            Map<String, Tag> values = ReflectionUtils.getMap(tag.getValue());

            boolean hasDirection = tag.containsKey("Direction");
            boolean hasLegacyDirection = tag.containsKey("Dir");

            if (hasTilePosition) {
                changed = true;
                Vector3 tilePosition = Vector3.at(tag.asInt("TileX"), tag.asInt("TileY"), tag.asInt("TileZ"));
                BlockVector3 newTilePosition = transform.apply(tilePosition.subtract(from)).add(to).toBlockPoint();

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
                        Vector3 vector = transform.apply(direction.toVector()).subtract(transform.apply(Vector3.ZERO)).normalize();
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
                Vector3 direction = Vector3.at(-xz * Math.sin(yaw), -Math.sin(pitch), xz * Math.cos(yaw));
                direction = transform.apply(direction);
                FloatTag yawTag = new FloatTag((float) direction.toYaw());
                FloatTag pitchTag = new FloatTag((float) direction.toPitch());
                values.put("Rotation", new ListTag(FloatTag.class, Arrays.asList(yawTag, pitchTag)));
            }

            if (changed) {
                return new BaseEntity(state.getType(), tag);
            }
        }

        return state;
    }
}
