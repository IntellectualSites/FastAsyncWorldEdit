package com.boydti.fawe.beta.test;

import com.boydti.fawe.beta.Filter;
import com.boydti.fawe.beta.FilterBlock;
import com.boydti.fawe.config.BBC;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.util.Countable;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CountFilter implements Filter {
    private int[] counter = new int[BlockTypes.states.length];

    @Override
    public void applyBlock(FilterBlock block) {
        counter[block.getOrdinal()]++;
    }

    public List<Countable<BlockState>> getDistribution() {
        List<Countable<BlockState>> distribution = new ArrayList<>();
        for (int i = 0; i < counter.length; i++) {
            int count = counter[i];
            if (count != 0) {
                distribution.add(new Countable<>(BlockTypes.states[i], count));
            }
        }
        Collections.sort(distribution);
        return distribution;
    }

    public void print(Actor actor, long size) {
        for (Countable c : getDistribution()) {
            String name = c.getID().toString();
            String str = String.format("%-7s (%.3f%%) %s",
                    String.valueOf(c.getAmount()),
                    c.getAmount() / (double) size * 100,
                    name);
            actor.print(BBC.getPrefix() + str);
        }
    }

    @Override
    public Filter fork() {
        return new CountFilter();
    }

    @Override
    public void join(Filter parent) {
        CountFilter other = (CountFilter) parent;
        for (int i = 0; i < counter.length; i++) {
            other.counter[i] += this.counter[i];
        }
    }
}
