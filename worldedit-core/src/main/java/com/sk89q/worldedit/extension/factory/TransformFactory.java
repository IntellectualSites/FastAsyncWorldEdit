package com.sk89q.worldedit.extension.factory;

import com.boydti.fawe.object.extent.ResettableExtent;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.factory.parser.transform.OffsetTransformParser;
import com.sk89q.worldedit.extension.factory.parser.transform.RotateTransformParser;
import com.sk89q.worldedit.extension.factory.parser.transform.ScaleTransformParser;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.internal.registry.AbstractFactory;
import com.sk89q.worldedit.internal.registry.InputParser;

public class TransformFactory extends AbstractFactory<ResettableExtent> {

    /**
     * Create a new factory.
     *
     * @param worldEdit     the WorldEdit instance
     */
    public TransformFactory(WorldEdit worldEdit) {
        super(worldEdit, new NullTransformParser(worldEdit));

        register(new OffsetTransformParser(worldEdit));
        register(new ScaleTransformParser(worldEdit));
        register(new RotateTransformParser(worldEdit));
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
