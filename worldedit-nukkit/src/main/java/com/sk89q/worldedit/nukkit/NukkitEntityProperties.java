package com.sk89q.worldedit.nukkit;

import com.fastasyncworldedit.nukkit.adapter.NukkitImplLoader;
import com.sk89q.worldedit.entity.metadata.EntityProperties;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Set;

/**
 * Conservative {@link EntityProperties} implementation for Nukkit entities.
 * <p>
 * Nukkit does not expose a Bukkit-like typed entity hierarchy consistently
 * across MOT and NKX, so classification is based on the active adapter's
 * Bedrock entity identifier and common NBT/name-tag fields. Unknown entities
 * are treated as pasteable but not as a specific removable category.
 */
class NukkitEntityProperties implements EntityProperties {

    private static final Set<String> PROJECTILES = Set.of(
            "minecraft:arrow",
            "minecraft:dragon_fireball",
            "minecraft:egg",
            "minecraft:ender_pearl",
            "minecraft:evocation_fang",
            "minecraft:fireball",
            "minecraft:fishing_hook",
            "minecraft:llama_spit",
            "minecraft:shulker_bullet",
            "minecraft:small_fireball",
            "minecraft:snowball",
            "minecraft:splash_potion",
            "minecraft:thrown_trident",
            "minecraft:wither_skull",
            "minecraft:xp_bottle"
    );
    private static final Set<String> ITEMS = Set.of("minecraft:item");
    private static final Set<String> FALLING_BLOCKS = Set.of("minecraft:falling_block");
    private static final Set<String> PAINTINGS = Set.of("minecraft:painting");
    private static final Set<String> ITEM_FRAMES = Set.of("minecraft:item_frame", "minecraft:glow_item_frame");
    private static final Set<String> BOATS = Set.of(
            "minecraft:boat",
            "minecraft:chest_boat"
    );
    private static final Set<String> MINECARTS = Set.of(
            "minecraft:minecart",
            "minecraft:chest_minecart",
            "minecraft:command_block_minecart",
            "minecraft:furnace_minecart",
            "minecraft:hopper_minecart",
            "minecraft:spawner_minecart",
            "minecraft:tnt_minecart"
    );
    private static final Set<String> TNT = Set.of("minecraft:tnt", "minecraft:tnt_minecart");
    private static final Set<String> EXPERIENCE_ORBS = Set.of("minecraft:xp_orb");
    private static final Set<String> ANIMALS = Set.of(
            "minecraft:allay",
            "minecraft:armadillo",
            "minecraft:bee",
            "minecraft:camel",
            "minecraft:cat",
            "minecraft:chicken",
            "minecraft:cow",
            "minecraft:donkey",
            "minecraft:fox",
            "minecraft:frog",
            "minecraft:goat",
            "minecraft:hoglin",
            "minecraft:horse",
            "minecraft:llama",
            "minecraft:mooshroom",
            "minecraft:mule",
            "minecraft:ocelot",
            "minecraft:panda",
            "minecraft:parrot",
            "minecraft:pig",
            "minecraft:polar_bear",
            "minecraft:rabbit",
            "minecraft:sheep",
            "minecraft:sniffer",
            "minecraft:strider",
            "minecraft:tadpole",
            "minecraft:turtle",
            "minecraft:wolf",
            "minecraft:zoglin"
    );
    private static final Set<String> AMBIENT = Set.of("minecraft:bat");
    private static final Set<String> NPCS = Set.of(
            "minecraft:npc",
            "minecraft:villager",
            "minecraft:villager_v2",
            "minecraft:wandering_trader"
    );
    private static final Set<String> GOLEMS = Set.of("minecraft:iron_golem", "minecraft:snow_golem");
    private static final Set<String> WATER_CREATURES = Set.of(
            "minecraft:axolotl",
            "minecraft:cod",
            "minecraft:dolphin",
            "minecraft:elder_guardian",
            "minecraft:glow_squid",
            "minecraft:guardian",
            "minecraft:pufferfish",
            "minecraft:salmon",
            "minecraft:squid",
            "minecraft:tropicalfish",
            "minecraft:tropical_fish",
            "minecraft:turtle"
    );
    private static final Set<String> LIVING_HOSTILES = Set.of(
            "minecraft:blaze",
            "minecraft:breeze",
            "minecraft:cave_spider",
            "minecraft:creeper",
            "minecraft:drowned",
            "minecraft:elder_guardian",
            "minecraft:ender_dragon",
            "minecraft:enderman",
            "minecraft:endermite",
            "minecraft:evocation_illager",
            "minecraft:evoker",
            "minecraft:ghast",
            "minecraft:guardian",
            "minecraft:husk",
            "minecraft:magma_cube",
            "minecraft:phantom",
            "minecraft:piglin",
            "minecraft:piglin_brute",
            "minecraft:pillager",
            "minecraft:ravager",
            "minecraft:shulker",
            "minecraft:silverfish",
            "minecraft:skeleton",
            "minecraft:slime",
            "minecraft:spider",
            "minecraft:stray",
            "minecraft:vex",
            "minecraft:vindicator",
            "minecraft:warden",
            "minecraft:witch",
            "minecraft:wither",
            "minecraft:wither_skeleton",
            "minecraft:zombie",
            "minecraft:zombie_pigman",
            "minecraft:zombie_villager",
            "minecraft:zombie_villager_v2",
            "minecraft:zombified_piglin"
    );
    private static final Set<String> OTHER_LIVING = Set.of(
            "minecraft:armor_stand",
            "minecraft:player"
    );
    private static final Set<String> TAMEABLE = Set.of(
            "minecraft:cat",
            "minecraft:donkey",
            "minecraft:horse",
            "minecraft:llama",
            "minecraft:mule",
            "minecraft:ocelot",
            "minecraft:parrot",
            "minecraft:wolf"
    );

    private final cn.nukkit.entity.Entity entity;
    private String identifier;

    NukkitEntityProperties(cn.nukkit.entity.Entity entity) {
        this.entity = entity;
    }

    @Override
    public boolean isPlayerDerived() {
        return entity instanceof cn.nukkit.Player || is("minecraft:player");
    }

    @Override
    public boolean isProjectile() {
        return isAny(PROJECTILES);
    }

    @Override
    public boolean isItem() {
        return isAny(ITEMS);
    }

    @Override
    public boolean isFallingBlock() {
        return isAny(FALLING_BLOCKS);
    }

    @Override
    public boolean isPainting() {
        return isAny(PAINTINGS);
    }

    @Override
    public boolean isItemFrame() {
        return isAny(ITEM_FRAMES);
    }

    @Override
    public boolean isBoat() {
        return isAny(BOATS) || identifierEndsWith("_boat");
    }

    @Override
    public boolean isMinecart() {
        return isAny(MINECARTS) || identifierEndsWith("_minecart");
    }

    @Override
    public boolean isTNT() {
        return isAny(TNT);
    }

    @Override
    public boolean isExperienceOrb() {
        return isAny(EXPERIENCE_ORBS);
    }

    @Override
    public boolean isLiving() {
        return isPlayerDerived()
                || isAnimal()
                || isAmbient()
                || isNPC()
                || isGolem()
                || isWaterCreature()
                || isAny(LIVING_HOSTILES)
                || isAny(OTHER_LIVING);
    }

    @Override
    public boolean isAnimal() {
        return isAny(ANIMALS);
    }

    @Override
    public boolean isAmbient() {
        return isAny(AMBIENT);
    }

    @Override
    public boolean isNPC() {
        return isAny(NPCS);
    }

    @Override
    public boolean isGolem() {
        return isAny(GOLEMS);
    }

    @Override
    public boolean isTamed() {
        return isAny(TAMEABLE) && (
                hasNamedTag("Owner")
                        || hasNamedTag("OwnerUUID")
                        || hasNamedTag("OwnerUniqueID")
                        || hasNamedTag("PersistentIDMSB")
                        || hasNamedTag("Tamed")
        );
    }

    @Override
    public boolean isTagged() {
        String nameTag = invokeString(entity, "getNameTag");
        return nameTag != null && !nameTag.isEmpty()
                || hasNamedTag("CustomName")
                || hasNamedTag("NameTag")
                || hasNamedTag("customName");
    }

    @Override
    public boolean isArmorStand() {
        return is("minecraft:armor_stand");
    }

    @Override
    public boolean isPasteable() {
        return !isPlayerDerived();
    }

    @Override
    public boolean isWaterCreature() {
        return isAny(WATER_CREATURES);
    }

    private boolean is(String expected) {
        String id = identifier();
        return expected.equals(id);
    }

    private boolean isAny(Set<String> expected) {
        String id = identifier();
        return id != null && expected.contains(id);
    }

    private boolean identifierEndsWith(String suffix) {
        String id = identifier();
        return id != null && id.endsWith(suffix);
    }

    private String identifier() {
        if (identifier != null) {
            return identifier;
        }
        if (entity instanceof cn.nukkit.Player) {
            identifier = "minecraft:player";
            return identifier;
        }
        try {
            identifier = normalize(NukkitImplLoader.get().getEntityIdentifier(entity));
            if (identifier != null) {
                return identifier;
            }
        } catch (IllegalStateException ignored) {
        }
        identifier = normalize(namedTagString("id"));
        if (identifier == null) {
            identifier = normalize(namedTagString("Id"));
        }
        return identifier;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String id = value.toLowerCase(Locale.ROOT);
        return id.indexOf(':') == -1 ? "minecraft:" + id : id;
    }

    private boolean hasNamedTag(String key) {
        Object tag = entity.namedTag;
        if (tag == null) {
            return false;
        }
        Boolean containsKey = invokeBoolean(tag, "containsKey", Object.class, key);
        if (containsKey != null) {
            return containsKey;
        }
        Boolean contains = invokeBoolean(tag, "contains", String.class, key);
        if (contains != null) {
            return contains;
        }
        Object value = invokeObject(tag, "get", Object.class, key);
        return value != null;
    }

    private String namedTagString(String key) {
        Object tag = entity.namedTag;
        if (tag == null) {
            return null;
        }
        String value = invokeString(tag, "getString", String.class, key);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        Object raw = invokeObject(tag, "get", Object.class, key);
        return raw != null ? raw.toString() : null;
    }

    private static String invokeString(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object value = method.invoke(target);
            return value instanceof String string ? string : null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    private static String invokeString(Object target, String methodName, Class<?> parameterType, Object argument) {
        Object value = invokeObject(target, methodName, parameterType, argument);
        return value instanceof String string ? string : null;
    }

    private static Boolean invokeBoolean(Object target, String methodName, Class<?> parameterType, Object argument) {
        Object value = invokeObject(target, methodName, parameterType, argument);
        return value instanceof Boolean bool ? bool : null;
    }

    private static Object invokeObject(Object target, String methodName, Class<?> parameterType, Object argument) {
        try {
            Method method = target.getClass().getMethod(methodName, parameterType);
            return method.invoke(target, argument);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

}
