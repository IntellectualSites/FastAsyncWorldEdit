package com.fastasyncworldedit.core.extent.transform;

import com.fastasyncworldedit.core.extent.ResettableExtent;
import com.fastasyncworldedit.core.math.random.SimpleRandom;
import com.fastasyncworldedit.core.math.random.TrueRandom;
import com.fastasyncworldedit.core.util.collection.RandomCollection;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Uses a random pattern of a weighted list of patterns.
 */
public class RandomTransform extends SelectTransform {

    private final SimpleRandom random;
    private final Map<ResettableExtent, Double> weights = new HashMap<>();

    private transient RandomCollection<ResettableExtent> collection;
    private transient LinkedHashSet<ResettableExtent> extents = new LinkedHashSet<>();

    public RandomTransform() {
        this(new TrueRandom());
    }

    /**
     * New instance
     *
     * @param random {@link SimpleRandom} used to choose between transforms, given weights
     */
    public RandomTransform(SimpleRandom random) {
        this.random = random;
    }

    @Override
    public AbstractDelegateExtent getExtent(int x, int y, int z) {
        return collection.next(x, y, z);
    }

    @Override
    public AbstractDelegateExtent getExtent(int x, int z) {
        return collection.next(x, 0, z);
    }

    @Override
    public ResettableExtent setExtent(Extent extent) {
        if (collection == null) {
            collection = RandomCollection.of(weights, random);
            extents = new LinkedHashSet<>(weights.keySet());
        }
        super.setExtent(extent);
        for (ResettableExtent current : extents) {
            current.setExtent(extent);
        }
        return this;
    }

    /**
     * Add a pattern to the weight list of patterns.
     * <p>
     * <p>The probability for the pattern added is chance / max where max is
     * the sum of the probabilities of all added patterns.</p>
     *
     * @param extent the extent
     * @param chance the chance, which can be any positive number
     */
    public void add(ResettableExtent extent, double chance) {
        checkNotNull(extent);
        weights.put(extent, chance);
        collection = RandomCollection.of(weights, random);
        this.extents.add(extent);
    }

    public Set<ResettableExtent> getExtents() {
        return extents;
    }

    public RandomCollection<ResettableExtent> getCollection() {
        return collection;
    }

}
