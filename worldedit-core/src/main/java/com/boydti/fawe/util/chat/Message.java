package com.boydti.fawe.util.chat;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FawePlayer;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.Actor;
import java.util.Objects;

public class Message {

    private Object builder;
    private boolean active;

    public Message() {
        try {
            reset(Fawe.get().getChatManager());
        } catch (Throwable e) {
            Fawe.debug("Doesn't support fancy chat for " + Fawe.imp().getPlatform());
            Fawe.get().setChatManager(new PlainChatManager());
            reset(Fawe.get().getChatManager());
        }
        active = !(Fawe.get().getChatManager() instanceof PlainChatManager);
    }

    public Message(BBC caption, Object... args) {
        this(BBC.getPrefix() + caption.format(args));
    }

    public Message(String text) {
        this();
        text(text);
    }

    public <T> T $(ChatManager<T> manager) {
        return (T) this.builder;
    }

    public <T> T reset(ChatManager<T> manager) {
        return (T) (this.builder = manager.builder());
    }

    public Message activeText(String text) {
        if (active) {
            text(text);
        }
        return this;
    }

    public boolean supportsInteraction() {
        return active;
    }

    public Message text(BBC caption, Object... args) {
        return text(caption.format(args));
    }

    public Message text(Object text) {
        Fawe.get().getChatManager().text(this, BBC.color(Objects.toString(text)));
        return this;
    }

    public Message link(String text) {
        Fawe.get().getChatManager().link(this, text);
        return this;
    }

    public Message tooltip(Message... tooltip) {
        Fawe.get().getChatManager().tooltip(this, tooltip);
        return this;
    }

    public Message tooltip(String tooltip) {
        return tooltip(new Message(tooltip));
    }

    public Message command(String command) {
        Fawe.get().getChatManager().command(this, (WorldEdit.getInstance().getConfiguration().noDoubleSlash ? "" : "/") + command);
        return this;
    }

    public Message prefix() {
        return text(BBC.getPrefix());
    }

    public Message newline() {
        return text("\n");
    }

    public Message cmdTip(String commandAndTooltip) {
        return tooltip(commandAndTooltip).command(commandAndTooltip);
    }

    public Message linkTip(String linkAndTooltip) {
        return tooltip(linkAndTooltip).link(linkAndTooltip);
    }

    public Message cmdOptions(String prefix, String suffix, String... options) {
        for (int i = 0; i < options.length; i++) {
            if (i != 0) text(" &8|&7 ");
            text("&7[&a" + options[i] + "&7]")
            .cmdTip(prefix + options[i] + suffix);
        }
        return this;
    }

    public Message suggestTip(String commandAndTooltip) {
        return tooltip(commandAndTooltip).suggest(commandAndTooltip);
    }

    public Message suggest(String command) {
        Fawe.get().getChatManager().suggest(this, command);
        return this;
    }

    public Message color(String color) {
        Fawe.get().getChatManager().color(this, BBC.color(color));
        return this;
    }

    public void send(Actor player) {
        send(FawePlayer.wrap(player));
    }

    public void send(FawePlayer player) {
        Fawe.get().getChatManager().send(this, player);
    }

    public Message paginate(String baseCommand, int page, int totalPages) {
        if (!active) {
            return text(BBC.PAGE_FOOTER.f(baseCommand, page + 1));
        }
        if (page < totalPages && page > 1) { // Back | Next
            this.text("&f<<").command(baseCommand + " " + (page - 1)).text("&8 | ").text("&f>>")
                    .command(baseCommand + " " + (page + 1));
        } else if (page <= 1 && totalPages > page) { // Next
            this.text("&8 -").text(" | ").text("&f>>")
                    .command(baseCommand + " " + (page + 1));

        } else if (page == totalPages && totalPages > 1) { // Back
            this.text("&f<<").command(baseCommand + " " + (page - 1)).text("&8 | ").text("- ");
        } else {
            this.text("&8 - | - ");
        }
        return this;
    }
}
