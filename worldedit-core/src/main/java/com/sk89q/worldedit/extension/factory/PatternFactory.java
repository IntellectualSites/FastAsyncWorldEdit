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

import com.fastasyncworldedit.core.extension.factory.parser.pattern.BiomePatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.BufferedPatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.ExistingPatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.Linear2DPatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.Linear3DPatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.PerlinPatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.RandomPatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.RidgedMultiFractalPatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.SimplexPatternParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.VoronoiPatternParser;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.factory.parser.pattern.BlockCategoryPatternParser;
import com.sk89q.worldedit.extension.factory.parser.pattern.ClipboardPatternParser;
import com.sk89q.worldedit.extension.factory.parser.pattern.RandomStatePatternParser;
import com.sk89q.worldedit.extension.factory.parser.pattern.SingleBlockPatternParser;
import com.sk89q.worldedit.extension.factory.parser.pattern.TypeOrStateApplyingPatternParser;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.registry.AbstractFactory;

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
        super(worldEdit, new SingleBlockPatternParser(worldEdit));

        // split and parse each sub-pattern
        register(new RandomPatternParser(worldEdit));

        // individual patterns
        register(new ClipboardPatternParser(worldEdit));
        register(new TypeOrStateApplyingPatternParser(worldEdit));
        register(new RandomStatePatternParser(worldEdit));
        register(new BlockCategoryPatternParser(worldEdit));

        //FAWE start
        register(new SimplexPatternParser(worldEdit));
        register(new VoronoiPatternParser(worldEdit));
        register(new PerlinPatternParser(worldEdit));
        register(new RidgedMultiFractalPatternParser(worldEdit));
        register(new BiomePatternParser(worldEdit));
        register(new Linear2DPatternParser(worldEdit));
        register(new Linear3DPatternParser(worldEdit));
        register(new BufferedPatternParser(worldEdit));
        register(new ExistingPatternParser(worldEdit));
        //FAWE end
    }

}
