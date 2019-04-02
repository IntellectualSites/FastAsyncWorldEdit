package com.thevoxelbox.voxelsniper;

import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.ChatColor;
import org.bukkit.Material;

/**
 *
 */
public class Message
{
    private static final int BRUSH_SIZE_WARNING_THRESHOLD = 20;
    private final SnipeData snipeData;

    /**
     * @param snipeData
     */
    public Message(SnipeData snipeData)
    {
        this.snipeData = snipeData;
    }

    /**
     * Send a brush message styled message to the player.
     *
     * @param brushMessage
     */
    public void brushMessage(String brushMessage)
    {
        snipeData.sendMessage(ChatColor.LIGHT_PURPLE + brushMessage);
    }

    /**
     * Display Brush Name.
     *
     * @param brushName
     */
    public void brushName(String brushName)
    {
        snipeData.sendMessage(ChatColor.AQUA + "Brush Type: " + ChatColor.LIGHT_PURPLE + brushName);
    }

    /**
     * Display Center Parameter.
     */
    public void center()
    {
        snipeData.sendMessage(ChatColor.DARK_BLUE + "Brush Center: " + ChatColor.DARK_RED + snipeData.getcCen());
    }

    /**
     * Display custom message.
     *
     * @param message
     */
    public void custom(String message)
    {
        snipeData.sendMessage(message);
    }

    /**
     * Display data value.
     */
    public void data()
    {
        snipeData.sendMessage(ChatColor.BLUE + "Data Variable: " + ChatColor.DARK_RED + snipeData.getPropertyId());
    }

    /**
     * Display voxel height.
     */
    public void height()
    {
        snipeData.sendMessage(ChatColor.DARK_AQUA + "Brush Height: " + ChatColor.DARK_RED + snipeData.getVoxelHeight());
    }

    /**
     * Display performer.
     *
     * @param performerName
     */
    public void performerName(String performerName)
    {
        this.snipeData.sendMessage(ChatColor.DARK_PURPLE + "Performer: " + ChatColor.DARK_GREEN + performerName);
    }

    /**
     * Displaye replace material.
     */
    @SuppressWarnings("deprecation")
    public void replace()
    {
        snipeData.sendMessage(ChatColor.AQUA + "Replace Material: " + BlockTypes.get(snipeData.getReplaceId()));
    }

    /**
     * Display replace data value.
     */
    public void replaceData()
    {
        snipeData.sendMessage(ChatColor.DARK_GRAY + "Replace Data Variable: " + ChatColor.DARK_RED + snipeData.getReplaceData());
    }

    /**
     * Display brush size.
     */
    public void size()
    {
        snipeData.sendMessage(ChatColor.GREEN + "Brush Size: " + ChatColor.DARK_RED + snipeData.getBrushSize());
        if (snipeData.getBrushSize() >= BRUSH_SIZE_WARNING_THRESHOLD)
        {
            snipeData.sendMessage(ChatColor.RED + "WARNING: Large brush size selected!");
        }
    }

    /**
     * Display toggle lightning message.
     */
    public void toggleLightning()
    {
        snipeData.sendMessage(ChatColor.GOLD + "Lightning mode has been toggled " + ChatColor.DARK_RED + ((snipeData.owner().getSnipeData(snipeData.owner().getCurrentToolId()).isLightningEnabled()) ? "on" : "off"));
    }

    /**
     * Display toggle printout message.
     */
    public final void togglePrintout()
    {
        snipeData.sendMessage(ChatColor.GOLD + "Brush info printout mode has been toggled " + ChatColor.DARK_RED + ((snipeData.owner().getSnipeData(snipeData.owner().getCurrentToolId()).isLightningEnabled()) ? "on" : "off"));
    }

    /**
     * Display toggle range message.
     */
    public void toggleRange()
    {
        snipeData.sendMessage(ChatColor.GOLD + "Distance Restriction toggled " + ChatColor.DARK_RED + ((snipeData.owner().getSnipeData(snipeData.owner().getCurrentToolId()).isRanged()) ? "on" : "off") + ChatColor.GOLD + ". Range is " + ChatColor.LIGHT_PURPLE + (double) snipeData.owner().getSnipeData(snipeData.owner().getCurrentToolId()).getRange());
    }

    /**
     * Display voxel type.
     */
    @SuppressWarnings("deprecation")
    public void voxel()
    {
        snipeData.sendMessage(ChatColor.GOLD + "Voxel: " + ChatColor.RED + BlockTypes.get(snipeData.getVoxelId()));
    }

    /**
     * Display voxel list.
     */
    public void voxelList()
    {
        if (snipeData.getVoxelList().isEmpty())
        {
            snipeData.sendMessage(ChatColor.DARK_GREEN + "No blocks selected!");
        }
        else
        {
            String returnValueBuilder = ChatColor.DARK_GREEN + "Block Types Selected: " + ChatColor.AQUA
                    + snipeData.getVoxelList();
            snipeData.sendMessage(returnValueBuilder);
        }
    }
}
