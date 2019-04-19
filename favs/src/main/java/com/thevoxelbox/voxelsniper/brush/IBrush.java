package com.thevoxelbox.voxelsniper.brush;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeAction;
import com.thevoxelbox.voxelsniper.SnipeData;

/**
 * Brush Interface.
 */
public interface IBrush {

    /**
     * @param vm Message object
     */
    void info(Message vm);

    /**
     * Handles parameters passed to brushes.
     *
     * @param par Array of string containing parameters
     * @param v   Snipe Data
     */
    void parameters(String[] par, SnipeData v);

    boolean perform(SnipeAction action, SnipeData data, AsyncBlock targetBlock, AsyncBlock lastBlock);

    /**
     * @return The name of the Brush
     */
    String getName();

    /**
     * @param name New name for the Brush
     */
    void setName(String name);

    /**
     * @return The name of the category the brush is in.
     */
    String getBrushCategory();

    /**
     * @return Permission node required to use this brush
     */
    String getPermissionNode();
}
