//package com.boydti.fawe.logging;
//
//import org.primesoft.blockshub.api.Vector;
//
//public class MutableVector extends Vector {
//
//    public double x, y, z;
//
//    public MutableVector() {
//        super(0, 0, 0);
//        this.x = 0;
//        this.y = 0;
//        this.z = 0;
//    }
//
//    @Override
//    public double getX() {
//        return x;
//    }
//
//    @Override
//    public double getY() {
//        return y;
//    }
//
//    @Override
//    public double getZ() {
//        return z;
//    }
//
//    public boolean equals(Object obj) {
//        if (!(obj instanceof Vector)) {
//            return false;
//        } else {
//            Vector v = (Vector) obj;
//            return this.x == v.getX() && this.z == v.getZ() && this.y == v.getY();
//        }
//    }
//
//    public int hashCode() {
//        byte hash = 3;
//        int hash1 = 59 * hash + (int) (Double.doubleToLongBits(this.x) ^ Double.doubleToLongBits(this.x) >>> 32);
//        hash1 = 59 * hash1 + (int) (Double.doubleToLongBits(this.y) ^ Double.doubleToLongBits(this.y) >>> 32);
//        hash1 = 59 * hash1 + (int) (Double.doubleToLongBits(this.z) ^ Double.doubleToLongBits(this.z) >>> 32);
//        return hash1;
//    }
//}
