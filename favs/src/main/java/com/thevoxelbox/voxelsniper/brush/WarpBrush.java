/*
  This file is part of VoxelSniper, licensed under the MIT License (MIT).

  Copyright (c) The VoxelBox <http://thevoxelbox.com>
  Copyright (c) contributors

  Permission is hereby granted, free of charge, to any person obtaining a copy
  of this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in
  all copies or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
  THE SOFTWARE.
 */
package com.thevoxelbox.voxelsniper.brush;

import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.util.TaskManager;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WarpBrush extends Brush {

    public WarpBrush() {
        this.setName("Warp");
    }

    @Override
    public final void info(final Message vm) {
        vm.brushName(this.getName());
    }

    @Override
    protected final void arrow(final SnipeData v) {
        Player player = v.owner().getPlayer();
        Location location = this.getLastBlock().getLocation();
        Location playerLocation = player.getLocation();
        location.setPitch(playerLocation.getPitch());
        location.setYaw(playerLocation.getYaw());
        location.setWorld(Bukkit.getWorld(location.getWorld().getName()));
        TaskManager.IMP.sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                player.teleport(location);
            }
        });
    }

    @Override
    protected final void powder(final SnipeData v) {
        Player player = v.owner().getPlayer();
        Location location = this.getLastBlock().getLocation();
        Location playerLocation = player.getLocation();
        location.setPitch(playerLocation.getPitch());
        location.setYaw(playerLocation.getYaw());
        location.setWorld(Bukkit.getWorld(location.getWorld().getName()));
        TaskManager.IMP.sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                player.teleport(location);
            }
        });
    }

    @Override
    public String getPermissionNode() {
        return "voxelsniper.brush.warp";
    }
}
