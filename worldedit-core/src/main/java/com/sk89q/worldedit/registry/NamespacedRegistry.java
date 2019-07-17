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

package com.sk89q.worldedit.registry;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class NamespacedRegistry<V extends RegistryItem> extends Registry<V> {
    private static final String MINECRAFT_NAMESPACE = "minecraft";

    private final String defaultNamespace;
    private final List<V> values = new ArrayList<>();
    private int lastInternalId = 0;

    public NamespacedRegistry(final String name) {
        this(name, MINECRAFT_NAMESPACE);
    }

    public NamespacedRegistry(final String name, final String defaultNamespace) {
        super(name);
        this.defaultNamespace = defaultNamespace;
    }

    public synchronized V register(final String key, final V value) {
        requireNonNull(key, "key");
        int index = key.indexOf(':');
        checkState(index > -1, "key is not namespaced");
        V existing = super.get(key);
        if (existing != null) {
            throw new UnsupportedOperationException("Replacing existing registrations is not supported");
        }
        value.setInternalId(lastInternalId++);
        values.add(value);
        super.register(key, value);
        if (key.startsWith(defaultNamespace)) {
            super.register(key.substring(index + 1), value);
        }
        return value;
    }

    public V getByInternalId(int index) {
        try {
            return values.get(index);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public int getInternalId(V value) {
        return value.getInternalId();
    }

    public int size() {
        return values.size();
    }

    private String orDefaultNamespace(final String key) {
        if (key.indexOf(':') == -1) {
            return defaultNamespace + ':' + key;
        }
        return key;
    }
}
