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

package com.thevoxelbox.voxelsniper.command;

import com.bekvon.bukkit.residence.commands.material;
import com.boydti.fawe.bukkit.favs.PatternUtil;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.thevoxelbox.voxelsniper.RangeBlockHelper;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.Sniper;
import com.thevoxelbox.voxelsniper.VoxelSniper;
import com.thevoxelbox.voxelsniper.api.command.VoxelCommand;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class VoxelVoxelCommand extends VoxelCommand {
    public VoxelVoxelCommand(VoxelSniper plugin) {
        super("VoxelVoxel", plugin);
        this.setIdentifier("v");
        this.setPermission("voxelsniper.sniper");
    }

    public boolean onCommand(Player player, String[] args) {
        Sniper sniper = this.plugin.getSniperManager().getSniperForPlayer(player);
        SnipeData snipeData = sniper.getSnipeData(sniper.getCurrentToolId());
        if(args.length == 0) {
            Block block = (new RangeBlockHelper(player, sniper.getWorld())).getTargetBlock();
            Material blockType = block.getType();

            BlockType weType = BukkitAdapter.adapt(blockType);
            if(!player.hasPermission("voxelsniper.ignorelimitations") && WorldEdit.getInstance().getConfiguration().disallowedBlocks.contains(weType.getId())) {
                player.sendMessage("You are not allowed to use " + blockType.name() + ". (WorldEdit config.yml)");
                return true;
            }

            snipeData.setVoxelId(weType.getInternalId());
            snipeData.getVoxelMessage().voxel();
            snipeData.setPattern(null, null);

            return true;
        } else {
            BlockType weType = BlockTypes.parse(args[0]);
            if(weType != null) {
                if(!player.hasPermission("voxelsniper.ignorelimitations") && WorldEdit.getInstance().getConfiguration().disallowedBlocks.contains(weType.getId())) {
                    player.sendMessage("You are not allowed to use " + weType + ".");
                    return true;
                } else {
                    snipeData.setVoxelId(weType.getInternalId());
                    snipeData.getVoxelMessage().voxel();
                    snipeData.setPattern(null, null);
                    return true;
                }
            } else {
                PatternUtil.parsePattern(player, snipeData, args[0]);
                return true;
            }
        }
    }

    public static Class<?> inject() {
        return VoxelVoxelCommand.class;
    }
}
