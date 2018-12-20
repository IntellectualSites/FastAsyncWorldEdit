package com.boydti.fawe.regions.general.plot;

import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.util.StringMan;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.wrappers.FakePlayer;
import com.intellectualcrafters.plot.commands.CommandCategory;
import com.intellectualcrafters.plot.commands.MainCommand;
import com.intellectualcrafters.plot.commands.RequiredType;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotArea;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.object.RunnableVal2;
import com.intellectualcrafters.plot.object.RunnableVal3;
import com.intellectualcrafters.plot.object.worlds.SinglePlotArea;
import com.intellectualcrafters.plot.util.SetupUtils;
import com.plotsquared.general.commands.Command;
import com.plotsquared.general.commands.CommandDeclaration;
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.extension.platform.CommandManager;

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
        checkTrue(args.length >= 1, C.COMMAND_SYNTAX, getUsage());
        final Plot plot = check(player.getCurrentPlot(), C.NOT_IN_PLOT);
        checkTrue(plot.isOwner(player.getUUID()), C.NOW_OWNER);
        checkTrue(plot.getRunning() == 0, C.WAIT_FOR_TIMER);
        final PlotArea area = plot.getArea();
        if (area instanceof SinglePlotArea) {
            plot.addRunning();
            FawePlayer<Object> fp = FawePlayer.wrap(player.getName());
            C.TASK_START.send(player);
            TaskManager.IMP.async(new Runnable() {
                @Override
                public void run() {
                    fp.runAction(new Runnable() {
                        @Override
                        public void run() {
                            String worldName = plot.getWorldName();
                            TaskManager.IMP.sync(new RunnableVal<Object>() {
                                @Override
                                public void run(Object value) {
                                    SetupUtils.manager.unload(worldName, true);
                                }
                            });
                            FakePlayer actor = FakePlayer.getConsole();
                            String cmd = "/replaceallpattern " + worldName + " " + StringMan.join(args, " ");
                            CommandEvent event = new CommandEvent(actor, cmd);
                            CommandManager.getInstance().handleCommandOnCurrentThread(event);
                            TaskManager.IMP.sync(new RunnableVal<Object>() {
                                @Override
                                public void run(Object value) {
                                    plot.teleportPlayer(player);
                                }
                            });
                            plot.removeRunning();
                        }
                    }, true, false);
                }
            });
        } else {
            player.sendMessage("Must have the `worlds` component enabled in the PlotSquared config.yml");
            return;
        }
    }
}