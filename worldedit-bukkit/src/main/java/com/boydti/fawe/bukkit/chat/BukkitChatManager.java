package com.boydti.fawe.bukkit.chat;

import com.boydti.fawe.bukkit.BukkitPlayer;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.util.chat.ChatManager;
import com.boydti.fawe.util.chat.Message;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;

public class BukkitChatManager implements ChatManager<FancyMessage> {

    @Override
    public FancyMessage builder() {
        return new FancyMessage("");
    }

    @Override
    public void color(Message message, String color) {
        message.$(this).color(ChatColor.getByChar(BBC.color(color).substring(1)));
    }

    @Override
    public void tooltip(Message message, Message... tooltips) {
        List<FancyMessage> lines = new ArrayList<>();
        for (Message tooltip : tooltips) {
            lines.add(tooltip.$(this));
        }
        message.$(this).formattedTooltip(lines);
    }

    @Override
    public void command(Message message, String command) {
        message.$(this).command(command);
    }

    @Override
    public void text(Message message, String text) {
        message.$(this).color(BBC.color(text));
    }

    @Override
    public void send(Message Message, FawePlayer player) {
        if (!(player instanceof BukkitPlayer)) {
            player.sendMessage(Message.$(this).toOldMessageFormat());
        } else {
            Message.$(this).send(((BukkitPlayer) player).parent);
        }
    }

    @Override
    public void suggest(Message Message, String command) {
        Message.$(this).suggest(command);
    }

    @Override
    public void link(Message Message, String url) {
        Message.$(this).link(url);
    }


}
