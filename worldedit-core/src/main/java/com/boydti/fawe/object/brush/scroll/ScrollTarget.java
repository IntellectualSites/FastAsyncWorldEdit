package com.boydti.fawe.object.brush.scroll;

import com.boydti.fawe.object.brush.TargetMode;
import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.entity.Player;

public class ScrollTarget extends ScrollAction {
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
