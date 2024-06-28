package com.fastasyncworldedit.core.exception;

import com.sk89q.worldedit.WorldEditException;

public class OutsideWorldBoundsException extends WorldEditException {

    private final int y;

    public OutsideWorldBoundsException(int y) {
        this.y = y;
    }

    public int y() {
        return y;
    }

}
