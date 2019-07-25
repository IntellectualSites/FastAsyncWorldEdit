package com.boydti.fawe.object.pattern;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.collection.LocalBlockVectorSet;
import com.boydti.fawe.util.FaweTimer;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import java.util.UUID;

public class BufferedPattern extends AbstractPattern implements ResettablePattern {
    protected final LocalBlockVectorSet set = new LocalBlockVectorSet();
    protected final FaweTimer timer;
    protected final long[] actionTime;

    protected final Pattern pattern;
    protected final UUID uuid;

    public BufferedPattern(FawePlayer fp, Pattern parent) {
        this.uuid = fp.getUUID();
        long[] tmp = fp.getMeta("lastActionTime");
        if (tmp == null) fp.setMeta("lastActionTime", tmp = new long[2]);
        actionTime = tmp;
        this.pattern = parent;
        this.timer = Fawe.get().getTimer();
    }

    @Override
    public BaseBlock apply(BlockVector3 position) {
        return pattern.apply(position);
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
