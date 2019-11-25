package com.boydti.fawe.object.brush.scroll;

import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.session.ClipboardHolder;
import java.util.List;

public class ScrollClipboard extends Scroll {
    private final List<ClipboardHolder> clipboards;
    private final LocalSession session;
    private int index = 0;

    public ScrollClipboard(BrushTool tool, LocalSession session, List<ClipboardHolder> clipboards) {
        super(tool);
        this.clipboards = clipboards;
        this.session = session;
    }

    @Override
    public boolean increment(Player player, int amount) {
        index = MathMan.wrap(index + amount, 0, clipboards.size() - 1);
        ClipboardHolder clipboard = clipboards.get(index);
        session.setClipboard(clipboard);
        return true;
    }
}
