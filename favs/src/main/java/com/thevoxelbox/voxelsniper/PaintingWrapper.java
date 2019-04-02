package com.thevoxelbox.voxelsniper;

import org.bukkit.Art;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;

import java.util.Set;

/**
 * Painting state change handler.
 *
 * @author Piotr
 */
public final class PaintingWrapper
{

    private PaintingWrapper()
    {
    }


    /**
     * The paint method used to scroll or set a painting to a specific type.
     *
     * @param p
     *         The player executing the method
     * @param auto
     *         Scroll automatically? If false will use 'choice' to try and set the painting
     * @param back
     *         Scroll in reverse?
     * @param choice
     *         Chosen index to set the painting to
     */
    @SuppressWarnings("deprecation")
    public static void paint(final Player p, final boolean auto, final boolean back, final int choice)
    {
        Location targetLocation = p.getTargetBlock(null, 4).getLocation();
        Chunk paintingChunk = p.getTargetBlock(null, 4).getLocation().getChunk();

        Double bestDistanceMatch = 50D;
        Painting bestMatch = null;

        for (Entity entity : paintingChunk.getEntities())
        {
            if (entity.getType() == EntityType.PAINTING)
            {
                Double distance = targetLocation.distanceSquared(entity.getLocation());

                if (distance <= 4 && distance < bestDistanceMatch)
                {
                    bestDistanceMatch = distance;
                    bestMatch = (Painting) entity;
                }
            }
        }

        if (bestMatch != null)
        {
            if (auto)
            {
                try
                {
                    final int i = bestMatch.getArt().getId() + (back ? -1 : 1);
                    Art art = Art.getById(i);

                    if (art == null)
                    {
                        p.sendMessage(ChatColor.RED + "This is the final painting, try scrolling to the other direction.");
                        return;
                    }

                    bestMatch.setArt(art);
                    p.sendMessage(ChatColor.GREEN + "Painting set to ID: " + (i));
                }
                catch (final Exception e)
                {
                    p.sendMessage(ChatColor.RED + "Oops. Something went wrong.");
                }
            }
            else
            {
                try
                {
                    Art art = Art.getById(choice);

                    bestMatch.setArt(art);
                    p.sendMessage(ChatColor.GREEN + "Painting set to ID: " + choice);
                }
                catch (final Exception exception)
                {
                    p.sendMessage(ChatColor.RED + "Your input was invalid somewhere.");
                }
            }
        }
    }
}
