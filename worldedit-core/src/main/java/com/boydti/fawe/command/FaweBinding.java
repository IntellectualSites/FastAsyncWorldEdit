package com.boydti.fawe.command;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.internal.command.WorldEditBinding;

public class FaweBinding extends WorldEditBinding {
    private final WorldEdit worldEdit;

    public FaweBinding(WorldEdit worldEdit) {
        super(worldEdit);
        this.worldEdit = worldEdit;
    }

    public WorldEdit getWorldEdit() {
        return worldEdit;
    }
}
