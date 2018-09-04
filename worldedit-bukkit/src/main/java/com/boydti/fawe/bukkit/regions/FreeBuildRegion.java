package com.boydti.fawe.bukkit.regions;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.boydti.fawe.bukkit.wrapper.AsyncWorld;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.queue.NullFaweQueue;
import com.boydti.fawe.regions.FaweMask;
import com.boydti.fawe.regions.FaweMaskManager;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredListener;

import java.util.ArrayList;

public class FreeBuildRegion extends BukkitMaskManager {
    private final ArrayList<RegisteredListener> listeners;

    public FreeBuildRegion() {
        super("freebuild");
        this.listeners = new ArrayList<>();
        RegisteredListener[] listeners = BlockPlaceEvent.getHandlerList().getRegisteredListeners();
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
        RegisteredListener[] listeners = currRegList.toArray(new RegisteredListener[currRegList.size()]);

        World world = player.parent.getWorld();
        AsyncWorld asyncWorld = AsyncWorld.wrap(world);

        Vector vec1 = asyncWorld.getMinimumPoint();
        Vector vec2 = asyncWorld.getMaximumPoint();
        Location pos1 = BukkitAdapter.adapt(world, vec1);
        Location pos2 = BukkitAdapter.adapt(world, vec2);

        AsyncBlock block = new AsyncBlock(asyncWorld, new NullFaweQueue(asyncWorld.getWorldName()), 0, 0, 0);
        BlockBreakEvent event = new BlockBreakEvent(block, player.parent);


        return new BukkitMask(pos1, pos2) {
            @Override
            public Region getRegion() {
                return new CuboidRegion(vec1, vec2) {
                    @Override
                    public boolean contains(int x, int y, int z) {
                        return contains(x, z);
                    }

                    private int lastX = Integer.MIN_VALUE, lastZ = Integer.MIN_VALUE;
                    private boolean lastResult;

                    @Override
                    public boolean contains(int x, int z) {
                        if (x == lastX && z == lastZ) return lastResult;
                        lastX = x;
                        lastZ = z;
                        int y = 128;
                        event.setCancelled(false);
                        block.setPosition(x, y, z);
                        try {
                            for (RegisteredListener listener : listeners) {
                                listener.callEvent(event);
                                if (event.isCancelled()) break;
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
