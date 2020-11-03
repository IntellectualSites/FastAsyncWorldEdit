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

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.factory.parser.mask.AdjacentMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.AirMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.AngleMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.BiomeMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.BlockCategoryMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.BlockStateMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.BlocksMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.ExistingMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.ExpressionMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.ExtremaMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.FalseMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.LazyRegionMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.LiquidMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.NegateMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.NoiseMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.OffsetMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.ROCAngleMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.RegionMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.RichOffsetMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.SimplexMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.SolidMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.SurfaceMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.TrueMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.WallMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.XAxisMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.YAxisMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.ZAxisMaskParser;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.NoMatchException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.MaskIntersection;
import com.sk89q.worldedit.internal.registry.AbstractFactory;
import com.sk89q.worldedit.internal.registry.InputParser;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A registry of known {@link Mask}s. Provides methods to instantiate
 * new masks from input.
 *
 * <p>Instances of this class can be taken from
 * {@link WorldEdit#getMaskFactory()}.</p>
 */
public final class MaskFactory extends AbstractFactory<Mask> {

    /**
     * Create a new mask registry.
     *
     * @param worldEdit the WorldEdit instance
     */
    public MaskFactory(WorldEdit worldEdit) {
        super(worldEdit, new BlocksMaskParser(worldEdit));

        register(new ExistingMaskParser(worldEdit));
        register(new AirMaskParser(worldEdit));
        register(new SolidMaskParser(worldEdit));
        register(new LazyRegionMaskParser(worldEdit));
        register(new RegionMaskParser(worldEdit));
        register(new OffsetMaskParser(worldEdit));
        register(new NoiseMaskParser(worldEdit));
        register(new BlockStateMaskParser(worldEdit));
        register(new NegateMaskParser(worldEdit));
        register(new ExpressionMaskParser(worldEdit));

        register(new BlockCategoryMaskParser(worldEdit));
        register(new BiomeMaskParser(worldEdit));
        // Mask Parsers from FAWE
        register(new AdjacentMaskParser(worldEdit));
        register(new AngleMaskParser(worldEdit));
        register(new ExtremaMaskParser(worldEdit));
        register(new FalseMaskParser(worldEdit));
        register(new LiquidMaskParser(worldEdit));
        //register(new RadiusMaskParser(worldEdit)); TODO: Adapt to work with FAWE's Chunk I/O
        register(new RichOffsetMaskParser(worldEdit));
        register(new ROCAngleMaskParser(worldEdit));
        register(new SimplexMaskParser(worldEdit));
        register(new SurfaceMaskParser(worldEdit));
        register(new TrueMaskParser(worldEdit));
        register(new WallMaskParser(worldEdit));
        register(new XAxisMaskParser(worldEdit));
        register(new YAxisMaskParser(worldEdit));
        register(new ZAxisMaskParser(worldEdit));

    }

    @Override
    public List<String> getSuggestions(String input) {
        final String[] split = input.split(" ");
        if (split.length > 1) {
            String prev = input.substring(0, input.lastIndexOf(" ")) + " ";
            return super.getSuggestions(split[split.length -1]).stream().map(s -> prev + s).collect(Collectors.toList());
        }
        return super.getSuggestions(input);
    }

    @Override
    public Mask parseFromInput(String input, ParserContext context) throws InputParseException {
        List<Mask> masks = new ArrayList<>();

        for (String component : input.split(" ")) {
            if (component.isEmpty()) {
                continue;
            }

            Mask match = null;
            for (InputParser<Mask> parser : getParsers()) {
                match = parser.parseFromInput(component, context);

                if (match != null) {
                    break;
                }
            }
            if (match == null) {
                throw new NoMatchException(TranslatableComponent.of("worldedit.error.no-match", TextComponent.of(component)));
            }
            masks.add(match);
        }

        switch (masks.size()) {
            case 0:
                throw new NoMatchException(TranslatableComponent.of("worldedit.error.no-match", TextComponent.of(input)));
            case 1:
                return masks.get(0).optimize();
            default:
                return new MaskIntersection(masks).optimize();
        }
    }

}
