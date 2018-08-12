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

package com.sk89q.worldedit;

import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.util.Direction;

/**
 * The player's direction.
 *
 * <p>In the future, this class will be replaced with {@link Direction}.</p>
 */
public enum PlayerDirection {

    NORTH(new Vector(0, 0, -1), new Vector(-1, 0, 0), true),
    NORTH_EAST((new Vector(1, 0, -1)), (new Vector(-1, 0, -1)), false),
    EAST(new Vector(1, 0, 0), new Vector(0, 0, -1), true),
    SOUTH_EAST((new Vector(1, 0, 1)), (new Vector(1, 0, -1)), false),
    SOUTH(new Vector(0, 0, 1), new Vector(1, 0, 0), true),
    SOUTH_WEST((new Vector(-1, 0, 1)), (new Vector(1, 0, 1)), false),
    WEST(new Vector(-1, 0, 0), new Vector(0, 0, 1), true),
    NORTH_WEST((new Vector(-1, 0, -1)), (new Vector(-1, 0, 1)), false),
    UP(new Vector(0, 1, 0), new Vector(0, 0, 1), true),
    DOWN(new Vector(0, -1, 0), new Vector(0, 0, 1), true);

    private final Vector dir;
    private final Vector leftDir;
    private final boolean isOrthogonal;

    PlayerDirection(Vector vec, Vector leftDir, boolean isOrthogonal) {
        this.dir = vec;
        this.leftDir = leftDir;
        this.isOrthogonal = isOrthogonal;
    }

    public static PlayerDirection valueOf(Player player, String dirStr) throws UnknownDirectionException {
        final PlayerDirection dir;

        switch (dirStr.charAt(0)) {
            case 'w':
                dir = PlayerDirection.WEST;
                break;

            case 'e':
                dir = PlayerDirection.EAST;
                break;

            case 's':
                if (dirStr.indexOf('w') > 0) {
                    return PlayerDirection.SOUTH_WEST;
                }

                if (dirStr.indexOf('e') > 0) {
                    return PlayerDirection.SOUTH_EAST;
                }
                dir = PlayerDirection.SOUTH;
                break;

            case 'n':
                if (dirStr.indexOf('w') > 0) {
                    return PlayerDirection.NORTH_WEST;
                }

                if (dirStr.indexOf('e') > 0) {
                    return PlayerDirection.NORTH_EAST;
                }
                dir = PlayerDirection.NORTH;
                break;

            case 'u':
                dir = PlayerDirection.UP;
                break;

            case 'd':
                dir = PlayerDirection.DOWN;
                break;

            case 'm': // me
            case 'f': // forward
                dir = player.getCardinalDirection(0);
                break;

            case 'b': // back
                dir = player.getCardinalDirection(180);
                break;

            case 'l': // left
                dir = player.getCardinalDirection(-90);
                break;

            case 'r': // right
                dir = player.getCardinalDirection(90);
                break;

            default:
                throw new UnknownDirectionException(dirStr);
        }
        return dir;
    }

    public Vector vector() {
        return dir;
    }

    @Deprecated
    public Vector leftVector() {
        return leftDir;
    }

    public boolean isOrthogonal() {
        return isOrthogonal;
    }

    public static Class<?> inject() {
        return PlayerDirection.class;
    }
}
