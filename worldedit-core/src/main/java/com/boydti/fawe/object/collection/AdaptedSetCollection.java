package com.boydti.fawe.object.collection;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

/**
 * Adapt a collection to a set
 * (It's assumed that the collection is set like, otherwise behavior will be weird)
 *
 * @param <T>
 */
public class AdaptedSetCollection<T, V> implements Set<V> {
    private final Function<T, V> adapter;
    private final Collection<V> adapted;
    private final Collection<T> original;

    public AdaptedSetCollection(Collection<T> collection, Function<T, V> adapter) {
        this.original = collection;
        this.adapted = Collections2.transform(collection, adapter);
        this.adapter = adapter;
    }

    public Collection<T> getOriginal() {
        return original;
    }

    @Override
    public int size() {
        return adapted.size();
    }

    @Override
    public boolean isEmpty() {
        return adapted.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return adapted.contains(o);
    }

    @NotNull
    @Override
    public Iterator<V> iterator() {
        return adapted.iterator();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return adapted.toArray();
    }

    @NotNull
    @Override
    public <V> V[] toArray(@NotNull V[] a) {
        return adapted.toArray(a);
    }

    @Override
    public boolean add(V v) {
        return adapted.add(v);
    }

    @Override
    public boolean remove(Object o) {
        return adapted.remove(o);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return adapted.containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends V> c) {
        return adapted.addAll(c);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return adapted.removeAll(c);
    }

    @Override
    public boolean removeIf(Predicate<? super V> filter) {
        return adapted.removeIf(filter);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return adapted.retainAll(c);
    }

    @Override
    public void clear() {
        adapted.clear();
    }

    @Override
    public boolean equals(Object o) {
        return adapted.equals(o);
    }

    @Override
    public int hashCode() {
        return adapted.hashCode();
    }

    @Override
    public Spliterator<V> spliterator() {
        return adapted.spliterator();
    }

    @Override
    public Stream<V> stream() {
        return adapted.stream();
    }

    @Override
    public Stream<V> parallelStream() {
        return adapted.parallelStream();
    }

    @Override
    public void forEach(Consumer<? super V> action) {
        adapted.forEach(action);
    }
}
