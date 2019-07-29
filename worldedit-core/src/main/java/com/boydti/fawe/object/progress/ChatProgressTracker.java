package com.boydti.fawe.object.progress;

import com.boydti.fawe.object.FawePlayer;

public class ChatProgressTracker extends DefaultProgressTracker {
    public ChatProgressTracker(FawePlayer player) {
        super(player);
        setInterval(getDelay() / 50);
    }

    @Override
    public void sendTile(String title, String sub) {
        getPlayer().sendMessage(title + sub);
    }
}
