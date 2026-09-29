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
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.extension.factory.parser.DefaultItemParser;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.NoMatchException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.internal.registry.AbstractFactory;
import com.sk89q.worldedit.internal.registry.InputParser;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;

public class ItemFactory extends AbstractFactory<BaseItem> {

    /**
     * Create a new instance.
     *
     * @param worldEdit the WorldEdit instance.
     */
    public ItemFactory(WorldEdit worldEdit) {
        super(worldEdit, new DefaultItemParser(worldEdit));
    }

    //FAWE start
    @Override
    public BaseItem parseFromInput(String input, ParserContext context) throws InputParseException {
        BaseItem match;

        for (InputParser<BaseItem> parser : parsers) {
            match = parser.parseFromInput(input, context);

            if (match != null) {
                return match;
            }
        }

        throw new NoMatchException(TranslatableComponent.of("worldedit.error.no-match", TextComponent.of(input)));
    }
    //FAWE end

}
