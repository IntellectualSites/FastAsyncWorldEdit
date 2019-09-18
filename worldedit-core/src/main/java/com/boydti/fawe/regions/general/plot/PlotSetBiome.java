package com.boydti.fawe.regions.general.plot;

import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.util.EditSessionBuilder;
import com.boydti.fawe.util.TaskManager;
import com.github.intellectualsites.plotsquared.commands.Command;
import com.github.intellectualsites.plotsquared.commands.CommandDeclaration;
import com.github.intellectualsites.plotsquared.plot.commands.CommandCategory;
import com.github.intellectualsites.plotsquared.plot.commands.MainCommand;
import com.github.intellectualsites.plotsquared.plot.commands.RequiredType;
import com.github.intellectualsites.plotsquared.plot.config.Captions;
import com.github.intellectualsites.plotsquared.plot.object.Plot;
import com.github.intellectualsites.plotsquared.plot.object.PlotPlayer;
import com.github.intellectualsites.plotsquared.plot.object.RegionWrapper;
import com.github.intellectualsites.plotsquared.plot.object.RunnableVal2;
import com.github.intellectualsites.plotsquared.plot.object.RunnableVal3;
import com.github.intellectualsites.plotsquared.plot.util.MainUtil;
import com.github.intellectualsites.plotsquared.plot.util.Permissions;
import com.github.intellectualsites.plotsquared.plot.util.StringMan;
import com.github.intellectualsites.plotsquared.plot.util.WorldUtil;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.biome.Biomes;
import com.sk89q.worldedit.world.registry.BiomeRegistry;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

@CommandDeclaration(
        command = "generatebiome",
        permission = "plots.generatebiome",
        category = CommandCategory.APPEARANCE,
        requiredType = RequiredType.NONE,
        description = "Generate a biome in your plot",
        aliases = {"bg", "gb"},
        usage = "/plots generatebiome <biome>"
)
public class PlotSetBiome extends Command {
    public PlotSetBiome() {
        super(MainCommand.getInstance(), true);
    }

    @Override
    public CompletableFuture<Boolean> execute(final PlotPlayer player, String[] args, RunnableVal3<Command, Runnable, Runnable> confirm, RunnableVal2<Command, CommandResult> whenDone) throws CommandException {
        final Plot plot = check(player.getCurrentPlot(), Captions.NOT_IN_PLOT);
        checkTrue(plot.isOwner(player.getUUID()) || Permissions.hasPermission(player, "plots.admin.command.generatebiome"), Captions.NO_PLOT_PERMS);
        if (plot.getRunning() != 0) {
            Captions.WAIT_FOR_TIMER.send(player);
            return null;
        }
        checkTrue(args.length == 1, Captions.COMMAND_SYNTAX, getUsage());
        final HashSet<RegionWrapper> regions = plot.getRegions();
        BiomeRegistry biomeRegistry = WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.GAME_HOOKS).getRegistries().getBiomeRegistry();
        Collection<BiomeType> knownBiomes = BiomeTypes.values();
        final BiomeType biome = Biomes.findBiomeByName(knownBiomes, args[0], biomeRegistry);
        if (biome == null) {
            String biomes = StringMan.join(WorldUtil.IMP.getBiomeList(), Captions.BLOCK_LIST_SEPARATOR.getTranslated());
            Captions.NEED_BIOME.send(player);
            MainUtil.sendMessage(player, Captions.SUBCOMMAND_SET_OPTIONS_HEADER.getTranslated() + biomes);
            return null;
        }
        confirm.run(this, new Runnable() {
            @Override
            public void run() {
                if (plot.getRunning() != 0) {
                    Captions.WAIT_FOR_TIMER.send(player);
                    return;
                }
                plot.addRunning();
                TaskManager.IMP.async(new Runnable() {
                    @Override
                    public void run() {
                        EditSession session = new EditSessionBuilder(plot.getArea().worldname)
                                .autoQueue(false)
                                .checkMemory(false)
                                .allowedRegionsEverywhere()
                                .player(FawePlayer.wrap(player.getName()))
                                .limitUnlimited()
                                .build();
                        long seed = ThreadLocalRandom.current().nextLong();
                        for (RegionWrapper region : regions) {
                            CuboidRegion cuboid = new CuboidRegion(BlockVector3.at(region.minX, 0, region.minZ), BlockVector3.at(region.maxX, 256, region.maxZ));
                            session.regenerate(cuboid, biome, seed);
                        }
                        session.flushQueue();
                        plot.removeRunning();
                    }
                });
            }
        }, null);

        return CompletableFuture.completedFuture(true);
    }
}
