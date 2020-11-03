/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.bukkit;

import com.boydti.fawe.config.Caption;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.util.StringUtil;
import com.sk89q.wepif.VaultResolver;
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
import com.sk89q.worldedit.util.formatting.component.TextUtils;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.sk89q.worldedit.util.formatting.text.adapter.bukkit.TextAdapter;
import com.sk89q.worldedit.util.formatting.text.event.ClickEvent;
import com.sk89q.worldedit.util.formatting.text.format.TextColor;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.gamemode.GameMode;
import com.sk89q.worldedit.world.gamemode.GameModes;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.permissions.PermissionAttachment;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BukkitPlayer extends AbstractPlayerActor {

    private final Player player;
    private final WorldEditPlugin plugin;
    private final PermissionAttachment permAttachment;
    /**
     * This constructs a new {@link BukkitPlayer} for the given {@link Player}.
     *
     * @param player The corresponding {@link Player} or null if you need a null WorldEdit player for some reason.
     */
    public BukkitPlayer(@Nullable Player player) {
        super(player != null ? getExistingMap(WorldEditPlugin.getInstance(), player) : new ConcurrentHashMap<>());
        this.plugin = WorldEditPlugin.getInstance();
        this.player = player;
        this.permAttachment = plugin.getPermissionAttachmentManager().getOrAddAttachment(player);
    }

    /**
     * This constructs a new {@link BukkitPlayer} for the given {@link Player}.
     *
     * @param plugin The running instance of {@link WorldEditPlugin}
     * @param player The corresponding {@link Player} or null if you need a null WorldEdit player for some reason.
     */
    public BukkitPlayer(@Nonnull WorldEditPlugin plugin, @Nullable Player player) {
        this.plugin = plugin;
        this.player = player;
        this.permAttachment = plugin.getPermissionAttachmentManager().getOrAddAttachment(player);
        if (player != null && Settings.IMP.CLIPBOARD.USE_DISK) {
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
                ? player.getInventory().getItemInMainHand()
                : player.getInventory().getItemInOffHand();
        return BukkitAdapter.adapt(itemStack);
    }

    @Override
    public BaseBlock getBlockInHand(HandSide handSide) throws WorldEditException {
        ItemStack itemStack = handSide == HandSide.MAIN_HAND
                ? player.getInventory().getItemInMainHand()
                : player.getInventory().getItemInOffHand();
        return BukkitAdapter.asBlockState(itemStack).toBaseBlock();
    }

    @Override
    public String getName() {
        return player.getName();
    }

    @Override
    public String getDisplayName() {
        return player.getDisplayName();
    }

    @Override
    public void giveItem(BaseItemStack itemStack) {
        final PlayerInventory inv = player.getInventory();
        ItemStack newItem = BukkitAdapter.adapt(itemStack);
        if (itemStack.getType().getId().equalsIgnoreCase(WorldEdit.getInstance().getConfiguration().wandItem)) {
            inv.remove(newItem);
        }
        final ItemStack item = player.getInventory().getItemInMainHand();
        player.getInventory().setItemInMainHand(newItem);
        HashMap<Integer, ItemStack> overflow = inv.addItem(item);
        if (!overflow.isEmpty()) {
            TaskManager.IMP.sync(new RunnableVal<Object>() {
                @Override
                public void run(Object value) {
                    for (Map.Entry<Integer, ItemStack> entry : overflow.entrySet()) {
                        ItemStack stack = entry.getValue();
                        if (stack.getType() != Material.AIR && stack.getAmount() > 0) {
                            Item
                                dropped = player.getWorld().dropItem(player.getLocation(), stack);
                            PlayerDropItemEvent event = new PlayerDropItemEvent(player, dropped);
                            Bukkit.getPluginManager().callEvent(event);
                            if (event.isCancelled()) {
                                dropped.remove();
                            }
                        }
                    }
                }
            });
        }
        player.updateInventory();
    }

    @Override
    public void printRaw(String msg) {
        for (String part : msg.split("\n")) {
            player.sendMessage(part);
        }
    }

    @Override
    public void print(String msg) {
        for (String part : msg.split("\n")) {
            player.sendMessage("§d" + part);
        }
    }

    @Override
    public void printDebug(String msg) {
        for (String part : msg.split("\n")) {
            player.sendMessage("§7" + part);
        }
    }

    @Override
    public void printError(String msg) {
        for (String part : msg.split("\n")) {
            player.sendMessage("§c" + part);
        }
    }

    @Override
    public void print(Component component) {
        component = Caption.color(TranslatableComponent.of("prefix", component), getLocale());
        TextAdapter.sendComponent(player, component);
    }

    @Override
    public boolean trySetPosition(Vector3 pos, float pitch, float yaw) {
        org.bukkit.World world = player.getWorld();
        if (pos instanceof com.sk89q.worldedit.util.Location) {
            com.sk89q.worldedit.util.Location loc = (com.sk89q.worldedit.util.Location) pos;
            Extent extent = loc.getExtent();
            if (extent instanceof World) {
                world = Bukkit.getWorld(((World) extent).getName());
            }
        }
        org.bukkit.World finalWorld = world;
        return TaskManager.IMP.sync(() -> player.teleport(new Location(finalWorld, pos.getX(), pos.getY(), pos.getZ(), yaw, pitch)));
    }

    @Override
    public String[] getGroups() {
        return plugin.getPermissionsResolver().getGroups(player);
    }

    @Override
    public BlockBag getInventoryBlockBag() {
        return new BukkitPlayerBlockBag(player);
    }

    @Override
    public GameMode getGameMode() {
        return GameModes.get(player.getGameMode().name().toLowerCase(Locale.ROOT));
    }

    @Override
    public void setGameMode(GameMode gameMode) {
        player.setGameMode(org.bukkit.GameMode.valueOf(gameMode.getId().toUpperCase(Locale.ROOT)));
    }

    @Override
    public boolean hasPermission(String perm) {
        return (!plugin.getLocalConfiguration().noOpPermissions && player.isOp())
                || plugin.getPermissionsResolver().hasPermission(
                    player.getWorld().getName(), player, perm);
    }

    @Override
    public void setPermission(String permission, boolean value) {
        /*
         *  Permissions are used to managing WorldEdit region restrictions
         *   - The `/wea` command will give/remove the required bypass permission
         */
        boolean usesuperperms = VaultResolver.perms == null;
        if (VaultResolver.perms != null) {
            if (value) {
                if (!VaultResolver.perms.playerAdd(player, permission)) {
                    usesuperperms = true;
                }
            } else {
                if (!VaultResolver.perms.playerRemove(player, permission)) {
                    usesuperperms = true;
                }
            }
        }
        if (usesuperperms) {
            permAttachment.setPermission(permission, value);
        }
    }

    @Override
    public World getWorld() {
        return BukkitAdapter.adapt(player.getWorld());
    }

    @Override
    public void dispatchCUIEvent(CUIEvent event) {
        String[] params = event.getParameters();
        String send = event.getTypeId();
        if (params.length > 0) {
            send = send + "|" + StringUtil.joinString(params, "|");
        }
        player.sendPluginMessage(plugin, WorldEditPlugin.CUI_PLUGIN_CHANNEL, send.getBytes(CUIChannelListener.UTF_8_CHARSET));
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public boolean isAllowedToFly() {
        return player.getAllowFlight();
    }

    @Override
    public void setFlying(boolean flying) {
        player.setFlying(flying);
    }

    @Override
    public BaseEntity getState() {
        throw new UnsupportedOperationException("Cannot create a state from this object");
    }

    @Override
    public com.sk89q.worldedit.util.Location getLocation() {
        Location nativeLocation = player.getLocation();
        Vector3 position = BukkitAdapter.asVector(nativeLocation);
        return new com.sk89q.worldedit.util.Location(
                getWorld(),
                position,
                nativeLocation.getYaw(),
                nativeLocation.getPitch());
    }

    @Override
    public boolean setLocation(com.sk89q.worldedit.util.Location location) {
        return player.teleport(BukkitAdapter.adapt(location));
    }

    @Override
    public Locale getLocale() {
        return TextUtils.getLocaleByMinecraftTag(player.getLocale());
    }

    @Override
    public void sendAnnouncements() {
        if (WorldEditPlugin.getInstance().getBukkitImplAdapter() == null) {
            printError(TranslatableComponent.of("worldedit.version.bukkit.unsupported-adapter",
                    TextComponent.of("https://intellectualsites.github.io/download/fawe.html", TextColor.AQUA)
                        .clickEvent(ClickEvent.openUrl("https://intellectualsites.github.io/download/fawe.html"))));
        }
    }

    @Nullable
    @Override
    public <T> T getFacet(Class<? extends T> cls) {
        return null;
    }

    @Override
    public SessionKey getSessionKey() {
        return new SessionKeyImpl(this.player.getUniqueId(), player.getName());
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
        Location loc = new Location(player.getWorld(), pos.getX(), pos.getY(), pos.getZ());
        if (block == null) {
            player.sendBlockChange(loc, player.getWorld().getBlockAt(loc).getBlockData());
        } else {
            player.sendBlockChange(loc, BukkitAdapter.adapt(block));
            if (block instanceof BaseBlock && ((BaseBlock) block).hasNbtData()) {
                BukkitImplAdapter adapter = WorldEditPlugin.getInstance().getBukkitImplAdapter();
                if (adapter != null) {
                    if (block.getBlockType() == BlockTypes.STRUCTURE_BLOCK) {
                        adapter.sendFakeNBT(player, pos, ((BaseBlock) block).getNbtData());
                        adapter.sendFakeOP(player);
                    }
                }
            }
        }
    }

    @Override
    public void sendTitle(Component title, Component sub) {
        String titleStr = WorldEditText.reduceToText(title, getLocale());
        String subStr = WorldEditText.reduceToText(sub, getLocale());
        player.sendTitle(titleStr, subStr, 0, 70, 20);
    }

    @Override
    public void unregister() {
        player.removeMetadata("WE", WorldEditPlugin.getInstance());
        plugin.getPermissionAttachmentManager().removeAttachment(player);
        super.unregister();
    }
}
