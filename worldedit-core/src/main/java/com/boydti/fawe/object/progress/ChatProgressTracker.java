package com.boydti.fawe.object.progress;

import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;

public class ChatProgressTracker extends DefaultProgressTracker {
    public ChatProgressTracker(Player player) {
        super(player);
        setInterval(getDelay() / 50);
    }

    @Override
    public void sendTile(Component title, Component sub) {
        getPlayer().print(TextComponent.builder().append(title).append("\n").append(sub).build());
    }
}
