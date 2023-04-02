package com.fastasyncworldedit.bukkit.listener;

import com.fastasyncworldedit.bukkit.FaweBukkit;
import com.fastasyncworldedit.core.util.FoliaSupport;
import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.util.FaweTimer;
import com.fastasyncworldedit.core.util.MathMan;
import com.fastasyncworldedit.core.util.TaskManager;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExpEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.NotePlayEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.Vector;

/**
 * @deprecated FAWE is not necessarily the tool you want to use to limit certain tick actions, e.g. fireworks or elytra flying.
 * The code is untouched since the 1.12 era and there is no guarantee that it will work or will be maintained in the future.
 */
@Deprecated(since = "2.0.0")
public abstract class ChunkListener implements Listener {

    private static final Logger LOGGER = LogManagerCompat.getLogger();
    protected int rateLimit = 0;
    protected Location lastCancelPos;
    private final int[] badLimit = new int[]{Settings.settings().TICK_LIMITER.PHYSICS_MS,
            Settings.settings().TICK_LIMITER.FALLING, Settings.settings().TICK_LIMITER.ITEMS};

    public ChunkListener() {
        if (Settings.settings().TICK_LIMITER.ENABLED) {
            PluginManager plm = Bukkit.getPluginManager();
            Plugin plugin = Fawe.<FaweBukkit>platform().getPlugin();
            plm.registerEvents(this, plugin);
            TaskManager.taskManager().repeat(() -> {
                Location tmpLoc = lastCancelPos;
                if (tmpLoc != null) {
                    LOGGER.info("[FAWE Tick Limiter] Detected and cancelled physics lag source at {}", tmpLoc);
                }
                rateLimit--;
                physicsFreeze = false;
                itemFreeze = false;
                lastZ = Integer.MIN_VALUE;
                physSkip = 0;
                physCancelPair = Long.MIN_VALUE;
                physCancel = false;
                lastCancelPos = null;

                counter.clear();
                for (Long2ObjectMap.Entry<Boolean> entry : badChunks.long2ObjectEntrySet()) {
                    long key = entry.getLongKey();
                    int x = MathMan.unpairIntX(key);
                    int z = MathMan.unpairIntY(key);
                    counter.put(key, badLimit);
                }
                badChunks.clear();
            }, Settings.settings().TICK_LIMITER.INTERVAL);
        }
    }

    protected abstract int getDepth(Exception ex);

    protected abstract StackTraceElement getElement(Exception ex, int index);

    /**
     * @deprecated see {@link com.fastasyncworldedit.bukkit.listener.ChunkListener} for an explanation of the deprecation
     */
    @Deprecated(since = "2.0.0")
    public static boolean physicsFreeze = false;
    /**
     * @deprecated see {@link com.fastasyncworldedit.bukkit.listener.ChunkListener} for an explanation of the deprecation
     */
    @Deprecated(since = "2.0.0")
    public static boolean itemFreeze = false;

    protected final Long2ObjectOpenHashMap<Boolean> badChunks = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<int[]> counter = new Long2ObjectOpenHashMap<>();
    private int lastX = Integer.MIN_VALUE;
    private int lastZ = Integer.MIN_VALUE;
    private int[] lastCount;

    /**
     * @deprecated see {@link com.fastasyncworldedit.bukkit.listener.ChunkListener} for an explanation of the deprecation
     */
    @Deprecated(since = "2.0.0")
    public int[] getCount(int cx, int cz) {
        if (lastX == cx && lastZ == cz) {
            return lastCount;
        }
        lastX = cx;
        lastZ = cz;
        long pair = MathMan.pairInt(cx, cz);
        int[] tmp = lastCount = counter.get(pair);
        if (tmp == null) {
            lastCount = tmp = new int[3];
            counter.put(pair, tmp);
        }
        return tmp;
    }

    /**
     * @deprecated see {@link com.fastasyncworldedit.bukkit.listener.ChunkListener} for an explanation of the deprecation
     */
    @Deprecated(since = "2.0.0")
    public void cleanup(Chunk chunk) {
        for (Entity entity : chunk.getEntities()) {
            if (entity.getType() == EntityType.DROPPED_ITEM) {
                entity.remove();
            }
        }

    }

    protected int physSkip;
    protected boolean physCancel;
    protected long physCancelPair;

    protected long physStart;
    protected long physTick;

    /**
     * @deprecated see {@link com.fastasyncworldedit.bukkit.listener.ChunkListener} for an explanation of the deprecation
     */
    @Deprecated(since = "2.0.0")
    public final void reset() {
        physSkip = 0;
        physStart = System.currentTimeMillis();
        physCancel = false;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockExplodeEvent event) {
        reset();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockBurnEvent event) {
        reset();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockCanBuildEvent event) {
        reset();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockDamageEvent event) {
        reset();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockDispenseEvent event) {
        reset();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockExpEvent event) {
        reset();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockFadeEvent event) {
        reset();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockFromToEvent event) {
        reset();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockGrowEvent event) {
        reset();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockIgniteEvent event) {
        reset();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockPlaceEvent event) {
        reset();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(FurnaceBurnEvent event) {
        reset();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(FurnaceSmeltEvent event) {
        reset();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(LeavesDecayEvent event) {
        reset();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(NotePlayEvent event) {
        reset();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(SignChangeEvent event) {
        reset();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockRedstoneEvent event) {
        reset();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
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
        } else {
            if ((++physSkip & 1023) != 0) {
                return;
            }
            FaweTimer timer = Fawe.instance().getTimer();
            if (timer.getTick() != physTick) {
                physTick = timer.getTick();
                physStart = System.currentTimeMillis();
                return;
            } else if (System.currentTimeMillis() - physStart
                    < Settings.settings().TICK_LIMITER.PHYSICS_MS) {
                return;
            }
        }
        Exception e = new Exception();
        int depth = getDepth(e);
        if (depth >= 256) {
            if (containsSetAir(e, event)) {
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
        physSkip = 1;
        physCancel = false;
    }

    protected boolean containsSetAir(Exception e, BlockPhysicsEvent event) {
        for (int frame = 25; frame < 35; frame++) {
            StackTraceElement elem = getElement(e, frame);
            if (elem != null) {
                String methodName = elem.getMethodName();
                // setAir | setTypeAndData (hacky, but this needs to be efficient)
                if (methodName.charAt(0) == 's' && methodName.length() == 6
                        || methodName.length() == 14) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void cancelNearby(int cx, int cz) {
        cancel(cx, cz);
        cancel(cx + 1, cz);
        cancel(cx - 1, cz);
        cancel(cx, cz + 1);
        cancel(cx, cz - 1);
        cancel(cx - 1, cz - 1);
        cancel(cx - 1, cz + 1);
        cancel(cx + 1, cz - 1);
        cancel(cx + 1, cz + 1);
    }

    private void cancel(int cx, int cz) {
        long key = MathMan.pairInt(cx, cz);
        badChunks.put(key, (Boolean) true);
        counter.put(key, badLimit);
        int[] count = getCount(cx, cz);
        count[0] = Integer.MAX_VALUE;
        count[1] = Integer.MAX_VALUE;
        count[2] = Integer.MAX_VALUE;

    }

    // Falling
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockChange(EntityChangeBlockEvent event) {
        if (physicsFreeze) {
            event.setCancelled(true);
            return;
        }
        Block block = event.getBlock();
        int x = block.getX();
        int z = block.getZ();
        int cx = x >> 4;
        int cz = z >> 4;
        int[] count = getCount(cx, cz);
        if (count[1] >= Settings.settings().TICK_LIMITER.FALLING) {
            event.setCancelled(true);
            return;
        }
        if (event.getEntityType() == EntityType.FALLING_BLOCK) {
            if (++count[1] >= Settings.settings().TICK_LIMITER.FALLING) {

                // Only cancel falling blocks when it's lagging
                if (Fawe.instance().getTimer().getTPS() < 18) {
                    cancelNearby(cx, cz);
                    if (rateLimit <= 0) {
                        rateLimit = 20;
                        lastCancelPos = block.getLocation();
                    }
                    event.setCancelled(true);
                } else {
                    count[1] = 0;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (physicsFreeze) {
            event.setCancelled(true);
            return;
        }
        Location loc = event.getLocation();
        int cx = loc.getBlockX() >> 4;
        int cz = loc.getBlockZ() >> 4;
        int[] count = getCount(cx, cz);
        if (count[2] >= Settings.settings().TICK_LIMITER.ITEMS) {
            event.setCancelled(true);
            return;
        }
        if (++count[2] >= Settings.settings().TICK_LIMITER.ITEMS) {
            cleanup(loc.getChunk());
            cancelNearby(cx, cz);
            if (rateLimit <= 0) {
                rateLimit = 20;
                LOGGER.warn(
                        "[FAWE `tick-limiter`] Detected and cancelled item lag source at {}", loc);
            }
            event.setCancelled(true);
        }
    }

}
