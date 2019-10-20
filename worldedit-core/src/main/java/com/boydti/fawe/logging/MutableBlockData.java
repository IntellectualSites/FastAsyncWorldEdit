package com.boydti.fawe.logging;

import com.sk89q.worldedit.world.block.BlockType;
import org.primesoft.blockshub.api.IBlockData;

public class MutableBlockData implements IBlockData {
    public BlockType type;
    public Object data; // Usually org.bukkit.block.data.BlockData

    public final boolean isAir() {
        return type.getMaterial().isAir();
    }

    public final <T> T getData(Class<T> dataType) {
        return (T) this.data;
    }
}
