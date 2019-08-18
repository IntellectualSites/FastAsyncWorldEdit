package com.boydti.fawe.bukkit.regions;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.boydti.fawe.bukkit.wrapper.AsyncWorld;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.regions.FaweMask;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.RegisteredListener;

import java.util.ArrayList;

public class FreeBuildRegion extends BukkitMaskManager {
    private final ArrayList<RegisteredListener> listeners;

    public FreeBuildRegion() {
        super("freebuild");
        this.listeners = new ArrayList<>();
        RegisteredListener[] listeners = BlockBreakEvent.getHandlerList().getRegisteredListeners();
        for (RegisteredListener listener : listeners) {
            if (listener.getPriority() == EventPriority.MONITOR) continue;
            if (!listener.isIgnoringCancelled()) continue;
            this.listeners.add(listener);
        }
    }

    @Override
    public boolean isExclusive() {
        return true;
    }

    @Override
    public FaweMask getMask(FawePlayer<Player> player, MaskType type) {
        if (type != MaskType.MEMBER) return null;
        ArrayList<RegisteredListener> currRegList = new ArrayList<>();
        for (RegisteredListener listener : this.listeners) {
            String name = listener.getPlugin().getName();
            if (!player.hasPermission("fawe.freebuild." + name.toLowerCase())) continue;
            currRegList.add(listener);
        }
        if (currRegList.isEmpty()) return null;
        RegisteredListener[] listeners = currRegList.toArray(new RegisteredListener[0]);

        World bukkitWorld = BukkitAdapter.adapt(player.toWorldEditPlayer().getWorld());
        AsyncWorld asyncWorld = AsyncWorld.wrap(bukkitWorld);

        BlockVector3 pos1 = BlockVector3.ZERO;
        BlockVector3 pos2 = BlockVector3.ZERO;

        AsyncBlock block = new AsyncBlock(asyncWorld, 0, 0, 0);
        BlockBreakEvent event = new BlockBreakEvent(block, BukkitAdapter.adapt(player.toWorldEditPlayer()));

        return new FaweMask(pos1, pos2) {

        @Override
            public boolean isValid(FawePlayer player, MaskType type) {
                return bukkitWorld == BukkitAdapter.adapt(player.toWorldEditPlayer().getWorld()) && type == MaskType.MEMBER;
            }

            @Override
            public Region getRegion() {
                return new CuboidRegion(BlockVector3.ZERO, BlockVector3.ZERO) {

                    @Override
                    public boolean contains(int x, int z) {
                        return contains(x, 127, z);
                    }

                    private int lastX = Integer.MIN_VALUE, lastZ = Integer.MIN_VALUE;
                    private boolean lastResult;

                    @Override
                    public boolean contains(int x, int y, int z) {
                        if (x == lastX && z == lastZ) return lastResult;
                        lastX = x;
                        lastZ = z;
                        event.setCancelled(false);
                        block.setPosition(x, y, z);
                        try {
                            synchronized (Bukkit.getPluginManager()) {
                                for (RegisteredListener listener : listeners) {
                                    listener.callEvent(event);
                                }
                            }
                        } catch (EventException e) {
                            throw new RuntimeException(e);
                        }
                        return lastResult = !event.isCancelled();
                    }
                };
            }
        };
    }
}
