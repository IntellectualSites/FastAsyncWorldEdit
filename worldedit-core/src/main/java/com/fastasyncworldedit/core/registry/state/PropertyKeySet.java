package com.fastasyncworldedit.core.registry.state;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

public class PropertyKeySet implements Set<PropertyKey> {

    private final BitSet bits = new BitSet(PropertyKey.getCount()); // still resizable

    public static PropertyKeySet empty() {
        return new PropertyKeySet();
    }

    public static PropertyKeySet ofCollection(Collection<? extends PropertyKey> collection) {
        PropertyKeySet set = new PropertyKeySet();
        if (collection instanceof PropertyKeySet pks) {
            // simple copy
            set.bits.or(pks.bits);
            return set;
        }
        for (PropertyKey key : collection) {
            set.bits.set(key.getId());
        }
        return set;
    }

    public static PropertyKeySet of(PropertyKey propertyKey) {
        PropertyKeySet set = new PropertyKeySet();
        set.bits.set(propertyKey.getId());
        return set;
    }

    public static PropertyKeySet of(PropertyKey... propertyKeys) {
        return ofCollection(Arrays.asList(propertyKeys));
    }

    @Override
    public int size() {
        return this.bits.cardinality();
    }

    @Override
    public boolean isEmpty() {
        return this.bits.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        if (!(o instanceof PropertyKey pk)) {
            return false;
        }
        return this.bits.get(pk.getId());
    }

    @Nonnull
    @Override
    public Iterator<PropertyKey> iterator() {
        return new PropertyKeyIterator();
    }

    @Nonnull
    @Override
    public Object[] toArray() {
        return toArray(new Object[0]);
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    @Override
    public <T> T[] toArray(@Nonnull T[] a) {
        T[] array = Arrays.copyOf(a, this.bits.cardinality());
        Iterator<PropertyKey> iter = iterator();
        for (int i = 0; i < array.length && iter.hasNext(); i++) {
            array[i] = (T) iter.next();
        }
        return array;
    }

    @Override
    public boolean add(PropertyKey propertyKey) {
        if (this.bits.get(propertyKey.getId())) {
            return false;
        }
        this.bits.set(propertyKey.getId());
        return true;
    }

    @Override
    public boolean remove(Object o) {
        if (!(o instanceof PropertyKey pk)) {
            return false;
        }
        if (!this.bits.get(pk.getId())) {
            return false;
        }
        this.bits.clear(pk.getId());
        return true;
    }

    @Override
    public boolean containsAll(@Nonnull Collection<?> c) {
        if (c instanceof PropertyKeySet pks) {
            return pks.bits.intersects(this.bits);
        }
        for (Object o : c) {
            if (!(o instanceof PropertyKey pk)) {
                return false;
            }
            if (!this.bits.get(pk.getId())) {
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean addAll(@Nonnull Collection<? extends PropertyKey> c) {
        int cardinality = this.bits.cardinality();
        if (c instanceof PropertyKeySet pks) {
            this.bits.or(pks.bits);
        } else {
            for (PropertyKey key : c) {
                this.bits.set(key.getId());
            }
        }
        return cardinality != this.bits.cardinality(); // if cardinality changed, this set was changed
    }

    @Override
    public boolean retainAll(@Nonnull Collection<?> c) {
        int cardinality = this.bits.cardinality();
        BitSet removal;
        if (c instanceof PropertyKeySet pks) {
            removal = pks.bits;
        } else {
            removal = new BitSet(this.bits.length());
            for (PropertyKey key : this) {
                if (!c.contains(key)) {
                    removal.set(key.getId());
                }
            }
        }
        this.bits.and(removal);
        return cardinality != this.bits.cardinality(); // if cardinality changed, this set was changed
    }

    @Override
    public boolean removeAll(@Nonnull Collection<?> c) {
        int cardinality = this.bits.cardinality();
        if (c instanceof PropertyKeySet pks) {
            this.bits.andNot(pks.bits);
        } else {
            for (Object o : c) { // mh
                if (o instanceof PropertyKey pk) {
                    this.bits.clear(pk.getId());
                }
            }
        }
        return cardinality != this.bits.cardinality(); // if cardinality changed, this set was changed
    }

    @Override
    public void clear() {
        this.bits.clear();
    }

    private class PropertyKeyIterator implements Iterator<PropertyKey> {

        private int current = bits.nextSetBit(0);

        @Override
        public boolean hasNext() {
            return this.current >= 0;
        }

        @Override
        public PropertyKey next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            PropertyKey next = PropertyKey.getById(this.current);
            this.current = bits.nextSetBit(this.current + 1);
            return next;
        }

    }

}
