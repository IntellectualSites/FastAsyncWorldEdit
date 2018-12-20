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

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.brush.visualization.VirtualWorld;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.object.pattern.PatternTraverser;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.wrappers.LocationMaskedPlayerWrapper;
import com.boydti.fawe.wrappers.PlayerWrapper;
import com.boydti.fawe.wrappers.WorldWrapper;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.command.tool.*;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.platform.*;
import com.sk89q.worldedit.extension.platform.permission.ActorSelectorLimits;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.session.request.Request;
import com.sk89q.worldedit.util.HandSide;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.World;

import javax.annotation.Nullable;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Manages registered {@link Platform}s for WorldEdit. Platforms are
 * implementations of WorldEdit.
 * <p>
 * <p>This class is thread-safe.</p>
 */
public class PlatformManager {

    private static final Logger logger = Logger.getLogger(PlatformManager.class.getCanonicalName());

    private final WorldEdit worldEdit;
    private final CommandManager commandManager;
    private final List<Platform> platforms = new ArrayList<Platform>();
    private final Map<Capability, Platform> preferences = new EnumMap<Capability, Platform>(Capability.class);
    private @Nullable String firstSeenVersion;
    private final AtomicBoolean initialized = new AtomicBoolean();
    private final AtomicBoolean configured = new AtomicBoolean();

    /**
     * Create a new platform manager.
     *
     * @param worldEdit the WorldEdit instance
     */
    public PlatformManager(WorldEdit worldEdit) {
        checkNotNull(worldEdit);
        this.worldEdit = worldEdit;
        this.commandManager = new CommandManager(worldEdit, this);

        // Register this instance for events
        worldEdit.getEventBus().register(this);
    }

    /**
     * Register a platform with WorldEdit.
     *
     * @param platform the platform
     */
    public synchronized void register(Platform platform) {
        checkNotNull(platform);

        logger.log(Level.FINE, "Got request to register " + platform.getClass() + " with WorldEdit [" + super.toString() + "]");

        // Just add the platform to the list of platforms: we'll pick favorites
        // once all the platforms have been loaded
        platforms.add(platform);

        // Make sure that versions are in sync
        if (firstSeenVersion != null) {
            if (!firstSeenVersion.equals(platform.getVersion())) {
                logger.log(Level.WARNING, "Multiple ports of WorldEdit are installed but they report different versions ({0} and {1}). " +
                                "If these two versions are truly different, then you may run into unexpected crashes and errors.",
                        new Object[]{firstSeenVersion, platform.getVersion()});
            }
        } else {
            firstSeenVersion = platform.getVersion();
        }
    }

    /**
     * Unregister a platform from WorldEdit.
     * <p>
     * <p>If the platform has been chosen for any capabilities, then a new
     * platform will be found.</p>
     *
     * @param platform the platform
     */
    public synchronized boolean unregister(Platform platform) {
        checkNotNull(platform);

        boolean removed = platforms.remove(platform);

        if (removed) {
            logger.log(Level.FINE, "Unregistering " + platform.getClass().getCanonicalName() + " from WorldEdit");

            boolean choosePreferred = false;

            // Check whether this platform was chosen to be the preferred one
            // for any capability and be sure to remove it
            Iterator<Entry<Capability, Platform>> it = preferences.entrySet().iterator();
            while (it.hasNext()) {
                Entry<Capability, Platform> entry = it.next();
                if (entry.getValue().equals(platform)) {
                    entry.getKey().unload(this, entry.getValue());
                    it.remove();
                    choosePreferred = true; // Have to choose new favorites
                }
            }

            if (choosePreferred) {
                choosePreferred();
            }
        }

        return removed;
    }

    /**
     * Get the preferred platform for handling a certain capability. Returns
     * null if none is available.
     *
     * @param capability the capability
     * @return the platform
     * @throws NoCapablePlatformException thrown if no platform is capable
     */
    public synchronized Platform queryCapability(Capability capability) throws NoCapablePlatformException {
        Platform platform = preferences.get(checkNotNull(capability));
        if (platform != null) {
            return platform;
        } else {
            if (preferences.isEmpty() && !platforms.isEmpty()) {
                return platforms.get(0); // Use the first available if preferences have not been decided yet.
            }
            throw new NoCapablePlatformException("No platform was found supporting " + capability.name());
        }
    }

    /**
     * Choose preferred platforms and perform necessary initialization.
     */
    private synchronized void choosePreferred() {
        for (Capability capability : Capability.values()) {
            Platform preferred = findMostPreferred(capability);
            if (preferred != null) {
                preferences.put(capability, preferred);
                capability.initialize(this, preferred);
            }
        }

        // Fire configuration event
        if (preferences.containsKey(Capability.CONFIGURATION) && configured.compareAndSet(false, true)) {
            worldEdit.getEventBus().post(new ConfigurationLoadEvent(queryCapability(Capability.CONFIGURATION).getConfiguration()));
        }
    }

    /**
     * Find the most preferred platform for a given capability from the list of
     * platforms. This does not use the map of preferred platforms.
     *
     * @param capability the capability
     * @return the most preferred platform, or null if no platform was found
     */
    private synchronized @Nullable Platform findMostPreferred(Capability capability) {
        Platform preferred = null;
        Preference highest = null;

        for (Platform platform : platforms) {
            Preference preference = platform.getCapabilities().get(capability);
            if (preference != null && (highest == null || preference.isPreferredOver(highest))) {
                preferred = platform;
                highest = preference;
            }
        }

        return preferred;
    }

    /**
     * Get a list of loaded platforms.
     * <p>
     * <p>The returned list is a copy of the original and is mutable.</p>
     *
     * @return a list of platforms
     */
    public synchronized List<Platform> getPlatforms() {
        return new ArrayList<Platform>(platforms);
    }

    /**
     * Given a world, possibly return the same world but using a different
     * platform preferred for world editing operations.
     *
     * @param base the world to match
     * @return the preferred world, if one was found, otherwise the given world
     */
    public World getWorldForEditing(World base) {
        checkNotNull(base);
        base = WorldWrapper.unwrap(base);
        World match = queryCapability(Capability.WORLD_EDITING).matchWorld(base);
        return match != null ? match : base;
    }

    /**
     * Given an actor, return a new one that may use a different platform
     * for permissions and world editing.
     *
     * @param base the base actor to match
     * @return a new delegate actor
     */
    @SuppressWarnings("unchecked")
    public <T extends Actor> T createProxyActor(T base) {
        checkNotNull(base);

        if (base instanceof Player) {
            Player player = (Player) base;
            FawePlayer fp = FawePlayer.wrap(player);
            return (T) fp.createProxy();
        } else {
            return base;
        }
    }

    /**
     * Get the command manager.
     *
     * @return the command manager
     */
    public CommandManager getCommandManager() {
        return commandManager;
    }

    /**
     * Get the current configuration.
     * <p>
     * <p>If no platform has been registered yet, then a default configuration
     * will be returned.</p>
     *
     * @return the configuration
     */
    public LocalConfiguration getConfiguration() {
        return queryCapability(Capability.CONFIGURATION).getConfiguration();
    }

    @Subscribe
    public void handlePlatformReady(PlatformReadyEvent event) {
        choosePreferred();
        if (initialized.compareAndSet(false, true)) {
            worldEdit.getEventBus().post(new PlatformInitializeEvent());
        }
    }

    private <T extends Tool> T reset(T tool) {
        new PatternTraverser(tool).reset(null);
        return tool;
    }

    @SuppressWarnings("deprecation")
    @Subscribe
    public void handleBlockInteract(BlockInteractEvent event) {
        // Create a proxy actor with a potentially different world for
        // making changes to the world
        Request.reset();
        final Actor actor = createProxyActor(event.getCause());
        try {
            final Location location = event.getLocation();
            final Vector vector = location.toVector();

            // At this time, only handle interaction from players
            if (actor instanceof Player) {
                final LocalSession session = worldEdit.getSessionManager().get(actor);
                Player playerActor = (Player) actor;

                VirtualWorld virtual = session.getVirtualWorld();
                if (virtual != null) {
                    virtual.handleBlockInteract(playerActor, vector, event);
                    if (event.isCancelled()) return;
                }

                if (event.getType() == Interaction.HIT) {
                    if (session.isToolControlEnabled() && playerActor.getItemInHand(HandSide.MAIN_HAND).getType().equals(getConfiguration().wandItem)) {
                        FawePlayer<?> fp = FawePlayer.wrap(playerActor);
                        if (!actor.hasPermission("worldedit.selection.pos")) {
                            return;
                        }
                        final RegionSelector selector = session.getRegionSelector(playerActor.getWorld());
                        final Player player = new LocationMaskedPlayerWrapper(PlayerWrapper.wrap((Player) actor), ((Player) actor).getLocation());
                        fp.runAction(new Runnable() {
                            @Override
                            public void run() {
                                if (selector.selectPrimary(location.toVector(), ActorSelectorLimits.forActor(player))) {
                                    selector.explainPrimarySelection(actor, session, vector);
                                }
                            }
                        }, false, true);

                        event.setCancelled(true);
                        return;
                    }
                    if (session.hasSuperPickAxe() && playerActor.isHoldingPickAxe()) {
                        final BlockTool superPickaxe = session.getSuperPickaxe();
                        if (superPickaxe != null && superPickaxe.canUse(playerActor)) {
                            FawePlayer<?> fp = FawePlayer.wrap(playerActor);
                            final Player player = new LocationMaskedPlayerWrapper(PlayerWrapper.wrap((Player) actor), ((Player) actor).getLocation());
                            fp.runAction(new Runnable() {
                                @Override
                                public void run() {
                                    reset(superPickaxe).actPrimary(queryCapability(Capability.WORLD_EDITING), getConfiguration(), player, session, location);
                                }
                            }, false, true);
                            event.setCancelled(true);
                            return;
                        }
                    }
                    final Tool tool = session.getTool(playerActor);
                    if (tool != null && tool instanceof DoubleActionBlockTool) {
                        if (tool.canUse(playerActor)) {
                            FawePlayer<?> fp = FawePlayer.wrap(playerActor);
                            final Player player = new LocationMaskedPlayerWrapper(PlayerWrapper.wrap((Player) actor), ((Player) actor).getLocation());
                            fp.runAction(new Runnable() {
                                @Override
                                public void run() {
                                    reset(((DoubleActionBlockTool) tool)).actSecondary(queryCapability(Capability.WORLD_EDITING), getConfiguration(), player, session, location);
                                }
                            }, false, true);
                            event.setCancelled(true);
                            return;
                        }
                    }
                } else if (event.getType() == Interaction.OPEN) {
                    if (session.isToolControlEnabled() && playerActor.getItemInHand(HandSide.MAIN_HAND).getType().equals(getConfiguration().wandItem)) {
                        FawePlayer<?> fp = FawePlayer.wrap(playerActor);
                        if (!actor.hasPermission("worldedit.selection.pos")) {
                            return;
                        }
                        if (fp.checkAction()) {
                            final RegionSelector selector = session.getRegionSelector(playerActor.getWorld());
                            final Player player = new LocationMaskedPlayerWrapper(PlayerWrapper.wrap((Player) actor), ((Player) actor).getLocation());
                            fp.runAction(new Runnable() {
                                @Override
                                public void run() {
                                    if (selector.selectSecondary(vector, ActorSelectorLimits.forActor(player))) {
                                        selector.explainSecondarySelection(actor, session, vector);
                                    }
                                }
                            }, false, true);
                        }
                        event.setCancelled(true);
                        return;
                    }

                    final Tool tool = session.getTool(playerActor);
                    if (tool != null && tool instanceof BlockTool) {
                        if (tool.canUse(playerActor)) {
                            FawePlayer<?> fp = FawePlayer.wrap(playerActor);
                            if (fp.checkAction()) {
                                final Player player = new LocationMaskedPlayerWrapper(PlayerWrapper.wrap((Player) actor), ((Player) actor).getLocation());
                                fp.runAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (tool instanceof BrushTool) {
                                            ((BlockTool) tool).actPrimary(queryCapability(Capability.WORLD_EDITING), getConfiguration(), player, session, location);
                                        } else {
                                            reset((BlockTool) tool).actPrimary(queryCapability(Capability.WORLD_EDITING), getConfiguration(), player, session, location);
                                        }
                                    }
                                }, false, true);
                                event.setCancelled(true);
                                return;
                            }
                        }
                    }
                }
            }
        } catch (Throwable e) {
            handleThrowable(e, actor);
        }
    }

    public void handleThrowable(Throwable e, Actor actor) {
        FaweException faweException = FaweException.get(e);
        if (faweException != null) {
            BBC.WORLDEDIT_CANCEL_REASON.send(actor, faweException.getMessage());
        } else {
            actor.printError("Please report this error: [See console]");
            actor.printRaw(e.getClass().getName() + ": " + e.getMessage());
            MainUtil.handleError(e);
        }
    }

    @SuppressWarnings("deprecation")
    @Subscribe
    public void handlePlayerInput(PlayerInputEvent event) {
        // Create a proxy actor with a potentially different world for
        // making changes to the world
        Player actor = createProxyActor(event.getPlayer());
        final Player player = new LocationMaskedPlayerWrapper(PlayerWrapper.wrap(actor), actor.getLocation(), true);
        final LocalSession session = worldEdit.getSessionManager().get(player);

        VirtualWorld virtual = session.getVirtualWorld();
        if (virtual != null) {
            virtual.handlePlayerInput(player,  event);
            if (event.isCancelled()) return;
        }

        try {
            switch (event.getInputType()) {
                case PRIMARY: {
                    if (player.getItemInHand(HandSide.MAIN_HAND).getType().equals(getConfiguration().navigationWand)) {
                        if (getConfiguration().navigationWandMaxDistance <= 0) {
                            return;
                        }

                        if (!player.hasPermission("worldedit.navigation.jumpto.tool")) {
                            return;
                        }

                        Location pos = player.getSolidBlockTrace(getConfiguration().navigationWandMaxDistance);
                        if (pos != null) {
                            player.findFreePosition(pos);
                        } else {
                            BBC.NO_BLOCK.send(player);
                        }

                        event.setCancelled(true);
                        return;
                    }

                    final Tool tool = session.getTool(player);
                    if (tool != null && tool instanceof DoubleActionTraceTool) {
                        if (tool.canUse(player)) {
                            FawePlayer<?> fp = FawePlayer.wrap(player);
                            fp.runAsyncIfFree(new Runnable() {
                                @Override
                                public void run() {
                                    reset((DoubleActionTraceTool) tool).actSecondary(queryCapability(Capability.WORLD_EDITING), getConfiguration(), player, session);
                                }
                            });
                            event.setCancelled(true);
                            return;
                        }
                    }
                    break;
                }

                case SECONDARY: {
                    if (player.getItemInHand(HandSide.MAIN_HAND).getType().equals(getConfiguration().navigationWand)) {
                        if (getConfiguration().navigationWandMaxDistance <= 0) {
                            return;
                        }

                        if (!player.hasPermission("worldedit.navigation.thru.tool")) {
                            return;
                        }

                        if (!player.passThroughForwardWall(40)) {
                            BBC.NAVIGATION_WAND_ERROR.send(player);
                        }

                        event.setCancelled(true);
                        return;
                    }

                    final Tool tool = session.getTool(player);
                    if (tool != null && tool instanceof TraceTool) {
                        if (tool.canUse(player)) {
                            FawePlayer<?> fp = FawePlayer.wrap(player);
                            fp.runAction(new Runnable() {
                                @Override
                                public void run() {
                                    reset((TraceTool) tool).actPrimary(queryCapability(Capability.WORLD_EDITING), getConfiguration(), player, session);
                                }
                            }, false, true);
                            event.setCancelled(true);
                            return;
                        }
                    }

                    break;
                }
            }
        } catch (Throwable e) {
            FaweException faweException = FaweException.get(e);
            if (faweException != null) {
                BBC.WORLDEDIT_CANCEL_REASON.send(player, faweException.getMessage());
            } else {
                player.printError("Please report this error: [See console]");
                player.printRaw(e.getClass().getName() + ": " + e.getMessage());
                MainUtil.handleError(e);
            }
        }
    }



}