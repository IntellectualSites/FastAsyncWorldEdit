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

import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.event.extent.ActorSaveClipboardEvent;
import com.fastasyncworldedit.core.extent.clipboard.MultiClipboardHolder;
import com.fastasyncworldedit.core.extent.clipboard.URIClipboardHolder;
import com.fastasyncworldedit.core.extent.clipboard.io.schematic.MinecraftStructure;
import com.fastasyncworldedit.core.util.MainUtil;
import com.google.common.collect.Multimap;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.argument.Arguments;
import com.sk89q.worldedit.command.util.AsyncCommandBuilder;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.extent.clipboard.io.share.ClipboardShareDestination;
import com.sk89q.worldedit.extent.clipboard.io.share.ClipboardShareMetadata;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.formatting.component.ErrorFormat;
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
import org.apache.logging.log4j.Logger;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.ArgFlag;
import org.enginehub.piston.annotation.param.Switch;
import org.enginehub.piston.exception.StopExecutionException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.regex.Pattern;

import static com.fastasyncworldedit.core.util.ReflectionUtils.as;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Commands that work with schematic files.
 */
@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class SchematicCommands {

    private static final Logger LOGGER = LogManagerCompat.getLogger();
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

    //FAWE start
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
    public void loadall(
            Actor actor, LocalSession session,
            @Arg(desc = "Format name.", def = "fast")
                    String formatName,
            @Arg(desc = "File name.")
                    String filename,
            @Switch(name = 'o', desc = "Overwrite/replace existing clipboard(s)")
                    boolean overwrite
//            @Switch(name = 'r', desc = "Apply random rotation") <- not implemented below.
//                    boolean randomRotate
    ) throws FilenameException {
        final ClipboardFormat format = ClipboardFormats.findByAlias(formatName);
        if (format == null) {
            actor.print(Caption.of("fawe.worldedit.clipboard.clipboard.invalid.format", formatName));
            return;
        }
        try {
            MultiClipboardHolder all = ClipboardFormats.loadAllFromInput(actor, filename, null, true);
            if (all != null) {
                if (overwrite) {
                    session.setClipboard(all);
                } else {
                    session.addClipboard(all);
                }
                actor.print(Caption.of("fawe.worldedit.schematic.schematic.loaded", filename));
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
    public void clear(Actor actor, LocalSession session) throws WorldEditException {
        session.setClipboard(null);
        actor.print(Caption.of("fawe.worldedit.clipboard.clipboard.cleared"));
    }

    @Command(
            name = "unload",
            desc = "Remove a clipboard from your multi-clipboard"
    )
    @CommandPermissions({"worldedit.clipboard.clear", "worldedit.schematic.clear"})
    public void unload(
            Actor actor, LocalSession session,
            @Arg(desc = "File name, requires extension.")
                    String fileName
    ) throws WorldEditException {
        URI uri;
        if (fileName.startsWith("file:/") || fileName.startsWith("http://") || fileName.startsWith("https://")) {
            uri = URI.create(fileName);
        } else {
            final LocalConfiguration config = this.worldEdit.getConfiguration();
            File working = this.worldEdit.getWorkingDirectoryPath(config.saveDir).toFile();
            File root = Settings.settings().PATHS.PER_PLAYER_SCHEMATICS ? new File(working, actor.getUniqueId().toString()) : working;
            uri = new File(root, fileName).toURI();
        }

        ClipboardHolder clipboard = session.getClipboard();
        if (clipboard instanceof URIClipboardHolder identifiable) {
            if (identifiable.contains(uri)) {
                if (identifiable instanceof MultiClipboardHolder multi) {
                    multi.remove(uri);
                    if (multi.getHolders().isEmpty()) {
                        session.setClipboard(null);
                    }
                } else {
                    session.setClipboard(null);
                }
                actor.print(Caption.of("fawe.worldedit.clipboard.clipboard.cleared"));
                return;
            }
        }
        actor.print(Caption.of("fawe.worldedit.clipboard.clipboard.uri.not.found", fileName));
    }

    //FAWE start
    @Command(
            name = "move",
            aliases = {"m"},
            desc = "Move your loaded schematic"
    )
    @CommandPermissions({"worldedit.schematic.move", "worldedit.schematic.move.other"})
    public void move(Actor actor, LocalSession session, @Arg(desc = "Directory.") String directory) throws WorldEditException,
            IOException {
        LocalConfiguration config = worldEdit.getConfiguration();
        File working = worldEdit.getWorkingDirectoryPath(config.saveDir).toFile();
        File dir = Settings.settings().PATHS.PER_PLAYER_SCHEMATICS ? new File(working, actor.getUniqueId().toString()) : working;
        File destDir = new File(dir, directory);
        if (!MainUtil.isInSubDirectory(working, destDir)) {
            actor.print(Caption.of("worldedit.schematic.directory-does-not-exist", TextComponent.of(String.valueOf(destDir))));
            return;
        }
        if (Settings.settings().PATHS.PER_PLAYER_SCHEMATICS && !MainUtil.isInSubDirectory(dir, destDir) && !actor.hasPermission(
                "worldedit.schematic.move.other")) {
            actor.print(Caption.of("fawe.error.no-perm", "worldedit.schematic.move.other"));
            return;
        }
        ClipboardHolder clipboard = session.getClipboard();
        List<File> sources = getFiles(clipboard);
        if (sources.isEmpty()) {
            actor.print(Caption.of("fawe.worldedit.schematic.schematic.none"));
            return;
        }
        if (!destDir.exists() && !destDir.mkdirs()) {
            actor.print(Caption.of("worldedit.schematic.file-perm-fail", TextComponent.of(String.valueOf(destDir))));
            return;
        }
        for (File source : sources) {
            File destFile = new File(destDir, source.getName());
            if (destFile.exists()) {
                actor.print(Caption.of("fawe.worldedit.schematic.schematic.move.exists", destFile));
                continue;
            }
            if (Settings.settings().PATHS.PER_PLAYER_SCHEMATICS && (!MainUtil.isInSubDirectory(
                    dir,
                    destFile
            ) || !MainUtil.isInSubDirectory(dir, source)) && !actor.hasPermission("worldedit.schematic.delete.other")) {
                actor.print(Caption.of("fawe.worldedit.schematic.schematic.move.failed", destFile,
                        Caption.of("fawe.error.no-perm", ("worldedit.schematic.move.other"))
                ));
                continue;
            }
            try {
                File cached = new File(source.getParentFile(), "." + source.getName() + ".cached");
                Files.move(source.toPath(), destFile.toPath());
                if (cached.exists()) {
                    Files.move(cached.toPath(), destFile.toPath());
                }
                actor.print(Caption.of("fawe.worldedit.schematic.schematic.move.success", source, destFile));
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
    //FAWE end

    @Command(
            name = "load",
            desc = "Load a schematic into your clipboard"
    )
    @CommandPermissions({"worldedit.clipboard.load", "worldedit.schematic.load", "worldedit.schematic.load.asset", "worldedit.schematic.load.web", "worldedit.schematic.load.other"})
    public void load(
            Actor actor, LocalSession session,
            @Arg(desc = "File name.")
                    String filename,
            //FAWE start - use format-name, random rotation
            @Arg(desc = "Format name.", def = "")
                    String formatName,
            @Switch(name = 'r', desc = "Apply random rotation to the clipboard")
                    boolean randomRotate
            //FAWE end
    ) throws FilenameException {
        LocalConfiguration config = worldEdit.getConfiguration();

        //FAWE start
        final Closer closer = Closer.create();
        ClipboardFormat format;
        InputStream in;
        // if format is set explicitly, do not look up by extension!
        boolean noExplicitFormat = formatName == null;
        if (noExplicitFormat) {
            formatName = "fast";
        }
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
                URL webUrl = new URL(Settings.settings().WEB.URL);
                if ((format = ClipboardFormats.findByAlias(formatName)) == null) {
                    actor.print(Caption.of("worldedit.schematic.unknown-format", TextComponent.of(formatName)));
                    return;
                }
                // The interface requires the correct schematic extension - otherwise it can't be downloaded
                // So it basically only supports .schem files (sponge v2 + v3) - or the correct extensions is specified manually
                // Sadly it's not really an API endpoint but spits out the HTML source of the uploader - so no real handling
                // can happen
                URL url = new URL(webUrl, "uploads/" + uuid + "." + format.getPrimaryFileExtension());
                final Path temp = Files.createTempFile("faweremoteschem", null);
                final File tempFile = temp.toFile();
                // delete temporary file when we're done
                closer.register((Closeable) () -> Files.deleteIfExists(temp));
                // write schematic into temporary file
                try (final ReadableByteChannel byteChannel = Channels.newChannel(url.openStream());
                     final FileOutputStream out = new FileOutputStream(tempFile);
                     final FileChannel outChannel = out.getChannel()) {
                    outChannel.transferFrom(byteChannel, 0, Long.MAX_VALUE);
                }
                // No format is specified -> try or fail
                if (noExplicitFormat && (format = ClipboardFormats.findByFile(tempFile)) == null) {
                    actor.print(Caption.of("fawe.worldedit.schematic.schematic.load-failure", TextComponent.of(filename)));
                    return;
                }
                in = new FileInputStream(tempFile);
                uri = temp.toUri();
            } else {
                File saveDir = worldEdit.getWorkingDirectoryPath(config.saveDir).toFile();
                File dir = Settings.settings().PATHS.PER_PLAYER_SCHEMATICS ? new File(saveDir, actor.getUniqueId().toString()) : saveDir;
                File file;
                if (filename.startsWith("#")) {
                    format = noExplicitFormat ? null : ClipboardFormats.findByAlias(formatName);
                    String[] extensions;
                    if (format != null) {
                        extensions = format.getFileExtensions().toArray(new String[0]);
                    } else {
                        extensions = ClipboardFormats.getFileExtensionArray();
                    }
                    file = actor.openFileOpenDialog(extensions);
                    if (file == null || !file.exists()) {
                        actor.print(Caption.of("worldedit.schematic.load.does-not-exist", TextComponent.of(filename)));
                        return;
                    }
                } else {
                    if (Settings.settings().PATHS.PER_PLAYER_SCHEMATICS && !actor.hasPermission("worldedit.schematic.load.other") && Pattern
                            .compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
                            .matcher(filename)
                            .find()) {
                        actor.print(Caption.of("fawe.error.no-perm", "worldedit.schematic.load.other"));
                        return;
                    }
                    if (!noExplicitFormat) {
                        format = ClipboardFormats.findByAlias(formatName);
                    } else if (filename.matches(".*\\.\\w.*")) {
                        format = ClipboardFormats
                                .findByExplicitExtension(filename.substring(filename.lastIndexOf('.') + 1));
                    } else {
                        format = null;
                    }
                    file = MainUtil.resolve(dir, filename, format, false);
                }
                if (file == null || !file.exists()) {
                    if (!filename.contains("../")) {
                        dir = this.worldEdit.getWorkingDirectoryPath(config.saveDir).toFile();
                        file = MainUtil.resolve(dir, filename, format, false);
                    }
                }
                if (file == null || !file.exists() || !MainUtil.isInSubDirectory(saveDir, file)) {
                    actor.printError(TextComponent.of("Schematic " + filename + " does not exist! (" + (file != null && file.exists()) +
                            "|" + file + "|" + (file != null && !MainUtil
                            .isInSubDirectory(saveDir, file)) + ")"));
                    return;
                }
                if (format == null) {
                    format = ClipboardFormats.findByFile(file);
                    if (format == null) {
                        if (noExplicitFormat) {
                            actor.print(Caption.of("fawe.worldedit.schematic.schematic.load-failure", TextComponent.of(file.getName())));
                        } else {
                            actor.print(Caption.of("worldedit.schematic.unknown-format", TextComponent.of(formatName)));
                        }
                        return;
                    }
                }
                in = new FileInputStream(file);
                uri = file.toURI();
            }
            closer.register(in);
            format.hold(actor, uri, in);
            if (randomRotate) {
                AffineTransform transform = new AffineTransform();
                int rotate = 90 * ThreadLocalRandom.current().nextInt(4);
                transform = transform.rotateY(rotate);
                session.getClipboard().setTransform(transform);
            }
            actor.print(Caption.of("fawe.worldedit.schematic.schematic.loaded", filename));
        } catch (IllegalArgumentException e) {
            actor.print(Caption.of("worldedit.schematic.unknown-filename", TextComponent.of(filename)));
        } catch (EOFException e) {
            // EOFException is extending IOException - but the IOException error is too generic.
            // EOF mostly occurs when there was unexpected content in the schematic - due to the wrong reader (= version)
            actor.print(Caption.of("fawe.worldedit.schematic.schematic.load-failure",
                    TextComponent.of(e.getMessage() != null ? e.getMessage() : "EOFException"))); // often null...
            LOGGER.error("Error loading a schematic", e);
        } catch (IOException e) {
            actor.print(Caption.of("worldedit.schematic.file-not-exist", TextComponent.of(Objects.toString(e.getMessage()))));
            LOGGER.warn("Failed to load a saved clipboard", e);
        } catch (Exception e) {
            actor.print(Caption.of("fawe.worldedit.schematic.schematic.load-failure", TextComponent.of(e.getMessage())));
            LOGGER.error("Error loading a schematic", e);
        } finally {
            try {
                closer.close();
            } catch (IOException e) {
                LOGGER.error("Failed to close schematic resources", e);
            }
        }
        //FAWE end
    }

    @Command(
            name = "save",
            desc = "Save your clipboard into a schematic file"
    )
    @CommandPermissions({"worldedit.clipboard.save", "worldedit.schematic.save", "worldedit.schematic.save.other", "worldedit.schematic.save.global"})
    public void save(
            Actor actor, LocalSession session,
            @Arg(desc = "File name.")
                    String filename,
            @Arg(desc = "Format name.", def = "fast") //FAWE: def: sponge -> fast
                ClipboardFormat format,
            @Switch(name = 'f', desc = "Overwrite an existing file.")
                    boolean allowOverwrite,
            //FAWE start
            @Switch(name = 'g', desc = "Bypasses per-player-schematic folders")
                    boolean global
    ) throws WorldEditException {

         if (global && !actor.hasPermission("worldedit.schematic.save.global")) {
             actor.print(Caption.of("fawe.error.no-perm", "worldedit.schematic.save.global"));
             return;
        }

        //FAWE end
        if (worldEdit.getPlatformManager().queryCapability(Capability.GAME_HOOKS).getDataVersion() == -1) {
            actor.print(TranslatableComponent.of("worldedit.schematic.unsupported-minecraft-version"));
            return;
        }

        LocalConfiguration config = worldEdit.getConfiguration();

        File dir = worldEdit.getWorkingDirectoryPath(config.saveDir).toFile();

        //FAWE start
        if (!global && Settings.settings().PATHS.PER_PLAYER_SCHEMATICS) {
            dir = new File(dir, actor.getUniqueId().toString());
        }

        boolean other = false;
        if (filename.contains("../")) {
            other = true;
            if (!actor.hasPermission("worldedit.schematic.save.other")) {
                actor.print(Caption.of("fawe.error.no-perm", "worldedit.schematic.save.other"));
                return;
            }
            if (filename.startsWith("../")) {
                dir = worldEdit.getWorkingDirectoryPath(config.saveDir).toFile();
                filename = filename.substring(3);
            }
        }

        File f = worldEdit.getSafeSaveFile(actor, dir, filename, format.getPrimaryFileExtension());
        int i = f.getName().lastIndexOf('.');
        if (i == -1 && f.getName().isEmpty() || i == 0) {
            File directory = f.getParentFile();
            int fileNumber = directory.exists() ? MainUtil.getMaxFileId(directory) : 0;
            String extension = i == 0 ? f.getName().substring(i + 1) : format.getPrimaryFileExtension();
            String name = String.format("%s.%s", fileNumber, extension);
            f = new File(directory, name);
            filename += name;
        }
        boolean overwrite = f.exists();
        if (overwrite) {
            if (!actor.hasPermission("worldedit.schematic.delete")) {
                throw new StopExecutionException(Caption.of("worldedit.schematic.already-exists"));
            }
            if (other) {
                if (!actor.hasPermission("worldedit.schematic.delete.other")) {
                    actor.print(Caption.of("fawe.error.no-perm", "worldedit.schematic.delete.other"));
                    return;
                }
            }
            if (!allowOverwrite) {
                actor.print(Caption.of("worldedit.schematic.save.already-exists"));
                return;
            }
        }
        //FAWE end

        // Create parent directories
        File parent = f.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs()) {
                throw new StopExecutionException(Caption.of(
                        "worldedit.schematic.save.failed-directory"));
            }
        }

        ClipboardHolder holder = session.getClipboard();

        SchematicSaveTask task = new SchematicSaveTask(actor, f, dir, format, holder, overwrite);
        AsyncCommandBuilder.wrap(task, actor)
                .registerWithSupervisor(worldEdit.getSupervisor(), "Saving schematic " + filename)
                .setDelayMessage(Caption.of("worldedit.schematic.save.saving"))
                .onSuccess(filename + " saved" + (overwrite ? " (overwriting previous file)." : "."), null)
                .onFailure(
                        Caption.of("worldedit.schematic.failed-to-save"),
                        worldEdit.getPlatformManager().getPlatformCommandManager().getExceptionConverter()
                )
                .buildAndExec(worldEdit.getExecutorService());
    }

    @Command(
            name = "share",
            desc = "Share your clipboard as a schematic online"
    )
    @CommandPermissions({ "worldedit.clipboard.share", "worldedit.schematic.share" })
    public void share(Actor actor, LocalSession session,
                      @Arg(desc = "Schematic name. Defaults to name-millis", def = "")
                          String schematicName,
                      @Arg(desc = "Share location", def = "arkitektonika") //FAWE: def: ehpaste -> arkitektonika
                          ClipboardShareDestination destination,
                      @Arg(desc = "Format name", def = "fast") //FAWE: def: sponge -> fast
                          ClipboardFormat format) throws WorldEditException {
        if (worldEdit.getPlatformManager().queryCapability(Capability.GAME_HOOKS).getDataVersion() == -1) {
            actor.printError(TranslatableComponent.of("worldedit.schematic.unsupported-minecraft-version"));
            return;
        }

        if (format == null) {
            format = destination.getDefaultFormat();
        }

        if (!destination.supportsFormat(format)) {
            actor.printError(Caption.of( //FAWE: TranslatableComponent -> Caption
                "worldedit.schematic.share.unsupported-format",
                TextComponent.of(destination.getName()),
                TextComponent.of(format.getName())
            ));
            return;
        }

        ClipboardHolder holder = session.getClipboard();

        SchematicShareTask task = new SchematicShareTask(actor, holder, destination, format, schematicName);
        AsyncCommandBuilder.wrap(task, actor)
                .registerWithSupervisor(worldEdit.getSupervisor(), "Sharing schematic")
                .setDelayMessage(TranslatableComponent.of("worldedit.schematic.save.saving"))
                .setWorkingMessage(TranslatableComponent.of("worldedit.schematic.save.still-saving"))
                .onSuccess("Shared", (consumer -> consumer.accept(actor)))
                .onFailure("Failed to share schematic", worldEdit.getPlatformManager().getPlatformCommandManager().getExceptionConverter())
                .buildAndExec(worldEdit.getExecutorService());
    }

    @Command(
            name = "delete",
            aliases = {"d"},
            desc = "Delete a saved schematic"
    )
    @CommandPermissions("worldedit.schematic.delete")
    public void delete(
            Actor actor, LocalSession session,
            @Arg(desc = "File name.")
            String filename
    ) throws WorldEditException, IOException {
        LocalConfiguration config = worldEdit.getConfiguration();
        //FAWE start
        File working = worldEdit.getWorkingDirectoryPath(config.saveDir).toFile();
        File dir = Settings.settings().PATHS.PER_PLAYER_SCHEMATICS ? new File(working, actor.getUniqueId().toString()) : working;
        List<File> files = new ArrayList<>();

        if (filename.equalsIgnoreCase("*")) {
            files.addAll(getFiles(session.getClipboard()));
        } else {
            File f = MainUtil.resolveRelative(new File(dir, filename));
            files.add(f);
        }

        if (files.isEmpty()) {
            actor.print(Caption.of("worldedit.schematic.delete.does-not-exist", TextComponent.of(filename)));
            return;
        }
        for (File f : files) {
            if (!MainUtil.isInSubDirectory(working, f) || !f.exists()) {
                actor.print(Caption.of("worldedit.schematic.delete.does-not-exist", TextComponent.of(filename)));
                continue;
            }
            if (Settings.settings().PATHS.PER_PLAYER_SCHEMATICS && !MainUtil.isInSubDirectory(dir, f) && !actor.hasPermission(
                    "worldedit.schematic.delete.other")) {
                actor.print(Caption.of("fawe.error.no-perm", "worldedit.schematic.delete.other"));
                continue;
            }
            if (!deleteFile(f)) {
                actor.print(Caption.of("worldedit.schematic.delete.failed", TextComponent.of(filename)));
                continue;
            }
            actor.print(Caption.of("worldedit.schematic.delete.deleted", filename));
        }
        //FAWE end
    }

    //FAWE start
    private boolean deleteFile(File file) {
        if (file.delete()) {
            new File(file.getParentFile(), "." + file.getName() + ".cached").delete();
            return true;
        }
        return false;
    }
    //FAWE end

    @Command(
            name = "formats",
            aliases = {"listformats", "f"},
            desc = "List available formats"
    )
    @CommandPermissions(
            value = "worldedit.schematic.formats",
            queued = false
    )
    public void formats(Actor actor) {
        actor.print(Caption.of("worldedit.schematic.formats.title"));
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
            actor.print(TextComponent.of(builder.toString()));
        }
    }

    @Command(
            name = "list",
            aliases = {"all", "ls"},
            desc = "List saved schematics",
            descFooter = "Note: Format is not fully verified until loading."
    )
    @CommandPermissions(
            value = "worldedit.schematic.list",
            queued = false
    )
    public void list(
            Actor actor, LocalSession session,
            @ArgFlag(name = 'p', desc = "Page to view.", def = "1")
                    int page,
            @Switch(name = 'd', desc = "Sort by date, oldest first")
                    boolean oldFirst,
            @Switch(name = 'n', desc = "Sort by date, newest first")
                    boolean newFirst,
            @ArgFlag(name = 'f', desc = "Restricts by format.", def = "")
                    String formatName,
            @Arg(name = "filter", desc = "Filter for schematics", def = "all")
                    String filter, Arguments arguments
    ) throws WorldEditException {
        if (oldFirst && newFirst) {
            throw new StopExecutionException(Caption.of("worldedit.schematic.sorting-old-new"));
        }
        //FAWE start
        String pageCommand = "/" + arguments.get();
        LocalConfiguration config = worldEdit.getConfiguration();
        File dir = worldEdit.getWorkingDirectoryPath(config.saveDir).toFile();

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
        boolean playerFolder = Settings.settings().PATHS.PER_PLAYER_SCHEMATICS;
        UUID uuid = playerFolder ? actor.getUniqueId() : null;
        List<File> files = UtilityCommands.getFiles(dir, actor, args, formatName, playerFolder, oldFirst, newFirst);
        List<Map.Entry<URI, String>> entries = UtilityCommands.filesToEntry(dir, files, uuid);

        Function<URI, Boolean> isLoaded = multi == null ? f -> false : multi::contains;

        List<Component> components = UtilityCommands.entryToComponent(dir, entries, isLoaded,
                (name, path, type, loaded) -> {
                    TextComponentProducer msg = new TextComponentProducer();

                    msg.append(Caption.of("worldedit.schematic.dash.symbol"));

                    if (loaded) {
                        msg.append(Caption.of("worldedit.schematic.minus.symbol")
                                .clickEvent(ClickEvent.of(ClickEvent.Action.SUGGEST_COMMAND, unload + " " + path))
                                .hoverEvent(HoverEvent.of(
                                        HoverEvent.Action.SHOW_TEXT,
                                        Caption.of("worldedit.schematic.unload")
                                )));
                    } else {
                        msg.append(Caption.of("worldedit.schematic.plus.symbol")
                                .clickEvent(ClickEvent.of(ClickEvent.Action.SUGGEST_COMMAND, loadMulti + " " + path))
                                .hoverEvent(HoverEvent.of(
                                        HoverEvent.Action.SHOW_TEXT,
                                        Caption.of("worldedit.schematic.clipboard")
                                )));
                    }
                    if (type != UtilityCommands.URIType.DIRECTORY) {
                        msg.append(Caption.of("worldedit.schematic.x.symbol")
                                .clickEvent(ClickEvent.of(ClickEvent.Action.SUGGEST_COMMAND, delete + " " + path))
                                .hoverEvent(HoverEvent.of(HoverEvent.Action.SHOW_TEXT, Caption.of("worldedit.schematic.delete")))
                        );
                    } else if (hasShow) {
                        msg.append(Caption.of("worldedit.schematic.0.symbol")
                                .clickEvent(ClickEvent.of(ClickEvent.Action.SUGGEST_COMMAND, showCmd + " " + path))
                                .hoverEvent(HoverEvent.of(
                                        HoverEvent.Action.SHOW_TEXT,
                                        Caption.of("worldedit.schematic.visualize")
                                ))
                        );
                    }
                    TextComponent msgElem = TextComponent.of(name);
                    if (type != UtilityCommands.URIType.DIRECTORY) {
                        msgElem = msgElem.clickEvent(ClickEvent.of(ClickEvent.Action.SUGGEST_COMMAND, loadSingle + " " + path));
                        msgElem = msgElem.hoverEvent(HoverEvent.of(
                                HoverEvent.Action.SHOW_TEXT,
                                Caption.of("worldedit.schematic.load")
                        ));
                    } else {
                        msgElem = msgElem.clickEvent(ClickEvent.of(ClickEvent.Action.SUGGEST_COMMAND, list + " " + path));
                        msgElem = msgElem.hoverEvent(HoverEvent.of(
                                HoverEvent.Action.SHOW_TEXT,
                                Caption.of("worldedit.schematic.list")
                        ));
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
                }
        );

        long totalBytes = 0;
        File parentDir = new File(dir.getAbsolutePath() + (playerFolder ? File.separator + uuid.toString() : ""));
        try {
            List<File> toAddUp = getFiles(parentDir, null, null);
            if (toAddUp != null && toAddUp.size() != 0) {
                for (File schem : toAddUp) {
                    if (schem.getName().endsWith(".schem") || schem.getName().endsWith(".schematic")) {
                        totalBytes += Files.size(Paths.get(schem.getAbsolutePath()));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        String headerBytesElem = String.format("%.1fkb", totalBytes / 1000.0);

        if (Settings.settings().PATHS.PER_PLAYER_SCHEMATICS && actor.getLimit().SCHEM_FILE_SIZE_LIMIT > -1) {
            headerBytesElem += String.format(
                    " / %dkb",
                    actor.getLimit().SCHEM_FILE_SIZE_LIMIT
            );
        }

        if (Settings.settings().PATHS.PER_PLAYER_SCHEMATICS) {
            String fullHeader = "| My Schematics: " + headerBytesElem + " |";
            PaginationBox paginationBox = PaginationBox.fromComponents(fullHeader, pageCommand, components);
            actor.print(paginationBox.create(page));
        } else {
            String fullHeader = "| Schematics: " + headerBytesElem + " |";
            PaginationBox paginationBox = PaginationBox.fromComponents(fullHeader, pageCommand, components);
            actor.print(paginationBox.create(page));
        }
        //FAWE end

    }

    private static class SchematicLoadTask implements Callable<ClipboardHolder> {

        private final Actor actor;
        private final File file;
        private final ClipboardFormat format;

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
                LOGGER.info(actor.getName() + " loaded " + file.getCanonicalPath());
                return new ClipboardHolder(clipboard);
            }
        }

    }

    private abstract static class SchematicOutputTask<T> implements Callable<T> {
        protected final Actor actor;
        protected final ClipboardFormat format;
        protected final ClipboardHolder holder;

        SchematicOutputTask(
                Actor actor,
                ClipboardFormat format,
                ClipboardHolder holder
        ) {
            this.actor = actor;
            this.format = format;
            this.holder = holder;
        }

        protected void writeToOutputStream(OutputStream outputStream) throws IOException, WorldEditException {
            Clipboard clipboard = holder.getClipboard();
            Transform transform = holder.getTransform();
            Clipboard target = clipboard.transform(transform);

            try (Closer closer = Closer.create()) {
                OutputStream stream = closer.register(outputStream);
                BufferedOutputStream bos = closer.register(new BufferedOutputStream(stream));
                ClipboardWriter writer = closer.register(format.getWriter(bos));
                writer.write(target);
            }
        }
    }

    private static class SchematicSaveTask extends SchematicOutputTask<Void> {
        private final Actor actor;
        private File file; //FAWE: un-finalize
        private final boolean overwrite;
        private final File rootDir; //FAWE: add root-dir

        SchematicSaveTask(
                Actor actor,
                File file,
                File rootDir,
                ClipboardFormat format,
                ClipboardHolder holder,
                boolean overwrite
        ) {
            super(actor, format, holder);
            this.actor = actor;
            this.file = file;
            this.overwrite = overwrite;
            this.rootDir = rootDir; //FAWE: add root-dir
        }

        @Override
        public Void call() throws Exception {
            Clipboard clipboard = holder.getClipboard();
            Transform transform = holder.getTransform();
            Clipboard target;

            //FAWE start
            boolean checkFilesize = Settings.settings().PATHS.PER_PLAYER_SCHEMATICS
                    && actor.getLimit().SCHEM_FILE_SIZE_LIMIT > -1;

            double directorysizeKb = 0;
            String curFilepath = file.getAbsolutePath();
            final String SCHEMATIC_NAME = file.getName();

            double oldKbOverwritten = 0;

            int numFiles = -1;
            if (checkFilesize) {
                List<File> toAddUp = getFiles(rootDir, null, null);
                if (toAddUp != null && toAddUp.size() != 0) {
                    for (File child : toAddUp) {
                        if (child.getName().endsWith(".schem") || child.getName().endsWith(".schematic")) {
                            directorysizeKb += Files.size(Paths.get(child.getAbsolutePath())) / 1000.0;
                            numFiles++;
                        }
                    }
                }
                if (overwrite) {
                    oldKbOverwritten = Files.size(Paths.get(file.getAbsolutePath())) / 1000.0;
                    int iter = 1;
                    while (new File(curFilepath + "." + iter + "." + format.getPrimaryFileExtension()).exists()) {
                        iter++;
                    }
                    file = new File(curFilepath + "." + iter + "." + format.getPrimaryFileExtension());
                }
            }


            if (Settings.settings().PATHS.PER_PLAYER_SCHEMATICS && actor.getLimit().SCHEM_FILE_NUM_LIMIT > -1) {

                if (numFiles == -1) {
                    numFiles = 0;
                    List<File> toAddUp = getFiles(rootDir, null, null);
                    if (toAddUp != null && toAddUp.size() != 0) {
                        for (File child : toAddUp) {
                            if (child.getName().endsWith(".schem") || child.getName().endsWith(".schematic")) {
                                numFiles++;
                            }
                        }
                    }
                }
                int limit = actor.getLimit().SCHEM_FILE_NUM_LIMIT;

                if (numFiles >= limit) {
                    TextComponent noSlotsErr = TextComponent.of( //TODO - to be moved into captions/translatablecomponents
                            String.format(
                                    "You have " + numFiles + "/" + limit + " saved schematics. Delete some to save this one!",
                                    TextColor.RED
                            ));
                    LOGGER.info(actor.getName() + " failed to save " + file.getCanonicalPath() + " - too many schematics!");
                    throw new WorldEditException(noSlotsErr) {
                    };
                }
            }
            //FAWE end

            // If we have a transform, bake it into the copy
            if (transform.isIdentity()) {
                target = clipboard;
            } else {
                target = clipboard.transform(transform);
            }

            try (Closer closer = Closer.create()) {
                FileOutputStream fos = closer.register(new FileOutputStream(file));
                BufferedOutputStream bos = closer.register(new BufferedOutputStream(fos));
                ClipboardWriter writer = closer.register(format.getWriter(bos));
                //FAWE start
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
                    double filesizeKb = Files.size(Paths.get(file.getAbsolutePath())) / 1000.0;

                    TextComponent filesizeNotif = TextComponent.of( //TODO - to be moved into captions/translatablecomponents
                            SCHEMATIC_NAME + " size: " + String.format("%.1f", filesizeKb) + "kb", TextColor.GRAY);
                    actor.print(filesizeNotif);

                    if (checkFilesize) {

                        double curKb = filesizeKb + directorysizeKb;
                        int allocatedKb = actor.getLimit().SCHEM_FILE_SIZE_LIMIT;

                        if (overwrite) {
                            curKb -= oldKbOverwritten;
                        }

                        if ((curKb) > allocatedKb) {
                            file.delete();
                            TextComponent notEnoughKbErr = TextComponent.of(
                                    //TODO - to be moved into captions/translatablecomponents
                                    "You're about to be at " + String.format("%.1f", curKb) + "kb of schematics. ("
                                            + String.format(
                                            "%dkb",
                                            allocatedKb
                                    ) + " available) Delete some first to save this one!",
                                    TextColor.RED
                            );
                            LOGGER.info(actor.getName() + " failed to save " + SCHEMATIC_NAME + " - not enough space!");
                            throw new WorldEditException(notEnoughKbErr) {
                            };
                        }
                        if (overwrite) {
                            new File(curFilepath).delete();
                            file.renameTo(new File(curFilepath));
                        } else {
                            numFiles++;
                        }
                        TextComponent kbRemainingNotif = TextComponent.of(
                                //TODO - to be moved into captions/translatablecomponents
                                "You have " + String.format("%.1f", (allocatedKb - curKb)) + "kb left for schematics.",
                                TextColor.GRAY
                        );
                        actor.print(kbRemainingNotif);
                    }

                    if (Settings.settings().PATHS.PER_PLAYER_SCHEMATICS && actor.getLimit().SCHEM_FILE_NUM_LIMIT > -1) {

                        TextComponent slotsRemainingNotif = TextComponent.of(
                                //TODO - to be moved into captions/translatablecomponents
                                "You have " + (actor.getLimit().SCHEM_FILE_NUM_LIMIT - numFiles)
                                        + " schematic file slots left.",
                                TextColor.GRAY
                        );
                        actor.print(slotsRemainingNotif);
                    }
                    LOGGER.info(actor.getName() + " saved " + file.getCanonicalPath());
                } else {
                    actor.print(Caption.of("fawe.cancel.reason.manual"));
                }
            }
            //FAWE end
            return null;
        }
    }

    private static class SchematicShareTask extends SchematicOutputTask<Consumer<Actor>> {
        private final Actor actor;
        private final String name;
        private final ClipboardShareDestination destination;

        SchematicShareTask(Actor actor,
                           ClipboardHolder holder,
                           ClipboardShareDestination destination,
                           ClipboardFormat format,
                           String name) {
            super(actor, format, holder);
            this.actor = actor;
            this.name = name;
            this.destination = destination;
        }

        @Override
        public Consumer<Actor> call() throws Exception {
            ClipboardShareMetadata metadata = new ClipboardShareMetadata(
                format,
                this.actor.getName(),
                name == null ? actor.getName() + "-" + System.currentTimeMillis() : name
            );

            return destination.share(metadata, this::writeToOutputStream);
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

        SchematicListTask(
                String prefix, int sortType, int page, String pageCommand,
                String filter, String formatName
        ) {
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

            //FAWE start
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

            //FAWE end
            PaginationBox paginationBox = new SchematicPaginationBox(prefix, files, pageCommand);
            return paginationBox.create(page);
        }

    }

    private static class SchematicPaginationBox extends PaginationBox {

        //FAWE start - Expand to per player schematics
        private final String prefix;
        private final File[] files;

        SchematicPaginationBox(String rootDir, File[] files, String pageCommand) {
            super("worldedit.schematic.available", pageCommand);
            this.prefix = rootDir == null ? "" : rootDir;
            this.files = files;
            //FAWE end
        }

        @Override
        public Component getComponent(int number) {
            checkArgument(number < files.length && number >= 0);
            //FAWE start - Per player schematic support & translatable things
            File file = files[number];
            Multimap<String, ClipboardFormat> exts = ClipboardFormats.getFileExtensionMap();
            String format = exts.get(com.google.common.io.Files.getFileExtension(file.getName()))
                    .stream().findFirst().map(ClipboardFormat::getName).orElse("Unknown");
            boolean inRoot = file.getParentFile().getName().equals(prefix);

            String path = inRoot ? file.getName() : file.getPath().split(Pattern.quote(prefix + File.separator))[1];

            return TextComponent.builder()
                    .content("")
                    .append(TranslatableComponent.of("worldedit.schematic.load.symbol")
                            .clickEvent(ClickEvent
                                    .of(ClickEvent.Action.RUN_COMMAND, "/schem load \"" + path + "\""))
                            .hoverEvent(HoverEvent.of(
                                    HoverEvent.Action.SHOW_TEXT,
                                    TranslatableComponent.of("worldedit.schematic.click-to-load")
                            )))
                    .append(TextComponent.space())
                    .append(TextComponent.of(path)
                            .hoverEvent(HoverEvent.of(HoverEvent.Action.SHOW_TEXT, TextComponent.of(format))))
                    .build();
            //FAWE end
        }

        @Override
        public int getComponentsSize() {
            return files.length;
        }

    }

}
