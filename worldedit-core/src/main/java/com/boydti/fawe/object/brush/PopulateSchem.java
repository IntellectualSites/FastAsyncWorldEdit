package com.boydti.fawe.object.brush;

import com.boydti.fawe.jnbt.anvil.generator.SchemGen;
import com.boydti.fawe.util.MaskTraverser;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
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
    public void build(EditSession editSession, Vector position, Pattern pattern, double size) throws MaxChangedBlocksException {
        new MaskTraverser(mask).reset(editSession);
        SchemGen gen = new SchemGen(mask, editSession, clipboards, randomRotate);
        CuboidRegion cuboid = new CuboidRegion(editSession.getWorld(), position.subtract(size, size, size), position.add(size, size, size));
        try {
            editSession.addSchems(cuboid, mask, clipboards, rarity, randomRotate);
        } catch (WorldEditException e) {
            throw new RuntimeException(e);
        }
    }
}
