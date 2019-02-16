package com.boydti.fawe.util.chat;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Commands;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.StringMan;
import com.sk89q.minecraft.util.commands.CommandLocals;
import com.sk89q.minecraft.util.commands.Link;
import com.sk89q.worldedit.extension.platform.CommandManager;
import com.sk89q.worldedit.util.command.CommandCallable;
import com.sk89q.worldedit.util.command.CommandMapping;
import com.sk89q.worldedit.util.command.Description;
import com.sk89q.worldedit.util.command.Dispatcher;
import com.sk89q.worldedit.util.command.Parameter;
import com.sk89q.worldedit.util.command.PrimaryAliasComparator;
import com.sk89q.worldedit.util.command.binding.Range;
import com.sk89q.worldedit.util.command.parametric.ParameterData;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;


import static com.google.common.base.Preconditions.checkNotNull;

public class UsageMessage extends Message {
    /**
     * Create a new usage box.
     *
     * @param command       the command to describe
     * @param commandString the command that was used, such as "/we" or "/brush sphere"
     */
    public UsageMessage(CommandCallable command, String commandString) {
        this(command, commandString, null);
    }

    /**
     * Create a new usage box.
     *
     * @param command       the command to describe
     * @param commandString the command that was used, such as "/we" or "/brush sphere"
     * @param locals        list of locals to use
     */
    public UsageMessage(CommandCallable command, String commandString, @Nullable CommandLocals locals) {
        checkNotNull(command);
        checkNotNull(commandString);
        if (command instanceof Dispatcher) {
            attachDispatcherUsage((Dispatcher) command, commandString, locals);
        } else {
            attachCommandUsage(command.getDescription(), commandString);
        }
    }

    private void attachDispatcherUsage(Dispatcher dispatcher, String commandString, @Nullable CommandLocals locals) {
        prefix();
        text(BBC.HELP_HEADER_SUBCOMMANDS.f());
        String prefix = !commandString.isEmpty() ? commandString + " " : "";

        List<CommandMapping> list = new ArrayList<>(dispatcher.getCommands());
        Collections.sort(list, new PrimaryAliasComparator(CommandManager.COMMAND_CLEAN_PATTERN));

        for (CommandMapping mapping : list) {
            boolean perm = locals == null || mapping.getCallable().testPermission(locals);
            newline();
            String cmd = prefix + mapping.getPrimaryAlias();
            text((perm ? BBC.HELP_ITEM_ALLOWED : BBC.HELP_ITEM_DENIED).format(cmd, mapping.getDescription().getDescription()));
            command(cmd);
        }
    }

    protected String separateArg(String arg) {
        return " " + arg;
    }

    private void attachCommandUsage(Description description, String commandString) {
        List<Parameter> params = description.getParameters();
        String[] usage;
        if (description.getUsage() != null) {
            usage = description.getUsage().split(" ", params.size());
        } else {
            usage = new String[params.size()];
            for (int i = 0; i < usage.length; i++) {
                Parameter param = params.get(i);
                boolean optional = param.isValueFlag() || param.isOptional();
                String arg;
                if (param.getFlag() != null) {
                    arg = "-" + param.getFlag();
                    if (param.isValueFlag())
                        arg += param.getName();
                } else {
                    arg = param.getName();
                    if (param.getDefaultValue() != null && param.getDefaultValue().length > 0)
                        arg += "=" + StringMan.join(param.getDefaultValue(), ",");
                }
                usage[i] = optional ? ("[" + arg + "]") : ("<" + arg + ">");
            }
        }

        prefix();
        text("&cUsage: ");
        text("&7" + commandString);
        suggestTip(commandString + " ");
        for (int i = 0; i < usage.length; i++) {
            String argStr = usage[i];
            text(separateArg(argStr.replaceAll("[\\[|\\]|<|>]", "&0$0&7")));

            if (params.isEmpty()) continue;
            Parameter param = params.get(i);

            StringBuilder tooltip = new StringBuilder();
            String command = null;
            String webpage = null;

            tooltip.append("Name: " + param.getName());
            if (param instanceof ParameterData) {
                ParameterData pd = (ParameterData) param;
                Type type = pd.getType();
                if (type instanceof Class) {
                    tooltip.append("\nType: " + ((Class) type).getSimpleName());
                }

                Range range = MainUtil.getOf(pd.getModifiers(), Range.class);
                if (range != null) {
                    String min = range.min() == Double.MIN_VALUE ? "(-∞" : ("[" + range.min());
                    String max = range.max() == Double.MAX_VALUE ? "∞)" : (range.max() + "]");
                    tooltip.append("\nRange: " + min + "," + max);
                }
                if (type instanceof Class) {
                    Link link = (Link) ((Class) type).getAnnotation(Link.class);
                    if (link != null) {
                        if (link.value().startsWith("http")) webpage = link.value();
                        else command = Commands.getAlias(link.clazz(), link.value());
                    }
                }
            }
            tooltip.append("\nOptional: " + (param.isOptional() || param.isValueFlag()));
            if (param.getDefaultValue() != null && param.getDefaultValue().length >= 0) {
                tooltip.append("\nDefault: " + param.getDefaultValue()[0]);
            } else if (argStr.contains("=")) {
                tooltip.append("\nDefault: " + argStr.split("[=|\\]|>]")[1]);
            }
            if (command != null || webpage != null) {
                tooltip.append("\nClick for more info");
            }
            tooltip(tooltip.toString());
            if (command != null) command(command);
            if (webpage != null) link(webpage);
        }

        newline();
        if (description.getHelp() != null) {
            text("&cHelp: &7" + description.getHelp());
        } else if (description.getDescription() != null) {
            text("&cDescription: &7" + description.getDescription());
        } else {
            text("No further help is available.");
        }
    }
}
