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

package com.sk89q.worldedit.extent.clipboard.io.share;

import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.util.arkitektonika.ArkitektonikaResponse;
import com.fastasyncworldedit.core.util.arkitektonika.ArkitektonikaSchematicUploader;
import com.google.common.collect.ImmutableSet;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.event.ClickEvent;
import com.sk89q.worldedit.util.formatting.text.format.TextColor;
import com.sk89q.worldedit.util.paste.EngineHubPaste;
import com.sk89q.worldedit.util.paste.PasteMetadata;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Consumer;

/**
 * A collection of natively supported clipboard share destinations.
 */
public enum BuiltInClipboardShareDestinations implements ClipboardShareDestination {

    /**
     * The EngineHub pastebin service, at https://paste.enginehub.org/
     */
    ENGINEHUB_PASTEBIN("enginehub_paste", "ehpaste") {
        @Override
        public String getName() {
            return "EngineHub Paste";
        }

        @Override
        public Consumer<Actor> share(ClipboardShareMetadata metadata, ShareOutputProvider serializer) throws Exception {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            serializer.writeTo(outputStream);

            PasteMetadata pasteMetadata = new PasteMetadata();
            pasteMetadata.author = metadata.author();
            pasteMetadata.extension = metadata.format().getPrimaryFileExtension();
            pasteMetadata.name = metadata.name();
            EngineHubPaste pasteService = new EngineHubPaste();

            URL url = pasteService.paste(new String(
                    Base64.getEncoder().encode(outputStream.toByteArray()),
                    StandardCharsets.UTF_8
            ), pasteMetadata).call();
            String urlString = url.toExternalForm() + ".schem";
            return actor -> actor.printInfo(TextComponent.of(urlString).clickEvent(ClickEvent.openUrl(urlString)));
        }

        @Override
        public ClipboardFormat getDefaultFormat() {
            return BuiltInClipboardFormat.SPONGE_V2_SCHEMATIC;
        }

        @Override
        public boolean supportsFormat(ClipboardFormat format) {
            return format == getDefaultFormat();
        }
    },

    //FAWE start - add arkitektonika
    ARKITEKTONIKA("arkitektonika", "fawe") {

        private ArkitektonikaSchematicUploader uploader;

        @Override
        public String getName() {
            return "Arkitektonika";
        }

        @Override
        public Consumer<Actor> share(final ClipboardShareMetadata metadata, final ShareOutputProvider serializer) throws
                Exception {
            if (uploader == null) {
                uploader = new ArkitektonikaSchematicUploader(Settings.settings().WEB.ARKITEKTONIKA_BACKEND_URL);
            }
            final ArkitektonikaResponse response = uploader.uploadBlocking(metadata, serializer);
            final String downloadUrl = Settings.settings().WEB.ARKITEKTONIKA_DOWNLOAD_URL.replace("{key}", response.downloadKey());
            final String deletionUrl = Settings.settings().WEB.ARKITEKTONIKA_DELETE_URL.replace("{key}", response.deletionKey());
            return actor -> {
                actor.print(Caption.of(
                        "worldedit.schematic.share.response.arkitektonika.download",
                        Caption.of("worldedit.schematic.share.response.arkitektonika.click-here")
                                .color(TextColor.GREEN).clickEvent(ClickEvent.openUrl(downloadUrl))
                ));
                actor.print(Caption.of(
                        "worldedit.schematic.share.response.arkitektonika.delete",
                        Caption.of("worldedit.schematic.share.response.arkitektonika.click-here")
                                .color(TextColor.RED).clickEvent(ClickEvent.openUrl(deletionUrl))
                ));
            };
        }

        @Override
        public ClipboardFormat getDefaultFormat() {
            return BuiltInClipboardFormat.FAST;
        }

        @Override
        public boolean supportsFormat(final ClipboardFormat format) {
            return format == BuiltInClipboardFormat.SPONGE_SCHEMATIC ||
                    format == BuiltInClipboardFormat.FAST ||
                    format == BuiltInClipboardFormat.MCEDIT_SCHEMATIC;
        }
    };
    //FAWE end

    private final ImmutableSet<String> aliases;

    BuiltInClipboardShareDestinations(String... aliases) {
        this.aliases = ImmutableSet.copyOf(aliases);
    }

    @Override
    public ImmutableSet<String> getAliases() {
        return this.aliases;
    }

    @Override
    public String getName() {
        return name();
    }
}
