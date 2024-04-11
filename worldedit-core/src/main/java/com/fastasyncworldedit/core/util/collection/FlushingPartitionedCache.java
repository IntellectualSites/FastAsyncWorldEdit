package com.fastasyncworldedit.core.util.collection;

import org.jetbrains.annotations.ApiStatus;

import java.io.Flushable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 *
 * @param <P> the partition key type
 * @param <E> the element type
 * @param <C> the partition value type
 * @since TODO
 */
@ApiStatus.Internal
public class FlushingPartitionedCache<P, E, C extends Collection<E>> implements Flushable {

    private final Map<P, C> map;
    private final Function<? super E, ? extends P> partition;
    private final Supplier<? extends C> constructor;
    private final BiConsumer<? super P, ? super C> flusher;
    private final int maxEntriesPerCollection;

    public FlushingPartitionedCache(
            Function<? super E, ? extends P> partition,
            Supplier<? extends C> constructor,
            BiConsumer<? super P, ? super C> flusher,
            int maxEntriesPerCollection,
            int maxCollections
    ) {
        this.partition = partition;
        this.constructor = constructor;
        this.flusher = flusher;
        this.maxEntriesPerCollection = maxEntriesPerCollection;
        this.map = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(final Map.Entry<P, C> eldest) {
                final boolean remove = size() > maxCollections;
                if (remove) {
                    flusher.accept(eldest.getKey(), eldest.getValue());
                }
                return remove;
            }
        };
    }

    public void insert(E element) {
        final P partition = this.partition.apply(element);
        final C coll = this.map.computeIfAbsent(partition, k -> this.constructor.get());
        coll.add(element);
        if (coll.size() > this.maxEntriesPerCollection) {
            this.flusher.accept(partition, coll);
            this.map.remove(partition);
        }
    }

    @Override
    public void flush() {
        for (final Map.Entry<P, C> entry : this.map.entrySet()) {
            this.flusher.accept(entry.getKey(), entry.getValue());
        }
        this.map.clear();
    }

}
