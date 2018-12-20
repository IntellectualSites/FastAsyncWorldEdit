package com.boydti.fawe.object.brush.scroll;

import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.entity.Player;

public class ScrollTargetOffset extends ScrollAction {
    public ScrollTargetOffset(BrushTool tool) {
        super(tool);
    }

    @Override
    public boolean increment(Player player, int amount) {
        BrushTool tool = getTool();
        tool.setTargetOffset(tool.getTargetOffset() + amount);
        return true;
    }
}
