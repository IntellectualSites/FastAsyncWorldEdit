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

package com.boydti.fawe.util;

import com.boydti.fawe.command.AnvilCommands;
import com.boydti.fawe.command.CFICommands;
import org.enginehub.piston.annotation.Command;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.NestedCommand;
import com.sk89q.worldedit.command.BiomeCommands;
import com.sk89q.worldedit.command.BrushCommands;
import com.sk89q.worldedit.command.BrushOptionsCommands;
import com.sk89q.worldedit.command.ChunkCommands;
import com.sk89q.worldedit.command.ClipboardCommands;
import com.sk89q.worldedit.command.GenerationCommands;
import com.sk89q.worldedit.command.HistoryCommands;
import com.sk89q.worldedit.command.MaskCommands;
import com.sk89q.worldedit.command.NavigationCommands;
import com.sk89q.worldedit.command.OptionsCommands;
import com.sk89q.worldedit.command.PatternCommands;
import com.sk89q.worldedit.command.RegionCommands;
import com.sk89q.worldedit.command.SchematicCommands;
import com.sk89q.worldedit.command.ScriptingCommands;
import com.sk89q.worldedit.command.SelectionCommands;
import com.sk89q.worldedit.command.SnapshotCommands;
import com.sk89q.worldedit.command.SnapshotUtilCommands;
import com.sk89q.worldedit.command.SuperPickaxeCommands;
import com.sk89q.worldedit.command.ToolCommands;
import com.sk89q.worldedit.command.TransformCommands;
import com.sk89q.worldedit.command.UtilityCommands;
import com.sk89q.worldedit.command.WorldEditCommands;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public final class DocumentationPrinter {

    private DocumentationPrinter() {
    }

    /**
     * Generates documentation.
     *
     * @param args arguments
     * @throws IOException thrown on I/O error
     */
    public static void main(String[] args) throws IOException {
        writePermissionsWikiTable();
    }

    private static void writePermissionsWikiTable()
            throws IOException {
        try (FileOutputStream fos = new FileOutputStream("wiki_permissions.md")) {
            PrintStream stream = new PrintStream(fos);

            stream.print("## Overview\n");
            stream.print("This page is generated from the source. " +
                    "Click one of the edit buttons below to modify a command class. " +
                    "You will need to find the parts which correspond to the documentation. " +
                    "Command documentation will be consistent with what is available ingame");
            stream.println();
            stream.println();
            stream.print("To view this information ingame use `//help [category|command]`\n");
            stream.print("## Command Syntax     \n");
            stream.print(" - `<arg>` - A required parameter     \n");
            stream.print(" - `[arg]` - An optional parameter     \n");
            stream.print(" - `<arg1|arg2>` - Multiple parameters options     \n");
            stream.print(" - `<arg=value>` - Default or suggested value     \n");
            stream.print(" - `-a` - A command flag e.g. `//<command> -a [flag-value]`");
            stream.println();
            stream.print("## See also\n");
            stream.print(" - [Masks](https://github.com/boy0001/FastAsyncWorldedit/wiki/WorldEdit---FAWE-mask-list)\n");
            stream.print(" - [Patterns](https://github.com/boy0001/FastAsyncWorldedit/wiki/WorldEdit-and-FAWE-patterns)\n");
            stream.print(" - [Transforms](https://github.com/boy0001/FastAsyncWorldedit/wiki/Transforms)\n");
            stream.println();
            stream.print("## Content");
            stream.println();
            stream.print("Click on a category to go to the list of commands, or `More Info` for detailed descriptions ");
            stream.println();
            StringBuilder builder = new StringBuilder();
            writePermissionsWikiTable(stream, builder, "/we ", WorldEditCommands.class);
            writePermissionsWikiTable(stream, builder, "/", UtilityCommands.class);
            writePermissionsWikiTable(stream, builder, "/", RegionCommands.class);
            writePermissionsWikiTable(stream, builder, "/", SelectionCommands.class);
            writePermissionsWikiTable(stream, builder, "/", HistoryCommands.class);
            writePermissionsWikiTable(stream, builder, "/schematic ", SchematicCommands.class);
            writePermissionsWikiTable(stream, builder, "/", ClipboardCommands.class);
            writePermissionsWikiTable(stream, builder, "/", GenerationCommands.class);
            writePermissionsWikiTable(stream, builder, "/", BiomeCommands.class);
            writePermissionsWikiTable(stream, builder, "/anvil ", AnvilCommands.class);
            writePermissionsWikiTable(stream, builder, "/sp ", SuperPickaxeCommands.class);
            writePermissionsWikiTable(stream, builder, "/", NavigationCommands.class);
            writePermissionsWikiTable(stream, builder, "/snapshot", SnapshotCommands.class);
            writePermissionsWikiTable(stream, builder, "/", SnapshotUtilCommands.class);
            writePermissionsWikiTable(stream, builder, "/", ScriptingCommands.class);
            writePermissionsWikiTable(stream, builder, "/", ChunkCommands.class);
            writePermissionsWikiTable(stream, builder, "/", OptionsCommands.class);
            writePermissionsWikiTable(stream, builder, "/", BrushOptionsCommands.class);
            writePermissionsWikiTable(stream, builder, "/tool ", ToolCommands.class);
            writePermissionsWikiTable(stream, builder, "/brush ", BrushCommands.class);
            writePermissionsWikiTable(stream, builder, "", MaskCommands.class, "/Masks");
            writePermissionsWikiTable(stream, builder, "", PatternCommands.class, "/Patterns");
            writePermissionsWikiTable(stream, builder, "", TransformCommands.class, "/Transforms");
            writePermissionsWikiTable(stream, builder, "/cfi ", CFICommands.class, "Create From Image");
            stream.println();
            stream.print("#### Uncategorized\n");
            stream.append("| Aliases | Permission | flags | Usage |\n");
            stream.append("| --- | --- | --- | --- |\n");
            stream.append("| //cancel | fawe.cancel | | Cancels your current operations |\n");
            stream.append("| /plot replaceall | plots.replaceall | | Replace all blocks in the plot world |\n");
//            stream.append("| /plot createfromimage | plots.createfromimage | | Starts world creation from a heightmap image: [More Info](https://github.com/boy0001/FastAsyncWorldedit/wiki/CreateFromImage) |\n");
            stream.print("\n---\n");

            stream.print(builder);
        }
    }

    private static void writePermissionsWikiTable(PrintStream stream, StringBuilder content, String prefix, Class<?> cls) {
        writePermissionsWikiTable(stream, content, prefix, cls, getName(cls));
    }

    public static String getName(Class cls) {
        return cls.getSimpleName().replaceAll("(\\p{Ll})(\\p{Lu})", "$1 $2");
    }

    private static void writePermissionsWikiTable(PrintStream stream, StringBuilder content, String prefix, Class<?> cls, String name) {
        stream.print(" - [`" + name + "`](#" + name.replaceAll(" ", "-").replaceAll("/", "").toLowerCase() + "-edittop) ");
        Command cmd = cls.getAnnotation(Command.class);
        if (cmd != null) {
            stream.print(" (" + cmd.desc() + ")");
        }
        stream.println();
        writePermissionsWikiTable(content, prefix, cls, name, true);
    }

    private static void writePermissionsWikiTable(StringBuilder stream, String prefix, Class<?> cls, String name, boolean title) {
        //  //setbiome || worldedit.biome.set || //setbiome || p || Sets the biome of the player's current block or region.
        if (title) {
            String path = "https://github.com/boy0001/FastAsyncWorldedit/edit/master/core/src/main/java/" + cls.getName().replaceAll("\\.", "/") + ".java";
            stream.append("### **" + name + "** `[`[`edit`](" + path + ")`|`[`top`](#overview)`]`");
            stream.append("\n");
            Command cmd = cls.getAnnotation(Command.class);
            if (cmd != null) {
                if (!cmd.desc().isEmpty()) {
                    stream.append("> (" + (cmd.desc()) + ")    \n");
                }
                if (!cmd.help().isEmpty()) {
                    stream.append("" + (cmd.help()) + "    \n");
                }
            }
            stream.append("\n");
            stream.append("---");
            stream.append("\n");
            stream.append("\n");
        }
        for (Method method : cls.getMethods()) {
            if (!method.isAnnotationPresent(Command.class)) {
                continue;
            }

            Command cmd = method.getAnnotation(Command.class);
            String[] aliases = cmd.aliases();
            String usage = prefix + aliases[0] + " " + cmd.usage();
            if (!cmd.flags().isEmpty()) {
                for (char c : cmd.flags().toCharArray()) {
                    usage += " [-" + c + "]";
                }
            }
//            stream.append("#### [`" + usage + "`](" + "https://github.com/boy0001/FastAsyncWorldedit/wiki/" + aliases[0] + ")\n");
            stream.append("#### `" + usage + "`\n");
            if (method.isAnnotationPresent(CommandPermissions.class)) {
                CommandPermissions perms = method.getAnnotation(CommandPermissions.class);
                stream.append("**Perm**: `" + StringMan.join(perms.value(), "`, `") + "`    \n");
            }
            String help = cmd.help() == null || cmd.help().isEmpty() ? cmd.desc() : cmd.help();
            stream.append("**Desc**: " + help.trim().replaceAll("\n", "<br />") + "    \n");

            if (method.isAnnotationPresent(NestedCommand.class)) {
                NestedCommand nested =
                        method.getAnnotation(NestedCommand.class);

                Class<?>[] nestedClasses = nested.value();
                for (Class clazz : nestedClasses) {
                    writePermissionsWikiTable(stream, prefix + cmd.aliases()[0] + " ", clazz, getName(clazz), false);
                }
            }
        }
        stream.append("\n");
        if (title) stream.append("---");
        stream.append("\n");
        stream.append("\n");
    }
}
