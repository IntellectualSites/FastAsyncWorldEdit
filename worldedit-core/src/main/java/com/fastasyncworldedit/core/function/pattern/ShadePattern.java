package com.fastasyncworldedit.core.function.pattern;

import com.fastasyncworldedit.core.util.TextureHolder;
import com.fastasyncworldedit.core.util.TextureUtil;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import static com.google.common.base.Preconditions.checkNotNull;

public class ShadePattern extends AbstractPattern {

    private final transient TextureUtil util;
    private final transient Extent extent;
    private final boolean darken;

    /**
     * Create a new {@link Pattern} instance
     *
     * @param extent extent to set to
     * @param holder {@link TextureHolder} to use for textures
     * @param darken if the shade should darken or lighten colours
     */
    public ShadePattern(Extent extent, TextureHolder holder, boolean darken) {
        checkNotNull(extent);
        this.extent = extent;
        this.util = holder.getTextureUtil();
        this.darken = darken;
    }

    /**
     * Create a new {@link Pattern} instance
     *
     * @param extent extent to set to
     * @param darken if the shade should darken or lighten colours
     * @param util   {@link TextureUtil} to use for textures
     * @since TODO
     */
    private ShadePattern(Extent extent, boolean darken, TextureUtil util) {
        checkNotNull(extent);
        this.extent = extent;
        this.util = util;
        this.darken = darken;
    }

    @Override
    public BaseBlock applyBlock(BlockVector3 position) {
        BlockType block = extent.getBlock(position).getBlockType();
        BlockType type;
        if (block == BlockTypes.GRASS_BLOCK) {
            int color = util.getColor(extent.getBiome(position));
            type = (darken ? util.getDarkerBlock(color) : util.getLighterBlock(color));
        } else {
            type = (darken ? util.getDarkerBlock(block) : util.getLighterBlock(block));
        }
        return type.getDefaultState().toBaseBlock();
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        BlockType type = get.getBlock(extent).getBlockType();
        BlockType newType;
        if (type == BlockTypes.GRASS_BLOCK) {
            int color = util.getColor(extent.getBiome(get));
            newType = (darken ? util.getDarkerBlock(color) : util.getLighterBlock(color));
        } else {
            newType = (darken ? util.getDarkerBlock(type) : util.getLighterBlock(type));
        }
        if (type != newType) {
            return set.setBlock(extent, newType.getDefaultState());
        }
        return false;
    }

    @Override
    public Pattern fork() {
        return new ShadePattern(extent, darken, util.fork());
    }

}
