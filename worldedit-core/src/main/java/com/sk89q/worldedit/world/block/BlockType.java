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

package com.sk89q.worldedit.world.block;

import com.fastasyncworldedit.core.function.mask.SingleBlockTypeMask;
import com.fastasyncworldedit.core.registry.state.PropertyKey;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.NullExtent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.Keyed;
import com.sk89q.worldedit.registry.NamespacedRegistry;
import com.sk89q.worldedit.registry.state.AbstractProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.concurrency.LazyReference;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.item.ItemTypes;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import com.sk89q.worldedit.world.registry.LegacyMapper;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkArgument;

//FAWE start - Pattern
public class BlockType implements Keyed, Pattern {
//FAWE end

    public static final NamespacedRegistry<BlockType> REGISTRY = new NamespacedRegistry<>("block type", true);
    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private final String id;
    @SuppressWarnings("this-escape")
    private final LazyReference<FuzzyBlockState> emptyFuzzy
            = LazyReference.from(() -> new FuzzyBlockState(this));
    //FAWE start
    private final BlockTypesCache.Settings settings;
    @Deprecated
    private final LazyReference<String> name = LazyReference.from(() -> WorldEdit.getInstance().getPlatformManager()
            .queryCapability(Capability.GAME_HOOKS).getRegistries().getBlockRegistry().getName(this));

    //FAWE start
    private Integer legacyCombinedId;
    private boolean initItemType;
    private ItemType itemType;

    protected BlockType(String id, int internalId, List<BlockState> states) {
        int i = id.indexOf("[");
        this.id = i == -1 ? id : id.substring(0, i);
        this.settings = new BlockTypesCache.Settings(this, id, internalId, states);
    }
    //FAWE end

    //FAWE start
    /**
     * @deprecated You should not be initialising your own BlockTypes, use {@link BlockTypes#get(String)} instead. If there is
     * a specific requirement to actually create new block types, please contact the FAWE devs to discuss. Use
     * {@link BlockTypes#get(String)} instead.
     */
    @Deprecated(since = "2.7.0")
    //FAWE end
    public BlockType(String id) {
        this(id, null);
    }

    //FAWE start
    /**
     * @deprecated You should not be initialising your own BlockTypes, use {@link BlockTypes#get(String)} instead. If there is
     * a specific requirement to actually create new block types, please contact the FAWE devs to discuss. Use
     * {@link BlockTypes#get(String)} instead.
     */
    @Deprecated(since = "2.7.0")
    //FAWE end
    public BlockType(String id, Function<BlockState, BlockState> values) {
        // If it has no namespace, assume minecraft.
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        this.id = id;
        //FAWE start
        //TODO fix the line below
        this.settings = new BlockTypesCache.Settings(this, id, 0, null);
    }

    @Deprecated
    public int getMaxStateId() {
        return settings.permutations;
    }
    //FAWE end

    /**
     * Gets the ID of this block.
     *
     * @return The id
     */
    @Override
    public String id() {
        return this.id;
    }

    public Component getRichName() {
        return WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.GAME_HOOKS)
                .getRegistries().getBlockRegistry().getRichName(this);
    }

    //FAWE start
    public String getNamespace() {
        String id = id();
        int i = id.indexOf(':');
        return i == -1 ? "minecraft" : id.substring(0, i);
    }

    public String getResource() {
        String id = id();
        return id.substring(id.indexOf(':') + 1);
    }
    //FAWE end

    /**
     * Gets the name of this block, or the ID if the name cannot be found.
     *
     * @return The name, or ID
     * @deprecated The name is now translatable, use {@link #getRichName()}.
     */
    @Deprecated
    public String getName() {
        String name = this.name.getValue();
        if (name == null || name.isEmpty()) {
            return id();
        }
        return name;
    }

    /*
    private BlockState computeDefaultState() {

        BlockState defaultState = Iterables.getFirst(getBlockStatesMap().values(), null);
        if (values != null) {
            defaultState = values.apply(defaultState);
        }
        return defaultState;
    }
    */

    @Deprecated
    public BlockState withPropertyId(int propertyId) {
        if (settings.stateOrdinals == null) {
            return settings.defaultState;
        } else if (propertyId >= settings.stateOrdinals.length || propertyId < 0) {
            LOGGER.error(
                    "Attempted to load blockstate with id {} of type {} outside of state ordinals length. Using default state.",
                    propertyId,
                    id()
            );
            return settings.defaultState;
        }
        int ordinal = settings.stateOrdinals[propertyId];
        if (ordinal >= BlockTypesCache.states.length || ordinal < 0) {
            LOGGER.error(
                    "Attempted to load blockstate with ordinal {} of type {} outside of states length. Using default state. Using default state.",
                    ordinal,
                    id()
            );
            return settings.defaultState;
        }
        return BlockTypesCache.states[ordinal];
    }

    @Deprecated
    public BlockState withStateId(int internalStateId) { //
        return this.withPropertyId(internalStateId >> BlockTypesCache.BIT_OFFSET);
    }
    //FAWE end

    /**
     * Gets the properties of this BlockType in a {@code key->property} mapping.
     *
     * @return The properties map
     */
    public Map<String, ? extends Property<?>> getPropertyMap() {
        return this.settings.propertiesMap;
    }

    /**
     * Gets the properties of this BlockType.
     *
     * @return the properties
     */
    public List<? extends Property<?>> getProperties() {
        //FAWE start - Don't use an ImmutableList here
        return this.settings.propertiesList;
        //FAWE end
    }

    //FAWE start
    @Deprecated
    public Set<? extends Property<?>> getPropertiesSet() {
        return this.settings.propertiesSet;
    }
    //FAWE end

    /**
     * Gets a property by name.
     *
     * @param name The name
     * @return The property
     */
    public <V> Property<V> getProperty(String name) {
        //FAWE start - use properties map
        return (Property<V>) this.settings.propertiesMap.get(name);
        //FAWE end
    }

    //FAWE start
    public boolean hasProperty(PropertyKey key) {
        int ordinal = key.getId();
        return this.settings.propertiesMapArr.length > ordinal && this.settings.propertiesMapArr[ordinal] != null;
    }

    /**
     * {@return whether this block type has a given property}
     *
     * @param property     the expected property
     * @since TODO
     */
    public boolean hasProperty(Property<?> property) {
        int ordinal = property.getKey().getId();
        Property<?> selfProperty;
        return this.settings.propertiesMapArr.length > ordinal
                && (selfProperty = this.settings.propertiesMapArr[ordinal]) != null
                && selfProperty == property;
    }

    public <V> Property<V> getProperty(PropertyKey key) {
        try {
            return (Property<V>) this.settings.propertiesMapArr[key.getId()];
        } catch (IndexOutOfBoundsException ignored) {
            return null;
        }
    }
    //FAWE end

    /**
     * Gets the default state of this block type.
     *
     * @return The default state
     */
    public BlockState getDefaultState() {
        //FAWE start - use settings
        return this.settings.defaultState;
        //FAWE end
    }

    public FuzzyBlockState getFuzzyMatcher() {
        return emptyFuzzy.getValue();
    }

    /**
     * Gets a list of all possible states for this BlockType.
     *
     * @return All possible states
     */
    public List<BlockState> getAllStates() {
        //FAWE start - use ordinals
        if (settings.stateOrdinals == null) {
            return Collections.singletonList(getDefaultState());
        }
        return IntStream.of(settings.stateOrdinals).filter(i -> i != -1).mapToObj(i -> BlockTypesCache.states[i]).collect(
                Collectors.toList());
        //FAWE end
    }

    /**
     * Gets a state of this BlockType with the given properties.
     *
     * @return The state, if it exists
     * @deprecated Not working. Not necessarily for removal, but WARNING DO NOT USE FOR NOW
     */
    @Deprecated(forRemoval = true)
    public BlockState getState(Map<Property<?>, Object> key) {
        //FAWE start - use ids & btp (block type property)
        int id = getInternalId();
        for (Map.Entry<Property<?>, Object> iter : key.entrySet()) {
            Property<?> prop = iter.getKey();
            Object value = iter.getValue();

            /*
             * TODO:
             * This is likely wrong. The only place this seems to currently (Dec 23 2018)
             * be invoked is via ForgeWorld, and value is a String when invoked there...
             */
            AbstractProperty btp = this.settings.propertiesMap.get(prop.getName());
            checkArgument(btp != null, "%s has no property named %s", this, prop.getName());
            id = btp.modify(id, btp.getValueFor((String) value));
        }
        return withStateId(id);
        //FAWE end
    }

    /**
     * Gets whether this block type has an item representation.
     *
     * @return If it has an item
     */
    public boolean hasItemType() {
        return getItemType() != null;
    }

    /**
     * Gets the item representation of this block type, if it exists.
     *
     * @return The item representation
     */
    @Nullable
    public ItemType getItemType() {
        //FAWE start - init this
        if (!initItemType) {
            initItemType = true;
            itemType = ItemTypes.get(this.id);
        }
        return itemType;
        //FAWE end
    }

    /**
     * Get the material for this BlockType.
     *
     * @return The material
     */
    public BlockMaterial getMaterial() {
        //FAWE start - use settings
        return this.settings.blockMaterial;
        //FAWE end
    }

    /**
     * Gets the legacy ID. Needed for legacy reasons.
     * <p>
     * DO NOT USE THIS.
     *
     * @return legacy id or 0, if unknown
     */
    @Deprecated
    public int getLegacyCombinedId() {
        //FAWE start - use LegacyMapper
        Integer combinedId = LegacyMapper.getInstance().getLegacyCombined(this);
        return combinedId == null ? 0 : combinedId;
        //FAWE end
    }

    /**
     * Gets the legacy data. Needed for legacy reasons.
     * <p>
     * DO NOT USE THIS.
     *
     * @return legacy data or 0, if unknown
     */
    @Deprecated
    public int getLegacyId() {
        //FAWE start
        return computeLegacy(0);
        //FAWE end
    }

    /**
     * Gets the legacy data. Needed for legacy reasons.
     *
     * <p>
     * DO NOT USE THIS.
     * </p>
     *
     * @return legacy data or 0, if unknown
     */
    @Deprecated
    public int getLegacyData() {
        //FAWE start
        return computeLegacy(1);
        //FAWE end
    }

    private int computeLegacy(int index) {
        //FAWE start
        if (this.legacyCombinedId == null) {
            this.legacyCombinedId = LegacyMapper.getInstance().getLegacyCombined(this.getDefaultState());
        }
        return index == 0 ? legacyCombinedId >> 4 : legacyCombinedId & 15;
        //FAWE end
    }

    @Override
    public String toString() {
        return id();
    }

    //FAWE start

    /**
     * The internal index of this type.
     *
     * <p>
     * This number is not necessarily consistent across restarts.
     * </p>
     *
     * @return internal id
     */
    public int getInternalId() {
        return this.settings.internalId;
    }

    @Override
    public int hashCode() {
        return settings.internalId; // stop changing this to WEs bad hashcode
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this; // stop changing this to a shitty string comparison
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        return set.setBlock(extent, getDefaultState());
    }

    @Override
    public BaseBlock applyBlock(BlockVector3 position) {
        return this.getDefaultState().toBaseBlock();
    }

    public SingleBlockTypeMask toMask() {
        return toMask(new NullExtent());
    }

    public SingleBlockTypeMask toMask(Extent extent) {
        return new SingleBlockTypeMask(extent, this);
    }
    //FAWE end
}
