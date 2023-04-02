package com.sk89q.worldedit.folia.adapter.impl.fawe.v1_19_R3;

import com.google.common.base.Suppliers;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.util.ReflectionUtil;
import com.sk89q.worldedit.bukkit.adapter.Refraction;
import com.sk89q.worldedit.folia.adapter.impl.fawe.v1_19_R3.nbt.PaperweightLazyCompoundTag;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.PushReaction;
import org.bukkit.craftbukkit.v1_19_R3.block.data.CraftBlockData;

public class PaperweightBlockMaterial implements BlockMaterial {

    private final Block block;
    private final BlockState blockState;
    private final Material material;
    private final boolean isTranslucent;
    private final CraftBlockData craftBlockData;
    private final org.bukkit.Material craftMaterial;
    private final int opacity;
    private final CompoundTag tile;

    public PaperweightBlockMaterial(Block block) {
        this(block, block.defaultBlockState());
    }

    public PaperweightBlockMaterial(Block block, BlockState blockState) {
        this.block = block;
        this.blockState = blockState;
        this.material = blockState.getMaterial();
        this.craftBlockData = CraftBlockData.fromData(blockState);
        this.craftMaterial = craftBlockData.getMaterial();
        BlockBehaviour.Properties blockInfo = ReflectionUtil.getField(BlockBehaviour.class, block,
                Refraction.pickName("properties", "aP"));
        this.isTranslucent = !(boolean) ReflectionUtil.getField(BlockBehaviour.Properties.class, blockInfo,
                Refraction.pickName("canOcclude", "n")
        );
        opacity = blockState.getLightBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        BlockEntity tileEntity = !(block instanceof EntityBlock) ? null : ((EntityBlock) block).newBlockEntity(
                BlockPos.ZERO,
                blockState
        );
        tile = tileEntity == null
                ? null
                : new PaperweightLazyCompoundTag(Suppliers.memoize(tileEntity::saveWithId));
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

    public Material getMaterial() {
        return material;
    }

    @Override
    public boolean isAir() {
        return blockState.isAir();
    }

    @Override
    public boolean isFullCube() {
        return craftMaterial.isOccluding();
    }

    @Override
    public boolean isOpaque() {
        return material.isSolidBlocking();
    }

    @Override
    public boolean isPowerSource() {
        return blockState.isSignalSource();
    }

    @Override
    public boolean isLiquid() {
        return material.isLiquid();
    }

    @Override
    public boolean isSolid() {
        return material.isSolid();
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
        return material.getPushReaction() == PushReaction.DESTROY;
    }

    @Override
    public boolean isUnpushable() {
        return material.getPushReaction() == PushReaction.BLOCK;
    }

    @Override
    public boolean isTicksRandomly() {
        return block.isRandomlyTicking(blockState);
    }

    @Override
    public boolean isMovementBlocker() {
        return material.isSolid();
    }

    @Override
    public boolean isBurnable() {
        return material.isFlammable();
    }

    @Override
    public boolean isToolRequired() {
        // Removed in 1.16.1, this is not present in higher versions
        return false;
    }

    @Override
    public boolean isReplacedDuringPlacement() {
        return material.isReplaceable();
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
    public CompoundTag getDefaultTile() {
        return tile;
    }

    @Override
    public int getMapColor() {
        // rgb field
        return material.getColor().col;
    }

}
