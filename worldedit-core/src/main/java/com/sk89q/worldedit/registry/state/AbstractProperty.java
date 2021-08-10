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

import com.fastasyncworldedit.core.registry.state.PropertyKey;
import com.fastasyncworldedit.core.util.MathMan;
import com.sk89q.worldedit.world.block.BlockTypesCache;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;

public abstract class AbstractProperty<T> implements Property<T> {

    //FAWE start
    private final PropertyKey key;
    //FAWE end
    private String name;
    private final List<T> values;

    //FAWE start
    private final int bitMask;
    private final int bitMaskInverse;
    private final int bitOffset;
    private final int numBits;

    public AbstractProperty(final String name, final List<T> values) {
        this(name, values, 0);
    }

    public AbstractProperty(final String name, final List<T> values, int bitOffset) {
        this.name = name;
        this.values = values;
        //FAWE end
        this.numBits = MathMan.log2nlz(values.size());
        this.bitOffset = bitOffset + BlockTypesCache.BIT_OFFSET;
        this.bitMask = (((1 << numBits) - 1)) << this.bitOffset;
        this.bitMaskInverse = ~this.bitMask;
        this.key = PropertyKey.getOrCreate(name);
    }

    //FAWE start
    @Override
    public PropertyKey getKey() {
        return key;
    }

    @Deprecated
    public int getNumBits() {
        return numBits;
    }

    @Deprecated
    public int getBitOffset() {
        return bitOffset;
    }

    @Deprecated
    public int getBitMask() {
        return bitMask;
    }

    //todo remove the following to allow for upstream compatibility.
    public abstract <C extends AbstractProperty<T>> C withOffset(int bitOffset);

    @Deprecated
    public int modify(int state, T value) {
        int index = getIndex(value);
        if (index != -1) {
            return modifyIndex(state, index);
        }
        return state;
    }

    public int modifyIndex(int state, int index) {
        return ((state & bitMaskInverse) | (index << this.bitOffset));
    }

    public T getValue(int state) {
        return values.get((state & bitMask) >> bitOffset);
    }

    public int getIndex(int state) {
        return (state & bitMask) >> bitOffset;
    }
    //FAWE end

    @Override
    public List<T> getValues() {
        return this.values;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{name=" + name + "}";
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String getName() {
        return this.name;
    }

    //FAWE start
    @Nullable
    @Override
    public T getValueFor(String string) throws IllegalArgumentException {
        return (T) string;
    }

    /**
     * Internal method for name setting post-deserialise. Do not use.
     */
    public void setName(final String name) {
        checkState(this.name == null, "name already set");
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Property)) {
            return false;
        }
        return getName().equals(((Property<?>) obj).getName());
    }
    //FAWE end
}
