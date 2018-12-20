package com.boydti.fawe.bukkit.util;

import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultUtil {
    public final Permission permission;

    public VaultUtil() {
        final RegisteredServiceProvider<Permission> permissionProvider = Bukkit.getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) {
            this.permission = permissionProvider.getProvider();
        } else {
            this.permission = null;
        }
    }
}
