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

package com.boydti.fawe.bukkit.adapter.mc1_14;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.DataFixerBuilder;
import com.mojang.datafixers.Dynamic;
import com.mojang.datafixers.schemas.Schema;
import com.sk89q.jnbt.CompoundTag;
import net.minecraft.server.v1_14_R1.ChatComponentText;
import net.minecraft.server.v1_14_R1.ChatDeserializer;
import net.minecraft.server.v1_14_R1.DataConverterRegistry;
import net.minecraft.server.v1_14_R1.DataConverterTypes;
import net.minecraft.server.v1_14_R1.DataFixTypes;
import net.minecraft.server.v1_14_R1.DynamicOpsNBT;
import net.minecraft.server.v1_14_R1.EnumColor;
import net.minecraft.server.v1_14_R1.EnumDirection;
import net.minecraft.server.v1_14_R1.IChatBaseComponent;
import net.minecraft.server.v1_14_R1.MinecraftKey;
import net.minecraft.server.v1_14_R1.NBTBase;
import net.minecraft.server.v1_14_R1.NBTTagCompound;
import net.minecraft.server.v1_14_R1.NBTTagFloat;
import net.minecraft.server.v1_14_R1.NBTTagList;
import net.minecraft.server.v1_14_R1.NBTTagString;
import net.minecraft.server.v1_14_R1.UtilColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Handles converting all Pre 1.13.2 data using the Legacy DataFix System (ported to 1.13.2)
 *
 * We register a DFU Fixer per Legacy Data Version and apply the fixes using legacy strategy
 * which is safer, faster and cleaner code.
 *
 * The pre DFU code did not fail when the Source version was unknown.
 *
 * This class also provides util methods for converting compounds to wrap the update call to
 * receive the source version in the compound
 *
 */
@SuppressWarnings("UnnecessarilyQualifiedStaticUsage")
public class DataConverters_1_14_R4 extends DataFixerBuilder implements com.sk89q.worldedit.world.DataFixer {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T fixUp(FixType<T> type, T original, int srcVer) {
        if (type == FixTypes.CHUNK) {
            return (T) fixChunk((CompoundTag) original, srcVer);
        } else if (type == FixTypes.BLOCK_ENTITY) {
            return (T) fixBlockEntity((CompoundTag) original, srcVer);
        } else if (type == FixTypes.ENTITY) {
            return (T) fixEntity((CompoundTag) original, srcVer);
        } else if (type == FixTypes.BLOCK_STATE) {
            return (T) fixBlockState((String) original, srcVer);
        } else if (type == FixTypes.ITEM_TYPE) {
            return (T) fixItemType((String) original, srcVer);
        } else if (type == FixTypes.BIOME) {
            return (T) fixBiome((String) original, srcVer);
        }
        return original;
    }

    private CompoundTag fixChunk(CompoundTag originalChunk, int srcVer) {
        NBTTagCompound tag = (NBTTagCompound) adapter.fromNative(originalChunk);
        NBTTagCompound fixed = convert(LegacyType.CHUNK, tag, srcVer);
        return (CompoundTag) adapter.toNative(fixed);
    }

    private CompoundTag fixBlockEntity(CompoundTag origTileEnt, int srcVer) {
        NBTTagCompound tag = (NBTTagCompound) adapter.fromNative(origTileEnt);
        NBTTagCompound fixed = convert(LegacyType.BLOCK_ENTITY, tag, srcVer);
        return (CompoundTag) adapter.toNative(fixed);
    }

    private CompoundTag fixEntity(CompoundTag origEnt, int srcVer) {
        NBTTagCompound tag = (NBTTagCompound) adapter.fromNative(origEnt);
        NBTTagCompound fixed = convert(LegacyType.ENTITY, tag, srcVer);
        return (CompoundTag) adapter.toNative(fixed);
    }

    private String fixBlockState(String blockState, int srcVer) {
        NBTTagCompound stateNBT = stateToNBT(blockState);
        Dynamic<NBTBase> dynamic = new Dynamic<>(OPS_NBT, stateNBT);
        NBTTagCompound fixed = (NBTTagCompound) INSTANCE.fixer.update(DataConverterTypes.m, dynamic, srcVer, DATA_VERSION).getValue();
        return nbtToState(fixed);
    }

    private String nbtToState(NBTTagCompound tagCompound) {
        StringBuilder sb = new StringBuilder();
        sb.append(tagCompound.getString("Name"));
        if (tagCompound.hasKeyOfType("Properties", 10)) {
            sb.append('[');
            NBTTagCompound props = tagCompound.getCompound("Properties");
            sb.append(props.getKeys().stream().map(k -> k + "=" + props.getString(k).replace("\"", "")).collect(Collectors.joining(",")));
            sb.append(']');
        }
        return sb.toString();
    }

    private static NBTTagCompound stateToNBT(String blockState) {
        int propIdx = blockState.indexOf('[');
        NBTTagCompound tag = new NBTTagCompound();
        if (propIdx < 0) {
            tag.setString("Name", blockState);
        } else {
            tag.setString("Name", blockState.substring(0, propIdx));
            NBTTagCompound propTag = new NBTTagCompound();
            String props = blockState.substring(propIdx + 1, blockState.length() - 1);
            String[] propArr = props.split(",");
            for (String pair : propArr) {
                final String[] split = pair.split("=");
                propTag.setString(split[0], split[1]);
            }
            tag.set("Properties", propTag);
        }
        return tag;
    }

    private String fixBiome(String key, int srcVer) {
        return fixName(key, srcVer, DataConverterTypes.x); // "biome"
    }

    private String fixItemType(String key, int srcVer) {
        return fixName(key, srcVer, DataConverterTypes.r); // "item_name"
    }

    private static String fixName(String key, int srcVer, TypeReference type) {
        return INSTANCE.fixer.update(type, new Dynamic<>(OPS_NBT, new NBTTagString(key)), srcVer, DATA_VERSION)
                .getValue().asString();
    }

    private final Spigot_v1_14_R4 adapter;

    private static final DynamicOpsNBT OPS_NBT = DynamicOpsNBT.a;
    private static final int LEGACY_VERSION = 1343;
    private static int DATA_VERSION;
    static DataConverters_1_14_R4 INSTANCE;

    private final Map<LegacyType, List<DataConverter>> converters = new EnumMap<>(LegacyType.class);
    private final Map<LegacyType, List<DataInspector>> inspectors = new EnumMap<>(LegacyType.class);

    // Set on build
    private DataFixer fixer;
    private static final Map<String, LegacyType> DFU_TO_LEGACY = new HashMap<>();

    public enum LegacyType {
        LEVEL(DataFixTypes.LEVEL.a()),
        PLAYER(DataFixTypes.PLAYER.a()),
        CHUNK(DataFixTypes.CHUNK.a()),
        BLOCK_ENTITY(DataConverterTypes.k), // "block_entity"
        ENTITY(DataConverterTypes.ENTITY),
        ITEM_INSTANCE(DataConverterTypes.ITEM_STACK),
        OPTIONS(DataFixTypes.OPTIONS.a()),
        STRUCTURE(DataFixTypes.STRUCTURE.a());

        private final TypeReference type;

        LegacyType(TypeReference type) {
            this.type = type;
            DFU_TO_LEGACY.put(type.typeName(), this);
        }

        public TypeReference getDFUType() {
            return type;
        }
    }

    DataConverters_1_14_R4(int dataVersion, Spigot_v1_14_R4 adapter) {
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
        return this.fixer = new WrappedDataFixer(DataConverterRegistry.a());
    }

    private class WrappedDataFixer implements DataFixer {
        private final DataFixer realFixer;

        WrappedDataFixer(DataFixer realFixer) {
            this.realFixer = realFixer;
        }

        @Override
        public <T> Dynamic<T> update(TypeReference type, Dynamic<T> dynamic, int sourceVer, int targetVer) {
            LegacyType legacyType = DFU_TO_LEGACY.get(type.typeName());
            if (sourceVer < LEGACY_VERSION && legacyType != null) {
                NBTTagCompound cmp = (NBTTagCompound) dynamic.getValue();
                int desiredVersion = Math.min(targetVer, LEGACY_VERSION);

                cmp = convert(legacyType, cmp, sourceVer, desiredVersion);
                sourceVer = desiredVersion;
                dynamic = new Dynamic(OPS_NBT, cmp);
            }
            return realFixer.update(type, dynamic, sourceVer, targetVer);
        }

        private NBTTagCompound convert(LegacyType type, NBTTagCompound cmp, int sourceVer, int desiredVersion) {
            List<DataConverter> converters = DataConverters_1_14_R4.this.converters.get(type);
            if (converters != null && !converters.isEmpty()) {
                for (DataConverter converter : converters) {
                    int dataVersion = converter.getDataVersion();
                    if (dataVersion > sourceVer && dataVersion <= desiredVersion) {
                        cmp = converter.convert(cmp);
                    }
                }
            }

            List<DataInspector> inspectors = DataConverters_1_14_R4.this.inspectors.get(type);
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

    public static NBTTagCompound convert(LegacyType type, NBTTagCompound cmp) {
        return convert(type.getDFUType(), cmp);
    }

    public static NBTTagCompound convert(LegacyType type, NBTTagCompound cmp, int sourceVer) {
        return convert(type.getDFUType(), cmp, sourceVer);
    }

    public static NBTTagCompound convert(LegacyType type, NBTTagCompound cmp, int sourceVer, int targetVer) {
        return convert(type.getDFUType(), cmp, sourceVer, targetVer);
    }

    public static NBTTagCompound convert(TypeReference type, NBTTagCompound cmp) {
        int i = cmp.hasKeyOfType("DataVersion", 99) ? cmp.getInt("DataVersion") : -1;
        return convert(type, cmp, i);
    }

    public static NBTTagCompound convert(TypeReference type, NBTTagCompound cmp, int sourceVer) {
        return convert(type, cmp, sourceVer, DATA_VERSION);
    }

    public static NBTTagCompound convert(TypeReference type, NBTTagCompound cmp, int sourceVer, int targetVer) {
        if (sourceVer >= targetVer) {
            return cmp;
        }
        return (NBTTagCompound) INSTANCE.fixer.update(type, new Dynamic<>(OPS_NBT, cmp), sourceVer, targetVer).getValue();
    }


    public interface DataInspector {
        NBTTagCompound inspect(NBTTagCompound cmp, int sourceVer, int targetVer);
    }

    public interface DataConverter {

        int getDataVersion();

        NBTTagCompound convert(NBTTagCompound cmp);
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
    private static final Map<String, MinecraftKey> OLD_ID_TO_KEY_MAP = new HashMap<>();

    static {
        final Map<String, MinecraftKey> map = OLD_ID_TO_KEY_MAP;
        map.put("EntityItem", new MinecraftKey("item"));
        map.put("EntityExperienceOrb", new MinecraftKey("xp_orb"));
        map.put("EntityAreaEffectCloud", new MinecraftKey("area_effect_cloud"));
        map.put("EntityGuardianElder", new MinecraftKey("elder_guardian"));
        map.put("EntitySkeletonWither", new MinecraftKey("wither_skeleton"));
        map.put("EntitySkeletonStray", new MinecraftKey("stray"));
        map.put("EntityEgg", new MinecraftKey("egg"));
        map.put("EntityLeash", new MinecraftKey("leash_knot"));
        map.put("EntityPainting", new MinecraftKey("painting"));
        map.put("EntityTippedArrow", new MinecraftKey("arrow"));
        map.put("EntitySnowball", new MinecraftKey("snowball"));
        map.put("EntityLargeFireball", new MinecraftKey("fireball"));
        map.put("EntitySmallFireball", new MinecraftKey("small_fireball"));
        map.put("EntityEnderPearl", new MinecraftKey("ender_pearl"));
        map.put("EntityEnderSignal", new MinecraftKey("eye_of_ender_signal"));
        map.put("EntityPotion", new MinecraftKey("potion"));
        map.put("EntityThrownExpBottle", new MinecraftKey("xp_bottle"));
        map.put("EntityItemFrame", new MinecraftKey("item_frame"));
        map.put("EntityWitherSkull", new MinecraftKey("wither_skull"));
        map.put("EntityTNTPrimed", new MinecraftKey("tnt"));
        map.put("EntityFallingBlock", new MinecraftKey("falling_block"));
        map.put("EntityFireworks", new MinecraftKey("fireworks_rocket"));
        map.put("EntityZombieHusk", new MinecraftKey("husk"));
        map.put("EntitySpectralArrow", new MinecraftKey("spectral_arrow"));
        map.put("EntityShulkerBullet", new MinecraftKey("shulker_bullet"));
        map.put("EntityDragonFireball", new MinecraftKey("dragon_fireball"));
        map.put("EntityZombieVillager", new MinecraftKey("zombie_villager"));
        map.put("EntityHorseSkeleton", new MinecraftKey("skeleton_horse"));
        map.put("EntityHorseZombie", new MinecraftKey("zombie_horse"));
        map.put("EntityArmorStand", new MinecraftKey("armor_stand"));
        map.put("EntityHorseDonkey", new MinecraftKey("donkey"));
        map.put("EntityHorseMule", new MinecraftKey("mule"));
        map.put("EntityEvokerFangs", new MinecraftKey("evocation_fangs"));
        map.put("EntityEvoker", new MinecraftKey("evocation_illager"));
        map.put("EntityVex", new MinecraftKey("vex"));
        map.put("EntityVindicator", new MinecraftKey("vindication_illager"));
        map.put("EntityIllagerIllusioner", new MinecraftKey("illusion_illager"));
        map.put("EntityMinecartCommandBlock", new MinecraftKey("commandblock_minecart"));
        map.put("EntityBoat", new MinecraftKey("boat"));
        map.put("EntityMinecartRideable", new MinecraftKey("minecart"));
        map.put("EntityMinecartChest", new MinecraftKey("chest_minecart"));
        map.put("EntityMinecartFurnace", new MinecraftKey("furnace_minecart"));
        map.put("EntityMinecartTNT", new MinecraftKey("tnt_minecart"));
        map.put("EntityMinecartHopper", new MinecraftKey("hopper_minecart"));
        map.put("EntityMinecartMobSpawner", new MinecraftKey("spawner_minecart"));
        map.put("EntityCreeper", new MinecraftKey("creeper"));
        map.put("EntitySkeleton", new MinecraftKey("skeleton"));
        map.put("EntitySpider", new MinecraftKey("spider"));
        map.put("EntityGiantZombie", new MinecraftKey("giant"));
        map.put("EntityZombie", new MinecraftKey("zombie"));
        map.put("EntitySlime", new MinecraftKey("slime"));
        map.put("EntityGhast", new MinecraftKey("ghast"));
        map.put("EntityPigZombie", new MinecraftKey("zombie_pigman"));
        map.put("EntityEnderman", new MinecraftKey("enderman"));
        map.put("EntityCaveSpider", new MinecraftKey("cave_spider"));
        map.put("EntitySilverfish", new MinecraftKey("silverfish"));
        map.put("EntityBlaze", new MinecraftKey("blaze"));
        map.put("EntityMagmaCube", new MinecraftKey("magma_cube"));
        map.put("EntityEnderDragon", new MinecraftKey("ender_dragon"));
        map.put("EntityWither", new MinecraftKey("wither"));
        map.put("EntityBat", new MinecraftKey("bat"));
        map.put("EntityWitch", new MinecraftKey("witch"));
        map.put("EntityEndermite", new MinecraftKey("endermite"));
        map.put("EntityGuardian", new MinecraftKey("guardian"));
        map.put("EntityShulker", new MinecraftKey("shulker"));
        map.put("EntityPig", new MinecraftKey("pig"));
        map.put("EntitySheep", new MinecraftKey("sheep"));
        map.put("EntityCow", new MinecraftKey("cow"));
        map.put("EntityChicken", new MinecraftKey("chicken"));
        map.put("EntitySquid", new MinecraftKey("squid"));
        map.put("EntityWolf", new MinecraftKey("wolf"));
        map.put("EntityMushroomCow", new MinecraftKey("mooshroom"));
        map.put("EntitySnowman", new MinecraftKey("snowman"));
        map.put("EntityOcelot", new MinecraftKey("ocelot"));
        map.put("EntityIronGolem", new MinecraftKey("villager_golem"));
        map.put("EntityHorse", new MinecraftKey("horse"));
        map.put("EntityRabbit", new MinecraftKey("rabbit"));
        map.put("EntityPolarBear", new MinecraftKey("polar_bear"));
        map.put("EntityLlama", new MinecraftKey("llama"));
        map.put("EntityLlamaSpit", new MinecraftKey("llama_spit"));
        map.put("EntityParrot", new MinecraftKey("parrot"));
        map.put("EntityVillager", new MinecraftKey("villager"));
        map.put("EntityEnderCrystal", new MinecraftKey("ender_crystal"));
        map.put("TileEntityFurnace", new MinecraftKey("furnace"));
        map.put("TileEntityChest", new MinecraftKey("chest"));
        map.put("TileEntityEnderChest", new MinecraftKey("ender_chest"));
        map.put("TileEntityRecordPlayer", new MinecraftKey("jukebox"));
        map.put("TileEntityDispenser", new MinecraftKey("dispenser"));
        map.put("TileEntityDropper", new MinecraftKey("dropper"));
        map.put("TileEntitySign", new MinecraftKey("sign"));
        map.put("TileEntityMobSpawner", new MinecraftKey("mob_spawner"));
        map.put("TileEntityNote", new MinecraftKey("noteblock"));
        map.put("TileEntityPiston", new MinecraftKey("piston"));
        map.put("TileEntityBrewingStand", new MinecraftKey("brewing_stand"));
        map.put("TileEntityEnchantTable", new MinecraftKey("enchanting_table"));
        map.put("TileEntityEnderPortal", new MinecraftKey("end_portal"));
        map.put("TileEntityBeacon", new MinecraftKey("beacon"));
        map.put("TileEntitySkull", new MinecraftKey("skull"));
        map.put("TileEntityLightDetector", new MinecraftKey("daylight_detector"));
        map.put("TileEntityHopper", new MinecraftKey("hopper"));
        map.put("TileEntityComparator", new MinecraftKey("comparator"));
        map.put("TileEntityFlowerPot", new MinecraftKey("flower_pot"));
        map.put("TileEntityBanner", new MinecraftKey("banner"));
        map.put("TileEntityStructure", new MinecraftKey("structure_block"));
        map.put("TileEntityEndGateway", new MinecraftKey("end_gateway"));
        map.put("TileEntityCommand", new MinecraftKey("command_block"));
        map.put("TileEntityShulkerBox", new MinecraftKey("shulker_box"));
        map.put("TileEntityBed", new MinecraftKey("bed"));
    }

    private static MinecraftKey getKey(String type) {
        final MinecraftKey key = OLD_ID_TO_KEY_MAP.get(type);
        if (key == null) {
            throw new IllegalArgumentException("Unknown mapping for " + type);
        }
        return key;
    }

    private static void convertCompound(LegacyType type, NBTTagCompound cmp, String key, int sourceVer, int targetVer) {
        cmp.set(key, convert(type, cmp.getCompound(key), sourceVer, targetVer));
    }

    private static void convertItem(NBTTagCompound nbttagcompound, String key, int sourceVer, int targetVer) {
        if (nbttagcompound.hasKeyOfType(key, 10)) {
            convertCompound(LegacyType.ITEM_INSTANCE, nbttagcompound, key, sourceVer, targetVer);
        }
    }

    private static void convertItems(NBTTagCompound nbttagcompound, String key, int sourceVer, int targetVer) {
        if (nbttagcompound.hasKeyOfType(key, 9)) {
            NBTTagList nbttaglist = nbttagcompound.getList(key, 10);

            for (int j = 0; j < nbttaglist.size(); ++j) {
                nbttaglist.set(j, convert(LegacyType.ITEM_INSTANCE, nbttaglist.getCompound(j), sourceVer, targetVer));
            }
        }

    }

    private static class DataConverterEquipment implements DataConverter {

        DataConverterEquipment() {}

        public int getDataVersion() {
            return 100;
        }

        public NBTTagCompound convert(NBTTagCompound cmp) {
            NBTTagList nbttaglist = cmp.getList("Equipment", 10);
            NBTTagList nbttaglist1;

            if (!nbttaglist.isEmpty() && !cmp.hasKeyOfType("HandItems", 10)) {
                nbttaglist1 = new NBTTagList();
                nbttaglist1.add(nbttaglist.get(0));
                nbttaglist1.add(new NBTTagCompound());
                cmp.set("HandItems", nbttaglist1);
            }

            if (nbttaglist.size() > 1 && !cmp.hasKeyOfType("ArmorItem", 10)) {
                nbttaglist1 = new NBTTagList();
                nbttaglist1.add(nbttaglist.get(1));
                nbttaglist1.add(nbttaglist.get(2));
                nbttaglist1.add(nbttaglist.get(3));
                nbttaglist1.add(nbttaglist.get(4));
                cmp.set("ArmorItems", nbttaglist1);
            }

            cmp.remove("Equipment");
            if (cmp.hasKeyOfType("DropChances", 9)) {
                nbttaglist1 = cmp.getList("DropChances", 5);
                NBTTagList nbttaglist2;

                if (!cmp.hasKeyOfType("HandDropChances", 10)) {
                    nbttaglist2 = new NBTTagList();
                    nbttaglist2.add(new NBTTagFloat(nbttaglist1.i(0)));
                    nbttaglist2.add(new NBTTagFloat(0.0F));
                    cmp.set("HandDropChances", nbttaglist2);
                }

                if (!cmp.hasKeyOfType("ArmorDropChances", 10)) {
                    nbttaglist2 = new NBTTagList();
                    nbttaglist2.add(new NBTTagFloat(nbttaglist1.i(1)));
                    nbttaglist2.add(new NBTTagFloat(nbttaglist1.i(2)));
                    nbttaglist2.add(new NBTTagFloat(nbttaglist1.i(3)));
                    nbttaglist2.add(new NBTTagFloat(nbttaglist1.i(4)));
                    cmp.set("ArmorDropChances", nbttaglist2);
                }

                cmp.remove("DropChances");
            }

            return cmp;
        }
    }

    private static class DataInspectorBlockEntity implements DataInspector {

        private static final Map<String, String> b = Maps.newHashMap();
        private static final Map<String, String> c = Maps.newHashMap();

        DataInspectorBlockEntity() {}

        @Nullable
        private static String convertEntityId(int i, String s) {
            String key = new MinecraftKey(s).toString();
            if (i < 515 && DataInspectorBlockEntity.b.containsKey(key)) {
                return DataInspectorBlockEntity.b.get(key);
            } else {
                return DataInspectorBlockEntity.c.get(key);
            }
        }

        public NBTTagCompound inspect(NBTTagCompound cmp, int sourceVer, int targetVer) {
            if (!cmp.hasKeyOfType("tag", 10)) {
                return cmp;
            } else {
                NBTTagCompound nbttagcompound1 = cmp.getCompound("tag");

                if (nbttagcompound1.hasKeyOfType("BlockEntityTag", 10)) {
                    NBTTagCompound nbttagcompound2 = nbttagcompound1.getCompound("BlockEntityTag");
                    String s = cmp.getString("id");
                    String s1 = convertEntityId(sourceVer, s);
                    boolean flag;

                    if (s1 == null) {
                        // CraftBukkit - Remove unnecessary warning (occurs when deserializing a Shulker Box item)
                        // DataInspectorBlockEntity.a.warn("Unable to resolve BlockEntity for ItemInstance: {}", s);
                        flag = false;
                    } else {
                        flag = !nbttagcompound2.hasKey("id");
                        nbttagcompound2.setString("id", s1);
                    }

                    convert(LegacyType.BLOCK_ENTITY, nbttagcompound2, sourceVer, targetVer);
                    if (flag) {
                        nbttagcompound2.remove("id");
                    }
                }

                return cmp;
            }
        }

        static {
            Map map = DataInspectorBlockEntity.b;

            map.put("minecraft:furnace", "Furnace");
            map.put("minecraft:lit_furnace", "Furnace");
            map.put("minecraft:chest", "Chest");
            map.put("minecraft:trapped_chest", "Chest");
            map.put("minecraft:ender_chest", "EnderChest");
            map.put("minecraft:jukebox", "RecordPlayer");
            map.put("minecraft:dispenser", "Trap");
            map.put("minecraft:dropper", "Dropper");
            map.put("minecraft:sign", "Sign");
            map.put("minecraft:mob_spawner", "MobSpawner");
            map.put("minecraft:noteblock", "Music");
            map.put("minecraft:brewing_stand", "Cauldron");
            map.put("minecraft:enhanting_table", "EnchantTable");
            map.put("minecraft:command_block", "CommandBlock");
            map.put("minecraft:beacon", "Beacon");
            map.put("minecraft:skull", "Skull");
            map.put("minecraft:daylight_detector", "DLDetector");
            map.put("minecraft:hopper", "Hopper");
            map.put("minecraft:banner", "Banner");
            map.put("minecraft:flower_pot", "FlowerPot");
            map.put("minecraft:repeating_command_block", "CommandBlock");
            map.put("minecraft:chain_command_block", "CommandBlock");
            map.put("minecraft:standing_sign", "Sign");
            map.put("minecraft:wall_sign", "Sign");
            map.put("minecraft:piston_head", "Piston");
            map.put("minecraft:daylight_detector_inverted", "DLDetector");
            map.put("minecraft:unpowered_comparator", "Comparator");
            map.put("minecraft:powered_comparator", "Comparator");
            map.put("minecraft:wall_banner", "Banner");
            map.put("minecraft:standing_banner", "Banner");
            map.put("minecraft:structure_block", "Structure");
            map.put("minecraft:end_portal", "Airportal");
            map.put("minecraft:end_gateway", "EndGateway");
            map.put("minecraft:shield", "Shield");
            map = DataInspectorBlockEntity.c;
            map.put("minecraft:furnace", "minecraft:furnace");
            map.put("minecraft:lit_furnace", "minecraft:furnace");
            map.put("minecraft:chest", "minecraft:chest");
            map.put("minecraft:trapped_chest", "minecraft:chest");
            map.put("minecraft:ender_chest", "minecraft:enderchest");
            map.put("minecraft:jukebox", "minecraft:jukebox");
            map.put("minecraft:dispenser", "minecraft:dispenser");
            map.put("minecraft:dropper", "minecraft:dropper");
            map.put("minecraft:sign", "minecraft:sign");
            map.put("minecraft:mob_spawner", "minecraft:mob_spawner");
            map.put("minecraft:noteblock", "minecraft:noteblock");
            map.put("minecraft:brewing_stand", "minecraft:brewing_stand");
            map.put("minecraft:enhanting_table", "minecraft:enchanting_table");
            map.put("minecraft:command_block", "minecraft:command_block");
            map.put("minecraft:beacon", "minecraft:beacon");
            map.put("minecraft:skull", "minecraft:skull");
            map.put("minecraft:daylight_detector", "minecraft:daylight_detector");
            map.put("minecraft:hopper", "minecraft:hopper");
            map.put("minecraft:banner", "minecraft:banner");
            map.put("minecraft:flower_pot", "minecraft:flower_pot");
            map.put("minecraft:repeating_command_block", "minecraft:command_block");
            map.put("minecraft:chain_command_block", "minecraft:command_block");
            map.put("minecraft:shulker_box", "minecraft:shulker_box");
            map.put("minecraft:white_shulker_box", "minecraft:shulker_box");
            map.put("minecraft:orange_shulker_box", "minecraft:shulker_box");
            map.put("minecraft:magenta_shulker_box", "minecraft:shulker_box");
            map.put("minecraft:light_blue_shulker_box", "minecraft:shulker_box");
            map.put("minecraft:yellow_shulker_box", "minecraft:shulker_box");
            map.put("minecraft:lime_shulker_box", "minecraft:shulker_box");
            map.put("minecraft:pink_shulker_box", "minecraft:shulker_box");
            map.put("minecraft:gray_shulker_box", "minecraft:shulker_box");
            map.put("minecraft:silver_shulker_box", "minecraft:shulker_box");
            map.put("minecraft:cyan_shulker_box", "minecraft:shulker_box");
            map.put("minecraft:purple_shulker_box", "minecraft:shulker_box");
            map.put("minecraft:blue_shulker_box", "minecraft:shulker_box");
            map.put("minecraft:brown_shulker_box", "minecraft:shulker_box");
            map.put("minecraft:green_shulker_box", "minecraft:shulker_box");
            map.put("minecraft:red_shulker_box", "minecraft:shulker_box");
            map.put("minecraft:black_shulker_box", "minecraft:shulker_box");
            map.put("minecraft:bed", "minecraft:bed");
            map.put("minecraft:standing_sign", "minecraft:sign");
            map.put("minecraft:wall_sign", "minecraft:sign");
            map.put("minecraft:piston_head", "minecraft:piston");
            map.put("minecraft:daylight_detector_inverted", "minecraft:daylight_detector");
            map.put("minecraft:unpowered_comparator", "minecraft:comparator");
            map.put("minecraft:powered_comparator", "minecraft:comparator");
            map.put("minecraft:wall_banner", "minecraft:banner");
            map.put("minecraft:standing_banner", "minecraft:banner");
            map.put("minecraft:structure_block", "minecraft:structure_block");
            map.put("minecraft:end_portal", "minecraft:end_portal");
            map.put("minecraft:end_gateway", "minecraft:end_gateway");
            map.put("minecraft:shield", "minecraft:shield");
        }
    }

    private static class DataInspectorEntity implements DataInspector {

        private static final Logger a = LogManager.getLogger(DataConverters_1_14_R4.class);

        DataInspectorEntity() {}

        public NBTTagCompound inspect(NBTTagCompound cmp, int sourceVer, int targetVer) {
            NBTTagCompound nbttagcompound1 = cmp.getCompound("tag");

            if (nbttagcompound1.hasKeyOfType("EntityTag", 10)) {
                NBTTagCompound nbttagcompound2 = nbttagcompound1.getCompound("EntityTag");
                String s = cmp.getString("id");
                String s1;

                if ("minecraft:armor_stand".equals(s)) {
                    s1 = sourceVer < 515 ? "ArmorStand" : "minecraft:armor_stand";
                } else {
                    if (!"minecraft:spawn_egg".equals(s)) {
                        return cmp;
                    }

                    s1 = nbttagcompound2.getString("id");
                }

                boolean flag;

                if (s1 == null) {
                    DataInspectorEntity.a.warn("Unable to resolve Entity for ItemInstance: {}", s);
                    flag = false;
                } else {
                    flag = !nbttagcompound2.hasKeyOfType("id", 8);
                    nbttagcompound2.setString("id", s1);
                }

                convert(LegacyType.ENTITY, nbttagcompound2, sourceVer, targetVer);
                if (flag) {
                    nbttagcompound2.remove("id");
                }
            }

            return cmp;
        }
    }


    private abstract static class DataInspectorTagged implements DataInspector {

        private final MinecraftKey key;

        DataInspectorTagged(String type) {
            this.key = getKey(type);
        }

        public NBTTagCompound inspect(NBTTagCompound cmp, int sourceVer, int targetVer) {
            if (this.key.equals(new MinecraftKey(cmp.getString("id")))) {
                cmp = this.inspectChecked(cmp, sourceVer, targetVer);
            }

            return cmp;
        }

        abstract NBTTagCompound inspectChecked(NBTTagCompound nbttagcompound, int sourceVer, int targetVer);
    }

    private static class DataInspectorItemList extends DataInspectorTagged {

        private final String[] keys;

        DataInspectorItemList(String oclass, String... astring) {
            super(oclass);
            this.keys = astring;
        }

        NBTTagCompound inspectChecked(NBTTagCompound nbttagcompound, int sourceVer, int targetVer) {
            for (String s : this.keys) {
                DataConverters_1_14_R4.convertItems(nbttagcompound, s, sourceVer, targetVer);
            }

            return nbttagcompound;
        }
    }
    private static class DataInspectorItem extends DataInspectorTagged {

        private final String[] keys;

        DataInspectorItem(String oclass, String... astring) {
            super(oclass);
            this.keys = astring;
        }

        NBTTagCompound inspectChecked(NBTTagCompound nbttagcompound, int sourceVer, int targetVer) {
            for (String key : this.keys) {
                DataConverters_1_14_R4.convertItem(nbttagcompound, key, sourceVer, targetVer);
            }

            return nbttagcompound;
        }
    }

    private static class DataConverterMaterialId implements DataConverter {

        private static final String[] materials = new String[2268];

        DataConverterMaterialId() {}

        public int getDataVersion() {
            return 102;
        }

        public NBTTagCompound convert(NBTTagCompound cmp) {
            if (cmp.hasKeyOfType("id", 99)) {
                short short0 = cmp.getShort("id");

                if (short0 > 0 && short0 < materials.length && materials[short0] != null) {
                    cmp.setString("id", materials[short0]);
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
        }
    }

    private static class DataConverterArmorStand implements DataConverter {

        DataConverterArmorStand() {}

        public int getDataVersion() {
            return 147;
        }

        public NBTTagCompound convert(NBTTagCompound cmp) {
            if ("ArmorStand".equals(cmp.getString("id")) && cmp.getBoolean("Silent") && !cmp.getBoolean("Marker")) {
                cmp.remove("Silent");
            }

            return cmp;
        }
    }

    private static class DataConverterBanner implements DataConverter {

        DataConverterBanner() {}

        public int getDataVersion() {
            return 804;
        }

        public NBTTagCompound convert(NBTTagCompound cmp) {
            if ("minecraft:banner".equals(cmp.getString("id")) && cmp.hasKeyOfType("tag", 10)) {
                NBTTagCompound nbttagcompound1 = cmp.getCompound("tag");

                if (nbttagcompound1.hasKeyOfType("BlockEntityTag", 10)) {
                    NBTTagCompound nbttagcompound2 = nbttagcompound1.getCompound("BlockEntityTag");

                    if (nbttagcompound2.hasKeyOfType("Base", 99)) {
                        cmp.setShort("Damage", (short) (nbttagcompound2.getShort("Base") & 15));
                        if (nbttagcompound1.hasKeyOfType("display", 10)) {
                            NBTTagCompound nbttagcompound3 = nbttagcompound1.getCompound("display");

                            if (nbttagcompound3.hasKeyOfType("Lore", 9)) {
                                NBTTagList nbttaglist = nbttagcompound3.getList("Lore", 8);

                                if (nbttaglist.size() == 1 && "(+NBT)".equals(nbttaglist.getString(0))) {
                                    return cmp;
                                }
                            }
                        }

                        nbttagcompound2.remove("Base");
                        if (nbttagcompound2.isEmpty()) {
                            nbttagcompound1.remove("BlockEntityTag");
                        }

                        if (nbttagcompound1.isEmpty()) {
                            cmp.remove("tag");
                        }
                    }
                }
            }

            return cmp;
        }
    }

    private static class DataConverterPotionId implements DataConverter {

        private static final String[] potions = new String[128];

        DataConverterPotionId() {}

        public int getDataVersion() {
            return 102;
        }

        public NBTTagCompound convert(NBTTagCompound cmp) {
            if ("minecraft:potion".equals(cmp.getString("id"))) {
                NBTTagCompound nbttagcompound1 = cmp.getCompound("tag");
                short short0 = cmp.getShort("Damage");

                if (!nbttagcompound1.hasKeyOfType("Potion", 8)) {
                    String s = DataConverterPotionId.potions[short0 & 127];

                    nbttagcompound1.setString("Potion", s == null ? "minecraft:water" : s);
                    cmp.set("tag", nbttagcompound1);
                    if ((short0 & 16384) == 16384) {
                        cmp.setString("id", "minecraft:splash_potion");
                    }
                }

                if (short0 != 0) {
                    cmp.setShort("Damage", (short) 0);
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

    private static class DataConverterSpawnEgg implements DataConverter {

        private static final String[] eggs = new String[256];

        DataConverterSpawnEgg() {}

        public int getDataVersion() {
            return 105;
        }

        public NBTTagCompound convert(NBTTagCompound cmp) {
            if ("minecraft:spawn_egg".equals(cmp.getString("id"))) {
                NBTTagCompound nbttagcompound1 = cmp.getCompound("tag");
                NBTTagCompound nbttagcompound2 = nbttagcompound1.getCompound("EntityTag");
                short short0 = cmp.getShort("Damage");

                if (!nbttagcompound2.hasKeyOfType("id", 8)) {
                    String s = DataConverterSpawnEgg.eggs[short0 & 255];

                    if (s != null) {
                        nbttagcompound2.setString("id", s);
                        nbttagcompound1.set("EntityTag", nbttagcompound2);
                        cmp.set("tag", nbttagcompound1);
                    }
                }

                if (short0 != 0) {
                    cmp.setShort("Damage", (short) 0);
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

    private static class DataConverterMinecart implements DataConverter {

        private static final List<String> a = Lists.newArrayList(new String[] { "MinecartRideable", "MinecartChest", "MinecartFurnace", "MinecartTNT", "MinecartSpawner", "MinecartHopper", "MinecartCommandBlock"});

        DataConverterMinecart() {}

        public int getDataVersion() {
            return 106;
        }

        public NBTTagCompound convert(NBTTagCompound cmp) {
            if ("Minecart".equals(cmp.getString("id"))) {
                String s = "MinecartRideable";
                int i = cmp.getInt("Type");

                if (i > 0 && i < DataConverterMinecart.a.size()) {
                    s = DataConverterMinecart.a.get(i);
                }

                cmp.setString("id", s);
                cmp.remove("Type");
            }

            return cmp;
        }
    }

    private static class DataConverterMobSpawner implements DataConverter {

        DataConverterMobSpawner() {}

        public int getDataVersion() {
            return 107;
        }

        public NBTTagCompound convert(NBTTagCompound cmp) {
            if (!"MobSpawner".equals(cmp.getString("id"))) {
                return cmp;
            } else {
                if (cmp.hasKeyOfType("EntityId", 8)) {
                    String s = cmp.getString("EntityId");
                    NBTTagCompound nbttagcompound1 = cmp.getCompound("SpawnData");

                    nbttagcompound1.setString("id", s.isEmpty() ? "Pig" : s);
                    cmp.set("SpawnData", nbttagcompound1);
                    cmp.remove("EntityId");
                }

                if (cmp.hasKeyOfType("SpawnPotentials", 9)) {
                    NBTTagList nbttaglist = cmp.getList("SpawnPotentials", 10);

                    for (int i = 0; i < nbttaglist.size(); ++i) {
                        NBTTagCompound nbttagcompound2 = nbttaglist.getCompound(i);

                        if (nbttagcompound2.hasKeyOfType("Type", 8)) {
                            NBTTagCompound nbttagcompound3 = nbttagcompound2.getCompound("Properties");

                            nbttagcompound3.setString("id", nbttagcompound2.getString("Type"));
                            nbttagcompound2.set("Entity", nbttagcompound3);
                            nbttagcompound2.remove("Type");
                            nbttagcompound2.remove("Properties");
                        }
                    }
                }

                return cmp;
            }
        }
    }

    private static class DataConverterUUID implements DataConverter {

        DataConverterUUID() {}

        public int getDataVersion() {
            return 108;
        }

        public NBTTagCompound convert(NBTTagCompound cmp) {
            if (cmp.hasKeyOfType("UUID", 8)) {
                cmp.a("UUID", UUID.fromString(cmp.getString("UUID")));
            }

            return cmp;
        }
    }

    private static class DataConverterHealth implements DataConverter {

        private static final Set<String> a = Sets.newHashSet(new String[] { "ArmorStand", "Bat", "Blaze", "CaveSpider", "Chicken", "Cow", "Creeper", "EnderDragon", "Enderman", "Endermite", "EntityHorse", "Ghast", "Giant", "Guardian", "LavaSlime", "MushroomCow", "Ozelot", "Pig", "PigZombie", "Rabbit", "Sheep", "Shulker", "Silverfish", "Skeleton", "Slime", "SnowMan", "Spider", "Squid", "Villager", "VillagerGolem", "Witch", "WitherBoss", "Wolf", "Zombie"});

        DataConverterHealth() {}

        public int getDataVersion() {
            return 109;
        }

        public NBTTagCompound convert(NBTTagCompound cmp) {
            if (DataConverterHealth.a.contains(cmp.getString("id"))) {
                float f;

                if (cmp.hasKeyOfType("HealF", 99)) {
                    f = cmp.getFloat("HealF");
                    cmp.remove("HealF");
                } else {
                    if (!cmp.hasKeyOfType("Health", 99)) {
                        return cmp;
                    }

                    f = cmp.getFloat("Health");
                }

                cmp.setFloat("Health", f);
            }

            return cmp;
        }
    }

    private static class DataConverterSaddle implements DataConverter {

        DataConverterSaddle() {}

        public int getDataVersion() {
            return 110;
        }

        public NBTTagCompound convert(NBTTagCompound cmp) {
            if ("EntityHorse".equals(cmp.getString("id")) && !cmp.hasKeyOfType("SaddleItem", 10) && cmp.getBoolean("Saddle")) {
                NBTTagCompound nbttagcompound1 = new NBTTagCompound();

                nbttagcompound1.setString("id", "minecraft:saddle");
                nbttagcompound1.setByte("Count", (byte) 1);
                nbttagcompound1.setShort("Damage", (short) 0);
                cmp.set("SaddleItem", nbttagcompound1);
                cmp.remove("Saddle");
            }

            return cmp;
        }
    }

    private static class DataConverterHanging implements DataConverter {

        DataConverterHanging() {}

        public int getDataVersion() {
            return 111;
        }

        public NBTTagCompound convert(NBTTagCompound cmp) {
            String s = cmp.getString("id");
            boolean flag = "Painting".equals(s);
            boolean flag1 = "ItemFrame".equals(s);

            if ((flag || flag1) && !cmp.hasKeyOfType("Facing", 99)) {
                EnumDirection enumdirection;

                if (cmp.hasKeyOfType("Direction", 99)) {
                    enumdirection = EnumDirection.fromType2(cmp.getByte("Direction"));
                    cmp.setInt("TileX", cmp.getInt("TileX") + enumdirection.getAdjacentX());
                    cmp.setInt("TileY", cmp.getInt("TileY") + enumdirection.getAdjacentY());
                    cmp.setInt("TileZ", cmp.getInt("TileZ") + enumdirection.getAdjacentZ());
                    cmp.remove("Direction");
                    if (flag1 && cmp.hasKeyOfType("ItemRotation", 99)) {
                        cmp.setByte("ItemRotation", (byte) (cmp.getByte("ItemRotation") * 2));
                    }
                } else {
                    enumdirection = EnumDirection.fromType2(cmp.getByte("Dir"));
                    cmp.remove("Dir");
                }

                cmp.setByte("Facing", (byte) enumdirection.get2DRotationValue());
            }

            return cmp;
        }
    }

    private static class DataConverterDropChances implements DataConverter {

        DataConverterDropChances() {}

        public int getDataVersion() {
            return 113;
        }

        public NBTTagCompound convert(NBTTagCompound cmp) {
            NBTTagList nbttaglist;

            if (cmp.hasKeyOfType("HandDropChances", 9)) {
                nbttaglist = cmp.getList("HandDropChances", 5);
                if (nbttaglist.size() == 2 && nbttaglist.i(0) == 0.0F && nbttaglist.i(1) == 0.0F) {
                    cmp.remove("HandDropChances");
                }
            }

            if (cmp.hasKeyOfType("ArmorDropChances", 9)) {
                nbttaglist = cmp.getList("ArmorDropChances", 5);
                if (nbttaglist.size() == 4 && nbttaglist.i(0) == 0.0F && nbttaglist.i(1) == 0.0F && nbttaglist.i(2) == 0.0F && nbttaglist.i(3) == 0.0F) {
                    cmp.remove("ArmorDropChances");
                }
            }

            return cmp;
        }
    }

    private static class DataConverterRiding implements DataConverter {

        DataConverterRiding() {}

        public int getDataVersion() {
            return 135;
        }

        public NBTTagCompound convert(NBTTagCompound cmp) {
            while (cmp.hasKeyOfType("Riding", 10)) {
                NBTTagCompound nbttagcompound1 = this.b(cmp);

                this.convert(cmp, nbttagcompound1);
                cmp = nbttagcompound1;
            }

            return cmp;
        }

        protected void convert(NBTTagCompound nbttagcompound, NBTTagCompound nbttagcompound1) {
            NBTTagList nbttaglist = new NBTTagList();

            nbttaglist.add(nbttagcompound);
            nbttagcompound1.set("Passengers", nbttaglist);
        }

        protected NBTTagCompound b(NBTTagCompound nbttagcompound) {
            NBTTagCompound nbttagcompound1 = nbttagcompound.getCompound("Riding");

            nbttagcompound.remove("Riding");
            return nbttagcompound1;
        }
    }

    private static class DataConverterBook implements DataConverter {

        DataConverterBook() {}

        public int getDataVersion() {
            return 165;
        }

        public NBTTagCompound convert(NBTTagCompound cmp) {
            if ("minecraft:written_book".equals(cmp.getString("id"))) {
                NBTTagCompound nbttagcompound1 = cmp.getCompound("tag");

                if (nbttagcompound1.hasKeyOfType("pages", 9)) {
                    NBTTagList nbttaglist = nbttagcompound1.getList("pages", 8);

                    for (int i = 0; i < nbttaglist.size(); ++i) {
                        String s = nbttaglist.getString(i);
                        Object object = null;

                        if (!"null".equals(s) && !UtilColor.b(s)) {
                            if ((s.charAt(0) != 34 || s.charAt(s.length() - 1) != 34) && (s.charAt(0) != 123 || s.charAt(s.length() - 1) != 125)) {
                                object = new ChatComponentText(s);
                            } else {
                                try {
                                    object = ChatDeserializer.a(DataConverterSignText.a, s, IChatBaseComponent.class, true);
                                    if (object == null) {
                                        object = new ChatComponentText("");
                                    }
                                } catch (JsonParseException jsonparseexception) {
                                    ;
                                }

                                if (object == null) {
                                    try {
                                        object = IChatBaseComponent.ChatSerializer.a(s);
                                    } catch (JsonParseException jsonparseexception1) {
                                        ;
                                    }
                                }

                                if (object == null) {
                                    try {
                                        object = IChatBaseComponent.ChatSerializer.b(s);
                                    } catch (JsonParseException jsonparseexception2) {
                                        ;
                                    }
                                }

                                if (object == null) {
                                    object = new ChatComponentText(s);
                                }
                            }
                        } else {
                            object = new ChatComponentText("");
                        }

                        nbttaglist.set(i, new NBTTagString(IChatBaseComponent.ChatSerializer.a((IChatBaseComponent) object)));
                    }

                    nbttagcompound1.set("pages", nbttaglist);
                }
            }

            return cmp;
        }
    }

    private static class DataConverterCookedFish implements DataConverter {

        private static final MinecraftKey a = new MinecraftKey("cooked_fished");

        DataConverterCookedFish() {}

        public int getDataVersion() {
            return 502;
        }

        public NBTTagCompound convert(NBTTagCompound cmp) {
            if (cmp.hasKeyOfType("id", 8) && DataConverterCookedFish.a.equals(new MinecraftKey(cmp.getString("id")))) {
                cmp.setString("id", "minecraft:cooked_fish");
            }

            return cmp;
        }
    }

    private static class DataConverterZombie implements DataConverter {

        private static final Random a = new Random();

        DataConverterZombie() {}

        public int getDataVersion() {
            return 502;
        }

        public NBTTagCompound convert(NBTTagCompound cmp) {
            if ("Zombie".equals(cmp.getString("id")) && cmp.getBoolean("IsVillager")) {
                if (!cmp.hasKeyOfType("ZombieType", 99)) {
                    int i = -1;

                    if (cmp.hasKeyOfType("VillagerProfession", 99)) {
                        try {
                            i = this.convert(cmp.getInt("VillagerProfession"));
                        } catch (RuntimeException runtimeexception) {
                            ;
                        }
                    }

                    if (i == -1) {
                        i = this.convert(DataConverterZombie.a.nextInt(6));
                    }

                    cmp.setInt("ZombieType", i);
                }

                cmp.remove("IsVillager");
            }

            return cmp;
        }

        private int convert(int i) {
            return i >= 0 && i < 6 ? i : -1;
        }
    }

    private static class DataConverterVBO implements DataConverter {

        DataConverterVBO() {}

        public int getDataVersion() {
            return 505;
        }

        public NBTTagCompound convert(NBTTagCompound cmp) {
            cmp.setString("useVbo", "true");
            return cmp;
        }
    }

    private static class DataConverterGuardian implements DataConverter {

        DataConverterGuardian() {}

        public int getDataVersion() {
            return 700;
        }

        public NBTTagCompound convert(NBTTagCompound cmp) {
            if ("Guardian".equals(cmp.getString("id"))) {
                if (cmp.getBoolean("Elder")) {
                    cmp.setString("id", "ElderGuardian");
                }

                cmp.remove("Elder");
            }

            return cmp;
        }
    }

    private static class DataConverterSkeleton implements DataConverter {

        DataConverterSkeleton() {}

        public int getDataVersion() {
            return 701;
        }

        public NBTTagCompound convert(NBTTagCompound cmp) {
            String s = cmp.getString("id");

            if ("Skeleton".equals(s)) {
                int i = cmp.getInt("SkeletonType");

                if (i == 1) {
                    cmp.setString("id", "WitherSkeleton");
                } else if (i == 2) {
                    cmp.setString("id", "Stray");
                }

                cmp.remove("SkeletonType");
            }

            return cmp;
        }
    }

    private static class DataConverterZombieType implements DataConverter {

        DataConverterZombieType() {}

        public int getDataVersion() {
            return 702;
        }

        public NBTTagCompound convert(NBTTagCompound cmp) {
            if ("Zombie".equals(cmp.getString("id"))) {
                int i = cmp.getInt("ZombieType");

                switch (i) {
                    case 0:
                    default:
                        break;

                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                        cmp.setString("id", "ZombieVillager");
                        cmp.setInt("Profession", i - 1);
                        break;

                    case 6:
                        cmp.setString("id", "Husk");
                }

                cmp.remove("ZombieType");
            }

            return cmp;
        }
    }

    private static class DataConverterHorse implements DataConverter {

        DataConverterHorse() {}

        public int getDataVersion() {
            return 703;
        }

        public NBTTagCompound convert(NBTTagCompound cmp) {
            if ("EntityHorse".equals(cmp.getString("id"))) {
                int i = cmp.getInt("Type");

                switch (i) {
                    case 0:
                    default:
                        cmp.setString("id", "Horse");
                        break;

                    case 1:
                        cmp.setString("id", "Donkey");
                        break;

                    case 2:
                        cmp.setString("id", "Mule");
                        break;

                    case 3:
                        cmp.setString("id", "ZombieHorse");
                        break;

                    case 4:
                        cmp.setString("id", "SkeletonHorse");
                }

                cmp.remove("Type");
            }

            return cmp;
        }
    }

    private static class DataConverterTileEntity implements DataConverter {

        private static final Map<String, String> a = Maps.newHashMap();

        DataConverterTileEntity() {}

        public int getDataVersion() {
            return 704;
        }

        public NBTTagCompound convert(NBTTagCompound cmp) {
            String s = DataConverterTileEntity.a.get(cmp.getString("id"));

            if (s != null) {
                cmp.setString("id", s);
            }

            return cmp;
        }

        static {
            DataConverterTileEntity.a.put("Airportal", "minecraft:end_portal");
            DataConverterTileEntity.a.put("Banner", "minecraft:banner");
            DataConverterTileEntity.a.put("Beacon", "minecraft:beacon");
            DataConverterTileEntity.a.put("Cauldron", "minecraft:brewing_stand");
            DataConverterTileEntity.a.put("Chest", "minecraft:chest");
            DataConverterTileEntity.a.put("Comparator", "minecraft:comparator");
            DataConverterTileEntity.a.put("Control", "minecraft:command_block");
            DataConverterTileEntity.a.put("DLDetector", "minecraft:daylight_detector");
            DataConverterTileEntity.a.put("Dropper", "minecraft:dropper");
            DataConverterTileEntity.a.put("EnchantTable", "minecraft:enchanting_table");
            DataConverterTileEntity.a.put("EndGateway", "minecraft:end_gateway");
            DataConverterTileEntity.a.put("EnderChest", "minecraft:ender_chest");
            DataConverterTileEntity.a.put("FlowerPot", "minecraft:flower_pot");
            DataConverterTileEntity.a.put("Furnace", "minecraft:furnace");
            DataConverterTileEntity.a.put("Hopper", "minecraft:hopper");
            DataConverterTileEntity.a.put("MobSpawner", "minecraft:mob_spawner");
            DataConverterTileEntity.a.put("Music", "minecraft:noteblock");
            DataConverterTileEntity.a.put("Piston", "minecraft:piston");
            DataConverterTileEntity.a.put("RecordPlayer", "minecraft:jukebox");
            DataConverterTileEntity.a.put("Sign", "minecraft:sign");
            DataConverterTileEntity.a.put("Skull", "minecraft:skull");
            DataConverterTileEntity.a.put("Structure", "minecraft:structure_block");
            DataConverterTileEntity.a.put("Trap", "minecraft:dispenser");
        }
    }

    private static class DataConverterEntity implements DataConverter {

        private static final Map<String, String> a = Maps.newHashMap();

        DataConverterEntity() {}

        public int getDataVersion() {
            return 704;
        }

        public NBTTagCompound convert(NBTTagCompound cmp) {
            String s = DataConverterEntity.a.get(cmp.getString("id"));

            if (s != null) {
                cmp.setString("id", s);
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

    private static class DataConverterPotionWater implements DataConverter {

        DataConverterPotionWater() {}

        public int getDataVersion() {
            return 806;
        }

        public NBTTagCompound convert(NBTTagCompound cmp) {
            String s = cmp.getString("id");

            if ("minecraft:potion".equals(s) || "minecraft:splash_potion".equals(s) || "minecraft:lingering_potion".equals(s) || "minecraft:tipped_arrow".equals(s)) {
                NBTTagCompound nbttagcompound1 = cmp.getCompound("tag");

                if (!nbttagcompound1.hasKeyOfType("Potion", 8)) {
                    nbttagcompound1.setString("Potion", "minecraft:water");
                }

                if (!cmp.hasKeyOfType("tag", 10)) {
                    cmp.set("tag", nbttagcompound1);
                }
            }

            return cmp;
        }
    }

    private static class DataConverterShulker implements DataConverter {

        DataConverterShulker() {}

        public int getDataVersion() {
            return 808;
        }

        public NBTTagCompound convert(NBTTagCompound cmp) {
            if ("minecraft:shulker".equals(cmp.getString("id")) && !cmp.hasKeyOfType("Color", 99)) {
                cmp.setByte("Color", (byte) 10);
            }

            return cmp;
        }
    }

    private static class DataConverterShulkerBoxItem implements DataConverter {

        public static final String[] a = new String[] { "minecraft:white_shulker_box", "minecraft:orange_shulker_box", "minecraft:magenta_shulker_box", "minecraft:light_blue_shulker_box", "minecraft:yellow_shulker_box", "minecraft:lime_shulker_box", "minecraft:pink_shulker_box", "minecraft:gray_shulker_box", "minecraft:silver_shulker_box", "minecraft:cyan_shulker_box", "minecraft:purple_shulker_box", "minecraft:blue_shulker_box", "minecraft:brown_shulker_box", "minecraft:green_shulker_box", "minecraft:red_shulker_box", "minecraft:black_shulker_box"};

        DataConverterShulkerBoxItem() {}

        public int getDataVersion() {
            return 813;
        }

        public NBTTagCompound convert(NBTTagCompound cmp) {
            if ("minecraft:shulker_box".equals(cmp.getString("id")) && cmp.hasKeyOfType("tag", 10)) {
                NBTTagCompound nbttagcompound1 = cmp.getCompound("tag");

                if (nbttagcompound1.hasKeyOfType("BlockEntityTag", 10)) {
                    NBTTagCompound nbttagcompound2 = nbttagcompound1.getCompound("BlockEntityTag");

                    if (nbttagcompound2.getList("Items", 10).isEmpty()) {
                        nbttagcompound2.remove("Items");
                    }

                    int i = nbttagcompound2.getInt("Color");

                    nbttagcompound2.remove("Color");
                    if (nbttagcompound2.isEmpty()) {
                        nbttagcompound1.remove("BlockEntityTag");
                    }

                    if (nbttagcompound1.isEmpty()) {
                        cmp.remove("tag");
                    }

                    cmp.setString("id", DataConverterShulkerBoxItem.a[i % 16]);
                }
            }

            return cmp;
        }
    }

    private static class DataConverterShulkerBoxBlock implements DataConverter {

        DataConverterShulkerBoxBlock() {}

        public int getDataVersion() {
            return 813;
        }

        public NBTTagCompound convert(NBTTagCompound cmp) {
            if ("minecraft:shulker".equals(cmp.getString("id"))) {
                cmp.remove("Color");
            }

            return cmp;
        }
    }

    private static class DataConverterLang implements DataConverter {

        DataConverterLang() {}

        public int getDataVersion() {
            return 816;
        }

        public NBTTagCompound convert(NBTTagCompound cmp) {
            if (cmp.hasKeyOfType("lang", 8)) {
                cmp.setString("lang", cmp.getString("lang").toLowerCase(Locale.ROOT));
            }

            return cmp;
        }
    }

    private static class DataConverterTotem implements DataConverter {

        DataConverterTotem() {}

        public int getDataVersion() {
            return 820;
        }

        public NBTTagCompound convert(NBTTagCompound cmp) {
            if ("minecraft:totem".equals(cmp.getString("id"))) {
                cmp.setString("id", "minecraft:totem_of_undying");
            }

            return cmp;
        }
    }

    private static class DataConverterBedBlock implements DataConverter {

        private static final Logger a = LogManager.getLogger(DataConverters_1_14_R4.class);

        DataConverterBedBlock() {}

        public int getDataVersion() {
            return 1125;
        }

        public NBTTagCompound convert(NBTTagCompound cmp) {
            boolean flag = true;

            try {
                NBTTagCompound nbttagcompound1 = cmp.getCompound("Level");
                int i = nbttagcompound1.getInt("xPos");
                int j = nbttagcompound1.getInt("zPos");
                NBTTagList nbttaglist = nbttagcompound1.getList("TileEntities", 10);
                NBTTagList nbttaglist1 = nbttagcompound1.getList("Sections", 10);

                for (int k = 0; k < nbttaglist1.size(); ++k) {
                    NBTTagCompound nbttagcompound2 = nbttaglist1.getCompound(k);
                    byte b0 = nbttagcompound2.getByte("Y");
                    byte[] abyte = nbttagcompound2.getByteArray("Blocks");

                    for (int l = 0; l < abyte.length; ++l) {
                        if (416 == (abyte[l] & 255) << 4) {
                            int i1 = l & 15;
                            int j1 = l >> 8 & 15;
                            int k1 = l >> 4 & 15;
                            NBTTagCompound nbttagcompound3 = new NBTTagCompound();

                            nbttagcompound3.setString("id", "bed");
                            nbttagcompound3.setInt("x", i1 + (i << 4));
                            nbttagcompound3.setInt("y", j1 + (b0 << 4));
                            nbttagcompound3.setInt("z", k1 + (j << 4));
                            nbttaglist.add(nbttagcompound3);
                        }
                    }
                }
            } catch (Exception exception) {
                DataConverterBedBlock.a.warn("Unable to datafix Bed blocks, level format may be missing tags.");
            }

            return cmp;
        }
    }

    private static class DataConverterBedItem implements DataConverter {

        DataConverterBedItem() {}

        public int getDataVersion() {
            return 1125;
        }

        public NBTTagCompound convert(NBTTagCompound cmp) {
            if ("minecraft:bed".equals(cmp.getString("id")) && cmp.getShort("Damage") == 0) {
                cmp.setShort("Damage", (short) EnumColor.RED.getColorIndex());
            }

            return cmp;
        }
    }

    private static class DataConverterSignText implements DataConverter {

        public static final Gson a = new GsonBuilder().registerTypeAdapter(IChatBaseComponent.class, new JsonDeserializer() {
            IChatBaseComponent a(JsonElement jsonelement, Type type, JsonDeserializationContext jsondeserializationcontext) throws JsonParseException {
                if (jsonelement.isJsonPrimitive()) {
                    return new ChatComponentText(jsonelement.getAsString());
                } else if (jsonelement.isJsonArray()) {
                    JsonArray jsonarray = jsonelement.getAsJsonArray();
                    IChatBaseComponent ichatbasecomponent = null;
                    Iterator iterator = jsonarray.iterator();

                    while (iterator.hasNext()) {
                        JsonElement jsonelement1 = (JsonElement) iterator.next();
                        IChatBaseComponent ichatbasecomponent1 = this.a(jsonelement1, jsonelement1.getClass(), jsondeserializationcontext);

                        if (ichatbasecomponent == null) {
                            ichatbasecomponent = ichatbasecomponent1;
                        } else {
                            ichatbasecomponent.addSibling(ichatbasecomponent1);
                        }
                    }

                    return ichatbasecomponent;
                } else {
                    throw new JsonParseException("Don\'t know how to turn " + jsonelement + " into a Component");
                }
            }

            public Object deserialize(JsonElement jsonelement, Type type, JsonDeserializationContext jsondeserializationcontext) throws JsonParseException {
                return this.a(jsonelement, type, jsondeserializationcontext);
            }
        }).create();

        DataConverterSignText() {}

        public int getDataVersion() {
            return 101;
        }

        public NBTTagCompound convert(NBTTagCompound cmp) {
            if ("Sign".equals(cmp.getString("id"))) {
                this.convert(cmp, "Text1");
                this.convert(cmp, "Text2");
                this.convert(cmp, "Text3");
                this.convert(cmp, "Text4");
            }

            return cmp;
        }

        private void convert(NBTTagCompound nbttagcompound, String s) {
            String s1 = nbttagcompound.getString(s);
            Object object = null;

            if (!"null".equals(s1) && !UtilColor.b(s1)) {
                if ((s1.charAt(0) != 34 || s1.charAt(s1.length() - 1) != 34) && (s1.charAt(0) != 123 || s1.charAt(s1.length() - 1) != 125)) {
                    object = new ChatComponentText(s1);
                } else {
                    try {
                        object = ChatDeserializer.a(DataConverterSignText.a, s1, IChatBaseComponent.class, true);
                        if (object == null) {
                            object = new ChatComponentText("");
                        }
                    } catch (JsonParseException jsonparseexception) {
                        ;
                    }

                    if (object == null) {
                        try {
                            object = IChatBaseComponent.ChatSerializer.a(s1);
                        } catch (JsonParseException jsonparseexception1) {
                            ;
                        }
                    }

                    if (object == null) {
                        try {
                            object = IChatBaseComponent.ChatSerializer.b(s1);
                        } catch (JsonParseException jsonparseexception2) {
                            ;
                        }
                    }

                    if (object == null) {
                        object = new ChatComponentText(s1);
                    }
                }
            } else {
                object = new ChatComponentText("");
            }

            nbttagcompound.setString(s, IChatBaseComponent.ChatSerializer.a((IChatBaseComponent) object));
        }
    }

    private static class DataInspectorPlayerVehicle implements DataInspector {
        @Override
        public NBTTagCompound inspect(NBTTagCompound cmp, int sourceVer, int targetVer) {
            if (cmp.hasKeyOfType("RootVehicle", 10)) {
                NBTTagCompound nbttagcompound1 = cmp.getCompound("RootVehicle");

                if (nbttagcompound1.hasKeyOfType("Entity", 10)) {
                    convertCompound(LegacyType.ENTITY, nbttagcompound1, "Entity", sourceVer, targetVer);
                }
            }

            return cmp;
        }
    }

    private static class DataInspectorLevelPlayer implements DataInspector {
        @Override
        public NBTTagCompound inspect(NBTTagCompound cmp, int sourceVer, int targetVer) {
            if (cmp.hasKeyOfType("Player", 10)) {
                convertCompound(LegacyType.PLAYER, cmp, "Player", sourceVer, targetVer);
            }

            return cmp;
        }
    }

    private static class DataInspectorStructure implements DataInspector {
        @Override
        public NBTTagCompound inspect(NBTTagCompound cmp, int sourceVer, int targetVer) {
            NBTTagList nbttaglist;
            int j;
            NBTTagCompound nbttagcompound1;

            if (cmp.hasKeyOfType("entities", 9)) {
                nbttaglist = cmp.getList("entities", 10);

                for (j = 0; j < nbttaglist.size(); ++j) {
                    nbttagcompound1 = (NBTTagCompound) nbttaglist.get(j);
                    if (nbttagcompound1.hasKeyOfType("nbt", 10)) {
                        convertCompound(LegacyType.ENTITY, nbttagcompound1, "nbt", sourceVer, targetVer);
                    }
                }
            }

            if (cmp.hasKeyOfType("blocks", 9)) {
                nbttaglist = cmp.getList("blocks", 10);

                for (j = 0; j < nbttaglist.size(); ++j) {
                    nbttagcompound1 = (NBTTagCompound) nbttaglist.get(j);
                    if (nbttagcompound1.hasKeyOfType("nbt", 10)) {
                        convertCompound(LegacyType.BLOCK_ENTITY, nbttagcompound1, "nbt", sourceVer, targetVer);
                    }
                }
            }

            return cmp;
        }
    }

    private static class DataInspectorChunks implements DataInspector {
        @Override
        public NBTTagCompound inspect(NBTTagCompound cmp, int sourceVer, int targetVer) {
            if (cmp.hasKeyOfType("Level", 10)) {
                NBTTagCompound nbttagcompound1 = cmp.getCompound("Level");
                NBTTagList nbttaglist;
                int j;

                if (nbttagcompound1.hasKeyOfType("Entities", 9)) {
                    nbttaglist = nbttagcompound1.getList("Entities", 10);

                    for (j = 0; j < nbttaglist.size(); ++j) {
                        nbttaglist.set(j, convert(LegacyType.ENTITY, (NBTTagCompound) nbttaglist.get(j), sourceVer, targetVer));
                    }
                }

                if (nbttagcompound1.hasKeyOfType("TileEntities", 9)) {
                    nbttaglist = nbttagcompound1.getList("TileEntities", 10);

                    for (j = 0; j < nbttaglist.size(); ++j) {
                        nbttaglist.set(j, convert(LegacyType.BLOCK_ENTITY, (NBTTagCompound) nbttaglist.get(j), sourceVer, targetVer));
                    }
                }
            }

            return cmp;
        }
    }

    private static class DataInspectorEntityPassengers implements DataInspector {
        @Override
        public NBTTagCompound inspect(NBTTagCompound cmp, int sourceVer, int targetVer) {
            if (cmp.hasKeyOfType("Passengers", 9)) {
                NBTTagList nbttaglist = cmp.getList("Passengers", 10);

                for (int j = 0; j < nbttaglist.size(); ++j) {
                    nbttaglist.set(j, convert(LegacyType.ENTITY, nbttaglist.getCompound(j), sourceVer, targetVer));
                }
            }

            return cmp;
        }
    }

    private static class DataInspectorPlayer implements DataInspector {
        @Override
        public NBTTagCompound inspect(NBTTagCompound cmp, int sourceVer, int targetVer) {
            convertItems(cmp, "Inventory", sourceVer, targetVer);
            convertItems(cmp, "EnderItems", sourceVer, targetVer);
            if (cmp.hasKeyOfType("ShoulderEntityLeft", 10)) {
                convertCompound(LegacyType.ENTITY, cmp, "ShoulderEntityLeft", sourceVer, targetVer);
            }

            if (cmp.hasKeyOfType("ShoulderEntityRight", 10)) {
                convertCompound(LegacyType.ENTITY, cmp, "ShoulderEntityRight", sourceVer, targetVer);
            }

            return cmp;
        }
    }

    private static class DataInspectorVillagers implements DataInspector {
        MinecraftKey entityVillager = getKey("EntityVillager");

        @Override
        public NBTTagCompound inspect(NBTTagCompound cmp, int sourceVer, int targetVer) {
            if (entityVillager.equals(new MinecraftKey(cmp.getString("id"))) && cmp.hasKeyOfType("Offers", 10)) {
                NBTTagCompound nbttagcompound1 = cmp.getCompound("Offers");

                if (nbttagcompound1.hasKeyOfType("Recipes", 9)) {
                    NBTTagList nbttaglist = nbttagcompound1.getList("Recipes", 10);

                    for (int j = 0; j < nbttaglist.size(); ++j) {
                        NBTTagCompound nbttagcompound2 = nbttaglist.getCompound(j);

                        convertItem(nbttagcompound2, "buy", sourceVer, targetVer);
                        convertItem(nbttagcompound2, "buyB", sourceVer, targetVer);
                        convertItem(nbttagcompound2, "sell", sourceVer, targetVer);
                        nbttaglist.set(j, nbttagcompound2);
                    }
                }
            }

            return cmp;
        }
    }

    private static class DataInspectorMobSpawnerMinecart implements DataInspector {
        MinecraftKey entityMinecartMobSpawner = getKey("EntityMinecartMobSpawner");
        MinecraftKey tileEntityMobSpawner = getKey("TileEntityMobSpawner");

        @Override
        public NBTTagCompound inspect(NBTTagCompound cmp, int sourceVer, int targetVer) {
            String s = cmp.getString("id");
            if (entityMinecartMobSpawner.equals(new MinecraftKey(s))) {
                cmp.setString("id", tileEntityMobSpawner.toString());
                convert(LegacyType.BLOCK_ENTITY, cmp, sourceVer, targetVer);
                cmp.setString("id", s);
            }

            return cmp;
        }
    }

    private static class DataInspectorMobSpawnerMobs implements DataInspector {
        MinecraftKey tileEntityMobSpawner = getKey("TileEntityMobSpawner");

        @Override
        public NBTTagCompound inspect(NBTTagCompound cmp, int sourceVer, int targetVer) {
            if (tileEntityMobSpawner.equals(new MinecraftKey(cmp.getString("id")))) {
                if (cmp.hasKeyOfType("SpawnPotentials", 9)) {
                    NBTTagList nbttaglist = cmp.getList("SpawnPotentials", 10);

                    for (int j = 0; j < nbttaglist.size(); ++j) {
                        NBTTagCompound nbttagcompound1 = nbttaglist.getCompound(j);

                        convertCompound(LegacyType.ENTITY, nbttagcompound1, "Entity", sourceVer, targetVer);
                    }
                }

                convertCompound(LegacyType.ENTITY, cmp, "SpawnData", sourceVer, targetVer);
            }

            return cmp;
        }
    }

    private static class DataInspectorCommandBlock implements DataInspector {
        MinecraftKey tileEntityCommand = getKey("TileEntityCommand");

        @Override
        public NBTTagCompound inspect(NBTTagCompound cmp, int sourceVer, int targetVer) {
            if (tileEntityCommand.equals(new MinecraftKey(cmp.getString("id")))) {
                cmp.setString("id", "Control");
                convert(LegacyType.BLOCK_ENTITY, cmp, sourceVer, targetVer);
                cmp.setString("id", "MinecartCommandBlock");
            }

            return cmp;
        }
    }
}
