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

package com.sk89q.worldedit.internal.command.exception;

import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.exception.BrushRadiusLimitException;
import com.fastasyncworldedit.core.exception.OutsideWorldBoundsException;
import com.fastasyncworldedit.core.exception.RadiusLimitException;
import com.fastasyncworldedit.core.internal.exception.FaweException;
import com.google.common.collect.ImmutableList;
import com.sk89q.worldedit.DisallowedItemException;
import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.InvalidItemException;
import com.sk89q.worldedit.MaxBrushRadiusException;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.MaxRadiusException;
import com.sk89q.worldedit.MissingWorldException;
import com.sk89q.worldedit.UnknownDirectionException;
import com.sk89q.worldedit.UnknownItemException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.InsufficientArgumentsException;
import com.sk89q.worldedit.command.tool.InvalidToolBindException;
import com.sk89q.worldedit.extent.clipboard.io.SchematicLoadException;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.regions.RegionOperationException;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.io.file.FileSelectionAbortedException;
import com.sk89q.worldedit.util.io.file.FilenameResolutionException;
import com.sk89q.worldedit.util.io.file.InvalidFilenameException;
import org.enginehub.piston.exception.CommandException;
import org.enginehub.piston.exception.UsageException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * converts WorldEdit exceptions and converts them into {@link CommandException}s.
 */
public class WorldEditExceptionConverter extends ExceptionConverterHelper {

    private static final Pattern numberFormat = Pattern.compile("^For input string: \"(.*)\"$");
    private final WorldEdit worldEdit;

    public WorldEditExceptionConverter(WorldEdit worldEdit) {
        checkNotNull(worldEdit);
        this.worldEdit = worldEdit;
    }

    private CommandException newCommandException(String message, Throwable cause) {
        return newCommandException(TextComponent.of(String.valueOf(message)), cause);
    }

    private CommandException newCommandException(Component message, Throwable cause) {
        return new CommandException(message, cause, ImmutableList.of());
    }

    @ExceptionMatch
    public void convert(NumberFormatException e) throws CommandException {
        final Matcher matcher = numberFormat.matcher(e.getMessage());

        if (matcher.matches()) {
            throw newCommandException(
                    Caption.of("worldedit.error.invalid-number.matches", TextComponent.of(matcher.group(1))),
                    e
            );
        } else {
            throw newCommandException(Caption.of("worldedit.error.invalid-number"), e);
        }
    }

    @ExceptionMatch
    public void convert(IncompleteRegionException e) throws CommandException {
        throw newCommandException(Caption.of("worldedit.error.incomplete-region"), e);
    }

    @ExceptionMatch
    public void convert(MissingWorldException e) throws CommandException {
        throw newCommandException(e.getRichMessage(), e);
    }

    @ExceptionMatch
    public void convert(UnknownItemException e) throws CommandException {
        throw newCommandException(Caption.of("worldedit.error.unknown-block", TextComponent.of(e.getID())), e);
    }

    @Deprecated
    @ExceptionMatch
    public void convert(InvalidItemException e) throws CommandException {
        throw newCommandException(e.getMessage(), e);
    }

    @ExceptionMatch
    public void convert(DisallowedItemException e) throws CommandException {
        throw newCommandException(Caption.of("worldedit.error.disallowed-block", TextComponent.of(e.getID())), e);
    }

    @ExceptionMatch
    public void convert(MaxChangedBlocksException e) throws CommandException {
        throw newCommandException(Caption.of("worldedit.error.max-changes", TextComponent.of(e.getBlockLimit())), e);
    }

    @ExceptionMatch
    public void convert(MaxBrushRadiusException e) throws CommandException {
        throw newCommandException(
                Caption.of("worldedit.error.max-brush-radius", TextComponent.of(worldEdit.getConfiguration().maxBrushRadius)),
                e
        );
    }

    @ExceptionMatch
    public void convert(MaxRadiusException e) throws CommandException {
        throw newCommandException(
                Caption.of("worldedit.error.max-radius", TextComponent.of(worldEdit.getConfiguration().maxRadius)),
                e
        );
    }

    //FAWE start
    @ExceptionMatch
    public void convert(BrushRadiusLimitException e) throws CommandException {
        throw newCommandException(Caption.of("fawe.error.limit.max-brush-radius", TextComponent.of(e.getMaxRadius())), e);
    }

    @ExceptionMatch
    public void convert(RadiusLimitException e) throws CommandException {
        throw newCommandException(Caption.of("fawe.error.limit.max-radius", TextComponent.of(e.getMaxRadius())), e);
    }

    @ExceptionMatch
    public void convert(OutsideWorldBoundsException e) throws CommandException {
        throw newCommandException(Caption.of("fawe.cancel.reason.world.limit", TextComponent.of(e.y())), e);
    }
    //FAWE end

    @ExceptionMatch
    public void convert(UnknownDirectionException e) throws CommandException {
        throw newCommandException(e.getRichMessage(), e);
    }

    @ExceptionMatch
    public void convert(InsufficientArgumentsException e) throws CommandException {
        throw newCommandException(e.getRichMessage(), e);
    }

    @ExceptionMatch
    public void convert(RegionOperationException e) throws CommandException {
        throw newCommandException(e.getRichMessage(), e);
    }

    @ExceptionMatch
    public void convert(ExpressionException e) throws CommandException {
        throw newCommandException(e.getMessage(), e);
    }

    @ExceptionMatch
    public void convert(EmptyClipboardException e) throws CommandException {
        throw newCommandException(Caption.of("worldedit.error.empty-clipboard"), e);
    }

    @ExceptionMatch
    public void convert(InvalidFilenameException e) throws CommandException {
        throw newCommandException(
                Caption.of("worldedit.error.invalid-filename", TextComponent.of(e.getFilename()), e.getRichMessage()),
                e
        );
    }

    @ExceptionMatch
    public void convert(FilenameResolutionException e) throws CommandException {
        throw newCommandException(
                Caption.of("worldedit.error.file-resolution", TextComponent.of(e.getFilename()), e.getRichMessage()),
                e
        );
    }

    @ExceptionMatch
    public void convert(InvalidToolBindException e) throws CommandException {
        throw newCommandException(
                Caption.of("worldedit.tool.error.cannot-bind", e.getItemType().getRichName(), e.getRichMessage()),
                e
        );
    }

    @ExceptionMatch
    public void convert(FileSelectionAbortedException e) throws CommandException {
        throw newCommandException(Caption.of("worldedit.error.file-aborted"), e);
    }

    @ExceptionMatch
    public void convert(SchematicLoadException e) throws CommandException {
        throw newCommandException(e.getRichMessage(), e);
    }

    @ExceptionMatch
    public void convert(WorldEditException e) throws CommandException {
        throw newCommandException(e.getRichMessage(), e);
    }

    // Prevent investigation into UsageExceptions
    @ExceptionMatch
    public void convert(UsageException e) throws CommandException {
        throw e;
    }

    //FAWE start
    @ExceptionMatch
    public void convert(FaweException e) throws CommandException {
        throw newCommandException(e.getComponent(), e);
    }
    //FAWE end

}
