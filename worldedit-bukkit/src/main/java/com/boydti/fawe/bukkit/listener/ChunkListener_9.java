package com.boydti.fawe.bukkit.listener;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.util.FaweTimer;
import com.boydti.fawe.util.MathMan;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPhysicsEvent;

//
public class ChunkListener_9 extends ChunkListener {

    private Exception exception;
    private StackTraceElement[] elements;

    public ChunkListener_9() {
        super();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    @Override
    public void onPhysics(BlockPhysicsEvent event) {
        if (physicsFreeze) {
            event.setCancelled(true);
            return;
        }
        if (physCancel) {
            Block block = event.getBlock();
            long pair = MathMan.pairInt(block.getX() >> 4, block.getZ() >> 4);
            if (physCancelPair == pair) {
                event.setCancelled(true);
                return;
            }
            if (badChunks.containsKey(pair)) {
                physCancelPair = pair;
                event.setCancelled(true);
                return;
            }
            if (System.currentTimeMillis() - physStart > Settings.IMP.TICK_LIMITER.PHYSICS_MS) {
                physCancelPair = pair;
                event.setCancelled(true);
                return;
            }
        }
        FaweTimer timer = Fawe.get().getTimer();
        if (timer.getTick() != physTick) {
            physTick = timer.getTick();
            physStart = System.currentTimeMillis();
            physSkip = 0;
            physCancel = false;
            return;
        }
        if ((++physSkip & 1023) == 0) {
            if (System.currentTimeMillis() - physStart > Settings.IMP.TICK_LIMITER.PHYSICS_MS) {
                Block block = event.getBlock();
                int cx = block.getX() >> 4;
                int cz = block.getZ() >> 4;
                physCancelPair = MathMan.pairInt(cx, cz);
                if (rateLimit <= 0) {
                    rateLimit = 20;
                    lastCancelPos = block.getLocation();
                }
                cancelNearby(cx, cz);
                event.setCancelled(true);
                physCancel = true;
                return;
            }
        }
    }

    private StackTraceElement[] getElements(Exception ex) {
        if (elements == null || ex != exception) {
            exception = ex;
            elements = ex.getStackTrace();
        }
        return elements;
    }

    @Override
    protected int getDepth(Exception ex) {
        return getElements(ex).length;
    }

    @Override
    protected StackTraceElement getElement(Exception ex, int i) {
        StackTraceElement[] elems = getElements(ex);
        return elems.length > i ? elems[i] : null;
    }
}