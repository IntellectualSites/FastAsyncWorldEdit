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

    public static Component color(Component parent) {
        if (parent instanceof TextComponent) {
            TextComponent text = (TextComponent) parent;
            String content = text.content();
            if (content.indexOf('&') != -1) {
                Component legacy = LegacyComponentSerializer.legacy().deserialize(content, '&');
                legacy = legacy.style(parent.style());
                if (!parent.children().isEmpty()) {
                    parent = TextComponent.builder().append(legacy).append(parent.children()).build();
                } else {
                    parent = legacy;
                }
            }
        }
        TextColor lastColor = parent.color();
        List<Component> children = parent.children();
        if (!children.isEmpty()) {
            for (int i = 0; i < children.size(); i++) {
                Component original = children.get(i);
                Component child = original;
                if (child.color() == null && lastColor != null) {
                    child = child.color(lastColor);
                }
                child = color(child);
                if (original != child) {
                    if (!(children instanceof ArrayList)) {
                        children = new ArrayList<>(children);
                    }
                    children.set(i, child);
                }
                if (child.color() != null) {
                    lastColor = child.color();
                }
            }
            if (children instanceof ArrayList) {
                parent = parent.children(children);
            }
        }
        if (parent.color() == null && lastColor != null) {
            parent = parent.color(lastColor);
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
