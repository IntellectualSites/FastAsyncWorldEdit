package com.fastasyncworldedit.core.extension.factory.parser.mask;

import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.extension.factory.parser.RichParser;
import com.fastasyncworldedit.core.function.mask.AngleMask;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.util.SuggestionHelper;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.util.formatting.text.TextComponent;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

public class AngleMaskParser extends RichParser<Mask> {

    private final String[] flags = new String[]{"-o"};

    public AngleMaskParser(WorldEdit worldEdit) {
        super(worldEdit, "/", "#angle", "\\");
    }

    @Override
    protected Stream<String> getSuggestions(String argumentInput, int index, ParserContext context) {
        if (index == 0 || index == 1) {
            return SuggestionHelper.suggestPositiveDoubles(argumentInput).flatMap(s -> Stream.of(s, s + "d"));
        } else if (index > 1 && index <= 1 + flags.length) {
            return Stream.of(flags);
        }
        return Stream.empty();
    }

    @Override
    protected Mask parseFromInput(@Nonnull String[] arguments, ParserContext context) throws InputParseException {
        if (arguments.length < 2 || arguments.length > 2 + flags.length) {
            return null;
        }
        String minArg = arguments[0];
        String maxArg = arguments[1];
        boolean degree = minArg.endsWith("d");
        if (degree ^ maxArg.endsWith("d")) {
            throw new InputParseException(Caption.of("fawe.error.mask.angle"));
        }
        boolean overlay = false;
        if (arguments.length > 2) {
            for (int index = 2; index < 2 + flags.length; index++) {
                String flag = arguments[index];
                if (flag.equals("-o")) {
                    overlay = true;
                } else {
                    throw new InputParseException(Caption.of(
                            "fawe.error.invalid-flag",
                            TextComponent.of(flag)
                    ));
                }
            }
        }
        double min;
        double max;
        if (degree) {
            double minDeg = Double.parseDouble(minArg.substring(0, minArg.length() - 1));
            double maxDeg = Double.parseDouble(maxArg.substring(0, maxArg.length() - 1));
            min = (Math.tan(minDeg * (Math.PI / 180)));
            max = (Math.tan(maxDeg * (Math.PI / 180)));
        } else {
            min = Double.parseDouble(minArg);
            max = Double.parseDouble(maxArg);
        }

        return new AngleMask(context.getExtent(), min, max, overlay, 1);
    }

}
