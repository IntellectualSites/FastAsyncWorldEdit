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

package com.sk89q.worldedit.extension.factory.parser;

import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.extent.inventory.SlottableBlockBag;
import com.fastasyncworldedit.core.jnbt.JSON2NBT;
import com.fastasyncworldedit.core.jnbt.NBTException;
import com.fastasyncworldedit.core.limit.FaweLimit;
import com.fastasyncworldedit.core.limit.PropertyRemap;
import com.fastasyncworldedit.core.util.MathMan;
import com.fastasyncworldedit.core.util.StringMan;
import com.fastasyncworldedit.core.world.block.BlanketBaseBlock;
import com.google.common.collect.Maps;
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
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.internal.registry.InputParser;
import com.sk89q.worldedit.internal.util.DeprecationUtil;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.HandSide;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.FuzzyBlockState;
import com.sk89q.worldedit.world.entity.EntityType;
import com.sk89q.worldedit.world.entity.EntityTypes;
import com.sk89q.worldedit.world.registry.LegacyMapper;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
                throw new InputParseException(e.getRichMessage());
            } catch (WorldEditException e) {
                throw new InputParseException(Caption.of("worldedit.error.unknown", e.getRichMessage()), e);
            }
        } else {
            throw new InputParseException(Caption.of(
                    "worldedit.error.parser.player-only",
                    TextComponent.of(handSide == HandSide.MAIN_HAND ? "hand" : "offhand")
            ));
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

    private static final String[] EMPTY_STRING_ARRAY = {};

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
                return BlockTypes.WHITE_WOOL.id();
            case "black":
                return BlockTypes.BLACK_WOOL.id();
            case "blue":
                return BlockTypes.BLUE_WOOL.id();
            case "brown":
                return BlockTypes.BROWN_WOOL.id();
            case "cyan":
                return BlockTypes.CYAN_WOOL.id();
            case "gray":
            case "grey":
                return BlockTypes.GRAY_WOOL.id();
            case "green":
                return BlockTypes.GREEN_WOOL.id();
            case "light_blue":
            case "lightblue":
                return BlockTypes.LIGHT_BLUE_WOOL.id();
            case "light_gray":
            case "light_grey":
            case "lightgray":
            case "lightgrey":
                return BlockTypes.LIGHT_GRAY_WOOL.id();
            case "lime":
                return BlockTypes.LIME_WOOL.id();
            case "magenta":
                return BlockTypes.MAGENTA_WOOL.id();
            case "orange":
                return BlockTypes.ORANGE_WOOL.id();
            case "pink":
                return BlockTypes.PINK_WOOL.id();
            case "purple":
                return BlockTypes.PURPLE_WOOL.id();
            case "yellow":
                return BlockTypes.YELLOW_WOOL.id();
            case "red":
                return BlockTypes.RED_WOOL.id();
            default:
                return string;
        }
    }

    //FAWE start - make public
    public static Map<Property<?>, Object> parseProperties(
            //FAWE end
            BlockType type,
            String[] stateProperties,
            ParserContext context,
            //FAWE start - if null should be returned instead of throwing an error
            boolean nullNotError
            //FAWE end
    ) throws NoMatchException {
        Map<Property<?>, Object> blockStates = new HashMap<>();

        //FAWE start - disallowed states
        if (context != null && context.getActor() != null && !context.getActor().getLimit().isUnlimited()) {
            for (String input : context.getActor().getLimit().DISALLOWED_BLOCKS) {
                if (input.indexOf('[') == -1 && input.indexOf(']') == -1) {
                    continue;
                }
                if (!type.id().equalsIgnoreCase(input.substring(0, input.indexOf('[')))) {
                    continue;
                }
                String[] properties = input.substring(input.indexOf('[') + 1, input.indexOf(']')).split(",");
                Set<String> blocked = Arrays.stream(properties).filter(s -> {
                    for (String in : stateProperties) {
                        if (in.equalsIgnoreCase(s)) {
                            return true;
                        }
                    }
                    return false;
                }).collect(Collectors.toSet());
                if (!blocked.isEmpty()) {
                    throw new DisallowedUsageException(Caption.of(
                            "fawe.error.limit.disallowed-property",
                            TextComponent.of(input)
                    ));
                }
            }
        }
        //FAWE end

        if (stateProperties.length > 0) { // Block data not yet detected
            // Parse the block data (optional)
            for (String parseableData : stateProperties) {
                try {
                    String[] parts = parseableData.split("=");
                    if (parts.length != 2) {
                        //FAWE start - if null should be returned instead of throwing an error
                        if (nullNotError) {
                            return null;
                        }
                        //FAWE end
                        throw new InputParseException(
                                Caption.of(
                                        "worldedit.error.parser.bad-state-format",
                                        TextComponent.of(parseableData)
                                )
                        );
                    }

                    @SuppressWarnings("unchecked")
                    Property<Object> propertyKey = (Property<Object>) type.getPropertyMap().get(parts[0]);
                    if (propertyKey == null) {
                        //FAWE start - nullable context
                        if (context != null && context.getActor() != null) {
                            //FAWE end
                            //FAWE start - if null should be returned instead of throwing an error
                            if (nullNotError) {
                                return null;
                            }
                            //FAWE end
                            throw new NoMatchException(Caption.of(
                                    "worldedit.error.parser.unknown-property",
                                    TextComponent.of(parts[0]),
                                    TextComponent.of(type.id())
                            ));
                        } else {
                            WorldEdit.logger.debug("Unknown property " + parts[0] + " for block " + type.id());
                        }
                        return Maps.newHashMap();
                    }
                    if (blockStates.containsKey(propertyKey)) {
                        //FAWE start - if null should be returned instead of throwing an error
                        if (nullNotError) {
                            return null;
                        }
                        //FAWE end
                        throw new InputParseException(Caption.of(
                                "worldedit.error.parser.duplicate-property",
                                TextComponent.of(parts[0])
                        ));
                    }
                    Object value;
                    try {
                        value = propertyKey.getValueFor(parts[1]);
                    } catch (IllegalArgumentException e) {
                        //FAWE start - if null should be returned instead of throwing an error
                        if (nullNotError) {
                            return null;
                        }
                        //FAWE end
                        throw new NoMatchException(Caption.of(
                                "worldedit.error.parser.unknown-value",
                                TextComponent.of(parts[1]),
                                TextComponent.of(propertyKey.getName())
                        ));
                    }

                    //FAWE start - blocked states
                    if (context != null && context.getActor() != null && !context.getActor().getLimit().isUnlimited()) {
                        if (context.getActor().getLimit().REMAP_PROPERTIES != null
                                && !context.getActor().getLimit().REMAP_PROPERTIES.isEmpty()) {
                            for (PropertyRemap remap : context.getActor().getLimit().REMAP_PROPERTIES) {
                                Object newValue = remap.apply(type, value);
                                if (newValue != value) {
                                    value = newValue;
                                    break;
                                }
                            }
                        }
                    }
                    //FAWE end

                    blockStates.put(propertyKey, value);
                } catch (NoMatchException | DisallowedUsageException e) {
                    throw e; // Pass-through
                } catch (Exception e) {
                    //FAWE start - if null should be returned instead of throwing an error
                    if (nullNotError) {
                        return null;
                    }
                    //FAWE end
                    throw new InputParseException(Caption.of(
                            "worldedit.error.parser.bad-state-format",
                            TextComponent.of(parseableData)
                    ));
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

    @Nonnull
    private BaseBlock parseLogic(String input, ParserContext context) throws InputParseException {
        //FAWE start
        String[] blockAndExtraData = input.trim().split("(?<!^)\\|");
        blockAndExtraData[0] = woolMapper(blockAndExtraData[0]);
        Map<Property<?>, Object> blockStates = new HashMap<>();
        //FAWE end

        BlockState state = null;

        // Legacy matcher
        if (context.isTryingLegacy()) {
            try {
                String[] split = blockAndExtraData[0].split(":", 2);
                if (split.length == 0) {
                    throw new InputParseException(Caption.of("worldedit.error.parser.invalid-colon"));
                } else if (split.length == 1) {
                    state = LegacyMapper.getInstance().getBlockFromLegacy(Integer.parseInt(split[0]));
                } else if (MathMan.isInteger(split[0])) {
                    int id = Integer.parseInt(split[0]);
                    int data = Integer.parseInt(split[1]);
                    //FAWE start
                    if (data < 0 || data >= 16) {
                        throw new InputParseException(Caption.of("fawe.error.parser.invalid-data", TextComponent.of(data)));
                    }
                    state = LegacyMapper.getInstance().getBlockFromLegacy(id, data);
                } else {
                    BlockType type = BlockTypes.get(split[0].toLowerCase(Locale.ROOT));
                    if (type != null) {
                        int data = Integer.parseInt(split[1]);
                        if (data < 0 || data >= 16) {
                            throw new InputParseException(Caption.of("fawe.error.parser.invalid-data", TextComponent.of(data)));
                        }
                        state = LegacyMapper.getInstance().getBlockFromLegacy(type.getLegacyCombinedId() >> 4, data);
                    }
                }
            } catch (NumberFormatException ignored) {
            }
        }

        CompoundTag nbt = null;
        //FAWE end
        if (state == null) {
            String typeString;
            String stateString = null;
            int stateStart = blockAndExtraData[0].indexOf('[');
            if (stateStart == -1) {
                typeString = blockAndExtraData[0];
            } else {
                typeString = blockAndExtraData[0].substring(0, stateStart);
                if (stateStart + 1 >= blockAndExtraData[0].length()) {
                    throw new InputParseException(Caption.of(
                            "worldedit.error.parser.hanging-lbracket",
                            TextComponent.of(stateStart)
                    ));
                }
                int stateEnd = blockAndExtraData[0].lastIndexOf(']');
                if (stateEnd < 0) {
                    throw new InputParseException(Caption.of("worldedit.error.parser.missing-rbracket"));
                }
                stateString = blockAndExtraData[0].substring(stateStart + 1, blockAndExtraData[0].length() - 1);
            }
            String[] stateProperties = EMPTY_STRING_ARRAY;
            if (stateString != null) {
                stateProperties = stateString.split(",");
            }
            if (typeString.isEmpty()) {
                throw new InputParseException(Caption.of(
                        "worldedit.error.parser.bad-state-format",
                        TextComponent.of(blockAndExtraData[0])
                ));
            }
            if ("hand".equalsIgnoreCase(typeString) || "h".equalsIgnoreCase(typeString)) {
                // Get the block type from the item in the user's hand.
                final BaseBlock blockInHand = getBlockInHand(context.requireActor(), HandSide.MAIN_HAND);
                //FAWE start
                state = blockInHand.toBlockState();
                nbt = blockInHand.getNbtData();
                //FAWE end
            } else if ("offhand".equalsIgnoreCase(typeString) || "oh".equalsIgnoreCase(typeString)) {
                // Get the block type from the item in the user's off hand.
                final BaseBlock blockInHand = getBlockInHand(context.requireActor(), HandSide.OFF_HAND);
                //FAWE start
                state = blockInHand.toBlockState();
                nbt = blockInHand.getNbtData();
                //FAWE end
            } else if (typeString.matches("pos[0-9]+")) {
                int index = Integer.parseInt(typeString.replaceAll("[a-z]+", ""));
                // Get the block type from the "primary position"
                final World world = context.requireWorld();
                final BlockVector3 primaryPosition;
                try {
                    primaryPosition = context.requireSession().getRegionSelector(world).getVertices().get(index - 1);
                } catch (IncompleteRegionException e) {
                    throw new InputParseException(Caption.of("worldedit.error.incomplete-region"));
                }
                state = world.getBlock(primaryPosition);
                nbt = state.getNbtData();
                //FAWE start
            } else if (typeString.matches("slot[0-9]+")) {
                int slot = Integer.parseInt(typeString.substring(4)) - 1;
                Actor actor = context.requireActor();
                if (!(actor instanceof Player)) {
                    throw new InputParseException(Caption.of("worldedit.command.player-only"));
                }
                Player player = (Player) actor;
                BlockBag bag = player.getInventoryBlockBag();
                if (!(bag instanceof SlottableBlockBag)) {
                    throw new InputParseException(Caption.of("fawe.error.unsupported"));
                }
                SlottableBlockBag slottable = (SlottableBlockBag) bag;
                BaseItem item = slottable.getItem(slot);

                if (!item.getType().hasBlockType()) {
                    throw new InputParseException(Caption.of("worldedit.error.not-a-block"));
                }
                state = item.getType().getBlockType().getDefaultState();
                nbt = item.getNbtData();
            } else {
                BlockType type = BlockTypes.parse(typeString.toLowerCase(Locale.ROOT), context);

                if (type != null) {
                    state = type.getDefaultState();
                }
                if (state == null) {
                    throw new NoMatchException(Caption.of("fawe.error.invalid-block-type", TextComponent.of(input)));
                }
                nbt = state.getNbtData();
            }
            //FAWE end

            //FAWE start -  Not null if nullNotError false.
            blockStates.putAll(parseProperties(state.getBlockType(), stateProperties, context, false));
            //FAWE end
            if (context.isPreferringWildcard()) {
                if (stateString == null || stateString.isEmpty()) {
                    state = new FuzzyBlockState(state);
                } else {
                    FuzzyBlockState.Builder fuzzyBuilder = FuzzyBlockState.builder();
                    fuzzyBuilder.type(state.getBlockType());
                    for (Map.Entry<Property<?>, Object> blockState : blockStates.entrySet()) {
                        @SuppressWarnings("unchecked")
                        Property<Object> objProp = (Property<Object>) blockState.getKey();
                        fuzzyBuilder.withProperty(objProp, blockState.getValue());
                    }
                    state = fuzzyBuilder.build();
                }
            } else {
                for (Map.Entry<Property<?>, Object> blockState : blockStates.entrySet()) {
                    @SuppressWarnings("unchecked")
                    Property<Object> objProp = (Property<Object>) blockState.getKey();
                    state = state.with(objProp, blockState.getValue());
                }
            }
        }
        // this should be impossible but IntelliJ isn't that smart
        if (state == null) {
            throw new NoMatchException(Caption.of("worldedit.error.unknown-block", TextComponent.of(input)));
        }

        //FAWE start
        if (blockAndExtraData.length > 1 && blockAndExtraData[1].startsWith("{")) {
            String joined = StringMan.join(Arrays.copyOfRange(blockAndExtraData, 1, blockAndExtraData.length), "|");
            try {
                nbt = JSON2NBT.getTagFromJson(joined);
            } catch (NBTException e) {
                throw new NoMatchException(TextComponent.of(e.getMessage()));
            }
        }
        //FAWE end

        // Check if the item is allowed
        BlockType blockType = state.getBlockType();

        if (context.isRestricted()) {
            Actor actor = context.requireActor();
            //FAWE start - per-limit disallowed blocks
            if (actor != null) {
                if (!actor.hasPermission("worldedit.anyblock")
                        && worldEdit.getConfiguration().disallowedBlocks.contains(blockType.id().toLowerCase(Locale.ROOT))) {
                    throw new DisallowedUsageException(Caption.of(
                            "worldedit.error.disallowed-block",
                            TextComponent.of(blockType.id())
                    ));
                }
                FaweLimit limit = actor.getLimit();
                if (!limit.isUnlimited()) {
                    // No need to account for blocked states/properties as it will simply return false in the equality check
                    // during contains.
                    if (limit.DISALLOWED_BLOCKS.contains(blockType.id().toLowerCase(Locale.ROOT))) {
                        throw new DisallowedUsageException(Caption.of(
                                "fawe.error.limit.disallowed-block",
                                TextComponent.of(blockType.id())
                        ));
                    }
                }
            }
            //FAWE end
        }

        //FAWE start - only handle if extra data is actually supplied or if the user has permission for nbt
        boolean allowWorkingDefault = context.requireActor().hasPermission("worldedit.anyblock.nbt") && nbt != null;
        if (DeprecationUtil.isSign(blockType) && (blockAndExtraData.length > 1 || allowWorkingDefault)) {
            //FAWE end
            // Allow special sign text syntax
            String[] text = new String[4];
            text[0] = blockAndExtraData.length > 1 ? blockAndExtraData[1] : "";
            text[1] = blockAndExtraData.length > 2 ? blockAndExtraData[2] : "";
            text[2] = blockAndExtraData.length > 3 ? blockAndExtraData[3] : "";
            text[3] = blockAndExtraData.length > 4 ? blockAndExtraData[4] : "";
            return validate(context, new SignBlock(state, text));
            //FAWE start - only handle if extra data is actually supplied or if the user has permission for nbt
        } else if (blockType == BlockTypes.SPAWNER && (blockAndExtraData.length > 1 || allowWorkingDefault)) {
            //FAWE end
            // Allow setting mob spawn type
            String mobName;
            if (blockAndExtraData.length > 1) {
                mobName = blockAndExtraData[1];
                EntityType ent = EntityTypes.get(mobName.toLowerCase(Locale.ROOT));
                if (ent == null) {
                    throw new NoMatchException(Caption.of("worldedit.error.unknown-entity", TextComponent.of(mobName)));
                }
                mobName = ent.id();
                if (!worldEdit.getPlatformManager().queryCapability(Capability.USER_COMMANDS).isValidMobType(mobName)) {
                    throw new NoMatchException(Caption.of("worldedit.error.unknown-mob", TextComponent.of(mobName)));
                }
            } else {
                mobName = EntityTypes.PIG.id();
            }
            return validate(context, new MobSpawnerBlock(state, mobName));
            //FAWE start - only handle if extra data is actually supplied or if the user has permission for nbt
        } else if ((blockType == BlockTypes.PLAYER_HEAD || blockType == BlockTypes.PLAYER_WALL_HEAD) && (blockAndExtraData.length > 1 || allowWorkingDefault)) {
            //FAWE end
            // allow setting type/player/rotation
            if (blockAndExtraData.length == 1) {
                return validate(context, new SkullBlock(state));
            }

            String type = blockAndExtraData[1];

            return validate(context, new SkullBlock(state, type.replace(" ", "_"))); // valid MC usernames
        } else {
            //FAWE start
            if (nbt == null) {
                nbt = state.getNbtData();
            }
            BaseBlock result;
            if (nbt != null) {
                result = !blockStates.isEmpty() ? state.toBaseBlock(nbt) : new BlanketBaseBlock(state, nbt);
            } else {
                result = !blockStates.isEmpty() ? new BaseBlock(state) : state.toBaseBlock();
            }
            return validate(context, result);
            //FAWE end
        }
    }

    //FAWE Start
    private <T extends BlockStateHolder> T validate(ParserContext context, T holder) {
        if (context.isRestricted()) {
            Actor actor = context.requireActor();
            if (!actor.hasPermission("worldedit.anyblock") && worldEdit.getConfiguration().checkDisallowedBlocks(holder)) {
                throw new DisallowedUsageException(Caption.of(
                        "worldedit.error.disallowed-block",
                        TextComponent.of(String.valueOf(holder))
                ));
            }
            CompoundTag nbt = holder.getNbtData();
            if (nbt != null) {
                if (nbt.equals(holder.getBlockType().getDefaultState().getNbtData())) {
                    if (!actor.hasPermission("worldedit.anyblock.default-nbt")) {
                        throw new DisallowedUsageException(Caption.of(
                                "fawe.error.nbt.forbidden",
                                TextComponent.of("worldedit.anyblock.default-nbt")
                        ));
                    }
                } else if (!actor.hasPermission("worldedit.anyblock.nbt")) {
                    throw new DisallowedUsageException(Caption.of(
                            "fawe.error.nbt.forbidden",
                            TextComponent.of("worldedit.anyblock.nbt")
                    ));
                }
            }
        }
        return holder;
    }
    //FAWE end
}
