package com.boydti.fawe.object.brush;

import com.boydti.fawe.util.MaskTraverser;
import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import java.util.List;

public class PopulateSchem implements Brush {
    private final Mask mask;
    private final boolean randomRotate;
    private final List<ClipboardHolder> clipboards;
    private final int rarity;

    public PopulateSchem(Mask mask, List<ClipboardHolder> clipboards, int rarity, boolean randomRotate) {
        this.mask = mask;
        this.clipboards = clipboards;
        this.rarity = rarity;
        this.randomRotate = randomRotate;
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) throws MaxChangedBlocksException {
        new MaskTraverser(mask).reset(editSession);
        int size1 = MathMan.roundInt(size);
        CuboidRegion cuboid = new CuboidRegion(editSession.getWorld(), position.subtract(size1, size1, size1), position.add(size1, size1, size1));
        try {
            editSession.addSchems(cuboid, mask, clipboards, rarity, randomRotate);
        } catch (WorldEditException e) {
            throw new RuntimeException(e);
        }
    }
}
