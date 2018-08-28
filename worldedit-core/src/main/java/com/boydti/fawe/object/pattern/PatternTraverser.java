package com.boydti.fawe.object.pattern;

import com.boydti.fawe.object.mask.ResettableMask;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.Pattern;
import java.lang.reflect.Field;
import java.util.Collection;

public class PatternTraverser {
    private final Object pattern;

    public PatternTraverser(Object start) {
        this.pattern = start;
    }

    public void reset(Extent newExtent) {
        reset(pattern, newExtent);
    }

    private void reset(Object pattern, Extent newExtent) {
        if (pattern == null) {
            return;
        }
        if (pattern instanceof ResettablePattern) {
            ((ResettablePattern) pattern).reset();
        }
        if (pattern instanceof ResettableMask) {
            ((ResettableMask) pattern).reset();
        }
        Class<?> current = pattern.getClass();
        while (current.getSuperclass() != null) {
            if (newExtent != null) {
                try {
                    Field field = current.getDeclaredField("extent");
                    field.setAccessible(true);
                    field.set(pattern, newExtent);
                } catch (NoSuchFieldException | IllegalAccessException | ClassCastException ignore) {
                }
            }
            try {
                Field field = current.getDeclaredField("pattern");
                field.setAccessible(true);
                Object next = field.get(pattern);
                reset(next, newExtent);
            } catch (NoSuchFieldException | IllegalAccessException | ClassCastException ignore) {
            }
            try {
                Field field = current.getDeclaredField("mask");
                field.setAccessible(true);
                Object next = field.get(pattern);
                reset(next, newExtent);
            } catch (NoSuchFieldException | IllegalAccessException ignore) {
            }
            try {
                Field field = current.getDeclaredField("material");
                field.setAccessible(true);
                Pattern next = (Pattern) field.get(pattern);
                reset(next, newExtent);
            } catch (NoSuchFieldException | IllegalAccessException | ClassCastException ignore) {
            }
            try {
                Field field = current.getDeclaredField("patterns");
                field.setAccessible(true);
                Collection<Pattern> patterns = (Collection<Pattern>) field.get(pattern);
                for (Pattern next : patterns) {
                    reset(next, newExtent);
                }
            } catch (NoSuchFieldException | IllegalAccessException | ClassCastException ignore) {
            }
            current = current.getSuperclass();
        }
    }
}
