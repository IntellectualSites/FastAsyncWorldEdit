package com.fastasyncworldedit.core.extension.factory.parser.pattern;

import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.extension.factory.parser.RichParser;
import com.fastasyncworldedit.core.function.pattern.BiomeApplyingPattern;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.NoMatchException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

public class BiomePatternParser extends RichParser<Pattern> {

    private final static String BIOME_PREFIX = "$";

    /**
     * Create a new biome pattern parser.
     *
     * @param worldEdit the worldedit instance.
     */
    public BiomePatternParser(WorldEdit worldEdit) {
        super(worldEdit, "#biome", "$");
    }

    // overridden to provide $<biome> too
    @Override
    public Pattern parseFromInput(String input, ParserContext context) throws InputParseException {
        if (input.startsWith(BIOME_PREFIX)) {
            String biomeId = input.substring(1);
            BiomeType biomeType = BiomeTypes.get(biomeId);
            if (biomeType == null) {
                throw new NoMatchException(Caption.of("worldedit.error.unknown-biome", TextComponent.of(biomeId)));
            }
            return new BiomeApplyingPattern(context.requireExtent(), biomeType);
        } else {
            return super.parseFromInput(input, context);
        }
    }

    // overridden to provide $<biome> too
    @Override
    public Stream<String> getSuggestions(String input, ParserContext context) {
        if (input.startsWith(BIOME_PREFIX)) {
            return BiomeType.REGISTRY.getSuggestions(input.substring(1)).map(biome -> BIOME_PREFIX + biome);
        } else {
            return super.getSuggestions(input, context);
        }
    }

    @Override
    protected Stream<String> getSuggestions(String argumentInput, int index, ParserContext context) {
        if (index == 0) {
            return BiomeType.REGISTRY.getSuggestions(argumentInput);
        }
        return Stream.empty();
    }

    @Override
    protected Pattern parseFromInput(@Nonnull String[] arguments, ParserContext context) throws InputParseException {
        if (arguments.length != 1) {
            throw new InputParseException(Caption.of("fawe.error.invalid-arguments", TextComponent.of("#biome[<biome>]")));
        }
        BiomeType biomeType = BiomeTypes.get(arguments[0]);
        if (biomeType == null) {
            throw new NoMatchException(Caption.of("worldedit.error.unknown-biome", TextComponent.of(arguments[0])));
        }
        return new BiomeApplyingPattern(context.requireExtent(), biomeType);
    }

}
