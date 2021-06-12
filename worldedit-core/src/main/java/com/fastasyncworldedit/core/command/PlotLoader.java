// TODO: Ping @MattBDev to reimplement 2020-02-04
//package com.boydti.fawe.command;
//
//import com.boydti.fawe.command.CFICommands.CFISettings;
//import com.boydti.fawe.object.RunnableVal;
//import com.boydti.fawe.util.TaskManager;
//import com.github.intellectualsites.plotsquared.plot.PlotSquared;
//import com.github.intellectualsites.plotsquared.plot.commands.Auto;
//import com.github.intellectualsites.plotsquared.plot.config.Captions;
//import com.github.intellectualsites.plotsquared.plot.config.Settings;
//import com.github.intellectualsites.plotsquared.plot.database.DBFunc;
//import com.github.intellectualsites.plotsquared.plot.object.Plot;
//import com.github.intellectualsites.plotsquared.plot.object.PlotArea;
//import com.github.intellectualsites.plotsquared.plot.object.PlotId;
//import com.github.intellectualsites.plotsquared.plot.object.PlotPlayer;
//import com.github.intellectualsites.plotsquared.plot.object.worlds.PlotAreaManager;
//import com.github.intellectualsites.plotsquared.plot.object.worlds.SinglePlotArea;
//import com.github.intellectualsites.plotsquared.plot.object.worlds.SinglePlotAreaManager;
//import com.sk89q.worldedit.extension.platform.Actor;
//import java.io.File;
//import java.io.IOException;
//import java.util.function.Function;
//
//public class PlotLoader {
//
//    @Deprecated
//    public static void autoClaimFromDatabase(PlotPlayer player, PlotArea area, PlotId start,
//        com.github.intellectualsites.plotsquared.plot.object.RunnableVal<Plot> whenDone) {
//        final Plot plot = area.getNextFreePlot(player, start);
//        if (plot == null) {
//            whenDone.run(null);
//            return;
//        }
//        whenDone.value = plot;
//        plot.owner = player.getUUID();
//        DBFunc.createPlotSafe(plot, whenDone,
//            () -> autoClaimFromDatabase(player, area, plot.getId(), whenDone));
//    }
//
//    public void load(Actor actor, CFISettings  settings, Function<File, Boolean> createTask) throws IOException {
//        PlotAreaManager manager = PlotSquared.get().getPlotAreaManager();
//        if (manager instanceof SinglePlotAreaManager) {
//            SinglePlotAreaManager sManager = (SinglePlotAreaManager) manager;
//            SinglePlotArea area = sManager.getArea();
//            PlotPlayer player = PlotPlayer.get(actor.getName());
//
//            actor.print("Claiming world");
//            Plot plot = TaskManager.IMP.sync(new RunnableVal<Plot>() {
//                @Override
//                public void run(Plot o) {
//                    int currentPlots = Settings.Limit.GLOBAL ? player.getPlotCount()
//                        : player.getPlotCount(area.worldname);
//                    int diff = player.getAllowedPlots() - currentPlots;
//                    if (diff < 1) {
//                        Captions.CANT_CLAIM_MORE_PLOTS_NUM.send(player, -diff);
//                        return;
//                    }
//
//                    if (area.getMeta("lastPlot") == null) {
//                        area.setMeta("lastPlot", new PlotId(0, 0));
//                    }
//                    PlotId lastId = (PlotId) area.getMeta("lastPlot");
//                    do {
//                        lastId = Auto.getNextPlotId(lastId, 1);
//                    } while (!area.canClaim(player, lastId, lastId));
//                    area.setMeta("lastPlot", lastId);
//                    this.value = area.getPlot(lastId);
//                    this.value.setOwner(player.getUUID());
//                }
//            });
//            if (plot != null) {
//
//                File folder = CFICommands.getFolder(plot.getWorldName());
//                Boolean result = createTask.apply(folder);
//                if (result == Boolean.TRUE) {
//                    TaskManager.IMP.sync(() -> plot.teleportPlayer(player));
//                }
//                return;
//            }
//        }
//        createTask.apply(null);
//    }
//}
