package com.boydti.fawe.object.brush;

import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.brush.Brush;

public abstract class AbstractBrush implements Brush {
    private BrushTool tool;

    public AbstractBrush(BrushTool tool) {
        this.tool = tool;
    }

    public void setTool(BrushTool tool) {
        this.tool = tool;
    }

    public BrushTool getTool() {
        return tool;
    }
}
