package com.boydti.fawe.beta.implementation.filter;

import com.boydti.fawe.beta.implementation.filter.block.FilterBlock;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.function.mask.ABlockMask;
import com.sk89q.worldedit.util.Countable;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DistrFilter extends ForkedFilter<DistrFilter> {

    private final int[] counter = new int[BlockTypes.states.length];

    public DistrFilter() {
        super(null);
    }

    private DistrFilter(DistrFilter root) {
        super(root);
    }

    @Override
    public DistrFilter init() {
        return new DistrFilter(this);
    }

    @Override
    public void join(DistrFilter filter) {
        for (int i = 0; i < filter.counter.length; i++) {
            this.counter[i] += filter.counter[i];
        }
    }

    @Override
    public final void applyBlock(FilterBlock block) {
        counter[block.getOrdinal()]++;
    }

    public int getTotal(ABlockMask mask) {
        int total = 0;
        for (int i = 0; i < counter.length; i++) {
            int value = counter[i];
            if (value != 0 && mask.test(BlockTypes.states[i])) {
                total += value;
            }
        }
        return total;
    }

    public int getTotal() {
        return Arrays.stream(counter).sum();
    }

    public List<Countable<BlockState>> getDistribution() {
        final List<Countable<BlockState>> distribution = new ArrayList<>();
        for (int i = 0; i < counter.length; i++) {
            final int count = counter[i];
            if (count != 0) {
                distribution.add(new Countable<>(BlockTypes.states[i], count));
            }
        }
        Collections.sort(distribution);
        return distribution;
    }

    public List<Countable<BlockType>> getTypeDistribution() {
        final List<Countable<BlockType>> distribution = new ArrayList<>();
        int[] typeCounter = new int[BlockTypes.values.length];
        for (int i = 0; i < counter.length; i++) {
            final int count = counter[i];
            if (count != 0) {
                BlockState state = BlockTypes.states[i];
                typeCounter[state.getBlockType().getInternalId()] += count;
            }
        }
        for (int i = 0; i < typeCounter.length; i++) {
            final int count = typeCounter[i];
            if (count != 0) {
                distribution.add(new Countable<>(BlockTypes.values[i], count));
            }
        }
        Collections.sort(distribution);
        return distribution;
    }

    public void print(Actor actor, long size) {
        for (Countable c : getDistribution()) {
            final String name = c.getID().toString();
            final String str = String.format("%-7s (%.3f%%) %s",
                String.valueOf(c.getAmount()),
                c.getAmount() / (double) size * 100,
                name);
            actor.print(str);
        }
    }
}
