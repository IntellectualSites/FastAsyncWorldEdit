package com.thevoxelbox.voxelsniper.util;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import org.bukkit.Material;
import org.bukkit.World;

/**
 * @author MikeMatrix
 */
public class BlockWrapper {

    private int id;
    private Material type;
    private int x;
    private int y;
    private int z;
    private int data;
    private World world;

    /**
     * @param block
     */
    @SuppressWarnings("deprecation")
    public BlockWrapper(final AsyncBlock block) {
        this.setId(block.getTypeId());
        this.setX(block.getX());
        this.setY(block.getY());
        this.setZ(block.getZ());
        this.setPropertyId(block.getPropertyId());
        this.setWorld(block.getWorld());
    }

    /**
     * @return the data
     */
    public final int getPropertyId() {
        return this.data;
    }

    /**
     * @param data the data to set
     */
    public final void setPropertyId(final int data) {
        this.data = data;
    }

    public Material getType() {
        return type;
    }

    public void setType(Material type) {
        this.type = type;
    }

    /**
     * @return the id
     */
    public final int getId() {
        return this.id;
    }

    /**
     * @param id the id to set
     */
    public final void setId(final int id) {
        this.id = id;
    }

    /**
     * @return the world
     */
    public final World getWorld() {
        return this.world;
    }

    /**
     * @param world the world to set
     */
    public final void setWorld(final World world) {
        this.world = world;
    }

    /**
     * @return the x
     */
    public final int getX() {
        return this.x;
    }

    /**
     * @param x the x to set
     */
    public final void setX(final int x) {
        this.x = x;
    }

    /**
     * @return the y
     */
    public final int getY() {
        return this.y;
    }

    /**
     * @param y the y to set
     */
    public final void setY(final int y) {
        this.y = y;
    }

    /**
     * @return the z
     */
    public final int getZ() {
        return this.z;
    }

    /**
     * @param z the z to set
     */
    public final void setZ(final int z) {
        this.z = z;
    }

}
