package com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter;

import com.google.common.collect.Sets;

import java.util.Set;

public class DataConverterHealth implements DataConverter {

    private static final Set<String> ENTITIES_NAMES = Sets.newHashSet(
            "ArmorStand",
            "Bat",
            "Blaze",
            "CaveSpider",
            "Chicken",
            "Cow",
            "Creeper",
            "EnderDragon",
            "Enderman",
            "Endermite",
            "EntityHorse",
            "Ghast",
            "Giant",
            "Guardian",
            "LavaSlime",
            "MushroomCow",
            "Ozelot",
            "Pig",
            "PigZombie",
            "Rabbit",
            "Sheep",
            "Shulker",
            "Silverfish",
            "Skeleton",
            "Slime",
            "SnowMan",
            "Spider",
            "Squid",
            "Villager",
            "VillagerGolem",
            "Witch",
            "WitherBoss",
            "Wolf",
            "Zombie"
    );

    public DataConverterHealth() {
    }

    public int getDataVersion() {
        return 109;
    }

    public net.minecraft.nbt.CompoundTag convert(net.minecraft.nbt.CompoundTag cmp) {
        if (DataConverterHealth.ENTITIES_NAMES.contains(cmp.getString("id"))) {
            float f;

            if (cmp.contains("HealF", 99)) {
                f = cmp.getFloat("HealF");
                cmp.remove("HealF");
            } else {
                if (!cmp.contains("Health", 99)) {
                    return cmp;
                }

                f = cmp.getFloat("Health");
            }

            cmp.putFloat("Health", f);
        }

        return cmp;
    }

}
