package com.boydti.fawe.command;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.factory.DefaultMaskParser;
import com.sk89q.worldedit.util.command.parametric.ParameterData;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MaskBinding extends FaweBinding {
    private final WorldEdit worldEdit;

    public MaskBinding(WorldEdit worldEdit) {
        super(worldEdit);
        this.worldEdit = worldEdit;
    }

    @Override
    public List<String> getSuggestions(ParameterData parameter, String prefix) {
        if (prefix.isEmpty()) {
            return Stream.concat(Stream.of("#"), BlockTypes.getNameSpaces().stream().map(n -> n + ":")).collect(Collectors.toList());
        }
        return super.getSuggestions(parameter, prefix);
    }
}