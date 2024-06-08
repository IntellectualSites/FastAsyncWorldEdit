package com.fastasyncworldedit.core.util.arkitektonika;

import com.fastasyncworldedit.core.internal.exception.FaweException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sk89q.worldedit.extent.clipboard.io.share.ClipboardShareMetadata;
import com.sk89q.worldedit.extent.clipboard.io.share.ShareOutputProvider;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.format.TextColor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

public class ArkitektonikaSchematicUploader {

    private static final String BOUNDARY_IDENTIFIER = "--";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private final String apiUrl;

    public ArkitektonikaSchematicUploader(String apiUrl) {
        this.apiUrl = apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;
    }

    public ArkitektonikaResponse uploadBlocking(ClipboardShareMetadata meta, ShareOutputProvider provider) throws IOException,
            InterruptedException {
        String boundary = UUID.randomUUID().toString();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        provider.writeTo(outputStream);

        final HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.concat(
                HttpRequest.BodyPublishers.ofString(BOUNDARY_IDENTIFIER + boundary + "\r\n"),
                HttpRequest.BodyPublishers.ofString("Content-Disposition: form-data; name=\"schematic\"; filename=\"" + meta.name() + "." + meta.format().getPrimaryFileExtension() + "\"\r\n\r\n"),
                HttpRequest.BodyPublishers.ofByteArray(outputStream.toByteArray()),
                HttpRequest.BodyPublishers.ofString("\r\n" + BOUNDARY_IDENTIFIER + boundary + BOUNDARY_IDENTIFIER)
        );

        final HttpResponse<String> response = HTTP_CLIENT.send(HttpRequest.newBuilder()
                .uri(URI.create(this.apiUrl + "/upload"))
                .header("Content-Type", "multipart/form-data; boundary=\"" + boundary + "\"")
                .POST(bodyPublisher).build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new FaweException(TextComponent
                    .of("Arkitektonika returned status code " + response.statusCode())
                    .color(TextColor.RED));
        }
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        return new ArkitektonikaResponse(
                json.get("download_key").getAsString(),
                json.get("delete_key").getAsString()
        );
    }

}
