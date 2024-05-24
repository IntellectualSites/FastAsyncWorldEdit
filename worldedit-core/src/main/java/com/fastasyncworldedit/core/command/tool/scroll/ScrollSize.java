package com.fastasyncworldedit.core.command.tool.scroll;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.entity.Player;

public class ScrollSize extends Scroll {

    public ScrollSize(BrushTool tool) {
        super(tool);
    }

    @Override
    public boolean increment(Player player, int amount) {
        int max = player.getLimit().MAX_RADIUS;
        double newSize = Math.max(0, Math.min(max == -1 ? 4095 : max, getTool().getSize() + amount));
        getTool().setSize(newSize);
        return true;
    }

}
