package com.sk89q.worldedit.function.pattern;

<<<<<<< HEAD
import com.boydti.fawe.object.collection.RandomCollection;
import com.boydti.fawe.object.random.SimpleRandom;
import com.boydti.fawe.object.random.TrueRandom;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.Extent;
=======
import static com.google.common.base.Preconditions.checkNotNull;

import com.sk89q.worldedit.math.BlockVector3;
>>>>>>> 399e0ad5... Refactor vector system to be cleaner
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Uses a random pattern of a weighted list of patterns.
 */
public class RandomPattern extends AbstractPattern {

    private final SimpleRandom random;
    private Map<Pattern, Double> weights = new HashMap<>();
    private RandomCollection<Pattern> collection;
    private LinkedHashSet<Pattern> patterns = new LinkedHashSet<>();

    public RandomPattern() {
        this(new TrueRandom());
    }

    public RandomPattern(SimpleRandom random) {
        this.random = random;
    }

    /**
     * Add a pattern to the weight list of patterns.
     * <p>
     * <p>The probability for the pattern added is chance / max where max is
     * the sum of the probabilities of all added patterns.</p>
     *
     * @param pattern the pattern
     * @param chance  the chance, which can be any positive number
     */
    public void add(Pattern pattern, double chance) {
        checkNotNull(pattern);
        Double existingWeight = weights.get(pattern);
        if (existingWeight != null) chance += existingWeight;
        weights.put(pattern, chance);
        collection = RandomCollection.of(weights, random);
        this.patterns.add(pattern);
    }

<<<<<<< HEAD
    public Set<Pattern> getPatterns() {
        return patterns;
=======
    @Override
    public BlockStateHolder apply(BlockVector3 position) {
        double r = random.nextDouble();
        double offset = 0;

        for (Chance chance : patterns) {
            if (r <= (offset + chance.getChance()) / max) {
                return chance.getPattern().apply(position);
            }
            offset += chance.getChance();
        }

        throw new RuntimeException("ProportionalFillPattern");
>>>>>>> 399e0ad5... Refactor vector system to be cleaner
    }

    public RandomCollection<Pattern> getCollection() {
        return collection;
    }

    @Override
    public BlockStateHolder apply(Vector get) {
        return collection.next(get.getBlockX(), get.getBlockY(), get.getBlockZ()).apply(get);
    }

    @Override
    public boolean apply(Extent extent, Vector set, Vector get) throws WorldEditException {
        return collection.next(get.getBlockX(), get.getBlockY(), get.getBlockZ()).apply(extent, set, get);
    }



}