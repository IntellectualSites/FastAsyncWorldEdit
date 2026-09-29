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

package com.sk89q.worldedit.internal.registry;

import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.extension.factory.parser.AliasedParser;
import com.fastasyncworldedit.core.extension.factory.parser.FaweParser;
import com.fastasyncworldedit.core.extension.factory.parser.pattern.RichPatternParser;
import com.sk89q.util.StringUtil;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.NoMatchException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.util.formatting.text.TextComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An abstract implementation of a factory for internal usage.
 *
 * @param <E> the element that the factory returns
 */
@SuppressWarnings("ProtectedField")
public abstract class AbstractFactory<E> {

    protected final WorldEdit worldEdit;
    //FAWE start
    protected final List<InputParser<E>> parsers = new ArrayList<>();
    private final FaweParser<E> richParser;
    //FWAE end

    /**
     * Create a new factory.
     *
     * @param worldEdit     the WorldEdit instance
     * @param defaultParser the parser to fall back to
     */
    protected AbstractFactory(WorldEdit worldEdit, InputParser<E> defaultParser) {
        //FAWE start
        this(worldEdit, defaultParser, null);
    }

    /**
     * Create a new factory with a given rich parser for FAWE rich parsing
     *
     * @param worldEdit     the WorldEdit instance
     * @param defaultParser the parser to fall back to
     * @param richParser    the rich parser
     * @since 2.11.0
     */
    protected AbstractFactory(WorldEdit worldEdit, InputParser<E> defaultParser, FaweParser<E> richParser) {
        checkNotNull(worldEdit);
        checkNotNull(defaultParser);
        this.worldEdit = worldEdit;
        this.parsers.add(defaultParser);
        this.richParser = richParser;
    }
    //FAWE end

    /**
     * Gets an immutable list of parsers.
     *
     * <p>
     * To add parsers, use the register method.
     * </p>
     *
     * @return the parsers
     */
    public List<InputParser<E>> getParsers() {
        return Collections.unmodifiableList(parsers);
    }

    //FAWE start

    /**
     * Parse a string and context to each {@link InputParser} added to this factory. If no result found, throws {@link InputParseException}
     *
     * @param input   input string
     * @param context input context
     * @return parsed result
     * @throws InputParseException if no result found
     */
    public E parseFromInput(String input, ParserContext context) throws InputParseException {
        List<E> parsed = new ArrayList<>();
        for (String component : StringUtil.split(input,' ', '[', ']')) {
            if (component.isEmpty()) {
                continue;
            }

            if (richParser != null) {
                E match = richParser.parseFromInput(component, context);
                if (match != null) {
                    parsed.add(match);
                    continue;
                }
            }
            parseFromParsers(context, parsed, component);
        }

        return getParsed(input, parsed);
    }
    //FAWE end

    @Deprecated
    public List<String> getSuggestions(String input) {
        return getSuggestions(input, new ParserContext());
    }

    public List<String> getSuggestions(String input, ParserContext context) {
        return parsers.stream().flatMap(
                p -> p.getSuggestions(input, context)
        ).collect(Collectors.toList());
    }

    /**
     * Registers an InputParser to this factory.
     *
     * @param inputParser The input parser
     */
    public void register(InputParser<E> inputParser) {
        checkNotNull(inputParser);

        parsers.add(parsers.size() - 1, inputParser);
    }

    /**
     * Test all parsers to see if alias is contained by one of them
     *
     * @param alias alias to test
     * @return if a parser contains the alias
     */
    public boolean containsAlias(String alias) {
        return parsers.stream().anyMatch(p -> {
            if (!(p instanceof AliasedParser)) {
                return false;
            }
            return ((AliasedParser) p).getMatchedAliases().contains(alias);
        });
    }

    //FAWE start
    protected void parseFromParsers(final ParserContext context, final List<E> parsed, final String component) {
        E match = null;
        for (InputParser<E> parser : getParsers()) {
            match = parser.parseFromInput(component, context);

            if (match != null) {
                break;
            }
        }
        if (match == null) {
            throw new NoMatchException(Caption.of("worldedit.error.no-match", TextComponent.of(component)));
        }
        parsed.add(match);
    }

    /**
     * Parses a pattern without considering parsing through the {@link RichPatternParser}, therefore not accepting
     * "richer" parsing where &amp; and , are used. Exists to prevent stack overflows.
     *
     * @param input   input string
     * @param context input context
     * @return parsed result
     * @throws InputParseException if no result found
     */
    public E parseWithoutRich(String input, ParserContext context) throws InputParseException {
        List<E> parsed = new ArrayList<>();

        for (String component : StringUtil.split(input, ' ', '[', ']')) {
            if (component.isEmpty()) {
                continue;
            }

            parseFromParsers(context, parsed, component);
        }

        return getParsed(input, parsed);
    }

    protected E getParsed(final String input, final List<E> parsed) {
        return parsed.isEmpty() ? null : parsed.get(0);
    }
    //FAWE end

}
