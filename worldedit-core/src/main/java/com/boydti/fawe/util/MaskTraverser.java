package com.boydti.fawe.util;

import com.boydti.fawe.object.mask.ResettableMask;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.function.mask.Mask;
import java.lang.reflect.Field;
import java.util.Collection;

public class MaskTraverser {
    private final Mask mask;

    public MaskTraverser(Mask start) {
        this.mask = start;
    }

    public void reset(Extent newExtent) {
        reset(mask, newExtent);
    }

    private void reset(Mask mask, Extent newExtent) {
        if (mask == null) {
            return;
        }
        if (mask instanceof ResettableMask) {
            ((ResettableMask) mask).reset();
        }
        Class<?> current = mask.getClass();
        while (current.getSuperclass() != null) {
            if (mask instanceof AbstractExtentMask) {
                AbstractExtentMask mask1 = (AbstractExtentMask) mask;
                mask1.setExtent(newExtent);
            } else {
                try {
                    Field field = current.getDeclaredField("extent");
                    field.setAccessible(true);
                    field.set(mask, newExtent);
                } catch (NoSuchFieldException | IllegalAccessException ignored) {
                }
            }
            try {
                Field field = current.getDeclaredField("mask");
                field.setAccessible(true);
                Mask next = (Mask) field.get(mask);
                reset(next, newExtent);
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
            }
            try {
                Field field = current.getDeclaredField("masks");
                field.setAccessible(true);
                Collection<Mask> masks = (Collection<Mask>) field.get(mask);
                for (Mask next : masks) {
                    reset(next, newExtent);
                }
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
            }
            current = current.getSuperclass();
        }
    }
}
