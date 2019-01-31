package com.sk89q.worldedit.world.block;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.world.registry.BlockMaterial;

public class BlockStateImpl extends BlockState {
    private final int internalId;
    private final int ordinal;
    private final BlockType type;
    private BlockMaterial material;
    private BaseBlock baseBlock;

    protected BlockStateImpl(BlockType type, int internalId, int ordinal) {
    	super(type);
        this.type = type;
        this.internalId = internalId;
        this.ordinal = ordinal;
        this.baseBlock = new BaseBlock(this);
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
    public final BlockType getBlockType() {
        return type;
    }

	@Override
	public BaseBlock toBaseBlock() {
		return this.baseBlock;
	}
}
