package com.fastasyncworldedit.core.extension.factory.parser.pattern;

import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.extension.factory.parser.RichParser;
import com.fastasyncworldedit.core.extent.clipboard.MultiClipboardHolder;
import com.fastasyncworldedit.core.function.pattern.RandomFullClipboardPattern;
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
    protected Stream<String> getSuggestions(String argumentInput, int index) {
        switch (index) {
            case 0:
                return Stream.empty();
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
            boolean rotate = arguments.length == 2 && Boolean.getBoolean(arguments[1]);
            boolean flip = arguments.length == 3 && Boolean.getBoolean(arguments[2]);
            List<ClipboardHolder> clipboards = null;
            switch (arguments[0].toLowerCase()) {
                case "#copy":
                case "#clipboard":
                    ClipboardHolder clipboard = context.requireSession().getExistingClipboard();
                    if (clipboard == null) {
                        throw new InputParseException(Caption.of("fawe.error.parse.no-clipboard", getPrefix()));
                    }
                    clipboards = Collections.singletonList(clipboard);
                    break;
                default:
                    Actor player = context.requireActor();
                    MultiClipboardHolder multi = ClipboardFormats.loadAllFromInput(player,
                            arguments[0], ClipboardFormats.findByAlias("fast"), true
                    );
                    if (multi == null) {
                        multi = ClipboardFormats.loadAllFromInput(player,
                                arguments[0], ClipboardFormats.findByAlias("sponge"), true
                        );
                    }
                    if (multi == null) {
                        multi = ClipboardFormats.loadAllFromInput(player,
                                arguments[0], ClipboardFormats.findByAlias("mcedit"), true
                        );
                    }
                    if (multi == null) {
                        throw new InputParseException(Caption.of("fawe.error.parse.no-clipboard-source", arguments[0]));
                    }
                    clipboards = multi.getHolders();
                    break;
            }
            return new RandomFullClipboardPattern(clipboards, rotate, flip);
        } catch (IOException e) {
            throw new InputParseException(Caption.of(e.getMessage()), e);
        }
    }

}
