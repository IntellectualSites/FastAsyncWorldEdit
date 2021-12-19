package com.fastasyncworldedit.bukkit.regions.plotsquared;

import com.fastasyncworldedit.core.util.TaskManager;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.command.CommandCategory;
import com.plotsquared.core.command.CommandDeclaration;
import com.plotsquared.core.command.RequiredType;
import com.plotsquared.core.command.SubCommand;
import com.plotsquared.core.configuration.caption.Placeholders;
import com.plotsquared.core.configuration.caption.StaticCaption;
import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.player.PlotPlayer;

@CommandDeclaration(command = "trimchunks",
        permission = "plots.admin",
        description = "Delete unmodified portions of your plotworld",
        requiredType = RequiredType.PLAYER,
        category = CommandCategory.ADMINISTRATION)
public class FaweTrim extends SubCommand {

    private boolean ran = false;

    @Override
    public boolean onCommand(final PlotPlayer<?> plotPlayer, final String[] strings) {
        if (ran) {
            plotPlayer.sendMessage(TranslatableCaption.miniMessage("error.task_in_process"));
            return false;
        }
        if (strings.length != 2) {
            plotPlayer.sendMessage(StaticCaption
                    .miniMessage("First make a backup of your world called <world-copy> then stand in the middle of an empty plot"));
            plotPlayer.sendMessage(StaticCaption.miniMessage("use /plot trimall <world> <boolean-delete-unowned>"));
            return false;
        }
        if (!PlotSquared.platform().worldUtil().isWorld(strings[0])) {
            plotPlayer.sendMessage(TranslatableCaption.miniMessage("errors.not_valid_plot_world"), Placeholders.miniMessage("value", strings[0]));
            return false;
        }
        ran = true;
        TaskManager.IMP.async(() -> {
            try {
                // TODO NOT IMPLEMENTED
                //PlotTrim trim = new PlotTrim(plotPlayer, plotPlayer.getPlotAreaAbs(), strings[0], Boolean.parseBoolean(strings[1]));
                //Location loc = plotPlayer.getLocation();
                //trim.setChunk(loc.getX() >> 4, loc.getZ() >> 4);
                //trim.run();
                //plotPlayer.sendMessage("Done!");
            } catch (Throwable e) {
                e.printStackTrace();
            }
            ran = false;
        });
        return true;
    }

}
