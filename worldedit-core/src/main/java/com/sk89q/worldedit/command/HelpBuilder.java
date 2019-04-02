package com.sk89q.worldedit.command;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.util.StringMan;
import com.google.common.base.Joiner;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.CommandManager;
import com.sk89q.worldedit.util.command.*;
import com.sk89q.worldedit.util.command.parametric.AParametricCallable;

import java.util.*;

public abstract class HelpBuilder implements Runnable {
    private final CommandCallable callable;
    private final CommandContext args;
    private final String prefix;
    private final int perPage;

    public HelpBuilder(CommandCallable callable, CommandContext args, final String prefix, int perPage) {
        if (callable == null) {
            callable = WorldEdit.getInstance().getPlatformManager().getCommandManager().getDispatcher();
        }
        this.callable = callable;
        this.args = args;
        this.prefix = prefix;
        this.perPage = perPage;
    }

    @Override
    public void run() {
        try {
            CommandCallable callable = this.callable;
            int page = -1;
            String category = null;
            int effectiveLength = args.argsLength();

            // Detect page from args
            try {
                if (effectiveLength > 0) {
                    page = args.getInteger(args.argsLength() - 1);
                    if (page <= 0) {
                        page = 1;
                    } else {
                        page--;
                    }
                    effectiveLength--;
                }
            } catch (Exception ignored) {
            }

            boolean isRootLevel = true;
            List<String> visited = new ArrayList<>();

            // Create the message
            if (callable instanceof Dispatcher) {
                Dispatcher dispatcher = (Dispatcher) callable;

                // Get a list of aliases
                List<CommandMapping> aliases = new ArrayList<>(dispatcher.getCommands());
                List<String> prefixes = Collections.nCopies(aliases.size(), "");
                // Group by callable

                if (page == -1 || effectiveLength > 0) {
                    Map<String, Map<CommandMapping, String>> grouped = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                    for (CommandMapping mapping : aliases) {
                        CommandCallable c = mapping.getCallable();
                        String group;
                        if (c instanceof DelegateCallable) {
                            c = ((DelegateCallable) c).getParent();
                        }
                        if (c instanceof AParametricCallable) {
                            Command command = ((AParametricCallable) c).getCommand();
                            if (command != null && command.aliases().length != 0) {
                                group = command.aliases()[0];
                            } else {
                                group = ((AParametricCallable) c).getGroup();
                            }
                        } else if (c instanceof Dispatcher) {
                            group = mapping.getPrimaryAlias();
                        } else {
                            group = "Unsorted";
                        }
                        group = group.replace("/", "");
                        group = StringMan.toProperCase(group);
                        Map<CommandMapping, String> queue = grouped.get(group);
                        if (queue == null) {
                            queue = new LinkedHashMap<>();
                            grouped.put(group, queue);
                        }
                        if (c instanceof Dispatcher) {
                            for (CommandMapping m : ((Dispatcher) c).getCommands()) {
                                queue.put(m, mapping.getPrimaryAlias() + " ");
                            }
                        } else {
                            // Sub commands get priority
                            queue.putIfAbsent(mapping, "");
                        }
                    }
                    if (effectiveLength > 0) {
                        String cat = args.getString(0);
                        Map<CommandMapping, String> mappings = effectiveLength == 1 ? grouped.get(cat) : null;
                        if (mappings == null) {
                            // Drill down to the command
                            for (int i = 0; i < effectiveLength; i++) {
                                String command = args.getString(i);

                                if (callable instanceof Dispatcher) {
                                    // Chop off the beginning / if we're are the root level
                                    if (isRootLevel && command.length() > 1 && command.charAt(0) == '/') {
                                        command = command.substring(1);
                                    }

                                    CommandMapping mapping = UtilityCommands.detectCommand((Dispatcher) callable, command, isRootLevel);
                                    if (mapping != null) {
                                        callable = mapping.getCallable();
                                    } else {
                                        if (isRootLevel) {
                                            Set<String> found = new HashSet<>();
                                            String arg = args.getString(i).toLowerCase();
                                            String closest = null;
                                            int distance = Integer.MAX_VALUE;
                                            for (CommandMapping map : aliases) {
                                                String desc = map.getDescription().getDescription();
                                                if (desc == null) desc = map.getDescription().getHelp();
                                                if (desc == null) desc = "";
                                                String[] descSplit = desc.replaceAll("[^A-Za-z0-9]", "").toLowerCase().split(" ");
                                                for (String alias : map.getAllAliases()) {
                                                    if (alias.equals(arg)) {
                                                        closest = map.getPrimaryAlias();
                                                        distance = 0;
                                                        found.add(map.getPrimaryAlias());
                                                    } else if (alias.contains(arg)) {
                                                        closest = map.getPrimaryAlias();
                                                        distance = 1;
                                                        found.add(map.getPrimaryAlias());
                                                    } else if (StringMan.isEqualIgnoreCaseToAny(arg, descSplit)) {
                                                        closest = map.getPrimaryAlias();
                                                        distance = 1;
                                                        found.add(map.getPrimaryAlias());
                                                    } else {
                                                        int currentDist = StringMan.getLevenshteinDistance(alias, arg);
                                                        if (currentDist < distance) {
                                                            distance = currentDist;
                                                            closest = map.getPrimaryAlias();
                                                        }
                                                    }
                                                }
                                            }
                                            found.add(closest);
                                            displayFailure(BBC.HELP_SUGGEST.f(arg, StringMan.join(found, ", ")));
                                            return;
                                        } else {
                                            String msg = String.format("The sub-command '%s' under '%s' could not be found.",
                                                    command, Joiner.on(" ").join(visited));
                                            displayFailure(msg);
                                            return;
                                        }
                                    }
                                    visited.add(args.getString(i));
                                    isRootLevel = false;
                                } else {
                                    String msg = String.format("'%s' has no sub-commands. (Maybe '%s' is for a parameter?)",
                                            Joiner.on(" ").join(visited), command);
                                    displayFailure(msg);
                                    return;
                                }
                            }
                            if (!(callable instanceof Dispatcher)) {
                                // TODO interactive box
                                String cmd = (WorldEdit.getInstance().getConfiguration().noDoubleSlash ? "" : "/") + Joiner.on(" ").join(visited);
                                displayUsage(callable, cmd);
                                return;
                            }
                            dispatcher = (Dispatcher) callable;
                            aliases = new ArrayList<>(dispatcher.getCommands());
                            prefixes = Collections.nCopies(aliases.size(), "");
                        } else {
                            aliases = new ArrayList<>();
                            prefixes = new ArrayList<>();
                            for (Map.Entry<CommandMapping, String> entry : mappings.entrySet()) {
                                aliases.add(entry.getKey());
                                prefixes.add(entry.getValue());
                            }
                        }
                        page = Math.max(0, page);
                    } else if (grouped.size() > 1) {
                        displayCategories(grouped);
                        return;
                    }
                }
//            else
                {
                    Collections.sort(aliases, new PrimaryAliasComparator(CommandManager.COMMAND_CLEAN_PATTERN));

                    // Calculate pagination
                    int offset = perPage * Math.max(0, page);
                    int pageTotal = (int) Math.ceil(aliases.size() / (double) perPage);

                    // Box
                    if (offset >= aliases.size()) {
                        displayFailure(String.format("There is no page %d (total number of pages is %d).", page + 1, pageTotal));
                    } else {
                        int end = Math.min(offset + perPage, aliases.size());
                        List<CommandMapping> subAliases = aliases.subList(offset, end);
                        List<String> subPrefixes = prefixes.subList(offset, end);
                        Map<CommandMapping, String> commandMap = new LinkedHashMap<>();
                        for (int i = 0; i < subAliases.size(); i++) {
                            commandMap.put(subAliases.get(i), subPrefixes.get(i));
                        }
                        String visitedString = Joiner.on(" ").join(visited);
                        displayCommands(commandMap, visitedString, page, pageTotal, effectiveLength);
                    }
                }
            } else {
                String cmd = (WorldEdit.getInstance().getConfiguration().noDoubleSlash ? "" : "/") + Joiner.on(" ").join(visited);
                displayUsage(callable, cmd);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public abstract void displayFailure(String message);

    public abstract void displayUsage(CommandCallable callable, String command);

    public abstract void displayCategories(Map<String, Map<CommandMapping, String>> categories);

    public abstract void displayCommands(Map<CommandMapping, String> commandMap, String visited, int page, int pageTotal, int effectiveLength);
}
