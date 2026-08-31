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

package com.sk89q.worldedit.command.util;

import com.fastasyncworldedit.core.util.TaskManager;
import com.sk89q.worldedit.entity.metadata.EntityProperties;
import com.sk89q.worldedit.function.EntityFunction;

import javax.annotation.Nullable;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The implementation of /remove.
 */
public class EntityRemover {

    private final Type type;

    private EntityRemover(Type type) {
        this.type = type;
    }

    public EntityFunction createFunction() {
        final Type type = this.type;
        checkNotNull(type, "type can't be null");
        return entity -> {
            EntityProperties registryType = entity.getFacet(EntityProperties.class);
            if (registryType != null) {
                if (type.matcher.test(registryType)) {
                    //FAWE start - Calling this async violates thread safety
                    TaskManager.taskManager().sync(entity::remove);
                    //FAWE end
                    return true;
                }
            }

            return false;
        };
    }

    public static EntityRemover fromString(String str) {
        Type type = Type.findByPattern(str);
        if (type != null) {
            return new EntityRemover(type);
        } else {
            throw new IllegalArgumentException(
                    "Acceptable types: projectiles, items, paintings, itemframes, boats, minecarts, tnt, xp, or all");
        }
    }

    public enum Type {
        ALL("all", Type::isAll),
        PROJECTILES("projectiles?|arrows?", EntityProperties::isProjectile),
        ITEMS("items?|drops?", EntityProperties::isItem),
        FALLING_BLOCKS("falling(blocks?|sand|gravel)", EntityProperties::isFallingBlock),
        PAINTINGS("paintings?|art", EntityProperties::isPainting),
        ITEM_FRAMES("(item)frames?", EntityProperties::isItemFrame),
        BOATS("boats?", EntityProperties::isBoat),
        MINECARTS("(mine)?carts?", EntityProperties::isMinecart),
        TNT("tnt", EntityProperties::isTNT),
        XP_ORBS("xp", EntityProperties::isExperienceOrb);

        private final Pattern pattern;
        private final Predicate<EntityProperties> matcher;

        private static final Type[] VALUES;

        Type(String pattern, final Predicate<EntityProperties> matcher) {
            this.pattern = Pattern.compile(pattern);
            this.matcher = matcher;
        }
        @Nullable
        public static Type findByPattern(String str) {
            for (Type type : Type.VALUES) {
                if (type.pattern.matcher(str).matches()) {
                    return type;
                }
            }

            return null;
        }

        private static boolean isAll(EntityProperties type) {
            for (Type value : Type.VALUES) {
                if (value.matcher.test(type)) {
                    return true;
                }
            }
            return false;
        }

        static {
            VALUES = values();
        }
    }
}
