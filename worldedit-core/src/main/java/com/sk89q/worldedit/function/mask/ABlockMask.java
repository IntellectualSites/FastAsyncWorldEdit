package com.sk89q.worldedit.function.mask;

import com.boydti.fawe.util.StringMan;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.ArrayList;
import java.util.List;

public abstract class ABlockMask extends AbstractExtentMask {
    public ABlockMask(Extent extent) {
        super(extent);
    }

    public abstract boolean test(BlockState state);

    @Override
    public String toString() {
        List<String> strings = new ArrayList<>();
        for (BlockType type : BlockTypes.values) {
            if (type != null) {
                boolean hasAll = true;
                boolean hasAny = false;
                List<BlockState> all = type.getAllStates();
                for (BlockState state : all) {
                    hasAll &= test(state);
                    hasAny = true;
                }
                if (hasAll) {
                    strings.add(type.getId());
                } else if (hasAny) {
                    for (BlockState state : all) {
                        if (test(state)) {
                            strings.add(state.getAsString());
                        }
                    }
                }
            }
        }
        return StringMan.join(strings, ",");
    }

    @Override
    public Mask tryCombine(Mask mask) {
        if (mask instanceof ABlockMask) {
            ABlockMask other = (ABlockMask) mask;
            BlockMask newMask = new BlockMask(getExtent());
            for (BlockState state : BlockTypes.states) {
                if (state != null) {
                    if (test(state) && other.test(state)) {
                        newMask.add(state);
                    }
                }
            }
            Mask tmp = newMask.tryOptimize();
            if (tmp == null) tmp = newMask;
            return tmp;
        }
        return null;
    }

    @Override
    public Mask tryOr(Mask mask) {
        if (mask instanceof ABlockMask) {
            ABlockMask other = (ABlockMask) mask;
            BlockMask newMask = new BlockMask(getExtent());
            for (BlockState state : BlockTypes.states) {
                if (state != null) {
                    if (test(state) || other.test(state)) {
                        newMask.add(state);
                    }
                }
            }
            Mask tmp = newMask.tryOptimize();
            if (tmp == null) tmp = newMask;
            return tmp;
        }
        return null;
    }
}
