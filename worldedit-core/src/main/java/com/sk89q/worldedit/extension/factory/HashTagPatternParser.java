package com.sk89q.worldedit.extension.factory;

import com.boydti.fawe.command.FaweParser;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        if (input.isEmpty()) return null;
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
                String command = pe.input;
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
                    if (charMask) {
                        switch (char0) {
                            case '$': {
                                command = command.substring(1);
                                String value = command + ((entry.getValue().isEmpty()) ? "" : "[" + StringMan.join(entry.getValue(), "][") + "]");
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
                                command = command.substring(percentIndex + 1);
                                if (!entry.getValue().isEmpty()) {
                                    if (!command.isEmpty()) command += " ";
                                    command += StringMan.join(entry.getValue(), " ");
                                }
                                pattern = parseFromInput(command, context);
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
                    if (!args.isEmpty()) {
                        command += " " + StringMan.join(args, " ");
                    }
                    pattern = (Pattern) dispatcher.call(command, locals, new String[0]);
                }
                if (pattern != null) {
                    patterns.add(pattern);
                    chances.add(chance);
                }
            }
        } catch (CommandException | ExpressionException e) {
            throw new RuntimeException(e);
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
