package com.fastasyncworldedit.core.function.pattern;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.math.BlockVectorSet;
import com.fastasyncworldedit.core.math.LocalBlockVectorSet;
import com.fastasyncworldedit.core.util.FaweTimer;
import com.fastasyncworldedit.core.util.collection.BlockVector3Set;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;

import javax.annotation.Nullable;

public class BufferedPattern extends AbstractPattern implements ResettablePattern {

    protected final BlockVector3Set set;
    protected final FaweTimer timer;
    protected final long[] actionTime;

    protected final Pattern pattern;

    /**
     * Create a new {@link Pattern} instance
     *
     * @param actor  actor associated with the pattern
     * @param parent pattern to set
     */
    public BufferedPattern(Actor actor, Pattern parent) {
        this(actor, parent, null);
    }

    /**
     * Create a new {@link Pattern} instance
     *
     * @param actor    actor associated with the pattern
     * @param parent   pattern to set
     * @param areaSize anticipated size of the edit
     * @since TODO
     */
    public BufferedPattern(Actor actor, Pattern parent, @Nullable BlockVector3 areaSize) {
        long[] tmp = actor.getMeta("lastActionTime");
        if (tmp == null) {
            actor.setMeta("lastActionTime", tmp = new long[2]);
        }
        actionTime = tmp;
        this.pattern = parent;
        this.timer = Fawe.instance().getTimer();
        if (areaSize != null && (areaSize.getBlockX() > 2048 || areaSize.getBlockZ() > 2048)) {
            set = new BlockVectorSet();
        } else {
            set = new LocalBlockVectorSet();
        }
    }

    @Override
    public BaseBlock applyBlock(BlockVector3 position) {
        return pattern.applyBlock(position);
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        actionTime[1] = timer.getTick();
        if (!set(get)) {
            return false;
        }
        return pattern.apply(extent, get, set);
    }

    public boolean set(BlockVector3 pos) {
        return set.add(pos);
    }

    @Override
    public void reset() {
        long now = timer.getTick();
        if (now - actionTime[1] > 5) {
            set.clear();
        }
        actionTime[1] = actionTime[0];
        actionTime[0] = now;
    }

}
