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

package com.sk89q.worldedit.session;

import com.boydti.fawe.object.collection.SoftHashMap;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.platform.ConfigurationLoadEvent;
import com.sk89q.worldedit.session.storage.JsonFileSessionStore;
import com.sk89q.worldedit.session.storage.SessionStore;
import com.sk89q.worldedit.session.storage.VoidStore;
import com.sk89q.worldedit.util.concurrency.EvenMoreExecutors;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.gamemode.GameModes;

import java.io.File;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Session manager for WorldEdit.
 * <p>
 * <p>Get a reference to one from {@link WorldEdit}.</p>
 * <p>
 * <p>While this class is thread-safe, the returned session may not be.</p>
 */
public class SessionManager {

    @Deprecated
    public static int EXPIRATION_GRACE = 600000;

    private static final ListeningExecutorService executorService = MoreExecutors.listeningDecorator(EvenMoreExecutors.newBoundedCachedThreadPool(0, 1, 5));
    private static final Logger log = Logger.getLogger(SessionManager.class.getCanonicalName());
    private final Timer timer = new Timer();
    private final WorldEdit worldEdit;
    private final Map<UUID, SessionHolder> sessions = new ConcurrentHashMap<>(8, 0.9f, 1);
    private final Map<UUID, Reference<SessionHolder>> softSessions = new SoftHashMap<>();

    private SessionStore store = new VoidStore();
    private File path;

    /**
     * Create a new session manager.
     *
     * @param worldEdit a WorldEdit instance
     */
    public SessionManager(WorldEdit worldEdit) {
        checkNotNull(worldEdit);
        this.worldEdit = worldEdit;

        worldEdit.getEventBus().register(this);
    }

    /**
     * Get whether a session exists for the given owner.
     *
     * @param owner the owner
     * @return true if a session exists
     */
    public synchronized boolean contains(SessionOwner owner) {
        checkNotNull(owner);
        return sessions.containsKey(getKey(owner)) || softSessions.containsKey(owner);
    }

    /**
     * Find a session by its name specified by {@link SessionKey#getName()}.
     *
     * @param name the name
     * @return the session, if found, otherwise {@code null}
     */
    @Nullable
    public synchronized LocalSession findByName(String name) {
        checkNotNull(name);
        for (SessionHolder holder : sessions.values()) {
            String test = holder.key.getName();
            if (test != null && name.equals(test)) {
                return holder.session;
            }
        }
        Iterator<Map.Entry<UUID, Reference<SessionHolder>>> iter = softSessions.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<UUID, Reference<SessionHolder>> entry = iter.next();
            UUID key = entry.getKey();
            SessionHolder holder = entry.getValue().get();
            if (holder == null) {
                iter.remove();
                continue;
            }
            String test = holder.key.getName();
            if (test != null && name.equals(test)) {
//                if (holder.key.isActive()) {
                iter.remove();
                sessions.put(key, holder);
//                }
                return holder.session;
            }
        }
        return null;
    }

    /**
     * Gets the session for an owner and return it if it exists, otherwise
     * return {@code null}.
     *
     * @param owner the owner
     * @return the session for the owner, if it exists
     */
    @Nullable
    public synchronized LocalSession getIfPresent(SessionOwner owner) {
        checkNotNull(owner);
        UUID key = getKey(owner);
        SessionHolder stored = sessions.get(key);
        if (stored != null) {
            return stored.session;
        } else {
            Reference<SessionHolder> reference = softSessions.get(key);
            if (reference != null) {
                stored = reference.get();
                if (stored != null) {
//                if (stored.key.isActive()) {
                    softSessions.remove(key);
                    sessions.put(key, stored);
//                }
                    return stored.session;
                }
            }
        }
        return null;
    }

    /**
     * Get the session for an owner and create one if one doesn't exist.
     *
     * @param owner the owner
     * @return a session
     */
    public synchronized LocalSession get(SessionOwner owner) {
        checkNotNull(owner);

        LocalSession session = getIfPresent(owner);
        LocalConfiguration config = worldEdit.getConfiguration();
        SessionKey sessionKey = owner.getSessionKey();

        // No session exists yet -- create one
        if (session == null) {
            try {
                session = store.load(getKey(sessionKey));
                session.postLoad();
            } catch (Throwable e) {
                log.log(Level.WARNING, "Failed to load saved session", e);
                session = new LocalSession();
            }

            session.setConfiguration(config);
            session.setBlockChangeLimit(config.defaultChangeLimit);

            sessions.put(getKey(owner), new SessionHolder(sessionKey, session));
        }

        // Set the limit on the number of blocks that an operation can
        // change at once, or don't if the owner has an override or there
        // is no limit. There is also a default limit
        int currentChangeLimit = session.getBlockChangeLimit();

        if (!owner.hasPermission("worldedit.limit.unrestricted") && config.maxChangeLimit > -1) {
            // If the default limit is infinite but there is a maximum
            // limit, make sure to not have it be overridden
            if (config.defaultChangeLimit < 0) {
                if (currentChangeLimit < 0 || currentChangeLimit > config.maxChangeLimit) {
                    session.setBlockChangeLimit(config.maxChangeLimit);
                }
            } else {
                // Bound the change limit
                int maxChangeLimit = config.maxChangeLimit;
                if (currentChangeLimit == -1 || currentChangeLimit > maxChangeLimit) {
                    session.setBlockChangeLimit(maxChangeLimit);
                }
            }
        }

        // Have the session use inventory if it's enabled and the owner
        // doesn't have an override
        session.setUseInventory(config.useInventory
                && !(config.useInventoryOverride
                && (owner.hasPermission("worldedit.inventory.unrestricted")
                || (config.useInventoryCreativeOverride && (!(owner instanceof Player) || ((Player) owner).getGameMode() == GameModes.CREATIVE)))));

        return session;
    }

    private void save(SessionHolder holder) {
        SessionKey key = holder.key;
        holder.session.setClipboard(null);
        if (key.isPersistent()) {
            try {
                if (holder.session.compareAndResetDirty()) {
                    if (holder.session.save()) {
                        store.save(getKey(key), holder.session);
                    } else if (path != null) {
                        File file = new File(path, getKey(key) + ".json");
                        if (file.exists()) {
                            if (!file.delete()) {
                                file.deleteOnExit();
                            }
                        }
                    }
                }
            } catch (IOException e) {
                log.log(Level.WARNING, "Failed to write session for UUID " + getKey(key), e);
            }
        }
    }

    /**
     * Get the key to use in the map for an owner.
     *
     * @param owner the owner
     * @return the key object
     */
    protected UUID getKey(SessionOwner owner) {
        return getKey(owner.getSessionKey());
    }


    /**
     * Get the key to use in the map for a {@code SessionKey}.
     *
     * @param key the session key object
     * @return the key object
     */
    protected UUID getKey(SessionKey key) {
        String forcedKey = System.getProperty("worldedit.session.uuidOverride");
        if (forcedKey != null) {
            return UUID.fromString(forcedKey);
        } else {
            return key.getUniqueId();
        }
    }

    /**
     * Remove the session for the given owner if one exists.
     *
     * @param owner the owner
     */
    public synchronized void remove(SessionOwner owner) {
        checkNotNull(owner);
        SessionHolder session = sessions.remove(getKey(owner));
        if (session != null) {
            save(session);
        }
    }

    public synchronized void forget(SessionOwner owner) {
        checkNotNull(owner);
        UUID key = getKey(owner);
        SessionHolder holder = sessions.remove(key);
        if (holder != null) {
            softSessions.put(key, new SoftReference(holder));
            save(holder);
        }
    }

    /**
     * Remove all sessions.
     */
    public synchronized void clear() {
        for (Map.Entry<UUID, SessionHolder> entry : sessions.entrySet()) {
            save(entry.getValue());
        }
        sessions.clear();
    }

    @Subscribe
    public void onConfigurationLoad(ConfigurationLoadEvent event) {
        LocalConfiguration config = event.getConfiguration();
        File dir = new File(config.getWorkingDirectory(), "sessions");
        store = new JsonFileSessionStore(dir);
        this.path = dir;
    }

    /**
     * Stores the owner of a session, the session, and the last active time.
     */
    private static class SessionHolder {
        private final SessionKey key;
        private final LocalSession session;
        private long lastActive = System.currentTimeMillis();

        private SessionHolder(SessionKey key, LocalSession session) {
            this.key = key;
            this.session = session;
        }
    }

    public static Class<?> inject() {
        return SessionManager.class;
    }


}