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

package com.sk89q.wepif;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.papermc.paper.persistence.PersistentDataContainerView;
import org.bukkit.BanEntry;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TestOfflinePermissible implements OfflinePlayer, Permissible {

    private boolean op;
    private UUID randomUuid = UUID.randomUUID();

    private final Map<String, Boolean> assignedPermissions = new HashMap<>();

    @Override
    public boolean isOp() {
        return op;
    }

    @Override
    public void setOp(boolean b) {
        this.op = b;
    }

    @Override
    public boolean isPermissionSet(String s) {
        return assignedPermissions.containsKey(s.toLowerCase(Locale.ROOT));
    }

    @Override
    public boolean isPermissionSet(Permission permission) {
        return isPermissionSet(permission.getName());
    }

    @Override
    public boolean hasPermission(String s) {
        if (isPermissionSet(s)) {
            return assignedPermissions.get(s.toLowerCase(Locale.ROOT));
        }
        return false;
    }

    @Override
    public boolean hasPermission(Permission permission) {
        return hasPermission(permission.getName());
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String s, boolean b) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String s, boolean b, int i) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, int i) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void removeAttachment(PermissionAttachment permissionAttachment) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void recalculatePermissions() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        Set<PermissionAttachmentInfo> ret = new HashSet<>();
        for (Map.Entry<String, Boolean> entry : assignedPermissions.entrySet()) {
            ret.add(new PermissionAttachmentInfo(this, entry.getKey(), null, entry.getValue()));
        }
        return ret;
    }

    public void setPermission(String permission, boolean value) {
        assignedPermissions.put(permission.toLowerCase(Locale.ROOT), value);
    }

    public void unsetPermission(String permission) {
        assignedPermissions.remove(permission.toLowerCase(Locale.ROOT));
    }

    public void clearPermissions() {
        assignedPermissions.clear();
    }

    // -- Unneeded OfflinePlayer methods

    @Override
    public boolean isOnline() {
        return false;
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public String getName() {
        return "Tester";
    }

    public UUID getUniqueId() {
        return randomUuid;
    }

    @Override
    public @NotNull PlayerProfile getPlayerProfile() {
        return null;
    }

    @Override
    public boolean isBanned() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <E extends BanEntry<? super PlayerProfile>> @Nullable E ban(
            @Nullable final String reason,
            @Nullable final Instant expires,
            @Nullable final String source
    ) {
        return null;
    }

    @Override
    public <E extends BanEntry<? super PlayerProfile>> @Nullable E ban(
            @Nullable final String reason,
            @Nullable final Duration duration,
            @Nullable final String source
    ) {
        return null;
    }

    @Override
    public @Nullable BanEntry<org.bukkit.profile.PlayerProfile> ban(
            @Nullable final String reason,
            @Nullable final Date expires,
            @Nullable final String source
    ) {
        return null;
    }

    @Override
    public boolean isWhitelisted() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setWhitelisted(boolean b) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Player getPlayer() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getFirstPlayed() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getLastPlayed() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean hasPlayedBefore() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Location getBedSpawnLocation() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Map<String, Object> serialize() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getLastLogin() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getLastSeen() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public @Nullable Location getRespawnLocation() {
        return null;
    }

    @Override
    public void incrementStatistic(@Nonnull Statistic statistic) throws IllegalArgumentException {

    }

    @Override
    public void decrementStatistic(@Nonnull Statistic statistic) throws IllegalArgumentException {

    }

    @Override
    public void incrementStatistic(@Nonnull Statistic statistic, int amount)
            throws IllegalArgumentException {

    }

    @Override
    public void decrementStatistic(@Nonnull Statistic statistic, int amount)
            throws IllegalArgumentException {

    }

    @Override
    public void setStatistic(@Nonnull Statistic statistic, int newValue)
            throws IllegalArgumentException {

    }

    @Override
    public int getStatistic(@Nonnull Statistic statistic) throws IllegalArgumentException {
        return 0;
    }

    @Override
    public void incrementStatistic(@Nonnull Statistic statistic, @Nonnull Material material)
            throws IllegalArgumentException {

    }

    @Override
    public void decrementStatistic(@Nonnull Statistic statistic, @Nonnull Material material)
            throws IllegalArgumentException {

    }

    @Override
    public int getStatistic(@Nonnull Statistic statistic, @Nonnull Material material)
            throws IllegalArgumentException {
        return 0;
    }

    @Override
    public void incrementStatistic(
            @Nonnull Statistic statistic, @Nonnull Material material,
            int amount
    ) throws IllegalArgumentException {

    }

    @Override
    public void decrementStatistic(
            @Nonnull Statistic statistic, @Nonnull Material material,
            int amount
    ) throws IllegalArgumentException {

    }

    @Override
    public void setStatistic(@Nonnull Statistic statistic, @Nonnull Material material, int newValue)
            throws IllegalArgumentException {

    }

    @Override
    public void incrementStatistic(@Nonnull Statistic statistic, @Nonnull EntityType entityType)
            throws IllegalArgumentException {

    }

    @Override
    public void decrementStatistic(@Nonnull Statistic statistic, @Nonnull EntityType entityType)
            throws IllegalArgumentException {

    }

    @Override
    public int getStatistic(@Nonnull Statistic statistic, @Nonnull EntityType entityType)
            throws IllegalArgumentException {
        return 0;
    }

    @Override
    public void incrementStatistic(
            @Nonnull Statistic statistic, @Nonnull EntityType entityType,
            int amount
    ) throws IllegalArgumentException {

    }

    @Override
    public void decrementStatistic(
            @Nonnull Statistic statistic, @Nonnull EntityType entityType,
            int amount
    ) {

    }

    @Override
    public void setStatistic(
            @Nonnull Statistic statistic, @Nonnull EntityType entityType,
            int newValue
    ) {

    }

    @Override
    public @Nullable Location getLastDeathLocation() {
        return null;
    }

    @Override
    public @Nullable Location getLocation() {
        return null;
    }

    // @Override
    public PersistentDataContainerView getPersistentDataContainer() {
        return null;
    }

}
