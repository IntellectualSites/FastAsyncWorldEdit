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

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.jnbt.anvil.generator.CavesGen;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.TextureUtil;
import com.boydti.fawe.util.image.ImageUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.Logging;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.RegionVisitor;
import com.sk89q.worldedit.internal.annotation.Selection;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.TreeGenerator.TreeType;
import com.sk89q.worldedit.util.command.binding.Range;
import com.sk89q.worldedit.util.command.binding.Switch;
import com.sk89q.worldedit.util.command.binding.Text;
import com.sk89q.worldedit.util.command.parametric.Optional;
import com.sk89q.worldedit.util.command.parametric.ParameterException;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockType;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import static com.sk89q.minecraft.util.commands.Logging.LogMode.*;


/**
 * Commands for the generation of shapes and other objects.
 */
@Command(aliases = {}, desc = "Create structures and features: [More Info](https://goo.gl/KuLFRW)")
public class GenerationCommands extends MethodCommands {

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public GenerationCommands(WorldEdit worldEdit) {
        super(worldEdit);
    }

    @Command(
            aliases = {"/caves"},
            usage = "[size=8] [freq=40] [rarity=7] [minY=8] [maxY=127] [sysFreq=1] [sysRarity=25] [pocketRarity=0] [pocketMin=0] [pocketMax=3]",
            desc = "Generates caves",
            help = "Generates a cave network"
    )
    @CommandPermissions("worldedit.generation.caves")
    @Logging(PLACEMENT)
    public void caves(FawePlayer fp, LocalSession session, EditSession editSession, @Selection Region region, @Optional("8") int size, @Optional("40") int frequency, @Optional("7") int rarity, @Optional("8") int minY, @Optional("127") int maxY, @Optional("1") int systemFrequency, @Optional("25") int individualRarity, @Optional("0") int pocketChance, @Optional("0") int pocketMin, @Optional("3") int pocketMax, CommandContext context) throws WorldEditException, ParameterException {
        fp.checkConfirmationRegion(() -> {
            CavesGen gen = new CavesGen(size, frequency, rarity, minY, maxY, systemFrequency, individualRarity, pocketChance, pocketMin, pocketMax);
            editSession.generate(region, gen);
            BBC.VISITOR_BLOCK.send(fp, editSession.getBlockChangeCount());
        }, getArguments(context), region, context);
    }

    // public void addOre(Mask mask, Pattern material, int size, int frequency, int rarity, int minY, int maxY) throws WorldEditException {

    @Command(
            aliases = {"/ores"},
            desc = "Generates ores",
            help = "Generates ores",
            min = 1,
            max = 1
    )
    @CommandPermissions("worldedit.generation.ore")
    @Logging(PLACEMENT)
    public void ores(FawePlayer player, LocalSession session, EditSession editSession, @Selection Region region, Mask mask, CommandContext context) throws WorldEditException, ParameterException {
        player.checkConfirmationRegion(() -> {
            editSession.addOres(region, mask);
            BBC.VISITOR_BLOCK.send(player, editSession.getBlockChangeCount());
        }, getArguments(context), region, context);
    }

    @Command(
            aliases = {"/image", "/img"},
            desc = "Generate an image",
            usage = "<imgur> [randomize=true] [complexity=100] [dimensions=100,100]",
            min = 1,
            max = 4
    )
    @CommandPermissions("worldedit.generation.image")
    @Logging(PLACEMENT)
    public void image(Player player, LocalSession session, EditSession editSession, String arg, @Optional("true") boolean randomize, @Optional("100") int threshold, @Optional BlockVector2 dimensions) throws WorldEditException, ParameterException, IOException {
        TextureUtil tu = Fawe.get().getCachedTextureUtil(randomize, 0, threshold);
        URL url = new URL(arg);
        if (!url.getHost().equalsIgnoreCase("i.imgur.com") && !url.getHost().equalsIgnoreCase("empcraft.com")) {
            throw new IOException("Only i.imgur.com or empcraft.com/ui links are allowed!");
        }
        FawePlayer<Object> fp = FawePlayer.wrap(player);
        BufferedImage image = MainUtil.readImage(url);
        if (dimensions != null) {
            image = ImageUtil.getScaledInstance(image, dimensions.getBlockX(), dimensions.getBlockZ(), RenderingHints.VALUE_INTERPOLATION_BILINEAR, false);
        }

//        MutableBlockVector3 pos1 = new MutableBlockVector3(player.getLocation().toBlockPoint());
//        MutableBlockVector3 pos2 = new MutableBlockVector3(pos1.add(image.getWidth() - 1, 0, image.getHeight() - 1));
      BlockVector3 pos1 = player.getLocation().toBlockPoint();
      BlockVector3 pos2 = pos1.add(image.getWidth() - 1, 0, image.getHeight() - 1);
        CuboidRegion region = new CuboidRegion(pos1, pos2);
        int[] count = new int[1];
        final BufferedImage finalImage = image;
        RegionVisitor visitor = new RegionVisitor(region, new RegionFunction() {
            @Override
            public boolean apply(BlockVector3 pos) throws WorldEditException {
                try {
                    int x = pos.getBlockX() - pos1.getBlockX();
                    int z = pos.getBlockZ() - pos1.getBlockZ();
                    int color = finalImage.getRGB(x, z);
                    BlockType block = tu.getNearestBlock(color);
                    count[0]++;
                    if (block != null) return editSession.setBlock(pos, block.getDefaultState());
                    return false;
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                return false;
            }
        }, editSession);
        Operations.completeBlindly(visitor);
        BBC.VISITOR_BLOCK.send(player, editSession.getBlockChangeCount());
    }

    @Command(
            aliases = {"/ore"},
            usage = "<mask> <pattern> <size> <freq> <rarity> <minY> <maxY>",
            desc = "Generates ores",
            help = "Generates ores",
            min = 7,
            max = 7
    )
    @CommandPermissions("worldedit.generation.ore")
    @Logging(PLACEMENT)
    public void ore(FawePlayer player, LocalSession session, EditSession editSession, @Selection Region region, Mask mask, Pattern material, @Range(min = 0) int size, int freq, @Range(min = 0, max = 100) int rarity, @Range(min = 0, max = 255) int minY, @Range(min = 0, max = 255) int maxY, CommandContext context) throws WorldEditException, ParameterException {
        player.checkConfirmationRegion(() -> {
            editSession.addOre(region, mask, material, size, freq, rarity, minY, maxY);
            BBC.VISITOR_BLOCK.send(player, editSession.getBlockChangeCount());
        }, getArguments(context), region, context);
    }

    @Command(
            aliases = {"/hcyl"},
            usage = "<pattern> <radius>[,<radius>] [height]",
            desc = "Generates a hollow cylinder.",
            help =
                    "Generates a hollow cylinder.\n" +
                            "By specifying 2 radii, separated by a comma,\n" +
                            "you can generate elliptical cylinders.\n" +
                            "The 1st radius is north/south, the 2nd radius is east/west.",
            min = 2,
            max = 4
    )
    @CommandPermissions("worldedit.generation.cylinder")
    @Logging(PLACEMENT)
    public void hcyl(FawePlayer fp, Player player, LocalSession session, EditSession editSession, Pattern pattern, BlockVector2 radius, @Optional("1") int height, @Range(min = 1) @Optional("1") double thickness, CommandContext context) throws WorldEditException, ParameterException {
        double max = MathMan.max(radius.getBlockX(), radius.getBlockZ());
        worldEdit.checkMaxRadius(max);
        BlockVector3 pos = session.getPlacementPosition(player);
        fp.checkConfirmationRadius(() -> {
            int affected = editSession.makeHollowCylinder(pos, pattern, radius.getX(), radius.getZ(), Math.min(256, height), thickness - 1);
            BBC.VISITOR_BLOCK.send(fp, affected);
        }, getArguments(context), (int) max, context);
    }

    @Command(
            aliases = {"/cyl"},
            usage = "<pattern> <radius>[,<radius>] [height]",
            flags = "h",
            desc = "Generates a cylinder.",
            help =
                    "Generates a cylinder.\n" +
                            "By specifying 2 radii, separated by a comma,\n" +
                            "you can generate elliptical cylinders.\n" +
                            "The 1st radius is north/south, the 2nd radius is east/west.",
            min = 2,
            max = 3
    )
    @CommandPermissions("worldedit.generation.cylinder")
    @Logging(PLACEMENT)
    public void cyl(FawePlayer fp, Player player, LocalSession session, EditSession editSession, Pattern pattern, BlockVector2 radius, @Optional("1") int height, @Switch('h') boolean hollow, CommandContext context) throws WorldEditException, ParameterException {
        double max = MathMan.max(radius.getBlockX(), radius.getBlockZ());
        worldEdit.checkMaxRadius(max);
        BlockVector3 pos = session.getPlacementPosition(player);
        fp.checkConfirmationRadius(() -> {
            int affected = editSession.makeCylinder(pos, pattern, radius.getX(), radius.getZ(), Math.min(256, height), !hollow);
            BBC.VISITOR_BLOCK.send(fp, affected);
        }, getArguments(context), (int) max, context);
    }

    @Command(
            aliases = {"/hsphere"},
            usage = "<pattern> <radius>[,<radius>,<radius>] [raised?]",
            desc = "Generates a hollow sphere.",
            help =
                    "Generates a hollow sphere.\n" +
                            "By specifying 3 radii, separated by commas,\n" +
                            "you can generate an ellipsoid. The order of the ellipsoid radii\n" +
                            "is north/south, up/down, east/west.",
            min = 2,
            max = 3
    )
    @CommandPermissions("worldedit.generation.sphere")
    @Logging(PLACEMENT)
    public void hsphere(FawePlayer fp, Player player, LocalSession session, EditSession editSession, Pattern pattern, BlockVector3 radius, @Optional("false") boolean raised, CommandContext context) throws WorldEditException, ParameterException {
        sphere(fp, player, session, editSession, pattern, radius, raised, true, context);
    }

    @Command(
            aliases = {"/sphere"},
            usage = "<pattern> <radius>[,<radius>,<radius>] [raised?]",
            flags = "h",
            desc = "Generates a filled sphere.",
            help =
                    "Generates a filled sphere.\n" +
                            "By specifying 3 radii, separated by commas,\n" +
                            "you can generate an ellipsoid. The order of the ellipsoid radii\n" +
                            "is north/south, up/down, east/west.",
            min = 2,
            max = 3
    )
    @CommandPermissions("worldedit.generation.sphere")
    @Logging(PLACEMENT)
    public void sphere(FawePlayer fp, Player player, LocalSession session, EditSession editSession, Pattern pattern, BlockVector3 radius, @Optional("false") boolean raised, @Switch('h') boolean hollow, CommandContext context) throws WorldEditException, ParameterException {
        double max = MathMan.max(radius.getBlockX(), radius.getBlockY(), radius.getBlockZ());
        worldEdit.checkMaxRadius(max);
        BlockVector3 pos = session.getPlacementPosition(player);
        BlockVector3 finalPos = raised ? pos.add(0, radius.getY(), 0) : pos;
        fp.checkConfirmationRadius(() -> {
            int affected = editSession.makeSphere(finalPos, pattern, radius.getX(), radius.getY(), radius.getZ(), !hollow);
            player.findFreePosition();
            BBC.VISITOR_BLOCK.send(fp, affected);
        }, getArguments(context), (int) max, context);
    }

    @Command(
            aliases = {"forestgen"},
            usage = "[size] [tree-type] [density]",
            desc = "Generate a forest",
            min = 0,
            max = 3
    )
    @CommandPermissions("worldedit.generation.forest")
    @Logging(POSITION)
    @SuppressWarnings("deprecation")
    public void forestGen(Player player, LocalSession session, EditSession editSession, @Optional("10") int size, @Optional("tree") TreeType type, @Optional("5") @Range(min = 0, max = 100) double density) throws WorldEditException, ParameterException {
        density = density / 100;
        int affected = editSession.makeForest(session.getPlacementPosition(player), size, density, type);
        player.print(BBC.getPrefix() + affected + " trees created.");
    }

    @Command(
            aliases = {"pumpkins"},
            usage = "[size=10] [density=0.02]",
            desc = "Generate pumpkin patches",
            min = 0,
            max = 2
    )
    @CommandPermissions("worldedit.generation.pumpkins")
    @Logging(POSITION)
    public void pumpkins(Player player, LocalSession session, EditSession editSession, @Optional("10") int apothem, @Optional("0.02") double density) throws WorldEditException, ParameterException {
        int affected = editSession.makePumpkinPatches(session.getPlacementPosition(player), apothem, density);
        BBC.COMMAND_PUMPKIN.send(player, affected);
    }

    @Command(
            aliases = {"/hpyramid"},
            usage = "<pattern> <size>",
            desc = "Generate a hollow pyramid",
            min = 2,
            max = 2
    )
    @CommandPermissions("worldedit.generation.pyramid")
    @Logging(PLACEMENT)
    public void hollowPyramid(FawePlayer fp, Player player, LocalSession session, EditSession editSession, Pattern pattern, @Range(min = 1) int size, CommandContext context) throws WorldEditException, ParameterException {
        pyramid(fp, player, session, editSession, pattern, size, true, context);
    }

    @Command(
            aliases = {"/pyramid"},
            usage = "<pattern> <size>",
            flags = "h",
            desc = "Generate a filled pyramid",
            min = 2,
            max = 2
    )
    @CommandPermissions("worldedit.generation.pyramid")
    @Logging(PLACEMENT)
    public void pyramid(FawePlayer fp, Player player, LocalSession session, EditSession editSession, Pattern pattern, @Range(min = 1) int size, @Switch('h') boolean hollow, CommandContext context) throws WorldEditException, ParameterException {
        BlockVector3 pos = session.getPlacementPosition(player);
        worldEdit.checkMaxRadius(size);
        fp.checkConfirmationRadius(() -> {
            int affected = editSession.makePyramid(pos, pattern, size, !hollow);
            player.findFreePosition();
            BBC.VISITOR_BLOCK.send(fp, affected);
        }, getArguments(context), size, context);
    }
    

    @Command(
            aliases = {"/generate", "/gen", "/g"},
            usage = "<pattern> <expression>",
            desc = "Generates a shape according to a formula.",
            help =
                    "Generates a shape according to a formula that is expected to\n" +
                            "return positive numbers (true) if the point is inside the shape\n" +
                            "Optionally set type/data to the desired block.\n" +
                            "Flags:\n" +
                            "  -h to generate a hollow shape\n" +
                            "  -r to use raw minecraft coordinates\n" +
                            "  -o is like -r, except offset from placement.\n" +
                            "  -c is like -r, except offset selection center.\n" +
                            "If neither -r nor -o is given, the selection is mapped to -1..1\n" +
                            "See also tinyurl.com/wesyntax.",
            flags = "hroc",
            min = 2,
            max = -1
    )
    @CommandPermissions("worldedit.generation.shape")
    @Logging(ALL)
    public void generate(FawePlayer fp, Player player, LocalSession session, EditSession editSession,
                         @Selection Region region,
                         Pattern pattern,
                         @Text String expression,
                         @Switch('h') boolean hollow,
                         @Switch('r') boolean useRawCoords,
                         @Switch('o') boolean offset,
                         @Switch('c') boolean offsetCenter, CommandContext context) throws WorldEditException {

        final Vector3 zero;
        Vector3 unit;

        if (useRawCoords) {
            zero = Vector3.ZERO;
            unit = Vector3.ONE;
        } else if (offset) {
            zero = session.getPlacementPosition(player).toVector3();
            unit = Vector3.ONE;
        } else if (offsetCenter) {
            final Vector3 min = region.getMinimumPoint().toVector3();
            final Vector3 max = region.getMaximumPoint().toVector3();

            zero = max.add(min).multiply(0.5);
            unit = Vector3.ONE;
        } else {
            final Vector3 min = region.getMinimumPoint().toVector3();
            final Vector3 max = region.getMaximumPoint().toVector3();

            zero = max.add(min).multiply(0.5);
            unit = max.subtract(zero);

            if (unit.getX() == 0) unit = unit.withX(1.0);
            if (unit.getY() == 0) unit = unit.withY(1.0);
            if (unit.getZ() == 0) unit = unit.withZ(1.0);
        }
        
        final Vector3 unit1 = unit;

        fp.checkConfirmationRegion(() -> {
            try {
                final int affected = editSession.makeShape(region, zero, unit1, pattern, expression, hollow);
                player.findFreePosition();
                BBC.VISITOR_BLOCK.send(fp, affected);
            } catch (ExpressionException e) {
                fp.sendMessage(BBC.getPrefix() + e.getMessage());
            }
        }, getArguments(context), region, context);
    }

    @Command(
            aliases = {"/generatebiome", "/genbiome", "/gb"},
            usage = "<biome> <expression>",
            desc = "Sets biome according to a formula.",
            help =
                    "Generates a shape according to a formula that is expected to\n" +
                            "return positive numbers (true) if the point is inside the shape\n" +
                            "Sets the biome of blocks in that shape.\n" +
                            "Flags:\n" +
                            "  -h to generate a hollow shape\n" +
                            "  -r to use raw minecraft coordinates\n" +
                            "  -o is like -r, except offset from placement.\n" +
                            "  -c is like -r, except offset selection center.\n" +
                            "If neither -r nor -o is given, the selection is mapped to -1..1\n" +
                            "See also tinyurl.com/wesyntax.",
            flags = "hroc",
            min = 2,
            max = -1
    )
    @CommandPermissions("worldedit.generation.shape.biome")
    @Logging(ALL)
    public void generateBiome(FawePlayer fp, Player player, LocalSession session, EditSession editSession,
                              @Selection Region region,
                              BiomeType target,
                              @Text String expression,
                              @Switch('h') boolean hollow,
                              @Switch('r') boolean useRawCoords,
                              @Switch('o') boolean offset,
                              @Switch('c') boolean offsetCenter,
                              CommandContext context) throws WorldEditException {
        final Vector3 zero;
        Vector3 unit;

        if (useRawCoords) {
            zero = Vector3.ZERO;
            unit = Vector3.ONE;
        } else if (offset) {
            zero = session.getPlacementPosition(player).toVector3();
            unit = Vector3.ONE;
        } else if (offsetCenter) {
            final Vector3 min = region.getMinimumPoint().toVector3();
            final Vector3 max = region.getMaximumPoint().toVector3();

            zero = max.add(min).multiply(0.5);
            unit = Vector3.ONE;
        } else {
            final Vector3 min = region.getMinimumPoint().toVector3();
            final Vector3 max = region.getMaximumPoint().toVector3();

            zero = max.add(min).multiply(0.5);
            unit = max.subtract(zero);

            if (unit.getX() == 0) unit = unit.withX(1.0);
            if (unit.getY() == 0) unit = unit.withY(1.0);
            if (unit.getZ() == 0) unit = unit.withZ(1.0);
        }
        
        final Vector3 unit1 = unit;
        
        fp.checkConfirmationRegion(() -> {
            try {
                final int affected = editSession.makeBiomeShape(region, zero, unit1, target, expression, hollow);
                player.findFreePosition();
                BBC.VISITOR_FLAT.send(fp, affected);
            } catch (ExpressionException e) {
                fp.sendMessage(BBC.getPrefix() + e.getMessage());
            }
        }, getArguments(context), region, context);
    }
}
