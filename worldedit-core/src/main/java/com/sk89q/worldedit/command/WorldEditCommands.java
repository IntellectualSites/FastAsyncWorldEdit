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
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.platform.ConfigurationLoadEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.CommandManager;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.extension.platform.PlatformManager;
import com.sk89q.worldedit.extension.platform.PlatformManager;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TimeZone;

@Command(aliases = {"worldedit", "we", "fawe"}, desc = "Updating, informational, debug and help commands")
public class WorldEditCommands {
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

    private final WorldEdit we;

    public WorldEditCommands(WorldEdit we) {
        this.we = we;
    }

    @Command(
        aliases = { "version", "ver" },
        usage = "",
        desc = "Get WorldEdit/FAWE version",
        min = 0,
        max = 0,
        queued = false
    )
    public void version(Actor actor) throws WorldEditException {
        FaweVersion fVer = Fawe.get().getVersion();
        String fVerStr = fVer == null ? "unknown" : "-" + fVer.build;
        actor.print("FastAsyncWorldEdit-1.14" + fVerStr + " by Empire92");
        if (fVer != null) {
            actor.printDebug("----------- Platforms -----------");
            FaweVersion version = Fawe.get().getVersion();
            Date date = new GregorianCalendar(2000 + version.year, version.month - 1, version.day).getTime();
            actor.printDebug(" - DATE: " + date.toLocaleString());
            actor.printDebug(" - COMMIT: " + Integer.toHexString(version.hash));
            actor.printDebug(" - BUILD: " + version.build);
            actor.printDebug(" - PLATFORM: " + Settings.IMP.PLATFORM);
            actor.printDebug("------------------------------------");
        }
        PlatformManager pm = we.getPlatformManager();

        actor.printDebug("----------- Platforms -----------");
        for (Platform platform : pm.getPlatforms()) {
            actor.printDebug(String.format("* %s", platform.getPlatformName()));
        }

        actor.printDebug("----------- Capabilities -----------");
        for (Capability capability : Capability.values()) {
            Platform platform = pm.queryCapability(capability);
            actor.printDebug(String.format(" - %s: %s", capability.name(), platform != null ? platform.getPlatformName() : "NONE"));
        }
        actor.printDebug("------------------------------------");
        actor.printDebug("Wiki: " + "https://github.com/boy0001/FastAsyncWorldedit/wiki");
    }

    @Command(
        aliases = { "reload" },
        usage = "",
        desc = "Reload configuration and translations",
        min = 0,
        max = 0
    )
    @CommandPermissions("worldedit.reload")
    public void reload(Actor actor) throws WorldEditException {
        we.getPlatformManager().queryCapability(Capability.CONFIGURATION).reload();
        we.getEventBus().post(new ConfigurationLoadEvent(we.getPlatformManager().queryCapability(Capability.CONFIGURATION).getConfiguration()));
        Fawe.get().setupConfigs();
        CommandManager.getInstance().register(we.getPlatformManager().queryCapability(Capability.USER_COMMANDS));
        actor.print("Configuration and translations reloaded!");
    }

    @Command(
            aliases = {"debugpaste"},
            usage = "",
            desc = "Upload latest.log, config.yml, message.yml and your commands.yml to https://athion.net/ISPaster/paste",
            min = 0,
            max = 0
    )
    @CommandPermissions("worldedit.debugpaste")
    public void debugpaste(Actor actor) throws WorldEditException, IOException {
        BBC.DOWNLOAD_LINK.send(actor, IncendoPaster.debugPaste());
    }

    @Command(
            aliases = {"threads"},
            usage = "",
            desc = "Print all thread stacks",
            min = 0,
            max = 0,
            queued = false
    )
    @CommandPermissions("worldedit.threads")
    public void threads(Actor actor) throws WorldEditException {
        Map<Thread, StackTraceElement[]> stacks = Thread.getAllStackTraces();
        for (Map.Entry<Thread, StackTraceElement[]> entry : stacks.entrySet()) {
            Thread thread = entry.getKey();
            actor.printDebug("--------------------------------------------------------------------------------------------");
            actor.printDebug("Thread: " + thread.getName() + " | Id: " + thread.getId() + " | Alive: " + thread.isAlive());
            for (StackTraceElement elem : entry.getValue()) {
                actor.printDebug(elem.toString());
            }
        }
    }

    @Command(
        aliases = { "cui" },
        usage = "",
        desc = "Complete CUI handshake (internal usage)",
        min = 0,
        max = 0
    )
    public void cui(Player player, LocalSession session) throws WorldEditException {
        session.setCUISupport(true);
        session.dispatchCUISetup(player);
    }

    @Command(
        aliases = { "tz" },
        usage = "[timezone]",
        desc = "Set your timezone for snapshots",
        min = 1,
        max = 1
    )
    public void tz(Player player, LocalSession session, CommandContext args) throws WorldEditException {
        TimeZone tz = TimeZone.getTimeZone(args.getString(0));
        session.setTimezone(tz);
        BBC.TIMEZONE_SET.send(player, tz.getDisplayName());
        BBC.TIMEZONE_DISPLAY.send(player, dateFormat.format(Calendar.getInstance(tz).getTime()));
    }

    @Command(
        aliases = { "help" },
        usage = "[<command>]",
            desc = "Displays help for FAWE commands",
        min = 0,
        max = -1,
        queued = false
    )
    @CommandPermissions("worldedit.help")
    public void help(Actor actor, CommandContext args) throws WorldEditException {
        UtilityCommands.help(args, we, actor);
    }
}
