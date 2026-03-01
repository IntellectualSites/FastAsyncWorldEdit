package com.sk89q.worldedit.nukkitmot;

import cn.nukkit.command.CommandSender;
import com.sk89q.worldedit.extension.platform.AbstractNonPlayerActor;
import com.sk89q.worldedit.session.SessionKey;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.auth.AuthorizationException;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.serializer.plain.PlainComponentSerializer;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

/**
 * Adapter for Nukkit CommandSender (console) to WorldEdit Actor.
 */
public class NukkitCommandSender extends AbstractNonPlayerActor {

    private static final UUID CONSOLE_UUID = UUID.nameUUIDFromBytes("CONSOLE".getBytes(StandardCharsets.UTF_8));

    private final CommandSender sender;

    public NukkitCommandSender(CommandSender sender) {
        this.sender = sender;
    }

    @Override
    public UUID getUniqueId() {
        return CONSOLE_UUID;
    }

    @Override
    public String getName() {
        return sender.getName();
    }

    @Deprecated
    @Override
    public void printRaw(String msg) {
        sender.sendMessage(msg);
    }

    @Deprecated
    @Override
    public void print(String msg) {
        sender.sendMessage(msg);
    }

    @Deprecated
    @Override
    public void printDebug(String msg) {
        sender.sendMessage(msg);
    }

    @Deprecated
    @Override
    public void printError(String msg) {
        sender.sendMessage("Error: " + msg);
    }

    @Override
    public void print(Component component) {
        sender.sendMessage(PlainComponentSerializer.INSTANCE.serialize(component));
    }

    @Override
    public Locale getLocale() {
        return Locale.getDefault();
    }

    @Override
    public boolean hasPermission(String permission) {
        return sender.hasPermission(permission);
    }

    @Override
    public void checkPermission(String permission) throws AuthorizationException {
        if (!hasPermission(permission)) {
            throw new AuthorizationException();
        }
    }

    @Override
    public void setPermission(String permission, boolean value) {
        // Nukkit console permissions are not modifiable at runtime
    }

    @Override
    public String[] getGroups() {
        return new String[0];
    }

    @Override
    public SessionKey getSessionKey() {
        return new SessionKey() {
            @Override
            public String getName() {
                return sender.getName();
            }

            @Override
            public boolean isActive() {
                return true;
            }

            @Override
            public boolean isPersistent() {
                return true;
            }

            @Nullable
            @Override
            public UUID getUniqueId() {
                return CONSOLE_UUID;
            }
        };
    }

}
