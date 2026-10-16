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

// $Id$

package com.sk89q.worldedit.util;

import com.sk89q.util.StringUtil;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.util.report.Unreported;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Simple LocalConfiguration that loads settings using
 * {@code java.util.Properties}.
 */
public class PropertiesConfiguration extends LocalConfiguration {

    @Unreported
    private static final Logger LOGGER = LogManagerCompat.getLogger();

    @Unreported
    protected Properties properties;
    @Unreported
    protected File path;

    /**
     * Construct the object. The configuration isn't loaded yet.
     *
     * @param path the path to the configuration
     */
    public PropertiesConfiguration(Path path) {
        this.path = path.toFile();

        properties = new Properties();
    }

    /**
     * Construct the object. The configuration isn't loaded yet.
     *
     * @param path the path to the configuration
     * @deprecated Use {@link PropertiesConfiguration#PropertiesConfiguration(Path)}
     */
    @Deprecated
    public PropertiesConfiguration(File path) {
        this(path.toPath());
    }

    @Override
    public void load() {
        try (InputStream stream = new FileInputStream(path)) {
            properties.load(stream);
        } catch (FileNotFoundException ignored) {
        } catch (IOException e) {
            LOGGER.warn("Failed to read configuration", e);
        }

        loadExtra();

        //FAWE start - the options are declared once, in LocalConfiguration
        loadOptions(new OptionSource() {
            @Override
            public boolean getBoolean(String path, String property, boolean def) {
                return property == null ? def : getBool(property, def);
            }

            @Override
            public int getInt(String path, String property, int def) {
                return property == null ? def : PropertiesConfiguration.this.getInt(property, def);
            }

            @Override
            public String getString(String path, String property, String def) {
                return property == null ? def : PropertiesConfiguration.this.getString(property, def);
            }

            @Override
            public List<String> getStringList(String path, String property, List<String> def) {
                return property == null ? def : List.copyOf(getStringSet(property, def.toArray(String[]::new)));
            }
        });
        //FAWE end

        LocalSession.MAX_HISTORY_SIZE = Math.max(15, getInt("history-size", 15));

        //FAWE start - a path without a parent is in the working directory, which exists
        File parent = path.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        //FAWE end
        try (OutputStream output = new FileOutputStream(path)) {
            properties.store(output, "Don't put comments; they get removed");
        } catch (IOException e) {
            LOGGER.warn("Failed to write configuration", e);
        }
    }

    /**
     * Called to load extra configuration.
     */
    protected void loadExtra() {
    }

    /**
     * Get a string value.
     *
     * @param key the key
     * @param def the default value
     * @return the value
     */
    protected String getString(String key, String def) {
        if (def == null) {
            def = "";
        }
        String val = properties.getProperty(key);
        if (val == null) {
            properties.setProperty(key, def);
            return def;
        } else {
            return val;
        }
    }

    /**
     * Get a boolean value.
     *
     * @param key the key
     * @param def the default value
     * @return the value
     */
    protected boolean getBool(String key, boolean def) {
        String val = properties.getProperty(key);
        if (val == null) {
            properties.setProperty(key, def ? "true" : "false");
            return def;
        } else {
            return val.equalsIgnoreCase("true")
                    || val.equals("1");
        }
    }

    /**
     * Get an integer value.
     *
     * @param key the key
     * @param def the default value
     * @return the value
     */
    protected int getInt(String key, int def) {
        String val = properties.getProperty(key);
        if (val == null) {
            properties.setProperty(key, String.valueOf(def));
            return def;
        } else {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                properties.setProperty(key, String.valueOf(def));
                return def;
            }
        }
    }

    /**
     * Get a double value.
     *
     * @param key the key
     * @param def the default value
     * @return the value
     */
    protected double getDouble(String key, double def) {
        String val = properties.getProperty(key);
        if (val == null) {
            properties.setProperty(key, String.valueOf(def));
            return def;
        } else {
            try {
                return Double.parseDouble(val);
            } catch (NumberFormatException e) {
                properties.setProperty(key, String.valueOf(def));
                return def;
            }
        }
    }

    /**
     * Get a double value.
     *
     * @param key the key
     * @param def the default value
     * @return the value
     */
    protected Set<Integer> getIntSet(String key, int[] def) {
        String val = properties.getProperty(key);
        if (val == null) {
            properties.setProperty(key, StringUtil.joinString(def, ",", 0));
            Set<Integer> set = new HashSet<>();
            for (int i : def) {
                set.add(i);
            }
            return set;
        } else {
            Set<Integer> set = new HashSet<>();
            String[] parts = val.split(",", 0);
            for (String part : parts) {
                try {
                    int v = Integer.parseInt(part.trim());
                    set.add(v);
                } catch (NumberFormatException ignored) {
                }
            }
            return set;
        }
    }

    /**
     * Get a String set.
     *
     * @param key the key
     * @param def the default value
     * @return the value
     */
    protected Set<String> getStringSet(String key, String[] def) {
        String val = properties.getProperty(key);
        if (val == null) {
            properties.setProperty(key, StringUtil.joinString(def, ",", 0));
            return new HashSet<>(Arrays.asList(def));
        } else {
            Set<String> set = new HashSet<>();
            String[] parts = val.split(",");
            for (String part : parts) {
                //FAWE start - an empty value is an empty set, not a set with an empty entry
                String v = part.trim();
                if (!v.isEmpty()) {
                    set.add(v);
                }
                //FAWE end
            }
            return set;
        }
    }

}
