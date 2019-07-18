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

import static com.boydti.fawe.util.ReflectionUtils.as;
import static com.google.common.base.Preconditions.checkNotNull;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Commands;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RunnableVal3;
import com.boydti.fawe.object.clipboard.MultiClipboardHolder;
import com.boydti.fawe.object.clipboard.URIClipboardHolder;
import com.boydti.fawe.object.clipboard.remap.ClipboardRemapper;
import com.boydti.fawe.object.schematic.StructureFormat;

import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.chat.Message;
import org.enginehub.piston.inject.InjectedValueAccess;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.extent.PlayerSaveClipboardEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.formatting.component.ErrorFormat;
import com.sk89q.worldedit.util.formatting.component.PaginationBox;
import com.sk89q.worldedit.util.formatting.component.SchematicPaginationBox;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.io.Closer;
import com.sk89q.worldedit.util.io.file.FilenameException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import java.util.regex.Pattern;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.ArgFlag;
import org.enginehub.piston.annotation.param.Switch;
import org.enginehub.piston.exception.StopExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import com.boydti.fawe.object.schematic.visualizer.SchemVis;

/**
 * Commands that work with schematic files.
 */
@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class SchematicCommands {

    private static final Logger log = LoggerFactory.getLogger(SchematicCommands.class);
    private final WorldEdit worldEdit;

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public SchematicCommands(WorldEdit worldEdit) {
        checkNotNull(worldEdit);
        this.worldEdit = worldEdit;
    }

    @Command(
            name = "loadall",
            desc = "Load multiple clipboards (paste will randomly choose one)",
        descFooter = "Load multiple clipboards (paste will randomly choose one)\n" +
        "The -r flag will apply random rotation"
    )
    @Deprecated
    @CommandPermissions({"worldedit.clipboard.load", "worldedit.schematic.load", "worldedit.schematic.load.web", "worldedit.schematic.load.asset"})
    public void loadall(final Player player, final LocalSession session,
                    @Arg(desc = "Format name.", def = "schematic")
                        final String formatName,
                    @Arg(desc = "File name.")
                        final String filename,
                    @Switch(name = 'r', desc = "Apply random rotation")
                        boolean randomRotate) throws FilenameException {
        final ClipboardFormat format = ClipboardFormats.findByAlias(formatName);
        if (format == null) {
            BBC.CLIPBOARD_INVALID_FORMAT.send(player, formatName);
            return;
        }
        try {
            MultiClipboardHolder all = ClipboardFormats.loadAllFromInput(player, filename, null, true);
            if (all != null) {
                session.addClipboard(all);
                BBC.SCHEMATIC_LOADED.send(player, filename);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Command(
            name = "clear",
            desc = "Clear your clipboard"
    )
    @CommandPermissions({"worldedit.clipboard.clear", "worldedit.schematic.clear"})
    public void clear(Player player, LocalSession session) throws WorldEditException {
        session.setClipboard(null);
        BBC.CLIPBOARD_CLEARED.send(player);
    }

    @Command(
            name = "unload",
            desc = "Remove a clipboard from your multi-clipboard"
    )
    @CommandPermissions({"worldedit.clipboard.clear", "worldedit.schematic.clear"})
    public void unload(Player player, LocalSession session, String fileName) throws WorldEditException {
        URI uri;
        if (fileName.startsWith("file:/") || fileName.startsWith("http://") || fileName.startsWith("https://")) {
            uri = URI.create(fileName);
        } else {
            final LocalConfiguration config = this.worldEdit.getConfiguration();
            File working = this.worldEdit.getWorkingDirectoryFile(config.saveDir);
            File root = Settings.IMP.PATHS.PER_PLAYER_SCHEMATICS ? new File(working, player.getUniqueId().toString()) : working;
            uri = new File(root, fileName).toURI();
        }

        ClipboardHolder clipboard = session.getClipboard();
        if (clipboard instanceof URIClipboardHolder) {
            URIClipboardHolder identifiable = (URIClipboardHolder) clipboard;
            if (identifiable.contains(uri)) {
                if (identifiable instanceof MultiClipboardHolder) {
                    MultiClipboardHolder multi = (MultiClipboardHolder) identifiable;
                    multi.remove(uri);
                    if (multi.getHolders().isEmpty()) session.setClipboard(null);
                } else {
                    session.setClipboard(null);
                }
                BBC.CLIPBOARD_CLEARED.send(player);
                return;
            }
        }
        BBC.CLIPBOARD_URI_NOT_FOUND.send(player, fileName);
    }

    @Command(
            name = "remap",
            desc = "Remap a clipboard between MCPE/PC values"
    )
    @Deprecated
    @CommandPermissions({"worldedit.schematic.remap"})
    public void remap(final Player player, final LocalSession session) throws WorldEditException {
        ClipboardRemapper remapper = new ClipboardRemapper(ClipboardRemapper.RemapPlatform.PE, ClipboardRemapper.RemapPlatform.PC);

        for (Clipboard clip : session.getClipboard().getClipboards()) {
            remapper.apply(clip);
        }
        player.print("Remapped schematic");
    }

    @Command(
        name = "load",
        desc = "Load a schematic into your clipboard"
    )
    @CommandPermissions({"worldedit.clipboard.load", "worldedit.schematic.load", "worldedit.schematic.load.asset", "worldedit.schematic.load.web", "worldedit.schematic.load.other"})
    public void load(Player player, LocalSession session,
                     @Arg(desc = "File name.")
                         String filename,
                     @Arg(desc = "Format name.", def = "sponge")
                         String formatName) throws FilenameException {
        LocalConfiguration config = worldEdit.getConfiguration();
        ClipboardFormat format = formatName == null ? null : ClipboardFormats.findByAlias(formatName);
        InputStream in = null;
        try {
            URI uri;
            if (filename.startsWith("url:")) {
                if (!player.hasPermission("worldedit.schematic.load.web")) {
                    BBC.NO_PERM.send(player, "worldedit.schematic.load.web");
                    return;
                }
                UUID uuid = UUID.fromString(filename.substring(4));
                URL base = new URL(Settings.IMP.WEB.URL);
                URL url = new URL(base, "uploads/" + uuid + "." + format.getPrimaryFileExtension());
                ReadableByteChannel rbc = Channels.newChannel(url.openStream());
                in = Channels.newInputStream(rbc);
                uri = url.toURI();
            } else {
                if (!player.hasPermission("worldedit.schematic.load") && !player.hasPermission("worldedit.clipboard.load")) {
                    BBC.NO_PERM.send(player, "worldedit.clipboard.load");
                    return;
                }
                File working = worldEdit.getWorkingDirectoryFile(config.saveDir);
                File dir = Settings.IMP.PATHS.PER_PLAYER_SCHEMATICS ? new File(working, player.getUniqueId().toString()) : working;
                File f;
                if (filename.startsWith("#")) {
                    String[] extensions;
                    if (format != null) {
                        extensions = format.getFileExtensions().toArray(new String[0]);
                    } else {
                        extensions = ClipboardFormats.getFileExtensionArray();
                    }
                    f = player.openFileOpenDialog(extensions);
                    if (f == null || !f.exists()) {
                        player.printError("Schematic " + filename + " does not exist! (" + f + ")");
                        return;
                    }
                } else {
                    if (Settings.IMP.PATHS.PER_PLAYER_SCHEMATICS && Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}").matcher(filename).find() && !player.hasPermission("worldedit.schematic.load.other")) {
                        BBC.NO_PERM.send(player, "worldedit.schematic.load.other");
                        return;
                    }
                    if (format == null && filename.matches(".*\\.[\\w].*")) {
                        String extension = filename.substring(filename.lastIndexOf('.') + 1);
                        format = ClipboardFormats.findByExtension(extension);
                    }
                    f = MainUtil.resolve(dir, filename, format, false);
                }
                if (f == null || !f.exists()) {
                    if (!filename.contains("../")) {
                        dir = this.worldEdit.getWorkingDirectoryFile(config.saveDir);
                        f = MainUtil.resolve(dir, filename, format, false);
                    }
                }
                if (f == null || !f.exists() || !MainUtil.isInSubDirectory(working, f)) {
                    player.printError("Schematic " + filename + " does not exist! (" + (f != null && f.exists()) + "|" + f + "|" + (f != null && !MainUtil.isInSubDirectory(working, f)) + ")");
                    return;
                }
                if (format == null) {
                    format = ClipboardFormats.findByFile(f);
                    if (format == null) {
                        BBC.CLIPBOARD_INVALID_FORMAT.send(player, f.getName());
                        return;
                    }
                }
                in = new FileInputStream(f);
                uri = f.toURI();
            }
            format.hold(player, uri, in);
            BBC.SCHEMATIC_LOADED.send(player, filename);
        } catch (IllegalArgumentException e) {
            player.printError("Unknown filename: " + filename);
        } catch (URISyntaxException | IOException e) {
            player.printError("File could not be read or it does not exist: " + e.getMessage());
            log.warn("Failed to load a saved clipboard", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    @Command(
        name = "save",
        desc = "Save a schematic into your clipboard"
    )
    @CommandPermissions({"worldedit.clipboard.save", "worldedit.schematic.save", "worldedit.schematic.save.other"})
    public void save(Player player, LocalSession session,
                     @Arg(desc = "File name.")
                         String filename,
                     @Arg(desc = "Format name.", def = "sponge")
                         String formatName,
                     @Switch(name = 'f', desc = "Overwrite an existing file.")
                         boolean allowOverwrite,
                     @Switch(name = 'g', desc = "//TODO")
                         boolean global
        ) throws WorldEditException {
        LocalConfiguration config = worldEdit.getConfiguration();

        File dir = worldEdit.getWorkingDirectoryFile(config.saveDir);

        if (!global && Settings.IMP.PATHS.PER_PLAYER_SCHEMATICS) {
            dir = new File(dir, player.getUniqueId().toString());
        }

        ClipboardFormat format = ClipboardFormats.findByAlias(formatName);
        if (format == null) {
            player.printError("Unknown schematic format: " + formatName);
            return;
        }

        boolean other = false;
        if (filename.contains("../")) {
            other = true;
            if (!player.hasPermission("worldedit.schematic.save.other")) {
                BBC.NO_PERM.send(player, "worldedit.schematic.save.other");
                return;
            }
            if (filename.startsWith("../")) {
                dir = worldEdit.getWorkingDirectoryFile(config.saveDir);
                filename = filename.substring(3);
            }
        }

        File f = worldEdit.getSafeSaveFile(player, dir, filename, format.getPrimaryFileExtension());

        boolean overwrite = f.exists();
        if (overwrite) {
            if (!player.hasPermission("worldedit.schematic.delete")) {
                throw new StopExecutionException(TextComponent.of("That schematic already exists!"));
            }
            if (other) {
                if (!player.hasPermission("worldedit.schematic.delete.other")) {
                    BBC.NO_PERM.send(player, "worldedit.schematic.delete.other");
                    return;
                }
            }
            if (!allowOverwrite) {
                player.printError("That schematic already exists. Use the -f flag to overwrite it.");
                return;
            }
        }

        if (f.getName().replaceAll("." + format.getPrimaryFileExtension(), "").isEmpty()) {
            File directory = f.getParentFile();
            if (directory.exists()) {
                int max = MainUtil.getMaxFileId(directory);
                f = new File(directory, max + "." + format.getPrimaryFileExtension());
            } else {
                f = new File(directory, "1." + format.getPrimaryFileExtension());
            }
        }

        // Create parent directories
        File parent = f.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs()) {
                throw new StopExecutionException(TextComponent.of(
                        "Could not create folder for schematics!"));
            }
        }
        try {
            if (!f.exists()) {
                f.createNewFile();
            } else if (!allowOverwrite) {
                BBC.SCHEMATIC_MOVE_EXISTS.send(player, f.getName());
            }

            try (FileOutputStream fos = new FileOutputStream(f)) {
                ClipboardHolder holder = session.getClipboard();
                Clipboard clipboard = holder.getClipboard();
                Transform transform = holder.getTransform();
                Clipboard target;

                // If we have a transform, bake it into the copy
                if (!transform.isIdentity()) {
                    FlattenedClipboardTransform result = FlattenedClipboardTransform.transform(clipboard, transform);
                    target = new BlockArrayClipboard(result.getTransformedRegion(), UUID.randomUUID());
                    target.setOrigin(clipboard.getOrigin());
                    Operations.completeLegacy(result.copyTo(target));
                } else {
                    target = clipboard;
                }

                URI uri = null;
                if (holder instanceof URIClipboardHolder) {
                    uri = ((URIClipboardHolder) holder).getURI(clipboard);
                }
                if (new PlayerSaveClipboardEvent(player, clipboard, uri, f.toURI()).call()) {
                    try (ClipboardWriter writer = format.getWriter(fos)) {
                        if (writer instanceof MinecraftStructure) {
                            ((MinecraftStructure) writer).write(target, player.getName());
                        } else {
                            writer.write(target);
                        }
                        log.info(player.getName() + " saved " + f.getCanonicalPath());
                        BBC.SCHEMATIC_SAVED.send(player, filename);
                    }
                } else {
                    BBC.WORLDEDIT_CANCEL_REASON_MANUAL.send(player);
                }
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            player.printError("Unknown filename: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
            player.printError("Schematic could not written: " + e.getMessage());
            log.warn("Failed to write a saved clipboard", e);
        }
    }

    @Command(
        name = "move",
        aliases = {"m"},
        desc = "Move your loaded schematic"
    )
    @CommandPermissions({"worldedit.schematic.move", "worldedit.schematic.move.other"})
    public void move(Player player, LocalSession session, String directory) throws WorldEditException, IOException {
        LocalConfiguration config = worldEdit.getConfiguration();
        File working = worldEdit.getWorkingDirectoryFile(config.saveDir);
        File dir = Settings.IMP.PATHS.PER_PLAYER_SCHEMATICS ? new File(working, player.getUniqueId().toString()) : working;
        File destDir = new File(dir, directory);
        if (!MainUtil.isInSubDirectory(working, destDir)) {
            player.printError("Directory " + destDir + " does not exist!");
            return;
        }
        if (Settings.IMP.PATHS.PER_PLAYER_SCHEMATICS && !MainUtil.isInSubDirectory(dir, destDir) && !player.hasPermission("worldedit.schematic.move.other")) {
            BBC.NO_PERM.send(player, "worldedit.schematic.move.other");
            return;
        }
        ClipboardHolder clipboard = session.getClipboard();
        List<File> sources = getFiles(clipboard);
        if (sources.isEmpty()) {
            BBC.SCHEMATIC_NONE.send(player);
            return;
        }
        if (!destDir.exists() && !destDir.mkdirs()) {
            player.printError("Creation of " + destDir + " failed! (check file permissions)");
            return;
        }
        for (File source : sources) {
            File destFile = new File(destDir, source.getName());
            if (destFile.exists()) {
                BBC.SCHEMATIC_MOVE_EXISTS.send(player, destFile);
                continue;
            }
            if (Settings.IMP.PATHS.PER_PLAYER_SCHEMATICS && (!MainUtil.isInSubDirectory(dir, destFile) || !MainUtil.isInSubDirectory(dir, source)) && !player.hasPermission("worldedit.schematic.delete.other")) {
                BBC.SCHEMATIC_MOVE_FAILED.send(player, destFile, BBC.NO_PERM.f("worldedit.schematic.move.other"));
                continue;
            }
            try {
                File cached = new File(source.getParentFile(), "." + source.getName() + ".cached");
                Files.move(source.toPath(), destFile.toPath());
                if (cached.exists()) Files.move(cached.toPath(), destFile.toPath());
                BBC.SCHEMATIC_MOVE_SUCCESS.send(player, source, destFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Command(
        name = "delete",
        aliases = {"d"},
        desc = "Delete a saved schematic"
    )
    @CommandPermissions({"worldedit.schematic.delete", "worldedit.schematic.delete.other"})
    public void delete(Actor actor, LocalSession session,
                       @Arg(desc = "File name.")
                           String filename) throws WorldEditException, IOException {
        LocalConfiguration config = worldEdit.getConfiguration();
        File working = worldEdit.getWorkingDirectoryFile(config.saveDir);
        File dir = Settings.IMP.PATHS.PER_PLAYER_SCHEMATICS ? new File(working, actor.getUniqueId().toString()) : working;
        List<File> files = new ArrayList<>();

        if (filename.equalsIgnoreCase("*")) {
            files.addAll(getFiles(session.getClipboard()));
        } else {
            File f = MainUtil.resolveRelative(new File(dir, filename));
            files.add(f);
        }

        if (files.isEmpty()) {
            BBC.SCHEMATIC_NONE.send(actor);
            return;
        }
        for (File f : files) {
            if (!MainUtil.isInSubDirectory(working, f) || !f.exists()) {
                actor.printError("Schematic " + filename + " does not exist! (" + f.exists() + "|" + f + "|" + (!MainUtil.isInSubDirectory(working, f)) + ")");
                continue;
            }
            if (Settings.IMP.PATHS.PER_PLAYER_SCHEMATICS && !MainUtil.isInSubDirectory(dir, f) && !actor.hasPermission("worldedit.schematic.delete.other")) {
                BBC.NO_PERM.send(actor, "worldedit.schematic.delete.other");
                continue;
            }
            if (!delete(f)) {
                actor.printError("Deletion of " + filename + " failed! Maybe it is read-only.");
                continue;
            }
            BBC.FILE_DELETED.send(actor, filename);
        }
    }

    private List<File> getFiles(ClipboardHolder clipboard) {
        Collection<URI> uris = Collections.emptyList();
        if (clipboard instanceof URIClipboardHolder) {
            uris = ((URIClipboardHolder) clipboard).getURIs();
        }
        List<File> files = new ArrayList<>();
        for (URI uri : uris) {
            File file = new File(uri);
            if (file.exists()) files.add(file);
        }
        return files;
    }

    @Command(
        name = "formats",
        aliases = {"listformats", "f"},
        desc = "List available formats"
    )
    @CommandPermissions("worldedit.schematic.formats")
    public void formats(Actor actor) {
        actor.print("Available clipboard formats (Name: Lookup names)");
        StringBuilder builder;
        boolean first = true;
        for (ClipboardFormat format : ClipboardFormats.getAll()) {
            builder = new StringBuilder();
            builder.append(format.getName()).append(": ");
            for (String lookupName : format.getAliases()) {
                if (!first) {
                    builder.append(", ");
                }
                builder.append(lookupName);
                first = false;
            }
            first = true;
            actor.print(builder.toString());
        }
    }


/*
    @Command(
            name = "show",
            desc = "Show a schematic",
            descFooter = "List all schematics in the schematics directory\n" +
                    " -f <format> restricts by format\n"
    )
    @CommandPermissions("worldedit.schematic.show")
    public void show(Player player, InjectedValueAccess args, @Switch(name='f', desc = "TODO") String formatName) {
        FawePlayer fp = FawePlayer.wrap(player);
        if (args.argsLength() == 0) {
            if (fp.getSession().getVirtualWorld() != null) fp.setVirtualWorld(null);
            else {
                BBC.COMMAND_SYNTAX.send(player, "/" + Commands.getAlias(SchematicCommands.class, "schematic") + " " + getCommand().aliases()[0] + " " + getCommand().usage());
            }
            return;
        }
        LocalConfiguration config = worldEdit.getConfiguration();
        File dir = worldEdit.getWorkingDirectoryFile(config.saveDir);
        try {
            SchemVis visExtent = new SchemVis(fp);
            LongAdder count = new LongAdder();
            UtilityCommands.getFiles(dir, player, args, 0, Character.MAX_VALUE, formatName, Settings.IMP.PATHS.PER_PLAYER_SCHEMATICS, file -> {
                if (file.isFile()) {
                    try {
                        visExtent.add(file);
                        count.add(1);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            long total = count.longValue();
            if (total == 0) {
                if (args.getJoinedStrings(0).toLowerCase().startsWith("all")) {
                    BBC.SCHEMATIC_NONE.send(player);
                } else {
                    String joined = args.getJoinedStrings(0);
                    String cmd = "/" + Commands.getAlias(SchematicCommands.class, "schematic") + " " + getCommand().aliases()[0] + " all " + joined;
                    BBC.HELP_SUGGEST.send(player, joined, cmd);
                }
                return;
            }
            visExtent.bind();
            visExtent.update();

            String cmdPrefix = "/" + (config.noDoubleSlash ? "" : "/");
            String cmdShow = Commands.getAlias(ClipboardCommands.class, "schematic") + " " + Commands.getAlias(ClipboardCommands.class, "show");
            BBC.SCHEMATIC_SHOW.send(fp, count.longValue(), args.getJoinedStrings(0), cmdShow);

            if (fp.getSession().getExistingClipboard() != null) {
                String cmd = cmdPrefix + Commands.getAlias(ClipboardCommands.class, "clearclipboard");
                BBC.SCHEMATIC_PROMPT_CLEAR.send(fp, cmd);
            }

        } catch (Throwable e) {
            fp.setVirtualWorld(null);
            throw e;
        }
    }
*/

    @Command(
        name = "list",
        aliases = {"all", "ls"},
        desc = "List saved schematics",
        descFooter = "Note: Format is not fully verified until loading."
    )
    @CommandPermissions("worldedit.schematic.list")
    public void list(FawePlayer fp, Actor actor, InjectedValueAccess args,
                     @ArgFlag(name = 'p', desc = "Page to view.", def = "1")
                         int page,
        @Switch(name = 'f', desc = "Restricts by format.")
            String formatName) throws WorldEditException {
        LocalConfiguration config = worldEdit.getConfiguration();
        File dir = worldEdit.getWorkingDirectoryFile(config.saveDir);

        String schemCmd = "/" + Commands.getAlias(SchematicCommands.class, "schematic");
        String loadSingle = schemCmd + " " + Commands.getAlias(SchematicCommands.class, "load");
        String loadMulti = schemCmd + " " + Commands.getAlias(SchematicCommands.class, "loadall");
        String unload = schemCmd + " " + Commands.getAlias(SchematicCommands.class, "unload");
        String delete = schemCmd + " " + Commands.getAlias(SchematicCommands.class, "delete");
        String list = schemCmd + " " + Commands.getAlias(SchematicCommands.class, "list");
        String showCmd = schemCmd + " " + Commands.getAlias(SchematicCommands.class, "show");

        URIClipboardHolder multi = as(URIClipboardHolder.class, fp.getSession().getExistingClipboard());

        final boolean hasShow = actor.hasPermission("worldedit.schematic.show");
        UtilityCommands.list(dir, actor, args, page, -1, formatName, Settings.IMP.PATHS.PER_PLAYER_SCHEMATICS, new RunnableVal3<Message, URI, String>() {
            @Override
            public void run(Message msg, URI uri, String relFilePath) {
                boolean isDir = false;
                boolean loaded = multi != null && multi.contains(uri);

                String name = relFilePath;
                String uriStr = uri.toString();
                if (uriStr.startsWith("file:/")) {
                    File file = new File(uri.getPath());
                    name = file.getName();
                    try {
                        if (!MainUtil.isInSubDirectory(dir, file)) {
                            throw new RuntimeException(new CommandException("Invalid path"));
                        }
                    } catch (IOException ignore) {}
                    if (file.isDirectory()) {
                        isDir = true;
                    } else if (name.indexOf('.') != -1) {
                        name = name.substring(0, name.lastIndexOf('.'));
                    }
                }  // url

                msg.text(" - ");

                if (msg.supportsInteraction()) {
                    if (loaded) {
                        msg.text("[-]").command(unload + " " + relFilePath).tooltip("Unload");
                    } else {
                        msg.text("[+]").command(loadMulti + " " + relFilePath).tooltip("Add to clipboard");
                    }
                    if (!isDir) msg.text("[X]").suggest("/" + delete + " " + relFilePath).tooltip("Delete");
                    else if (hasShow) msg.text("[O]").command(showCmd + " " + args.getJoinedStrings(0) + " " + relFilePath).tooltip("Show");
                    msg.text(name);
                    if (isDir) {
                        msg.command(list + " " + relFilePath).tooltip("List");
                    } else {
                        msg.command(loadSingle + " " + relFilePath).tooltip("Load");
                    }
                } else {
                    msg.text(name);
                }
            }
        });
    }

    private static class SchematicLoadTask implements Callable<ClipboardHolder> {
        private final Player player;
        private final File file;
        private final ClipboardFormat format;

        SchematicLoadTask(Player player, File file, ClipboardFormat format) {
            this.player = player;
            this.file = file;
            this.format = format;
        }

        @Override
        public ClipboardHolder call() throws Exception {
            try (Closer closer = Closer.create()) {
                FileInputStream fis = closer.register(new FileInputStream(file));
                BufferedInputStream bis = closer.register(new BufferedInputStream(fis));
                ClipboardReader reader = closer.register(format.getReader(bis));

                Clipboard clipboard = reader.read();
                log.info(player.getName() + " loaded " + file.getCanonicalPath());
                return new ClipboardHolder(clipboard);
            }
        }
    }

    private static class SchematicSaveTask implements Callable<Void> {
        private final Player player;
        private final File file;
        private final ClipboardFormat format;
        private final ClipboardHolder holder;
        private final boolean overwrite;

        SchematicSaveTask(Player player, File file, ClipboardFormat format, ClipboardHolder holder, boolean overwrite) {
            this.player = player;
            this.file = file;
            this.format = format;
            this.holder = holder;
            this.overwrite = overwrite;
        }

        @Override
        public Void call() throws Exception {
            Clipboard clipboard = holder.getClipboard();
            Transform transform = holder.getTransform();
            Clipboard target;

            // If we have a transform, bake it into the copy
            if (transform.isIdentity()) {
                target = clipboard;
            } else {
                FlattenedClipboardTransform result = FlattenedClipboardTransform.transform(clipboard, transform);
                target = new BlockArrayClipboard(result.getTransformedRegion());
                target.setOrigin(clipboard.getOrigin());
                Operations.completeLegacy(result.copyTo(target));
            }

            try (Closer closer = Closer.create()) {
                FileOutputStream fos = closer.register(new FileOutputStream(file));
                BufferedOutputStream bos = closer.register(new BufferedOutputStream(fos));
                ClipboardWriter writer = closer.register(format.getWriter(bos));
                writer.write(target);

                log.info(player.getName() + " saved " + file.getCanonicalPath() + (overwrite ? " (overwriting previous file)" : ""));
            }
            return null;
        }
    }

    private static class SchematicListTask implements Callable<Component> {
        private final String prefix;
        private final int sortType;
        private final int page;
        private final File rootDir;
        private final String pageCommand;

        SchematicListTask(String prefix, int sortType, int page, String pageCommand) {
            this.prefix = prefix;
            this.sortType = sortType;
            this.page = page;
            this.rootDir = WorldEdit.getInstance().getWorkingDirectoryFile(prefix);
            this.pageCommand = pageCommand;
        }

        @Override
        public Component call() throws Exception {
            List<File> fileList = allFiles(rootDir);

            if (fileList == null || fileList.isEmpty()) {
                return ErrorFormat.wrap("No schematics found.");
            }

            File[] files = new File[fileList.size()];
            fileList.toArray(files);
            // cleanup file list
            Arrays.sort(files, (f1, f2) -> {
                // http://stackoverflow.com/questions/203030/best-way-to-list-files-in-java-sorted-by-date-modified
                int res;
                if (sortType == 0) { // use name by default
                    int p = f1.getParent().compareTo(f2.getParent());
                    if (p == 0) { // same parent, compare names
                        res = f1.getName().compareTo(f2.getName());
                    } else { // different parent, sort by that
                        res = p;
                    }
                } else {
                    res = Long.compare(f1.lastModified(), f2.lastModified()); // use date if there is a flag
                    if (sortType == 1) res = -res; // flip date for newest first instead of oldest first
                }
                return res;
            });

            PaginationBox paginationBox = new SchematicPaginationBox(prefix, files, pageCommand);
            return paginationBox.create(page);
        }
    }

    private static List<File> allFiles(File root) {
        File[] files = root.listFiles();
        if (files == null) return null;
        List<File> fileList = new ArrayList<>();
        for (File f : files) {
            if (f.isDirectory()) {
                List<File> subFiles = allFiles(f);
                if (subFiles == null) continue; // empty subdir
                fileList.addAll(subFiles);
            } else {
                fileList.add(f);
            }
        }
        return fileList;
    }

    private boolean delete(File file) {
        if (file.delete()) {
            new File(file.getParentFile(), "." + file.getName() + ".cached").delete();
            return true;
        }
        return false;
    }
}
