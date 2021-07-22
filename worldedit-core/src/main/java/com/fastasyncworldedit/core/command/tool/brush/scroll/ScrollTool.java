package com.fastasyncworldedit.core.command.tool.brush.scroll;

import com.sk89q.worldedit.entity.Player;

public interface ScrollTool {
    boolean increment(Player player, int amount);
}
