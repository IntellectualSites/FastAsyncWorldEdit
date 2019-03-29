package com.thevoxelbox.voxelsniper;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FawePlayer;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.extension.platform.CommandManager;
import com.sk89q.worldedit.util.command.parametric.ExceptionConverter;
import com.thevoxelbox.voxelsniper.api.command.VoxelCommand;
import com.thevoxelbox.voxelsniper.command.*;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * @author Voxel
 */
public class VoxelSniperListener implements Listener
{

    private static final String SNIPER_PERMISSION = "voxelsniper.sniper";
    private final VoxelSniper plugin;
    private Map<String, VoxelCommand> commands = new HashMap<>();

    /**
     * @param plugin
     */
    public VoxelSniperListener(final VoxelSniper plugin)
    {
        this.plugin = plugin;
        addCommand(new VoxelBrushCommand(plugin));
        addCommand(new VoxelBrushToolCommand(plugin));
        addCommand(new VoxelCenterCommand(plugin));
        addCommand(new VoxelChunkCommand(plugin));
        addCommand(new VoxelDefaultCommand(plugin));
        addCommand(new VoxelGoToCommand(plugin));
        addCommand(new VoxelHeightCommand(plugin));
        addCommand(new VoxelInkCommand(plugin));
        addCommand(new VoxelInkReplaceCommand(plugin));
        addCommand(new VoxelListCommand(plugin));
        addCommand(new VoxelPaintCommand(plugin));
        addCommand(new VoxelPerformerCommand(plugin));
        addCommand(new VoxelReplaceCommand(plugin));
        addCommand(new VoxelSniperCommand(plugin));
        addCommand(new VoxelUndoCommand(plugin));
        addCommand(new VoxelUndoUserCommand(plugin));
        addCommand(new VoxelVoxelCommand(plugin));
    }

    private void addCommand(final VoxelCommand command)
    {
        this.commands.put(command.getIdentifier().toLowerCase(), command);
    }

    /**
     * @param player
     * @param split
     * @param command
     * @return boolean Success.
     */
    public boolean onCommand(final Player player, final String[] split, final String command)
    {
        VoxelCommand found = this.commands.get(command.toLowerCase());
        if (found == null)
        {
            return false;
        }

        if (!hasPermission(found, player))
        {
            player.sendMessage(ChatColor.RED + "Insufficient Permissions.");
            return true;
        }

        FawePlayer fp = FawePlayer.wrap(player);
        if (!fp.runAction(() -> {
            ExceptionConverter exceptionConverter = CommandManager.getInstance().getExceptionConverter();
            try {
                try {
                    found.onCommand(player, split);
                    return;
                } catch (Throwable t) {
                    Throwable next = t;
                    exceptionConverter.convert(next);
                    while (next.getCause() != null) {
                        next = next.getCause();
                        exceptionConverter.convert(next);
                    }
                    throw next;
                }
            } catch (CommandException e) {
                String message = e.getMessage();
                if (message != null) {
                    fp.sendMessage(e.getMessage());
                    return;
                }
                e.printStackTrace();
            } catch (Throwable e) {
                e.printStackTrace();
            }
            fp.sendMessage("An unknown FAWE error has occurred! Please see console.");
        }, false, true)) {
            BBC.WORLDEDIT_COMMAND_LIMIT.send(fp);
        }
        return true;
    }

    private boolean hasPermission(final VoxelCommand command, final Player player)
    {
        if (command == null || player == null)
        {
            // Just a usual check for nulls
            return false;
        }
        else if (command.getPermission() == null || command.getPermission().isEmpty())
        {
            // This is for commands that do not require a permission node to be executed
            return true;
        }
        else
        {
            // Should utilize Vault for permission checks if available
            return player.hasPermission(command.getPermission());
        }
    }

    /**
     * @param event
     */
    @EventHandler(ignoreCancelled = false)
    public final void onPlayerInteract(final PlayerInteractEvent event)
    {
        Player player = event.getPlayer();

        if (!player.hasPermission(SNIPER_PERMISSION))
        {
            return;
        }

        try
        {
            Sniper sniper = plugin.getSniperManager().getSniperForPlayer(player);
            if (sniper.isEnabled() && sniper.snipe(event.getAction(), event.getMaterial(), event.getClickedBlock(), event.getBlockFace()))
            {
                event.setCancelled(true);
            }
        }
        catch (final Throwable ignored)
        {
            ignored.printStackTrace();
        }
    }

    /**
     * @param event
     */
    @EventHandler
    public final void onPlayerJoin(final PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        Sniper sniper = plugin.getSniperManager().getSniperForPlayer(player);

        if (player.hasPermission(SNIPER_PERMISSION) && plugin.getVoxelSniperConfiguration().isMessageOnLoginEnabled())
        {
            sniper.displayInfo();
        }
    }
}
