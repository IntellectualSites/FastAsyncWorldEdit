/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.extension.factory.parser;

import com.boydti.fawe.config.Caption;
import com.google.common.collect.Maps;

import com.sk89q.worldedit.util.formatting.WorldEditText;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.boydti.fawe.jnbt.JSON2NBT;
import com.boydti.fawe.jnbt.NBTException;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.StringMan;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.NotABlockException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.blocks.MobSpawnerBlock;
import com.sk89q.worldedit.blocks.SignBlock;
import com.sk89q.worldedit.blocks.SkullBlock;
import com.sk89q.worldedit.command.util.SuggestionHelper;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.input.DisallowedUsageException;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.NoMatchException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.extent.inventory.SlottableBlockBag;
import com.sk89q.worldedit.internal.registry.InputParser;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.HandSide;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockCategories;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.FuzzyBlockState;
import com.sk89q.worldedit.world.registry.LegacyMapper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import com.sk89q.worldedit.world.entity.EntityType;
import com.sk89q.worldedit.world.entity.EntityTypes;

import java.util.Map;
import java.util.stream.Stream;

/**
 * Parses block input strings.
 */
public class DefaultBlockParser extends InputParser<BaseBlock> {

    public DefaultBlockParser(WorldEdit worldEdit) {
        super(worldEdit);
    }

    private static BaseBlock getBlockInHand(Actor actor, HandSide handSide) throws InputParseException {
        if (actor instanceof Player) {
            try {
                return ((Player) actor).getBlockInHand(handSide);
            } catch (NotABlockException e) {
                throw new InputParseException("You're not holding a block!");
            } catch (WorldEditException e) {
                throw new InputParseException("Unknown error occurred: " + e.getMessage(), e);
            }
        } else {
            throw new InputParseException("The user is not a player!");
        }
    }

    @Override
    public BaseBlock parseFromInput(String input, ParserContext context)
            throws InputParseException {
        String originalInput = input;
        input = input.replace(";", "|");
        Exception suppressed = null;
        try {
            BaseBlock modified = parseLogic(input, context);
            if (modified != null) {
                return modified;
            }
        } catch (Exception e) {
            suppressed = e;
        }
        try {
            return parseLogic(originalInput, context);
        } catch (Exception e) {
            if (suppressed != null) {
                e.addSuppressed(suppressed);
            }
            throw e;
        }
    }

    private static String[] EMPTY_STRING_ARRAY = {};

    /**
     * Backwards compatibility for wool colours in block syntax.
     *
     * @param string Input string
     * @return Mapped string
     */
    @SuppressWarnings("ConstantConditions")
    private String woolMapper(String string) {
        switch (string.toLowerCase(Locale.ROOT)) {
            case "white":
                return BlockTypes.WHITE_WOOL.getId();
            case "black":
                return BlockTypes.BLACK_WOOL.getId();
            case "blue":
                return BlockTypes.BLUE_WOOL.getId();
            case "brown":
                return BlockTypes.BROWN_WOOL.getId();
            case "cyan":
                return BlockTypes.CYAN_WOOL.getId();
            case "gray":
            case "grey":
                return BlockTypes.GRAY_WOOL.getId();
            case "green":
                return BlockTypes.GREEN_WOOL.getId();
            case "light_blue":
            case "lightblue":
                return BlockTypes.LIGHT_BLUE_WOOL.getId();
            case "light_gray":
            case "light_grey":
            case "lightgray":
            case "lightgrey":
                return BlockTypes.LIGHT_GRAY_WOOL.getId();
            case "lime":
                return BlockTypes.LIME_WOOL.getId();
            case "magenta":
                return BlockTypes.MAGENTA_WOOL.getId();
            case "orange":
                return BlockTypes.ORANGE_WOOL.getId();
            case "pink":
                return BlockTypes.PINK_WOOL.getId();
            case "purple":
                return BlockTypes.PURPLE_WOOL.getId();
            case "yellow":
                return BlockTypes.YELLOW_WOOL.getId();
            case "red":
                return BlockTypes.RED_WOOL.getId();
            default:
                return string;
        }
    }

    private static Map<Property<?>, Object> parseProperties(BlockType type, String[] stateProperties, ParserContext context) throws NoMatchException {
        Map<Property<?>, Object> blockStates = new HashMap<>();

        if (stateProperties.length > 0) { // Block data not yet detected
            // Parse the block data (optional)
            for (String parseableData : stateProperties) {
                try {
                    String[] parts = parseableData.split("=");
                    if (parts.length != 2) {
                        throw new NoMatchException("Bad state format in " + parseableData);
                    }

                    @SuppressWarnings("unchecked")
                    Property<Object> propertyKey = (Property<Object>) type.getPropertyMap().get(parts[0]);
                    if (propertyKey == null) {
                        if (context.getActor() != null) {
                            throw new NoMatchException("Unknown property " + parts[0] + " for block " + type.getId());
                        } else {
                            WorldEdit.logger.warn("Unknown property " + parts[0] + " for block " + type.getId());
                        }
                        return Maps.newHashMap();
                    }
                    if (blockStates.containsKey(propertyKey)) {
                        throw new NoMatchException("Duplicate property " + parts[0]);
                    }
                    Object value;
                    try {
                        value = propertyKey.getValueFor(parts[1]);
                    } catch (IllegalArgumentException e) {
                        throw new NoMatchException("Unknown value " + parts[1] + " for state " + parts[0]);
                    }

                    blockStates.put(propertyKey, value);
                } catch (NoMatchException e) {
                    throw e; // Pass-through
                } catch (Exception e) {
                    WorldEdit.logger.warn("Unknown state '" + parseableData + "'", e);
                    throw new NoMatchException("Unknown state '" + parseableData + "'");
                }
            }
        }

        return blockStates;
    }

    @Override
    public Stream<String> getSuggestions(String input) {
        final int idx = input.lastIndexOf('[');
        if (idx < 0) {
            return SuggestionHelper.getNamespacedRegistrySuggestions(BlockType.REGISTRY, input);
        }
        String blockType = input.substring(0, idx);
        BlockType type = BlockTypes.get(blockType.toLowerCase(Locale.ROOT));
        if (type == null) {
            return Stream.empty();
        }

        String props = input.substring(idx + 1);
        if (props.isEmpty()) {
            return type.getProperties().stream().map(p -> input + p.getName() + "=");
        }

        return SuggestionHelper.getBlockPropertySuggestions(blockType, props);
    }

    private BaseBlock parseLogic(String input, ParserContext context) throws InputParseException {
        String[] blockAndExtraData = input.trim().split("\\|", 2);
        blockAndExtraData[0] = woolMapper(blockAndExtraData[0]);

        BlockState state = null;

        // Legacy matcher
        if (context.isTryingLegacy()) {
            try {
                String[] split = blockAndExtraData[0].split(":", 2);
                if (split.length == 0) {
                    throw new InputParseException("Invalid colon.");
                } else if (split.length == 1) {
                    state = LegacyMapper.getInstance().getBlockFromLegacy(Integer.parseInt(split[0]));
                } else if (MathMan.isInteger(split[0])) {
                    int id = Integer.parseInt(split[0]);
                    int data = Integer.parseInt(split[1]);
                    if (data < 0 || data >= 16) {
                        throw new InputParseException("Invalid data " + data);
                    }
                    state = LegacyMapper.getInstance().getBlockFromLegacy(id, data);
                } else {
                    BlockType type = BlockTypes.get(split[0].toLowerCase(Locale.ROOT));
                    if (type != null) {
                        int data = Integer.parseInt(split[1]);
                        if (data < 0 || data >= 16) {
                            throw new InputParseException("Invalid data " + data);
                        }
                        state = LegacyMapper.getInstance().getBlockFromLegacy(type.getLegacyCombinedId() >> 4, data);
                    }
                }
            } catch (NumberFormatException ignored) {
            }
        }

        CompoundTag nbt = null;
        if (state == null) {
            String typeString;
            String stateString = null;
            int stateStart = blockAndExtraData[0].indexOf('[');
            if (stateStart == -1) {
                typeString = blockAndExtraData[0];
            } else {
                typeString = blockAndExtraData[0].substring(0, stateStart);
                if (stateStart + 1 >= blockAndExtraData[0].length()) {
                    throw new InputParseException("Invalid format. Hanging bracket @ " + stateStart + ".");
                }
                int stateEnd = blockAndExtraData[0].lastIndexOf(']');
                if (stateEnd < 0) {
                    throw new InputParseException("Invalid format. Unclosed property.");
                }
                stateString = blockAndExtraData[0].substring(stateStart + 1, blockAndExtraData[0].length() - 1);
            }
            if (typeString.isEmpty()) {
                throw new InputParseException("Invalid format");
            }
            // PosX
            if (typeString.matches("pos[0-9]+")) {
                int index = Integer.parseInt(typeString.replaceAll("[a-z]+", ""));
                // Get the block type from the "primary position"
                final World world = context.requireWorld();
                final BlockVector3 primaryPosition;
                try {
                    primaryPosition = context.requireSession().getRegionSelector(world).getVertices().get(index - 1);
                } catch (IncompleteRegionException e) {
                    throw new InputParseException("Your selection is not complete.");
                }
                state = world.getBlock(primaryPosition);
            } else {
                if ("hand".equalsIgnoreCase(typeString)) {
                    // Get the block type from the item in the user's hand.
                    BaseBlock block = getBlockInHand(context.requireActor(), HandSide.MAIN_HAND);
                    state = block.toBlockState();
                    nbt = block.getNbtData();
                } else if ("offhand".equalsIgnoreCase(typeString)) {
                    // Get the block type from the item in the user's off hand.
                    BaseBlock block = getBlockInHand(context.requireActor(), HandSide.OFF_HAND);
                    state = block.toBlockState();
                    nbt = block.getNbtData();
                } else if (typeString.matches("slot[0-9]+")) {
                    int slot = Integer.parseInt(typeString.substring(4)) - 1;
                    Actor actor = context.requireActor();
                    if (!(actor instanceof Player)) {
                        throw new InputParseException("The user is not a player!");
                    }
                    Player player = (Player) actor;
                    BlockBag bag = player.getInventoryBlockBag();
                    if (bag == null || !(bag instanceof SlottableBlockBag)) {
                        throw new InputParseException("Unsupported!");
                    }
                    SlottableBlockBag slottable = (SlottableBlockBag) bag;
                    BaseItem item = slottable.getItem(slot);

                    if (!item.getType().hasBlockType()) {
                        throw new InputParseException("You're not holding a block!");
                    }
                    state = item.getType().getBlockType().getDefaultState();
                    nbt = item.getNbtData();
                } else {
                    BlockType type = BlockTypes.parse(typeString.toLowerCase(Locale.ROOT));

                    if (type != null) {
                        state = type.getDefaultState();
                    }
                    if (state == null) {
                        throw new NoMatchException("Does not match a valid block type: '" + input + "'");
                    }
                }
            }
            if (nbt == null) nbt = state.getNbtData();

            if (stateString != null) {
                state = BlockState.get(state.getBlockType(), "[" + stateString + "]", state);
                if (context.isPreferringWildcard()) {
                    if (stateString.isEmpty()) {
                        state = new FuzzyBlockState(state);
                    } else {
                        BlockType type = state.getBlockType();
                        FuzzyBlockState.Builder fuzzyBuilder = FuzzyBlockState.builder();
                        fuzzyBuilder.type(type);
                        String[] entries = stateString.split(",");
                        for (String entry : entries) {
                            String[] split = entry.split("=");
                            String key = split[0];
                            String val = split[1];
                            Property<Object> prop = type.getProperty(key);
                            fuzzyBuilder.withProperty(prop, prop.getValueFor(val));
                        }
                        state = fuzzyBuilder.build();
                    }
                }
            }
        }
        // this should be impossible but IntelliJ isn't that smart
        if (state == null) {
            throw new NoMatchException("Does not match a valid block type: '" + input + "'");
        }

        if (blockAndExtraData.length > 1 && blockAndExtraData[1].startsWith("{")) {
            String joined = StringMan.join(Arrays.copyOfRange(blockAndExtraData, 1, blockAndExtraData.length), "|");
            try {
                nbt = JSON2NBT.getTagFromJson(joined);
            } catch (NBTException e) {
                throw new NoMatchException(e.getMessage());
            }
        }

        // Check if the item is allowed
        BlockType blockType = state.getBlockType();

        if (nbt != null) return validate(context, state.toBaseBlock(nbt));

        if (blockType == BlockTypes.SIGN || blockType == BlockTypes.WALL_SIGN
                || BlockCategories.SIGNS.contains(blockType)) {
            // Allow special sign text syntax
            String[] text = new String[4];
            text[0] = blockAndExtraData.length > 1 ? blockAndExtraData[1] : "";
            text[1] = blockAndExtraData.length > 2 ? blockAndExtraData[2] : "";
            text[2] = blockAndExtraData.length > 3 ? blockAndExtraData[3] : "";
            text[3] = blockAndExtraData.length > 4 ? blockAndExtraData[4] : "";
            return validate(context, new SignBlock(state, text));
        } else if (blockType == BlockTypes.SPAWNER) {
            // Allow setting mob spawn type
            if (blockAndExtraData.length > 1) {
                String mobName = blockAndExtraData[1];
                EntityType mobType = EntityTypes.parse(mobName);
                return validate(context, new MobSpawnerBlock(state, mobName));
            } else {
                return validate(context, new MobSpawnerBlock(state, EntityTypes.PIG.getId()));
            }
        } else if (blockType == BlockTypes.PLAYER_HEAD || blockType == BlockTypes.PLAYER_WALL_HEAD) {
            // allow setting type/player/rotation
            if (blockAndExtraData.length <= 1) {
                return validate(context, new SkullBlock(state));
            }

            String type = blockAndExtraData[1];

            return validate(context, new SkullBlock(state, type.replace(" ", "_"))); // valid MC usernames
        } else {
            return validate(context, state.toBaseBlock());
        }
    }

    private <T extends BlockStateHolder> T validate(ParserContext context, T holder) {
        if (context.isRestricted()) {
            Actor actor = context.requireActor();
            if (!actor.hasPermission("worldedit.anyblock") && worldEdit.getConfiguration().checkDisallowedBlocks(holder)) {
                throw new DisallowedUsageException(Caption.toString(TranslatableComponent.of("fawe.error.block.not.allowed", holder)));
            }
            CompoundTag nbt = holder.getNbtData();
            if (nbt != null) {
                if (!actor.hasPermission("worldedit.anyblock")) {
                    throw new DisallowedUsageException("You are not allowed to use nbt'");
                }
            }
        }
        return holder;
    }
}
