package com.fastasyncworldedit.nukkit.listener;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerItemHeldEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.math.BlockFace;
import com.fastasyncworldedit.core.command.tool.ResettableTool;
import com.fastasyncworldedit.core.command.tool.scroll.ScrollTool;
import com.fastasyncworldedit.nukkit.NukkitPlayer;
import com.fastasyncworldedit.nukkit.WorldEditNukkitPlugin;
import com.fastasyncworldedit.nukkit.adapter.NukkitAdapter;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.tool.Tool;
import com.sk89q.worldedit.event.platform.SessionIdleEvent;
import com.sk89q.worldedit.internal.event.InteractionDebouncer;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.Location;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Nukkit event listener for WorldEdit interactions.
 */
public class NukkitWorldEditListener implements Listener {

    private final WorldEditNukkitPlugin plugin;
    private final InteractionDebouncer debouncer;
    /**
     * Tracks players whose LEFT_CLICK_BLOCK was already handled by {@link #onPlayerInteract},
     * so that duplicate {@code PlayerInteractEvent} and {@code #onBlockBreak} can be skipped.
     * Entries expire after 1 seconds in case {@code BlockBreakEvent} is never fired.
     */
    private final Cache<Player, Boolean> handledLeftClick = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.SECONDS)
            .weakKeys()
            .build();
    private final Cache<Player, Integer> heldSlots = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .weakKeys()
            .build();

    public NukkitWorldEditListener(WorldEditNukkitPlugin plugin) {
        this.plugin = plugin;
        this.debouncer = new InteractionDebouncer(plugin.getInternalPlatform());
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

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!plugin.getInternalPlatform().isHookingEvents()) {
            return;
        }

        Player nukkitPlayer = event.getPlayer();
        NukkitPlayer player = NukkitAdapter.adapt(nukkitPlayer);
        WorldEdit we = WorldEdit.getInstance();

        if (resetToolIfSneaking(event, player)) {
            event.setCancelled(true);
            return;
        }

        // Skip duplicate interactions within the same tick (e.g. client click spam). Matches
        // Bukkit WorldEditListener; LEFT_CLICK_BLOCK is exempt as it carries block-specific state.
        if (event.getAction() != PlayerInteractEvent.Action.LEFT_CLICK_BLOCK) {
            Optional<Boolean> previousResult = debouncer.getDuplicateInteractionResult(player);
            if (previousResult.isPresent()) {
                if (previousResult.get()) {
                    event.setCancelled(true);
                }
                return;
            }
        }

        boolean result = false;
        switch (event.getAction()) {
            case LEFT_CLICK_BLOCK -> {
                if (handledLeftClick.getIfPresent(nukkitPlayer) != null) {
                    event.setCancelled(true);
                    return;
                }
                Block block = event.getBlock();
                Location loc = new Location(
                        player.getWorld(),
                        Vector3.at(block.getFloorX(), block.getFloorY(), block.getFloorZ())
                );
                Direction direction = adaptFace(event.getFace());
                // BrushTool is a TraceTool, not a BlockTool/DoubleActionBlockTool, so
                // handleBlockLeftClick (HIT) does not route to it. Fall back to handleArmSwing
                // (PRIMARY input) which is where brushes actually fire, matching Bukkit.
                result = we.handleBlockLeftClick(player, loc, direction) || we.handleArmSwing(player);
                if (result) {
                    handledLeftClick.put(nukkitPlayer, Boolean.TRUE);
                }
            }
            case LEFT_CLICK_AIR -> result = we.handleArmSwing(player);
            case RIGHT_CLICK_BLOCK -> {
                Block block = event.getBlock();
                Location loc = new Location(
                        player.getWorld(),
                        Vector3.at(block.getFloorX(), block.getFloorY(), block.getFloorZ())
                );
                Direction direction = adaptFace(event.getFace());
                // Same TraceTool fallback as LEFT_CLICK_BLOCK: handleBlockRightClick (OPEN) does
                // not route to BrushTool, so fall back to handleRightClick (SECONDARY input).
                result = we.handleBlockRightClick(player, loc, direction) || we.handleRightClick(player);
            }
            case RIGHT_CLICK_AIR -> result = we.handleRightClick(player);
            default -> {
                // PHYSICAL and other actions are not handled
            }
        }
        debouncer.setLastInteraction(player, result);
        if (result) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        if (!plugin.getInternalPlatform().isHookingEvents()) {
            return;
        }

        Player nukkitPlayer = event.getPlayer();
        int newSlot = event.getSlot();
        Integer cachedSlot = heldSlots.getIfPresent(nukkitPlayer);
        int oldSlot = cachedSlot != null ? cachedSlot : nukkitPlayer.getInventory().getHeldItemSlot();
        heldSlots.put(nukkitPlayer, newSlot);

        if (nukkitPlayer.isSneaking() || oldSlot == newSlot) {
            return;
        }

        NukkitPlayer player = NukkitAdapter.adapt(nukkitPlayer);
        Tool tool = player.getSession().getTool(player);
        if (tool instanceof ScrollTool scrollable) {
            int increment = (((newSlot - oldSlot) <= 4) && ((newSlot - oldSlot) > 0)) || ((newSlot - oldSlot) < -4)
                    ? 1
                    : -1;
            if (scrollable.increment(player, increment)) {
                event.setCancelled(true);
                nukkitPlayer.getInventory().setHeldItemSlot(oldSlot);
                heldSlots.put(nukkitPlayer, oldSlot);
            }
        }
    }

    private boolean resetToolIfSneaking(PlayerInteractEvent event, NukkitPlayer player) {
        if (!event.getPlayer().isSneaking() || event.getAction() == PlayerInteractEvent.Action.PHYSICAL) {
            return false;
        }
        LocalSession session = player.getSession();
        Tool tool = session.getTool(player);
        return tool instanceof ResettableTool resettable && resettable.reset();
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
        if (!plugin.getInternalPlatform().isHookingEvents()) {
            return;
        }

        Player nukkitPlayer = event.getPlayer();

        // Skip if already handled by PlayerInteractEvent(LEFT_CLICK_BLOCK)
        if (handledLeftClick.getIfPresent(nukkitPlayer) != null) {
            event.setCancelled(true);
            return;
        }

        NukkitPlayer player = NukkitAdapter.adapt(nukkitPlayer);
        Block block = event.getBlock();
        Location loc = new Location(
                player.getWorld(),
                Vector3.at(block.getFloorX(), block.getFloorY(), block.getFloorZ())
        );
        Direction direction = adaptFace(event.getFace());
        // Fall back to handleArmSwing so brushes (TraceTool) fire when only BlockBreakEvent is
        // emitted (e.g. creative/server-authoritative breaking), matching the PlayerInteractEvent
        // path and Bukkit behaviour.
        if (WorldEdit.getInstance().handleBlockLeftClick(player, loc, direction)
                || WorldEdit.getInstance().handleArmSwing(player)) {
            handledLeftClick.put(nukkitPlayer, Boolean.TRUE);
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.getInternalPlatform().isHookingEvents()) {
            return;
        }
        Player nukkitPlayer = event.getPlayer();
        // Hydrate session defaults (history limits, tool bindings) for the joining player. The
        // NukkitPlayer cache is a WeakHashMap keyed by the live Player object, so a fresh
        // NukkitPlayer is created automatically on relog.
        NukkitPlayer wePlayer = NukkitAdapter.adapt(nukkitPlayer);
        LocalSession session;
        if ((session = WorldEdit.getInstance().getSessionManager().getIfPresent(wePlayer)) != null) {
            session.loadDefaults(wePlayer, true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!plugin.getInternalPlatform().isHookingEvents()) {
            return;
        }

        Player nukkitPlayer = event.getPlayer();
        NukkitPlayer wePlayer = NukkitAdapter.adapt(nukkitPlayer);
        wePlayer.removePermissionAttachment();
        WorldEdit.getInstance().getEventBus().post(
                new SessionIdleEvent(new NukkitPlayer.SessionKeyImpl(nukkitPlayer))
        );
        NukkitAdapter.uncachePlayer(nukkitPlayer);
        heldSlots.invalidate(nukkitPlayer);
        debouncer.clearInteraction(wePlayer);
    }

}
