package com.boydti.fawe.regions.general.plot;

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
import com.plotsquared.general.commands.Command;
import com.plotsquared.general.commands.CommandDeclaration;
import com.sk89q.worldedit.WorldEdit;

@CommandDeclaration(
        command = "cfi",
        permission = "plots.createfromimage",
        aliases = {"createfromheightmap", "createfromimage", "cfhm"},
        category = CommandCategory.APPEARANCE,
        requiredType = RequiredType.NONE,
        description = "Generate a world from an image heightmap: [More info](https://goo.gl/friFbV)",
        usage = "/plots cfi [url or dimensions]"
)
public class CFIRedirect extends Command {
    private final WorldEdit we;

    public CFIRedirect() {
        super(MainCommand.getInstance(), true);
        this.we = WorldEdit.getInstance();
    }

    @Override
    public void execute(final PlotPlayer player, String[] args, RunnableVal3<Command, Runnable, Runnable> confirm, RunnableVal2<Command, CommandResult> whenDone) throws CommandException {
        checkTrue(args.length >= 1, C.COMMAND_SYNTAX, getUsage());
        final Plot plot = check(player.getCurrentPlot(), C.NOT_IN_PLOT);
        checkTrue(plot.isOwner(player.getUUID()), C.NOW_OWNER);
        checkTrue(plot.getRunning() == 0, C.WAIT_FOR_TIMER);
        final PlotArea area = plot.getArea();
        if (area instanceof SinglePlotArea) {
            player.sendMessage("The command has been changed to: //cfi");
        } else {
            player.sendMessage("Must have the `worlds` component enabled in the PlotSquared config.yml");
            return;
        }
    }
}