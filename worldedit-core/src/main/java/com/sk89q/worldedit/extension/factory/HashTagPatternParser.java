package com.sk89q.worldedit.extension.factory;

import com.boydti.fawe.command.FaweParser;
import com.boydti.fawe.command.SuggestInputParseException;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.random.TrueRandom;
import com.boydti.fawe.util.StringMan;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandLocals;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.PatternCommands;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.NoMatchException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.pattern.RandomPattern;
import com.sk89q.worldedit.internal.command.ActorAuthorizer;
import com.sk89q.worldedit.internal.command.WorldEditBinding;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.util.command.Dispatcher;
import com.sk89q.worldedit.util.command.SimpleDispatcher;
import com.sk89q.worldedit.util.command.parametric.ParametricBuilder;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HashTagPatternParser extends FaweParser<Pattern> {
    private final Dispatcher dispatcher;

    public HashTagPatternParser(WorldEdit worldEdit) {
        super(worldEdit);
        this.dispatcher = new SimpleDispatcher();
        this.register(new PatternCommands(worldEdit));
    }

    @Override
    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    public void register(Object clazz) {
        ParametricBuilder builder = new ParametricBuilder();
        builder.setAuthorizer(new ActorAuthorizer());
        builder.addBinding(new WorldEditBinding(worldEdit));
        builder.registerMethodsAsCommands(dispatcher, clazz);
    }

    @Override
    public Pattern parseFromInput(String input, ParserContext context) throws InputParseException {
        if (input.isEmpty()) {
            throw new SuggestInputParseException("No input provided", "", () -> Stream.concat(Stream.of("#", ",", "&"), BlockTypes.getNameSpaces().stream().map(n -> n + ":")).collect(Collectors.toList()));
        }
        List<Double> chances = new ArrayList<>();
        List<Pattern> patterns = new ArrayList<>();
        final CommandLocals locals = new CommandLocals();
        Actor actor = context != null ? context.getActor() : null;
        if (actor != null) {
            locals.put(Actor.class, actor);
        }
        try {
            for (Map.Entry<ParseEntry, List<String>> entry : parse(input)) {
                ParseEntry pe = entry.getKey();
                final String command = pe.input;
                String full = pe.full;
                Pattern pattern = null;
                double chance = 1;
                if (command.isEmpty()) {
                    pattern = parseFromInput(StringMan.join(entry.getValue(), ','), context);
                } else if (dispatcher.get(command) == null) {
                    // Legacy patterns
                    char char0 = command.charAt(0);
                    boolean charMask = input.length() > 1 && input.charAt(1) != '[';
                    if (charMask && input.charAt(0) == '=') {
                        return parseFromInput(char0 + "[" + input.substring(1) + "]", context);
                    }
                    if (char0 == '#') {
                        throw new SuggestInputParseException(new NoMatchException("Unknown pattern: " + full + ", See: //patterns"), full,
                                () -> {
                                    if (full.length() == 1) return new ArrayList<>(dispatcher.getPrimaryAliases());
                                    return dispatcher.getAliases().stream().filter(
                                            s -> s.startsWith(command.toLowerCase())
                                    ).collect(Collectors.toList());
                                }
                        );
                    }


                    if (charMask) {
                        switch (char0) {
                            case '$': {
                                String value = command.substring(1) + ((entry.getValue().isEmpty()) ? "" : "[" + StringMan.join(entry.getValue(), "][") + "]");
                                if (value.contains(":")) {
                                    if (value.charAt(0) == ':') value.replaceFirst(":", "");
                                    value = value.replaceAll(":", "][");
                                }
                                pattern = parseFromInput(char0 + "[" + value + "]", context);
                                break;
                            }
                        }
                    }
                    if (pattern == null) {
                        if (command.startsWith("[")) {
                            int end = command.lastIndexOf(']');
                            pattern = parseFromInput(command.substring(1, end == -1 ? command.length() : end), context);
                        } else {
                            int percentIndex = command.indexOf('%');
                            if (percentIndex != -1) {  // Legacy percent pattern
                                chance = Expression.compile(command.substring(0, percentIndex)).evaluate();
                                String value = command.substring(percentIndex + 1);
                                if (!entry.getValue().isEmpty()) {
                                    if (!value.isEmpty()) value += " ";
                                    value += StringMan.join(entry.getValue(), " ");
                                }
                                pattern = parseFromInput(value, context);
                            } else { // legacy block pattern
                                try {
                                    pattern = worldEdit.getBlockFactory().parseFromInput(pe.full, context);
                                } catch (NoMatchException e) {
                                    throw new NoMatchException(e.getMessage() + " See: //patterns");
                                }
                            }
                        }
                    }
                } else {
                    List<String> args = entry.getValue();
                    String cmdArgs = ((args.isEmpty()) ? "" : " " + StringMan.join(args, " "));
                    try {
                        pattern = (Pattern) dispatcher.call(command + cmdArgs, locals, new String[0]);
                    } catch (SuggestInputParseException rethrow) {
                        throw rethrow;
                    } catch (Throwable e) {
                        throw SuggestInputParseException.of(e, full, () -> {
                            try {
                                List<String> suggestions = dispatcher.get(command).getCallable().getSuggestions(cmdArgs, locals);
                                if (suggestions.size() <= 2) {
                                    for (int i = 0; i < suggestions.size(); i++) {
                                        String suggestion = suggestions.get(i);
                                        if (suggestion.indexOf(' ') != 0) {
                                            String[] split = suggestion.split(" ");
                                            suggestion = BBC.color("[" + StringMan.join(split, "][") + "]");
                                            suggestions.set(i, suggestion);
                                        }
                                    }
                                }
                                return suggestions;
                            } catch (CommandException e1) {
                                throw new InputParseException(e1.getMessage());
                            } catch (Throwable e2) {
                                e2.printStackTrace();
                                throw new InputParseException(e2.getMessage());
                            }
                        });
                    }
                }
                if (pattern != null) {
                    patterns.add(pattern);
                    chances.add(chance);
                }
            }
        } catch (InputParseException rethrow) {
            throw rethrow;
        } catch (Throwable e) {
            e.printStackTrace();
            throw new InputParseException(e.getMessage(), e);
        }
        if (patterns.isEmpty()) {
            return null;
        } else if (patterns.size() == 1) {
            return patterns.get(0);
        } else {
            RandomPattern random = new RandomPattern(new TrueRandom());
            for (int i = 0; i < patterns.size(); i++) {
                random.add(patterns.get(i), chances.get(i));
            }
            return random;
        }
    }


}
