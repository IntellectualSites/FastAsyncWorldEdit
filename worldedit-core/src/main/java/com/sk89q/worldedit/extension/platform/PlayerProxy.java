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

package com.sk89q.worldedit.extension.platform;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.internal.cui.CUIEvent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.session.SessionKey;
import com.sk89q.worldedit.util.HandSide;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.gamemode.GameMode;

import javax.annotation.Nullable;

import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

public class PlayerProxy extends AbstractPlayerActor {

    private final Player basePlayer;
    private final Actor permActor;
    private final Actor cuiActor;
    private final World world;
    private Vector3 offset = Vector3.ZERO;

    public PlayerProxy(Player basePlayer, Actor permActor, Actor cuiActor, World world) {
        checkNotNull(basePlayer);
        checkNotNull(permActor);
        checkNotNull(cuiActor);
        checkNotNull(world);
        this.basePlayer = basePlayer;
        this.permActor = permActor;
        this.cuiActor = cuiActor;
        this.world = world;
    }

    public void setOffset(Vector3 position) {
        this.offset = position;
    }


    @Override
    public BaseBlock getBlockInHand(HandSide handSide) throws WorldEditException {
        return basePlayer.getBlockInHand(handSide);
    }

    @Override
    public UUID getUniqueId() {
        return basePlayer.getUniqueId();
    }

    @Override
    public BaseItemStack getItemInHand(HandSide handSide) {
        return basePlayer.getItemInHand(handSide);
    }

    @Override
    public void giveItem(BaseItemStack itemStack) {
        basePlayer.giveItem(itemStack);
    }

    @Override
    public BlockBag getInventoryBlockBag() {
        return basePlayer.getInventoryBlockBag();
    }

    @Override
    public String getName() {
        return basePlayer.getName();
    }

    @Override
    public String getDisplayName() {
        return basePlayer.getDisplayName();
    }

    @Override
    public BaseEntity getState() {
        throw new UnsupportedOperationException("Can't withPropertyId() on a player");
    }

    @Override
    public Location getLocation() {
        Location loc = this.basePlayer.getLocation();
        return new Location(loc.getExtent(), loc.toVector().add(offset), loc.getDirection());
    }

    @Override
    public boolean setLocation(Location location) {
        return basePlayer.setLocation(location);
    }

    @Override
    public void setPosition(Vector3 pos, float pitch, float yaw) {
        basePlayer.setPosition(pos, pitch, yaw);
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public void printRaw(String msg) {
        basePlayer.printRaw(msg);
    }

    @Override
    public void printDebug(String msg) {
        basePlayer.printDebug(msg);
    }

    @Override
    public void print(String msg) {
        basePlayer.print(msg);
    }

    @Override
    public void printError(String msg) {
        basePlayer.printError(msg);
    }

    @Override
    public String[] getGroups() {
        return permActor.getGroups();
    }

    @Override
    public boolean hasPermission(String perm) {
        return permActor.hasPermission(perm);
    }

    @Override
    public void dispatchCUIEvent(CUIEvent event) {
        cuiActor.dispatchCUIEvent(event);
    }

    @Nullable
    @Override
    public <T> T getFacet(Class<? extends T> cls) {
        return basePlayer.getFacet(cls);
    }

    @Override
    public SessionKey getSessionKey() {
        return basePlayer.getSessionKey();
    }

    @Override
    public GameMode getGameMode() {
        return basePlayer.getGameMode();
    }

    @Override
    public void setGameMode(GameMode gameMode) {
        basePlayer.setGameMode(gameMode);
    }

    @Override
    public <B extends BlockStateHolder<B>> void sendFakeBlock(BlockVector3 pos, B block) {
        basePlayer.sendFakeBlock(pos, block);
    }
}

