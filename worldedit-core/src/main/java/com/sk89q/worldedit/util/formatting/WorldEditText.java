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

package com.sk89q.worldedit.util.formatting;

import com.google.common.collect.ImmutableList;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import org.enginehub.piston.config.Config;
import org.enginehub.piston.config.ConfigHolder;
import org.enginehub.piston.config.TextConfig;
import org.enginehub.piston.util.TextHelper;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

public class WorldEditText {
    public static final ConfigHolder CONFIG_HOLDER = ConfigHolder.create();
    private static final Method METHOD_APPLY;

    static {
        CONFIG_HOLDER.getConfig(TextConfig.commandPrefix()).setValue("/");
        try {
            METHOD_APPLY = Config.class.getDeclaredMethod("apply", List.class);
            METHOD_APPLY.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static Component format(Component component, Locale locale) {
        return CONFIG_HOLDER.replace(WorldEdit.getInstance().getTranslationManager().convertText(recursiveReplace(component), locale));
    }

    private static Component recursiveReplace(Component input) {
        if (input instanceof TranslatableComponent) {
            TranslatableComponent tc = (TranslatableComponent)input;
            List<Component> args = tc.args();
            if (args != (args = replaceChildren(args))) {
                input = tc = tc.args(args);
            }
            if (CONFIG_HOLDER.getConfigs().containsKey(tc.key())) {
                Config config = CONFIG_HOLDER.getConfigs().get(tc.key());
                try {
                    return (Component) METHOD_APPLY.invoke(config, replaceChildren(tc.args()));
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
        List<Component> original = input.children();
        List<Component> replacement = replaceChildren(original);
        return original == replacement ? input : input.children(replacement);
    }

    private static List<Component> replaceChildren(List<Component> input) {
        if (input.isEmpty()) {
            return input;
        }
        ImmutableList.Builder<Component> copy = ImmutableList.builder();
        boolean modified = false;
        for (Component component : input) {
            Component replacement = recursiveReplace(component);
            if (replacement != component) {
                modified = true;
            }
            copy.add(replacement);
        }
        return modified ? copy.build() : input;
    }

    public static String reduceToText(Component component, Locale locale) {
        return TextHelper.reduceToText(format(component, locale));
    }

    private WorldEditText() {
    }

}
