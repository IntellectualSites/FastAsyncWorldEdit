package com.boydti.fawe.bukkit.listener;

import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.brush.MovableTool;
import com.boydti.fawe.object.brush.ResettableTool;
import com.boydti.fawe.object.brush.scroll.ScrollTool;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.command.tool.Tool;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
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
        FawePlayer<Object> fp = FawePlayer.wrap(bukkitPlayer);
        com.sk89q.worldedit.entity.Player player = fp.getPlayer();
        LocalSession session = fp.getSession();
        Tool tool = session.getTool(player);
        if (tool instanceof ScrollTool) {
            final int slot = event.getNewSlot();
            final int oldSlot = event.getPreviousSlot();
            final int ri;
            if ((((slot - oldSlot) <= 4) && ((slot - oldSlot) > 0)) || (((slot - oldSlot) < -4))) {
                ri = 1;
            } else {
                ri = -1;
            }
            ScrollTool scrollable = (ScrollTool) tool;
            if (scrollable.increment(player, ri)) {
                if (Settings.IMP.EXPERIMENTAL.PERSISTENT_BRUSHES) {
                    bukkitPlayer.getInventory().setHeldItemSlot(oldSlot);
                } else {
                    final PlayerInventory inv = bukkitPlayer.getInventory();
                    final ItemStack item = inv.getItem(slot);
                    final ItemStack newItem = inv.getItem(oldSlot);
                    inv.setItem(slot, newItem);
                    inv.setItem(oldSlot, item);
                    bukkitPlayer.updateInventory();
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if ((from.getYaw() != to.getYaw() &&  from.getPitch() != to.getPitch()) || from.getBlockX() != to.getBlockX() || from.getBlockZ() != to.getBlockZ() || from.getBlockY() != to.getBlockY()) {
            Player bukkitPlayer = event.getPlayer();
            FawePlayer<Object> fp = FawePlayer.wrap(bukkitPlayer);
            com.sk89q.worldedit.entity.Player player = fp.getPlayer();
            LocalSession session = fp.getSession();
            Tool tool = session.getTool(player);
            if (tool != null) {
                if (tool instanceof MovableTool) {
                    ((MovableTool) tool).move(player);
                }
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
            FawePlayer<Object> fp = FawePlayer.wrap(bukkitPlayer);
            com.sk89q.worldedit.entity.Player player = fp.getPlayer();
            LocalSession session = fp.getSession();
            Tool tool = session.getTool(player);
            if (tool instanceof ResettableTool) {
                if (((ResettableTool) tool).reset()) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
