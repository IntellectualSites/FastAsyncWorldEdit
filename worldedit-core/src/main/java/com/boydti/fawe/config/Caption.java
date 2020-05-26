package com.boydti.fawe.config;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.util.formatting.WorldEditText;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.sk89q.worldedit.util.formatting.text.format.TextColor;
import com.sk89q.worldedit.util.formatting.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class Caption {
    public static String toString(Component component) {
        return toString(component, WorldEdit.getInstance().getTranslationManager().getDefaultLocale());
    }

    public static String toString(Component component, Locale locale) {
        return WorldEditText.reduceToText(color(component), locale);
    }

    /**
     * Colorize a component with legacy color codes
     * @param component
     * @param locale
     * @return Component
     */
    public static Component color(Component component, Locale locale) {
        return color(WorldEditText.format(component, locale));
    }

    private static Component color(TextComponent text) {
        String content = text.content();
        if (content.indexOf('&') != -1) {
            TextComponent legacy = LegacyComponentSerializer.INSTANCE.deserialize(content, '&');
            legacy = (TextComponent) legacy.style(text.style());
            if (!text.children().isEmpty()) {
                text = TextComponent.builder().append(legacy).append(text.children()).build();
            } else {
                text = legacy;
            }
        }
        return text;
    }

    private static List<Component> color(Component input, List<Component> components) {
        TextColor lastColor = input.color();
        if (!components.isEmpty()) {
            for (int i = 0; i < components.size(); i++) {
                Component original = components.get(i);
                Component child = original;
                if (child.color() == null && lastColor != null) {
                    child = child.color(lastColor);
                }
                child = color(child);
                if (original != child) {
                    if (!(components instanceof ArrayList)) {
                        components = new ArrayList<>(components);
                    }
                    components.set(i, child);
                }
                if (child.color() != null) {
                    lastColor = child.color();
                }
            }
        }
        return components;
    }

    public static Component color(Component parent) {
        if (parent instanceof TextComponent) {
            parent = color((TextComponent) parent);
        }
        TextColor lastColor = parent.color();
        List<Component> children = parent.children();
        if (children != (children = color(parent, children))) {
            parent = parent.children(children);
        }
        if (parent instanceof TranslatableComponent) {
            TranslatableComponent tc = (TranslatableComponent) parent;
            List<Component> args = tc.args();
            if (args != (args = color(parent, args))) {
                parent = tc.args(args);
            }
        }
        if (parent.color() == null) {
            if (!children.isEmpty()) {
                lastColor = children.get(children.size() - 1).color();
            }
            if (lastColor != null) {
                parent = parent.color(lastColor);
            }
        }
        return parent;
    }

    public static TranslatableComponent of(@Nonnull final String key, @Nullable final TextColor color, @Nonnull final List<? extends Component> args) {
        return TranslatableComponent.of(key, color, Collections.emptySet(), args);
    }

    @Nonnull
    public static TranslatableComponent of(@Nonnull final String key, final Object... args) {
        List<Component> components = Arrays.stream(args)
                .map(arg -> arg instanceof Component ? (Component) arg : TextComponent.of(Objects.toString(arg)))
                .collect(Collectors.toList());
        return TranslatableComponent.of(key, components);
    }
}
