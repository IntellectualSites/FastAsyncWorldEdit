package com.sk89q.worldedit.registry;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static org.enginehub.piston.converter.SuggestionHelper.limitByPrefix;

public interface IRegistry<V> extends Iterable<V> {
    String getName();

    V get(final String key);

    Set<String> keySet();

    Collection<V> values();

    @Override
    default Iterator<V> iterator() {
        return values().iterator();
    }

    default <V extends Keyed> Stream<String> getSuggestions(String input) {
        return limitByPrefix(keySet().stream(), input).stream();
    }
}
