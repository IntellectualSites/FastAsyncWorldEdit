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

package com.sk89q.worldedit.blocks;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.util.gson.GsonUtil;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a sign block.
 */
public class SignBlock extends BaseBlock {

    private String[] text;

    private static final String EMPTY =  "{\"text\":\"\"}";

    /**
     * Construct the sign with text.
     *
     * @param blockState The block state
     * @param text lines of text
     */
    public SignBlock(BlockState blockState, String[] text) {
        super(blockState);
        if (text == null) {
            this.text = new String[] { EMPTY, EMPTY, EMPTY, EMPTY };
            return;
        }
        for (int i = 0; i < text.length; i++) {
            if (text[i].isEmpty()) {
                text[i] = EMPTY;
            } else {
                text[i] = "{\"text\":" + GsonUtil.stringValue(text[i]) + "}";
            }
        }
        this.text = text;
    }

    /**
     * Get the text.
     *
     * @return the text
     */
    public String[] getText() {
        return text;
    }

    /**
     * Set the text.
     *
     * @param text the text to set
     */
    public void setText(String[] text) {
        if (text == null) {
            throw new IllegalArgumentException("Can't set null text for a sign");
        }
        this.text = text;
    }

    @Override
    public boolean hasNbtData() {
        return true;
    }

    @Override
    public String getNbtId() {
        return "minecraft:sign";
    }

    @Override
    public CompoundTag getNbtData() {
        Map<String, Tag> values = new HashMap<>();
        for(int i = 0; i < 4; i++) {
            values.put("Text" + (i + 1), new StringTag(text[i]));
        }
        return new CompoundTag(values);
    }

    @Override
    public void setNbtData(CompoundTag rootTag) {
        if (rootTag == null) {
            return;
        }

        text = new String[] { EMPTY, EMPTY, EMPTY, EMPTY };

        Tag idTag = values.get("id");
        if (!(idTag instanceof StringTag) || !((StringTag) idTag).getValue().equals(getNbtId())) {
            throw new RuntimeException(String.format("'%s' tile entity expected", getNbtId()));
        }
        
        Map<String, Tag> values = rootTag.getValue();
        
        for(int i = 0; i < 4; i++) {
            Tag tag = values.get("Text" + (i + 1));
            if (tag instanceof StringTag) {
            text[i] = ((StringTag) tag).getValue();
            }
        }
    }

}
