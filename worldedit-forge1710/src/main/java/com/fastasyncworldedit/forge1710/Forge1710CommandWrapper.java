package com.fastasyncworldedit.forge1710;

import com.fastasyncworldedit.forge1710.entity.Forge1710CommandSender;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.event.platform.CommandSuggestionEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.internal.util.Substring;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import org.enginehub.piston.Command;
import org.enginehub.piston.inject.InjectedValueStore;
import org.enginehub.piston.inject.Key;
import org.enginehub.piston.inject.MapBackedValueStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Bridges one WorldEdit command (and its aliases) into the 1.7.10 command manager.
 */
public class Forge1710CommandWrapper extends CommandBase {

    private final Command command;

    public Forge1710CommandWrapper(Command command) {
        this.command = command;
    }

    static Actor actorFor(ICommandSender sender) {
        if (sender instanceof EntityPlayerMP player) {
            return Forge1710Adapter.adapt(player);
        }
        return new Forge1710CommandSender(sender);
    }

    @Override
    public String getCommandName() {
        return command.getName();
    }

    @Override
    public List<String> getCommandAliases() {
        return new ArrayList<>(command.getAliases());
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/" + command.getName();
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        if (command.getCondition() == Command.Condition.TRUE) {
            return true;
        }
        Actor actor = actorFor(sender);
        InjectedValueStore store = MapBackedValueStore.create();
        store.injectValue(Key.of(Actor.class), context -> Optional.of(actor));
        return command.getCondition().satisfied(store);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        StringBuilder input = new StringBuilder(command.getName());
        for (String arg : args) {
            input.append(' ').append(arg);
        }
        WorldEdit.getInstance().getEventBus().post(new CommandEvent(actorFor(sender), input.toString()));
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        StringBuilder input = new StringBuilder(command.getName());
        for (String arg : args) {
            input.append(' ').append(arg);
        }
        CommandSuggestionEvent event = new CommandSuggestionEvent(actorFor(sender), input.toString());
        WorldEdit.getInstance().getEventBus().post(event);
        List<String> result = new ArrayList<>();
        for (Substring suggestion : event.getSuggestions()) {
            result.add(suggestion.getSubstring());
        }
        return result;
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

    @Override
    public int compareTo(Object other) {
        if (other instanceof net.minecraft.command.ICommand command) {
            return getCommandName().compareTo(command.getCommandName());
        }
        return 0;
    }

}
