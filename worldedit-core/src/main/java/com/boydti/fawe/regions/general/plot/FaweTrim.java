package com.boydti.fawe.regions.general.plot;

import com.boydti.fawe.util.TaskManager;
import com.github.intellectualsites.plotsquared.commands.CommandDeclaration;
import com.github.intellectualsites.plotsquared.plot.commands.CommandCategory;
import com.github.intellectualsites.plotsquared.plot.commands.RequiredType;
import com.github.intellectualsites.plotsquared.plot.commands.SubCommand;
import com.github.intellectualsites.plotsquared.plot.config.Captions;
import com.github.intellectualsites.plotsquared.plot.object.Location;
import com.github.intellectualsites.plotsquared.plot.object.PlotPlayer;
import com.github.intellectualsites.plotsquared.plot.util.WorldUtil;

@CommandDeclaration(
        command = "trimchunks",
        permission = "plots.admin",
        description = "Delete unmodified portions of your plotworld",
        usage = "/plot trimchunks <world> <boolean-delete-unowned>",
        requiredType = RequiredType.PLAYER,
        category = CommandCategory.ADMINISTRATION)
public class FaweTrim extends SubCommand {

    private boolean ran = false;

    @Override
    public boolean onCommand(final PlotPlayer plotPlayer, final String[] strings) {
        if (ran) {
            plotPlayer.sendMessage("Already running!");
            return false;
        }
        if (strings.length != 2) {
            plotPlayer.sendMessage("First make a backup of your world called <world-copy> then stand in the middle of an empty plot");
            plotPlayer.sendMessage("use /plot trimall <world> <boolean-delete-unowned>");
            return false;
        }
        if (!WorldUtil.IMP.isWorld(strings[0])) {
            Captions.NOT_VALID_PLOT_WORLD.send(plotPlayer, strings[0]);
            return false;
        }
        ran = true;
        TaskManager.IMP.async(new Runnable() {
            @Override
            public void run() {
                try {
                    PlotTrim trim = new PlotTrim(plotPlayer, plotPlayer.getPlotAreaAbs(), strings[0], Boolean.parseBoolean(strings[1]));
                    Location loc = plotPlayer.getLocation();
                    trim.setChunk(loc.getX() >> 4, loc.getZ() >> 4);
                    trim.run();
                    plotPlayer.sendMessage("Done!");
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                ran = false;
            }
        });
        return true;
    }
}
