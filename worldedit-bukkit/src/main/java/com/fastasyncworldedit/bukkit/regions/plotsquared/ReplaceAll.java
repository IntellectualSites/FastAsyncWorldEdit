/*
package com.boydti.fawe.regions.general.plot;

import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.util.StringMan;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.wrappers.FakePlayer;
import com.github.intellectualsites.plotsquared.commands.Command;
import com.github.intellectualsites.plotsquared.commands.CommandDeclaration;
import com.github.intellectualsites.plotsquared.plot.commands.CommandCategory;
import com.github.intellectualsites.plotsquared.plot.commands.MainCommand;
import com.github.intellectualsites.plotsquared.plot.commands.RequiredType;
import com.github.intellectualsites.plotsquared.plot.config.Captions;
import com.github.intellectualsites.plotsquared.plot.object.Plot;
import com.github.intellectualsites.plotsquared.plot.object.PlotArea;
import com.github.intellectualsites.plotsquared.plot.object.PlotPlayer;
import com.github.intellectualsites.plotsquared.plot.object.RunnableVal2;
import com.github.intellectualsites.plotsquared.plot.object.RunnableVal3;
import com.github.intellectualsites.plotsquared.plot.object.worlds.SinglePlotArea;
import com.github.intellectualsites.plotsquared.plot.util.SetupUtils;
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.extension.platform.PlatformCommandManager;

@CommandDeclaration(
        command = "replaceall",
        permission = "plots.replaceall",
        category = CommandCategory.APPEARANCE,
        requiredType = RequiredType.NONE,
        description = "Replace all block in the plot",
        usage = "/plots replaceall <from> <to>"
)
public class ReplaceAll extends Command {
    public ReplaceAll() {
        super(MainCommand.getInstance(), true);
    }

    @Override
    public void execute(final PlotPlayer player, String[] args, RunnableVal3<Command, Runnable, Runnable> confirm, RunnableVal2<Command, CommandResult> whenDone) throws CommandException {
        checkTrue(args.length >= 1, Captions.COMMAND_SYNTAX, getUsage());
        final Plot plot = check(player.getCurrentPlot(), Captions.NOT_IN_PLOT);
        checkTrue(plot.isOwner(player.getUUID()), Captions.NOW_OWNER);
        checkTrue(plot.getRunning() == 0, Captions.WAIT_FOR_TIMER);
        final PlotArea area = plot.getArea();
        if (area instanceof SinglePlotArea) {
            plot.addRunning();
            FawePlayer<Object> fp = FawePlayer.wrap(player.getName());
            Captions.TASK_START.send(player);
            TaskManager.taskManager().async(() -> fp.runAction(() -> {
                String worldName = plot.getWorldName();
                TaskManager.taskManager().sync(new RunnableVal<Object>() {
                    @Override
                    public void run(Object value) {
                        SetupUtils.manager.unload(worldName, true);
                    }
                });
                FakePlayer actor = FakePlayer.getConsole();
                String cmd = "/replaceallpattern " + worldName + " " + StringMan.join(args, " ");
                CommandEvent event = new CommandEvent(actor, cmd);
                PlatformCommandManager.getInstance().handleCommandOnCurrentThread(event);
                TaskManager.taskManager().sync(new RunnableVal<Object>() {
                    @Override
                    public void run(Object value) {
                        plot.teleportPlayer(player);
                    }
                });
                plot.removeRunning();
            }, true, false));
        } else {
            player.sendMessage("Must have the `worlds` component enabled in the PlotSquared config.yml");
            return;
        }
    }
}
*/
