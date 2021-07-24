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

import javax.annotation.Nullable;
import java.util.List;

public class BooleanProperty extends AbstractProperty<Boolean> {

    //FAWE start
    private final int defaultIndex;

    public BooleanProperty(final String name, final List<Boolean> values) {
        this(name, values, 0);
    }

    private BooleanProperty(final String name, final List<Boolean> values, int bitOffset) {
        super(name, values, bitOffset);
        defaultIndex = values.get(0).booleanValue() == Boolean.TRUE ? 0 : 1;
    }

    @Override
    public int getIndex(Boolean value) {
        return value ? defaultIndex : 1 - defaultIndex;
    }

    @Override
    public BooleanProperty withOffset(int bitOffset) {
        return new BooleanProperty(getName(), getValues(), bitOffset);
    }

    @Override
    public int getIndexFor(CharSequence string) throws IllegalArgumentException {
        switch (string.charAt(0)) {
            case 't':
                return defaultIndex;
            case 'f':
                return 1 - defaultIndex;
            default:
                return -1;
        }
    }
    //FAWE end

    @Nullable
    @Override
    public Boolean getValueFor(String string) {
        boolean val = Boolean.parseBoolean(string);
        if (!getValues().contains(val)) {
            throw new IllegalArgumentException("Invalid boolean value: " + string + ". Must be in " + getValues().toString());
        }
        return val;
    }

}
