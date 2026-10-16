package com.fastasyncworldedit.core.configuration;

import com.fastasyncworldedit.core.limit.FaweLimit;
import com.fastasyncworldedit.core.limit.PropertyRemap;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Works out the limit of an actor from the limit groups they have permission for.
 * <p>
 * Everything that only depends on the configuration is prepared once per set of groups, so resolving an actor's limit,
 * which commands and parsers do several times each, costs the permission checks and little else.
 */
final class LimitResolver {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    // Not volatile: a resolver is immutable, so the worst a stale read does is prepare the groups again
    private static LimitResolver cached;

    private final Map<String, Settings.Limits> source;
    private final Group[] groups;

    private LimitResolver(Map<String, Settings.Limits> limits) {
        this.source = limits;
        List<Group> ordered = new ArrayList<>(limits.size());
        Group fallback = null;
        for (Map.Entry<String, Settings.Limits> entry : limits.entrySet()) {
            Group group = new Group(entry.getKey(), entry.getValue());
            if (group.fallback) {
                fallback = group;
            } else {
                ordered.add(group);
            }
        }
        // The default group goes last as it also applies to actors that matched no other group
        if (fallback != null) {
            ordered.add(fallback);
        }
        this.groups = ordered.toArray(Group[]::new);
    }

    static FaweLimit resolve(Map<String, Settings.Limits> limits, Actor actor) {
        if (actor.hasPermission("fawe.limit.unlimited")) {
            return FaweLimit.MAX.copy();
        }
        LimitResolver resolver = cached;
        if (resolver == null || resolver.source != limits) {
            cached = resolver = new LimitResolver(limits);
        }
        FaweLimit limit = new FaweLimit();
        boolean found = false;
        for (Group group : resolver.groups) {
            if (actor.hasPermission(group.permission) || !found && group.fallback) {
                group.mergeInto(limit, !found);
                found = true;
            }
        }
        return limit;
    }

    /**
     * {@return the blocks with the default namespace added where there is none, in lower case}
     */
    private static Set<String> namespaced(List<String> blocks) {
        Set<String> result = new HashSet<>();
        for (String block : blocks) {
            result.add((block.contains(":") ? block : "minecraft:" + block).toLowerCase(Locale.ROOT));
        }
        return Set.copyOf(result);
    }

    /**
     * {@return what both sets have in common, where a set without entries restricts nothing and so wins}
     */
    private static <T> Set<T> mostPermissive(Set<T> current, Set<T> other) {
        if (current.isEmpty() || other.isEmpty()) {
            return Collections.emptySet();
        }
        Set<T> common = new HashSet<>(current);
        common.retainAll(other);
        return common.isEmpty() ? Collections.emptySet() : common;
    }

    private static int orUnlimited(int value) {
        return value != -1 ? value : Integer.MAX_VALUE;
    }

    private static long orUnlimited(long value) {
        return value != -1 ? value : Long.MAX_VALUE;
    }

    private static final class Group {

        private final String name;
        private final boolean fallback;
        private final String permission;
        private final Settings.Limits limits;
        private final Set<String> stripNbt;
        private final Set<String> disallowedBlocks;
        // Parsed on first use as it needs the block registry, which is not there yet when the configuration is loaded
        private Set<PropertyRemap<?>> remaps;

        private Group(String name, Settings.Limits limits) {
            this.name = name;
            this.fallback = name.equals(Settings.DEFAULT_LIMIT);
            this.permission = "fawe.limit." + name;
            this.limits = limits;
            this.stripNbt = Set.copyOf(limits.stripNbt());
            this.disallowedBlocks = namespaced(limits.disallowedBlocks());
        }

        /**
         * Widens a limit by this group, so that an actor in several groups gets the most generous value of each.
         *
         * @param first if this is the first group merged into the limit
         */
        private void mergeInto(FaweLimit limit, boolean first) {
            limit.MAX_ACTIONS = Math.max(limit.MAX_ACTIONS, orUnlimited(limits.maxActions()));
            limit.MAX_CHANGES.set(Math.max(limit.MAX_CHANGES.get(), orUnlimited(limits.maxChanges())));
            limit.MAX_BLOCKSTATES.set(Math.max(limit.MAX_BLOCKSTATES.get(), orUnlimited(limits.maxBlockstates())));
            limit.MAX_CHECKS.set(Math.max(limit.MAX_CHECKS.get(), orUnlimited(limits.maxChecks())));
            limit.MAX_ENTITIES.set(Math.max(limit.MAX_ENTITIES.get(), orUnlimited(limits.maxEntities())));
            limit.MAX_FAILS.set(Math.max(limit.MAX_FAILS.get(), orUnlimited(limits.maxFails())));
            limit.MAX_ITERATIONS.set(Math.max(limit.MAX_ITERATIONS.get(), orUnlimited(limits.maxIterations())));
            limit.MAX_RADIUS = Math.max(limit.MAX_RADIUS, orUnlimited(limits.maxRadius()));
            limit.MAX_SUPER_PICKAXE_SIZE = Math.max(limit.MAX_SUPER_PICKAXE_SIZE, orUnlimited(limits.maxSuperPickaxeSize()));
            limit.MAX_BRUSH_RADIUS = Math.max(limit.MAX_BRUSH_RADIUS, orUnlimited(limits.maxBrushRadius()));
            limit.MAX_BUTCHER_RADIUS = Math.max(limit.MAX_BUTCHER_RADIUS, orUnlimited(limits.maxButcherRadius()));
            limit.MAX_HISTORY = Math.max(limit.MAX_HISTORY, orUnlimited(limits.maxHistoryMb()));
            limit.SCHEM_FILE_NUM_LIMIT = Math.max(limit.SCHEM_FILE_NUM_LIMIT, orUnlimited(limits.schemFileNumLimit()));
            limit.SCHEM_FILE_SIZE_LIMIT = Math.max(limit.SCHEM_FILE_SIZE_LIMIT, orUnlimited(limits.schemFileSizeLimit()));
            limit.MAX_EXPRESSION_MS = Math.max(limit.MAX_EXPRESSION_MS, orUnlimited(limits.maxExpressionMs()));
            limit.INVENTORY_MODE = Math.min(limit.INVENTORY_MODE, limits.inventoryMode());
            limit.SPEED_REDUCTION = Math.min(limit.SPEED_REDUCTION, limits.speedReduction());
            limit.FAST_PLACEMENT |= limits.fastPlacement();
            limit.CONFIRM_LARGE &= limits.confirmLarge();
            limit.RESTRICT_HISTORY_TO_REGIONS &= limits.restrictHistoryToRegions();
            limit.UNIVERSAL_DISALLOWED_BLOCKS &= limits.universalDisallowedBlocks();
            limit.ALLOW_LEGACY &= limits.allowLegacy();
            limit.SKIP_ENTITY_SPAWN_EVENTS |= limits.skipCreatureSpawnEvents();
            if (first) {
                limit.STRIP_NBT = stripNbt;
                limit.DISALLOWED_BLOCKS = disallowedBlocks;
                limit.REMAP_PROPERTIES = remaps();
            } else {
                limit.STRIP_NBT = mostPermissive(limit.STRIP_NBT, stripNbt);
                limit.DISALLOWED_BLOCKS = mostPermissive(limit.DISALLOWED_BLOCKS, disallowedBlocks);
                limit.REMAP_PROPERTIES = mostPermissive(limit.REMAP_PROPERTIES, remaps());
            }
        }

        private Set<PropertyRemap<?>> remaps() {
            Set<PropertyRemap<?>> remaps = this.remaps;
            if (remaps == null) {
                this.remaps = remaps = limits.remapProperties().isEmpty() ? Set.of() : parseRemaps();
            }
            return remaps;
        }

        /**
         * Parses entries of the form {@code property[old:new,old:new]} for every property of that name.
         */
        private Set<PropertyRemap<?>> parseRemaps() {
            Set<PropertyRemap<?>> remaps = new HashSet<>();
            for (String entry : limits.remapProperties()) {
                int open = entry.indexOf('[');
                int close = entry.indexOf(']');
                if (open < 0 || close < open) {
                    LOGGER.warn("Ignoring remap-properties entry '{}' of limit group '{}', expected name[old:new]", entry,
                            name
                    );
                    continue;
                }
                List<Property<?>> properties = BlockTypesCache
                        .getAllProperties()
                        .get(entry.substring(0, open).toLowerCase(Locale.ROOT));
                if (properties == null) {
                    continue;
                }
                String[] mappings = entry.substring(open + 1, close).split(",");
                for (Property<?> property : properties) {
                    for (String mapping : mappings) {
                        try {
                            String[] fromTo = mapping.split(":");
                            remaps.add(property.getRemap(property.getValueFor(fromTo[0]), property.getValueFor(fromTo[1])));
                        } catch (IllegalArgumentException | IndexOutOfBoundsException ignored) {
                            // This property is unlikely to be the one being targeted.
                            break;
                        }
                    }
                }
            }
            return Set.copyOf(remaps);
        }

    }

}
