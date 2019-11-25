package com.boydti.fawe.beta.filters;

import com.boydti.fawe.beta.FilterBlock;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.util.Countable;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CountFilter extends ForkedFilter<CountFilter> {
    private final int[] counter = new int[BlockTypes.states.length];

    public CountFilter() {
        super(null);
    }

    private CountFilter(CountFilter root) {
        super(root);
    }

    @Override
    public CountFilter init() {
        return new CountFilter(this);
    }

    @Override
    public void join(CountFilter filter) {
        for (int i = 0; i < filter.counter.length; i++) {
            this.counter[i] += filter.counter[i];
        }
    }

    /*
    Implementation
     */

    @Override
    public final void applyBlock(final FilterBlock block) {
        counter[block.getOrdinal()]++;
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

    public void print(final Actor actor, final long size) {
        for (final Countable c : getDistribution()) {
            final String name = c.getID().toString();
            final String str = String.format("%-7s (%.3f%%) %s",
                    String.valueOf(c.getAmount()),
                    c.getAmount() / (double) size * 100,
                    name);
            actor.print(str);
        }
    }
}
