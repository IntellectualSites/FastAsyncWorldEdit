package com.fastasyncworldedit.forge1710.registry;

import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import javax.annotation.Nullable;

/**
 * Block material backed by a 1.7.10 {@link Block}. Values that 1.7.10 only exposes per world position fall back to the
 * block's defaults; mod blocks that throw are treated conservatively.
 */
public class Forge1710BlockMaterial implements BlockMaterial {

    private final Block block;
    private final Material material;
    private final boolean hasTile;

    public Forge1710BlockMaterial(Block block) {
        this.block = block;
        this.material = block.getMaterial();
        boolean tile;
        try {
            tile = block.hasTileEntity(0);
        } catch (Throwable t) {
            tile = false;
        }
        this.hasTile = tile;
    }

    @Override
    public boolean isAir() {
        return material == Material.air;
    }

    @Override
    public boolean isFullCube() {
        return block.renderAsNormalBlock();
    }

    @Override
    public boolean isOpaque() {
        return block.isOpaqueCube();
    }

    @Override
    public boolean isPowerSource() {
        return block.canProvidePower();
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
        try {
            return block.getBlockHardness(null, 0, 0, 0);
        } catch (Throwable t) {
            return 1.0F;
        }
    }

    @Override
    public float getResistance() {
        try {
            return block.getExplosionResistance(null);
        } catch (Throwable t) {
            return 1.0F;
        }
    }

    @Override
    public float getSlipperiness() {
        return block.slipperiness;
    }

    @Override
    public int getLightValue() {
        return block.getLightValue();
    }

    @Override
    public boolean isFragileWhenPushed() {
        return material.getMaterialMobility() == 1;
    }

    @Override
    public boolean isUnpushable() {
        return material.getMaterialMobility() == 2;
    }

    @Override
    public boolean isTicksRandomly() {
        return block.getTickRandomly();
    }

    @Override
    public boolean isMovementBlocker() {
        return material.blocksMovement();
    }

    @Override
    public boolean isBurnable() {
        return material.getCanBurn();
    }

    @Override
    public boolean isToolRequired() {
        return !material.isToolNotRequired();
    }

    @Override
    public boolean isReplacedDuringPlacement() {
        return material.isReplaceable();
    }

    @Override
    public boolean isTranslucent() {
        return !material.isOpaque();
    }

    @Override
    public boolean hasContainer() {
        return hasTile;
    }

    @Override
    public int getLightOpacity() {
        return block.getLightOpacity();
    }

    @Override
    public boolean isTile() {
        return hasTile;
    }

    @Override
    public @Nullable FaweCompoundTag defaultTile() {
        return null;
    }

    @Override
    public int getMapColor() {
        try {
            return block.getMapColor(0).colorValue;
        } catch (Throwable t) {
            return 0;
        }
    }

}
