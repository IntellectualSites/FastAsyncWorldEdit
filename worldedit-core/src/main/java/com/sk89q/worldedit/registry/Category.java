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

package com.sk89q.worldedit.registry;

import com.fastasyncworldedit.core.registry.RegistryItem;

import java.util.HashSet;
import java.util.Set;

//FAWE start - implements RegistryItem
public abstract class Category<T extends Keyed> implements RegistryItem {
//FAWE end

    private final Set<T> set = new HashSet<>();
    protected final String id;
    private boolean empty = true;

    protected Category(final String id) {
        this.id = id;
    }

    public final String id() {
        return this.id;
    }

    public final Set<T> getAll() {
        if (this.empty) {
            this.set.addAll(this.load());
            this.empty = false;
        }
        return this.set;
    }

    //FAWE start
    private int internalId;

    @Override
    public void setInternalId(int internalId) {
        this.internalId = internalId;
    }

    @Override
    public int getInternalId() {
        return internalId;
    }

    protected abstract Set<T> load();
    //FAWE end

    /**
     * Checks if this category contains {@code object}.
     *
     * @param object the object
     * @return {@code true} if this category contains the object
     */
    public boolean contains(final T object) {
        return this.getAll().contains(object);
    }

    public void invalidateCache() {
        this.set.clear();
        this.empty = true;
    }

    @Override
    public String toString() {
        return id();
    }

}
