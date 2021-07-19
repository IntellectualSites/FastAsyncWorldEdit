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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public class EnumProperty extends AbstractProperty<String> {

    //FAWE start
    private final Map<CharSequence, Integer> offsets = new HashMap<>();
    //FAWE end

    public EnumProperty(final String name, final List<String> values) {
        this(name, values, 0);
    }

    //FAWE start
    private EnumProperty(final String name, final List<String> values, int bitOffset) {
        super(name, values, bitOffset);
        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i).intern();
            values.set(i, value);
            offsets.put(value, i);
        }
    }

    @Override
    public EnumProperty withOffset(int bitOffset) {
        return new EnumProperty(getName(), getValues(), bitOffset);
    }

    @Override
    public int getIndexFor(CharSequence string) throws IllegalArgumentException {
        Integer value = offsets.get(string);
        return value == null ? -1 : value;
    }
    //FAWE end

    @Nullable
    @Override
    public String getValueFor(String string) {
        Integer offset = offsets.get(string);
        if (offset == null) {
            throw new IllegalArgumentException("Invalid value: " + string + ". Must be in " + getValues().toString());
        }
        return getValues().get(offset);
    }
}
