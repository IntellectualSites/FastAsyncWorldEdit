package com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter;

public class DataConverterMaterialId implements DataConverter {

    private static final String[] materials = new String[2268];

    public DataConverterMaterialId() {
    }

    public int getDataVersion() {
        return 102;
    }

    public net.minecraft.nbt.CompoundTag convert(net.minecraft.nbt.CompoundTag cmp) {
        if (cmp.contains("id", 99)) {
            short short0 = cmp.getShort("id");

            if (short0 > 0 && short0 < materials.length && materials[short0] != null) {
                cmp.putString("id", materials[short0]);
            }
        }

        return cmp;
    }

    static {
        materials[1] = "minecraft:stone";
        materials[2] = "minecraft:grass";
        materials[3] = "minecraft:dirt";
        materials[4] = "minecraft:cobblestone";
        materials[5] = "minecraft:planks";
        materials[6] = "minecraft:sapling";
        materials[7] = "minecraft:bedrock";
        materials[8] = "minecraft:flowing_water";
        materials[9] = "minecraft:water";
        materials[10] = "minecraft:flowing_lava";
        materials[11] = "minecraft:lava";
        materials[12] = "minecraft:sand";
        materials[13] = "minecraft:gravel";
        materials[14] = "minecraft:gold_ore";
        materials[15] = "minecraft:iron_ore";
        materials[16] = "minecraft:coal_ore";
        materials[17] = "minecraft:log";
        materials[18] = "minecraft:leaves";
        materials[19] = "minecraft:sponge";
        materials[20] = "minecraft:glass";
        materials[21] = "minecraft:lapis_ore";
        materials[22] = "minecraft:lapis_block";
        materials[23] = "minecraft:dispenser";
        materials[24] = "minecraft:sandstone";
        materials[25] = "minecraft:noteblock";
        materials[27] = "minecraft:golden_rail";
        materials[28] = "minecraft:detector_rail";
        materials[29] = "minecraft:sticky_piston";
        materials[30] = "minecraft:web";
        materials[31] = "minecraft:tallgrass";
        materials[32] = "minecraft:deadbush";
        materials[33] = "minecraft:piston";
        materials[35] = "minecraft:wool";
        materials[37] = "minecraft:yellow_flower";
        materials[38] = "minecraft:red_flower";
        materials[39] = "minecraft:brown_mushroom";
        materials[40] = "minecraft:red_mushroom";
        materials[41] = "minecraft:gold_block";
        materials[42] = "minecraft:iron_block";
        materials[43] = "minecraft:double_stone_slab";
        materials[44] = "minecraft:stone_slab";
        materials[45] = "minecraft:brick_block";
        materials[46] = "minecraft:tnt";
        materials[47] = "minecraft:bookshelf";
        materials[48] = "minecraft:mossy_cobblestone";
        materials[49] = "minecraft:obsidian";
        materials[50] = "minecraft:torch";
        materials[51] = "minecraft:fire";
        materials[52] = "minecraft:mob_spawner";
        materials[53] = "minecraft:oak_stairs";
        materials[54] = "minecraft:chest";
        materials[56] = "minecraft:diamond_ore";
        materials[57] = "minecraft:diamond_block";
        materials[58] = "minecraft:crafting_table";
        materials[60] = "minecraft:farmland";
        materials[61] = "minecraft:furnace";
        materials[62] = "minecraft:lit_furnace";
        materials[65] = "minecraft:ladder";
        materials[66] = "minecraft:rail";
        materials[67] = "minecraft:stone_stairs";
        materials[69] = "minecraft:lever";
        materials[70] = "minecraft:stone_pressure_plate";
        materials[72] = "minecraft:wooden_pressure_plate";
        materials[73] = "minecraft:redstone_ore";
        materials[76] = "minecraft:redstone_torch";
        materials[77] = "minecraft:stone_button";
        materials[78] = "minecraft:snow_layer";
        materials[79] = "minecraft:ice";
        materials[80] = "minecraft:snow";
        materials[81] = "minecraft:cactus";
        materials[82] = "minecraft:clay";
        materials[84] = "minecraft:jukebox";
        materials[85] = "minecraft:fence";
        materials[86] = "minecraft:pumpkin";
        materials[87] = "minecraft:netherrack";
        materials[88] = "minecraft:soul_sand";
        materials[89] = "minecraft:glowstone";
        materials[90] = "minecraft:portal";
        materials[91] = "minecraft:lit_pumpkin";
        materials[95] = "minecraft:stained_glass";
        materials[96] = "minecraft:trapdoor";
        materials[97] = "minecraft:monster_egg";
        materials[98] = "minecraft:stonebrick";
        materials[99] = "minecraft:brown_mushroom_block";
        materials[100] = "minecraft:red_mushroom_block";
        materials[101] = "minecraft:iron_bars";
        materials[102] = "minecraft:glass_pane";
        materials[103] = "minecraft:melon_block";
        materials[106] = "minecraft:vine";
        materials[107] = "minecraft:fence_gate";
        materials[108] = "minecraft:brick_stairs";
        materials[109] = "minecraft:stone_brick_stairs";
        materials[110] = "minecraft:mycelium";
        materials[111] = "minecraft:waterlily";
        materials[112] = "minecraft:nether_brick";
        materials[113] = "minecraft:nether_brick_fence";
        materials[114] = "minecraft:nether_brick_stairs";
        materials[116] = "minecraft:enchanting_table";
        materials[119] = "minecraft:end_portal";
        materials[120] = "minecraft:end_portal_frame";
        materials[121] = "minecraft:end_stone";
        materials[122] = "minecraft:dragon_egg";
        materials[123] = "minecraft:redstone_lamp";
        materials[125] = "minecraft:double_wooden_slab";
        materials[126] = "minecraft:wooden_slab";
        materials[127] = "minecraft:cocoa";
        materials[128] = "minecraft:sandstone_stairs";
        materials[129] = "minecraft:emerald_ore";
        materials[130] = "minecraft:ender_chest";
        materials[131] = "minecraft:tripwire_hook";
        materials[133] = "minecraft:emerald_block";
        materials[134] = "minecraft:spruce_stairs";
        materials[135] = "minecraft:birch_stairs";
        materials[136] = "minecraft:jungle_stairs";
        materials[137] = "minecraft:command_block";
        materials[138] = "minecraft:beacon";
        materials[139] = "minecraft:cobblestone_wall";
        materials[141] = "minecraft:carrots";
        materials[142] = "minecraft:potatoes";
        materials[143] = "minecraft:wooden_button";
        materials[145] = "minecraft:anvil";
        materials[146] = "minecraft:trapped_chest";
        materials[147] = "minecraft:light_weighted_pressure_plate";
        materials[148] = "minecraft:heavy_weighted_pressure_plate";
        materials[151] = "minecraft:daylight_detector";
        materials[152] = "minecraft:redstone_block";
        materials[153] = "minecraft:quartz_ore";
        materials[154] = "minecraft:hopper";
        materials[155] = "minecraft:quartz_block";
        materials[156] = "minecraft:quartz_stairs";
        materials[157] = "minecraft:activator_rail";
        materials[158] = "minecraft:dropper";
        materials[159] = "minecraft:stained_hardened_clay";
        materials[160] = "minecraft:stained_glass_pane";
        materials[161] = "minecraft:leaves2";
        materials[162] = "minecraft:log2";
        materials[163] = "minecraft:acacia_stairs";
        materials[164] = "minecraft:dark_oak_stairs";
        materials[170] = "minecraft:hay_block";
        materials[171] = "minecraft:carpet";
        materials[172] = "minecraft:hardened_clay";
        materials[173] = "minecraft:coal_block";
        materials[174] = "minecraft:packed_ice";
        materials[175] = "minecraft:double_plant";
        materials[256] = "minecraft:iron_shovel";
        materials[257] = "minecraft:iron_pickaxe";
        materials[258] = "minecraft:iron_axe";
        materials[259] = "minecraft:flint_and_steel";
        materials[260] = "minecraft:apple";
        materials[261] = "minecraft:bow";
        materials[262] = "minecraft:arrow";
        materials[263] = "minecraft:coal";
        materials[264] = "minecraft:diamond";
        materials[265] = "minecraft:iron_ingot";
        materials[266] = "minecraft:gold_ingot";
        materials[267] = "minecraft:iron_sword";
        materials[268] = "minecraft:wooden_sword";
        materials[269] = "minecraft:wooden_shovel";
        materials[270] = "minecraft:wooden_pickaxe";
        materials[271] = "minecraft:wooden_axe";
        materials[272] = "minecraft:stone_sword";
        materials[273] = "minecraft:stone_shovel";
        materials[274] = "minecraft:stone_pickaxe";
        materials[275] = "minecraft:stone_axe";
        materials[276] = "minecraft:diamond_sword";
        materials[277] = "minecraft:diamond_shovel";
        materials[278] = "minecraft:diamond_pickaxe";
        materials[279] = "minecraft:diamond_axe";
        materials[280] = "minecraft:stick";
        materials[281] = "minecraft:bowl";
        materials[282] = "minecraft:mushroom_stew";
        materials[283] = "minecraft:golden_sword";
        materials[284] = "minecraft:golden_shovel";
        materials[285] = "minecraft:golden_pickaxe";
        materials[286] = "minecraft:golden_axe";
        materials[287] = "minecraft:string";
        materials[288] = "minecraft:feather";
        materials[289] = "minecraft:gunpowder";
        materials[290] = "minecraft:wooden_hoe";
        materials[291] = "minecraft:stone_hoe";
        materials[292] = "minecraft:iron_hoe";
        materials[293] = "minecraft:diamond_hoe";
        materials[294] = "minecraft:golden_hoe";
        materials[295] = "minecraft:wheat_seeds";
        materials[296] = "minecraft:wheat";
        materials[297] = "minecraft:bread";
        materials[298] = "minecraft:leather_helmet";
        materials[299] = "minecraft:leather_chestplate";
        materials[300] = "minecraft:leather_leggings";
        materials[301] = "minecraft:leather_boots";
        materials[302] = "minecraft:chainmail_helmet";
        materials[303] = "minecraft:chainmail_chestplate";
        materials[304] = "minecraft:chainmail_leggings";
        materials[305] = "minecraft:chainmail_boots";
        materials[306] = "minecraft:iron_helmet";
        materials[307] = "minecraft:iron_chestplate";
        materials[308] = "minecraft:iron_leggings";
        materials[309] = "minecraft:iron_boots";
        materials[310] = "minecraft:diamond_helmet";
        materials[311] = "minecraft:diamond_chestplate";
        materials[312] = "minecraft:diamond_leggings";
        materials[313] = "minecraft:diamond_boots";
        materials[314] = "minecraft:golden_helmet";
        materials[315] = "minecraft:golden_chestplate";
        materials[316] = "minecraft:golden_leggings";
        materials[317] = "minecraft:golden_boots";
        materials[318] = "minecraft:flint";
        materials[319] = "minecraft:porkchop";
        materials[320] = "minecraft:cooked_porkchop";
        materials[321] = "minecraft:painting";
        materials[322] = "minecraft:golden_apple";
        materials[323] = "minecraft:sign";
        materials[324] = "minecraft:wooden_door";
        materials[325] = "minecraft:bucket";
        materials[326] = "minecraft:water_bucket";
        materials[327] = "minecraft:lava_bucket";
        materials[328] = "minecraft:minecart";
        materials[329] = "minecraft:saddle";
        materials[330] = "minecraft:iron_door";
        materials[331] = "minecraft:redstone";
        materials[332] = "minecraft:snowball";
        materials[333] = "minecraft:boat";
        materials[334] = "minecraft:leather";
        materials[335] = "minecraft:milk_bucket";
        materials[336] = "minecraft:brick";
        materials[337] = "minecraft:clay_ball";
        materials[338] = "minecraft:reeds";
        materials[339] = "minecraft:paper";
        materials[340] = "minecraft:book";
        materials[341] = "minecraft:slime_ball";
        materials[342] = "minecraft:chest_minecart";
        materials[343] = "minecraft:furnace_minecart";
        materials[344] = "minecraft:egg";
        materials[345] = "minecraft:compass";
        materials[346] = "minecraft:fishing_rod";
        materials[347] = "minecraft:clock";
        materials[348] = "minecraft:glowstone_dust";
        materials[349] = "minecraft:fish";
        materials[350] = "minecraft:cooked_fish"; // Paper - cooked_fished -> cooked_fish
        materials[351] = "minecraft:dye";
        materials[352] = "minecraft:bone";
        materials[353] = "minecraft:sugar";
        materials[354] = "minecraft:cake";
        materials[355] = "minecraft:bed";
        materials[356] = "minecraft:repeater";
        materials[357] = "minecraft:cookie";
        materials[358] = "minecraft:filled_map";
        materials[359] = "minecraft:shears";
        materials[360] = "minecraft:melon";
        materials[361] = "minecraft:pumpkin_seeds";
        materials[362] = "minecraft:melon_seeds";
        materials[363] = "minecraft:beef";
        materials[364] = "minecraft:cooked_beef";
        materials[365] = "minecraft:chicken";
        materials[366] = "minecraft:cooked_chicken";
        materials[367] = "minecraft:rotten_flesh";
        materials[368] = "minecraft:ender_pearl";
        materials[369] = "minecraft:blaze_rod";
        materials[370] = "minecraft:ghast_tear";
        materials[371] = "minecraft:gold_nugget";
        materials[372] = "minecraft:nether_wart";
        materials[373] = "minecraft:potion";
        materials[374] = "minecraft:glass_bottle";
        materials[375] = "minecraft:spider_eye";
        materials[376] = "minecraft:fermented_spider_eye";
        materials[377] = "minecraft:blaze_powder";
        materials[378] = "minecraft:magma_cream";
        materials[379] = "minecraft:brewing_stand";
        materials[380] = "minecraft:cauldron";
        materials[381] = "minecraft:ender_eye";
        materials[382] = "minecraft:speckled_melon";
        materials[383] = "minecraft:spawn_egg";
        materials[384] = "minecraft:experience_bottle";
        materials[385] = "minecraft:fire_charge";
        materials[386] = "minecraft:writable_book";
        materials[387] = "minecraft:written_book";
        materials[388] = "minecraft:emerald";
        materials[389] = "minecraft:item_frame";
        materials[390] = "minecraft:flower_pot";
        materials[391] = "minecraft:carrot";
        materials[392] = "minecraft:potato";
        materials[393] = "minecraft:baked_potato";
        materials[394] = "minecraft:poisonous_potato";
        materials[395] = "minecraft:map";
        materials[396] = "minecraft:golden_carrot";
        materials[397] = "minecraft:skull";
        materials[398] = "minecraft:carrot_on_a_stick";
        materials[399] = "minecraft:nether_star";
        materials[400] = "minecraft:pumpkin_pie";
        materials[401] = "minecraft:fireworks";
        materials[402] = "minecraft:firework_charge";
        materials[403] = "minecraft:enchanted_book";
        materials[404] = "minecraft:comparator";
        materials[405] = "minecraft:netherbrick";
        materials[406] = "minecraft:quartz";
        materials[407] = "minecraft:tnt_minecart";
        materials[408] = "minecraft:hopper_minecart";
        materials[417] = "minecraft:iron_horse_armor";
        materials[418] = "minecraft:golden_horse_armor";
        materials[419] = "minecraft:diamond_horse_armor";
        materials[420] = "minecraft:lead";
        materials[421] = "minecraft:name_tag";
        materials[422] = "minecraft:command_block_minecart";
        materials[2256] = "minecraft:record_13";
        materials[2257] = "minecraft:record_cat";
        materials[2258] = "minecraft:record_blocks";
        materials[2259] = "minecraft:record_chirp";
        materials[2260] = "minecraft:record_far";
        materials[2261] = "minecraft:record_mall";
        materials[2262] = "minecraft:record_mellohi";
        materials[2263] = "minecraft:record_stal";
        materials[2264] = "minecraft:record_strad";
        materials[2265] = "minecraft:record_ward";
        materials[2266] = "minecraft:record_11";
        materials[2267] = "minecraft:record_wait";
        // Paper start
        materials[409] = "minecraft:prismarine_shard";
        materials[410] = "minecraft:prismarine_crystals";
        materials[411] = "minecraft:rabbit";
        materials[412] = "minecraft:cooked_rabbit";
        materials[413] = "minecraft:rabbit_stew";
        materials[414] = "minecraft:rabbit_foot";
        materials[415] = "minecraft:rabbit_hide";
        materials[416] = "minecraft:armor_stand";
        materials[423] = "minecraft:mutton";
        materials[424] = "minecraft:cooked_mutton";
        materials[425] = "minecraft:banner";
        materials[426] = "minecraft:end_crystal";
        materials[427] = "minecraft:spruce_door";
        materials[428] = "minecraft:birch_door";
        materials[429] = "minecraft:jungle_door";
        materials[430] = "minecraft:acacia_door";
        materials[431] = "minecraft:dark_oak_door";
        materials[432] = "minecraft:chorus_fruit";
        materials[433] = "minecraft:chorus_fruit_popped";
        materials[434] = "minecraft:beetroot";
        materials[435] = "minecraft:beetroot_seeds";
        materials[436] = "minecraft:beetroot_soup";
        materials[437] = "minecraft:dragon_breath";
        materials[438] = "minecraft:splash_potion";
        materials[439] = "minecraft:spectral_arrow";
        materials[440] = "minecraft:tipped_arrow";
        materials[441] = "minecraft:lingering_potion";
        materials[442] = "minecraft:shield";
        materials[443] = "minecraft:elytra";
        materials[444] = "minecraft:spruce_boat";
        materials[445] = "minecraft:birch_boat";
        materials[446] = "minecraft:jungle_boat";
        materials[447] = "minecraft:acacia_boat";
        materials[448] = "minecraft:dark_oak_boat";
        materials[449] = "minecraft:totem_of_undying";
        materials[450] = "minecraft:shulker_shell";
        materials[452] = "minecraft:iron_nugget";
        materials[453] = "minecraft:knowledge_book";
        // Paper end
    }

}
