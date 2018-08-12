package com.boydti.fawe.wrappers;

import com.sk89q.worldedit.entity.Player;

/**
 * Still prints error messages
 */
public class SilentPlayerWrapper extends PlayerWrapper {
    public SilentPlayerWrapper(Player parent) {
        super(parent);
    }

    @Override
    public void print(String msg) {
    }

    @Override
    public void printDebug(String msg) {
    }

    @Override
    public void printRaw(String msg) {
    }
}
