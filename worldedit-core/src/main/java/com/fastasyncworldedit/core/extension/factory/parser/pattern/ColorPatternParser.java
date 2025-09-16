package com.fastasyncworldedit.core.extension.factory.parser.pattern;

import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.extension.factory.parser.RichParser;
import com.fastasyncworldedit.core.util.MathMan;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.util.SuggestionHelper;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.util.formatting.text.TextComponent;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.stream.Stream;

public class ColorPatternParser extends RichParser<Pattern> {

    /**
     * Create a new rich parser with a defined prefix for the result, e.g. {@code #simplex}.
     *
     * @param worldEdit the worldedit instance.
     */
    public ColorPatternParser(WorldEdit worldEdit) {
        super(worldEdit, "#color", "#colour");
    }

    @Override
    public Stream<String> getSuggestions(String argumentInput, int index, ParserContext context) {
        if (index > 4) {
            return Stream.empty();
        }
        return SuggestionHelper.suggestPositiveIntegers(argumentInput);
    }

    @Override
    public Pattern parseFromInput(@Nonnull String[] input, ParserContext context) throws InputParseException {
        if (input.length != 4) {
            throw new InputParseException(Caption.of(
                    "fawe.error.command.syntax",
                    TextComponent.of(getPrefix() + "[r][g][b][a] (e.g. " + getPrefix() + "[156][100][0][120])")
            ));
        }
        Color color = new Color(
                MathMan.clamp(Integer.parseInt(input[0]), 0, 255),
                MathMan.clamp(Integer.parseInt(input[1]), 0, 255),
                MathMan.clamp(Integer.parseInt(input[2]), 0, 255),
                MathMan.clamp(Integer.parseInt(input[3]), 0, 255)
        );
        return context.requireSession().getTextureUtil().getNearestBlock(color.getRGB());
    }

}
