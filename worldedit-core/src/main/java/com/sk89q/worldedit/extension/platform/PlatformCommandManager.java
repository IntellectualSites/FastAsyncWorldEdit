/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.extension.platform;

import static com.google.common.base.Preconditions.checkNotNull;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.command.AnvilCommands;
import com.boydti.fawe.command.AnvilCommandsRegistration;
import com.boydti.fawe.command.CFICommand;
import com.boydti.fawe.command.CFICommands;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.task.ThrowableSupplier;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.wrappers.LocationMaskedPlayerWrapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.ApplyBrushCommands;
import com.sk89q.worldedit.command.BiomeCommands;
import com.sk89q.worldedit.command.BiomeCommandsRegistration;
import com.sk89q.worldedit.command.BrushCommands;
import com.sk89q.worldedit.command.BrushCommandsRegistration;
import com.sk89q.worldedit.command.BrushOptionsCommands;
import com.sk89q.worldedit.command.ChunkCommands;
import com.sk89q.worldedit.command.ChunkCommandsRegistration;
import com.sk89q.worldedit.command.ClipboardCommands;
import com.sk89q.worldedit.command.ClipboardCommandsRegistration;
import com.sk89q.worldedit.command.ExpandCommands;
import com.sk89q.worldedit.command.GeneralCommands;
import com.sk89q.worldedit.command.GeneralCommandsRegistration;
import com.sk89q.worldedit.command.GenerationCommands;
import com.sk89q.worldedit.command.GenerationCommandsRegistration;
import com.sk89q.worldedit.command.HistoryCommands;
import com.sk89q.worldedit.command.HistoryCommandsRegistration;
import com.sk89q.worldedit.command.NavigationCommands;
import com.sk89q.worldedit.command.NavigationCommandsRegistration;
import com.sk89q.worldedit.command.PaintBrushCommands;
import com.sk89q.worldedit.command.PatternCommands;
import com.sk89q.worldedit.command.RegionCommands;
import com.sk89q.worldedit.command.RegionCommandsRegistration;
import com.sk89q.worldedit.command.SchematicCommands;
import com.sk89q.worldedit.command.SchematicCommandsRegistration;
import com.sk89q.worldedit.command.ScriptingCommands;
import com.sk89q.worldedit.command.ScriptingCommandsRegistration;
import com.sk89q.worldedit.command.SelectionCommands;
import com.sk89q.worldedit.command.SelectionCommandsRegistration;
import com.sk89q.worldedit.command.SnapshotCommands;
import com.sk89q.worldedit.command.SnapshotCommandsRegistration;
import com.sk89q.worldedit.command.SnapshotUtilCommands;
import com.sk89q.worldedit.command.SnapshotUtilCommandsRegistration;
import com.sk89q.worldedit.command.SuperPickaxeCommands;
import com.sk89q.worldedit.command.SuperPickaxeCommandsRegistration;
import com.sk89q.worldedit.command.ToolCommands;
import com.sk89q.worldedit.command.ToolCommandsRegistration;
import com.sk89q.worldedit.command.ToolUtilCommands;
import com.sk89q.worldedit.command.ToolUtilCommandsRegistration;
import com.sk89q.worldedit.command.TransformCommands;
import com.sk89q.worldedit.command.UtilityCommands;
import com.sk89q.worldedit.command.UtilityCommandsRegistration;
import com.sk89q.worldedit.command.WorldEditCommands;
import com.sk89q.worldedit.command.WorldEditCommandsRegistration;
import com.sk89q.worldedit.command.argument.Arguments;
import com.sk89q.worldedit.command.argument.BooleanConverter;
import com.sk89q.worldedit.command.argument.CommaSeparatedValuesConverter;
import com.sk89q.worldedit.command.argument.DirectionConverter;
import com.sk89q.worldedit.command.argument.DirectionVectorConverter;
import com.sk89q.worldedit.command.argument.EntityRemoverConverter;
import com.sk89q.worldedit.command.argument.EnumConverter;
import com.sk89q.worldedit.command.argument.FactoryConverter;
import com.sk89q.worldedit.command.argument.RegionFactoryConverter;
import com.sk89q.worldedit.command.argument.RegistryConverter;
import com.sk89q.worldedit.command.argument.VectorConverter;
import com.sk89q.worldedit.command.argument.ZonedDateTimeConverter;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandQueuedCondition;
import com.sk89q.worldedit.command.util.PermissionCondition;
import com.sk89q.worldedit.command.util.SubCommandPermissionCondition;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.event.platform.CommandSuggestionEvent;
import com.sk89q.worldedit.extension.platform.binding.AnnotatedBindings;
import com.sk89q.worldedit.extension.platform.binding.CommandBindings;
import com.sk89q.worldedit.extension.platform.binding.ConsumeBindings;
import com.sk89q.worldedit.extension.platform.binding.ProvideBindings;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.internal.command.CommandArgParser;
import com.sk89q.worldedit.internal.command.CommandLoggingHandler;
import com.sk89q.worldedit.internal.command.CommandRegistrationHandler;
import com.sk89q.worldedit.internal.command.exception.ExceptionConverter;
import com.sk89q.worldedit.internal.command.exception.WorldEditExceptionConverter;
import com.sk89q.worldedit.internal.util.Substring;
import com.sk89q.worldedit.scripting.CommandScriptLoader;
import com.sk89q.worldedit.session.request.Request;
import com.sk89q.worldedit.util.auth.AuthorizationException;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.sk89q.worldedit.util.formatting.text.format.TextColor;
import com.sk89q.worldedit.util.logging.DynamicStreamHandler;
import com.sk89q.worldedit.util.logging.LogFormat;
import com.sk89q.worldedit.world.World;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.enginehub.piston.Command;
import org.enginehub.piston.CommandManager;
import org.enginehub.piston.TextConfig;
import org.enginehub.piston.converter.ArgumentConverter;
import org.enginehub.piston.converter.ArgumentConverters;
import org.enginehub.piston.converter.ConversionResult;
import org.enginehub.piston.exception.CommandException;
import org.enginehub.piston.exception.CommandExecutionException;
import org.enginehub.piston.exception.ConditionFailedException;
import org.enginehub.piston.exception.UsageException;
import org.enginehub.piston.gen.CommandRegistration;
import org.enginehub.piston.impl.CommandManagerServiceImpl;
import org.enginehub.piston.inject.InjectedValueAccess;
import org.enginehub.piston.inject.InjectedValueStore;
import org.enginehub.piston.inject.Key;
import org.enginehub.piston.inject.MapBackedValueStore;
import org.enginehub.piston.inject.MemoizingValueAccess;
import org.enginehub.piston.inject.MergedValueAccess;
import org.enginehub.piston.part.SubCommandPart;
import org.enginehub.piston.suggestion.Suggestion;
import org.enginehub.piston.util.HelpGenerator;
import org.enginehub.piston.util.ValueProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Handles the registration and invocation of commands.
 *
 * <p>This class is primarily for internal usage.</p>
 */
public final class PlatformCommandManager {

    public static final Pattern COMMAND_CLEAN_PATTERN = Pattern.compile("^[/]+");
    private static final Logger log = LoggerFactory.getLogger(PlatformCommandManager.class);
    private static final java.util.logging.Logger COMMAND_LOG =
        java.util.logging.Logger.getLogger("com.sk89q.worldedit.CommandLog");

    static {
        TextConfig.setCommandPrefix("/");
    }

    private final WorldEdit worldEdit;
    private final PlatformManager platformManager;
    private final CommandManagerServiceImpl commandManagerService;
    private final CommandManager commandManager;
    private final InjectedValueStore globalInjectedValues;
    private final DynamicStreamHandler dynamicHandler = new DynamicStreamHandler();
    private final WorldEditExceptionConverter exceptionConverter;
    public final CommandRegistrationHandler registration;
    private static PlatformCommandManager INSTANCE;

    /**
     * Create a new instance.
     *
     * @param worldEdit the WorldEdit instance
     */
    public PlatformCommandManager(final WorldEdit worldEdit, PlatformManager platformManager) {
        checkNotNull(worldEdit);
        checkNotNull(platformManager);
        INSTANCE = this;

        this.worldEdit = worldEdit;
        this.platformManager = platformManager;
        this.exceptionConverter = new WorldEditExceptionConverter(worldEdit);
        this.commandManagerService = new CommandManagerServiceImpl();
        this.commandManager = commandManagerService.newCommandManager();
        this.globalInjectedValues = MapBackedValueStore.create();
        this.registration = new CommandRegistrationHandler(
            ImmutableList.of(
                new CommandLoggingHandler(worldEdit, COMMAND_LOG)
            ));
        // setup separate from main constructor
        // ensures that everything is definitely assigned
        initialize();
    }

    private void initialize() {
        // Register this instance for command events
        worldEdit.getEventBus().register(this);

        // Setup the logger
        COMMAND_LOG.addHandler(dynamicHandler);

        // Set up the commands manager
        registerAlwaysInjectedValues();
        registerArgumentConverters();
        registerAllCommands();
    }

    private void registerArgumentConverters() {
        DirectionVectorConverter.register(worldEdit, commandManager);
        DirectionConverter.register(worldEdit, commandManager);
        FactoryConverter.register(worldEdit, commandManager);
        for (int count = 2; count <= 3; count++) {
            commandManager.registerConverter(Key.of(double.class, Annotations.radii(count)),
                CommaSeparatedValuesConverter.wrapAndLimit(ArgumentConverters.get(
                    TypeToken.of(double.class)
                ), count)
            );
        }
        VectorConverter.register(commandManager);
        EnumConverter.register(commandManager);
        RegistryConverter.register(commandManager);
        ZonedDateTimeConverter.register(commandManager);
        BooleanConverter.register(commandManager);
        EntityRemoverConverter.register(commandManager);
        RegionFactoryConverter.register(commandManager);
    }

    public void registerAlwaysInjectedValues() {
        globalInjectedValues.injectValue(Key.of(InjectedValueAccess.class), Optional::of);
        register(new AnnotatedBindings(worldEdit));
        register(new CommandBindings(worldEdit));
        register(new ConsumeBindings(worldEdit));
        register(new ProvideBindings(worldEdit));
        register(new ProvideBindings(worldEdit));
    }

    public void register(Object classWithMethods) {
        // TODO NOT IMPLEMENTED - register the following using a custom processor / annotations
    }

    private <CI> void registerSubCommands(String name, List<String> aliases, String desc,
                                      CommandRegistration<CI> registration, CI instance) {
        registerSubCommands(name, aliases, desc, registration, instance, m -> {});
    }

    private <CI> void registerSubCommands(String name, List<String> aliases, String desc,
                                          CommandRegistration<CI> registration, CI instance,
                                          Consumer<CommandManager> additionalConfig) {
        commandManager.register(name, cmd -> {
            cmd.aliases(aliases);
            cmd.description(TextComponent.of(desc));
            cmd.action(Command.Action.NULL_ACTION);

            CommandManager manager = commandManagerService.newCommandManager();
            this.registration.register(
                manager,
                registration,
                instance
            );
            additionalConfig.accept(manager);

            final List<Command> subCommands = manager.getAllCommands().collect(Collectors.toList());
            cmd.addPart(SubCommandPart.builder(TranslatableComponent.of("worldedit.argument.action"),
                TextComponent.of("Sub-command to run."))
                .withCommands(subCommands)
                .required()
                .build());

            cmd.condition(new SubCommandPermissionCondition.Generator(subCommands).build());
        });
    }

    public void registerAllCommands() {
        if (Settings.IMP.ENABLED_COMPONENTS.COMMANDS) {
            // TODO NOT IMPLEMENTED dunno why these have issues generating
            registerSubCommands(
                "patterns",
                ImmutableList.of(),
                "Patterns determine what blocks are placed",
                PatternCommandsRegistration.builder(),
                new PatternCommands()
            );
            registerSubCommands(
                "transforms",
                ImmutableList.of(),
                "Transforms modify how a block is placed",
                TransformCommandsRegistration.builder(),
                new TransformCommands()
            );
            registerSubCommands(
                "schematic",
                ImmutableList.of("schem", "/schematic", "/schem"),
                "Schematic commands for saving/loading areas",
                SchematicCommandsRegistration.builder(),
                new SchematicCommands(worldEdit)
            );
            registerSubCommands(
                "snapshot",
                ImmutableList.of("snap"),
                "Snapshot commands for restoring backups",
                SnapshotCommandsRegistration.builder(),
                new SnapshotCommands(worldEdit)
            );
            registerSubCommands(
                "superpickaxe",
                ImmutableList.of("pickaxe", "sp"),
                "Super-pickaxe commands",
                SuperPickaxeCommandsRegistration.builder(),
                new SuperPickaxeCommands(worldEdit)
            );
            registerSubCommands(
                    "brush",
                    ImmutableList.of("br", "/brush", "/br"),
                    "Brushing commands",
                    BrushCommandsRegistration.builder(),
                    new BrushCommands(worldEdit),
                    manager -> {
                        PaintBrushCommands.register(commandManagerService, manager, registration);
                        ApplyBrushCommands.register(commandManagerService, manager, registration);
                    }
            );
            registerSubCommands(
                "brush",
                ImmutableList.of("br", "/b"),
                "Tool commands",
                BrushOptionsCommandsRegistration.builder(),
                new BrushOptionsCommands(worldEdit)
            );
            registerSubCommands(
                "worldedit",
                ImmutableList.of("we"),
                "WorldEdit commands",
                WorldEditCommandsRegistration.builder(),
                new WorldEditCommands(worldEdit)
            );
            registerSubCommands(
                "/anvil",
                ImmutableList.of(),
                "Manipulate billions of blocks https://github.com/boy0001/FastAsyncWorldedit/wiki/Anvil-API",
                AnvilCommandsRegistration.builder(),
                new AnvilCommands(worldEdit)
            );
            registerSubCommands(
                "transforms",
                ImmutableList.of(),
                "Transforms modify how a block is placed\n" +
                    " - Use [brackets] for arguments\n" +
                    " - Use , to OR multiple\n" +
                    " - Use & to AND multiple\n" +
                    "More Info: https://git.io/v9KHO",
                TransformCommandsRegistration.builder(),
                new TransformCommands()
            );
            this.registration.register(
                commandManager,
                BiomeCommandsRegistration.builder(),
                new BiomeCommands()
            );
            this.registration.register(
                commandManager,
                ChunkCommandsRegistration.builder(),
                new ChunkCommands(worldEdit)
            );
            this.registration.register(
                commandManager,
                ClipboardCommandsRegistration.builder(),
                new ClipboardCommands(worldEdit)
            );
            this.registration.register(
                commandManager,
                GeneralCommandsRegistration.builder(),
                new GeneralCommands(worldEdit)
            );
            this.registration.register(
                    commandManager,
                    GenerationCommandsRegistration.builder(),
                    new GenerationCommands(worldEdit)
            );
            this.registration.register(
                    new CFICommand(commandManager),
                    CFICommands.builder(),
                    new CFICommands(worldEdit)
            );
            this.registration.register(
                commandManager,
                HistoryCommandsRegistration.builder(),
                new HistoryCommands(worldEdit)
            );
            this.registration.register(
                commandManager,
                NavigationCommandsRegistration.builder(),
                new NavigationCommands(worldEdit)
            );
            this.registration.register(
                commandManager,
                RegionCommandsRegistration.builder(),
                new RegionCommands(worldEdit)
            );
            this.registration.register(
                commandManager,
                ScriptingCommandsRegistration.builder(),
                new ScriptingCommands(worldEdit)
            );
            this.registration.register(
                commandManager,
                SelectionCommandsRegistration.builder(),
                new SelectionCommands(worldEdit)
            );
            ExpandCommands.register(registration, commandManager, commandManagerService);
            this.registration.register(
                commandManager,
                SnapshotUtilCommandsRegistration.builder(),
                new SnapshotUtilCommands(worldEdit)
            );
            this.registration.register(
                commandManager,
                ToolCommandsRegistration.builder(),
                new ToolCommands(worldEdit)
            );
            this.registration.register(
                commandManager,
                ToolUtilCommandsRegistration.builder(),
                new ToolUtilCommands(worldEdit)
            );
            this.registration.register(
                commandManager,
                UtilityCommandsRegistration.builder(),
                new UtilityCommands(worldEdit)
            );
        }
    }

//    /**
//     * Initialize the dispatcher
//     */
//    public synchronized void setupDispatcher() {
//        if (Settings.IMP.ENABLED_COMPONENTS.COMMANDS) {
//            DispatcherNode graph = new CommandGraph().builder(builder).commands();
//
//            for (Map.Entry<Object, String[]> entry : methodMap.entrySet()) {
//                // add  command
//                String[] aliases = entry.getValue();
//                if (aliases.length == 0) {
//                    graph = graph.registerMethods(entry.getKey());
//                } else {
//                    graph = graph.group(aliases).registerMethods(entry.getKey()).parent();
//                }
//            }
//
//            for (Map.Entry<CommandCallable, String[][]> entry : commandMap.entrySet()) {
//                String[][] aliases = entry.getValue();
//                CommandCallable callable = entry.getKey();
//                if (aliases[0].length == 0) {
//                    graph = graph.register(callable, aliases[1]);
//                } else {
//                    graph = graph.group(aliases[0]).register(callable, aliases[1]).parent();
//                }
//            }
//
//            commandMap.clear();
//            methodMap.clear();
//
//            dispatcher = graph
//                    .group("/anvil")
//                    .describeAs("Anvil command")
//                    .registerMethods(new AnvilCommands(worldEdit)).parent()
//                    .registerMethods(new CFICommand(worldEdit, builder))
//                    .registerMethods(new OptionsCommands(worldEdit))
//                    .registerMethods(new BrushOptionsCommands(worldEdit))
//                    .registerMethods(new BrushOptionsCommands(worldEdit))
//                    .register(adapt(new ShapedBrushCommand(new DeformCommand(), "worldedit.brush.deform")), "deform")
//                    .register(adapt(new ShapedBrushCommand(new ApplyCommand(new ReplaceParser(), "Set all blocks within region"), "worldedit.brush.set")), "set")
//                    .register(adapt(new ShapedBrushCommand(new PaintCommand(), "worldedit.brush.paint")), "paint")
//                    .register(adapt(new ShapedBrushCommand(new ApplyCommand(), "worldedit.brush.apply")), "apply")
//                    .register(adapt(new ShapedBrushCommand(new PaintCommand(new TreeGeneratorParser("treeType")), "worldedit.brush.forest")), "forest")
//                    .register(adapt(new ShapedBrushCommand(ProvidedValue.create(new Deform("y-=1", Mode.RAW_COORD), "Raise one block"), "worldedit.brush.raise")), "raise")
//                    .register(adapt(new ShapedBrushCommand(ProvidedValue.create(new Deform("y+=1", Mode.RAW_COORD), "Lower one block"), "worldedit.brush.lower")), "lower")
//                    .parent()
//                    .group("superpickaxe", "pickaxe", "sp").describeAs("Super-pickaxe commands")
//                    .registerMethods(new SuperPickaxeCommands(worldEdit))
//                    .parent().graph().getDispatcher();
//
//            if (platform != null) {
//                platform.registerCommands(dispatcher);
//            }
//        }
//    }

    public static PlatformCommandManager getInstance() {
        return INSTANCE;
    }

    public ExceptionConverter getExceptionConverter() {
        return exceptionConverter;
    }

    void registerCommandsWith(Platform platform) {
        log.info("Registering commands with " + platform.getClass().getCanonicalName());

        // Delay command registration to allow time for other plugins to hook into FAWE
        try {
            new CommandScriptLoader().load();
        } catch (Throwable e) {
            e.printStackTrace();
        }

        LocalConfiguration config = platform.getConfiguration();
        boolean logging = config.logCommands;
        String path = config.logFile;

        // Register log
        if (!logging || path.isEmpty()) {
            dynamicHandler.setHandler(null);
            COMMAND_LOG.setLevel(Level.OFF);
        } else {
            File file = new File(config.getWorkingDirectory(), path);
            COMMAND_LOG.setLevel(Level.ALL);

            log.info("Logging WorldEdit commands to " + file.getAbsolutePath());

            try {
                dynamicHandler.setHandler(new FileHandler(file.getAbsolutePath(), true));
            } catch (IOException e) {
                log.warn("Could not use command log file " + path + ": " + e.getMessage());
            }

            dynamicHandler.setFormatter(new LogFormat(config.logFormat));
        }

        platform.registerCommands(commandManager);
    }

    void removeCommands() {
        dynamicHandler.setHandler(null);
    }

    public Stream<Substring> parseArgs(String input) {
        return new CommandArgParser(CommandArgParser.spaceSplit(input.substring(1))).parseArgs();
    }

    public <T> Collection<T> parse(Class<T> clazz, String arguments, @Nullable Actor actor) {
        List<T> def = Collections.emptyList();
        Optional<ArgumentConverter<T>> converterOptional = commandManager.getConverter(Key.of(clazz));
        if (converterOptional.isPresent()) {
            ArgumentConverter<T> converter = converterOptional.get();
            MemoizingValueAccess injectedValues = initializeInjectedValues(() -> arguments, actor);
            ConversionResult<T> result = converter.convert(arguments, injectedValues);
            return result.orElse(def);
        }
        return def;
    }

    private Actor wrapActor(Actor actor, InjectedValueStore context) {
        if (actor instanceof Player) {
            final Set<String> failedPermissions = new LinkedHashSet<>();
            Player player = (Player) actor;
            Player unwrapped = LocationMaskedPlayerWrapper.unwrap(player);
            actor = new LocationMaskedPlayerWrapper(unwrapped, player.getLocation(), true) {
                @Override
                public boolean hasPermission(String permission) {
                    if (!super.hasPermission(permission)) {
                        failedPermissions.add(permission);
                        return false;
                    }
                    return true;
                }
                @Override
                public void checkPermission(String permission) throws AuthorizationException {
                    try {
                        super.checkPermission(permission);
                    } catch (AuthorizationException e) {
                        failedPermissions.add(permission);
                        throw e;
                    }
                }
            };
            context.injectValue(Key.of(CommandPermissions.class), i -> Optional.of(new CommandPermissions() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return CommandPermissions.class;
                }
                @Override
                public String[] value() {
                    return failedPermissions.toArray(new String[0]);
                }
            }));
        }
        return actor;
    }

    @Subscribe
    public void handleCommand(CommandEvent event) {
        Request.reset();
        Actor actor = event.getActor();
        String args = event.getArguments();
        CommandEvent finalEvent = new CommandEvent(actor, args);
        final FawePlayer<Object> fp = FawePlayer.wrap(actor);
        TaskManager.IMP.taskNow(() -> {
            int space0 = args.indexOf(' ');
            String arg0 = space0 == -1 ? args : args.substring(0, space0);
            Optional<Command> optional = commandManager.getCommand(arg0);
            if (!optional.isPresent()) return;
            Command cmd = optional.get();
            CommandQueuedCondition queued = cmd.getCondition().as(CommandQueuedCondition.class).orElse(null);
            if (queued != null && !queued.isQueued()) {
                handleCommandOnCurrentThread(finalEvent);
                return;
            }
            if (!fp.runAction(
                () -> PlatformCommandManager.this.handleCommandOnCurrentThread(finalEvent), false, true)) {
                BBC.WORLDEDIT_COMMAND_LIMIT.send(fp);
            }
            finalEvent.setCancelled(true);
        }, Fawe.isMainThread());
    }

    public void handleCommandOnCurrentThread(CommandEvent event) {
        Actor actor = platformManager.createProxyActor(event.getActor());
        String[] split = parseArgs(event.getArguments())
            .map(Substring::getSubstring)
            .toArray(String[]::new);

        // No command found!
        if (!commandManager.containsCommand(split[0])) {
            return;
        }

        LocalSession session = worldEdit.getSessionManager().get(actor);
        Request.request().setSession(session);
        if (actor instanceof Entity) {
            Extent extent = ((Entity) actor).getExtent();
            if (extent instanceof World) {
                Request.request().setWorld(((World) extent));
            }
        }
        LocalConfiguration config = worldEdit.getConfiguration();

        MemoizingValueAccess context = initializeInjectedValues(event::getArguments, actor);

        ThrowableSupplier<Throwable> task =
                () -> commandManager.execute(context, ImmutableList.copyOf(split));

        handleCommandTask(task, context, session, event);
    }

    public void handleCommandTask(ThrowableSupplier<Throwable> task, InjectedValueAccess context, @Nullable LocalSession session, CommandEvent event) {
        Request.reset();
        Actor actor = context.injectedValue(Key.of(Actor.class)).orElseThrow(() -> new IllegalStateException("No player"));

        long start = System.currentTimeMillis();

        try {
            // This is a bit of a hack, since the call method can only throw CommandExceptions
            // everything needs to be wrapped at least once. Which means to handle all WorldEdit
            // exceptions without writing a hook into every dispatcher, we need to unwrap these
            // exceptions and rethrow their converted form, if their is one.
            try {
                task.get();
            } catch (Throwable t) {
                // Use the exception converter to convert the exception if any of its causes
                // can be converted, otherwise throw the original exception
                Throwable next = t;
                do {
                    exceptionConverter.convert(next);
                    next = next.getCause();
                } while (next != null);

                throw t;
            }
        } catch (ConditionFailedException e) {
            if (e.getCondition() instanceof PermissionCondition) {
                actor.printError("You are not permitted to do that. Are you in the right mode?");
            } else {
                actor.print(e.getRichMessage());
            }
        } catch (UsageException e) {
            actor.print(TextComponent.builder("")
                .color(TextColor.RED)
                .append(e.getRichMessage())
                .build());
            ImmutableList<Command> cmd = e.getCommands();
            if (!cmd.isEmpty()) {
                actor.print(TextComponent.builder("Usage: ")
                    .color(TextColor.RED)
                    .append(HelpGenerator.create(e.getCommandParseResult()).getUsage())
                    .build());
            }
        } catch (CommandExecutionException e) {
            handleUnknownException(actor, e.getCause());
        } catch (CommandException e) {
            actor.print(TextComponent.builder("")
                    .color(TextColor.RED)
                    .append(e.getRichMessage())
                    .build());
        } catch (Throwable t) {
            handleUnknownException(actor, t);
        } finally {
            if (context instanceof MemoizingValueAccess) {
                context = ((MemoizingValueAccess) context).snapshotMemory();
            } else {
                System.out.println("Invalid context " + context);
            }
            Optional<EditSession> editSessionOpt = context.injectedValue(Key.of(EditSession.class));

            if (editSessionOpt.isPresent()) {
                EditSession editSession = editSessionOpt.get();
                session.remember(editSession);
                editSession.flushQueue();

                long time = System.currentTimeMillis() - start;
                if (time > 1000) {
                    BBC.ACTION_COMPLETE.send(actor, time / 1000D);
                }

                worldEdit.flushBlockBag(actor, editSession);
            }
            Request.reset();
        }

        event.setCancelled(true);
    }

    private MemoizingValueAccess initializeInjectedValues(Arguments arguments, Actor actor) {
        InjectedValueStore store = MapBackedValueStore.create();
        Actor finalActor = wrapActor(actor, store);
        store.injectValue(Key.of(Actor.class), ValueProvider.constant(finalActor));
        if (finalActor instanceof Player) {
            store.injectValue(Key.of(Player.class), ValueProvider.constant((Player) finalActor));
        } else {
            store.injectValue(Key.of(Player.class), context -> {
                throw new CommandException(TextComponent.of("This command must be used with a player."), ImmutableList.of());
            });
        }
        store.injectValue(Key.of(Arguments.class), ValueProvider.constant(arguments));
        store.injectValue(Key.of(LocalSession.class),
            context -> {
                LocalSession localSession = worldEdit.getSessionManager().get(finalActor);
                localSession.tellVersion(finalActor);
                return Optional.of(localSession);
            });

        return MemoizingValueAccess.wrap(
            MergedValueAccess.of(store, globalInjectedValues)
        );
    }

    private void handleUnknownException(Actor actor, Throwable t) {
        actor.printError("Please report this error: [See console]");
        actor.printRaw(t.getClass().getName() + ": " + t.getMessage());
        log.error("An unexpected error while handling a WorldEdit command", t);
    }

    @Subscribe
    public void handleCommandSuggestion(CommandSuggestionEvent event) {
        try {
            String arguments = event.getArguments();
            List<Substring> split = parseArgs(arguments).collect(Collectors.toList());
            List<String> argStrings = split.stream()
                .map(Substring::getSubstring)
                .collect(Collectors.toList());
            MemoizingValueAccess access = initializeInjectedValues(() -> arguments, event.getActor());
            ImmutableSet<Suggestion> suggestions;
            try {
                suggestions = commandManager.getSuggestions(access, argStrings);
            } catch (Throwable t) { // catch errors which are *not* command exceptions generated by parsers/suggesters
                if (!(t instanceof CommandException)) {
                    log.debug("Unexpected error occurred while generating suggestions for input: " + arguments, t);
                    return;
                }
                throw t;
            }

            event.setSuggestions(suggestions.stream()
                .map(suggestion -> {
                    int noSlashLength = arguments.length() - 1;
                    Substring original = suggestion.getReplacedArgument() == split.size()
                        ? Substring.from(arguments, noSlashLength, noSlashLength)
                        : split.get(suggestion.getReplacedArgument());
                    // increase original points by 1, for removed `/` in `parseArgs`
                    return Substring.wrap(
                        suggestion.getSuggestion(),
                        original.getStart() + 1,
                        original.getEnd() + 1
                    );
                }).collect(Collectors.toList()));
        } catch (ConditionFailedException e) {
            if (e.getCondition() instanceof PermissionCondition) {
                event.setSuggestions(new ArrayList<>());
            }
        }
    }

    /**
     * Get the command manager instance.
     *
     * @return the command manager
     */
    public CommandManager getCommandManager() {
        return commandManager;
    }

}
