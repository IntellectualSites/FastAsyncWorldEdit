package com.boydti.fawe.config;

import com.sk89q.worldedit.util.formatting.WorldEditText;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Caption {
    /**
     * Colorize a component with legacy color codes
     * @param parent
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
        List<Component> children = parent.children();
        if (!children.isEmpty()) {
            for (int i = 0; i < children.size(); i++) {
                Component child = children.get(i);
                Component coloredChild = color(child);
                if (coloredChild != child) {
                    if (!(children instanceof ArrayList)) {
                        children = new ArrayList<>(children);
                    }
                    children.set(i, coloredChild);
                }
            }
            if (children instanceof ArrayList) {
                parent = parent.children(children);
            }
        }
        return parent;
    }

}
