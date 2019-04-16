package com.boydti.fawe.bukkit.v0;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.bukkit.FaweBukkit;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.util.FaweTimer;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.TaskManager;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExpEvent;
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
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.Vector;

import java.lang.reflect.Method;
import java.util.List;

public abstract class ChunkListener implements Listener {

    protected int rateLimit = 0;
    private int[] badLimit = new int[]{Settings.IMP.TICK_LIMITER.PHYSICS_MS, Settings.IMP.TICK_LIMITER.FALLING, Settings.IMP.TICK_LIMITER.ITEMS};

    public ChunkListener() {
        if (Settings.IMP.TICK_LIMITER.ENABLED) {
            PluginManager plm = Bukkit.getPluginManager();
            Plugin plugin = Fawe.<FaweBukkit>imp().getPlugin();
            plm.registerEvents(this, plugin);
            try { plm.registerEvents(new ChunkListener_8Plus(this), plugin); } catch (Throwable ignore) {}
            TaskManager.IMP.repeat(() -> {
                rateLimit--;
                physicsFreeze = false;
                itemFreeze = false;
                lastZ = Integer.MIN_VALUE;
                physSkip = 0;
                physCancelPair = Long.MIN_VALUE;
                physCancel = false;

                counter.clear();
                for (Long2ObjectMap.Entry<Boolean> entry : badChunks.long2ObjectEntrySet()) {
                    long key = entry.getLongKey();
                    int x = MathMan.unpairIntX(key);
                    int z = MathMan.unpairIntY(key);
                    counter.put(key, badLimit);
                }
                badChunks.clear();
            }, Settings.IMP.TICK_LIMITER.INTERVAL);
        }
    }

    protected abstract int getDepth(Exception ex);
    protected abstract StackTraceElement getElement(Exception ex, int index);

    public static boolean physicsFreeze = false;
    public static boolean itemFreeze = false;

    protected final Long2ObjectOpenHashMap<Boolean> badChunks = new Long2ObjectOpenHashMap<>();
    private Long2ObjectOpenHashMap<int[]> counter = new Long2ObjectOpenHashMap<>();
    private int lastX = Integer.MIN_VALUE, lastZ = Integer.MIN_VALUE;
    private int[] lastCount;

    public int[] getCount(int cx, int cz) {
        if (lastX == cx && lastZ == cz) {
            return lastCount;
        }
        lastX = cx;
        lastZ = cz;
        long pair = MathMan.pairInt(cx, cz);
        int[] tmp = lastCount = counter.get(pair);
        if (tmp == null) {
            lastCount = tmp =  new int[3];
            counter.put(pair, tmp);
        }
        return tmp;
    }

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

    public final void reset() {
        physSkip = 0;
        physStart = System.currentTimeMillis();
        physCancel = false;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockBurnEvent event) { reset(); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockCanBuildEvent event) { reset(); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockDamageEvent event) { reset(); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockDispenseEvent event) { reset(); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockExpEvent event) { reset(); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockFadeEvent event) { reset(); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockFromToEvent event) { reset(); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockGrowEvent event) { reset(); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockIgniteEvent event) { reset(); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockPlaceEvent event) { reset(); }

//    @EventHandler(priority = EventPriority.LOWEST)
//    public void event(BrewEvent event) { reset(); }
//
//    @EventHandler(priority = EventPriority.LOWEST)
//    public void event(BrewingStandFuelEvent event) { reset(); }
//
//    @EventHandler(priority = EventPriority.LOWEST)
//    public void event(CauldronLevelChangeEvent event ) { reset(); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(FurnaceBurnEvent event) { reset(); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(FurnaceSmeltEvent event) { reset(); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(LeavesDecayEvent event) { reset(); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(NotePlayEvent event) { reset(); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(SignChangeEvent event) { reset(); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockRedstoneEvent event) { reset(); }

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
            if ((++physSkip & 1023) != 0) return;
            FaweTimer timer = Fawe.get().getTimer();
            if (timer.getTick() != physTick) {
                physTick = timer.getTick();
                physStart = System.currentTimeMillis();
                return;
            } else if (System.currentTimeMillis() - physStart < Settings.IMP.TICK_LIMITER.PHYSICS_MS) {
                return;
            }
        }
//        switch (event.getChangedType()) {
//            case AIR:
//            case CAVE_AIR:
//            case VOID_AIR:
//                break;
//            case REDSTONE_WIRE::
//                return;
//        }
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
                    Fawe.debug("[FAWE `tick-limiter`] Detected and cancelled physics  lag source at " + block.getLocation());
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
                if (methodName.charAt(0) == 's' && methodName.length() == 6 || methodName.length() == 14) {
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
        if (count[1] >= Settings.IMP.TICK_LIMITER.FALLING) {
            event.setCancelled(true);
            return;
        }
        if (event.getEntityType() == EntityType.FALLING_BLOCK) {
            if (++count[1] >= Settings.IMP.TICK_LIMITER.FALLING) {

                // Only cancel falling blocks when it's lagging
                if (Fawe.get().getTimer().getTPS() < 18) {
                    cancelNearby(cx, cz);
                    if (rateLimit <= 0) {
                        rateLimit = 20;
                        Fawe.debug("[FAWE `tick-limiter`] Detected and cancelled falling block lag source at " + block.getLocation());
                    }
                    event.setCancelled(true);
                    return;
                } else {
                    count[1] = 0;
                }
            }
        }
    }

    /**
     * Prevent FireWorks from loading chunks
     * @param event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!Settings.IMP.TICK_LIMITER.FIREWORKS_LOAD_CHUNKS) {
            Chunk chunk = event.getChunk();
            Entity[] entities = chunk.getEntities();
            World world = chunk.getWorld();

            Exception e = new Exception();
            int start = 14;
            int end = 22;
            int depth = Math.min(end, getDepth(e));

            for (int frame = start; frame < depth; frame++) {
                StackTraceElement elem = getElement(e, frame);
                if (elem == null) return;
                String className = elem.getClassName();
                int len = className.length();
                if (className != null) {
                    if (len > 15 && className.charAt(len - 15) == 'E' && className.endsWith("EntityFireworks")) {
                        for (Entity ent : world.getEntities()) {
                            if (ent.getType() == EntityType.FIREWORK) {
                                Vector velocity = ent.getVelocity();
                                double vertical = Math.abs(velocity.getY());
                                if (Math.abs(velocity.getX()) > vertical || Math.abs(velocity.getZ()) > vertical) {
                                    Fawe.debug("[FAWE `tick-limiter`] Detected and cancelled rogue FireWork at " + ent.getLocation());
                                    ent.remove();
                                }
                            }
                        }
                    }
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
        if (count[2] >= Settings.IMP.TICK_LIMITER.ITEMS) {
            event.setCancelled(true);
            return;
        }
        if (++count[2] >= Settings.IMP.TICK_LIMITER.ITEMS) {
            cleanup(loc.getChunk());
            cancelNearby(cx, cz);
            if (rateLimit <= 0) {
                rateLimit = 20;
                Fawe.debug("[FAWE `tick-limiter`] Detected and cancelled item lag source at " + loc);
            }
            event.setCancelled(true);
            return;
        }
    }
}
