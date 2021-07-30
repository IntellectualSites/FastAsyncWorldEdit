package com.fastasyncworldedit.core.command.tool.scroll;

import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.entity.Player;

public class ScrollTargetOffset extends Scroll {

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
