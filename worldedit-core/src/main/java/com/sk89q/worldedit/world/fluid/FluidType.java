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

package com.sk89q.worldedit.world.fluid;

import com.fastasyncworldedit.core.registry.RegistryItem;
import com.sk89q.worldedit.registry.Keyed;
import com.sk89q.worldedit.registry.NamespacedRegistry;

/**
 * Minecraft now has a 'fluid' system. This is a stub class to represent what it may be in the future.
 */
//FAWE start - implements RegistryItem, not a record (internalId needs mutability)
public class FluidType implements RegistryItem, Keyed {
//FAWE end

    public static final NamespacedRegistry<FluidType> REGISTRY = new NamespacedRegistry<>("fluid type");

    //FAWE start
    private final String id;

    public FluidType(String id) {
        this.id = id;
    }

    /**
     * Gets the ID of this block.
     *
     * @return The id
     * @since 2.11.0
     */
    public String id() {
        return this.id;
    }

    /**
     * Gets the ID of this block.
     *
     * @return The id
     * @deprecated use {@link #id()}
     */
    @Deprecated(forRemoval = true, since = "2.11.0")
    @Override
    public String getId() {
        return this.id;
    }

    //FAWE start
    private int internalId;

    //UNUSED
    @Override
    public void setInternalId(int internalId) {
        this.internalId = internalId;
    }

    //UNUSED
    @Override
    public int getInternalId() {
        return internalId;
    }
    //FAWE end

    @Override
    public String toString() {
        return id();
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof FluidType && this.id.equals(((FluidType) obj).id);
    }

}
