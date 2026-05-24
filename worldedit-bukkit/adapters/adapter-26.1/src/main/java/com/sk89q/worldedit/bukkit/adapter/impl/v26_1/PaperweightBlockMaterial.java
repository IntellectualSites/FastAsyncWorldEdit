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

package com.sk89q.worldedit.bukkit.adapter.impl.v26_1;

import com.fastasyncworldedit.bukkit.adapter.BukkitBlockMaterial;
import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.sk89q.worldedit.bukkit.adapter.impl.fawe.v26_1.PaperweightGetBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;

public class PaperweightBlockMaterial extends BukkitBlockMaterial<Block, BlockState> {

    public PaperweightBlockMaterial(Block block) {
        this(block, block.defaultBlockState());
    }

    public PaperweightBlockMaterial(Block block, BlockState blockState) {
        super(block, blockState, blockState.asBlockData());
    }

    @Override
    protected FaweCompoundTag tileForBlock(final Block block) {
        BlockEntity tileEntity = !(block instanceof EntityBlock eb) ? null : eb.newBlockEntity(BlockPos.ZERO, this.blockState);
        return tileEntity == null ? null : PaperweightGetBlocks.NMS_TO_TILE.apply(tileEntity);
    }

    @Override
    public boolean isAir() {
        return this.blockState.isAir();
    }

    @Override
    public boolean isFullCube() {
        return Block.isShapeFullBlock(this.blockState.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
    }

    @Override
    public boolean isOpaque() {
        return this.blockState.canOcclude();
    }

    @Override
    public boolean isPowerSource() {
        return this.blockState.isSignalSource();
    }

    @Override
    public boolean isLiquid() {
        return !this.blockState.getFluidState().is(Fluids.EMPTY);
    }

    @Override
    public boolean isSolid() {
        return this.blockState.isSolidRender();
    }

    @Override
    public float getHardness() {
        return this.blockState.destroySpeed;
    }

    @Override
    public float getResistance() {
        return this.block.getExplosionResistance();
    }

    @Override
    public float getSlipperiness() {
        return this.block.getFriction();
    }

    @Override
    public int getLightValue() {
        return this.blockState.getLightEmission();
    }

    @Override
    public int getLightOpacity() {
        return this.blockState.getLightDampening();
    }

    @Override
    public boolean isFragileWhenPushed() {
        return this.blockState.getPistonPushReaction() == PushReaction.DESTROY;
    }

    @Override
    public boolean isUnpushable() {
        return this.blockState.getPistonPushReaction() == PushReaction.BLOCK;
    }

    @Override
    public boolean isTicksRandomly() {
        return this.blockState.isRandomlyTicking();
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isMovementBlocker() {
        return this.blockState.blocksMotion();
    }

    @Override
    public boolean isReplacedDuringPlacement() {
        return this.blockState.canBeReplaced();
    }

    @Override
    public boolean isTranslucent() {
        return !this.blockState.canOcclude();
    }

    @Override
    public int getMapColor() {
        // rgb field
        return this.block.defaultMapColor().col;
    }

}
