/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thevoxelbox.voxelsniper.brush.perform;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.Undo;
import org.bukkit.World;

/**
 * @author Voxel
 */
public abstract class vPerformer {

    public String name = "Performer";
    protected Undo h;
    protected World w;

    public abstract void info(Message vm);

    public abstract void init(com.thevoxelbox.voxelsniper.SnipeData v);

    public void setUndo() {
        h = new Undo();
    }

    public abstract void perform(AsyncBlock b);

    public Undo getUndo() {
        Undo temp = h;
        h = null;
        return temp;
    }

    public boolean isUsingReplaceMaterial() {
        return false;
    }
}
