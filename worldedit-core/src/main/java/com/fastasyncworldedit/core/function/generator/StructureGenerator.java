package com.fastasyncworldedit.core.function.generator;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.generation.StructureType;

/**
 * Generate a structure at the given location
 *
 * @since TODO
 */
public class StructureGenerator implements RegionFunction {

    private final StructureType structureType;
    private final EditSession editSession;

    /**
     * Create a new instance.
     *
     * @param editSession   the edit session
     * @param structureType the structure type
     *
     * @since TODO
     */
    public StructureGenerator(EditSession editSession, StructureType structureType) {
        this.editSession = editSession;
        this.structureType = structureType;
    }

    @Override
    public boolean apply(BlockVector3 position) throws WorldEditException {
        return editSession.getWorld().generateStructure(structureType, editSession, position);
    }

}
