package com.boydti.fawe.bukkit;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FaweCommand;
import com.sk89q.worldedit.bukkit.BukkitBlockCommandSender;
import com.sk89q.worldedit.bukkit.BukkitCommandSender;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extension.platform.Actor;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BukkitCommand implements CommandExecutor {

    private final FaweCommand cmd;

    public BukkitCommand(FaweCommand cmd) {
        this.cmd = cmd;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command cmd, String label, String[] args) {
        final Actor plr = wrapCommandSender(sender);
        if (!sender.hasPermission(this.cmd.getPerm()) && !sender.isOp()) {
            BBC.NO_PERM.send(plr, this.cmd.getPerm());
            return true;
        }
        this.cmd.executeSafe(plr, args);
        return true;
    }

    /**
     * Used to wrap a Bukkit Player as a WorldEdit Player.
     *
     * @param player a player
     * @return a wrapped player
     */
    public com.sk89q.worldedit.bukkit.BukkitPlayer wrapPlayer(Player player) {
        return new BukkitPlayer(WorldEditPlugin.getInstance(), player);
    }

    public Actor wrapCommandSender(CommandSender sender) {
        if (sender instanceof Player) {
            return wrapPlayer((Player) sender);
        } else if (sender instanceof BlockCommandSender) {
            return new BukkitBlockCommandSender(WorldEditPlugin.getInstance(), (BlockCommandSender) sender);
        }

        return new BukkitCommandSender(WorldEditPlugin.getInstance(), sender);
    }
}
