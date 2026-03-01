package com.sk89q.worldedit.nukkitmot;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.math.BlockFace;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.event.platform.SessionIdleEvent;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.Location;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Nukkit event listener for WorldEdit interactions.
 */
public class NukkitWorldEditListener implements Listener {

    private final WorldEditNukkitPlugin plugin;
    /**
     * Tracks players whose LEFT_CLICK_BLOCK was already handled by {@link #onPlayerInteract},
     * so that {@link #onBlockBreak} can skip duplicate processing for the same click.
     */
    private final Set<Player> handledLeftClick = Collections.newSetFromMap(new WeakHashMap<>());

    public NukkitWorldEditListener(WorldEditNukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player nukkitPlayer = event.getPlayer();
        NukkitPlayer player = NukkitAdapter.adapt(nukkitPlayer);
        WorldEdit we = WorldEdit.getInstance();

        PlayerInteractEvent.Action action = event.getAction();
        switch (action) {
            case LEFT_CLICK_BLOCK -> {
                Block block = event.getBlock();
                Location loc = new Location(
                        player.getWorld(),
                        Vector3.at(block.getFloorX(), block.getFloorY(), block.getFloorZ())
                );
                Direction direction = adaptFace(event.getFace());
                boolean handled = we.handleBlockLeftClick(player, loc, direction);
                if (handled) {
                    handledLeftClick.add(nukkitPlayer);
                    event.setCancelled(true);
                }
            }
            case LEFT_CLICK_AIR -> {
                if (we.handleArmSwing(player)) {
                    event.setCancelled(true);
                }
            }
            case RIGHT_CLICK_BLOCK -> {
                Block block = event.getBlock();
                Location loc = new Location(
                        player.getWorld(),
                        Vector3.at(block.getFloorX(), block.getFloorY(), block.getFloorZ())
                );
                Direction direction = adaptFace(event.getFace());
                boolean handled = we.handleBlockRightClick(player, loc, direction);
                if (handled) {
                    event.setCancelled(true);
                }
            }
            case RIGHT_CLICK_AIR -> {
                if (we.handleRightClick(player)) {
                    event.setCancelled(true);
                }
            }
            default -> {
                // PHYSICAL and other actions are not handled
            }
        }
    }

    /**
     * Handle block break events for left-click tool interaction.
     * <p>
     * When server authoritative block breaking is enabled (e.g. creative mode),
     * {@code PlayerInteractEvent(LEFT_CLICK_BLOCK)} is not fired — only {@code BlockBreakEvent} is.
     * This handler delegates to {@code handleBlockLeftClick} to follow the standard FAWE async path.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onBlockBreak(BlockBreakEvent event) {
        Player nukkitPlayer = event.getPlayer();

        // Skip if already handled by PlayerInteractEvent(LEFT_CLICK_BLOCK)
        if (handledLeftClick.remove(nukkitPlayer)) {
            event.setCancelled(true);
            return;
        }

        NukkitPlayer player = NukkitAdapter.adapt(nukkitPlayer);
        WorldEdit we = WorldEdit.getInstance();

        Block block = event.getBlock();
        Location loc = new Location(
                player.getWorld(),
                Vector3.at(block.getFloorX(), block.getFloorY(), block.getFloorZ())
        );
        Direction direction = adaptFace(event.getFace());
        boolean handled = we.handleBlockLeftClick(player, loc, direction);
        if (handled) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player nukkitPlayer = event.getPlayer();
        WorldEdit.getInstance().getEventBus().post(
                new SessionIdleEvent(new NukkitPlayer.SessionKeyImpl(nukkitPlayer))
        );
        NukkitAdapter.uncachePlayer(nukkitPlayer);
    }

    private static Direction adaptFace(BlockFace face) {
        if (face == null) {
            return Direction.UP;
        }
        return switch (face) {
            case DOWN -> Direction.DOWN;
            case UP -> Direction.UP;
            case NORTH -> Direction.NORTH;
            case SOUTH -> Direction.SOUTH;
            case WEST -> Direction.WEST;
            case EAST -> Direction.EAST;
        };
    }

}
