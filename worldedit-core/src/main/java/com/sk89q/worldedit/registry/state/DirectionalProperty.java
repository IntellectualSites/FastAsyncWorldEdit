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

package com.sk89q.worldedit.registry.state;

import com.sk89q.worldedit.util.Direction;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;

public class DirectionalProperty extends AbstractProperty<Direction> {

    private final int[] map;

    public DirectionalProperty(final String name, final List<Direction> values) {
        this(name, values, 0);
    }

    private DirectionalProperty(final String name, final List<Direction> values, int bitOffset) {
        super(name, values, bitOffset);
        this.map = new int[Direction.values().length];
        Arrays.fill(this.map, -1);
        for (int i = 0; i < values.size(); i++) {
            this.map[values.get(i).ordinal()] = i;
        }
    }

    @Override
    public DirectionalProperty withOffset(int bitOffset) {
        return new DirectionalProperty(getName(), getValues(), bitOffset);
    }

    @Override
    public int getIndex(Direction value) {
        return this.map[value.ordinal()];
    }

    @Override
    public int getIndexFor(CharSequence string) throws IllegalArgumentException {
        Direction dir = Direction.get(string);
        if (dir == null) {
            return -1;
        }
        return getIndex(dir);
    }

    @Nullable
    @Override
    public Direction getValueFor(final String string) {
        Direction direction = Direction.valueOf(string.toUpperCase(Locale.ROOT));
        if (!getValues().contains(direction)) {
            throw new IllegalArgumentException("Invalid direction value: " + string + ". Must be in " + getValues().toString());
        }
        return direction;
    }
}
