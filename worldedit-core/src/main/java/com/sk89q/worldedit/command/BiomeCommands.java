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

package com.sk89q.worldedit.command;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Commands;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.visitor.Fast2DIterator;
import com.boydti.fawe.util.chat.Message;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.Logging;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.function.FlatRegionFunction;
import com.sk89q.worldedit.function.FlatRegionMaskingFilter;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.biome.BiomeReplace;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Mask2D;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.visitor.FlatRegionVisitor;
import com.sk89q.worldedit.function.visitor.RegionVisitor;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.FlatRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.Regions;
import com.sk89q.worldedit.util.Countable;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.command.binding.Switch;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeData;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.registry.BiomeRegistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.sk89q.minecraft.util.commands.Logging.LogMode.REGION;

/**
 * Implements biome-related commands such as "/biomelist".
 */
@Command(aliases = {}, desc = "Change, list and inspect biomes")
public class BiomeCommands extends MethodCommands {

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public BiomeCommands(WorldEdit worldEdit) {
        super(worldEdit);
    }

    private BiomeRegistry getBiomeRegistry() {
        return worldEdit.getPlatformManager().queryCapability(Capability.GAME_HOOKS).getRegistries().getBiomeRegistry();
    }

    @Command(
            aliases = {"biomelist", "biomels"},
            usage = "[page]",
            desc = "Gets all biomes available.",
            max = 1
    )
    @CommandPermissions("worldedit.biome.list")
    public void biomeList(Player player, CommandContext args) throws WorldEditException {
        int page;
        int offset;
        int count = 0;

        if (args.argsLength() == 0 || (page = args.getInteger(0)) < 2) {
            page = 1;
            offset = 0;
        } else {
            offset = (page - 1) * 19;
        }

        BiomeRegistry biomeRegistry = getBiomeRegistry();
        Collection<BiomeType> biomes = BiomeTypes.values();
        int totalPages = biomes.size() / 19 + 1;
        Message msg = BBC.BIOME_LIST_HEADER.m(page, totalPages);
        String setBiome = Commands.getAlias(BiomeCommands.class, "/setbiome");
        for (BiomeType biome : biomes) {
            if (offset > 0) {
                offset--;
            } else {
                BiomeData data = biomeRegistry.getData(biome);
                if (data != null) {
                    msg.newline().text(data.getName()).cmdTip(setBiome + " " + data.getName());
                } else {
                    msg.newline().text("<? #" + biome.getId() + ">").cmdTip(setBiome + " " + biome.getId());
                }
                if (++count == 18) {
                    break;
                }
            }
        }
        msg.newline().paginate(getCommand().aliases()[0], page, totalPages);
        msg.send(player);
    }

    @Command(
            aliases = {"biomeinfo"},
            flags = "pt",
            desc = "Get the biome of the targeted block.",
            help =
                    "Get the biome of the block.\n" +
                            "By default use all the blocks contained in your selection.\n" +
                            "-t use the block you are looking at.\n" +
                            "-p use the block you are currently in",
            max = 0
    )
    @CommandPermissions("worldedit.biome.info")
    public void biomeInfo(Player player, LocalSession session, final EditSession editSession, CommandContext args) throws WorldEditException {
        BiomeRegistry biomeRegistry = getBiomeRegistry();
        Collection<BiomeType> values = BiomeTypes.values();
        final int[] biomes = new int[values.size()];
        final String qualifier;

        int size = 0;
        if (args.hasFlag('t')) {
            Location blockPosition = player.getBlockTrace(300);
            if (blockPosition == null) {
                BBC.NO_BLOCK.send(player);
                return;
            }

            BiomeType biome = player.getWorld().getBiome(blockPosition.toBlockPoint().toBlockVector2());
            biomes[biome.getInternalId()]++;
            size = 1;
        } else if (args.hasFlag('p')) {
            BiomeType biome = player.getWorld().getBiome(player.getLocation().toBlockPoint().toBlockVector2());
            biomes[biome.getInternalId()]++;
            size = 1;
        } else {
            World world = player.getWorld();
            Region region = session.getSelection(world);

            if (region instanceof FlatRegion) {
                for (BlockVector2 pt : new Fast2DIterator(((FlatRegion) region).asFlatRegion(), editSession)) {
                    biomes[editSession.getBiome(pt).getInternalId()]++;
                    size++;
                }
            } else {
                RegionVisitor visitor = new RegionVisitor(region, new RegionFunction() {
                    @Override
                    public boolean apply(BlockVector3 position) throws WorldEditException {
                        biomes[editSession.getBiome(position.toBlockVector2()).getInternalId()]++;
                        return true;
                    }
                }, editSession);
                Operations.completeBlindly(visitor);
                size += visitor.getAffected();
            }
        }

        BBC.BIOME_LIST_HEADER.send(player, 1, 1);

        List<Countable<BiomeType>> distribution = new ArrayList<>();
        for (int i = 0; i < biomes.length; i++) {
            int count = biomes[i];
            if (count != 0) {
                distribution.add(new Countable<>(BiomeTypes.get(i), count));
            }
        }
        Collections.sort(distribution);
        for (Countable<BiomeType> c : distribution) {
            BiomeData data = biomeRegistry.getData(c.getID());
            String str = String.format("%-7s (%.3f%%) %s #%d",
                    String.valueOf(c.getAmount()),
                    c.getAmount() / (double) size * 100,
                    data == null ? "Unknown" : data.getName(),
                    c.getID().getId());
            player.print(BBC.getPrefix() + str);
        }
    }

    @Command(
            aliases = {"/setbiome"},
            usage = "<biome>",
            flags = "p",
            desc = "Sets the biome of the player's current block or region.",
            help =
                    "Set the biome of the region.\n" +
                            "By default use all the blocks contained in your selection.\n" +
                            "-p use the block you are currently in"
    )
    @Logging(REGION)
    @CommandPermissions("worldedit.biome.set")
    public void setBiome(Player player, LocalSession session, EditSession editSession, BiomeType target, @Switch('p') boolean atPosition) throws WorldEditException {
        World world = player.getWorld();
        Region region;
        Mask mask = editSession.getMask();
        Mask2D mask2d = mask != null ? mask.toMask2D() : null;

        if (atPosition) {
            region = new CuboidRegion(player.getLocation().toBlockPoint(), player.getLocation().toBlockPoint());
        } else {
            region = session.getSelection(world);
        }

        FlatRegionFunction replace = new BiomeReplace(editSession, target);
        if (mask2d != null) {
            replace = new FlatRegionMaskingFilter(mask2d, replace);
        }
        FlatRegionVisitor visitor = new FlatRegionVisitor(Regions.asFlatRegion(region), replace);
        Operations.completeLegacy(visitor);

        BBC.BIOME_CHANGED.send(player, visitor.getAffected());
        if (!FawePlayer.wrap(player).hasPermission("fawe.tips"))
            BBC.TIP_BIOME_PATTERN.or(BBC.TIP_BIOME_MASK).send(player);
    }
}
