package com.boydti.fawe.util;

import com.boydti.fawe.object.FawePlayer;

public enum Perm {
    /*
     * Permission related functions
     */
    ADMIN("fawe.admin", "admin");

    public String s;
    public String cat;

    Perm(String perm, String cat) {
        this.s = perm;
        this.cat = cat;
    }

    public boolean has(FawePlayer<?> player) {
        return this.hasPermission(player, this);
    }

    public boolean hasPermission(FawePlayer<?> player, Perm perm) {
        return hasPermission(player, perm.s);
    }

    public static boolean hasPermission(FawePlayer<?> player, String perm) {
        if (player == null || player.hasPermission(ADMIN.s)) {
            return true;
        }
        if (player.hasPermission(perm)) {
            return true;
        }
        final String[] nodes = perm.split("\\.");
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
