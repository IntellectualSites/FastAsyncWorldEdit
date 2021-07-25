package com.fastasyncworldedit.core.extension.factory.parser.mask;

import com.fastasyncworldedit.core.extension.factory.parser.RichParser;
import com.fastasyncworldedit.core.function.mask.AdjacentAnyMask;
import com.fastasyncworldedit.core.function.mask.AdjacentMask;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.util.SuggestionHelper;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Locatable;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.world.World;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

public class AdjacentMaskParser extends RichParser<Mask> {

    public AdjacentMaskParser(WorldEdit worldEdit) {
        super(worldEdit, "~");
    }

    @Override
    protected Stream<String> getSuggestions(String argumentInput, int index) {
        if (index == 0) {
            return worldEdit.getMaskFactory().getSuggestions(argumentInput).stream();
        } else if (index == 1 || index == 2) {
            return SuggestionHelper.suggestPositiveDoubles(argumentInput);
        }
        return Stream.empty();
    }

    @Override
    protected Mask parseFromInput(@Nonnull String[] arguments, ParserContext context) throws InputParseException {
        if (arguments.length == 0) {
            return null;
        }
        Mask subMask = worldEdit.getMaskFactory().parseFromInput(arguments[0], context);
        int min = arguments.length > 1 ? Integer.parseInt(arguments[1]) : -1;
        int max = arguments.length > 2 ? Integer.parseInt(arguments[2]) : -1;
        if (min == -1 && max == -1) {
            min = 1;
            max = 8;
        } else if (max == -1) {
            max = min;
        }
        if (max >= 8 && min == 1) {

            int minY;
            int maxY;
            Extent extent = null;

            if (context.getActor() instanceof Player) {
                extent = ((Player) context.getActor()).getWorld();
            } else if (context.getWorld() != null) {
                extent = context.getWorld();
            } else if (context.getActor() instanceof Locatable) {
                extent = ((Locatable) context.getActor()).getExtent();
            } else if (context.getExtent() != null) {
                extent = context.getExtent();
            }

            if (extent != null) {
                minY = extent.getMinY();
                maxY = extent.getMaxY();
            } else {
                minY = 0;
                maxY = 255;
            }
            return new AdjacentAnyMask(subMask, minY, maxY);
        }
        return new AdjacentMask(subMask, min, max);
    }

}
