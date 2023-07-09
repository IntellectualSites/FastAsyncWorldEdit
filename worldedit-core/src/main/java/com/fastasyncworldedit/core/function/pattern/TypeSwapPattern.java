package com.fastasyncworldedit.core.function.pattern;

import com.fastasyncworldedit.core.extent.filter.block.FilterBlock;
import com.fastasyncworldedit.core.util.StringMan;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.regex.Pattern;

/**
 * Pattern that replaces blocks based on their ID, matching for an "input" and replacing with an "output" string. The "input"
 * string may be regex. Keeps as much of the block state as possible, excluding NBT data.
 *
 * @since TODO
 */
public class TypeSwapPattern extends AbstractExtentPattern {

    private final String inputString;
    private final String outputString;
    private Pattern inputPattern = null;

    /**
     * Create a new instance
     *
     * @param extent       extent to use
     * @param inputString  string to replace. May be regex.
     * @param outputString string to replace with
     * @since TODO
     */
    public TypeSwapPattern(Extent extent, String inputString, String outputString) {
        super(extent);
        this.inputString = inputString;
        this.outputString = outputString;
        if (!StringMan.isAlphanumericUnd(inputString)) {
            this.inputPattern = Pattern.compile(inputString);
        }
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        BlockState existing = get.getBlock(extent);
        String oldId = existing.getBlockType().getId();
        String newId;
        if (inputPattern != null) {
            newId = inputPattern.matcher(oldId).replaceAll(outputString);
        } else {
            newId = oldId.replace(inputString, outputString);
        }
        if (newId.equals(oldId)) {
            return false;
        }
        BlockType newType = BlockTypes.get(newId);
        if (newType == null) {
            return false;
        }
        BlockState newBlock = newType.getDefaultState().withProperties(existing);
        return set.setBlock(extent, newBlock);
    }

    @Override
    public void applyBlock(final FilterBlock block) {
        BlockState existing = block.getBlock();
        String oldId = existing.getBlockType().getId();
        String newId;
        if (inputPattern != null) {
            newId = inputPattern.matcher(oldId).replaceAll(outputString);
        } else {
            newId = oldId.replace(inputString, outputString);
        }
        if (newId.equals(oldId)) {
            return;
        }
        BlockType newType = BlockTypes.get(newId);
        if (newType == null) {
            return;
        }
        BlockState newBlock = newType.getDefaultState().withProperties(existing);
        block.setBlock(newBlock);
    }

    @Override
    public BaseBlock applyBlock(final BlockVector3 position) {
        BaseBlock existing = position.getFullBlock(getExtent());
        String oldId = existing.getBlockType().getId();
        String newId;
        if (inputPattern != null) {
            newId = inputPattern.matcher(oldId).replaceAll(outputString);
        } else {
            newId = oldId.replace(inputString, outputString);
        }
        if (newId.equals(oldId)) {
            return existing;
        }
        BlockType newType = BlockTypes.get(newId);
        if (newType == null) {
            return existing;
        }
        return newType.getDefaultState().withProperties(existing.toBlockState()).toBaseBlock(newType.getDefaultState().getNbtReference());
    }

}
