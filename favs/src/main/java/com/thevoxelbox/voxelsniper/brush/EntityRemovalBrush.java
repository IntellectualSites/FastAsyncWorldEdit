package com.thevoxelbox.voxelsniper.brush;

import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.PatternSyntaxException;

/**
 *
 */
public class EntityRemovalBrush extends Brush
{
    private final List<String> exemptions = new ArrayList<String>(3);

    /**
     *
     */
    public EntityRemovalBrush()
    {
        this.setName("Entity Removal");

        exemptions.add("org.bukkit.entity.Player");
        exemptions.add("org.bukkit.entity.Hanging");
        exemptions.add("org.bukkit.entity.NPC");
    }

    private void radialRemoval(SnipeData v)
    {
        final Chunk targetChunk = getTargetBlock().getChunk();
        int entityCount = 0;
        int chunkCount = 0;

        try
        {
            entityCount += removeEntities(targetChunk);

            int radius = Math.round(v.getBrushSize() / 16);

            for (int x = targetChunk.getX() - radius; x <= targetChunk.getX() + radius; x++)
            {
                for (int z = targetChunk.getZ() - radius; z <= targetChunk.getZ() + radius; z++)
                {
                    entityCount += removeEntities(getWorld().getChunkAt(x, z));

                    chunkCount++;
                }
            }
        }
        catch (final PatternSyntaxException pse)
        {
            pse.printStackTrace();
            v.sendMessage(ChatColor.RED + "Error in RegEx: " + ChatColor.LIGHT_PURPLE + pse.getPattern());
            v.sendMessage(ChatColor.RED + String.format("%s (Index: %d)", pse.getDescription(), pse.getIndex()));
        }
        v.sendMessage(ChatColor.GREEN + "Removed " + ChatColor.RED + entityCount + ChatColor.GREEN + " entities out of " + ChatColor.BLUE + chunkCount + ChatColor.GREEN + (chunkCount == 1 ? " chunk." : " chunks."));
    }

    private int removeEntities(Chunk chunk) throws PatternSyntaxException
    {
        int entityCount = 0;

        for (Entity entity : chunk.getEntities())
        {
            if (isClassInExemptionList(entity.getClass()))
            {
                continue;
            }

            entity.remove();
            entityCount++;
        }

        return entityCount;
    }

    private boolean isClassInExemptionList(Class<? extends Entity> entityClass) throws PatternSyntaxException
    {
        // Create a list of superclasses and interfaces implemented by the current entity type
        final List<String> entityClassHierarchy = new ArrayList<String>();

        Class<?> currentClass = entityClass;
        while (currentClass != null && !currentClass.equals(Object.class))
        {
            entityClassHierarchy.add(currentClass.getCanonicalName());

            for (final Class<?> intrf : currentClass.getInterfaces())
            {
                entityClassHierarchy.add(intrf.getCanonicalName());
            }

            currentClass = currentClass.getSuperclass();
        }

        for (final String exemptionPattern : exemptions)
        {
            for (final String typeName : entityClassHierarchy)
            {
                if (typeName.matches(exemptionPattern))
                {
                    return true;
                }

            }
        }

        return false;
    }

    @Override
    protected void arrow(SnipeData v)
    {
        this.radialRemoval(v);
    }

    @Override
    protected void powder(SnipeData v)
    {
        this.radialRemoval(v);
    }

    @Override
    public void info(Message vm)
    {
        vm.brushName(getName());

        final StringBuilder exemptionsList = new StringBuilder(ChatColor.GREEN + "Exemptions: " + ChatColor.LIGHT_PURPLE);
        for (Iterator it = exemptions.iterator(); it.hasNext(); )
        {
            exemptionsList.append(it.next());
            if (it.hasNext())
            {
                exemptionsList.append(", ");
            }
        }
        vm.custom(exemptionsList.toString());

        vm.size();
    }

    @Override
    public void parameters(final String[] par, final SnipeData v)
    {
        for (final String currentParam : par)
        {
            if (currentParam.startsWith("+") || currentParam.startsWith("-"))
            {
                final boolean isAddOperation = currentParam.startsWith("+");

                // +#/-# will suppress auto-prefixing
                final String exemptionPattern = currentParam.startsWith("+#") || currentParam.startsWith("-#") ?
                        currentParam.substring(2) :
                        (currentParam.contains(".") ? currentParam.substring(1) : ".*." + currentParam.substring(1));

                if (isAddOperation)
                {
                    exemptions.add(exemptionPattern);
                    v.sendMessage(String.format("Added %s to entity exemptions list.", exemptionPattern));
                }
                else
                {
                    exemptions.remove(exemptionPattern);
                    v.sendMessage(String.format("Removed %s from entity exemptions list.", exemptionPattern));
                }
            }

            if (currentParam.equalsIgnoreCase("list-exemptions") || currentParam.equalsIgnoreCase("lex"))
            {
                for (final String exemption : exemptions)
                {
                    v.sendMessage(ChatColor.LIGHT_PURPLE + exemption);
                }
            }
        }
    }

    @Override
    public String getPermissionNode()
    {
        return "voxelsniper.brush.entityremoval";
    }
}
