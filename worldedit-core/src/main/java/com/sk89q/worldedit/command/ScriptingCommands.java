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

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.wrappers.LocationMaskedPlayerWrapper;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.Logging;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.CommandManager;
import com.sk89q.worldedit.scripting.CraftScriptContext;
import com.sk89q.worldedit.scripting.CraftScriptEngine;
import com.sk89q.worldedit.scripting.RhinoCraftScriptEngine;
import org.mozilla.javascript.NativeJavaObject;

import javax.script.ScriptException;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.sk89q.minecraft.util.commands.Logging.LogMode.ALL;

/**
 * Commands related to scripting.
 */
@Command(aliases = {}, desc = "Run craftscripts: [More Info](https://goo.gl/dHDxLG)")
public class ScriptingCommands {

    private final WorldEdit worldEdit;

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public ScriptingCommands(WorldEdit worldEdit) {
        checkNotNull(worldEdit);
        this.worldEdit = worldEdit;
    }

    @Command(
            aliases = {"setupdispatcher"},
            desc = "",
            usage = "",
            min = 1,
            max = 1
    )
    public void setupdispatcher(Player player, LocalSession session, final CommandContext args) throws WorldEditException {
        CommandManager.getInstance().setupDispatcher();
    }

    public static <T> T runScript(Player player, File f, String[] args) throws WorldEditException {
        String filename = f.getPath();
        int index = filename.lastIndexOf(".");
        String ext = filename.substring(index + 1);

        if (!ext.equalsIgnoreCase("js")) {
            player.printError("Only .js scripts are currently supported");
            return null;
        }

        String script;

        try {
            InputStream file;

            if (!f.exists()) {
                file = WorldEdit.class.getResourceAsStream("craftscripts/" + filename);

                if (file == null) {
                    player.printError("Script does not exist: " + filename);
                    return null;
                }
            } else {
                file = new FileInputStream(f);
            }

            byte[] data;
            try (DataInputStream in = new DataInputStream(file)) {
                data = new byte[in.available()];
                in.readFully(data);
            }
            script = new String(data, 0, data.length, StandardCharsets.UTF_8);
        } catch (IOException e) {
            player.printError("Script read error: " + e.getMessage());
            return null;
        }

        WorldEdit worldEdit1 = WorldEdit.getInstance();
        LocalSession session = worldEdit1.getSessionManager().get(player);

        CraftScriptEngine engine;

        Object result = null;
        try {

            engine = new RhinoCraftScriptEngine();
        } catch (NoClassDefFoundError e) {
            player.printError("Failed to find an installed script engine.");
            player.printError("Download: https://github.com/downloads/mozilla/rhino/rhino1_7R4.zip");
            player.printError("Extract: `js.jar` to `plugins` or `mods` directory`");
            player.printError("More info: https://github.com/boy0001/CraftScripts/");
            return null;
        }

        engine.setTimeLimit(worldEdit1.getConfiguration().scriptTimeout);

        CraftScriptContext scriptContext = new CraftScriptContext(worldEdit1, worldEdit1.getPlatformManager().queryCapability(Capability.USER_COMMANDS),
                                                                  worldEdit1.getConfiguration(), session, player, args);

        Map<String, Object> vars = new HashMap<>();
        vars.put("argv", args);
        vars.put("context", scriptContext);
        vars.put("actor", player);
        vars.put("player", player);

        try {
            result = engine.evaluate(script, filename, vars);
        } catch (ScriptException e) {
            e.printStackTrace();
            player.printError(BBC.getPrefix() + "Failed to execute:");
            player.printRaw(e.getMessage());
        } catch (NumberFormatException | WorldEditException e) {
            throw e;
        } catch (Throwable e) {
            player.printError("Failed to execute (see console):");
            player.printRaw(e.getClass().getCanonicalName());
            e.printStackTrace();
        }
        if (result instanceof NativeJavaObject) {
            return (T) ((NativeJavaObject) result).unwrap();
        }
        return (T) result;
    }

    @Command(
        aliases = { "cs" },
        usage = "<filename> [args...]",
        desc = "Execute a CraftScript",
        min = 1,
        max = -1
    )
    @CommandPermissions("worldedit.scripting.execute")
    @Logging(ALL)
    public void execute(Player player, LocalSession session, CommandContext args) throws WorldEditException {

        String[] scriptArgs = args.getSlice(1);
        String name = args.getString(0);

        if (!player.hasPermission("worldedit.scripting.execute." + name)) {
            BBC.SCRIPTING_NO_PERM.send(player);
            return;
        }

        session.setLastScript(name);

        File dir = worldEdit.getWorkingDirectoryFile(worldEdit.getConfiguration().scriptsDir);
        File f = worldEdit.getSafeOpenFile(player, dir, name, "js", "js");
        try {
            new RhinoCraftScriptEngine();
        } catch (NoClassDefFoundError e) {
            player.printError("Failed to find an installed script engine.");
            player.printError("Download: https://github.com/downloads/mozilla/rhino/rhino1_7R4.zip");
            player.printError("Extract: `js.jar` to `plugins` or `mods` directory`");
            player.printError("More info: https://github.com/boy0001/CraftScripts/");
            return;
        }
        runScript(LocationMaskedPlayerWrapper.unwrap(player), f, scriptArgs);
    }

    @Command(
        aliases = { ".s" },
        usage = "[args...]",
        desc = "Execute last CraftScript",
        min = 0,
        max = -1
    )
    @CommandPermissions("worldedit.scripting.execute")
    @Logging(ALL)
    public void executeLast(Player player, LocalSession session, CommandContext args) throws WorldEditException {

        String lastScript = session.getLastScript();

        if (!player.hasPermission("worldedit.scripting.execute." + lastScript)) {
            BBC.SCRIPTING_NO_PERM.send(player);
            return;
        }

        if (lastScript == null) {
            BBC.SCRIPTING_CS.send(player);
            return;
        }

        String[] scriptArgs = args.getSlice(0);

        File dir = worldEdit.getWorkingDirectoryFile(worldEdit.getConfiguration().scriptsDir);
        File f = worldEdit.getSafeOpenFile(player, dir, lastScript, "js", "js");

        worldEdit.runScript(player, f, scriptArgs);
    }
}
