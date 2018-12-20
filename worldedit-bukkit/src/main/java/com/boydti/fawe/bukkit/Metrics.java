package com.boydti.fawe.bukkit;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.io.FastByteArrayOutputStream;
import com.boydti.fawe.object.io.PGZIPOutputStream;
import com.boydti.fawe.util.MainUtil;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.scheduler.BukkitTask;

public class Metrics {

    /**
     * The current revision number.
     */
    private static final int REVISION = 7;
    /**
     * The base url of the metrics domain.
     */
    private static final String BASE_URL = "http://report.mcstats.org";
    /**
     * The url used to report a server's status.
     */
    private static final String REPORT_URL = "/plugin/%s";
    /**
     * Interval of time to ping (in minutes).
     */
    private static final int PING_INTERVAL = 15;
    /**
     * The plugin this metrics submits for.
     */
    private final Plugin plugin;
    /**
     * All of the custom graphs to submit to metrics.
     */
    private final Set<Graph> graphs = Collections.synchronizedSet(new HashSet<Graph>());
    /**
     * Unique server id.
     */
    private final String guid;
    /**
     * Debug mode.
     */
    private final boolean debug;
    /**
     * The scheduled task.
     */
    private volatile BukkitTask task = null;

    public Metrics(Plugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        this.plugin = plugin;
        this.guid = UUID.randomUUID().toString();
        this.debug = false;
    }

    /**
     * GZip compress a string of bytes.
     *
     * @param input
     *
     * @return byte[] the file as a byte array
     */
    public static byte[] gzip(String input) {
        FastByteArrayOutputStream baos = new FastByteArrayOutputStream();
        PGZIPOutputStream gzos = null;
        try {
            gzos = new PGZIPOutputStream(baos);
            gzos.write(input.getBytes("UTF-8"));
        } catch (IOException e) {
            MainUtil.handleError(e);
        } finally {
            if (gzos != null) {
                try {
                    gzos.close();
                } catch (IOException ignore) {
                }
            }
        }
        return baos.toByteArray();
    }

    /**
     * Appends a json encoded key/value pair to the given string builder.
     *
     * @param json
     * @param key
     * @param value
     *
     */
    private static void appendJSONPair(StringBuilder json, String key, String value) {
        boolean isValueNumeric = false;
        try {
            if (value.equals("0") || !value.endsWith("0")) {
                Double.parseDouble(value);
                isValueNumeric = true;
            }
        } catch (NumberFormatException e) {
            isValueNumeric = false;
        }
        if (json.charAt(json.length() - 1) != '{') {
            json.append(',');
        }
        json.append(escapeJSON(key));
        json.append(':');
        if (isValueNumeric) {
            json.append(value);
        } else {
            json.append(escapeJSON(value));
        }
    }

    /**
     * Escape a string to create a valid JSON string
     *
     * @param text
     *
     * @return String
     */
    private static String escapeJSON(String text) {
        StringBuilder builder = new StringBuilder();
        builder.append('"');
        for (int index = 0; index < text.length(); index++) {
            char chr = text.charAt(index);
            switch (chr) {
                case '"':
                case '\\':
                    builder.append('\\');
                    builder.append(chr);
                    break;
                case '\b':
                    builder.append("\\b");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                default:
                    if (chr < ' ') {
                        String t = "000" + Integer.toHexString(chr);
                        builder.append("\\u" + t.substring(t.length() - 4));
                    } else {
                        builder.append(chr);
                    }
                    break;
            }
        }
        builder.append('"');
        return builder.toString();
    }

    /**
     * Encode text as UTF-8
     *
     * @param text the text to encode
     *
     * @return the encoded text, as UTF-8
     */
    private static String urlEncode(String text) throws UnsupportedEncodingException {
        return URLEncoder.encode(text, "UTF-8");
    }

    /**
     * Construct and create a Graph that can be used to separate specific plotters to their own graphs on the metrics
     * website. Plotters can be added to the graph object returned.
     *
     * @param name The name of the graph
     *
     * @return Graph object created. Will never return NULL under normal circumstances unless bad parameters are given
     */
    public Graph createGraph(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Graph name cannot be null");
        }
        // Construct the graph object
        Graph graph = new Graph(name);
        // Now we can add our graph
        this.graphs.add(graph);
        // and return back
        return graph;
    }

    /**
     * Add a Graph object to BukkitMetrics that represents data for the plugin that should be sent to the backend
     *
     * @param graph The name of the graph
     */
    public void addGraph(Graph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("Graph cannot be null");
        }
        this.graphs.add(graph);
    }

    /**
     * Start measuring statistics. This will immediately create an async repeating task as the plugin and send the
     * initial data to the metrics backend, and then after that it will post in increments of PING_INTERVAL * 1200
     * ticks.
     *
     * @return True if statistics measuring is running, otherwise false.
     */
    public boolean start() {
        // Is metrics already running?
        if (this.task != null) {
            return true;
        }
        // Begin hitting the server with glorious data
        this.task = this.plugin.getServer().getScheduler().runTaskTimerAsynchronously(this.plugin, new Runnable() {
            private boolean firstPost = true;

            @Override
            public void run() {
                try {
                    postPlugin(!this.firstPost);
                    // After the first post we set firstPost to
                    // false
                    // Each post thereafter will be a ping
                    this.firstPost = false;
                } catch (IOException e) {
                    MainUtil.handleError(e);
                    if (Metrics.this.debug) {
                        Bukkit.getLogger().log(Level.INFO, "[Metrics] " + e.getMessage());
                    }
                }
            }
        }, 0, PING_INTERVAL * 1200);
        return true;
    }

    /**
     * Enables metrics for the server by setting "opt-out" to false in the config file and starting the metrics task.
     *
     * @throws IOException
     */
    public void enable() {
        // Enable Task, if it is not running
        if (this.task == null) {
            start();
        }
    }

    /**
     * Disables metrics for the server by setting "opt-out" to true in the config file and canceling the metrics task.
     *
     */
    public void disable() {
        // Disable Task, if it is running
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }

    /**
     * Gets the File object of the config file that should be used to store
     * data such as the GUID and opt-out status.
     *
     * @return the File object for the config file
     */
    public File getConfigFile() {
        // I believe the easiest way to get the base folder (e.g craftbukkit set
        // via -P) for plugins to use
        // is to abuse the plugin object we already have
        // plugin.getDataFolder() => base/plugins/PluginA/
        // pluginsFolder => base/plugins/
        // The base is not necessarily relative to the startup directory.
        File pluginsFolder = this.plugin.getDataFolder().getParentFile();
        // return => base/plugins/PluginMetrics/config.yml
        return new File(new File(pluginsFolder, "PluginMetrics"), "config.yml");
    }

    /**
     * Generic method that posts a plugin to the metrics website.
     */
    private void postPlugin(boolean isPing) throws IOException {
        // Server software specific section
        PluginDescriptionFile description = this.plugin.getDescription();
        String pluginName = description.getName();
        boolean onlineMode = Bukkit.getServer().getOnlineMode(); // TRUE if online mode is enabled
        String pluginVersion = description.getVersion();
        String serverVersion = Bukkit.getVersion();
        int playersOnline = 0;
        try {
            if (Bukkit.class.getMethod("getOnlinePlayers", new Class<?>[0]).getReturnType() == Collection.class) {
                playersOnline = ((Collection<?>) Bukkit.class.getMethod("getOnlinePlayers", new Class<?>[0]).invoke(null)).size();
            } else {
                playersOnline = ((Player[]) Bukkit.class.getMethod("getOnlinePlayers", new Class<?>[0]).invoke(null)).length;
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {
            ex.printStackTrace();
        }
        // END server software specific section -- all code below does not use
        // any code outside of this class / Java
        // Construct the post data
        StringBuilder json = new StringBuilder(1024);
        json.append('{');
        // The plugin's description file containing all of the plugin data such as name, version, author, etc
        appendJSONPair(json, "guid", this.guid);
        appendJSONPair(json, "plugin_version", pluginVersion);
        appendJSONPair(json, "server_version", serverVersion);
        appendJSONPair(json, "players_online", Integer.toString(playersOnline));
        // New data as of R6
        String osname = System.getProperty("os.name");
        String osarch = System.getProperty("os.arch");
        String osversion = System.getProperty("os.version");
        String java_version = System.getProperty("java.version");
        int coreCount = Runtime.getRuntime().availableProcessors();
        // normalize os arch .. amd64 -> x86_64
        if (osarch.equals("amd64")) {
            osarch = "x86_64";
        }
        appendJSONPair(json, "osname", osname);
        appendJSONPair(json, "osarch", osarch);
        appendJSONPair(json, "osversion", osversion);
        appendJSONPair(json, "cores", Integer.toString(coreCount));
        appendJSONPair(json, "auth_mode", onlineMode ? "1" : "0");
        appendJSONPair(json, "java_version", java_version);
        // If we're pinging, append it
        if (isPing) {
            appendJSONPair(json, "ping", "1");
        }
        if (!this.graphs.isEmpty()) {
            synchronized (this.graphs) {
                json.append(',');
                json.append('"');
                json.append("graphs");
                json.append('"');
                json.append(':');
                json.append('{');
                boolean firstGraph = true;
                for (Graph graph : this.graphs) {
                    StringBuilder graphJson = new StringBuilder();
                    graphJson.append('{');
                    for (Plotter plotter : graph.getPlotters()) {
                        appendJSONPair(graphJson, plotter.getColumnName(), Integer.toString(plotter.getValue()));
                    }
                    graphJson.append('}');
                    if (!firstGraph) {
                        json.append(',');
                    }
                    json.append(escapeJSON(graph.getName()));
                    json.append(':');
                    json.append(graphJson);
                    firstGraph = false;
                }
                json.append('}');
            }
        }
        // close json
        json.append('}');
        // Create the url
        URL url = new URL(BASE_URL + String.format(REPORT_URL, urlEncode(pluginName)));
        // Connect to the website
        URLConnection connection;
        // Mineshafter creates a socks proxy, so we can safely bypass it
        // It does not reroute POST requests so we need to go around it
        if (isMineshafterPresent()) {
            connection = url.openConnection(Proxy.NO_PROXY);
        } else {
            connection = url.openConnection();
        }
        byte[] uncompressed = json.toString().getBytes();
        byte[] compressed = gzip(json.toString());
        // Headers
        connection.addRequestProperty("User-Agent", "MCStats/" + REVISION);
        connection.addRequestProperty("Content-Type", "application/json");
        connection.addRequestProperty("Content-Encoding", "gzip");
        connection.addRequestProperty("Content-Length", Integer.toString(compressed.length));
        connection.addRequestProperty("Accept", "application/json");
        connection.addRequestProperty("Connection", "close");
        connection.setDoOutput(true);
        if (this.debug) {
            Fawe.debug("[Metrics] Prepared request for " + pluginName + " uncompressed=" + uncompressed.length + " compressed=" + compressed.length);
        }
        try {
            try (OutputStream os = connection.getOutputStream()) {
                os.write(compressed);
                os.flush();
            }
            String response;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                response = reader.readLine();
                if (this.debug) {
                    Fawe.debug("[Metrics] Response for " + pluginName + ": " + response);
                }
            }
            if (response == null || response.startsWith("ERR") || response.startsWith("7")) {
                if (response == null) {
                    response = "null";
                } else if (response.startsWith("7")) {
                    response = response.substring(response.startsWith("7,") ? 2 : 1);
                }
                throw new IOException(response);
            } else {
                // Is this the first update this hour?
                if ("1".equals(response) || response.contains("This is your first update this hour")) {
                    synchronized (this.graphs) {
                        for (Graph graph : this.graphs) {
                            for (Plotter plotter : graph.getPlotters()) {
                                plotter.reset();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (this.debug) {
                MainUtil.handleError(e);
            }
        }
    }

    /**
     * Check if mineshafter is present. If it is, we need to bypass it to send POST requests
     *
     * @return true if mineshafter is installed on the server
     */
    private boolean isMineshafterPresent() {
        try {
            Class.forName("mineshafter.MineServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Represents a custom graph on the website
     */
    public static class Graph {

        /**
         * The graph's name, alphanumeric and spaces only :) If it does not comply to the above when submitted, it is
         * rejected
         */
        private final String name;
        /**
         * The set of plotters that are contained within this graph
         */
        private final Set<Plotter> plotters = new LinkedHashSet<>();

        private Graph(String name) {
            this.name = name;
        }

        /**
         * Gets the graph's name
         *
         * @return the Graph's name
         */
        public String getName() {
            return this.name;
        }

        /**
         * Add a plotter to the graph, which will be used to plot entries
         *
         * @param plotter the plotter to add to the graph
         */
        public void addPlotter(Plotter plotter) {
            this.plotters.add(plotter);
        }

        /**
         * Remove a plotter from the graph
         *
         * @param plotter the plotter to remove from the graph
         */
        public void removePlotter(Plotter plotter) {
            this.plotters.remove(plotter);
        }

        /**
         * Gets an <b>unmodifiable</b> set of the plotter objects in the graph
         *
         * @return an unmodifiable {@link Set} of the plotter objects
         */
        public Set<Plotter> getPlotters() {
            return Collections.unmodifiableSet(this.plotters);
        }

        @Override
        public int hashCode() {
            return this.name.hashCode();
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof Graph)) {
                return false;
            }
            Graph graph = (Graph) object;
            return graph.name.equals(this.name);
        }

        /**
         * Called when the server owner decides to opt-out of BukkitMetrics while the server is running.
         */
        protected void onOptOut() {
        }
    }

    /**
     * Interface used to collect custom data for a plugin
     */
    public abstract static class Plotter {

        /**
         * The plot's name
         */
        private final String name;

        /**
         * Construct a plotter with the default plot name
         */
        public Plotter() {
            this("Default");
        }

        /**
         * Construct a plotter with a specific plot name
         *
         * @param name the name of the plotter to use, which will show up on the website
         */
        public Plotter(String name) {
            this.name = name;
        }

        /**
         * Get the current value for the plotted point. Since this function defers to an external function it may or may
         * not return immediately thus cannot be guaranteed to be thread friendly or safe. This function can be called
         * from any thread so care should be taken when accessing resources that need to be synchronized.
         *
         * @return the current value for the point to be plotted.
         */
        public abstract int getValue();

        /**
         * Get the column name for the plotted point
         *
         * @return the plotted point's column name
         */
        public String getColumnName() {
            return this.name;
        }

        /**
         * Called after the website graphs have been updated
         */
        public void reset() {
        }

        @Override
        public int hashCode() {
            return getColumnName().hashCode();
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof Plotter)) {
                return false;
            }
            Plotter plotter = (Plotter) object;
            return plotter.name.equals(this.name) && plotter.getValue() == getValue();
        }
    }
}