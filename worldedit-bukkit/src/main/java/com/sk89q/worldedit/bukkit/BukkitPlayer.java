/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.bukkit;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.bukkit.FaweBukkit;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.util.StringUtil;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.extension.platform.AbstractPlayerActor;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.internal.cui.CUIEvent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.session.SessionKey;
import com.sk89q.worldedit.util.HandSide;
import com.sk89q.worldedit.util.formatting.WorldEditText;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.adapter.bukkit.TextAdapter;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.gamemode.GameMode;
import com.sk89q.worldedit.world.gamemode.GameModes;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BukkitPlayer extends AbstractPlayerActor {

    private Player player;
    private WorldEditPlugin plugin;

    public BukkitPlayer(Player player) {
        super(getExistingMap(WorldEditPlugin.getInstance(), player));
        this.plugin = WorldEditPlugin.getInstance();
        this.player = player;
    }

    public BukkitPlayer(WorldEditPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        init();
    }

    private void init() {
        if (Settings.IMP.CLIPBOARD.USE_DISK) {
            loadClipboardFromDisk();
        }
    }

    private static Map<String, Object> getExistingMap(WorldEditPlugin plugin, Player player) {
        BukkitPlayer cached = plugin.getCachedPlayer(player);
        if (cached != null) {
            return cached.getRawMeta();
        }
        return new ConcurrentHashMap<>();
    }

    @Override
    public UUID getUniqueId() {
        return player.getUniqueId();
    }

    @Override
    public BaseItemStack getItemInHand(HandSide handSide) {
        ItemStack itemStack = handSide == HandSide.MAIN_HAND
                ? getPlayer().getInventory().getItemInMainHand()
                : getPlayer().getInventory().getItemInOffHand();
        return BukkitAdapter.adapt(itemStack);
    }

    @Override
    public BaseBlock getBlockInHand(HandSide handSide) throws WorldEditException {
        ItemStack itemStack = handSide == HandSide.MAIN_HAND
                ? getPlayer().getInventory().getItemInMainHand()
                : getPlayer().getInventory().getItemInOffHand();
        return BukkitAdapter.asBlockState(itemStack).toBaseBlock();
    }

    @Override
    public String getName() {
        return player.getName();
    }

    @Override
    public String getDisplayName() {
        return getPlayer().getDisplayName();
    }

    @Override
    public void giveItem(BaseItemStack itemStack) {
        final PlayerInventory inv = getPlayer().getInventory();
        ItemStack newItem = BukkitAdapter.adapt(itemStack);
        if (itemStack.getType().getId().equalsIgnoreCase(WorldEdit.getInstance().getConfiguration().wandItem)) {
            inv.remove(newItem);
        }
        final ItemStack item = getPlayer().getInventory().getItemInMainHand();
        getPlayer().getInventory().setItemInMainHand(newItem);
        HashMap<Integer, ItemStack> overflow = inv.addItem(item);
        if (!overflow.isEmpty()) {
            TaskManager.IMP.sync(new RunnableVal<Object>() {
                @Override
                public void run(Object value) {
                    for (Map.Entry<Integer, ItemStack> entry : overflow.entrySet()) {
                        ItemStack stack = entry.getValue();
                        if (stack.getType() != Material.AIR && stack.getAmount() > 0) {
                            Item
                                dropped = getPlayer().getWorld().dropItem(getPlayer().getLocation(), stack);
                            PlayerDropItemEvent event = new PlayerDropItemEvent(player, dropped);
                            if (event.isCancelled()) {
                                dropped.remove();
                            }
                        }
                    }
                }
            });
        }
        getPlayer().updateInventory();
    }

    @Override
    public void printRaw(String msg) {
        for (String part : msg.split("\n")) {
            getPlayer().sendMessage(part);
        }
    }

    @Override
    public void print(String msg) {
        for (String part : msg.split("\n")) {
            getPlayer().sendMessage("\u00A7d" + part);
        }
    }

    @Override
    public void printDebug(String msg) {
        for (String part : msg.split("\n")) {
            getPlayer().sendMessage("\u00A77" + part);
        }
    }

    @Override
    public void printError(String msg) {
        for (String part : msg.split("\n")) {
            getPlayer().sendMessage("§c" + part);
        }
    }

    @Override
    public void print(Component component) {
        TextAdapter.sendComponent(getPlayer(), WorldEditText.format(component));
    }

    @Override
    public void setPosition(Vector3 pos, float pitch, float yaw) {
        org.bukkit.World world = getPlayer().getWorld();
        if (pos instanceof com.sk89q.worldedit.util.Location) {
            com.sk89q.worldedit.util.Location loc = (com.sk89q.worldedit.util.Location) pos;
            Extent extent = loc.getExtent();
            if (extent instanceof World) {
                world = Bukkit.getWorld(((World) extent).getName());
            }
        }
        getPlayer().teleport(new Location(world, pos.getX(), pos.getY(), pos.getZ(), yaw, pitch));
    }

    @Override
    public String[] getGroups() {
        return plugin.getPermissionsResolver().getGroups(getPlayer());
    }

    @Override
    public BlockBag getInventoryBlockBag() {
        return new BukkitPlayerBlockBag(getPlayer());
    }

    @Override
    public GameMode getGameMode() {
        return GameModes.get(getPlayer().getGameMode().name().toLowerCase(Locale.ROOT));
    }

    @Override
    public void setGameMode(GameMode gameMode) {
        getPlayer().setGameMode(org.bukkit.GameMode.valueOf(gameMode.getId().toUpperCase(Locale.ROOT)));
    }

    @Override
    public boolean hasPermission(String perm) {
        return (!plugin.getLocalConfiguration().noOpPermissions && player.isOp())
            || plugin.getPermissionsResolver().hasPermission(player.getWorld().getName(), player, perm);
    }

    @Override public boolean togglePermission(String permission) {
        if (this.hasPermission(permission)) {
            getPlayer().addAttachment(plugin).setPermission(permission, false);
            return false;
        } else {
            getPlayer().addAttachment(plugin).setPermission(permission, true);
            return true;
        }
    }

    @Override
    public void setPermission(String permission, boolean value) {
        /*
         *  Permissions are used to managing WorldEdit region restrictions
         *   - The `/wea` command will give/remove the required bypass permission
         */
        if (Fawe.<FaweBukkit>imp().getVault() == null || Fawe.<FaweBukkit> imp().getVault().permission == null) {
            getPlayer().addAttachment(Fawe.<FaweBukkit> imp().getPlugin()).setPermission(permission, value);
        } else if (value) {
            if (!Fawe.<FaweBukkit> imp().getVault().permission.playerAdd(player, permission)) {
                getPlayer().addAttachment(Fawe.<FaweBukkit> imp().getPlugin()).setPermission(permission, value);
            }
        } else if (!Fawe.<FaweBukkit>imp().getVault().permission.playerRemove(player, permission)) {
            getPlayer().addAttachment(Fawe.<FaweBukkit>imp().getPlugin()).setPermission(permission, value);
        }
    }

    @Override
    public World getWorld() {
        return BukkitAdapter.adapt(getPlayer().getWorld());
    }

    @Override
    public void dispatchCUIEvent(CUIEvent event) {
        String[] params = event.getParameters();
        String send = event.getTypeId();
        if (params.length > 0) {
            send = send + "|" + StringUtil.joinString(params, "|");
        }
        getPlayer().sendPluginMessage(plugin, WorldEditPlugin.CUI_PLUGIN_CHANNEL, send.getBytes(CUIChannelListener.UTF_8_CHARSET));
    }

    public Player getPlayer() {
        if (!player.isValid()) {
            Player tmp = Bukkit.getPlayer(getUniqueId());
            if (tmp != null) {
                player = tmp;
            }
        }
        return player;
    }

    @Override
    public boolean isAllowedToFly() {
        return getPlayer().getAllowFlight();
    }

    @Override
    public void setFlying(boolean flying) {
        getPlayer().setFlying(flying);
    }

    @Override
    public BaseEntity getState() {
        throw new UnsupportedOperationException("Cannot create a state from this object");
    }

    @Override
    public com.sk89q.worldedit.util.Location getLocation() {
        Location nativeLocation = getPlayer().getLocation();
        Vector3 position = BukkitAdapter.asVector(nativeLocation);
        return new com.sk89q.worldedit.util.Location(
                getWorld(),
                position,
                nativeLocation.getYaw(),
                nativeLocation.getPitch());
    }

    @Override
    public boolean setLocation(com.sk89q.worldedit.util.Location location) {
        return getPlayer().teleport(BukkitAdapter.adapt(location));
    }

    @Nullable
    @Override
    public <T> T getFacet(Class<? extends T> cls) {
        return null;
    }

    @Override
    public SessionKey getSessionKey() {
        return new SessionKeyImpl(getUniqueId(), getName());
    }

    private static class SessionKeyImpl implements SessionKey {
        // If not static, this will leak a reference

        private final UUID uuid;
        private final String name;

        private SessionKeyImpl(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }

        @Override
        public UUID getUniqueId() {
            return uuid;
        }

        @Nullable
        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isActive() {
            // This is a thread safe call on CraftBukkit because it uses a
            // CopyOnWrite list for the list of players, but the Bukkit
            // specification doesn't require thread safety (though the
            // spec is extremely incomplete)
            return Bukkit.getServer().getPlayer(uuid) != null;
        }

        @Override
        public boolean isPersistent() {
            return true;
        }

    }

    @Override
    public <B extends BlockStateHolder<B>> void sendFakeBlock(BlockVector3 pos, B block) {
        Location loc = new Location(getPlayer().getWorld(), pos.getX(), pos.getY(), pos.getZ());
        if (block == null) {
            getPlayer().sendBlockChange(loc, getPlayer().getWorld().getBlockAt(loc).getBlockData());
        } else {
            getPlayer().sendBlockChange(loc, BukkitAdapter.adapt(block));
            if (block instanceof BaseBlock && ((BaseBlock) block).hasNbtData()) {
                BukkitImplAdapter adapter = WorldEditPlugin.getInstance().getBukkitImplAdapter();
                if (adapter != null) {
                    adapter.sendFakeNBT(player, pos, ((BaseBlock) block).getNbtData());
                    if (block.getBlockType() == BlockTypes.STRUCTURE_BLOCK) {
                        adapter.sendFakeOP(player);
                    }
                }
            }
        }
    }

    @Override
    public void sendTitle(String title, String sub) {
        getPlayer().sendTitle(ChatColor.GOLD + title, ChatColor.GOLD + sub, 0, 70, 20);
        Bukkit.getServer().dispatchCommand(player, "title " + getName() + " subtitle [{\"text\":\"" + sub + "\",\"color\":\"gold\"}]");
        Bukkit.getServer().dispatchCommand(player, "title " + getName() + " title [{\"text\":\"" + title + "\",\"color\":\"gold\"}]");
    }

    @Override
    public void unregister() {
        getPlayer().removeMetadata("WE", WorldEditPlugin.getInstance());
    }
}
