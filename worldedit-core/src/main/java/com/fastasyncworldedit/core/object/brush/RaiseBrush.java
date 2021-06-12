package com.fastasyncworldedit.core.object.brush;

public class RaiseBrush extends ErodeBrush {
    public RaiseBrush() {
        this(6, 0, 1, 1);
    }
    public RaiseBrush(int erodeFaces, int erodeRec, int fillFaces, int fillRec) {
        super(erodeFaces, erodeRec, fillFaces, fillRec);
    }
}
