package com.boydti.fawe.util;

import com.sk89q.worldedit.util.auth.Subject;

public enum Permission {
    /*
     * Permission related functions
     */
    ADMIN("fawe.admin", "admin");

    public String permission;
    public String cat;

    Permission(String permission, String category) {
        this.permission = permission;
        this.cat = category;
    }


    public static boolean hasPermission(Subject player, String permission) {
        if (player == null || player.hasPermission(ADMIN.permission)) {
            return true;
        }
        if (player.hasPermission(permission)) {
            return true;
        }
        final String[] nodes = permission.split("\\.");
        final StringBuilder n = new StringBuilder();
        for (int i = 0; i < nodes.length - 1; i++) {
            n.append(nodes[i]).append(".");
            if (player.hasPermission(n + "*")) {
                return true;
            }
        }
        return false;
    }
}
