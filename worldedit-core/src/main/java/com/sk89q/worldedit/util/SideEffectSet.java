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

package com.sk89q.worldedit.util;

import com.google.common.collect.Maps;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SideEffectSet {

    //FAWE start - assign value map
    private static final SideEffectSet DEFAULT = new SideEffectSet(
            Arrays.stream(SideEffect.values())
                    .filter(SideEffect::isExposed)
                    .collect(Collectors.toMap(Function.identity(), SideEffect::getDefaultValue)));
    //FAWE end
    private static final SideEffectSet NONE = new SideEffectSet(
        Arrays.stream(SideEffect.values())
            .filter(SideEffect::isExposed)
            .collect(Collectors.toMap(Function.identity(), state -> SideEffect.State.OFF))
    );

    private final Map<SideEffect, SideEffect.State> sideEffects;
    private final Set<SideEffect> appliedSideEffects;
    private final boolean appliesAny;

    public SideEffectSet(Map<SideEffect, SideEffect.State> sideEffects) {
        this.sideEffects = Maps.immutableEnumMap(sideEffects);

        appliedSideEffects = sideEffects.entrySet()
                .stream()
                .filter(entry -> entry.getValue() != SideEffect.State.OFF)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        //FAWE start
        appliesAny = sideEffects.isEmpty() || !appliedSideEffects.isEmpty(); // Empty side effects implies default
        //FAWE end
    }

    //FAWE start - simple overload method for setting side effects

    /**
     * Create a new {@link SideEffectSet} with the given side effect set to "on"
     *
     * @since 2.12.3
     */
    public SideEffectSet with(SideEffect sideEffect) {
        return with(sideEffect, SideEffect.State.ON);
    }

    /**
     * Create a new {@link SideEffectSet} with the given side effect set to "off"
     *
     * @since 2.12.3
     */
    public SideEffectSet without(SideEffect sideEffect) {
        return with(sideEffect, SideEffect.State.OFF);
    }
    //FAWE end

    public SideEffectSet with(SideEffect sideEffect, SideEffect.State state) {
        Map<SideEffect, SideEffect.State> entries = this.sideEffects.isEmpty()
                ? Maps.newEnumMap(SideEffect.class)
                : new EnumMap<>(this.sideEffects);
        entries.put(sideEffect, state);
        return new SideEffectSet(entries);
    }

    public boolean doesApplyAny() {
        return this.appliesAny;
    }

    public SideEffect.State getState(SideEffect effect) {
        return sideEffects.getOrDefault(effect, effect.getDefaultValue());
    }

    /**
     * Gets whether this side effect is not off.
     *
     * <p>
     * This returns whether it is either delayed or on.
     * </p>
     *
     * @param effect The side effect
     * @return Whether it should apply
     */
    public boolean shouldApply(SideEffect effect) {
        return getState(effect) != SideEffect.State.OFF;
    }

    public Set<SideEffect> getSideEffectsToApply() {
        return this.appliedSideEffects;
    }

    public static SideEffectSet defaults() {
        return DEFAULT;
    }

    public static SideEffectSet none() {
        return NONE;
    }

    //FAWE start

    /**
     * API-friendly side effect set.
     * Sets:
     *  - Heightmaps
     *  - Lighting (if set to mode 1 or 2 in config)
     * Does not set:
     *  - History
     *  - Neighbours
     *  - Lighting (if set to mode 0 in config
     *
     * @since 2.12.3
     */
    public static SideEffectSet api() {
        return defaults().without(SideEffect.HISTORY);
    }
    //FAWE end

}
