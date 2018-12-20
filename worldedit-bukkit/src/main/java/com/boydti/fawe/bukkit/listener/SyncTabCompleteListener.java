package com.boydti.fawe.bukkit.listener;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;

import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.server.TabCompleteEvent;

import java.util.List;

public class SyncTabCompleteListener extends ATabCompleteListener {
    public SyncTabCompleteListener(WorldEditPlugin worldEdit) {
        super(worldEdit);
    }

    @EventHandler
    public void onTabComplete(TabCompleteEvent event) {
        if (event.getSender() instanceof ConsoleCommandSender || event.getBuffer().startsWith("/")) {
            List<String> result = this.onTab(event.getBuffer(), event.getSender());
            if (result != null) {
                event.setCompletions(result);
            }
        }
    }
}
