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

package com.sk89q.worldedit.bukkit.adapter.ext.fawe;

import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.DataFixerBuilder;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.DataConverter;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.DataConverterArmorStand;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.DataConverterBanner;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.DataConverterBedBlock;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.DataConverterBedItem;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.DataConverterBook;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.DataConverterCookedFish;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.DataConverterDropChances;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.DataConverterEntity;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.DataConverterEquipment;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.DataConverterGuardian;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.DataConverterHanging;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.DataConverterHealth;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.DataConverterHorse;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.DataConverterLang;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.DataConverterMaterialId;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.DataConverterMinecart;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.DataConverterMobSpawner;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.DataConverterPotionId;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.DataConverterPotionWater;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.DataConverterRiding;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.DataConverterSaddle;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.DataConverterShulker;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.DataConverterShulkerBoxBlock;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.DataConverterShulkerBoxItem;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.DataConverterSignText;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.DataConverterSkeleton;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.DataConverterSpawnEgg;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.DataConverterTileEntity;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.DataConverterTotem;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.DataConverterUUID;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.DataConverterVBO;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.DataConverterZombie;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.DataConverterZombieType;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.inspector.DataInspector;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.inspector.DataInspectorBlockEntity;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.inspector.DataInspectorChunks;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.inspector.DataInspectorCommandBlock;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.inspector.DataInspectorEntity;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.inspector.DataInspectorEntityPassengers;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.inspector.DataInspectorItem;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.inspector.DataInspectorItemList;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.inspector.DataInspectorLevelPlayer;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.inspector.DataInspectorMobSpawnerMinecart;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.inspector.DataInspectorMobSpawnerMobs;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.inspector.DataInspectorPlayer;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.inspector.DataInspectorPlayerVehicle;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.inspector.DataInspectorStructure;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.inspector.DataInspectorVillagers;
import com.sk89q.worldedit.util.nbt.CompoundBinaryTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Handles converting all Pre 1.13.2 data using the Legacy DataFix System (ported to 1.13.2)
 * <p>
 * We register a DFU Fixer per Legacy Data Version and apply the fixes using legacy strategy
 * which is safer, faster and cleaner code.
 * <p>
 * The pre DFU code did not fail when the Source version was unknown.
 * <p>
 * This class also provides util methods for converting compounds to wrap the update call to
 * receive the source version in the compound
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class PaperweightDataConverters extends DataFixerBuilder implements com.sk89q.worldedit.world.DataFixer {

    //FAWE start - BinaryTag
    @SuppressWarnings("unchecked")
    @Override
    public <T> T fixUp(FixType<T> type, T original, int srcVer) {
        if (type == FixTypes.CHUNK) {
            return (T) fixChunk((CompoundBinaryTag) original, srcVer);
        } else if (type == FixTypes.BLOCK_ENTITY) {
            return (T) fixBlockEntity((CompoundBinaryTag) original, srcVer);
        } else if (type == FixTypes.ENTITY) {
            return (T) fixEntity((CompoundBinaryTag) original, srcVer);
        } else if (type == FixTypes.BLOCK_STATE) {
            return (T) fixBlockState((String) original, srcVer);
        } else if (type == FixTypes.ITEM_TYPE) {
            return (T) fixItemType((String) original, srcVer);
        } else if (type == FixTypes.BIOME) {
            return (T) fixBiome((String) original, srcVer);
        }
        return original;
    }

    private CompoundBinaryTag fixChunk(CompoundBinaryTag originalChunk, int srcVer) {
        net.minecraft.nbt.CompoundTag tag = (net.minecraft.nbt.CompoundTag) adapter.fromNativeBinary(originalChunk);
        net.minecraft.nbt.CompoundTag fixed = convert(LegacyType.CHUNK, tag, srcVer);
        return (CompoundBinaryTag) adapter.toNativeBinary(fixed);
    }

    private CompoundBinaryTag fixBlockEntity(CompoundBinaryTag origTileEnt, int srcVer) {
        net.minecraft.nbt.CompoundTag tag = (net.minecraft.nbt.CompoundTag) adapter.fromNativeBinary(origTileEnt);
        net.minecraft.nbt.CompoundTag fixed = convert(LegacyType.BLOCK_ENTITY, tag, srcVer);
        return (CompoundBinaryTag) adapter.toNativeBinary(fixed);
    }

    private CompoundBinaryTag fixEntity(CompoundBinaryTag origEnt, int srcVer) {
        net.minecraft.nbt.CompoundTag tag = (net.minecraft.nbt.CompoundTag) adapter.fromNativeBinary(origEnt);
        net.minecraft.nbt.CompoundTag fixed = convert(LegacyType.ENTITY, tag, srcVer);
        return (CompoundBinaryTag) adapter.toNativeBinary(fixed);
    }
    //FAWE end

    private String fixBlockState(String blockState, int srcVer) {
        net.minecraft.nbt.CompoundTag stateNBT = stateToNBT(blockState);
        Dynamic<net.minecraft.nbt.Tag> dynamic = new Dynamic<>(OPS_NBT, stateNBT);
        net.minecraft.nbt.CompoundTag fixed = (net.minecraft.nbt.CompoundTag) INSTANCE.fixer.update(
                References.BLOCK_STATE,
                dynamic,
                srcVer,
                DATA_VERSION
        ).getValue();
        return nbtToState(fixed);
    }

    private String nbtToState(net.minecraft.nbt.CompoundTag tagCompound) {
        StringBuilder sb = new StringBuilder();
        sb.append(tagCompound.getString("Name"));
        if (tagCompound.contains("Properties", 10)) {
            sb.append('[');
            net.minecraft.nbt.CompoundTag props = tagCompound.getCompound("Properties");
            sb.append(props
                    .getAllKeys()
                    .stream()
                    .map(k -> k + "=" + props.getString(k).replace("\"", ""))
                    .collect(Collectors.joining(",")));
            sb.append(']');
        }
        return sb.toString();
    }

    private static net.minecraft.nbt.CompoundTag stateToNBT(String blockState) {
        int propIdx = blockState.indexOf('[');
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        if (propIdx < 0) {
            tag.putString("Name", blockState);
        } else {
            tag.putString("Name", blockState.substring(0, propIdx));
            net.minecraft.nbt.CompoundTag propTag = new net.minecraft.nbt.CompoundTag();
            String props = blockState.substring(propIdx + 1, blockState.length() - 1);
            String[] propArr = props.split(",");
            for (String pair : propArr) {
                final String[] split = pair.split("=");
                propTag.putString(split[0], split[1]);
            }
            tag.put("Properties", propTag);
        }
        return tag;
    }

    private String fixBiome(String key, int srcVer) {
        return fixName(key, srcVer, References.BIOME);
    }

    private String fixItemType(String key, int srcVer) {
        return fixName(key, srcVer, References.ITEM_NAME);
    }

    private static String fixName(String key, int srcVer, TypeReference type) {
        return INSTANCE.fixer.update(type, new Dynamic<>(OPS_NBT, net.minecraft.nbt.StringTag.valueOf(key)), srcVer, DATA_VERSION)
                .getValue().getAsString();
    }

    private final PaperweightAdapter adapter;

    private static final NbtOps OPS_NBT = NbtOps.INSTANCE;
    private static final int LEGACY_VERSION = 1343;
    private static int DATA_VERSION;
    static PaperweightDataConverters INSTANCE;

    private final Map<LegacyType, List<DataConverter>> converters = new EnumMap<>(LegacyType.class);
    private final Map<LegacyType, List<DataInspector>> inspectors = new EnumMap<>(LegacyType.class);

    // Set on build
    private DataFixer fixer;
    private static final Map<String, LegacyType> DFU_TO_LEGACY = new HashMap<>();

    public enum LegacyType {
        LEVEL(References.LEVEL),
        PLAYER(References.PLAYER),
        CHUNK(References.CHUNK),
        BLOCK_ENTITY(References.BLOCK_ENTITY),
        ENTITY(References.ENTITY),
        ITEM_INSTANCE(References.ITEM_STACK),
        OPTIONS(References.OPTIONS),
        STRUCTURE(References.STRUCTURE);

        private final TypeReference type;

        LegacyType(TypeReference type) {
            this.type = type;
            DFU_TO_LEGACY.put(type.typeName(), this);
        }

        public TypeReference getDFUType() {
            return type;
        }
    }

    PaperweightDataConverters(int dataVersion, PaperweightAdapter adapter) {
        super(dataVersion);
        DATA_VERSION = dataVersion;
        INSTANCE = this;
        this.adapter = adapter;
        registerConverters();
        registerInspectors();
    }


    // Called after fixers are built and ready for FIXING
    @Override
    public DataFixer build(final Executor executor) {
        return this.fixer = new WrappedDataFixer(DataFixers.getDataFixer());
    }

    @SuppressWarnings("unchecked")
    private class WrappedDataFixer implements DataFixer {

        private final DataFixer realFixer;

        WrappedDataFixer(DataFixer realFixer) {
            this.realFixer = realFixer;
        }

        @Override
        public <T> Dynamic<T> update(TypeReference type, Dynamic<T> dynamic, int sourceVer, int targetVer) {
            LegacyType legacyType = DFU_TO_LEGACY.get(type.typeName());
            if (sourceVer < LEGACY_VERSION && legacyType != null) {
                net.minecraft.nbt.CompoundTag cmp = (net.minecraft.nbt.CompoundTag) dynamic.getValue();
                int desiredVersion = Math.min(targetVer, LEGACY_VERSION);

                cmp = convert(legacyType, cmp, sourceVer, desiredVersion);
                sourceVer = desiredVersion;
                dynamic = new Dynamic(OPS_NBT, cmp);
            }
            return realFixer.update(type, dynamic, sourceVer, targetVer);
        }

        private net.minecraft.nbt.CompoundTag convert(
                LegacyType type,
                net.minecraft.nbt.CompoundTag cmp,
                int sourceVer,
                int desiredVersion
        ) {
            List<DataConverter> converters = PaperweightDataConverters.this.converters.get(type);
            if (converters != null && !converters.isEmpty()) {
                for (DataConverter converter : converters) {
                    int dataVersion = converter.getDataVersion();
                    if (dataVersion > sourceVer && dataVersion <= desiredVersion) {
                        cmp = converter.convert(cmp);
                    }
                }
            }

            List<DataInspector> inspectors = PaperweightDataConverters.this.inspectors.get(type);
            if (inspectors != null && !inspectors.isEmpty()) {
                for (DataInspector inspector : inspectors) {
                    cmp = inspector.inspect(cmp, sourceVer, desiredVersion);
                }
            }

            return cmp;
        }

        @Override
        public Schema getSchema(int i) {
            return realFixer.getSchema(i);
        }

    }

    public static net.minecraft.nbt.CompoundTag convert(LegacyType type, net.minecraft.nbt.CompoundTag cmp) {
        return convert(type.getDFUType(), cmp);
    }

    public static net.minecraft.nbt.CompoundTag convert(LegacyType type, net.minecraft.nbt.CompoundTag cmp, int sourceVer) {
        return convert(type.getDFUType(), cmp, sourceVer);
    }

    public static net.minecraft.nbt.CompoundTag convert(
            LegacyType type,
            net.minecraft.nbt.CompoundTag cmp,
            int sourceVer,
            int targetVer
    ) {
        return convert(type.getDFUType(), cmp, sourceVer, targetVer);
    }

    public static net.minecraft.nbt.CompoundTag convert(TypeReference type, net.minecraft.nbt.CompoundTag cmp) {
        int i = cmp.contains("DataVersion", 99) ? cmp.getInt("DataVersion") : -1;
        return convert(type, cmp, i);
    }

    public static net.minecraft.nbt.CompoundTag convert(TypeReference type, net.minecraft.nbt.CompoundTag cmp, int sourceVer) {
        return convert(type, cmp, sourceVer, DATA_VERSION);
    }

    public static net.minecraft.nbt.CompoundTag convert(
            TypeReference type,
            net.minecraft.nbt.CompoundTag cmp,
            int sourceVer,
            int targetVer
    ) {
        if (sourceVer >= targetVer) {
            return cmp;
        }
        return (net.minecraft.nbt.CompoundTag) INSTANCE.fixer
                .update(type, new Dynamic<>(OPS_NBT, cmp), sourceVer, targetVer)
                .getValue();
    }


    private void registerInspector(LegacyType type, DataInspector inspector) {
        this.inspectors.computeIfAbsent(type, k -> new ArrayList<>()).add(inspector);
    }

    private void registerConverter(LegacyType type, DataConverter converter) {
        int version = converter.getDataVersion();

        List<DataConverter> list = this.converters.computeIfAbsent(type, k -> new ArrayList<>());
        if (!list.isEmpty() && list.get(list.size() - 1).getDataVersion() > version) {
            for (int j = 0; j < list.size(); ++j) {
                if (list.get(j).getDataVersion() > version) {
                    list.add(j, converter);
                    break;
                }
            }
        } else {
            list.add(converter);
        }
    }

    private void registerInspectors() {
        registerEntityItemList("EntityHorseDonkey", "SaddleItem", "Items");
        registerEntityItemList("EntityHorseMule", "Items");
        registerEntityItemList("EntityMinecartChest", "Items");
        registerEntityItemList("EntityMinecartHopper", "Items");
        registerEntityItemList("EntityVillager", "Inventory");
        registerEntityItemListEquipment("EntityArmorStand");
        registerEntityItemListEquipment("EntityBat");
        registerEntityItemListEquipment("EntityBlaze");
        registerEntityItemListEquipment("EntityCaveSpider");
        registerEntityItemListEquipment("EntityChicken");
        registerEntityItemListEquipment("EntityCow");
        registerEntityItemListEquipment("EntityCreeper");
        registerEntityItemListEquipment("EntityEnderDragon");
        registerEntityItemListEquipment("EntityEnderman");
        registerEntityItemListEquipment("EntityEndermite");
        registerEntityItemListEquipment("EntityEvoker");
        registerEntityItemListEquipment("EntityGhast");
        registerEntityItemListEquipment("EntityGiantZombie");
        registerEntityItemListEquipment("EntityGuardian");
        registerEntityItemListEquipment("EntityGuardianElder");
        registerEntityItemListEquipment("EntityHorse");
        registerEntityItemListEquipment("EntityHorseDonkey");
        registerEntityItemListEquipment("EntityHorseMule");
        registerEntityItemListEquipment("EntityHorseSkeleton");
        registerEntityItemListEquipment("EntityHorseZombie");
        registerEntityItemListEquipment("EntityIronGolem");
        registerEntityItemListEquipment("EntityMagmaCube");
        registerEntityItemListEquipment("EntityMushroomCow");
        registerEntityItemListEquipment("EntityOcelot");
        registerEntityItemListEquipment("EntityPig");
        registerEntityItemListEquipment("EntityPigZombie");
        registerEntityItemListEquipment("EntityRabbit");
        registerEntityItemListEquipment("EntitySheep");
        registerEntityItemListEquipment("EntityShulker");
        registerEntityItemListEquipment("EntitySilverfish");
        registerEntityItemListEquipment("EntitySkeleton");
        registerEntityItemListEquipment("EntitySkeletonStray");
        registerEntityItemListEquipment("EntitySkeletonWither");
        registerEntityItemListEquipment("EntitySlime");
        registerEntityItemListEquipment("EntitySnowman");
        registerEntityItemListEquipment("EntitySpider");
        registerEntityItemListEquipment("EntitySquid");
        registerEntityItemListEquipment("EntityVex");
        registerEntityItemListEquipment("EntityVillager");
        registerEntityItemListEquipment("EntityVindicator");
        registerEntityItemListEquipment("EntityWitch");
        registerEntityItemListEquipment("EntityWither");
        registerEntityItemListEquipment("EntityWolf");
        registerEntityItemListEquipment("EntityZombie");
        registerEntityItemListEquipment("EntityZombieHusk");
        registerEntityItemListEquipment("EntityZombieVillager");
        registerEntityItemSingle("EntityFireworks", "FireworksItem");
        registerEntityItemSingle("EntityHorse", "ArmorItem");
        registerEntityItemSingle("EntityHorse", "SaddleItem");
        registerEntityItemSingle("EntityHorseMule", "SaddleItem");
        registerEntityItemSingle("EntityHorseSkeleton", "SaddleItem");
        registerEntityItemSingle("EntityHorseZombie", "SaddleItem");
        registerEntityItemSingle("EntityItem", "Item");
        registerEntityItemSingle("EntityItemFrame", "Item");
        registerEntityItemSingle("EntityPotion", "Potion");

        registerInspector(LegacyType.BLOCK_ENTITY, new DataInspectorItem("TileEntityRecordPlayer", "RecordItem"));
        registerInspector(LegacyType.BLOCK_ENTITY, new DataInspectorItemList("TileEntityBrewingStand", "Items"));
        registerInspector(LegacyType.BLOCK_ENTITY, new DataInspectorItemList("TileEntityChest", "Items"));
        registerInspector(LegacyType.BLOCK_ENTITY, new DataInspectorItemList("TileEntityDispenser", "Items"));
        registerInspector(LegacyType.BLOCK_ENTITY, new DataInspectorItemList("TileEntityDropper", "Items"));
        registerInspector(LegacyType.BLOCK_ENTITY, new DataInspectorItemList("TileEntityFurnace", "Items"));
        registerInspector(LegacyType.BLOCK_ENTITY, new DataInspectorItemList("TileEntityHopper", "Items"));
        registerInspector(LegacyType.BLOCK_ENTITY, new DataInspectorItemList("TileEntityShulkerBox", "Items"));
        registerInspector(LegacyType.BLOCK_ENTITY, new DataInspectorMobSpawnerMobs());
        registerInspector(LegacyType.CHUNK, new DataInspectorChunks());
        registerInspector(LegacyType.ENTITY, new DataInspectorCommandBlock());
        registerInspector(LegacyType.ENTITY, new DataInspectorEntityPassengers());
        registerInspector(LegacyType.ENTITY, new DataInspectorMobSpawnerMinecart());
        registerInspector(LegacyType.ENTITY, new DataInspectorVillagers());
        registerInspector(LegacyType.ITEM_INSTANCE, new DataInspectorBlockEntity());
        registerInspector(LegacyType.ITEM_INSTANCE, new DataInspectorEntity());
        registerInspector(LegacyType.LEVEL, new DataInspectorLevelPlayer());
        registerInspector(LegacyType.PLAYER, new DataInspectorPlayer());
        registerInspector(LegacyType.PLAYER, new DataInspectorPlayerVehicle());
        registerInspector(LegacyType.STRUCTURE, new DataInspectorStructure());
    }

    private void registerConverters() {
        registerConverter(LegacyType.ENTITY, new DataConverterEquipment());
        registerConverter(LegacyType.BLOCK_ENTITY, new DataConverterSignText());
        registerConverter(LegacyType.ITEM_INSTANCE, new DataConverterMaterialId());
        registerConverter(LegacyType.ITEM_INSTANCE, new DataConverterPotionId());
        registerConverter(LegacyType.ITEM_INSTANCE, new DataConverterSpawnEgg());
        registerConverter(LegacyType.ENTITY, new DataConverterMinecart());
        registerConverter(LegacyType.BLOCK_ENTITY, new DataConverterMobSpawner());
        registerConverter(LegacyType.ENTITY, new DataConverterUUID());
        registerConverter(LegacyType.ENTITY, new DataConverterHealth());
        registerConverter(LegacyType.ENTITY, new DataConverterSaddle());
        registerConverter(LegacyType.ENTITY, new DataConverterHanging());
        registerConverter(LegacyType.ENTITY, new DataConverterDropChances());
        registerConverter(LegacyType.ENTITY, new DataConverterRiding());
        registerConverter(LegacyType.ENTITY, new DataConverterArmorStand());
        registerConverter(LegacyType.ITEM_INSTANCE, new DataConverterBook());
        registerConverter(LegacyType.ITEM_INSTANCE, new DataConverterCookedFish());
        registerConverter(LegacyType.ENTITY, new DataConverterZombie());
        registerConverter(LegacyType.OPTIONS, new DataConverterVBO());
        registerConverter(LegacyType.ENTITY, new DataConverterGuardian());
        registerConverter(LegacyType.ENTITY, new DataConverterSkeleton());
        registerConverter(LegacyType.ENTITY, new DataConverterZombieType());
        registerConverter(LegacyType.ENTITY, new DataConverterHorse());
        registerConverter(LegacyType.BLOCK_ENTITY, new DataConverterTileEntity());
        registerConverter(LegacyType.ENTITY, new DataConverterEntity());
        registerConverter(LegacyType.ITEM_INSTANCE, new DataConverterBanner());
        registerConverter(LegacyType.ITEM_INSTANCE, new DataConverterPotionWater());
        registerConverter(LegacyType.ENTITY, new DataConverterShulker());
        registerConverter(LegacyType.ITEM_INSTANCE, new DataConverterShulkerBoxItem());
        registerConverter(LegacyType.BLOCK_ENTITY, new DataConverterShulkerBoxBlock());
        registerConverter(LegacyType.OPTIONS, new DataConverterLang());
        registerConverter(LegacyType.ITEM_INSTANCE, new DataConverterTotem());
        registerConverter(LegacyType.CHUNK, new DataConverterBedBlock());
        registerConverter(LegacyType.ITEM_INSTANCE, new DataConverterBedItem());
    }

    private void registerEntityItemList(String type, String... keys) {
        registerInspector(LegacyType.ENTITY, new DataInspectorItemList(type, keys));
    }

    private void registerEntityItemSingle(String type, String key) {
        registerInspector(LegacyType.ENTITY, new DataInspectorItem(type, key));
    }

    private void registerEntityItemListEquipment(String type) {
        registerEntityItemList(type, "ArmorItems", "HandItems");
    }

    public static void convertCompound(
            LegacyType type,
            net.minecraft.nbt.CompoundTag cmp,
            String key,
            int sourceVer,
            int targetVer
    ) {
        cmp.put(key, convert(type, cmp.getCompound(key), sourceVer, targetVer));
    }

    public static void convertItem(net.minecraft.nbt.CompoundTag nbttagcompound, String key, int sourceVer, int targetVer) {
        if (nbttagcompound.contains(key, 10)) {
            convertCompound(LegacyType.ITEM_INSTANCE, nbttagcompound, key, sourceVer, targetVer);
        }
    }

    public static void convertItems(net.minecraft.nbt.CompoundTag nbttagcompound, String key, int sourceVer, int targetVer) {
        if (nbttagcompound.contains(key, 9)) {
            net.minecraft.nbt.ListTag nbttaglist = nbttagcompound.getList(key, 10);

            for (int j = 0; j < nbttaglist.size(); ++j) {
                nbttaglist.set(j, convert(LegacyType.ITEM_INSTANCE, nbttaglist.getCompound(j), sourceVer, targetVer));
            }
        }

    }
}
