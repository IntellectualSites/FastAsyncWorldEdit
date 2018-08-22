package com.thevoxelbox.voxelsniper.util;

import com.google.common.collect.ImmutableList;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.NullExtent;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.mask.BlockMaskBuilder;
import com.sk89q.worldedit.world.block.BlockState;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Container class for multiple ID/Datavalue pairs.
 */
public class VoxelList
{

    private BlockMask mask = new BlockMask(NullExtent.INSTANCE);

    /**
     * Adds the specified id, data value pair to the VoxelList. A data value of -1 will operate on all data values of that id.
     * 
     * @param i
     */
    public void add(BlockState i)
    {
        this.mask = mask.toBuilder().add(i).build(NullExtent.INSTANCE);
    }

    public void add(BlockMask mask)
    {

        this.mask = (BlockMask) mask.and(mask);
    }

    /**
     * Removes the specified id, data value pair from the VoxelList.
     * 
     * @return true if this list contained the specified element
     */
    public boolean removeValue(final BlockState state)
    {
        this.mask = mask.toBuilder().remove(state).build(NullExtent.INSTANCE);
        return true;
    }

    public boolean removeValue(final BlockMask state)
    {
        this.mask = (BlockMask) mask.and(state.inverse());
        return true;
    }

    /**
     * @param i
     * @return true if this list contains the specified element
     */
    public boolean contains(final BlockData i)
    {
        return mask.test(BukkitAdapter.adapt(i));
    }

    /**
     * Clears the VoxelList.
     */
    public void clear()
    {
        mask = mask.toBuilder().clear().build(NullExtent.INSTANCE);
    }

    /**
     * Returns true if this list contains no elements.
     *
     * @return true if this list contains no elements
     */
    public boolean isEmpty()
    {
        return mask.toBuilder().isEmpty();
    }

    /**
     * Returns a defensive copy of the List with pairs.
     *
     * @return defensive copy of the List with pairs
     */
    public String toString()
    {
        return mask.toString();
    }


}
