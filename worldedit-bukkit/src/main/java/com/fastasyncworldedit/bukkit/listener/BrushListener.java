package com.fastasyncworldedit.bukkit.listener;

import com.fastasyncworldedit.core.command.tool.ResettableTool;
import com.fastasyncworldedit.core.command.tool.scroll.ScrollTool;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.command.tool.Tool;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.plugin.Plugin;

public class BrushListener implements Listener {

    public BrushListener(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemHoldEvent(final PlayerItemHeldEvent event) {
        final Player bukkitPlayer = event.getPlayer();
        if (bukkitPlayer.isSneaking()) {
            return;
        }
        BukkitPlayer player = BukkitAdapter.adapt(bukkitPlayer);
        LocalSession session = player.getSession();
        Tool tool = session.getTool(player);
        if (tool instanceof ScrollTool scrollable) {
            final int slot = event.getNewSlot();
            final int oldSlot = event.getPreviousSlot();
            final int ri;
            if ((((slot - oldSlot) <= 4) && ((slot - oldSlot) > 0)) || ((slot - oldSlot) < -4)) {
                ri = 1;
            } else {
                ri = -1;
            }
            if (scrollable.increment(player, ri)) {
                bukkitPlayer.getInventory().setHeldItemSlot(oldSlot);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        Player bukkitPlayer = event.getPlayer();
        if (bukkitPlayer.isSneaking()) {
            if (event.getAction() == Action.PHYSICAL) {
                return;
            }
            com.sk89q.worldedit.entity.Player player = BukkitAdapter.adapt(bukkitPlayer);
            LocalSession session = player.getSession();
            Tool tool = session.getTool(player);
            if (tool instanceof ResettableTool) {
                if (((ResettableTool) tool).reset()) {
                    event.setCancelled(true);
                }
            }
        }
    }

}
