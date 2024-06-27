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

package com.sk89q.jnbt;

import com.fastasyncworldedit.core.jnbt.NumberTag;
import org.enginehub.linbus.tree.LinIntTag;

/**
 * The {@code TAG_Int} tag.
 *
 * @deprecated Use {@link LinIntTag}.
 */
@Deprecated
public final class IntTag extends NumberTag<LinIntTag> {

    /**
     * Creates the tag with an empty name.
     *
     * @param value the value of the tag
     */
    public IntTag(int value) {
        this(LinIntTag.of(value));
    }

    public IntTag(LinIntTag tag) {
        super(tag);
    }

    @Override
    public Integer getValue() {
        return linTag.value();
    }

    //FAWE start
    @Override
    public int getTypeCode() {
        return NBTConstants.TYPE_INT;
    }
    //FAWE end

}
