package com.boydti.fawe.util;

import com.boydti.fawe.Fawe;
import com.google.common.base.Charsets;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.*;

/**
 * Single class paster for the Incendo paste service
 *
 * @author Sauilitired
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class IncendoPaster {

    /**
     * Upload service URL
     */
    public static final String UPLOAD_PATH = "https://incendo.org/paste/upload";
    /**
     * Valid paste applications
     */
    public static final Collection<String>
        VALID_APPLICATIONS = Arrays
        .asList("plotsquared", "fastasyncworldedit", "incendopermissions", "kvantum");

    private final Collection<PasteFile> files = new ArrayList<>();
    private final String pasteApplication;

    /**
     * Construct a new paster
     *
     * @param pasteApplication The application that is sending the paste
     */
    public IncendoPaster(final String pasteApplication) {
        if (pasteApplication == null || pasteApplication.isEmpty()) {
            throw new IllegalArgumentException("paste application cannot be null, nor empty");
        }
        if (!VALID_APPLICATIONS.contains(pasteApplication.toLowerCase(Locale.ENGLISH))) {
            throw new IllegalArgumentException(
                String.format("Unknown application name: %s", pasteApplication));
        }
        this.pasteApplication = pasteApplication;
    }

    /**
     * Get an immutable collection containing all the files that have been added to this paster
     *
     * @return Unmodifiable collection
     */
    public final Collection<PasteFile> getFiles() {
        return Collections.unmodifiableCollection(this.files);
    }

    /**
     * Add a file to the paster
     *
     * @param file File to paste
     */
    public void addFile(final PasteFile file) {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        // Check to see that no duplicate files are submitted
        for (final PasteFile pasteFile : this.files) {
            if (pasteFile.fileName.equalsIgnoreCase(file.getFileName())) {
                throw new IllegalArgumentException(String.format("Found duplicate file with name %s",
                    file.getFileName()));
            }
        }
        this.files.add(file);
    }

    /**
     * Create a JSON string from the submitted information
     *
     * @return compiled JSON string
     */
    private String toJsonString() {
        final StringBuilder builder = new StringBuilder("{\n");
        builder.append("\"paste_application\": \"").append(this.pasteApplication).append("\",\n\"files\": \"");
        Iterator<PasteFile> fileIterator = this.files.iterator();
        while (fileIterator.hasNext()) {
            final PasteFile file = fileIterator.next();
            builder.append(file.getFileName());
            if (fileIterator.hasNext()) {
                builder.append(",");
            }
        }
        builder.append("\",\n");
        fileIterator = this.files.iterator();
        while (fileIterator.hasNext()) {
            final PasteFile file = fileIterator.next();
            builder.append("\"file-").append(file.getFileName()).append("\": \"")
                .append(file.getContent().replaceAll("\"", "\\\\\"")).append("\"");
            if (fileIterator.hasNext()) {
                builder.append(",\n");
            }
        }
        builder.append("\n}");
        return builder.toString();
    }

    /**
     * Upload the paste and return the status message
     *
     * @return Status message
     * @throws Throwable any and all exceptions
     */
    public final String upload() throws Throwable {
        final URL url = new URL(UPLOAD_PATH);
        final URLConnection connection = url.openConnection();
        final HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setDoOutput(true);
        final byte[] content = toJsonString().getBytes(Charsets.UTF_8);
        httpURLConnection.setFixedLengthStreamingMode(content.length);
        httpURLConnection.setRequestProperty("Content-Type", "application/json");
        httpURLConnection.setRequestProperty("Accept", "*/*");
        httpURLConnection.connect();
        try (final OutputStream stream = httpURLConnection.getOutputStream()) {
            stream.write(content);
        }
        if (!httpURLConnection.getResponseMessage().contains("OK")) {
            throw new IllegalStateException(String.format("Server returned status: %d %s",
                httpURLConnection.getResponseCode(), httpURLConnection.getResponseMessage()));
        }
        final StringBuilder input = new StringBuilder();
        try (final BufferedReader inputStream = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()))) {
            String line;
            while ((line = inputStream.readLine()) != null) {
                input.append(line).append("\n");
            }
        }
        return input.toString();
    }

    /**
     * Simple class that represents a paste file
     */
    public static class PasteFile {

        private final String fileName;
        private final String content;

        /**
         * Construct a new paste file
         *
         * @param fileName File name, cannot be empty, nor null
         * @param content File content, cannot be empty, nor null
         */
        public PasteFile(final String fileName, final String content) {
            if (fileName == null || fileName.isEmpty()) {
                throw new IllegalArgumentException("file name cannot be null, nor empty");
            }
            if (content == null || content.isEmpty()) {
                throw new IllegalArgumentException("content cannot be null, nor empty");
            }
            this.fileName = fileName;
            this.content = content;
        }

        /**
         * Get the file name
         *
         * @return File name
         */
        public String getFileName() {
            return this.fileName;
        }

        /**
         * Get the file content as a single string
         *
         * @return File content
         */
        public String getContent() {
            return this.content;
        }
    }

    public static String debugPaste() throws IOException {
        final IncendoPaster incendoPaster = new IncendoPaster("fastasyncworldedit");

        StringBuilder b = new StringBuilder();
        b.append(
            "# Welcome to this paste\n# It is meant to provide us at IntellectualSites with better information about your "
                + "problem\n");
        b.append("\n# Server Information\n");
        b.append("server.platform: ").append(Fawe.imp().getPlatform()).append('\n');
        b.append(Fawe.imp().getDebugInfo()).append('\n');
        b.append("\n\n# YAY! Now, let's see what we can find in your JVM\n");
        Runtime runtime = Runtime.getRuntime();
        b.append("memory.free: ").append(runtime.freeMemory()).append('\n');
        b.append("memory.max: ").append(runtime.maxMemory()).append('\n');
        b.append("java.specification.version: '").append(System.getProperty("java.specification.version")).append("'\n");
        b.append("java.vendor: '").append(System.getProperty("java.vendor")).append("'\n");
        b.append("java.version: '").append(System.getProperty("java.version")).append("'\n");
        b.append("os.arch: '").append(System.getProperty("os.arch")).append("'\n");
        b.append("os.name: '").append(System.getProperty("os.name")).append("'\n");
        b.append("os.version: '").append(System.getProperty("os.version")).append("'\n\n");
        b.append("# Okay :D Great. You are now ready to create your bug report!");
        b.append("\n# You can do so at https://github.com/boy0001/FastAsyncWorldedit/issues");
        b.append("\n# or via our Discord at https://discord.gg/ngZCzbU");
        incendoPaster.addFile(new IncendoPaster.PasteFile("information", b.toString()));

        try {
            final File logFile = new File(Fawe.imp().getDirectory(), "../../logs/latest.log");
            final String file;
            if (Files.size(logFile.toPath()) > 14_000_000) {
                file = "too big :(";
            } else {
                file = readFile(logFile);
            }
            incendoPaster.addFile(new IncendoPaster.PasteFile("latest.log", file));
        } catch (IOException ignored) {
        }

        incendoPaster.addFile(new PasteFile("config.yml", readFile(new File(Fawe.imp().getDirectory(), "config.yml"))));
        incendoPaster.addFile(new PasteFile("message.yml", readFile(new File(Fawe.imp().getDirectory(), "message.yml"))));
        incendoPaster.addFile(new PasteFile("commands.yml", readFile(new File(Fawe.imp().getDirectory(), "commands.yml"))));

        final String rawResponse;
        try {
            rawResponse = incendoPaster.upload();
        } catch (Throwable throwable) {
            throw new IOException(String.format("Failed to upload files: %s", throwable.getMessage()), throwable);
        }
        final JsonObject jsonObject = new JsonParser().parse(rawResponse).getAsJsonObject();

        if (jsonObject.has("created")) {
            final String pasteId = jsonObject.get("paste_id").getAsString();
            return String.format("https://incendo.org/paste/view/%s", pasteId);
        } else {
            throw new IOException(String.format("Failed to upload files: %s",
                jsonObject.get("response").getAsString()));
        }
    }

    private static String readFile(final File file) throws IOException {
        final StringBuilder content = new StringBuilder();
        final List<String> lines = new ArrayList<>();
        try (final BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        for (int i = Math.max(0, lines.size() - 1000); i < lines.size(); i++) {
            content.append(lines.get(i)).append("\n");
        }
        return content.toString();
    }

}
