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

import com.google.common.base.Function;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sk89q.worldedit.math.BlockVector3;

import javax.annotation.Nullable;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Combines several masks and requires that all masks return true
 * when a certain position is tested. It serves as a logical AND operation
 * on a list of masks.
 */
public class MaskIntersection extends AbstractMask {

    private final Set<Mask> masks = new LinkedHashSet<>();
    private Mask[] masksArray;

    /**
     * Create a new intersection.
     *
     * @param masks a list of masks
     */
    public MaskIntersection(Collection<Mask> masks) {
        checkNotNull(masks);
        this.masks.addAll(masks);
        formArray();
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
            masksArray = masks.toArray(new Mask[masks.size()]);
        }
    }

    public Function<Map.Entry<Mask, Mask>, Mask> pairingFunction() {
        return input -> input.getKey().and(input.getValue());
    }

    private void optimizeMasks(Set<Mask> ignore) {
        LinkedHashSet<Mask> newMasks = null;
        for (Mask mask : masks) {
            if (ignore.contains(mask)) continue;
            Mask newMask = mask.optimize();
            if (newMask != null) {
                if (newMask != mask) {
                    if (newMasks == null) newMasks = new LinkedHashSet<>();
                    newMasks.add(newMask);
                }
            } else {
                ignore.add(mask);
            }
            if (newMasks != null) {
                masks.clear();
                masks.addAll(newMasks);
            }
        }
    }

    @Override
    public Mask optimize() {
        Set<Mask> optimized = new HashSet<>();
        Set<Map.Entry<Mask, Mask>> failedCombines = new HashSet<>();
        // Combine the masks
        while (combine(pairingFunction(), failedCombines));
        // Optimize / combine
        do optimizeMasks(optimized);
        while (combine(pairingFunction(), failedCombines));
        // Return result
        formArray();
        if (masks.size() == 0) return Masks.alwaysTrue();
        if (masks.size() == 1) return masks.iterator().next();
        return this;
    }

    private boolean combine(Function<Map.Entry<Mask, Mask>, Mask> pairing, Set<Map.Entry<Mask, Mask>> failedCombines) {
        boolean hasOptimized = false;
        while (true) {
            Mask[] result = null;
            outer:
            for (Mask mask : masks) {
                for (Mask other : masks) {
                    AbstractMap.SimpleEntry pair = new AbstractMap.SimpleEntry(mask, other);
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
        if (masks.isEmpty()) {
            return false;
        }

        for (Mask mask : masks) {
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
