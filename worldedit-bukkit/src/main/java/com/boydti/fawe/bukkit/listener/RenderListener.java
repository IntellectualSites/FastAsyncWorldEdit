package com.boydti.fawe.bukkit.listener;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.util.TaskManager;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;

public class RenderListener implements Listener {

    private final Map<UUID, int[]> views = new ConcurrentHashMap<>();
    private Iterator<Map.Entry<UUID, int[]>> entrySet;
    private int OFFSET = 6;

    public RenderListener(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        TaskManager.IMP.repeat(new Runnable() {
            private long last = 0;

            @Override
            public void run() {
                if (views.isEmpty()) return;

                long now = System.currentTimeMillis();
                int tps32 = (int) (Math.round(Fawe.get().getTimer().getTPS()) * 32);
                long diff = now - last;
                last = now;
                if (diff > 75) {
                    OFFSET = diff > 100 ? 0 : 4;
                    return;
                }
                int timeOut;
                if (diff < 55 && tps32 > 608) {
                    OFFSET = 8;
                    timeOut = 2;
                } else {
                    int tpsSqr = tps32 * tps32;
                    OFFSET = 1 + (tps32 / 102400);
                    timeOut = 162 - (tps32 / 2560);
                }
                if (entrySet == null || !entrySet.hasNext()) {
                    entrySet = views.entrySet().iterator();
                }
                int nowTick = (int) (Fawe.get().getTimer().getTick());
                while (entrySet.hasNext()) {
                    Map.Entry<UUID, int[]> entry = entrySet.next();
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player != null) {
                        int[] value = entry.getValue();
                        if (nowTick - value[1] >= timeOut) {
                            value[1] = nowTick + 1;
                            setViewDistance(player, Math.max(4, value[0] + 1));
                            long spent = System.currentTimeMillis() - now;
                            if (spent > 5) {
                                if (spent > 10)
                                    value[1] = nowTick + 20;
                                return;
                            }
                        }
                    }
                }
            }
        }, 1);
    }

    public void setViewDistance(Player player, int value) {
        UUID uuid = player.getUniqueId();
        if (value == Settings.IMP.EXPERIMENTAL.DYNAMIC_CHUNK_RENDERING) {
            views.remove(uuid);
        } else {
            int[] val = views.get(uuid);
            if (val == null) {
                val = new int[] {value, (int) Fawe.get().getTimer().getTick()};
                UUID uid = player.getUniqueId();
                views.put(uid, val);
            } else {
                if (value <= val[0]) {
                    val[1] = (int) Fawe.get().getTimer().getTick();
                }
                if (val[0] == value) {
                    return;
                } else {
                    val[0] = value;
                }
            }
        }
        throw new UnsupportedOperationException("TODO FIXME: PAPER 1.14");
//        player.setViewDistance(value);
    }

    public int getViewDistance(Player player) {
        int[] value = views.get(player.getUniqueId());
        return value == null ? Settings.IMP.EXPERIMENTAL.DYNAMIC_CHUNK_RENDERING : value[0];
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        setViewDistance(event.getPlayer(), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getBlockX() >> OFFSET != to.getBlockX() >> OFFSET || from.getBlockZ() >> OFFSET != to.getBlockZ() >> OFFSET) {
            Player player = event.getPlayer();
            int currentView = getViewDistance(player);
            setViewDistance(player, Math.max(currentView - 1, 1));
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Player player = event.getPlayer();
        setViewDistance(player, 1);
        FawePlayer fp = FawePlayer.wrap(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerLeave(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uid = player.getUniqueId();
        views.remove(uid);
    }
}