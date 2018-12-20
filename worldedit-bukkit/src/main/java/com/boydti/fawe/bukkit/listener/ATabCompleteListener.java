package com.boydti.fawe.bukkit.listener;

import com.boydti.fawe.object.string.MutableCharSequence;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.event.platform.CommandSuggestionEvent;
import com.sk89q.worldedit.extension.platform.CommandManager;
import com.sk89q.worldedit.util.command.CommandMapping;
import com.sk89q.worldedit.util.command.Dispatcher;

import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;

import java.util.List;

public class ATabCompleteListener implements Listener {
    private final WorldEditPlugin worldEdit;

    public ATabCompleteListener(WorldEditPlugin worldEdit) {
        this.worldEdit = worldEdit;
    }
    public List<String> onTab(String buffer, CommandSender sender) {
        int firstSpace = buffer.indexOf(' ');
        if (firstSpace == -1) return null;
        MutableCharSequence mBuffer = MutableCharSequence.getTemporal();
        mBuffer.setString(buffer);
        mBuffer.setSubstring(0, firstSpace);
        int index;
        String label = buffer.substring(index = (mBuffer.indexOf(':') == -1 ? 1 : mBuffer.indexOf(':') + 1), firstSpace);
        Dispatcher dispatcher = CommandManager.getInstance().getDispatcher();
        CommandMapping weCommand = dispatcher.get(label);
        if (weCommand != null) {
            CommandSuggestionEvent event = new CommandSuggestionEvent(worldEdit.wrapCommandSender(sender), buffer.substring(index, buffer.length()));
            worldEdit.getWorldEdit().getEventBus().post(event);
            List<String> suggestions = event.getSuggestions();
            if (suggestions != null) {
                return suggestions;
            }
        }
        return null;
    }
}
