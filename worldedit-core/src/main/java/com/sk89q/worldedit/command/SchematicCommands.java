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

import javax.annotation.Nullable;
import com.boydti.fawe.config.Caption;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.clipboard.MultiClipboardHolder;
import com.boydti.fawe.object.clipboard.URIClipboardHolder;
import com.boydti.fawe.object.schematic.MinecraftStructure;
import com.boydti.fawe.util.MainUtil;
import com.google.common.base.Function;
import com.google.common.collect.Multimap;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.argument.Arguments;
import com.sk89q.worldedit.command.util.AsyncCommandBuilder;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.extent.ActorSaveClipboardEvent;
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
import com.sk89q.worldedit.util.formatting.component.MessageBox;
import com.sk89q.worldedit.util.formatting.component.PaginationBox;
import com.sk89q.worldedit.util.formatting.component.TextComponentProducer;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.sk89q.worldedit.util.formatting.text.event.ClickEvent;
import com.sk89q.worldedit.util.formatting.text.event.HoverEvent;
import com.sk89q.worldedit.util.formatting.text.format.TextColor;
import com.sk89q.worldedit.util.io.Closer;
import com.sk89q.worldedit.util.io.file.FilenameException;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.ArgFlag;
import org.enginehub.piston.annotation.param.Switch;
import org.enginehub.piston.exception.StopExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import static com.boydti.fawe.util.ReflectionUtils.as;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

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

    //TODO filtering for directories, global, and private scheamtics needs to be reimplemented here
    private static List<File> getFiles(File root, String filter, ClipboardFormat format) {
        File[] files = root.listFiles();
        if (files == null) {
            return null;
        }
        //Only get the files that match the format parameter
        if (format != null) {
            files = Arrays.stream(files).filter(format::isFormat).toArray(File[]::new);
        }
        List<File> fileList = new ArrayList<>();
        for (File f : files) {
            if (f.isDirectory()) {
                List<File> subFiles = getFiles(f, filter, format);
                if (subFiles == null) {
                    continue; // empty subdir
                }
                fileList.addAll(subFiles);
            } else {
                fileList.add(f);
            }
        }
        return fileList;
    }

    @Command(
        name = "loadall",
        desc = "Load multiple clipboards (paste will randomly choose one)"
    )
    @Deprecated
    @CommandPermissions({"worldedit.clipboard.load", "worldedit.schematic.load", "worldedit.schematic.load.web", "worldedit.schematic.load.asset"})
    public void loadall(Player player, LocalSession session,
                        @Arg(desc = "Format name.", def = "schematic")
                            String formatName,
                        @Arg(desc = "File name.")
                            String filename,
                        @Switch(name = 'r', desc = "Apply random rotation")
                            boolean randomRotate) throws FilenameException {
        final ClipboardFormat format = ClipboardFormats.findByAlias(formatName);
        if (format == null) {
            player.print(Caption.of("fawe.worldedit.clipboard.clipboard.invalid.format", formatName));
            return;
        }
        try {
            MultiClipboardHolder all = ClipboardFormats.loadAllFromInput(player, filename, null, true);
            if (all != null) {
                session.addClipboard(all);
                player.print(Caption.of("fawe.worldedit.schematic.schematic.loaded", filename));
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
        player.print(TranslatableComponent.of("fawe.worldedit.clipboard.clipboard.cleared"));
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
                    if (multi.getHolders().isEmpty()) {
                        session.setClipboard(null);
                    }
                } else {
                    session.setClipboard(null);
                }
                player.print(TranslatableComponent.of("fawe.worldedit.clipboard.clipboard.cleared"));
                return;
            }
        }
        player.print(Caption.of("fawe.worldedit.clipboard.clipboard.uri.not.found", fileName));
    }

    @Command(
        name = "load",
        desc = "Load a schematic into your clipboard"
    )
    @CommandPermissions({"worldedit.clipboard.load", "worldedit.schematic.load", "worldedit.schematic.load.asset", "worldedit.schematic.load.web", "worldedit.schematic.load.other"})
    public void load(Actor actor, LocalSession session,
                     @Arg(desc = "File name.")
                         String filename,
                     @Arg(desc = "Format name.", def = "sponge")
                         String formatName) throws FilenameException {
        LocalConfiguration config = worldEdit.getConfiguration();

        ClipboardFormat format = null;
        InputStream in = null;
        try {
            URI uri;
            if (formatName.startsWith("url:")) {
                String t = filename;
                filename = formatName;
                formatName = t;
            }
            if (filename.startsWith("url:")) {
                if (!actor.hasPermission("worldedit.schematic.load.web")) {
                    actor.print(Caption.of("fawe.error.no-perm", "worldedit.schematic.load.web"));
                    return;
                }
                UUID uuid = UUID.fromString(filename.substring(4));
                URL webUrl = new URL(Settings.IMP.WEB.URL);
                format = ClipboardFormats.findByAlias(formatName);
                URL url = new URL(webUrl, "uploads/" + uuid + "." + format.getPrimaryFileExtension());
                ReadableByteChannel byteChannel = Channels.newChannel(url.openStream());
                in = Channels.newInputStream(byteChannel);
                uri = url.toURI();
            } else {
                File saveDir = worldEdit.getWorkingDirectoryFile(config.saveDir);
                File dir = Settings.IMP.PATHS.PER_PLAYER_SCHEMATICS ? new File(saveDir, actor.getUniqueId().toString()) : saveDir;
                File file;
                if (filename.startsWith("#")) {
                    format = ClipboardFormats.findByAlias(formatName);
                    String[] extensions;
                    if (format != null) {
                        extensions = format.getFileExtensions().toArray(new String[0]);
                    } else {
                        extensions = ClipboardFormats.getFileExtensionArray();
                    }
                    file = actor.openFileOpenDialog(extensions);
                    if (file == null || !file.exists()) {
                        actor.printError(TranslatableComponent.of("worldedit.schematic.load.does-not-exist", TextComponent.of(filename)));
                        return;
                    }
                } else {
                    if (Settings.IMP.PATHS.PER_PLAYER_SCHEMATICS && !actor.hasPermission("worldedit.schematic.load.other") && Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}").matcher(filename).find()) {
                        actor.print(Caption.of("fawe.error.no-perm", "worldedit.schematic.load.other"));
                        return;
                    }
                    if (filename.matches(".*\\.[\\w].*")) {
                        format = ClipboardFormats
                            .findByExtension(filename.substring(filename.lastIndexOf('.') + 1));
                    } else {
                        format = ClipboardFormats.findByAlias(formatName);
                    }
                    file = MainUtil.resolve(dir, filename, format, false);
                }
                if (file == null || !file.exists()) {
                    if (!filename.contains("../")) {
                        dir = this.worldEdit.getWorkingDirectoryFile(config.saveDir);
                        file = MainUtil.resolve(dir, filename, format, false);
                    }
                }
                if (file == null || !file.exists() || !MainUtil.isInSubDirectory(saveDir, file)) {
                    actor.printError("Schematic " + filename + " does not exist! (" + (file != null && file.exists()) + "|" + file + "|" + (file != null && !MainUtil.isInSubDirectory(saveDir, file)) + ")");
                    return;
                }
                if (format == null) {
                    format = ClipboardFormats.findByFile(file);
                    if (format == null) {
                        actor.printError(TranslatableComponent.of("worldedit.schematic.unknown-format", TextComponent.of(formatName)));
                        return;
                    }
                }
                in = new FileInputStream(file);
                uri = file.toURI();
            }
            format.hold(actor, uri, in);
            actor.print(Caption.of("fawe.worldedit.schematic.schematic.loaded", filename));
        } catch (IllegalArgumentException e) {
            actor.printError("Unknown filename: " + filename);
        } catch (URISyntaxException | IOException e) {
            actor.printError("File could not be read or it does not exist: " + e.getMessage());
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
    public void save(Actor actor, LocalSession session,
                     @Arg(desc = "File name.")
                         String filename,
                     @Arg(desc = "Format name.", def = "sponge")
                         String formatName,
                     @Switch(name = 'f', desc = "Overwrite an existing file.")
                         boolean allowOverwrite,
                     @Switch(name = 'g', desc = "//TODO")
                         boolean global) throws WorldEditException {
        LocalConfiguration config = worldEdit.getConfiguration();

        File dir = worldEdit.getWorkingDirectoryFile(config.saveDir);

        if (!global && Settings.IMP.PATHS.PER_PLAYER_SCHEMATICS) {
            dir = new File(dir, actor.getUniqueId().toString());
        }

        ClipboardFormat format = ClipboardFormats.findByAlias(formatName);
        if (format == null) {
            actor.printError(TranslatableComponent.of("worldedit.schematic.unknown-format", TextComponent.of(formatName)));
            return;
        }

        boolean other = false;
        if (filename.contains("../")) {
            other = true;
            if (!actor.hasPermission("worldedit.schematic.save.other")) {
                actor.print(Caption.of("fawe.error.no-perm", "worldedit.schematic.save.other"));
                return;
            }
            if (filename.startsWith("../")) {
                dir = worldEdit.getWorkingDirectoryFile(config.saveDir);
                filename = filename.substring(3);
            }
        }

        File f = worldEdit.getSafeSaveFile(actor, dir, filename, format.getPrimaryFileExtension());

        boolean overwrite = f.exists();
        if (overwrite) {
            if (!actor.hasPermission("worldedit.schematic.delete")) {
                throw new StopExecutionException(TextComponent.of("That schematic already exists!"));
            }
            if (other) {
                if (!actor.hasPermission("worldedit.schematic.delete.other")) {
                    actor.print(Caption.of("fawe.error.no-perm", "worldedit.schematic.delete.other"));
                    return;
                }
            }
            if (!allowOverwrite) {
                actor.printError(TranslatableComponent.of("worldedit.schematic.save.already-exists"));
                return;
            }
        }

        // Create parent directories
        File parent = f.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs()) {
                throw new StopExecutionException(TranslatableComponent.of(
                    "worldedit.schematic.save.failed-directory"));
            }
        }

        ClipboardHolder holder = session.getClipboard();

        SchematicSaveTask task = new SchematicSaveTask(actor, f, format, holder, overwrite);
        AsyncCommandBuilder.wrap(task, actor)
            .registerWithSupervisor(worldEdit.getSupervisor(), "Saving schematic " + filename)
            .sendMessageAfterDelay(TranslatableComponent.of("worldedit.schematic.save.saving"))
            .onFailure("Failed to save schematic", worldEdit.getPlatformManager().getPlatformCommandManager().getExceptionConverter())
            .buildAndExec(worldEdit.getExecutorService());
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
            player.print(Caption.of("fawe.error.no-perm", "worldedit.schematic.move.other"));
            return;
        }
        ClipboardHolder clipboard = session.getClipboard();
        List<File> sources = getFiles(clipboard);
        if (sources.isEmpty()) {
            player.printError(TranslatableComponent.of("fawe.worldedit.schematic.schematic.none"));
            return;
        }
        if (!destDir.exists() && !destDir.mkdirs()) {
            player.printError("Creation of " + destDir + " failed! (check file permissions)");
            return;
        }
        for (File source : sources) {
            File destFile = new File(destDir, source.getName());
            if (destFile.exists()) {
                player.print(Caption.of("fawe.worldedit.schematic.schematic.move.exists", destFile));
                continue;
            }
            if (Settings.IMP.PATHS.PER_PLAYER_SCHEMATICS && (!MainUtil.isInSubDirectory(dir, destFile) || !MainUtil.isInSubDirectory(dir, source)) && !player.hasPermission("worldedit.schematic.delete.other")) {
                player.print(Caption.of("fawe.worldedit.schematic.schematic.move.failed", destFile,
                    Caption.of("fawe.error.no-perm", ("worldedit.schematic.move.other"))));
                continue;
            }
            try {
                File cached = new File(source.getParentFile(), "." + source.getName() + ".cached");
                Files.move(source.toPath(), destFile.toPath());
                if (cached.exists()) {
                    Files.move(cached.toPath(), destFile.toPath());
                }
                player.print(Caption.of("fawe.worldedit.schematic.schematic.move.success", source, destFile));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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
            if (file.exists()) {
                files.add(file);
            }
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
        actor.printInfo(TranslatableComponent.of("worldedit.schematic.formats.title"));
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
            actor.printInfo(TextComponent.of(builder.toString()));
        }
    }

    @Command(
        name = "list",
        aliases = {"all", "ls"},
        desc = "List saved schematics",
        descFooter = "Note: Format is not fully verified until loading."
    )
    @CommandPermissions("worldedit.schematic.list")
    public void list(Actor actor, LocalSession session,
                     @ArgFlag(name = 'p', desc = "Page to view.", def = "1")
                         int page,
                     @Switch(name = 'd', desc = "Sort by date, oldest first")
                         boolean oldFirst,
                     @Switch(name = 'n', desc = "Sort by date, newest first")
                         boolean newFirst,
                     @ArgFlag(name = 'f', desc = "Restricts by format.", def = "")
                         String formatName,
                     @Arg(name = "filter", desc = "Filter for schematics", def = "all")
                         String filter, Arguments arguments) throws WorldEditException {
        if (oldFirst && newFirst) {
            throw new StopExecutionException(TextComponent.of("Cannot sort by oldest and newest."));
        }
        String pageCommand = "/" + arguments.get();
        LocalConfiguration config = worldEdit.getConfiguration();
        File dir = worldEdit.getWorkingDirectoryFile(config.saveDir);

        String schemCmd = "//schematic";
        String loadSingle = schemCmd + " load";
        String loadMulti = schemCmd + " loadall";
        String unload = schemCmd + " unload";
        String delete = schemCmd + " delete";
        String list = schemCmd + " list";
        String showCmd = schemCmd + " show";

        List<String> args = filter.isEmpty() ? Collections.emptyList() : Arrays.asList(filter.split(" "));

        URIClipboardHolder multi = as(URIClipboardHolder.class, session.getExistingClipboard());

        final boolean hasShow = false;

        //If player forgot -p argument
        boolean playerFolder = Settings.IMP.PATHS.PER_PLAYER_SCHEMATICS;
        UUID uuid = playerFolder ? actor.getUniqueId() : null;
        List<File> files = UtilityCommands.getFiles(dir, actor, args, formatName, playerFolder, oldFirst, newFirst);
        List<Map.Entry<URI, String>> entries = UtilityCommands.filesToEntry(dir, files, uuid);

        Function<URI, Boolean> isLoaded = multi == null ? f -> false : multi::contains;

        List<Component> components = UtilityCommands.entryToComponent(dir, entries, isLoaded,
            (name, path, type, loaded) -> {
                TextColor color = TextColor.GRAY;
                switch (type) {
                    case URL:
                        color = TextColor.DARK_GRAY;
                        break;
                    case FILE:
                        color = TextColor.GREEN;
                        break;
                    case DIRECTORY:
                        color = TextColor.GOLD;
                        break;
                }

                TextComponentProducer msg = new TextComponentProducer();

                msg.append(TextComponent.of(" - ", color));

                if (loaded) {
                    msg.append(TextComponent.of("[-]", TextColor.RED)
                        .clickEvent(ClickEvent.of(ClickEvent.Action.SUGGEST_COMMAND, unload + " " + path))
                        .hoverEvent(HoverEvent.of(HoverEvent.Action.SHOW_TEXT, TextComponent.of("Unload"))));
                } else {
                    msg.append(TextComponent.of("[+]", TextColor.GREEN)
                        .clickEvent(ClickEvent.of(ClickEvent.Action.SUGGEST_COMMAND, loadMulti + " " + path))
                        .hoverEvent(HoverEvent.of(HoverEvent.Action.SHOW_TEXT, TextComponent.of("Add to clipboard"))));
                }
                if (type != UtilityCommands.URIType.DIRECTORY) {
                    msg.append(TextComponent.of("[X]", TextColor.DARK_RED)
                        .clickEvent(ClickEvent.of(ClickEvent.Action.SUGGEST_COMMAND, delete + " " + path))
                        .hoverEvent(HoverEvent.of(HoverEvent.Action.SHOW_TEXT, TextComponent.of("delete")))
                    );
                } else if (hasShow) {
                    msg.append(TextComponent.of("[O]", TextColor.DARK_AQUA)
                        .clickEvent(ClickEvent.of(ClickEvent.Action.SUGGEST_COMMAND, showCmd + " " + path))
                        .hoverEvent(HoverEvent.of(HoverEvent.Action.SHOW_TEXT, TextComponent.of("visualize")))
                    );
                }
                TextComponent msgElem = TextComponent.of(name, color);
                if (type != UtilityCommands.URIType.DIRECTORY) {
                    msgElem = msgElem.clickEvent(ClickEvent.of(ClickEvent.Action.SUGGEST_COMMAND, loadSingle + " " + path));
                    msgElem = msgElem.hoverEvent(HoverEvent.of(HoverEvent.Action.SHOW_TEXT, TextComponent.of("Load")));
                } else {
                    msgElem = msgElem.clickEvent(ClickEvent.of(ClickEvent.Action.SUGGEST_COMMAND, list + " " + path));
                    msgElem = msgElem.hoverEvent(HoverEvent.of(HoverEvent.Action.SHOW_TEXT, TextComponent.of("List")));
                }
                msg.append(msgElem);

                if (type == UtilityCommands.URIType.FILE) {
                    long filesize = 0;
                    try {
                        filesize = Files.size(Paths.get(dir.getAbsolutePath() + File.separator
                            + (playerFolder ? (uuid.toString() + File.separator) : "") + path));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    TextComponent sizeElem = TextComponent.of(String.format(" (%.1f kb)", filesize / 1000.0), TextColor.GRAY);
                    msg.append(sizeElem);
                }
                return msg.create();
            });

        long total_bytes = 0;
        File parent_dir = new File(dir.getAbsolutePath() + (playerFolder ? File.separator + uuid.toString() : ""));
        try {
            for (File schem : parent_dir.listFiles()) {
                if (schem.getName().endsWith(".schem") || schem.getName().endsWith(".schematic")) {
                    total_bytes += Files.size(Paths.get(schem.getAbsolutePath()));
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        String header_bytes_elem = String.format("%.1fkb", total_bytes / 1000.0);

        if (Settings.IMP.PATHS.PER_PLAYER_SCHEMATICS && Settings.IMP.EXPERIMENTAL.PER_PLAYER_FILE_SIZE_LIMIT > -1) {
            header_bytes_elem += String.format(" / %dkb",
                Settings.IMP.EXPERIMENTAL.PER_PLAYER_FILE_SIZE_LIMIT );
        }

        String full_header = "| Schematics: " + header_bytes_elem + " |";
        PaginationBox paginationBox = PaginationBox.fromComponents(full_header, pageCommand, components);
        actor.print(paginationBox.create(page));
    }

    @Command(
        name = "delete",
        aliases = {"d"},
        desc = "Delete a saved schematic"
    )
    @CommandPermissions("worldedit.schematic.delete")
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
            actor.printError(TranslatableComponent.of("worldedit.schematic.delete.does-not-exist", TextComponent.of(filename)));
            return;
        }
        for (File f : files) {
            if (!MainUtil.isInSubDirectory(working, f) || !f.exists()) {
                actor.printError(TranslatableComponent.of("worldedit.schematic.delete.does-not-exist", TextComponent.of(filename)));
                continue;
            }
            if (Settings.IMP.PATHS.PER_PLAYER_SCHEMATICS && !MainUtil.isInSubDirectory(dir, f) && !actor.hasPermission("worldedit.schematic.delete.other")) {
                actor.print(Caption.of("fawe.error.no-perm", "worldedit.schematic.delete.other"));
                continue;
            }
            if (!deleteFile(f)) {
                actor.printError(TranslatableComponent.of("worldedit.schematic.delete.failed", TextComponent.of(filename)));
                continue;
            }
            actor.print(Caption.of("worldedit.schematic.delete.deleted", filename));
        }
    }

    private boolean deleteFile(File file) {
        if (file.delete()) {
            new File(file.getParentFile(), "." + file.getName() + ".cached").delete();
            return true;
        }
        return false;
    }

    private static class SchematicLoadTask implements Callable<ClipboardHolder> {
        private final Actor actor;
        private final ClipboardFormat format;
        private final File file;

        SchematicLoadTask(Actor actor, File file, ClipboardFormat format) {
            this.actor = actor;
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
                log.info(actor.getName() + " loaded " + file.getCanonicalPath());
                return new ClipboardHolder(clipboard);
            }
        }
    }

    private static class SchematicSaveTask implements Callable<Void> {
        private final Actor actor;
        private final ClipboardFormat format;
        private final ClipboardHolder holder;
        private final boolean overwrite;
        private File file;

        SchematicSaveTask(Actor actor, File file, ClipboardFormat format, ClipboardHolder holder, boolean overwrite) {
            this.actor = actor;
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

            boolean check_filesize = false;

            if (Settings.IMP.PATHS.PER_PLAYER_SCHEMATICS
                && Settings.IMP.EXPERIMENTAL.PERPLAYER_FILESIZELIMIT > -1) {
                check_filesize = true;
            }

            double directorysize_kb = 0;
            String cur_filepath = file.getAbsolutePath();
            final String SCHEMATIC_NAME = file.getName();

            double overwrite_old_kb = 0;
            String overwrite_path = cur_filepath;

            if (check_filesize) {
                File parent_dir = new File(file.getParent());

                for (File child : parent_dir.listFiles()) {
                    if (child.getName().endsWith(".schem") || child.getName().endsWith(".schematic")) {
                        directorysize_kb += Files.size(Paths.get(child.getAbsolutePath())) / 1000.0;
                    }
                }


                if (overwrite) {
                    overwrite_old_kb = Files.size(Paths.get(file.getAbsolutePath())) / 1000.0;
                    int iter = 1;
                    while (new File(overwrite_path + "." + iter + "." + format.getPrimaryFileExtension()).exists()) {
                        iter++;
                    }
                    file = new File(overwrite_path + "." + iter + "." + format.getPrimaryFileExtension());
                }
            }


            if (Settings.IMP.PATHS.PER_PLAYER_SCHEMATICS && Settings.IMP.EXPERIMENTAL.PER_PLAYER_FILE_NUM_LIMIT > -1) {

                int cur_files = new File(file.getParent()).listFiles().length;
                int limit = Settings.IMP.EXPERIMENTAL.PER_PLAYER_FILE_NUM_LIMIT ;

                if (cur_files >= limit) {

                    TextComponent no_slots_err = TextComponent.of( //TODO - to be moved into captions/translatablecomponents
                        "You have " + String.format("You have " + cur_files + "/" + limit + " saved schematics. Delete some to save this one!",
                            TextColor.RED));
                    actor.printError(no_slots_err);

                    log.info(actor.getName() + " failed to save " + file.getCanonicalPath() + " - too many schematics!");
                    return null;
                }
            }

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
                URI uri = null;
                if (holder instanceof URIClipboardHolder) {
                    uri = ((URIClipboardHolder) holder).getURI(clipboard);
                }
                if (new ActorSaveClipboardEvent(actor, clipboard, uri, file.toURI()).call()) {
                    if (writer instanceof MinecraftStructure) {
                        ((MinecraftStructure) writer).write(target, actor.getName());
                    } else {
                        writer.write(target);
                    }

                    closer.close(); // release the new .schem file so that its size can be measured
                    double filesize_kb = Files.size(Paths.get(file.getAbsolutePath())) / 1000.0;

                    TextComponent filesize_notif = TextComponent.of( //TODO - to be moved into captions/translatablecomponents
                        SCHEMATIC_NAME + " size: " + String.format("%.1f", filesize_kb) + "kb", TextColor.GRAY);
                    actor.print(filesize_notif);

                    if (check_filesize) {

                        double cur_kb = filesize_kb + directorysize_kb;
                        int allocated_kb = Settings.IMP.EXPERIMENTAL.PERPLAYER_FILESIZELIMIT;

                        if (overwrite) {
                            cur_kb -= overwrite_old_kb;
                        }

                        if ((cur_kb) > allocated_kb) {
                            file.delete();
                            TextComponent no_kb_err = TextComponent.of( //TODO - to be moved into captions/translatablecomponents
                                "You're about to be at " + String.format("%.1f", cur_kb) + "kb of schematics. ("
                                    + String.format("%dkb", allocated_kb) + " available) Delete some first to save this one!",
                                TextColor.RED);
                            actor.printError(no_kb_err);
                            log.info(actor.getName() + " failed to save " + SCHEMATIC_NAME + " - not enough space!");
                            return null;
                        }

                        if (overwrite) {
                            new File(cur_filepath).delete();
                            file.renameTo(new File(cur_filepath));
                        }
                        TextComponent kb_left_notif = TextComponent.of( //TODO - to be moved into captions/translatablecomponents
                            "You have " + String.format("%.1f", (allocated_kb - cur_kb)) + "kb left for schematics.", TextColor.GRAY);
                        actor.print(kb_left_notif);
                    }

                    if (Settings.IMP.PATHS.PER_PLAYER_SCHEMATICS && Settings.IMP.EXPERIMENTAL.PER_PLAYER_FILE_NUM_LIMIT > -1) {
                        int cur_files = new File(file.getParent()).listFiles().length;

                        TextComponent slots_left_notif = TextComponent.of( //TODO - to be moved into captions/translatablecomponents
                            "You have " + (Settings.IMP.EXPERIMENTAL.PER_PLAYER_FILE_NUM_LIMIT - cur_files)
                                + " schematic file slots left.", TextColor.GRAY);
                        actor.print(slots_left_notif);
                    }

                    log.info(actor.getName() + " saved " + file.getCanonicalPath());
                    actor.print(Caption.of("fawe.worldedit.schematic.schematic.saved", SCHEMATIC_NAME));
                } else {
                    actor.printError(TranslatableComponent.of("fawe.cancel.worldedit.cancel.reason.manual"));
                }
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
        private final String filter;
        private final String formatName;

        SchematicListTask(String prefix, int sortType, int page, String pageCommand,
                          String filter, String formatName) {
            this.prefix = prefix;
            this.sortType = sortType;
            this.page = page;
            this.rootDir = WorldEdit.getInstance().getWorkingDirectoryFile(prefix);
            this.pageCommand = pageCommand;
            this.filter = filter;
            this.formatName = formatName;
        }

        @Override
        public Component call() throws Exception {
            ClipboardFormat format = ClipboardFormats.findByAlias(formatName);
            List<File> fileList = getFiles(rootDir, filter, format);

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
                    if (sortType == 1) {
                        res = -res; // flip date for newest first instead of oldest first
                    }
                }
                return res;
            });

            PaginationBox paginationBox = new SchematicPaginationBox(prefix, files, pageCommand);
            return paginationBox.create(page);
        }
    }

    private static class SchematicPaginationBox extends PaginationBox {
        private final String prefix;
        private final File[] files;

        SchematicPaginationBox(String rootDir, File[] files, String pageCommand) {
            super("Available schematics", pageCommand);
            this.prefix = rootDir == null ? "" : rootDir;
            this.files = files;
        }

        @Override
        public Component getComponent(int number) {
            checkArgument(number < files.length && number >= 0);
            File file = files[number];
            Multimap<String, ClipboardFormat> exts = ClipboardFormats.getFileExtensionMap();
            String format = exts.get(com.google.common.io.Files.getFileExtension(file.getName()))
                .stream().findFirst().map(ClipboardFormat::getName).orElse("Unknown");
            boolean inRoot = file.getParentFile().getName().equals(prefix);

            String path = inRoot ? file.getName() : file.getPath().split(Pattern.quote(prefix + File.separator))[1];

            return TextComponent.builder()
                .content("")
                .append(TextComponent.of("[L]")
                    .color(TextColor.GOLD)
                    .clickEvent(ClickEvent
                        .of(ClickEvent.Action.RUN_COMMAND, "/schem load \"" + path + "\""))
                    .hoverEvent(HoverEvent.of(HoverEvent.Action.SHOW_TEXT, TextComponent.of("Click to load"))))
                .append(TextComponent.space())
                .append(TextComponent.of(path)
                    .color(TextColor.DARK_GREEN)
                    .hoverEvent(HoverEvent.of(HoverEvent.Action.SHOW_TEXT, TextComponent.of(format))))
                .build();
        }

        @Override
        public int getComponentsSize() {
            return files.length;
        }
    }

}
