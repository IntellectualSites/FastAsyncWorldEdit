package com.boydti.fawe.bukkit.regions;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.boydti.fawe.bukkit.wrapper.AsyncWorld;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.queue.NullFaweQueue;
import com.boydti.fawe.regions.FaweMask;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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

        World bukkitWorld = player.parent.getWorld();
        AsyncWorld asyncWorld = AsyncWorld.wrap(bukkitWorld);

        BlockVector3 vec1 = BlockVector3.at(0, 0, 0);
        Location pos1 = BukkitAdapter.adapt(bukkitWorld, vec1);
        Location pos2 = BukkitAdapter.adapt(bukkitWorld, vec1);

        AsyncBlock block = new AsyncBlock(asyncWorld, new NullFaweQueue(asyncWorld.getWorldName(), BlockTypes.STONE.getDefaultState()), 0, 0, 0);
        BlockBreakEvent event = new BlockBreakEvent(block, player.parent);

        return new FaweMask(BukkitAdapter.adapt(pos1).toBlockPoint(), BukkitAdapter.adapt(pos2).toBlockPoint()) {

        @Override
            public boolean isValid(FawePlayer player, MaskType type) {
                return bukkitWorld == ((FawePlayer<Player>)player).parent.getWorld() && type == MaskType.MEMBER;
            }

            @Override
            public Region getRegion() {
                return new CuboidRegion(vec1, vec1) {

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
