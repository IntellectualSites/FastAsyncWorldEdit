package com.fastasyncworldedit.forge1710.entity;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.util.TaskManager;
import com.sk89q.worldedit.extension.platform.AbstractNonPlayerActor;
import com.sk89q.worldedit.session.SessionKey;
import com.sk89q.worldedit.util.formatting.WorldEditText;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.serializer.legacy.LegacyComponentSerializer;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

/**
 * Non-player command sender (server console, command blocks, RCON).
 */
public class Forge1710CommandSender extends AbstractNonPlayerActor {

    private static final UUID CONSOLE_UUID = UUID.nameUUIDFromBytes("FAWE-Forge1710-Console".getBytes(StandardCharsets.UTF_8));

    private final ICommandSender sender;

    public Forge1710CommandSender(ICommandSender sender) {
        this.sender = sender;
    }

    @Override
    public String getName() {
        return sender.getCommandSenderName();
    }

    @Override
    public void printRaw(String msg) {
        send(msg);
    }

    @Override
    public void printDebug(String msg) {
        send(msg);
    }

    @Override
    public void print(String msg) {
        send(msg);
    }

    @Override
    public void printError(String msg) {
        send(msg);
    }

    @Override
    public void print(Component component) {
        send(LegacyComponentSerializer.legacy().serialize(WorldEditText.format(component, getLocale())));
    }

    private void send(String msg) {
        for (String part : msg.split("\n")) {
            Runnable r = () -> sender.addChatMessage(new ChatComponentText(part));
            if (Fawe.isMainThread()) {
                r.run();
            } else {
                TaskManager.taskManager().task(r);
            }
        }
    }

    @Override
    public Locale getLocale() {
        return Locale.US;
    }

    @Override
    public UUID getUniqueId() {
        return CONSOLE_UUID;
    }

    @Override
    public String[] getGroups() {
        return new String[0];
    }

    @Override
    public void checkPermission(String permission) {
    }

    @Override
    public boolean hasPermission(String permission) {
        return true;
    }

    @Override
    public void setPermission(String permission, boolean value) {
    }

    @Override
    public SessionKey getSessionKey() {
        return new SessionKey() {
            @Override
            public UUID getUniqueId() {
                return CONSOLE_UUID;
            }

            @Nullable
            @Override
            public String getName() {
                return sender.getCommandSenderName();
            }

            @Override
            public boolean isActive() {
                return true;
            }

            @Override
            public boolean isPersistent() {
                return true;
            }
        };
    }

}
