package com.fastasyncworldedit.core.extension.factory.parser.mask;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.util.SuggestionHelper;
import com.fastasyncworldedit.core.object.parser.RichParser;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.MaskIntersection;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.mask.OffsetMask;
import com.sk89q.worldedit.math.BlockVector3;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public class RichOffsetMaskParser extends RichParser<Mask> {

    /**
     * Create a new rich parser with a defined prefix for the result, e.g. {@code #simplex}.
     *
     * @param worldEdit the worldedit instance.
     */
    public RichOffsetMaskParser(WorldEdit worldEdit) {
        super(worldEdit, "#offset");
    }

    @Override
    protected Stream<String> getSuggestions(String argumentInput, int index) {
        if (index < 3) {
            return SuggestionHelper.suggestPositiveIntegers(argumentInput);
        }
        if (index == 3) {
            return worldEdit.getMaskFactory().getSuggestions(argumentInput).stream();
        }
        return Stream.empty();
    }

    @Override
    protected Mask parseFromInput(@NotNull String[] arguments, ParserContext context) throws InputParseException {
        if (arguments.length != 4) {
            return null;
        }
        int x = Integer.parseInt(arguments[0]);
        int y = Integer.parseInt(arguments[1]);
        int z = Integer.parseInt(arguments[2]);
        Mask submask = worldEdit.getMaskFactory().parseFromInput(arguments[3], context);
        OffsetMask offsetMask = new OffsetMask(submask, BlockVector3.at(x, y, z));
        return new MaskIntersection(offsetMask, Masks.negate(submask));
    }
}
