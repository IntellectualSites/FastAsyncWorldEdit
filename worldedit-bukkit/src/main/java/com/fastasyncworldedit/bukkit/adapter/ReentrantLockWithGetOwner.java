package com.fastasyncworldedit.bukkit.adapter;

import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockWithGetOwner extends ReentrantLock {

    @Override
    public Thread getOwner() {
        return super.getOwner();
    }

}
