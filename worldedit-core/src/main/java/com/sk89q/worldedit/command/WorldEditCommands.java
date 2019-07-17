/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.command;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweVersion;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.util.IncendoPaster;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import com.sk89q.worldedit.command.util.PrintCommandHelp;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.platform.ConfigurationLoadEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.extension.platform.PlatformManager;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.zone.ZoneRulesException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.ArgFlag;
import org.enginehub.piston.annotation.param.Switch;

@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class WorldEditCommands {

    private static final DateTimeFormatter dateFormat = DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH:mm:ss z");

    private final WorldEdit we;

    public WorldEditCommands(WorldEdit we) {
        this.we = we;
    }

    @Command(
        name = "version",
        aliases = { "ver" },
        desc = "Get WorldEdit/FAWE version"
        //queued = false
    )
    public void version(Actor actor) {
        FaweVersion fVer = Fawe.get().getVersion();
        String fVerStr = fVer == null ? "unknown" : "-" + fVer.build;
        actor.print("FastAsyncWorldEdit" + fVerStr + " created by Empire92");
        if (fVer != null) {
            actor.printDebug("----------- Platforms -----------");
            FaweVersion version = Fawe.get().getVersion();
            Date date = new GregorianCalendar(2000 + version.year, version.month - 1, version.day)
                .getTime();
            actor.printDebug(" - DATE: " + date.toLocaleString());
            actor.printDebug(" - COMMIT: " + Integer.toHexString(version.hash));
            actor.printDebug(" - BUILD: " + version.build);
            actor.printDebug(" - PLATFORM: " + Settings.IMP.PLATFORM);
            actor.printDebug("------------------------------------");
        }
        PlatformManager pm = we.getPlatformManager();

        actor.printDebug("----------- Platforms -----------");
        for (Platform platform : pm.getPlatforms()) {
            actor.printDebug(String.format("* %s (%s)", platform.getPlatformName(), platform.getPlatformVersion()));
        }

        actor.printDebug("----------- Capabilities -----------");
        for (Capability capability : Capability.values()) {
            Platform platform = pm.queryCapability(capability);
            actor.printDebug(String.format("%s: %s", capability.name(), platform != null ? platform.getPlatformName() : "NONE"));
        }
        actor.printDebug("------------------------------------");
        actor.printDebug("Wiki: " + "https://github.com/boy0001/FastAsyncWorldedit/wiki");
    }

    @Command(
        name = "reload",
        desc = "Reload configuration and translations"
    )
    @CommandPermissions("worldedit.reload")
    public void reload(Actor actor) {
        we.getPlatformManager().queryCapability(Capability.CONFIGURATION).reload();
        we.getEventBus().post(new ConfigurationLoadEvent(we.getPlatformManager().queryCapability(Capability.CONFIGURATION).getConfiguration()));
        Fawe.get().setupConfigs();
        actor.print("Configuration and translations reloaded!");
    }

    @Command(
        name = "report",
        aliases = { "debugpaste" },
        desc = "Writes a report of latest.log, config.yml, message.yml and your commands.yml to https://athion.net/ISPaster/paste"
//      queued = false
    )
    @CommandPermissions({"worldedit.report", "worldedit.debugpaste"})
    public void report(Actor actor) throws WorldEditException, IOException {
        BBC.DOWNLOAD_LINK.send(actor, IncendoPaster.debugPaste());
    }

    @Command(
        name = "threads",
        desc = "Print all thread stacks"
        //queued = false
    )
    @CommandPermissions("worldedit.threads")
    public void threads(Actor actor) throws WorldEditException {
        Map<Thread, StackTraceElement[]> stacks = Thread.getAllStackTraces();
        for (Map.Entry<Thread, StackTraceElement[]> entry : stacks.entrySet()) {
            Thread thread = entry.getKey();
            actor.printDebug(
                "--------------------------------------------------------------------------------------------");
            actor.printDebug(
                "Thread: " + thread.getName() + " | Id: " + thread.getId() + " | Alive: " + thread
                    .isAlive());
            for (StackTraceElement elem : entry.getValue()) {
                actor.printDebug(elem.toString());
            }
        }
    }

    @Command(
        name = "cui",
        desc = "Complete CUI handshake (internal usage)"
    )
    public void cui(Player player, LocalSession session) {
        session.setCUISupport(true);
        session.dispatchCUISetup(player);
    }

    @Command(
        name = "tz",
        desc = "Set your timezone for snapshots"
    )
    public void tz(Player player, LocalSession session,
                   @Arg(desc = "The timezone to set")
                       String timezone) {
        try {
            ZoneId tz = ZoneId.of(timezone);
            session.setTimezone(tz);
            BBC.TIMEZONE_SET.send(player, tz.getDisplayName(
                TextStyle.FULL, Locale.ENGLISH
            ));
            BBC.TIMEZONE_DISPLAY
                .send(player, dateFormat.format(ZonedDateTime.now(tz)));
        } catch (ZoneRulesException e) {
            player.printError("Invalid timezone");
        }
    }

    @Command(
        name = "help",
        desc = "Displays help for FAWE commands"
        //queued = false
    )
    @CommandPermissions("worldedit.help")
    public void help(Actor actor,
                     @Switch(name = 's', desc = "List sub-commands of the given command, if applicable")
                         boolean listSubCommands,
                     @ArgFlag(name = 'p', desc = "The page to retrieve", def = "1")
                         int page,
                     @Arg(desc = "The command to retrieve help for", def = "", variable = true)
                         List<String> command) throws WorldEditException {
        PrintCommandHelp.help(command, page, listSubCommands, we, actor);
    }
}
