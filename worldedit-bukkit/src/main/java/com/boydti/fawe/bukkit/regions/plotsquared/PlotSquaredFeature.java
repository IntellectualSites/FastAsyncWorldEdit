package com.boydti.fawe.bukkit.regions.plotsquared;

import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.regions.FaweMask;
import com.boydti.fawe.regions.FaweMaskManager;
import com.boydti.fawe.regions.general.RegionFilter;
import com.github.intellectualsites.plotsquared.plot.util.UUIDHandler;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.command.MainCommand;
import com.plotsquared.core.configuration.Settings;
import com.plotsquared.core.database.DBFunc;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.flag.implementations.DoneFlag;
import com.plotsquared.core.plot.flag.implementations.NoWorldeditFlag;
import com.plotsquared.core.util.RegionManager;
import com.plotsquared.core.util.SchematicHandler;
import com.plotsquared.core.util.WEManager;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionIntersection;
import com.sk89q.worldedit.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlotSquaredFeature extends FaweMaskManager {

    private static final Logger log = LoggerFactory.getLogger(PlotSquaredFeature.class);

    public PlotSquaredFeature() {
        super("PlotSquared");
        log.debug("Optimizing PlotSquared");
        if (com.boydti.fawe.config.Settings.IMP.ENABLED_COMPONENTS.PLOTSQUARED_HOOK) {
            Settings.Enabled_Components.WORLDEDIT_RESTRICTIONS = false;
            try {
                setupBlockQueue();
                setupSchematicHandler();
                setupRegionManager();
            } catch (Throwable ignored) {
                log.debug("Please update PlotSquared: https://www.spigotmc.org/resources/plotsquared-v5.77506/");
            }
            if (Settings.PLATFORM.toLowerCase(Locale.ROOT).startsWith("bukkit")) {
                new FaweTrim();
            }
            if (MainCommand.getInstance().getCommand("generatebiome") == null) {
                new PlotSetBiome();
            }
        }
        // TODO: revisit this later on
        /*
        try {
            if (Settings.Enabled_Components.WORLDS) {
                new ReplaceAll();
            }
        } catch (Throwable e) {
            log.debug("You need to update PlotSquared to access the CFI and REPLACEALL commands");
        }
        */
    }

    public static String getName(UUID uuid) {
        return UUIDHandler.getName(uuid);
    }

    private void setupBlockQueue() throws RuntimeException {
        // If it's going to fail, throw an error now rather than later
        //QueueProvider provider = QueueProvider.of(FaweLocalBlockQueue.class, null);
        //GlobalBlockQueue.IMP.setProvider(provider);
        //HybridPlotManager.REGENERATIVE_CLEAR = false;
        //log.debug(" - QueueProvider: " + FaweLocalBlockQueue.class);
        //log.debug(" - HybridPlotManager.REGENERATIVE_CLEAR: " + HybridPlotManager.REGENERATIVE_CLEAR);
    }

    private void setupRegionManager() throws RuntimeException {
        RegionManager.manager = new FaweRegionManager(RegionManager.manager);
        log.debug(" - RegionManager: " + RegionManager.manager);
    }

    private void setupSchematicHandler() throws RuntimeException {
        SchematicHandler.manager = new FaweSchematicHandler();
        log.debug(" - SchematicHandler: " + SchematicHandler.manager);
    }

    public boolean isAllowed(Player player, Plot plot, MaskType type) {
        if (plot == null) {
            return false;
        }
        UUID uid = player.getUniqueId();
        return !plot.getFlag(NoWorldeditFlag.class) && (plot.isOwner(uid) || type == MaskType.MEMBER && (plot.getTrusted().contains(uid) || plot
            .getTrusted().contains(DBFunc.EVERYONE) || (plot.getMembers().contains(uid) || plot.getMembers().contains(DBFunc.EVERYONE)) && player
            .hasPermission("fawe.plotsquared.member")) || player.hasPermission("fawe.plotsquared.admin"));
    }

    @Override
    public FaweMask getMask(Player player, MaskType type) {
        final PlotPlayer pp = PlotPlayer.wrap(player.getUniqueId());
        if (pp == null) {
            return null;
        }
        final Set<CuboidRegion> regions;
        Plot plot = pp.getCurrentPlot();
        if (isAllowed(player, plot, type)) {
            regions = plot.getRegions();
        } else {
            plot = null;
            regions = WEManager.getMask(pp);
            if (regions.size() == 1) {
                CuboidRegion region = regions.iterator().next();
                if (region.getMinimumPoint().getX() == Integer.MIN_VALUE && region.getMaximumPoint().getX() == Integer.MAX_VALUE) {
                    regions.clear();
                }
            }
        }
        if (regions.isEmpty()) {
            return null;
        }
        PlotArea area = pp.getApplicablePlotArea();
        final Plot finalPlot = plot;
        if (Settings.Done.RESTRICT_BUILDING && DoneFlag.isDone(finalPlot) || regions.isEmpty()) {
            return null;
        }

        Region maskedRegion;
        if (regions.size() == 1) {
            int min = area != null ? area.getMinBuildHeight() : 0;
            int max = area != null ? Math.min(255, area.getMaxBuildHeight()) : 255;

            final CuboidRegion region = regions.iterator().next();
            final BlockVector3 pos1 = BlockVector3.at(region.getMinimumX(), min, region.getMinimumZ());
            final BlockVector3 pos2 = BlockVector3.at(region.getMaximumX(), max, region.getMaximumZ());
            maskedRegion = new CuboidRegion(pos1, pos2);
        } else {
            World world = FaweAPI.getWorld(area.getWorldName());
            List<Region> weRegions = regions.stream().map(
                r -> new CuboidRegion(world, BlockVector3.at(r.getMinimumX(), r.getMinimumY(), r.getMinimumZ()),
                    BlockVector3.at(r.getMaximumX(), r.getMaximumY(), r.getMaximumZ()))).collect(Collectors.toList());
            maskedRegion = new RegionIntersection(world, weRegions);
        }

        return new FaweMask(maskedRegion) {
            @Override
            public boolean isValid(Player player, MaskType type) {
                if (Settings.Done.RESTRICT_BUILDING && DoneFlag.isDone(finalPlot)) {
                    return false;
                }
                return isAllowed(player, finalPlot, type);
            }
        };
    }

    @Override
    public RegionFilter getFilter(String world) {
        PlotArea area = PlotSquared.get().getPlotArea(world, null);
        if (area != null) {
            return new PlotRegionFilter(area);
        }
        return null;
    }
}
