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
import com.sk89q.minecraft.util.commands.Step;
import com.sk89q.worldedit.command.ToolUtilCommands;
import com.sk89q.worldedit.internal.annotation.Range;
import org.enginehub.piston.annotation.Command;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.minecraft.util.commands.NestedCommand;
import com.sk89q.worldedit.command.BiomeCommands;
import com.sk89q.worldedit.command.BrushCommands;
import com.sk89q.worldedit.command.ChunkCommands;
import com.sk89q.worldedit.command.ClipboardCommands;
import com.sk89q.worldedit.command.GenerationCommands;
import com.sk89q.worldedit.command.HistoryCommands;
//import com.sk89q.worldedit.command.MaskCommands;
import com.sk89q.worldedit.command.NavigationCommands;
//import com.sk89q.worldedit.command.PatternCommands;
import com.sk89q.worldedit.command.RegionCommands;
import com.sk89q.worldedit.command.SchematicCommands;
import com.sk89q.worldedit.command.ScriptingCommands;
import com.sk89q.worldedit.command.SelectionCommands;
import com.sk89q.worldedit.command.SnapshotCommands;
import com.sk89q.worldedit.command.SnapshotUtilCommands;
import com.sk89q.worldedit.command.SuperPickaxeCommands;
import com.sk89q.worldedit.command.ToolCommands;
//import com.sk89q.worldedit.command.TransformCommands;
import com.sk89q.worldedit.command.UtilityCommands;
import com.sk89q.worldedit.command.WorldEditCommands;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.ArgFlag;
import org.enginehub.piston.annotation.param.Switch;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
            stream.print(" - `-a` - A command flag e.g., `//<command> -a [flag-value]`");
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
            writePermissionsWikiTable(stream, builder, "/", ToolUtilCommands.class);
            writePermissionsWikiTable(stream, builder, "/tool ", ToolCommands.class);
            writePermissionsWikiTable(stream, builder, "/brush ", BrushCommands.class);
            //writePermissionsWikiTable(stream, builder, "", MaskCommands.class, "/Masks");
            //writePermissionsWikiTable(stream, builder, "", PatternCommands.class, "/Patterns");
            //writePermissionsWikiTable(stream, builder, "", TransformCommands.class, "/Transforms");
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
        if (title) {
            String path = "https://github.com/boy0001/FastAsyncWorldedit/edit/master/core/src/main/java/" + cls.getName().replaceAll("\\.", "/") + ".java";
            stream.append("### **" + name + "** `[`[`edit`](" + path + ")`|`[`top`](#overview)`]`");
            stream.append("\n");
            Command cmd = cls.getAnnotation(Command.class);
            if (cmd != null) {
                if (!cmd.desc().isEmpty()) {
                    stream.append("> (" + (cmd.desc()) + ")    \n");
                }
                if (!cmd.descFooter().isEmpty()) {
                    stream.append("" + (cmd.descFooter()) + "    \n");
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
            String usage = prefix + aliases[0] + " " + getUsage(cmd, method);

            stream.append("#### `" + usage + "`\n");
            if (method.isAnnotationPresent(CommandPermissions.class)) {
                CommandPermissions perms = method.getAnnotation(CommandPermissions.class);
                stream.append("**Perm**: `" + StringMan.join(perms.value(), "`, `") + "`    \n");
            }
            String help = getDesc(cmd, method);
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

    public static String getDesc(Command command, Method method) {
        Parameter[] params = method.getParameters();
        List<String> desc = new ArrayList<>();
        for (Parameter param : params) {
            String[] info = getParamInfo(param);
            if (info != null) {
                desc.add(info[0].replace("%s0", info[1]) + " - " + info[2] + ": " + info[3]);
            }
        }
        String footer = command.descFooter();
        if (!footer.isEmpty()) footer += "\n";
        return footer + StringMan.join(desc, "\n");
    }

    public static String getUsage(Command command, Method method) {
        Parameter[] params = method.getParameters();
        List<String> usage = new ArrayList<>();
        for (Parameter param : params) {
            String[] info = getParamInfo(param);
            if (info != null) {
                usage.add(info[0].replace("%s0", info[1]));
            }
        }
        return StringMan.join(usage, " ");
    }

    /*
    Return format, name, type, description
     */
    public static String[] getParamInfo(Parameter param) {
        Switch switchAnn = param.getAnnotation(Switch.class);
        Arg argAnn = param.getAnnotation(Arg.class);
        Range rangeAnn = param.getAnnotation(Range.class);
        Step stepAnn = param.getAnnotation(Step.class);
        if (switchAnn != null || argAnn != null || rangeAnn != null || stepAnn != null) {
            String[] result = new String[] { "[%s0]", param.getName(), param.getType().getSimpleName(), ""};
            boolean optional = argAnn != null && argAnn.def().length != 0;
            if (optional) {
                result[0] = "<%s0>";
            }
            if (argAnn != null) result[1] = argAnn.name();
            if (argAnn != null) {
                if (argAnn.def().length != 0) {
                    result[0] = result[0].replace("%s0", "%s0=" + argAnn.def());
                }
                result[3] = argAnn.desc();
            } else if (switchAnn != null) {
                result[0] = result[0].replace("%s0", "-" + switchAnn.name() + " %s0");
            }
            if (switchAnn != null) result[3] = switchAnn.desc();
            if (rangeAnn != null) {
                String step;
                String min = rangeAnn.min() == Double.MIN_VALUE ? "(-∞" : ("[" + rangeAnn.min());
                String max = rangeAnn.max() == Double.MAX_VALUE ? "∞)" : (rangeAnn.max() + "]");
                result[0] += min + "," + max;
            }
            if (stepAnn != null) {
                result[0] += "⦧" + stepAnn.value();
            }
            return result;
        }
        return null;
    }

    public static Collection<ArgFlag> getFlags(Command command, Method method) {
        Parameter[] params = method.getParameters();
        List<ArgFlag> flags = new ArrayList<>();
        for (Parameter param : params) {
            ArgFlag flagAnn = param.getAnnotation(ArgFlag.class);
            if (flagAnn != null) flags.add(flagAnn);
        }
        return flags;
    }
}
