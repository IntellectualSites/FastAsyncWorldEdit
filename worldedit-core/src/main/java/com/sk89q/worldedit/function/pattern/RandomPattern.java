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

package com.sk89q.worldedit.function.pattern;

import com.fastasyncworldedit.core.math.random.SimpleRandom;
import com.fastasyncworldedit.core.math.random.TrueRandom;
import com.fastasyncworldedit.core.util.collection.RandomCollection;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Uses a random pattern of a weighted list of patterns.
 */
public class RandomPattern extends AbstractPattern {

    //FAWE start - SimpleRandom > Random, LHS<P> > List
    private final SimpleRandom random;
    private Map<Pattern, Double> weights = new LinkedHashMap<>();
    private RandomCollection<Pattern> collection;
    private LinkedHashSet<Pattern> patterns = new LinkedHashSet<>();
    //FAWE end

    //FAWE start
    public RandomPattern() {
        this(new TrueRandom());
    }

    public RandomPattern(SimpleRandom random) {
        this.random = random;
    }

    /**
     * Create a random pattern from an existing one but with a different random.
     *
     * @param random the new random to use.
     * @param parent the existing random pattern.
     */
    public RandomPattern(SimpleRandom random, RandomPattern parent) {
        this.random = random;
        this.weights = parent.weights;
        this.collection = RandomCollection.of(weights, random);
        this.patterns = parent.patterns;
    }

    private RandomPattern(SimpleRandom random, Map<Pattern, Double> weights) {
        this.random = random;
        this.weights = weights;
        this.collection = RandomCollection.of(weights, random);
        this.patterns = new LinkedHashSet<>(weights.keySet());
    }
    //FAWE end

    /**
     * Add a pattern to the weight list of patterns.
     *
     * <p>The probability for the pattern added is chance / max where max is
     * the sum of the probabilities of all added patterns.</p>
     *
     * @param pattern the pattern
     * @param chance  the chance, which can be any positive number
     */
    public void add(Pattern pattern, double chance) {
        checkNotNull(pattern);
        //FAWE start - Double, weights, patterns and collection
        Double existingWeight = weights.get(pattern);
        if (existingWeight != null) {
            chance += existingWeight;
        }
        weights.put(pattern, chance);
        collection = RandomCollection.of(weights, random);
        this.patterns.add(pattern);
    }

    public Set<Pattern> getPatterns() {
        return patterns;
    }

    public RandomCollection<Pattern> getCollection() {
        return collection;
    }

    @Override
    public BaseBlock applyBlock(BlockVector3 position) {
        return collection.next(position.x(), position.y(), position.z()).applyBlock(position);
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        return collection.next(get.x(), get.y(), get.z()).apply(extent, get, set);
    }

    @Override
    public Pattern fork() {
        final LinkedHashMap<Pattern, Double> newWeights = new LinkedHashMap<>();
        this.weights.forEach((p, w) -> newWeights.put(p.fork(), w));
        return new RandomPattern(this.random, newWeights);
    }

    //FAWE end

}
