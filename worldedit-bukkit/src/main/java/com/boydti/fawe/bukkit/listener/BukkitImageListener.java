package com.boydti.fawe.bukkit.listener;

import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.bukkit.util.image.BukkitImageViewer;
import com.boydti.fawe.object.brush.BrushSettings;
import com.boydti.fawe.object.brush.visualization.cfi.HeightMapMCAGenerator;
import com.boydti.fawe.util.EditSessionBuilder;
import com.boydti.fawe.util.ExtentTraverser;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.util.image.ImageViewer;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.InvalidToolBindException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Rotation;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;

public class BukkitImageListener implements Listener {

    private Location mutable = new Location(Bukkit.getWorlds().get(0), 0, 0, 0);

    public BukkitImageListener(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    //TODO Fix along with CFI code 2020-02-04
//    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
//    public void onPlayerInteractEntity(AsyncPlayerChatEvent event) {
//        Set<Player> recipients = event.getRecipients();
//        Iterator<Player> iter = recipients.iterator();
//        while (iter.hasNext()) {
//            Player player = iter.next();
//            BukkitPlayer bukkitPlayer = BukkitAdapter.adapt(player);
//            CFICommands.CFISettings settings = bukkitPlayer.getMeta("CFISettings");
//            if (player.equals(event.getPlayer()) || !bukkitPlayer.hasMeta() || settings == null || !settings.hasGenerator()) {
//                continue;
//            }
//
//            String name = player.getName().toLowerCase();
//            if (!event.getMessage().toLowerCase().contains(name)) {
//                ArrayDeque<String> buffered = bukkitPlayer.getMeta("CFIBufferedMessages");
//                if (buffered == null) {
//                    bukkitPlayer.setMeta("CFIBufferedMessaged", buffered = new ArrayDeque<>());
//                }
//                String full = String.format(event.getFormat(), event.getPlayer().getDisplayName(),
//                    event.getMessage());
//                buffered.add(full);
//                iter.remove();
//            }
//        }
//    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
        if (!(event.getRemover() instanceof Player)) {
            return;
        }
        handleInteract(event, (Player) event.getRemover(), event.getEntity(), false);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        handleInteract(event, (Player) event.getDamager(), event.getEntity(), false);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.useItemInHand() == Event.Result.DENY) {
            return;
        }

        Player player = event.getPlayer();
        BukkitPlayer bukkitPlayer = BukkitAdapter.adapt(player);
        if (bukkitPlayer.getMeta("CFISettings") == null) {
            return;
        }
        try {
            if (event.getHand() == EquipmentSlot.OFF_HAND) {
                return;
            }
        } catch (NoSuchFieldError | NoSuchMethodError ignored) {
        }

        List<Block> target = player.getLastTwoTargetBlocks(null, 100);
        if (target.isEmpty()) {
            return;
        }

        Block targetBlock = target.get(0);
        World world = player.getWorld();
        mutable.setWorld(world);
        mutable.setX(targetBlock.getX() + 0.5);
        mutable.setY(targetBlock.getY() + 0.5);
        mutable.setZ(targetBlock.getZ() + 0.5);
        Collection<Entity> entities = world.getNearbyEntities(mutable, 0.46875, 0, 0.46875);

        if (!entities.isEmpty()) {
            Action action = event.getAction();
            boolean primary =
                action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;

            double minDist = Integer.MAX_VALUE;
            ItemFrame minItemFrame = null;

            for (Entity entity : entities) {
                if (entity instanceof ItemFrame) {
                    ItemFrame itemFrame = (ItemFrame) entity;
                    Location loc = itemFrame.getLocation();
                    double dx = loc.getX() - mutable.getX();
                    double dy = loc.getY() - mutable.getY();
                    double dz = loc.getZ() - mutable.getZ();
                    double dist = dx * dx + dy * dy + dz * dz;
                    if (dist < minDist) {
                        minItemFrame = itemFrame;
                        minDist = dist;
                    }
                }
            }
            if (minItemFrame != null) {
                handleInteract(event, minItemFrame, primary);
                if (event.isCancelled()) {
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        handleInteract(event, event.getRightClicked(), true);
    }

    private BukkitImageViewer get(HeightMapMCAGenerator generator) {
        if (generator == null) {
            return null;
        }

        ImageViewer viewer = generator.getImageViewer();
        if (!(viewer instanceof BukkitImageViewer)) {
            return null;
        }

        return (BukkitImageViewer) viewer;
    }

    private void handleInteract(PlayerEvent event, Entity entity, boolean primary) {
        handleInteract(event, event.getPlayer(), entity, primary);
    }

    private void handleInteract(Event event, Player player, Entity entity, boolean primary) {
        //todo fix with cfi code 2020-02-04
//        if (!(entity instanceof ItemFrame)) {
//            return;
//        }
//        ItemFrame itemFrame = (ItemFrame) entity;
//
//        BukkitPlayer bukkitPlayer = BukkitAdapter.adapt(player);
//        CFICommands.CFISettings settings = bukkitPlayer.getMeta("CFISettings");
//        HeightMapMCAGenerator generator = settings == null ? null : settings.getGenerator();
//        BukkitImageViewer viewer = get(generator);
//        if (viewer == null) {
//            return;
//        }
//
//        if (itemFrame.getRotation() != Rotation.NONE) {
//            itemFrame.setRotation(Rotation.NONE);
//        }
//
//        LocalSession session = bukkitPlayer.getSession();
//        BrushTool tool;
//        try {
//            tool = session.getBrushTool(bukkitPlayer, false);
//        } catch (InvalidToolBindException e) {
//            return;
//        }
//
//        ItemFrame[][] frames = viewer.getItemFrames();
//        if (frames == null || tool == null) {
//            viewer.selectFrame(itemFrame);
//            player.updateInventory();
//            TaskManager.IMP.laterAsync(() -> viewer.view(generator), 1);
//            return;
//        }
//
//        BrushSettings context = primary ? tool.getPrimary() : tool.getSecondary();
//        Brush brush = context.getBrush();
//        if (brush == null) {
//            return;
//        }
//        tool.setContext(context);
//
//        if (event instanceof Cancellable) {
//            ((Cancellable) event).setCancelled(true);
//        }
//
//        Location target = itemFrame.getLocation();
//        Location source = player.getLocation();
//
//        double yawRad = Math.toRadians(source.getYaw() + 90d);
//        double pitchRad = Math.toRadians(-source.getPitch());
//
//        double a = Math.cos(pitchRad);
//        double xRat = Math.cos(yawRad) * a;
//        double zRat = Math.sin(yawRad) * a;
//
//        BlockFace facing = itemFrame.getFacing();
//        double thickness = 1 / 32D + 1 / 128D;
//        double modX = facing.getModX();
//        double modZ = facing.getModZ();
//        double dx = source.getX() - target.getX() - modX * thickness;
//        double dy = source.getY() + player.getEyeHeight() - target.getY();
//        double dz = source.getZ() - target.getZ() - modZ * thickness;
//
//        double offset;
//        double localX;
//        if (modX != 0) {
//            offset = dx / xRat;
//            localX = (-modX) * (dz - offset * zRat);
//        } else {
//            offset = dz / zRat;
//            localX = (modZ) * (dx - offset * xRat);
//        }
//        double localY = dy - offset * Math.sin(pitchRad);
//        int localPixelX = (int) ((localX + 0.5) * 128);
//        int localPixelY = (int) ((localY + 0.5) * 128);
//
//        UUID uuid = itemFrame.getUniqueId();
//        for (int blockX = 0; blockX < frames.length; blockX++) {
//            for (int blockY = 0; blockY < frames[0].length; blockY++) {
//                if (uuid.equals(frames[blockX][blockY].getUniqueId())) {
//                    int pixelX = localPixelX + blockX * 128;
//                    int pixelY = (128 * frames[0].length) - (localPixelY + blockY * 128 + 1);
//
//                    int width = generator.getWidth();
//                    int length = generator.getLength();
//                    int worldX = (int) (pixelX * width / (frames.length * 128d));
//                    int worldZ = (int) (pixelY * length / (frames[0].length * 128d));
//
//                    if (worldX < 0 || worldX > width || worldZ < 0 || worldZ > length) {
//                        return;
//                    }
//
//                    bukkitPlayer.runAction(() -> {
//                        BlockVector3 wPos = BlockVector3.at(worldX, 0, worldZ);
//                        viewer.refresh();
//                        int topY = generator
//                            .getNearestSurfaceTerrainBlock(wPos.getBlockX(), wPos.getBlockZ(), 255,
//                                0, 255);
//                        wPos = wPos.withY(topY);
//
//                        EditSession es = new EditSessionBuilder(bukkitPlayer.getWorld()).player(bukkitPlayer)
//                            .combineStages(false).autoQueue(false).blockBag(null).limitUnlimited()
//                            .build();
//                        ExtentTraverser last = new ExtentTraverser(es.getExtent()).last();
//                        Extent extent = last.get();
//                        if (extent instanceof IQueueExtent) {
//                            last = last.previous();
//                        }
//                        last.setNext(generator);
//                        try {
//                            brush.build(es, wPos, context.getMaterial(), context.getSize());
//                        } catch (WorldEditException e) {
//                            e.printStackTrace();
//                        }
//                        es.flushQueue();
//                        viewer.view(generator);
//                    }, true, true);
//
//                    return;
//                }
//            }
//        }
    }
}
