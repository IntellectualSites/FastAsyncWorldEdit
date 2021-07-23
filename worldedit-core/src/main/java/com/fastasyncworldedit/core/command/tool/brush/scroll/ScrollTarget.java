package com.fastasyncworldedit.core.command.tool.brush.scroll;

import com.fastasyncworldedit.core.command.tool.TargetMode;
import com.fastasyncworldedit.core.util.MathMan;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.entity.Player;

public class ScrollTarget extends Scroll {
    public ScrollTarget(BrushTool tool) {
        super(tool);
    }

    @Override
    public boolean increment(Player player, int amount) {
        TargetMode mode = getTool().getTargetMode();
        int index = mode.ordinal() + amount;
        TargetMode[] modes = TargetMode.values();
        TargetMode newMode = modes[MathMan.wrap(index, 0, modes.length - 1)];
        getTool().setTargetMode(newMode);
        return true;
    }
}
