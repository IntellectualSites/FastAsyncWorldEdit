/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.command;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.FaweVersion;
import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.limit.FaweLimit;
import com.fastasyncworldedit.core.util.UpdateNotification;
import com.intellectualsites.paster.IncendoPaster;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import com.sk89q.worldedit.command.util.HookMode;
import com.sk89q.worldedit.command.util.PrintCommandHelp;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.platform.ConfigurationLoadEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.event.ClickEvent;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.ArgFlag;
import org.enginehub.piston.annotation.param.Switch;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.zone.ZoneRulesException;
import java.util.List;
import java.util.Map;

@CommandContainer(superTypes = {CommandPermissionsConditionGenerator.Registration.class})
public class WorldEditCommands {

    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    private final WorldEdit we;

    public WorldEditCommands(WorldEdit we) {
        this.we = we;
    }

    @Command(
            name = "version",
            aliases = {"ver"},
            desc = "Get the FAWE version"
    )
    @CommandPermissions(queued = false)
    public void version(Actor actor) {
        //FAWE start - use own, minimized message that doesn't print "Platforms" and "Capabilities"
        FaweVersion fVer = Fawe.instance().getVersion();
        String fVerStr = fVer == null ? "unknown" : "-" + fVer.build;
        actor.print(TextComponent.of("FastAsyncWorldEdit" + fVerStr));
        actor.print(TextComponent.of("Authors: Empire92, MattBDev, IronApollo, dordsor21 and NotMyFault"));
        actor.print(TextComponent.of("Wiki: https://intellectualsites.github.io/fastasyncworldedit-documentation/")
                .clickEvent(ClickEvent.openUrl("https://intellectualsites.github.io/fastasyncworldedit-documentation/")));
        actor.print(TextComponent.of("Discord: https://discord.gg/intellectualsites")
                .clickEvent(ClickEvent.openUrl("https://discord.gg/intellectualsites")));
        UpdateNotification.doUpdateNotification(actor);
        //FAWE end
    }

    @Command(
            name = "reload",
            desc = "Reload configuration and translations"
    )
    @CommandPermissions("worldedit.reload")
    public void reload(Actor actor) {
        we.getPlatformManager().queryCapability(Capability.CONFIGURATION).reload();
        we.getEventBus().post(new ConfigurationLoadEvent(we
                .getPlatformManager()
                .queryCapability(Capability.CONFIGURATION)
                .getConfiguration()));
        //FAWE start
        Fawe.instance().setupConfigs();
        FaweLimit.MAX.CONFIRM_LARGE =
                Settings.settings().LIMITS.get("default").CONFIRM_LARGE || Settings.settings().GENERAL.LIMIT_UNLIMITED_CONFIRMS;
        //FAWE end
        actor.print(Caption.of("worldedit.reload.config"));
    }

    //FAWE start
    @Command(
            name = "debugpaste",
            desc = "Writes a report of latest.log, config.yml, worldedit-config.yml, strings.json to https://athion.net/ISPaster/paste"
    )
    @CommandPermissions(value = {"worldedit.report", "worldedit.debugpaste"}, queued = false)
    public void report(Actor actor) throws WorldEditException {
        String dest;
        try {
            final File logFile = new File("logs/latest.log");
            final File config = new File(Fawe.platform().getDirectory(), "config.yml");
            final File worldeditConfig = new File(Fawe.platform().getDirectory(), "worldedit-config.yml");
            dest = IncendoPaster.debugPaste(logFile, Fawe.platform().getDebugInfo(), config, worldeditConfig);
        } catch (IOException e) {
            actor.printInfo(TextComponent.of(e.getMessage()));
            return;
        }
        actor.print(Caption.of("worldedit.report.written", TextComponent.of(dest).clickEvent(
                ClickEvent.openUrl(dest))));
    }

    @Command(
            name = "threads",
            desc = "Print all thread stacks"
    )
    @CommandPermissions(value = "worldedit.threads", queued = false)
    public void threads(Actor actor) throws WorldEditException {
        Map<Thread, StackTraceElement[]> stacks = Thread.getAllStackTraces();
        for (Map.Entry<Thread, StackTraceElement[]> entry : stacks.entrySet()) {
            Thread thread = entry.getKey();
            actor.printDebug(TextComponent.of(
                    "--------------------------------------------------------------------------------------------"));
            actor.printDebug("Thread: " + thread.getName() + " | Id: " + thread.getId() + " | Alive: " + thread.isAlive());
            for (StackTraceElement elem : entry.getValue()) {
                actor.printDebug(TextComponent.of(elem.toString()));
            }
        }
    }
    //FAWE end

    @Command(
            name = "trace",
            desc = "Toggles trace hook"
    )
    @CommandPermissions(value = "worldedit.trace", queued = false)
    void trace(
            Actor actor, LocalSession session,
            @Arg(desc = "The mode to set the trace hook to", def = "")
                    HookMode hookMode
    ) {
        boolean previousMode = session.isTracingActions();
        boolean newMode;
        if (hookMode != null) {
            newMode = hookMode == HookMode.ACTIVE;
            if (newMode == previousMode) {
                actor.print(Caption.of(previousMode
                        ? "worldedit.trace.active.already"
                        : "worldedit.trace.inactive.already"));
                return;
            }
        } else {
            newMode = !previousMode;
        }
        session.setTracingActions(newMode);
        actor.print(Caption.of(newMode ? "worldedit.trace.active" : "worldedit.trace.inactive"));
    }

    @Command(
            name = "cui",
            desc = "Complete CUI handshake (internal usage)"
    )
    @CommandPermissions(value = "worldedit.cui", queued = false)
    public void cui(Player player, LocalSession session) {
        session.setCUISupport(true);
        session.dispatchCUISetup(player);
    }

    @Command(
            name = "tz",
            desc = "Set your timezone for snapshots"
    )
    @CommandPermissions(value = "worldedit.timezone", queued = false)
    public void tz(
            Actor actor, LocalSession session,
            @Arg(desc = "The timezone to set")
                    String timezone
    ) {
        try {
            ZoneId tz = ZoneId.of(timezone);
            session.setTimezone(tz);
            actor.print(Caption.of("worldedit.timezone.set", TextComponent.of(tz.getDisplayName(
                    TextStyle.FULL, actor.getLocale()
            ))));
            actor.print(Caption.of(
                    "worldedit.timezone.current",
                    TextComponent.of(dateFormat.withLocale(actor.getLocale()).format(ZonedDateTime.now(tz)))
            ));
        } catch (ZoneRulesException e) {
            actor.print(Caption.of("worldedit.timezone.invalid"));
        }
    }

    @Command(
            name = "help",
            desc = "Displays help for WorldEdit commands"
    )
    @CommandPermissions(value = "worldedit.help", queued = false)
    public void help(
            Actor actor,
            @Switch(name = 's', desc = "List sub-commands of the given command, if applicable")
                    boolean listSubCommands,
            @ArgFlag(name = 'p', desc = "The page to retrieve", def = "1")
                    int page,
            @Arg(desc = "The command to retrieve help for", def = "", variable = true)
                    List<String> command
    ) throws WorldEditException {
        PrintCommandHelp.help(command, page, listSubCommands,
                we.getPlatformManager().getPlatformCommandManager().getCommandManager(), actor, "/worldedit help"
        );
    }

}
