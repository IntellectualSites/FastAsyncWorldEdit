package com.fastasyncworldedit.core.extension.factory;

import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.extension.factory.parser.transform.DefaultTransformParser;
import com.fastasyncworldedit.core.extension.factory.parser.transform.Linear3DTransformParser;
import com.fastasyncworldedit.core.extension.factory.parser.transform.LinearTransformParser;
import com.fastasyncworldedit.core.extension.factory.parser.transform.OffsetTransformParser;
import com.fastasyncworldedit.core.extension.factory.parser.transform.PatternTransformParser;
import com.fastasyncworldedit.core.extension.factory.parser.transform.RandomTransformParser;
import com.fastasyncworldedit.core.extension.factory.parser.transform.RotateTransformParser;
import com.fastasyncworldedit.core.extension.factory.parser.transform.ScaleTransformParser;
import com.fastasyncworldedit.core.extension.factory.parser.transform.SpreadTransformParser;
import com.fastasyncworldedit.core.extent.ResettableExtent;
import com.fastasyncworldedit.core.extent.transform.RandomTransform;
import com.fastasyncworldedit.core.math.random.TrueRandom;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.NoMatchException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.internal.registry.AbstractFactory;
import com.sk89q.worldedit.internal.registry.InputParser;
import com.sk89q.worldedit.util.formatting.text.TextComponent;

import java.util.ArrayList;
import java.util.List;

public class TransformFactory extends AbstractFactory<ResettableExtent> {

    private final DefaultTransformParser defaultTransformParser;

    /**
     * Create a new factory.
     *
     * @param worldEdit the WorldEdit instance
     */
    public TransformFactory(WorldEdit worldEdit) {
        super(worldEdit, new NullTransformParser(worldEdit));

        defaultTransformParser = new DefaultTransformParser(worldEdit);

        // split and parse each sub-transform
        register(new RandomTransformParser(worldEdit));

        register(new OffsetTransformParser(worldEdit));
        register(new ScaleTransformParser(worldEdit));
        register(new RotateTransformParser(worldEdit));
        register(new SpreadTransformParser(worldEdit));
        register(new PatternTransformParser(worldEdit));
        register(new LinearTransformParser(worldEdit));
        register(new Linear3DTransformParser(worldEdit));
    }

    @Override
    public ResettableExtent parseFromInput(String input, ParserContext context) throws InputParseException {
        List<ResettableExtent> transforms = new ArrayList<>();

        for (String component : input.split(" ")) {
            if (component.isEmpty()) {
                continue;
            }

            ResettableExtent match = defaultTransformParser.parseFromInput(component, context);
            if (match != null) {
                transforms.add(match);
                continue;
            }
            parseFromParsers(context, transforms, component, match);
        }

        return getResettableExtent(input, transforms);
    }

    private void parseFromParsers(
            final ParserContext context,
            final List<ResettableExtent> ResettableExtents,
            final String component,
            ResettableExtent match
    ) {
        for (InputParser<ResettableExtent> parser : getParsers()) {
            match = parser.parseFromInput(component, context);

            if (match != null) {
                break;
            }
        }
        if (match == null) {
            throw new NoMatchException(Caption.of("worldedit.error.no-match", TextComponent.of(component)));
        }
        ResettableExtents.add(match);
    }

    public ResettableExtent parseWithoutDefault(String input, ParserContext context) throws InputParseException {
        List<ResettableExtent> ResettableExtents = new ArrayList<>();

        for (String component : input.split(" ")) {
            if (component.isEmpty()) {
                continue;
            }

            ResettableExtent match = null;

            parseFromParsers(context, ResettableExtents, component, match);
        }

        return getResettableExtent(input, ResettableExtents);
    }

    private ResettableExtent getResettableExtent(final String input, final List<ResettableExtent> transforms) {
        switch (transforms.size()) {
            case 0:
                throw new NoMatchException(Caption.of("worldedit.error.no-match", TextComponent.of(input)));
            case 1:
                return transforms.get(0);
            default:
                RandomTransform randomTransform = new RandomTransform(new TrueRandom());
                for (ResettableExtent transform : transforms) {
                    randomTransform.add(transform, 1d);
                }
                return randomTransform;
        }
    }

    // TODO is there a better default?
    private static final class NullTransformParser extends InputParser<ResettableExtent> {

        protected NullTransformParser(WorldEdit worldEdit) {
            super(worldEdit);
        }

        @Override
        public ResettableExtent parseFromInput(String input, ParserContext context) throws InputParseException {
            return null;
        }

    }

}
