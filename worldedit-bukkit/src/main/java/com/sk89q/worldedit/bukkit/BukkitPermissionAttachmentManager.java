package com.sk89q.worldedit.bukkit;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

public class BukkitPermissionAttachmentManager {

    private final WorldEditPlugin plugin;
    private final Map<Player, PermissionAttachment> attachments = new HashMap<>();

    public BukkitPermissionAttachmentManager(WorldEditPlugin plugin) {
        this.plugin = plugin;
    }

    public PermissionAttachment getOrAddAttachment(@Nullable final Player p) {
        if (p == null) {
            return null;
        }
        PermissionAttachment attachment = attachments.get(p);

        if (attachment != null) {
            return attachment;
        }

        synchronized (this) {
            return attachments.computeIfAbsent(p, k -> k.addAttachment(plugin));
        }
    }

    public void removeAttachment(@Nullable final Player p) {
        if (p == null || attachments.get(p) == null) {
            return;
        }
        synchronized (this) {
            PermissionAttachment attach = attachments.remove(p);
            if (attach != null) {
                p.removeAttachment(attach);
            }
        }
    }
}
