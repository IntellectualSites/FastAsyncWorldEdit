package com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter;

public class DataConverterSpawnEgg implements DataConverter {

    private static final String[] eggs = new String[256];

    public DataConverterSpawnEgg() {
    }

    public int getDataVersion() {
        return 105;
    }

    public net.minecraft.nbt.CompoundTag convert(net.minecraft.nbt.CompoundTag cmp) {
        if ("minecraft:spawn_egg".equals(cmp.getString("id"))) {
            net.minecraft.nbt.CompoundTag nbttagcompound1 = cmp.getCompound("tag");
            net.minecraft.nbt.CompoundTag nbttagcompound2 = nbttagcompound1.getCompound("EntityTag");
            short short0 = cmp.getShort("Damage");

            if (!nbttagcompound2.contains("id", 8)) {
                String s = DataConverterSpawnEgg.eggs[short0 & 255];

                if (s != null) {
                    nbttagcompound2.putString("id", s);
                    nbttagcompound1.put("EntityTag", nbttagcompound2);
                    cmp.put("tag", nbttagcompound1);
                }
            }

            if (short0 != 0) {
                cmp.putShort("Damage", (short) 0);
            }
        }

        return cmp;
    }

    static {

        DataConverterSpawnEgg.eggs[1] = "Item";
        DataConverterSpawnEgg.eggs[2] = "XPOrb";
        DataConverterSpawnEgg.eggs[7] = "ThrownEgg";
        DataConverterSpawnEgg.eggs[8] = "LeashKnot";
        DataConverterSpawnEgg.eggs[9] = "Painting";
        DataConverterSpawnEgg.eggs[10] = "Arrow";
        DataConverterSpawnEgg.eggs[11] = "Snowball";
        DataConverterSpawnEgg.eggs[12] = "Fireball";
        DataConverterSpawnEgg.eggs[13] = "SmallFireball";
        DataConverterSpawnEgg.eggs[14] = "ThrownEnderpearl";
        DataConverterSpawnEgg.eggs[15] = "EyeOfEnderSignal";
        DataConverterSpawnEgg.eggs[16] = "ThrownPotion";
        DataConverterSpawnEgg.eggs[17] = "ThrownExpBottle";
        DataConverterSpawnEgg.eggs[18] = "ItemFrame";
        DataConverterSpawnEgg.eggs[19] = "WitherSkull";
        DataConverterSpawnEgg.eggs[20] = "PrimedTnt";
        DataConverterSpawnEgg.eggs[21] = "FallingSand";
        DataConverterSpawnEgg.eggs[22] = "FireworksRocketEntity";
        DataConverterSpawnEgg.eggs[23] = "TippedArrow";
        DataConverterSpawnEgg.eggs[24] = "SpectralArrow";
        DataConverterSpawnEgg.eggs[25] = "ShulkerBullet";
        DataConverterSpawnEgg.eggs[26] = "DragonFireball";
        DataConverterSpawnEgg.eggs[30] = "ArmorStand";
        DataConverterSpawnEgg.eggs[41] = "Boat";
        DataConverterSpawnEgg.eggs[42] = "MinecartRideable";
        DataConverterSpawnEgg.eggs[43] = "MinecartChest";
        DataConverterSpawnEgg.eggs[44] = "MinecartFurnace";
        DataConverterSpawnEgg.eggs[45] = "MinecartTNT";
        DataConverterSpawnEgg.eggs[46] = "MinecartHopper";
        DataConverterSpawnEgg.eggs[47] = "MinecartSpawner";
        DataConverterSpawnEgg.eggs[40] = "MinecartCommandBlock";
        DataConverterSpawnEgg.eggs[48] = "Mob";
        DataConverterSpawnEgg.eggs[49] = "Monster";
        DataConverterSpawnEgg.eggs[50] = "Creeper";
        DataConverterSpawnEgg.eggs[51] = "Skeleton";
        DataConverterSpawnEgg.eggs[52] = "Spider";
        DataConverterSpawnEgg.eggs[53] = "Giant";
        DataConverterSpawnEgg.eggs[54] = "Zombie";
        DataConverterSpawnEgg.eggs[55] = "Slime";
        DataConverterSpawnEgg.eggs[56] = "Ghast";
        DataConverterSpawnEgg.eggs[57] = "PigZombie";
        DataConverterSpawnEgg.eggs[58] = "Enderman";
        DataConverterSpawnEgg.eggs[59] = "CaveSpider";
        DataConverterSpawnEgg.eggs[60] = "Silverfish";
        DataConverterSpawnEgg.eggs[61] = "Blaze";
        DataConverterSpawnEgg.eggs[62] = "LavaSlime";
        DataConverterSpawnEgg.eggs[63] = "EnderDragon";
        DataConverterSpawnEgg.eggs[64] = "WitherBoss";
        DataConverterSpawnEgg.eggs[65] = "Bat";
        DataConverterSpawnEgg.eggs[66] = "Witch";
        DataConverterSpawnEgg.eggs[67] = "Endermite";
        DataConverterSpawnEgg.eggs[68] = "Guardian";
        DataConverterSpawnEgg.eggs[69] = "Shulker";
        DataConverterSpawnEgg.eggs[90] = "Pig";
        DataConverterSpawnEgg.eggs[91] = "Sheep";
        DataConverterSpawnEgg.eggs[92] = "Cow";
        DataConverterSpawnEgg.eggs[93] = "Chicken";
        DataConverterSpawnEgg.eggs[94] = "Squid";
        DataConverterSpawnEgg.eggs[95] = "Wolf";
        DataConverterSpawnEgg.eggs[96] = "MushroomCow";
        DataConverterSpawnEgg.eggs[97] = "SnowMan";
        DataConverterSpawnEgg.eggs[98] = "Ozelot";
        DataConverterSpawnEgg.eggs[99] = "VillagerGolem";
        DataConverterSpawnEgg.eggs[100] = "EntityHorse";
        DataConverterSpawnEgg.eggs[101] = "Rabbit";
        DataConverterSpawnEgg.eggs[120] = "Villager";
        DataConverterSpawnEgg.eggs[200] = "EnderCrystal";
    }

}
