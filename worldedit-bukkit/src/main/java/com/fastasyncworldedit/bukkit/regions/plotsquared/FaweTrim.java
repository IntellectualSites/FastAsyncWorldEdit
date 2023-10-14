package com.fastasyncworldedit.bukkit.regions.plotsquared;

import com.fastasyncworldedit.core.util.TaskManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.command.CommandCategory;
import com.plotsquared.core.command.CommandDeclaration;
import com.plotsquared.core.command.RequiredType;
import com.plotsquared.core.command.SubCommand;
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
    public boolean onCommand(final PlotPlayer plotPlayer, final String[] strings) {
        if (ran) {
            plotPlayer.sendMessage(TranslatableCaption.of("error.task_in_process"));
            return false;
        }
        if (strings.length != 2) {
            plotPlayer.sendMessage(StaticCaption
                    .of("First make a backup of your world called <world-copy> then stand in the middle of an empty plot"));
            plotPlayer.sendMessage(StaticCaption.of("use /plot trimall <world> <boolean-delete-unowned>"));
            return false;
        }
        if (!PlotSquared.platform().worldUtil().isWorld(strings[0])) {
            plotPlayer.sendMessage(TranslatableCaption.of("errors.not_valid_plot_world"), TagResolver.resolver("value", Tag.inserting(Component.text(strings[0]))));
            return false;
        }
        ran = true;
        TaskManager.taskManager().async(() -> {
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
