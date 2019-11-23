package com.boydti.fawe.wrappers;

import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.util.formatting.text.Component;

/**
 * Avoids printing any messages
 */
public class SilentPlayerWrapper extends AsyncPlayer {
    public SilentPlayerWrapper(Player parent) {
        super(parent);
    }

    @Override
    public void print(String msg) {
    }

    @Override
    public void print(Component component) {
        super.print(component);
    }

    @Override
    public void printDebug(String msg) {
    }

    @Override
    public void printError(String msg) {
    }

    @Override
    public void printRaw(String msg) {
    }
}
