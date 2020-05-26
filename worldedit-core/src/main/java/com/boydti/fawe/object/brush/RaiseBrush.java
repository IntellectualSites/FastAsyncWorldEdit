package com.boydti.fawe.object.brush;

public class RaiseBrush extends ErodeBrush {
    public RaiseBrush() {
        this(6, 0, 1, 1);
    }
    public RaiseBrush(int erodeFaces, int erodeRec, int fillFaces, int fillRec) {
        super(2, 1, 5, 1);
    }
}
