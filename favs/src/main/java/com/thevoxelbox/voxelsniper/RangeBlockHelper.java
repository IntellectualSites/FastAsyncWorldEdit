/**
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
package com.thevoxelbox.voxelsniper;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.boydti.fawe.bukkit.wrapper.AsyncWorld;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.registry.LegacyMapper;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class RangeBlockHelper {
    private static final int MAXIMUM_WORLD_HEIGHT = 255;
    private static final double DEFAULT_PLAYER_VIEW_HEIGHT = 1.65D;
    private static final double DEFAULT_LOCATION_VIEW_HEIGHT = 0.0D;
    private static final double DEFAULT_STEP = 0.2D;
    private static final int DEFAULT_RANGE = 250;
    private Location playerLoc;
    private double rotX;
    private double rotY;
    private double viewHeight;
    private double rotXSin;
    private double rotXCos;
    private double rotYSin;
    private double rotYCos;
    private double length;
    private double hLength;
    private double step;
    private double range;
    private double playerX;
    private double playerY;
    private double playerZ;
    private double xOffset;
    private double yOffset;
    private double zOffset;
    private int lastX;
    private int lastY;
    private int lastZ;
    private int targetX;
    private int targetY;
    private int targetZ;
    private AsyncWorld world;

    public RangeBlockHelper(Location location) {
        this.init(location, 250.0D, 0.2D, 0.0D);
    }

    public RangeBlockHelper(Location location, int range, double step) {
        this.world = (AsyncWorld) location.getWorld();
        this.init(location, (double)range, step, 0.0D);
    }

    public RangeBlockHelper(Player player, int range, double step) {
        if (player != null) {
            this.world = VoxelSniper.getInstance().getSniperManager().getSniperForPlayer(player).getWorld();
        }
        this.init(player.getLocation(), (double)range, step, 1.65D);
    }

    public RangeBlockHelper(Player player, AsyncWorld world) {
        if (player != null && (world == null || player.getWorld().getName().equals(world.getName()))) {
            this.world = VoxelSniper.getInstance().getSniperManager().getSniperForPlayer(player).getWorld();
        } else {
            this.world = world;
        }
        this.init(player.getLocation(), 250.0D, 0.2D, 1.65D);
    }

    public RangeBlockHelper(Player player, AsyncWorld world, double range) {
        if (player != null && (world == null || player.getWorld().getName().equals(world.getName()))) {
            this.world = VoxelSniper.getInstance().getSniperManager().getSniperForPlayer(player).getWorld();
        } else {
            this.world = world;
        }
        this.init(player.getLocation(), range, 0.2D, 1.65D);
        this.fromOffworld();
    }

    public final void fromOffworld() {
        if(this.targetY <= 255) {
            if(this.targetY < 0) {
                while(this.targetY < 0 && this.length <= this.range) {
                    this.lastX = this.targetX;
                    this.lastY = this.targetY;
                    this.lastZ = this.targetZ;

                    while(true) {
                        this.length += this.step;
                        this.hLength = this.length * this.rotYCos;
                        this.yOffset = this.length * this.rotYSin;
                        this.xOffset = this.hLength * this.rotXCos;
                        this.zOffset = this.hLength * this.rotXSin;
                        this.targetX = (int)Math.floor(this.xOffset + this.playerX);
                        this.targetY = (int)Math.floor(this.yOffset + this.playerY);
                        this.targetZ = (int)Math.floor(this.zOffset + this.playerZ);
                        if(this.length > this.range || this.targetX != this.lastX || this.targetY != this.lastY || this.targetZ != this.lastZ) {
                            break;
                        }
                    }
                }
            }
        } else {
            while(this.targetY > 255 && this.length <= this.range) {
                this.lastX = this.targetX;
                this.lastY = this.targetY;
                this.lastZ = this.targetZ;

                while(true) {
                    this.length += this.step;
                    this.hLength = this.length * this.rotYCos;
                    this.yOffset = this.length * this.rotYSin;
                    this.xOffset = this.hLength * this.rotXCos;
                    this.zOffset = this.hLength * this.rotXSin;
                    this.targetX = (int)Math.floor(this.xOffset + this.playerX);
                    this.targetY = (int)Math.floor(this.yOffset + this.playerY);
                    this.targetZ = (int)Math.floor(this.zOffset + this.playerZ);
                    if(this.length > this.range || this.targetX != this.lastX || this.targetY != this.lastY || this.targetZ != this.lastZ) {
                        break;
                    }
                }
            }
        }

    }

    public final AsyncBlock getCurBlock() {
        return this.length <= this.range && this.targetY <= 255 && this.targetY >= 0?this.world.getBlockAt(this.targetX, this.targetY, this.targetZ):null;
    }

    private boolean isAir(Material m) {
        switch (m) {
            case AIR:
            case CAVE_AIR:
            case VOID_AIR:
                return true;
            default:
                return false;
        }
    }

    public final AsyncBlock getFaceBlock() {
        while(this.getNextBlock() != null && isAir(this.getCurBlock().getType())) {
            ;
        }

        if(this.getCurBlock() != null) {
            return this.getLastBlock();
        } else {
            return null;
        }
    }

    public final AsyncBlock getLastBlock() {
        return this.lastY <= 255 && this.lastY >= 0?this.world.getBlockAt(this.lastX, this.lastY, this.lastZ):null;
    }

    public final AsyncBlock getNextBlock() {
        this.lastX = this.targetX;
        this.lastY = this.targetY;
        this.lastZ = this.targetZ;

        do {
            this.length += this.step;
            this.hLength = this.length * this.rotYCos;
            this.yOffset = this.length * this.rotYSin;
            this.xOffset = this.hLength * this.rotXCos;
            this.zOffset = this.hLength * this.rotXSin;
            this.targetX = (int)Math.floor(this.xOffset + this.playerX);
            this.targetY = (int)Math.floor(this.yOffset + this.playerY);
            this.targetZ = (int)Math.floor(this.zOffset + this.playerZ);
        } while(this.length <= this.range && this.targetX == this.lastX && this.targetY == this.lastY && this.targetZ == this.lastZ);

        return this.length <= this.range && this.targetY <= 255 && this.targetY >= 0?this.world.getBlockAt(this.targetX, this.targetY, this.targetZ):null;
    }

    public final AsyncBlock getRangeBlock() {
        this.fromOffworld();
        return this.length > this.range?null:this.getRange();
    }

    public final AsyncBlock getTargetBlock() {
        this.fromOffworld();

        while(this.getNextBlock() != null && isAir(this.getCurBlock().getType())) {
            ;
        }

        return this.getCurBlock();
    }

    public final void setCurBlock(int type) {
        if(this.getCurBlock() != null) {
            this.world.getBlockAt(this.targetX, this.targetY, this.targetZ).setType(getType(type));
        }

    }

    public final void setFaceBlock(int type) {
        while(this.getNextBlock() != null && isAir(this.getCurBlock().getType())) {
            ;
        }

        if(this.getCurBlock() != null) {
            this.world.getBlockAt(this.targetX, this.targetY, this.targetZ).setType(getType(type));
        }

    }

    private Material getType(int id) {
        return BukkitAdapter.adapt(LegacyMapper.getInstance().getBlockFromLegacy(id).getBlockType());
    }

    public final void setLastBlock(int type) {
        if(this.getLastBlock() != null) {
            this.world.getBlockAt(this.lastX, this.lastY, this.lastZ).setType(getType(type));
        }

    }

    public final void setTargetBlock(int type) {
        while(this.getNextBlock() != null && isAir(this.getCurBlock().getType())) {
            ;
        }

        if(this.getCurBlock() != null) {
            this.world.getBlockAt(this.targetX, this.targetY, this.targetZ).setType(getType(type));
        }

    }

    private AsyncBlock getRange() {
        this.lastX = this.targetX;
        this.lastY = this.targetY;
        this.lastZ = this.targetZ;

        do {
            this.length += this.step;
            this.hLength = this.length * this.rotYCos;
            this.yOffset = this.length * this.rotYSin;
            this.xOffset = this.hLength * this.rotXCos;
            this.zOffset = this.hLength * this.rotXSin;
            this.targetX = (int)Math.floor(this.xOffset + this.playerX);
            this.targetY = (int)Math.floor(this.yOffset + this.playerY);
            this.targetZ = (int)Math.floor(this.zOffset + this.playerZ);
        } while(this.length <= this.range && this.targetX == this.lastX && this.targetY == this.lastY && this.targetZ == this.lastZ);

        AsyncBlock block = world.getBlockAt(this.targetX, this.targetY, this.targetZ);
        Material type = block.getType();
        return !isAir(type) ? block : (this.length <= this.range && this.targetY <= 255 && this.targetY >= 0?this.getRange():this.world.getBlockAt(this.lastX, this.lastY, this.lastZ));
    }

    private void init(Location location, double range, double step, double viewHeight) {
        this.playerLoc = location;
        this.viewHeight = viewHeight;
        this.playerX = this.playerLoc.getX();
        this.playerY = this.playerLoc.getY() + this.viewHeight;
        this.playerZ = this.playerLoc.getZ();
        this.range = range;
        this.step = step;
        this.length = 0.0D;
        this.rotX = (double)((this.playerLoc.getYaw() + 90.0F) % 360.0F);
        this.rotY = (double)(this.playerLoc.getPitch() * -1.0F);
        this.rotYCos = Math.cos(Math.toRadians(this.rotY));
        this.rotYSin = Math.sin(Math.toRadians(this.rotY));
        this.rotXCos = Math.cos(Math.toRadians(this.rotX));
        this.rotXSin = Math.sin(Math.toRadians(this.rotX));
        this.targetX = (int)Math.floor(this.playerLoc.getX());
        this.targetY = (int)Math.floor(this.playerLoc.getY() + this.viewHeight);
        this.targetZ = (int)Math.floor(this.playerLoc.getZ());
        this.lastX = this.targetX;
        this.lastY = this.targetY;
        this.lastZ = this.targetZ;
    }

    public static Class<?> inject() {
        return RangeBlockHelper.class;
    }
}
