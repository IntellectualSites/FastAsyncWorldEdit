package com.fastasyncworldedit.core.extension.factory.parser.pattern;

import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.extension.factory.parser.RichParser;
import com.fastasyncworldedit.core.extent.clipboard.MultiClipboardHolder;
import com.fastasyncworldedit.core.function.pattern.RandomFullClipboardPattern;
import com.google.common.base.Function;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.util.SuggestionHelper;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.formatting.text.TextComponent;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class RandomFullClipboardPatternParser extends RichParser<Pattern> {

    /**
     * Create a new rich parser with a defined prefix for the result, e.g. {@code #simplex}.
     *
     * @param worldEdit the worldedit instance.
     */
    public RandomFullClipboardPatternParser(WorldEdit worldEdit) {
        super(worldEdit, "#fullcopy");
    }

    @Override
    protected Stream<String> getSuggestions(String argumentInput, int index, ParserContext context) {
        switch (index) {
            case 0:
                if (argumentInput.equals("#") || argumentInput.equals("#c")) {
                    return Stream.of("#copy", "#clipboard");
                } else if ("#copy".startsWith(argumentInput.toLowerCase(Locale.ROOT))) {
                    return Stream.of("#copy");
                } else if ("#clipboard".startsWith(argumentInput.toLowerCase(Locale.ROOT))) {
                    return Stream.of("#clipboard");
                } else {
                    return Stream.empty();
                }
            case 1:
            case 2:
                return SuggestionHelper.suggestBoolean(argumentInput);
            default:
                return Stream.empty();
        }
    }

    @Override
    protected Pattern parseFromInput(@Nonnull String[] arguments, ParserContext context) throws InputParseException {
        if (arguments.length == 0 || arguments.length > 3) {
            throw new InputParseException(Caption.of(
                    "fawe.error.command.syntax",
                    TextComponent.of(getPrefix() + "[pattern] (e.g. " + getPrefix() + "[#copy][true][false])")
            ));
        }
        try {
            boolean rotate = arguments.length >= 2 && Boolean.parseBoolean(arguments[1]);
            boolean flip = arguments.length == 3 && Boolean.parseBoolean(arguments[2]);
            List<ClipboardHolder> clipboards;
            if ("#copy".startsWith(arguments[0].toLowerCase(Locale.ROOT)) ||
                    "#clipboard".startsWith(arguments[0].toLowerCase(Locale.ROOT))) {
                ClipboardHolder clipboard = context.requireSession().getExistingClipboard();
                if (clipboard == null) {
                    throw new InputParseException(Caption.of("fawe.error.parse.no-clipboard", getPrefix()));
                }
                clipboards = Collections.singletonList(clipboard);
            } else {
                Actor player = context.requireActor();
                MultiClipboardHolder multi = ClipboardFormats.loadAllFromInput(player,
                        arguments[0], ClipboardFormats.findByAlias("fast"), false
                );
                if (multi == null) {
                    multi = ClipboardFormats.loadAllFromInput(player,
                            arguments[0], ClipboardFormats.findByAlias("mcedit"), false
                    );
                }
                if (multi == null) {
                    multi = ClipboardFormats.loadAllFromInput(player,
                            arguments[0], ClipboardFormats.findByAlias("sponge"), false
                    );
                }
                if (multi == null) {
                    throw new InputParseException(Caption.of("fawe.error.parse.no-clipboard-source", arguments[0]));
                }
                clipboards = multi.getHolders();
            }
            return new RandomFullClipboardPattern(clipboards, rotate, flip);
        } catch (IOException e) {
            throw new InputParseException(TextComponent.of(e.getMessage()), e);
        }
    }

}
