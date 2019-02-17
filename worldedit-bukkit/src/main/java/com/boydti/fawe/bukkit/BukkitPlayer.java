package com.boydti.fawe.bukkit;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FaweLocation;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.wrappers.PlayerWrapper;
import java.lang.reflect.Method;
import java.util.UUID;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class BukkitPlayer extends FawePlayer<Player> {

    private static ConsoleCommandSender console;

    public BukkitPlayer(final Player parent) {
        super(parent);
    }

    @Override
    public String getName() {
        return this.parent.getName();
    }

    @Override
    public UUID getUUID() {
        return this.parent.getUniqueId();
    }

    @Override
    public boolean hasPermission(final String perm) {
        return this.parent.hasPermission(perm);
    }

    @Override
    public boolean isSneaking() {
        return parent.isSneaking();
    }

    @Override
    public void setPermission(final String perm, final boolean flag) {
        /*
         *  Permissions are used to managing WorldEdit region restrictions
         *   - The `/wea` command will give/remove the required bypass permission
         */
        if (Fawe.<FaweBukkit> imp().getVault() == null || Fawe.<FaweBukkit> imp().getVault().permission == null) {
            this.parent.addAttachment(Fawe.<FaweBukkit> imp().getPlugin()).setPermission(perm, flag);
        } else if (flag) {
            if (!Fawe.<FaweBukkit> imp().getVault().permission.playerAdd(this.parent, perm)) {
                this.parent.addAttachment(Fawe.<FaweBukkit> imp().getPlugin()).setPermission(perm, flag);
            }
        } else {
            if (!Fawe.<FaweBukkit> imp().getVault().permission.playerRemove(this.parent, perm)) {
                this.parent.addAttachment(Fawe.<FaweBukkit> imp().getPlugin()).setPermission(perm, flag);
            }
        }
    }


    @Override
    public void resetTitle() {
        sendTitle("","");
    }

    public void sendTitle(String title, String sub) {
        try {
            Method methodSendTitle = Player.class.getDeclaredMethod("sendTitle", String.class, String.class, int.class, int.class, int.class);
            methodSendTitle.invoke(parent, ChatColor.GOLD + title, ChatColor.GOLD + sub, 0, 70, 20);
            return;
        } catch (Throwable ignore) {
            try {
                Method methodSendTitle = Player.class.getDeclaredMethod("sendTitle", String.class, String.class);
                methodSendTitle.invoke(parent, ChatColor.GOLD + title, ChatColor.GOLD + sub);
                return;
            } catch (Throwable ignore2) {}
        }
        if (console == null) {
            console = Bukkit.getConsoleSender();
            Bukkit.getServer().dispatchCommand(console, "gamerule sendCommandFeedback false");
            Bukkit.getServer().dispatchCommand(console, "title " + getName() + " times 0 60 20");
        }
        Bukkit.getServer().dispatchCommand(console, "title " + getName() + " subtitle [{\"text\":\"" + sub + "\",\"color\":\"gold\"}]");
        Bukkit.getServer().dispatchCommand(console, "title " + getName() + " title [{\"text\":\"" + title + "\",\"color\":\"gold\"}]");
    }

    @Override
    public void sendMessage(final String message) {
        this.parent.sendMessage(BBC.color(message));
    }

    @Override
    public void executeCommand(final String cmd) {
        Bukkit.getServer().dispatchCommand(this.parent, cmd);
    }

    @Override
    public FaweLocation getLocation() {
        final Location loc = this.parent.getLocation();
        return new FaweLocation(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    @Override
    public com.sk89q.worldedit.entity.Player toWorldEditPlayer() {
        return WorldEditPlugin.getInstance().wrapPlayer(this.parent);
    }

}
