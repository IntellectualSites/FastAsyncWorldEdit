package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_20_R2;

import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.sk89q.util.ReflectionUtil;
import com.sk89q.worldedit.bukkit.adapter.Refraction;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import org.bukkit.craftbukkit.v1_20_R2.block.data.CraftBlockData;

import javax.annotation.Nullable;

public class PaperweightBlockMaterial implements BlockMaterial {

    private final Block block;
    private final BlockState blockState;
    private final boolean isTranslucent;
    private final CraftBlockData craftBlockData;
    private final org.bukkit.Material craftMaterial;
    private final int opacity;
    private final FaweCompoundTag tile;

    public PaperweightBlockMaterial(Block block) {
        this(block, block.defaultBlockState());
    }

    public PaperweightBlockMaterial(Block block, BlockState blockState) {
        this.block = block;
        this.blockState = blockState;
        this.craftBlockData = CraftBlockData.fromData(blockState);
        this.craftMaterial = craftBlockData.getMaterial();
        BlockBehaviour.Properties blockInfo = ReflectionUtil.getField(BlockBehaviour.class, block,
                Refraction.pickName("properties", "aN"));
        this.isTranslucent = !(boolean) ReflectionUtil.getField(BlockBehaviour.Properties.class, blockInfo,
                Refraction.pickName("canOcclude", "m")
        );
        opacity = blockState.getLightBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        BlockEntity tileEntity = !(block instanceof EntityBlock) ? null : ((EntityBlock) block).newBlockEntity(
                BlockPos.ZERO,
                blockState
        );
        tile = tileEntity == null
                ? null
                : PaperweightGetBlocks.NMS_TO_TILE.apply(tileEntity);
    }

    public Block getBlock() {
        return block;
    }

    public BlockState getState() {
        return blockState;
    }

    public CraftBlockData getCraftBlockData() {
        return craftBlockData;
    }

    @Override
    public boolean isAir() {
        return blockState.isAir();
    }

    @Override
    public boolean isFullCube() {
        return Block.isShapeFullBlock(blockState.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
    }

    @Override
    public boolean isOpaque() {
        return blockState.isOpaque();
    }

    @Override
    public boolean isPowerSource() {
        return blockState.isSignalSource();
    }

    @Override
    public boolean isLiquid() {
        // TODO: Better check ?
        return block instanceof LiquidBlock;
    }

    @Override
    public boolean isSolid() {
        // TODO: Replace
        return blockState.isSolid();
    }

    @Override
    public float getHardness() {
        return craftBlockData.getState().destroySpeed;
    }

    @Override
    public float getResistance() {
        return block.getExplosionResistance();
    }

    @Override
    public float getSlipperiness() {
        return block.getFriction();
    }

    @Override
    public int getLightValue() {
        return blockState.getLightEmission();
    }

    @Override
    public int getLightOpacity() {
        return opacity;
    }

    @Override
    public boolean isFragileWhenPushed() {
        return blockState.getPistonPushReaction() == PushReaction.DESTROY;
    }

    @Override
    public boolean isUnpushable() {
        return blockState.getPistonPushReaction() == PushReaction.BLOCK;
    }

    @Override
    public boolean isTicksRandomly() {
        return block.isRandomlyTicking(blockState);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isMovementBlocker() {
        return blockState.blocksMotion();
    }

    @Override
    public boolean isBurnable() {
        return craftMaterial.isBurnable();
    }

    @Override
    public boolean isToolRequired() {
        // Removed in 1.16.1, this is not present in higher versions
        return false;
    }

    @Override
    public boolean isReplacedDuringPlacement() {
        return blockState.canBeReplaced();
    }

    @Override
    public boolean isTranslucent() {
        return isTranslucent;
    }

    @Override
    public boolean hasContainer() {
        return block instanceof EntityBlock;
    }

    @Override
    public boolean isTile() {
        return block instanceof EntityBlock;
    }

    @Override
    public @Nullable FaweCompoundTag defaultTile() {
        return tile;
    }

    @Override
    public int getMapColor() {
        // rgb field
        return block.defaultMapColor().col;
    }

}
