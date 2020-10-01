package com.sk89q.worldedit.bukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

public class BukkitPermissionAttachmentManager {

    private final WorldEditPlugin plugin;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<Player, PermissionAttachment> attachments = new HashMap();

    public BukkitPermissionAttachmentManager(WorldEditPlugin plugin) {
        this.plugin = plugin;
    }

    public synchronized PermissionAttachment addAttachment(Player p) {
        PermissionAttachment attach;

        //try find attachment
        lock.readLock().lock();
        try {
            attach = attachments.get(p);
        } finally {
            lock.readLock().unlock();
        }

        if (attach == null) {
            lock.writeLock().lock();
            try {
                //check again - do not remove this
                attach = attachments.get(p);
                if (attach == null) {
                    attach = p.addAttachment(plugin);
                    attachments.put(p, attach);
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
        return attach;
    }

    public void removeAttachment(Player p) {
        PermissionAttachment attach;
        lock.writeLock().lock();
        try {
            attach = attachments.remove(p);
        } finally {
            lock.writeLock().unlock();
        }
        if (attach != null) {
            p.removeAttachment(attach);
        }
    }
}
