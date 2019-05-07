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

import com.boydti.fawe.command.SuggestInputParseException;
import com.boydti.fawe.config.BBC;
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
import com.sk89q.worldedit.blocks.metadata.MobType;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.input.DisallowedUsageException;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.NoMatchException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.extent.inventory.SlottableBlockBag;
import com.sk89q.worldedit.internal.registry.InputParser;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.HandSide;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.FuzzyBlockState;
import com.sk89q.worldedit.world.registry.LegacyMapper;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Parses block input strings.
 */
public class DefaultBlockParser extends InputParser<BaseBlock> {

    public DefaultBlockParser(WorldEdit worldEdit) {
        super(worldEdit);
    }

    private static BlockState getBlockInHand(Actor actor, HandSide handSide) throws InputParseException {
        if (actor instanceof Player) {
            try {
                return ((Player) actor).getBlockInHand(handSide).toImmutableState();
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

    private BaseBlock parseLogic(String input, ParserContext context) throws InputParseException {
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
                    int id = Integer.parseInt(split[0]);
                    int data = Integer.parseInt(split[1]);
                    if (data < 0 || data >= 16) {
                        throw new InputParseException("Invalid data " + data);
                    }
                    state = LegacyMapper.getInstance().getBlockFromLegacy(id, data);
                } else {
                    BlockType type = BlockTypes.get(split[0].toLowerCase());
                    if (type != null) {
                        int data = Integer.parseInt(split[1]);
                        if (data < 0 || data >= 16) {
                            throw new InputParseException("Invalid data " + data);
                        }
                        state = LegacyMapper.getInstance().getBlockFromLegacy(type.getLegacyCombinedId() >> 4, data);
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
                final BlockVector3 primaryPosition;
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
                    throw new InputParseException(BBC.getPrefix() + "You're not holding a block!");
                }
                state = item.getType().getBlockType().getDefaultState();
                nbt = item.getNbtData();
            } else {
                BlockType type = BlockTypes.parse(typeString.toLowerCase());
                if (type != null) state = type.getDefaultState();
                if (state == null) {
                    throw new NoMatchException(BBC.getPrefix() + "Does not match a valid block type: '" + input + "'");
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

        if (blockType == BlockTypes.SIGN || blockType == BlockTypes.WALL_SIGN) {
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
                for (MobType mobType : MobType.values()) {
                    if (mobType.getName().toLowerCase().equals(mobName.toLowerCase())) {
                        mobName = mobType.getName();
                        break;
                    }
                }
                Platform capability = worldEdit.getPlatformManager().queryCapability(Capability.USER_COMMANDS);
                if (!capability.isValidMobType(mobName)) {
                    final String finalMobName = mobName.toLowerCase();
                    throw new SuggestInputParseException(BBC.getPrefix() + "Unknown mob type '" + mobName + "'", mobName, () -> Stream.of(MobType.values())
                    .map(m -> m.getName().toLowerCase())
                    .filter(s -> s.startsWith(finalMobName))
                    .collect(Collectors.toList()));
                }
                return validate(context, new MobSpawnerBlock(state, mobName));
            } else {
                return validate(context, new MobSpawnerBlock(state, MobType.PIG.getName()));
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
                throw new DisallowedUsageException(BBC.BLOCK_NOT_ALLOWED + " '" + holder + "'");
            }
            CompoundTag nbt = holder.getNbtData();
            if (nbt != null) {
                if (!actor.hasPermission("worldedit.anyblock")) {
                    throw new DisallowedUsageException("You are not allowed to nbt'");
                }
            }
        }
        return holder;
    }
}