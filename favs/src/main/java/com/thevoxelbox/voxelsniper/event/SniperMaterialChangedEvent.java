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

package com.thevoxelbox.voxelsniper.event;

import com.boydti.fawe.Fawe;
import com.thevoxelbox.voxelsniper.Sniper;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.material.MaterialData;

public class SniperMaterialChangedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Sniper sniper;
    private final BlockData originalMaterial;
    private final BlockData newMaterial;
    private final String toolId;

    public SniperMaterialChangedEvent(Sniper sniper, String toolId, BlockData originalMaterial, BlockData newMaterial) {
        super(!Fawe.get().isMainThread());
        this.sniper = sniper;
        this.originalMaterial = originalMaterial;
        this.newMaterial = newMaterial;
        this.toolId = toolId;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public BlockData getOriginalMaterial() {
        return this.originalMaterial;
    }

    public BlockData getNewMaterial() {
        return this.newMaterial;
    }

    public Sniper getSniper() {
        return this.sniper;
    }

    public String getToolId() {
        return this.toolId;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static Class<?> inject() {
        return SniperMaterialChangedEvent.class;
    }
}
