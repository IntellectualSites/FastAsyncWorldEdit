package com.sk89q.worldedit.extension.factory;

<<<<<<< HEAD
import com.boydti.fawe.command.FaweParser;
import com.boydti.fawe.command.SuggestInputParseException;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.util.StringMan;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandLocals;
=======
import com.sk89q.worldedit.IncompleteRegionException;
>>>>>>> 399e0ad5... Refactor vector system to be cleaner
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.MaskCommands;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.NoMatchException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.BlockMaskBuilder;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.MaskIntersection;
<<<<<<< HEAD
import com.sk89q.worldedit.function.mask.MaskUnion;
import com.sk89q.worldedit.internal.command.ActorAuthorizer;
import com.sk89q.worldedit.internal.command.WorldEditBinding;
=======
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.mask.NoiseFilter;
import com.sk89q.worldedit.function.mask.OffsetMask;
import com.sk89q.worldedit.function.mask.RegionMask;
import com.sk89q.worldedit.function.mask.SolidBlockMask;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.internal.registry.InputParser;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.noise.RandomNoise;
import com.sk89q.worldedit.regions.shape.WorldEditExpressionEnvironment;
>>>>>>> 399e0ad5... Refactor vector system to be cleaner
import com.sk89q.worldedit.session.request.Request;
import com.sk89q.worldedit.util.command.Dispatcher;
import com.sk89q.worldedit.util.command.SimpleDispatcher;
import com.sk89q.worldedit.util.command.parametric.ParametricBuilder;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefaultMaskParser extends FaweParser<Mask> {
    private final Dispatcher dispatcher;
    private final Pattern INTERSECTION_PATTERN = Pattern.compile("[&|;]+(?![^\\[]*\\])");

    public DefaultMaskParser(WorldEdit worldEdit) {
        super(worldEdit);
        this.dispatcher = new SimpleDispatcher();
        this.register(new MaskCommands(worldEdit));
    }

    @Override
    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    public void register(Object clazz) {
        ParametricBuilder builder = new ParametricBuilder();
        builder.setAuthorizer(new ActorAuthorizer());
        builder.addBinding(new WorldEditBinding(worldEdit));
        builder.registerMethodsAsCommands(dispatcher, clazz);
    }

    @Override
    public Mask parseFromInput(String input, ParserContext context) throws InputParseException {
        if (input.isEmpty()) {
            throw new SuggestInputParseException("No input provided", "", () -> Stream.concat(Stream.of("#", ",", "&"), BlockTypes.getNameSpaces().stream().map(n -> n + ":")).collect(Collectors.toList()));
        }
        Extent extent = Request.request().getExtent();
        if (extent == null) extent = context.getExtent();
        List<List<Mask>> masks = new ArrayList<>();
        masks.add(new ArrayList<>());

        final CommandLocals locals = new CommandLocals();
        Actor actor = context != null ? context.getActor() : null;
        if (actor != null) {
            locals.put(Actor.class, actor);
        }
        try {
            List<Map.Entry<ParseEntry, List<String>>> parsed = parse(input);
            for (Map.Entry<ParseEntry, List<String>> entry : parsed) {
                ParseEntry pe = entry.getKey();
                final String command = pe.input;
                String full = pe.full;
                Mask mask = null;
                if (command.isEmpty()) {
                    mask = parseFromInput(StringMan.join(entry.getValue(), ','), context);
                } else if (dispatcher.get(command) == null) {
                    // Legacy patterns
                    char char0 = command.charAt(0);
                    boolean charMask = input.length() > 1 && input.charAt(1) != '[';
                    if (charMask && input.charAt(0) == '=') {
                        return parseFromInput(char0 + "[" + input.substring(1) + "]", context);
                    }
                    if (char0 == '#' || char0 == '?') {
                        throw new SuggestInputParseException(new NoMatchException("Unknown mask: " + full + ", See: //masks"), full,
                                () -> {
                                    if (full.length() == 1) return new ArrayList<>(dispatcher.getPrimaryAliases());
                                    return dispatcher.getAliases().stream().filter(
                                            s -> s.startsWith(command.toLowerCase())
                                    ).collect(Collectors.toList());
                                }
                        );
                    }
                    // Legacy syntax
                    if (charMask) {
                        switch (char0) {
                            case '\\': //
                            case '/': //
                            case '{': //
                            case '$': //
                            case '%': {
                                String value = command.substring(1) + ((entry.getValue().isEmpty()) ? "" : "[" + StringMan.join(entry.getValue(), "][") + "]");
                                if (value.contains(":")) {
                                    if (value.charAt(0) == ':') value.replaceFirst(":", "");
                                    value = value.replaceAll(":", "][");
                                }
                                mask = parseFromInput("#" + char0 + "[" + value + "]", context);
                                break;
                            }
                            case '|':
                            case '~':
                            case '<':
                            case '>':
                            case '!':
                                input = input.substring(input.indexOf(char0) + 1);
                                mask = parseFromInput(char0 + "[" + input + "]", context);
                                if (actor != null) {
                                    BBC.COMMAND_CLARIFYING_BRACKET.send(actor, char0 + "[" + input + "]");
                                }
                                return mask;
                        }
                    }
                    if (mask == null) {
                        if (command.startsWith("[")) {
                            int end = command.lastIndexOf(']');
                            mask = parseFromInput(command.substring(1, end == -1 ? command.length() : end), context);
                        } else {
                            List<String> entries = entry.getValue();
                            BlockMaskBuilder builder = new BlockMaskBuilder();
//                            if (StringMan.containsAny(full, "\\^$.|?+(){}<>~$!%^&*+-/"))
                            {
                                try {
                                    builder.addRegex(full);
                                } catch (SuggestInputParseException rethrow) {
                                    throw rethrow;
                                } catch (InputParseException ignore) {}
                            }
                            if (mask == null) {
                                context.setPreferringWildcard(true);
                                context.setRestricted(false);
                                BlockStateHolder block = worldEdit.getBlockFactory().parseFromInput(full, context);
                                builder.add(block);
                                mask = builder.build(extent);
                            }
                        }
                    }
                } else {
<<<<<<< HEAD
                    List<String> args = entry.getValue();
                    String cmdArgs = ((args.isEmpty()) ? "" : " " + StringMan.join(args, " "));
                    try {
                        mask = (Mask) dispatcher.call(command + cmdArgs, locals, new String[0]);
                    } catch (SuggestInputParseException rethrow) {
                        throw rethrow;
                    } catch (Throwable e) {
                        throw SuggestInputParseException.of(e, full, () -> {
                            try {
                                List<String> suggestions = dispatcher.get(command).getCallable().getSuggestions(cmdArgs, locals);
                                if (suggestions.size() <= 2) {
                                    for (int i = 0; i < suggestions.size(); i++) {
                                        String suggestion = suggestions.get(i);
                                        if (suggestion.indexOf(' ') != 0) {
                                            String[] split = suggestion.split(" ");
                                            suggestion = BBC.color("[" + StringMan.join(split, "][") + "]");
                                            suggestions.set(i, suggestion);
                                        }
                                    }
                                }
                                return suggestions;
                            } catch (CommandException e1) {
                                throw new InputParseException(e1.getMessage());
                            } catch (Throwable e2) {
                                e2.printStackTrace();
                                throw new InputParseException(e2.getMessage());
                            }
                        });
=======
                    throw new NoMatchException("Unrecognized mask '" + component + '\'');
                }

            case '>':
            case '<':
                Mask submask;
                if (component.length() > 1) {
                    submask = getBlockMaskComponent(masks, component.substring(1), context);
                } else {
                    submask = new ExistingBlockMask(extent);
                }
                OffsetMask offsetMask = new OffsetMask(submask, new BlockVector3(0, firstChar == '>' ? -1 : 1, 0));
                return new MaskIntersection(offsetMask, Masks.negate(submask));

            case '$':
                Set<BaseBiome> biomes = new HashSet<>();
                String[] biomesList = component.substring(1).split(",");
                BiomeRegistry biomeRegistry = WorldEdit.getInstance().getPlatformManager()
                        .queryCapability(Capability.GAME_HOOKS).getRegistries().getBiomeRegistry();
                List<BaseBiome> knownBiomes = biomeRegistry.getBiomes();
                for (String biomeName : biomesList) {
                    BaseBiome biome = Biomes.findBiomeByName(knownBiomes, biomeName, biomeRegistry);
                    if (biome == null) {
                        throw new InputParseException("Unknown biome '" + biomeName + '\'');
>>>>>>> 399e0ad5... Refactor vector system to be cleaner
                    }
                }
<<<<<<< HEAD
                if (pe.and) {
                    masks.add(new ArrayList<>());
=======

                return Masks.asMask(new BiomeMask2D(context.requireExtent(), biomes));

            case '%':
                int i = Integer.parseInt(component.substring(1));
                return new NoiseFilter(new RandomNoise(), ((double) i) / 100);

            case '=':
                try {
                    Expression exp = Expression.compile(component.substring(1), "x", "y", "z");
                    WorldEditExpressionEnvironment env = new WorldEditExpressionEnvironment(
                            Request.request().getEditSession(), Vector3.ONE, Vector3.ZERO);
                    exp.setEnvironment(env);
                    return new ExpressionMask(exp);
                } catch (ExpressionException e) {
                    throw new InputParseException("Invalid expression: " + e.getMessage());
>>>>>>> 399e0ad5... Refactor vector system to be cleaner
                }
                masks.get(masks.size() - 1).add(mask);
            }
        } catch (InputParseException rethrow) {
            throw rethrow;
        } catch (Throwable e) {
            e.printStackTrace();
            throw new InputParseException(e.getMessage(), e);
        }
        List<Mask> maskUnions = new ArrayList<>();
        for (List<Mask> maskList : masks) {
            if (maskList.size() == 1) {
                maskUnions.add(maskList.get(0));
            } else if (maskList.size() != 0) {
                maskUnions.add(new MaskUnion(maskList));
            }
        }
        if (maskUnions.size() == 1) {
            return maskUnions.get(0);
        } else if (maskUnions.size() != 0) {
            return new MaskIntersection(maskUnions);
        } else {
            return null;
        }
    }
}
