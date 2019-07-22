package com.sk89q.worldedit.extension.platform.binding;

import com.sk89q.worldedit.WorldEdit;

public class CommandBindings extends Bindings {
    private final WorldEdit worldEdit;

    public CommandBindings(WorldEdit worldEdit) {
        this.worldEdit = worldEdit;
    }
}
