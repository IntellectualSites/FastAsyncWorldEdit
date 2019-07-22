package com.boydti.fawe.config;

import com.boydti.fawe.configuration.ConfigurationSection;
import com.boydti.fawe.configuration.file.YamlConfiguration;
import com.boydti.fawe.util.StringMan;
import org.enginehub.piston.annotation.Command;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Commands {

    private static YamlConfiguration cmdConfig;
    private static File cmdFile;

    public static void load(File file) {
        cmdFile = file;
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            cmdConfig = YamlConfiguration.loadConfiguration(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Command translate(Class clazz, final Command command) {
        if (cmdConfig == null || command instanceof TranslatedCommand) {
            return command;
        }
        return new TranslatedCommand(clazz.getSimpleName(), command);
    }

    public static String getAlias(Class clazz, String command) {
        if (cmdConfig == null) {
            return command;
        }
        List<String> aliases = cmdConfig.getStringList(clazz + "." + command + ".aliases");
        if (aliases == null) {
            aliases = cmdConfig.getStringList(command + ".aliases");
        }
        return (aliases == null || aliases.isEmpty()) ? command : aliases.get(0);
    }

    public static class TranslatedCommand implements Command {
        private final String name;
        private final String[] aliases;
        private final String desc;
        private final String descFooter;
        private final Command command;

        public TranslatedCommand(String clazz, Command command) {
            String id = command.aliases()[0];
            ConfigurationSection commands;
            if (cmdConfig.contains(clazz + "." + id) || !cmdConfig.contains(id)) {
                commands = cmdConfig.getConfigurationSection(clazz + "." + id);
            } else {
                commands = cmdConfig.getConfigurationSection(id);
            }
            boolean set = false;
            if (commands == null) {
                set = (commands = cmdConfig.createSection(clazz + "." + id)) != null;
            }

            HashMap<String, Object> options = new HashMap<>();
            options.put("name", command.name());
            options.put("aliases", new ArrayList<>(Arrays.asList(command.aliases())));
            options.put("help", command.desc());
            options.put("desc", command.descFooter());
            for (Map.Entry<String, Object> entry : options.entrySet()) {
                String key = entry.getKey();
                if (!commands.contains(key)) {
                    commands.set(key, entry.getValue());
                    set = true;
                }
            }
            if (set) {
                try {
                    cmdConfig.save(cmdFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            this.name = commands.getString("name");
            this.aliases = commands.getStringList("aliases").toArray(new String[0]);
            this.desc = commands.getString("help");
            this.descFooter = commands.getString("desc");
            this.command = command;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return this.command.annotationType();
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String[] aliases() {
            return aliases;
        }

        @Override
        public String desc() {
            return desc;
        }

        @Override
        public String descFooter() {
            return descFooter;
        }
    }
}
