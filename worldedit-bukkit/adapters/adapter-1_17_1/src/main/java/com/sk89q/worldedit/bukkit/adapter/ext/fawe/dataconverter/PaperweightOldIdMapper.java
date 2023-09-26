package com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public final class PaperweightOldIdMapper {

    private static final Map<String, ResourceLocation> OLD_ID_TO_KEY_MAP = new HashMap<>();

    static {
        final Map<String, ResourceLocation> map = OLD_ID_TO_KEY_MAP;
        map.put("EntityItem", new ResourceLocation("item"));
        map.put("EntityExperienceOrb", new ResourceLocation("xp_orb"));
        map.put("EntityAreaEffectCloud", new ResourceLocation("area_effect_cloud"));
        map.put("EntityGuardianElder", new ResourceLocation("elder_guardian"));
        map.put("EntitySkeletonWither", new ResourceLocation("wither_skeleton"));
        map.put("EntitySkeletonStray", new ResourceLocation("stray"));
        map.put("EntityEgg", new ResourceLocation("egg"));
        map.put("EntityLeash", new ResourceLocation("leash_knot"));
        map.put("EntityPainting", new ResourceLocation("painting"));
        map.put("EntityTippedArrow", new ResourceLocation("arrow"));
        map.put("EntitySnowball", new ResourceLocation("snowball"));
        map.put("EntityLargeFireball", new ResourceLocation("fireball"));
        map.put("EntitySmallFireball", new ResourceLocation("small_fireball"));
        map.put("EntityEnderPearl", new ResourceLocation("ender_pearl"));
        map.put("EntityEnderSignal", new ResourceLocation("eye_of_ender_signal"));
        map.put("EntityPotion", new ResourceLocation("potion"));
        map.put("EntityThrownExpBottle", new ResourceLocation("xp_bottle"));
        map.put("EntityItemFrame", new ResourceLocation("item_frame"));
        map.put("EntityWitherSkull", new ResourceLocation("wither_skull"));
        map.put("EntityTNTPrimed", new ResourceLocation("tnt"));
        map.put("EntityFallingBlock", new ResourceLocation("falling_block"));
        map.put("EntityFireworks", new ResourceLocation("fireworks_rocket"));
        map.put("EntityZombieHusk", new ResourceLocation("husk"));
        map.put("EntitySpectralArrow", new ResourceLocation("spectral_arrow"));
        map.put("EntityShulkerBullet", new ResourceLocation("shulker_bullet"));
        map.put("EntityDragonFireball", new ResourceLocation("dragon_fireball"));
        map.put("EntityZombieVillager", new ResourceLocation("zombie_villager"));
        map.put("EntityHorseSkeleton", new ResourceLocation("skeleton_horse"));
        map.put("EntityHorseZombie", new ResourceLocation("zombie_horse"));
        map.put("EntityArmorStand", new ResourceLocation("armor_stand"));
        map.put("EntityHorseDonkey", new ResourceLocation("donkey"));
        map.put("EntityHorseMule", new ResourceLocation("mule"));
        map.put("EntityEvokerFangs", new ResourceLocation("evocation_fangs"));
        map.put("EntityEvoker", new ResourceLocation("evocation_illager"));
        map.put("EntityVex", new ResourceLocation("vex"));
        map.put("EntityVindicator", new ResourceLocation("vindication_illager"));
        map.put("EntityIllagerIllusioner", new ResourceLocation("illusion_illager"));
        map.put("EntityMinecartCommandBlock", new ResourceLocation("commandblock_minecart"));
        map.put("EntityBoat", new ResourceLocation("boat"));
        map.put("EntityMinecartRideable", new ResourceLocation("minecart"));
        map.put("EntityMinecartChest", new ResourceLocation("chest_minecart"));
        map.put("EntityMinecartFurnace", new ResourceLocation("furnace_minecart"));
        map.put("EntityMinecartTNT", new ResourceLocation("tnt_minecart"));
        map.put("EntityMinecartHopper", new ResourceLocation("hopper_minecart"));
        map.put("EntityMinecartMobSpawner", new ResourceLocation("spawner_minecart"));
        map.put("EntityCreeper", new ResourceLocation("creeper"));
        map.put("EntitySkeleton", new ResourceLocation("skeleton"));
        map.put("EntitySpider", new ResourceLocation("spider"));
        map.put("EntityGiantZombie", new ResourceLocation("giant"));
        map.put("EntityZombie", new ResourceLocation("zombie"));
        map.put("EntitySlime", new ResourceLocation("slime"));
        map.put("EntityGhast", new ResourceLocation("ghast"));
        map.put("EntityPigZombie", new ResourceLocation("zombie_pigman"));
        map.put("EntityEnderman", new ResourceLocation("enderman"));
        map.put("EntityCaveSpider", new ResourceLocation("cave_spider"));
        map.put("EntitySilverfish", new ResourceLocation("silverfish"));
        map.put("EntityBlaze", new ResourceLocation("blaze"));
        map.put("EntityMagmaCube", new ResourceLocation("magma_cube"));
        map.put("EntityEnderDragon", new ResourceLocation("ender_dragon"));
        map.put("EntityWither", new ResourceLocation("wither"));
        map.put("EntityBat", new ResourceLocation("bat"));
        map.put("EntityWitch", new ResourceLocation("witch"));
        map.put("EntityEndermite", new ResourceLocation("endermite"));
        map.put("EntityGuardian", new ResourceLocation("guardian"));
        map.put("EntityShulker", new ResourceLocation("shulker"));
        map.put("EntityPig", new ResourceLocation("pig"));
        map.put("EntitySheep", new ResourceLocation("sheep"));
        map.put("EntityCow", new ResourceLocation("cow"));
        map.put("EntityChicken", new ResourceLocation("chicken"));
        map.put("EntitySquid", new ResourceLocation("squid"));
        map.put("EntityWolf", new ResourceLocation("wolf"));
        map.put("EntityMushroomCow", new ResourceLocation("mooshroom"));
        map.put("EntitySnowman", new ResourceLocation("snowman"));
        map.put("EntityOcelot", new ResourceLocation("ocelot"));
        map.put("EntityIronGolem", new ResourceLocation("villager_golem"));
        map.put("EntityHorse", new ResourceLocation("horse"));
        map.put("EntityRabbit", new ResourceLocation("rabbit"));
        map.put("EntityPolarBear", new ResourceLocation("polar_bear"));
        map.put("EntityLlama", new ResourceLocation("llama"));
        map.put("EntityLlamaSpit", new ResourceLocation("llama_spit"));
        map.put("EntityParrot", new ResourceLocation("parrot"));
        map.put("EntityVillager", new ResourceLocation("villager"));
        map.put("EntityEnderCrystal", new ResourceLocation("ender_crystal"));
        map.put("TileEntityFurnace", new ResourceLocation("furnace"));
        map.put("TileEntityChest", new ResourceLocation("chest"));
        map.put("TileEntityEnderChest", new ResourceLocation("ender_chest"));
        map.put("TileEntityRecordPlayer", new ResourceLocation("jukebox"));
        map.put("TileEntityDispenser", new ResourceLocation("dispenser"));
        map.put("TileEntityDropper", new ResourceLocation("dropper"));
        map.put("TileEntitySign", new ResourceLocation("sign"));
        map.put("TileEntityMobSpawner", new ResourceLocation("mob_spawner"));
        map.put("TileEntityNote", new ResourceLocation("noteblock"));
        map.put("TileEntityPiston", new ResourceLocation("piston"));
        map.put("TileEntityBrewingStand", new ResourceLocation("brewing_stand"));
        map.put("TileEntityEnchantTable", new ResourceLocation("enchanting_table"));
        map.put("TileEntityEnderPortal", new ResourceLocation("end_portal"));
        map.put("TileEntityBeacon", new ResourceLocation("beacon"));
        map.put("TileEntitySkull", new ResourceLocation("skull"));
        map.put("TileEntityLightDetector", new ResourceLocation("daylight_detector"));
        map.put("TileEntityHopper", new ResourceLocation("hopper"));
        map.put("TileEntityComparator", new ResourceLocation("comparator"));
        map.put("TileEntityFlowerPot", new ResourceLocation("flower_pot"));
        map.put("TileEntityBanner", new ResourceLocation("banner"));
        map.put("TileEntityStructure", new ResourceLocation("structure_block"));
        map.put("TileEntityEndGateway", new ResourceLocation("end_gateway"));
        map.put("TileEntityCommand", new ResourceLocation("command_block"));
        map.put("TileEntityShulkerBox", new ResourceLocation("shulker_box"));
        map.put("TileEntityBed", new ResourceLocation("bed"));
    }

    public static ResourceLocation getKey(String type) {
        final ResourceLocation key = OLD_ID_TO_KEY_MAP.get(type);
        if (key == null) {
            throw new IllegalArgumentException("Unknown mapping for " + type);
        }
        return key;
    }

}
