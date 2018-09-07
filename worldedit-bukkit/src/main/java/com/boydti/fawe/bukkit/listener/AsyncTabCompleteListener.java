package com.boydti.fawe.bukkit.listener;

import com.boydti.fawe.util.TaskManager;
import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.server.TabCompleteEvent;

import java.util.Collections;
import java.util.List;

public class AsyncTabCompleteListener extends ATabCompleteListener {
    public AsyncTabCompleteListener(WorldEditPlugin worldEdit) {
        super(worldEdit);
        Bukkit.getPluginManager().registerEvents(this, worldEdit);
    }

    @EventHandler
    public void onTabComplete(AsyncTabCompleteEvent event) {
        if (event.isCommand()) {
            List<String> result = this.onTab(event.getBuffer(), event.getSender());
            if (result != null) {
                event.setCompletions(result);
                event.setHandled(true); // Doesn't work
            }
        }
    }
}