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

import javax.annotation.Nullable;
import java.util.Locale;

public class EntityTypes {

    @Nullable public static final EntityType AREA_EFFECT_CLOUD = get("minecraft:area_effect_cloud");
    @Nullable public static final EntityType ARMOR_STAND = get("minecraft:armor_stand");
    @Nullable public static final EntityType ARROW = get("minecraft:arrow");
    @Nullable public static final EntityType BAT = get("minecraft:bat");
    @Nullable public static final EntityType BEE = get("minecraft:bee");
    @Nullable public static final EntityType BLAZE = get("minecraft:blaze");
    @Nullable public static final EntityType BOAT = get("minecraft:boat");
    @Nullable public static final EntityType CAT = get("minecraft:cat");
    @Nullable public static final EntityType CAVE_SPIDER = get("minecraft:cave_spider");
    @Nullable public static final EntityType CHEST_MINECART = get("minecraft:chest_minecart");
    @Nullable public static final EntityType CHICKEN = get("minecraft:chicken");
    @Nullable public static final EntityType COD = get("minecraft:cod");
    @Nullable public static final EntityType COMMAND_BLOCK_MINECART = get("minecraft:command_block_minecart");
    @Nullable public static final EntityType COW = get("minecraft:cow");
    @Nullable public static final EntityType CREEPER = get("minecraft:creeper");
    @Nullable public static final EntityType DOLPHIN = get("minecraft:dolphin");
    @Nullable public static final EntityType DONKEY = get("minecraft:donkey");
    @Nullable public static final EntityType DRAGON_FIREBALL = get("minecraft:dragon_fireball");
    @Nullable public static final EntityType DROWNED = get("minecraft:drowned");
    @Nullable public static final EntityType EGG = get("minecraft:egg");
    @Nullable public static final EntityType ELDER_GUARDIAN = get("minecraft:elder_guardian");
    @Nullable public static final EntityType END_CRYSTAL = get("minecraft:end_crystal");
    @Nullable public static final EntityType ENDER_DRAGON = get("minecraft:ender_dragon");
    @Nullable public static final EntityType ENDER_PEARL = get("minecraft:ender_pearl");
    @Nullable public static final EntityType ENDERMAN = get("minecraft:enderman");
    @Nullable public static final EntityType ENDERMITE = get("minecraft:endermite");
    @Nullable public static final EntityType EVOKER = get("minecraft:evoker");
    @Nullable public static final EntityType EVOKER_FANGS = get("minecraft:evoker_fangs");
    @Nullable public static final EntityType EXPERIENCE_BOTTLE = get("minecraft:experience_bottle");
    @Nullable public static final EntityType EXPERIENCE_ORB = get("minecraft:experience_orb");
    @Nullable public static final EntityType EYE_OF_ENDER = get("minecraft:eye_of_ender");
    @Nullable public static final EntityType FALLING_BLOCK = get("minecraft:falling_block");
    @Nullable public static final EntityType FIREBALL = get("minecraft:fireball");
    @Nullable public static final EntityType FIREWORK_ROCKET = get("minecraft:firework_rocket");
    @Nullable public static final EntityType FISHING_BOBBER = get("minecraft:fishing_bobber");
    @Nullable public static final EntityType FOX = get("minecraft:fox");
    @Nullable public static final EntityType FURNACE_MINECART = get("minecraft:furnace_minecart");
    @Nullable public static final EntityType GHAST = get("minecraft:ghast");
    @Nullable public static final EntityType GIANT = get("minecraft:giant");
    @Nullable public static final EntityType GUARDIAN = get("minecraft:guardian");
    @Nullable public static final EntityType HOPPER_MINECART = get("minecraft:hopper_minecart");
    @Nullable public static final EntityType HORSE = get("minecraft:horse");
    @Nullable public static final EntityType HUSK = get("minecraft:husk");
    @Nullable public static final EntityType ILLUSIONER = get("minecraft:illusioner");
    @Nullable public static final EntityType IRON_GOLEM = get("minecraft:iron_golem");
    @Nullable public static final EntityType ITEM = get("minecraft:item");
    @Nullable public static final EntityType ITEM_FRAME = get("minecraft:item_frame");
    @Nullable public static final EntityType LEASH_KNOT = get("minecraft:leash_knot");
    @Nullable public static final EntityType LIGHTNING_BOLT = get("minecraft:lightning_bolt");
    @Nullable public static final EntityType LLAMA = get("minecraft:llama");
    @Nullable public static final EntityType LLAMA_SPIT = get("minecraft:llama_spit");
    @Nullable public static final EntityType MAGMA_CUBE = get("minecraft:magma_cube");
    @Nullable public static final EntityType MINECART = get("minecraft:minecart");
    @Nullable public static final EntityType MOOSHROOM = get("minecraft:mooshroom");
    @Nullable public static final EntityType MULE = get("minecraft:mule");
    @Nullable public static final EntityType OCELOT = get("minecraft:ocelot");
    @Nullable public static final EntityType PAINTING = get("minecraft:painting");
    @Nullable public static final EntityType PANDA = get("minecraft:panda");
    @Nullable public static final EntityType PARROT = get("minecraft:parrot");
    @Nullable public static final EntityType PHANTOM = get("minecraft:phantom");
    @Nullable public static final EntityType PIG = get("minecraft:pig");
    @Nullable public static final EntityType PILLAGER = get("minecraft:pillager");
    @Nullable public static final EntityType PLAYER = get("minecraft:player");
    @Nullable public static final EntityType POLAR_BEAR = get("minecraft:polar_bear");
    @Nullable public static final EntityType POTION = get("minecraft:potion");
    @Nullable public static final EntityType PUFFERFISH = get("minecraft:pufferfish");
    @Nullable public static final EntityType RABBIT = get("minecraft:rabbit");
    @Nullable public static final EntityType RAVAGER = get("minecraft:ravager");
    @Nullable public static final EntityType SALMON = get("minecraft:salmon");
    @Nullable public static final EntityType SHEEP = get("minecraft:sheep");
    @Nullable public static final EntityType SHULKER = get("minecraft:shulker");
    @Nullable public static final EntityType SHULKER_BULLET = get("minecraft:shulker_bullet");
    @Nullable public static final EntityType SILVERFISH = get("minecraft:silverfish");
    @Nullable public static final EntityType SKELETON = get("minecraft:skeleton");
    @Nullable public static final EntityType SKELETON_HORSE = get("minecraft:skeleton_horse");
    @Nullable public static final EntityType SLIME = get("minecraft:slime");
    @Nullable public static final EntityType SMALL_FIREBALL = get("minecraft:small_fireball");
    @Nullable public static final EntityType SNOW_GOLEM = get("minecraft:snow_golem");
    @Nullable public static final EntityType SNOWBALL = get("minecraft:snowball");
    @Nullable public static final EntityType SPAWNER_MINECART = get("minecraft:spawner_minecart");
    @Nullable public static final EntityType SPECTRAL_ARROW = get("minecraft:spectral_arrow");
    @Nullable public static final EntityType SPIDER = get("minecraft:spider");
    @Nullable public static final EntityType SQUID = get("minecraft:squid");
    @Nullable public static final EntityType STRAY = get("minecraft:stray");
    @Nullable public static final EntityType TNT = get("minecraft:tnt");
    @Nullable public static final EntityType TNT_MINECART = get("minecraft:tnt_minecart");
    @Nullable public static final EntityType TRADER_LLAMA = get("minecraft:trader_llama");
    @Nullable public static final EntityType TRIDENT = get("minecraft:trident");
    @Nullable public static final EntityType TROPICAL_FISH = get("minecraft:tropical_fish");
    @Nullable public static final EntityType TURTLE = get("minecraft:turtle");
    @Nullable public static final EntityType VEX = get("minecraft:vex");
    @Nullable public static final EntityType VILLAGER = get("minecraft:villager");
    @Nullable public static final EntityType VINDICATOR = get("minecraft:vindicator");
    @Nullable public static final EntityType WANDERING_TRADER = get("minecraft:wandering_trader");
    @Nullable public static final EntityType WITCH = get("minecraft:witch");
    @Nullable public static final EntityType WITHER = get("minecraft:wither");
    @Nullable public static final EntityType WITHER_SKELETON = get("minecraft:wither_skeleton");
    @Nullable public static final EntityType WITHER_SKULL = get("minecraft:wither_skull");
    @Nullable public static final EntityType WOLF = get("minecraft:wolf");
    @Nullable public static final EntityType ZOMBIE = get("minecraft:zombie");
    @Nullable public static final EntityType ZOMBIE_HORSE = get("minecraft:zombie_horse");
    @Nullable public static final EntityType ZOMBIE_PIGMAN = get("minecraft:zombie_pigman");
    @Nullable public static final EntityType ZOMBIE_VILLAGER = get("minecraft:zombie_villager");

    private EntityTypes() {
    }

    public static @Nullable EntityType get(final String id) {
        return EntityType.REGISTRY.get(id);
    }

    private static String convertEntityId(String id) {
        if (id.startsWith("minecraft:")) id = id.substring(10);
        switch(id) {
            case "AreaEffectCloud": return "area_effect_cloud";
            case "ArmorStand": return "armor_stand";
            case "CaveSpider": return "cave_spider";
            case "MinecartChest": return "chest_minecart";
            case "DragonFireball": return "dragon_fireball";
            case "ThrownEgg": return "egg";
            case "EnderDragon": return "ender_dragon";
            case "ThrownEnderpearl": return "ender_pearl";
            case "FallingSand": return "falling_block";
            case "FireworksRocketEntity": return "fireworks_rocket";
            case "MinecartFurnace": return "furnace_minecart";
            case "MinecartHopper": return "hopper_minecart";
            case "EntityHorse": return "horse";
            case "ItemFrame": return "item_frame";
            case "LeashKnot": return "leash_knot";
            case "LightningBolt": return "lightning_bolt";
            case "LavaSlime": return "magma_cube";
            case "MinecartRideable": return "minecart";
            case "MushroomCow": return "mooshroom";
            case "Ozelot": return "ocelot";
            case "PolarBear": return "polar_bear";
            case "ThrownPotion": return "potion";
            case "ShulkerBullet": return "shulker_bullet";
            case "SmallFireball": return "small_fireball";
            case "MinecartSpawner": return "spawner_minecart";
            case "SpectralArrow": return "spectral_arrow";
            case "PrimedTnt": return "tnt";
            case "MinecartTNT": return "tnt_minecart";
            case "VillagerGolem": return "villager_golem";
            case "WitherBoss": return "wither";
            case "WitherSkull": return "wither_skull";
            case "PigZombie": return "zombie_pigman";
            case "XPOrb": return "experience_orb";
            case "ThrownExpBottle": return "experience_bottle";
            case "EyeOfEnderSignal": return "eye_of_ender";
            case "EnderCrystal": return "end_crystal";
            case "MinecartCommandBlock": return "command_block_minecart";
            case "SnowMan": return "snow_golem";
            case "areaeffectcloud": return "area_effect_cloud";
            case "armorstand": return "armor_stand";
            case "cavespider": return "cave_spider";
            case "minecartchest": return "chest_minecart";
            case "dragonfireball": return "dragon_fireball";
            case "thrownegg": return "egg";
            case "enderdragon": return "ender_dragon";
            case "thrownenderpearl": return "ender_pearl";
            case "fallingsand": return "falling_block";
            case "fireworksrocketentity": return "fireworks_rocket";
            case "minecartfurnace": return "furnace_minecart";
            case "minecarthopper": return "hopper_minecart";
            case "entityhorse": return "horse";
            case "itemframe": return "item_frame";
            case "leashknot": return "leash_knot";
            case "lightningbolt": return "lightning_bolt";
            case "lavaslime": return "magma_cube";
            case "minecartrideable": return "minecart";
            case "mushroomcow": return "mooshroom";
            case "ozelot": return "ocelot";
            case "polarbear": return "polar_bear";
            case "thrownpotion": return "potion";
            case "shulkerbullet": return "shulker_bullet";
            case "smallfireball": return "small_fireball";
            case "minecartspawner": return "spawner_minecart";
            case "spectralarrow": return "spectral_arrow";
            case "primedtnt": return "tnt";
            case "minecarttnt": return "tnt_minecart";
            case "villagergolem": return "villager_golem";
            case "witherboss": return "wither";
            case "witherskull": return "wither_skull";
            case "pigzombie": return "zombie_pigman";
            case "xporb":
            case "xp_orb":
                return "experience_orb";
            case "thrownexpbottle":
            case "xp_bottle":
                return "experience_bottle";
            case "eyeofendersignal":
            case "eye_of_ender_signal":
                return "eye_of_ender";
            case "endercrystal":
            case "ender_crystal":
                return "end_crystal";
            case "fireworks_rocket": return "firework_rocket";
            case "minecartcommandblock":
            case "commandblock_minecart":
                return "command_block_minecart";
            case "snowman":
                return "snow_golem";
            case "villager_golem": return "iron_golem";
            case "evocation_fangs": return "evoker_fangs";
            case "evocation_illager": return "evoker";
            case "vindication_illager": return "vindicator";
            case "illusion_illager": return "illusioner";
            default: {
                if (Character.isUpperCase(id.charAt(0))) {
                    return convertEntityId(id.toLowerCase(Locale.ROOT));
                }
                return id;
            }
        }
    }

    public static EntityType parse(String id) {
        return get(convertEntityId(id));
    }

}
