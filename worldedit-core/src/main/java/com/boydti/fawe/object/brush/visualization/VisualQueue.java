package com.boydti.fawe.object.brush.visualization;

import com.boydti.fawe.object.task.SingleThreadIntervalQueue;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.Tool;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.util.HandSide;

public class VisualQueue extends SingleThreadIntervalQueue<Player> {

    public VisualQueue(int interval) {
        super(interval);
    }

    @Override
    public void operate(Player player) {
        LocalSession session = player.getSession();
        Tool tool = session.getTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        if (tool instanceof BrushTool) {
            BrushTool brushTool = (BrushTool) tool;
            if (brushTool.getVisualMode() != VisualMode.NONE) {
                try {
                    brushTool.visualize(player);
                } catch (Throwable e) {
                    WorldEdit.getInstance().getPlatformManager().handleThrowable(e, player);
                }
            }
        }
    }
}
