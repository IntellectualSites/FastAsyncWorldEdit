/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.extent.world;

import com.fastasyncworldedit.core.util.TaskManager;
import com.fastasyncworldedit.core.util.task.RunnableVal;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.enginehub.linbus.tree.LinCompoundTag;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Makes changes to the world as if a player had done so during survival mode.
 *
 * <p>Note that this extent may choose to not call the underlying
 * extent and may instead call methods on the {@link World} that is passed
 * in the constructor. For that reason, if you wish to "catch" changes, you
 * should catch them before the changes reach this extent.</p>
 */
public class SurvivalModeExtent extends AbstractDelegateExtent {

    private final World world;
    private boolean toolUse = false;
    private boolean stripNbt = false;

    /**
     * Create a new instance.
     *
     * @param extent the extent
     * @param world  the world
     */
    public SurvivalModeExtent(Extent extent, World world) {
        super(extent);
        checkNotNull(world);
        this.world = world;
    }

    /**
     * Return whether changes to the world should be simulated with the
     * use of game tools (such as pickaxes) whenever possible and reasonable.
     *
     * <p>For example, we could pretend that the act of setting a coal ore block
     * to air (nothing) was the act of a player mining that coal ore block
     * with a pickaxe, which would mean that a coal item would be dropped.</p>
     *
     * @return true if tool use is to be simulated
     */
    public boolean hasToolUse() {
        return toolUse;
    }

    /**
     * Set whether changes to the world should be simulated with the
     * use of game tools (such as pickaxes) whenever possible and reasonable.
     *
     * @param toolUse true if tool use is to be simulated
     * @see #hasToolUse() for an explanation
     */
    public void setToolUse(boolean toolUse) {
        this.toolUse = toolUse;
    }

    public boolean hasStripNbt() {
        return stripNbt;
    }

    public void setStripNbt(boolean stripNbt) {
        this.stripNbt = stripNbt;
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 location, B block) throws WorldEditException {
        if (toolUse && block.getBlockType().getMaterial().isAir()) {
            Collection<BaseItemStack> drops = world.getBlockDrops(location);
            boolean canSet = super.setBlock(location, block);
            if (canSet) {
                TaskManager.taskManager().sync(new RunnableVal<>() {
                    @Override
                    public void run(Object value) {
                        for (BaseItemStack stack : drops) {
                            world.dropItem(location.toVector3(), stack);
                        }
                    }
                });

                return true;
            } else {
                return false;
            }
        } else {
            // Can't be an inlined check due to inconsistent generic return type
            if (stripNbt) {
                return super.setBlock(location, block.toBaseBlock((LinCompoundTag) null));
            } else {
                return super.setBlock(location, block);
            }
        }
    }

}
