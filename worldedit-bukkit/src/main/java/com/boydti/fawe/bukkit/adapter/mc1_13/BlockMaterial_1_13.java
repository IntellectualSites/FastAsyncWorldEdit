package com.boydti.fawe.bukkit.adapter.mc1_13;

import com.sk89q.util.ReflectionUtil;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import net.minecraft.server.v1_13_R2.Block;
import net.minecraft.server.v1_13_R2.EnumPistonReaction;
import net.minecraft.server.v1_13_R2.IBlockData;
import net.minecraft.server.v1_13_R2.ITileEntity;
import net.minecraft.server.v1_13_R2.Material;
import org.bukkit.craftbukkit.v1_13_R2.block.data.CraftBlockData;

public class BlockMaterial_1_13 implements BlockMaterial {
    private final Block block;
    private final IBlockData defaultState;
    private final Material material;
    private final boolean isTranslucent;
    private final CraftBlockData craftBlockData;
    private final org.bukkit.Material craftMaterial;

    public BlockMaterial_1_13(Block block) {
        this(block, block.getBlockData());
    }

    public BlockMaterial_1_13(Block block, IBlockData defaultState) {
        this.block = block;
        this.defaultState = defaultState;
        this.material = defaultState.getMaterial();
        this.craftBlockData = CraftBlockData.fromData(defaultState);
        this.craftMaterial = craftBlockData.getMaterial();
        this.isTranslucent = ReflectionUtil.getField(Block.class, block, "n");
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
        return block.strength;
    }

    @Override
    public float getResistance() {
        return block.getDurability();
    }

    @Override
    public float getSlipperiness() {
        return block.n();
    }

    @Override
    public int getLightValue() {
        return defaultState.e();
    }

    @Override
    public int getLightOpacity() {
        return isTranslucent() ? 15 : 0;
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
        return !material.isAlwaysDestroyable();
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
        return material.i().rgb;
    }
}
