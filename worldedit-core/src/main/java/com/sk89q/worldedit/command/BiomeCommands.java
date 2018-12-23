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
<<<<<<< HEAD
import com.sk89q.worldedit.function.visitor.RegionVisitor;
=======
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
>>>>>>> 399e0ad5... Refactor vector system to be cleaner
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.FlatRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.Regions;
import com.sk89q.worldedit.util.Countable;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.command.binding.Switch;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.biome.BiomeData;
import com.sk89q.worldedit.world.registry.BiomeRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


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
        List<BaseBiome> biomes = biomeRegistry.getBiomes();
        int totalPages = biomes.size() / 19 + 1;
        Message msg = BBC.BIOME_LIST_HEADER.m(page, totalPages);
        String setBiome = Commands.getAlias(BiomeCommands.class, "/setbiome");
        for (BaseBiome biome : biomes) {
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
        final int[] biomes = new int[256];
        final String qualifier;

        int size = 0;
        if (args.hasFlag('t')) {
            Location blockPosition = player.getBlockTrace(300);
            if (blockPosition == null) {
                BBC.NO_BLOCK.send(player);
                return;
            }

<<<<<<< HEAD
            BaseBiome biome = player.getWorld().getBiome(blockPosition.toVector().toVector2D());
            biomes[biome.getId()]++;
            size = 1;
        } else if (args.hasFlag('p')) {
            BaseBiome biome = player.getWorld().getBiome(player.getLocation().toVector().toVector2D());
            biomes[biome.getId()]++;
            size = 1;
=======
            BaseBiome biome = player.getWorld().getBiome(blockPosition.toVector().toBlockPoint().toBlockVector2());
            biomes.add(biome);

            qualifier = "at line of sight point";
        } else if (args.hasFlag('p')) {
            BaseBiome biome = player.getWorld().getBiome(player.getLocation().toVector().toBlockPoint().toBlockVector2());
            biomes.add(biome);

            qualifier = "at your position";
>>>>>>> 399e0ad5... Refactor vector system to be cleaner
        } else {
            World world = player.getWorld();
            Region region = session.getSelection(world);

            if (region instanceof FlatRegion) {
<<<<<<< HEAD
                for (Vector2D pt : new Fast2DIterator(((FlatRegion) region).asFlatRegion(), editSession)) {
                    biomes[editSession.getBiome(pt).getId()]++;
                    size++;
                }
            } else {
                RegionVisitor visitor = new RegionVisitor(region, new RegionFunction() {
                    @Override
                    public boolean apply(Vector position) throws WorldEditException {
                        biomes[editSession.getBiome(position.toVector2D()).getId()]++;
                        return true;
                    }
                }, editSession);
                Operations.completeBlindly(visitor);
                size += visitor.getAffected();
=======
                for (BlockVector2 pt : ((FlatRegion) region).asFlatRegion()) {
                    biomes.add(world.getBiome(pt));
                }
            } else {
                for (BlockVector3 pt : region) {
                    biomes.add(world.getBiome(pt.toBlockVector2()));
                }
>>>>>>> 399e0ad5... Refactor vector system to be cleaner
            }
        }

        BBC.BIOME_LIST_HEADER.send(player, 1, 1);

        List<Countable<BaseBiome>> distribution = new ArrayList<>();
        for (int i = 0; i < biomes.length; i++) {
            int count = biomes[i];
            if (count != 0) {
                distribution.add(new Countable<BaseBiome>(new BaseBiome(i), count));
            }
        }
        Collections.sort(distribution);
        for (Countable<BaseBiome> c : distribution) {
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
    public void setBiome(Player player, LocalSession session, EditSession editSession, BaseBiome target, @Switch('p') boolean atPosition) throws WorldEditException {
        World world = player.getWorld();
        Region region;
        Mask mask = editSession.getMask();
        Mask2D mask2d = mask != null ? mask.toMask2D() : null;

        if (atPosition) {
            region = new CuboidRegion(player.getLocation().toVector().toBlockPoint(), player.getLocation().toVector().toBlockPoint());
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
