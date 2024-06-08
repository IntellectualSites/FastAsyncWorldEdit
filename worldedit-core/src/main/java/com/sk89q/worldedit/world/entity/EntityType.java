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

package com.sk89q.worldedit.world.entity;

import com.fastasyncworldedit.core.registry.RegistryItem;
import com.sk89q.worldedit.registry.Keyed;
import com.sk89q.worldedit.registry.NamespacedRegistry;

//FAWE start - implements RegistryItem, not a record (internalId needs mutability)
public class EntityType implements RegistryItem, Keyed {
//FAWE end

    public static final NamespacedRegistry<EntityType> REGISTRY = new NamespacedRegistry<>("entity type", true);

    //FAWE start
    private final String id;
    private int internalId;

    public EntityType(String id) {
        // If it has no namespace, assume minecraft.
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        this.id = id;
    }

    /**
     * Gets the id of this entity type.
     *
     * @return the id
     * @since TODO
     */
    public String id() {
        return this.id;
    }

    /**
     * Gets the id of this entity type.
     *
     * @return the id
     * @deprecated use {@link #id()}
     */
    @Deprecated(forRemoval = true, since = "TODO")
    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void setInternalId(int internalId) {
        this.internalId = internalId;
    }

    @Override
    public int getInternalId() {
        return internalId;
    }
    //FAWE end

    /**
     * Gets the name of this item, or the ID if the name cannot be found.
     *
     * @return The name, or ID
     */
    public String getName() {
        return id();
    }

    @Override
    public String toString() {
        return id();
    }

    //FAWE start
    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof EntityType && this.id.equals(((EntityType) obj).id);
    }
    //FAWE end

}
