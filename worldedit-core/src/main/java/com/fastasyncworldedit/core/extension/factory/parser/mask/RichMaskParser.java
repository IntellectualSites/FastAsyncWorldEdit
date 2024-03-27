package com.fastasyncworldedit.core.extension.factory.parser.mask;

import com.fastasyncworldedit.core.command.SuggestInputParseException;
import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.extension.factory.parser.FaweParser;
import com.fastasyncworldedit.core.function.mask.BlockMaskBuilder;
import com.fastasyncworldedit.core.function.mask.MaskUnion;
import com.fastasyncworldedit.core.util.StringMan;
import com.sk89q.minecraft.util.commands.CommandLocals;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.NoMatchException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.MaskIntersection;
import com.sk89q.worldedit.internal.command.CommandArgParser;
import com.sk89q.worldedit.internal.util.Substring;
import com.sk89q.worldedit.session.request.Request;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.event.ClickEvent;
import com.sk89q.worldedit.world.block.BaseBlock;
import org.enginehub.piston.inject.MemoizingValueAccess;
import org.enginehub.piston.suggestion.Suggestion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Attempts to parse masks given rich inputs, allowing for &amp; and ,. Also allows for nested masks
 */
public class RichMaskParser extends FaweParser<Mask> {

    /**
     * New instance
     *
     * @param worldEdit {@link WorldEdit} instance.
     */
    public RichMaskParser(WorldEdit worldEdit) {
        super(worldEdit);
    }

    @Override
    public Mask parseFromInput(String input, ParserContext context) throws InputParseException {
        if (input.isEmpty()) {
            throw new SuggestInputParseException(Caption.of("fawe.error.no-input-provided"), () -> Stream
                    .of("#", ",", "&")
                    .map(n -> n + ":")
                    .collect(Collectors.toList())
                    // TODO namespaces
            );
        }
        Extent extent = context.getExtent();
        if (extent == null) {
            extent = Request.request().getExtent();
        }
        List<List<Mask>> masks = new ArrayList<>();
        masks.add(new ArrayList<>());

        final CommandLocals locals = new CommandLocals();
        Actor actor = context.getActor();
        if (actor != null) {
            locals.put(Actor.class, actor);
        }
        try {
            List<Map.Entry<ParseEntry, List<String>>> parsed = parse(input);
            for (Map.Entry<ParseEntry, List<String>> entry : parsed) {
                ParseEntry pe = entry.getKey();
                final String command = pe.getInput();
                String full = pe.getFull();
                Mask mask = null;
                if (command.isEmpty()) {
                    mask = parseFromInput(StringMan.join(entry.getValue(), ','), context);
                } else if (!worldEdit.getMaskFactory().containsAlias(command)) {
                    // Legacy patterns
                    char char0 = command.charAt(0);
                    boolean charMask = input.length() > 1 && input.charAt(1) != '[';
                    if (charMask && input.charAt(0) == '=') {
                        mask = parseFromInput(char0 + "[" + input.substring(1) + "]", context);
                    }
                    if (char0 == '#' && command.length() > 1 && command.charAt(1) != '#') {
                        throw new SuggestInputParseException(
                                new NoMatchException(Caption.of("fawe.error.parse.unknown-mask", full,
                                        TextComponent
                                                .of("https://intellectualsites.github.io/fastasyncworldedit-documentation/patterns/patterns"
                                                )
                                                .clickEvent(ClickEvent.openUrl(
                                                        "https://intellectualsites.github.io/fastasyncworldedit-documentation/patterns/patterns"
                                                ))
                                )),
                                () -> {
                                    if (full.length() == 1) {
                                        return new ArrayList<>(worldEdit.getMaskFactory().getSuggestions("", context));
                                    }
                                    return new ArrayList<>(worldEdit
                                            .getMaskFactory()
                                            .getSuggestions(command.toLowerCase(Locale.ROOT), context));
                                }
                        );
                    }
                    // Legacy syntax
                    if (charMask) {
                        switch (char0) {
                            case '\\', '/', '{', '|', '~' -> {
                                String value = command.substring(1) + ((entry.getValue().isEmpty())
                                        ? ""
                                        : "[" + StringMan.join(
                                        entry.getValue(),
                                        "]["
                                ) + "]");
                                if (value.contains(":")) {
                                    if (value.charAt(0) == ':') {
                                        value = value.replaceFirst(":", "");
                                    }
                                    value = value.replaceAll(":", "][");
                                }
                                mask = parseFromInput(char0 + "[" + value + "]", context);
                            }
                            case '%', '$', '<', '>', '!' -> {
                                input = input.substring(input.indexOf(char0) + 1);
                                mask = parseFromInput(char0 + "[" + input + "]", context);
                                if (mask != null) {
                                    return mask;
                                }
                            }
                            case '#' -> {
                                if (!(input.charAt(1) == '#')) {
                                    break;
                                }
                                mask = worldEdit.getMaskFactory().parseWithoutRich(full, context);
                            }
                        }
                    }
                    if (mask == null) {
                        if (command.startsWith("[")) {
                            int end = command.lastIndexOf(']');
                            mask = parseFromInput(command.substring(1, end == -1 ? command.length() : end), context);
                        } else {
                            BlockMaskBuilder builder = new BlockMaskBuilder();
                            try {
                                builder.addRegex(full);
                            } catch (InputParseException ignored) {
                                builder.clear();
                                context.setPreferringWildcard(false);
                                context.setRestricted(false);
                                BaseBlock block = worldEdit.getBlockFactory().parseFromInput(full, context);
                                builder.add(block);
                            } catch (PatternSyntaxException e) {
                                throw new SuggestInputParseException(
                                        new NoMatchException(Caption.of("fawe.error.parse.unknown-mask", full,
                                                TextComponent
                                                        .of("https://intellectualsites.github.io/fastasyncworldedit-documentation/masks/masks"
                                                        )
                                                        .clickEvent(ClickEvent.openUrl(
                                                                "https://intellectualsites.github.io/fastasyncworldedit-documentation/masks/masks"
                                                        ))
                                        )),
                                        () -> {
                                            if (full.length() == 1) {
                                                return new ArrayList<>(worldEdit.getMaskFactory().getSuggestions("", context));
                                            }
                                            return new ArrayList<>(worldEdit
                                                    .getMaskFactory()
                                                    .getSuggestions(command.toLowerCase(Locale.ROOT), context));
                                        }
                                );
                            }
                            mask = builder.build(extent);
                        }
                    }
                } else {
                    List<String> args = entry.getValue();
                    try {
                        mask = worldEdit.getMaskFactory().parseWithoutRich(full, context);
                    } catch (SuggestInputParseException rethrow) {
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
                if (pe.isAnd()) {
                    masks.add(new ArrayList<>());
                }
                masks.get(masks.size() - 1).add(mask);
            }
        } catch (InputParseException rethrow) {
            throw rethrow;
        } catch (Throwable e) {
            e.printStackTrace();
            throw new InputParseException(TextComponent.of(e.getMessage()), e);
        }
        List<Mask> maskUnions = new ArrayList<>();
        for (List<Mask> maskList : masks) {
            if (maskList.size() == 1) {
                maskUnions.add(maskList.get(0));
            } else if (maskList.size() != 0) {
                maskUnions.add(new MaskUnion(maskList));
            }
        }
        if (maskUnions.size() == 1) {
            return maskUnions.get(0);
        } else if (maskUnions.size() != 0) {
            return new MaskIntersection(maskUnions);
        } else {
            return null;
        }
    }

    @Override
    public List<String> getMatchedAliases() {
        return Collections.emptyList();
    }

}
