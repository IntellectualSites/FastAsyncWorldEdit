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
 * @since 2.7.0
 */
public class TypeSwapPattern extends AbstractExtentPattern {

    private static final Pattern SPLITTER = Pattern.compile("[|,]");

    private final String inputString;
    private final String outputString;
    private final String[] inputs;
    private Pattern inputPattern = null;

    /**
     * Create a new instance
     *
     * @param extent       extent to use
     * @param inputString  string to replace. May be regex.
     * @param outputString string to replace with
     * @param allowRegex   if regex should be allowed for input string matching
     * @since 2.7.0
     */
    public TypeSwapPattern(Extent extent, String inputString, String outputString, boolean allowRegex) {
        super(extent);
        this.inputString = inputString;
        this.outputString = outputString;
        if (!StringMan.isAlphanumericUnd(inputString)) {
            if (allowRegex) {
                this.inputPattern = Pattern.compile(inputString.replace(",", "|"));
                inputs = null;
            } else {
                inputs = SPLITTER.split(inputString);
            }
        } else {
            inputs = null;
        }
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        BlockState existing = get.getBlock(extent);
        BlockState newBlock = getNewBlock(existing);
        if (newBlock == null) {
            return false;
        }
        return set.setBlock(extent, newBlock);
    }

    @Override
    public void applyBlock(final FilterBlock block) {
        BlockState existing = block.getBlock();
        BlockState newState = getNewBlock(existing);
        if (newState != null) {
            block.setBlock(newState);
        }
    }

    @Override
    public BaseBlock applyBlock(final BlockVector3 position) {
        BaseBlock existing = position.getFullBlock(getExtent());
        BlockState newState = getNewBlock(existing.toBlockState());
        return newState == null ? existing : newState.toBaseBlock();
    }

    private BlockState getNewBlock(BlockState existing) {
        String oldId = existing.getBlockType().getId();
        String newId = oldId;
        if (inputPattern != null) {
            newId = inputPattern.matcher(oldId).replaceAll(outputString);
        } else if (inputs != null && inputs.length > 0) {
            for (String input : inputs) {
                newId = newId.replace(input, outputString);
            }
        } else {
            newId = oldId.replace(inputString, outputString);
        }
        if (newId.equals(oldId)) {
            return null;
        }
        BlockType newType = BlockTypes.get(newId);
        if (newType == null) {
            return null;
        }
        return newType.getDefaultState().withProperties(existing);
    }

}
