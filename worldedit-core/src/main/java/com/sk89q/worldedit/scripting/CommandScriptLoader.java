package com.sk89q.worldedit.scripting;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.command.FaweParser;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.util.MainUtil;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.BrushCommands;
import com.sk89q.worldedit.extension.factory.parser.mask.DefaultMaskParser;
import com.sk89q.worldedit.extension.factory.parser.pattern.DefaultPatternParser;
import com.sk89q.worldedit.extension.platform.PlatformCommandManager;
import com.sk89q.worldedit.util.command.ProcessedCallable;
import com.sk89q.worldedit.util.command.parametric.FunctionParametricCallable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CommandScriptLoader {
    private final NashornCraftScriptEngine engine;
    private final String loader;

    public CommandScriptLoader() throws IOException {
        this.engine = new NashornCraftScriptEngine();

        try (InputStream inputStream = WorldEdit.class.getResourceAsStream("/cs_adv.js")) {
            this.loader = CharStreams.toString(new InputStreamReader(inputStream, Charsets.UTF_8));
        }
    }

    /**
     * Load all file commands
     * @throws Throwable
     */
    public void load() throws Throwable {
        File commands = MainUtil.getFile(Fawe.imp().getDirectory(), Settings.IMP.PATHS.COMMANDS);
        if (commands.exists()) {
            for (File file : commands.listFiles()) add(new String[0], file);
        }
    }

    private void add(String[] aliases, File file) throws Throwable {
        if (file.isDirectory()) {
            if (aliases.length == 0) {
                String[] newAliases = new String[] {file.getName()};
                for (File newFile : file.listFiles()) {
                    add(newAliases, newFile);
                }
            } else {
                Fawe.debug("Ignoring nested directory: " + file);
            }
        } else {
            String name = file.getName();
            if (name.endsWith(".js")) {
                Fawe.debug("Loading script: " + name);
                List<FunctionParametricCallable> cmds = getCommands(file, Collections.emptyMap());
                if (aliases.length == 1) {
                    FaweParser parser = null;
                    switch (aliases[0]) {
                        case "brush":
                            if (!cmds.isEmpty()) {
                                BrushCommands processor = new BrushCommands(WorldEdit.getInstance());
                                for (FunctionParametricCallable cmd : cmds) {
                                    ProcessedCallable processed = new ProcessedCallable(cmd, processor);
                                    PlatformCommandManager.getInstance().registerCommand(aliases, cmd.getCommand(), processed);
                                }
                            }
                            return;
                        case "patterns":
                            parser = FaweAPI.getParser(DefaultPatternParser.class);
                            break;
                        case "masks":
                            parser = FaweAPI.getParser(DefaultMaskParser.class);
                            break;
                    }
                    if (parser != null) {
                        for (FunctionParametricCallable cmd : cmds) {
                            parser.getDispatcher().registerCommand(cmd, cmd.getCommand().aliases());
                        }
                        return;
                    }
                }
                for (FunctionParametricCallable cmd : cmds) {
                    PlatformCommandManager.getInstance().registerSubCommands(name);registerCommand(aliases, cmd.getCommand(), cmd);
                }
            }
        }
    }

    private List<FunctionParametricCallable> getCommands(File file, Map<String, Object> vars) throws Throwable {
        String script = new String(Files.readAllBytes(file.toPath())) + loader;
        return (List) engine.evaluate(script, file.getPath(), vars);
    }
}
