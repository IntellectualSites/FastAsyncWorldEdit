//package com.boydti.fawe.command;
//
//import com.sk89q.worldedit.WorldEdit;
//import com.sk89q.worldedit.util.command.parametric.ParameterData;
//import com.sk89q.worldedit.world.block.BlockTypes;
//
//import java.util.List;
//import java.util.stream.Collectors;
//import java.util.stream.Stream;
//
//public class PatternBinding extends FaweBinding {
//    private final WorldEdit worldEdit;
//
//    public PatternBinding(WorldEdit worldEdit) {
//        super(worldEdit);
//        this.worldEdit = worldEdit;
//    }
//
//    @Override
//    public List<String> getSuggestions(ParameterData parameter, String prefix) {
//        if (prefix.isEmpty()) {
//            return Stream.concat(Stream.of("#"), BlockTypes.getNameSpaces().stream()).collect(Collectors.toList());
//        }
//        return super.getSuggestions(parameter, prefix);
//    }
//}
