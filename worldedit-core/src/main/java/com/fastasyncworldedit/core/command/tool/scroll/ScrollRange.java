package com.fastasyncworldedit.core.command.tool.scroll;

import com.fastasyncworldedit.core.util.MathMan;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.entity.Player;

public class ScrollRange extends Scroll {

    public ScrollRange(BrushTool tool) {
        super(tool);
    }

    @Override
    public boolean increment(Player player, int amount) {
        int max = player.getLimit().MAX_BRUSH_RADIUS;
        int newSize = MathMan.wrap(getTool().getRange() + amount, (int) (getTool().getSize() + 1), max);
        getTool().setRange(newSize);
        return true;
    }

}
