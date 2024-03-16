package com.fastasyncworldedit.core.extent;

import com.fastasyncworldedit.core.Fawe;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public final class SlowExtent extends AbstractDelegateExtent {

    private final long THRESHOLD = 50 * 1000000; // 1 tick
    private final long nanos;
    private long increment;

    public SlowExtent(Extent extent, long nanos) {
        super(extent);
        this.nanos = nanos;
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T block) throws WorldEditException {
        delay();
        return super.setBlock(x, y, z, block);
    }

    public void delay() {
        increment += nanos;
        if (increment >= THRESHOLD) {
            long wait = increment / 1000000;
            if (!Fawe.isMainThread()) {
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            increment -= wait * 1000000;
        }
    }

}
