/*
 * FastAsyncWorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) IntellectualSites <https://intellectualsites.com>
 * Copyright (C) IntellectualSites team and contributors
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

package com.fastasyncworldedit.core.configuration.caption;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.util.io.ResourceLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caption map for FAWE-specific messages stored in MiniMessage format.
 *
 * <p>Messages are loaded from {@code lang/fawe_messages.json} bundled in the classpath.
 * Users may override individual messages by placing a {@code lang/fawe_messages.json}
 * file inside the FAWE data folder; any keys present in that file take precedence over
 * the bundled defaults.</p>
 *
 * <p>Unlike {@link com.sk89q.worldedit.util.translation.TranslationManager}, this class
 * does <em>not</em> double single-quotes, because MiniMessage does not use
 * {@link java.text.MessageFormat} syntax.</p>
 *
 * @see FaweCaption
 */
public final class FaweCaptionMap {

    private static final Logger LOGGER = LogManager.getLogger("FAWE/" + FaweCaptionMap.class.getSimpleName());
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final Type STRING_MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();
    private static final String MESSAGES_FILE = "lang/fawe_messages.json";

    private static volatile FaweCaptionMap instance;

    private final Map<String, String> messages;

    private FaweCaptionMap(final Map<String, String> messages) {
        this.messages = messages;
    }

    /**
     * Returns the singleton instance, loading it lazily on first call.
     *
     * @return the caption map instance
     */
    public static FaweCaptionMap getInstance() {
        if (instance == null) {
            synchronized (FaweCaptionMap.class) {
                if (instance == null) {
                    instance = load();
                }
            }
        }
        return instance;
    }

    /**
     * Reload the caption map from disk (e.g. on {@code /worldedit reload}).
     */
    public static void reload() {
        synchronized (FaweCaptionMap.class) {
            instance = load();
        }
    }

    private static FaweCaptionMap load() {
        Map<String, String> messages = new ConcurrentHashMap<>();

        // Load bundled defaults from the classpath
        URL resource = FaweCaptionMap.class.getClassLoader().getResource(MESSAGES_FILE);
        if (resource != null) {
            try (InputStream stream = resource.openStream();
                 InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                Map<String, String> loaded = GSON.fromJson(reader, STRING_MAP_TYPE);
                if (loaded != null) {
                    messages.putAll(loaded);
                }
            } catch (IOException e) {
                LOGGER.error("Failed to load bundled {}", MESSAGES_FILE, e);
            }
        } else {
            LOGGER.warn("Bundled {} not found in classpath", MESSAGES_FILE);
        }

        // Apply user overrides from the FAWE/WorldEdit data folder
        Path localFile = null;
        try {
            ResourceLoader resourceLoader = WorldEdit.getInstance().getPlatformManager()
                    .queryCapability(com.sk89q.worldedit.extension.platform.Capability.CONFIGURATION)
                    .getResourceLoader();
            localFile = resourceLoader.getLocalResource(MESSAGES_FILE);

            // Save bundled defaults if the local file does not yet exist
            if (!Files.exists(localFile) && !messages.isEmpty()) {
                try {
                    Files.createDirectories(localFile.getParent());
                    try (Writer writer = Files.newBufferedWriter(localFile, StandardCharsets.UTF_8)) {
                        GSON.toJson(messages, STRING_MAP_TYPE, writer);
                    }
                    LOGGER.info("Saved default {} to {}", MESSAGES_FILE, localFile);
                } catch (IOException e) {
                    LOGGER.warn("Failed to save default {} to {}", MESSAGES_FILE, localFile, e);
                }
            }

            if (Files.exists(localFile)) {
                try (InputStream stream = Files.newInputStream(localFile);
                     InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    Map<String, String> overrides = GSON.fromJson(reader, STRING_MAP_TYPE);
                    if (overrides != null) {
                        messages.putAll(overrides);
                        LOGGER.info("Loaded {} user-defined override(s) from {}", overrides.size(), localFile);
                    }
                } catch (IOException e) {
                    LOGGER.warn("Failed to load user overrides from {}", localFile, e);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not load user overrides for {}", MESSAGES_FILE, e);
        }

        return new FaweCaptionMap(Collections.unmodifiableMap(messages));
    }

    /**
     * Get the raw MiniMessage template string for the given key.
     *
     * @param key    the caption key, e.g. {@code "fawe.error.no-perm"}
     * @param locale the desired locale (reserved for future i18n support)
     * @return the raw MiniMessage template, or the key itself if not found
     */
    public String getMessage(final String key, final Locale locale) {
        return messages.getOrDefault(key, key);
    }

    /**
     * Get the raw MiniMessage template string for the given key using the default locale.
     *
     * @param key the caption key
     * @return the raw MiniMessage template, or the key itself if not found
     */
    public String getMessage(final String key) {
        return messages.getOrDefault(key, key);
    }

    /**
     * Returns {@code true} if a message with the given key is registered.
     *
     * @param key the caption key to check
     * @return {@code true} if present
     */
    public boolean containsKey(final String key) {
        return messages.containsKey(key);
    }

}
