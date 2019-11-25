package com.sk89q.worldedit.math;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;

import java.util.Comparator;

public class DelegateBlockVector3 extends BlockVector3 {
    private BlockVector3 parent;

    public DelegateBlockVector3 init(BlockVector3 parent) {
        this.parent = parent;
        return this;
    }

    public static BlockVector3 at(double x, double y, double z) {
        return BlockVector3.at(x, y, z);
    }

    public static BlockVector3 at(int x, int y, int z) {
        return BlockVector3.at(x, y, z);
    }

    public static boolean isLongPackable(BlockVector3 location) {
        return BlockVector3.isLongPackable(location);
    }

    public static void checkLongPackable(BlockVector3 location) {
        BlockVector3.checkLongPackable(location);
    }

    public static BlockVector3 fromLongPackedForm(long packed) {
        return BlockVector3.fromLongPackedForm(packed);
    }

    public static Comparator<BlockVector3> sortByCoordsYzx() {
        return BlockVector3.sortByCoordsYzx();
    }

    @Override
    public MutableBlockVector3 setComponents(double x, double y, double z) {
        return parent.setComponents(x, y, z);
    }

    @Override
    public MutableBlockVector3 setComponents(int x, int y, int z) {
        return parent.setComponents(x, y, z);
    }

    @Override
    public MutableBlockVector3 mutX(double x) {
        return parent.mutX(x);
    }

    @Override
    public MutableBlockVector3 mutY(double y) {
        return parent.mutY(y);
    }

    @Override
    public MutableBlockVector3 mutZ(double z) {
        return parent.mutZ(z);
    }

    @Override
    public MutableBlockVector3 mutX(int x) {
        return parent.mutX(x);
    }

    @Override
    public MutableBlockVector3 mutY(int y) {
        return parent.mutY(y);
    }

    @Override
    public MutableBlockVector3 mutZ(int z) {
        return parent.mutZ(z);
    }

    @Override
    public BlockVector3 toImmutable() {
        return parent.toImmutable();
    }

    @Override
    public long toLongPackedForm() {
        return parent.toLongPackedForm();
    }

    @Override
    public int getX() {
        return parent.getX();
    }

    @Override
    public int getBlockX() {
        return parent.getBlockX();
    }

    @Override
    public BlockVector3 withX(int x) {
        return parent.withX(x);
    }

    @Override
    public int getY() {
        return parent.getY();
    }

    @Override
    public int getBlockY() {
        return parent.getBlockY();
    }

    @Override
    public BlockVector3 withY(int y) {
        return parent.withY(y);
    }

    @Override
    public int getZ() {
        return parent.getZ();
    }

    @Override
    public int getBlockZ() {
        return parent.getBlockZ();
    }

    @Override
    public BlockVector3 withZ(int z) {
        return parent.withZ(z);
    }

    @Override
    public BlockVector3 add(BlockVector3 other) {
        return parent.add(other);
    }

    @Override
    public BlockVector3 add(int x, int y, int z) {
        return parent.add(x, y, z);
    }

    @Override
    public BlockVector3 add(BlockVector3... others) {
        return parent.add(others);
    }

    @Override
    public BlockVector3 subtract(BlockVector3 other) {
        return parent.subtract(other);
    }

    @Override
    public BlockVector3 subtract(int x, int y, int z) {
        return parent.subtract(x, y, z);
    }

    @Override
    public BlockVector3 subtract(BlockVector3... others) {
        return parent.subtract(others);
    }

    @Override
    public BlockVector3 multiply(BlockVector3 other) {
        return parent.multiply(other);
    }

    @Override
    public BlockVector3 multiply(int x, int y, int z) {
        return parent.multiply(x, y, z);
    }

    @Override
    public BlockVector3 multiply(BlockVector3... others) {
        return parent.multiply(others);
    }

    @Override
    public BlockVector3 multiply(int n) {
        return parent.multiply(n);
    }

    @Override
    public BlockVector3 divide(BlockVector3 other) {
        return parent.divide(other);
    }

    @Override
    public BlockVector3 divide(int x, int y, int z) {
        return parent.divide(x, y, z);
    }

    @Override
    public BlockVector3 divide(int n) {
        return parent.divide(n);
    }

    @Override
    public BlockVector3 shr(int x, int y, int z) {
        return parent.shr(x, y, z);
    }

    @Override
    public BlockVector3 shr(int n) {
        return parent.shr(n);
    }

    @Override
    public BlockVector3 shl(int x, int y, int z) {
        return parent.shl(x, y, z);
    }

    @Override
    public BlockVector3 shl(int n) {
        return parent.shl(n);
    }

    @Override
    public double length() {
        return parent.length();
    }

    @Override
    public int lengthSq() {
        return parent.lengthSq();
    }

    @Override
    public double distance(BlockVector3 other) {
        return parent.distance(other);
    }

    @Override
    public int distanceSq(BlockVector3 other) {
        return parent.distanceSq(other);
    }

    @Override
    public BlockVector3 normalize() {
        return parent.normalize();
    }

    @Override
    public double dot(BlockVector3 other) {
        return parent.dot(other);
    }

    @Override
    public BlockVector3 cross(BlockVector3 other) {
        return parent.cross(other);
    }

    @Override
    public boolean containedWithin(BlockVector3 min, BlockVector3 max) {
        return parent.containedWithin(min, max);
    }

    @Override
    public BlockVector3 clampY(int min, int max) {
        return parent.clampY(min, max);
    }

    @Override
    public BlockVector3 floor() {
        return parent.floor();
    }

    @Override
    public BlockVector3 ceil() {
        return parent.ceil();
    }

    @Override
    public BlockVector3 round() {
        return parent.round();
    }

    @Override
    public BlockVector3 abs() {
        return parent.abs();
    }

    @Override
    public BlockVector3 transform2D(double angle, double aboutX, double aboutZ, double translateX, double translateZ) {
        return parent.transform2D(angle, aboutX, aboutZ, translateX, translateZ);
    }

    @Override
    public double toPitch() {
        return parent.toPitch();
    }

    @Override
    public double toYaw() {
        return parent.toYaw();
    }

    @Override
    public BlockVector3 getMinimum(BlockVector3 v2) {
        return parent.getMinimum(v2);
    }

    @Override
    public BlockVector3 getMaximum(BlockVector3 v2) {
        return parent.getMaximum(v2);
    }

    @Override
    public boolean setOrdinal(Extent orDefault, int ordinal) {
        return parent.setOrdinal(orDefault, ordinal);
    }

    @Override
    public boolean setBlock(Extent orDefault, BlockState state) {
        return parent.setBlock(orDefault, state);
    }

    @Override
    public boolean setFullBlock(Extent orDefault, BaseBlock block) {
        return parent.setFullBlock(orDefault, block);
    }

    @Override
    public boolean setBiome(Extent orDefault, BiomeType biome) {
        return parent.setBiome(orDefault, biome);
    }

    @Override
    public int getOrdinal(Extent orDefault) {
        return parent.getOrdinal(orDefault);
    }

    @Override
    public char getOrdinalChar(Extent orDefault) {
        return parent.getOrdinalChar(orDefault);
    }

    @Override
    public BlockState getBlock(Extent orDefault) {
        return parent.getBlock(orDefault);
    }

    @Override
    public BaseBlock getFullBlock(Extent orDefault) {
        return parent.getFullBlock(orDefault);
    }

    @Override
    public CompoundTag getNbtData(Extent orDefault) {
        return parent.getNbtData(orDefault);
    }

    @Override
    public BlockState getOrdinalBelow(Extent orDefault) {
        return parent.getOrdinalBelow(orDefault);
    }

    @Override
    public BlockState getStateAbove(Extent orDefault) {
        return parent.getStateAbove(orDefault);
    }

    @Override
    public BlockState getStateRelativeY(Extent orDefault, int y) {
        return parent.getStateRelativeY(orDefault, y);
    }

    @Override
    public BlockVector2 toBlockVector2() {
        return parent.toBlockVector2();
    }

    @Override
    public Vector3 toVector3() {
        return parent.toVector3();
    }

    @Override
    public boolean equals(Object obj) {
        return parent.equals(obj);
    }

    @Override
    public int hashCode() {
        return parent.hashCode();
    }

    @Override
    public String toString() {
        return parent.toString();
    }

    @Override
    public BlockVector3 plus(BlockVector3 other) {
        return parent.plus(other);
    }
}
