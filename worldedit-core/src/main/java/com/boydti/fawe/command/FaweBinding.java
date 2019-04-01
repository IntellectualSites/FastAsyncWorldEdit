package com.boydti.fawe.command;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.util.command.parametric.BindingHelper;

public class FaweBinding extends BindingHelper {
    private final WorldEdit worldEdit;

    public FaweBinding(WorldEdit worldEdit) {
        this.worldEdit = worldEdit;
    }

    public WorldEdit getWorldEdit() {
        return worldEdit;
    }
}
