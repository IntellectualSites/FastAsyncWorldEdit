package com.sk89q.worldedit.nukkit;

import cn.nukkit.plugin.Plugin;
import com.sk89q.worldedit.util.PropertiesConfiguration;

import java.io.File;

/**
 * Nukkit platform configuration using properties file.
 */
public class NukkitConfiguration extends PropertiesConfiguration {

    public NukkitConfiguration(Plugin plugin) {
        super(new File(plugin.getDataFolder(), "worldedit.properties").toPath());
    }

}
