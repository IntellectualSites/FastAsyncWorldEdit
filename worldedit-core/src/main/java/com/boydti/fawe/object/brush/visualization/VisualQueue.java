package com.boydti.fawe.object.brush.visualization;

import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.task.SingleThreadIntervalQueue;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.Tool;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.entity.Player;

public class VisualQueue extends SingleThreadIntervalQueue<FawePlayer> {

    public VisualQueue(int interval) {
        super(interval);
    }

    @Override
    public void operate(FawePlayer fp) {
        LocalSession session = fp.getSession();
        Player player = fp.getPlayer();
        Tool tool = session.getTool(player);
        Brush brush;
        if (tool instanceof BrushTool) {
            BrushTool brushTool = (BrushTool) tool;
            if (brushTool.getVisualMode() != VisualMode.NONE) {
                try {
                    brushTool.visualize(BrushTool.BrushAction.PRIMARY, player);
                } catch (Throwable e) {
                    WorldEdit.getInstance().getPlatformManager().handleThrowable(e, player);
                }
            }
        }
    }
}