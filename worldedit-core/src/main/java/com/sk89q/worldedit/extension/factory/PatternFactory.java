/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.extension.factory;

import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.AngleColorPatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.AverageColorPatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.BiomePatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.BufferedPattern2DParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.BufferedPatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.ColorPatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.DarkenPatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.DesaturatePatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.ExistingPatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.ExpressionPatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.HotbarPatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.LightenPatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.Linear2DPatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.Linear3DPatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.LinearPatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.MaskedPatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.NoXPatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.NoYPatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.NoZPatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.OffsetPatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.PerlinPatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.RandomFullClipboardPatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.RandomOffsetPatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.RelativePatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.RichPatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.RidgedMultiFractalPatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.SaturatePatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.SimplexPatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.SolidRandomOffsetPatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.SurfaceRandomOffsetPatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.TypeSwapPatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.VoronoiPatternParser;
import com.fastasyncworldedit.core.math.random.TrueRandom;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.factory.parser.pattern.BlockCategoryPatternParser;
import com.sk89q.worldedit.extension.factory.parser.pattern.ClipboardPatternParser;
import com.sk89q.worldedit.extension.factory.parser.pattern.RandomPatternParser;
import com.sk89q.worldedit.extension.factory.parser.pattern.RandomStatePatternParser;
import com.sk89q.worldedit.extension.factory.parser.pattern.SingleBlockPatternParser;
import com.sk89q.worldedit.extension.factory.parser.pattern.TypeOrStateApplyingPatternParser;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.NoMatchException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.pattern.RandomPattern;
import com.sk89q.worldedit.internal.registry.AbstractFactory;
import com.sk89q.worldedit.util.formatting.text.TextComponent;

import java.util.List;

/**
 * A registry of known {@link Pattern}s. Provides methods to instantiate
 * new patterns from input.
 *
 * <p>Instances of this class can be taken from
 * {@link WorldEdit#getPatternFactory()}.</p>
 */
public final class PatternFactory extends AbstractFactory<Pattern> {

    /**
     * Create a new instance.
     *
     * @param worldEdit the WorldEdit instance
     */
    public PatternFactory(WorldEdit worldEdit) {
        //FAWE start - rich pattern parsing
        super(worldEdit, new SingleBlockPatternParser(worldEdit), new RichPatternParser(worldEdit));
        //FAWE end

        // split and parse each sub-pattern
        register(new RandomPatternParser(worldEdit));

        // individual patterns
        register(new ClipboardPatternParser(worldEdit));
        register(new TypeOrStateApplyingPatternParser(worldEdit));
        register(new RandomStatePatternParser(worldEdit));
        register(new BlockCategoryPatternParser(worldEdit));

        //FAWE start
        register(new AngleColorPatternParser(worldEdit));
        register(new AverageColorPatternParser(worldEdit));
        register(new BiomePatternParser(worldEdit));
        register(new BufferedPatternParser(worldEdit));
        register(new BufferedPattern2DParser(worldEdit));
        register(new ColorPatternParser(worldEdit));
        register(new DarkenPatternParser(worldEdit));
        register(new DesaturatePatternParser(worldEdit));
        register(new ExistingPatternParser(worldEdit));
        register(new ExpressionPatternParser(worldEdit));
        register(new HotbarPatternParser(worldEdit));
        register(new LightenPatternParser(worldEdit));
        register(new Linear2DPatternParser(worldEdit));
        register(new Linear3DPatternParser(worldEdit));
        register(new LinearPatternParser(worldEdit));
        register(new MaskedPatternParser(worldEdit));
        register(new NoXPatternParser(worldEdit));
        register(new NoYPatternParser(worldEdit));
        register(new NoZPatternParser(worldEdit));
        register(new OffsetPatternParser(worldEdit));
        register(new PerlinPatternParser(worldEdit));
        register(new RandomFullClipboardPatternParser(worldEdit));
        register(new RandomOffsetPatternParser(worldEdit));
        register(new RelativePatternParser(worldEdit));
        register(new RidgedMultiFractalPatternParser(worldEdit));
        register(new SaturatePatternParser(worldEdit));
        register(new SimplexPatternParser(worldEdit));
        register(new SolidRandomOffsetPatternParser(worldEdit));
        register(new SurfaceRandomOffsetPatternParser(worldEdit));
        register(new TypeSwapPatternParser(worldEdit));
        register(new VoronoiPatternParser(worldEdit));
    }

    //FAWE start - rich pattern parsing

    @Override
    public Pattern parseFromInput(String input, ParserContext context) throws InputParseException {
        return super.parseFromInput(input, context);
    }

    @Override
    protected Pattern getParsed(final String input, final List<Pattern> patterns) {
        switch (patterns.size()) {
            case 0:
                throw new NoMatchException(Caption.of("worldedit.error.no-match", TextComponent.of(input)));
            case 1:
                return patterns.get(0);
            default:
                RandomPattern randomPattern = new RandomPattern(new TrueRandom());
                for (Pattern pattern : patterns) {
                    randomPattern.add(pattern, 1d);
                }
                return randomPattern;
        }
    }
    //FAWE end

}
