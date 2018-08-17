package com.sk89q.worldedit.extension.factory;

import com.boydti.fawe.command.FaweParser;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.util.StringMan;
import com.sk89q.minecraft.util.commands.CommandLocals;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.command.MaskCommands;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.NoMatchException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.*;
import com.sk89q.worldedit.internal.command.ActorAuthorizer;
import com.sk89q.worldedit.internal.command.WorldEditBinding;
import com.sk89q.worldedit.session.request.Request;
import com.sk89q.worldedit.util.command.Dispatcher;
import com.sk89q.worldedit.util.command.SimpleDispatcher;
import com.sk89q.worldedit.util.command.parametric.ParametricBuilder;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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
        if (input.isEmpty()) return null;
        Extent extent = Request.request().getExtent();
        if (extent == null) extent = context.getExtent();
//        List<Mask> intersection = new ArrayList<>();
//        List<Mask> union = new ArrayList<>();
        List<List<Mask>> masks = new ArrayList<>();
        masks.add(new ArrayList<>());

        final CommandLocals locals = new CommandLocals();
        Actor actor = context != null ? context.getActor() : null;
        if (actor != null) {
            locals.put(Actor.class, actor);
        }
        //
        try {
            List<Map.Entry<ParseEntry, List<String>>> parsed = parse(input);
            for (Map.Entry<ParseEntry, List<String>> entry : parsed) {
                ParseEntry pe = entry.getKey();
                String command = pe.input;
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
                    if (mask == null) {
                        // Legacy syntax
                        if (charMask) {
                            switch (char0) {
                                case '\\': //
                                case '/': //
                                case '{': //
                                case '$': //
                                case '%': {
                                    command = command.substring(1);
                                    String value = command + ((entry.getValue().isEmpty()) ? "" : "[" + StringMan.join(entry.getValue(), "][") + "]");
                                    if (value.contains(":")) {
                                        if (value.charAt(0) == ':') value.replaceFirst(":", "");
                                        value = value.replaceAll(":", "][");
                                    }
                                    mask = parseFromInput(char0 + "[" + value + "]", context);
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
                                try {
                                    BlockMaskBuilder builder = new BlockMaskBuilder().addRegex(pe.full);
                                    if (builder.isEmpty()) {
                                        try {
                                            context.setPreferringWildcard(true);
                                            context.setRestricted(false);
                                            BlockStateHolder block = worldEdit.getBlockFactory().parseFromInput(pe.full, context);
                                            builder.add(block);
                                        } catch (NoMatchException e) {
                                            throw new NoMatchException(e.getMessage() + " See: //masks");
                                        }
                                    }
                                    mask = builder.build(extent);
                                } catch (PatternSyntaxException regex) {
                                    throw new InputParseException(regex.getMessage());
                                }
                            }
                        }
                    }
                } else {
                    List<String> args = entry.getValue();
                    if (!args.isEmpty()) {
                        command += " " + StringMan.join(args, " ");
                    }
                    mask = (Mask) dispatcher.call(command, locals, new String[0]);
                }
                if (pe.and) {
                    masks.add(new ArrayList<>());
                }
                masks.get(masks.size() - 1).add(mask);
            }
        } catch (InputParseException ignore) {
            throw ignore;
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
