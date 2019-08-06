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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.sk89q.worldedit.command.util.Logging.LogMode.ALL;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.wrappers.LocationMaskedPlayerWrapper;
import org.enginehub.piston.inject.InjectedValueAccess;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import com.sk89q.worldedit.command.util.Logging;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.PlatformCommandManager;
import com.sk89q.worldedit.scripting.CraftScriptContext;
import com.sk89q.worldedit.scripting.CraftScriptEngine;
import com.sk89q.worldedit.scripting.RhinoCraftScriptEngine;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.script.ScriptException;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.mozilla.javascript.NativeJavaObject;


/**
 * Commands related to scripting.
 */
@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
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
        name = "setupdispatcher",
        desc = ""
    )
    @CommandPermissions("fawe.setupdispatcher")
    public void setupdispatcher(Player player, LocalSession session, final InjectedValueAccess args) throws WorldEditException {
        PlatformCommandManager.getInstance().registerAllCommands();
    }

    @Command(
            name = "cs",
            desc = "Execute a CraftScript"
    )
    @CommandPermissions("worldedit.scripting.execute")
    @Logging(ALL)
    public void execute(Player player, LocalSession session,
                        @Arg(desc = "Filename of the CraftScript to load")
                            String filename,
                        @Arg(desc = "Arguments to the CraftScript", def = "", variable = true)
                            List<String> commandStr) throws WorldEditException {
        if (!player.hasPermission("worldedit.scripting.execute." + filename)) {
            BBC.SCRIPTING_NO_PERM.send(player);
            return;
        }

        session.setLastScript(filename);

        File dir = worldEdit.getWorkingDirectoryFile(worldEdit.getConfiguration().scriptsDir);
        File f = worldEdit.getSafeOpenFile(player, dir, filename, "js", "js");

        worldEdit.runScript(player, f, Stream.concat(Stream.of(filename), commandStr.stream())
                .toArray(String[]::new));
    }

    @Command(
            name = ".s",
            desc = "Execute last CraftScript"
    )
    @CommandPermissions("worldedit.scripting.execute")
    @Logging(ALL)
    public void executeLast(Player player, LocalSession session,
                            @Arg(desc = "Arguments to the CraftScript", def = "", variable = true)
                                    List<String> commandStr) throws WorldEditException {

        String lastScript = session.getLastScript();

        if (!player.hasPermission("worldedit.scripting.execute." + lastScript)) {
            BBC.SCRIPTING_NO_PERM.send(player);
            return;
        }

        if (lastScript == null) {
            BBC.SCRIPTING_CS.send(player);
            return;
        }

        File dir = worldEdit.getWorkingDirectoryFile(worldEdit.getConfiguration().scriptsDir);
        File f = worldEdit.getSafeOpenFile(player, dir, lastScript, "js", "js");

        worldEdit.runScript(player, f, Stream.concat(Stream.of(lastScript), commandStr.stream())
                .toArray(String[]::new));
    }
}
