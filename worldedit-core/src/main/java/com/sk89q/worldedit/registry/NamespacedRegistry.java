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
import static org.enginehub.piston.converter.SuggestionHelper.byPrefix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public final class NamespacedRegistry<V extends Keyed> extends Registry<V> {
    private static final String MINECRAFT_NAMESPACE = "minecraft";
    private final Set<String> knownNamespaces = new HashSet<>();
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

    @Nullable
    @Override
    public V get(final String key) {
        return super.get(this.orDefaultNamespace(key));
    }

    @Override
    public synchronized V register(final String key, final V value) {
        requireNonNull(key, "key");
        final int i = key.indexOf(':');
        checkState(i > 0, "key is not namespaced");
        if (value instanceof RegistryItem) {
            ((RegistryItem) value).setInternalId(lastInternalId++);
        }
        values.add(value);
        final V registered = super.register(key, value);
        knownNamespaces.add(key.substring(0, i));
        return registered;
    }

    public V getByInternalId(int index) {
        try {
            return values.get(index);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public int size() {
        return values.size();
    }

    /**
     * Get a set of the namespaces of all registered keys.
     *
     * @return set of namespaces
     */
    public Set<String> getKnownNamespaces() {
        return Collections.unmodifiableSet(knownNamespaces);
    }

    /**
     * Get the default namespace for this registry.
     *
     * @return the default namespace
     */
    public String getDefaultNamespace() {
        return defaultNamespace;
    }

    private String orDefaultNamespace(final String key) {
        if (key.indexOf(':') == -1) {
            return defaultNamespace + ':' + key;
        }
        return key;
    }

    public <V1 extends Keyed> Stream<String> getSuggestions(String input) {
        if (input.isEmpty() || input.equals(":")) {
            final Set<String> namespaces = getKnownNamespaces();
            if (namespaces.size() == 1) {
                return keySet().stream();
            } else {
                return namespaces.stream().map(s -> s + ":");
            }
        }
        if (input.startsWith(":")) { // special case - search across namespaces
            final String term = input.substring(1).toLowerCase(Locale.ROOT);
            Predicate<String> search = byPrefix(term);
            return keySet().stream().filter(s -> search.test(s.substring(s.indexOf(':') + 1)));
        }
        // otherwise, we actually have some text to search
        if (input.indexOf(':') < 0) {
            // don't yet have namespace - search namespaces + default
            final String lowerSearch = input.toLowerCase(Locale.ROOT);
            String defKey = getDefaultNamespace() + ":" + lowerSearch;
            return Stream.concat(keySet().stream().filter(s -> s.startsWith(defKey)),
                    getKnownNamespaces().stream().filter(n -> n.startsWith(lowerSearch)).map(n -> n + ":"));
        }
        // have a namespace - search that
        Predicate<String> search = byPrefix(input.toLowerCase(Locale.ROOT));
        return keySet().stream().filter(search);
    }
}
