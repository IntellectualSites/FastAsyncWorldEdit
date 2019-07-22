package com.sk89q.worldedit.extension.platform.binding;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.internal.annotation.Validate;

public class ConsumeBindings extends Bindings {
    private final WorldEdit worldEdit;

    public ConsumeBindings(WorldEdit worldEdit) {
        this.worldEdit = worldEdit;
    }

}
