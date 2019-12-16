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

package com.sk89q.worldedit.extent.clipboard.io;

import static com.google.common.base.Preconditions.checkNotNull;

import com.boydti.fawe.config.Caption;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.clipboard.LazyClipboardHolder;
import com.boydti.fawe.object.clipboard.MultiClipboardHolder;
import com.boydti.fawe.object.clipboard.URIClipboardHolder;
import com.boydti.fawe.object.io.FastByteArrayOutputStream;
import com.boydti.fawe.util.MainUtil;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.Actor;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.annotation.Nullable;

public class ClipboardFormats {

    private static final Map<String, ClipboardFormat> aliasMap = new HashMap<>();
    private static final Multimap<String, ClipboardFormat> fileExtensionMap = HashMultimap.create();
    private static final List<ClipboardFormat> registeredFormats = new ArrayList<>();

    public static void registerClipboardFormat(ClipboardFormat format) {
        checkNotNull(format);

        for (String key : format.getAliases()) {
            String lowKey = key.toLowerCase(Locale.ENGLISH);
            ClipboardFormat old = aliasMap.put(lowKey, format);
            if (old != null) {
                aliasMap.put(lowKey, old);
                WorldEdit.logger.warn(format.getClass().getName() + " cannot override existing alias '" + lowKey + "' used by " + old.getClass().getName());
            }
        }
        for (String ext : format.getFileExtensions()) {
            String lowExt = ext.toLowerCase(Locale.ENGLISH);
            fileExtensionMap.put(lowExt, format);
        }
        registeredFormats.add(format);
    }

    static {
        for (BuiltInClipboardFormat format : BuiltInClipboardFormat.values()) {
            registerClipboardFormat(format);
        }
    }

    /**
     * Find the clipboard format named by the given alias.
     *
     * @param alias
     *            the alias
     * @return the format, otherwise null if none is matched
     */
    @Nullable
    public static ClipboardFormat findByAlias(String alias) {
        checkNotNull(alias);
        return aliasMap.get(alias.toLowerCase(Locale.ENGLISH).trim());
    }

    /**
     * Detect the format of given a file.
     *
     * @param file
     *            the file
     * @return the format, otherwise null if one cannot be detected
     */
    @Nullable
    public static ClipboardFormat findByFile(File file) {
        checkNotNull(file);

        for (ClipboardFormat format : registeredFormats) {
            if (format.isFormat(file)) {
                return format;
            }
        }

        return null;
    }

    /**
     * Detect the format using the given extension
     * @param extension the extension
     * @return the format, otherwise null if one cannot be detected
     */
    @Nullable
    public static ClipboardFormat findByExtension(String extension) {
        checkNotNull(extension);

        Collection<Entry<String, ClipboardFormat>> entries = getFileExtensionMap().entries();
        for(Map.Entry<String, ClipboardFormat> entry : entries) {
            if(entry.getKey().equalsIgnoreCase(extension)) {
                return entry.getValue();
            }
        }
        return null;

    }

    /**
     * @return a multimap from a file extension to the potential matching formats.
     */
    public static Multimap<String, ClipboardFormat> getFileExtensionMap() {
        return Multimaps.unmodifiableMultimap(fileExtensionMap);
    }

    public static Collection<ClipboardFormat> getAll() {
        return Collections.unmodifiableCollection(registeredFormats);
    }

    /**
     * Not public API, only used by SchematicCommands.
     * It is not in SchematicCommands because it may rely on internal register calls.
     */
    public static String[] getFileExtensionArray() {
        return fileExtensionMap.keySet().toArray(new String[fileExtensionMap.keySet().size()]);
    }

    private ClipboardFormats() {
    }

    public static MultiClipboardHolder loadAllFromInput(Actor player, String input, ClipboardFormat format, boolean message) throws IOException {
        checkNotNull(player);
        checkNotNull(input);
        WorldEdit worldEdit = WorldEdit.getInstance();
        LocalConfiguration config = worldEdit.getConfiguration();
        if (input.startsWith("url:")) {
            if (!player.hasPermission("worldedit.schematic.load.web")) {
                if (message) player.print(Caption.of("fawe.error.no.perm", "worldedit.schematic.load.web"));
                return null;
            }
            URL base = new URL(Settings.IMP.WEB.URL);
            input = new URL(base, "uploads/" + input.substring(4) + "." + format.getPrimaryFileExtension()).toString();
        }
        if (input.startsWith("http")) {
            if (!player.hasPermission("worldedit.schematic.load.asset")) {
                if (message) player.print(Caption.of("fawe.error.no.perm", "worldedit.schematic.load.asset"));
                return null;
            }
            URL url = new URL(input);
            URL webInterface = new URL(Settings.IMP.WEB.ASSETS);
            if (!url.getHost().equalsIgnoreCase(webInterface.getHost())) {
                if (message) player.print(Caption.of("fawe.error.web.unauthorized", url));
                return null;
            }
            return loadAllFromUrl(url);
        } else {
            if (Settings.IMP.PATHS.PER_PLAYER_SCHEMATICS && Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}").matcher(input).find() && !player.hasPermission("worldedit.schematic.load.other")) {
                player.print(Caption.of("fawe.error.no.perm", "worldedit.schematic.load.other"));
                return null;
            }
            File working = worldEdit.getWorkingDirectoryFile(config.saveDir);
            File dir = Settings.IMP.PATHS.PER_PLAYER_SCHEMATICS ? new File(working, player.getUniqueId().toString()) : working;
            File f;
            if (input.startsWith("#")) {
                String[] extensions;
                if (format != null) {
                    extensions = format.getFileExtensions().toArray(new String[0]);
                } else {
                    extensions = ClipboardFormats.getFileExtensionArray();
                }
                f = player.openFileOpenDialog(extensions);
                if (f == null || !f.exists()) {
                    if (message) player.printError("Schematic " + input + " does not exist! (" + f + ")");
                    return null;
                }
            } else {
                if (Settings.IMP.PATHS.PER_PLAYER_SCHEMATICS && Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}").matcher(input).find() && !player.hasPermission("worldedit.schematic.load.other")) {
                    if (message) player.print(Caption.of("fawe.error.no.perm", "worldedit.schematic.load.other"));
                    return null;
                }
                if (format == null && input.matches(".*\\.[\\w].*")) {
                    String extension = input.substring(input.lastIndexOf('.') + 1);
                    format = findByExtension(extension);
                }
                f = MainUtil.resolve(dir, input, format, true);
            }
            if (f == null || !f.exists()) {
                if (!input.contains("../")) {
                    dir = worldEdit.getWorkingDirectoryFile(config.saveDir);
                    f = MainUtil.resolve(dir, input, format, true);
                }
            }
            if (f == null || !f.exists() || !MainUtil.isInSubDirectory(working, f)) {
                if (message) player.printError("Schematic " + input + " does not exist! (" + ((f != null) && f.exists()) + "|" + f + "|" + (f != null && !MainUtil.isInSubDirectory(working, f)) + ")");
                return null;
            }
            if (format == null && f.isFile()) {
                format = findByFile(f);
                if (format == null) {
                    player.print(Caption.of("fawe.worldedit.clipboard.clipboard.invalid.format" , f.getName()));
                    return null;
                }
            }
            if (!f.exists()) {
                if (message) player.print(Caption.of("fawe.error.schematic.not.found" , input));
                return null;
            }
            if (!f.isDirectory()) {
                ByteSource source = Files.asByteSource(f);
                URI uri = f.toURI();
                return new MultiClipboardHolder(uri, new LazyClipboardHolder(f.toURI(), source, format, null));
            }
            URIClipboardHolder[] clipboards = loadAllFromDirectory(f);
            if (clipboards.length < 1) {
                if (message) player.print(Caption.of("fawe.error.schematic.not.found" , input));
                return null;
            }
            return new MultiClipboardHolder(f.toURI(), clipboards);
        }
    }

    public static URIClipboardHolder[] loadAllFromDirectory(File dir) {
        HashSet<String> extensions = new HashSet<>(Arrays.asList(ClipboardFormats.getFileExtensionArray()));
        File[] files = dir.listFiles(pathname -> {
            String input = pathname.getName();
            String extension = input.substring(input.lastIndexOf('.') + 1);
            return (extensions.contains(extension.toLowerCase(Locale.ENGLISH)));
        });
        LazyClipboardHolder[] clipboards = new LazyClipboardHolder[files.length];
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            ByteSource source = Files.asByteSource(file);
            ClipboardFormat format = findByFile(file);
            clipboards[i] = new LazyClipboardHolder(file.toURI(), source, format, null);
        }
        return clipboards;
    }

    public static MultiClipboardHolder loadAllFromUrl(URL url) throws IOException {
        List<LazyClipboardHolder> clipboards = new ArrayList<>();
        try (ReadableByteChannel rbc = Channels.newChannel(url.openStream())) {
            try (InputStream in = Channels.newInputStream(rbc)) {
                try (ZipInputStream zip = new ZipInputStream(in)) {
                    ZipEntry entry;
                    byte[] buffer = new byte[8192];
                    while ((entry = zip.getNextEntry()) != null) {
                        String filename = entry.getName();
                        ClipboardFormat format = findByExtension(filename);
                        if (format != null) {
                            FastByteArrayOutputStream out = new FastByteArrayOutputStream();
                            int len;
                            while ((len = zip.read(buffer)) > 0) {
                                out.write(buffer, 0, len);
                            }
                            byte[] array = out.toByteArray();
                            ByteSource source = ByteSource.wrap(array);
                            LazyClipboardHolder clipboard = new LazyClipboardHolder(url.toURI(), source, format, null);
                            clipboards.add(clipboard);
                        }
                    }
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        LazyClipboardHolder[] arr = clipboards.toArray(new LazyClipboardHolder[0]);
        try {
            MultiClipboardHolder multi = new MultiClipboardHolder(url.toURI());
            for (LazyClipboardHolder h : arr) multi.add(h);
            return multi;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
