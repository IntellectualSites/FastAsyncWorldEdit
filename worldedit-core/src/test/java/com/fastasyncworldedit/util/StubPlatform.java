package com.fastasyncworldedit.util;

import com.fastasyncworldedit.core.extent.processor.lighting.RelighterFactory;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.AbstractPlatform;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.Preference;
import com.sk89q.worldedit.internal.Constants;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.registry.Registries;
import org.enginehub.piston.CommandManager;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public class StubPlatform extends AbstractPlatform {

    @Override
    public Registries getRegistries() {
        return null;
    }

    @Override
    public int getDataVersion() {
        return Constants.DATA_VERSION_MC_1_18;
    }

    @Override
    public boolean isValidMobType(final String type) {
        return false;
    }

    @Override
    public void reload() {

    }

    @Nullable
    @Override
    public Player matchPlayer(final Player player) {
        return null;
    }

    @Nullable
    @Override
    public World matchWorld(final World world) {
        return null;
    }

    @Override
    public void registerCommands(final CommandManager commandManager) {

    }

    @Override
    public void setGameHooksEnabled(final boolean enabled) {

    }

    @Override
    public LocalConfiguration getConfiguration() {
        return null;
    }

    @Override
    public String getVersion() {
        return "TEST";
    }

    @Override
    public String getPlatformName() {
        return "TEST";
    }

    @Override
    public String getPlatformVersion() {
        return "TEST";
    }

    @Override
    public Map<Capability, Preference> getCapabilities() {
        Map<Capability, Preference> capabilities = new EnumMap<>(Capability.class);
        capabilities.put(Capability.WORLD_EDITING, Preference.PREFER_OTHERS);
        return capabilities;
    }

    @Override
    public Set<SideEffect> getSupportedSideEffects() {
        return null;
    }

    @Override
    public RelighterFactory getRelighterFactory() {
        return null;
    }

    // Use most "extreme" value
    @Override
    public int versionMinY() {
        return -64;
    }

    // Use most "extreme" value
    @Override
    public int versionMaxY() {
        return 319;
    }

}
