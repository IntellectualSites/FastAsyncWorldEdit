package com.sk89q.worldedit.nukkitmot;

import cn.nukkit.AdventureSettings;
import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.level.Level;
import cn.nukkit.network.protocol.UpdateBlockPacket;
import cn.nukkit.permission.PermissionAttachment;
import com.fastasyncworldedit.nukkitmot.NukkitPlayerBlockBag;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.extension.platform.AbstractPlayerActor;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.internal.cui.CUIEvent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.session.SessionKey;
import com.sk89q.worldedit.util.HandSide;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.formatting.WorldEditText;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.serializer.plain.PlainComponentSerializer;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.gamemode.GameMode;
import com.sk89q.worldedit.world.gamemode.GameModes;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.UUID;

public class NukkitPlayer extends AbstractPlayerActor {

    private final Player player;
    private PermissionAttachment permAttachment;

    public NukkitPlayer(Player player) {
        this.player = player;
    }

    @Override
    public UUID getUniqueId() {
        return player.getUniqueId();
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
    public BaseItemStack getItemInHand(HandSide handSide) {
        return NukkitAdapter.adaptItemStack(player.getInventory().getItemInHand());
    }

    @Override
    public void giveItem(BaseItemStack itemStack) {
        player.getInventory().addItem(NukkitAdapter.adaptItem(itemStack));
    }

    @Deprecated
    @Override
    public void printRaw(String msg) {
        for (String part : msg.split("\n")) {
            player.sendMessage(part);
        }
    }

    @Deprecated
    @Override
    public void print(String msg) {
        for (String part : msg.split("\n")) {
            player.sendMessage("§d" + part);
        }
    }

    @Deprecated
    @Override
    public void printDebug(String msg) {
        for (String part : msg.split("\n")) {
            player.sendMessage("§7" + part);
        }
    }

    @Deprecated
    @Override
    public void printError(String msg) {
        for (String part : msg.split("\n")) {
            player.sendMessage("§c" + part);
        }
    }

    @Override
    public void print(Component component) {
        String text = PlainComponentSerializer.INSTANCE.serialize(
                WorldEditText.format(component, getLocale())
        );
        player.sendMessage(text);
    }

    @Override
    public boolean trySetPosition(Vector3 pos, float pitch, float yaw) {
        cn.nukkit.level.Location loc = new cn.nukkit.level.Location(
                pos.x(), pos.y(), pos.z(), yaw, pitch, player.getLevel()
        );
        return player.teleport(loc);
    }

    @Override
    public String[] getGroups() {
        return new String[0];
    }

    @Override
    public BlockBag getInventoryBlockBag() {
        return new NukkitPlayerBlockBag(player);
    }

    @Override
    public GameMode getGameMode() {
        return switch (player.getGamemode()) {
            case 1 -> GameModes.CREATIVE;
            case 2 -> GameModes.ADVENTURE;
            case 3 -> GameModes.SPECTATOR;
            default -> GameModes.SURVIVAL;
        };
    }

    @Override
    public void setGameMode(GameMode gameMode) {
        int mode = switch (gameMode.id()) {
            case "creative" -> Player.CREATIVE;
            case "adventure" -> Player.ADVENTURE;
            case "spectator" -> Player.SPECTATOR;
            default -> Player.SURVIVAL;
        };
        player.setGamemode(mode);
    }

    @Override
    public boolean hasPermission(String perm) {
        return player.isOp() || player.hasPermission(perm);
    }

    @Override
    public void setPermission(String permission, boolean value) {
        if (permAttachment == null) {
            permAttachment = player.addAttachment(WorldEditNukkitPlugin.getInstance());
        }
        permAttachment.setPermission(permission, value);
    }

    void removePermissionAttachment() {
        if (permAttachment != null) {
            permAttachment.remove();
            permAttachment = null;
        }
    }

    @Override
    public World getWorld() {
        return NukkitAdapter.adapt(player.getLevel());
    }

    @Override
    public void dispatchCUIEvent(CUIEvent event) {
        // CUI not supported on Bedrock clients
    }

    @Override
    public boolean isAllowedToFly() {
        return player.getAllowFlight();
    }

    @Override
    public void setFlying(boolean flying) {
        player.getAdventureSettings().set(AdventureSettings.Type.ALLOW_FLIGHT, true);
        player.getAdventureSettings().set(AdventureSettings.Type.FLYING, flying);
        player.getAdventureSettings().update();
    }

    @Override
    public BaseEntity getState() {
        throw new UnsupportedOperationException("Cannot create a state from this object");
    }

    @Override
    public Location getLocation() {
        return NukkitAdapter.adapt(player.getLocation());
    }

    @Override
    public boolean setLocation(Location location) {
        return player.teleport(NukkitAdapter.adapt(location));
    }

    @Override
    public Locale getLocale() {
        try {
            String code = player.getLanguageCode().name(); // e.g. "zh_CN"
            return Locale.forLanguageTag(code.replace('_', '-'));
        } catch (Exception e) {
            return Locale.getDefault();
        }
    }

    @Nullable
    @Override
    public <T> T getFacet(Class<? extends T> cls) {
        return null;
    }

    @Override
    public SessionKey getSessionKey() {
        return new SessionKeyImpl(player);
    }

    @Override
    public <B extends BlockStateHolder<B>> void sendFakeBlock(BlockVector3 pos, B block) {
        Level level = player.getLevel();
        if (block == null) {
            // Restore real block
            cn.nukkit.math.Vector3 vec = NukkitAdapter.adapt(pos);
            level.sendBlocks(new Player[]{player}, new cn.nukkit.math.Vector3[]{vec},
                    UpdateBlockPacket.FLAG_ALL);
        } else {
            int fullId = NukkitAdapter.adaptFullId(block.toImmutableState());
            int blockId = fullId >> cn.nukkit.block.Block.DATA_BITS;
            int meta = fullId & cn.nukkit.block.Block.DATA_MASK;

            UpdateBlockPacket pk = new UpdateBlockPacket();
            pk.x = pos.x();
            pk.y = pos.y();
            pk.z = pos.z();
            pk.flags = UpdateBlockPacket.FLAG_ALL;
            pk.blockRuntimeId = cn.nukkit.level.GlobalBlockPalette.getOrCreateRuntimeId(
                    player.getGameVersion(), blockId, meta
            );
            player.dataPacket(pk);
        }
    }

    @Override
    public void sendTitle(Component title, Component sub) {
        String titleStr = PlainComponentSerializer.INSTANCE.serialize(title);
        String subStr = PlainComponentSerializer.INSTANCE.serialize(sub);
        player.sendTitle(titleStr, subStr, 0, 70, 20);
    }

    public Player getPlayer() {
        return player;
    }

    static class SessionKeyImpl implements SessionKey {

        private final UUID uuid;
        private final String name;

        SessionKeyImpl(Player player) {
            this.uuid = player.getUniqueId();
            this.name = player.getName();
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
            Player player = Server.getInstance().getPlayer(uuid).orElse(null);
            return player != null;
        }

        @Override
        public boolean isPersistent() {
            return true;
        }

    }

}
