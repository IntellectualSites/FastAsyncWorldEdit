package com.boydti.fawe.regions.general.integrations.plotquared;

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
import com.sk89q.worldedit.WorldEdit;
import java.util.concurrent.CompletableFuture;

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
    public CompletableFuture<Boolean> execute(final PlotPlayer player, String[] args, RunnableVal3<Command, Runnable, Runnable> confirm, RunnableVal2<Command, CommandResult> whenDone) throws CommandException {
        checkTrue(args.length >= 1, Captions.COMMAND_SYNTAX, getUsage());
        final Plot plot = check(player.getCurrentPlot(), Captions.NOT_IN_PLOT);
        checkTrue(plot.isOwner(player.getUUID()), Captions.NOW_OWNER);
        checkTrue(plot.getRunning() == 0, Captions.WAIT_FOR_TIMER);
        final PlotArea area = plot.getArea();
        if (area instanceof SinglePlotArea) {
            player.sendMessage("The command has been changed to: //cfi");
        } else {
            player.sendMessage("Must have the `worlds` component enabled in the PlotSquared config.yml");
            return CompletableFuture.completedFuture(false);
        }
        return CompletableFuture.completedFuture(true);
    }
}
