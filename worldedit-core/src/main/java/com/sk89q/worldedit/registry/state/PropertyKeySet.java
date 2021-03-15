package com.sk89q.worldedit.registry.state;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
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
        if (collection instanceof PropertyKeySet) {
            // simple copy
            set.bits.or(((PropertyKeySet) collection).bits);
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
        if (!(o instanceof PropertyKey)) return false;
        return this.bits.get(((PropertyKey) o).getId());
    }

    @NotNull
    @Override
    public Iterator<PropertyKey> iterator() {
        return new PropertyKeyIterator();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return toArray(new Object[0]);
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        T[] array = Arrays.copyOf(a, this.bits.cardinality());
        Iterator<PropertyKey> iter = iterator();
        for (int i = 0; i < array.length && iter.hasNext(); i++) {
            //noinspection unchecked
            array[i] = (T) iter.next();
        }
        return array;
    }

    @Override
    public boolean add(PropertyKey propertyKey) {
        if (this.bits.get(propertyKey.getId())) return false;
        this.bits.set(propertyKey.getId());
        return true;
    }

    @Override
    public boolean remove(Object o) {
        if (!(o instanceof PropertyKey)) return false;
        if (!this.bits.get(((PropertyKey) o).getId())) return false;
        this.bits.clear(((PropertyKey) o).getId());
        return true;
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        if (c instanceof PropertyKeySet) {
            return ((PropertyKeySet) c).bits.intersects(this.bits);
        }
        for (Object o : c) {
            if (!(o instanceof PropertyKey)) return false;
            if (!this.bits.get(((PropertyKey) o).getId())) return false;
        }
        return false;
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends PropertyKey> c) {
        int cardinality = this.bits.cardinality();
        if (c instanceof PropertyKeySet) {
            this.bits.or(((PropertyKeySet) c).bits);
        } else {
            for (PropertyKey key : c) {
                this.bits.set(key.getId());
            }
        }
        return cardinality != this.bits.cardinality(); // if cardinality changed, this set was changed
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        int cardinality = this.bits.cardinality();
        BitSet removal;
        if (c instanceof PropertyKeySet) {
            removal = ((PropertyKeySet) c).bits;
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
    public boolean removeAll(@NotNull Collection<?> c) {
        int cardinality = this.bits.cardinality();
        if (c instanceof PropertyKeySet) {
            this.bits.andNot(((PropertyKeySet) c).bits);
        } else {
            for (Object o : c) { // mh
                if (o instanceof PropertyKey) {
                    this.bits.clear(((PropertyKey) o).getId());
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
            if (!hasNext()) throw new NoSuchElementException();
            PropertyKey next = PropertyKey.getById(this.current);
            this.current = bits.nextSetBit(this.current + 1);
            return next;
        }
    }
}
