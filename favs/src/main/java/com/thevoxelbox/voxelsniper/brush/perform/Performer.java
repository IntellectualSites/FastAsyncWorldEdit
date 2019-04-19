/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thevoxelbox.voxelsniper.brush.perform;

import com.thevoxelbox.voxelsniper.Message;

/**
 * @author Voxel
 */
public interface Performer {

    void parse(String[] args, com.thevoxelbox.voxelsniper.SnipeData v);

    void showInfo(Message vm);
}
