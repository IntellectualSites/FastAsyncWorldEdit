package com.sk89q.worldedit.world.block;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.BlockMaterial;
import com.sk89q.worldedit.extension.platform.Capability;

public class BlockStateImpl extends BlockState {
    private final int internalId;
    private final int ordinal;
    private final BlockTypes type;
    private BlockMaterial material;

    protected BlockStateImpl(BlockTypes type, int internalId, int ordinal) {
        this.type = type;
        this.internalId = internalId;
        this.ordinal = ordinal;
    }

    public BlockMaterial getMaterial() {
        if (this.material == null) {
            if (type == BlockTypes.__RESERVED__) {
                return this.material = type.getMaterial();
            }
            synchronized (this) {
                if (this.material == null) {
                    this.material = WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.GAME_HOOKS).getRegistries().getBlockRegistry().getMaterial(this);
                }
            }
        }
        return material;
    }

    @Deprecated
    public int getInternalId() {
        return this.internalId;
    }

    @Override
    public int getOrdinal() {
        return ordinal;
    }

    @Override
    public final BlockTypes getBlockType() {
        return type;
    }
}
