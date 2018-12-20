package com.boydti.fawe.bukkit.util.image;

import com.boydti.fawe.util.image.Drawable;
import com.boydti.fawe.util.image.ImageUtil;
import com.boydti.fawe.util.image.ImageViewer;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collection;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Rotation;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
//import org.inventivetalent.mapmanager.MapManagerPlugin;
//import org.inventivetalent.mapmanager.controller.MapController;
//import org.inventivetalent.mapmanager.controller.MultiMapController;
//import org.inventivetalent.mapmanager.manager.MapManager;
//import org.inventivetalent.mapmanager.wrapper.MapWrapper;

public class BukkitImageViewer implements ImageViewer {
//    private final MapManager mapManager;
//    private final Player player;
//    private BufferedImage last;
    private ItemFrame[][] frames;
//    private boolean reverse;

    public BukkitImageViewer(Player player) {
//        mapManager = ((MapManagerPlugin) Bukkit.getPluginManager().getPlugin("MapManager")).getMapManager();
//        this.player = player;
    }
//
    public void selectFrame(ItemFrame start) {
//        Location pos1 = start.getLocation().clone();
//        Location pos2 = start.getLocation().clone();
//
//        BlockFace facing = start.getFacing();
//        int planeX = facing.getModX() == 0 ? 1 : 0;
//        int planeY = facing.getModY() == 0 ? 1 : 0;
//        int planeZ = facing.getModZ() == 0 ? 1 : 0;
//
//        ItemFrame[][] res = find(pos1, pos2, facing);
//        Location tmp;
//        while (true) {
//            if (res != null) {
//                frames = res;
//            }
//            tmp = pos1.clone().subtract(planeX, planeY, planeZ);
//            if ((res = find(tmp, pos2, facing)) != null) {
//                pos1 = tmp;
//                continue;
//            }
//            tmp = pos2.clone().add(planeX, planeY, planeZ);
//            if ((res = find(pos1, tmp, facing)) != null) {
//                pos2 = tmp;
//                continue;
//            }
//            tmp = pos1.clone().subtract(planeX, 0, planeZ);
//            if ((res = find(tmp, pos2, facing)) != null) {
//                pos1 = tmp;
//                continue;
//            }
//            tmp = pos2.clone().add(planeX, 0, planeZ);
//            if ((res = find(pos1, tmp, facing)) != null) {
//                pos2 = tmp;
//                continue;
//            }
//            tmp = pos1.clone().subtract(0, 1, 0);
//            if ((res = find(tmp, pos2, facing)) != null) {
//                pos1 = tmp;
//                continue;
//            }
//            tmp = pos2.clone().add(0, 1, 0);
//            if ((res = find(pos1, tmp, facing)) != null) {
//                pos2 = tmp;
//                continue;
//            }
//            break;
//        }
    }
//
    public ItemFrame[][] getItemFrames() {
        return frames;
    }
//
//    private ItemFrame[][] find(Location pos1, Location pos2, BlockFace facing) {
//        try {
//            Location distance = pos2.clone().subtract(pos1).add(1, 1, 1);
//            int width = Math.max(distance.getBlockX(), distance.getBlockZ());
//            ItemFrame[][] frames = new ItemFrame[width][distance.getBlockY()];
//
//            World world = pos1.getWorld();
//
//            this.reverse = (facing == BlockFace.NORTH || facing == BlockFace.EAST);
//            int v = 0;
//            for (double y = pos1.getY(); y <= pos2.getY(); y++, v++) {
//                int h = 0;
//                for (double z = pos1.getZ(); z <= pos2.getZ(); z++) {
//                    for (double x = pos1.getX(); x <= pos2.getX(); x++, h++) {
//                        Location pos = new Location(world, x, y, z);
//                        Collection<Entity> entities = world.getNearbyEntities(pos, 0.1, 0.1, 0.1);
//                        boolean contains = false;
//                        for (Entity ent : entities) {
//                            if (ent instanceof ItemFrame && ((ItemFrame) ent).getFacing() == facing) {
//                                ItemFrame itemFrame = (ItemFrame) ent;
//                                itemFrame.setRotation(Rotation.NONE);
//                                contains = true;
//                                frames[reverse ? width - 1 - h : h][v] = (ItemFrame) ent;
//                                break;
//                            }
//                        }
//                        if (!contains) return null;
//                    }
//                }
//            }
//            return frames;
//        } catch (Throwable e) {
//            e.printStackTrace();
//        }
//        return null;
//    }

    @Override
    public void view(Drawable drawable) {
//        view(null, drawable);
    }
//
//    private void view(@Nullable BufferedImage image, @Nullable Drawable drawable) {
//        if (image == null && drawable == null) throw new IllegalArgumentException("An image or drawable must be provided. Both cannot be null");
//        boolean initializing = last == null;
//
//        if (this.frames != null) {
//            if (image == null && drawable != null) image = drawable.draw();
//            last = image;
//            int width = frames.length;
//            int height = frames[0].length;
//            BufferedImage scaled = ImageUtil.getScaledInstance(image, 128 * width, 128 * height, RenderingHints.VALUE_INTERPOLATION_BILINEAR, false);
//            MapWrapper mapWrapper = mapManager.wrapMultiImage(scaled, width, height);
//            MultiMapController controller = (MultiMapController) mapWrapper.getController();
//            controller.addViewer(player);
//            controller.sendContent(player);
//            controller.showInFrames(player, frames, true);
//        } else {
//            int slot = getMapSlot(player);
//            if (slot == -1) {
//                if (initializing) {
//                    player.getInventory().setItemInMainHand(new ItemStack(Material.MAP));
//                } else {
//                    return;
//                }
//            } else if (player.getInventory().getHeldItemSlot() != slot) {
//                player.getInventory().setHeldItemSlot(slot);
//            }
//            if (image == null && drawable != null) image = drawable.draw();
//            last = image;
//            BufferedImage scaled = ImageUtil.getScaledInstance(image, 128, 128, RenderingHints.VALUE_INTERPOLATION_BILINEAR, false);
//            MapWrapper mapWrapper = mapManager.wrapImage(scaled);
//            MapController controller = mapWrapper.getController();
//            controller.addViewer(player);
//            controller.sendContent(player);
//            controller.showInHand(player, true);
//        }
//    }
//
//    private int getMapSlot(Player player) {
//        PlayerInventory inventory = player.getInventory();
//        for (int i = 0; i < 9; i++) {
//            ItemStack item = inventory.getItem(i);
//            if (item != null && item.getType() == Material.MAP) {
//                return i;
//            }
//        }
//        return -1;
//    }
//
    public void refresh() {
//        if (last != null) view(last, null);
    }

    @Override
    public void close() throws IOException {
//        last = null;
    }
}