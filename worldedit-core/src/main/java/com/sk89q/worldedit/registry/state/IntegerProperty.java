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

import com.boydti.fawe.util.StringMan;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

public class IntegerProperty extends AbstractProperty<Integer> {

    private final int[] map;

    public IntegerProperty(final String name, final List<Integer> values) {
        this(name, values, 0);
    }

    private IntegerProperty(final String name, final List<Integer> values, int bitOffset) {
        super(name, values, bitOffset);
        int max = Collections.max(values);
        this.map = new int[max + 1];
        for (int i = 0; i < values.size(); i++) {
            this.map[values.get(i)] = i;
        }
    }

    @Override
    public IntegerProperty withOffset(int bitOffset) {
        return new IntegerProperty(getName(), getValues(), bitOffset);
    }

    @Override
    public int getIndex(Integer value) {
        try {
            return this.map[value];
        } catch (IndexOutOfBoundsException ignored) {
            return -1;
        }
    }

    @Override
    public int getIndexFor(CharSequence string) throws IllegalArgumentException {
        return this.map[StringMan.parseInt(string)];
    }

    @Nullable
    @Override
    public Integer getValueFor(String string) {
        try {
            int val = Integer.parseInt(string);
            /*
            //It shouldn't matter if this check is slow. It's an important check
            if (!getValues().contains(val)) {
                throw new IllegalArgumentException("Invalid int value: " + string + ". Must be in " + getValues().toString());
            }
            */
            // An exception will get thrown anyway if the property doesn't exist, so it's not really that important. Anyway, we can check the array instead of the string list
            if (val > 0 && val >= map.length) {
                throw new IllegalArgumentException("Invalid int value: " + string + ". Must be in " + getValues().toString());
            }
            return val;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid int value: " + string + ". Not an int.");
        }
    }
}
