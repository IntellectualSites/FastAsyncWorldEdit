package com.boydti.fawe.config;

import com.boydti.fawe.configuration.ConfigurationSection;
import com.boydti.fawe.configuration.file.YamlConfiguration;
import com.sk89q.minecraft.util.commands.Command;
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

    public static Command fromArgs(String[] aliases, String usage, String desc, int min, int max, String flags, String help) {
        return new Command() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Command.class;
            }
            @Override
            public String[] aliases() {
                return aliases;
            }
            @Override
            public String usage() {
                return usage;
            }
            @Override
            public String desc() {
                return desc;
            }
            @Override
            public int min() {
                return min;
            }
            @Override
            public int max() {
                return max;
            }
            @Override
            public String flags() {
                return flags;
            }
            @Override
            public String help() {
                return help;
            }
            @Override
            public boolean anyFlags() {
                return !(flags.isEmpty() || flags.matches("[a-z]+"));
            }
        };
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
        private final String[] aliases;
        private final String usage;
        private final String desc;
        private final String help;
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
            options.put("aliases", new ArrayList<String>(Arrays.asList(command.aliases())));
            options.put("usage", command.usage());
            options.put("desc", command.desc());
            options.put("help", command.help());
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
            this.aliases = commands.getStringList("aliases").toArray(new String[0]);
            this.usage = commands.getString("usage");
            this.desc = commands.getString("desc");
            this.help = commands.getString("help");
            this.command = command;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return command.annotationType();
        }

        @Override
        public String[] aliases() {
            return aliases;
        }

        @Override
        public String usage() {
            return usage;
        }

        @Override
        public String desc() {
            return desc;
        }

        @Override
        public int min() {
            return command.min();
        }

        @Override
        public int max() {
            return command.max();
        }

        @Override
        public String flags() {
            return command.flags();
        }

        @Override
        public String help() {
            return help;
        }

        @Override
        public boolean anyFlags() {
            return command.anyFlags();
        }
    }

    public static Class<Commands> inject() {
        return Commands.class;
    }
}
