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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.sk89q.worldedit.command.util.Logging.LogMode.ALL;
import static com.sk89q.worldedit.command.util.Logging.LogMode.PLACEMENT;
import static com.sk89q.worldedit.command.util.Logging.LogMode.POSITION;
import static com.sk89q.worldedit.internal.command.CommandUtil.checkCommandArgument;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.jnbt.anvil.generator.CavesGen;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.TextureUtil;
import com.boydti.fawe.util.image.ImageUtil;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import com.sk89q.worldedit.command.util.Logging;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.RegionVisitor;
import com.sk89q.worldedit.internal.annotation.Range;
import com.sk89q.worldedit.internal.annotation.Selection;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.TreeGenerator.TreeType;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockType;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.Switch;
import org.enginehub.piston.inject.InjectedValueAccess;

/**
 * Commands for the generation of shapes and other objects.
 */
@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class GenerationCommands {

    private final WorldEdit worldEdit;

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public GenerationCommands(WorldEdit worldEdit) {
        checkNotNull(worldEdit);
        this.worldEdit = worldEdit;
    }

    @Command(
            name = "/caves",
            desc = "Generates a cave network"
    )
    @CommandPermissions("worldedit.generation.caves")
    @Logging(PLACEMENT)
    public void caves(Player player, LocalSession session, EditSession editSession, @Selection Region region,
                      @Arg(name = "size", desc = "TODO", def = "8") int sizeOpt,
                      @Arg(desc = "TODO", def = "40") int frequency,
                      @Arg(desc = "TODO", def = "7") int rarityOpt,
                      @Arg(desc = "TODO", def = "8") int minYopt,
                      @Arg(desc = "TODO", def = "127") int maxYopt,
                      @Arg(desc = "TODO", def = "1") int systemFrequency,
                      @Arg(desc = "TODO", def = "25") int individualRarityOpt,
                      @Arg(desc = "TODO", def = "0") int pocketChance,
                      @Arg(desc = "TODO", def = "0") int pocketMin,
                      @Arg(desc = "TODO", def = "3") int pocketMax, InjectedValueAccess context) throws WorldEditException {
        player.checkConfirmationRegion(() -> {
            CavesGen gen = new CavesGen(sizeOpt, frequency, rarityOpt, minYopt, maxYopt, systemFrequency, individualRarityOpt, pocketChance, pocketMin, pocketMax);
            editSession.generate(region, gen);
            BBC.VISITOR_BLOCK.send(player, editSession.getBlockChangeCount());
        }, "/caves", region, context);
    }


    @Command(
            name = "/ores",
            desc = "Generates ores"
    )
    @CommandPermissions("worldedit.generation.ore")
    @Logging(PLACEMENT)
    public void ores(Player fp, LocalSession session, EditSession editSession, @Selection Region region, Mask mask, InjectedValueAccess context) throws WorldEditException {
        fp.checkConfirmationRegion(() -> {
            editSession.addOres(region, mask);
            BBC.VISITOR_BLOCK.send(fp, editSession.getBlockChangeCount());
        }, "/ores", region, context);
    }

    @Command(
            name = "/img",
            desc = "Generate an image"
    )
    @CommandPermissions("worldedit.generation.image")
    @Logging(PLACEMENT)
    public void image(Player player, LocalSession session, EditSession editSession, String argStr, @Arg(name = "randomize", desc = "boolean", def = "true") boolean randomize,
                      @Arg(desc = "TODO", def = "100") int threshold, @Arg(name = "dimensions", desc = "BlockVector2", def = "") BlockVector2 dimensions) throws WorldEditException, IOException {
        TextureUtil tu = Fawe.get().getCachedTextureUtil(randomize, 0, threshold);
        URL url = new URL(argStr);
        if (!url.getHost().equalsIgnoreCase("i.imgur.com") && !url.getHost().equalsIgnoreCase("empcraft.com")) {
            throw new IOException("Only i.imgur.com or empcraft.com/ui links are allowed!");
        }
        BufferedImage image = MainUtil.readImage(url);
        if (dimensions != null) {
            image = ImageUtil.getScaledInstance(image, dimensions.getBlockX(), dimensions.getBlockZ(), RenderingHints.VALUE_INTERPOLATION_BILINEAR, false);
        }

      BlockVector3 pos1 = player.getLocation().toBlockPoint();
      BlockVector3 pos2 = pos1.add(image.getWidth() - 1, 0, image.getHeight() - 1);
        CuboidRegion region = new CuboidRegion(pos1, pos2);
        int[] count = new int[1];
        final BufferedImage finalImage = image;
        RegionVisitor visitor = new RegionVisitor(region, pos -> {
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
        });
        Operations.completeBlindly(visitor);
        BBC.VISITOR_BLOCK.send(player, editSession.getBlockChangeCount());
    }

    @Command(
            name = "/ore",
            desc = "Generates ores"
    )
    @CommandPermissions("worldedit.generation.ore")
    @Logging(PLACEMENT)
    public void ore(Player fp, LocalSession session, EditSession editSession, @Selection Region region, Mask mask, Pattern material, @Arg(name="size", desc="Ore vein size") @Range(min = 0) int size, int freq, @Range(min = 0, max = 100) int rarity, @Range(min = 0, max = 255) int minY, @Range(min = 0, max = 255) int maxY, InjectedValueAccess context) throws WorldEditException {
        fp.checkConfirmationRegion(() -> {
            editSession.addOre(region, mask, material, size, freq, rarity, minY, maxY);
            BBC.VISITOR_BLOCK.send(fp, editSession.getBlockChangeCount());
        }, "/ore", region, context);
    }

    @Command(
        name = "/hcyl",
        desc = "Generates a hollow cylinder."
    )
    @CommandPermissions("worldedit.generation.cylinder")
    @Logging(PLACEMENT)
    public void hcyl(Actor actor, LocalSession session, EditSession editSession,
                    @Arg(desc = "The pattern of blocks to generate")
                        Pattern pattern,
                        BlockVector2 radius,
                    @Arg(desc = "The height of the cylinder", def = "1")
                                int height,
                    @Range(min = 1) @Arg(name = "thickness", desc = "double", def = "1") double thickness, InjectedValueAccess context) throws WorldEditException {
        double max = MathMan.max(radius.getBlockX(), radius.getBlockZ());
        worldEdit.checkMaxRadius(max);
        BlockVector3 pos = session.getPlacementPosition(actor);
        actor.checkConfirmationRadius(() -> {
            int affected = editSession.makeHollowCylinder(pos, pattern, radius.getX(), radius.getZ(), Math.min(256, height), thickness - 1);
            BBC.VISITOR_BLOCK.send(actor, affected);
        }, "/hcyl", (int) max, context);
    }

    @Command(
        name = "/cyl",
        desc = "Generates a cylinder."
    )
    @CommandPermissions("worldedit.generation.cylinder")
    @Logging(PLACEMENT)
    public void cyl(Actor actor, LocalSession session, EditSession editSession,
                   @Arg(desc = "The pattern of blocks to generate")
                       Pattern pattern,
        BlockVector2 radius,
                   @Arg(desc = "The height of the cylinder", def = "1")
                       int height,
                   @Switch(name = 'h', desc = "Make a hollow cylinder")
                       boolean hollow, InjectedValueAccess context) throws WorldEditException {
        double max = Math.max(radius.getBlockX(), radius.getBlockZ());
        worldEdit.checkMaxRadius(max);
        BlockVector3 pos = session.getPlacementPosition(actor);
        actor.checkConfirmationRadius(() -> {
            int affected = editSession.makeCylinder(pos, pattern, radius.getX(), radius.getZ(), Math.min(256, height), !hollow);
            BBC.VISITOR_BLOCK.send(actor, affected);
        }, "/cyl", (int) max, context);
    }

    @Command(
        name = "/hsphere",
        desc = "Generates a hollow sphere."
    )
    @CommandPermissions("worldedit.generation.sphere")
    @Logging(PLACEMENT)
    public void hsphere(Actor actor, LocalSession session, EditSession editSession,
                       @Arg(desc = "The pattern of blocks to generate")
                           Pattern pattern,
                       @Arg(desc = "The radii of the sphere. Order is N/S, U/D, E/W") BlockVector3 radii,
                       @Switch(name = 'r', desc = "Raise the bottom of the sphere to the placement position")
                           boolean raised,
                        InjectedValueAccess context) throws WorldEditException {
       sphere(actor, session, editSession, pattern, radii, raised, true, context);
    }

    @Command(
        name = "/sphere",
        desc = "Generates a filled sphere."
    )
    @CommandPermissions("worldedit.generation.sphere")
    @Logging(PLACEMENT)
    public void sphere(Actor actor, LocalSession session, EditSession editSession,
        @Arg(desc = "The pattern of blocks to generate")
            Pattern pattern,
        @Arg(desc = "The radii of the sphere. Order is N/S, U/D, E/W")
            BlockVector3 radii,
        @Switch(name = 'r', desc = "Raise the bottom of the sphere to the placement position")
            boolean raised,
        @Switch(name = 'h', desc = "Make a hollow sphere")
            boolean hollow, InjectedValueAccess context) throws WorldEditException {
        double max = MathMan.max(radii.getBlockX(), radii.getBlockY(), radii.getBlockZ());
        worldEdit.checkMaxRadius(max);
        BlockVector3 pos = session.getPlacementPosition(actor);
        BlockVector3 finalPos = raised ? pos.add(0, radii.getY(), 0) : pos;
        actor.checkConfirmationRadius(() -> {
            int affected = editSession.makeSphere(finalPos, pattern, radii.getX(), radii.getY(), radii.getZ(), !hollow);
            if (actor instanceof Player) {
                ((Player) actor).findFreePosition();
            }
            BBC.VISITOR_BLOCK.send(actor, affected);
        }, "sphere", (int) max, context);
    }

    @Command(
        name = "forestgen",
        desc = "Generate a forest"
    )
    @CommandPermissions("worldedit.generation.forest")
    @Logging(POSITION)
    public int forestGen(Actor actor, LocalSession session, EditSession editSession,
                         @Arg(desc = "The size of the forest, in blocks", def = "10")
                             int size,
                         @Arg(desc = "The type of forest", def = "tree")
                             TreeType type,
                         @Arg(desc = "The density of the forest, between 0 and 100", def = "5")
                             double density) throws WorldEditException {
        checkCommandArgument(0 <= density && density <= 100, "Density must be between 0 and 100");
        worldEdit.checkMaxRadius(size);
        density /= 100;
        int affected = editSession.makeForest(session.getPlacementPosition(actor), size, density, type);
        actor.print(affected + " trees created.");
        return affected;
    }

    @Command(
        name = "pumpkins",
        desc = "Generate pumpkin patches"
    )
    @CommandPermissions("worldedit.generation.pumpkins")
    @Logging(POSITION)
    public int pumpkins(Actor actor, LocalSession session, EditSession editSession,
                        @Arg(desc = "The size of the patch", def = "10")
                            int size,
                        @Arg(desc = "//TODO", def = "10")
                            int apothem,
                        @Arg(desc = "//TODO ", def = "0.02")
                            double density) throws WorldEditException {
        checkCommandArgument(0 <= density && density <= 100, "Density must be between 0 and 100");
        worldEdit.checkMaxRadius(size);
        int affected = editSession.makePumpkinPatches(session.getPlacementPosition(actor), apothem, density);
        BBC.COMMAND_PUMPKIN.send(actor, affected);
        return affected;
    }

    @Command(
        name = "/hpyramid",
        desc = "Generate a hollow pyramid"
    )
    @CommandPermissions("worldedit.generation.pyramid")
    @Logging(PLACEMENT)
    public void hollowPyramid(Actor actor, LocalSession session, EditSession editSession,
                             @Arg(desc = "The pattern of blocks to set")
                                 Pattern pattern,
                             @Arg(desc = "The size of the pyramid")
                                 int size, InjectedValueAccess context) throws WorldEditException {
        pyramid(actor, session, editSession, pattern, size, true, context);
    }

    @Command(
        name = "/pyramid",
        desc = "Generate a filled pyramid"
    )
    @CommandPermissions("worldedit.generation.pyramid")
    @Logging(PLACEMENT)
    public void pyramid(Actor actor, LocalSession session, EditSession editSession,
                       @Arg(desc = "The pattern of blocks to set")
                           Pattern pattern,
                       @Arg(desc = "The size of the pyramid")
                           int size,
                       @Switch(name = 'h', desc = "Make a hollow pyramid")
                           boolean hollow,
                        InjectedValueAccess context) throws WorldEditException {
        worldEdit.checkMaxRadius(size);
        BlockVector3 pos = session.getPlacementPosition(actor);
        actor.checkConfirmationRadius(() -> {
            int affected = editSession.makePyramid(pos, pattern, size, !hollow);
            if (actor instanceof Player) {
                ((Player) actor).findFreePosition();
            }
            BBC.VISITOR_BLOCK.send(actor, affected);
        }, "/pyramid", size, context);
    }

    @Command(
        name = "/generate",
        aliases = { "/gen", "/g" },
        desc = "Generates a shape according to a formula.",
        descFooter = "See also https://tinyurl.com/weexpr."
    )
    @CommandPermissions("worldedit.generation.shape")
    @Logging(ALL)
    public void generate(Actor actor, LocalSession session, EditSession editSession,
                        @Selection Region region,
                        @Arg(desc = "The pattern of blocks to set")
                            Pattern pattern,
                        @Arg(desc = "Expression to test block placement locations and set block type", variable = true)
                            List<String> expression,
                        @Switch(name = 'h', desc = "Generate a hollow shape")
                            boolean hollow,
                        @Switch(name = 'r', desc = "Use the game's coordinate origin")
                            boolean useRawCoords,
                        @Switch(name = 'o', desc = "Use the placement's coordinate origin")
                            boolean offset,
                        @Switch(name = 'c', desc = "Use the selection's center as origin")
                            boolean offsetCenter,
                         InjectedValueAccess context) throws WorldEditException {

        final Vector3 zero;
        Vector3 unit;

        if (useRawCoords) {
            zero = Vector3.ZERO;
            unit = Vector3.ONE;
        } else if (offset) {
            zero = session.getPlacementPosition(actor).toVector3();
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

        actor.checkConfirmationRegion(() -> {
            try {
                final int affected = editSession.makeShape(region, zero, unit1, pattern, String.join(" ", expression), hollow, session.getTimeout());
                if (actor instanceof Player) {
                    ((Player) actor).findFreePosition();
                }
                BBC.VISITOR_BLOCK.send(actor, affected);
            } catch (ExpressionException e) {
                actor.printError(e.getMessage());
            }
        }, "/generate", region, context);
    }

    @Command(
        name = "/generatebiome",
        aliases = { "/genbiome", "/gb" },
        desc = "Sets biome according to a formula.",
        descFooter = "Formula must return positive numbers (true) if the point is inside the shape\n" +
                "Sets the biome of blocks in that shape.\n"
                +"See also https://tinyurl.com/weexpr."
    )
    @CommandPermissions("worldedit.generation.shape.biome")
    @Logging(ALL)
    public void generateBiome(Actor actor, LocalSession session, EditSession editSession,
                             @Selection Region region,
                             @Arg(desc = "The biome type to set")
                                 BiomeType target,
                             @Arg(desc = "Expression to test block placement locations and set biome type", variable = true)
                                 List<String> expression,
                             @Switch(name = 'h', desc = "Generate a hollow shape")
                                 boolean hollow,
                             @Switch(name = 'r', desc = "Use the game's coordinate origin")
                                 boolean useRawCoords,
                             @Switch(name = 'o', desc = "Use the placement's coordinate origin")
                                 boolean offset,
                             @Switch(name = 'c', desc = "Use the selection's center as origin")
                                 boolean offsetCenter,
                             InjectedValueAccess context) throws WorldEditException {
        final Vector3 zero;
        Vector3 unit;

        if (useRawCoords) {
            zero = Vector3.ZERO;
            unit = Vector3.ONE;
        } else if (offset) {
            zero = session.getPlacementPosition(actor).toVector3();
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
        actor.checkConfirmationRegion(() -> {
            try {
                final int affected = editSession.makeBiomeShape(region, zero, unit1, target, String.join(" ", expression), hollow, session.getTimeout());
                BBC.VISITOR_FLAT.send(actor, affected);
            } catch (ExpressionException e) {
                actor.printError(e.getMessage());
            }
        }, "/generatebiome", region, context);
    }

}
