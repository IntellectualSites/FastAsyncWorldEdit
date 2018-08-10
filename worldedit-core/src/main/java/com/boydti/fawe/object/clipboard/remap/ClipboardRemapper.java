package com.boydti.fawe.object.clipboard.remap;

import com.boydti.fawe.FaweCache;
import com.google.common.io.Resources;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellectualcrafters.plot.util.MathMan;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.item.ItemTypes;
import com.sk89q.worldedit.world.registry.BundledBlockData;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO FIXME
public class ClipboardRemapper {
    private final RemapPlatform from;

    public enum RemapPlatform {
        PE,
        PC
        ;
        public RemapPlatform opposite() {
            return this == PE ? PC : PE;
        }
    }

    public ClipboardRemapper() {
        this.from = null;
    }

    private WikiScraper scraper;

    public synchronized WikiScraper loadItemMapping() throws IOException {
        WikiScraper tmp = scraper;
        if (tmp == null) {
            scraper = tmp = new WikiScraper();
        }
        return tmp;
    }

    // TODO FIXME
    public BaseItem remapItem(String name, int damage) {
//        if (name.isEmpty()) return new BaseItem(0);
//        if (from == RemapPlatform.PC) {
//            BundledBlockData.BlockEntry state = BundledBlockData.getInstance().findById(name);
//            if (state != null) {
//                BaseBlock remapped = remap(new BaseBlock(state.legacyId, damage));
//                return new BaseItem(remapped.getId(), (short) remapped.getData());
//            } else {
//                try {
//                    name = name.replace("minecraft:", "");
//                    WikiScraper scraper = loadItemMapping();
//                    Map<String, Integer> mapFrom = scraper.scapeOrCache(WikiScraper.Wiki.valueOf("ITEM_MAPPINGS_" + from.name()));
//                    Map<String, Integer> mapTo = scraper.scapeOrCache(WikiScraper.Wiki.valueOf("ITEM_MAPPINGS_" + from.opposite().name()));
//                    scraper.expand(mapTo);
//                    switch (name) {
//                        case "spruce_boat":  return new BaseItem(333, (short) 1);
//                        case "birch_boat":  return new BaseItem(333, (short) 2);
//                        case "jungle_boat":  return new BaseItem(333, (short) 3);
//                        case "acacia_boat":  return new BaseItem(333, (short) 4);
//                        case "dark_oak_boat": return new BaseItem(333, (short) 5);
//                        case "water_bucket": return new BaseItem(325, (short) 8);
//                        case "lava_bucket": return new BaseItem(325, (short) 10);
//                        case "milk_bucket": return new BaseItem(325, (short) 1);
//                        case "tipped_arrow":
//                        case "spectral_arrow":
//                            name = "arrow"; // Unsupported
//                            break;
//                        case "totem_of_undying":
//                            name = "totem";
//                            break;
//                        case "furnace_minecart":
//                            name = "minecart"; // Unsupported
//                            break;
//                    }
//                    Integer itemId = mapTo.get(name);
//                    if (itemId == null) itemId = mapTo.get(name.replace("_", ""));
//                    if (itemId == null) itemId = mapFrom.get(name);
//                    if (itemId != null) return new BaseItem(itemId, (short) damage);
//                } catch (IOException ignore) {
//                    ignore.printStackTrace();
//                }
//            }
//        }
        return new BaseItem(ItemTypes.AIR);
    }

    public Map.Entry<String, Integer> remapItem(int id, int data) {
        throw new UnsupportedOperationException("TODO");
    }

    public int remapEntityId(String id) {
        try {
            Map<String, Integer> mappings = loadItemMapping().scapeOrCache(WikiScraper.Wiki.ENTITY_MAPPINGS);
            id = id.replace("minecraft:", "");
            Integer legacyId = mappings.get(id);
            if (legacyId != null) return legacyId;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public String remapBlockEntityId(String id) {
        if (from == null) return id;
        switch (from) {
            case PE: {
                switch (id) {
                    case "EnchantTable": return "minecraft:enchanting_table";
                    case "Music": return "minecraft:noteblock";
                    case "Chest": return "minecraft:trapped_chest";
                    case "ChalkboardBlock": return "minecraft:chalk_board_block";
                    case "FallingSand": return "minecraft:falling_block";
                    case "FireworksRocketEntity": return "minecraft:fireworks_rocket";
                    case "LavaSlime": return "minecraft:magma_cube";
                    case "MinecartChest": return "minecraft:chest_minecart";
                    case "MinecartCommandBlock": return "minecraft:commandblock_minecart";
                    case "MinecartFurnace": return "minecraft:furnace_minecart";
                    case "MinecartHopper": return "minecraft:hopper_minecart";
                    case "MinecartRideable": return "minecraft:minecart";
                    case "MinecartSpawner": return "minecraft:spawner_minecart";
                    case "MinecartTNT": return "minecraft:tnt_minecart";
                    case "MushroomCow": return "minecraft:mooshroom";
                    case "Ocelot": return "minecraft:ocelot";
                    case "PigZombie": return "minecraft:zombie_pigman";
                    case "PrimedTnt": return "minecraft:tnt";
                    case "SnowMan": return "minecraft:snowman";
                    case "ThrownEgg": return "minecraft:egg";
                    case "ThrownEnderpearl": return "minecraft:ender_pearl";
                    case "ThrownExpBottle": return "minecraft:xp_bottle";
                    case "ThrownPotion": return "minecraft:potion";
                    case "WitherBoss": return "minecraft:wither";
                    case "XPOrb": return "minecraft:xp_orb";
                }
                StringBuilder result = new StringBuilder("minecraft:");
                for (int i = 0; i < result.length(); i++) {
                    char c = id.charAt(i);
                    if (Character.isUpperCase(c)) {
                        c = Character.toLowerCase(c);
                        if (i != 0) result.append('_');
                    }
                    result.append(c);
                }
                id = result.toString();
                return id;
            }
            case PC: {
                switch (id) {
                    case "minecraft:enchanting_table": return "EnchantTable";
                    case "minecraft:noteblock": return "Music";
                    case "minecraft:trapped_chest": return "Chest";
                    case "minecraft:chalk_board_block": return "ChalkboardBlock";
                    case "minecraft:falling_block": return "FallingSand";
                    case "minecraft:fireworks_rocket": return "FireworksRocketEntity";
                    case "minecraft:magma_cube": return "LavaSlime";
                    case "minecraft:chest_minecart": return "MinecartChest";
                    case "minecraft:commandblock_minecart": return "MinecartCommandBlock";
                    case "minecraft:furnace_minecart": return "MinecartFurnace";
                    case "minecraft:hopper_minecart": return "MinecartHopper";
                    case "minecraft:minecart": return "MinecartRideable";
                    case "minecraft:spawner_minecart": return "MinecartSpawner";
                    case "minecraft:tnt_minecart": return "MinecartTNT";
                    case "minecraft:mooshroom": return "MushroomCow";
                    case "minecraft:ocelot": return "Ocelot";
                    case "minecraft:zombie_pigman": return "PigZombie";
                    case "minecraft:tnt": return "PrimedTnt";
                    case "minecraft:snowman": return "SnowMan";
                    case "minecraft:egg": return "ThrownEgg";
                    case "minecraft:ender_pearl": return "ThrownEnderpearl";
                    case "minecraft:xp_bottle": return "ThrownExpBottle";
                    case "minecraft:potion": return "ThrownPotion";
                    case "minecraft:wither": return "WitherBoss";
                    case "minecraft:xp_orb": return "XPOrb";
                }
                id = id.replace("minecraft:", "");
                StringBuilder result = new StringBuilder();
                boolean toUpper = false;
                for (int i = 0; i < id.length(); i++) {
                    char c = id.charAt(i);
                    if (i == 0) toUpper = true;
                    if (c == '_') {
                        toUpper = true;
                    } else {
                        result.append(toUpper ? Character.toUpperCase(c) : c);
                        toUpper = false;
                    }
                }
                id = result.toString();
                return id;
            }
        }
        return id;
    }

    private Map<String, List<Integer>> parse(File file) throws IOException {
        JsonParser parser = new JsonParser();
        JsonObject toParse = (JsonObject) parser.parse(Resources.toString(file.toURL(), Charset.defaultCharset()));
        Map<String, List<Integer>> map = new HashMap<>();

//        outer:
//        for (Map.Entry<String, JsonElement> entry : toParse.entrySet()) {
//            String key = entry.getKey();
//            JsonObject value = entry.getValue().getAsJsonObject();
//            if (MathMan.isInteger(key)) {
//                int id = Integer.parseInt(key);
//                for (Map.Entry<String, JsonElement> dataEntry : value.entrySet()) {
//                    String dataKey = dataEntry.getKey();
//                    if (MathMan.isInteger(dataKey)) {
//                        int data = Integer.parseInt(dataEntry.getKey());
//                        int combined = (id << 4) + data;
//                        String name = dataEntry.getValue().getAsJsonObject().get("intermediateID").getAsString();
//                        map.putIfAbsent(name, new ArrayList<>());
//                        map.get(name).add(combined);
//                    }
//                }
//            } else {
//                String name = entry.getKey();
//                int id = value.get("id").getAsInt();
//                int data = value.get("data").getAsInt();
//                int combined = FaweCache.getCombined(id, data);
//                map.putIfAbsent(name, new ArrayList<>());
//                map.get(name).add(combined);
//            }
//        }
        return map;
    }

    private HashMap<BaseBlock, BaseBlock> getPEtoPCMappings() {
        HashMap<BaseBlock, BaseBlock> mapPEtoPC = new HashMap<>();
        mapPEtoPC.put(new BaseBlock(281, -1), new BaseBlock(25, -1));
        mapPEtoPC.put(new BaseBlock(95,-1), new BaseBlock(166,-1));
        mapPEtoPC.put(new BaseBlock(125,-1), new BaseBlock(158,-1));
        mapPEtoPC.put(new BaseBlock(126,-1), new BaseBlock(157,-1));
        mapPEtoPC.put(new BaseBlock(157,-1), new BaseBlock(125,-1));
        mapPEtoPC.put(new BaseBlock(158,-1), new BaseBlock(126,-1));
        mapPEtoPC.put(new BaseBlock(188,-1), new BaseBlock(210,-1));
        mapPEtoPC.put(new BaseBlock(189,-1), new BaseBlock(211,-1));

        mapPEtoPC.put(new BaseBlock(155,4), new BaseBlock(155,10));
        mapPEtoPC.put(new BaseBlock(155,3), new BaseBlock(155,6));

        mapPEtoPC.put(new BaseBlock(198,-1), new BaseBlock(208,-1));
        mapPEtoPC.put(new BaseBlock(207,-1), new BaseBlock(212,-1));
        { // beetroot
            mapPEtoPC.put(new BaseBlock(244, 2), new BaseBlock(207, 1));
            mapPEtoPC.put(new BaseBlock(244, 4), new BaseBlock(207, 2));
            mapPEtoPC.put(new BaseBlock(244, 7), new BaseBlock(207, 3));
            for (int data = 3; data < 16; data++) mapPEtoPC.putIfAbsent(new BaseBlock(244, data), new BaseBlock(207, data));
        }

        for (int data = 0; data < 16; data++) {
            mapPEtoPC.put(new BaseBlock(218, data), new BaseBlock(219 + data, -1));
        }

        mapPEtoPC.put(new BaseBlock(220,-1), new BaseBlock(235,-1));
        mapPEtoPC.put(new BaseBlock(220,-1), new BaseBlock(235,-1));
        mapPEtoPC.put(new BaseBlock(221,-1), new BaseBlock(236,-1));
        mapPEtoPC.put(new BaseBlock(222,-1), new BaseBlock(237,-1));
        mapPEtoPC.put(new BaseBlock(223,-1), new BaseBlock(238,-1));
        mapPEtoPC.put(new BaseBlock(224,-1), new BaseBlock(239,-1));
        mapPEtoPC.put(new BaseBlock(225,-1), new BaseBlock(240,-1));
        mapPEtoPC.put(new BaseBlock(226,-1), new BaseBlock(241,-1));
        mapPEtoPC.put(new BaseBlock(227,-1), new BaseBlock(242,-1));
        mapPEtoPC.put(new BaseBlock(228,-1), new BaseBlock(243,-1));
        mapPEtoPC.put(new BaseBlock(229,-1), new BaseBlock(244,-1));
        mapPEtoPC.put(new BaseBlock(231,-1), new BaseBlock(246,-1));
        mapPEtoPC.put(new BaseBlock(232,-1), new BaseBlock(247,-1));
        mapPEtoPC.put(new BaseBlock(233,-1), new BaseBlock(248,-1));
        mapPEtoPC.put(new BaseBlock(234,-1), new BaseBlock(249,-1));

        for (int id = 220; id <= 235; id++) {
            int pcId = id + 15;
            int peId = id == 230 ? 219 : id;
            mapPEtoPC.put(new BaseBlock(peId,3), new BaseBlock(pcId,0));
            mapPEtoPC.put(new BaseBlock(peId,4), new BaseBlock(pcId,1));
            mapPEtoPC.put(new BaseBlock(peId,2), new BaseBlock(pcId,2));
            mapPEtoPC.put(new BaseBlock(peId,5), new BaseBlock(pcId,3));
        }

        for (int id : new int[] {29, 33, 34}) {
            mapPEtoPC.put(new BaseBlock(id,3), new BaseBlock(id,2));
            mapPEtoPC.put(new BaseBlock(id,2), new BaseBlock(id,3));
            mapPEtoPC.put(new BaseBlock(id,5), new BaseBlock(id,4));
            mapPEtoPC.put(new BaseBlock(id,4), new BaseBlock(id,5));

//            mapPEtoPC.put(new BaseBlock(id,11), new BaseBlock(id,10));
//            mapPEtoPC.put(new BaseBlock(id,10), new BaseBlock(id,11));
//            mapPEtoPC.put(new BaseBlock(id,13), new BaseBlock(id,12));
//            mapPEtoPC.put(new BaseBlock(id,12), new BaseBlock(id,13));
        }
        mapPEtoPC.put(new BaseBlock(250,-1), new BaseBlock(36,-1));

        mapPEtoPC.put(new BaseBlock(236,-1), new BaseBlock(251,-1));
        mapPEtoPC.put(new BaseBlock(237,-1), new BaseBlock(252,-1));
        mapPEtoPC.put(new BaseBlock(240,-1), new BaseBlock(199,-1));
        mapPEtoPC.put(new BaseBlock(241,-1), new BaseBlock(95,-1));
        mapPEtoPC.put(new BaseBlock(243,0), new BaseBlock(3, 2));

        mapPEtoPC.put(new BaseBlock(251,-1), new BaseBlock(218,-1));

        mapPEtoPC.put(new BaseBlock(168,2), new BaseBlock(168,1));
        mapPEtoPC.put(new BaseBlock(168,1), new BaseBlock(168,2));

        mapPEtoPC.put(new BaseBlock(44,7), new BaseBlock(44,6));
        mapPEtoPC.put(new BaseBlock(44,6), new BaseBlock(44,7));
        // Top variant
        mapPEtoPC.put(new BaseBlock(44,7 + 8), new BaseBlock(44,6 + 8));
        mapPEtoPC.put(new BaseBlock(44,6 + 8), new BaseBlock(44,7 + 8));

        mapPEtoPC.put(new BaseBlock(43,7), new BaseBlock(43,6));
        mapPEtoPC.put(new BaseBlock(43,6), new BaseBlock(43,7));

        mapPEtoPC.put(new BaseBlock(36,-1), new BaseBlock(34, 1));
        mapPEtoPC.put(new BaseBlock(85, 1), new BaseBlock(188,-1));
        mapPEtoPC.put(new BaseBlock(85,2), new BaseBlock(189,-1));
        mapPEtoPC.put(new BaseBlock(85,3), new BaseBlock(190,-1));
        mapPEtoPC.put(new BaseBlock(85,4), new BaseBlock(192,-1));
        mapPEtoPC.put(new BaseBlock(85, 5), new BaseBlock(191,-1));
        mapPEtoPC.put(new BaseBlock(202,-1), new BaseBlock(203,2));

        mapPEtoPC.put(new BaseBlock(201,2), new BaseBlock(202,0));
        mapPEtoPC.put(new BaseBlock(201,10), new BaseBlock(202,8));
        mapPEtoPC.put(new BaseBlock(201,6), new BaseBlock(202,4));
        mapPEtoPC.put(new BaseBlock(181,1), new BaseBlock(204,-1));
        {
            for (int data = 0; data < 16; data++) mapPEtoPC.put(new BaseBlock(208, data), new BaseBlock(198, data));
            mapPEtoPC.put(new BaseBlock(208,4), new BaseBlock(198,5));
            mapPEtoPC.put(new BaseBlock(208,2), new BaseBlock(198,3));
            mapPEtoPC.put(new BaseBlock(208,5), new BaseBlock(198,4));
            mapPEtoPC.put(new BaseBlock(208,3), new BaseBlock(198,2));
        }

        for (int id : new int[] {77, 143}) { // button
            mapPEtoPC.put(new BaseBlock(id,4), new BaseBlock(id,2));
            mapPEtoPC.put(new BaseBlock(id,1), new BaseBlock(id,5));
            mapPEtoPC.put(new BaseBlock(id,2), new BaseBlock(id,4));
            mapPEtoPC.put(new BaseBlock(id,5), new BaseBlock(id,1));

            mapPEtoPC.put(new BaseBlock(id,13), new BaseBlock(id,9));
            mapPEtoPC.put(new BaseBlock(id,12), new BaseBlock(id,10));
            mapPEtoPC.put(new BaseBlock(id,10), new BaseBlock(id,12));
            mapPEtoPC.put(new BaseBlock(id,9), new BaseBlock(id,13));
        }

        // leaves
        for (int data = 4; data < 8; data++) mapPEtoPC.put(new BaseBlock(18,data + 4), new BaseBlock(18,data));
        for (int data = 4; data < 8; data++)  mapPEtoPC.put(new BaseBlock(18,data + 8), new BaseBlock(161,data));

        for (int id : new int[] {96, 167}) { // trapdoor
            mapPEtoPC.put(new BaseBlock(id, 2), new BaseBlock(id, 0));
            mapPEtoPC.put(new BaseBlock(id, 3), new BaseBlock(id, 1));
            mapPEtoPC.put(new BaseBlock(id, 6), new BaseBlock(id, 8));
            mapPEtoPC.put(new BaseBlock(id, 7), new BaseBlock(id, 9));
            for (int data = 0; data < 4; data++) mapPEtoPC.putIfAbsent(new BaseBlock(id, data), new BaseBlock(id, 3 - data));
            for (int data = 4; data < 12; data++) mapPEtoPC.putIfAbsent(new BaseBlock(id, data), new BaseBlock(id, 15 - data));
            for (int data = 12; data < 16; data++) mapPEtoPC.putIfAbsent(new BaseBlock(id, data), new BaseBlock(id, 27 - data));
        }

        return mapPEtoPC;
    }

    public ClipboardRemapper(RemapPlatform fromPlatform, RemapPlatform toPlatform) {
        if (fromPlatform == toPlatform) {
            this.from = null;
            return;
        }

        HashMap<BaseBlock, BaseBlock> mapPEtoPC = getPEtoPCMappings();


        for (Map.Entry<BaseBlock, BaseBlock> entry : mapPEtoPC.entrySet()) {
            BaseBlock from = entry.getKey();
            BaseBlock to = entry.getValue();
            if (fromPlatform == RemapPlatform.PE) {
                add(from, to);
            } else {
                add(to, from);
            }
        }

        // TODO any custom ids
        switch (fromPlatform) {
            case PE:
                for (int data = 0; data < 8; data++) add(new BaseBlock(182, 1), new BaseBlock(205, data));
                for (int data = 8; data < 16; data++) add(new BaseBlock(182, 9), new BaseBlock(205, data));
                for (int data = 0; data < 8; data++) add(new BaseBlock(182, 0), new BaseBlock(182, data));
                for (int data = 8; data < 16; data++) add(new BaseBlock(182, 8), new BaseBlock(182, data));
                break;
            case PC:
                add(new BaseBlock(202, -1), new BaseBlock(201, -1));
                add(new BaseBlock(204, -1), new BaseBlock(201, -1));
                for (int data = 0; data < 8; data++) add(new BaseBlock(205, data), new BaseBlock(182, 1));
                for (int data = 8; data < 16; data++) add(new BaseBlock(205, data), new BaseBlock(182, 9));
                for (int data = 0; data < 8; data++) add(new BaseBlock(182, data), new BaseBlock(182, 0));
                for (int data = 8; data < 16; data++) add(new BaseBlock(182, data), new BaseBlock(182, 8));

                for (int id : new int[] {29, 33, 34}) {
                    for (int data = 8; data < 16; data++) {
                        add(new BaseBlock(id, data), remap(new BaseBlock(id, data - 8)));
                    }
                }

                break;
        }

        this.from = fromPlatform;
    }

    public void addBoth(BaseBlock from, BaseBlock to) {
        add(from, to);
        add(to, from);
    }

    public void apply(Clipboard clipboard) throws WorldEditException {
//        if (clipboard instanceof BlockArrayClipboard) {
//            BlockArrayClipboard bac = (BlockArrayClipboard) clipboard;
//            bac.IMP = new RemappedClipboard(bac.IMP, this);
//        } else {
//            Region region = clipboard.getRegion();
//            for (BlockVector pos : region) {
//                BaseBlock block = clipboard.getBlock(pos);
//                BaseBlock newBlock = remap(block);
//                if (block != newBlock) {
//                    clipboard.setBlock(pos, newBlock);
//                }
//            }
//        }
    }

    private char[] remapCombined = new char[Character.MAX_VALUE + 1];
    private boolean[] remap = new boolean[Character.MAX_VALUE + 1];
    private boolean[] remapIds = new boolean[4096];
    private boolean[] remapAllIds = new boolean[4096];
    private boolean[] remapAnyIds = new boolean[4096];
    private boolean[] remapData = new boolean[16];

    public boolean hasRemapData(int data) {
        return remapData[data];
    }

    public boolean hasRemapId(int id) {
        return remapAnyIds[id];
    }

    public boolean hasRemap(int id) {
        return remapIds[id];
    }

    public int remapId(int id) {
        if (remapAllIds[id]) {
            return remapCombined[id << 4] >> 4;
        }
        return id;
    }

    public void add(BlockStateHolder from, BlockStateHolder to) {
//        if (from.getData() != to.getData()) {
//            if (from.getData() == -1) {
//                Arrays.fill(remapData, true);
//            } else {
//                remapData[from.getData()] = true;
//            }
//        }
//        if (from.getData() == -1) {
//            for (int data = 0; data < 16; data++) {
//                int combinedFrom = (from.getId() << 4) + data;
//                int combinedTo = to.getData() == -1 ? (to.getId() << 4) + data : to.getCombined();
//                remap[combinedFrom] = true;
//                remapCombined[combinedFrom] = (char) combinedTo;
//                remapIds[combinedFrom >> 4] = true;
//                if (from.getId() != to.getId()) {
//                    remapAnyIds[combinedFrom >> 4] = true;
//                    remapAllIds[from.getId()] = true;
//                }
//            }
//        } else {
//            int data = from.getData();
//            int combinedFrom = (from.getId() << 4) + data;
//            int combinedTo = to.getData() == -1 ? (to.getId() << 4) + data : to.getCombined();
//            remap[combinedFrom] = true;
//            remapCombined[combinedFrom] = (char) combinedTo;
//            remapIds[combinedFrom >> 4] = true;
//            if (from.getId() != to.getId()) {
//                remapAnyIds[combinedFrom >> 4] = true;
//                remapAllIds[from.getId()] = false;
//            }
//        }
    }

    public BlockStateHolder remap(BlockStateHolder block) {
//        int combined = block.getCombined();
//        if (remap[combined]) {
//            char value = remapCombined[combined];
//            BaseBlock newBlock = FaweCache.CACHE_BLOCK[value];
//            newBlock.setNbtData(block.getNbtData());
//            return newBlock;
//        }
        return block;
    }

    public void remap(CompoundTag tag) {

    }
}
