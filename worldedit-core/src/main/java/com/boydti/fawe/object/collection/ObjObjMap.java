package com.boydti.fawe.object.collection;

import javax.annotation.Nonnull;
import java.util.Arrays;

import static it.unimi.dsi.fastutil.HashCommon.arraySize;

public class ObjObjMap<K, V> {
    private static final Object FREE_KEY = new Object();
    private static final Object REMOVED_KEY = new Object();

    /**
     * Keys and values
     */
    private Object[] m_data;

    /**
     * Value for the null key (if inserted into a map)
     */
    private Object m_nullValue;
    private boolean m_hasNull;

    /**
     * Fill factor, must be between (0 and 1)
     */
    private final float m_fillFactor;
    /**
     * We will resize a map once it reaches this size
     */
    private int m_threshold;
    /**
     * Current map size
     */
    private int m_size;
    /**
     * Mask to calculate the original position
     */
    private int m_mask;
    /**
     * Mask to wrap the actual array pointer
     */
    private int m_mask2;

    public ObjObjMap(final int size, final float fillFactor) {
        if (fillFactor <= 0 || fillFactor >= 1) {
            throw new IllegalArgumentException("FillFactor must be in (0, 1)");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive!");
        }
        final int capacity = arraySize(size, fillFactor);
        m_mask = capacity - 1;
        m_mask2 = capacity * 2 - 1;
        m_fillFactor = fillFactor;

        m_data = new Object[capacity * 2];
        Arrays.fill(m_data, FREE_KEY);

        m_threshold = (int) (capacity * fillFactor);
    }

    public V get(@Nonnull final K key) {
//        if ( key == null )
//            return (V) m_nullValue; //we null it on remove, so safe not to check a flag here

        int ptr = (key.hashCode() & m_mask) << 1;
        Object k = m_data[ptr];

//        if ( k == FREE_KEY )
//            return null;  //end of chain already
        if (k == (key)) //we check FREE and REMOVED prior to this call
        {
            return (V) m_data[ptr + 1];
        }
        while (true) {
            ptr = (ptr + 2) & m_mask2; //that's next index
            k = m_data[ptr];
//            if ( k == FREE_KEY )
//                return null;
            if (k == (key)) {
                return (V) m_data[ptr + 1];
            }
        }
    }

    public V put(final K key, final V value) {
        if (key == null) {
            return insertNullKey(value);
        }

        int ptr = getStartIndex(key) << 1;
        Object k = m_data[ptr];

        if (k == FREE_KEY) //end of chain already
        {
            m_data[ptr] = key;
            m_data[ptr + 1] = value;
            if (m_size >= m_threshold) {
                rehash(m_data.length * 2); //size is set inside
            } else {
                ++m_size;
            }
            return null;
        } else if (k == (key)) //we check FREE and REMOVED prior to this call
        {
            final Object ret = m_data[ptr + 1];
            m_data[ptr + 1] = value;
            return (V) ret;
        }

        int firstRemoved = -1;
        if (k == REMOVED_KEY) {
            firstRemoved = ptr; //we may find a key later
        }

        while (true) {
            ptr = (ptr + 2) & m_mask2; //that's next index calculation
            k = m_data[ptr];
            if (k == FREE_KEY) {
                if (firstRemoved != -1) {
                    ptr = firstRemoved;
                }
                m_data[ptr] = key;
                m_data[ptr + 1] = value;
                if (m_size >= m_threshold) {
                    rehash(m_data.length * 2); //size is set inside
                } else {
                    ++m_size;
                }
                return null;
            } else if (k == (key)) {
                final Object ret = m_data[ptr + 1];
                m_data[ptr + 1] = value;
                return (V) ret;
            } else if (k == REMOVED_KEY) {
                if (firstRemoved == -1) {
                    firstRemoved = ptr;
                }
            }
        }
    }

    public V remove(final K key) {
        if (key == null) {
            return removeNullKey();
        }

        int ptr = getStartIndex(key) << 1;
        Object k = m_data[ptr];
        if (k == FREE_KEY) {
            return null;  //end of chain already
        } else if (k == (key)) //we check FREE and REMOVED prior to this call
        {
            --m_size;
            if (m_data[(ptr + 2) & m_mask2] == FREE_KEY) {
                m_data[ptr] = FREE_KEY;
            } else {
                m_data[ptr] = REMOVED_KEY;
            }
            final V ret = (V) m_data[ptr + 1];
            m_data[ptr + 1] = null;
            return ret;
        }
        while (true) {
            ptr = (ptr + 2) & m_mask2; //that's next index calculation
            k = m_data[ptr];
            if (k == FREE_KEY) {
                return null;
            } else if (k == (key)) {
                --m_size;
                if (m_data[(ptr + 2) & m_mask2] == FREE_KEY) {
                    m_data[ptr] = FREE_KEY;
                } else {
                    m_data[ptr] = REMOVED_KEY;
                }
                final V ret = (V) m_data[ptr + 1];
                m_data[ptr + 1] = null;
                return ret;
            }
        }
    }

    private V insertNullKey(final V value) {
        if (m_hasNull) {
            final Object ret = m_nullValue;
            m_nullValue = value;
            return (V) ret;
        } else {
            m_nullValue = value;
            ++m_size;
            return null;
        }
    }

    private V removeNullKey() {
        if (m_hasNull) {
            final Object ret = m_nullValue;
            m_nullValue = null;
            m_hasNull = false;
            --m_size;
            return (V) ret;
        } else {
            return null;
        }
    }

    public int size() {
        return m_size;
    }

    private void rehash(final int newCapacity) {
        m_threshold = (int) (newCapacity / 2 * m_fillFactor);
        m_mask = newCapacity / 2 - 1;
        m_mask2 = newCapacity - 1;

        final int oldCapacity = m_data.length;
        final Object[] oldData = m_data;

        m_data = new Object[newCapacity];
        Arrays.fill(m_data, FREE_KEY);

        m_size = m_hasNull ? 1 : 0;

        for (int i = 0; i < oldCapacity; i += 2) {
            final Object oldKey = oldData[i];
            if (oldKey != FREE_KEY && oldKey != REMOVED_KEY) {
                put((K) oldKey, (V) oldData[i + 1]);
            }
        }
    }

    public int getStartIndex(final Object key) {
        //key is not null here
        return key.hashCode() & m_mask;
    }
}
