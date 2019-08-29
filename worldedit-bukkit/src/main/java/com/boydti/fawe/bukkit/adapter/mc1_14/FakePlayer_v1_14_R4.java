package com.boydti.fawe.bukkit.adapter.mc1_14;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.v1_14_R1.DamageSource;
import net.minecraft.server.v1_14_R1.DimensionManager;
import net.minecraft.server.v1_14_R1.Entity;
import net.minecraft.server.v1_14_R1.EntityPlayer;
import net.minecraft.server.v1_14_R1.IChatBaseComponent;
import net.minecraft.server.v1_14_R1.ITileInventory;
import net.minecraft.server.v1_14_R1.PacketPlayInSettings;
import net.minecraft.server.v1_14_R1.PlayerInteractManager;
import net.minecraft.server.v1_14_R1.Statistic;
import net.minecraft.server.v1_14_R1.Vec3D;
import net.minecraft.server.v1_14_R1.WorldServer;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import javax.annotation.Nullable;
import java.util.OptionalInt;
import java.util.UUID;

class FakePlayer_v1_14_R4 extends EntityPlayer {
    private static final GameProfile FAKE_WORLDEDIT_PROFILE = new GameProfile(UUID.nameUUIDFromBytes("worldedit".getBytes()), "[WorldEdit]");

    FakePlayer_v1_14_R4(WorldServer world) {
        super(world.getMinecraftServer(), world, FAKE_WORLDEDIT_PROFILE, new PlayerInteractManager(world));
    }

    @Override
    public Vec3D bP() {
        return new Vec3D(0, 0, 0);
    }

    @Override
    public void tick() {
    }

    @Override
    public void die(DamageSource damagesource) {
    }

    @Override
    public Entity a(DimensionManager dimensionmanager, TeleportCause cause) {
        return this;
    }

    @Override
    public OptionalInt openContainer(@Nullable ITileInventory itileinventory) {
        return OptionalInt.empty();
    }

    @Override
    public void a(PacketPlayInSettings packetplayinsettings) {
    }

    @Override
    public void sendMessage(IChatBaseComponent ichatbasecomponent) {
    }

    @Override
    public void a(IChatBaseComponent ichatbasecomponent, boolean flag) {
    }

    @Override
    public void a(Statistic<?> statistic, int i) {
    }

    @Override
    public void a(Statistic<?> statistic) {
    }

    @Override
    public boolean isInvulnerable(DamageSource damagesource) {
        return true;
    }

    @Override
    public boolean p(boolean flag) { // canEat, search for foodData usage
        return true;
    }
}