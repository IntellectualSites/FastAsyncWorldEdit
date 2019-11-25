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

package com.sk89q.worldedit.function.mask;

import static com.google.common.base.Preconditions.checkNotNull;

import com.boydti.fawe.Fawe;
import com.sk89q.worldedit.math.BlockVector3;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;

/**
 * Combines several masks and requires that all masks return true
 * when a certain position is tested. It serves as a logical AND operation
 * on a list of masks.
 */
public class MaskIntersection extends AbstractMask {

    private final Set<Mask> masks;
    private Mask[] masksArray;

    /**
     * Create a new intersection.
     *
     * @param masks a list of masks
     */
    public MaskIntersection(Collection<Mask> masks) {
        checkNotNull(masks);
        this.masks = new LinkedHashSet<>(masks);
        formArray();
    }

    public static Mask of(Mask... masks) {
        Set<Mask> set = new LinkedHashSet<>();
        for (Mask mask : masks) {
            if (mask == Masks.alwaysFalse()) {
                return mask;
            }
            if (mask != null && mask != Masks.alwaysTrue()) {
                if (mask.getClass() == MaskIntersection.class) {
                    set.addAll(((MaskIntersection) mask).getMasks());
                } else {
                    set.add(mask);
                }
            }
        }
        switch (set.size()) {
            case 0:
                return Masks.alwaysTrue();
            case 1:
                return set.iterator().next();
            default:
                return new MaskIntersection(masks).optimize();
        }
    }

    /**
     * Create a new intersection.
     *
     * @param mask a list of masks
     */
    public MaskIntersection(Mask... mask) {
        this(Arrays.asList(checkNotNull(mask)));
    }

    private void formArray() {
        if (masks.isEmpty()) {
            masksArray = new Mask[]{Masks.alwaysFalse()};
        } else {
            masksArray = masks.toArray(new Mask[0]);
        }
    }

    public Function<Entry<Mask, Mask>, Mask> pairingFunction() {
        return input -> input.getKey().tryCombine(input.getValue());
    }

    private boolean optimizeMasks(Set<Mask> ignore) {
        boolean changed = false;
        // Optimize sub masks
        for (int i = 0; i < masksArray.length; i++) {
            Mask mask = masksArray[i];
            if (ignore.contains(mask)) continue;
            Mask newMask = mask.tryOptimize();
            if (newMask != null) {
                changed = true;
                masksArray[i] = newMask;
            } else {
                ignore.add(mask);
            }
        }
        if (changed) {
            masks.clear();
            Collections.addAll(masks, masksArray);
        }
        // Optimize this
        boolean formArray = false;
        for (Mask mask : masksArray) {
            if (mask.getClass() == this.getClass()) {
                this.masks.remove(mask);
                this.masks.addAll(((MaskIntersection) mask).getMasks());
                formArray = true;
                changed = true;
            }
        }
        if (formArray) formArray();
        return changed;
    }

    @Override
    public Mask tryOptimize() {
        int maxIteration = 1000;
        Set<Mask> optimized = new HashSet<>();
        Set<Map.Entry<Mask, Mask>> failedCombines = new HashSet<>();
        // Combine the masks
        boolean changed = false;
        while (changed |= combineMasks(pairingFunction(), failedCombines));
        // Optimize / combine
        do changed |= optimizeMasks(optimized);
        while (changed |= combineMasks(pairingFunction(), failedCombines) && --maxIteration > 0);

        if (maxIteration == 0) {
            Fawe.debug("Failed optimize MaskIntersection");
            for (Mask mask : masks) {
                System.out.println(mask.getClass() + " / " + mask);
            }
        }
        // Return result
        formArray();
        if (masks.isEmpty()) return Masks.alwaysTrue();
        if (masks.size() == 1) return masks.iterator().next();
        return changed ? this : null;
    }

    private boolean combineMasks(Function<Entry<Mask, Mask>, Mask> pairing, Set<Map.Entry<Mask, Mask>> failedCombines) {
        boolean hasOptimized = false;
        while (true) {
            Mask[] result = null;
            outer:
            for (Mask mask : masks) {
                for (Mask other : masks) {
                    if (mask != other) {
                        AbstractMap.SimpleEntry<Mask, Mask> pair = new AbstractMap.SimpleEntry<>(mask, other);
                        if (failedCombines.contains(pair)) continue;
                        Mask combined = pairing.apply(pair);
                        if (combined != null) {
                            result = new Mask[]{combined, mask, other};
                            break outer;
                        } else {
                            failedCombines.add(pair);
                        }
                    }
                }
            }
            if (result == null) break;
            masks.remove(result[1]);
            masks.remove(result[2]);
            masks.add(result[0]);
            hasOptimized = true;
        }
        return hasOptimized;
    }

    /**
     * Add some masks to the list.
     *
     * @param masks the masks
     */
    public void add(Collection<Mask> masks) {
        checkNotNull(masks);
        this.masks.addAll(masks);
        formArray();
    }

    /**
     * Add some masks to the list.
     *
     * @param mask the masks
     */
    public void add(Mask... mask) {
        add(Arrays.asList(checkNotNull(mask)));
    }

    /**
     * Get the masks that are tested with.
     *
     * @return the masks
     */
    public Collection<Mask> getMasks() {
        return masks;
    }

    public final Mask[] getMasksArray() {
        return masksArray;
    }

    @Override
    public boolean test(BlockVector3 vector) {
        if (masksArray.length == 0) {
            return false;
        }

        for (Mask mask : masksArray) {
            if (!mask.test(vector)) {
                return false;
            }
        }

        return true;
    }

    @Nullable
    @Override
    public Mask2D toMask2D() {
        List<Mask2D> mask2dList = new ArrayList<>();
        for (Mask mask : masks) {
            Mask2D mask2d = mask.toMask2D();
            if (mask2d != null) {
                mask2dList.add(mask2d);
            } else {
                return null;
            }
        }
        return new MaskIntersection2D(mask2dList);
    }

}
