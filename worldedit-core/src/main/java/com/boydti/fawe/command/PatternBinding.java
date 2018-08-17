package com.boydti.fawe.command;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.util.command.parametric.ParameterData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PatternBinding extends FaweBinding {
    private final WorldEdit worldEdit;

    public PatternBinding(WorldEdit worldEdit) {
        super(worldEdit);
        this.worldEdit = worldEdit;
    }

    @Override
    public List<String> getSuggestions(ParameterData parameter, String prefix) {
        return super.getSuggestions(parameter, prefix);
//        int index = prefix.lastIndexOf(",|%");
//        String start = index != -1 ? prefix.substring(0, index) : "";
//        String current = index != -1 ? prefix.substring(index) : prefix;
//        if (current.isEmpty()) {
//            return MainUtil.prepend(start, Arrays.asList(HashTagPatternParser.ALL_PATTERNS));
//        }
//        if (current.startsWith("#") || current.startsWith("=")) {
//            return new ArrayList<>();
//        }
//        if ("hand".startsWith(prefix)) {
//            return MainUtil.prepend(start, Arrays.asList("hand"));
//        }
//        if ("pos1".startsWith(prefix)) {
//            return MainUtil.prepend(start, Arrays.asList("pos1"));
//        }
//        if (current.contains("|")) {
//            return new ArrayList<>();
//        }
//        String[] split2 = current.split(":");
//        if (split2.length == 2 || current.endsWith(":")) {
//            start = (start.isEmpty() ? split2[0] : start + " " + split2[0]) + ":";
//            current = split2.length == 2 ? split2[1] : "";
//            return MainUtil.prepend(start, MainUtil.filter(current, BundledBlockData.getInstance().getBlockStates(split2[0])));
//        }
//        List<String> blocks = BundledBlockData.getInstance().getBlockNames(split2[0]);
//        return MainUtil.prepend(start, blocks);
    }
}
