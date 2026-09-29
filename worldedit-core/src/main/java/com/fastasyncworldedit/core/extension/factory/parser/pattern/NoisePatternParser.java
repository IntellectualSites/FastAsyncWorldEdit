package com.fastasyncworldedit.core.extension.factory.parser.pattern;

import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.extension.factory.parser.RichParser;
import com.fastasyncworldedit.core.math.random.NoiseRandom;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.util.SuggestionHelper;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.pattern.RandomPattern;
import com.sk89q.worldedit.math.noise.NoiseGenerator;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import javax.annotation.Nonnull;
import java.util.function.Supplier;
import java.util.stream.Stream;

public abstract class NoisePatternParser extends RichParser<Pattern> {

    private final String name;
    private final Supplier<NoiseGenerator> generatorSupplier;

    /**
     * Create a new noise parser with a defined name, e.g. {@code #simplex}.
     *
     * @param worldEdit         the worldedit instance.
     * @param name              the name of this noise.
     * @param generatorSupplier the supplier to get a {@link NoiseGenerator} instance from.
     */
    protected NoisePatternParser(WorldEdit worldEdit, String name, Supplier<NoiseGenerator> generatorSupplier) {
        super(worldEdit, '#' + name);
        this.name = name;
        this.generatorSupplier = generatorSupplier;
    }

    @Override
    protected Stream<String> getSuggestions(String argumentInput, int index, ParserContext context) {
        if (index == 0) {
            return SuggestionHelper.suggestPositiveDoubles(argumentInput);
        }
        if (index == 1) {
            return worldEdit.getPatternFactory().getSuggestions(argumentInput, context).stream();
        }
        return Stream.empty();
    }

    @Override
    protected Pattern parseFromInput(@Nonnull String[] arguments, ParserContext context) {
        if (arguments.length != 2) {
            throw new InputParseException(Caption.of(
                    "fawe.error.command.syntax",
                    TextComponent.of(getPrefix() + "[scale][pattern] (e.g. " + getPrefix() + "[5][dirt,stone])")
            ));
        }
        double scale = parseScale(arguments[0]);
        Pattern inner = worldEdit.getPatternFactory().parseFromInput(arguments[1], context);
        if (inner instanceof RandomPattern) {
            return new RandomPattern(new NoiseRandom(this.generatorSupplier.get(), scale), (RandomPattern) inner);
        } else if (inner instanceof BlockStateHolder) {
            return inner; // single blocks won't have any impact on how a noise behaves
        } else {
            throw new InputParseException(TextComponent.of("Pattern " + inner.getClass().getSimpleName()
                    + " cannot be used with #" + this.name));
        }
    }

    /**
     * Modifies the given argument to match the requirements of the noise generator.
     *
     * @param argument the parsed scale argument.
     * @return the modified scale.
     */
    protected double parseScale(String argument) {
        double scale = Double.parseDouble(argument);
        return 1d / Math.max(1, scale);
    }

}
