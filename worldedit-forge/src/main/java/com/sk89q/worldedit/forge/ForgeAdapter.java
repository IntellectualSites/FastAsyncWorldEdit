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

package com.sk89q.worldedit.forge;

<<<<<<< HEAD
import com.sk89q.worldedit.Vector;
=======
import com.google.common.collect.ImmutableList;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.registry.state.BooleanProperty;
import com.sk89q.worldedit.registry.state.DirectionalProperty;
import com.sk89q.worldedit.registry.state.EnumProperty;
import com.sk89q.worldedit.registry.state.IntegerProperty;
import com.sk89q.worldedit.registry.state.Property;
>>>>>>> 399e0ad5... Refactor vector system to be cleaner
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.World;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

final class ForgeAdapter {

    private ForgeAdapter() {
    }

    public static World adapt(net.minecraft.world.World world) {
        return new ForgeWorld(world);
    }

    public static Vector3 adapt(Vec3d vector) {
        return Vector3.at(vector.x, vector.y, vector.z);
    }

    public static Vector3 adapt(BlockPos pos) {
        return Vector3.at(pos.getX(), pos.getY(), pos.getZ());
    }

    public static Vec3d toVec3(BlockVector3 vector) {
        return new Vec3d(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
    }

    public static EnumFacing adapt(Direction face) {
        switch (face) {
            case NORTH: return EnumFacing.NORTH;
            case SOUTH: return EnumFacing.SOUTH;
            case WEST: return EnumFacing.WEST;
            case EAST: return EnumFacing.EAST;
            case DOWN: return EnumFacing.DOWN;
            case UP:
            default:
                return EnumFacing.UP;
        }
    }

<<<<<<< HEAD
    public static BlockPos toBlockPos(Vector vector) {
=======
    public static Direction adaptEnumFacing(EnumFacing face) {
        switch (face) {
            case NORTH: return Direction.NORTH;
            case SOUTH: return Direction.SOUTH;
            case WEST: return Direction.WEST;
            case EAST: return Direction.EAST;
            case DOWN: return Direction.DOWN;
            case UP:
            default:
                return Direction.UP;
        }
    }

    public static BlockPos toBlockPos(BlockVector3 vector) {
>>>>>>> 399e0ad5... Refactor vector system to be cleaner
        return new BlockPos(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
    }

}
