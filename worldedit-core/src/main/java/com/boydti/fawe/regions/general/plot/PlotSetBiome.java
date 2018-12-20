package com.boydti.fawe.regions.general.plot;

import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.PseudoRandom;
import com.boydti.fawe.util.EditSessionBuilder;
import com.boydti.fawe.util.TaskManager;
import com.intellectualcrafters.plot.commands.CommandCategory;
import com.intellectualcrafters.plot.commands.MainCommand;
import com.intellectualcrafters.plot.commands.RequiredType;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.object.RegionWrapper;
import com.intellectualcrafters.plot.object.RunnableVal2;
import com.intellectualcrafters.plot.object.RunnableVal3;
import com.intellectualcrafters.plot.util.MainUtil;
import com.intellectualcrafters.plot.util.Permissions;
import com.intellectualcrafters.plot.util.StringMan;
import com.intellectualcrafters.plot.util.WorldUtil;
import com.plotsquared.general.commands.Command;
import com.plotsquared.general.commands.CommandDeclaration;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.biome.Biomes;
import com.sk89q.worldedit.world.registry.BiomeRegistry;
import java.util.HashSet;
import java.util.List;

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
    public void execute(final PlotPlayer player, String[] args, RunnableVal3<Command, Runnable, Runnable> confirm, RunnableVal2<Command, CommandResult> whenDone) throws CommandException {
        final Plot plot = check(player.getCurrentPlot(), C.NOT_IN_PLOT);
        checkTrue(plot.isOwner(player.getUUID()) || Permissions.hasPermission(player, "plots.admin.command.generatebiome"), C.NO_PLOT_PERMS);
        if (plot.getRunning() != 0) {
            C.WAIT_FOR_TIMER.send(player);
            return;
        }
        checkTrue(args.length == 1, C.COMMAND_SYNTAX, getUsage());
        final HashSet<RegionWrapper> regions = plot.getRegions();
        BiomeRegistry biomeRegistry = WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.GAME_HOOKS).getRegistries().getBiomeRegistry();
        List<BaseBiome> knownBiomes = biomeRegistry.getBiomes();
        final BaseBiome biome = Biomes.findBiomeByName(knownBiomes, args[0], biomeRegistry);
        if (biome == null) {
            String biomes = StringMan.join(WorldUtil.IMP.getBiomeList(), C.BLOCK_LIST_SEPARATER.s());
            C.NEED_BIOME.send(player);
            MainUtil.sendMessage(player, C.SUBCOMMAND_SET_OPTIONS_HEADER.s() + biomes);
            return;
        }
        confirm.run(this, new Runnable() {
            @Override
            public void run() {
                if (plot.getRunning() != 0) {
                    C.WAIT_FOR_TIMER.send(player);
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
                        long seed = PseudoRandom.random.nextLong();
                        for (RegionWrapper region : regions) {
                            CuboidRegion cuboid = new CuboidRegion(new Vector(region.minX, 0, region.minZ), new Vector(region.maxX, 256, region.maxZ));
                            session.regenerate(cuboid, biome, seed);
                        }
                        session.flushQueue();
                        plot.removeRunning();
                    }
                });
            }
        }, null);

    }
}
