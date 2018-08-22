package com.thevoxelbox.voxelsniper.brush;

import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.List;

/**
 * http://www.voxelwiki.com/minecraft/Voxelsniper#The_Jockey_Brush
 *
 * @author Voxel
 * @author Monofraps
 */
public class JockeyBrush extends Brush
{
    private static final int ENTITY_STACK_LIMIT = 50;
    private JockeyType jockeyType = JockeyType.NORMAL_ALL_ENTITIES;
    private Entity jockeyedEntity = null;

    /**
     *
     */
    public JockeyBrush()
    {
        this.setName("Jockey");
    }

    private void sitOn(final SnipeData v)
    {
        final Chunk targetChunk = this.getWorld().getChunkAt(this.getTargetBlock().getLocation());
        final int targetChunkX = targetChunk.getX();
        final int targetChunkZ = targetChunk.getZ();

        double range = Double.MAX_VALUE;
        Entity closest = null;

        for (int x = targetChunkX - 1; x <= targetChunkX + 1; x++)
        {
            for (int y = targetChunkZ - 1; y <= targetChunkZ + 1; y++)
            {
                for (final Entity entity : this.getWorld().getChunkAt(x, y).getEntities())
                {
                    if (entity.getEntityId() == v.owner().getPlayer().getEntityId())
                    {
                        continue;
                    }

                    if (jockeyType == JockeyType.NORMAL_PLAYER_ONLY || jockeyType == JockeyType.INVERSE_PLAYER_ONLY)
                    {
                        if (!(entity instanceof Player))
                        {
                            continue;
                        }
                    }

                    final Location entityLocation = entity.getLocation();
                    final double entityDistance = entityLocation.distance(v.owner().getPlayer().getLocation());

                    if (entityDistance < range)
                    {
                        range = entityDistance;
                        closest = entity;
                    }
                }
            }
        }

        if (closest != null)
        {
            final Player player = v.owner().getPlayer();
            final PlayerTeleportEvent playerTeleportEvent = new PlayerTeleportEvent(player, player.getLocation(), closest.getLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);

            Bukkit.getPluginManager().callEvent(playerTeleportEvent);

            if (!playerTeleportEvent.isCancelled())
            {
                if (jockeyType == JockeyType.INVERSE_PLAYER_ONLY || jockeyType == JockeyType.INVERSE_ALL_ENTITIES)
                {
                    player.setPassenger(closest);
                }
                else
                {
                    closest.setPassenger(player);
                    jockeyedEntity = closest;
                }
                v.sendMessage(ChatColor.GREEN + "You are now saddles on entity: " + closest.getEntityId());
            }
        }
        else
        {
            v.sendMessage(ChatColor.RED + "Could not find any entities");
        }
    }

    private void stack(final SnipeData v)
    {
        final int brushSizeDoubled = v.getBrushSize() * 2;

        List<Entity> nearbyEntities = v.owner().getPlayer().getNearbyEntities(brushSizeDoubled, brushSizeDoubled, brushSizeDoubled);
        Entity lastEntity = v.owner().getPlayer();
        int stackHeight = 0;

        for (Entity entity : nearbyEntities)
        {
            if (!(stackHeight >= ENTITY_STACK_LIMIT))
            {
                if (jockeyType == JockeyType.STACK_ALL_ENTITIES)
                {
                    lastEntity.setPassenger(entity);
                    lastEntity = entity;
                    stackHeight++;
                }
                else if (jockeyType == JockeyType.STACK_PLAYER_ONLY)
                {
                    if (entity instanceof Player)
                    {
                        lastEntity.setPassenger(entity);
                        lastEntity = entity;
                        stackHeight++;
                    }
                }
                else
                {
                    v.owner().getPlayer().sendMessage("You broke stack! :O");
                }
            }
            else
            {
                return;
            }
        }

    }

    @Override
    protected final void arrow(final SnipeData v)
    {
        if (jockeyType == JockeyType.STACK_ALL_ENTITIES || jockeyType == JockeyType.STACK_PLAYER_ONLY)
        {
            stack(v);
        }
        else
        {
            this.sitOn(v);
        }
    }

    @Override
    protected final void powder(final SnipeData v)
    {
        if (jockeyType == JockeyType.INVERSE_PLAYER_ONLY || jockeyType == JockeyType.INVERSE_ALL_ENTITIES)
        {
            v.owner().getPlayer().eject();
            v.owner().getPlayer().sendMessage(ChatColor.GOLD + "The guy on top of you has been ejected!");
        }
        else
        {
            if (jockeyedEntity != null)
            {
                jockeyedEntity.eject();
                jockeyedEntity = null;
                v.owner().getPlayer().sendMessage(ChatColor.GOLD + "You have been ejected!");
            }
        }

    }

    @Override
    public final void info(final Message vm)
    {
        vm.brushName(this.getName());
        vm.custom("Current jockey mode: " + ChatColor.GREEN + jockeyType.toString());
        vm.custom(ChatColor.GREEN + "Help: " + ChatColor.AQUA + "http://www.voxelwiki.com/minecraft/Voxelsniper#The_Jockey_Brush");
    }

    @Override
    public final void parameters(final String[] par, final SnipeData v)
    {
        boolean inverse = false;
        boolean playerOnly = false;
        boolean stack = false;

        try
        {
            for (String parameter : par)
            {
                if (parameter.startsWith("-i:"))
                {
                    inverse = parameter.endsWith("y");
                }

                if (parameter.startsWith("-po:"))
                {
                    playerOnly = parameter.endsWith("y");
                }
                if (parameter.startsWith("-s:"))
                {
                    stack = parameter.endsWith("y");
                }

            }

            if (inverse)
            {
                if (playerOnly)
                {
                    jockeyType = JockeyType.INVERSE_PLAYER_ONLY;
                }
                else
                {
                    jockeyType = JockeyType.INVERSE_ALL_ENTITIES;
                }
            }
            else if (stack)
            {
                if (playerOnly)
                {
                    jockeyType = JockeyType.STACK_PLAYER_ONLY;
                }
                else
                {
                    jockeyType = JockeyType.STACK_ALL_ENTITIES;
                }
            }
            else
            {
                if (playerOnly)
                {
                    jockeyType = JockeyType.NORMAL_PLAYER_ONLY;
                }
                else
                {
                    jockeyType = JockeyType.NORMAL_ALL_ENTITIES;
                }
            }

            v.sendMessage("Current jockey mode: " + ChatColor.GREEN + jockeyType.toString());
        }
        catch (Exception exception)
        {
            v.sendMessage("Error while parsing your arguments.");
            exception.printStackTrace();
        }
    }

    /**
     * Available types of jockey modes.
     */
    private enum JockeyType
    {
        NORMAL_ALL_ENTITIES("Normal (All)"), NORMAL_PLAYER_ONLY("Normal (Player only)"), INVERSE_ALL_ENTITIES("Inverse (All)"), INVERSE_PLAYER_ONLY("Inverse (Player only)"), STACK_ALL_ENTITIES("Stack (All)"), STACK_PLAYER_ONLY("Stack (Player only)");
        private String name;

        JockeyType(String name)
        {
            this.name = name;
        }

        @Override
        public String toString()
        {
            return this.name;
        }
    }

    @Override
    public String getPermissionNode()
    {
        return "voxelsniper.brush.jockey";
    }
}
