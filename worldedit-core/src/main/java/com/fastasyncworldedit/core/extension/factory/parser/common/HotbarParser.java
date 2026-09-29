package com.fastasyncworldedit.core.extension.factory.parser.common;

import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.extent.inventory.SlottableBlockBag;
import com.fastasyncworldedit.core.limit.FaweLimit;
import com.google.common.collect.ImmutableList;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.input.DisallowedUsageException;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.internal.registry.SimpleInputParser;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.world.block.BlockType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public abstract class HotbarParser<T> extends SimpleInputParser<T> {

    private final List<String> aliases = ImmutableList.of("#hotbar");

    protected HotbarParser(final WorldEdit worldEdit) {
        super(worldEdit);
    }

    @Override
    public List<String> getMatchedAliases() {
        return aliases;
    }

    protected List<BlockType> getBlockTypes(ParserContext context) {
        Player player = context.requirePlayer();
        BlockBag bag = player.getInventoryBlockBag();
        if (!(bag instanceof final SlottableBlockBag slottable)) {
            // Matches DefaultBlockParser
            throw new InputParseException(Caption.of("fawe.error.unsupported"));
        }
        List<BlockType> types = new ArrayList<>();
        FaweLimit limit = player.getLimit();
        boolean anyBlock = player.hasPermission("worldedit.anyblock");
        for (int slot = 0; slot < 9; slot++) {
            BaseItem item = slottable.getItem(slot);
            if (item != null && item.getType().hasBlockType()) {
                BlockType type = item.getType().getBlockType();
                if (!anyBlock && worldEdit.getConfiguration().disallowedBlocks.contains(type.id().toLowerCase(Locale.ROOT))) {
                    throw new DisallowedUsageException(Caption.of(
                            "worldedit.error.disallowed-block",
                            TextComponent.of(type.getId())
                    ));
                }
                if (!limit.isUnlimited()) {
                    if (limit.DISALLOWED_BLOCKS.contains(type.id().toLowerCase(Locale.ROOT))) {
                        throw new DisallowedUsageException(Caption.of(
                                "fawe.error.limit.disallowed-block",
                                TextComponent.of(type.getId())
                        ));
                    }
                }
                types.add(type);
            }
        }
        if (types.isEmpty()) {
            throw new InputParseException(Caption.of("fawe.error.no-valid-on-hotbar"));
        }
        return types;
    }

}
