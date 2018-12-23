package com.boydti.fawe.object.brush;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.brush.visualization.VisualExtent;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;

public class LineBrush implements Brush, ResettableTool {

    private final boolean shell, select, flat;
    private BlockVector3 pos1;

    public LineBrush(boolean shell, boolean select, boolean flat) {
        this.shell = shell;
        this.select = select;
        this.flat = flat;
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, final Pattern pattern, double size) throws MaxChangedBlocksException {
        boolean visual = (editSession.getExtent() instanceof VisualExtent);
        if (pos1 == null) {
            if (!visual) {
                pos1 = position;
                BBC.BRUSH_LINE_PRIMARY.send(editSession.getPlayer(), position);
            }
            return;
        }
        editSession.drawLine(pattern, pos1, position, size, !shell, flat);
        if (!visual) {
            BBC.BRUSH_LINE_SECONDARY.send(editSession.getPlayer());
            if (!select) {
                pos1 = null;
                return;
            } else {
                pos1 = position;
            }
        }
    }

    @Override
    public boolean reset() {
        pos1 = null;
        return true;
    }
}
