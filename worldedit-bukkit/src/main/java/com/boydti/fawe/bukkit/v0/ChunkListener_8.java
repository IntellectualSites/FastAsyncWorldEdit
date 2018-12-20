package com.boydti.fawe.bukkit.v0;

import sun.misc.SharedSecrets;

public class ChunkListener_8 extends ChunkListener {

    @Override
    protected int getDepth(Exception ex) {
        return SharedSecrets.getJavaLangAccess().getStackTraceDepth(ex);
    }

    @Override
    protected StackTraceElement getElement(Exception ex, int index) {
        return SharedSecrets.getJavaLangAccess().getStackTraceElement(ex, index);
    }
}