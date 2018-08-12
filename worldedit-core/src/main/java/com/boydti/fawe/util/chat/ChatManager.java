package com.boydti.fawe.util.chat;

import com.boydti.fawe.object.FawePlayer;

public interface ChatManager<T> {
    T builder();

    void color(Message message, String color);

    void tooltip(Message message, Message... tooltip);

    void command(Message message, String command);

    void text(Message message, String text);

    void send(Message message, FawePlayer player);

    void suggest(Message message, String command);

    void link(Message message, String url);
}
