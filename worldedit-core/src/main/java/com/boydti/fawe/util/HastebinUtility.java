package com.boydti.fawe.util;

import com.boydti.fawe.Fawe;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HastebinUtility {

    public static final String BIN_URL = "https://hastebin.com/documents", USER_AGENT = "Mozilla/5.0";
    public static final Pattern PATTERN = Pattern.compile("\\{\"key\":\"([\\S\\s]*)\"\\}");

    public static String upload(final String string) throws IOException {
        final URL url = new URL(BIN_URL);
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setDoOutput(true);

        try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
            outputStream.write(string.getBytes());
            outputStream.flush();
        }

        StringBuilder response;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            response = new StringBuilder();

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }

        Matcher matcher = PATTERN.matcher(response.toString());
        if (matcher.matches()) {
            return "https://hastebin.com/" + matcher.group(1);
        } else {
            throw new RuntimeException("Couldn't read response!");
        }
    }

    public static String upload(final File file) throws IOException {
        final StringBuilder content = new StringBuilder();
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int i = 0;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        for (int i = Math.max(0, lines.size() - 1000); i < lines.size(); i++) {
            content.append(lines.get(i)).append("\n");
        }
        return upload(content.toString());
    }

    public static String debugPaste() throws IOException {
        String settingsYML = HastebinUtility.upload(new File(Fawe.imp().getDirectory(), "config.yml"));
        String messagesYML = HastebinUtility.upload(new File(Fawe.imp().getDirectory(), "message.yml"));
        String commandsYML = HastebinUtility.upload(new File(Fawe.imp().getDirectory(), "commands.yml"));
        String latestLOG;
        try {
            latestLOG = HastebinUtility.upload(new File(Fawe.imp().getDirectory(), "../../logs/latest.log"));
        } catch (IOException ignored) {
            latestLOG = "too big :(";
        }
        StringBuilder b = new StringBuilder();
        b.append(
                "# Welcome to this paste\n# It is meant to provide us at IntellectualSites with better information about your "
                        + "problem\n\n# We will start with some informational files\n");
        b.append("links.config_yml: ").append(settingsYML).append('\n');
        b.append("links.messages_yml: ").append(messagesYML).append('\n');
        b.append("links.commands_yml: ").append(commandsYML).append('\n');
        b.append("links.latest_log: ").append(latestLOG).append('\n');
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

        String link = HastebinUtility.upload(b.toString());
        return link;
    }

}
