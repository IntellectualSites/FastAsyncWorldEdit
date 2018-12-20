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

package com.sk89q.worldedit.extension.factory;

import com.boydti.fawe.command.SuggestInputParseException;
import com.boydti.fawe.jnbt.JSON2NBT;
import com.boydti.fawe.jnbt.NBTException;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.StringMan;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.blocks.MobSpawnerBlock;
import com.sk89q.worldedit.blocks.SignBlock;
import com.sk89q.worldedit.blocks.SkullBlock;
import com.sk89q.worldedit.blocks.metadata.MobType;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.input.DisallowedUsageException;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.NoMatchException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.extent.inventory.SlottableBlockBag;
import com.sk89q.worldedit.internal.registry.InputParser;
import com.sk89q.worldedit.util.HandSide;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.registry.LegacyMapper;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Parses block input strings.
 */
public class DefaultBlockParser extends InputParser<BlockStateHolder> {

    public DefaultBlockParser(WorldEdit worldEdit) {
        super(worldEdit);
    }

    private static BlockState getBlockInHand(Actor actor, HandSide handSide) throws InputParseException {
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

    public BlockStateHolder parseFromInput(String input, ParserContext context)
            throws InputParseException {
        String originalInput = input;
        input = input.replace(";", "|");
        Exception suppressed = null;
        try {
            BlockStateHolder modified = parseLogic(input, context);
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

    private static String[] EMPTY_STRING_ARRAY = new String[]{};

    /**
     * Backwards compatibility for wool colours in block syntax.
     *
     * @param string Input string
     * @return Mapped string
     */
    private String woolMapper(String string) {
        switch (string.toLowerCase()) {
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

    private BlockStateHolder parseLogic(String input, ParserContext context) throws InputParseException {
        String[] blockAndExtraData = input.trim().split("\\|", 2);
        blockAndExtraData[0] = woolMapper(blockAndExtraData[0]);

        BlockState state = null;
        CompoundTag nbt = null;

        // Legacy matcher
        if (context.isTryingLegacy()) {
            try {
                String[] split = blockAndExtraData[0].split(":");
                if (split.length == 1) {
                    state = LegacyMapper.getInstance().getBlockFromLegacy(Integer.parseInt(split[0]));
                } else if (MathMan.isInteger(split[0])) {
                    state = LegacyMapper.getInstance().getBlockFromLegacy(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
                } else {
                    BlockTypes type = BlockTypes.get(split[0].toLowerCase());
                    if (type != null) {
                        state = LegacyMapper.getInstance().getBlockFromLegacy(type.getLegacyCombinedId() >> 4, Integer.parseInt(split[1]));
                    }
                }
            } catch (NumberFormatException ignore) {}
        }

        if (state == null) {
            String typeString;
            String stateString = null;
            int stateStart = blockAndExtraData[0].indexOf('[');
            if (stateStart == -1) {
                typeString = blockAndExtraData[0];
            } else {
                typeString = blockAndExtraData[0].substring(0, stateStart);
                stateString = blockAndExtraData[0].substring(stateStart + 1, blockAndExtraData[0].length() - 1);
            }
            if (typeString == null || typeString.isEmpty()) {
                throw new InputParseException("Invalid format");
            }
            // PosX
            if (typeString.matches("pos[0-9]+")) {
                int index = Integer.parseInt(typeString.replaceAll("[a-z]+", ""));
                // Get the block type from the "primary position"
                final World world = context.requireWorld();
                final Vector primaryPosition;
                try {
                    primaryPosition = context.requireSession().getRegionSelector(world).getVerticies().get(index - 1);
                } catch (IncompleteRegionException e) {
                    throw new InputParseException("Your selection is not complete.");
                }
                state = world.getBlock(primaryPosition);
            } else if (typeString.equalsIgnoreCase("hand")) {
                // Get the block type from the item in the user's hand.
                state = getBlockInHand(context.requireActor(), HandSide.MAIN_HAND);
            } else if (typeString.equalsIgnoreCase("offhand")) {
                // Get the block type from the item in the user's off hand.
                state = getBlockInHand(context.requireActor(), HandSide.OFF_HAND);
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
                BlockTypes type = BlockTypes.parse(typeString.toLowerCase());
                if (type != null) state = type.getDefaultState();
                if (state == null) {
                    throw new NoMatchException("Does not match a valid block type: '" + input + "'");
                }
            }
            if (nbt == null) nbt = state.getNbtData();

            if (stateString != null) {
                state = BlockState.get(state.getBlockType(), "[" + stateString + "]", state);
            }
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
        BlockTypes blockType = state.getBlockType();

        if (context.isRestricted()) {
            Actor actor = context.requireActor();
            if (actor != null) {
                if (!actor.hasPermission("worldedit.anyblock") && worldEdit.getConfiguration().disallowedBlocks.contains(blockType)) {
                    throw new DisallowedUsageException("You are not allowed to use '" + input + "'");
                }
                if (nbt != null) {
                    if (!actor.hasPermission("worldedit.anyblock")) {
                        throw new DisallowedUsageException("You are not allowed to nbt'");
                    }
                }
            }
        }

        if (nbt != null) return new BaseBlock(state, nbt);

        if (blockType == BlockTypes.SIGN || blockType == BlockTypes.WALL_SIGN) {
            // Allow special sign text syntax
            String[] text = new String[4];
            text[0] = blockAndExtraData.length > 1 ? blockAndExtraData[1] : "";
            text[1] = blockAndExtraData.length > 2 ? blockAndExtraData[2] : "";
            text[2] = blockAndExtraData.length > 3 ? blockAndExtraData[3] : "";
            text[3] = blockAndExtraData.length > 4 ? blockAndExtraData[4] : "";
            return new SignBlock(state, text);
        } else if (blockType == BlockTypes.SPAWNER) {
            // Allow setting mob spawn type
            if (blockAndExtraData.length > 1) {
                String mobName = blockAndExtraData[1];
                for (MobType mobType : MobType.values()) {
                    if (mobType.getName().toLowerCase().equals(mobName.toLowerCase())) {
                        mobName = mobType.getName();
                        break;
                    }
                }
                Platform capability = worldEdit.getPlatformManager().queryCapability(Capability.USER_COMMANDS);
                if (!capability.isValidMobType(mobName)) {
                    final String finalMobName = mobName.toLowerCase();
                    throw new SuggestInputParseException("Unknown mob type '" + mobName + "'", mobName, () -> Stream.of(MobType.values())
                    .map(m -> m.getName().toLowerCase())
                    .filter(s -> s.startsWith(finalMobName))
                    .collect(Collectors.toList()));
                }
                return new MobSpawnerBlock(state, mobName);
            } else {
                return new MobSpawnerBlock(state, MobType.PIG.getName());
            }
        } else if (blockType == BlockTypes.PLAYER_HEAD || blockType == BlockTypes.PLAYER_WALL_HEAD) {
            // allow setting type/player/rotation
            if (blockAndExtraData.length <= 1) {
                return new SkullBlock(state);
            }

            String type = blockAndExtraData[1];

            return new SkullBlock(state, type.replace(" ", "_")); // valid MC usernames
        } else {
            return state;
        }
    }
}