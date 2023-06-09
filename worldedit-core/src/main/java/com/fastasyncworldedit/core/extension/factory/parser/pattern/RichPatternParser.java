package com.fastasyncworldedit.core.extension.factory.parser.pattern;

import com.fastasyncworldedit.core.command.SuggestInputParseException;
import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.extension.factory.parser.FaweParser;
import com.fastasyncworldedit.core.math.random.TrueRandom;
import com.fastasyncworldedit.core.util.StringMan;
import com.sk89q.minecraft.util.commands.CommandLocals;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.NoMatchException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.pattern.RandomPattern;
import com.sk89q.worldedit.internal.command.CommandArgParser;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.internal.util.Substring;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.event.ClickEvent;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.enginehub.piston.inject.MemoizingValueAccess;
import org.enginehub.piston.suggestion.Suggestion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RichPatternParser extends FaweParser<Pattern> {

    private static final java.util.regex.Pattern percentPatternRegex = java.util.regex.Pattern.compile("[0-9]+(\\.[0-9]*)?%.*");

    /**
     * Create a new rich pattern-parser.
     *
     * @param worldEdit the worldedit instance.
     */
    public RichPatternParser(WorldEdit worldEdit) {
        super(worldEdit);
    }

    @Override
    public Pattern parseFromInput(String input, ParserContext context) throws InputParseException {
        if (input.isEmpty()) {
            throw new SuggestInputParseException(
                    Caption.of("fawe.error.no-input-provided"),
                    () -> Stream
                            .concat(Stream.of("#", ",", "&"), BlockTypes.getNameSpaces().stream().map(n -> n + ":"))
                            .collect(Collectors.toList())
            );
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
                final String command = pe.getInput();
                String full = pe.getFull();
                Pattern pattern = null;
                double chance = 1;
                if (command.isEmpty()) {
                    pattern = parseFromInput(StringMan.join(entry.getValue(), ','), context);
                } else if (!worldEdit.getPatternFactory().containsAlias(command)) {
                    // Legacy patterns
                    char char0 = command.charAt(0);
                    boolean charPattern = input.length() > 1 && input.charAt(1) != '[';
                    if (charPattern && input.charAt(0) == '=') {
                        pattern = parseFromInput(char0 + "[" + input.substring(1) + "]", context);
                    }
                    if (char0 == '#' && command.length() > 1 && command.charAt(1) != '#') {
                        throw new SuggestInputParseException(
                                new NoMatchException(Caption.of("fawe.error.parse.unknown-pattern", full,
                                        TextComponent
                                                .of("https://intellectualsites.github.io/fastasyncworldedit-documentation/patterns/patterns"
                                                )
                                                .clickEvent(
                                                        ClickEvent.openUrl(
                                                                "https://intellectualsites.github.io/fastasyncworldedit-documentation/patterns/patterns"
                                                        ))
                                )),
                                () -> {
                                    if (full.length() == 1) {
                                        return new ArrayList<>(worldEdit.getPatternFactory().getSuggestions(""));
                                    }
                                    return new ArrayList<>(worldEdit
                                            .getPatternFactory()
                                            .getSuggestions(command.toLowerCase(Locale.ROOT)));
                                }
                        );
                    }

                    if (charPattern) {
                        if (char0 == '$' || char0 == '^' || char0 == '*' || (char0 == '#' && input.charAt(1) == '#')) {
                            pattern = worldEdit.getPatternFactory().parseWithoutRich(full, context);
                        }
                    }
                    if (pattern == null) {
                        if (command.startsWith("[")) {
                            int end = command.lastIndexOf(']');
                            pattern = parseFromInput(command.substring(1, end == -1 ? command.length() : end), context);
                        } else {
                            int percentIndex = command.indexOf('%');
                            if (percentIndex != -1 && percentPatternRegex.matcher(command).matches()) {  // Legacy percent pattern
                                chance = Expression.compile(command.substring(0, percentIndex)).evaluate();
                                String value = command.substring(percentIndex + 1);
                                if (!entry.getValue().isEmpty()) {
                                    boolean addBrackets = !value.isEmpty();
                                    if (addBrackets) {
                                        value += "[";
                                    }
                                    value += StringMan.join(entry.getValue(), " ");
                                    if (addBrackets) {
                                        value += "]";
                                    }
                                }
                                pattern = parseFromInput(value, context);
                            } else { // legacy block pattern
                                try {
                                    pattern = worldEdit.getBlockFactory().parseFromInput(pe.getFull(), context);
                                } catch (NoMatchException e) {
                                    throw new NoMatchException(Caption.of("fawe.error.parse.unknown-pattern", full,
                                            TextComponent
                                                    .of("https://intellectualsites.github.io/fastasyncworldedit-documentation/patterns/patterns"
                                                    )
                                                    .clickEvent(
                                                            com.sk89q.worldedit.util.formatting.text.event.ClickEvent.openUrl(
                                                                    "https://intellectualsites.github.io/fastasyncworldedit-documentation/patterns/patterns"
                                                            ))
                                    ));
                                }
                            }
                        }
                    }
                } else {
                    List<String> args = entry.getValue();
                    try {
                        pattern = worldEdit.getPatternFactory().parseWithoutRich(full, context);
                    } catch (InputParseException rethrow) {
                        throw rethrow;
                    } catch (Throwable e) {
                        throw SuggestInputParseException.of(e, full, () -> {
                            try {
                                String cmdArgs = ((args.isEmpty()) ? "" : " " + StringMan.join(args, " "));
                                List<Substring> split =
                                        CommandArgParser.forArgString(cmdArgs).parseArgs().toList();
                                List<String> argStrings = split
                                        .stream()
                                        .map(Substring::getSubstring)
                                        .collect(Collectors.toList());
                                MemoizingValueAccess access = getPlatform().initializeInjectedValues(() -> cmdArgs,
                                        actor,
                                        null, true
                                );
                                List<String> suggestions = getPlatform().getCommandManager().getSuggestions(
                                        access,
                                        argStrings
                                ).stream().map(Suggestion::getSuggestion).collect(Collectors.toUnmodifiableList());
                                List<String> result = new ArrayList<>();
                                if (suggestions.size() <= 2) {
                                    for (int i = 0; i < suggestions.size(); i++) {
                                        String suggestion = suggestions.get(i);
                                        if (suggestion.indexOf(' ') != 0) {
                                            String[] splitSuggestion = suggestion.split(" ");
                                            suggestion = "[" + StringMan.join(splitSuggestion, "][") + "]";
                                            result.set(i, suggestion);
                                        }
                                    }
                                }
                                return result;
                            } catch (Throwable e2) {
                                e2.printStackTrace();
                                throw new InputParseException(TextComponent.of(e2.getMessage()));
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
            throw new InputParseException(TextComponent.of(e.getMessage()), e);
        }
        if (patterns.isEmpty()) {
            return null;
        }
        if (patterns.size() == 1) {
            return patterns.get(0);
        }
        RandomPattern random = new RandomPattern(new TrueRandom());
        for (int i = 0; i < patterns.size(); i++) {
            random.add(patterns.get(i), chances.get(i));
        }
        return random;
    }

    @Override
    public List<String> getMatchedAliases() {
        return Collections.emptyList();
    }

}
