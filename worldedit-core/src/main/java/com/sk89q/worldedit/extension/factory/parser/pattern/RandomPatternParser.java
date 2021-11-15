package com.sk89q.worldedit.extension.factory.parser.pattern;

import com.fastasyncworldedit.core.configuration.Caption;
import com.sk89q.util.StringUtil;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.pattern.RandomPattern;
import com.sk89q.worldedit.internal.registry.InputParser;
import com.sk89q.worldedit.util.formatting.text.TextComponent;

import java.util.List;
import java.util.stream.Stream;

public class RandomPatternParser extends InputParser<Pattern> {

    /**
     * Create a new input parser.
     *
     * @param worldEdit the worldedit instance.
     */
    public RandomPatternParser(WorldEdit worldEdit) {
        super(worldEdit);
    }

    @Override
    public Stream<String> getSuggestions(String input) {
        //FAWE start
        List<String> patterns = StringUtil.split(input, ',', '[', ']');
        if (patterns.size() == 1) {
            return Stream.empty();
        }
        // get suggestions for the last token only
        String token = patterns.get(patterns.size() - 1);
        String previous = String.join(",", patterns.subList(0, patterns.size() - 1));
        if (token.matches("[0-9]+(\\.[0-9]*)?%.*")) {
            String[] p = token.split("%");
            if (p.length < 2) {
                return Stream.empty();
            } else {
                token = p[1];
            }
        }
        final List<String> innerSuggestions = worldEdit.getPatternFactory().getSuggestions(token);
        return innerSuggestions.stream().map(s -> previous + "," + s);
        //FAWE end
    }

    @Override
    public Pattern parseFromInput(String input, ParserContext context) throws InputParseException {
        RandomPattern randomPattern = new RandomPattern();

        //FAWE start
        List<String> patterns = StringUtil.split(input, ',', '[', ']');
        //FAWE end
        if (patterns.size() == 1) {
            return null; // let a 'single'-pattern parser handle it
        }
        for (String token : patterns) {
            double chance;
            Pattern innerPattern;

            // Parse special percentage syntax
            if (token.matches("[0-9]+(\\.[0-9]*)?%.*")) {
                //FAWE start
                String[] p = token.split("%", 2);
                //FAWE end

                if (p.length < 2) {
                    throw new InputParseException(Caption.of(
                            "worldedit.error.parser.missing-random-type",
                            TextComponent.of(input)
                    ));
                } else {
                    chance = Double.parseDouble(p[0]);
                    innerPattern = worldEdit.getPatternFactory().parseFromInput(p[1], context);
                }
            } else {
                chance = 1;
                innerPattern = worldEdit.getPatternFactory().parseFromInput(token, context);
            }

            randomPattern.add(innerPattern, chance);
        }

        return randomPattern;
    }

}
