package com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter;

import com.google.common.collect.Maps;

import java.util.Map;

public class DataConverterEntity implements DataConverter {

    private static final Map<String, String> a = Maps.newHashMap();

    public DataConverterEntity() {
    }

    public int getDataVersion() {
        return 704;
    }

    public net.minecraft.nbt.CompoundTag convert(net.minecraft.nbt.CompoundTag cmp) {
        String s = DataConverterEntity.a.get(cmp.getString("id"));

        if (s != null) {
            cmp.putString("id", s);
        }

        return cmp;
    }

    static {
        DataConverterEntity.a.put("AreaEffectCloud", "minecraft:area_effect_cloud");
        DataConverterEntity.a.put("ArmorStand", "minecraft:armor_stand");
        DataConverterEntity.a.put("Arrow", "minecraft:arrow");
        DataConverterEntity.a.put("Bat", "minecraft:bat");
        DataConverterEntity.a.put("Blaze", "minecraft:blaze");
        DataConverterEntity.a.put("Boat", "minecraft:boat");
        DataConverterEntity.a.put("CaveSpider", "minecraft:cave_spider");
        DataConverterEntity.a.put("Chicken", "minecraft:chicken");
        DataConverterEntity.a.put("Cow", "minecraft:cow");
        DataConverterEntity.a.put("Creeper", "minecraft:creeper");
        DataConverterEntity.a.put("Donkey", "minecraft:donkey");
        DataConverterEntity.a.put("DragonFireball", "minecraft:dragon_fireball");
        DataConverterEntity.a.put("ElderGuardian", "minecraft:elder_guardian");
        DataConverterEntity.a.put("EnderCrystal", "minecraft:ender_crystal");
        DataConverterEntity.a.put("EnderDragon", "minecraft:ender_dragon");
        DataConverterEntity.a.put("Enderman", "minecraft:enderman");
        DataConverterEntity.a.put("Endermite", "minecraft:endermite");
        DataConverterEntity.a.put("EyeOfEnderSignal", "minecraft:eye_of_ender_signal");
        DataConverterEntity.a.put("FallingSand", "minecraft:falling_block");
        DataConverterEntity.a.put("Fireball", "minecraft:fireball");
        DataConverterEntity.a.put("FireworksRocketEntity", "minecraft:fireworks_rocket");
        DataConverterEntity.a.put("Ghast", "minecraft:ghast");
        DataConverterEntity.a.put("Giant", "minecraft:giant");
        DataConverterEntity.a.put("Guardian", "minecraft:guardian");
        DataConverterEntity.a.put("Horse", "minecraft:horse");
        DataConverterEntity.a.put("Husk", "minecraft:husk");
        DataConverterEntity.a.put("Item", "minecraft:item");
        DataConverterEntity.a.put("ItemFrame", "minecraft:item_frame");
        DataConverterEntity.a.put("LavaSlime", "minecraft:magma_cube");
        DataConverterEntity.a.put("LeashKnot", "minecraft:leash_knot");
        DataConverterEntity.a.put("MinecartChest", "minecraft:chest_minecart");
        DataConverterEntity.a.put("MinecartCommandBlock", "minecraft:commandblock_minecart");
        DataConverterEntity.a.put("MinecartFurnace", "minecraft:furnace_minecart");
        DataConverterEntity.a.put("MinecartHopper", "minecraft:hopper_minecart");
        DataConverterEntity.a.put("MinecartRideable", "minecraft:minecart");
        DataConverterEntity.a.put("MinecartSpawner", "minecraft:spawner_minecart");
        DataConverterEntity.a.put("MinecartTNT", "minecraft:tnt_minecart");
        DataConverterEntity.a.put("Mule", "minecraft:mule");
        DataConverterEntity.a.put("MushroomCow", "minecraft:mooshroom");
        DataConverterEntity.a.put("Ozelot", "minecraft:ocelot");
        DataConverterEntity.a.put("Painting", "minecraft:painting");
        DataConverterEntity.a.put("Pig", "minecraft:pig");
        DataConverterEntity.a.put("PigZombie", "minecraft:zombie_pigman");
        DataConverterEntity.a.put("PolarBear", "minecraft:polar_bear");
        DataConverterEntity.a.put("PrimedTnt", "minecraft:tnt");
        DataConverterEntity.a.put("Rabbit", "minecraft:rabbit");
        DataConverterEntity.a.put("Sheep", "minecraft:sheep");
        DataConverterEntity.a.put("Shulker", "minecraft:shulker");
        DataConverterEntity.a.put("ShulkerBullet", "minecraft:shulker_bullet");
        DataConverterEntity.a.put("Silverfish", "minecraft:silverfish");
        DataConverterEntity.a.put("Skeleton", "minecraft:skeleton");
        DataConverterEntity.a.put("SkeletonHorse", "minecraft:skeleton_horse");
        DataConverterEntity.a.put("Slime", "minecraft:slime");
        DataConverterEntity.a.put("SmallFireball", "minecraft:small_fireball");
        DataConverterEntity.a.put("SnowMan", "minecraft:snowman");
        DataConverterEntity.a.put("Snowball", "minecraft:snowball");
        DataConverterEntity.a.put("SpectralArrow", "minecraft:spectral_arrow");
        DataConverterEntity.a.put("Spider", "minecraft:spider");
        DataConverterEntity.a.put("Squid", "minecraft:squid");
        DataConverterEntity.a.put("Stray", "minecraft:stray");
        DataConverterEntity.a.put("ThrownEgg", "minecraft:egg");
        DataConverterEntity.a.put("ThrownEnderpearl", "minecraft:ender_pearl");
        DataConverterEntity.a.put("ThrownExpBottle", "minecraft:xp_bottle");
        DataConverterEntity.a.put("ThrownPotion", "minecraft:potion");
        DataConverterEntity.a.put("Villager", "minecraft:villager");
        DataConverterEntity.a.put("VillagerGolem", "minecraft:villager_golem");
        DataConverterEntity.a.put("Witch", "minecraft:witch");
        DataConverterEntity.a.put("WitherBoss", "minecraft:wither");
        DataConverterEntity.a.put("WitherSkeleton", "minecraft:wither_skeleton");
        DataConverterEntity.a.put("WitherSkull", "minecraft:wither_skull");
        DataConverterEntity.a.put("Wolf", "minecraft:wolf");
        DataConverterEntity.a.put("XPOrb", "minecraft:xp_orb");
        DataConverterEntity.a.put("Zombie", "minecraft:zombie");
        DataConverterEntity.a.put("ZombieHorse", "minecraft:zombie_horse");
        DataConverterEntity.a.put("ZombieVillager", "minecraft:zombie_villager");
    }

}
