package com.boydti.fawe.object.pattern;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.collection.LocalBlockVectorSet;
import com.boydti.fawe.util.FaweTimer;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.io.IOException;
import java.util.UUID;

public class BufferedPattern extends AbstractPattern implements ResettablePattern {
    protected transient LocalBlockVectorSet set = new LocalBlockVectorSet();
    protected transient FaweTimer timer;
    protected transient long[] actionTime;

    protected final Pattern pattern;
    protected final UUID uuid;

    public BufferedPattern(FawePlayer fp, Pattern parent) {
        this.uuid = fp.getUUID();
        this.actionTime = fp.getMeta("lastActionTime");
        if (actionTime == null) fp.setMeta("lastActionTime", actionTime = new long[2]);
        this.pattern = parent;
        this.timer = Fawe.get().getTimer();
    }

    @Override
    public BlockStateHolder apply(Vector position) {
        return pattern.apply(position);
    }

    @Override
    public boolean apply(Extent extent, Vector setPosition, Vector getPosition) throws WorldEditException {
        long now = timer.getTick();
        try {
            if (!set(setPosition)) {
                return false;
            }
            return pattern.apply(extent, setPosition, getPosition);
        } catch (UnsupportedOperationException ignore) {
        }
        return false;
    }

    public boolean set(Vector pos) {
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

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        set = new LocalBlockVectorSet();
        timer = Fawe.get().getTimer();
        FawePlayer fp = Fawe.get().getCachedPlayer(uuid);
        if (fp != null) {
            this.actionTime = fp.getMeta("lastActionTime");
            if (actionTime == null) fp.setMeta("lastActionTime", actionTime = new long[2]);
        } else {
            actionTime = new long[2];
        }
    }
}
