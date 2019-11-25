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

package com.sk89q.worldedit.extension.factory.parser.mask;

import com.boydti.fawe.command.FaweParser;
import com.boydti.fawe.command.SuggestInputParseException;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.util.StringMan;
import com.google.common.collect.Iterables;
import com.sk89q.minecraft.util.commands.CommandLocals;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.BlockMaskBuilder;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.MaskIntersection;
import com.sk89q.worldedit.function.mask.MaskUnion;
import com.sk89q.worldedit.session.request.Request;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefaultMaskParser extends FaweParser<Mask> {
    public DefaultMaskParser(WorldEdit worldEdit) {
        super(worldEdit, "masks");
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
                } else {
                    List<String> args = entry.getValue();
                    String cmdArgs = ((args.isEmpty()) ? "" : " " + StringMan.join(args, " "));
                    try {
                        mask = parse(command + cmdArgs, context);
                    } catch (SuggestInputParseException rethrow) {
                        throw rethrow;
                    } catch (Throwable e) {
                        // TODO NOT IMPLEMENTED
//                        throw SuggestInputParseException.of(e, full, () -> {
//                            try {
//                                List<String> suggestions = dispatcher.get(command).getCallable().getSuggestions(cmdArgs, locals);
//                                if (suggestions.size() <= 2) {
//                                    for (int i = 0; i < suggestions.size(); i++) {
//                                        String suggestion = suggestions.get(i);
//                                        if (suggestion.indexOf(' ') != 0) {
//                                            String[] split = suggestion.split(" ");
//                                            suggestion = "[" + StringMan.join(split, "][") + "]";
//                                            suggestions.set(i, suggestion);
//                                        }
//                                    }
//                                }
//                                return suggestions;
//                            } catch (CommandException e1) {
//                                throw new InputParseException(e1.getMessage());
//                            } catch (Throwable e2) {
//                                e2.printStackTrace();
//                                throw new InputParseException(e2.getMessage());
//                            }
//                        });
                    }
                    if (mask == null) {
                        // Legacy patterns
                        char char0 = command.charAt(0);
                        boolean charMask = input.length() > 1 && input.charAt(1) != '[';
                        if (charMask && input.charAt(0) == '=') {
                            return parseFromInput(char0 + "[" + input.substring(1) + "]", context);
                        }
                        if (char0 == '#' || char0 == '?') {
                            // TODO NOT IMPLEMENTED
//                            throw new SuggestInputParseException(new NoMatchException("Unknown mask: " + full + ", See: //masks"), full,
//                                    () -> {
//                                        if (full.length() == 1) return new ArrayList<>(dispatcher.getPrimaryAliases());
//                                        return dispatcher.getAliases().stream().filter(
//                                                s -> s.startsWith(command.toLowerCase())
//                                        ).collect(Collectors.toList());
//                                    }
//                            );
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
                                } catch (InputParseException ignore) {}
                            }
                            if (mask == null) {
                                context.setPreferringWildcard(false);
                                context.setRestricted(false);
                                BlockStateHolder block = worldEdit.getBlockFactory().parseFromInput(full, context);
                                builder.add(block);
                                mask = builder.build(extent);
                            }
                        }
                    }
                }
                if (pe.and) {
                    masks.add(new ArrayList<>());
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
