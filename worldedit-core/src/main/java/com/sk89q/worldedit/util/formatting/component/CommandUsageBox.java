/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.util.formatting.component;

import com.boydti.fawe.config.BBC;
import com.sk89q.minecraft.util.commands.CommandLocals;
import com.sk89q.worldedit.extension.platform.CommandManager;
import com.sk89q.worldedit.util.command.CommandCallable;
import com.sk89q.worldedit.util.command.CommandMapping;
import com.sk89q.worldedit.util.command.Description;
import com.sk89q.worldedit.util.command.Dispatcher;
import com.sk89q.worldedit.util.command.PrimaryAliasComparator;
import com.sk89q.worldedit.util.formatting.Style;
import com.sk89q.worldedit.util.formatting.StyledFragment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A box to describe usage of a command.
 */
public class CommandUsageBox extends StyledFragment {

    /**
     * Create a new usage box.
     *
     * @param command       the command to describe
     * @param commandString the command that was used, such as "/we" or "/brush sphere"
     */
    public CommandUsageBox(CommandCallable command, String commandString) {
        this(command, commandString, null);
    }

    /**
     * Create a new usage box.
     *
     * @param command       the command to describe
     * @param commandString the command that was used, such as "/we" or "/brush sphere"
     * @param locals        list of locals to use
     */
    public CommandUsageBox(CommandCallable command, String commandString, @Nullable CommandLocals locals) {
        checkNotNull(command);
        checkNotNull(commandString);
        if (command instanceof Dispatcher) {
            attachDispatcherUsage((Dispatcher) command, commandString, locals);
        } else {
            attachCommandUsage(command.getDescription(), commandString);
        }
    }

    private void attachDispatcherUsage(Dispatcher dispatcher, String commandString, @Nullable CommandLocals locals) {
        CommandListBox box = new CommandListBox(BBC.HELP_HEADER_SUBCOMMANDS.f());
        String prefix = !commandString.isEmpty() ? commandString + " " : "";

        List<CommandMapping> list = new ArrayList<CommandMapping>(dispatcher.getCommands());
        Collections.sort(list, new PrimaryAliasComparator(CommandManager.COMMAND_CLEAN_PATTERN));

        for (CommandMapping mapping : list) {
            boolean perm = locals == null || mapping.getCallable().testPermission(locals);
            box.appendCommand(prefix + mapping.getPrimaryAlias(), mapping.getDescription().getDescription(), perm);
        }

        append(box);
    }

    private void attachCommandUsage(Description description, String commandString) {
        MessageBox box = new MessageBox(BBC.HELP_HEADER_COMMAND.f(commandString));
        StyledFragment contents = box.getContents();

        if (description.getUsage() != null) {
            contents.append(new Label().append(BBC.COMMAND_SYNTAX.f(description.getUsage())));
        } else {
            contents.createFragment(Style.GRAY);
            contents.append(new Subtle().append("Usage information is not available."));
        }

        contents.newLine();

        contents.createFragment(Style.GRAY);
        if (description.getHelp() != null) {
            contents.append(description.getHelp());
        } else if (description.getDescription() != null) {
            contents.append(description.getDescription());
        } else {
            contents.append(new Subtle().append("No further help is available."));
        }

        append(box);
    }



}
