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
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Commands;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RunnableVal3;
import com.boydti.fawe.object.clipboard.MultiClipboardHolder;
import com.boydti.fawe.object.clipboard.URIClipboardHolder;
import com.boydti.fawe.object.clipboard.remap.ClipboardRemapper;
import com.boydti.fawe.object.schematic.StructureFormat;
import com.boydti.fawe.object.schematic.visualizer.SchemVis;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.chat.Message;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.extent.PlayerSaveClipboardEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.command.binding.Switch;
import com.sk89q.worldedit.util.command.parametric.Optional;
import com.sk89q.worldedit.util.io.file.FilenameException;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;


import static com.boydti.fawe.util.ReflectionUtils.as;

/**
 * Commands that work with schematic files.
 */
@Command(aliases = {"schematic", "schem", "/schematic", "/schem", "clipboard", "/clipboard"}, desc = "Commands that work with schematic files")
public class SchematicCommands extends MethodCommands {

    private static final Logger log = Logger.getLogger(SchematicCommands.class.getCanonicalName());

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public SchematicCommands(final WorldEdit worldEdit) {
        super(worldEdit);
    }

    @Command(
            aliases = {"loadall"},
            usage = "[<format>] <filename|url>",
            help = "Load multiple clipboards\n" +
                    "The -r flag will apply random rotation",
            desc = "Load multiple clipboards (paste will randomly choose one)"
    )
    @Deprecated
    @CommandPermissions({"worldedit.clipboard.load", "worldedit.schematic.load", "worldedit.schematic.upload"})
    public void loadall(final Player player, final LocalSession session, @Optional("schematic") final String formatName, final String filename, @Switch('r') boolean randomRotate) throws FilenameException {
        final ClipboardFormat format = ClipboardFormat.findByAlias(formatName);
        if (format == null) {
            BBC.CLIPBOARD_INVALID_FORMAT.send(player, formatName);
            return;
        }
        try {
            MultiClipboardHolder all = format.loadAllFromInput(player, filename, true);
            if (all != null) {
                session.addClipboard(all);
                BBC.SCHEMATIC_LOADED.send(player, filename);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Command(
            aliases = {"clear"},
            usage = "",
            desc = "Clear your clipboard",
            min = 0,
            max = 0
    )
    @CommandPermissions({"worldedit.clipboard.clear", "worldedit.schematic.clear"})
    public void clear(Player player, LocalSession session) throws WorldEditException {
        session.setClipboard(null);
        BBC.CLIPBOARD_CLEARED.send(player);
    }

    @Command(
            aliases = {"unload"},
            usage = "[file]",
            desc = "Remove a clipboard from your multi-clipboard",
            min = 1,
            max = 1
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

        boolean removed = false;
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
            aliases = {"remap"},
            help = "Remap a clipboard between MCPE/PC values\n",
            desc = "Remap a clipboard between MCPE/PC values\n"
    )
    @Deprecated
    @CommandPermissions({"worldedit.schematic.remap"})
    public void remap(final Player player, final LocalSession session) throws WorldEditException {
        ClipboardRemapper remapper;
        if (Fawe.imp().getPlatform().equalsIgnoreCase("nukkit")) {
            remapper = new ClipboardRemapper(ClipboardRemapper.RemapPlatform.PC, ClipboardRemapper.RemapPlatform.PE);
        } else {
            remapper = new ClipboardRemapper(ClipboardRemapper.RemapPlatform.PE, ClipboardRemapper.RemapPlatform.PC);
        }

        for (Clipboard clip : session.getClipboard().getClipboards()) {
            remapper.apply(clip);
        }
        player.print(BBC.getPrefix() + "Remapped schematic");
    }

    @Command(aliases = {"load"}, usage = "[<format>] <filename>", desc = "Load a schematic into your clipboard")
    @Deprecated
    @CommandPermissions({"worldedit.clipboard.load", "worldedit.schematic.load", "worldedit.schematic.upload", "worldedit.schematic.load.other"})
    public void load(final Player player, final LocalSession session, @Optional("schematic") final String formatName, String filename) throws FilenameException {
        final LocalConfiguration config = this.worldEdit.getConfiguration();
        final ClipboardFormat format = ClipboardFormat.findByAlias(formatName);
        if (format == null) {
            BBC.CLIPBOARD_INVALID_FORMAT.send(player, formatName);
            return;
        }
        InputStream in = null;
        try {
            URI uri;
            if (filename.startsWith("url:")) {
                if (!player.hasPermission("worldedit.schematic.upload")) {
                    BBC.NO_PERM.send(player, "worldedit.schematic.upload");
                    return;
                }
                UUID uuid = UUID.fromString(filename.substring(4));
                URL base = new URL(Settings.IMP.WEB.URL);
                URL url = new URL(base, "uploads/" + uuid + ".schematic");
                ReadableByteChannel rbc = Channels.newChannel(url.openStream());
                in = Channels.newInputStream(rbc);
                uri = url.toURI();
            } else {
                if (!player.hasPermission("worldedit.schematic.load") && !player.hasPermission("worldedit.clipboard.load")) {
                    BBC.NO_PERM.send(player, "worldedit.clipboard.load");
                    return;
                }
                File working = this.worldEdit.getWorkingDirectoryFile(config.saveDir);
                File dir = Settings.IMP.PATHS.PER_PLAYER_SCHEMATICS ? new File(working, player.getUniqueId().toString()) : working;
                File f;
                if (filename.startsWith("#")) {
                    f = player.openFileOpenDialog(new String[] { format.getExtension() });
                    if (!f.exists()) {
                        player.printError("Schematic " + filename + " does not exist! (" + f + ")");
                        return;
                    }
                } else {
                    if (Settings.IMP.PATHS.PER_PLAYER_SCHEMATICS && Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}").matcher(filename).find() && !player.hasPermission("worldedit.schematic.load.other")) {
                        BBC.NO_PERM.send(player, "worldedit.schematic.load.other");
                        return;
                    }
                    if (!filename.matches(".*\\.[\\w].*")) {
                        filename += "." + format.getExtension();
                    }
                    f = MainUtil.resolveRelative(new File(dir, filename));
                }
                if (f.getName().replaceAll("." + format.getExtension(), "").isEmpty()) {
                    File directory = f.getParentFile();
                    if (directory.exists()) {
                        int max = MainUtil.getMaxFileId(directory) - 1;
                        f = new File(directory, max + "." + format.getExtension());
                    } else {
                        f = new File(directory, "1." + format.getExtension());
                    }
                }
                if (!f.exists()) {
                    if (!filename.contains("../")) {
                        dir = this.worldEdit.getWorkingDirectoryFile(config.saveDir);
                        f = this.worldEdit.getSafeSaveFile(player, dir, filename, format.getExtension(), format.getExtension());
                    }
                }
                if (!f.exists() || !MainUtil.isInSubDirectory(working, f)) {
                    player.printError("Schematic " + filename + " does not exist! (" + f.exists() + "|" + f + "|" + (!MainUtil.isInSubDirectory(working, f)) + ")");
                    return;
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
            log.log(Level.WARNING, "Failed to load a saved clipboard", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    @Command(aliases = {"save"}, usage = "[format] <filename>", desc = "Save a schematic into your clipboard")
    @Deprecated
    @CommandPermissions({"worldedit.clipboard.save", "worldedit.schematic.save", "worldedit.schematic.save.other"})
    public void save(final Player player, final LocalSession session, @Optional("nbt") final String formatName, String filename, @Switch('g') boolean global) throws CommandException, WorldEditException {
        final LocalConfiguration config = this.worldEdit.getConfiguration();
        final ClipboardFormat format = ClipboardFormat.findByAlias(formatName);
        if (format == null) {
            player.printError("Unknown schematic format: " + formatName);
            return;
        }
        File working = this.worldEdit.getWorkingDirectoryFile(config.saveDir);
        File dir = !global && Settings.IMP.PATHS.PER_PLAYER_SCHEMATICS ? new File(working, player.getUniqueId().toString()) : working;
        if (filename.contains("../")) {
            if (!player.hasPermission("worldedit.schematic.save.other")) {
                BBC.NO_PERM.send(player, "worldedit.schematic.save.other");
                return;
            }
            if (filename.startsWith("../")) {
                dir = working;
                filename = filename.substring(3);
            }
        }
        File f = this.worldEdit.getSafeSaveFile(player, dir, filename, format.getExtension(), format.getExtension());
        if (f.getName().replaceAll("." + format.getExtension(), "").isEmpty()) {
            File directory = f.getParentFile();
            if (directory.exists()) {
                int max = MainUtil.getMaxFileId(directory);
                f = new File(directory, max + "." + format.getExtension());
            } else {
                f = new File(directory, "1." + format.getExtension());
            }
        }
        final File parent = f.getParentFile();
        if ((parent != null) && !parent.exists()) {
            if (!parent.mkdirs()) {
                try {
                    Files.createDirectories(parent.toPath());
                } catch (IOException e) {
                    e.printStackTrace();
                    log.info("Could not create folder for schematics!");
                    return;
                }
            }
        }
        try {
            if (!f.exists()) {
                f.createNewFile();
            }
            try (FileOutputStream fos = new FileOutputStream(f)) {
                final ClipboardHolder holder = session.getClipboard();
                final Clipboard clipboard = holder.getClipboard();
                final Transform transform = holder.getTransform();
                final Clipboard target;

                // If we have a transform, bake it into the copy
                if (!transform.isIdentity()) {
                    final FlattenedClipboardTransform result = FlattenedClipboardTransform.transform(clipboard, transform);
                    target = new BlockArrayClipboard(result.getTransformedRegion(), UUID.randomUUID());
                    target.setOrigin(clipboard.getOrigin());
                    Operations.completeLegacy(result.copyTo(target));
                } else {
                    target = clipboard;
                }

                URI uri = null;
                if (holder instanceof URIClipboardHolder) uri = ((URIClipboardHolder) holder).getURI(clipboard);
                if (new PlayerSaveClipboardEvent(player, clipboard, uri, f.toURI()).call()) {
                    try (ClipboardWriter writer = format.getWriter(fos)) {
                        if (writer instanceof StructureFormat) {
                            ((StructureFormat) writer).write(target, player.getName());
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
            log.log(Level.WARNING, "Failed to write a saved clipboard", e);
        }
    }

    @Command(aliases = {"move", "m"}, usage = "<directory>", desc = "Move your loaded schematic", help = "Move your currently loaded schematics", min = 1, max = 1)
    @CommandPermissions({"worldedit.schematic.move", "worldedit.schematic.move.other"})
    public void move(final Player player, final LocalSession session, final CommandContext args) throws WorldEditException {
        final LocalConfiguration config = this.worldEdit.getConfiguration();
        final File working = this.worldEdit.getWorkingDirectoryFile(config.saveDir);
        final File dir = Settings.IMP.PATHS.PER_PLAYER_SCHEMATICS ? new File(working, player.getUniqueId().toString()) : working;
        File destDir = new File(dir, args.getString(0));
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

    @Command(aliases = {"delete", "d"}, usage = "<filename|*>", desc = "Delete a saved schematic", help = "Delete a schematic from the schematic list", min = 1, max = 1)
    @CommandPermissions({"worldedit.schematic.delete", "worldedit.schematic.delete.other"})
    public void delete(final Player player, final LocalSession session, final CommandContext args) throws WorldEditException {
        final LocalConfiguration config = this.worldEdit.getConfiguration();
        final File working = this.worldEdit.getWorkingDirectoryFile(config.saveDir);
        final File dir = Settings.IMP.PATHS.PER_PLAYER_SCHEMATICS ? new File(working, player.getUniqueId().toString()) : working;
        List<File> files = new ArrayList<>();

        final String filename = args.getString(0);

        if (filename.equalsIgnoreCase("*")) {
            files.addAll(getFiles(session.getClipboard()));
        } else {
            File f = MainUtil.resolveRelative(new File(dir, filename));
            files.add(f);
        }
        if (files.isEmpty()) {
            BBC.SCHEMATIC_NONE.send(player);
            return;
        }
        for (File f : files) {
            if (!MainUtil.isInSubDirectory(working, f) || !f.exists()) {
                player.printError("Schematic " + filename + " does not exist! (" + f.exists() + "|" + f + "|" + (!MainUtil.isInSubDirectory(working, f)) + ")");
                continue;
            }
            if (Settings.IMP.PATHS.PER_PLAYER_SCHEMATICS && !MainUtil.isInSubDirectory(dir, f) && !player.hasPermission("worldedit.schematic.delete.other")) {
                BBC.NO_PERM.send(player, "worldedit.schematic.delete.other");
                continue;
            }
            if (!delete(f)) {
                player.printError("Deletion of " + filename + " failed! Maybe it is read-only.");
                continue;
            }
            BBC.FILE_DELETED.send(player, filename);
        }
    }

    private List<File> getFiles(ClipboardHolder clipboard) {
        List<File> files = new ArrayList<>();
        Collection<URI> uris = Collections.emptyList();
        if (clipboard instanceof URIClipboardHolder) {
            uris = ((URIClipboardHolder) clipboard).getURIs();
        }
        for (URI uri : uris) {
            File file = new File(uri);
            if (file.exists()) files.add(file);
        }
        return files;
    }

    private boolean delete(File file) {
        if (file.delete()) {
            new File(file.getParentFile(), "." + file.getName() + ".cached").delete();
            return true;
        }
        return false;
    }

    @Command(aliases = {"formats", "listformats", "f"}, desc = "List available formats", max = 0)
    @CommandPermissions("worldedit.schematic.formats")
    public void formats(final Actor actor) throws WorldEditException {
        BBC.SCHEMATIC_FORMAT.send(actor);
        Message m = new Message(BBC.SCHEMATIC_FORMAT).newline();
        String baseCmd = Commands.getAlias(SchematicCommands.class, "schematic") + " " + Commands.getAlias(SchematicCommands.class, "save");
        boolean first = true;
        for (final ClipboardFormat format : ClipboardFormat.values()) {
            StringBuilder builder = new StringBuilder();
            builder.append(format.name()).append(": ");
            for (final String lookupName : format.getAliases()) {
                if (!first) {
                    builder.append(", ");
                }
                builder.append(lookupName);
                first = false;
            }
            String cmd = baseCmd + " " + format.name() + " <filename>";
            m.text(builder).suggestTip(cmd).newline();
            first = true;
        }
        m.send(actor);
    }


    @Command(
            aliases = {"show"},
            desc = "Show a schematic",
            usage = "[global|mine|<filter>]",
            min = 0,
            max = -1,
            flags = "dnp",
            help = "List all schematics in the schematics directory\n" +
                    " -f <format> restricts by format\n"
    )
    @CommandPermissions("worldedit.schematic.show")
    public void show(Player player, CommandContext args, @Switch('f') String formatName) {
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

    @Command(
            aliases = {"list", "ls", "all"},
            desc = "List saved schematics",
            usage = "[global|mine|<filter>] [page=1]",
            min = 0,
            max = -1,
            flags = "dnp",
            help = "List all schematics in the schematics directory\n" +
                    " -p <page> prints the requested page\n" +
                    " -f <format> restricts by format\n"
    )
    @CommandPermissions("worldedit.schematic.list")
    public void list(FawePlayer fp, Actor actor, CommandContext args, @Switch('p') @Optional("1") int page, @Switch('f') String formatName) throws WorldEditException {
        if (args.argsLength() == 0) {
            BBC.COMMAND_SYNTAX.send(fp, getCommand().usage());
            return;
        }
        LocalConfiguration config = worldEdit.getConfiguration();
        String prefix = config.noDoubleSlash ? "" : "/";
        File dir = worldEdit.getWorkingDirectoryFile(config.saveDir);

        String schemCmd = prefix + Commands.getAlias(SchematicCommands.class, "schematic");
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
                String color;
                String uriStr = uri.toString();
                if (uriStr.startsWith("file:/")) {
                    File file = new File(uri.getPath());
                    name = file.getName();
                    if (file.isDirectory()) {
                        isDir = true;
                        color = "&6";
                    } else {
                        color = "&a";
                        if (name.indexOf('.') != -1) name = name.substring(0, name.lastIndexOf('.'));
                    }
                } else if (uriStr.startsWith("http://") || uriStr.startsWith("https://")) {
                    // url
                    color = "&9";
                } else {
                    color = "&7";
                }

                msg.text("&8 - ");

                if (msg.supportsInteraction()) {
                    if (loaded) {
                        msg.text("&7[&c-&7]").command(unload + " " + relFilePath).tooltip("Unload");
                    } else {
                        msg.text("&7[&a+&7]").command(loadMulti + " " + relFilePath).tooltip("Add to clipboard");
                    }
                    if (!isDir) msg.text("&7[&cX&7]").suggest("/" + delete + " " + relFilePath).tooltip("Delete");
                    else if (hasShow) msg.text("&7[&3O&7]").command(showCmd + " " + args.getJoinedStrings(0) + " " + relFilePath).tooltip("Show");
                    msg.text(color + name);
                    if (isDir) {
                        msg.command(list + " " + relFilePath).tooltip("List");
                    } else {
                        msg.command(loadSingle + " " + relFilePath).tooltip("Load");
                    }
                } else {
                    msg.text(color).text(name);
                }
            }
        });
    }

    public static Class<?> inject() {
        return SchematicCommands.class;
    }
}
