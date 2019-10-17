package com.boydti.fawe.bukkit.listener;

import com.boydti.fawe.object.brush.MovableTool;
import com.boydti.fawe.object.brush.ResettableTool;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.command.tool.Tool;
import com.sk89q.worldedit.util.HandSide;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;

public class BrushListener implements Listener {
    public BrushListener(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if ((from.getYaw() != to.getYaw() &&  from.getPitch() != to.getPitch()) || from.getBlockX() != to.getBlockX() || from.getBlockZ() != to.getBlockZ() || from.getBlockY() != to.getBlockY()) {
            Player bukkitPlayer = event.getPlayer();
            com.sk89q.worldedit.entity.Player player = BukkitAdapter.adapt(bukkitPlayer);
            LocalSession session = player.getSession();
            Tool tool = session.getTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
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
            com.sk89q.worldedit.entity.Player player = BukkitAdapter.adapt(bukkitPlayer);
            LocalSession session = player.getSession();
            Tool tool = session.getTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
            if (tool instanceof ResettableTool) {
                if (((ResettableTool) tool).reset()) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
