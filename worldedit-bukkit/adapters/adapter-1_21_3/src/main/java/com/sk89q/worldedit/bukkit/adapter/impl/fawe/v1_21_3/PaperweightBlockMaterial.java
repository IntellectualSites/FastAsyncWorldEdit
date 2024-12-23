package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_21_3;

import com.fastasyncworldedit.bukkit.adapter.BukkitBlockMaterial;
import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

public class PaperweightBlockMaterial extends BukkitBlockMaterial<Block, BlockState> {

    public PaperweightBlockMaterial(Block block) {
        this(block, block.defaultBlockState());
    }

    public PaperweightBlockMaterial(Block block, BlockState blockState) {
        super(block, blockState, CraftBlockData.fromData(blockState));
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
        return this.blockState.getLightBlock();
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
