package com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter;

public class DataConverterPotionId implements DataConverter {

    private static final String[] potions = new String[128];

    public DataConverterPotionId() {
    }

    public int getDataVersion() {
        return 102;
    }

    public net.minecraft.nbt.CompoundTag convert(net.minecraft.nbt.CompoundTag cmp) {
        if ("minecraft:potion".equals(cmp.getString("id"))) {
            net.minecraft.nbt.CompoundTag nbttagcompound1 = cmp.getCompound("tag");
            short short0 = cmp.getShort("Damage");

            if (!nbttagcompound1.contains("Potion", 8)) {
                String s = DataConverterPotionId.potions[short0 & 127];

                nbttagcompound1.putString("Potion", s == null ? "minecraft:water" : s);
                cmp.put("tag", nbttagcompound1);
                if ((short0 & 16384) == 16384) {
                    cmp.putString("id", "minecraft:splash_potion");
                }
            }

            if (short0 != 0) {
                cmp.putShort("Damage", (short) 0);
            }
        }

        return cmp;
    }

    static {
        DataConverterPotionId.potions[0] = "minecraft:water";
        DataConverterPotionId.potions[1] = "minecraft:regeneration";
        DataConverterPotionId.potions[2] = "minecraft:swiftness";
        DataConverterPotionId.potions[3] = "minecraft:fire_resistance";
        DataConverterPotionId.potions[4] = "minecraft:poison";
        DataConverterPotionId.potions[5] = "minecraft:healing";
        DataConverterPotionId.potions[6] = "minecraft:night_vision";
        DataConverterPotionId.potions[7] = null;
        DataConverterPotionId.potions[8] = "minecraft:weakness";
        DataConverterPotionId.potions[9] = "minecraft:strength";
        DataConverterPotionId.potions[10] = "minecraft:slowness";
        DataConverterPotionId.potions[11] = "minecraft:leaping";
        DataConverterPotionId.potions[12] = "minecraft:harming";
        DataConverterPotionId.potions[13] = "minecraft:water_breathing";
        DataConverterPotionId.potions[14] = "minecraft:invisibility";
        DataConverterPotionId.potions[15] = null;
        DataConverterPotionId.potions[16] = "minecraft:awkward";
        DataConverterPotionId.potions[17] = "minecraft:regeneration";
        DataConverterPotionId.potions[18] = "minecraft:swiftness";
        DataConverterPotionId.potions[19] = "minecraft:fire_resistance";
        DataConverterPotionId.potions[20] = "minecraft:poison";
        DataConverterPotionId.potions[21] = "minecraft:healing";
        DataConverterPotionId.potions[22] = "minecraft:night_vision";
        DataConverterPotionId.potions[23] = null;
        DataConverterPotionId.potions[24] = "minecraft:weakness";
        DataConverterPotionId.potions[25] = "minecraft:strength";
        DataConverterPotionId.potions[26] = "minecraft:slowness";
        DataConverterPotionId.potions[27] = "minecraft:leaping";
        DataConverterPotionId.potions[28] = "minecraft:harming";
        DataConverterPotionId.potions[29] = "minecraft:water_breathing";
        DataConverterPotionId.potions[30] = "minecraft:invisibility";
        DataConverterPotionId.potions[31] = null;
        DataConverterPotionId.potions[32] = "minecraft:thick";
        DataConverterPotionId.potions[33] = "minecraft:strong_regeneration";
        DataConverterPotionId.potions[34] = "minecraft:strong_swiftness";
        DataConverterPotionId.potions[35] = "minecraft:fire_resistance";
        DataConverterPotionId.potions[36] = "minecraft:strong_poison";
        DataConverterPotionId.potions[37] = "minecraft:strong_healing";
        DataConverterPotionId.potions[38] = "minecraft:night_vision";
        DataConverterPotionId.potions[39] = null;
        DataConverterPotionId.potions[40] = "minecraft:weakness";
        DataConverterPotionId.potions[41] = "minecraft:strong_strength";
        DataConverterPotionId.potions[42] = "minecraft:slowness";
        DataConverterPotionId.potions[43] = "minecraft:strong_leaping";
        DataConverterPotionId.potions[44] = "minecraft:strong_harming";
        DataConverterPotionId.potions[45] = "minecraft:water_breathing";
        DataConverterPotionId.potions[46] = "minecraft:invisibility";
        DataConverterPotionId.potions[47] = null;
        DataConverterPotionId.potions[48] = null;
        DataConverterPotionId.potions[49] = "minecraft:strong_regeneration";
        DataConverterPotionId.potions[50] = "minecraft:strong_swiftness";
        DataConverterPotionId.potions[51] = "minecraft:fire_resistance";
        DataConverterPotionId.potions[52] = "minecraft:strong_poison";
        DataConverterPotionId.potions[53] = "minecraft:strong_healing";
        DataConverterPotionId.potions[54] = "minecraft:night_vision";
        DataConverterPotionId.potions[55] = null;
        DataConverterPotionId.potions[56] = "minecraft:weakness";
        DataConverterPotionId.potions[57] = "minecraft:strong_strength";
        DataConverterPotionId.potions[58] = "minecraft:slowness";
        DataConverterPotionId.potions[59] = "minecraft:strong_leaping";
        DataConverterPotionId.potions[60] = "minecraft:strong_harming";
        DataConverterPotionId.potions[61] = "minecraft:water_breathing";
        DataConverterPotionId.potions[62] = "minecraft:invisibility";
        DataConverterPotionId.potions[63] = null;
        DataConverterPotionId.potions[64] = "minecraft:mundane";
        DataConverterPotionId.potions[65] = "minecraft:long_regeneration";
        DataConverterPotionId.potions[66] = "minecraft:long_swiftness";
        DataConverterPotionId.potions[67] = "minecraft:long_fire_resistance";
        DataConverterPotionId.potions[68] = "minecraft:long_poison";
        DataConverterPotionId.potions[69] = "minecraft:healing";
        DataConverterPotionId.potions[70] = "minecraft:long_night_vision";
        DataConverterPotionId.potions[71] = null;
        DataConverterPotionId.potions[72] = "minecraft:long_weakness";
        DataConverterPotionId.potions[73] = "minecraft:long_strength";
        DataConverterPotionId.potions[74] = "minecraft:long_slowness";
        DataConverterPotionId.potions[75] = "minecraft:long_leaping";
        DataConverterPotionId.potions[76] = "minecraft:harming";
        DataConverterPotionId.potions[77] = "minecraft:long_water_breathing";
        DataConverterPotionId.potions[78] = "minecraft:long_invisibility";
        DataConverterPotionId.potions[79] = null;
        DataConverterPotionId.potions[80] = "minecraft:awkward";
        DataConverterPotionId.potions[81] = "minecraft:long_regeneration";
        DataConverterPotionId.potions[82] = "minecraft:long_swiftness";
        DataConverterPotionId.potions[83] = "minecraft:long_fire_resistance";
        DataConverterPotionId.potions[84] = "minecraft:long_poison";
        DataConverterPotionId.potions[85] = "minecraft:healing";
        DataConverterPotionId.potions[86] = "minecraft:long_night_vision";
        DataConverterPotionId.potions[87] = null;
        DataConverterPotionId.potions[88] = "minecraft:long_weakness";
        DataConverterPotionId.potions[89] = "minecraft:long_strength";
        DataConverterPotionId.potions[90] = "minecraft:long_slowness";
        DataConverterPotionId.potions[91] = "minecraft:long_leaping";
        DataConverterPotionId.potions[92] = "minecraft:harming";
        DataConverterPotionId.potions[93] = "minecraft:long_water_breathing";
        DataConverterPotionId.potions[94] = "minecraft:long_invisibility";
        DataConverterPotionId.potions[95] = null;
        DataConverterPotionId.potions[96] = "minecraft:thick";
        DataConverterPotionId.potions[97] = "minecraft:regeneration";
        DataConverterPotionId.potions[98] = "minecraft:swiftness";
        DataConverterPotionId.potions[99] = "minecraft:long_fire_resistance";
        DataConverterPotionId.potions[100] = "minecraft:poison";
        DataConverterPotionId.potions[101] = "minecraft:strong_healing";
        DataConverterPotionId.potions[102] = "minecraft:long_night_vision";
        DataConverterPotionId.potions[103] = null;
        DataConverterPotionId.potions[104] = "minecraft:long_weakness";
        DataConverterPotionId.potions[105] = "minecraft:strength";
        DataConverterPotionId.potions[106] = "minecraft:long_slowness";
        DataConverterPotionId.potions[107] = "minecraft:leaping";
        DataConverterPotionId.potions[108] = "minecraft:strong_harming";
        DataConverterPotionId.potions[109] = "minecraft:long_water_breathing";
        DataConverterPotionId.potions[110] = "minecraft:long_invisibility";
        DataConverterPotionId.potions[111] = null;
        DataConverterPotionId.potions[112] = null;
        DataConverterPotionId.potions[113] = "minecraft:regeneration";
        DataConverterPotionId.potions[114] = "minecraft:swiftness";
        DataConverterPotionId.potions[115] = "minecraft:long_fire_resistance";
        DataConverterPotionId.potions[116] = "minecraft:poison";
        DataConverterPotionId.potions[117] = "minecraft:strong_healing";
        DataConverterPotionId.potions[118] = "minecraft:long_night_vision";
        DataConverterPotionId.potions[119] = null;
        DataConverterPotionId.potions[120] = "minecraft:long_weakness";
        DataConverterPotionId.potions[121] = "minecraft:strength";
        DataConverterPotionId.potions[122] = "minecraft:long_slowness";
        DataConverterPotionId.potions[123] = "minecraft:leaping";
        DataConverterPotionId.potions[124] = "minecraft:strong_harming";
        DataConverterPotionId.potions[125] = "minecraft:long_water_breathing";
        DataConverterPotionId.potions[126] = "minecraft:long_invisibility";
        DataConverterPotionId.potions[127] = null;
    }

}
