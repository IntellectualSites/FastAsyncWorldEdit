package com.boydti.fawe.util.metrics;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweVersion;
import com.boydti.fawe.configuration.file.YamlConfiguration;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.io.PGZIPOutputStream;
import com.boydti.fawe.util.TaskManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.net.ssl.HttpsURLConnection;

/**
 * bStats collects some data for plugin authors.
 *
 * Check out https://bStats.org/ to learn more about bStats!
 */
public class BStats implements Closeable {

    // The version of this bStats class
    public static final int B_STATS_VERSION = 1;

    // The url to which the data is sent
    private final String url;

    // The plugin
    private final String plugin;
    private final String platform;
    private final boolean online;
    private final String serverVersion;
    private final String pluginVersion;
    private Timer timer;
    private Gson gson = new Gson();

    // Is bStats enabled on this server?
    private volatile boolean enabled;

    // The uuid of the server
    private UUID serverUUID;

    // Should failed requests be logged?
    private boolean logFailedRequests = false;

    // A list with all known metrics class objects including this one
    private static Class<?> usedMetricsClass;
    private static final ConcurrentLinkedQueue<Object> knownMetricsInstances = new ConcurrentLinkedQueue<>();

    public BStats() {
        this("FastAsyncWorldEdit", Fawe.get().getVersion(), Fawe.imp().getPlatformVersion(), Fawe.imp().getPlatform(), Fawe.imp().isOnlineMode());
    }

    public int getPlayerCount() {
        return Fawe.imp() == null ? 1 : Fawe.imp().getPlayerCount();
    }

    private BStats(String plugin, FaweVersion faweVersion, String serverVersion, String platform, boolean online) {
        this.url = "https://bStats.org/submitData/" + platform;
        this.plugin = plugin;
        this.pluginVersion = "" + faweVersion;
        this.serverVersion = serverVersion;
        this.platform = platform;
        this.online = online;

        File configFile = new File(getJarFile().getParentFile(), "bStats" + File.separator + "config.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        if (config.isSet("serverUuid")) {
            try {
                serverUUID = UUID.fromString(config.getString("serverUuid"));
            } catch (IllegalArgumentException ignore) {}
        }
        // Check if the config file exists
        if (serverUUID == null) {

            // Add default values
            config.addDefault("enabled", true);
            // Every server gets it's unique random id.
            config.addDefault("serverUuid", (serverUUID = UUID.randomUUID()).toString());
            // Should failed request be logged?
            config.addDefault("logFailedRequests", false);

            // Inform the server owners about bStats
            config.options().header(
                    "bStats collects some data for plugin authors like how many servers are using their plugins.\n" +
                            "To honor their work, you should not disable it.\n" +
                            "This has nearly no effect on the server performance!\n" +
                            "Check out https://bStats.org/ to learn more :)"
            ).copyDefaults(true);
            try {
                config.save(configFile);
            } catch (IOException ignored) { }
        }


        if (usedMetricsClass != null) {
            // Already an instance of this class
            linkMetrics(this);
            return;
        }
        this.usedMetricsClass = getFirstBStatsClass();
        if (usedMetricsClass == null) {
            // Failed to get first metrics class
            return;
        }
        if (usedMetricsClass == getClass()) {
            // We are the first! :)
            linkMetrics(this);
            enabled = true;
        } else {
            // We aren't the first so we link to the first metrics class
            try {
                usedMetricsClass.getMethod("linkMetrics", Object.class).invoke(null,this);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                if (logFailedRequests) {
                    System.out.println("Failed to link to first metrics class " + usedMetricsClass.getName() + "!");
                }
            }
        }
    }

    public void start() {
        if (enabled) {
            startSubmitting();
        }
    }

    /**
     * Links an other metrics class with this class.
     * This method is called using Reflection.
     *
     * @param metrics An object of the metrics class to link.
     */
    public static void linkMetrics(Object metrics) {
        if (!knownMetricsInstances.contains(metrics)) knownMetricsInstances.add(metrics);
    }

    /**
     * Gets the plugin specific data.
     * This method is called using Reflection.
     *
     * @return The plugin specific data.
     */
    public JsonObject getPluginData() {
        JsonObject data = new JsonObject();

        data.addProperty("pluginName", plugin);
        data.addProperty("pluginVersion", pluginVersion);

        JsonArray customCharts = new JsonArray();
        data.add("customCharts", customCharts);

        return data;
    }

    private void startSubmitting() {
        this.timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!enabled) {
                    timer.cancel();
                    return;
                }
                submitData();
            }
        // No 2m delay, as this is only started after the server is loaded
        }, 0, 1000*60*30);
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    @Override
    public void close() {
        enabled = false;
        if (timer != null) {
            timer.cancel();
        }
    }

    /**
     * Gets the server specific data.
     *
     * @return The server specific data.
     */
    private JsonObject getServerData() {
        int playerAmount = getPlayerCount();
        int onlineMode = online ? 1 : 0;

        int managedServers = 1;

        // OS/Java specific data
        String javaVersion = System.getProperty("java.version");
        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");
        String osVersion = System.getProperty("os.version");
        int coreCount = Runtime.getRuntime().availableProcessors();

        JsonObject data = new JsonObject();

        data.addProperty("serverUUID", serverUUID.toString());

        data.addProperty("playerAmount", playerAmount);
        data.addProperty("managedServers", managedServers);
        data.addProperty("onlineMode", onlineMode);
        data.addProperty(platform + "Version", serverVersion);

        data.addProperty("javaVersion", javaVersion);
        data.addProperty("osName", osName);
        data.addProperty("osArch", osArch);
        data.addProperty("osVersion", osVersion);
        data.addProperty("coreCount", coreCount);

        return data;
    }

    /**
     * Collects the data and sends it afterwards.
     */
    private void submitData() {
        final JsonObject data = getServerData();

        final JsonArray pluginData = new JsonArray();
        // Search for all other bStats Metrics classes to get their plugin data
        for (Object metrics : knownMetricsInstances) {
            Object plugin = TaskManager.IMP.sync(new RunnableVal<Object>() {
                @Override
                public void run(Object value) {
                    try {
                    this.value = metrics.getClass().getMethod("getPluginData").invoke(metrics);
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NullPointerException | JsonSyntaxException ignored) {}
                }
            });
            if (plugin != null) {
                if (plugin instanceof JsonObject) {
                    pluginData.add((JsonObject) plugin);
                } else {
                    pluginData.add(gson.fromJson(plugin.toString(), JsonObject.class));
                }
            }
        }

        data.add("plugins", pluginData);

        try {
            // Send the data
            sendData(data);
        } catch (Exception e) {
            // Something went wrong! :(
            if (logFailedRequests) {
                System.err.println("Could not submit plugin stats!");
            }
        }
    }

    /**
     * Gets the first bStat Metrics class.
     *
     * @return The first bStats metrics class.
     */
    private Class<?> getFirstBStatsClass() {
        Path configPath = getJarFile().toPath().getParent().resolve("bStats");
        configPath.toFile().mkdirs();
        File tempFile = new File(configPath.toFile(), "temp.txt");

        try {
            String className = readFile(tempFile);
            if (className != null) {
                try {
                    // Let's check if a class with the given name exists.
                    return Class.forName(className);
                } catch (ClassNotFoundException ignored) { }
            }
            writeFile(tempFile, getClass().getName());
            return getClass();
        } catch (IOException e) {
            if (logFailedRequests) {
                System.err.println("Failed to get first bStats class!");
            }
            return null;
        }
    }

    private File getJarFile() {
        try {
            URL url = BStats.class.getProtectionDomain().getCodeSource().getLocation();
            return new File(new URL(url.toURI().toString().split("\\!")[0].replaceAll("jar:file", "file")).toURI().getPath());
        } catch (MalformedURLException | URISyntaxException | SecurityException e) {
            return new File(".", "plugins");
        }
    }

    /**
     * Reads the first line of the file.
     *
     * @param file The file to read. Cannot be null.
     * @return The first line of the file or <code>null</code> if the file does not exist or is empty.
     * @throws IOException If something did not work :(
     */
    private String readFile(File file) throws IOException {
        if (!file.exists()) {
            return null;
        }
        try (
                FileReader fileReader = new FileReader(file);
                BufferedReader bufferedReader =  new BufferedReader(fileReader);
        ) {
            return bufferedReader.readLine();
        }
    }

    /**
     * Writes a String to a file. It also adds a note for the user,
     *
     * @param file The file to write to. Cannot be null.
     * @param lines The lines to write.
     * @throws IOException If something did not work :(
     */
    private void writeFile(File file, String... lines) throws IOException {
        if (!file.exists()) {
            file.createNewFile();
        }
        try (
                FileWriter fileWriter = new FileWriter(file);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)
        ) {
            for (String line : lines) {
                bufferedWriter.write(line);
                bufferedWriter.newLine();
            }
        }
    }

    /**
     * Sends the data to the bStats server.
     *
     * @param data The data to send.
     * @throws Exception If the request failed.
     */
    private void sendData(JsonObject data) throws Exception {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }

        HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();

        // Compress the data to save bandwidth
        byte[] compressedData = compress(data.toString());

        // Add headers
        connection.setRequestMethod("POST");
        connection.addRequestProperty("Accept", "application/json");
        connection.addRequestProperty("Connection", "close");
        connection.addRequestProperty("Content-Encoding", "gzip"); // We gzip our request
        connection.addRequestProperty("Content-Length", String.valueOf(compressedData.length));
        connection.setRequestProperty("Content-Type", "application/json"); // We send our data in JSON format
        connection.setRequestProperty("User-Agent", "MC-Server/" + B_STATS_VERSION);

        // Send data
        connection.setDoOutput(true);
        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.write(compressedData);
        outputStream.flush();
        outputStream.close();

        connection.getInputStream().close(); // We don't care about the response - Just send our data :)
    }

    /**
     * Gzips the given String.
     *
     * @param str The string to gzip.
     * @return The gzipped String.
     * @throws IOException If the compression failed.
     */
    private byte[] compress(final String str) throws IOException {
        if (str == null) {
            return null;
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PGZIPOutputStream gzip = new PGZIPOutputStream(outputStream);
        gzip.write(str.getBytes("UTF-8"));
        gzip.close();
        return outputStream.toByteArray();
    }

}