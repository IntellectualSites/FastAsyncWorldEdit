/*
 * FastAsyncWorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) IntellectualSites <https://intellectualsites.com>
 * Copyright (C) IntellectualSites team and contributors
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

package com.fastasyncworldedit.core.configuration.caption;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Locale;

/**
 * A FAWE caption backed by a MiniMessage template loaded from
 * {@code lang/fawe_messages.json}.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // No placeholders
 * actor.print(FaweCaption.of("fawe.info.set.region"));
 *
 * // Named TagResolver placeholders (recommended)
 * actor.print(FaweCaption.of("fawe.error.no-perm"),
 *         TagResolver.resolver("permission", Tag.inserting(Component.text(node))));
 *
 * // Positional convenience helpers (maps to <arg0>, <arg1>, …)
 * actor.print(FaweCaption.of("fawe.info.lighting.propagate.selection"),
 *         FaweCaption.arg(0, chunkCount));
 * }</pre>
 *
 * <p>Every message automatically receives a {@code <prefix>} tag that expands to the
 * FAWE prefix defined by the {@code "prefix"} key in {@code fawe_messages.json}, so
 * message templates should start with {@code <prefix>}.</p>
 *
 * @see FaweCaptionMap
 */
public final class FaweCaption {

    /** Message key used for the FAWE prefix. */
    public static final String PREFIX_KEY = "prefix";

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final String key;

    private FaweCaption(final String key) {
        this.key = key;
    }

    // -----------------------------------------------------------------------
    // Factory
    // -----------------------------------------------------------------------

    /**
     * Create a {@link FaweCaption} for the given message key.
     *
     * @param key the caption key, e.g. {@code "fawe.error.no-perm"}
     * @return a new caption instance
     */
    public static @Nonnull FaweCaption of(final @Nonnull String key) {
        return new FaweCaption(key);
    }

    // -----------------------------------------------------------------------
    // Positional arg helpers
    // -----------------------------------------------------------------------

    /**
     * Create a {@link TagResolver} that maps {@code <argN>} to the given {@link Component}.
     *
     * @param index     zero-based position index
     * @param component the component value
     * @return a resolver for {@code <argN>}
     */
    public static @Nonnull TagResolver arg(final int index, final @Nonnull Component component) {
        return TagResolver.resolver("arg" + index, Tag.inserting(component));
    }

    /**
     * Create a {@link TagResolver} that maps {@code <argN>} to the given plain text value.
     *
     * @param index the zero-based position index
     * @param value the string value
     * @return a resolver for {@code <argN>}
     */
    public static @Nonnull TagResolver arg(final int index, final @Nonnull Object value) {
        return arg(index, value instanceof Component c ? c : Component.text(String.valueOf(value)));
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    /**
     * Returns the message key.
     *
     * @return the caption key
     */
    public @Nonnull String getKey() {
        return key;
    }

    // -----------------------------------------------------------------------
    // Rendering
    // -----------------------------------------------------------------------

    /**
     * Render this caption as an Adventure {@link Component}.
     *
     * <p>A {@code <prefix>} resolver is automatically appended so that message
     * templates can include the FAWE prefix via {@code <prefix>}.</p>
     *
     * @param locale    the locale used to look up the message template
     * @param resolvers additional resolvers for placeholder substitution
     * @return the rendered Adventure component
     */
    public @Nonnull Component toComponent(final @Nonnull Locale locale, final @Nonnull TagResolver... resolvers) {
        final String template = FaweCaptionMap.getInstance().getMessage(key, locale);
        if (PREFIX_KEY.equals(key)) {
            // Avoid infinite recursion: the prefix does not include itself
            return MINI_MESSAGE.deserialize(template);
        }
        // Inject <prefix> resolver automatically
        final TagResolver[] finalResolvers = Arrays.copyOf(resolvers, resolvers.length + 1);
        finalResolvers[finalResolvers.length - 1] = TagResolver.resolver(
                PREFIX_KEY,
                Tag.inserting(FaweCaption.of(PREFIX_KEY).toComponent(locale))
        );
        return MINI_MESSAGE.deserialize(template, finalResolvers);
    }

    @Override
    public String toString() {
        return "FaweCaption{key='" + key + "'}";
    }

}
