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

package com.sk89q.worldedit.world.entity;

import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.registry.LegacyMapper;

import javax.annotation.Nullable;
import java.util.*;

public enum EntityTypes implements EntityType {
    /*
     -----------------------------------------------------
        Replaced at runtime by the entity registry
     -----------------------------------------------------
     */
    __RESERVED__,
    AREA_EFFECT_CLOUD,
    ARMOR_STAND,
    ARROW,
    BAT,
    BLAZE,
    BOAT,
    CAVE_SPIDER,
    CHEST_MINECART,
    CHICKEN,
    COD,
    COMMAND_BLOCK_MINECART,
    COW,
    CREEPER,
    DOLPHIN,
    DONKEY,
    DRAGON_FIREBALL,
    DROWNED,
    EGG,
    ELDER_GUARDIAN,
    END_CRYSTAL,
    ENDER_DRAGON,
    ENDER_PEARL,
    ENDERMAN,
    ENDERMITE,
    EVOKER,
    EVOKER_FANGS,
    EXPERIENCE_BOTTLE,
    EXPERIENCE_ORB,
    EYE_OF_ENDER,
    FALLING_BLOCK,
    FIREBALL,
    FIREWORK_ROCKET,
    FISHING_BOBBER,
    FURNACE_MINECART,
    GHAST,
    GIANT,
    GUARDIAN,
    HOPPER_MINECART,
    HORSE,
    HUSK,
    ILLUSIONER,
    IRON_GOLEM,
    ITEM,
    ITEM_FRAME,
    LEASH_KNOT,
    LIGHTNING_BOLT,
    LLAMA,
    LLAMA_SPIT,
    MAGMA_CUBE,
    MINECART,
    MOOSHROOM,
    MULE,
    OCELOT,
    PAINTING,
    PARROT,
    PHANTOM,
    PIG,
    PLAYER,
    POLAR_BEAR,
    POTION,
    PUFFERFISH,
    RABBIT,
    SALMON,
    SHEEP,
    SHULKER,
    SHULKER_BULLET,
    SILVERFISH,
    SKELETON,
    SKELETON_HORSE,
    SLIME,
    SMALL_FIREBALL,
    SNOW_GOLEM,
    SNOWBALL,
    SPAWNER_MINECART,
    SPECTRAL_ARROW,
    SPIDER,
    SQUID,
    STRAY,
    TNT,
    TNT_MINECART,
    TRIDENT,
    TROPICAL_FISH,
    TURTLE,
    VEX,
    VILLAGER,
    VINDICATOR,
    WITCH,
    WITHER,
    WITHER_SKELETON,
    WITHER_SKULL,
    WOLF,
    ZOMBIE,
    ZOMBIE_HORSE,
    ZOMBIE_PIGMAN,
    ZOMBIE_VILLAGER,

    ;

    private String id;
    private int internalId;

    EntityTypes() {
        this(null);
    }

    EntityTypes(String id) {
        init(id);
    }

    private void init(String id) {
        if (id == null) id = "minecraft:" + name().toLowerCase();
            // If it has no namespace, assume minecraft.
        else if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        this.id = id;
        this.internalId = ordinal();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return getId();
    }

    @Override
    public int getInternalId() {
        return internalId;
    }

    /*
     -----------------------------------------------------
                    Static Initializer
     -----------------------------------------------------
     */

    public static EntityType parse(String id) {
        if (id.startsWith("minecraft:")) id = id.substring(10);
        switch (id) {
            case "FallingSand": return EntityTypes.FALLING_BLOCK;
            case "FireworksRocketEntity": return EntityTypes.FIREWORK_ROCKET;
            case "LavaSlime": return EntityTypes.MAGMA_CUBE;
            case "MinecartChest": return EntityTypes.CHEST_MINECART;
            case "MinecartCommandBlock": return EntityTypes.COMMAND_BLOCK_MINECART;
            case "MinecartFurnace": return EntityTypes.FURNACE_MINECART;
            case "MinecartHopper": return EntityTypes.HOPPER_MINECART;
            case "MinecartRideable": return EntityTypes.MINECART;
            case "MinecartSpawner": return EntityTypes.SPAWNER_MINECART;
            case "MinecartTNT": return EntityTypes.TNT_MINECART;
            case "MushroomCow": return EntityTypes.MOOSHROOM;
            case "PigZombie": return EntityTypes.ZOMBIE_PIGMAN;
            case "PrimedTnt": return EntityTypes.TNT;
            case "SnowMan": return EntityTypes.SNOW_GOLEM;
            case "ThrownEgg": return EntityTypes.EGG;
            case "ThrownEnderpearl": return EntityTypes.ENDER_PEARL;
            case "ThrownExpBottle": return EntityTypes.EXPERIENCE_BOTTLE;
            case "ThrownPotion": return EntityTypes.POTION;
            case "WitherBoss": return EntityTypes.WITHER;
            case "XPOrb": return EntityTypes.EXPERIENCE_ORB;
            default:
                if (Character.isUpperCase(id.charAt(0))) {
                    StringBuilder result = new StringBuilder();
                    for (int i = 0; i < id.length(); i++) {
                        char c = id.charAt(i);
                        if (Character.isUpperCase(c)) {
                            c = Character.toLowerCase(c);
                            if (i != 0) result.append('_');
                        }
                        result.append(c);
                    }
                    return parse(result.toString());
                }
                switch (id.toLowerCase()) {
                    case "xp_orb":
                        return EntityTypes.EXPERIENCE_ORB;
                    case "xp_bottle":
                        return EntityTypes.EXPERIENCE_BOTTLE;
                    case "eye_of_ender_signal":
                        return EntityTypes.EYE_OF_ENDER;
                    case "ender_crystal":
                        return EntityTypes.END_CRYSTAL;
                    case "fireworks_rocket":
                        return EntityTypes.FIREWORK_ROCKET;
                    case "commandblock_minecart":
                        return EntityTypes.COMMAND_BLOCK_MINECART;
                    case "snowman":
                        return EntityTypes.SNOW_GOLEM;
                    case "villager_golem":
                        return EntityTypes.IRON_GOLEM;
                    case "evocation_fangs":
                        return EntityTypes.EVOKER_FANGS;
                    case "evocation_illager":
                        return EntityTypes.EVOKER;
                    case "vindication_illager":
                        return EntityTypes.VINDICATOR;
                    case "illusion_illager":
                        return EntityTypes.ILLUSIONER;
                    default:
                        return get(id);
                }
        }
    }

    private static final Map<String, EntityTypes> $REGISTRY = new HashMap<>();
    private static int $LENGTH;
    public static final EntityTypes[] values;

    static {
        try {
            Collection<String> ents = WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.GAME_HOOKS).getRegistries().getEntityRegistry().registerEntities();
            EntityTypes[] oldValues = values();
            $LENGTH = oldValues.length;
            LinkedHashSet<EntityTypes> newValues = new LinkedHashSet<>(Arrays.asList(oldValues));
            if (!ents.isEmpty()) { // No types found - use defaults
                for (String ent : ents) {
                    EntityTypes registered = register(ent);
                    if (!newValues.contains(registered)) newValues.add(registered);
                }
            }
            // Cache the values
            values = newValues.toArray(new EntityTypes[newValues.size()]);
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static EntityTypes register(final String id) {
        // Get the enum name (remove namespace if minecraft:)
        int propStart = id.indexOf('[');
        String typeName = id.substring(0, propStart == -1 ? id.length() : propStart);
        String enumName = (typeName.startsWith("minecraft:") ? typeName.substring(10) : typeName).toUpperCase();
        // Check existing
        EntityTypes existing = null;
        try { existing = valueOf(enumName.toUpperCase()); } catch (IllegalArgumentException ignore) {}
        if (existing == null) {
            existing = ReflectionUtils.addEnum(EntityTypes.class, enumName);
        }
        int internalId = existing.ordinal();
        if (existing.id == null) {
            existing.init(null);
        }
        if (internalId == 0 && existing != __RESERVED__) {
            existing.internalId = $LENGTH++;
        }
        if (typeName.startsWith("minecraft:")) $REGISTRY.put(typeName.substring(10), existing);
        $REGISTRY.put(typeName, existing);
        return existing;
    }

    public static final @Nullable EntityTypes get(final String id) {
        return $REGISTRY.get(id);
    }

    @Deprecated
    public static final EntityTypes get(final int ordinal) {
        return values[ordinal];
    }

    public static int size() {
        return values.length;
    }

}
