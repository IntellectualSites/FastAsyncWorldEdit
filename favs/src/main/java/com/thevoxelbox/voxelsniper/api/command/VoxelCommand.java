package com.thevoxelbox.voxelsniper.api.command;

import com.thevoxelbox.voxelsniper.VoxelSniper;
import org.bukkit.entity.Player;

public abstract class VoxelCommand
{

    private final String name;
    private String description = "";
    private String permission = "";
    private String identifier = "";
    protected final VoxelSniper plugin;

    public VoxelCommand(String name, final VoxelSniper plugin)
    {
        this.name = name;
        this.plugin = plugin;
    }

    public abstract boolean onCommand(final Player player, final String[] args);

    public String getDescription()
    {
        return description;
    }

    public String getPermission()
    {
        return this.permission;
    }

    public String getName()
    {
        return this.name;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public void setPermission(String permission)
    {
        this.permission = permission;
    }

    public String getIdentifier()
    {
        return this.identifier;
    }

    public void setIdentifier(String identifier)
    {
        this.identifier = identifier;
    }

    public boolean isIdentifier(String offered)
    {
        return this.identifier.isEmpty() || this.identifier.equalsIgnoreCase(offered);
    }

    /**
     * Padds an empty String to the front of the array.
     *
     * @param args Array to pad empty string in front of
     * @return padded array
     */
    protected String[] hackTheArray(String[] args)
    {
        String[] returnValue = new String[args.length + 1];
        returnValue[0] = "";
        for (int i = 0, argsLength = args.length; i < argsLength; i++)
        {
            String arg = args[i];
            returnValue[i + 1] = arg;
        }
        return returnValue;
    }

}
