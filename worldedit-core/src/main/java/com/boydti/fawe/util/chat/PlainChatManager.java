package com.boydti.fawe.util.chat;


import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FawePlayer;

import java.util.ArrayList;
import java.util.List;

public class PlainChatManager implements ChatManager<List<StringBuilder>> {

    @Override
    public List<StringBuilder> builder() {
        return new ArrayList<>();
    }

    @Override
    public void color(Message message, String color) {
        List<StringBuilder> parts = message.$(this);
        parts.get(parts.size() - 1).insert(0, color);
    }

    @Override
    public void tooltip(Message message, Message... tooltips) {
    }

    @Override
    public void command(Message message, String command) {
    }

    @Override
    public void text(Message message, String text) {
        message.$(this).add(new StringBuilder(BBC.color(text)));
    }

    @Override
    public void send(Message plotMessage, FawePlayer player) {
        StringBuilder built = new StringBuilder();
        for (StringBuilder sb : plotMessage.$(this)) {
            built.append(sb);
        }
        player.sendMessage(built.toString());
    }

    @Override
    public void suggest(Message plotMessage, String command) {
    }

    @Override
    public void link(Message message, String url) {
    }
}
