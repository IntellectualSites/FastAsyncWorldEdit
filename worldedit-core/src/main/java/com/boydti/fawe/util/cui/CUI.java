package com.boydti.fawe.util.cui;

import com.boydti.fawe.object.FawePlayer;
import com.sk89q.worldedit.internal.cui.CUIEvent;

public abstract class CUI {
    private final FawePlayer player;

    public CUI(FawePlayer player) {
        this.player = player;
    }

    public <T> FawePlayer<T> getPlayer() {
        return player;
    }

    public abstract void dispatchCUIEvent(CUIEvent event);
}
