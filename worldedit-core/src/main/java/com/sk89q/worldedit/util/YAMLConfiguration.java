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

package com.sk89q.worldedit.util;

import com.sk89q.util.yaml.YAMLProcessor;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.session.SessionManager;
import com.sk89q.worldedit.util.report.Unreported;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;

/**
 * A less simple implementation of {@link LocalConfiguration}
 * using YAML configuration files.
 */
public class YAMLConfiguration extends LocalConfiguration {

    @Unreported
    protected final YAMLProcessor config;
    @Unreported
    protected final Logger logger;

    public YAMLConfiguration(YAMLProcessor config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    @Override
    public void load() {
        try {
            config.load();
        } catch (IOException e) {
            logger.warn("Error loading WorldEdit configuration", e);
        }

        //FAWE start - the options are declared once, in LocalConfiguration
        loadOptions(new OptionSource() {
            @Override
            public boolean getBoolean(String path, String property, boolean def) {
                return config.getBoolean(path, def);
            }

            @Override
            public int getInt(String path, String property, int def) {
                return config.getInt(path, def);
            }

            @Override
            public String getString(String path, String property, String def) {
                return config.getString(path, def);
            }

            @Override
            public List<String> getStringList(String path, String property, List<String> def) {
                return config.getStringList(path, def);
            }
        });
        //FAWE end

        LocalSession.MAX_HISTORY_SIZE = Math.max(0, config.getInt("history.size", 15));
        SessionManager.EXPIRATION_GRACE = config.getInt("history.expiration", 10) * 60 * 1000;
    }

    public void unload() {
    }

}
