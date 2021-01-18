package com.boydti.fawe.bukkit.adapter.mc1_16_5;

import com.sk89q.util.ReflectionUtil;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import net.minecraft.server.v1_16_R3.Block;
import net.minecraft.server.v1_16_R3.BlockAccessAir;
import net.minecraft.server.v1_16_R3.BlockBase;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.EnumPistonReaction;
import net.minecraft.server.v1_16_R3.IBlockData;
import net.minecraft.server.v1_16_R3.ITileEntity;
import net.minecraft.server.v1_16_R3.Material;
import org.bukkit.craftbukkit.v1_16_R3.block.data.CraftBlockData;

public class BlockMaterial_1_16_5 implements BlockMaterial {
    private final Block block;
    private final IBlockData defaultState;
    private final Material material;
    private final boolean isTranslucent;
    private final CraftBlockData craftBlockData;
    private final org.bukkit.Material craftMaterial;
    private final int opacity;

    public BlockMaterial_1_16_5(Block block) {
        this(block, block.getBlockData());
    }

    public BlockMaterial_1_16_5(Block block, IBlockData defaultState) {
        this.block = block;
        this.defaultState = defaultState;
        this.material = defaultState.getMaterial();
        this.craftBlockData = CraftBlockData.fromData(defaultState);
        this.craftMaterial = craftBlockData.getMaterial();
        BlockBase.Info blockInfo = ReflectionUtil.getField(Block.class, block, "aB");
        this.isTranslucent = !(boolean)ReflectionUtil.getField(BlockBase.Info.class, blockInfo, "n");
        opacity = defaultState.b(BlockAccessAir.INSTANCE, BlockPosition.ZERO);
    }

    public Block getBlock() {
        return block;
    }

    public IBlockData getState() {
        return defaultState;
    }

    public CraftBlockData getCraftBlockData() {
        return craftBlockData;
    }

    public Material getMaterial() {
        return material;
    }

    @Override
    public boolean isAir() {
        return defaultState.isAir();
    }

    @Override
    public boolean isFullCube() {
        return craftMaterial.isOccluding();
    }

    @Override
    public boolean isOpaque() {
        return material.f();
    }

    @Override
    public boolean isPowerSource() {
        return defaultState.isPowerSource();
    }

    @Override
    public boolean isLiquid() {
        return material.isLiquid();
    }

    @Override
    public boolean isSolid() {
        return material.isBuildable();
    }

    @Override
    public float getHardness() {
        return craftBlockData.getState().strength;
    }

    @Override
    public float getResistance() {
        return block.getDurability();
    }

    @Override
    public float getSlipperiness() {
        return block.getFrictionFactor();
    }

    @Override
    public int getLightValue() {
        return defaultState.f();
    }

    @Override
    public int getLightOpacity() {
        return opacity;
    }

    @Override
    public boolean isFragileWhenPushed() {
        return material.getPushReaction() == EnumPistonReaction.DESTROY;
    }

    @Override
    public boolean isUnpushable() {
        return material.getPushReaction() == EnumPistonReaction.BLOCK;
    }

    @Override
    public boolean isTicksRandomly() {
        return block.isTicking(defaultState);
    }

    @Override
    public boolean isMovementBlocker() {
        return material.isSolid();
    }

    @Override
    public boolean isBurnable() {
        return material.isBurnable();
    }

    @Override
    public boolean isToolRequired() {
        //TODO Removed in 1.16.1 Replacement not found.
        return true;
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
        return block instanceof ITileEntity;
    }

    @Override
    public int getMapColor() {
        return material.h().rgb;
    }
}
