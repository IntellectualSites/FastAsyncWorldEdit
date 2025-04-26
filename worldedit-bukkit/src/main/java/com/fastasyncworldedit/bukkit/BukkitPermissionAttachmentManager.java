package com.fastasyncworldedit.bukkit;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.PermissionAttachment;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public class BukkitPermissionAttachmentManager {

    private final WorldEditPlugin plugin;
    private final Map<Player, PermissionAttachment> attachments = Collections.synchronizedMap(new WeakHashMap<>());
    private PermissionAttachment noopAttachment;

    public BukkitPermissionAttachmentManager(WorldEditPlugin plugin) {
        this.plugin = plugin;
    }

    @Nullable
    public PermissionAttachment getOrAddAttachment(@Nullable Player p) {
        if (p instanceof OfflinePlayer offline) {
            p = offline.getPlayer();
        }
        if (p == null || !p.isOnline()) {
            return null; // The attachment is only used for setting permissions (e.g. when toggling bypass) so null is acceptable
        }
        if (p.hasMetadata("NPC")) {
            if (this.noopAttachment == null) {
                this.noopAttachment = new PermissionAttachment(plugin, new PermissibleBase(null));
            }
            return noopAttachment;
        }
        return attachments.computeIfAbsent(p, k -> k.addAttachment(plugin));
    }

    public void removeAttachment(@Nullable final Player p) {
        if (p == null) {
            return;
        }
        PermissionAttachment attach = attachments.remove(p);
        if (attach != null) {
            attach.remove();
        }
    }

}
