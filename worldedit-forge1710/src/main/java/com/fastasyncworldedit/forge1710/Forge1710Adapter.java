package com.fastasyncworldedit.forge1710;

import com.fastasyncworldedit.forge1710.entity.Forge1710Player;
import com.fastasyncworldedit.forge1710.registry.Forge1710Registries;
import com.fastasyncworldedit.forge1710.world.Forge1710World;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.item.ItemTypes;
import cpw.mods.fml.common.registry.GameData;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

public final class Forge1710Adapter {

    private static final Map<WorldServer, Forge1710World> WORLDS = new WeakHashMap<>();

    private Forge1710Adapter() {
    }

    public static Forge1710World adapt(WorldServer world) {
        synchronized (WORLDS) {
            return WORLDS.computeIfAbsent(world, Forge1710World::new);
        }
    }

    public static Forge1710Player adapt(EntityPlayerMP player) {
        return new Forge1710Player(player);
    }

    public static String biomeId(BiomeGenBase biome) {
        String name = biome.biomeName == null ? "biome_" + biome.biomeID : biome.biomeName;
        return "minecraft:" + name.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    public static BaseItemStack adapt(@Nullable ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return new BaseItemStack(ItemTypes.AIR, 0);
        }
        String name = GameData.getItemRegistry().getNameForObject(stack.getItem());
        ItemType type = name == null ? null : ItemTypes.get(name.toLowerCase(Locale.ROOT));
        return new BaseItemStack(type != null ? type : ItemTypes.AIR, stack.stackSize);
    }

    @Nullable
    public static ItemStack toNative(BaseItemStack stack) {
        Item item = Forge1710Registries.getInstance().getItemRegistry().getItem(stack.getType());
        if (item == null) {
            return null;
        }
        return new ItemStack(item, Math.max(1, stack.getAmount()), 0);
    }

}
