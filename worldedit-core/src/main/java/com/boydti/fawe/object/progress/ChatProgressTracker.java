package com.boydti.fawe.object.progress;

import com.sk89q.worldedit.entity.Player;

public class ChatProgressTracker extends DefaultProgressTracker {
    public ChatProgressTracker(Player player) {
        super(player);
        setInterval(getDelay() / 50);
    }

    @Override
    public void sendTile(String title, String sub) {
        getPlayer().print(title + sub);
    }
}
